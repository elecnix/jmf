/*
 * @(#)AUMux.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.multiplexer.audio;

import javax.media.Time;
import javax.media.Duration;
import javax.media.Buffer;
import javax.media.Multiplexer;
import javax.media.Format;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.FileTypeDescriptor;
import com.sun.media.format.WavAudioFormat;
import javax.media.format.UnsupportedFormatException;
import java.io.IOException;
import javax.media.Control;
import javax.media.IncompatibleSourceException;
import javax.media.format.AudioFormat;


public class AUMux extends com.sun.media.multiplexer.BasicMux {

    private final static int HEADER_SIZE = 24;
    private final static int UNKNOWN_ENCODING = -1;
    // private boolean bigEndian;
    private AudioFormat audioFormat;
    private int sampleSizeInBits;
    private int encoding;
    private double sampleRate;
    private int channels;    

    private final static int AU_SUN_MAGIC =     0x2e736e64;
    private final static int AU_ULAW_8 = 1; /* 8-bit ISDN u-law */
    private final static int AU_ALAW_8 = 27; /* 8-bit ISDN A-law */
    private final static int AU_LINEAR_8 = 2; /* 8-bit linear PCM */
    private final static int AU_LINEAR_16 = 3; /* 16-bit linear PCM */
    private final static int AU_LINEAR_24 = 4; /* 24-bit linear PCM */
    private final static int AU_LINEAR_32 = 5; /* 32-bit linear PCM */
    private final static int AU_FLOAT = 6; /* 32-bit IEEE floating point */
    private final static int AU_DOUBLE = 7; /* 64-bit IEEE floating point */

    public AUMux() {
	supportedInputs = new Format[1];
	supportedInputs[0] = new AudioFormat(null);
	supportedOutputs = new ContentDescriptor[1];
	supportedOutputs[0] = new FileTypeDescriptor(FileTypeDescriptor.BASIC_AUDIO);
    }

    public String getName() {
	return "Basic Audio Multiplexer";
    }

    public int setNumTracks(int nTracks) {
	if (nTracks != 1)
	    return 1;
	else
	    return super.setNumTracks(nTracks);
    }

    protected void writeHeader() {
	bufClear();
	bufWriteInt(AU_SUN_MAGIC);
	bufWriteInt(HEADER_SIZE);
	bufWriteInt(-1); // Size of data, to be filled in at the end
	bufWriteInt(encoding);
	bufWriteInt((int) sampleRate);
	bufWriteInt(channels);
	bufFlush();
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

	// Linear Data has to be in big endian and should be signed
	if (encodingString.equalsIgnoreCase(AudioFormat.LINEAR)) {
	    if (sampleSizeInBits > 8 &&
		audioFormat.getEndian() == AudioFormat.LITTLE_ENDIAN)
		return null;

	    if (audioFormat.getSigned() == AudioFormat.UNSIGNED)
		return null;

	    if (audioFormat.getEndian() == AudioFormat.NOT_SPECIFIED ||
		audioFormat.getSigned() == AudioFormat.NOT_SPECIFIED)
		audioFormat = (AudioFormat)audioFormat.intersects(bigEndian);
	}

	// System.out.println("encodingString is " + encodingString);
	encoding = getEncoding(encodingString, sampleSizeInBits);
	if (encoding == UNKNOWN_ENCODING) {
	    reason = "No support for encoding " + encodingString;
	}

	sampleRate = audioFormat.getSampleRate();
	channels = audioFormat.getChannels();

	if (reason == null)
	    return audioFormat;
	else
	    return null;
    }

    protected void writeFooter() {
	seek(8);
	bufClear();
	bufWriteInt(fileSize - HEADER_SIZE);
	bufFlush();
    }

    // TODO: add other encodings: G723_3??, G723_5??, adpcm
    private int getEncoding(String encodingString, int sampleSizeInBits) {
	if (encodingString.equalsIgnoreCase(AudioFormat.ULAW))
	    return AU_ULAW_8;
	else if (encodingString.equalsIgnoreCase(AudioFormat.ALAW))
	    return AU_ALAW_8;
	else if (encodingString.equalsIgnoreCase(AudioFormat.LINEAR)) {
	    if (sampleSizeInBits == 8)
		return AU_LINEAR_8;
	    else if (sampleSizeInBits == 16)
		return AU_LINEAR_16;
	    else if (sampleSizeInBits == 24)
		return AU_LINEAR_24;
	    else if (sampleSizeInBits == 32)
		return AU_LINEAR_32;
	    else
		return UNKNOWN_ENCODING;
	} else if (encodingString.equalsIgnoreCase("float"))
	    return AU_FLOAT;
	else if (encodingString.equalsIgnoreCase("double"))
	    return AU_DOUBLE;
	else {
	    //System.out.println("getEncoding: returns UNKNOWN_ENCODING");
	    return UNKNOWN_ENCODING;
	}
    }
}
