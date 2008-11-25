/*
 * @(#)JavaDecoder.java	1.8	02/08/21 SMI
 * 
 * Copyright 1998-1999 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */
package com.sun.media.codec.audio.mpa;

import javax.media.format.AudioFormat;
import javax.media.Format;
import javax.media.Buffer;
import javax.media.ResourceUnavailableException;
import com.sun.media.util.Arch;
import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;

/**
 * Class declaration
 *
 *
 * @author Liang He
 * @version 1.8, 08/21/02
 */
public class JavaDecoder extends com.sun.media.codec.audio.AudioCodec {
    private int			   pendingDataSize = 0;
    private static final int       OUTSIZE = 32 * 1024;
    private byte[]		   pendingData = new byte[OUTSIZE];
    private codecLib.mpa.Decoder   decoder = null;
    private codecLib.mpa.FrameInfo info = null;
    private boolean		   expectingSameInputBuffer = false;
    private long		   accumTS = 0;
    private AudioFormat		   aFormat = null;

    /**
     * Constructor declaration
     *
     *
     * @see
     */
    public JavaDecoder() {
	inputFormats = new Format[] {

/**
 -ivg
 Disable MP3 decoding for now.
 
	    new AudioFormat(AudioFormat.MPEGLAYER3,
			    16000., 
			    Format.NOT_SPECIFIED, 
			    Format.NOT_SPECIFIED, 
			    Format.NOT_SPECIFIED,    // endian
			    AudioFormat.SIGNED),
	    new AudioFormat(AudioFormat.MPEGLAYER3, 
			    22050.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, 
			    Format.NOT_SPECIFIED,    // endian
			    AudioFormat.SIGNED),
	    new AudioFormat(AudioFormat.MPEGLAYER3, 
			    24000.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, 
			    Format.NOT_SPECIFIED,    // endian
			    AudioFormat.SIGNED),
	    new AudioFormat(AudioFormat.MPEGLAYER3, 
			    32000.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, 
			    Format.NOT_SPECIFIED,    // endian
			    AudioFormat.SIGNED),
	    new AudioFormat(AudioFormat.MPEGLAYER3, 
			    44100.,
 			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, 
			    Format.NOT_SPECIFIED,    // endian
			    AudioFormat.SIGNED),
	    new AudioFormat(AudioFormat.MPEGLAYER3, 
			    48000.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, 
			    Format.NOT_SPECIFIED,    // endian
			    AudioFormat.SIGNED),
 */

	    new AudioFormat(AudioFormat.MPEG,
			    16000., 
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, 
			    Format.NOT_SPECIFIED,    // endian
			    AudioFormat.SIGNED),
	    new AudioFormat(AudioFormat.MPEG,
			    22050., 
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, 
			    Format.NOT_SPECIFIED,    // endian
			    AudioFormat.SIGNED),
	    new AudioFormat(AudioFormat.MPEG,
			    24000., 
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, 
			    Format.NOT_SPECIFIED,    // endian
			    AudioFormat.SIGNED),
	    new AudioFormat(AudioFormat.MPEG,
			    32000., 
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, 
			    Format.NOT_SPECIFIED,    // endian
			    AudioFormat.SIGNED),
	    new AudioFormat(AudioFormat.MPEG,
			    44100., 
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, 
			    Format.NOT_SPECIFIED,    // endian
			    AudioFormat.SIGNED),
	    new AudioFormat(AudioFormat.MPEG,
			    48000., 
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, 
			    Format.NOT_SPECIFIED,    // endian
			    AudioFormat.SIGNED), 
	};
    }

    /**
     * Method declaration
     *
     *
     * @return
     *
     * @see
     */
    public String getName() {
	return "MPEG Layer 3 Decoder";
    } 

    /**
     * Method declaration
     *
     *
     * @param input
     *
     * @return
     *
     * @see
     */
    public Format[] getSupportedOutputFormats(Format input) {
	if (input == null) {
	    return new Format[] {
		new AudioFormat(AudioFormat.LINEAR)
	    };
	} else if (input instanceof AudioFormat) {
	    AudioFormat af = (AudioFormat) input;
	    outputFormats = new Format[] {
		new AudioFormat(AudioFormat.LINEAR,
				af.getSampleRate(), 
				af.getSampleSizeInBits(),
				af.getChannels(),
				AudioFormat.BIG_ENDIAN,
				/*Arch.isBigEndian()
				? AudioFormat.BIG_ENDIAN 
				: AudioFormat.LITTLE_ENDIAN, */
				AudioFormat.SIGNED)
	    };
	} else {
	    outputFormats = new Format[0];
	}
	return outputFormats;
    } 

    /**
     * Method declaration
     *
     *
     * @throws ResourceUnavailableException
     *
     * @see
     */
    public synchronized void open() throws ResourceUnavailableException {
	if (decoder != null) {
	    close();
	} 

	try {
	    decoder = new codecLib.mpa.Decoder();
	    pendingDataSize = 0;
	    expectingSameInputBuffer = false;
	    accumTS = 0;
	    aFormat = (AudioFormat)outputFormat;
	    // System.err.println("Input format is " + inputFormat);
	    // System.err.println("Output format is " + outputFormat);
	    return;
	} catch (Throwable e) {
	    System.out.println("mpa JavaDecoder: open " + e);
	} 

	throw new ResourceUnavailableException("could not open " + getName());
    } 

    /**
     * Method declaration
     *
     *
     * @see
     */
    public synchronized void close() {
	if (decoder != null) {
	    decoder = null;
	} 
	if (info != null) {
	    info = null;
	} 
    } 

    /**
     * Method declaration
     *
     *
     * @see
     */
    public synchronized void reset() {
	if (decoder != null) {
	    close();

	    try {
		open();
	    } catch (ResourceUnavailableException rue) {
		System.err.println("MP3 Decoder: " + rue);
	    } 
	} 
    } 

    float[][] fsamp = 
	new float[codecLib.mpa.Decoder.MAX_CHANNELS][codecLib.mpa.Decoder.SAMPLES_IN_CHANNEL];
    int[]     fsampOffset = new int[codecLib.mpa.Decoder.MAX_CHANNELS];
    int MAXOUTFRAMESIZE = codecLib.mpa.Decoder.MAX_CHANNELS
			* codecLib.mpa.Decoder.SAMPLES_IN_CHANNEL
			* 2;
    int MIMINFRAMESIZE = codecLib.mpa.Decoder.MIN_BYTES_IN_FRAME;
    int outFrameSize = 0;

    /**
     * Method declaration
     *
     *
     * @param in
     * @param out
     *
     * @return
     *
     * @see
     */
    public synchronized int process(Buffer in, Buffer out) {
	if (isEOM(in)) {
	    propagateEOM(out);
	    return BUFFER_PROCESSED_OK;
	} 

	Object inObject = in.getData();
	Object outObject = out.getData();

	if (outObject == null) {
	    outObject = new byte[OUTSIZE];
	    out.setData(outObject);
	} 

	if (!(inObject instanceof byte[]) ||
	    !(outObject instanceof byte[])) {
	    return BUFFER_PROCESSED_FAILED;
	} 

	byte[] inData = (byte[])inObject;
	byte[] outData = (byte[])outObject;
	int    inLength = in.getLength();
	int    inOffset = in.getOffset();
	int    outDataSize = outData.length;
	int    outOffset = 0;
	int    pendingDataOffset = 0;
	int    byteCount = 0;

	if (!expectingSameInputBuffer) {
	    if ((pendingDataSize + inLength) <= pendingData.length) {
		System.arraycopy(inData, inOffset,
				 pendingData, pendingDataSize, 
				 inLength);
		pendingDataSize += inLength;
	    }
	} 

	if (decoder != null) {
	    while (true) {

		// Does the output buffer have enough space left?
		if ((outDataSize - outOffset) < MAXOUTFRAMESIZE) {
		    break;
		} 

		// Does the input buffer have enough data?
		if (pendingDataSize < MIMINFRAMESIZE) {
		    break;
		} 

		if (info == null) {
		    info = new codecLib.mpa.FrameInfo();
		    try {
			decoder.getNextFrameInfo(info,
						 pendingData, 
						 pendingDataOffset, 
						 pendingDataSize);
			outFrameSize = info.getNumberOfSamples()
				     * info.getNumberOfChannels()
				     * 2;
		    } catch (codecLib.mpa.MPADException e) {
		        //System.out.println("mpa JavaDecoder: getNextFrameInfo " + e);
			info = null;
			break;
		    } 
		}

		try {
		    byteCount = decoder.decode(fsamp, fsampOffset, 
					       pendingData, 
					       pendingDataOffset, 
					       pendingDataSize);
		} catch (codecLib.mpa.MPADException e) {

		    if (e.getState() == codecLib.mpa.Constants.ERR_NOSUPPORT)
			return BUFFER_PROCESSED_FAILED;

		    try {
			decoder.getCurrFrameInfo(info);
		    } catch (codecLib.mpa.MPADException e2) {
			//System.out.println("mpa JavaDecoder: getCurrFrameInfo " + e2);
			info = null;
			break;
		    }

		    if (e.getState() == codecLib.mpa.Constants.ERR_NOPREV) { 
			byteCount = info.getHeaderOffset() + info.getFrameLength();
			pendingDataOffset += byteCount;
			pendingDataSize -= byteCount;
			continue;
		    }

		    //System.out.println("mpa JavaDecoder: decode " + e);
		    info = null;
		    break;
		} 

		if (info.getNumberOfChannels() == 1) {
		    codecLib.mpa.OutputConverter.convert(outData, outOffset, 
							 fsamp[0], fsampOffset[0], 
							 info.getNumberOfSamples());
		} else {
		    codecLib.mpa.OutputConverter.convert(outData, outOffset,
							 fsamp[0], fsampOffset[0],
							 fsamp[1], fsampOffset[1],
							 info.getNumberOfSamples());
		} 

		outOffset += outFrameSize;
		pendingDataOffset += byteCount;
		pendingDataSize -= byteCount;
	    } 
	} 

	// Move the last chunk to the beginning of the pendingData buffer
	if (pendingDataOffset != 0) {
	    System.arraycopy(pendingData, pendingDataOffset,
			     pendingData, 0, 
			     pendingDataSize);
	} 

	out.setLength(outOffset);
	out.setFormat(outputFormat);
	if (aFormat != null && accumTS != 0 && in.getTimeStamp() > 0)
	    out.setTimeStamp(in.getTimeStamp() + aFormat.computeDuration(accumTS));


	if (pendingDataSize > 1024) {
	    expectingSameInputBuffer = true;
	    accumTS += out.getLength();
	    return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
	} else {
	    accumTS = 0;
	    expectingSameInputBuffer = false;
	    return BUFFER_PROCESSED_OK;
	} 
    } 
}
