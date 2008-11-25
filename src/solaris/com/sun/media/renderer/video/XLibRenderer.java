/*
 * @(#)XLibRenderer.java	1.11 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.video;

import javax.media.*;
import javax.media.Format;
import javax.media.format.*;
import javax.media.renderer.VideoRenderer;
import java.awt.*;
import java.awt.image.*;
import java.util.Vector;
import com.sun.media.*;
import com.sun.media.util.WindowUtil;

public class XLibRenderer extends BasicVideoRenderer
implements com.sun.media.util.DynamicPlugIn {

    private static boolean available = true;
    private static int jawtAvail = WindowUtil.getJAWTAvail();

    private    int        blitter     = 0;
    protected  Object     data        = null;

    private    int        defBitsPerPixel = 32;
    private    int        defRedMask   = 0xFF << 0;
    private    int        defGreenMask = 0xFF << 8;
    private    int        defBlueMask  = 0xFF << 16;
    private    int        offsetX = 0;
    private    int        offsetY = 0;

    private    int        bytesPerPixel = 4;
    private    int        bitsPerPixel;
    private    int        rMask;
    private    int        gMask;
    private    int        bMask;
    private    int        pixelStride;
    private    int        lineStride;
    private    int        lastOutWidth = -1;
    private    int        lastOutHeight = -1;
    private    boolean    firstTime = true;
    
    private native void  xlibSetJAWT(int jawt);
    private synchronized native boolean xlibInitialize();
    private synchronized native boolean xlibSetComponent(Object component);
    private synchronized native boolean xlibSetInputFormat(int width, int height,
							   int strideX);
    private synchronized native boolean xlibSetOutputSize(int width, int height);
    private synchronized native boolean xlibDraw(Object data, long dataBytes, int aes);
    private synchronized native boolean xlibFree();

    protected VideoFormat defaultFormat;

    public XLibRenderer() {
	super("XLib Renderer");
	try {
	    JMFSecurityManager.loadLibrary("jmutil");
	    JMFSecurityManager.loadLibrary("jmxlib");
	} catch (Exception e) {
	    e.printStackTrace();
	    available = false;
	} catch (UnsatisfiedLinkError ule) {
	    ule.printStackTrace();
	    available = false;
	}
	if (!available)
	    throw new RuntimeException("Could not load jmxlib library");
	xlibSetJAWT(jawtAvail);
	if (xlibInitialize()) {
	    ColorModel cm = Toolkit.getDefaultToolkit().getColorModel();
	    if (!(cm instanceof DirectColorModel))
		throw new RuntimeException("Cannot render to non-TrueColor visuals");

	    DirectColorModel dcm = (DirectColorModel) cm;
	    int pixelStride = 1;
	    defBitsPerPixel = dcm.getPixelSize();
	    Class arrayType = Format.intArray;
	    if (defBitsPerPixel == 16 || defBitsPerPixel == 15) {
		defBitsPerPixel = 16;
		arrayType = Format.shortArray;
		bytesPerPixel = 2;
	    }
	    if (defBitsPerPixel == 24) {
		defBitsPerPixel = 32;
		arrayType = Format.intArray;
	    }

	    defRedMask = dcm.getRedMask();
	    defBlueMask = dcm.getBlueMask();
	    defGreenMask = dcm.getGreenMask();
	    
	    defaultFormat = new RGBFormat(null, Format.NOT_SPECIFIED,
					  arrayType,
					  Format.NOT_SPECIFIED, // frame rate
					  defBitsPerPixel, 
					  defRedMask, defGreenMask, defBlueMask,
					  pixelStride,
					  Format.NOT_SPECIFIED,
					  Format.FALSE, // flipped
					  Format.NOT_SPECIFIED); // endian
	    supportedFormats = new VideoFormat[1];
	    supportedFormats[0] = defaultFormat;
	    close();
	} else {
	    available = false;
	}
    }

    public void open() throws ResourceUnavailableException {

	if (!available)
	    throw new ResourceUnavailableException("XLib not available");
    }

    public void reset() {
    }
    
    public void close() {
	if (available && blitter != 0) {
	    xlibFree();
	}
    }

    /**
     * Set the data input format.
     * @return false if the format is not supported.
     */
    public Format setInputFormat(Format format) {
	if (!available)
	    return null;
	if (!(format instanceof RGBFormat))
	    return null;
	if (!(Toolkit.getDefaultToolkit().getColorModel() instanceof DirectColorModel))
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
	    } else if (inputFormat instanceof IndexedColorFormat) {
		lineStride = ((IndexedColorFormat)format).getLineStride();
	    } else
		return null;

	    // Inform the native code of the input format change
	    synchronized (this) {
		if (blitter != 0) {
		    xlibSetInputFormat(inWidth, inHeight, lineStride);
		}
	    }
	    
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

    protected synchronized void removingComponent() {
	if (blitter != 0) {
	    xlibFree();
	}
    }

    protected int doProcess(Buffer buffer) {
	return doProcess(buffer, false);
    }

    protected int doProcess(Buffer buffer, boolean repaint) {
	boolean resetOutputSize = false;
	if (!available || component == null)
	    return BUFFER_PROCESSED_OK;
	if (!repaint) {
	    if (!buffer.getFormat().equals(inputFormat)) {
		if (setInputFormat(buffer.getFormat()) == null)
		    return BUFFER_PROCESSED_FAILED;
	    }

	    data = getInputData(buffer);
	} else
	    if (data == null)
		return BUFFER_PROCESSED_FAILED;

	synchronized (this) {
	    if (!componentAvailable || data == null)
		return BUFFER_PROCESSED_OK;
	    if (blitter == 0 || firstTime) {
		int handle = 0;
		firstTime = false;
		if (blitter == 0) {
		    handle = com.sun.media.util.WindowUtil.getWindowHandle(component);
		    if (handle == 0)
			return BUFFER_PROCESSED_OK;
		    //System.err.println("handle is " + handle);
		    xlibInitialize();
		    xlibSetComponent(component);
		    xlibSetInputFormat(inWidth, inHeight, lineStride);
		    resetOutputSize = true;
		    // Reset it to force calling setInputFormat
		    inputFormat = new VideoFormat(null);
		}
		if (blitter == 0) {
		    return BUFFER_PROCESSED_FAILED;
		}
	    }
	    
	    if (outWidth > 0 && outHeight > 0) {
		if (outWidth != lastOutWidth || outHeight != lastOutHeight || resetOutputSize) {
		    outWidth &= ~1;
		    lastOutWidth = outWidth;
		    lastOutHeight = outHeight;
		    if (blitter != 0)
			xlibSetOutputSize(outWidth, outHeight);
		}
		long dataBytes = getNativeData(data);
		if (data == null)
		    return BUFFER_PROCESSED_OK;
		return (xlibDraw(data, dataBytes, bytesPerPixel))?
		    BUFFER_PROCESSED_OK : BUFFER_PROCESSED_FAILED;
	    } else
		return BUFFER_PROCESSED_FAILED;
	}
    }

    public Format [] getBaseInputFormats() {
	Format [] formats = new Format[1];
	formats[0] = new RGBFormat();
	return formats;
    }

    public Format [] getBaseOutputFormats() {
	Format [] formats = new Format[0];
	return formats;
    }

    protected synchronized void repaint() {
	if (!isStarted() && data != null)
	    doProcess(null, true);
    }
}
