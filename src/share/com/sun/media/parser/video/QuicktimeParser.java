/*
 * @(#)QuicktimeParser.java	1.76 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.parser.video;

import java.io.IOException;
import java.awt.Dimension;
import javax.media.Track;
import javax.media.IncompatibleSourceException;
import javax.media.BadHeaderException;
import javax.media.Time;
import javax.media.Duration;
import javax.media.TrackListener;
import javax.media.Buffer;
import javax.media.protocol.DataSource;
import javax.media.protocol.SourceStream;
import javax.media.protocol.PullSourceStream;
import javax.media.protocol.Seekable;
import javax.media.Format;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.CachedStream;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;
import com.sun.media.parser.BasicPullParser;
import com.sun.media.util.SettableTime;

public class QuicktimeParser extends BasicPullParser {

    private final boolean enableHintTrackSupport = true;

    private static ContentDescriptor[] supportedFormat = new ContentDescriptor[] {new ContentDescriptor("video.quicktime")};
    private PullSourceStream stream = null;
    private Track[] tracks;
    private Seekable seekableStream;
    private boolean mdatAtomPresent = false;
    private boolean moovAtomPresent = false;

    public final static int MVHD_ATOM_SIZE = 100;
    public final static int TKHD_ATOM_SIZE = 84;
    public final static int MDHD_ATOM_SIZE = 24;
    public final static int MIN_HDLR_ATOM_SIZE = 24;
    public final static int MIN_STSD_ATOM_SIZE = 8;
    public final static int MIN_STTS_ATOM_SIZE = 8;
    public final static int MIN_STSC_ATOM_SIZE = 8;
    public final static int MIN_STSZ_ATOM_SIZE = 8;
    public final static int MIN_STCO_ATOM_SIZE = 8;
    public final static int MIN_STSS_ATOM_SIZE = 8;
    public final static int MIN_VIDEO_SAMPLE_DATA_SIZE = 70;
    public final static int MIN_AUDIO_SAMPLE_DATA_SIZE = 20;
    public final static int TRACK_ENABLED = 0x1;
    public final static int TRACK_IN_MOVIE = 0x2;
    public final static int TRACK_IN_PREVIEW = 0x4;
    public final static int TRACK_IN_POSTER = 0x8;
    public final static String VIDEO = "vide";
    public final static String AUDIO = "soun";
    public final static String HINT = "hint";

    private final static int DATA_SELF_REFERENCE_FLAG = 0x1;

    private final static int HINT_NOP_IGNORE = 0;
    private final static int HINT_IMMEDIATE_DATA = 1;
    private final static int HINT_SAMPLE_DATA = 2;
    private final static int HINT_SAMPLE_DESCRIPTION = 3;

    private MovieHeader movieHeader = new MovieHeader();

    private int numTracks = 0;
    private int numSupportedTracks = 0;
    private int numberOfHintTracks = 0;

    private static int MAX_TRACKS_SUPPORTED = 100;
    private TrakList[] trakList = new TrakList[MAX_TRACKS_SUPPORTED];
    private TrakList currentTrack; // The better way is use pass it as parameter
    private int keyFrameTrack = -1;
    private SettableTime mediaTime = new SettableTime(0L);

    private int hintAudioTrackNum = -1;

    private boolean debug = false;
    private boolean debug1 = false;
    private boolean debug2 = false;
    // Used to make the seek and the subsequent readBytes call atomic
    // operations, so that the video and audio track
    // threads don't trample each other.
    private Object seekSync = new Object();


    private int tmpIntBufferSize = 16 * 1024;
    private byte[] tmpBuffer = new byte[tmpIntBufferSize * 4];
    /**
     * Quicktime format requires that the stream be seekable and
     * random accessible. 
     */
    protected boolean supports(SourceStream[] s) {
	return seekable;
    }

    public void setSource(DataSource source)
	throws IOException, IncompatibleSourceException {

	super.setSource(source);
	stream = (PullSourceStream) streams[0];
	seekableStream = (Seekable) streams[0];
    }

    private CachedStream getCacheStream() {
	return cacheStream;
    }

    public ContentDescriptor [] getSupportedInputContentDescriptors() {
	return supportedFormat;
    }

    public Track[] getTracks() throws IOException, BadHeaderException {
 	if (tracks != null)
 	    return tracks;
	
	if (seekableStream == null) {
	    return new Track[0];
	}

	if (cacheStream != null) {
	    // Disable jitter buffer during parsing of the header
	    cacheStream.setEnabledBuffering(false);
	}
	readHeader();
	if (cacheStream != null) {
	    cacheStream.setEnabledBuffering(true);
	}

	tracks = new Track[numSupportedTracks];

	// System.out.println("numTracks is " + numTracks);
	// System.out.println("numSupportedTracks is " + numSupportedTracks);
	int index = 0;
	for (int i = 0; i < numSupportedTracks; i++) {
	    TrakList trakInfo = trakList[i];
	    if (trakInfo.trackType.equals(AUDIO)) {
		tracks[i] = new AudioTrack(trakInfo);
// 		System.out.println("track id " + (index-1) + " : " +
// 				   tracks[index-1]);
	    } else if (trakInfo.trackType.equals(VIDEO)) {
		tracks[i] = new VideoTrack(trakInfo);
// 		System.out.println("track id " + (index-1) + " : " +
// 				   tracks[index-1]);
	    }
	}
	for (int i = 0; i < numSupportedTracks; i++) {
	    TrakList trakInfo = trakList[i];
	    if (trakInfo.trackType.equals(HINT)) {
		int trackBeingHinted = trakInfo.trackIdOfTrackBeingHinted;
		for (int j = 0; j < numTracks; j++) {
		    if (trackBeingHinted == trakList[j].id) {
			trakInfo.indexOfTrackBeingHinted = j;
			String hintedTrackType = trakList[j].trackType;
			String encodingOfHintedTrack = 
                           trakList[trakInfo.indexOfTrackBeingHinted].media.encoding;

			if (encodingOfHintedTrack.equals("agsm"))
			    encodingOfHintedTrack = "gsm";

			String rtpEncoding = encodingOfHintedTrack + "/rtp";
			if (hintedTrackType.equals(AUDIO)) {
			    int channels;
			    String encoding;
			    int frameSizeInBytes;
			    int samplesPerBlock;
			    int sampleRate;
			    Audio audio = (Audio) (trakList[j].media);

			    hintAudioTrackNum = i;

			    channels =  audio.channels;
			    frameSizeInBytes =  audio.frameSizeInBits / 8;
			    samplesPerBlock = audio.samplesPerBlock;
			    sampleRate = audio.sampleRate;

			    ((Hint) trakInfo.media).format =
				new AudioFormat(rtpEncoding,
						(double) sampleRate,
						8, // sampleSizeInBits [$$$ hardcoded]
						channels);

  			    tracks[i] = new HintAudioTrack(trakInfo,
  								 channels,
								 rtpEncoding,
  								 frameSizeInBytes,
  								 samplesPerBlock,
  								 sampleRate);
// 			    System.out.println("track id " + (index-1) + " : " +
// 					       tracks[index-1]);

			} else if (hintedTrackType.equals(VIDEO)) {

			    int indexOfTrackBeingHinted = trakInfo.indexOfTrackBeingHinted;
			    TrakList sampleTrakInfo = null;
			    if (indexOfTrackBeingHinted >= 0) {
				sampleTrakInfo = trakList[indexOfTrackBeingHinted];
			    }

			    int width = 0;
			    int height = 0;
			    if (sampleTrakInfo != null) {
				Video sampleTrakVideo = (Video) sampleTrakInfo.media;
				width = sampleTrakVideo.width;
				height = sampleTrakVideo.height;
			    }

			    if ( (width > 0) && (height > 0) ) {
				((Hint) trakInfo.media).format =
				    new VideoFormat(rtpEncoding,
						new Dimension(width, height),
						Format.NOT_SPECIFIED,
						null, Format.NOT_SPECIFIED);

// 				System.out.println("VIDEO HINT TRACK FORMAT is " +
// 						   ((Hint) trakInfo.media).format);
			    }
			    HintVideoTrack hintVideoTrack = 
				new HintVideoTrack(trakInfo);
                            tracks[i] = hintVideoTrack;

// 			    System.out.println("track id " + (index-1) + " : " +
// 					       tracks[index-1]);
			}
			break;
		    }
		}
	    }
	}
	return tracks;
    }


    private void /* for now void */ readHeader()
	throws IOException, BadHeaderException {

	while ( parseAtom() );
	if ( !moovAtomPresent )
	    throw new BadHeaderException("moov atom not present");
	
	if ( !mdatAtomPresent )
	    throw new BadHeaderException("mdat atom not present");

// 	System.out.println("Number of tracks is " + numTracks);
// 	System.out.println("Number of supported/valid tracks is " + numSupportedTracks);

	for (int i = 0; i < numSupportedTracks; i++) {
	    TrakList trak = trakList[i];

// 	    System.out.println("track index " + i + " encoding " +
// 			       trak.media.encoding);

// 	    System.out.println("Number of frames in track " +
// 			       trak.trackType + " : " +
// 			       + i +
// 			       " is " + trak.numberOfSamples);
// 	    System.out.println("Duration of track " + i +
// 			       trak.duration.getSeconds());
	    
	    if (trak.buildSyncTable()) {
		keyFrameTrack = i;
	    }
	    // System.out.println("$$$$ Call buildSamplePerChunkTable for track id " + trak.id);
	    trak.buildSamplePerChunkTable();
	    // Table is built for VIDEO and hint tracks but not
	    // for audio tracks.
	    if ( !trak.trackType.equals(AUDIO) ) {
		trak.buildSampleOffsetTable();

//  		System.out.println("Creating buildSampleOffsetTable for track " +
// 				   trak.trackType + " : " +
// 				   trak.sampleOffsetTable);

		trak.buildStartTimeAndDurationTable();

		float frameRate = (float) (trak.numberOfSamples /
		                     trak.duration.getSeconds());
		//$$$$$		((Video) trak.media).frameRate = frameRate;
		trak.media.frameRate = frameRate;

	    }
	    // NOTE: The next method should be called after buildSampleOffsetTable()
	    trak.buildCumulativeSamplePerChunkTable();
	    trak.media.createFormat();
	    // System.out.println("track " + (i+1) + " info: ");
	    // System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
	    // System.out.println(trak);
	    // System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n\n");
	}
    }


    public Time setPosition(Time where, int rounding) {

	double time = where.getSeconds();
	if (time < 0)
	    time = 0;
// 	if ( (keyFrameTrack != -1) && (tracks[keyFrameTrack].isEnabled()) ) {

	int keyT;

	if ( (((keyT = keyFrameTrack) != -1) && (tracks[keyFrameTrack].isEnabled())) ||
	     (((keyT = hintAudioTrackNum) != -1) && (tracks[hintAudioTrackNum].isEnabled())) ) {

			       
	    TrakList trakInfo = trakList[keyT];
	    int index = trakInfo.time2Index(time);

	    if (index < 0) {
		((MediaTrack)tracks[keyT]).setSampleIndex(trakInfo.numberOfSamples + 1); // past eom
	    } else {
		int syncIndex;
		
		if (keyT == keyFrameTrack) {
		    if (index >= trakInfo.syncSampleMapping.length) {
			index = trakInfo.syncSampleMapping.length - 1;
		    }
		    if (trakInfo.syncSampleMapping != null) {
			syncIndex = trakInfo.syncSampleMapping[index];
			double newtime = trakInfo.index2TimeAndDuration(syncIndex).startTime;
			time = newtime;
		    } else {
			// Note: you won't come here because syncSampleMapping wont
			// be null in this case.
			syncIndex = index;
		    }
		} else { // hint audio track
		    syncIndex = index;
		    double newtime = trakInfo.index2TimeAndDuration(syncIndex).startTime;
		    time = newtime;
		}
		((MediaTrack)tracks[keyT]).setSampleIndex(syncIndex);
	    }
	}

	for (int i = 0; i < numSupportedTracks; i++) {
	    if (i == keyT)
		continue;


	    if (!tracks[i].isEnabled())
		continue;

	    // TODO: See if you can just call a setPosition or
	    // setIndex method for each media type, instead of
	    // using if statement
	    TrakList trakInfo = trakList[i];
	    // Note that the time here may not be the same as the
	    // the "Time where" parameter passed into this method.
	    // The time may be changed if it doesn't map to a keyFrame
	    // in the Video track.
	    int index = trakInfo.time2Index(time);

// 	    if ( trakInfo.trackType.equals(VIDEO) || 
// 		 trakInfo.trackType.equals(HINT) ) {

	    if ( trakInfo.trackType.equals(VIDEO) || 
		 ( trakInfo.trackType.equals(HINT) &&
		   (tracks[i] instanceof HintVideoTrack)) ) {

		if (index < 0) {
		    ((MediaTrack)tracks[i]).setSampleIndex(trakInfo.numberOfSamples + 1); // past eom
		} else {
		    int syncIndex;
		    if (trakInfo.syncSampleMapping != null) {
			syncIndex = trakInfo.syncSampleMapping[index];
		    } else
			syncIndex = index;
		    ((MediaTrack)tracks[i]).setSampleIndex(syncIndex);
		}
	    } else { // TODO: if you have other track types, then check for AUDIO here

		if (index < 0) {
		    ((MediaTrack)tracks[i]).setChunkNumber(trakInfo.numberOfChunks + 1); // past eom
		} else {
		    int sampleOffsetInChunk;
		    
		    ((MediaTrack)tracks[i]).setSampleIndex(index);
		    // $$$$$ IMPORTANT TODO: fix this as the index2Chunk method
		    // takes index starting from 1, not 0
		    int chunkNumber = trakInfo.index2Chunk(index);
		    
		    if (chunkNumber != 0) {
			if ( trakInfo.constantSamplesPerChunk == -1) {
			    // Note samplesPerChunk array contains cumulative
			    // samples per chunk
			    sampleOffsetInChunk = index -
				trakInfo.samplesPerChunk[chunkNumber-1];
			} else {
			    // TODO: need to test this case
			    sampleOffsetInChunk = index -
				chunkNumber *
				trakInfo.constantSamplesPerChunk;
			}
		    } else {
			sampleOffsetInChunk = index;
		    }
		    ((AudioTrack)tracks[i]).setChunkNumberAndSampleOffset(chunkNumber,
									  sampleOffsetInChunk);
		}
	    }
	}
	if (cacheStream != null) {
	    synchronized(this) {
		cacheStream.abortRead();
	    }
	}
	synchronized(mediaTime) {
	    mediaTime.set(time);
	}
	return mediaTime;
    }


    public Time getMediaTime() {
	return null;  // TODO
    }

    public Time getDuration() {
	return movieHeader.duration;
    }


    /**
     * Returns a descriptive name for the plug-in.
     * This is a user readable string.
     */
    public String getName() {
	return "Parser for quicktime file format";
    }

    private boolean parseAtom() throws BadHeaderException {
	boolean readSizeField = false;

	try {
	    int atomSize = readInt(stream);
// 	    System.out.println("atomSize is " + atomSize);
	    readSizeField = true;
	    
	    String atom = readString(stream);
// 	    System.out.println("atom is " + atom);
	    
	    if ( atomSize < 8 )
		throw new BadHeaderException(atom + ": Bad Atom size " + atomSize);

	    if ( atom.equals("moov") )
		return parseMOOV(atomSize - 8);
	    if ( atom.equals("mdat") )
		return parseMDAT(atomSize - 8);
	    skipAtom(atom + " [not implemented]", atomSize - 8);
	    return true;
	} catch (IOException e) {
	    // System.err.println("parseAtom: IOException " + e);
	    if (!readSizeField) {
		// System.out.println("EOM");
		return false; // EOM. Parsing done
	    }
	    throw new BadHeaderException("Unexpected End of Media");
	}
    }

    private void skipAtom(String atom, int size) throws IOException {
	if (debug2)
	    System.out.println("skip unsupported atom " + atom);
	skip(stream, size);
    }

    /**
     * Required atoms are mvhd and trak
     * Doesn't say in the spec. that trak is a required atom,
     * but can we play a qt file without a trak atom??
     */
    private boolean parseMOOV(int moovSize) throws BadHeaderException {
	boolean trakAtomPresent = false;

	try {
	    moovAtomPresent = true;
	    long moovMax = getLocation(stream) + moovSize;
	    int remainingSize = moovSize;


	    int atomSize = readInt(stream);
	    String atom = readString(stream);

	    if ( atomSize < 8 )
		throw new BadHeaderException(atom + ": Bad Atom size " + atomSize);

	    if ( ! atom.equals("mvhd") ) {
		if (atom.equals("cmov"))
		    throw new BadHeaderException("Compressed movie headers are not supported");
		else
		    throw new BadHeaderException("Expected mvhd atom but got " + atom);
	    }
	    parseMVHD(atomSize - 8);
// 	    System.out.println("Duration of movie is " +
// 			       movieHeader.duration.getSeconds());
	    remainingSize -= atomSize;

	    // TODO: before calling parseXXX, should check if
	    // (atomSize - 8) >= remainingSize
	    while (remainingSize > 0) {
		atomSize = readInt(stream);
		atom = readString(stream);
		if (atom.equals("trak")) {
		    if (trakList[numSupportedTracks] == null) {
			trakList[numSupportedTracks] = currentTrack = new TrakList();
		    }
		    if (parseTRAK(atomSize - 8)) {
			numSupportedTracks++;
		    }
		    trakAtomPresent = true;
		    numTracks++;
		} else if (atom.equals("ctab")) {
		    parseCTAB(atomSize - 8);
		} else {
		    skipAtom(atom + " [atom in moov: not implemented]", atomSize - 8);
		}
               remainingSize -= atomSize;
	    }

	    if (!trakAtomPresent)
		throw new BadHeaderException("trak atom not present in trak atom container");
	    // Parsing is done if the MDAT atom has also been seen.
	    return !mdatAtomPresent;
	} catch (IOException e) {
	    throw new BadHeaderException("IOException when parsing the header");
	}
    }

    private boolean parseMDAT(int size) throws BadHeaderException {
	try {
	    mdatAtomPresent = true;
	    movieHeader.mdatStart = getLocation( stream ); // Need this ??? TODO
	    movieHeader.mdatSize = size; // Need this ??? TODO
	    /** Seek past the MDAT atom only if the MOOV atom
	     *  hasn't been seen yet. 
	     *  The only reason to seek past the MDAT atom even if the
	     *  MOOV atom has been seen is to handle top level atoms
	     *  like PNOT (Movie Preview data). We currently don't support
	     *  PNOT atom.
	     *  Also, We don't know how fast the
	     *  seek is. If it is based on RandomAccessFile like
	     *  Sun's file datasource, then it is pretty fast.
	     *  But if it a cached http datasource over a slow
	     *  internet connection, then the seek will take a long
	     *  time. So seeking past the MDAT atom is not done unless
	     *  the MOOV atom hasn't been seen yet.
	     */
	    if (!moovAtomPresent) {
		skip(stream, size);
		return true; // Parsing continues as MOOV atom hasn't been seen
	    }
	    return false; // Parsing done
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past MDAT atom");
	}
    }

    /**
     * MVHD is a leaf atom of size MVHD_ATOM_SIZE (100)
     */
    private void parseMVHD(int size) throws BadHeaderException {
	try {
	    if (size != MVHD_ATOM_SIZE) {
		throw new BadHeaderException("mvhd atom: header size is incorrect");
	    }

	    // Skip version(1), flags(3), create time (4), mod time (4)
	    skip(stream, 12);

	    movieHeader.timeScale = readInt(stream);
	    int duration = readInt(stream);
	    movieHeader.duration = new Time((double) duration /
					    movieHeader.timeScale);
	    int preferredRate = readInt(stream);
	    int preferredVolume = readShort(stream);
	    
	    skip(stream, 10); // Reserved 
	    skip(stream, 36); // MATRIX
	    
	    int previewTime = readInt(stream);
	    int previewDuration = readInt(stream);
	    int posterTime = readInt(stream);
	    int selectionTime = readInt(stream);
	    int selectionDuration = readInt(stream);
	    int currentTime = readInt(stream);
	    int nextTrackID = readInt(stream);
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past MVHD atom");
	}
    }
    

    /**
     * Required atoms are tkhd and mdia
     */
    private boolean parseTRAK(int trakSize) throws BadHeaderException {
	boolean mdiaAtomPresent = false;
	boolean supported = false; // is trackType supported
	try {
	    int remainingSize = trakSize;
	    int atomSize = readInt(stream);
	    String atom = readString(stream);
	    
	    if ( atomSize < 8 )
		throw new BadHeaderException(atom + ": Bad Atom size " + atomSize);
	    
	    if ( ! atom.equals("tkhd") ) {
		throw new BadHeaderException("Expected tkhd atom but got " + atom);
	    }
	    parseTKHD(atomSize - 8);
	    remainingSize -= atomSize;
	    // TODO: before calling parseXXX, should check if
	    // (atomSize - 8) >= remainingSize
	    while (remainingSize > 0) {
		atomSize = readInt(stream);
		atom = readString(stream);
		if (atom.equals("mdia")) {
		    supported = parseMDIA(atomSize - 8);
		    mdiaAtomPresent = true;
		} else if (atom.equals("tref")) {
		    parseTREF(atomSize - 8);
		} else {
		    skipAtom(atom + " [atom in trak: not implemented]", atomSize - 8);
		}
		remainingSize -= atomSize;
	    }
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past TRAK atom");
	}
	if (!mdiaAtomPresent)
	    throw new BadHeaderException("mdia atom not present in trak atom container");

	// FYI$$: Some files like vinton.mov have an audio track but
	// do not have a stsd chunk. Apple's movie player plays only
	// the video in this case. This case is now handled.
	// gracefully. But there may be other cases like this.
	// We need to update the if statement accordingly.
	if ( supported &&
	     (currentTrack.media == null) ) {
	    supported = false;
	}
	return supported;
    }
	

    private void parseCTAB(int ctabSize) throws BadHeaderException { // TODO
	try {
	    // System.out.println("ctab not handled yet");
	    skip(stream, ctabSize); // DUMMY 
	} catch (IOException e) {
	    //TODO
	    throw new BadHeaderException("....");
	}
    }
    
    
    /**
     * TKHD is a leaf atom of size TKHD_ATOM_SIZE (84)
     */
    private void parseTKHD(int tkhdSize) throws BadHeaderException {
	try {
	    if (tkhdSize != TKHD_ATOM_SIZE) {
		throw new BadHeaderException("mvhd atom: header size is incorrect");
	    }
	    int iVersionPlusFlag = readInt(stream);
	    currentTrack.flag = iVersionPlusFlag & 0xFFFFFF;
	    skip(stream, 8); // Skip creation time and modification time
	    currentTrack.id = readInt(stream);
// 	    System.out.println("<<<<<<<< id is >>>>>> " + currentTrack.id);
// 	    System.out.println("<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>");
// 	    System.out.println("<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>");
// 	    System.out.println("<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>");
// 	    System.out.println("<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>");
// 	    System.out.println("<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>");
// 	    System.out.println("<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>");
	    skip(stream, 4); // Skip reserved field
	    int duration = readInt(stream);
	    currentTrack.duration = new Time((double) duration /
					     movieHeader.timeScale);
	    skip(stream, tkhdSize -4 -8 -4 -4 -4); // Skip the rest of the fields
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past TKHD atom");
	}
    }
    
    /**
     * Required atoms are mdhd, hdlr and minf
     * Doesn't say in the spec. that hdlr or minf is a required atom,
     * but can we play a qt file without a hdlr or minf atoms
     * hdlr atom should come before minf atom
     * Return true if the trackType is supported
     *
     * moov/trak/mdia
     */
    private boolean parseMDIA(int mdiaSize) throws BadHeaderException {
	boolean hdlrAtomPresent = false;
	boolean minfAtomPresent = false;;

	try {
	    currentTrack.trackType = null;
	    int remainingSize = mdiaSize;
	    int atomSize = readInt(stream);
	    String atom = readString(stream);

	    if ( atomSize < 8 )
		throw new BadHeaderException(atom + ": Bad Atom size " + atomSize);
	    
	    if ( ! atom.equals("mdhd") ) {
		throw new BadHeaderException("Expected mdhd atom but got " + atom);
	    }
	    parseMDHD(atomSize - 8);
	    remainingSize -= atomSize;
	    // TODO: before calling parseXXX, should check if
	    // (atomSize - 8) >= remainingSize
	    while (remainingSize > 0) {
		atomSize = readInt(stream);
		atom = readString(stream);
		if (atom.equals("hdlr")) {
		    parseHDLR(atomSize - 8); // Updates trackType in currentTrack
		    hdlrAtomPresent = true;
		} else if (atom.equals("minf")) {
		    if (currentTrack.trackType == null) {
			throw new BadHeaderException("In MDIA atom container minf atom appears before hdlr");
		    }
		    if (currentTrack.supported) {
			parseMINF(atomSize - 8);
		    } else {
			skipAtom(atom + " [atom in mdia] as trackType " +
				 currentTrack.trackType + " is not supported",
				 atomSize - 8);
		    }
		    minfAtomPresent = true;
		} else {
		    skipAtom(atom + " [atom in mdia: not implemented]", atomSize - 8);
		}
		remainingSize -= atomSize;
	    }
	    if (!hdlrAtomPresent)
		throw new BadHeaderException("hdlr atom not present in mdia atom container");
	    if (!minfAtomPresent)
		throw new BadHeaderException("minf atom not present in mdia atom container");

	    return (currentTrack.supported);
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past MDIA atom");
	}
    }

    /**
     * moov/trak/mdia/mdhd
     */
    private void parseMDHD(int mdhdSize) throws BadHeaderException {
	try {
	    if (mdhdSize != MDHD_ATOM_SIZE) {
		throw new BadHeaderException("mdhd atom: header size is incorrect");
	    }
	    
	    // Skip version(1), flags(3), creation time(4), modification time(4)
	    skip(stream, 12);
	    int timeScale = readInt(stream);
	    int duration = readInt(stream);
	    currentTrack.mediaDuration = new Time((double) duration / timeScale);
	    currentTrack.mediaTimeScale = timeScale;
	    skip(stream, 4); // Skip language(2) and quality(2) fields
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past MDHD atom");
	}
    }

    /**
     * moov/trak/mdia/hdlr
     */
    private void parseHDLR(int hdlrSize) throws BadHeaderException {
	try {
	    if (hdlrSize < MIN_HDLR_ATOM_SIZE) {
		throw new BadHeaderException("hdlr atom: header size is incorrect");
	    }
	    
	    // Skip version(1), flags(3), component type(4)
	    skip(stream, 8);
	    currentTrack.trackType = readString(stream);
	    // System.out.println("track type is " + currentTrack.trackType);
	    currentTrack.supported = isSupported(currentTrack.trackType);

	    // Skip the rest of the fields including the variable component
	    // name field
	    skip(stream, hdlrSize -8 -4);
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past HDLR atom");
	}
    }

    // Typically HINT tracks will have tref atom specifying the track being hinted
    // Typically the first child atom is the hint atom. NOTE$$: Assuming that this is the
    // case.
    private void parseTREF(int size) throws BadHeaderException {
	try {

	    int childAtomSize = readInt(stream);
	    size -= 4;
	    // System.out.println("parseTREF: childAtomSize is " + childAtomSize);
	    
	    String atom = readString(stream);
	    size -= 4;
	    
	    if (atom.equalsIgnoreCase("hint")) {
		currentTrack.trackIdOfTrackBeingHinted = readInt(stream);
		size -= 4;
		// System.out.println("trackBeingHinted is " + currentTrack.trackIdOfTrackBeingHinted);
	    }
	    skip(stream, size);
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past HDLR atom");
	}
    }
	

    /**
     * Required atoms are [vsg]mhd, hdlr
     * Optional atoms are dinf and stbl
     * Currently we skip [vsg]mhd, hdlr and dinf atoms and only
     * handle stbl
     */
    
    private void parseMINF(int minfSize) throws BadHeaderException {

	boolean hdlrAtomPresent = false;
	try {
	    int remainingSize = minfSize;
	    int atomSize = readInt(stream);
	    String atom = readString(stream);

	    if ( atomSize < 8 )
		throw new BadHeaderException(atom + ": Bad Atom size " + atomSize);
	    
	    if ( ! atom.endsWith("hd") ) {
		throw new BadHeaderException("Expected media information header atom but got " + atom);
	    }
	    skipAtom(atom + " [atom in minf: not implemented]", atomSize - 8);

	    remainingSize -= atomSize;
	    // TODO: before calling parseXXX, should check if
	    // (atomSize - 8) >= remainingSize
	    while (remainingSize > 0) {
		atomSize = readInt(stream);
		atom = readString(stream);
		if (atom.equals("hdlr")) {
		    skipAtom(atom + " [atom in minf: not implemented]", atomSize - 8);
		    hdlrAtomPresent = true;
		} else if (atom.equals("dinf")) {
		    parseDINF(atomSize - 8);
		} else if (atom.equals("stbl")) {
		    parseSTBL(atomSize - 8);
		} else {
		    skipAtom(atom + " [atom in minf: not implemented]", atomSize - 8);
		}
		remainingSize -= atomSize;
	    }
	    if (!hdlrAtomPresent)
		throw new BadHeaderException("hdlr atom not present in minf atom container");

	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past MINF atom");
	}
    }


    private void parseDINF(int dinfSize) throws BadHeaderException {
	try {
	    int remainingSize = dinfSize;

	    // System.out.println("dinfSize is " + dinfSize);
	    while (remainingSize > 0) {
		int atomSize = readInt(stream);
		String atom = readString(stream);
		// System.out.println("dinf: atomSize is " + atomSize);
		// System.out.println("dinf: atom is " + atom);
		if (atom.equals("dref")) {
		    parseDREF(atomSize - 8);
		} else {
		    skipAtom(atom + " [Unknown atom in dinf]", atomSize - 8);
		}
		remainingSize -= atomSize;
	    }
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past DIMF atom");
	}
    }
    


    private void parseDREF(int drefSize) throws BadHeaderException {
	try {
	    // TODO: add size check
// 	    if (drefSize < MIN_DREF_ATOM_SIZE) {
// 		throw new BadHeaderException("dref atom: header size is incorrect");
// 	    }

	    skip(stream, 4); // skip version and flags
	    int numEntries = readInt(stream);
	    // System.out.println("dref: number of entries is " + numEntries);

	    for (int i = 0; i < numEntries; i++) {
		int drefEntrySize = readInt(stream);
		// System.out.println("drefEntrySize is " + drefSize);
		int type = readInt(stream);
		// System.out.println("dref entry type is " + type);
		/**
		 * Version: A 1-byte specification of the version of
		 * these data references.
		 * Flags: A 3-byte space for data reference flags.
		 * There is one defined flag. Self reference This flag
		 * indicates that the media's data is in the same file
		 * as the movie atom. On the Macintosh, and other file
		 * systems with multifork files, set this flag to 1
		 * even if the data resides in a different fork
		 * from the movie atom. This flag's value is 0x0001.
		 */
		int versionPlusFlag = readInt(stream);
		// System.out.println("versionPlusFlag is " + versionPlusFlag);
		skip(stream, drefEntrySize -(4+4+4));
		if ( (versionPlusFlag & DATA_SELF_REFERENCE_FLAG) <= 0 ) {
		    throw new BadHeaderException("Only self contained Quicktime movies are supported");
		}
	    }
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past DREF atom");
	}
    }
    

    /**
     * Required atoms are none.
     */
    private void parseSTBL(int stblSize) throws BadHeaderException {
	try {
	    int remainingSize = stblSize;

	    while (remainingSize > 0) {
		int atomSize = readInt(stream);
		String atom = readString(stream);
		if (atom.equals("stsd")) {
		    parseSTSD(atomSize - 8);
		} else if (atom.equals("stts")) {
		    parseSTTS(atomSize - 8);
		} else if (atom.equals("stss")) {
		    parseSTSS(atomSize - 8);
		} else if (atom.equals("stsc")) {
		    parseSTSC(atomSize - 8);
		} else if (atom.equals("stsz")) {
		    parseSTSZ(atomSize - 8);
		} else if (atom.equals("stco")) {
		    parseSTCO(atomSize - 8);
		} else if (atom.equals("stsh")) {
		    //		    parseSTSH(atomSize - 8);
		    skipAtom(atom + " [not implemented]", atomSize - 8);
		} else {
		    skipAtom(atom + " [UNKNOWN atom in stbl: ignored]", atomSize - 8);
		}
		remainingSize -= atomSize;
	    }
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past STBL atom");
	}
    }


    /**
     * STSD is a leaf atom of minimum size MIN_STSD_ATOM_SIZE (8)
     */
    private void parseSTSD(int stsdSize) throws BadHeaderException {
	// System.out.println("stsd size is " + stsdSize);

	try {
	    if (stsdSize < MIN_STSD_ATOM_SIZE) {
		throw new BadHeaderException("stsd atom: header size is incorrect");
	    }

	    // Note: if the trackType is not
	    // supported the minf atom is skipped and so you will not
	    // come here.

	    skip(stream, 4); // skip version and flags

	    int numEntries = readInt(stream);

	    //$$ System.out.println("stsd: numEntries is " + numEntries);
	    if ( numEntries > 1) {
		// System.err.println("Multiple formats in a track not supported");
	    }
	    for (int i = 0; i < numEntries; i++) {
		int sampleDescriptionSize = readInt(stream);
		//$$ System.out.println("stsd: sampleDescriptionSize is " + sampleDescriptionSize);
		// CHECK ?? spec. says int but is it a 4 letter String????
		String encoding = readString(stream);
		// System.out.println("stsd: encoding is " + encoding);

		if (i != 0) {
		    skip(stream, sampleDescriptionSize - 8);
		    continue;
		}

		// skip(stream, 8); // 6 reserved bytes + 2 for data reference index
		skip(stream, 6); // 6 reserved bytes

		// TODO: check of sampleDescriptionSize is atleast 16 bytes
		if (currentTrack.trackType.equals(VIDEO)) {
		    currentTrack.media = 
			parseVideoSampleData(encoding,
					     sampleDescriptionSize -4 -4 -6);
		} else if (currentTrack.trackType.equals(AUDIO)) {
		    currentTrack.media = 
			parseAudioSampleData(encoding,
					     sampleDescriptionSize -4 -4 -6);
		} else if (currentTrack.trackType.equals(HINT)) {
		    numberOfHintTracks++;
		    currentTrack.media = 
			parseHintSampleData(encoding,
					     sampleDescriptionSize -4 -4 -6);
		} else {
		    // Note: you will never come into this else block.
		    // If the trackType is not supported, the minf atom is skipped
		    // and so you will not come here.

		    skip(stream, 
			 sampleDescriptionSize - 4 - 4 -6);
		}
		
	    }

	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past STSD atom");
	}
    }

    private Video parseVideoSampleData(String encoding, int dataSize)
	throws IOException, BadHeaderException {
	// TODO: check for  dataSize >= MIN_VIDEO_SAMPLE_DATA_SIZE
	skip(stream, 2); // data reference index
	/**
	 * Skip versiom(2), Revision Level (2), Vendor(4),
	 * Temporal Quality (4), Spatial Quality (4);
	 */
	skip(stream, 16);

	Video video = new Video();
	video.encoding = encoding;

	video.width = readShort(stream);
	video.height = readShort(stream);
	/**
	 * Skip Horizontal resolution (4),
	 * Skip Vertical resolution (4),
	 * Skip data size (4),
	 * Skip frame count (2),
	 */
	skip(stream, 14);
	/* Skip compressor name */
	skip(stream, 32);
	video.pixelDepth = readShort(stream);
	video.colorTableID = readShort(stream);

	int colorTableSize = 0;
	if (video.colorTableID == 0) {
	    // Color table follows colorTableID
	    colorTableSize = readInt(stream);
	    skip(stream, colorTableSize -4); // TODO: DUMMY
	}

	skip(stream, dataSize - 2 - MIN_VIDEO_SAMPLE_DATA_SIZE -
	           - colorTableSize); // 2 for data ref. index
	return video;
    }


    private Audio parseAudioSampleData(String encoding, int dataSize)
	throws IOException, BadHeaderException {
	skip(stream, 2); // data reference index
	// TODO: check for  dataSize >= MIN_AUDIO_SAMPLE_DATA_SIZE

	/**
	 * Skip versiom(2), Revision Level (2), Vendor(4),
	 */
	skip(stream, 8);

	Audio audio = new Audio();
	audio.encoding = encoding;
	audio.channels = readShort(stream);
	audio.bitsPerSample = readShort(stream);

	/**
	 * Skip compression id (2),
	 * Skip packset size (2),
	 */
	skip(stream, 4);
	int sampleRate = readInt(stream);
	/**
	 * The media timeScale (foound in the mdhd atom) seems to
	 * represent the sampleRate (because it represents units/sec)
	 * This sampleRate field for some reason contains the
	 * timeScale shifted left by 16 bits. In other words sampleRate
	 * is media timeScale times 65536.
	 * Instead of dividing this by 65536, I am just using the
	 * media timeScale as sampleRate
	 * CHECK
	 */
	// audio.sampleRate = sampleRate >> 16; // Also works
	audio.sampleRate = currentTrack.mediaTimeScale;
	// System.out.println("mediaTimeScale is " + currentTrack.mediaTimeScale);

	skip(stream, dataSize -2 -MIN_AUDIO_SAMPLE_DATA_SIZE); // 2 for data ref. index
	return audio;
    }


    private Hint parseHintSampleData(String encoding, int dataSize)
	throws IOException, BadHeaderException {

	// TODO: check for  dataSize >= MIN_HINT_SAMPLE_DATA_SIZE

	if (!encoding.equals("rtp ")) {
	    System.err.println("Hint track Data Format is not rtp");
	}
	// System.out.println("parseHintSampleData: dataSize is " + dataSize);


	Hint hint = new Hint();

	int dataReferenceIndex = readShort(stream);
	int hintTrackVersion = readShort(stream);

 	if (hintTrackVersion == 0) {
 	    System.err.println("Hint Track version #0 is not supported");
 	    System.err.println("Use QuickTimePro to convert it to version #1");
	    currentTrack.supported = false;
	    if ((dataSize - 2 - 2) > 0)
		skip(stream, (dataSize -2 -2));
	    return hint;
	}

	int lastCompatibleHintTrackVersion = readShort(stream);


	int maxPacketSize = readInt(stream);
	currentTrack.maxPacketSize = maxPacketSize;
	int remaining = dataSize -2 -2 -2 -4;

	if (debug1) {
	    System.out.println("dataReferenceIndex is " + dataReferenceIndex);
	    System.out.println("hintTrackVersion is " + hintTrackVersion);
	    System.out.println("lastCompatibleHintTrackVersion is " + lastCompatibleHintTrackVersion);
	    System.out.println("maxPacketSize is " + maxPacketSize);
	    System.out.println("remaining is " + remaining);
	}

	while (remaining > 8) {
	    // Additional data is present;

	    int entryLength = readInt(stream);
	    remaining -= 4;
	    if ( entryLength > 8) {
		if (debug2)
		    System.out.println("entryLength is " + entryLength);
		// entryLength -= 4;
		String dataTag = readString(stream);
		if (debug2)
		    System.out.println("dataTag is " + dataTag);
		remaining -= 4;
		// entryLength -= 4;
		// TODO: assuming that the data tag is 'tims'. It can be tsro,snro,rely
		if (dataTag.equals("tims")) {
		    // 32-bit integer specifying the RTP timescale. This entry is
		    // required for RTP data.
		    int rtpTimeScale = readInt(stream);
		    // System.out.println("  rtpTimeScale is " + rtpTimeScale);
		    // currentTrack.rtpTimeScale = dataValue;
		    // entryLength -= 4;
		    remaining -= 4;
		} else if (dataTag.equals("tsro")) {
		    // 32-bit integer specifying the offset to add to the stored
		    // timestamp when sending RTP packets. If this entry is not
		    // present, a random offset should be used, as specified by the
		    // IETF. If this entry is 0, use an offset of 0 (no offset).

		    System.out.println("QuicktimeParser: rtp: tsro dataTag not supported");
		    int rtpTimeStampOffset = readInt(stream);
		    remaining -= 4;
		} else if (dataTag.equals("snro")) {
		    // 32-bit integer specifying the offset to add to the sequence
		    // number when sending RTP packets. If this entry is not present, a
		    // random offset should be used, as specified by the IETF. If this
		    // entry is 0, use an offset of 0 (no offset).

		    System.out.println("QuicktimeParser: rtp: snro dataTag not supported");
		    int rtpSequenceNumberOffset = readInt(stream);
		    // System.out.println("rtpSequenceNumberOffset is " + rtpSequenceNumberOffset);
		    remaining -= 4;
		} else if (dataTag.equals("rely")) {
		    // 8-bit flag indicating whether this track should or must be sent
		    // over a reliable transport, such as TCP/IP. If this entry is not
		    // present, unreliable transport should be used, such as RTP/UDP.
		    // The current client software for QuickTime streaming will only
		    // receive streaming tracks sent using RTP/UDP.

		    System.out.println("QuicktimeParser: rtp: rely dataTag not supported");
		    int rtpReliableTransportFlag = readByte(stream);
		    // System.out.println("rtpReliableTransportFlag is " + rtpReliableTransportFlag);
		    remaining--;
		} else {
		    // Unknown flag: Error.
		    // TODO: handle this without skipping if possible
		    // May not be possible because we don't know how many bytes
		    // to skip before the next tag.
		    skip(stream, remaining);
		    remaining = 0;
		}
	    } else {
		skip(stream, remaining);
		remaining = 0;
		break;
	    }
	}
	if (remaining > 0)
	    skip(stream, remaining);
	return hint;
    }


    /**
     * Time to Sample atom	
     * STTS is a leaf atom of minimum size MIN_STTS_ATOM_SIZE (8)
     */
    private void parseSTTS(int sttsSize) throws BadHeaderException {
	if (debug2)
	    System.out.println("parseSTTS: " + sttsSize);
	try {
	    
	    if (sttsSize < MIN_STTS_ATOM_SIZE) {
		throw new BadHeaderException("stts atom: header size is incorrect");
	    }
	    
	    /**
	     * Skip versiom(1), Flags(3)
	     */
	    skip(stream, 4);
	    int numEntries = readInt(stream);
	    
	    if (debug2)
		System.out.println("numEntries is " + numEntries);
	    int requiredSize = (sttsSize - MIN_STTS_ATOM_SIZE - numEntries*8);
	    if ( requiredSize < 0) {
		throw new BadHeaderException("stts atom: inconsistent number_of_entries field");
	    }
	    int totalNumSamples = 0;

	    double timeScaleFactor = (1.0 / currentTrack.mediaTimeScale);
	    if ( numEntries == 1) {
 		totalNumSamples = readInt(stream);
		currentTrack.durationOfSamples = readInt(stream) * timeScaleFactor;
	    } else {
		int[] timeToSampleIndices = new int[numEntries];
		double[] durations = new double[numEntries];

		timeToSampleIndices[0] = readInt(stream);
		totalNumSamples += timeToSampleIndices[0];
		durations[0] = readInt(stream) * timeScaleFactor *
		    timeToSampleIndices[0];
		                       
		int remaining = numEntries - 1; // As first 2 entries is already read.
		// 2 ints are written in each loop
		int numIntsWrittenPerLoop = 2;
		// integer division
		int maxEntriesPerLoop = tmpIntBufferSize / numIntsWrittenPerLoop;
		int i = 1;
		while (remaining > 0) {
		    int numEntriesPerLoop =
			(remaining > maxEntriesPerLoop) ? maxEntriesPerLoop : remaining;

		    readBytes(stream, tmpBuffer,
			      numEntriesPerLoop * numIntsWrittenPerLoop * 4);


		    int offset = 0;
		    for (int ii = 1; ii <= numEntriesPerLoop; ii++, i++) {
			timeToSampleIndices[i] = 
			    parseIntFromArray(tmpBuffer, offset, true);
			offset += 4;
			int value = parseIntFromArray(tmpBuffer, offset, true);
			offset += 4;
			durations[i] += ( value * timeScaleFactor *
					  timeToSampleIndices[i] +
					  durations[i-1] );
			totalNumSamples += timeToSampleIndices[i];
			timeToSampleIndices[i] = totalNumSamples;
		    }
		    remaining -= numEntriesPerLoop;
		}
		currentTrack.timeToSampleIndices = timeToSampleIndices;
		currentTrack.cumulativeDurationOfSamples = durations;
	    }

	    if (currentTrack.numberOfSamples == 0) {
		currentTrack.numberOfSamples = totalNumSamples;
	    } else {
		// TODO: if not they are inconsistent: should throw BadHeaderException
	    }
	    

	    skip(stream, requiredSize);
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past STTS atom");
	}
    }

    private void parseSTSC(int stscSize) throws BadHeaderException {
	try {
	    if (stscSize < MIN_STSC_ATOM_SIZE) {
		throw new BadHeaderException("stsc atom: header size is incorrect");
	    }
	    
	    /**
	     * Skip versiom(1), Flags(3)
	     */
	    skip(stream, 4);
	    int numEntries = readInt(stream);
	    int requiredSize = (stscSize - MIN_STSC_ATOM_SIZE - numEntries*12);
	    if ( requiredSize < 0) {
		throw new BadHeaderException("stsc atom: inconsistent number_of_entries field");
	    }
	    /**
	     * At this point we don't know how many chunks there are
	     * and so we cannot compute the samplePerChunk array
	     * TODO: make use of the sampleDescriptionId field
	     */
	    int compactSamplesChunkNum[] = new int[numEntries];
	    int compactSamplesPerChunk[] = new int[numEntries];
	    byte[] tmpBuf = new byte[numEntries*4*3];
	    readBytes(stream, tmpBuf, numEntries*4*3);
	    int offset = 0;
	    for (int i = 0; i < numEntries; i++) {
 		compactSamplesChunkNum[i] = parseIntFromArray(tmpBuf, offset, true);
		offset += 4;
 		compactSamplesPerChunk[i] = parseIntFromArray(tmpBuf, offset, true);
		offset += 4;
 		// int sampleDescriptionId = readInt(stream);
		offset += 4; // skip next 4 bytes
	    }
	    tmpBuf = null;
	    currentTrack.compactSamplesChunkNum = compactSamplesChunkNum;
	    currentTrack.compactSamplesPerChunk = compactSamplesPerChunk;
	    skip(stream, requiredSize);
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past STSC atom");
	}
    }

    /**
     * Sample Size Atom
     * STSZ is a leaf atom of minimum size MIN_STSZ_ATOM_SIZE (8)
     */
    private void parseSTSZ(int stszSize) throws BadHeaderException {
	if (debug2)
	    System.out.println("parseSTSZ: " + stszSize);
	try {
	    if (stszSize < MIN_STSZ_ATOM_SIZE) {
		throw new BadHeaderException("stsz atom: header size is incorrect");
	    }
	    
	    /**
	     * Skip versiom(1), Flags(3)
	     */
	    skip(stream, 4);
	    currentTrack.sampleSize = readInt(stream);
	    if (currentTrack.sampleSize != 0) {
		// All samples are of same sample size
		skip(stream, stszSize - MIN_STSZ_ATOM_SIZE);
		currentTrack.media.maxSampleSize = currentTrack.sampleSize;
		return;
            }
	    
	    // All samples are not of same size
	    if ( (stszSize - MIN_STSZ_ATOM_SIZE) < 4) { // for numEntries
		throw new BadHeaderException("stsz atom: incorrect atom size");
            }
	    
	    int numEntries = readInt(stream);
	    if (currentTrack.numberOfSamples == 0) {
		currentTrack.numberOfSamples = numEntries; // TODO: ????
	    } else {
		    // TODO: if not they are inconsistent: should throw BadHeaderException
	    }
	    
	    int requiredSize = (stszSize - MIN_STSZ_ATOM_SIZE 
                                - 4 // for numEntries
                                - numEntries*4);
	    if ( requiredSize < 0) {
		throw new BadHeaderException("stsz atom: inconsistent number_of_entries field");
	    }
	    int[] sampleSizeArray = new int[numEntries];
	    int maxSampleSize = Integer.MIN_VALUE;
	    int value;

	    int remaining = numEntries;
	    // 1 int is written in each loop
	    int numIntsWrittenPerLoop = 1;
	    int maxEntriesPerLoop = tmpIntBufferSize / numIntsWrittenPerLoop;
	    int i = 0;
	    while (remaining > 0) {
		int numEntriesPerLoop =
		    (remaining > maxEntriesPerLoop) ? maxEntriesPerLoop : remaining;
		
		readBytes(stream, tmpBuffer,
			  numEntriesPerLoop * numIntsWrittenPerLoop * 4);
		int offset = 0;
		for (int ii = 1; ii <= numEntriesPerLoop; ii++, i++) {
		    value = parseIntFromArray(tmpBuffer, offset, true);
		    offset += 4;
		    if (value > maxSampleSize)
			maxSampleSize = value;
		    sampleSizeArray[i] = value;
		}
		remaining -= numEntriesPerLoop;
	    }
	    currentTrack.sampleSizeArray = sampleSizeArray;
	    currentTrack.media.maxSampleSize = maxSampleSize;
	    skip(stream, requiredSize);
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past STSZ atom");
	}
    }

    /**
     * Chunk offset atom
     * STCO is a leaf atom of minimum size MIN_STCO_ATOM_SIZE (8)
     */
    private void parseSTCO(int stcoSize) throws BadHeaderException {
	if (debug2)
	    System.out.println("rtp:parseSTCO: " + stcoSize);

	try {
	    if (stcoSize < MIN_STCO_ATOM_SIZE) {
		throw new BadHeaderException("stco atom: header size is incorrect");
	    }
	    
	    /**
	     * Skip versiom(1), Flags(3)
	     */
	    skip(stream, 4);
	    // numEntries should be equal to number of Chunks
	    int numEntries = readInt(stream);
	    currentTrack.numberOfChunks = numEntries;
	    int[] chunkOffsets = new int[numEntries];
	    int requiredSize = (stcoSize - MIN_STCO_ATOM_SIZE - numEntries*4);
	    if ( requiredSize < 0) {
		throw new BadHeaderException("stco atom: inconsistent number_of_entries field");
	    }

	    int remaining = numEntries;
	    // 1 int is written in each loop
	    int numIntsWrittenPerLoop = 1;
	    int maxEntriesPerLoop = tmpIntBufferSize / numIntsWrittenPerLoop;
	    int i = 0;
	    while (remaining > 0) {
		int numEntriesPerLoop =
		    (remaining > maxEntriesPerLoop) ? maxEntriesPerLoop : remaining;
		
		readBytes(stream, tmpBuffer,
			  numEntriesPerLoop * numIntsWrittenPerLoop * 4);
		int offset = 0;
		for (int ii = 1; ii <= numEntriesPerLoop; ii++, i++) {
		    chunkOffsets[i] = parseIntFromArray(tmpBuffer, offset, true);
		    offset += 4;
		}
		remaining -= numEntriesPerLoop;
	    }
	    currentTrack.chunkOffsets = chunkOffsets;
	    skip(stream, requiredSize);
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past STCO atom");
	}
    }

    /**
     * Sync sample atom
     * STSS is a leaf atom of minimum size MIN_STSS_ATOM_SIZE (8)
     */
    private void parseSTSS(int stssSize) throws BadHeaderException {
	try {
	    if (stssSize < MIN_STSS_ATOM_SIZE) {
		throw new BadHeaderException("stss atom: header size is incorrect");
	    }
	    
	    /**
	     * Skip versiom(1), Flags(3)
	     */
	    skip(stream, 4);
	    int numEntries = readInt(stream);

	    int requiredSize = (stssSize - MIN_STSS_ATOM_SIZE - numEntries*4);
	    if ( requiredSize < 0) {
		throw new BadHeaderException("stss atom: inconsistent number_of_entries field");
	    }

	    if (numEntries < 1) {
		skip(stream, requiredSize);
		return;
	    }

	    int[] syncSamples = new int[numEntries];

	    int remaining = numEntries;
	    // 1 int is written in each loop
	    int numIntsWrittenPerLoop = 1;
	    int maxEntriesPerLoop = tmpIntBufferSize / numIntsWrittenPerLoop;
	    int i = 0;
	    while (remaining > 0) {
		int numEntriesPerLoop =
		    (remaining > maxEntriesPerLoop) ? maxEntriesPerLoop : remaining;
		
		readBytes(stream, tmpBuffer,
			  numEntriesPerLoop * numIntsWrittenPerLoop * 4);
		int offset = 0;
		for (int ii = 1; ii <= numEntriesPerLoop; ii++, i++) {
		    syncSamples[i] = parseIntFromArray(tmpBuffer, offset, true);
		    offset += 4;
		}
		remaining -= numEntriesPerLoop;
	    }
	    currentTrack.syncSamples = syncSamples;
	    skip(stream, requiredSize);
	} catch (IOException e) {
	    throw new BadHeaderException("Got IOException when seeking past STSS atom");
	}
    }


    private boolean isSupported(String trackType) {
	if (enableHintTrackSupport) {
	    return ( trackType.equals(VIDEO) ||
		     trackType.equals(AUDIO) ||
		     trackType.equals(HINT)
		     );
	} else {
	    return ( trackType.equals(VIDEO) ||
		     trackType.equals(AUDIO)
		     );
	}
    }


    private class MovieHeader {
	int timeScale;
	// Duration of the longest track in the movie
	Time duration = Duration.DURATION_UNKNOWN;

	// TODO: remove mdatStart as it is not necessary
	long mdatStart;
	long mdatSize;
    }

    private abstract class Media {
	// Obtained from moov/trak/mdia/minf/stbl/stsd
	String encoding;
        int maxSampleSize;
	abstract Format createFormat();
  
        float frameRate;  // $$$ KLUGE moved from Video sub-class
    }

    private class Audio extends Media {
	// Obtained from moov/trak/mdia/minf/stbl/stsd
	int channels; // Number of channels
	// Number if bits in each uncompressed sound sample.
	int bitsPerSample;
	int sampleRate;
	AudioFormat format = null;
	int frameSizeInBits;
	int samplesPerBlock = 1;

	public String toString() {
	    String info;

 	    info = ("Audio: " + format + "\n");
	    info += ("encoding is " + encoding + "\n");
	    info += ("Number of channels " + channels + "\n");
	    info += ("Bits per sample " + bitsPerSample + "\n");
	    info += ("sampleRate " + sampleRate + "\n");
	    return info;
	}

	Format createFormat() {

	    if (format != null)
		return format;
	    
	    String encodingString = null;
	    boolean signed = true;
	    boolean bigEndian = true;


	    if ( encoding.equals("ulaw") || encoding.equals("alaw") ) {
		// For ulaw and alaw, it is always 8 bits per sample
		bitsPerSample = 8;
	    }

	    // TODO: Calculate for the different encodings
	    // This is used by the codec (eg wav block alogn)
	    frameSizeInBits =  channels * bitsPerSample;

	    if ( encoding.equals("ulaw") ) {
		encodingString = AudioFormat.ULAW;
		signed = false;
	    } else if ( encoding.equals("alaw") ) {
		encodingString = AudioFormat.ALAW;
		signed = false;
	    } else if (encoding.equals("twos")) {
		/**
		 * 'twos' Samples are stored uncompressed,
		 * in twos-complement format (sample values range from
		 * -128 to 127 for 8-bit audio, and -32768 to 32767
		 * for 16 bit audio; 0 is always silence
		 */
		
		encodingString = AudioFormat.LINEAR;
	    } else if (encoding.equals("ima4")) {
		encodingString = AudioFormat.IMA4;
		/**
		 * Each packet contains 64 samples. Each sample is 4 bits/channel.
		 * So 64 samples is 32 bytes/channel.
		 * The 2 in the equation refers two bytes that the Apple's
		 * IMA compressor puts at the front of each packet, which 
		 * are referred to as predictor bytes
		 */
		samplesPerBlock = 64;
		frameSizeInBits = (32 + 2) * channels * 8;
	    } else if (encoding.equals("raw ")) {
		/**
		 * 'raw ' Samples are stored uncompressed in
		 * offset-binary format (values range from 0 to 255;
		 * 128 is silence
		 * JavaSound handles this directly. i.e no conversion
		 * is necessary
		 */
		encodingString = AudioFormat.LINEAR;
		signed = false;
		// System.out.println("raw: format: signed " + signed);
	    } else if (encoding.equals("agsm")) {
		encodingString = AudioFormat.GSM;
		/**
		 * Each frame that consists of 160 speech samples
		 * requires 33 bytes
		 */
		samplesPerBlock = 33;
		frameSizeInBits = 33 * 8;
	    } else if (encoding.equals("mac3")) {
		encodingString = AudioFormat.MAC3;
	    } else if (encoding.equals("mac6")) {
		encodingString = AudioFormat.MAC6;
	    } else {
		// NOTE: We should try to map encoding to an
		// encodingString constant defined in
		// AudioFormat
		// System.err.println("WARNING: No mapping done for encoding " + encoding +
		//		   "to an encoding string in AudioFormat");
		encodingString = encoding;
	    }

	    // TODO: put correct values here
	    // TODO: See if you need to change bitsPerSample,
	    // bitsPerSample is given in number of bits in
	    // the uncompressed sound sample. So for ima4
	    // it will be 16. See if you need to set it to 4
	    // when creating AudioFormat

	    format = new AudioFormat(encodingString,
				     sampleRate,
				     bitsPerSample,
				     channels,
				     bigEndian ? AudioFormat.BIG_ENDIAN : AudioFormat.LITTLE_ENDIAN,
				     signed ? AudioFormat.SIGNED : AudioFormat.UNSIGNED,
				     frameSizeInBits,
				     Format.NOT_SPECIFIED, // No FRAME_RATE specified
				     Format.byteArray);

	    return format;
	}
    }

    private class Video extends Media {
	// Obtained from moov/trak/mdia/minf/stbl/stsd
	int width;
	int height;
	int pixelDepth;
	int colorTableID;
	VideoFormat format;
	//	float frameRate;  $$$ KLUGE moved to base class

	Format createFormat() {
	    if (format != null)
		return format;

	    // TODO: map the encodingString properly to the Strings in
	    // VideoFormat
	    // System.out.println("Video: Frame rate is " + frameRate);
	    /**
	     * Uncompressed RGB
	     * Uncompressed RGB data is stored in a variety of different 
	     * formats. The format used depends on the Depth field of the video
	     * sample description. For all depths, the image data is padded on
	     * each scan line to ensure that each scan line begins on an even
	     * byte boundary.
	     * For depths of 1, 2, 4, and 8, the values stored are indexes
	     * into the color table specified in the Color table id field.
	     * For a depth of 16, the pixels are stored as 5-5-5 RGB values
	     * with the high bit of each 16-bit integer set to 0.
	     * For a depth of 24, the pixels are stored packed together
	     * in RGB order. For a depth of 32, the pixels are stored with an
	     * 8-bit alpha channel, followed by 8-bit RGB components.
	     */
	    if ( encoding.toLowerCase().startsWith("raw") ) {
		encoding = "rgb";
		if (pixelDepth == 24) {
		    format = new RGBFormat(new java.awt.Dimension(width, height),
					   Format.NOT_SPECIFIED, // maxDataLength
					   Format.byteArray,
					   frameRate,
					   pixelDepth,
					   1, 2, 3, // RGB order
					   3, // pixelStride
					   width*3, // lineStride
					   Format.FALSE, // Flipped
					   RGBFormat.BIG_ENDIAN);
		} else if (pixelDepth == 16) {
		    format = new RGBFormat(new java.awt.Dimension(width, height),
					   Format.NOT_SPECIFIED, // maxDataLength
					   Format.byteArray,
					   frameRate,
					   pixelDepth,
					   0x7C00, // red mask
					   0x03E0, // green mask
                                           0x001F, // blue mask
					   2, // pixelStride
					   width*2, // lineStride
					   Format.FALSE, // Flipped
					   RGBFormat.BIG_ENDIAN);
		} else if (pixelDepth == 32) {
		    encoding = "rgb";
		    
		    // This is for pixel depth 32
		    format = new RGBFormat(new java.awt.Dimension(width, height),
					   Format.NOT_SPECIFIED, // maxDataLength
					   Format.byteArray,
					   frameRate,
					   pixelDepth,
					   2, 3, 4, // ARGB
					   4, // pixelStride
					   width*4, // lineStride
					   Format.FALSE, // Flipped
					   RGBFormat.BIG_ENDIAN);
		}
	    } else if ( encoding.toLowerCase().equals("8bps") ) {
		// Not supported
		// This is a 32 bit format with alpha, red, green and blue.
		// Appears to be some type of run length encoding compression.
		// No info on this format available.
		// Will try to support it if there is a demand from the users.

		format = new VideoFormat(encoding,
					 new java.awt.Dimension(width, height),
					 maxSampleSize, Format.byteArray,
					 frameRate);

	    } else if (encoding.toLowerCase().equals("yuv2")) {
		// Component Video: Interleaved YUV 4:2:2
		// YUV 4:2:2 interleaved format. The components are ordered as
		// Y1, U, Y2, V.
		// offset-binary format

		format = new YUVFormat(new java.awt.Dimension(width, height),
				       Format.NOT_SPECIFIED, // maxDataLength
				       Format.byteArray,
				       frameRate,
				       (YUVFormat.YUV_YUYV | YUVFormat.YUV_SIGNED),
				       width*2,  // StrideY
				       width*2, // StrideUV
				       0, // offset Y
				       1,  // offset U
				       3); // offset V

	    } else {
		format = new VideoFormat(encoding,
					 new java.awt.Dimension(width, height),
					 maxSampleSize, Format.byteArray,
					 frameRate);
	    }
	    return format;
	}

	public String toString() {
	    String info;

	    info = ("Video: " + format + "\n");
	    info += ("encoding is " + encoding + "\n");
	    // info += ("width is " + width + "\n");
	    // info += ("height is " + height + "\n");
	    info += ("pixelDepth is " + pixelDepth + "\n");
	    // info += ("colorTableID is " + colorTableID + "\n");
	    
	    return info;
	}

    }

    private class Hint extends Media {
	Format format = null;

	Format createFormat() {
	    return format;
 	}
    }


    // The layout of a track header atom.
    //
    // Field Descriptions
    // Version: A 1-byte specification of the version this track header.
    // Track header flags: Three bytes that are reserved for the track 
    // 		header flags indicate how the track is used in the movie.
    // 		The following flags are valid(all flags are enable when 
    //		set to 1).	
    // 		Track enabled: indicates that the track is enabled. Flag 
    // 		value is 0x0001.
    // 		Track in movie: indicates that the track is used in the 
    //		movie. Flag value is 0x0002.
    // 		Track in Preview: indicates that the track is used in the
    // 		movie's preview. Flag value is 0x0004. 
    //		Track in poster: indicates that the track is used in the
    //		movie's poster. Flag value is 0x0008.
    // Creation time: A 32-bit integer that indicates (in seconds) when 
    //		the track header was created.
    // Modification time: A 32-bit integer that indicates (in seconds) when
    //		the track header was changed.
    // TrackID:	A 32-bit integer that uniquely identifies the track. A value
    //		of 0 must never be used for a trackID.
    // Duration: A time value that indicates the duration of this track.
    //		Note: this property is derived form the durations of all the
    //		track's edits.
    // Layer: 	A 16 bits integer that indicates this track's spatial priority
    //		in its movie. The QuickTime Movie Toolbox uses this value to 
    //		to determine how tracks overlay one another. Tracks with lower
    //		layer values are displayed in front of the tracks with higher
    //		layer values.
    // Alternative group: A 16 bit integer that specifies a collection of movie
    // 		data for one another. QuickTime chooses one track from the group    //		to be used when the movie is played. The choice may be based.
    //		on such considerations as playback quality or language and the
    //		capabilities of the computer.
    // Volume: 	A 16 bit fixed point value that indicates how loudly this track
    //		sound is to be played. A value of 1.0 indicates normal volume.
    // Matrix:	The matrix structure associated with this track. 
    // Track Width: A 32-bit-fixed point number that specifies the width of
    //		this track in pixels.
    // Track height: A 32-bit-fixed point number that specifies the height of
    //		this track in pixels.

    private class TrakList {
	// Obtained from moov/trak/tkhd atom
	int flag;
	int id;
	// Duration: A time value that indicates the duration of this
	// track (in the movie's time coordinate system). Note that this
	// property is derived from the track's edits. The value of this
	// field is equal to the sum of the durations of all of the
	// track's edits.
	Time duration = Duration.DURATION_UNKNOWN;

	// Obtained from moov/trak/mdia/mdhd
	int mediaTimeScale;
	Time mediaDuration = Duration.DURATION_UNKNOWN;

	// Obtained from moov/trak/mdia/hdlr
	String trackType;

	int numberOfSamples; // Does it need to be long
	// Obtained from moov/trak/mdia/minf/stbl/stsz
	int sampleSize = 0; // If all samples are of the same size
	int[] sampleSizeArray;

	// From the implementation
	boolean supported; // Is this track type supported

	// TODO: In future, this may become an array of Media, if multiple
	// formats per track are supported.
	// Obtained from moov/trak/mdia/minf/stbl/stsd
	Media media; // Info specific to each track type.

	// Obtained from moov/trak/mdia/minf/stbl/stco
	int numberOfChunks;
	int chunkOffsets[] = new int[0];

	// Obtained from moov/trak/mdia/minf/stbl/stsc
	int compactSamplesChunkNum[] = new int[0];
	int compactSamplesPerChunk[] = new int[0];

	// Computed from above 2
	// set if all chunks have same # of samples
	int constantSamplesPerChunk = -1;
	// TODO: change this to cumSamplesPerChunk
	int samplesPerChunk[];

	// Obtained from moov/trak/mdia/minf/stbl/stts
	double durationOfSamples = -1.0;
	// TODO: change timeToSampleIndices to cumulativeTimeToSampleIndices
	int timeToSampleIndices[] = new int[0];
	double cumulativeDurationOfSamples[] = new double[0];
	double startTimeOfSampleArray[] = new double[0]; // Not used by AUDIO
	double durationOfSampleArray[] = new double[0];  // Not used by AUDIO

	// TODO: can this be int instead of long
	long[] sampleOffsetTable;

	int[] syncSamples;
	int[] syncSampleMapping;
	TimeAndDuration timeAndDuration = new TimeAndDuration();
	
	int trackIdOfTrackBeingHinted = -1;
	int indexOfTrackBeingHinted = -1;
	int maxPacketSize = -1;

	void buildSamplePerChunkTable() {
	    int i,j;
	    if (numberOfChunks <= 0)
		return;
	    if (compactSamplesPerChunk.length == 1) {
		constantSamplesPerChunk = compactSamplesPerChunk[0];
		// System.out.println("constantSamplesPerChunk is " + constantSamplesPerChunk);
		return;
	    }

	    samplesPerChunk = new int[numberOfChunks];
	    i = 1;
	    for (j = 0; j < compactSamplesChunkNum.length -1; j++) {
		int numSamples = compactSamplesPerChunk[j];
		// System.out.println("numSamples is " + numSamples);
		while ( i != compactSamplesChunkNum[j+1]) {
		    samplesPerChunk[i-1] = numSamples;
		    i++;
		}
	    }
	    for (; i <= numberOfChunks; i++) {
		samplesPerChunk[i-1] = compactSamplesPerChunk[j];
	    }
	}

	void buildCumulativeSamplePerChunkTable() {
	    // Calculate cumulative samples per chunk, unless
	    // all chunks have the same number of samples
	    // System.out.println("buildCumulativeSamplePerChunkTable: constantSamplesPerChunk " +
	    //			       constantSamplesPerChunk);

	    if ( constantSamplesPerChunk == -1) {
		for (int i = 1; i < numberOfChunks; i++) {
		    samplesPerChunk[i] += samplesPerChunk[i-1];
		    // System.out.println("cum samplesPerChunk: " + samplesPerChunk[i]);
		}
	    }
	}

	// Used by video track only as the number of samples in
	// audio is generally quite large
	void buildSampleOffsetTable() {
	    sampleOffsetTable = new long[numberOfSamples];
	    
	    int index = 0;
	    long offset;
	    int i, j;

	    if (sampleSize != 0) {
		// All samples are of same size

		if (constantSamplesPerChunk != -1) {
		    for (i = 0; i < numberOfChunks; i++) {
			offset = chunkOffsets[i];
			for (j = 0; j < constantSamplesPerChunk; j++) {
			    sampleOffsetTable[index++] = offset + (j * sampleSize);
			}
		    }
		} else {
		    for (i = 0; i < numberOfChunks; i++) {
			offset = chunkOffsets[i];
			for (j = 0; j < samplesPerChunk[i]; j++) {
			    sampleOffsetTable[index++] = offset + (j * sampleSize);
			}
		    }
		}
	    } else {
		int numSamplesInChunk = 0; // initialize to keep compiler happy
                if (constantSamplesPerChunk != -1)
		    numSamplesInChunk = constantSamplesPerChunk;
		for (i = 0; i < numberOfChunks; i++) {
		    offset = chunkOffsets[i];
		    
		    // Handle first sample in each chunk 
		    sampleOffsetTable[index] = offset;
		    index++;

		    if (constantSamplesPerChunk == -1)
			numSamplesInChunk = samplesPerChunk[i];

		    for (j = 1; j < numSamplesInChunk; j++) {
			sampleOffsetTable[index] = sampleOffsetTable[index-1] +
			    sampleSizeArray[index-1];
			index++;
		    }
		}
	    }
	}

	boolean buildSyncTable() {
	    
	    if (syncSamples == null)
		return false;

	    /** CHECK: I don't know of any audio encoding in QuickTime
	     * in which not all frames are key frames. If they exist then we 
	     * don't want to build the
	     * syncSampleMapping array because it is as big as the number of
	     * samples. The number of samples in audio is generally quite
	     * large. Also, it complicates things like synchronization
	     * if both the video track and audio track have key frames.
	     * This is because the sample that corresponds to a time
	     * may not be a key frame in both audio and video tracks
	     * and the nearest keyframe in both audio and video may
	     * correspond to different times.
	     * Note that we have the same problem if there are 2 or more
	     * video tracks with keyframes.
	     */
	    if (!trackType.equals(VIDEO))
		return false;

	    int numEntries = syncSamples.length;
	    if (numEntries == numberOfSamples) {
		// Every frame is a key frame. Ignoring sync table atom
		// System.out.println("Every frame is a key frame. Ignoring sync table atom");
		syncSamples = null;
		return false;
	    }

	    syncSampleMapping = new int[numberOfSamples];
	    int index = 0;
	    int previous;
	    if (syncSamples[0] != 1) {
		// Bug in the sync table of the QuickTime file
		// The first sample should always be a key frame
		previous = syncSampleMapping[0] = 0;
	    } else {
		previous = syncSampleMapping[0] = 0;
		index++;
	    }
	    
	    for (; index < syncSamples.length; index++) {
		int next = syncSamples[index] - 1;
		syncSampleMapping[next] = next;
		int range = next - previous - 1;
		for (int j = previous+1; j < next; j++) {

		    // Return the closest keyframe
		    // if ((float)(j - previous)/range <= 0.5)
		    //   syncSampleMapping[j] = previous;
		    // else
		    //   syncSampleMapping[j] = next;
		    
		    // Return the previous keyframe
		    syncSampleMapping[j] = previous;
		}
		previous = next;
	    }
	    int lastSyncFrame = syncSamples[syncSamples.length - 1] -1;
	    for (index = lastSyncFrame+1; index < numberOfSamples; index++) {
		syncSampleMapping[index] = lastSyncFrame;
	    }

	    return true; // syncSampleMapping table has been built
	}

	int time2Index(double time) {
	    
	    // TODO: do bounds checking

	    // Note: In all places that call time2Index,
	    // time parameter is set to 0 if it is < 0
	    if (time < 0)
		time = 0;

	    int length = timeToSampleIndices.length;
	    int sampleIndex;

	    // TODO: do bounds checking
	    if (length == 0) {
		sampleIndex = (int)
		    ((time / mediaDuration.getSeconds()) * numberOfSamples + 0.5);
		if (sampleIndex >= numberOfSamples)
		    return -1; // PAST EOM // sampleIndex = numberOfSamples -1;
		return sampleIndex;
				   
	    }

	    // Note: length will always be atleast 2
	    int foundIndex;

	    int approxLocation = (int)
		((time / mediaDuration.getSeconds()) * length);

	    if (approxLocation == length) // TODO: check to see if you need this
		approxLocation--;

	    if (approxLocation >= cumulativeDurationOfSamples.length) {
		return -1; // PAST EOM
	    }

	    int i;
	    if (cumulativeDurationOfSamples[approxLocation] < time) {
		// increment = 1;
		for (i = approxLocation+1; i < length; i++) {
		    if (cumulativeDurationOfSamples[i] >= time) {
			break;
		    }
		}
		foundIndex = i;
	    } else if (cumulativeDurationOfSamples[approxLocation] > time) {
		// increment = -1;
		for (i = approxLocation-1; i >= 0; i--) {
		    if (cumulativeDurationOfSamples[i] < time) {
			break;
		    }
		}
		foundIndex = i+1;
	    } else {
		foundIndex = approxLocation;
	    }

	    if (foundIndex == length)
		foundIndex--;

	    double delta = cumulativeDurationOfSamples[foundIndex] - time;
	    int samples;
	    double duration;
	    if (foundIndex == 0) {
		sampleIndex = timeToSampleIndices[foundIndex];
		samples = sampleIndex;
		duration = cumulativeDurationOfSamples[foundIndex];
	    } else {
		sampleIndex = timeToSampleIndices[foundIndex];
		samples = sampleIndex -
		    timeToSampleIndices[foundIndex-1];
		duration = cumulativeDurationOfSamples[foundIndex] -
		    cumulativeDurationOfSamples[foundIndex-1];
	    }
	    double fraction = delta / duration;
	    sampleIndex -= (samples * fraction);
	    return sampleIndex;
	}

	// TODO: make time and duration in nanoseconds rather than seconds.
	TimeAndDuration index2TimeAndDuration(int index) {

	    double startTime = 0.;
	    double duration = 0.;
	    
	    try {
		if (index < 0)
		    index = 0;
		else if (index >= numberOfSamples) {
		    index = numberOfSamples -1;
		}
		int length = timeToSampleIndices.length;
		
		if (length == 0) {
		    // All samples have the same duration
		    duration = durationOfSamples;
		    startTime = duration * index;
		} else if (startTimeOfSampleArray.length >= index) {
		    duration = durationOfSampleArray[index];
		    startTime = startTimeOfSampleArray[index];
		} else {
		    // TODO: compute this only once
		    float factor = (float) length / numberOfSamples;
		    int location = (int) (index * factor);
		    duration = 0; // DUMMY TODO
		    startTime = 0; // DUMMY TODO
		}
	    } finally {
		synchronized(timeAndDuration) {
		    timeAndDuration.startTime = startTime;
		    timeAndDuration.duration = duration;
		    return timeAndDuration;
		}
	    }
	}

	// TODO: Looks like this wants index starting from 1 not 0
	// May want to change it so that the index starts from 0
	int index2Chunk(int index) {
	    int chunk;

	    if ( constantSamplesPerChunk != -1) {
		chunk = index / constantSamplesPerChunk;
		return chunk;
	    }
	    int length = samplesPerChunk.length;
	    int approxChunk = (int)((float) (index / numberOfSamples) * length);
	    if (approxChunk == length)
		approxChunk--;

	    int i;

	    if (samplesPerChunk[approxChunk] < index) {
		// increment = 1;
		for (i = approxChunk+1; i < length; i++) {
		    if (samplesPerChunk[i] >= index) {
			break;
		    }
		}
		chunk = i;
	    } else if (samplesPerChunk[approxChunk] > index) {
		// increment = -1;
		for (i = approxChunk-1; i >= 0; i--) {
		    if (samplesPerChunk[i] < index) {
			break;
		    }
		}
		chunk = i+1;
	    } else {
		chunk = approxChunk;
	    }
	    return chunk;

	}

	// NOTE: $$$ May compute wrong info for audio because even though
	// the spec. says that sampleSize in stsz atom represents
	// size in bytes of the sample, I always get 1 even for
	// 16 bit linear stereo files.
	long index2Offset(int index) {
// 	    int chunk = index2Chunk(index);
// 	    System.out.println("OLD chunk value " + chunk);
	    int chunk = index2Chunk(index+1);
	    if (debug)
		System.out.println(" index2Chunk chunk is " + chunk);

	    if (chunk >= chunkOffsets.length) {
		// At or beyond EOM
		// TODO: Can use a better constant
		return com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD;
	    }

	    long offset = chunkOffsets[chunk];

	    if (/*true*/debug1) {
		System.out.println("index2Offset: index, chunk, chunkOffset " +
				   index + " : " + chunk + " : " + offset);
		// Remove the debug if and print $$$
// 		if (constantSamplesPerChunk == -1) {
// 		    System.out.println("samples in chunk " + chunk + " ==> " +
// 				   samplesPerChunk[chunk]);
// 		}
	    }

	    int sampleNumInChunk;
	    int start;

	    // System.out.println("  constantSamplesPerChunk is " + constantSamplesPerChunk);
	    if ( constantSamplesPerChunk != -1) {
		sampleNumInChunk = (index % constantSamplesPerChunk);
		start = chunk * constantSamplesPerChunk;
	    } else {
		if (chunk == 0)
		    start = 0;
		else
		    start = samplesPerChunk[chunk -1];
		sampleNumInChunk = index - start;
		if (/*true*/debug1) {
		    System.out.println("index, start, sampleNumInChunk " +
				       index + " : " + start + " : " +
				       sampleNumInChunk);
		    System.out.println("sampleSize is " + sampleSize);
		}
	    }
	    if (debug1)
		System.out.println("sampleSize is " + sampleSize);
	    if (sampleSize != 0) {
		// All the samples are of the same size
		offset += (sampleSize * sampleNumInChunk);
	    } else {
		for (int i = 0; i < sampleNumInChunk; i++)
		    offset += sampleSizeArray[start++];
	    }
	    return offset;
	}


	// Used for VIDEO. Not for AUDIO as the number of samples
	// is generally quite large.
	void buildStartTimeAndDurationTable() {
	    if (debug2) {
		System.out.println("buildStartTimeAndDurationTable"); //$$$
	    }
	    int length = timeToSampleIndices.length;

	    // No need for table as all samples are of the same duration
	    if (length == 0)
		return;

	    startTimeOfSampleArray = new double[numberOfSamples];
	    durationOfSampleArray  = new double[numberOfSamples];
	    int previousSamples = 0;
	    double previousDuration = 0.0;
	    double time = 0.;
	    int index = 0;
	    for (int i = 0; i < length; i++) {
		int numSamples = timeToSampleIndices[i];
		double duration = (cumulativeDurationOfSamples[i] -
				   previousDuration)/(numSamples - previousSamples);
// 		if (debug2)
// 		    System.out.println("duration is " + duration); //$$$
		for (int j = 0; j < (numSamples - previousSamples); j++) {
		    startTimeOfSampleArray[index] = time;
		    durationOfSampleArray[index] = duration;
		    index++;
		    time += duration;
		}
		previousSamples = numSamples;
		previousDuration = cumulativeDurationOfSamples[i];
	    }
	}

	public String toString() {
	    String info = "";

	    info += ("track id is " + id + "\n");
	    info += ("duration itrack is " + duration.getSeconds() + "\n");
	    info += ("duration of media is " + mediaDuration.getSeconds() + "\n");
	    info += ("trackType is " + trackType + "\n");
	    // info += ("numberOfSamples is " + numberOfSamples + "\n");
	    // info += ("sampleSize is " + sampleSize + "\n");
	    // info += ("maxSampleSize is " + media.maxSampleSize + "\n");
	    // info += ("numberOfChunks is " + numberOfChunks + "\n");
	    info += media;

	    return info;
	}

    }

    // TODO extend BasicTrack if possible
    private abstract class MediaTrack implements Track {
	TrakList trakInfo;
	boolean enabled = true;
	int numBuffers = 4; // TODO: check
	Format format;
	long sequenceNumber = 0;
	int chunkNumber = 0;
	int sampleIndex = 0;
	int useChunkNumber = 0;
	int useSampleIndex = 0;
	QuicktimeParser parser = QuicktimeParser.this;
	CachedStream cacheStream = parser.getCacheStream();
	int constantSamplesPerChunk;
	int[] samplesPerChunk;
	protected TrackListener listener;

	MediaTrack(TrakList trakInfo) {
	    this.trakInfo = trakInfo;
	    if (trakInfo != null) {
		enabled = ( (trakInfo.flag & TRACK_ENABLED) != 0);
		format = trakInfo.media.createFormat();
		samplesPerChunk = trakInfo.samplesPerChunk;
		constantSamplesPerChunk = trakInfo.constantSamplesPerChunk;
	    }
	}

	// TODO: create a list of TrackListeners
	public void setTrackListener(TrackListener l) {
	    listener = l;
	}

	public Format getFormat() {
	    return format;
	}
	
	
	public void setEnabled(boolean t) {
	    enabled = t;
	}
	
	public boolean isEnabled() {
	    return enabled;
	}
	
	public Time getDuration() {
	    return trakInfo.duration;
	}
	
	
	public Time getStartTime() {
	    return new Time(0); // TODO
	}
	
	synchronized void setSampleIndex(int index) {
	    sampleIndex = index;
	}

	synchronized void setChunkNumber(int number) {
	    chunkNumber = number;
	}

	public void readFrame(Buffer buffer) {
	    if (buffer == null)
		return;
	    
	    if (!enabled) {
		buffer.setDiscard(true);
		return;
	    }
	    

	    synchronized (this) {
		useChunkNumber = chunkNumber;
		useSampleIndex = sampleIndex;
	    }

	    // TODO: handle chunkNumber < 0 case differently
// 	    System.out.println("useChunkNumber: numchunks " +
// 			       useChunkNumber + " : " +
// 			       trakInfo.numberOfChunks);


	    if ( (useChunkNumber >= trakInfo.numberOfChunks) ||
		 (useChunkNumber < 0 ) ) {
		buffer.setEOM(true);
		return;
	    }

	    buffer.setFormat(format); // Need to do this every time ???
	    doReadFrame(buffer);
	}
	
	abstract void doReadFrame(Buffer buffer);

	public int mapTimeToFrame(Time t) {
	    return FRAME_UNKNOWN;
	}
	
	public Time mapFrameToTime(int frameNumber) {
	    return TIME_UNKNOWN;
	}
    }
    
    private class AudioTrack extends MediaTrack  {
	String encoding;
	int channels;
	int sampleOffsetInChunk = -1;
	int useSampleOffsetInChunk = 0;
	int frameSizeInBytes;
	int samplesPerBlock;
	int sampleRate;

	AudioTrack(TrakList trakInfo,
		   int channels,
		   String encoding,
		   int frameSizeInBytes,
		   int samplesPerBlock,
		   int sampleRate) {
	    super(trakInfo);
	    this.channels = channels;
	    this.encoding = encoding;
	    this.frameSizeInBytes = frameSizeInBytes;
	    this.samplesPerBlock = samplesPerBlock;
	    this.sampleRate = sampleRate;
	}


	AudioTrack(TrakList trakInfo) {
	    super(trakInfo);
	    if (trakInfo != null) { // remove this if $$$$$
		channels =  ((Audio)trakInfo.media).channels;
		encoding = trakInfo.media.encoding;
		frameSizeInBytes =  ((Audio)trakInfo.media).frameSizeInBits / 8;
		samplesPerBlock = ((Audio)trakInfo.media).samplesPerBlock;
		sampleRate = ((Audio)trakInfo.media).sampleRate;
	    }
	}
	
 	synchronized void setChunkNumberAndSampleOffset(int number, int offset) {
 	    chunkNumber = number;
	    sampleOffsetInChunk = offset;
 	}

	void doReadFrame(Buffer buffer) {
	    int samples;

// 	    if (debug1) {
// 		System.out.println("audio: doReadFrame: " + useChunkNumber +
// 				   " : " + sampleOffsetInChunk);
// 	    }
	    
	    synchronized (this) {
		if (sampleOffsetInChunk == -1) {
		    useSampleOffsetInChunk = 0;
		} else {
		    useSampleOffsetInChunk = sampleOffsetInChunk;
		    sampleOffsetInChunk = -1; // Reset sampleOffsetInChunk
		}
	    }
	    // TODO: compute this properly for all encodings.
	    // TODO: compute a multiply factor once and use it
	    // to multiply samplesPerChunk[chunkNumber]

	    long samplesPlayed;
	    if (constantSamplesPerChunk != -1) {
		samples = constantSamplesPerChunk;
		samplesPlayed = constantSamplesPerChunk * useChunkNumber;
	    } else if (useChunkNumber > 0) {
		samples = samplesPerChunk[useChunkNumber] -
		          samplesPerChunk[useChunkNumber - 1];
		samplesPlayed = samplesPerChunk[useChunkNumber];
	    } else {
		samples = samplesPerChunk[useChunkNumber];
		samplesPlayed = 0;
	    }

	    int byteOffsetFromSampleOffset;
	    if (samplesPerBlock > 1) {
		int skipBlocks = useSampleOffsetInChunk / samplesPerBlock; // Integer division
		useSampleOffsetInChunk = skipBlocks * samplesPerBlock;
		byteOffsetFromSampleOffset = frameSizeInBytes * skipBlocks;
	    } else {
		byteOffsetFromSampleOffset = useSampleOffsetInChunk * frameSizeInBytes;
	    }

	    samples -= useSampleOffsetInChunk;
	    samplesPlayed += useSampleOffsetInChunk; // TODO $$$ CHECK
// 	    System.out.println("samples is " + samples);
// 	    System.out.println("samplesPlayed is " + samplesPlayed);
// 	    System.out.println("time is " + ((double) samplesPlayed / sampleRate));
	    
	    int needBufferSize;
	    // TODO: See if you can build an array of size numChunks
	    // that holds the computed needBufferSize, so that we don't
	    // have to compute it each time
	    if (encoding.equals("ima4")) {
		/**
		 * Each packet contains 64 samples. Each sample is 4 bits/channel.
		 * So 64 samples is 32 bytes/channel.
		 * The 2 in the equation refers two bytes that the Apple's
		 * IMA compressor puts at the front of each packet, which 
		 * are referred to as predictor bytes
		 */
		// needBufferSize = samples/64 * (32 + 2) * channels; // REMOVE $$
		needBufferSize = samples/samplesPerBlock * (32 + 2) * channels;
	    } else if (encoding.equals("agsm")) {
		/**
		 * Each frame that consists of 160 speech samples
		 * requires 33 bytes
		 */
		// needBufferSize = (samples / 160) * 33; // REMOVE $$
		needBufferSize = (samples / 160) * samplesPerBlock;
	    } else {
		needBufferSize = samples *
		    ((AudioFormat)format).getSampleSizeInBits()/8 * channels;
	    }
	    
	    // System.out.println("needBufferSize is " + needBufferSize);
	    Object obj = buffer.getData();
	    byte[] data;
	    
	    if  ( (obj == null) ||
		  (! (obj instanceof byte[]) ) ||
		  ( ((byte[])obj).length < needBufferSize) ) {
		data = new byte[needBufferSize];
		buffer.setData(data);
	    } else {
		data = (byte[]) obj;
	    }
	    
	    try {
		int actualBytesRead;
		synchronized(seekSync) {
		    int offset = trakInfo.chunkOffsets[useChunkNumber];

		    {
			if (sampleIndex != useSampleIndex) { // Changed by setPosition()
			    // System.out.println("parser: audio: discard");
			    buffer.setDiscard(true);
			    return;
			}
		    }

		    if ( (cacheStream != null) && (listener != null) ) {
//			if ( cacheStream.willReadBytesBlock(offset, needBufferSize) ) {
			if ( cacheStream.willReadBytesBlock(offset+byteOffsetFromSampleOffset,
							    needBufferSize) ) {
			    listener.readHasBlocked(this);
			} else { // TODO: REMOVE ELSE BLOCK
// 			    System.out.println("audio: won't block: " + offset + " : " +
// 					       needBufferSize);
			}
		    }

		    // long pos = seekableStream.seek(offset);
		    // System.out.println("seek to " + (offset + byteOffsetFromSampleOffset));
		    long pos = seekableStream.seek(offset + byteOffsetFromSampleOffset);
		    if ( pos == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
			buffer.setDiscard(true);
			return;
		    }
		    actualBytesRead = parser.readBytes(stream, data, needBufferSize);
		    // System.out.println("actualBytesRead is " + actualBytesRead);
		    if ( actualBytesRead == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
			buffer.setDiscard(true);
			return;
		    }
		}
		buffer.setLength(actualBytesRead);
		// TODO: need setSequenceNumber and getSequenceNumber in Buffer
		buffer.setSequenceNumber(++sequenceNumber);
		if (sampleRate > 0) {
		    long timeStamp =
			(samplesPlayed * 1000000000L) / sampleRate;
		    // System.out.println("audio timeStamp is " + timeStamp);
		    buffer.setTimeStamp(timeStamp);
		    buffer.setDuration(Buffer.TIME_UNKNOWN);
		}
	    } catch (IOException e) {
		// System.err.println("readFrame: " + e);
		buffer.setLength(0);
		buffer.setEOM(true);
		// TODO: $$$$ Update maxFrame and duration
		// System.out.print("After EOM Updating maxLocation from " + maxLocation);
		// maxLocation = parser.getLocation(stream);
		// System.out.println(" to " + maxLocation);
	    }
	    synchronized(this) {
		if (chunkNumber == useChunkNumber) // Not changed by setPosition()
		    // System.out.println("AudioTrack: increment chunkNumber");
		    chunkNumber++;
	    }
	}
    }

    private class VideoTrack extends MediaTrack  {
	int needBufferSize;
	boolean variableSampleSize = true;


	VideoTrack(TrakList trakInfo) {
	    super(trakInfo);
	    if (trakInfo != null) { // remove this if $$$$$
		if (trakInfo.sampleSize != 0) {
		    variableSampleSize = false;
		    needBufferSize = trakInfo.sampleSize;
		}
	    }
	}
	
	void doReadFrame(Buffer buffer) {
	    // File buglife.mov is inconsistent. The stts chunk has 2 entries
	    // with 1842 and 1 samples, which means that there are 1843 samples
	    // in the movie. But the stsz chunk has only 1842 entries.
	    // To workaround this inconsistency, I check for sampleSizeArray.length
	    // also.
	    if ( useSampleIndex >= trakInfo.numberOfSamples ) {
		buffer.setLength(0);
		buffer.setEOM(true);
		return;
	    }
	    if ( variableSampleSize ) {
		if (useSampleIndex >= trakInfo.sampleSizeArray.length) {
		    buffer.setLength(0);
		    buffer.setEOM(true);
		    return;
		}
		needBufferSize = trakInfo.sampleSizeArray[useSampleIndex];
	    }

// 	    System.out.println("needBufferSize is " + needBufferSize);
	    long offset = trakInfo.sampleOffsetTable[useSampleIndex];
	    Object obj = buffer.getData();
	    byte[] data;
	    
	    if  ( (obj == null) ||
		  (! (obj instanceof byte[]) ) ||
		  ( ((byte[])obj).length < needBufferSize) ) {
		data = new byte[needBufferSize];
		buffer.setData(data);
	    } else {
		data = (byte[]) obj;
	    }
	    try {
		int actualBytesRead;
		synchronized(seekSync) {

		    {
			if (sampleIndex != useSampleIndex) { // Changed by setPosition()
			    // System.out.println("parser: video: discard");
			    buffer.setDiscard(true);
			    return;
			}
		    }

		    if  ( (cacheStream != null) && (listener != null) ) {
			if ( cacheStream.willReadBytesBlock(offset, needBufferSize) ) {
// 			    System.out.println("video: will block: " + offset + " : " +
// 					       needBufferSize);
			    listener.readHasBlocked(this);
			} else { // TODO: REMOVE ELSE BLOCK
// 			    System.out.println("video: won't block: " + offset + " : " +
// 					       needBufferSize);
			}

		    }
// 		    System.out.println("doReadFrame: offset is " + offset);

		    long pos = seekableStream.seek(offset);
// 		    System.out.println("seek returns " + pos);
		    if ( pos == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
			buffer.setDiscard(true);
			return;
		    }
// 		    {// DEBUG BLOCK
// 			int numPackets = parser.readShort(stream);
// 			System.out.println("num packets " + numPackets);
// 			needBufferSize -= 2;
// 			parser.readShort(stream); // reserved
// 			needBufferSize -= 2;

// 			for (int i = 0; i < numPackets; i++) {
// 			    System.out.println("Packet # " + i);
// 			    int relativeTransmissionTime = parser.readInt(stream);
// 			    System.out.println("  relativeTransmissionTime is " +
// 					       relativeTransmissionTime);
// 			    needBufferSize -= 4;

// 			    int rtpHeaderInfo = parser.readShort(stream);
// 			    System.out.println("  rtpHeaderInfo is " + rtpHeaderInfo);
// 			    needBufferSize -= 2;

// 			    int rtpSequenceNumber = parser.readShort(stream);
// 			    System.out.println("  rtpSequenceNumber is " + rtpSequenceNumber);
// 			    needBufferSize -= 2;

// 			    int flags = parser.readShort(stream);
// 			    System.out.println("  flags is " + flags);
// 			    needBufferSize -= 2;

// 			    int entriesInDataTable = parser.readShort(stream);
// 			    System.out.println("  entriesInDataTable is " + entriesInDataTable);
// 			    needBufferSize -= 2;

// 			// TODO: If bit 13 of flag is set, Extra info TLV table is present

// 			    // 			int tlvTableSize = parser.readInt(stream);
// 			    // 			System.out.println("tlvTableSize is " +
// 			    // 					   tlvTableSize);
// 			    // 			needBufferSize -= 4;
			    
// 			    for (int j = 0; j < entriesInDataTable; j++) {
// 				int dataBlockSource = parser.readByte(stream);
// 				System.out.println("    dataBlockSource is " + dataBlockSource);
// 				// TODO: assuming dataBlockSource 1, that is Immediate data
// 				needBufferSize--;;
// 				int length = parser.readByte(stream);
// 				System.out.println("    data table length is " + length);
// 				needBufferSize--;;
// 				// 			    skip(stream, length);
// 				// 			    needBufferSize -= length;
// 				parser.skip(stream, 14);
// 				needBufferSize -= 14;
// 			    }
// 			}
// 			System.out.println("needBufferSize after is " + needBufferSize);
// 		    }

		    actualBytesRead = parser.readBytes(stream, data, needBufferSize);
		    // System.out.println("actualBytesRead: " + actualBytesRead);
		    if ( actualBytesRead == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
			buffer.setDiscard(true);
			return;
		    }
		}
		/**//*System.out.println("  actualBytesRead is " + actualBytesRead);*/
		buffer.setLength(actualBytesRead);

		int[] syncSampleMapping = trakInfo.syncSampleMapping;
		boolean keyFrame = true;
		if (syncSampleMapping != null) {
		    keyFrame = (syncSampleMapping[useSampleIndex] == useSampleIndex);
		}
		// Note: if syncSampleMapping is null, then every frame is a key frame.
		if (keyFrame) {
		    buffer.setFlags(buffer.getFlags() | Buffer.FLAG_KEY_FRAME);
		    // System.out.println("readFrame: returns keyframe " + useSampleIndex);
		}

		// TODO: need setSequenceNumber and getSequenceNumber in Buffer
		buffer.setSequenceNumber(++sequenceNumber);
		TimeAndDuration td = trakInfo.index2TimeAndDuration(useSampleIndex);
		// System.out.println("seq num:, timestamp is " + sequenceNumber +
		//	   " : " + td.startTime);
		buffer.setTimeStamp((long) (td.startTime * 1E9));
		buffer.setDuration((long) (td.duration * 1E9));
		
	    } catch (IOException e) {
		// System.err.println("readFrame: " + e);
		buffer.setLength(0);
		buffer.setEOM(true);
		// TODO: $$$$ Update maxFrame and duration
		// System.out.print("After EOM Updating maxLocation from " + maxLocation);
		// maxLocation = parser.getLocation(stream);
		// System.out.println(" to " + maxLocation);
	    }
	    synchronized(this) {
		if (sampleIndex == useSampleIndex) // Not changed by setPosition()
		    sampleIndex++;
	    }
	}

	public int mapTimeToFrame(Time t) {
	    double time = t.getSeconds();

	    if (time < 0)
		return FRAME_UNKNOWN;

	    int index = trakInfo.time2Index(time);

// 	    System.out.println("index is " + index);
// 	    System.out.println("max index " + trakInfo.numberOfSamples);

// 	    if ( (index < 0) || (index >= trakInfo.numberOfSamples) )
// 		return FRAME_UNKNOWN;

	    if ( index < 0 )
		return trakInfo.numberOfSamples - 1;

// 	    System.out.println(" mapTimeToFrame: " + time + " : " +
// 			       index);
// 	    { // DEBUG BLOCK REMOVE
// 		mapFrameToTime(index);
// 	    }
	    return index;
	}
	
	public Time mapFrameToTime(int frameNumber) {
	    if ( (frameNumber < 0) || (frameNumber >= trakInfo.numberOfSamples) )
		return TIME_UNKNOWN;

// 	    System.out.println("mapFrameToTime: frame rate is " +
// 			       ((Video) trakInfo.media).frameRate);
	    double time = frameNumber / ((Video) trakInfo.media).frameRate;
// 	    System.out.println("mapFrameToTime: " + frameNumber +
// 			       " ==> " + time);
	    return new Time(time);
	}

    }

    private class HintAudioTrack extends AudioTrack  {

	int hintSampleSize;
	int indexOfTrackBeingHinted = trakInfo.indexOfTrackBeingHinted;
	int maxPacketSize;
	int currentPacketNumber = 0;
	int numPacketsInSample = -1;
	long offsetToStartOfPacketInfo = -1;
	TrakList sampleTrakInfo;
	boolean variableSampleSize = true;

	HintAudioTrack(TrakList trakInfo,
		       int channels,
		       String encoding,
		       int frameSizeInBytes,
		       int samplesPerBlock,
		       int sampleRate) {
	    super(trakInfo, channels, encoding, frameSizeInBytes, samplesPerBlock, sampleRate);

	    format = ( (Hint) trakInfo.media).format;
	    //	    hintSampleSize = needBufferSize;
	    maxPacketSize = trakInfo.maxPacketSize;

	    if (indexOfTrackBeingHinted >= 0) {
		sampleTrakInfo = trakList[indexOfTrackBeingHinted];
	    } else { // $$$ REMOVE else DEBUG BLOCK
		if (debug) {
		    System.out.println("sampleTrakInfo is not set " + indexOfTrackBeingHinted);
		}
	    }

// 	    System.out.println("HintAudioTrack: cons: numchunks of hint, sample trak " +
// 			       trakInfo.numberOfChunks + " : " +
// 			       trakList[indexOfTrackBeingHinted].numberOfChunks);
// 	    System.out.println("HintAudioTrack: cons: constantSamplesPerChunk in hint, sample " + 
// 			       trakInfo.constantSamplesPerChunk + " : " +
// 			       trakList[indexOfTrackBeingHinted].constantSamplesPerChunk);
	    if (trakInfo.sampleSize != 0) {
		variableSampleSize = false;
		hintSampleSize = trakInfo.sampleSize;
	    }

	}

	// HintAudioTrack readFrame overrides method in base class
	// MediaTrack because you can't use the chunknumber which in
	// this case is the sample number to check 
	// (useChunkNumber >= trakInfo.numberOfChunks) and call
	// buffer.setEOM(true).
	public void readFrame(Buffer buffer) {

	    if (buffer == null)
		return;
	    
	    if (!enabled) {
		buffer.setDiscard(true);
		return;
	    }
	    
 	    synchronized (this) {
 		useChunkNumber = chunkNumber;
 		useSampleIndex = sampleIndex;
 	    }

	    // Doesn't apply for HintAudioTrack
// 	    if ( (useChunkNumber >= trakInfo.numberOfChunks) ||
// 		 (useChunkNumber < 0 ) ) {
// 		System.out.println("MediaTrack EOM: chunkNumber > numberOfChunks");
// 		buffer.setEOM(true);
// 		return;
// 	    }

	    buffer.setFormat(format); // Need to do this every time ???
	    doReadFrame(buffer);
	}

	// NEW::
	synchronized void setSampleIndex(int index) {
	    chunkNumber = index;
	    sampleIndex = index;
	}

	void doReadFrame(Buffer buffer) {
	    int samples;

	    if (debug1) {
		System.out.println("audio: hint doReadFrame: " + useChunkNumber +
				   " : " + sampleOffsetInChunk);
	    }
	    
	    boolean rtpMarkerSet = false;
	    boolean paddingPresent;
	    boolean extensionHeaderPresent;
	    int rtpSequenceNumber;
	    int relativeTransmissionTime;

	    if (indexOfTrackBeingHinted < 0) {
		buffer.setDiscard(true);
		return;
	    }

	    int rtpOffset = 0;
	    int remainingHintSampleSize;

// 	    if ( variableSampleSize )
// 		hintSampleSize = trakInfo.sampleSizeArray[useSampleIndex];

	    // REPLACED ABOVE WITH
	    if ( variableSampleSize ) {
		if (useSampleIndex >= trakInfo.sampleSizeArray.length) {
		    hintSampleSize = trakInfo.sampleSizeArray[trakInfo.sampleSizeArray.length-1];
		} else {
		    hintSampleSize = trakInfo.sampleSizeArray[useSampleIndex];
		}
	    }
	    
	    remainingHintSampleSize = hintSampleSize;

	    if (debug1) {
		System.out.println("hintSampleSize is " + hintSampleSize);
// 		System.out.println("useSampleIndex, offset " + useSampleIndex +
// 				   " : " + offset);
	    }

	    Object obj = buffer.getData();
	    byte[] data;
	    
	    if  ( (obj == null) ||
		  (! (obj instanceof byte[]) ) ||
		  ( ((byte[])obj).length < maxPacketSize) ) {
		data = new byte[maxPacketSize];
		buffer.setData(data);
	    } else {
		data = (byte[]) obj;
	    }

	    try {
		int actualBytesRead;
		synchronized(seekSync) {

		    {
			if (sampleIndex != useSampleIndex) { // Changed by setPosition()
			    // System.out.println("parser: audio: discard");
			    // System.out.println("$$$$ REMOVE: SHOULDN'T HAPPEN: setPosition discard");
			    buffer.setDiscard(true);
			    currentPacketNumber = 0;
			    numPacketsInSample = -1;
			    offsetToStartOfPacketInfo = -1;
			    rtpOffset = 0;
			    return;
			}
		    }

		    // NOTE: this useChunkNumber is actually sample number
		    long offset = trakInfo.index2Offset(useChunkNumber);
		    if (debug) {
			System.out.println("audio: Calling index2Offset on hint track with arg " +
					   useChunkNumber);
			System.out.println("offset is " + offset);
		    }


		    if ( offset == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
			buffer.setLength(0);
			buffer.setEOM(true);
			return;
		    }

		    if ( (cacheStream != null) && (listener != null) ) {

			if ( cacheStream.willReadBytesBlock(offset, hintSampleSize) ) {
// 			if ( cacheStream.willReadBytesBlock(offset+byteOffsetFromSampleOffset,
// 							    needBufferSize) ) {
			    listener.readHasBlocked(this);
			} else { // TODO: REMOVE ELSE BLOCK
// 			    System.out.println("audio: won't block: " + offset + " : " +
// 					       needBufferSize);
			}
		    }

		    long pos;
		    if (/*true*/debug1) {
			System.out.println("currentPacketNumber is " + currentPacketNumber);
		    }
		    if (offsetToStartOfPacketInfo < 0) {
			if (debug1) {
			    System.out.println("NEW SEEK");
			}
			pos = seekableStream.seek(offset);
			// System.out.println("seek returns " + pos);
			if ( pos == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
			    buffer.setDiscard(true);
			    return;
			}
			
			numPacketsInSample = parser.readShort(stream);
			if (debug) {
			    System.out.println("num packets in sample " + numPacketsInSample);
			}
			
			if (numPacketsInSample < 1) {
			    buffer.setDiscard(true);
			    return;
			}

			remainingHintSampleSize -= 2;
			parser.readShort(stream); // reserved
			remainingHintSampleSize -= 2;
		    } else {
			// PACKET TABLE STARTS HERE
// 			System.out.println("NO NEW SEEK but to offsetToStartOfPacketInfo " +
// 					   offsetToStartOfPacketInfo);

			pos = seekableStream.seek(offsetToStartOfPacketInfo);
			if ( pos == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
			    buffer.setDiscard(true);
			    return;
			}
		    }

		    // TODO: check to see of remainingHintSampleSize is positive

		    relativeTransmissionTime = parser.readInt(stream);
		    remainingHintSampleSize -= 4;

// 		    short rtpHeaderInfo = (short) parser.readShort(stream);
 		    int rtpHeaderInfo = parser.readShort(stream);
		    if (debug) {
			System.out.println("rtpHeaderInfo is " +
					   Integer.toHexString(rtpHeaderInfo));
		    }
		    rtpMarkerSet =  ((rtpHeaderInfo & 0x80) > 0);
// 		    System.out.println("rtp marker present? " + rtpMarkerSet); // REMOVE $$

		    remainingHintSampleSize -= 2;
		    rtpSequenceNumber = parser.readShort(stream);
		    remainingHintSampleSize -= 2;

		    paddingPresent = ( (rtpHeaderInfo & 0x2000) > 0);
		    extensionHeaderPresent = ( (rtpHeaderInfo & 0x1000) > 0);
// 		    System.out.println("rtp payload type " +
// 					   (rtpHeaderInfo & 0x007F)); // DELETE $$$


		    if (paddingPresent) {
			// System.err.println("qtparser:audio:rtpheader:paddingPresent"); // TODO
		    }

		    if (extensionHeaderPresent) {
			// System.err.println("qtparser:audio:rtpheader:extensionHeaderPresent"); // TODO

		    }

		    int flags = parser.readShort(stream);

		    if (debug) {
// 			System.out.println("rtp version " +
// 					   (rtpHeaderInfo & 0xC000));

			System.out.println("rtp marker present? " + rtpMarkerSet);
			System.out.println("rtp payload type " +
					   (rtpHeaderInfo & 0x007F));

			System.out.println("padding? " + paddingPresent);
			System.out.println("extension header? " + extensionHeaderPresent);
			System.out.println("audio hint: flags is " + Integer.toHexString(flags));
			// 		    System.out.println("Check if flag has X bit set " +
			// 				        ( (flags & 0x0004) > 0 ));
			// 		    System.out.println("  audio hint flags is " + flags);
		    }

		    remainingHintSampleSize -= 2;
		    
		    int entriesInDataTable = parser.readShort(stream);
		    remainingHintSampleSize -= 2;

		    // TODO: If bit 13 of flag is set, Extra info TLV table is present
			
		    // 			int tlvTableSize = parser.readInt(stream);
		    // 			System.out.println("tlvTableSize is " +
		    // 					   tlvTableSize);
		    // 			remainingHintSampleSize -= 4;
			
		    // TODO: If bit 13 of flag is set, Extra info TLV table is present
		    boolean extraInfoTLVPresent = ( (flags & 0x0004) > 0);

		    if (extraInfoTLVPresent) {
			int tlvTableSize = parser.readInt(stream);
			// TODO: for now extra info TLV is skipped
			skip(stream, tlvTableSize-4);

			if (debug) {
			    System.err.println("audio: extraInfoTLVPresent: Skipped"); //TODO?
			    System.out.println("tlvTableSize is " +
					       tlvTableSize);
			}

		    }

		    if (/*true*/debug) { // debug1
			System.out.println("Packet # " + currentPacketNumber);
			System.out.println("  relativeTransmissionTime is " +
					   relativeTransmissionTime);
			System.out.println("  rtpSequenceNumber is " + rtpSequenceNumber);
			System.out.println("  entriesInDataTable is " + entriesInDataTable);
		    }

		    for (int j = 0; j < entriesInDataTable; j++) {
			int dataBlockSource = parser.readByte(stream);
			remainingHintSampleSize--;;
			if (debug1) {
			    System.out.println("    dataBlockSource is " + dataBlockSource);
			}

			if ( dataBlockSource == HINT_IMMEDIATE_DATA ) {
			    int length = parser.readByte(stream);
			    // TODO: length should not be more than 14
			    // System.out.println("    data table length is " + length);
			    remainingHintSampleSize--;;

			    // System.out.println("IMM:rtpOffset is " + rtpOffset);
			    parser.readBytes(stream, data, rtpOffset, length);
			    rtpOffset += length;;
			    parser.skip(stream, (14-length));
			    remainingHintSampleSize -= 14;
			} else if (dataBlockSource == HINT_SAMPLE_DATA) {
			    int trackRefIndex = parser.readByte(stream);
			    if (debug1) {
				System.out.println("     audio:trackRefIndex is " + trackRefIndex);
			    }
			    // Note: Only trackRefIndex value of 0 and -1 are supported
			    // This means that the hint track referes to one media track
			    // A positive value implies that that the hint track refers
			    // to multiple media tracks -- this is not supported
			    if (trackRefIndex > 0) {
				System.err.println("     Currently we don't support hint tracks that refer to multiple media tracks: " + trackRefIndex);
				buffer.setDiscard(true);
				return;
			    }
				
			    int numBytesToCopy = parser.readShort(stream);
			    int sampleNumber = parser.readInt(stream);
			    int byteOffset = parser.readInt(stream);
			    int bytesPerCompresionBlock = parser.readShort(stream);
			    int samplesPerCompresionBlock = parser.readShort(stream);

			    if (/*true*/debug1) {
				System.out.println("     sample Number is " + sampleNumber);
				System.out.println("     numBytesToCopy is " + numBytesToCopy);
				System.out.println("     byteOffset is " + byteOffset);
				System.out.println("     bytesPerCompresionBlock is " + bytesPerCompresionBlock);
				System.out.println("     samplesPerCompresionBlock is " + samplesPerCompresionBlock);
			    }
			    remainingHintSampleSize -= 15;
			    long saveCurrentPos =  parser.getLocation(stream);
			    
			    // We have already taken care of trackRefIndex > 0 case
			    TrakList useTrakInfo;
			    if (trackRefIndex == 0) {
				useTrakInfo = sampleTrakInfo;
				if (debug2)
				    System.out.println("set useTrakInfo as sampleTrakInfo");
			    } else {
				// trackRefIndex < 0
				// $$ Note: To be precise we should check for trackRefIndex == -1
				// Data resides in a sample in the hint track itself
// 				System.out.println("trackRefIndex " + trackRefIndex +
// 						   " using trakInfo");
				useTrakInfo = trakInfo;
			    }

			    // sample numbers start from 1. But the array starts from
                            // 0. SO subtract 1

			    if (debug1) {
				System.out.println("useTrakInfo is " + useTrakInfo);
				System.out.println("useTrakInfo.sampleOffsetTable is " +
						   useTrakInfo.sampleOffsetTable);
			    }

			    long sampleOffset;
			    if (useTrakInfo.sampleOffsetTable == null) {
//$$?? 				System.out.println("useChunkNumber is " + useChunkNumber);
//$$?? 				sampleOffset = useTrakInfo.chunkOffsets[useChunkNumber];
				// sampleOffset = useTrakInfo.index2Offset(sampleNumber);

				// System.out.println("  audio: Calling index2Offset on audio sample track with arg " +
				// 				       (sampleNumber-1));

				sampleOffset = useTrakInfo.index2Offset(sampleNumber-1);
				if (debug1) {
				    System.out.println("chunkOffsets size is " +
						   useTrakInfo.chunkOffsets.length);
				    System.out.println("sampleOffset from index2Offset " +
						       sampleOffset);
				}
			    } else {
				sampleOffset =
				    useTrakInfo.sampleOffsetTable[sampleNumber-1];
			    }
			    // System.out.println("     sampleOffset is " + sampleOffset);
			    // System.out.println("  byteOffset is " + byteOffset);
			    sampleOffset += byteOffset;
			    // System.out.println("     sampleOffset + byteOffset " + sampleOffset);

			    pos = seekableStream.seek(sampleOffset);
			    if ( pos == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
				buffer.setDiscard(true);
				// System.err.println("seek error1 set offsetToStartOfPacketInfo -1");
				offsetToStartOfPacketInfo = -1;
				return;
			    }

			    if (/*true*/debug1) {
 				System.out.println("Audio: Seek to " + sampleOffset + " and read " + numBytesToCopy + " bytes into buffer with offset " + rtpOffset);
 			    }
// 			    {//DEBUG BLOCK REMOVE
// 				if (trackRefIndex < 0) {
// 				    System.out.println("Seek to " + sampleOffset + " and read " + numBytesToCopy + " bytes into buffer with offset " + rtpOffset);
// 				}
// 			    }

//  			    {//DEBUG BLOCK REMOVE
// 				if ( (numBytesToCopy & 0x1) > 0 ) {
// 				    numBytesToCopy--; // Make numBytesToCopy even
// 				    System.out.println("$$$ MAKING numBytesToCopy even");
// 				}
// 			    }

			    parser.readBytes(stream, data, rtpOffset, numBytesToCopy);
			    rtpOffset += numBytesToCopy;

			    // restore position
			    pos = seekableStream.seek(saveCurrentPos);
			    if ( pos == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
				buffer.setDiscard(true);
				// System.err.println("seek error2 set offsetToStartOfPacketInfo -1");
				offsetToStartOfPacketInfo = -1;
				return;
			    }
			} else if ( dataBlockSource == HINT_NOP_IGNORE ) {
			    // No Data, IGNORE
			    int length = parser.readByte(stream);
			    parser.skip(stream, length);
			    remainingHintSampleSize -= length;
			} else {
			    //TODO
			    // Need to support HINT_SAMPLE_DESCRIPTION
			    System.err.println("DISCARD: dataBlockSource " + dataBlockSource +
					       " not supported");
			    buffer.setDiscard(true);
			    offsetToStartOfPacketInfo = -1;
			    return;
			}
		    }
		    actualBytesRead = rtpOffset;
		    if (debug1) {
			System.out.println("Actual size of packet sent " + rtpOffset);
		    }
		    rtpOffset = 0;
		    // System.out.println("remainingHintSampleSize after is " + remainingHintSampleSize);
		    // Note: remainingHintSampleSize should be 0.

		    offsetToStartOfPacketInfo = parser.getLocation(stream);
// 		    System.out.println("offsetToStartOfPacketInfo from getLocation is " +
// 				       offsetToStartOfPacketInfo);

		    if ( actualBytesRead == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
			// System.err.println("DISCARD actualBytesRead  is LENGTH_DISCARD");

			buffer.setDiscard(true);
			return;
		    }
		}
		buffer.setLength(actualBytesRead);

		if (rtpMarkerSet) {
		    if (debug)
			System.out.println("rtpMarkerSet: true");
		    buffer.setFlags(buffer.getFlags() | Buffer.FLAG_RTP_MARKER);
		} else {
		    if (debug)
			System.out.println("rtpMarkerSet: false");
		    buffer.setFlags(buffer.getFlags() & ~Buffer.FLAG_RTP_MARKER);
		}
		buffer.setSequenceNumber(rtpSequenceNumber);
		// buffer.setTimeStamp((long) relativeTransmissionTime);

		double startTime = trakInfo.index2TimeAndDuration(useChunkNumber).startTime;
// 		System.out.println("AUDIO HINT: time for sample # " + useChunkNumber +
// 				   " is " + startTime + " [ " +
// 				   (startTime*1000) + " msec]");

		long timeStamp =
		    (long) ((startTime /*+ scaledRelativeTransmissionTime*/) * 1E9);
		buffer.setTimeStamp(timeStamp);

		buffer.setDuration(Buffer.TIME_UNKNOWN);
	    } catch (IOException e) {
		// System.err.println("readFrame: " + e);
		buffer.setLength(0);
		buffer.setEOM(true);
		// TODO: $$$$ Update maxFrame and duration
		// System.out.print("After EOM Updating maxLocation from " + maxLocation);
		// maxLocation = parser.getLocation(stream);
		// System.out.println(" to " + maxLocation);
	    }
	    synchronized(this) {
		if (chunkNumber != useChunkNumber) { // changed by setPosition()

		    // System.out.println("$$$$ REMOVE: SHOULDN'T HAPPEN: chunkNumber: setPosition discard");

		    currentPacketNumber = 0;
		    numPacketsInSample = -1;
		    offsetToStartOfPacketInfo = -1;
		    rtpOffset = 0;
		} else {
//  		    System.out.println("increment current packetnumber old, total" +
//  				       currentPacketNumber + " : " + numPacketsInSample);
		    currentPacketNumber++;
		    if (currentPacketNumber >= numPacketsInSample) {
			chunkNumber++;
			currentPacketNumber = 0;
			numPacketsInSample = -1;
			offsetToStartOfPacketInfo = -1;
			rtpOffset = 0;
		    }
		}
	    }
// 	    System.out.println("RETURN FROM readFrame, offsetToStartOfPacketInfo is " +
// 			       offsetToStartOfPacketInfo);
	}



    }


    private class HintVideoTrack extends VideoTrack  {
	int hintSampleSize;
	int indexOfTrackBeingHinted = trakInfo.indexOfTrackBeingHinted;
	int maxPacketSize;
	int currentPacketNumber = 0;
	int numPacketsInSample = -1;
	long offsetToStartOfPacketInfo = -1;
	TrakList sampleTrakInfo = null;

	HintVideoTrack(TrakList trakInfo) {
	    super(trakInfo);
	    format = ( (Hint) trakInfo.media).format;
	    hintSampleSize = needBufferSize;
	    maxPacketSize = trakInfo.maxPacketSize;

	    if (debug1) {
		System.out.println("HintVideoTrack: Index of hinted track: " +
				   trakInfo.indexOfTrackBeingHinted);
		System.out.println("HintVideoTrack: packet size is " +
				   maxPacketSize);
	    }
	    if (indexOfTrackBeingHinted >= 0) {
		sampleTrakInfo = trakList[indexOfTrackBeingHinted];
	    } else { // $$$ REMOVE else DEBUG BLOCK
		if (debug) {
		    System.out.println("sampleTrakInfo is not set " + indexOfTrackBeingHinted);
		}
	    }
	}


	void doReadFrame(Buffer buffer) {

	    boolean rtpMarkerSet = false;
	    boolean paddingPresent;
	    boolean extensionHeaderPresent;
	    int rtpSequenceNumber;
	    int relativeTransmissionTime;

	    if (indexOfTrackBeingHinted < 0) {
		buffer.setDiscard(true);
		return;
	    }

	    if ( useSampleIndex >= trakInfo.numberOfSamples ) {
		buffer.setLength(0);
		buffer.setEOM(true);
		//System.out.println("VIDEO: return EOM");
		return;
	    }
	    int rtpOffset = 0;

	    int remainingHintSampleSize;
	    if ( variableSampleSize )
		hintSampleSize = trakInfo.sampleSizeArray[useSampleIndex];
	    
	    remainingHintSampleSize = hintSampleSize;
	    long offset = trakInfo.sampleOffsetTable[useSampleIndex];

	    if (debug1) {
		System.out.println("hintSampleSize is " + hintSampleSize);
		System.out.println("useSampleIndex, offset " + useSampleIndex +
				   " : " + offset);
	    }

	    Object obj = buffer.getData();
	    byte[] data;
	    
	    if  ( (obj == null) ||
		  (! (obj instanceof byte[]) ) ||
		  ( ((byte[])obj).length < maxPacketSize) ) {
		data = new byte[maxPacketSize];
		buffer.setData(data);
	    } else {
		data = (byte[]) obj;
	    }

	    try {
		int actualBytesRead;
		synchronized(seekSync) {
		    {
			if (sampleIndex != useSampleIndex) { // Changed by setPosition()
			    // System.out.println("parser: video: discard");
			    buffer.setDiscard(true);
			    currentPacketNumber = 0;
			    numPacketsInSample = -1;
			    offsetToStartOfPacketInfo = -1;
			    rtpOffset = 0;
			    return;
			}
		    }

		    if  ( (cacheStream != null) && (listener != null) ) {
			if ( cacheStream.willReadBytesBlock(offset, hintSampleSize) ) {
// 			    System.out.println("video: will block: " + offset + " : " +
// 					       hintSampleSize);
			    listener.readHasBlocked(this);
			} else { // TODO: REMOVE ELSE BLOCK
// 			    System.out.println("video: won't block: " + offset + " : " +
// 					       hintSampleSize);
			}

		    }
		    long pos;
		    if (offsetToStartOfPacketInfo < 0) {
			pos = seekableStream.seek(offset);
			// System.out.println("seek returns " + pos);
			if ( pos == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
			    buffer.setDiscard(true);
			    return;
			}
			
			numPacketsInSample = parser.readShort(stream);
			if (/*true*/debug) {
			    System.out.println("video: num packets in sample " + numPacketsInSample);
			}
			
			if (numPacketsInSample < 1) {
			    buffer.setDiscard(true);
			    return;
			}

			remainingHintSampleSize -= 2;
			parser.readShort(stream); // reserved
			remainingHintSampleSize -= 2;
		    } else {
			// PACKET TABLE STARTS HERE
			pos = seekableStream.seek(offsetToStartOfPacketInfo);
			if ( pos == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
			    buffer.setDiscard(true);
			    return;
			}
		    }

		    // TODO: check to see of remainingHintSampleSize is positive

		    relativeTransmissionTime = parser.readInt(stream);
		    remainingHintSampleSize -= 4;
		    // short rtpHeaderInfo = (short) parser.readShort(stream);
		    int rtpHeaderInfo = (short) parser.readShort(stream);
		    rtpMarkerSet =  ((rtpHeaderInfo & 0x80) > 0);
		    remainingHintSampleSize -= 2;
		    rtpSequenceNumber = parser.readShort(stream);
		    remainingHintSampleSize -= 2;

		    paddingPresent = ( (rtpHeaderInfo & 0x2000) > 0);
		    extensionHeaderPresent = ( (rtpHeaderInfo & 0x1000) > 0);

		    if (paddingPresent) {
			// System.err.println("video: paddingPresent: SHOULD BE HANDLED"); // TODO
		    }

		    if (extensionHeaderPresent) {
			// System.err.println("video: extensionHeaderPresent: SHOULD BE HANDLED"); // TODO
		    }

		    int flags = parser.readShort(stream);

		    if (debug) {
// 			System.out.println("rtp version " +
// 					   (rtpHeaderInfo & 0xC000));
			System.out.println("rtp marker present? " + rtpMarkerSet);
			System.out.println("rtp payload type " +
					   (rtpHeaderInfo & 0x007F));

			System.out.println("padding? " + paddingPresent);
			System.out.println("extension header? " + extensionHeaderPresent);


			System.out.println("video hint: flags is " + Integer.toHexString(flags));
			// 		    System.out.println("Check if flag has X bit set " +
			// 				        ( (flags & 0x0004) > 0 ));
		    }

		    remainingHintSampleSize -= 2;
		    
		    int entriesInDataTable = parser.readShort(stream);
		    remainingHintSampleSize -= 2;

		    // TODO: If bit 13 of flag is set, Extra info TLV table is present
			
		    // 			int tlvTableSize = parser.readInt(stream);
		    // 			System.out.println("tlvTableSize is " +
		    // 					   tlvTableSize);
		    // 			remainingHintSampleSize -= 4;
			

		    boolean extraInfoTLVPresent = ( (flags & 0x0004) > 0);
		    if (extraInfoTLVPresent) {
			int tlvTableSize = parser.readInt(stream);
			// TODO: for now extra info TLV is skipped
			skip(stream, tlvTableSize-4);
			if (debug) {
			    System.err.println("video: extraInfoTLVPresent: Skipped"); // TODO
			    System.out.println("tlvTableSize is " +
					       tlvTableSize);
			}
		    }

		    if (debug) { // debug1
			System.out.println("Packet # " + currentPacketNumber);
			System.out.println("  relativeTransmissionTime is " +
					   relativeTransmissionTime);
			System.out.println("$$$ relativeTransmissionTime is in timescale " +
					   trakInfo.mediaTimeScale);
			// System.out.println("  rtpHeaderInfo is " + rtpHeaderInfo);
			System.out.println("  rtpSequenceNumber is " + rtpSequenceNumber);
			System.out.println("  entriesInDataTable is " + entriesInDataTable);
		    }

		    for (int j = 0; j < entriesInDataTable; j++) {
			int dataBlockSource = parser.readByte(stream);
			remainingHintSampleSize--;;
			if (debug1) {
			    System.out.println("    dataBlockSource is " + dataBlockSource);
			}

			if ( dataBlockSource == HINT_IMMEDIATE_DATA ) {
			    int length = parser.readByte(stream);
			    // TODO: length should not be more than 14
			    // System.out.println("    data table length is " + length);
			    remainingHintSampleSize--;;

			    // System.out.println("IMM:rtpOffset is " + rtpOffset);
// 			    System.out.println("video IMM data read " + length +
// 					       " bytes into data");
			    parser.readBytes(stream, data, rtpOffset, length);
			    rtpOffset += length;;
			    parser.skip(stream, (14-length));
			    remainingHintSampleSize -= 14;
			} else if (dataBlockSource == HINT_SAMPLE_DATA) {
			    int trackRefIndex = parser.readByte(stream);
			    if (debug1) {
				System.out.println("     video: trackRefIndex is " + trackRefIndex);
			    }
			    // Note: Only trackRefIndex value of 0 and -1 are supported
			    // This means that the hint track referes to one media track
			    // A positive value implies that that the hint track refers
			    // to multiple media tracks -- this is not supported
			    if (trackRefIndex > 0) {
				System.err.println("     Currently we don't support hint tracks that refer to multiple media tracks");
				buffer.setDiscard(true);
				return;
			    }
				
			    int numBytesToCopy = parser.readShort(stream);
			    int sampleNumber = parser.readInt(stream);
			    int byteOffset = parser.readInt(stream);
			    int bytesPerCompresionBlock = parser.readShort(stream);
			    int samplesPerCompresionBlock = parser.readShort(stream);

			    if (debug1) {
				System.out.println("     sample Number is " + sampleNumber);
				System.out.println("     numBytesToCopy is " + numBytesToCopy);
				System.out.println("     byteOffset is " + byteOffset);
				System.out.println("     bytesPerCompresionBlock is " + bytesPerCompresionBlock);
				System.out.println("     samplesPerCompresionBlock is " + samplesPerCompresionBlock);
			    }
			    remainingHintSampleSize -= 15;
			    long saveCurrentPos =  parser.getLocation(stream);
			    
			    // We have already taken care of trackRefIndex > 0 case
			    TrakList useTrakInfo;
			    if (trackRefIndex == 0) {
				useTrakInfo = sampleTrakInfo;
			    } else {
				// trackRefIndex < 0
				// $$ Note: To be precise we should check for trackRefIndex == -1
				// Data resides in a sample in the hint track itself
// 				System.out.println("trackRefIndex " + trackRefIndex +
// 						   " using trakInfo");
				useTrakInfo = trakInfo;
			    }

			    // sample numbers start from 1. But the array starts from
                            // 0. SO subtract 1

// 			    System.out.println("useTrakInfo is " + useTrakInfo);
// 			    System.out.println("useTrakInfo.sampleOffsetTable is " +
// 					       useTrakInfo.sampleOffsetTable);
			    long sampleOffset =
				useTrakInfo.sampleOffsetTable[sampleNumber-1];
			    // System.out.println("     sampleOffset is " + sampleOffset);
			    sampleOffset += byteOffset;
			    // System.out.println("     sampleOffset + byteOffset " + sampleOffset);

			    pos = seekableStream.seek(sampleOffset);
			    if ( pos == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
				buffer.setDiscard(true);
				offsetToStartOfPacketInfo = -1;
				return;
			    }

			    if (debug1) {
				System.out.println("     read " + numBytesToCopy + " bytes from offset " + rtpOffset);
			    }
			    parser.readBytes(stream, data, rtpOffset, numBytesToCopy);
			    rtpOffset += numBytesToCopy;

			    // restore position
			    pos = seekableStream.seek(saveCurrentPos);
			    if ( pos == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
				buffer.setDiscard(true);
				offsetToStartOfPacketInfo = -1;
				return;
			    }
			} else {
			    //TODO
			    // Need to support 0 ==> No Data and
			    //                 3 ==> Sample description data
			    buffer.setDiscard(true);
			    offsetToStartOfPacketInfo = -1;
			    return;
			}
		    }
		    actualBytesRead = rtpOffset;
		    if (debug1) {
			System.out.println("Actual size of packet sent " + rtpOffset);
		    }
		    rtpOffset = 0;
		    // System.out.println("remainingHintSampleSize after is " + remainingHintSampleSize);
		    // Note: remainingHintSampleSize should be 0.

		    offsetToStartOfPacketInfo = parser.getLocation(stream);

		    if ( actualBytesRead == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
			buffer.setDiscard(true);
			return;
		    }
		}
		buffer.setLength(actualBytesRead);

		if (rtpMarkerSet) {
		    if (debug)
			System.out.println("rtpMarkerSet: true");
		    buffer.setFlags(buffer.getFlags() | Buffer.FLAG_RTP_MARKER);
		} else {
		    if (debug)
			System.out.println("rtpMarkerSet: false");
		    buffer.setFlags(buffer.getFlags() & ~Buffer.FLAG_RTP_MARKER);
		}
		buffer.setSequenceNumber(rtpSequenceNumber);
		// buffer.setTimeStamp((long) relativeTransmissionTime);

		TimeAndDuration td = trakInfo.index2TimeAndDuration(useSampleIndex);
		double startTime = td.startTime;
// 		System.out.println("VIDEO HINT: time for sample # " + useSampleIndex +
// 				   " is " + startTime + " [ " +
// 				   (startTime*1000) + " msec]");

// 		double scaledRelativeTransmissionTime =
// 		    (double) relativeTransmissionTime / (trakInfo.mediaTimeScale);

// 		System.out.println("scaledRelativeTransmissionTime for " +
// 				   relativeTransmissionTime +
// 				   " is " +
// 				   scaledRelativeTransmissionTime);
// 		long timeStamp =
// 		    (long) ((startTime + scaledRelativeTransmissionTime) * 1E9);
// 		System.out.println("Video timeStamp: " +
// 				   (startTime + scaledRelativeTransmissionTime));
// 		buffer.setTimeStamp(timeStamp);


		long timeStamp =
		    (long) ((startTime /*+ scaledRelativeTransmissionTime*/) * 1E9);
// 		System.out.println("VIDEO HINT: setTimeStamp in msec "
// 				   + (int) (startTime*1000)
// 				   + " sampleindex " + useSampleIndex);
		buffer.setTimeStamp(timeStamp);
		

// 		{ //DEBUG BLOCK print data values
// 		    for (int tt = 0; tt < 30; tt++) {
// 			int vv = (int) data[tt];
// 		        System.out.println(Integer.toHexString(vv));
// 		    }
// 		}
		buffer.setDuration((long) (td.duration * 1E9));
	    } catch (IOException e) {
		// System.err.println("readFrame: " + e);
		buffer.setLength(0);
		buffer.setEOM(true);
		// TODO: $$$$ Update maxFrame and duration
		// System.out.print("After EOM Updating maxLocation from " + maxLocation);
		// maxLocation = parser.getLocation(stream);
		// System.out.println(" to " + maxLocation);
	    }
	    synchronized(this) {
		if (sampleIndex != useSampleIndex) { // changed by setPosition()
		    currentPacketNumber = 0;
		    numPacketsInSample = -1;
		    offsetToStartOfPacketInfo = -1;
		    rtpOffset = 0;
		} else {
// 		    System.out.println("increment current packetnumber old, total" +
// 				       currentPacketNumber + " : " + numPacketsInSample);
		    currentPacketNumber++;
// 		    System.out.println("currentPacketNumber >= numPacketsInSample " +
// 				       currentPacketNumber + " : " +
// 				       numPacketsInSample);
		    if (currentPacketNumber >= numPacketsInSample) {
			sampleIndex++;
			currentPacketNumber = 0;
			numPacketsInSample = -1;
			offsetToStartOfPacketInfo = -1;
			rtpOffset = 0;
		    }
		}
	    }
	}

    }


    private class TimeAndDuration {
	double startTime;
	double duration;
    }

    // TODO: ENHANCE: See if you free up some arrays that you no longer need.
    // For e.g for video trak, the sampletochunk array if all the
    // offsets for all the samples have been calculated.

    // TODO: ENHANCE
    // for video because the number of samples is not large
    // time to sample #, offset[sample #]
    // For audio number of samples is large, so
    // time to sample #, sample # to chunk number, chunk # to chunk offset,
    // offset within chunk from sample size.
    // For very very large video files, the number of video samples
    // may be large and so you may have to follow the approach used for
    // audio. Need some experimentation to decide the
    // cutoff max_num_samples_in_video value

}
