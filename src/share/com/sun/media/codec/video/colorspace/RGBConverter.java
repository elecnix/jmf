/*
 * @(#)RGBConverter.java	1.15 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.video.colorspace;

import javax.media.Format;
import javax.media.format.*;
import javax.media.Buffer;
import javax.media.Control;
import javax.media.ResourceUnavailableException;
import javax.media.control.FrameProcessingControl;
import com.sun.media.BasicCodec;
import java.awt.Dimension;
import java.awt.Component;

public abstract class RGBConverter extends BasicCodec {

    private static final String PLUGIN_NAME = "RGB To RGB Converter";
    private FrameProcessingControl frameControl = null;
    private boolean dropFrame;

    public RGBConverter() {
	inputFormats = new Format [] { new RGBFormat() };
	outputFormats = new Format [] { new RGBFormat() };
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
	else if (input instanceof RGBFormat) {
	    RGBFormat rgb = (RGBFormat) input;
	    Dimension size = rgb.getSize();
	    float frameRate = rgb.getFrameRate();
	    int bpp = rgb.getBitsPerPixel();
	    RGBFormat bits_16_p = new RGBFormat(size, size.width * size.height,
						Format.shortArray,
						frameRate,
						16,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED,
						1, size.width,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED);
	    RGBFormat bits_16_up = new RGBFormat(size, size.width * size.height * 2,
						 Format.byteArray,
						 frameRate,
						 16,
						 Format.NOT_SPECIFIED,
						 Format.NOT_SPECIFIED,
						 Format.NOT_SPECIFIED,
						 2, size.width * 2,
						 Format.NOT_SPECIFIED,
						 Format.NOT_SPECIFIED);
	    RGBFormat masks_565 = new RGBFormat(null, Format.NOT_SPECIFIED,
					       null,
					       Format.NOT_SPECIFIED,
					       Format.NOT_SPECIFIED,
					       0xF800, 0x07E0, 0x001F,
					       Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
					       Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
	    RGBFormat masks_555 = new RGBFormat(null, Format.NOT_SPECIFIED,
						null,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED,
						0x7C00, 0x03E0, 0x001F,
						Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
	    RGBFormat bits_24_up = new RGBFormat(size, size.width * size.height * 3,
						 Format.byteArray,
						 frameRate,
						 24,
						 Format.NOT_SPECIFIED, //R
						 Format.NOT_SPECIFIED, //G
						 Format.NOT_SPECIFIED, //B
						 3, size.width * 3,
						 Format.NOT_SPECIFIED,
						 Format.NOT_SPECIFIED);
	    RGBFormat masks_RGB = new RGBFormat(null, Format.NOT_SPECIFIED, null,
						Format.NOT_SPECIFIED, // framerate
						Format.NOT_SPECIFIED, // bpp
						1, 2, 3,
						Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
	    RGBFormat masks_BGR = new RGBFormat(null, Format.NOT_SPECIFIED, null,
						Format.NOT_SPECIFIED, // framerate
						Format.NOT_SPECIFIED, // bpp
						3, 2, 1,
						Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
	    RGBFormat bits_32_p = new RGBFormat(size, size.width * size.height, Format.intArray,
						frameRate,
						32,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED,
						1, size.width,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED);
	    RGBFormat bits_32_up = new RGBFormat(size, size.width * size.height * 4,
						 Format.byteArray,
						 frameRate,
						 32,
						 Format.NOT_SPECIFIED, //R
						 Format.NOT_SPECIFIED, //G
						 Format.NOT_SPECIFIED, //B
						 4, size.width * 4,
						 Format.NOT_SPECIFIED,
						 Format.NOT_SPECIFIED);
	    RGBFormat masks_234 = new RGBFormat(null, Format.NOT_SPECIFIED, null,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED,
						2, 3, 4,
						Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
	    RGBFormat masks_432 = new RGBFormat(null, Format.NOT_SPECIFIED, null,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED,
						4, 3, 2,
						Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
	    RGBFormat masks_123 = new RGBFormat(null, Format.NOT_SPECIFIED, null,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED,
						1, 2, 3,
						Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
	    RGBFormat flipped = new RGBFormat(null, Format.NOT_SPECIFIED, null,
					      Format.NOT_SPECIFIED,
					      Format.NOT_SPECIFIED,
					      Format.NOT_SPECIFIED, //R
					      Format.NOT_SPECIFIED,
					      Format.NOT_SPECIFIED,
					      Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
					      RGBFormat.TRUE, Format.NOT_SPECIFIED);
	    RGBFormat straight = new RGBFormat(null, Format.NOT_SPECIFIED, null,
					       Format.NOT_SPECIFIED,
					       Format.NOT_SPECIFIED,
					       Format.NOT_SPECIFIED, //R
					       Format.NOT_SPECIFIED,
					       Format.NOT_SPECIFIED,
					       Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
					       RGBFormat.FALSE, Format.NOT_SPECIFIED);
	    RGBFormat big = new RGBFormat(null, Format.NOT_SPECIFIED, null,
					  Format.NOT_SPECIFIED,
					  Format.NOT_SPECIFIED,
					  Format.NOT_SPECIFIED, //R
					  Format.NOT_SPECIFIED,
					  Format.NOT_SPECIFIED,
					  Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
					  Format.NOT_SPECIFIED,
					  RGBFormat.BIG_ENDIAN);
	    RGBFormat little = new RGBFormat(null, Format.NOT_SPECIFIED, null,
					     Format.NOT_SPECIFIED,
					     Format.NOT_SPECIFIED,
					     Format.NOT_SPECIFIED, //R
					     Format.NOT_SPECIFIED,
					     Format.NOT_SPECIFIED,
					     Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
					     Format.NOT_SPECIFIED,
					     RGBFormat.LITTLE_ENDIAN);
	    RGBFormat masks_321 = new RGBFormat(null, Format.NOT_SPECIFIED, null,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED,
						3, 2, 1,
						Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
	    RGBFormat masks_XRGB = new RGBFormat(null, Format.NOT_SPECIFIED, null,
						 Format.NOT_SPECIFIED,
						 Format.NOT_SPECIFIED,
						 0xFF0000, 0x00FF00, 0x0000FF,
						 Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
						 Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
	    RGBFormat masks_XBGR = new RGBFormat(null, Format.NOT_SPECIFIED, null,
						 Format.NOT_SPECIFIED,
						 Format.NOT_SPECIFIED,
						 0x0000FF, 0x00FF00, 0xFF0000,
						 Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
						 Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);

	    Format [] out;
	    out = new Format [] {
		bits_16_p.intersects(masks_565).intersects(flipped),
		bits_16_p.intersects(masks_565).intersects(straight),
		
		bits_16_up.intersects(masks_565).intersects(flipped).intersects(little),
		bits_16_up.intersects(masks_565).intersects(flipped).intersects(big),
		bits_16_up.intersects(masks_565).intersects(straight).intersects(little),
		bits_16_up.intersects(masks_565).intersects(straight).intersects(big),
		
		bits_16_p.intersects(masks_555).intersects(flipped),
		bits_16_p.intersects(masks_555).intersects(straight),
		
		bits_16_up.intersects(masks_555).intersects(flipped).intersects(little),
		bits_16_up.intersects(masks_555).intersects(flipped).intersects(big),
		bits_16_up.intersects(masks_555).intersects(straight).intersects(little),
		bits_16_up.intersects(masks_555).intersects(straight).intersects(big),
		
		bits_24_up.intersects(masks_RGB).intersects(flipped),
		bits_24_up.intersects(masks_RGB).intersects(straight),
		bits_24_up.intersects(masks_BGR).intersects(flipped),
		bits_24_up.intersects(masks_BGR).intersects(straight),

		bits_32_p.intersects(masks_XRGB).intersects(flipped),
		bits_32_p.intersects(masks_XRGB).intersects(straight),
		bits_32_p.intersects(masks_XBGR).intersects(flipped),
		bits_32_p.intersects(masks_XBGR).intersects(straight),

		bits_32_up.intersects(masks_123).intersects(flipped),
		bits_32_up.intersects(masks_123).intersects(straight),
		bits_32_up.intersects(masks_321).intersects(flipped),
		bits_32_up.intersects(masks_321).intersects(straight),
		bits_32_up.intersects(masks_234).intersects(flipped),
		bits_32_up.intersects(masks_234).intersects(straight),
		bits_32_up.intersects(masks_432).intersects(flipped),
		bits_32_up.intersects(masks_432).intersects(straight)
	    };

	    return out;
	} else
	    return null;
    }

    protected abstract void sixteenToSixteen(
					Object inData, int inPS, int inLS, int inBPP,
					int inRed, int inGreen, int inBlue,
					boolean inPacked, int inEndian,
					Object outData, int outPS, int outLS, int outBPP,
					int outRed, int outGreen, int outBlue,
					boolean outPacked, int outEndian,
					int width, int height, boolean flip
					);
    
    protected abstract void sixteenToComponent(
					Object inData, int inPS, int inLS, int inBPP,
					int inRed, int inGreen, int inBlue,
					boolean inPacked, int inEndian,
					Object outData, int outPS, int outLS, int outBPP,
					int outRed, int outGreen, int outBlue,
					boolean outPacked, int outEndian,
					int width, int height, boolean flip
					);
    
    protected abstract void componentToSixteen(
					Object inData, int inPS, int inLS, int inBPP,
					int inRed, int inGreen, int inBlue,
					boolean inPacked, int inEndian,
					Object outData, int outPS, int outLS, int outBPP,
					int outRed, int outGreen, int outBlue,
					boolean outPacked, int outEndian,
					int width, int height, boolean flip
					);
    
    protected abstract void componentToComponent(
					Object inData, int inPS, int inLS, int inBPP,
					int inRed, int inGreen, int inBlue,
					boolean inPacked, int inEndian,
					Object outData, int outPS, int outLS, int outBPP,
					int outRed, int outGreen, int outBlue,
					boolean outPacked, int outEndian,
					int width, int height, boolean flip
					);

    public Format setInputFormat(Format in) {
	Format returnFormat = super.setInputFormat(in);
	if (returnFormat == null)
	    return null;
	if (((RGBFormat)returnFormat).getBitsPerPixel() < 15)
	    return null;
	Dimension size = ((VideoFormat)in).getSize();

	if (opened) {
	    // possible format change...
	    outputFormat = updateRGBFormat((VideoFormat)in, (RGBFormat)outputFormat);
	    /*
	    RGBFormat oldFormat = (RGBFormat) outputFormat;
	    int lineStride = size.width * oldFormat.getPixelStride();
	    outputFormat = new RGBFormat(size,
					 lineStride * size.height,
					 oldFormat.getDataType(),
					 ((VideoFormat)inputFormat).getFrameRate(),
					 oldFormat.getBitsPerPixel(),
					 oldFormat.getRedMask(),
					 oldFormat.getGreenMask(),
					 oldFormat.getBlueMask(),
					 oldFormat.getPixelStride(),
					 lineStride,
					 oldFormat.getFlipped(),
					 oldFormat.getEndian()
					 );
	    */
	}
	
	return returnFormat;
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

	RGBFormat inputRGB = (RGBFormat) inputFormat;
	RGBFormat outputRGB = (RGBFormat) outputFormat;

	Object inObject = inBuffer.getData();
	Object outObject = outBuffer.getData();

	// Check for input data type
	if (inObject.getClass() != inputFormat.getDataType())
	    return BUFFER_PROCESSED_FAILED;

	int outMaxDataLen = outputRGB.getMaxDataLength();
	int outLength = 0;
	if (outObject != null) {
	    if (outObject.getClass() == Format.byteArray)
		outLength = ((byte[])outObject).length;
	    else if (outObject.getClass() == Format.shortArray)
		outLength = ((short[])outObject).length;
	    else if (outObject.getClass() == Format.intArray)
		outLength = ((int[])outObject).length;
	}

	// Allocate output data if necessary
	if (  outObject == null ||
	      outLength < outMaxDataLen ||
	      outputFormat != outBuffer.getFormat() ||
	      !outputFormat.equals(outBuffer.getFormat())  ) {
	    
	    Class outputDataType = outputFormat.getDataType();
	    if (outputDataType == Format.byteArray)
		outObject = new byte[outputRGB.getMaxDataLength()];
	    else if (outputDataType == Format.shortArray)
		outObject = new short[outputRGB.getMaxDataLength()];
	    else if (outputDataType == Format.intArray)
		outObject = new int[outputRGB.getMaxDataLength()];
	    else
		return BUFFER_PROCESSED_FAILED;
	    outBuffer.setData(outObject);
	}

	// Check for output data type
	if (outObject.getClass() != outputFormat.getDataType())
	    return BUFFER_PROCESSED_FAILED;
	
	int inBPP = inputRGB.getBitsPerPixel();
	int outBPP = outputRGB.getBitsPerPixel();
	boolean inPacked = (inputRGB.getDataType() != Format.byteArray);
	boolean outPacked = (outputRGB.getDataType() != Format.byteArray);
	int inPS = inputRGB.getPixelStride();
	int outPS = outputRGB.getPixelStride();
	int inEndian = inputRGB.getEndian();
	int outEndian = outputRGB.getEndian();
	int inRed = inputRGB.getRedMask();
	int inGreen = inputRGB.getGreenMask();
	int inBlue = inputRGB.getBlueMask();
	int outRed = outputRGB.getRedMask();
	int outGreen = outputRGB.getGreenMask();
	int outBlue = outputRGB.getBlueMask();
	int inLS = inputRGB.getLineStride();
	int outLS = outputRGB.getLineStride();
	
	boolean flip = (inputRGB.getFlipped() != outputRGB.getFlipped());
	Dimension size = inputRGB.getSize();
	int width = size.width;
	int height = size.height;
	
 	if (inBPP == 16 && outBPP == 16) {
	    sixteenToSixteen(inObject, inPS, inLS, inBPP,
			     inRed, inGreen, inBlue, inPacked, inEndian,
			     outObject, outPS, outLS, outBPP,
			     outRed, outGreen, outBlue, outPacked, outEndian,
			     width, height, flip);
	} else if (inBPP == 16 && outBPP >= 24) {
	    sixteenToComponent(inObject, inPS, inLS, inBPP,
			       inRed, inGreen, inBlue, inPacked, inEndian,
			       outObject, outPS, outLS, outBPP,
			       outRed, outGreen, outBlue, outPacked, outEndian,
			       width, height, flip);
	} else if (inBPP >= 24 && outBPP == 16) {
	    componentToSixteen(inObject, inPS, inLS, inBPP,
			       inRed, inGreen, inBlue, inPacked, inEndian,
			       outObject, outPS, outLS, outBPP,
			       outRed, outGreen, outBlue, outPacked, outEndian,
			       width, height, flip);
	} else if (inBPP >= 24 && outBPP >= 24) {
	    //System.err.println("inobject size = " + ((byte[])inObject).length);
	    //System.err.println("outobject size = " + ((byte[])outObject).length);
	    componentToComponent(inObject, inPS, inLS, inBPP,
				 inRed, inGreen, inBlue, inPacked, inEndian,
				 outObject, outPS, outLS, outBPP,
				 outRed, outGreen, outBlue, outPacked, outEndian,
				 width, height, flip);
	}
	
	outBuffer.setFormat(outputFormat);
	outBuffer.setLength(outputRGB.getMaxDataLength());
	//outBuffer.setFlags(outBuffer.getFlags() | Buffer.FLAG_DISCARD);
	return BUFFER_PROCESSED_OK;
    }

    public void open() throws ResourceUnavailableException {
	super.open();
    }

    public void close() {
	super.close();
    }

    public void reset() {
    }

    protected int getShift(int mask) {
	int shift = 0;
	while ((mask & 1) == 0) {
	    mask = mask >> 1;
	    shift++;
	}
	return shift;
    }
}

    
