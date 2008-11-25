/*
 * @(#)JavaRGBToYUV.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.video.colorspace;

import javax.media.*;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;
import java.awt.Component;
import com.sun.media.util.*;
import com.sun.media.BasicCodec;
import java.awt.Dimension;
import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;
import javax.media.control.FrameProcessingControl;

public class JavaRGBToYUV extends BasicCodec {

    private static final String PLUGIN_NAME = "RGB To YUV";
    private FrameProcessingControl frameControl = null;
    private boolean dropFrame = false;

    public JavaRGBToYUV() {
	int NS = Format.NOT_SPECIFIED;
	
	inputFormats = new Format [] {
	    new RGBFormat(null, NS, Format.byteArray, NS,
			  24,
			  NS, NS, NS, NS, NS, NS, NS),
	    new RGBFormat(null, NS, Format.intArray, NS,
			  32,
			  0xFF0000, 0xFF00, 0xFF, 1, NS, NS, NS),
	    new RGBFormat(null, NS, Format.intArray, NS,
			  32,
			  0xFF, 0xFF00, 0xFF0000, 1, NS, NS, NS),
	};

	outputFormats = new Format [] {
	    new YUVFormat(YUVFormat.YUV_420)
	};

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

    public String getName() {
	return PLUGIN_NAME;
    }
    
    public Format [] getSupportedOutputFormats(Format input) {

	if (input == null)
	    return outputFormats;
	else if (input instanceof RGBFormat &&
		 matches(input, inputFormats) != null) {
	    RGBFormat rgb = (RGBFormat) input;
	    Dimension size = rgb.getSize();
	    float frameRate = rgb.getFrameRate();
	    int bpp = rgb.getBitsPerPixel();
	    int scan = (size.width + 1) & ~1;
	    
	    YUVFormat output = new YUVFormat(size,
					     scan * size.height * 3 / 2,
					     Format.byteArray,
					     frameRate,
					     YUVFormat.YUV_420,
					     scan, scan / 2,
					     0, scan * size.height,
					     (scan * size.height * 5) / 4);

	    Format [] outputs = new Format [] {output};
	    return outputs;
	} else {
	    return new Format[0];
	}
    }

    public Format setInputFormat(Format input) {
	Format ret = super.setInputFormat(input);
	if (opened) {
	    outputFormat = getSupportedOutputFormats(ret)[0];
	}
	return ret;
    }

    public int process(Buffer inBuffer, Buffer outBuffer) {
	if (isEOM(inBuffer)) {
	    propagateEOM(outBuffer);
	    return BUFFER_PROCESSED_OK;
	}

	if (dropFrame) {
	    outBuffer.setFlags(outBuffer.getFlags() | Buffer.FLAG_DISCARD);
	    return BUFFER_PROCESSED_OK;
	}

	Format inF = inBuffer.getFormat();

	if ( !(inF instanceof RGBFormat) ||
	     inBuffer.getData() == null )
	    return BUFFER_PROCESSED_FAILED;

	Object inData = inBuffer.getData();
	validateByteArraySize(outBuffer,
			      ((VideoFormat)outputFormat).getMaxDataLength());

	outBuffer.setFormat(outputFormat);
	outBuffer.setLength(((VideoFormat)outputFormat).getMaxDataLength());
	if (((RGBFormat)inF).getBitsPerPixel() == 24) 
	    return convert24(inBuffer, outBuffer);
	else
	    return convertInt(inBuffer, outBuffer);
    }

    protected int convert24(Buffer inBuffer, Buffer outBuffer) {
	RGBFormat rgb = (RGBFormat) inBuffer.getFormat();
	YUVFormat yuv = (YUVFormat) outBuffer.getFormat();
	Dimension size = rgb.getSize();
	byte [] outData = (byte[]) outBuffer.getData();
	byte [] inData = (byte[]) inBuffer.getData();
	boolean flipped = rgb.getFlipped() == Format.TRUE;
	int increment = (flipped)? -1 : 1;
	int ystride = yuv.getStrideY();
	int uvstride = yuv.getStrideUV();
	int pointerY = yuv.getOffsetY() +
	               ystride * ((flipped)? (size.height - 1) : 0);
	int pointerU = yuv.getOffsetU() +
	               uvstride * ((flipped)? (size.height/2 - 1) : 0);
	int pointerV = yuv.getOffsetV() +
	               uvstride * ((flipped)? (size.height/2 - 1) : 0);
	int pRGB = 0;
	int rgbscan = rgb.getLineStride();
	int pixstride = rgb.getPixelStride();
	int rOffset = rgb.getRedMask() - 1;
	int gOffset = rgb.getGreenMask() - 1;
	int bOffset = rgb.getBlueMask() - 1;
	int yval, uval, vval;
	int rval, gval, bval;

	for (int y = 0; y < size.height; y+=2) {
	    for (int x = 0; x < size.width; x+=2) {
		// First pixel, first row
		rval = inData[pRGB+rOffset] & 0xFF;
		gval = inData[pRGB+gOffset] & 0xFF;
		bval = inData[pRGB+bOffset] & 0xFF;
		yval = (rval*257 + gval*504 + bval*98) / 1000 + 16;
		uval = (-rval*148 - gval*291 + bval*439) / 1000 + 128;
		vval = (rval*439 - gval*368 - bval*71) / 1000 + 128;
		//System.err.println(yval + " " + uval + " " + vval);
		outData[pointerY] = (byte) (yval & 0xFF);

		// Second pixel, first row
		rval = inData[pRGB+rOffset+pixstride] & 0xFF;
		gval = inData[pRGB+gOffset+pixstride] & 0xFF;
		bval = inData[pRGB+bOffset+pixstride] & 0xFF;
		yval = (rval*257 + gval*504 + bval*98) / 1000 + 16;
		uval += (-rval*148 - gval*291 + bval*439) / 1000 + 128;
		vval += (rval*439 - gval*368 - bval*71) / 1000 + 128;

		outData[pointerY+1] = (byte) (yval & 0xFF);

		// First pixel, second row
		rval = inData[pRGB+rOffset+rgbscan] & 0xFF;
		gval = inData[pRGB+gOffset+rgbscan] & 0xFF;
		bval = inData[pRGB+bOffset+rgbscan] & 0xFF;
		yval = (rval*257 + gval*504 + bval*98) / 1000 + 16;
		uval += (-rval*148 - gval*291 + bval*439) / 1000 + 128;
		vval += (rval*439 - gval*368 - bval*71) / 1000 + 128;
		
		outData[pointerY+(increment*ystride)] = (byte) (yval & 0xFF);

		// Second pixel, second row
		rval = inData[pRGB+rOffset+rgbscan+pixstride] & 0xFF;
		gval = inData[pRGB+gOffset+rgbscan+pixstride] & 0xFF;
		bval = inData[pRGB+bOffset+rgbscan+pixstride] & 0xFF;
		yval = (rval*257 + gval*504 + bval*98) / 1000 + 16;
		uval += (-rval*148 - gval*291 + bval*439) / 1000 + 128;
		vval += (rval*439 - gval*368 - bval*71) / 1000 + 128;
		
		outData[pointerY+(increment*ystride)+1] = (byte) (yval & 0xFF);
		outData[pointerU] = (byte) ((uval >> 2) & 0xFF);
		outData[pointerV] = (byte) ((vval >> 2) & 0xFF);
		pointerY += 2;
		pointerU++;
		pointerV++;
		pRGB += pixstride * 2;
	    }
	    pRGB += (rgbscan * 2) - (size.width * pixstride);
	    pointerY += (increment * ystride * 2) - size.width;
	    pointerU += (increment * uvstride) - (size.width / 2);
	    pointerV += (increment * uvstride) - (size.width / 2);
	}
	return BUFFER_PROCESSED_OK;
    }

    protected int convertInt(Buffer inBuffer, Buffer outBuffer) {
	RGBFormat rgb = (RGBFormat) inBuffer.getFormat();
	YUVFormat yuv = (YUVFormat) outBuffer.getFormat();
	Dimension size = rgb.getSize();
	byte [] outData = (byte[]) outBuffer.getData();
	int [] inData = (int[]) inBuffer.getData();
	boolean flipped = rgb.getFlipped() == Format.TRUE;
	int increment = (flipped)? -1 : 1;
	int ystride = yuv.getStrideY();
	int uvstride = yuv.getStrideUV();
	int pointerY = yuv.getOffsetY() +
	               ystride * ((flipped)? (size.height - 1) : 0);
	int pointerU = yuv.getOffsetU() +
	               uvstride * ((flipped)? (size.height/2 - 1) : 0);
	int pointerV = yuv.getOffsetV() +
	               uvstride * ((flipped)? (size.height/2 - 1) : 0);
	int pRGB = 0;
	int rgbscan = rgb.getLineStride();
	int rOffset = 16;
	int gOffset = 8;
	int bOffset = 0;
	int yval, uval, vval;
	int rval, gval, bval;

	if (rgb.getRedMask() == 0xFF) { // XBGR
	    rOffset = 0;
	    bOffset = 16;
	}

	for (int y = 0; y < size.height; y+=2) {
	    for (int x = 0; x < size.width; x+=2) {
		// First pixel, first row
		rval = (inData[pRGB] >> rOffset) & 0xFF;
		gval = (inData[pRGB] >> gOffset) & 0xFF;
		bval = (inData[pRGB] >> bOffset) & 0xFF;
		yval = (rval*257 + gval*504 + bval*98) / 1000 + 16;
		uval = (-rval*148 - gval*291 + bval*439) / 1000 + 128;
		vval = (rval*439 - gval*368 - bval*71) / 1000 + 128;
		//System.err.println(yval + " " + uval + " " + vval);
		outData[pointerY] = (byte) (yval & 0xFF);

		// Second pixel, first row
		rval = (inData[pRGB+1] >> rOffset) & 0xFF;
		gval = (inData[pRGB+1] >> gOffset) & 0xFF;
		bval = (inData[pRGB+1] >> bOffset) & 0xFF;
		yval = (rval*257 + gval*504 + bval*98) / 1000 + 16;
		uval += (-rval*148 - gval*291 + bval*439) / 1000 + 128;
		vval += (rval*439 - gval*368 - bval*71) / 1000 + 128;

		outData[pointerY+1] = (byte) (yval & 0xFF);

		// First pixel, second row
		rval = (inData[pRGB+rgbscan] >> rOffset) & 0xFF;
		gval = (inData[pRGB+rgbscan] >> gOffset) & 0xFF;
		bval = (inData[pRGB+rgbscan] >> bOffset) & 0xFF;
		yval = (rval*257 + gval*504 + bval*98) / 1000 + 16;
		uval += (-rval*148 - gval*291 + bval*439) / 1000 + 128;
		vval += (rval*439 - gval*368 - bval*71) / 1000 + 128;
		
		outData[pointerY+(increment*ystride)] = (byte) (yval & 0xFF);

		// Second pixel, second row
		rval = (inData[pRGB+rgbscan+1] >> rOffset) & 0xFF;
		gval = (inData[pRGB+rgbscan+1] >> gOffset) & 0xFF;
		bval = (inData[pRGB+rgbscan+1] >> bOffset) & 0xFF;
		yval = (rval*257 + gval*504 + bval*98) / 1000 + 16;
		uval += (-rval*148 - gval*291 + bval*439) / 1000 + 128;
		vval += (rval*439 - gval*368 - bval*71) / 1000 + 128;
		
		outData[pointerY+(increment*ystride)+1] = (byte) (yval & 0xFF);
		outData[pointerU] = (byte) ((uval >> 2) & 0xFF);
		outData[pointerV] = (byte) ((vval >> 2) & 0xFF);
		pointerY += 2;
		pointerU++;
		pointerV++;
		pRGB += 2;
	    }
	    pRGB += (rgbscan * 2) - size.width;
	    pointerY += (increment * ystride * 2) - size.width;
	    pointerU += (increment * uvstride) - (size.width / 2);
	    pointerV += (increment * uvstride) - (size.width / 2);
	}
	return BUFFER_PROCESSED_OK;
    }

    public void reset() {
    }

}
