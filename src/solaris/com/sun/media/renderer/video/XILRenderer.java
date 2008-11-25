/*
 * @(#)XILRenderer.java	1.36 03/04/23
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

public final class XILRenderer extends BasicVideoRenderer
implements com.sun.media.util.DynamicPlugIn {

    private static boolean available = true;

    /****************************************************************
    * XILLock shared with com.sun.media.protocol.sunvideo.XILCapture
    * to serialize use of the XIL library for both rendering and
    * SunVideo capture.
    * NOTE: Delays in XILCapture routines will affect XIL rendering.
    * For example, setting Jpeg capture quality high with a high
    * frame rate results in jerky rendering of video other than
    * the capture stream.
    ****************************************************************/
    public static Integer XILLock = new Integer(1);

    private    int        nativeData    = 0;
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

    private    int        oldWidth = -1;
    private    int        oldHeight = -1;
    
    private synchronized native boolean xilInitialize();
    private synchronized native boolean xilSetComponent(int windowHandle, int bitsPP);
    private synchronized native boolean xilSetInputFormat(int width, int height,
							 int strideX);
    private synchronized native boolean xilSetOutputSize(int width, int height);
    private synchronized native boolean xilDraw(int x, int y,
						int width, int height, Object data,
						long dataBytes);
    private synchronized native boolean xilFree();

    protected VideoFormat defaultFormat;

    public XILRenderer() {
	super("XIL Renderer");
	boolean greenOnlyVM = false;
	try {
	    Object jmfSecurity = JMFSecurityManager.getJMFSecurity();
	    if (jmfSecurity.getClass().getName().equals("com.sun.media.NetscapeSecurity"))
		greenOnlyVM = true;
	} catch (SecurityException se) {
	} catch (NullPointerException npe) {
	}
	if (!available || !WindowUtil.canUseXIL(greenOnlyVM)) {
	    available = false;
	    throw new RuntimeException("No XIL on Solaris 2.6 w/Green Threads");
	}
	try {
	    JMFSecurityManager.loadLibrary("jmutil");
	    JMFSecurityManager.loadLibrary("jmxil");
	} catch (Exception e) {
	    available = false;
	} catch (UnsatisfiedLinkError ule) {
	    available = false;
	}
	if (!available)
	    throw new RuntimeException("Could not load libjmxil library");
	synchronized (XILLock) {
	    if (available)
		if (xilInitialize()) {
		    /*
		      if (defBitsPerPixel == 8) {
		      defaultFormat = new IndexedColorFormat(null, 0,
		      Format.byteArray,
		      256,
		      null, null, null,
		      0,
		      Format.DATA_TYPE |
		      IndexedColorFormat.MAP_SIZE);
		      
		      } else {
		    */
		    defaultFormat = new RGBFormat(null, Format.NOT_SPECIFIED,
						  Format.intArray,
						  Format.NOT_SPECIFIED, // frame rate
						  32, 
						  defRedMask, defGreenMask, defBlueMask,
						  1,
						  Format.NOT_SPECIFIED,
						  Format.FALSE, // flipped
						  Format.NOT_SPECIFIED); // endian

		    /*}*/
		    supportedFormats = new VideoFormat[1];
		    supportedFormats[0] = defaultFormat;
		    
		} else {
		    System.err.println("XILRenderer.xilInitialize() failed");
		    available = false;
		}
	}
    }

    public void open() throws ResourceUnavailableException {

	if (!available)
	    throw new ResourceUnavailableException("XIL not available");
    }

    public void reset() {
    }
    
    public synchronized void close() {
	synchronized (XILLock) {
	    if (available && nativeData != 0)
		xilFree();
	    nativeData = 0;
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
	    synchronized (XILLock) {
		xilSetInputFormat(inWidth, inHeight, lineStride);
		componentChanged = false;
	    }
	    
	    if (outWidth == -1 || outHeight == -1) {
		outWidth = inWidth;
		outHeight = inHeight;
	    }
	    // Determine the bytesPerPixel of the data buffer
	    Class dataType =  inputFormat.getDataType();
	    if (dataType == Format.intArray)
		bytesPerPixel = 4;
	    else if (dataType == Format.shortArray)
		bytesPerPixel = 2;
	    else if (dataType == Format.byteArray)
		bytesPerPixel = 1;
	    /*
	    if (component != null)
		component.setSize(outWidth, outHeight);
	    */
	    // All's well
	    return format;
	} else {
	    // Unsupported format
	    return null;
	}
    }

    protected synchronized void removingComponent() {
	close();
    }

    protected int doProcess(Buffer buffer) {
	return doProcess(buffer, false);
    }

    boolean componentChanged = false;

    protected int doProcess(Buffer buffer, boolean repaint) {
	
	if (!available || component == null)
	    return BUFFER_PROCESSED_OK;
	/*
	if (outWidth != oldWidth || outHeight != oldHeight) {
	    oldHeight = outHeight;
	    oldWidth = outWidth;
	    close();
	    Toolkit.getDefaultToolkit().sync();
	}
	*/
	synchronized (this) {
	    if (!componentAvailable || !component.isShowing())
		return BUFFER_PROCESSED_OK;
	    
	    if (nativeData == 0) {
		Toolkit.getDefaultToolkit().sync();
		int handle = com.sun.media.util.WindowUtil.getWindowHandle(component);
		if (handle != 0) {
		    synchronized (XILLock) {
			xilSetComponent(handle, defBitsPerPixel);
		    }
		    // Reset it to force calling setInputFormat
		    inputFormat = new VideoFormat(null);
		    componentChanged = true;
		    if (nativeData == 0) {
			return BUFFER_PROCESSED_FAILED;
		    }
		} else
		    return BUFFER_PROCESSED_OK;
	    }

	    if (!repaint) {
		data = null;
		if (!buffer.getFormat().equals(inputFormat)) {
		    if (setInputFormat(buffer.getFormat()) == null)
			return BUFFER_PROCESSED_FAILED;
		}

		if (buffer instanceof ExtBuffer) {
		    ((ExtBuffer)buffer).setNativePreferred(true);
		    data = ((ExtBuffer)buffer).getNativeData();
		}
		if (data == null)
		    data = buffer.getData();
	    } else
		if (data == null || componentChanged)
		    return BUFFER_PROCESSED_FAILED;
	    
	    synchronized (XILLock) {
		if (outWidth > 0 && outHeight > 0) {
		    long dataBytes = 0;
		    if (data instanceof NBA)
			dataBytes = ((NBA)data).getNativeData();
		    if (data == null)
			return BUFFER_PROCESSED_OK;
		    //System.err.println("dataBytes = " + dataBytes);
		    return (xilDraw(offsetX, offsetY, outWidth, outHeight, data, dataBytes))?
			BUFFER_PROCESSED_OK : BUFFER_PROCESSED_FAILED;
		}
		else
		    return BUFFER_PROCESSED_FAILED;
	    }
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
