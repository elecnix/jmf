/*
 * @(#)AviParser.java	1.39 03/03/30
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.parser.video;

import java.io.IOException;
import javax.media.Track;
import javax.media.Time;
import javax.media.Duration;
import javax.media.IncompatibleSourceException;
import javax.media.BadHeaderException;
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
import com.sun.media.format.WavAudioFormat;
import javax.media.format.VideoFormat;
import com.sun.media.parser.BasicPullParser;
import com.sun.media.vfw.BitMapInfo;


public class AviParser extends BasicPullParser {

    private static ContentDescriptor[] supportedFormat =
	new ContentDescriptor[] {new ContentDescriptor("video.x_msvideo")};
    private PullSourceStream stream = null;
    private CachedStream cacheStream;
    private Track[] tracks;
    private Seekable seekableStream;
    private int numSupportedTracks = 0;
    private int length;
    private int audioTrack = -1;
    private int videoTrack = -1;
    private int keyFrameTrack = -1;
    private final static int SIZE_OF_AVI_INDEX   = 16;
    private final static int AVIH_HEADER_LENGTH = 56;
    private final static int STRH_HEADER_LENGTH = 56;
    private final static int STRF_VIDEO_HEADER_LENGTH = 40;
    private final static int STRF_AUDIO_HEADER_LENGTH = 16;

    final static int AVIF_HASINDEX 		= 0x00000010;
    final static int AVIF_MUSTUSEINDEX		= 0x00000020;
    final static int AVIF_ISINTERLEAVED 	= 0x00000100;
    final static int AVIF_WASCAPTUREFILE	= 0x00010000;
    final static int AVIF_COPYRIGHTED		= 0x00020000;
    final static int AVIF_KEYFRAME		= 0x00000010;

    final static String AUDIO         = "auds";
    final static String VIDEO         = "vids";

    final static String LISTRECORDCHUNK        = "rec ";
    final static String VIDEO_MAGIC		= "dc"; // Video
    final static String VIDEO_MAGIC_JPEG	= "db"; // Video
    final static String VIDEO_MAGIC_IV32a	= "iv"; // Indeo 3.2
    final static String VIDEO_MAGIC_IV32b	= "32"; // Indeo 3.2
    final static String VIDEO_MAGIC_IV31	= "31"; // Indeo 3.1
    final static String VIDEO_MAGIC_CVID	= "id"; // Cinepak
    final static String AUDIO_MAGIC		= "wb"; // Audio

    private int usecPerFrame = 0;
    private long nanoSecPerFrame = 0;
    private int maxBytesPerSecond;
    private int paddingGranularity;
    private int flags;
    private int totalFrames = 0;
    private int initialFrames;
    private int numTracks = 0;
    private int suggestedBufferSize;
    private int width;
    private int height;
    private TrakList[] trakList;
    private int idx1MinimumChunkOffset;
    private int moviOffset = 0;
    private Time duration = Duration.DURATION_UNKNOWN;

    private boolean moviChunkSeen = false;
    private boolean idx1ChunkSeen = false;
    private int maxAudioChunkIndex = 0;
    private int maxVideoChunkIndex = 0;

    private int extraHeaderLength = 0;
    private byte[] codecSpecificHeader = null; // TODO: Move it into Audio class?


    // Used to make the seek and the subsequent readBytes call atomic
    // operations, so that the video and audio track
    // threads don't trample each other.
    private Object seekSync = new Object();

    /**
     * Avi format requires that the stream be seekable and
     * random accessible. 
     */
    protected boolean supports(SourceStream[] streams) {
	return seekable;
    }

    public void setSource(DataSource source)
	throws IOException, IncompatibleSourceException {

	super.setSource(source);
	stream = (PullSourceStream) streams[0];
	seekableStream = (Seekable) streams[0];
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

	readHeader();
 	if (!moviChunkSeen) {
 	    throw new BadHeaderException("No movi chunk");
 	}

	if (!idx1ChunkSeen) {
	    // System.err.println("Currently files with no idx1 chunk are not supported");
 	    throw new BadHeaderException("Currently files with no idx1 chunk are not supported");
	}

	if (numTracks <= 0) {
	    throw new BadHeaderException("Error parsing header");
	}

	tracks = new Track[numTracks];


	// System.out.println("Number of tracks: " + tracks.length);
	for (int i = 0; i < tracks.length; i++) {
	    // System.out.println("Track # " + (i+1));
	    TrakList trakInfo = trakList[i];
	    if (trakInfo.trackType.equals(AUDIO)) {
		tracks[i] = new AudioTrack(trakInfo);
	    } else if (trakInfo.trackType.equals(VIDEO)) {
// 		System.out.println("Number of frames in Video track is " +
// 				   trakInfo.maxChunkIndex);
		tracks[i] = new VideoTrack(trakInfo);
	    }
	}
	return tracks;

    }

    private void readHeader()
	throws IOException, BadHeaderException {
	
	String magicRIFF = readString(stream);
	if (!(magicRIFF.equals("RIFF"))) {
	    throw new BadHeaderException("AVI Parser: expected string RIFF, got "
					 + magicRIFF);
	}

	length = readInt(stream, /* bigEndian = */ false);
	length += 8; // For RIFF and AVI

	String magicAVI = readString(stream);
	if (!(magicAVI.equals("AVI "))) {
	    throw new BadHeaderException("AVI Parser: expected string AVI, got "
					 + magicAVI);
	}

	int currentTrack = 0;
	while (getLocation(stream) <= (length-12)) {
	    String next = readString(stream);
	    int subchunkLength = readInt(stream, /* bigEndian = */ false);
	    if (next.equals("LIST")) {
		String subchunk = readString(stream);
		if (subchunk.equals("hdrl")) {
		    parseHDRL();
		} else if (subchunk.equals("strl")) {
		    parseSTRL(subchunkLength, currentTrack);
		    currentTrack++;
		} else if (subchunk.equals("movi"))
		    parseMOVI(subchunkLength - 4);
		else {
		    // System.err.println("Unsupported subchunk " + subchunk +
		    //  " in LIST");
		    skip(stream, subchunkLength-4);
		}
	    } else if (next.equals("idx1")) {
		parseIDX1(subchunkLength);
	    } else {
		skip(stream, subchunkLength);
		if ( (subchunkLength & 1) > 0)
		    skip(stream, 1);
	    }
	}
	if ( (totalFrames != 0) && (usecPerFrame != 0) ) {
	    duration = new Time((long) usecPerFrame * totalFrames * 1000);
	}
    }

    // The inner class can use this method as they cannot use
    // the getLocation(stream) method.
    private long getLocation() {
	return getLocation(stream);
    }

    private void parseHDRL() throws BadHeaderException {

	try {
	    String next = readString(stream);
	    if (!next.equals("avih")) {
		throw new BadHeaderException("AVI Parser: expected string AVIH, got "
					 + next);
	    }
	    int headerLength = readInt(stream, /* bigEndian = */ false);
	    parseAVIH(headerLength);
	    trakList = new TrakList[numTracks];
	} catch (IOException e) {
	    throw new BadHeaderException("IOException when parsing hdrl");
	}

    }


    private void parseSTRL(int length, int currentTrack) throws BadHeaderException {
 	try {
	    if (currentTrack >= trakList.length ) {
		throw new BadHeaderException("inconsistent number of strl atoms");
	    }

	    length -= 12; // for "LIST <length> strl"
	    while (length >= 12) { // TODO: check
		String subchunkid = readString(stream);
		int subchunkLength = readInt(stream, /* bigEndian = */ false);
		if (subchunkid.equals("strh")) {
		    parseSTRH(subchunkLength, currentTrack);
		} else if (subchunkid.equals("strf")) {
		    if (trakList[currentTrack] == null) {
			throw new BadHeaderException("strf doesn't have a strh atom preceding it");
		    }
		    parseSTRF(subchunkLength, currentTrack);
		} else {
		    // System.err.println("Unsupported subchunk " + subchunkid +
		    //	       " in strl. length " + subchunkLength);
		    if ( (subchunkLength & 1) > 0) {
			// Some avi files like billy.avi are don't have strn
			// chunks with incorrect odd number for the length.
			// The actual offset is 1 more. If this correction
			// is not made all the remaining chunks will be read
			// incorrectly.
			subchunkLength++;
		    }
		    skip(stream, subchunkLength);
		}
		length -= (subchunkLength + 4); // 4 is for subchunkid
	    }
	} catch (IOException e) {
	    throw new BadHeaderException("IOException when parsing hdrl");
	}
    }

    private void parseSTRH(int length, int currentTrack) throws BadHeaderException {
  	try {
	    if (length < STRH_HEADER_LENGTH) {
		throw new BadHeaderException("strh: header length should be atleast " +
					     STRH_HEADER_LENGTH + " but is " +
					     length);
	    }

	    trakList[currentTrack] = new TrakList();
	    trakList[currentTrack].trackType = readString(stream);
	    trakList[currentTrack].streamHandler = readString(stream);
	    trakList[currentTrack].flags = readInt(stream, /* bigEndian = */ false);
	    trakList[currentTrack].priority = readInt(stream, /* bigEndian = */ false);
	    trakList[currentTrack].initialFrames = readInt(stream, /* bigEndian = */ false);
	    trakList[currentTrack].scale = readInt(stream, /* bigEndian = */ false);
	    trakList[currentTrack].rate = readInt(stream, /* bigEndian = */ false);
	    trakList[currentTrack].start = readInt(stream, /* bigEndian = */ false);
	    trakList[currentTrack].length = readInt(stream, /* bigEndian = */ false);
	    trakList[currentTrack].suggestedBufferSize = readInt(stream, /* bigEndian = */ false);
	    trakList[currentTrack].quality = readInt(stream, /* bigEndian = */ false);
	    trakList[currentTrack].sampleSize = readInt(stream, /* bigEndian = */ false);

	    skip(stream, 8); // Padding
	    if ( (length - STRH_HEADER_LENGTH) > 0)
		skip(stream, length - STRH_HEADER_LENGTH);

 	} catch (IOException e) {
 	    throw new BadHeaderException("IOException when parsing hdrl");
 	}
    }


    private void parseSTRF(int length, int currentTrack) throws BadHeaderException {
  	try {
	    String trackType = trakList[currentTrack].trackType;
	    if (trackType.equals(VIDEO)) {
		Video video = new Video();
		video.size = readInt(stream, /* bigEndian = */ false);
		video.width = readInt(stream, /* bigEndian = */ false);
		video.height = readInt(stream, /* bigEndian = */ false);
		video.planes = readShort(stream, /* bigEndian = */ false);
		video.depth = readShort(stream, /* bigEndian = */ false);
		// Instead of readString, read the four bytes to see
		// if its a raw format.
		byte [] intArray = new byte[4];
		readBytes(stream, intArray, 4);
		if (intArray[0] > 32) {
		    video.compressor = new String(intArray);
		} else {
		    switch (intArray[0]) {
		    case 0:
			video.compressor = VideoFormat.RGB;
			break;
		    case 1:
			video.compressor = "rle8";
			break;
		    case 2:
			video.compressor = "rle4";
			break;
		    case 3:
			video.compressor = VideoFormat.RGB;
			break;
		    }
		}
		
		// Get the BITMAPINFO data needed by the decompressor
		BitMapInfo bmi = new BitMapInfo();
		bmi.biWidth = video.width;
		bmi.biHeight = video.height;
		bmi.biPlanes = video.planes;
		bmi.biBitCount = video.depth;
		bmi.fourcc = new String(video.compressor);
		video.bitMapInfo = bmi;
		bmi.biSizeImage = readInt(stream, false);
		bmi.biXPelsPerMeter = readInt(stream, false);
		bmi.biYPelsPerMeter = readInt(stream, false);
		bmi.biClrUsed = readInt(stream, false);
		bmi.biClrImportant = readInt(stream, false);
		

		if ( (length - STRF_VIDEO_HEADER_LENGTH) > 0) {
		    bmi.extraSize = (length - STRF_VIDEO_HEADER_LENGTH);
		    bmi.extraBytes = new byte[bmi.extraSize];
		    readBytes(stream, bmi.extraBytes, bmi.extraSize);
		}
		
		trakList[currentTrack].media = video;
		trakList[currentTrack].media.maxSampleSize =
		    trakList[currentTrack].suggestedBufferSize;
		videoTrack = currentTrack;
	    } else if (trackType.equals(AUDIO)) {
		Audio audio = new Audio();

                audio.formatTag = readShort(stream, /* bigEndian = */ false);
                audio.channels = readShort(stream, /* bigEndian = */ false);
                audio.sampleRate = readInt(stream, /* bigEndian = */ false);
                audio.avgBytesPerSec = readInt(stream, /* bigEndian = */ false);
                audio.blockAlign = readShort(stream, /* bigEndian = */ false);
                audio.bitsPerSample = readShort(stream, /* bigEndian = */ false);

		int remainingFormatSize = length - STRF_AUDIO_HEADER_LENGTH;

		codecSpecificHeader = null;
		int extraFieldsSize = 0;
		if (remainingFormatSize >= 2) {
		    extraFieldsSize = readShort(stream, /* bigEndian = */ false);
		    remainingFormatSize -= 2;

		    if (extraFieldsSize > 0) {
			codecSpecificHeader = new byte[extraFieldsSize];
			readBytes(stream, codecSpecificHeader, codecSpecificHeader.length);
			remainingFormatSize -= extraFieldsSize;
		    }


		    // TODO: do other encodings provide samplesPerBlock?
		    // Note that this info is there in codecSpecificHeader
		    if ( (audio.formatTag == WavAudioFormat.WAVE_FORMAT_ADPCM) ||
			 (audio.formatTag == WavAudioFormat.WAVE_FORMAT_DVI_ADPCM) ||
			 (audio.formatTag == WavAudioFormat.WAVE_FORMAT_GSM610) ) {

			if (extraFieldsSize < 2) {
			    throw new
				BadHeaderException("samplesPerBlock field not available for encoding" + audio.formatTag);
							 
			}
			audio.samplesPerBlock = BasicPullParser.parseShortFromArray(codecSpecificHeader,
								/* bigEndian = */ false);
		    }
		}
		    
		if (remainingFormatSize < 0) {
		    throw new BadHeaderException("Avi Parser: incorrect headersize in the STRF");
		}

 		if ( remainingFormatSize > 0)
 		    skip(stream, length - STRF_AUDIO_HEADER_LENGTH);

		trakList[currentTrack].media = audio;
		audioTrack = currentTrack;
	    } else {
		throw new BadHeaderException("strf: unsupported stream type " + trackType);
	    }

 	} catch (IOException e) {
 	    throw new BadHeaderException("IOException when parsing hdrl");
 	}
    }

    private void parseAVIH(int length) throws BadHeaderException {
 	try {
	    if (length < AVIH_HEADER_LENGTH) {
		throw new BadHeaderException("avih: header size is not 56");
	    }

	    usecPerFrame = readInt(stream, /* bigEndian = */ false);
	    nanoSecPerFrame = usecPerFrame * 1000;
	    maxBytesPerSecond = readInt(stream, /* bigEndian = */ false);
	    paddingGranularity = readInt(stream, /* bigEndian = */ false);
	    flags = readInt(stream, /* bigEndian = */ false);
	    totalFrames = readInt(stream, /* bigEndian = */ false);
	    initialFrames = readInt(stream, /* bigEndian = */ false);
	    numTracks = readInt(stream, /* bigEndian = */ false);
	    suggestedBufferSize = readInt(stream, /* bigEndian = */ false);
	    width = readInt(stream, /* bigEndian = */ false);
	    height = readInt(stream, /* bigEndian = */ false);
	    skip(stream, 4*4); // int reserved[4]
	    if ( (length - AVIH_HEADER_LENGTH) > 0)
		skip(stream, length - AVIH_HEADER_LENGTH);
 	} catch (IOException e) {
 	    throw new BadHeaderException("IOException when parsing hdrl");
 	}
    }


    private void parseIDX1(int length) throws BadHeaderException {
	try {
	    if (!moviChunkSeen) {
		throw new BadHeaderException("idx1 chunk appears before movi chunk");
	    }
	    // TODO: check for valid length value
	    int numIndices = (length / SIZE_OF_AVI_INDEX);
	    String id;
	    int flag;
	    int chunkOffset;
	    int chunkLength;

	    for (int i = 0; i < numTracks; i++) {
		if (trakList[i] == null) {
		    throw new BadHeaderException("Bad file format");
		}
		trakList[i].chunkInfo = new AVIIndexEntry[numIndices];
		if (trakList[i].trackType.equals(VIDEO)) {
		    trakList[i].keyFrames = new int[numIndices];
		}
	    }

	    idx1MinimumChunkOffset = Integer.MAX_VALUE;

	    for (int i = 0; i < numIndices; i++) {
		id = readString(stream);
 		if (id.equals(LISTRECORDCHUNK)) {
		    // $$$ DISCARD for now
		    readInt(stream, /* bigEndian = */ false);
		    readInt(stream, /* bigEndian = */ false);
		    readInt(stream, /* bigEndian = */ false);
 		    continue;
		}
		int streamNumber;
		try {
		    streamNumber = Integer.parseInt(id.substring(0,2));
		} catch (NumberFormatException e) {
		    // DISCARD chunk at it doesn't represent a stream
		    readInt(stream, /* bigEndian = */ false);
		    readInt(stream, /* bigEndian = */ false);
		    readInt(stream, /* bigEndian = */ false);
 		    continue;
		}

		if ( (streamNumber < 0) || (streamNumber >= numTracks) ) {
		    throw new BadHeaderException("index chunk has illegal stream # " +
						 streamNumber);
		}
 		flag = readInt(stream, /* bigEndian = */ false);
 		chunkOffset = readInt(stream, /* bigEndian = */ false);
 		chunkLength = readInt(stream, /* bigEndian = */ false);
		
		AVIIndexEntry[] chunkInfo = trakList[streamNumber].chunkInfo;
		int index = trakList[streamNumber].maxChunkIndex;
		
		chunkInfo[index] = new AVIIndexEntry();
		chunkInfo[index].id = id;
		chunkInfo[index].flag = flag;
		chunkInfo[index].chunkOffset = chunkOffset;
		chunkInfo[index].chunkLength = chunkLength;
		
		if (trakList[streamNumber].trackType.equals(AUDIO)) {
		    int c = trakList[streamNumber].tmpCumulativeChunkLength += chunkLength;
		    chunkInfo[index].cumulativeChunkLength = c;
		}
		
		if (trakList[streamNumber].trackType.equals(VIDEO)) {
		    if ( (flag & AVIF_KEYFRAME) > 0 ) {
			int keyFrameIndex = trakList[streamNumber].numKeyFrames;
			trakList[streamNumber].keyFrames[keyFrameIndex] = index;
			trakList[streamNumber].numKeyFrames++;
		    }
		}
		trakList[streamNumber].maxChunkIndex++;
		
		if (chunkOffset < idx1MinimumChunkOffset) {
		    idx1MinimumChunkOffset = chunkOffset;
		}
	    }

	    // For video tracks, if all the frames are not key frames,
	    // build the indexToKeyframeIndex table
	    // which maps a video frame to a key frame.
	    for (int i = 0; i < numTracks; i++) {
		if (trakList[i].trackType.equals(VIDEO)) {
		    int numKeyFrames = trakList[i].numKeyFrames;
		    if (numKeyFrames > 0)
			keyFrameTrack = i;
		    int maxChunkIndex = trakList[i].maxChunkIndex;
		    if ( (numKeyFrames > 0) && (numKeyFrames < maxChunkIndex) ) {
			trakList[i].indexToKeyframeIndex =
			    buildIndexToKeyFrameIndexTable(trakList[i].keyFrames,
							   numKeyFrames,
							   maxChunkIndex);
		    }
		    trakList[i].keyFrames = null;
		}
	    }

	    if (idx1MinimumChunkOffset >=  moviOffset) {
		// idx1 chunk offsets refer to start of the file.
		moviOffset = 0;
	    }
	    moviOffset += 8; // for chunk id and size
	} catch (IOException e) {
	    throw new BadHeaderException("IOException when parsing IDX1");
	}
	idx1ChunkSeen = true;
    }

    private void parseMOVI(int length) throws BadHeaderException {
	try {
	    moviChunkSeen = true;
	    if ( (flags & AVIF_HASINDEX) > 0) {
		// Subtract 4 to include MOVI string
		moviOffset = (int) getLocation(stream) - 4;
		skip(stream, length);
	    } else {
		// System.out.println("parseMOVI: NO AVIF_HASINDEX"); // REMOVE
	    }
	} catch (IOException e) {
	    throw new BadHeaderException("IOException when parsing movi");
	}
    }

    public Time setPosition(Time where, int rounding) {
	int keyframeNum = -1;
	if ( (keyFrameTrack != -1) && (tracks[keyFrameTrack].isEnabled()) ) {
	    // keyframe track present and is enabled

	    TrakList trakInfo = trakList[keyFrameTrack];
	    Track track = tracks[keyFrameTrack];
	    int frameNum = track.mapTimeToFrame(where);
	    keyframeNum = frameNum;
	    // TODO: handle FRAME_UNKNOWN

	    if (trakInfo.indexToKeyframeIndex.length > frameNum) {
		keyframeNum = trakInfo.indexToKeyframeIndex[frameNum];
	    }

	    if (keyframeNum != frameNum) {
		where = track.mapFrameToTime(keyframeNum);
	    }
	}
	for (int i = 0; i < numTracks; i++) {
	    if (!tracks[i].isEnabled())
		continue;

	    int chunkNumber =0;
	    int offsetWithinChunk = 0;
	    try {
		if (i == keyFrameTrack) {
		    chunkNumber = keyframeNum;
		    continue;
		}

		TrakList trakInfo = trakList[i];
		if (trakInfo.trackType.equals("vids")) {
		    if (usecPerFrame != 0) {
			chunkNumber = (int) (where.getNanoseconds() / nanoSecPerFrame);
			if (chunkNumber < 0)
			    chunkNumber = 0;
			else if (chunkNumber >= trakInfo.maxChunkIndex) {
			    continue; // EOM
			}
		    }
		} else if (trakInfo.trackType.equals("auds")) {
		    int bytePos = (int) ( where.getSeconds() *
					  ((Audio) trakInfo.media).avgBytesPerSec);
		    if (bytePos < 0)
			bytePos = 0;

		    // Note: the else statement can also handle the if
		    // case, ie maxChunkIndex == 1, but is separated here
		    // for clarity and a slight efficiency.
		    if (trakInfo.maxChunkIndex == 1) {
			if (bytePos >= trakInfo.chunkInfo[0].chunkLength) {
			    chunkNumber = trakInfo.maxChunkIndex; // EOM
			    continue; // EOM
			}
			chunkNumber = 0;
			offsetWithinChunk = bytePos;
		    } else {
			int approx;
			chunkNumber = trakInfo.getChunkNumber(bytePos);
			if (chunkNumber >= trakInfo.maxChunkIndex)
			    continue; // EOM
			
			approx = trakInfo.chunkInfo[chunkNumber].cumulativeChunkLength -
			    trakInfo.chunkInfo[chunkNumber].chunkLength;
			offsetWithinChunk = bytePos - approx;
		    }

		    if ( (offsetWithinChunk & 1) > 0)
			offsetWithinChunk--;
		    
		    int blockAlign = ((Audio) trakInfo.media).blockAlign;
		    if (blockAlign != 0) {
			offsetWithinChunk -= (offsetWithinChunk % blockAlign);
		    }
		}
	    } finally {
		((MediaTrack)tracks[i]).setChunkNumberAndOffset(chunkNumber,
								offsetWithinChunk);
	    }
	}
	return where;
    }

    public Time getMediaTime() {
	return null;  // TODO
    }

    public Time getDuration() {
	return duration;
    }

    /**
     * Returns a descriptive name for the plug-in.
     * This is a user readable string.
     */
    public String getName() {
	return "Parser for avi file format";
    }

    private boolean isSupported(String trackType) {
	return ( trackType.equals(VIDEO) || trackType.equals(AUDIO) );
    }


    private int[] buildIndexToKeyFrameIndexTable(int[] syncSamples,
					 int numKeyFrames,
					 int numberOfSamples) {
	
	int[] syncSampleMapping = new int[numberOfSamples];
	int index = 0;
	int previous;
	if (syncSamples[0] != 0) {
	    // Bug in the sync table of the avi file
	    // The first sample should always be a key frame
	    previous = syncSampleMapping[0] = 0;
	} else {
	    previous = syncSampleMapping[0] = 0;
	    index++;
	}
	
	for (; index < numKeyFrames; index++) {
	    int next = syncSamples[index];
	    for (int j = previous+1; j < next; j++) {
		syncSampleMapping[j] = previous;
	    }

	    syncSampleMapping[next] = next;

	    previous = next;
	}
	int lastSyncFrame = syncSamples[numKeyFrames - 1];
	for (index = lastSyncFrame+1; index < numberOfSamples; index++) {
	    syncSampleMapping[index] = lastSyncFrame;
	}
	return syncSampleMapping;
    }

    private abstract class Media {
        int maxSampleSize;
	abstract Format createFormat();
    }

    private class Audio extends Media {
	int formatTag;
	int channels;
	int sampleRate;
	int avgBytesPerSec;
	int blockAlign;
	int bitsPerSample;
	int samplesPerBlock;
	AudioFormat format = null;

	Format createFormat() {
	    if (format != null)
		return format;
	    String encodingString = (String)
		WavAudioFormat.formatMapper.get(new Integer(formatTag));
	    if (encodingString == null) {
		encodingString = "unknown";
	    }

	    boolean signed;
	    if (bitsPerSample > 8)
		signed = true;
	    else
		signed = false;

// 	    format = new AudioFormat(encodingString,
// 				     (double) sampleRate,
// 				     bitsPerSample,
// 				     channels,
// 				     AudioFormat.LITTLE_ENDIAN,
// 				     signed ? AudioFormat.SIGNED : AudioFormat.UNSIGNED,
// 				     /*frameSizeInBits=*/blockAlign * 8,
// 				     Format.NOT_SPECIFIED, // No FRAME_RATE specified
// 				     Format.byteArray);



	    // TODO: If possible create WavAudioFormat only when necessary otherwise
	    // create AudioFormat
	    format = new WavAudioFormat(encodingString,
					sampleRate,
					bitsPerSample,
					channels,
					/*frameSizeInBits=*/blockAlign * 8,
					avgBytesPerSec,
					AudioFormat.LITTLE_ENDIAN,
					signed ? AudioFormat.SIGNED : AudioFormat.UNSIGNED,
					Format.NOT_SPECIFIED, // No FRAME_RATE specified
					Format.byteArray,
					codecSpecificHeader);

	    // System.out.println("  AudioFormat " +  format);
	    return format;

	}

	public String toString() {
	    System.out.println("Audio Media: " + format);
	    System.out.println("Number of channels " + channels);
	    System.out.println("average bytes per second " + avgBytesPerSec);
	    System.out.println("sampleRate " + sampleRate);
	    System.out.println("blockAlign " + blockAlign);
	    System.out.println("bitsPerSample " + bitsPerSample);
	    System.out.println("formatTag " + formatTag);
	    return super.toString();
	}
    }

    private class Video extends Media {
	int size;
	int width;
	int height;
	int planes;
	int depth;
	String compressor;
	VideoFormat format = null;
	BitMapInfo bitMapInfo = null;


	Format createFormat() {
	    if (format != null)
		return format;
	    //System.err.println("BMI = " + bitMapInfo);
	    if (usecPerFrame != 0) {
		format = bitMapInfo.createVideoFormat(Format.byteArray,
						      (float) ((1.0/usecPerFrame)*1000000));
	    } else {
		format = bitMapInfo.createVideoFormat(Format.byteArray);
	    }
		    
	    // System.out.println("  VideoFormat " +  format);
	    return format;
	}

	public String toString() {
	    System.out.println("size is " + size);
	    System.out.println("width is " + width);
	    System.out.println("height is " + height);
	    System.out.println("planes is " + planes);
	    System.out.println("depth is " + depth);
	    System.out.println("compressor is " + compressor);
	    
	    return super.toString();
	}

    }


    private class TrakList {
	Time duration = Duration.DURATION_UNKNOWN; // TODO: NEED THIS?
	
	String trackType;                 // Chunk identifier
	String streamHandler;              // Device handler identifier
	int flags;                      // Data Parameters
	int priority;                   // Set to 0
	int initialFrames;              // Number of initial audio frames
	int scale;                      // Unit used to measure time
	int rate;                       // Data rate of playback
	int start;                      // Starting time of AVI data
	int length;                     // Size of AVI data chunk
	int suggestedBufferSize;        // Minimum playback buffer size
	int quality;                    // Sample quality factor
	int sampleSize;                 // Size of the sample in bytes

	Media media; // Info specific to each track type

	// From the implementation
	// Can be used as a debugging aid to disable a track
	boolean supported = true; // Is this track type supported

	AVIIndexEntry[] chunkInfo = new AVIIndexEntry[0];
	int maxChunkIndex = 0;

	int[] indexToKeyframeIndex = new int[0];
	int[] keyFrames = new int[0];
    
	// boolean allKeyFrames = false;
	int numKeyFrames = 0;
	int tmpCumulativeChunkLength = 0;


	// TODO: speedup: use binary search
	int getChunkNumber(int offset) {
	    for (int i = 0; i < maxChunkIndex; i++) {
		if (offset < chunkInfo[i].cumulativeChunkLength) {
		    return i;
		}
	    }
	    return maxChunkIndex; // EOM
	}
    }

    // TODO extend BasicTrack if possible
    private abstract class MediaTrack implements Track {
	protected TrakList trakInfo;
	private boolean enabled = true;
	private int numBuffers = 4; // TODO: check
	private Format format;
	private long sequenceNumber = 0;
	private int chunkNumber = 0;
	protected int useChunkNumber = 0;
	protected int offsetWithinChunk = -1;
	protected int useOffsetWithinChunk = 0;
	private AviParser parser = AviParser.this;
	private AVIIndexEntry indexEntry;
	private Object header = null;
	private TrackListener listener;

	MediaTrack(TrakList trakInfo) {
	    this.trakInfo = trakInfo;
	    format = trakInfo.media.createFormat();
	}


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
	
 	synchronized void setChunkNumberAndOffset(int number, int offset) {
 	    chunkNumber = number;
	    offsetWithinChunk = offset;
 	}


	public void readFrame(Buffer buffer) {
	    if (buffer == null)
		return;
	    
	    if (!enabled) {
		buffer.setDiscard(true);
		return;
	    }
	    
	    synchronized (this) {
		if (offsetWithinChunk == -1) {
		    useOffsetWithinChunk = 0;
		} else {
		    useOffsetWithinChunk = offsetWithinChunk;
		    offsetWithinChunk = -1; // Reset offsetWithinChunk
		}
		useChunkNumber = chunkNumber;
	    }

	    // TODO: handle chunkNumber < 0 case differently
 	    if ( (useChunkNumber >= trakInfo.maxChunkIndex) ||
 		 (useChunkNumber < 0 ) ) {
		buffer.setLength(0);
		buffer.setEOM(true);
 		return;
 	    }
	    buffer.setFormat(format); // Need to do this every time ???

	    indexEntry = trakInfo.chunkInfo[useChunkNumber];


	    int chunkLength = indexEntry.chunkLength;

	    Object obj = buffer.getData();
	    byte[] data;

	    buffer.setHeader(new Integer(indexEntry.flag));

	    if  ( (obj == null) ||
		  (! (obj instanceof byte[]) ) ||
		  ( ((byte[])obj).length < chunkLength) ) {
		data = new byte[chunkLength];
		buffer.setData(data);
	    } else {
		data = (byte[]) obj;
	    }

	    try {
		int actualBytesRead;
		synchronized (seekSync) {
		    seekableStream.seek(indexEntry.chunkOffset
					+ moviOffset +
					useOffsetWithinChunk);
		    actualBytesRead = parser.readBytes(stream, data,
                                   chunkLength - useOffsetWithinChunk);
		    offsetWithinChunk = 0;
		    buffer.setTimeStamp(getTimeStamp());
		}
		buffer.setLength(actualBytesRead);
		long frameDuration = Buffer.TIME_UNKNOWN;
		if (trakInfo.trackType.equals(VIDEO)) {
		    if (nanoSecPerFrame > 0)
			frameDuration = nanoSecPerFrame;
		    if (
			// All Frames are key frames
			(trakInfo.indexToKeyframeIndex.length == 0) ||
			// or current frame is a key frame
			 (useChunkNumber == trakInfo.indexToKeyframeIndex[useChunkNumber])
			) {
			buffer.setFlags(buffer.getFlags() | Buffer.FLAG_KEY_FRAME);
			// System.out.println("avi: chunk " + useChunkNumber + "is a keyframe");
		    }
		}
		buffer.setDuration(frameDuration);
		// TODO: need setSequenceNumber and getSequenceNumber in Buffer
		buffer.setSequenceNumber(++sequenceNumber);
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
		    chunkNumber++;
	    }
	    // If necessary call doReadFrame(buffer) here
	}
	
	abstract void doReadFrame(Buffer buffer);

	public int mapTimeToFrame(Time t) {
	    return FRAME_UNKNOWN;
	}
	
	public Time mapFrameToTime(int frameNumber) {
	    return TIME_UNKNOWN;
	}
	
	abstract long getTimeStamp();
    }

    private class AudioTrack extends MediaTrack  {
	int channels;
	int avgBytesPerSec;
	AVIIndexEntry[] chunkInfo;

	AudioTrack(TrakList trakInfo) {
	    super(trakInfo);
	    channels =  ((Audio)trakInfo.media).channels;
	    avgBytesPerSec = ((Audio) trakInfo.media).avgBytesPerSec;
	    chunkInfo = trakInfo.chunkInfo;
	}
	
	void doReadFrame(Buffer buffer) {
	}

	long getTimeStamp() {
	    if (avgBytesPerSec > 0) {
		long bytes = useOffsetWithinChunk;
		if (useChunkNumber > 0) {
		    bytes += chunkInfo[useChunkNumber - 1].cumulativeChunkLength;
		}
// 		System.out.println("Audio: getTimeStamp: " +
// 				   ((float) bytes) / avgBytesPerSec);
		return (long) (((float) bytes / avgBytesPerSec) * 1E9);
	    } else {
		return 0;
	    }
	}
    }

    private class VideoTrack extends MediaTrack  {
	int needBufferSize;
	boolean variableSampleSize = true;

	VideoTrack(TrakList trakInfo) {
	    super(trakInfo);
	}
	
	void doReadFrame(Buffer buffer) {
	}

	long getTimeStamp() {
// 	    System.out.println("Video: getTimeStamp: " + 
// 			       (useChunkNumber * usecPerFrame * 1000L));
	    return ((long)useChunkNumber * (long)usecPerFrame * 1000L);
	}

	public int mapTimeToFrame(Time t) {
	    if (nanoSecPerFrame <= 0)
		return FRAME_UNKNOWN;

	    if (t.getNanoseconds() < 0)
		return FRAME_UNKNOWN;

	    int chunkNumber;
	    chunkNumber = (int) (t.getNanoseconds() / nanoSecPerFrame);

	    if (chunkNumber >= trakInfo.maxChunkIndex)
		return trakInfo.maxChunkIndex - 1;

// 	    System.out.println("mapTimeToFrame: " + t.getSeconds() +
// 			       " ==> " + chunkNumber);

//  	    { // DEBUG BLOCK REMOVE
//  		mapFrameToTime(chunkNumber);
//  	    }
	    return chunkNumber;
	}

	public Time mapFrameToTime(int frameNumber) {
	    if ( (frameNumber < 0) || (frameNumber >= trakInfo.maxChunkIndex) )
		return TIME_UNKNOWN;

	    long time = frameNumber * nanoSecPerFrame;

//  	    System.out.println("mapFrameToTime: " + frameNumber +
//  			       " ==> " + (time/1000000000.0));
	    
	    return new Time(time);
	}
    }

    // An Index Chunk has the identifier idx1 and must appear after hdrl and
    // movi chunks. This chunk contains a list of all chunks within the AVI
    // chunk, along with their locations, and is used for random access of audio
    // and video data.
    //
    private class AVIIndexEntry {
	public String id;                       // Chunk identifier reference
	public int flag;                       // Type of Chunk referenced
	public int chunkOffset;                // Position of Chunk in file
	public int chunkLength;                // Length of chunk in bytes
	// Currently only audio track uses cumulativeChunkLength
	public int cumulativeChunkLength = 0;       // Derived data
    }

}
