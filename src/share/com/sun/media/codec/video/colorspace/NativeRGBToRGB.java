/*
 * @(#)NativeRGBToRGB.java	1.14 03/04/23
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.video.colorspace;

import javax.media.Format;
import javax.media.format.*;
import javax.media.*;
import com.sun.media.util.Arch;
import java.awt.Dimension;
import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;

public class NativeRGBToRGB extends com.sun.media.BasicCodec {

    // Have we loaded the native library?
    private static boolean loaded = false;

    protected int rm1, rm2, gm1, gm2, bm1, bm2;
    
    // Used by native code
    protected int rMask;
    protected int gMask;
    protected int bMask;
    protected int rShift;
    protected int gShift;
    protected int bShift;
    private int conversionFun = 0;

    public NativeRGBToRGB() {

    }

    public String getName() {
	return "RGB To RGB Converter";
    }

    protected boolean supportedRGB(Format format) {
	if (!(format instanceof RGBFormat))
	    return false;

	RGBFormat rgb = (RGBFormat) format;
	int bitsPerPixel = rgb.getBitsPerPixel();
	int pixelStride = rgb.getPixelStride();
	int lineStride = rgb.getLineStride();
	int redMask = rgb.getRedMask();
	int greenMask = rgb.getGreenMask();
	int blueMask = rgb.getBlueMask();
	Dimension size = rgb.getSize();
	Class dataType = rgb.getDataType();
	int elementsPerPixel = 1;

	switch (bitsPerPixel) {
	case 15:
	case 16:
	    if (!( dataType == Format.byteArray ||
		   dataType == Format.shortArray ))
		return false;
	    if (dataType == Format.byteArray)
		elementsPerPixel = 2;
	    break;
	case 24:
	    if (!( dataType == Format.byteArray ))
		return false;
	    elementsPerPixel = 3;
	    break;
	case 32:
	    if (dataType == Format.byteArray)
		elementsPerPixel = 4;
	}

	if (size != null) {
	    if (lineStride < size.width * elementsPerPixel)
		return false;
	    if (pixelStride < elementsPerPixel)
		return false;
	}

	return true;
    }

    public Format [] getSupportedInputFormats() {
	// RGBFormat input = new RGBFormat();
	// return new RGBFormat[] {input};

	// Return only formats known to work at the moment
	RGBFormat [] formats = new RGBFormat[] {
	    new RGBFormat(null, Format.NOT_SPECIFIED,
			  Format.byteArray,
			  Format.NOT_SPECIFIED, // frame rate
			  24,
			  3, 2, 1,
			  3, Format.NOT_SPECIFIED,
			  Format.FALSE, // flipped
			  Format.NOT_SPECIFIED), // endian

	    new RGBFormat(null, Format.NOT_SPECIFIED,
			  Format.byteArray,
			  Format.NOT_SPECIFIED, // frame rate
			  16,
			  0xF800, 0x07E0, 0x001F,
			  2, Format.NOT_SPECIFIED,
			  Format.FALSE, // flipped
			  Format.NOT_SPECIFIED) // endian

	};
	
	return formats;
    }

    public Format [] getSupportedOutputFormats(Format in) {
	if (!(supportedRGB(in)))
	    return new RGBFormat[] { new RGBFormat() };

	RGBFormat rgb = (RGBFormat) in;
	Dimension size = rgb.getSize();

	if (size == null)
	    size = new Dimension(320, 240);
	
	RGBFormat [] formats = new RGBFormat[] {
	    new RGBFormat(size, size.width * size.height,
			  Format.intArray,
			  rgb.getFrameRate(),			       
			  32,
			  0xFF0000, 0x00FF00, 0x0000FF,
			  1, size.width,
			  rgb.getFlipped(),
			  rgb.getEndian()),
	    new RGBFormat(size, size.width * size.height * 4,
			  Format.byteArray,
			  rgb.getFrameRate(),
			  32,
			  2, 3, 4,
			  4, size.width * 4,
			  rgb.getFlipped(),
			  rgb.getEndian()),

	    new RGBFormat(size, size.width * size.height * 3,
			  Format.byteArray,
			  rgb.getFrameRate(),			       
			  24,
			  1, 2, 3,
			  3, size.width * 3,
			  rgb.getFlipped(),
			  rgb.getEndian()),

	    new RGBFormat(size, size.width * size.height,
			  Format.shortArray,
			  rgb.getFrameRate(),			       
			  16,
			  0xF800, 0x07E0, 0x001F,
			  1, size.width,
			  rgb.getFlipped(),
			  rgb.getEndian()),

	    new RGBFormat(size, size.width * size.height,
			  Format.byteArray,
			  rgb.getFrameRate(),			       
			  16,
			  0xF800, 0x07E0, 0x001F,
			  2, size.width * 2,
			  rgb.getFlipped(),
			  rgb.getEndian()),

	    new RGBFormat(size, size.width * size.height,
			  Format.shortArray,
			  rgb.getFrameRate(),			       
			  16,
			  0x7C00, 0x03E0, 0x001F,
			  1, size.width,
			  rgb.getFlipped(),
			  rgb.getEndian())
	};
	
	return formats;
    }


    public void open() throws ResourceUnavailableException {
	// Will throw a ResourceUnavailableException if it couldn't load
	if (!loaded) {
	    try {
		JMFSecurityManager.loadLibrary("jmutil");
		loaded = true;
	    } catch (UnsatisfiedLinkError e) {
		throw new ResourceUnavailableException(e.getMessage());
	    }
	}

	if (!supportedRGB(inputFormat) ||
	    !supportedRGB(outputFormat))
	    throw new ResourceUnavailableException("Invalid Formats");
    }

    protected void initialize() {
	RGBFormat rgb1 = (RGBFormat) inputFormat;
	RGBFormat rgb2 = (RGBFormat) outputFormat;
	rm1 = rgb1.getRedMask();
	gm1 = rgb1.getGreenMask();
	bm1 = rgb1.getBlueMask();
	rm2 = rgb2.getRedMask();
	gm2 = rgb2.getGreenMask();
	bm2 = rgb2.getBlueMask();
	boolean packed1 = rgb1.getDataType() != Format.byteArray;
	boolean packed2 = rgb2.getDataType() != Format.byteArray;
	int bytesPerPixel1 = rgb1.getBitsPerPixel();
	int bytesPerPixel2 = rgb2.getBitsPerPixel();

	if (bytesPerPixel1 == 15)
	    bytesPerPixel1 = 2;
	else
	    bytesPerPixel1 /= 8;
	
	if (bytesPerPixel2 == 15)
	    bytesPerPixel2 = 2;
	else
	    bytesPerPixel2 /= 8;
	
	switch (bytesPerPixel1) {
	case 3:
	    if (Arch.isBigEndian()) {
		rm1 = 0xFF << ((3 - rm1) * 8);
		gm1 = 0xFF << ((3 - gm1) * 8);
		bm1 = 0xFF << ((3 - bm1) * 8);
	    } else {
		rm1 = 0xFF << ((rm1) * 8);
		gm1 = 0xFF << ((gm1) * 8);
		bm1 = 0xFF << ((bm1) * 8);
	    }
	    break;
	case 4:
	    if (packed1)
		break;
	    if (Arch.isBigEndian()) {
		rm1 = 0xFF << ((4 - rm1) * 8);
		gm1 = 0xFF << ((4 - gm1) * 8);
		bm1 = 0xFF << ((4 - bm1) * 8);
	    } else {
		rm1 = 0xFF << ((rm1 - 1) * 8);
		gm1 = 0xFF << ((gm1 - 1) * 8);
		bm1 = 0xFF << ((bm1 - 1) * 8);
	    }
	}

	switch (bytesPerPixel2) {
	case 3:
	    if (Arch.isBigEndian()) {
		rm2 = 0xFF << ((3 - rm2) * 8);
		gm2 = 0xFF << ((3 - gm2) * 8);
		bm2 = 0xFF << ((3 - bm2) * 8);
	    } else {
		rm2 = 0xFF << ((rm2) * 8);
		gm2 = 0xFF << ((gm2) * 8);
		bm2 = 0xFF << ((bm2) * 8);
	    }
	    break;
	case 4:
	    if (packed2)
		break;
	    if (Arch.isBigEndian()) {
		rm2 = 0xFF << ((4 - rm2) * 8);
		gm2 = 0xFF << ((4 - gm2) * 8);
		bm2 = 0xFF << ((4 - bm2) * 8);
	    } else {
		rm2 = 0xFF << ((rm1 - 1) * 8);
		gm2 = 0xFF << ((gm1 - 1) * 8);
		bm2 = 0xFF << ((bm1 - 1) * 8);
	    }
	}
	// native call
	initConverter(rm1, gm1, bm1, bytesPerPixel1, rm2, gm2, bm2, bytesPerPixel2);
	
    }
    
    
    public int process(Buffer inBuffer, Buffer outBuffer) {
	if (!loaded)
	    return BUFFER_PROCESSED_FAILED;
	
	if (isEOM(inBuffer)) {
	    propagateEOM(outBuffer);
	    return BUFFER_PROCESSED_OK;
	}

	if (conversionFun == 0) {
	    initialize();
	}

	if (conversionFun == 0)
	    return BUFFER_PROCESSED_FAILED;

	int aes1, aes2, inStride, outStride;
	Object inData, outData;
	RGBFormat inFormat = (RGBFormat) inBuffer.getFormat();
	RGBFormat outFormat = (RGBFormat) outBuffer.getFormat();

	if (outFormat == null) {
	    outFormat = (RGBFormat) outputFormat;
	    outBuffer.setFormat(outFormat);
	}
	
	aes1 = getArrayElementSize(inFormat.getDataType());
	aes2 = getArrayElementSize(outFormat.getDataType());
	
	inData = inBuffer.getData();
	outData = outBuffer.getData();

	if (outData == null) {
	    int maxDataLength = outFormat.getMaxDataLength();

	    switch (aes2) {
	    case 2:
		outData = new short[maxDataLength];
		break;
	    case 4:
		outData = new int[maxDataLength];
		break;
	    case 1:
		outData = new byte[maxDataLength];
		break;
	    default:
		return BUFFER_PROCESSED_FAILED;
	    }
	    
	    outBuffer.setData(outData);
	}

	Dimension size = inFormat.getSize();
	
	convert(inData, aes1,
		inFormat.getLineStride() / inFormat.getPixelStride(),
		outData, aes2,
		outFormat.getLineStride() / outFormat.getPixelStride(),
		size.width, size.height);
	outBuffer.setLength(outFormat.getLineStride() * size.height);

	return BUFFER_PROCESSED_OK;
    }

    private native void initConverter(int rm1, int gm1, int bm1, int byteDepth1,
				      int rm2, int gm2, int bm2, int byteDepth2);
    private native boolean convert(Object inData, int aes1, int inStride,
				   Object outData, int aes2, int outStride,
				   int width, int height);
}
    
	
