/*
 * @(#)NativeDecoder.java	1.12 00/11/06
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

package com.sun.media.codec.video.jpeg;

import javax.media.*;
import javax.media.Format;
import javax.media.format.*;
import javax.media.control.*;

import java.awt.Dimension;
import java.awt.Component;

import com.sun.media.*;
import com.sun.media.util.*;

public final class NativeDecoder extends BasicCodec {

    // I/O formats
    private VideoFormat inputFormat = null;
    private RGBFormat  outputFormat = null;

    // Have we loaded the native library?
    private static boolean loaded = false;

    // Assume we can load it.
    private static boolean canLoad = true;

    // Pointer to native structure
    private int peer = 0;

    int returnVal = 0;

    private boolean dropFrame = false;
    private boolean minimal = false;
    private int decimation = -1;

    /****************************************************************
     * Codec Methods
     ****************************************************************/

    // Initialize default formats.
    public NativeDecoder() {
	inputFormats = new VideoFormat[2];
	inputFormats[0] = new VideoFormat(VideoFormat.JPEG);
	inputFormats[1] = new VideoFormat(VideoFormat.MJPG);
	outputFormats = new RGBFormat[1];
	outputFormats[0] = new RGBFormat();

	FrameProcessingControl fpc = new FrameProcessingControl() {
	    public boolean setMinimalProcessing(boolean newMinimal) {
		minimal = newMinimal;
		return minimal;
	    }

	    public void setFramesBehind(float frames) {
		if (frames >= 1)
		    dropFrame = true;
		else
		    dropFrame = false;
	    }

	    public Component getControlComponent() {
		return null;
	    }

            public int getFramesDropped() {
                return 0;       ///XXX not implemented
            }

	};

	controls = new Control[1];
	controls[0] = fpc;
    }

    protected Format getInputFormat() {
	return inputFormat;
    }

    protected Format getOutputFormat() {
	return outputFormat;
    }

    // Return supported output formats
    public Format [] getSupportedOutputFormats(Format in) {
	if (in == null)
	    return outputFormats;

	// Make sure the input is RGB video format
	if (!verifyInputFormat(in))
	    return new Format[0];

	return computeOutputFormats(in);
    }

    private boolean verifyInputFormat(Format input) {
	if (!(input instanceof VideoFormat))
	    return false;
	if (input.getEncoding().equalsIgnoreCase(VideoFormat.JPEG) ||
	    input.getEncoding().equalsIgnoreCase(VideoFormat.MJPG) )
	    return true;
	return false;
    }

    public Format setInputFormat(Format input) {
	if (!verifyInputFormat(input))
	    return null;
	inputFormat = (VideoFormat) input;
	if (opened) {
	    close();
	    outputFormat = updateRGBFormat(inputFormat, outputFormat);
	}
	return input;
    }

    public Format setOutputFormat(Format output) {
	if (matches(output, outputFormats) == null){
	    return null;
	}
	outputFormat = (RGBFormat) output;
	return output;
    }

    private final VideoFormat[] computeOutputFormats(Format in) {
	// Calculate the properties
	VideoFormat jpeg = (VideoFormat) in;
	Dimension size = jpeg.getSize();
	if (size == null)
	    size = new Dimension(320, 240);
	int area = ((size.width + 7) & ~7) * ((size.height + 7) & ~7);
	RGBFormat [] rgb = new RGBFormat[] {
	    new RGBFormat(size,
			  area * 3,
			  Format.byteArray,
			  jpeg.getFrameRate(),
			  24,
			  1, 2, 3,
			  3, size.width * 3,
			  RGBFormat.TRUE,
			  Format.NOT_SPECIFIED),
	    new RGBFormat(size,
			  area * 3,
			  Format.byteArray,
			  jpeg.getFrameRate(),
			  24,
			  3, 2, 1,
			  3, size.width * 3,
			  RGBFormat.TRUE,
			  Format.NOT_SPECIFIED),
	    new RGBFormat(size,
			  area,
			  Format.intArray,
			  jpeg.getFrameRate(),
			  32,
			  0xFF0000, 0xFF00, 0xFF,
			  1, size.width,
			  RGBFormat.TRUE,
			  Format.NOT_SPECIFIED),
	    new RGBFormat(size,
			  area,
			  Format.intArray,
			  jpeg.getFrameRate(),
			  32,
			  0xFF, 0xFF00, 0xFF0000,
			  1, size.width,
			  RGBFormat.TRUE,
			  Format.NOT_SPECIFIED),
	    new RGBFormat(size,
			  area * 3,
			  Format.byteArray,
			  jpeg.getFrameRate(),
			  24,
			  1, 2, 3,
			  3, size.width * 3,
			  RGBFormat.FALSE,
			  Format.NOT_SPECIFIED),
	    new RGBFormat(size,
			  area * 3,
			  Format.byteArray,
			  jpeg.getFrameRate(),
			  24,
			  3, 2, 1,
			  3, size.width * 3,
			  RGBFormat.FALSE,
			  Format.NOT_SPECIFIED),
	    new RGBFormat(size,
			  area,
			  Format.intArray,
			  jpeg.getFrameRate(),
			  32,
			  0xFF0000, 0xFF00, 0xFF,
			  1, size.width,
			  RGBFormat.FALSE,
			  Format.NOT_SPECIFIED),
	    new RGBFormat(size,
			  area,
			  Format.intArray,
			  jpeg.getFrameRate(),
			  32,
			  0xFF, 0xFF00, 0xFF0000,
			  1, size.width,
			  RGBFormat.FALSE,
			  Format.NOT_SPECIFIED)
	};
	return rgb;
    }

    public void open() throws ResourceUnavailableException {
	if (!canLoad)
	    throw new ResourceUnavailableException("Unable to load" +
						   " native JPEG converter");

	if (!loaded) {
	    try {
		JMFSecurityManager.loadLibrary( "jmutil");
		JMFSecurityManager.loadLibrary( "jmjpeg");
		loaded = true;
	    } catch (Throwable t) {
		canLoad = false;
		throw new ResourceUnavailableException("Unable to load " +
						       "native JPEG decoder");
	    }
	}

	if (inputFormat == null || outputFormat == null)
	    throw new ResourceUnavailableException("Formats not set " +
						   "on the JPEG decoder");

	if (peer != 0)
	    close();

	Dimension size = inputFormat.getSize();
	
	try {
	    peer = initJPEGDecoder(size.width, size.height);
	} catch (Throwable t) {
	}
	
	if (inputFormat instanceof JPEGFormat) {
	    decimation = ((JPEGFormat)inputFormat).getDecimation();
	}
	
	if (peer == 0)
	    throw new ResourceUnavailableException("Unable to initialize JPEG decoder");
	super.open();
    }

    public synchronized void close() {
	if (peer != 0)
	    freeJPEGDecoder(peer);
	peer = 0;
	super.close();
    }

    public void reset() {
	// Anything to do?
    }

    public synchronized int process(Buffer inBuffer, Buffer outBuffer) {
	Object header = null;
	Format inFormat;
	Format outFormat = null;
	byte [] inData;
	boolean flipped;

	// EndOfMedia?
	if (isEOM(inBuffer)) {
	    propagateEOM(outBuffer);
	    return BUFFER_PROCESSED_OK;
	}

	// Dropping frames?
	if (minimal || dropFrame) {
	    outBuffer.setFlags(outBuffer.getFlags() | Buffer.FLAG_DISCARD);
	    return BUFFER_PROCESSED_OK;
	}
	
	inFormat = inBuffer.getFormat();
	inData = (byte[]) inBuffer.getData();

	if (inBuffer.getLength() < 1)
	    return BUFFER_PROCESSED_OK;

	if (!inFormat.equals(inputFormat)) {
	    setInputFormat(inFormat);
	    close();
	}

	if (outFormat == null) {
	    outBuffer.setFormat(outputFormat);
	    outFormat = outputFormat;
	}

	Object outData = validateData(outBuffer, 0, true);

	flipped = ((RGBFormat)outFormat).getFlipped() == RGBFormat.TRUE;
	if (peer == 0) {
	    try {
		open();
	    } catch (ResourceUnavailableException re) {
		return BUFFER_PROCESSED_FAILED;
	    }
	}

	Dimension size = inputFormat.getSize();
	synchronized (NativeEncoder.processLock) {
	    if (outData instanceof byte[]) {
		returnVal =
		    decodeJPEGToByte(peer,
				     inData,
				     inBuffer.getLength(),
				     size.width,
				     size.height,
				     (byte[]) outData,
				     outputFormat.getMaxDataLength(),
				     flipped,
				     outputFormat.getRedMask(),
				     outputFormat.getGreenMask(),
				     outputFormat.getBlueMask(),
				     outputFormat.getBitsPerPixel());
		
		outBuffer.setLength(size.width * size.height *
				    outputFormat.getBitsPerPixel() / 8);
	    } else if (outData instanceof int[]) {
		returnVal =
		    decodeJPEGToInt(peer,
				    inData,
				    inBuffer.getLength(),
				    size.width,
				    size.height,
				    (int[]) outData,
				    outputFormat.getMaxDataLength(),
				    flipped,
				    outputFormat.getRedMask(),
				    outputFormat.getGreenMask(),
				    outputFormat.getBlueMask(),
				    outputFormat.getBitsPerPixel());
		outBuffer.setLength(size.width * size.height);
	    } else if (outData instanceof NBA) {
		NBA nba = (NBA) outData;
		returnVal =
		    decodeJPEGToNBA(peer,
				    inData,
				    inBuffer.getLength(),
				    size.width,
				    size.height,
				    nba.getNativeData(),
				    outputFormat.getMaxDataLength(),
				    flipped,
				    outputFormat.getRedMask(),
				    outputFormat.getGreenMask(),
				    outputFormat.getBlueMask(),
				    outputFormat.getBitsPerPixel());
		outBuffer.setLength(size.width * size.height);
		if (outputFormat.getDataType() == Format.byteArray)
		    outBuffer.setLength(outBuffer.getLength() *
					outputFormat.getBitsPerPixel() / 8);
	    }
	}
	if (returnVal > 0) {
	    outBuffer.setOffset(0);
	    inBuffer.setLength(0);
	    outBuffer.setFlags(outBuffer.getFlags() | Buffer.FLAG_KEY_FRAME);
	    outBuffer.setTimeStamp(inBuffer.getTimeStamp());
	    return BUFFER_PROCESSED_OK;
	}
	outBuffer.setDiscard(true);
	return BUFFER_PROCESSED_FAILED;
    }// end of process()

    protected void finalize() {
	close();
    }

    public String getName() {
	return "JPEG Decoder";
    }

    /****************************************************************
     * Native Methods
     ****************************************************************/

    // Initializes the native decoder
    private native int initJPEGDecoder(int width, int height);
    
    /*
     * Decodes the JPEG data and returns the output length (positive)
     * Returns zero if it couldn't decode, or a negative value to indicate
     * the error.
     */
    private native int decodeJPEGToByte(int peer, byte [] inData, int inLength,
					int width, int height,
					byte [] outData, int outLength,
					boolean flipped,
					int red, int green, int blue,
					int bitsPerPixel);

    /*
     * Decodes the JPEG data and returns the output length (positive)
     * Returns zero if it couldn't decode, or a negative value to indicate
     * the error.
     */
    private native int decodeJPEGToInt(int peer, byte [] inData, int inLength,
					int width, int height,
					int [] outData, int outLength,
					boolean flipped,
					int red, int green, int blue,
					int bitsPerPixel);
    /*
     * Decodes the JPEG data and returns the output length (positive)
     * Returns zero if it couldn't decode, or a negative value to indicate
     * the error.
     */
    private native int decodeJPEGToNBA(int peer, byte [] inData, int inLength,
					int width, int height,
					long outData, int outLength,
					boolean flipped,
					int red, int green, int blue,
					int bitsPerPixel);

    // Frees any native structures
    private native boolean freeJPEGDecoder(int peer);

}





