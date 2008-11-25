/*
 * @(#)YUVToRGB.java	1.37 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.video.colorspace;

import javax.media.*;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;
import com.sun.media.util.*;
import com.sun.media.BasicCodec;
import com.sun.media.NBA;
import java.awt.Dimension;
import java.awt.Component;
import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;
import javax.media.control.FrameProcessingControl;

/**
 * A Codec to convert YUVFormat buffer to RGBFormat buffer.
 */
public class YUVToRGB extends BasicCodec {

    // Have we loaded the native library?
    private static boolean loaded = false;

    // Native structure pointer
    private int peer = 0;

    // The default input and output formats
    protected Format [] defInputFormats;
    protected Format [] defOutputFormats;

    private YUVFormat inputFormat = null;
    private RGBFormat outputFormat = null;

    private FrameProcessingControl frameControl = null;
    private boolean dropFrame;

    /* Initialize the default formats */
    public YUVToRGB() {
	defInputFormats = new Format[1];
	defInputFormats[0] = new YUVFormat();
	defOutputFormats = new Format[1];
	defOutputFormats[0] = new RGBFormat();

	// Create frame dropping control
	if (frameControl == null) {
	    class FPC implements FrameProcessingControl {
		public void setFramesBehind(float frames) {
		    if (frames > 0) {
			dropFrame = true;
		    } else {
			dropFrame = false;
		    }
		}

		public boolean setMinimalProcessing(boolean minimal) {
		    dropFrame = minimal;
		    return dropFrame;
		}

		public Component getControlComponent() {
		    return null;
		}

                public int getFramesDropped() {
                    return 0;       ///XXX not implemented
                }

	    }

	    frameControl = new FPC();
	    controls = new Control[1];
	    controls[0] = frameControl;
	}
    }

    protected Format getInputFormat() {
	return inputFormat;
    }

    protected Format getOutputFormat() {
	return outputFormat;
    }

    /**
     * Return the list of format types that can be accepted as input.
     */
    public Format [] getSupportedInputFormats() {
	return defInputFormats;
    }

    /**
     * Return the list of formats supported at the output.
     */
    public Format [] getSupportedOutputFormats(Format in) {

	if (in == null)
	    return defOutputFormats;
	else if (in instanceof YUVFormat) {
	    YUVFormat yuvIn = (YUVFormat) in;
	    Dimension size = yuvIn.getSize();
	    int lineStride = 0;
	    if (size != null) {
		lineStride = size.width;
		if ((lineStride & 1) != 0)
		    lineStride ++;
	    }

	    RGBFormat rgbOut [] = null;

	    if (Arch.getAlignment() == 1) {
		rgbOut = new RGBFormat[6];

		// 15bit
		rgbOut[0] = new RGBFormat(size,
					  lineStride * size.height,
					  Format.shortArray,
					  yuvIn.getFrameRate(),	       
					  16,
					  0x1F << 10,
					  0x1F << 5,
					  0x1F,
					  1, lineStride,
					  Format.FALSE, // flipped
					  Format.NOT_SPECIFIED // endian
					  );

		// 16bit
		rgbOut[1] = new RGBFormat(size,
					  lineStride * size.height,
					  Format.shortArray,
					  yuvIn.getFrameRate(),			       
					  16,
					  0x1F << 11,
					  0x3F << 5,
					  0x1F,
					  1, lineStride,
					  Format.FALSE, // flipped
					  Format.NOT_SPECIFIED // endian
					  );

		// 24bit
		rgbOut[2] = new RGBFormat(size,
					  lineStride * size.height * 3 + 1,
					  Format.byteArray,
					  yuvIn.getFrameRate(),			       
					  24,
					  1, 2, 3,
					  3, lineStride * 3,
					  Format.FALSE, // flipped
					  Format.NOT_SPECIFIED // endian
					  );

		rgbOut[3] = new RGBFormat(size,
					  lineStride * size.height * 3 + 1,
					  Format.byteArray,
					  yuvIn.getFrameRate(),	       
					  24,
					  3, 2, 1,
					  3, lineStride * 3,
					  Format.FALSE, // flipped
					  Format.NOT_SPECIFIED // endian
					  );

		// 32bit
		rgbOut[4] = new RGBFormat(size,
					  lineStride * size.height,
					  Format.intArray,
					  yuvIn.getFrameRate(),			       
					  32,
					  0xFF0000, 0xFF00, 0xFF,
					  1, lineStride,
					  Format.FALSE, // flipped
					  Format.NOT_SPECIFIED // endian
					  );
		// 32bit
		rgbOut[5] = new RGBFormat(size,
					  lineStride * size.height,
					  Format.intArray,
					  yuvIn.getFrameRate(),			       
					  32,
					  0xFF, 0xFF00, 0xFF0000,
					  1, lineStride,
					  Format.FALSE, // flipped
					  Format.NOT_SPECIFIED // endian
					  );
	    } else {
		rgbOut = new RGBFormat[2];
		// 32 bit only
		rgbOut[0] = new RGBFormat(size,
					  lineStride * size.height,
					  Format.intArray,
					  yuvIn.getFrameRate(),			       
					  32,
					  0xFF0000, 0xFF00, 0xFF,
					  1, lineStride,
					  Format.FALSE, // flipped
					  Format.NOT_SPECIFIED // endian
					  );
		// 32 bit only
		rgbOut[1] = new RGBFormat(size,
					  lineStride * size.height,
					  Format.intArray,
					  yuvIn.getFrameRate(),			       
					  32,
					  0xFF, 0xFF00, 0xFF0000,
					  1, lineStride,
					  Format.FALSE, // flipped
					  Format.NOT_SPECIFIED // endian
					  );
	    }
	    return rgbOut;
	} else
	    return new Format[0];
    }

    public Format setInputFormat(Format in) {
	boolean formatChanged = false;
	
	if (!(in instanceof YUVFormat))
	    return null;

	YUVFormat yuv = (YUVFormat) in;
	Dimension size = yuv.getSize();
	int yStride = yuv.getStrideY();
	int uvStride = yuv.getStrideUV();
	int type = yuv.getYuvType();
	Class bufType = yuv.getDataType();
	int offsetY = yuv.getOffsetY();
	int offsetU = yuv.getOffsetU();
	int offsetV = yuv.getOffsetV();

	if (size == null || size.width < 1 || size.height < 1 ||
	    (type & YUVFormat.YUV_411) != 0 || bufType != Format.byteArray)
	    return null;

	if ((type & YUVFormat.YUV_YUYV) != 0 && (offsetY == offsetU || offsetY == offsetV ||
						 offsetV == offsetU))
	    return null;

	// TODO : Make other safety checks

	if (peer != 0) {
	    formatChanged = true;
	    close();
	}
	
	inputFormat = yuv;

	// If there is an outputFormat size change, we'll need to
	// update the output format too.
	if (outputFormat != null && size != null &&
	    (!size.equals(outputFormat.getSize()) || formatChanged)) {

	    int lineStride = size.width;
	    if ((lineStride & 1) != 0)
		lineStride ++;

	    lineStride *= outputFormat.getPixelStride();
	    outputFormat = new RGBFormat(size,
					 lineStride * size.height + 4,
					 outputFormat.getDataType(),
					 outputFormat.getFrameRate(),
					 outputFormat.getBitsPerPixel(),
					 outputFormat.getRedMask(),
					 outputFormat.getGreenMask(),
					 outputFormat.getBlueMask(),
					 outputFormat.getPixelStride(),
					 lineStride,
					 outputFormat.getFlipped(),
					 outputFormat.getEndian());
	}

	if (formatChanged) {
	    try {
		open();
	    } catch (ResourceUnavailableException rue) {
		return null;
	    }
	}
	return in;
    }

    public Format setOutputFormat(Format out) {
	if (!(out instanceof RGBFormat))
	    return null;

	RGBFormat rgb = (RGBFormat) out;
	Dimension outSize = rgb.getSize();
	int pixelStride = rgb.getPixelStride();
	int lineStride = rgb.getLineStride();

	if (outSize == null ||
	    pixelStride < 1 || lineStride < outSize.width)
	    return null;

	// TODO : Make other safety checks

	outputFormat = rgb;
	return out;
    }

    public int process(Buffer inBuffer, Buffer outBuffer) {
	if (isEOM(inBuffer)) {
	    propagateEOM(outBuffer);
	    return BUFFER_PROCESSED_OK;
	}

	if (inBuffer.isDiscard() || dropFrame == true) {
	    outBuffer.setDiscard(true);
	    return BUFFER_PROCESSED_OK;
	}

	long inDataBytes = 0;
	long outDataBytes = 0;
	Object inData = null;
	Object outData = null;
	Format inFormat = inBuffer.getFormat();
	if (inFormat != inputFormat && !(inFormat.equals(inputFormat))) {
	    setInputFormat(inFormat);
	}

	inData = getInputData(inBuffer);
	inDataBytes = getNativeData(inData);

	outData = getOutputData(outBuffer); 

	if (  outData == null ||
	      outBuffer.getFormat() != outputFormat ||
	      !outBuffer.getFormat().equals(outputFormat) ) {
	    RGBFormat rgb = (RGBFormat) outputFormat;
	    int stride = rgb.getLineStride();
	    int dataSize = rgb.getSize().height * stride + 1;
	    Class dataType = rgb.getDataType();
	    /*
	    if (dataType == Format.byteArray)
		outData = new byte[dataSize + 1]; // add 1 to workaround IV32
	                                          // crash. typecasting to int
	                                          // causes overwriting of 1
	                                          // byte past array.

	    else if (dataType == Format.shortArray)
		outData = new short[dataSize];
	    else if (dataType == Format.intArray)
		outData = new int[dataSize];
	    else
		return BUFFER_PROCESSED_FAILED;
	    outBuffer.setData(outData);
	    */
	    //System.err.println("dataSize " + dataSize);
	    outBuffer.setLength(dataSize);
	    outBuffer.setFormat(outputFormat);

	}
	
	outData = validateData(outBuffer, 0, true);
	outDataBytes = getNativeData(outData);

	if (inBuffer.getLength() < 10) {
	    outBuffer.setDiscard(true);
	    return BUFFER_PROCESSED_OK;
	}

	int inWidth = inputFormat.getStrideY();
	int strideUV = inputFormat.getStrideUV();
	int inHeight = inputFormat.getSize().height;
	int offsetY = inputFormat.getOffsetY();
	int offsetU = inputFormat.getOffsetU();
	int offsetV = inputFormat.getOffsetV();
	int outWidth = outputFormat.getLineStride();
	int outHeight = outputFormat.getSize().height;
	int decimation = inputFormat.getYuvType();
	int clipWidth = inputFormat.getSize().width;
	int clipHeight = inputFormat.getSize().height;
	
	// Translate to native decimation code
	switch (decimation & ~YUVFormat.YUV_SIGNED) {
	case YUVFormat.YUV_420:
	    decimation = 1; break;
	case YUVFormat.YUV_422:
	    decimation = 2; break;
	case YUVFormat.YUV_111:
	    decimation = 4; break;
	case 512: // Indeo 3.2
	    decimation = 5; break;
	case YUVFormat.YUV_YVU9:
	    decimation = 3; break;
	case YUVFormat.YUV_YUYV:
	    decimation = 6; break;
	}

	if (outputFormat.getBitsPerPixel() == 24)
	    outWidth /= 3;

	boolean result = convert(peer,
				 inData,
				 inDataBytes,
				 outData,
				 outDataBytes,
				 inWidth, inHeight,
				 outWidth, outHeight,
				 clipWidth, clipHeight,
				 offsetY, offsetU, offsetV,
				 inWidth, strideUV,
				 decimation,
				 outputFormat.getBitsPerPixel() / 8);

	if (result) {
	    outBuffer.setTimeStamp(inBuffer.getTimeStamp());
	    outBuffer.setLength(outputFormat.getLineStride() *
			      outputFormat.getSize().height);
	    return BUFFER_PROCESSED_OK;
	} else
	    return BUFFER_PROCESSED_FAILED;
    }

    public synchronized void open() throws ResourceUnavailableException {
	if (!loaded) {
	    try {
		JMFSecurityManager.loadLibrary("jmutil");
		loaded = true;
	    } catch (UnsatisfiedLinkError e) {
		throw new ResourceUnavailableException(e.getMessage());
	    }
	}
	if (inputFormat == null || outputFormat == null)
	    throw new ResourceUnavailableException("Incorrect formats set on YUVToRGB converter");

	if (peer != 0)
	    close();
	
	try {
	    int redMask = outputFormat.getRedMask();
	    int greenMask = outputFormat.getGreenMask();
	    int blueMask = outputFormat.getBlueMask();
	    int bitsPerPixel = outputFormat.getBitsPerPixel();
	    if (bitsPerPixel == 24 && outputFormat.getDataType() == Format.byteArray) {
		redMask = 0xFF   << ((redMask - 1) * 8);
		greenMask = 0xFF << ((greenMask - 1) * 8);
		blueMask = 0xFF  << ((blueMask - 1) * 8);
	    }
	    
	    peer = initConverter(redMask, greenMask, blueMask, bitsPerPixel,
				 (inputFormat.getYuvType() & YUVFormat.YUV_SIGNED) != 0);
	    
	} catch (Throwable t) {
	}
	
	if (peer == 0)
	    throw new ResourceUnavailableException("Unable to initialize YUVToRGB converter");
    }

    public synchronized void close() {
	if (peer != 0)
	    freeConverter(peer);
	peer = 0;
    }

    public synchronized void reset() {
	//close();
	//open();
    }

    public void finalize() {
	close();
    }

    public String getName() {
	return "YUV To RGB Converter";
    }

    private native int initConverter(int redMask, int greenMask, int blueMask,
				     int depth, boolean signed);

    private native boolean convert(int peer,
				   Object inData,
				   long inDataBytes,
				   Object outData,
				   long outDataBytes,
				   int inWidth, int inHeight,
				   int outWidth, int outHeight,
				   int clipWidth, int clipHeight,
				   int offsetY, int offsetU, int offsetV,
				   int strideY, int strideUV,
				   int decimation,
				   int bytesPerPixel);

    private native boolean freeConverter(int peer);

}

