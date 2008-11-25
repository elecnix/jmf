/*
 * @(#)QuicktimeMux.java	1.28 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.multiplexer.video;

import javax.media.Time;
import javax.media.Duration;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.PlugIn;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.SourceTransferHandler;
import com.sun.media.format.WavAudioFormat;
import com.sun.media.format.AviVideoFormat;
import javax.media.format.UnsupportedFormatException;
import java.io.IOException;
import javax.media.Control;
import javax.media.IncompatibleSourceException;
import javax.media.format.AudioFormat;
import javax.media.format.*;
import com.sun.media.util.ByteBuffer;
import com.sun.media.datasink.RandomAccess;
import java.util.Vector;
import java.util.Hashtable;
import java.awt.Dimension;

public class QuicktimeMux extends com.sun.media.multiplexer.BasicMux {

    private int removeCount = 0; //$$ TODO: REMOVE
    // TODO: remove unused variables $$$$
    private boolean sourceConnected = false;
    private boolean sinkConnected = false;
    private boolean closed = false;
    private boolean opened = false;
    private int debugCounter = 0;
    private Hashtable streamNumberHash;
    private TrakInfo[] trakInfoArray;
    private int  dataSize = 0;
    private Format [] rgbFormats;
    private Format [] yuvFormats;

//     private int[] suggestedBufferSizes;
//     private int[] suggestedBufferSizeOffsets;
    private int[] scaleOffsets;
    private boolean[] endOfMediaStatus;
    private int numberOfEoms = 0;
    private int numberOfTracks = 0;
    private int numberOfSupportedTracks = 0;

    // NOTE: Currently, only audio and video tracks are supported
    private final static String VIDEO = "vide";
    private final static String AUDIO = "soun";

    private long mdatOffset;
    private long moovOffset;
    private int mdatLength;
    private int moovLength;
    private long mvhdDurationOffset;
    private static Hashtable audioFourccMapper = new Hashtable();
    private static Hashtable videoFourccMapper = new Hashtable();

    private final int movieTimeScale = 60000;
    private final int DEFAULT_FRAME_RATE = 15;
    private final int DEFAULT_FRAME_DURATION = (int) ((1. / DEFAULT_FRAME_RATE) * movieTimeScale);

    private final int TRAK_ENABLED = 1;
    private final int TRAK_IN_MOVIE = 2;
//     private final int TRAK_IN_PREVIEW = 4;
//     private final int TRAK_IN_POSTER = 4;

    private final static int DATA_SELF_REFERENCE_FLAG = 0x1;

    private final static boolean ALWAYS_USE_ONE_ENTRY_FOR_STTS = false;
    private final static int EPSILON_DURATION = 1000000; // In Nanoseconds [1 msec]

    private final static int MVHD_ATOM_SIZE = 100;
    private final static int TKHD_ATOM_SIZE = 84;
    private final static int MDHD_ATOM_SIZE = 24;

    // Two passes are required to create a streamable quicktime file.
    private boolean requireTwoPass = true;


    static {
	// Note AudioFormat.LINEAR is a special case because, the
	// fourcc code could be 'twos' or 'raw '
	audioFourccMapper.put(AudioFormat.ALAW, AudioFormat.ALAW);
	//	audioFourccMapper.put(AudioFormat.ULAW, AudioFormat.ULAW);
	// audioFourccMapper.put(AudioFormat.ULAW, "ulaw");
	// For some reason, in AudioFormat, the ULAW string is in all
	// upper case. Need to investigate
	audioFourccMapper.put("ulaw", "ulaw");

	audioFourccMapper.put(AudioFormat.IMA4, AudioFormat.IMA4);
	audioFourccMapper.put(AudioFormat.GSM, "agsm");
	audioFourccMapper.put(AudioFormat.MAC3, AudioFormat.MAC3);
	audioFourccMapper.put(AudioFormat.MAC6, AudioFormat.MAC6);

	videoFourccMapper.put(VideoFormat.RGB, VideoFormat.RGB);
	videoFourccMapper.put(VideoFormat.CINEPAK, VideoFormat.CINEPAK);
	videoFourccMapper.put(VideoFormat.JPEG, VideoFormat.JPEG);
	videoFourccMapper.put(VideoFormat.H261, VideoFormat.H261);
	videoFourccMapper.put(VideoFormat.H263, VideoFormat.H263);
	videoFourccMapper.put(VideoFormat.INDEO32, VideoFormat.INDEO32);
	videoFourccMapper.put(VideoFormat.INDEO41, VideoFormat.INDEO41);
	videoFourccMapper.put(VideoFormat.INDEO50, VideoFormat.INDEO50);
	videoFourccMapper.put(VideoFormat.MJPG, VideoFormat.MJPG);
	videoFourccMapper.put(VideoFormat.MJPEGA, VideoFormat.MJPEGA);
	videoFourccMapper.put(VideoFormat.MJPEGB, VideoFormat. MJPEGB);
	videoFourccMapper.put(VideoFormat.MPEG, VideoFormat. MPEG);
	videoFourccMapper.put(VideoFormat.RPZA, VideoFormat.RPZA); // Apple Video
	videoFourccMapper.put(VideoFormat.YUV, "yuv2"); // Component Video
	// TODO: ADD DV - NTSC
	// TODO: Add Animation ['rle '], BMP, Graphics ['smc '], Sorenson, TGA, TIFF, PNG
	// TODO: Add Kodak Photo CD ['kpcd']
    }


    public QuicktimeMux() {
	supportedInputs = new Format[2];
	supportedInputs[0] = new AudioFormat(null);
	supportedInputs[1] = new VideoFormat(null);

	supportedOutputs = new ContentDescriptor[1];
	supportedOutputs[0] = new FileTypeDescriptor(FileTypeDescriptor.QUICKTIME);
	int NS = Format.NOT_SPECIFIED;
	rgbFormats = new Format[] {
	    new RGBFormat(null, NS, Format.byteArray, NS, 16,
			  0x1F << 10, 0x1F << 5, 0x1F,
			  2, NS, RGBFormat.FALSE, RGBFormat.BIG_ENDIAN),
	    new RGBFormat(null, NS, Format.byteArray, NS, 24,
			  1, 2, 3, 3, NS, RGBFormat.FALSE, NS),
	    new RGBFormat(null, NS, Format.byteArray, NS, 32,
			  2, 3, 4, 4, NS, RGBFormat.FALSE, NS)
	};
	yuvFormats = new Format[] {
	    new YUVFormat(null, NS, Format.byteArray, NS,
			  YUVFormat.YUV_YUYV | YUVFormat.YUV_SIGNED,
			  NS, NS, 0, 1, 3)
	};
	    
    }

    public String getName() {
	return "Quicktime Multiplexer";
    }


    // Big Endian Format for intersecting with the input format.
    Format bigEndian = new AudioFormat(null,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.BIG_ENDIAN,
			AudioFormat.NOT_SPECIFIED);

    public Format setInputFormat(Format input, int trackID) {
	if (trakInfoArray == null) {
	    trakInfoArray = new TrakInfo[numTracks];
	    endOfMediaStatus = new boolean[numTracks];
	}
	if (! (input instanceof VideoFormat ||
	       input instanceof AudioFormat) ) {
	    trakInfoArray[trackID] = new TrakInfo();
	    trakInfoArray[trackID].format = input;
	    trakInfoArray[trackID].supported = false;
	    return input;
	}

	String encoding = input.getEncoding();
	if (input instanceof VideoFormat) {
	    if (videoFourccMapper.get(encoding.toLowerCase()) == null) {
		return null;
	    }
	    if ( encoding.equalsIgnoreCase(VideoFormat.RGB) ) {
		if (matches(input, rgbFormats) == null)
		    return null;
	    }
	    if ( encoding.equalsIgnoreCase(VideoFormat.YUV) ) {
		if (matches(input, yuvFormats) == null)
		    return null;
	    }
	    VideoTrakInfo vti = new VideoTrakInfo();
	    trakInfoArray[trackID] = vti;
	    vti.supported = true;
	    vti.type = VIDEO;
	    vti.encoding = encoding;
	    vti.format = input;
	    //   vti.videoFormat = (VideoFormat) input;
	    vti.videoFormat = (VideoFormat) null; // filled in later in doProcess
// 	    float frameRate = ((VideoFormat)input).getFrameRate();
// 	    vti.frameRate = frameRate;

// 	    if (frameRate  > 0) { // that is it is not 0 or NOT_SPECIFIED
// 		vti.frameDuration = (int) ((1 / frameRate) * movieTimeScale);
// 	    } else {
// 		vti.frameDuration = DEFAULT_FRAME_DURATION;
// 	    }
	    // System.out.println("frameDuration is " + vti.frameDuration);
	} else if (input instanceof AudioFormat) {

	    if (encoding.equalsIgnoreCase(AudioFormat.LINEAR)) {
		AudioFormat af = (AudioFormat) input;
		if (af.getSampleSizeInBits() > 8) {
		    if (af.getSigned() == AudioFormat.UNSIGNED)
			return null;
		    if (af.getEndian() == AudioFormat.LITTLE_ENDIAN)
			return null;
		    else if (af.getEndian() == AudioFormat.NOT_SPECIFIED)
			input = af.intersects(bigEndian);
		}
	    } else {
		if (audioFourccMapper.get(encoding.toLowerCase()) == null)
		    return null;
	    }

	    AudioTrakInfo ati = new AudioTrakInfo();
	    trakInfoArray[trackID] = ati;
	    ati.supported = true;
	    ati.type = AUDIO;
	    ati.encoding = encoding;
	    ati.format = input;
	    ati.audioFormat = (AudioFormat) input;
	    
	    ati.frameSizeInBytes = ati.audioFormat.getFrameSizeInBits()/8;
	    if (ati.frameSizeInBytes <= 0)
		ati.frameSizeInBytes = ati.audioFormat.getSampleSizeInBits() *
		    ati.audioFormat.getChannels() / 8;
	    
	    //System.out.println("frameSizeInBytes is " + ati.frameSizeInBytes + " " + ati.audioFormat.getFrameSizeInBits());
	    if (encoding.equalsIgnoreCase(AudioFormat.IMA4)) {
		ati.samplesPerBlock = ati.IMA4_SAMPLES_PER_BLOCK;
	    } else if (encoding.equalsIgnoreCase(AudioFormat.GSM)) {
		ati.samplesPerBlock = ati.GSM_SAMPLES_PER_BLOCK;
	    } else if (encoding.equalsIgnoreCase(AudioFormat.MAC3)) {
		ati.samplesPerBlock = ati.MAC3_SAMPLES_PER_BLOCK;
	    } else if (encoding.equalsIgnoreCase(AudioFormat.MAC6)) {
		ati.samplesPerBlock = ati.MAC6_SAMPLES_PER_BLOCK;
	    }
	}

	//streamNumberHash = new Hashtable(numStreams);
// 	suggestedBufferSizes = new int[numStreams];
// 	suggestedBufferSizeOffsets = new int[numStreams];
	//endOfMediaStatus = new boolean[num]; // java initializes them to false, right ??
	if (trakInfoArray[trackID].supported) {
	    numberOfSupportedTracks++;
	    // 		suggestedBufferSizes[i] = -1;  // TODO: need this??? what is this
	    // 		suggestedBufferSizeOffsets[i] = -1; // TODO: need this??? what is this
	}

	inputs[trackID] = input;
	return input;
    }

    public synchronized int doProcess(Buffer buffer, int trackID) {
	byte [] data;
	int length;
	
	if (buffer.isEOM()) {
	    if (!endOfMediaStatus[trackID]) {
		endOfMediaStatus[trackID] = true;
		numberOfEoms++;
		if (numberOfEoms == numTracks)
		    return super.doProcess(buffer, trackID);
		else
		    return BUFFER_PROCESSED_OK;
	    }
	}

	if (!trakInfoArray[trackID].initFormat) {
	    if ( trakInfoArray[trackID] instanceof VideoTrakInfo ) {
		VideoTrakInfo vti = (VideoTrakInfo)trakInfoArray[trackID];
		 vti.videoFormat = (VideoFormat) buffer.getFormat();
		 vti.frameRate = vti.videoFormat.getFrameRate();

		 if (vti.frameRate  > 0) { // that is it is not 0 or NOT_SPECIFIED
		     vti.frameDuration = 
                          (int) ( ((1 / vti.frameRate) * movieTimeScale) + 0.5);
		 } else {
		     vti.frameRate = DEFAULT_FRAME_RATE;
		     vti.frameDuration = DEFAULT_FRAME_DURATION;
		 }
	    }
	    trakInfoArray[trackID].initFormat = true;
	}


	Object obj = buffer.getData();
	if (obj == null)
	    return BUFFER_PROCESSED_FAILED;
	data = (byte[]) obj;
	if (data == null)
	    return BUFFER_PROCESSED_FAILED;
	length = buffer.getLength();

	dataSize += length;

	TrakInfo trakInfo = trakInfoArray[trackID];
	write(data, 0, length);
	int chunkOffset = (int) (filePointer - length);

	// For all tracks
	int chunkOffsetsIndex = trakInfo.chunkOffsetsIndex++;
	int numChunkOffsetsArraysUsed = trakInfo.numChunkOffsetsArraysUsed;
	trakInfo.chunkOffsetsArray[numChunkOffsetsArraysUsed-1][chunkOffsetsIndex] =
	    chunkOffset;
	if (++chunkOffsetsIndex >= trakInfo.MAX_CHUNKOFFSETS_ARRAYSIZE) {
	    trakInfo.chunkOffsetsIndex = 0;
	    trakInfo.chunkOffsetsArray[numChunkOffsetsArraysUsed] =
		new int[trakInfo.MAX_CHUNKOFFSETS_ARRAYSIZE];
	    trakInfo.numChunkOffsetsArraysUsed++;
	    if (++numChunkOffsetsArraysUsed >= trakInfo.MAX_CHUNKOFFSETS_NUMARRAYS) {
		System.err.println("Cannot create quicktime file with more than " +
				   (trakInfo.MAX_CHUNKOFFSETS_NUMARRAYS *
				    trakInfo.MAX_CHUNKOFFSETS_ARRAYSIZE) + " chunks ");
		return BUFFER_PROCESSED_FAILED;
	    }
	}

	String type = trakInfo.type;
	VideoTrakInfo vti = null;
	AudioTrakInfo ati = null;

	if (type.equals(VIDEO)) {
	    vti = (VideoTrakInfo) trakInfo;

	    /// For STSZ (sample size atom): START
	    int sampleSizeIndex = vti.sampleSizeIndex++;
	    int numSampleSizeArraysUsed = vti.numSampleSizeArraysUsed;
	    vti.sampleSize[numSampleSizeArraysUsed-1][sampleSizeIndex] = length;
	    if ( vti.constantSampleSize && (length != vti.sampleSize[0][0]) ) {
		vti.constantSampleSize = false;
	    }

	    // store durations based on (timestamp - previous timestamp).
	    // The timeStamps will be used later to create the stts chunk
	    // for the videotrack.
	    if (vti.minDuration >= 0 ) { // $$ should this be > 0?
		long timeStamp = buffer.getTimeStamp();
		if (timeStamp <= Buffer.TIME_UNKNOWN) { // $$ should this be <= 0?
		    // If you get any timestamp with TIME_UNKNOWN (-1) then
		    // don't bother storing any more time stamps in the timestamps array.
		    // In this case we will have a stts chunk with 1 entry which will
		    // be the frame duration obtained from the frame rate
		    // System.out.println("Got neg timestamp. stop collecting timestamps");
		    vti.minDuration = -1;
		} else if (vti.totalFrames > 0) {
		    long durationOfBufferData = (timeStamp - vti.previousTimeStamp);
		    if (durationOfBufferData < vti.minDuration)
			vti.minDuration = durationOfBufferData;
		    else if (durationOfBufferData > vti.maxDuration)
			vti.maxDuration = durationOfBufferData;
		    
		    int timeStampIndex = vti.timeStampIndex++;
		    int numTimeStampArraysUsed = vti.numTimeStampArraysUsed;
		    
// 		    System.out.println((numTimeStampArraysUsed-1) + " : " +
// 				       timeStampIndex + " : " + durationOfBufferData);
		    
		    vti.timeStamps[numTimeStampArraysUsed-1][timeStampIndex] =
			durationOfBufferData;
		    
		    if (++timeStampIndex >= vti.MAX_TIMESTAMP_ARRAYSIZE) {
			vti.timeStampIndex = 0;
			// System.out.println("Creating new timeStamps array");
			vti.timeStamps[numTimeStampArraysUsed] =
			    new long[vti.MAX_TIMESTAMP_ARRAYSIZE];
			vti.numTimeStampArraysUsed++;
			if (++numTimeStampArraysUsed >= vti.MAX_TIMESTAMP_NUMARRAYS) {
			    System.err.println("Cannot create quicktime file with more than " +
					       (vti.MAX_TIMESTAMP_NUMARRAYS *
						vti.MAX_TIMESTAMP_ARRAYSIZE) + " frames ");
			    return BUFFER_PROCESSED_FAILED;
			}
		    }
		}
		vti.previousTimeStamp = timeStamp;
	    }
		

	    if (++sampleSizeIndex >= vti.MAX_SAMPLE_SIZE_ARRAYSIZE) {
		vti.sampleSizeIndex = 0;
		vti.sampleSize[numSampleSizeArraysUsed] =
		    new int[vti.MAX_SAMPLE_SIZE_ARRAYSIZE];
		vti.numSampleSizeArraysUsed++;
		if (++numSampleSizeArraysUsed >= vti.MAX_SAMPLE_SIZE_NUMARRAYS) {
		    System.err.println("Cannot create quicktime file with more than " +
				       (vti.MAX_SAMPLE_SIZE_NUMARRAYS *
					vti.MAX_SAMPLE_SIZE_ARRAYSIZE) + " samples ");
		    return BUFFER_PROCESSED_FAILED;
		}
	    }
	    /// For STSZ (sample size atom): END
	    
	    /// For STSS (sync sample atom): START
	    boolean keyframe = ((buffer.getFlags() & Buffer.FLAG_KEY_FRAME) > 0);
	    if (keyframe) {
		int keyFrameIndex = vti.keyFrameIndex++;
		
		int numKeyFrameArraysUsed = vti.numKeyFrameArraysUsed;
		vti.keyFrames[numKeyFrameArraysUsed-1][keyFrameIndex] =
		    vti.totalFrames+1; // Frame Numbering starts from 1, not 0
		
		
		if (++keyFrameIndex >= vti.MAX_KEYFRAME_ARRAYSIZE) {
		    vti.keyFrameIndex = 0;
// 				System.out.println("Create new keyframe array for index " +
// 						   numKeyFrameArraysUsed);
		    vti.keyFrames[numKeyFrameArraysUsed] =
			new int[vti.MAX_KEYFRAME_ARRAYSIZE];
		    vti.numKeyFrameArraysUsed++;
		    if (++numKeyFrameArraysUsed >= vti.MAX_KEYFRAME_NUMARRAYS) {
			System.err.println("Cannot create quicktime file with more than " +
					   (vti.MAX_KEYFRAME_NUMARRAYS *
					    vti.MAX_KEYFRAME_ARRAYSIZE) + " keyframes ");
			//close();
			return BUFFER_PROCESSED_FAILED;
		    }
		}
	    }
	    /// For STSS (sync sample atom): END
	} else {
	    // TODO: for now else means audio
	    // But this won't be the case when we support
	    // tracks like MUSI (ie midi) etc.
	    ati = (AudioTrakInfo) trakInfo;
	    // System.out.println("audio: chunk length is " + length);
	    /// For STSC (sample per chunk): START
	    int samplesPerChunk = (length / ati.frameSizeInBytes) * ati.samplesPerBlock;
	    // System.out.println("samplesPerChunk is " + samplesPerChunk);
	    
	    ati.numSamples += samplesPerChunk;
	    
	    if (ati.previousSamplesPerChunk != samplesPerChunk) {
		// Another samplesPerChunk entry
		int samplesPerChunkIndex = ati.samplesPerChunkIndex;
		int numSamplesPerChunkArraysUsed = ati.numSamplesPerChunkArraysUsed;
		
		ati.samplesPerChunkArray[
					 numSamplesPerChunkArraysUsed-1
		][samplesPerChunkIndex] =
		    trakInfo.totalFrames + 1; // Chunk numbers start from 1
		samplesPerChunkIndex++;
		ati.samplesPerChunkArray[
					 numSamplesPerChunkArraysUsed-1
		][samplesPerChunkIndex] =
		    samplesPerChunk;
		samplesPerChunkIndex++;
		ati.samplesPerChunkIndex = samplesPerChunkIndex;
		ati.previousSamplesPerChunk = samplesPerChunk;
		
		
		if (++samplesPerChunkIndex >= ati.MAX_SAMPLESPERCHUNK_ARRAYSIZE) {
		    ati.samplesPerChunkIndex = 0;
		    ati.samplesPerChunkArray[numSamplesPerChunkArraysUsed] =
			new int[ati.MAX_SAMPLESPERCHUNK_ARRAYSIZE];
		    ati.numSamplesPerChunkArraysUsed++;
		    if (++numSamplesPerChunkArraysUsed >= ati.MAX_SAMPLESPERCHUNK_NUMARRAYS) {
			System.err.println("Cannot create quicktime file with more than " +
					   (ati.MAX_SAMPLESPERCHUNK_NUMARRAYS *
					    ati.MAX_SAMPLESPERCHUNK_ARRAYSIZE) + " chunks ");
			//close();
			return BUFFER_PROCESSED_FAILED;
		    }
		}
	    }
	    /// For STSC (sample per chunk): END
	}
	
	
	
	// 		    if (length > suggestedBufferSizes[streamNumber])
	// 			suggestedBufferSizes[streamNumber] = length;
	// 		    //}
	
	
	trakInfo.totalFrames++;
	//  		    if (isVideoFormat)
	//  			totalVideoFrames++;
	
	// 		    totalFrames++;
	// 		    // Handle this case properly. Internal error?
	// 		    if ( totalFrames >= MAX_FRAMES_STORED ) {
	// 			System.err.println("Cannot store more than " + MAX_FRAMES_STORED +
	// 					   " frames");
	// 			close();
	// 			return;
	// 		    }
	// 		    // System.out.println("Frame " + totalFrames);
	return BUFFER_PROCESSED_OK;	
    }
	
    protected void writeHeader() {
	mdatOffset = filePointer;
	bufClear();
	bufWriteInt(0);
	bufWriteBytes("mdat");
	bufFlush();
	dataSize = 0;
    }

    public boolean requireTwoPass() {
	return requireTwoPass;
    }

    protected void writeFooter() {
	moovOffset = filePointer;
	seek((int)mdatOffset);
	bufClear();
	mdatLength = 8 + dataSize;
	bufWriteInt(mdatLength);
	bufFlush();
	seek((int)moovOffset);
	writeMOOV();
	int maxTrackDuration = -1;
	
	for (int i = 0; i < numTracks; i++) {
	    if (!trakInfoArray[i].supported)
		continue;
	    //  		    System.out.println("Duration of track 1 " +
	    //  				       trakInfoArray[i].duration);

	    writeSize(trakInfoArray[i].tkhdDurationOffset, trakInfoArray[i].duration);

	    if (trakInfoArray[i].type.equals(VIDEO)) {
		writeSize(trakInfoArray[i].mdhdDurationOffset, trakInfoArray[i].duration);
	    }

	    if (trakInfoArray[i].duration > maxTrackDuration) {
		maxTrackDuration = trakInfoArray[i].duration;
	    }
	}
	// update duartion at mvhdDurationOffset
	writeSize(mvhdDurationOffset, maxTrackDuration);
	
	RandomAccess st;
	if (requireTwoPass &&  (sth != null) &&
	    (sth instanceof RandomAccess) ) {
	    // See if there is space to write the streamable quicktime file.
	    if ( (st = (RandomAccess) sth).write(-1, moovLength + mdatLength)) {
		updateSTCO();
		// At this point the non-streamable file cannot be played as the
		// updateSTCO updates the offsets so as to work with the streamable
		// file. But if the 2 st.write calls don't succeed then you will
		// have neither streamable nor non-streamable file.
		// But this shouldn't happen due to lack of disk space because
		// the 'if' statement succeeds only if a dummy file of the
		// required size could be created.
		
		write(null, 0, -1); // EOS. I don't think this is necessary as it is nop $$$
		// TODO: $$$ If these 2 writes don't succeed, then throw appropriate
		// Exception. "Error creating Quicktime file.
		st.write(moovOffset, moovLength);
		st.write(mdatOffset, mdatLength);
		// Done writing moov and mdat chunks to streamable file.
	    } else {
		System.err.println("No space to write streamable file");
	    }
	    st.write(-1, -1);
	}

    }

    private int writeMOOV() {
	long offset = filePointer;
	bufClear();
	bufWriteInt(0); // This is filled in later
	bufWriteBytes("moov");
	bufFlush();
	
	int size = 8;
	size += writeMVHD();

	for (int i = 0; i < numTracks; i++) {
	    if (!trakInfoArray[i].supported)
		continue;
	    size += writeTRAK(i, trakInfoArray[i].type);
	}
	moovLength = size;
	return writeSize(offset, size);
    }


    private int writeMVHD() {
	bufClear();
	bufWriteInt(MVHD_ATOM_SIZE + 8);
	bufWriteBytes("mvhd");
	bufWriteInt(0); // Skip version(1), flags(3)
	bufWriteInt(0); // create time
	bufWriteInt(0); // mod time
	bufWriteInt(movieTimeScale); // time scale
	mvhdDurationOffset = filePointer;
	bufWriteInt(0); // duration. Will be filled at the end
	bufWriteInt(65536); // preferredRate
	bufWriteShort((short)0xff); // preferredVolume
	
	// 10 reserved bytes
	bufWriteInt(0); // reserved
	bufWriteInt(0); // reserved
	bufWriteShort((short)0); // reserved
	/////////////////////////
	bufFlush();
	writeMatrix();
	bufClear();
	bufWriteInt(0); // previewTime
	bufWriteInt(0); // previewDuration
	bufWriteInt(0); // posterTime
	bufWriteInt(0); // selectionTime
	bufWriteInt(0); // selectionDuration
	bufWriteInt(0); // currentTime
	bufWriteInt(numberOfSupportedTracks + 1); // nextTrackID
	bufFlush();
	return MVHD_ATOM_SIZE + 8;
    }

    // TODO: Choose a good name
    private int writeSize(long offset, int size) {
	long currentOffset = filePointer;
	seek((int)offset);
	bufClear();
	bufWriteInt(size);
	bufFlush();
	seek((int)currentOffset);
	return size;
    }

    /**
     * Required atoms are tkhd and mdia
     */
    private int writeTRAK(int streamNumber, String type) {
	long offset = filePointer;
	bufClear();
	bufWriteInt(0);
	bufWriteBytes("trak");
	bufFlush();
	int size = 8;

	size += writeTKHD(streamNumber, type);
	size += writeMDIA(streamNumber, type);
	return writeSize(offset, size);
    }


    private int writeTKHD(int streamNumber, String type) {
	int width = 0;
	int height = 0;
	int duration = 0;
	int volume = 0;
	// For TKHD, the duration for all the tracks is computed
	// based on movieTimeScale
	if (type.equals(VIDEO)) {
	    VideoTrakInfo videoTrakInfo = (VideoTrakInfo) trakInfoArray[streamNumber];
	    Dimension size = null;
	    VideoFormat vf = videoTrakInfo.videoFormat;
	    if (vf != null) {
		size = vf.getSize();
	    }
	    if (size != null) {
		width = size.width;
		height = size.height;
	    } else {
		// TODO: throw internal error
	    }

	    // System.out.println("tkhd: width, height " + width + " : " + height);
	    // videoTrakInfo.duration = videoTrakInfo.frameDuration * videoTrakInfo.totalFrames;
	    duration = videoTrakInfo.duration;
	} else {
	    AudioTrakInfo audioTrakInfo = (AudioTrakInfo) trakInfoArray[streamNumber];

	    float sampleRate = (int) audioTrakInfo.audioFormat.getSampleRate();
	    float epsilon = 0.01F; // TODO: CHECK TO SEE IF THIS EPSILON MAKES SENSE
	    duration = (int) ( (audioTrakInfo.numSamples / sampleRate) * movieTimeScale
			 + epsilon );
	    audioTrakInfo.duration = duration;

//  	    System.out.println("duration of audio track " + (streamNumber+1) + 
//  			       " is " + audioTrakInfo.duration);

	    volume = 255;

	}
	bufClear();
	bufWriteInt(TKHD_ATOM_SIZE + 8);
	bufWriteBytes("tkhd");
	bufWriteInt(TRAK_ENABLED | TRAK_IN_MOVIE); // version + flag [track is enabled]
	bufWriteInt(0); // create time
	bufWriteInt(0); // mod time
	bufWriteInt(streamNumber+1);

	bufWriteInt(0); // reserved
	trakInfoArray[streamNumber].tkhdDurationOffset = filePointer;
	bufWriteInt(duration); // duration. Will be updated later with computed duration
	bufWriteInt(0); // reserved
	bufWriteInt(0); // reserved

	bufWriteShort((short)0); // layer TODO
	bufWriteShort((short)0); // alternate group TODO
	bufWriteShort((short)volume);
	bufWriteShort((short)0); // reserved
	bufFlush();
	writeMatrix();
	bufClear();
	bufWriteInt(width * 65536); // Track Width, fixed point
	bufWriteInt(height * 65536); // Track Height, fixed point
	bufFlush();
	return TKHD_ATOM_SIZE + 8;
    }

    /**
     * Required atoms are mdhd
     */
    private int writeMDIA(int streamNumber, String type) {
	long offset = filePointer;
	bufClear();
	bufWriteInt(0);
	bufWriteBytes("mdia");
	bufFlush();
	int size = 8;
	size += writeMDHD(streamNumber, type);
	size += writeMhlrHdlr(streamNumber, type);
	size += writeMINF(streamNumber, type);
	return writeSize(offset, size);
    }


    private void writeMatrix() {
	// Matrix
	bufClear();
	bufWriteInt(65536);
	bufWriteInt(0);
	bufWriteInt(0);
	bufWriteInt(0);
	bufWriteInt(65536);
	bufWriteInt(0);
	bufWriteInt(0);
	bufWriteInt(0);
	bufWriteInt(1073741824);
	bufFlush();
    }



    private int writeMDHD(int streamNumber, String type) {
	int timeScale = 0;
	int duration = 0;
	if (type.equals(VIDEO)) {
	    // For video, the timescale is typically the movie time
	    // scale. So we can have the same duration value that
	    // we had in TKHD for this track
	    timeScale = movieTimeScale;

	    VideoTrakInfo videoTrakInfo = (VideoTrakInfo) trakInfoArray[streamNumber];
	    duration = videoTrakInfo.duration; // Computed by writeTKHD earlier
// 	    System.out.println("duration of track " + (streamNumber+1) +
// 			       " is " + videoTrakInfo.duration);
	} else {
	    // For audio, the timeScale is typically the sampleRate
	    // audio duration should be computed based on this timeScale
	    // Due to this different timeScale, the duration recorded in
	    // MDHD will have a different value from that in the 
	    // corresponding TKHD
	    
	    AudioTrakInfo audioTrakInfo = (AudioTrakInfo) trakInfoArray[streamNumber];
	    timeScale = (int) audioTrakInfo.audioFormat.getSampleRate();
	    duration = audioTrakInfo.numSamples; //? TODO CHECK
	}

	bufClear();
	bufWriteInt(MDHD_ATOM_SIZE + 8);
	bufWriteBytes("mdhd");
	bufWriteInt(1); // version + flag [track is enabled]
	bufWriteInt(0); // create time
	bufWriteInt(0); // mod time
	bufWriteInt(timeScale);
	trakInfoArray[streamNumber].mdhdDurationOffset = (int) filePointer;
	bufWriteInt(duration); // duration. Will be updated later for video with computed duration
	bufWriteShort((short)0); // language
	bufWriteShort((short)0); // quality
	bufFlush();
	return MDHD_ATOM_SIZE + 8;
    }

    /**
     * Required atoms: vmhd or smhd and hdlr
     */
    private int writeMINF(int streamNumber, String type) {
	long offset = filePointer;
	bufClear();
	bufWriteInt(0); // Size
	bufWriteBytes("minf");
	bufFlush();
	int size = 8;
	if (type.equals(VIDEO)) {
	    size += writeVMHD(streamNumber, type);
	} else {
	    size += writeSMHD(streamNumber, type);
	}
	size += writeDHlrHdlr(streamNumber, type); ///??? Data Handler
	size += writeDINF(streamNumber, type);
	size += writeSTBL(streamNumber, type);
	return writeSize(offset, size);
    }


    private int writeVMHD(int streamNumber, String type) {
	bufClear();
	bufWriteInt(8 + 12);
	bufWriteBytes("vmhd");
	bufWriteInt(1); // Version + Flags
	bufWriteShort((short)64); // Graphics Mode
	bufWriteShort((short)32768); // Opcolor: Red
	bufWriteShort((short)32768); // Opcolor: Green
	bufWriteShort((short)32768); // Opcolor: Blue
	bufFlush();
	return (8 + 12);
    }

    private int writeSMHD(int streamNumber, String type) {
	bufClear();
	bufWriteInt(8 + 8);
	bufWriteBytes("smhd");
	bufWriteInt(0); // Version + Flags
	bufWriteShort((short)0); // Balance
	bufWriteShort((short)0); // Reserved
	bufFlush();
	return (8 + 8);
    }

    private int  writeMhlrHdlr(int streamNumber, String type) {
	bufClear();
	bufWriteInt(8 + 28);
	bufWriteBytes("hdlr");
	bufWriteInt(0); // Version + Flags
	bufWriteBytes("mhlr");
	bufWriteBytes(type);
	bufWriteBytes("    "); // Component Manufacturer ??
	bufWriteInt(0); // flags ??
	bufWriteInt(0); // flags mask ??
	bufWriteBytes("    "); // Component Name ??
	bufFlush();
	
	return (8 + 28);
	
    }

    private int  writeDHlrHdlr(int streamNumber, String type) {
	bufClear();
	bufWriteInt(8 + 28);
	bufWriteBytes("hdlr");
	bufWriteInt(0); // Version + Flags
	bufWriteBytes("dhlr");
	bufWriteBytes("alis");
	bufWriteBytes("    "); // Component Manufacturer ??
	bufWriteInt(0); // flags ??
	bufWriteInt(0); // flags mask ??
	bufWriteBytes("    "); // Component Name ??
	bufFlush();
	
	return (8 + 28);
    }


    /**
     * Structure of dinf
     *  size: 36  [4]
     *  dinf      [4]
     *    size: 28  [4]
     *    dref      [4]
     *    version+flags [4]
     *    num_entries [4]
     *      size 12 [4]
     *      type alis [4]
     *      version+flags 1 <self_contained>   [4]
     *      data 0 [0]
     */
    private int  writeDINF(int streamNumber, String type) {
	bufClear();
	bufWriteInt(36);
	bufWriteBytes("dinf");
	bufWriteInt(28);
	bufWriteBytes("dref");
	bufWriteInt(0);  // Version + Flags
	bufWriteInt(1);  // Number of entries
	bufWriteInt(12);
	bufWriteBytes("alis");
	bufWriteInt(DATA_SELF_REFERENCE_FLAG);  // Version + Flags
	bufFlush();
	return 36;
    }


    private int  writeSTBL(int streamNumber, String type) {
	long offset = filePointer;
	bufClear();
	bufWriteInt(0);
	bufWriteBytes("stbl");
	bufFlush();
	int size = 8;
	size += writeSTSD(streamNumber, type);
	size += writeSTTS(streamNumber, type);
	size += writeSTSS(streamNumber, type);
	size += writeSTSC(streamNumber, type);
	size += writeSTSZ(streamNumber, type);
	size += writeSTCO(streamNumber, type);
	return writeSize(offset, size);
    }

    private int  writeSTSD(int streamNumber, String type) {
	long offset = filePointer;
	bufClear();
	bufWriteInt(0);
	bufWriteBytes("stsd");
	int size = 8;
	bufWriteInt(0);  // Version + Flags
	bufWriteInt(1);  // Number of entries
	bufFlush();
	size += 8;
	if (type.equals(VIDEO)) {
	    size += writeVideoSampleDescription(streamNumber, type);
	} else {
	    // Will be Audio
	    size += writeAudioSampleDescription(streamNumber, type);
	}
	return writeSize(offset, size);
    }

    private int  writeVideoSampleDescription(int streamNumber, String type) {

	VideoTrakInfo videoTrakInfo = (VideoTrakInfo) trakInfoArray[streamNumber];
	int width = videoTrakInfo.videoFormat.getSize().width;
	int height = videoTrakInfo.videoFormat.getSize().height;

	long offset = filePointer;
	// Sample Description Table
	bufClear();
	bufWriteInt(0); // Size

	int size = 4;
	String encoding = videoTrakInfo.encoding;
	String fourcc = null;
	int bitsPerPixel;
	if (encoding.equalsIgnoreCase(VideoFormat.RGB)) {
	    RGBFormat rgbFormat = (RGBFormat) videoTrakInfo.format;
	    bitsPerPixel = rgbFormat.getBitsPerPixel();
	    fourcc = "raw ";
	} else {
	    fourcc = (String) videoFourccMapper.get(encoding.toLowerCase());
	    // Check: For all non-RGB formats, set bitsPerPixel, ie depth
	    // to 24. The stsd chunk has a depth field that is set to this value
	    bitsPerPixel = 24;
	}
	bufWriteBytes(fourcc);
	size += 4;
	// 6 [4 + 2] reserved bytes
	bufWriteInt(0); // reserved
	bufWriteShort((short)0); // reserved
	size += 6;
	/** Data Reference Index:
	 * A 16-bit integer that contains the index of the data
	 * reference to use to retrieve data associated with samples
	 * that use this sample description. Data references are
	 * stored in data reference atoms.
	 */
	bufWriteShort((short)1); // Data Reference index.
	size += 2;

	// This section takes up 70 bytes
	bufWriteShort((short)0); // Version # of compressed data
	bufWriteShort((short)0); // Revision level
	bufWriteBytes("appl"); // Vendor
	bufWriteInt(1023); // $$ TODO: ASK: Temporal Quality [No info to set this value]
	bufWriteInt(1023); // $$ TODO: ASK Spatial Quality [No info to set this value]
	bufWriteShort((short)width);
	bufWriteShort((short)height);
	// $$ Horizontal Resolution in pixels/inch [No info to set this value]
	bufWriteInt(4718592); // 72 pixels/inch * 65536
	// $$ Vertical Resolution in pixels/inch [No info to set this value]
	bufWriteInt(4718592); // 72 pixels/inch * 65536
	bufWriteInt(0); // Data size [always set to 0]
	// A 16-bit integer that indicates how many frames of compressed data
	// are stored in each sample. Usually set to 1.
	bufWriteShort((short)1);
	// Compressor name takes up 32 bytes, fill the rest with space
	bufWriteBytes(fourcc); // TODO: use full name, e.g Cinepak instead of cvid
	bufWriteBytes("                            "); // 28 spaces
	bufWriteShort((short)bitsPerPixel);
	bufWriteShort((short)-1); // Use Default color table.
	bufFlush();
	size += 70;
	return writeSize(offset, size);
    }

    private int  writeAudioSampleDescription(int streamNumber, String type) {

	AudioTrakInfo audioTrakInfo = (AudioTrakInfo) trakInfoArray[streamNumber];
	AudioFormat audioFormat = audioTrakInfo.audioFormat;
	int channels = audioFormat.getChannels();
	int sampleSizeInBits = audioFormat.getSampleSizeInBits();
	int sampleRate = (int) audioFormat.getSampleRate();

	long offset = filePointer;
	// Sample Description Table
	bufClear();
	bufWriteInt(0); // Size

	int size = 4;
	String encoding = audioTrakInfo.encoding;
	String fourcc;
	if (encoding.equalsIgnoreCase(AudioFormat.LINEAR)) {
	    // for 16 bit fourcc will always be 'twos'
	    // for 8 bit signed fourcc is 'twos'
	    // for 8 bit unsigned fourcc is 'raw '
	    if ( (sampleSizeInBits == 8) &&
		 (audioFormat.getSigned() == AudioFormat.UNSIGNED) ) {
		fourcc = "raw ";
	    } else {
		fourcc = "twos";
	    }
	} else {
	    fourcc = (String) audioFourccMapper.get(encoding.toLowerCase());
	}
	bufWriteBytes(fourcc);
	size += 4;

	// 6 [4 + 2] reserved bytes
	bufWriteInt(0); // reserved
	bufWriteShort((short)0); // reserved
	size += 6;
	/** Data Reference Index:
	 * A 16-bit integer that contains the index of the data
	 * reference to use to retrieve data associated with samples
	 * that use this sample description. Data references are
	 * stored in data reference atoms.
	 */
	bufWriteShort((short)1); // Data Reference index.
	size += 2;

	// This section takes up 20 bytes
	bufWriteShort((short)0); // Version: Must be set to 0
	bufWriteShort((short)0); // Revision Level: Must be set to 0
	bufWriteInt(0); // Vendor: Must be set to 0
	bufWriteShort((short)channels);
	bufWriteShort((short)sampleSizeInBits);
	bufWriteShort((short)0); // Compression ID: Must be set to 0
	bufWriteShort((short)0); // Packet Size: Must be set to 0
	bufWriteInt(sampleRate * 65536); // Unsigned Fixed Point
	bufFlush();
	size += 20;
	return writeSize(offset, size);
    }

    private int  writeSTTS(int streamNumber, String type) {
	long offset = filePointer;
	bufClear();
	bufWriteInt(0);
	bufWriteBytes("stts");
	int size = 8;
	bufWriteInt(0);  // Version + Flags
	size += 4;
	if (type.equals(VIDEO)) {
	    VideoTrakInfo vti = (VideoTrakInfo) trakInfoArray[streamNumber];

 	    if (ALWAYS_USE_ONE_ENTRY_FOR_STTS ||
		(vti.minDuration <= -1) /* $$ should it be <= 0? */ ||
  		( (vti.maxDuration - vti.minDuration) < EPSILON_DURATION ) ) {

		bufWriteInt(1);  // Number of entries
		size += 4;
		bufWriteInt(vti.totalFrames);
		bufWriteInt(vti.frameDuration);
		vti.duration = vti.totalFrames * vti.frameDuration;
		size += 8;
	    } else {
		// TODO: Collapse entries with the same duration so that
		// to make this table small
		// System.out.println("numFrames is " + vti.totalFrames);
		bufWriteInt(vti.totalFrames);  // Number of entries
		size += 4;
		
		vti.duration = 0;

		long[][] timeStamps = vti.timeStamps;
		int indexi = 0;
		int indexj = 0;
		long timeStamp = 0;
		int numEntries = vti.totalFrames-1;
		int bytesPerLoop = 2* 4;
		int actualBufSize = ( (maxBufSize - 200) / bytesPerLoop ) * bytesPerLoop;
		int requiredSize = numEntries * bytesPerLoop;

		int numLoops;
		int entriesPerLoop;
		if (requiredSize <= actualBufSize) {
		    numLoops = 1;
		    entriesPerLoop = numEntries;
		} else {
		    numLoops = requiredSize / actualBufSize;
		    if ( ( (float) requiredSize / actualBufSize ) > numLoops )
			numLoops++;
		    entriesPerLoop = actualBufSize / bytesPerLoop;
		}
		for (int ii = 0; ii < numLoops; ii++) {
		    for (int jj = 0; jj < entriesPerLoop; jj++) {
			bufWriteInt(1); // Num frames with this duration
			timeStamp = timeStamps[indexi][indexj++];
			// duration
			int dur = (int) ( 0.5 + (timeStamp / 1000000000.) * movieTimeScale);
			bufWriteInt(dur);
			vti.duration += dur;
			size += 8;
			if (indexj >= vti.MAX_TIMESTAMP_ARRAYSIZE) {
			    indexi++;
			    indexj = 0;
			}
		    }
		    bufFlush();
		    bufClear();
		    if (ii == numLoops -2) {
			entriesPerLoop = numEntries - (numLoops -1) * entriesPerLoop;
		    }
		}

		// Last frame gets same duration as previous frame
		if (vti.totalFrames > 1) {
		    bufWriteInt(1); // Num frames with this duration
		    int dur = (int) ( (timeStamp / 1000000000.) * movieTimeScale);
		    bufWriteInt(dur);
		    size += 8;
		    vti.duration += dur;
		    // System.out.println("vti.duration final is " + vti.duration);
		}
	    }

	    for (int i = 0; i < vti.numTimeStampArraysUsed; i++) {
		vti.timeStamps[i] = null; // Can be garbage collected
	    }
	} else {
	    // Will be Audio
	    AudioTrakInfo ati = (AudioTrakInfo) trakInfoArray[streamNumber];
	    bufWriteInt(1);  // Number of entries
	    size += 4;
 	    bufWriteInt(ati.numSamples);
 	    bufWriteInt(1); // Sample duration
	    size += 8;
	}
	if (bufLength > 0) {
	    bufFlush();
	}
	return writeSize(offset, size);
    }

    /**
     * Sync Sample Atom
     */
    private int  writeSTSS(int streamNumber, String type) {
	if ( !type.equals(VIDEO) ) { // sync sample atom applies only to video tracks
	    return 0;
	}
	VideoTrakInfo vti = (VideoTrakInfo) trakInfoArray[streamNumber];
	int numKeyFrameArraysUsed = vti.numKeyFrameArraysUsed;

	int numKeyFrames = (numKeyFrameArraysUsed - 1) * vti.MAX_KEYFRAME_ARRAYSIZE +
	                   vti.keyFrameIndex;
	// System.out.println("stts:numKeyFrames is " + numKeyFrames);
	if ( numKeyFrames == 0) {
	    com.sun.media.Log.warning("Error: There should be atleast 1 keyframe in the track. All frames are now treated as keyframes");
	    return 0;
	}

	if (numKeyFrames == vti.totalFrames) {
	    // No sync sample atom as all frames are key frames	    
	    for (int i = 0; i < numKeyFrameArraysUsed; i++) {
		vti.keyFrames[i] = null; // Can be garbage collected
	    }
	    return 0; 
	}

	long offset = filePointer;
	bufClear();
	bufWriteInt(0);
	bufWriteBytes("stss");
	int size = 8;
	bufWriteInt(0);  // Version + Flags
	size += 4;

	int[][] keyFrames = vti.keyFrames;
	bufWriteInt(numKeyFrames); // Number of entries
	size += 4;
		
	int numEntries = numKeyFrames;
	int bytesPerLoop = 1 * 4;



	int actualBufSize = ( (maxBufSize - 200) / bytesPerLoop ) * bytesPerLoop;
	int requiredSize = numEntries * bytesPerLoop;
	
	int numLoops;
	int entriesPerLoop;
	if (requiredSize <= actualBufSize) {
	    numLoops = 1;
	    entriesPerLoop = numEntries;
	} else {
	    numLoops = requiredSize / actualBufSize;
	    if ( ( (float) requiredSize / actualBufSize ) > numLoops )
		numLoops++;
	    entriesPerLoop = actualBufSize / bytesPerLoop;
	}
	
	int indexi = 0;
	int indexj = 0;
	for (int ii = 0; ii < numLoops; ii++) {
	    for (int jj = 0; jj < entriesPerLoop; jj++) {
		bufWriteInt(keyFrames[indexi][indexj++]);
		if (indexj >= vti.MAX_KEYFRAME_ARRAYSIZE) {
		    indexi++;
		    indexj = 0;
		}
	    }
	    bufFlush();
	    bufClear();
	    if (ii == numLoops -2) {
		entriesPerLoop = numEntries - (numLoops -1) * entriesPerLoop;
	    }
	}
	size += (numKeyFrames * 4);

	return writeSize(offset, size);
    }

    private int  writeSTSC(int streamNumber, String type) {
	long offset = filePointer;
	bufClear();
	bufWriteInt(0);
	bufWriteBytes("stsc");
	int size = 8;
	bufWriteInt(0);  // Version + Flags
	size += 4;

	if (type.equals(VIDEO)) {
	    // All chunks have only one sample, ie video frame
	    VideoTrakInfo vti = (VideoTrakInfo) trakInfoArray[streamNumber];
	    bufWriteInt(1);  // Number of entries
	    size += 4;
	    bufWriteInt(1);  // First Chunk
	    bufWriteInt(1);  // Samples/Chunk
	    bufWriteInt(1);  // Sample Description
	    size += 12;
	} else {
	    // Will be Audio
	    AudioTrakInfo ati = (AudioTrakInfo) trakInfoArray[streamNumber];
	    int numberOfEntries =
		( (ati.numSamplesPerChunkArraysUsed - 1) * ati.MAX_SAMPLESPERCHUNK_ARRAYSIZE +
		     ati.samplesPerChunkIndex) / 2;
	    bufWriteInt(numberOfEntries);  // Number of entries
	    size += 4;

	    int numEntries = numberOfEntries;
	    int bytesPerLoop = 3 * 4;
	    int actualBufSize = ( (maxBufSize - 200) / bytesPerLoop ) * bytesPerLoop;
	    int requiredSize = numEntries * bytesPerLoop;
	    
	    int numLoops;
	    int entriesPerLoop;
	    if (requiredSize <= actualBufSize) {
		numLoops = 1;
		entriesPerLoop = numEntries;
	    } else {
		numLoops = requiredSize / actualBufSize;
		if ( ( (float) requiredSize / actualBufSize ) > numLoops )
		    numLoops++;
		entriesPerLoop = actualBufSize / bytesPerLoop;
	    }
	    int indexi = 0;
	    int indexj = 0;
	    int samplesPerChunkArray[][] = ati.samplesPerChunkArray;
	    for (int ii = 0; ii < numLoops; ii++) {
		for (int jj = 0; jj < entriesPerLoop; jj++) {
		    bufWriteInt(samplesPerChunkArray[indexi][indexj++]); // First Chunk
		    bufWriteInt(samplesPerChunkArray[indexi][indexj++]); // Samples/Chunk
		    bufWriteInt(1); // Sample Description ID
		    if (indexj >= ati.MAX_SAMPLESPERCHUNK_ARRAYSIZE) {
			indexi++;
			indexj = 0;
		    }
		}
		bufFlush();
		bufClear();
		if (ii == numLoops -2) {
		    entriesPerLoop = numEntries - (numLoops -1) * entriesPerLoop;
		}
	    }
	    size += (numberOfEntries * 12);
	}
	if (bufLength > 0) {
	    bufFlush();
	}
	return writeSize(offset, size);
    }


    private int  writeSTSZ(int streamNumber, String type) {
	long offset = filePointer;
	bufClear();
	bufWriteInt(0);
	bufWriteBytes("stsz");
	int size = 8;
	bufWriteInt(0);  // Version + Flags
	size += 4;


	TrakInfo trakInfo = trakInfoArray[streamNumber];


	if (type.equals(AUDIO)) {
	    // All Samples have the same sample size
	    bufWriteInt( 1 ); // sampleSize
	    bufWriteInt( ((AudioTrakInfo)trakInfo).numSamples);
	    size += 8;
	} else if (type.equals(VIDEO)) {
            VideoTrakInfo vti = (VideoTrakInfo) trakInfo;
	    int numSampleSizeArraysUsed = vti.numSampleSizeArraysUsed;
	    int numberOfEntries = trakInfo.totalFrames;
	    if (trakInfo.constantSampleSize) {
		// All Samples have the same sample size
		// This is always the case for audio but sometimes for
		// video, for e.g uncompressed video.
		int sampleSize = vti.sampleSize[0][0];
		bufWriteInt(sampleSize);  // sampleSize
		bufWriteInt(numberOfEntries);
		size += 8;
	    } else {
		int[][] sampleSize = vti.sampleSize;
		bufWriteInt(0);  // implies that sampleSize is not constant
		bufWriteInt(numberOfEntries); // Number of Entries
		size += 8;
		
		int numEntries = numberOfEntries;
		int bytesPerLoop = 1 * 4;
		int actualBufSize = ( (maxBufSize - 200) / bytesPerLoop ) * bytesPerLoop;
		int requiredSize = numEntries * bytesPerLoop;
		
		int numLoops;
		int entriesPerLoop;
		if (requiredSize <= actualBufSize) {
		    numLoops = 1;
		    entriesPerLoop = numEntries;
		} else {
		    numLoops = requiredSize / actualBufSize;
		    if ( ( (float) requiredSize / actualBufSize ) > numLoops )
			numLoops++;
		    entriesPerLoop = actualBufSize / bytesPerLoop;
		}
		int indexi = 0;
		int indexj = 0;

		for (int ii = 0; ii < numLoops; ii++) {
		    for (int jj = 0; jj < entriesPerLoop; jj++) {
			bufWriteInt(sampleSize[indexi][indexj++]);
			if (indexj >= vti.MAX_SAMPLE_SIZE_ARRAYSIZE) {
			    indexi++;
			    indexj = 0;
			}
		    }
		    bufFlush();
		    bufClear();
		    if (ii == numLoops -2) {
			entriesPerLoop = numEntries - (numLoops -1) * entriesPerLoop;
		    }
		}
		size += (numberOfEntries * 4);
	    }
	    // Is this garbage collection necessary?
	    for (int i = 0; i < numSampleSizeArraysUsed; i++) {
		vti.sampleSize[i] = null; // Can be garbage collected.
	    }
	}
	
	if (bufLength > 0) {
	    bufFlush();
	}
	return writeSize(offset, size);
    }


    private int  writeSTCO(int streamNumber, String type) {
	long offset = filePointer;
	bufClear();
	bufWriteInt(0);
	bufWriteBytes("stco");
	int size = 8;
	bufWriteInt(0);  // Version + Flags
	size += 4;

	TrakInfo trakInfo = trakInfoArray[streamNumber];
	int numChunkOffsetsArraysUsed = trakInfo.numChunkOffsetsArraysUsed;
	int[][] chunkOffsetsArray = trakInfo.chunkOffsetsArray;
		
	bufWriteInt(trakInfo.totalFrames); // Number of Entries
	size += 4;
		
	int numEntries = trakInfo.totalFrames;
	int bytesPerLoop = 1 * 4;
	int actualBufSize = ( (maxBufSize - 200) / bytesPerLoop ) * bytesPerLoop;
	int requiredSize = numEntries * bytesPerLoop;
	
	int numLoops;
	int entriesPerLoop;
	if (requiredSize <= actualBufSize) {
	    numLoops = 1;
	    entriesPerLoop = numEntries;
	} else {
	    numLoops = requiredSize / actualBufSize;
	    if ( ( (float) requiredSize / actualBufSize ) > numLoops )
		numLoops++;
	    entriesPerLoop = actualBufSize / bytesPerLoop;
	}
	int indexi = 0;
	int indexj = 0;
	trakInfo.chunkOffsetOffset = filePointer;

	int off;

	for (int ii = 0; ii < numLoops; ii++) {
	    for (int jj = 0; jj < entriesPerLoop; jj++) {
		off = (int) (chunkOffsetsArray[indexi][indexj++] - mdatOffset);
		bufWriteInt(off);
		if (indexj >= trakInfo.MAX_CHUNKOFFSETS_ARRAYSIZE) {
		    indexi++;
		    indexj = 0;
		}
	    }
	    bufFlush();
	    bufClear();
	    if (ii == numLoops -2) {
		entriesPerLoop = numEntries - (numLoops -1) * entriesPerLoop;
	    }
	}
	size += (trakInfo.totalFrames * 4);
	return writeSize(offset, size);
    }


    // Update the chunkoffset values. This needs to be done because
    // the MOOV chunk will be moved in front of the MDAT chunk to
    // create a streamable file
    private void  updateSTCO() {
	for (int streamNumber = 0; streamNumber < trakInfoArray.length; streamNumber++) {
	    TrakInfo trakInfo = trakInfoArray[streamNumber];
	    int numChunkOffsetsArraysUsed = trakInfo.numChunkOffsetsArraysUsed;
	    int[][] chunkOffsetsArray = trakInfo.chunkOffsetsArray;
		
	    int chunkOffsetOffset = trakInfo.chunkOffsetOffset;
	    seek(chunkOffsetOffset);
	    bufClear();

	    int numEntries = trakInfo.totalFrames;
	    int bytesPerLoop = 1 * 4;
	    int actualBufSize = ( (maxBufSize - 200) / bytesPerLoop ) * bytesPerLoop;
	    int requiredSize = numEntries * bytesPerLoop;
	    
	    int numLoops;
	    int entriesPerLoop;
	    if (requiredSize <= actualBufSize) {
		numLoops = 1;
		entriesPerLoop = numEntries;
	    } else {
		numLoops = requiredSize / actualBufSize;
		if ( ( (float) requiredSize / actualBufSize ) > numLoops )
		    numLoops++;
		entriesPerLoop = actualBufSize / bytesPerLoop;
	    }
	    int indexi = 0;
	    int indexj = 0;
	    int off;

	    for (int ii = 0; ii < numLoops; ii++) {
		for (int jj = 0; jj < entriesPerLoop; jj++) {
		    off = (int) (chunkOffsetsArray[indexi][indexj++] + moovLength);
		    bufWriteInt(off);
		    if (indexj >= trakInfo.MAX_CHUNKOFFSETS_ARRAYSIZE) {
			indexi++;
			indexj = 0;
		    }
		}
		bufFlush();
		bufClear();
		if (ii == numLoops -2) {
		    entriesPerLoop = numEntries - (numLoops -1) * entriesPerLoop;
		}
	    }
	}
    }

    /***********************************************************************
     * INNER CLASSES
     ***********************************************************************/

    private class TrakInfo {
	boolean initFormat = false;
	boolean supported = false;
	String type;
	String encoding;
	Format format;
	long tkhdDurationOffset = -1;
	long mdhdDurationOffset = -1;

	int totalFrames = 0; // TODO changeName?: this is actually total # of chunks
	int duration; // duration of track

	// TODO: if constantSampleSize is always true for audio,
	// move it to VideoTrakInfo class.
	boolean constantSampleSize = true;

 	final int MAX_CHUNKOFFSETS_NUMARRAYS = 1000;
 	final int MAX_CHUNKOFFSETS_ARRAYSIZE = 1000;
	int numChunkOffsetsArraysUsed = 1;
	int chunkOffsetsIndex = 0;
	int chunkOffsetsArray[][];

	int chunkOffsetOffset;

	public TrakInfo() {
	   chunkOffsetsArray = new int[MAX_CHUNKOFFSETS_NUMARRAYS][];
	   chunkOffsetsArray[0] = new int[MAX_CHUNKOFFSETS_ARRAYSIZE];
	}

	public String toString() {
	    if (!supported) {
		System.out.println("No support for format " + format);
	    }
	    return (type + ": " + encoding + " : totalFrames " + totalFrames);
	}
    }

    private class VideoTrakInfo extends TrakInfo {
	VideoFormat videoFormat;
	float frameRate;
	int frameDuration;

	// TODO?: AT THIS POINT I DON"T THINK I NEED A sampleSize
	// array for audio. Because all samples will be of the same
	// size. If not the stsz atom for audio will be huge as we
	// need to list the size for every sample
	// In the extremely unlikely case that this is not true,
	// we need to move the next
	// 5 lines to the base class. If that is done, for audio
	// usr rle so that we don't need large arrays, also we need
	// to update constantSampleSize boolean and set it to false
	// if all samples are not of the same sample size
	final int MAX_SAMPLE_SIZE_NUMARRAYS = 1000;
	final int MAX_SAMPLE_SIZE_ARRAYSIZE = 1000*2; // Should be even [for rle audio]
	int numSampleSizeArraysUsed = 1;
	int sampleSizeIndex = 0;
	int sampleSize[][];

	final int MAX_KEYFRAME_NUMARRAYS = 1000;
	final int MAX_KEYFRAME_ARRAYSIZE = 1000;
	int numKeyFrameArraysUsed = 1;
	int keyFrameIndex = 0;
	int keyFrames[][];

	final int MAX_TIMESTAMP_NUMARRAYS = 1000;
	final int MAX_TIMESTAMP_ARRAYSIZE = 1000;
	int numTimeStampArraysUsed = 1;
	int timeStampIndex = 0;
	long timeStamps[][];


	long minDuration = Long.MAX_VALUE;
	long maxDuration = -1;
	long previousTimeStamp;

	public VideoTrakInfo() {
	    sampleSize = new int[MAX_SAMPLE_SIZE_NUMARRAYS][];
	    sampleSize[0] = new int[MAX_SAMPLE_SIZE_ARRAYSIZE];

	    keyFrames = new int[MAX_KEYFRAME_NUMARRAYS][];
	    keyFrames[0] = new int[MAX_KEYFRAME_ARRAYSIZE];

	    timeStamps = new long[MAX_TIMESTAMP_NUMARRAYS][];
	    timeStamps[0] = new long[MAX_TIMESTAMP_ARRAYSIZE];
	}

	public String toString() {
	    return (super.toString() + (" \n frameRate " + frameRate + " : frameDuration " + frameDuration));
	}
    }

    private class AudioTrakInfo extends TrakInfo {
	AudioFormat audioFormat;

	final int IMA4_SAMPLES_PER_BLOCK = 64;
	final int GSM_SAMPLES_PER_BLOCK = 160;
	final int MAC3_SAMPLES_PER_BLOCK = 6;
	final int MAC6_SAMPLES_PER_BLOCK = 6;
	
	int samplesPerBlock = 1;
	int numSamples = 0;
	int frameSizeInBytes; // TODO: see if you need this


	// Note: the reason we don't have this array for video also
	// is that in the JMF architecture there will be one video
	// frame per Buffer (Chunk). That is one sample (frame) per chunk.
	// Note that the Sample Description ID will always be 1
	final int MAX_SAMPLESPERCHUNK_NUMARRAYS = 1000;
        // Should be even as we will have <chunk#, samples/chunk> pairs
	final int MAX_SAMPLESPERCHUNK_ARRAYSIZE = 500*2;
	int numSamplesPerChunkArraysUsed = 1;
	int samplesPerChunkIndex = 0;
	int samplesPerChunkArray[][];
	int previousSamplesPerChunk = -1;

	public AudioTrakInfo() {
	    samplesPerChunkArray = new int[MAX_SAMPLESPERCHUNK_NUMARRAYS][];
	    samplesPerChunkArray[0] = new int[MAX_SAMPLESPERCHUNK_ARRAYSIZE];
	    samplesPerChunkArray[0][0] = 1; // first chunk
	    samplesPerChunkArray[0][1] = -1; // sample/chunk not initialized
	}

    }
   
}
