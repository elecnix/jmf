/*
 * @(#)AVIMux.java	1.36 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.multiplexer.video;

import java.awt.Dimension;
import java.util.Hashtable;
import javax.media.Time;
import javax.media.Duration;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.PlugIn;
import javax.media.protocol.Seekable;
import javax.media.protocol.PushDataSource;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.SourceStream;
import javax.media.protocol.SourceTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.FileTypeDescriptor;
import com.sun.media.format.WavAudioFormat;
import com.sun.media.format.AviVideoFormat;
import javax.media.format.*;
import javax.media.format.UnsupportedFormatException;
import java.io.IOException;
import javax.media.Control;
import javax.media.IncompatibleSourceException;
import javax.media.format.AudioFormat;
import javax.media.format.*;
import com.sun.media.util.ByteBuffer;
import java.util.Vector;

public class AVIMux extends com.sun.media.multiplexer.BasicMux {

    private int[] suggestedBufferSizes;
    private int[] suggestedBufferSizeOffsets;
    private int[] scaleOffsets;
    private boolean[] endOfMediaStatus;
    private int numberOfEoms = 0;
    private int width = 0;
    private int height = 0;
    private final static int MAX_FRAMES_STORED = 20000;
    
    // TODO: AviParser and this class should shared these
    // constants. Can put constants in a separate class
    private final static int AVIH_HEADER_LENGTH = 56;
    private final static int STRH_HEADER_LENGTH = 56;
    private final static int STRF_VIDEO_HEADER_LENGTH = 40;
    private final static int STRF_AUDIO_HEADER_LENGTH = 16;

    final static String AUDIO         = "auds";
    final static String VIDEO         = "vids";

    //

    final static int AVIF_HASINDEX 		= 0x00000010;
    final static int AVIF_MUSTUSEINDEX		= 0x00000020;
    final static int AVIF_ISINTERLEAVED 	= 0x00000100;
    final static int AVIF_WASCAPTUREFILE	= 0x00010000;
    final static int AVIF_COPYRIGHTED		= 0x00020000;
    final static int AVIF_KEYFRAME		= 0x00000010;

    // AVI Header
    //    private int usecPerFrame = 100000; // Default value: 10 frames/sec ????
    private int usecPerFrame = -1;
    private float frameRate = -1.0F;
    private int maxBytesPerSecond;
    private int paddingGranularity;
    private long avgFrameTime;
    //    private int flags = (AVIF_HASINDEX | AVIF_MUSTUSEINDEX);
    private int flags = (AVIF_HASINDEX);
    private int totalDataLength = 0;
    private int totalFrames = 0;
    private int totalVideoFrames = 0;
    private int initialFrames;
    private int[] reserved = new int[4];

    private Vector chunkList = new Vector(1);
    private final int BUF_SIZE = 16384;
    private ByteBuffer bbuf = new ByteBuffer(BUF_SIZE);
    private int chunkOffset = 4;                // Position of Chunk in file
    private int moviOffset = 0;
    private int avihOffset = 0;
    private int hdrlSizeOffset = 0;
    private int totalStrlLength = 0;

    private int blockAlign = 1; // TODO: when we support multiple audio tracks
                                // they may have different blockAlign

    private int samplesPerBlock = -1; // TODO: when we support multiple audio tracks
                                // they may have different samplesPerBlock

    private double sampleRate  = 0.; // TODO: when we support multiple audio tracks
                                         // they may have different durations

    private double audioDuration  = 0.; // TODO: when we support multiple audio tracks
                                         // they may have different durations

    private int averageBytesPerSecond = -1; // TODO: when we support multiple audio tracks
                                            // they may have different averageBytesPerSecond

    private int mp3BitRate = -1; // TODO: when we support multiple audio tracks
                                  // they may have different bit rates

    private long cumulativeInterFrameTimeVideo = 0;
    private long previousTimeStampVideo = 0;
    final static String LISTRECORDCHUNK        = "rec ";
    final static String VIDEO_MAGIC		= "dc"; // Video
    final static String VIDEO_MAGIC_JPEG	= "db"; // Video
    final static String VIDEO_MAGIC_IV32a	= "iv"; // Indeo 3.2
    final static String VIDEO_MAGIC_IV32b	= "32"; // Indeo 3.2
    final static String VIDEO_MAGIC_IV31	= "31"; // Indeo 3.1
    final static String VIDEO_MAGIC_CVID	= "id"; // Cinepak
    final static String AUDIO_MAGIC		= "wb"; // Audio

    //    private long nanoSecPerFrame = 0;


    public AVIMux() {
	supportedInputs = new Format[2];
	supportedInputs[0] = new AudioFormat(null);
	supportedInputs[1] = new VideoFormat(null);

	supportedOutputs = new ContentDescriptor[1];
	supportedOutputs[0] = new FileTypeDescriptor(FileTypeDescriptor.MSVIDEO);

	chunkList.addElement(bbuf);
    }

    private Format [] createRGBFormats(Dimension size) {
	int NS = Format.NOT_SPECIFIED;
	Format [] rgbFormats = new Format[] {
	    new RGBFormat(size, size.width * size.height * 2,
			  Format.byteArray, NS, 16,
			  0x1F << 10, 0x1F << 5, 0x1F,
			  2, size.width * 2, RGBFormat.TRUE, RGBFormat.LITTLE_ENDIAN),
	    new RGBFormat(size, size.width * size.height * 3,
			  Format.byteArray, NS, 24,
			  3, 2, 1, 3, size.width * 3, RGBFormat.TRUE, NS),
	    new RGBFormat(size, size.width * size.height * 4,
			  Format.byteArray, NS, 32,
			  3, 2, 1, 4, size.width * 4, RGBFormat.TRUE, NS)
	};
	return rgbFormats;
    }

    private Format [] createYUVFormats(Dimension size) {
	int NS = Format.NOT_SPECIFIED;
	Format [] yuvFormats = new Format[] {
	    // UYVY
	    new YUVFormat(size, size.width * size.height * 2, Format.byteArray, NS,
			  YUVFormat.YUV_YUYV,
			  size.width * 2, size.width * 2, 1, 0, 2),
	    // YUY2
	    new YUVFormat(size, size.width * size.height * 2, Format.byteArray, NS,
			  YUVFormat.YUV_YUYV,
			  size.width * 2, size.width * 2, 0, 1, 3),
	    // 4:2:0 planar
	    new YUVFormat(size, (size.width * size.height * 3) / 2,
			  Format.byteArray, NS,
			  YUVFormat.YUV_420,
			  size.width, size.width / 2,
			  0, size.width * size.height, (size.width * size.height * 5) / 4),
	    // 4:2:0 planar
	    new YUVFormat(size, (size.width * size.height * 3) / 2,
			  Format.byteArray, NS,
			  YUVFormat.YUV_420,
			  size.width, size.width / 2,
			  0, (size.width * size.height * 5) / 4,size.width * size.height)
	};
	return yuvFormats;
    }

    public String getName() {
	return "AVI Multiplexer";
    }

    // Little Endian Format for intersecting with the input format.
    Format littleEndian = new AudioFormat(null,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.LITTLE_ENDIAN,
			AudioFormat.NOT_SPECIFIED);

    // Little Endian Format for intersecting with the input format.
    Format signed = new AudioFormat(null,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.LITTLE_ENDIAN,
			AudioFormat.SIGNED);

    Format unsigned = new AudioFormat(null,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.LITTLE_ENDIAN,
			AudioFormat.UNSIGNED);

    public Format setInputFormat(Format input, int trackID) {

	String reason = null;
	//System.err.println("Got input format " + input + " #" + trackID);
	if (input instanceof AudioFormat) {
	    AudioFormat af = (AudioFormat) input;
	    WavAudioFormat wavAudioFormat = null;

	    if (input instanceof WavAudioFormat) {
		wavAudioFormat = (WavAudioFormat) input;
	    }

	    String encoding = af.getEncoding();
	    if (encoding == null)
		return null;

// 	    if (encoding.equalsIgnoreCase(AudioFormat.LINEAR) &&
// 		af.getSampleSizeInBits() > 8) {
// 		if (af.getEndian() == AudioFormat.BIG_ENDIAN)
// 		    return null;
// 		if (af.getEndian() == AudioFormat.NOT_SPECIFIED)
// 		    input = af.intersects(littleEndian);
// 	    }

// 	    encoding = encoding.toLowerCase();
	    if (encoding.equalsIgnoreCase(AudioFormat.LINEAR)) {
		if ( af.getSampleSizeInBits() > 8 ) {
		    if (af.getEndian() == AudioFormat.BIG_ENDIAN)
		    return null;

		if (af.getSigned() == AudioFormat.UNSIGNED)
		    return null;

		// Data has to be little endian and signed.
	        if (af.getEndian() == AudioFormat.NOT_SPECIFIED ||
		    af.getSigned() == AudioFormat.NOT_SPECIFIED) {
		    input = (AudioFormat)af.intersects(signed);
		}
		} else {
		    if (af.getSigned() == AudioFormat.SIGNED)
			return null;

		    // Data has to be unsigned.
		    if (af.getEndian() == AudioFormat.NOT_SPECIFIED ||
			af.getSigned() == AudioFormat.NOT_SPECIFIED) {
			input = (AudioFormat)af.intersects(unsigned);
		    }
		}
	    }
	
	    Integer formatTag = (Integer)
		WavAudioFormat.reverseFormatMapper.get(encoding.toLowerCase());
	    if ( (formatTag == null) ||
		 (af.getEncoding().equalsIgnoreCase(AudioFormat.TRUESPEECH)) ||
		 (af.getEncoding().toLowerCase().startsWith("voxware")) ) {

		reason = "Cannot handle format";
		return null;
	    }

	    // For certain encodings additional encoding-specific information
	    // is required which can only be obtained thru WavAudioFormat
	    short wFormatTag = formatTag.shortValue();
	    switch (wFormatTag) {
	    case WavAudioFormat.WAVE_FORMAT_ADPCM:
	    case WavAudioFormat.WAVE_FORMAT_DVI_ADPCM:
	    case WavAudioFormat.WAVE_FORMAT_GSM610:
		if (wavAudioFormat == null) {
		    reason = "A WavAudioFormat is required " +
			" to provide encoding specific information for this " +
			"encoding " + wFormatTag;
		    return null;
	        }
	    }
	} else if (input instanceof VideoFormat) {
	    VideoFormat vf = (VideoFormat) input;
	    // TODO check video formats for known raw
	    String encoding = vf.getEncoding();
	    Dimension size = vf.getSize();
	    if (size == null)
		size = new Dimension(320, 240);
	    if (encoding == null)
		return null;
	    // encoding = encoding.toLowerCase();
	    if (encoding.equalsIgnoreCase(VideoFormat.RGB)) {
		if (matches(vf, createRGBFormats(size)) == null)
		    return null;
	    } else if (encoding.equalsIgnoreCase(VideoFormat.YUV)) {
		if (matches(vf, createYUVFormats(size)) == null)
		    return null;
	    } else if (encoding.equalsIgnoreCase("jpeg")) {
		return null;
	    } else if (encoding.length() > 4)
 		return null; // Allow fourcc
	    
	    frameRate = vf.getFrameRate();
	    if (frameRate > 0)
		usecPerFrame = (int) ((1/frameRate) * 1000000);
	    avgFrameTime = usecPerFrame * 1000;
	} else {
	    reason = "Can only support Audio and Video formats";
	}

	if (reason != null) {
	    return null;
	} else {
	    inputs[trackID] = input;

	    return input;
	}
    }

    public int setNumTracks(int nTracks) {
	//System.err.println("Got numTracks = " + nTracks);
	if (nTracks > 2)
	    return 2;
	else {
	    suggestedBufferSizeOffsets = new int[nTracks];
	    suggestedBufferSizes = new int[nTracks];
	    endOfMediaStatus = new boolean[nTracks];
	    for (int i = 0; i < nTracks; i++) {
		suggestedBufferSizes[i] = -1;
		suggestedBufferSizeOffsets[i] = -1;
	    }

	    return super.setNumTracks(nTracks);
	}
    }

    public synchronized int doProcess(Buffer buffer, int trackID) {
	boolean isVideoFormat;
	
	if (buffer.isEOM()) {
	    numberOfEoms++;
	    if (numberOfEoms >= numTracks)
		return super.doProcess(buffer, trackID);
	    else
		return BUFFER_PROCESSED_OK;
	}

	
	if (buffer.getData() == null)
	    return BUFFER_PROCESSED_OK;
	
	isVideoFormat = buffer.getFormat() instanceof VideoFormat;

	if (isVideoFormat) {
	    long timeStamp = buffer.getTimeStamp();
	    if (timeStamp - previousTimeStampVideo > avgFrameTime * 1.9) {
		Buffer blankBuffer;
		int blankFrames = (int) ((timeStamp-previousTimeStampVideo) /
		                         avgFrameTime);
		for (int i = 0; i < blankFrames; i++) {
		    blankBuffer = new Buffer();
		    blankBuffer.setTimeStamp(previousTimeStampVideo +
					     i * avgFrameTime);
		    blankBuffer.setFormat(buffer.getFormat());
		    blankBuffer.setDuration(avgFrameTime);
		    blankBuffer.setSequenceNumber(buffer.getSequenceNumber());
		    blankBuffer.setFlags(buffer.getFlags() &
					 ~Buffer.FLAG_KEY_FRAME);
		    int result = writeFrame(blankBuffer, trackID);
		    if (result != BUFFER_PROCESSED_OK)
			return result;
		    //System.err.println("Inserted blank");
		}
	    }
	    //System.err.println("writing valid frame");
	}

	return writeFrame(buffer, trackID);
    }

    private int writeFrame(Buffer buffer, int trackID) {
	boolean isVideoFormat;
	int     pad;
	int     length;
	int     flag;
	
	isVideoFormat = buffer.getFormat() instanceof VideoFormat;

	length = buffer.getLength();
	if ((length & 1) > 0)
	    pad = 1;
	else
	    pad = 0;

	String aviEncodingMagic = getAviEncodingMagic(trackID, isVideoFormat);
	bufClear();
	bufWriteBytes(aviEncodingMagic);
	bufWriteIntLittleEndian(length + pad);
	bufFlush();
	
	if (length > 0)
	    write((byte[])buffer.getData(), buffer.getOffset(), length);
	if (pad > 0) {
	    bufClear();
	    bufWriteByte((byte)0);
	    bufFlush();
	}

	totalDataLength += length + pad;
	
	if (length > suggestedBufferSizes[trackID])
	    suggestedBufferSizes[trackID] = length;
	
	if (bbuf.length == BUF_SIZE) {
	    bbuf = new ByteBuffer(BUF_SIZE);
	    chunkList.addElement(bbuf);
	}

	bbuf.writeBytes(aviEncodingMagic);
	flag = (buffer.getFlags() & Buffer.FLAG_KEY_FRAME) != 0
	       ? AVIF_KEYFRAME
	       : 0;
	bbuf.writeIntLittleEndian(flag);
	bbuf.writeIntLittleEndian(chunkOffset);
	bbuf.writeIntLittleEndian(length);
	chunkOffset += (length + pad + 8);
	if (isVideoFormat) {
	    long timeStamp = buffer.getTimeStamp();
	    //System.out.println("AVIMux: timeStamp is " + timeStamp);
	    if (totalVideoFrames > 0)
		cumulativeInterFrameTimeVideo += (timeStamp - previousTimeStampVideo);
	    // System.out.println("AVIMux: cumulativeInterFrameTimeVideo is " + cumulativeInterFrameTimeVideo);
	    previousTimeStampVideo = timeStamp;
	    totalVideoFrames++;
	} else {
	    if (samplesPerBlock != -1) {
		int numBlocks = (length / blockAlign);
		int numSamples = numBlocks * samplesPerBlock;
		audioDuration  += (numSamples / sampleRate);
	    } else {
		if (averageBytesPerSecond > 0) {
		    audioDuration += ((double)length / averageBytesPerSecond);
		}
	    }
	}


	totalFrames++;
	return BUFFER_PROCESSED_OK;
    }
	
    protected void writeHeader() {

	for (int i = 0; i < inputs.length; i++) {

	    // TODO: Handle multiple audio tracks
	    if (inputs[i] instanceof AudioFormat) {
		AudioFormat af = (AudioFormat) inputs[i];
		WavAudioFormat wavAudioFormat = null;
		
		sampleRate = af.getSampleRate();
		if (af.getEncoding().equalsIgnoreCase(AudioFormat.LINEAR)) {
		    samplesPerBlock = 1;
		}

		if (inputs[i] instanceof WavAudioFormat) {
		    wavAudioFormat = (WavAudioFormat) inputs[i];
		    
		    byte[] codecSpecificHeader = wavAudioFormat.getCodecSpecificHeader();
		    if ( (!af.getEncoding().equalsIgnoreCase(AudioFormat.MPEGLAYER3)) &&
			 (codecSpecificHeader != null) &&
			 (codecSpecificHeader.length >= 2) ) {
			try {
			    samplesPerBlock =
				com.sun.media.parser.BasicPullParser.parseShortFromArray(
											 codecSpecificHeader, /*bigEndian*/ false);

			} catch (IOException e) {
			    System.err.println("Unable to parse codecSpecificHeader");
			    // samplesPerBlock = 1;
			}
		    }
		}
	    }
	}


	bufClear();
	bufWriteBytes("RIFF");
	bufSkip(4);
	bufWriteBytes("AVI ");
	bufWriteBytes("LIST");
	hdrlSizeOffset = filePointer;
	bufSkip(4);
	bufWriteBytes("hdrl");
	bufWriteBytes("avih");
	bufWriteIntLittleEndian(AVIH_HEADER_LENGTH);
	avihOffset = filePointer;
	bufSkip(AVIH_HEADER_LENGTH);

	scaleOffsets = new int[numTracks];
	for (int i = 0; i < numTracks; i++) {
	    Format format = inputs[i];
	    boolean isVideo = (format instanceof VideoFormat);
	    bufWriteBytes("LIST");

	    byte [] codecSpecificHeader = null;
	    int extraByteLength = 0; // size of codecSpecificHeader
	    RGBFormat rgbFormat;
	    AviVideoFormat aviVideoFormat = null;
	    WavAudioFormat wavAudioFormat = null;
	    int planes = 1; // Default value
	    int depth = 24; // Default value: Is this correct? $$$
	    String yuvEncoding = null;

	    String encoding = format.getEncoding();
	    int wFormatTag = -1;

	    if (isVideo) {
		int bytesInBitmap = 40;
		rgbFormat = null;
		
		if (format instanceof RGBFormat)
		    rgbFormat = (RGBFormat) format;
		else if (format instanceof YUVFormat) {
		    YUVFormat yuv = (YUVFormat) format;
		    if (  yuv.getYuvType() == YUVFormat.YUV_YUYV &&
			  yuv.getStrideY() == yuv.getSize().width * 2 &&
			  yuv.getOffsetY() == 0 && yuv.getOffsetU() == 1 &&
			  yuv.getOffsetV() == 3 )
			yuvEncoding = "YUY2";
		    else if (  yuv.getYuvType() == YUVFormat.YUV_YUYV &&
			       yuv.getStrideY() == yuv.getSize().width * 2  &&
			       yuv.getOffsetY() == 1 && yuv.getOffsetU() == 0 &&
			       yuv.getOffsetV() == 2 )
			yuvEncoding = "UYVY";
		    else if (  yuv.getYuvType() == YUVFormat.YUV_YUYV &&
			       yuv.getStrideY() == yuv.getSize().width * 2  &&
			       yuv.getOffsetY() == 0 && yuv.getOffsetU() == 3 &&
			       yuv.getOffsetV() == 1 )
			yuvEncoding = "YVYU";
		    else if (  yuv.getYuvType() == YUVFormat.YUV_420 ) {
			if ( yuv.getStrideY() == yuv.getSize().width &&
			     yuv.getStrideUV() == yuv.getSize().width / 2 ) {
			    if ( yuv.getOffsetU() < yuv.getOffsetV() )
				yuvEncoding = "I420";
			    else
				yuvEncoding = "YV12";
			}
		    }
		}
		if (format instanceof AviVideoFormat)
		    aviVideoFormat = (AviVideoFormat) format;
		
		if (aviVideoFormat != null) {
		    planes = aviVideoFormat.getPlanes();
		    depth = aviVideoFormat.getBitsPerPixel();
		    codecSpecificHeader = aviVideoFormat.getCodecSpecificHeader();
		} else if (rgbFormat != null) {
		    depth = rgbFormat.getBitsPerPixel();
		}
	    } else {
		if (format instanceof WavAudioFormat) {
		    wavAudioFormat = (WavAudioFormat) format;
		    codecSpecificHeader = wavAudioFormat.getCodecSpecificHeader();
		}
		if (codecSpecificHeader == null) {
		    Integer formatTag =
			(Integer)
			WavAudioFormat.reverseFormatMapper.get(encoding.toLowerCase());
		    if (formatTag != null) {
			wFormatTag = formatTag.shortValue();
			if (wFormatTag == WavAudioFormat.WAVE_FORMAT_MPEG_LAYER3) {
			    extraByteLength = 12;
			}
		    }
		}
	    }

	    if ((extraByteLength <= 0) && codecSpecificHeader != null) {
		extraByteLength = codecSpecificHeader.length;
	    }
	    

	    int strlLength = 0;
	    if (isVideo) {
		strlLength =  STRH_HEADER_LENGTH +
		    STRF_VIDEO_HEADER_LENGTH + 20 + extraByteLength;
		bufWriteIntLittleEndian(strlLength);
	    } else {
		if (extraByteLength > 0) {
		    strlLength = STRH_HEADER_LENGTH +
			STRF_AUDIO_HEADER_LENGTH + 20 + extraByteLength + 2;
		} else {
		    strlLength = STRH_HEADER_LENGTH +
			STRF_AUDIO_HEADER_LENGTH + 20;
		}
		bufWriteIntLittleEndian(strlLength);
	    }
	    totalStrlLength += strlLength;
	    bufWriteBytes("strl");
	    bufWriteBytes("strh");
	    bufWriteIntLittleEndian(STRH_HEADER_LENGTH);

	    
	    /// STRH
	    if (isVideo) {
		bufWriteBytes(VIDEO);
		{
		    // TODO: cleanup
		    if (encoding.startsWith("rgb"))
			encoding = "DIB ";
		    else if (yuvEncoding != null) {
			encoding = yuvEncoding;
		    }
		    bufWriteBytes(encoding);
		}
	    } else {
		bufWriteBytes(AUDIO);
		bufWriteIntLittleEndian(0); // encoding
	    }
	    bufWriteIntLittleEndian(0); // flags
	    bufWriteIntLittleEndian(0); // Priority
	    bufWriteIntLittleEndian(0); // Initial Frames
	    scaleOffsets[i] = filePointer;

	    bufWriteIntLittleEndian(1); // Scale: will be updated
	    bufWriteIntLittleEndian(15); // Rate: will be updated
	    bufWriteIntLittleEndian(0); // Start
	    bufWriteIntLittleEndian(0); // Length ??
	    suggestedBufferSizeOffsets[i] = filePointer;
	    bufWriteIntLittleEndian(0); // Suggested buffer size ??
	    bufWriteIntLittleEndian(10000); // quality ??
	    bufWriteIntLittleEndian(0); // sample size: will be updated for audio
	    bufWriteIntLittleEndian(0); // Padding 1
	    bufWriteIntLittleEndian(0); // Padding 2

	    /// STRF
	    // Number of bytes required by bitmap structure
	    bufWriteBytes("strf");
	    if (isVideo) {
		// Chunk length
		bufWriteIntLittleEndian(STRF_VIDEO_HEADER_LENGTH + extraByteLength);
		// Number of bytes required by bitmap structure
		bufWriteIntLittleEndian(STRF_VIDEO_HEADER_LENGTH + extraByteLength);
		width = ((VideoFormat)format).getSize().width;
		height = ((VideoFormat)format).getSize().height;
		bufWriteIntLittleEndian(width);
		bufWriteIntLittleEndian(height);
		bufWriteShortLittleEndian((short)planes); // video.planes ???
		bufWriteShortLittleEndian((short)depth); // video.depth   ???
		
		if (encoding.startsWith("DIB"))
		    bufWriteIntLittleEndian(0);
		else
		    bufWriteBytes(encoding); // ???
		
		int biSizeImage = 0;
		int biXPelsPerMeter = 0;
		int biYPelsPerMeter = 0;
		int biClrUsed = 0;
		int biClrImportant = 0;

		if (aviVideoFormat != null) {
		    if (aviVideoFormat.getImageSize() != Format.NOT_SPECIFIED)
			biSizeImage = aviVideoFormat.getImageSize();
		    if (aviVideoFormat.getXPelsPerMeter() != Format.NOT_SPECIFIED)
			biXPelsPerMeter = aviVideoFormat.getXPelsPerMeter();
		    if (aviVideoFormat.getYPelsPerMeter() != Format.NOT_SPECIFIED)
			biYPelsPerMeter = aviVideoFormat.getYPelsPerMeter();
		    if (aviVideoFormat.getClrUsed() != Format.NOT_SPECIFIED)
			biClrUsed = aviVideoFormat.getClrUsed();
		    if (aviVideoFormat.getClrImportant() != Format.NOT_SPECIFIED)
			biClrImportant = aviVideoFormat.getClrImportant();
		}

		bufWriteIntLittleEndian(biSizeImage); // bmi biSizeImage
		bufWriteIntLittleEndian(biXPelsPerMeter); // bmi biXPelsPerMeter
		bufWriteIntLittleEndian(biYPelsPerMeter); // bmi biYPelsPerMeter
		bufWriteIntLittleEndian(biClrUsed); // bmi biClrUsed
		bufWriteIntLittleEndian(biClrImportant); // bmi biClrImportant


	    } else { // Audio
		AudioFormat audioFormat = (AudioFormat) format;
		if (extraByteLength > 0) {
		    bufWriteIntLittleEndian(STRF_AUDIO_HEADER_LENGTH +
					    extraByteLength + 2);
		} else {
		    bufWriteIntLittleEndian(STRF_AUDIO_HEADER_LENGTH);
		}

		{ // MAY REMOVE BLOCK
		    if (encoding.equals("unknown")) {
			encoding = AudioFormat.LINEAR; // try WAVE_FORMAT_PCM
		    }
		}

		Integer formatTag = (Integer)
		    WavAudioFormat.reverseFormatMapper.get(encoding.toLowerCase());
		if (formatTag == null) {
		    // TODO : Make this check in setInputFormat
		} else {
		    bufWriteShortLittleEndian(formatTag.shortValue()); // encoding
		    short numChannels = (short) audioFormat.getChannels();
		    // System.out.println("AVIMux: numChannels is " + numChannels);
		    bufWriteShortLittleEndian(numChannels);
		    bufWriteIntLittleEndian((int) audioFormat.getSampleRate());
		    short sampleSizeInBits = (short) (audioFormat.getSampleSizeInBits());

		    if (wavAudioFormat != null) {
			averageBytesPerSecond = wavAudioFormat.getAverageBytesPerSecond(); //TODO: per audio track

			if (formatTag.shortValue() == WavAudioFormat.WAVE_FORMAT_MPEG_LAYER3) {
			    mp3BitRate = averageBytesPerSecond * 8;
			}
		    } else if (formatTag.shortValue() == WavAudioFormat.WAVE_FORMAT_MPEG_LAYER3) {
			
			int frameRate = (int) audioFormat.getFrameRate();

			if (frameRate != Format.NOT_SPECIFIED) {
			    averageBytesPerSecond = frameRate;
			    mp3BitRate = averageBytesPerSecond * 8;
			} else {
			    averageBytesPerSecond = 
				(int) audioFormat.getSampleRate() * numChannels *
				(sampleSizeInBits/8);
			}

		    } else {
			averageBytesPerSecond = 
			    (int) audioFormat.getSampleRate() * numChannels *
			          (sampleSizeInBits/8);
		    }
		    bufWriteIntLittleEndian(averageBytesPerSecond);
		    blockAlign = audioFormat.getFrameSizeInBits()/8;
		    if (blockAlign < 1) {
			blockAlign = (sampleSizeInBits * numChannels)/8;
		    }
		    if (blockAlign == 0)
			blockAlign = 1;
		    
		    if (mp3BitRate > 0) {
			// For mp3, set blockAlign to 1
			blockAlign = 1;
		    }
		    bufWriteShortLittleEndian((short)(blockAlign));
		    bufWriteShortLittleEndian(sampleSizeInBits);
		}
	    }
	    if (extraByteLength > 0) {
		if (!isVideo) {
		    if (codecSpecificHeader != null) {
			bufWriteShortLittleEndian((short) codecSpecificHeader.length);
			bufWriteBytes(codecSpecificHeader);
		    } else {
			Integer formatTag =
			    (Integer)
			    WavAudioFormat.reverseFormatMapper.get(encoding.toLowerCase());
			if (formatTag != null) {
			    wFormatTag = formatTag.shortValue();
			    if (wFormatTag == WavAudioFormat.WAVE_FORMAT_MPEG_LAYER3) {
				AudioFormat af = (AudioFormat) inputs[i];
				int frameRate = (int) af.getFrameRate();

				int blockSize;
				if (frameRate > 0) {
				    float temp = (72F * frameRate * 8) / 8000F;
				    temp *= (8000F / af.getSampleRate());
				    blockSize = (int) temp;
				} else {
				    blockSize = 417; // ???
				}


				bufWriteShortLittleEndian((short) 12); // Length of extra data
				bufWriteShortLittleEndian((short)1); // wID
				bufWriteIntLittleEndian(2); // fdwFlags
				bufWriteShortLittleEndian((short) blockSize); // nBlockSize
				bufWriteShortLittleEndian((short)1); // nFramesPerBlock
				bufWriteShortLittleEndian((short)1393); // nCodecDelay
			    }
			}
		    }
		} else {
		    bufWriteBytes(codecSpecificHeader); // VIDEO
		}
	    }
	}
	bufWriteBytes("LIST");
	moviOffset = filePointer;
	bufSkip(4); // List Length: To be filled in later
	bufWriteBytes("movi");
	bufFlush();
	/*fileSize = filePointer // header size
	    +  8; // 4 for the idx1 string and 4 for the idx1 length
	*/
	seek(hdrlSizeOffset);
	int hdrlSize = totalStrlLength + AVIH_HEADER_LENGTH +
	    4 /* bytes */ *
	    (1 + // for "hdrl" string
             2 + // for "avih" and avih_size
             2 * (numTracks)); // "LIST" and LIST_SIZE for the strl in every track
	// System.out.println("hdrlSize is " + hdrlSize);
	bufClear();
	bufWriteIntLittleEndian(hdrlSize); // This is filled in later
	//System.err.println("$$$ Wrote hdrlSize = " + hdrlSize );
	bufFlush();
	seek(moviOffset + 8);
    }

    protected void writeFooter() {
	writeIDX1Chunk();
	writeAVIH();
	seek(moviOffset);
	// for each frame you neeed 4 bytes for the chunk identifier (eg 01wb)
	// and 4 four bytes for the chunk length.
	// You need 4 bytes for the "movi" string
	bufClear();
	bufWriteIntLittleEndian(4 + totalDataLength + (totalFrames * 8)); // movi size
	bufFlush();
	
	for (int i = 0; i < numTracks; i++) {
	    int offset = suggestedBufferSizeOffsets[i];
	    if (offset > 0) {
		seek(offset);
		bufClear();
		bufWriteIntLittleEndian(suggestedBufferSizes[i]);
		bufFlush();
	    }
	    seek(scaleOffsets[i]);
	    if (inputs[i] instanceof VideoFormat) { // Video stream
		int rateVal = 10000;
		bufClear();
		bufWriteIntLittleEndian((int) (usecPerFrame / 100)); // scale
		bufWriteIntLittleEndian(rateVal);

		bufWriteIntLittleEndian(0); // Start
		bufWriteIntLittleEndian(totalVideoFrames);

		bufFlush();

		// sample size field not modified for video
	    } else { // audio stream
		AudioFormat audioFormat = (AudioFormat) inputs[i];
		// scale is frameSizeInBytes is blockAlign

		if (mp3BitRate > 0) {
		    bufClear();
		    bufWriteIntLittleEndian(8); // scale
		    bufFlush();

		    bufClear();
		    bufWriteIntLittleEndian(mp3BitRate); // rate is bit rate
		    bufFlush();
		    // for mp3 encoding set sample size to 1
		    blockAlign = 1;
		} else {
		    bufClear();
		    bufWriteIntLittleEndian(blockAlign);
		    bufFlush();
		    
		    int factor = 1;
		    if (samplesPerBlock > 0)
			factor = samplesPerBlock;
		    int rate = (int) ( (audioFormat.getSampleRate()
					/ factor) * blockAlign);
		    bufClear();
		    bufWriteIntLittleEndian(rate);
		    bufFlush();
		}


		// Update sampleSize field. sampleSize is blockAlign
		seek(filePointer + 16); // skip to the sampleSize field
		bufClear();
		bufWriteIntLittleEndian(blockAlign); // sampleSize
		bufFlush();
	    }
	}
	// Write file length "RIFF <length> AVI "
	seek(4);
	bufClear();
	bufWriteIntLittleEndian(fileSize-8);
	bufFlush();
	//System.err.println("Done pushing out AVI file");
    }
    
    private void writeIDX1Chunk() {
	bufClear();
	bufWriteBytes("idx1");
	bufWriteIntLittleEndian((totalFrames * 16)); // 16 is size of AVIIndexEntry
	bufFlush();
	
	for (int i = 0; i < chunkList.size(); i++) {
// 	    System.out.println("idx1 Frame # " + i + ": " +
// 			       aviIndexEntry[i].id + " : " +
// 			       (aviIndexEntry[i].flag) + " : " +
// 			       // (aviIndexEntry[i].chunkOffset + moviOffset /**+ 8*/) + " : " +
// 			       (aviIndexEntry[i].chunkOffset) + " : " +
// 			       (aviIndexEntry[i].chunkLength));
	    ByteBuffer bbuf = (ByteBuffer) chunkList.elementAt(i);
	    write(bbuf.buffer, 0, bbuf.length);
	}
    }

    private void writeAVIH() {

	// TODO: Need to modify this logic if we support more
	// than 2 tracks, e.g 1 video and 4 audio tracks.
	int audioFrames = 0;
	if (totalVideoFrames <= 0) {
	    // has no video track
	    usecPerFrame = 1000;
	    audioFrames = (int) ((audioDuration * 1000000.) / usecPerFrame);
	} else {
  	    // System.out.println("AVIMux: writeAVIH: usecPerFrame from format " + usecPerFrame);
  	    // System.out.println("AVIMux: cumulativeInterFrameTimeVideo is " +
	    //  			       cumulativeInterFrameTimeVideo);
	    // System.out.println("AVIMux: totalVideoFrames is " + totalVideoFrames);
	    int computedUsecPerFrame =
		(int) (cumulativeInterFrameTimeVideo / (1000. * (totalVideoFrames - 1)));
 	    // System.out.println("AVIMux: computedUsecPerFrame is " + computedUsecPerFrame);

	    // Note: always using computedUsecPerFrame
	    usecPerFrame = computedUsecPerFrame;
	}
	seek(avihOffset);
	bufClear();
	bufWriteIntLittleEndian(usecPerFrame);
	bufWriteIntLittleEndian(maxBytesPerSecond);
	bufWriteIntLittleEndian(paddingGranularity);
	bufWriteIntLittleEndian(flags);
	// TODO: Need to modify this logic if we support more
	// than 2 tracks, e.g 1 video and 4 audio tracks.
	if (totalVideoFrames > 0)
	    bufWriteIntLittleEndian(totalVideoFrames);
	else {
	    bufWriteIntLittleEndian(audioFrames); // bufWriteIntLittleEndian(totalFrames);
	}
	bufWriteIntLittleEndian(initialFrames);
	bufWriteIntLittleEndian(numTracks);
	// I see 32768 as suggested buffer size in the avi header of many
	// avi files.
	// There is also a suggestedBufferSize field in the strh chuck of
	// each track. This will contain the max buffer size for each track


	//	bufWriteIntLittleEndian(32768); // $$$$$$$$$ ====> UNCOMMENT $$$$
	bufWriteIntLittleEndian(0); // $$$$$$$$$ ====> REMOVE $$$$

	// $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
	// For width and height fields what do you set
	// for an audio only avi file?? What do you set
	// if you have multiple video tracks
	// Can you have an audio only avi file?
	// Can you have multiple video track in an avi file;
	// I don't think so.
	// $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
	bufWriteIntLittleEndian(width);
	bufWriteIntLittleEndian(height);
	bufWriteIntLittleEndian(0); // padding 1
	bufWriteIntLittleEndian(0); // padding 2
	bufWriteIntLittleEndian(0); // padding 3
	bufWriteIntLittleEndian(0); // padding 4
	bufFlush();
    }

    // RETURN: TODO: use hashtable
    //    private String getAviEncodingMagic(int streamNumber, Format format) {
    private String getAviEncodingMagic(int streamNumber, boolean isVideoFormat) {
	String encoding = inputs[streamNumber].getEncoding().toLowerCase();
	String magic;

	if (isVideoFormat) {
	    // $$$ NOT COMPLETE. USE Hashtable $$$ TODO
	    if (encoding.equalsIgnoreCase(VideoFormat.CINEPAK))
		magic = VIDEO_MAGIC_CVID;
	    else if (encoding.startsWith("iv32"))  // ??
		magic = VIDEO_MAGIC_IV32b;
	    else if (encoding.startsWith("iv31")) // ??
		magic = VIDEO_MAGIC_IV31;
	    else if (encoding.startsWith("iv")) // ??
		magic = VIDEO_MAGIC_IV32a;
	    else
		magic = VIDEO_MAGIC;
	} else
	    magic = AUDIO_MAGIC;
	
	String streamPrefix = null;
	// TODO: use equivalent of sprintf to generate streamPrefix from streamNumber
	// TEMPORARY HACK
	if (streamNumber == 0) {
	    streamPrefix = "00";
	} else if (streamNumber == 1) {
	    streamPrefix = "01";
	} else if (streamNumber == 2) {
	    streamPrefix = "02";
	} else if (streamNumber == 3) {
	    streamPrefix = "03";
	} else if (streamNumber == 4) {
	    streamPrefix = "04";
	}

	return (streamPrefix + magic);
    }
}
