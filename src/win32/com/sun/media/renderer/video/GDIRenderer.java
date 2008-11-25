/*
 * @(#)GDIRenderer.java	1.28 03/04/23
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.video;

import javax.media.*;
import javax.media.Format;
import javax.media.format.*;
import javax.media.renderer.VideoRenderer;
import java.awt.*;
import java.util.Vector;
import com.sun.media.*;

public class GDIRenderer extends BasicVideoRenderer {

    private static boolean available = false;
    
    static {
	try {
	    JMFSecurityManager.loadLibrary("jmgdi");
	    available = true;
	} catch (Exception e) {
	} catch (UnsatisfiedLinkError ule) {
	}
    }

    private    int        blitter       = 0;
    protected  Object     data        = null;
    //private    int        blitterAvailable = 0;
    private    int        defbitsPerPixel;
    private    int        defrMask;
    private    int        defgMask;
    private    int        defbMask;
    private    int        offsetX = 0;
    private    int        offsetY = 0;

    private    int        bytesPerPixel = 4;
    private    int        bitsPerPixel;
    private    int        rMask;
    private    int        gMask;
    private    int        bMask;
    private    int        pixelStride;
    private    int        lineStride;
    private    boolean    flipped;
    private    int        handle = 0;
    
    
    private synchronized native boolean gdiInitialize();
    private synchronized native boolean gdiSetComponent(int windowHandle);
    private synchronized native boolean gdiSetOutputSize(int width, int height);
    private synchronized native boolean gdiDraw(Object data,
						long dataBytes,
						int elSize, // 1 for byte[], 2 for short[]
						            // 4 for int[]
						int bytesPerPixel,
						int srcWidth, int srcHeight,
						int srcStride,
						int dstWidth, int dstHeight,
						int rm, int gm, int bm,
						boolean flipped,
						int windowHandle);
    private synchronized native boolean gdiFree();

    protected VideoFormat defaultFormat;
    
    public GDIRenderer() {
	super("Windows GDI Renderer");

	if (available) {
	    if (gdiInitialize()) {
		Class arrayType;

		if (defbitsPerPixel <= 8)
		    arrayType = Format.byteArray;
		else if (defbitsPerPixel <= 16)
		    arrayType = Format.shortArray;
		else if (defbitsPerPixel <= 24)
		    arrayType = Format.byteArray;
		else
		    arrayType = Format.intArray;

		defaultFormat = new RGBFormat(null, Format.NOT_SPECIFIED,
					      arrayType,
					      Format.NOT_SPECIFIED, // frame rate
					      defbitsPerPixel,
					      defrMask, defgMask, defbMask,
					      (defbitsPerPixel == 24)? 3:1,
					      Format.NOT_SPECIFIED,
					      Format.NOT_SPECIFIED,
					      Format.NOT_SPECIFIED);

		supportedFormats = new VideoFormat[8];
		supportedFormats[0] = defaultFormat;

		// The remaining are general formats supported by GDI
		// 15 bit
		supportedFormats[1] = new RGBFormat(null,
						    Format.NOT_SPECIFIED,
						    Format.shortArray,
						    Format.NOT_SPECIFIED, // frame rate
						    16,
						    0x7C00, 0x03E0, 0x001F,
						    1,
						    Format.NOT_SPECIFIED,
						    Format.NOT_SPECIFIED,
						    Format.NOT_SPECIFIED);
		// 15 bit
		supportedFormats[2] = new RGBFormat(null,
						    Format.NOT_SPECIFIED,
						    Format.byteArray,
						    Format.NOT_SPECIFIED, // frame rate
						    16,
						    0x7C00, 0x03E0, 0x001F,
						    2,
						    Format.NOT_SPECIFIED,
						    Format.NOT_SPECIFIED,
						    RGBFormat.LITTLE_ENDIAN);
		// 16 bit
		supportedFormats[3] = new RGBFormat(null,
						    Format.NOT_SPECIFIED,
						    Format.shortArray,
						    Format.NOT_SPECIFIED, // frame rate
						    16,
						    0xF800, 0x07E0, 0x001F,
						    1,
						    Format.NOT_SPECIFIED,
						    Format.NOT_SPECIFIED,
						    Format.NOT_SPECIFIED);
		// 16 bit
		supportedFormats[4] = new RGBFormat(null,
						    Format.NOT_SPECIFIED,
						    Format.byteArray,
						    Format.NOT_SPECIFIED, // frame rate
						    16,
						    0xF800, 0x07E0, 0x001F,
						    2,
						    Format.NOT_SPECIFIED,
						    Format.NOT_SPECIFIED,
						    RGBFormat.LITTLE_ENDIAN);
		// 24 bit
		supportedFormats[5] = new RGBFormat(null,
						    Format.NOT_SPECIFIED,
						    Format.byteArray,
						    Format.NOT_SPECIFIED, // frame rate
						    24,
						    3, 2, 1,
						    3,
						    Format.NOT_SPECIFIED,
						    Format.NOT_SPECIFIED,
						    Format.NOT_SPECIFIED);
		// 32 bit xRGB
		supportedFormats[6] = new RGBFormat(null,
						    Format.NOT_SPECIFIED,
						    Format.intArray,
						    Format.NOT_SPECIFIED, // frame rate
						    32,
						    0x00FF0000, 0x0000FF00, 0x000000FF,
						    1,
						    Format.NOT_SPECIFIED,
						    Format.NOT_SPECIFIED,
						    Format.NOT_SPECIFIED);
		// 32 bit xRGB
		supportedFormats[7] = new RGBFormat(null,
						    Format.NOT_SPECIFIED,
						    Format.byteArray,
						    Format.NOT_SPECIFIED, // frame rate
						    32,
						    3, 2, 1,
						    4,
						    Format.NOT_SPECIFIED,
						    Format.NOT_SPECIFIED,
						    Format.NOT_SPECIFIED);
	    } else {
		System.err.println("GDIRenderer. gdiInitialize() failed");
		available = false;
	    }
	}
    }

    public void open() throws ResourceUnavailableException {
	if (!available)
	    throw new ResourceUnavailableException("GDI not available !!!");
	handle = 0;
	if (blitter == 0)
	    gdiInitialize();
	if (blitter == 0)
	    throw new ResourceUnavailableException("GDIRenderer couldn't open");
    }

    public synchronized void reset() {
	handle = 0;
    }
    
    public void close() {
	if (available)
	    gdiFree();
    }

    /**
     * Set the data input format.
     * @return false if the format is not supported.
     */
    public Format setInputFormat(Format format) {
	if (!available)
	    return null;
	if (super.setInputFormat(format) != null) {
	    if (inputFormat instanceof RGBFormat) {
		RGBFormat rgbf = (RGBFormat) inputFormat;
		bitsPerPixel = rgbf.getBitsPerPixel();
		rMask = rgbf.getRedMask();
		gMask = rgbf.getGreenMask();
		bMask = rgbf.getBlueMask();
		lineStride = rgbf.getLineStride();
		pixelStride = rgbf.getPixelStride();
		flipped = (rgbf.getFlipped() == Format.TRUE);
		if (inputFormat.getDataType() == Format.byteArray && bitsPerPixel > 16) {
		    int bypp = bitsPerPixel / 8;
		    rMask = 0xFF << ((rMask - 1) * 8);
		    gMask = 0xFF << ((gMask - 1) * 8);
		    bMask = 0xFF << ((bMask - 1) * 8);
		}
	    } else if (inputFormat instanceof IndexedColorFormat) {
		bitsPerPixel = 8;
		lineStride = ((IndexedColorFormat)format).getLineStride();
	    } else
		return null;
	    
	    // Inform the native code of the input format change
	    if (outWidth == -1 || outHeight == -1) {
		outWidth = inWidth;
		outHeight = inHeight;
	    }

	    if (component != null)
		component.setSize(outWidth, outHeight);
	    // All's well
	    return format;
	} else {
	    // Unsupported format
	    return null;
	}
    }

    protected int doProcess(Buffer buffer) {
	return doProcess(buffer, false);
    }

    protected synchronized int doProcess(Buffer buffer, boolean repainting) {
	if (!available || component == null)
	    return BUFFER_PROCESSED_OK;
	if (handle == 0)
	    handle = com.sun.media.util.WindowUtil.getWindowHandle(component);
	if (handle == 0)
	    return BUFFER_PROCESSED_OK;
	if (!repainting) {
	    if (!buffer.getFormat().equals(inputFormat)) {
		if (setInputFormat(buffer.getFormat()) == null)
		    return BUFFER_PROCESSED_FAILED;
	    }
	}
	if (!repainting)
	    data = getInputData(buffer);

	int elSize = 1;
	if (data instanceof short[])
	    elSize = 2;
	else if (data instanceof int[])
	    elSize = 4;

	bytesPerPixel = bitsPerPixel / 8;

	long dataBytes = 0;
	if (data instanceof NBA) {
	    dataBytes = ((NBA)data).getNativeData();
	}

	if (data == null)
	    return BUFFER_PROCESSED_OK;
	
	if (outWidth > 0 && outHeight > 0) {
	    int returned =  (gdiDraw(data, dataBytes, elSize, bytesPerPixel,
				     inWidth, inHeight,
				     lineStride / pixelStride, outWidth, outHeight,
				     rMask, gMask, bMask, flipped, handle))?
		BUFFER_PROCESSED_OK : BUFFER_PROCESSED_FAILED;
	    return returned;
	} else
	    return BUFFER_PROCESSED_OK;
    }

    protected synchronized void repaint() {
	if (!isStarted() && data != null)
	    doProcess(null, true);
    }

    protected synchronized void setAvailable(boolean available) {
	super.setAvailable(available);
	handle = 0;
    }
}
