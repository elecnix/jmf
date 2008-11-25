/*
 * @(#)DDRenderer.java	1.31 03/04/23
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

public class DDRenderer extends BasicVideoRenderer
implements com.sun.media.util.DynamicPlugIn {

    private static boolean available = false;
    private static Integer processLock = new Integer(1);
    static {
	try {
	    JMFSecurityManager.loadLibrary("jmddraw");
	    available = true;
	} catch (Exception e) {
	} catch (UnsatisfiedLinkError ule) {
	}
    }

    private    int        blitter     = 0;
    private static boolean yuyvInUse  = true;
    private    boolean    yuyvInUseByMe = false;
    protected  Object     data        = null;
    //private    int        blitterAvailable = 0;
    private    int        defbitsPerPixel;
    private    int        defrMask;
    private    int        defgMask;
    private    int        defbMask;
    private    int        offX = 0;
    private    int        offY = 0;

    private    int        bytesPerPixel = 4;
    private    int        bitsPerPixel;
    private    int        rMask;
    private    int        gMask;
    private    int        bMask;
    private    int        pixelStride;
    private    int        lineStride;
    private    boolean    upsideDown;

    private    int        offsetY;
    private    int        offsetU;
    private    int        offsetV;
    
    private synchronized native boolean dxInitialize();
    private synchronized native boolean dxSetComponent(int windowHandle);
    private synchronized native boolean dxSetInputFormat(int width, int height,
							 int strideX, int bpp,
							 int rm, int gm, int bm, boolean flipped);
    private synchronized native boolean dxSetFourCCInputFormat(int width,
							       int height,
							       int yuvType);
    private synchronized native boolean dxSetOutputSize(int width, int height);
    private synchronized native boolean dxDraw(Object array, long bytes,
					       int width, int height);
    private synchronized native boolean dxFree();

    protected VideoFormat defaultFormat;
    private int fccSupported = 0;
    private final int YUYV = YUVFormat.YUV_YUYV;
    private final int P420 = YUVFormat.YUV_420;
    private final int P422 = YUVFormat.YUV_422;

    public DDRenderer() {
	super("DirectDraw Renderer");

	if (available) {
	    if (dxInitialize()) {
		Class arrayType;
		int nFormats = 1;
		
		if (defbitsPerPixel <= 8)
		    arrayType = Format.byteArray;
		else if (defbitsPerPixel <= 16)
		    arrayType = Format.shortArray;
		else if (defbitsPerPixel <= 24)
		    arrayType = Format.byteArray;
		else
		    arrayType = Format.intArray;

		if (defbitsPerPixel == 24) {
		    defrMask = 3; defgMask = 2; defbMask = 1;
		}

		
		if (yuyvInUse)
		    fccSupported = 0;
		if ((fccSupported & (YUYV)) != 0)
		    nFormats+=2;

		defaultFormat = new RGBFormat(null,
					      Format.NOT_SPECIFIED,
					      arrayType,
					      Format.NOT_SPECIFIED, // frame rate
					      defbitsPerPixel,
					      defrMask, defgMask, defbMask,
					      (defbitsPerPixel == 24)? 3:1,
					      Format.NOT_SPECIFIED,
					      Format.NOT_SPECIFIED,
					      Format.NOT_SPECIFIED);

		supportedFormats = new VideoFormat[nFormats];
		supportedFormats[0] = defaultFormat;

		if ((fccSupported & (YUYV)) != 0) {
		    supportedFormats[1] = new YUVFormat(YUVFormat.YUV_422);
		    supportedFormats[2] = new YUVFormat(YUVFormat.YUV_420);
		}

	    } else {
		available = false;
	    }
	}
    }

    public synchronized void open() throws ResourceUnavailableException {
	if (!available)
	    throw new ResourceUnavailableException("DirectDraw not available");
	if (inputFormat instanceof YUVFormat) {
	    yuyvInUse = true;
	    yuyvInUseByMe = true;
	}
    }

    public void reset() {
    }
    
    public synchronized void close() {
	if (available) {
	    dxFree();
	    if (yuyvInUseByMe) {
		yuyvInUseByMe = false;
		yuyvInUse = false;
	    }
	}
    }

    /**
     * Set the data input format.
     * @return false if the format is not supported.
     */
    public synchronized Format setInputFormat(Format format) {
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
		if (bitsPerPixel == 24)
		    lineStride /= 3;
		pixelStride = rgbf.getPixelStride();
		if (rgbf.getFlipped() == Format.TRUE)
		    upsideDown = true;
		else
		    upsideDown = false;
		// Inform the native code of the input format change
		if (!dxSetInputFormat(inWidth, inHeight, lineStride,
				      bitsPerPixel, rMask, gMask, bMask, upsideDown))
		    return null;
	    } else if (inputFormat instanceof IndexedColorFormat) {
		lineStride = ((IndexedColorFormat)format).getLineStride();
	    } else if (inputFormat instanceof YUVFormat) {
		if ((fccSupported  & (YUYV)) == 0)
		    return null;
		YUVFormat yuv = (YUVFormat) inputFormat;
		int yuvType = yuv.getYuvType();
		if (yuvType != YUVFormat.YUV_420 &&
		    yuvType != YUVFormat.YUV_422)
		    return null;
		lineStride = yuv.getStrideY();
		pixelStride = 1;
		offsetY = yuv.getOffsetY();
		offsetU = yuv.getOffsetU();
		offsetV = yuv.getOffsetV();
		
		if (!dxSetFourCCInputFormat(inWidth, inHeight, yuvType))
		    return null;
	    } else
		return null;
	    
	    if (outWidth == -1 || outHeight == -1) {
		outWidth = inWidth;
		outHeight = inHeight;
	    }
	    // Determine the bytesPerPixel of the data data
	    Class dataType =  inputFormat.getDataType();
	    if (dataType == Format.intArray)
		bytesPerPixel = 4;
	    else if (dataType == Format.shortArray)
		bytesPerPixel = 2;
	    else if (dataType == Format.byteArray)
		bytesPerPixel = 1;

	    if (component != null)
		component.setSize(outWidth, outHeight);
	    // All's well
	    return format;
	} else {
	    // Unsupported format
	    return null;
	}
    }

    protected synchronized void removingComponent() {
	if (blitter != 0)
	    dxFree();
    }

    protected synchronized int doProcess(Buffer buffer) {
	if (!available || component == null)
	    return BUFFER_PROCESSED_OK;
	synchronized (this) {
	    if (!componentAvailable)
		return BUFFER_PROCESSED_OK;
	    
	    if (blitter == 0) {
		int handle = com.sun.media.util.WindowUtil.getWindowHandle(component);
		if (handle != 0) {
		    dxSetComponent(handle);
		    inputFormat = new VideoFormat(null);
		}
		if (blitter == 0) {
		    System.err.println("Could not create blitter");
		    return BUFFER_PROCESSED_FAILED;
		}
	    }
	    
	    if (!buffer.getFormat().equals(inputFormat)) {
		if (setInputFormat(buffer.getFormat()) == null)
		    return BUFFER_PROCESSED_FAILED;
	    }

	    synchronized (processLock) {
		long dataBytes = 0;
		data = getInputData(buffer);
		if (data instanceof NBA) {
		    dataBytes = ((NBA)data).getNativeData();
		}

		if (data == null)
		    return BUFFER_PROCESSED_OK;
		
		if (outWidth > 0 && outHeight > 0)
		    return (dxDraw(data, dataBytes, outWidth, outHeight))?
			BUFFER_PROCESSED_OK : BUFFER_PROCESSED_FAILED;
		else
		    return BUFFER_PROCESSED_FAILED;
	    }
	}
    }

    protected synchronized void repaint() {
	if (!isStarted() && data != null && blitter != 0)
	    if (outWidth > 0 && outHeight > 0) {
		long dataBytes = 0;
		if (data instanceof NBA) {
		    dataBytes = ((NBA)data).getNativeData();
		}
		
		dxDraw(data, dataBytes, outWidth, outHeight);
	    }
    }

    /****************************************************************
     * DynamicPlugIn methods
     ****************************************************************/
    
    public Format [] getBaseInputFormats() {
	Format [] formats = new Format[2];
	formats[0] = new RGBFormat();
	formats[1] = new YUVFormat();
	return formats;
    }

    public Format [] getBaseOutputFormats() {
	Format [] formats = new Format[0];
	return formats;
    }

    /****************************************************************
     * Local Methods
     ****************************************************************/

    protected Color getPreferredBackground() {
	return new Color(255, 0, 255);
    }
}
