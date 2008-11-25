/*
 * @(#)Handler.java	1.6 99/05/10
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.media.multiplexer.audio;

import javax.media.Time;
import javax.media.Duration;
import javax.media.Buffer;
import javax.media.Multiplexer;
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
import com.sun.media.BasicPlugIn;
import com.sun.media.format.WavAudioFormat;
import javax.media.format.UnsupportedFormatException;
import java.io.IOException;
import javax.media.Control;
import javax.media.IncompatibleSourceException;
import javax.media.format.AudioFormat;


public class AIFFMux extends com.sun.media.multiplexer.BasicMux {

    private Format format;
    private AudioFormat audioFormat;
    private int sampleSizeInBits;
    private double sampleRate;
    private int channels;    
    private int blockAlign = 1;
    private int dataSizeOffset;
    private int maxFrames = 0;
    private int maxFramesOffset = 0;
    private String formType;
    private String aiffEncoding;
    private int headerSize = 0;
    private int dataSize = 0;
    
    private static int AIFCVersion1   = 0xA2805140;

    private final static String FormID = "FORM"; //   ID for Form Chunk
    private final static String FormatVersionID = "FVER";   // ID for Format Version Chunk
    private final static String CommonID = "COMM";   // ID for Common Chunk
    private final static String SoundDataID = "SSND";   // ID for Sound Data Chunk
    private final static int CommonIDSize = 18;   // ID for Common Chunk for AIFF

    public AIFFMux() {
	supportedInputs = new Format[1];
	supportedInputs[0] = new AudioFormat(null);
	//				     8000.0,
	//				     33 * 8,
	//				     1);
	supportedOutputs = new ContentDescriptor[1];
	supportedOutputs[0] = new FileTypeDescriptor(FileTypeDescriptor.AIFF);
    }

    public String getName() {
	return "AIFF Audio Multiplexer";
    }

    public int setNumTracks(int nTracks) {
	if (nTracks != 1)
	    return 1;
	else
	    return super.setNumTracks(nTracks);
    }

    protected void writeHeader() {
	bufClear();
	bufWriteBytes(FormID);
	bufWriteInt(0); // This is filled in later when filesize is known
	bufWriteBytes(formType);

	if (formType.equals("AIFC")) {
	    bufWriteBytes(FormatVersionID);
	    bufWriteInt(4);
	    bufWriteInt(AIFCVersion1);
	}

	bufWriteBytes(CommonID);
	int commonIDSize = CommonIDSize;
	if (formType.equals("AIFC")) {
	    commonIDSize += 8; // for 2 4cc strings representing
	                       // Compression type and name.
	}

	bufWriteInt(commonIDSize);

	bufWriteShort((short) channels);
	maxFramesOffset = (int) filePointer;
	bufWriteInt(maxFrames);   // To be filled in later
	bufWriteShort((short) sampleSizeInBits);

	// Write sample rate as IEEE extended.
	// For now write the integer portion of sampleRate
        // TODO: Verify this calculation

	int exponent = 16398;
        double highMantissa;

	highMantissa = sampleRate;
	while (highMantissa < 44000) {
	    highMantissa *= 2;
	    exponent--;
	}
 	bufWriteShort((short) exponent);  // Exponent
	bufWriteInt( ((int) highMantissa) << 16);
 	bufWriteInt(0); // low Mantissa

	// Note: Since we use 4cc codes, Compression Type
	// and Compression Name take up 8 bytes
	if (formType.equals("AIFC")) {
	    bufWriteBytes(aiffEncoding); // Compression Type
	    bufWriteBytes(aiffEncoding); // Compression Name
	}

	bufWriteBytes(SoundDataID);
	dataSizeOffset = filePointer;
	bufWriteInt(0); // This is filled in later when datasize is known
	// Both offset and blocksize set to 0 upon recommendation in
	// "Sound Manager" Chapter in "Inside Macintosh Sound:, page 2-87
	bufWriteInt(0); // offset
	bufWriteInt(0); // blocksize
	bufFlush();
	headerSize = filePointer;
	//	fileSize = 
	//System.out.println("fileSize size after header is done: " + fileSize);
    }

    // Big Endian Format for intersecting with the input format.
    Format bigEndian = new AudioFormat(null,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.NOT_SPECIFIED,
			AudioFormat.BIG_ENDIAN,
			AudioFormat.SIGNED);

    public Format setInputFormat(Format format, int trackID) {

	String reason = null;
	
	if (!(format instanceof AudioFormat))
	    return null;
	
	audioFormat = (AudioFormat) format;

	String encodingString = audioFormat.getEncoding();
	sampleSizeInBits = audioFormat.getSampleSizeInBits();
	sampleRate = audioFormat.getSampleRate();
	channels = audioFormat.getChannels();

	blockAlign = channels * sampleSizeInBits / 8;

	if (encodingString.equalsIgnoreCase(AudioFormat.LINEAR)) {

	    if (sampleSizeInBits > 8 &&
		audioFormat.getEndian() == AudioFormat.LITTLE_ENDIAN)
		return null;

	    if (audioFormat.getSigned() == AudioFormat.UNSIGNED)
		return null;

	    if (audioFormat.getEndian() == AudioFormat.NOT_SPECIFIED ||
		audioFormat.getSigned() == AudioFormat.NOT_SPECIFIED)
		format = audioFormat.intersects(bigEndian);

	    // No compression: formType is AIFF
	    formType = "AIFF";
	    aiffEncoding = "NONE";

	} else {
	    // Some compression: formType is AIFC
	    formType = "AIFC";
	    if (encodingString.equalsIgnoreCase(AudioFormat.ULAW))
		aiffEncoding = "ulaw";
	    else if (encodingString.equalsIgnoreCase(AudioFormat.ALAW))
		aiffEncoding = "alaw";
	    else if (encodingString.equalsIgnoreCase(AudioFormat.IMA4)) {
		aiffEncoding = "ima4";
		/**
		 * Each packet contains 64 samples. Each sample is 4 bits/channel.
		 * So 64 samples is 32 bytes/channel.
		 * The 2 in the equation refers two bytes that the Apple's
		 * IMA compressor puts at the front of each packet, which 
		 * are referred to as predictor bytes
		 */
		blockAlign = (32 + 2) * channels;
	    } else if (encodingString.equalsIgnoreCase(AudioFormat.MAC3)) {
		aiffEncoding = encodingString;
		// 2 bytes represent 6 samples
		blockAlign = 2;
	    } else if (encodingString.equalsIgnoreCase(AudioFormat.MAC6)) {
		aiffEncoding = encodingString;
		// 1 byte represent 6 samples
		blockAlign = 1;
		// TODO: gsm
		// 	    } else if (encodingString.equalsIgnoreCase(AudioFormat.GSM)) {
		// 		aiffEncoding = "???";
		// 		/**
		// 		 * Each frame that consists of 160 speech samples
		// 		 * requires 33 bytes
		// 		 */
		// 		blockAlign = 33;
	    } else {
		reason = "Cannot handle encoding " + encodingString;
	    }
	}
	if (reason == null) {
	    inputs[0] = format;
	    return format;
	} else
	    return null;
    }
    
    protected void writeFooter() {
	byte [] dummy = new byte[] { 0 };
	
	dataSize = filePointer - headerSize;
	
	if ((filePointer & 1) != 0) {
	    // add a padding byte
	    write(dummy, 0, 1);
	}

	bufClear();
	seek(4);
	bufWriteInt(fileSize);
	bufFlush();

	bufClear();
	seek(maxFramesOffset);
	maxFrames = dataSize / blockAlign;
	//	System.out.println("maxFrames is " + maxFrames);
	bufWriteInt(maxFrames);
	bufFlush();
	
	bufClear();
	seek(dataSizeOffset);
	bufWriteInt(dataSize + 8); // 8 is for offset and blockSize
	bufFlush();
    }
}
