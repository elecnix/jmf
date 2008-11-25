/*
 * @(#)NativeEncoder.java	1.41 00/11/06
 *
 * Copyright 1998 by Sun Microsystems, Inc.,
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
import com.sun.media.format.AviVideoFormat;
import com.sun.media.controls.QualityAdapter;

import javax.media.rtp.RTPHeader;

public class NativeEncoder extends BasicCodec {

    // I/O formats
    private RGBFormat inputFormat = null;
    private VideoFormat outputFormat = null;

    // Have we loaded the native library?
    private static boolean loaded = false;

    // Assume we can load it.
    private static boolean canLoad = true;

    // Pointer to native structure
    private int peer = 0;

    private static int QSCALE = 99;
    private float quality = 0.6f;
    private float prevQuality = 0;
    private boolean isMotionJPEG = false;
    private boolean firstFrame = true;
    
    // default packet size of JPEG payload for RTP. can be changed
    // using encoding controls ?
    private int PACKET_SIZE = 960;
    
    // current sequence number on RTP format packets.
    private int currentSeq =  (int) System.currentTimeMillis();
    
    // current timestamp on RTP format packets.
    private long timestamp = (long) (System.currentTimeMillis() * Math.random());

    // output buffer in which encoded JPEG RTP buffer is stored
    // data will be copied from this buffer into output buffer until
    // entire compressed RTP_JPEG frame has been packetized into
    // smaller RTP packets.  
    private byte[] rtp_data = null;

    static Integer processLock = new Integer(1);

    // next 4 varaibles used for RTP packetization
    private int copyLength = PACKET_SIZE;

    private boolean newframe = true;

    private int current_offset = 0;
    
    int returnVal = 0;

    private boolean dropFrame = false;
    private boolean minimal = false;

    // default frame rate for RTP JPEG format. Should normally be
    // specified when frame rate is set.
    private static final int DEFAULT_FRAMERATE = 15;

    /****************************************************************
     * Codec Methods
     ****************************************************************/

    // Initialize default formats.
    public NativeEncoder() {
	inputFormats = new RGBFormat[1];
	inputFormats[0] = new RGBFormat();
	outputFormats = new VideoFormat[2];
	outputFormats[0] = new JPEGFormat();
	outputFormats[1] = new VideoFormat(VideoFormat.MJPG);
	class QCA extends QualityAdapter implements Owned {
	    public QCA() {
		super(0.6f, 0f, 1f, true);
	    }
	    
	    public float setQuality(float newValue) {
		quality = super.setQuality(newValue);
		return quality;
	    }

	    public String getName() {
		return "JPEG Quality";
	    }

	    public Object getOwner() {
		return NativeEncoder.this;
	    }
	}

	QualityControl qualityControl = new QCA();

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

	controls = new Control[2];
	controls[0] = qualityControl;
	controls[1] = fpc;
    }

    protected Format getInputFormat() {
	return inputFormat;
    }

    protected Format getOutputFormat() {
	return outputFormat;
    }

    private byte [] mjpgExtraBytes = new byte[] {
	44, 0, 0, 0,
	24, 0, 0, 0,
	0,  0, 0, 0,
	2,  0, 0, 0,
	8,  0, 0, 0,
	2,  0, 0, 0,
	1,  0, 0, 0,
    };

    protected float qfToQuality(float quality) {
	if (quality < 0.1f)
	    quality = 0.1f;
	if (isMotionJPEG)
	    return quality * (float) QSCALE;
	if (quality < 0.8f) {
	    return quality * 1.25f * QSCALE;
	} else {
	    return (float) QSCALE;
	}
    }

    protected int qfToDecimation(float quality) {
	if (isMotionJPEG)
	    return 2;
	
	if (quality >= 0.8f) {
	    if (quality >= 0.90f)
		return 4;
	    else
		return 2;
	} else
	    return 1;
    }

    protected int qfToType(float quality) {
	switch (qfToDecimation(quality)) {
	case 1:
	    return JPEGFormat.DEC_420;
	case 2:
	    return JPEGFormat.DEC_422;
	case 4:
	    return JPEGFormat.DEC_444;
	}
	return 1;
    }

    // Return supported output formats
    public Format [] getSupportedOutputFormats(Format in) {
	if (in == null)
	    return outputFormats;

	// Make sure the input is RGB video format
	if (!verifyInputFormat(in))
	    return new Format[0];

	Format out [] = new Format[2];
	RGBFormat rgb = (RGBFormat) in;
	Dimension size = rgb.getSize();
	int maxDataLength = size.width * size.height * 3;
	VideoFormat jpeg = new JPEGFormat(size, maxDataLength,
					  Format.byteArray,
					  rgb.getFrameRate(),
					  // -- hsy for bugid 4414481
					  //(int) (qfToQuality(quality)),
					  //qfToType(quality));
					  Format.NOT_SPECIFIED,
					  Format.NOT_SPECIFIED);
	VideoFormat mjpg = new AviVideoFormat(VideoFormat.MJPG,
					      size, maxDataLength,
					      Format.byteArray,
					      rgb.getFrameRate(),
					      1, 24, maxDataLength,
					      0, 0, 0, 0,
					      mjpgExtraBytes);
	out[0] = jpeg;
	out[1] = mjpg;
	
	return out;
    }

    private boolean verifyInputFormat(Format input) {
	if (!(input instanceof RGBFormat))
	    return false;
	RGBFormat rgb = (RGBFormat) input;
	if ( rgb.getDataType() != Format.byteArray ||
	     rgb.getBitsPerPixel() != 24 ||
	     rgb.getRedMask() != 3 ||
	     rgb.getGreenMask() != 2 ||
	     rgb.getBlueMask() != 1 ||
	     rgb.getSize() == null ||
	     rgb.getLineStride() < rgb.getSize().width ||
	     rgb.getPixelStride() != 3 )
	    return false;
	return true;
    }

    public Format setInputFormat(Format input) {
	if (!verifyInputFormat(input))
	    return null;

	inputFormat = (RGBFormat) input;
	if (opened) {
	    close();
	    Dimension size = inputFormat.getSize();
	    int maxDataLength = size.width * size.height * 3;
	    if (outputFormat instanceof JPEGFormat) 
		outputFormat = new JPEGFormat(size,
					      maxDataLength,
					      Format.byteArray,
					      inputFormat.getFrameRate(),
					      (int) qfToQuality(quality),
					      qfToType(quality));
	    else
		outputFormat = new AviVideoFormat(VideoFormat.MJPG,
						  size, maxDataLength,
						  Format.byteArray,
						  inputFormat.getFrameRate(),
						  1, 24, maxDataLength,
						  0, 0, 0, 0,
						  mjpgExtraBytes);
	}	    
	return input;
    }

    public Format setOutputFormat(Format output) {
	if (matches(output, outputFormats) == null){
	    return null;
	}
	outputFormat = (VideoFormat) output;
	if (outputFormat.getEncoding().equalsIgnoreCase(VideoFormat.MJPG))
	    isMotionJPEG = true;
	else
	    isMotionJPEG = false;
	return output;
    }

    public void open() throws ResourceUnavailableException {
	if (!canLoad)
	    throw new ResourceUnavailableException("Unable to load" +
						   " native JPEG converter");

	// Size restriction - 8x8
	Dimension size = inputFormat.getSize();
	if (size == null || (size.width % 8) != 0 || (size.height % 8) != 0 ) {
	    Log.error("Class: " + this);
	    Log.error("  can only encode in sizes of multiple of 8 pixels.");
	    throw new ResourceUnavailableException("Unable to encode in size " + size);
	}

	if (!loaded) {
	    try {
		JMFSecurityManager.loadLibrary( "jmutil");
		JMFSecurityManager.loadLibrary( "jmjpeg");
		loaded = true;
	    } catch (Throwable t) {
		canLoad = false;
		throw new ResourceUnavailableException("Unable to load " +
						       "native JPEG encoder");
	    }
	}

	if (inputFormat == null || outputFormat == null)
	    throw new ResourceUnavailableException("Formats not set " +
						   "on the JPEG encoder");

	if (peer != 0)
	    close();
	try {
	    peer = initJPEGEncoder(size.width, size.height,
				   (int) qfToQuality(quality),
				   qfToDecimation(quality));
	    firstFrame = true;
	} catch (Throwable t) {
	}
	
	if (peer == 0)
	    throw new ResourceUnavailableException("Unable to initialize JPEG encoder");
	super.open();
    }

    public synchronized void close() {
	if (peer != 0)
	    freeJPEGEncoder(peer);
	peer = 0;
	super.close();
    }

    public void reset() {
	// Anything to do?
    }

    public synchronized int process(Buffer inBuffer, Buffer outBuffer) {
	Object header = null;
	float changeQuality;
	int changeDecimation;
	Format inFormat;
	Object inData;
	long inBytes;
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
	inData = getInputData(inBuffer);
	inBytes = getNativeData(inData);
	flipped = ((RGBFormat) inFormat).getFlipped() == Format.TRUE;

	//if (outputFormat.getEncoding().equals(VideoFormat.JPEG)){

	byte [] outData = (byte[]) outBuffer.getData();
	if (outData == null ||
	    outData.length < outputFormat.getMaxDataLength()) {
	    outData = new byte[outputFormat.getMaxDataLength()];
	    outBuffer.setData(outData);
	}

	outBuffer.setFormat(outputFormat);
	    
	if (prevQuality != quality) {
	    prevQuality = quality;
	    changeQuality = qfToQuality(quality);
	    outputFormat = new JPEGFormat(outputFormat.getSize(),
					  outputFormat.getMaxDataLength(),
					  Format.byteArray,
					  outputFormat.getFrameRate(),
					  (int) qfToQuality(quality),
					  qfToType(quality));
	    close();
	    // Allow changing decimation only if this is the first encoded frame
	    if (firstFrame) 
		changeDecimation = qfToDecimation(quality);
	    else
		changeDecimation = -1;
	} else { // dont change quality
	    changeQuality = -1;
	    changeDecimation = -1;
	}

	if (peer == 0) {
	    try {
		open();
	    } catch (ResourceUnavailableException re) {
		return BUFFER_PROCESSED_FAILED;
	    }
	}

	Dimension size = inputFormat.getSize();
	synchronized (processLock) {
	    returnVal =
		encodeJPEG(peer,
			   inData,
			   inBytes,
			   size.width,
			   size.height,
			   outData,
			   outData.length,
			   (int) changeQuality,
			   (int) changeDecimation,
			   flipped);
	}
	firstFrame = false;
	if (returnVal > 0) {
	    outBuffer.setLength(returnVal);
	    outBuffer.setOffset(0);
	    inBuffer.setLength(0);
	    outBuffer.setFlags(Buffer.FLAG_KEY_FRAME);
	    outBuffer.setTimeStamp(inBuffer.getTimeStamp());
	    outBuffer.setFormat(outputFormat);
	    return BUFFER_PROCESSED_OK;
	}
	outBuffer.setDiscard(true);
	return BUFFER_PROCESSED_FAILED;
    }// end of process()

    public void finalize() {
	close();
    }

    public String getName() {
	return "JPEG Encoder";
    }

    /****************************************************************
     * Native Methods
     ****************************************************************/

    // Initializes the native encoder
    // Decimation is 1, 2, or 4 for YUV 4:2:0, 4:2:2 and 4:4:4 resp.
    private native int initJPEGEncoder(int width, int height,
				       int quality, int decimation);
    
    /*
     * Encodes the RGB data and returns the output length (positive)
     * Returns zero if it couldn't encode, or a negative value to indicate
     * the error.
     */
    private native int encodeJPEG(int peer, Object inData, long inBytes,
				  int width, int height,
				  byte [] outData, int length, int quality,
				  int decimation, boolean flipped);

    // Frees any native structures
    private native boolean freeJPEGEncoder(int peer);

    /****************************************************************
     * Test Code
     ****************************************************************/

    public static void main(String [] args) {
	int width = 320;
	int height = 240;
	RGBFormat in = new RGBFormat(new Dimension(width, height),
				     width * height * 3,
				     Format.byteArray,
				     Format.NOT_SPECIFIED, // frame rate
				     24,
				     1, 2, 3, 3, width * 3,
				     Format.FALSE, // flipped
				     Format.NOT_SPECIFIED // endian
				     );
	VideoFormat out = new VideoFormat(VideoFormat.JPEG,
					  new Dimension(width, height),
					  width * height * 3,
					  Format.byteArray,
					  // frame rate
					  Format.NOT_SPECIFIED
					  );

	NativeEncoder e = new NativeEncoder();
	if (e.setInputFormat(in) != null) {
	    if (e.setOutputFormat(out) != null) {
		try {
		    e.open();
		} catch (ResourceUnavailableException rue) {
		    System.err.println("Couldn't open encoder");
		    System.exit(0);
		}

		byte [] rgbData  = new byte[width * height * 3];

		System.err.println("Filling rgb data");
		for (int i = 0; i < width * height * 3; i++)
		    rgbData[i] = (byte) (((i % 3) * (i % width)) % 255);

		System.err.println("Encoding");
		
		Buffer inBuffer = new Buffer();
		inBuffer.setFormat(in);
		inBuffer.setData(rgbData);
		inBuffer.setLength(width * height * 3);

		Buffer outBuffer = new Buffer();
		outBuffer.setFormat(out);

		int result = e.process(inBuffer, outBuffer);
		System.err.println("Result = " + result);
		e.close();
	    }
	
	}

	System.exit(0);
    }
		
}





