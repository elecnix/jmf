/*
 * @(#)SunRayRenderer.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.video;

import javax.media.*;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;
import javax.media.renderer.VideoRenderer;
import java.awt.*;
import java.awt.image.*;
import java.util.Vector;
import java.io.File;
import com.sun.media.*;
import com.sun.media.util.WindowUtil;
import com.sun.media.util.MediaThread;


public class SunRayRenderer extends BasicVideoRenderer
implements com.sun.media.util.DynamicPlugIn {

    private static boolean available = false;
    private static int jawtAvail = WindowUtil.getJAWTAvail();

    private static boolean debug = false;

    private    int        blitter     = 0;
    protected  Object     data        = null;

    private    int        offsetX = 0;
    private    int        offsetY = 0;

    private    int        yuvType;
    private    int        yOff;
    private    int        uOff;
    private    int        vOff;
    private    int        yStride;
    private    int        uvStride;
    private    int        lastOutWidth = -1;
    private    int        lastOutHeight = -1;
    private    boolean    firstTime = true;

    private    EventThread  xeventThread = null;
    
    private native void srSetJAWT(int jawt);
    private synchronized native boolean srInitialize();
    private synchronized native boolean srDisplayIsSunRay();
    private synchronized native boolean srSetComponent(Object component);
    private synchronized native boolean srSetInputFormat(int width, int height,
						   int offY, int offU, int offV,
						   int strideY, int strideUV,
						   int subX, int subY);
    private synchronized native boolean srSetOutputSize(int width, int height);
    private synchronized native boolean srDraw(Object data);
    private synchronized native boolean srFree();
    private synchronized native boolean srEventCheck();

    static {
	if (debug) {
	    System.err.println("SunRayRenderer static entered");
	}
	try {
	    Object jmfSecurity = JMFSecurityManager.getJMFSecurity();
	    if (jmfSecurity != null &&
    jmfSecurity.getClass().getName().equals("com.sun.media.NetscapeSecurity")) {
		// Netscape only supports green threads and libutmedia
		// uses pthread_mutex so requires native threads.
		if (debug) {
		    System.err.println("SunRayRenderer under Netscape");
		}
		available = false;
	    } else {
		// avoid load failed messages on non-SunRay systems.
		// Note: doesn't work if Sun Ray server installed in
		// non-standard directory.
		File lib = new File("/opt/SUNWut/lib/libutmedia.so");
		if (lib.exists()) {
		    JMFSecurityManager.loadLibrary("jmutil");
		    JMFSecurityManager.loadLibrary("jmsunray");
		    available = true;
		} else {
		    if (debug) {
			System.err.println("SunRayRenderer no libutmedia");
		    }
		    available = false;
		}
	    }
	} catch (UnsatisfiedLinkError ule) {
	    if (debug) {
		System.err.println("SunRayRenderer load libjmsunray failed");
	    }
	    available = false;
	} catch (SecurityException se) {
	    if (debug) {
		System.err.println("SunRayRenderer security exception");
	    }
	    available = false;
	} catch (NullPointerException npe) {
	    if (debug) {
		System.err.println("SunRayRenderer null pointer exception");
	    }
	    available = false;
	} catch (Exception e) {
	    System.err.println("SunRayRenderer exception: " + e);
	    e.printStackTrace();
	    available = false;
	}
    }

    public SunRayRenderer() {
	super("SunRay Renderer");
	if (debug) {
	    System.err.println("SunRayRenderer <init>() entered");
	}
	supportedFormats = new VideoFormat[] {
				new YUVFormat(YUVFormat.YUV_411),
				new YUVFormat(YUVFormat.YUV_420),
				new YUVFormat(YUVFormat.YUV_422),
				new YUVFormat(YUVFormat.YUV_111)
	};
	if (!available) {
//	    throw new RuntimeException("Could not load jmsunray library");
	    return;
	}

	srSetJAWT(jawtAvail);

	if (!srDisplayIsSunRay()) {
	    if (debug) {
		System.err.println("SunRayRenderer display is not a Sun Ray ");
	    }
	    available = false;
//	    throw new RuntimeException("Display is not a Sun Ray");
	    return;
	}
	if (!srInitialize()) {
	    available = false;
	}
	if (debug) {
	    System.err.println("SunRayRenderer <init>() completed");
	}
    }

    public void open() throws ResourceUnavailableException {

	if (debug) {
	    System.err.println("SunRayRenderer open() entered");
	}
	if (!available) {
	    if (debug) {
		System.err.println("SunRayRenderer Sun Ray not available");
	    }
	    throw new ResourceUnavailableException("Sun Ray not available");
	}
    }

    public void reset() {
    }

    public synchronized void close() {
	if (debug) {
	    System.err.println("SunRayRenderer close() entered");
	}
	if (xeventThread != null) {
	    xeventThread.stopChecking();
	    xeventThread.interrupt();
	    while (xeventThread.isAlive()) {
		try {
		    Thread.currentThread().sleep(5);
		} catch (InterruptedException ex) {
		}
	    }
	    xeventThread = null;
	}
	if (available && blitter != 0)
	    srFree();
	if (debug) {
	    System.err.println("SunRayRenderer close() complete");
	}
    }

    /**
     * Set the data input format.
     * @return false if the format is not supported.
     */
    public Format setInputFormat(Format format) {
	if (debug) {
	    System.err.println("SunRayRenderer setInputFormat() entered");
	}
	if (!available) {
	    if (debug) {
		System.err.println("SunRayRenderer setInputFormat !available");
	    }
	    return null;
	}

	if (!(format instanceof YUVFormat))
	    return null;
	if (super.setInputFormat(format) != null) {
	    if (inputFormat instanceof YUVFormat) {
		YUVFormat yuvf = (YUVFormat) inputFormat;
		yuvType = yuvf.getYuvType();
		yOff = yuvf.getOffsetY();
		uOff = yuvf.getOffsetU();
		vOff = yuvf.getOffsetV();
		yStride = yuvf.getStrideY();
		uvStride = yuvf.getStrideUV();
	    } else
		return null;

	    int subx = 4;	// assume 411
	    int suby = 4;
	    if (yuvType == YUVFormat.YUV_422) {
		subx = 2;
	        suby = 2;
	    } else if (yuvType == YUVFormat.YUV_111) {
		subx = 1;
		suby = 1;
	    } else if (yuvType == YUVFormat.YUV_420) {
		subx = 2;
	        suby = 2;
	    }

	    // Inform the native code of the input format change
	    srSetInputFormat(inWidth, inHeight,
				yOff, uOff, vOff,
				yStride, uvStride,
				subx, suby);
	    
	    if (outWidth == -1 || outHeight == -1) {
		outWidth = inWidth;
		outHeight = inHeight;
	    }

	    if (component != null)
		component.setSize(outWidth, outHeight);
	    // All's well
	    if (debug) {
		System.err.println("SunRayRenderer setInputFormat() returned "
								+ format);
	    }
	    return format;
	} else {
	    // Unsupported format
	    return null;
	}
    }

    protected void setAvailable(boolean on) {
	if (debug) {
	    System.err.println("SunRayRenderer setAvailable() entered");
	}
	Thread xevt = null;
	super.setAvailable(on);
	synchronized (this) {
	    if (on) {
		if (xeventThread == null) {
		    xeventThread = new EventThread(this);
		    xeventThread.start();
		}
	    } else {
		if (xeventThread != null) {
		    xeventThread.stopChecking();
		    xeventThread.interrupt();
		    xevt = xeventThread;
		    xeventThread = null;
		    data = null;
		}
	    }
	}
	// wait for the thread to end outside the synchronized block
	// to avoid a deadlock
	if (xevt != null) {
	    while (xevt.isAlive()) {
		try {
		    Thread.currentThread().sleep(5);
		} catch (InterruptedException ex) {
		}
	    }
	}
    }

    protected synchronized void removingComponent() {
	if (debug) {
	    System.err.println("SunRayRenderer removingComponent() entered");
	}
	if (blitter != 0) {
	    srFree();
	}
    }

    protected int doProcess(Buffer buffer) {
	return doProcess(buffer, false);
    }

    protected int doProcess(Buffer buffer, boolean repaint) {
//	if (debug) {
//	    System.err.println("SunRayRenderer doProcess() entered");
//	}
	if (!available || component == null)
	    return BUFFER_PROCESSED_OK;
	synchronized (this) {
	    if (!componentAvailable)
		return BUFFER_PROCESSED_OK;
	    if (blitter == 0 || firstTime) {
		firstTime = false;
		if (blitter == 0)
		    srInitialize();
		int handle = com.sun.media.util.WindowUtil.getWindowHandle(component);
		if (handle != 0) {
		    srSetComponent(component);
		    // Reset it to force calling setInputFormat
		    inputFormat = new VideoFormat(null);
		}
		if (blitter == 0) {
		    if (debug) {
			System.err.println("Could not create blitter");
		    }
		    return BUFFER_PROCESSED_FAILED;
		}
	    }

	    if (!repaint) {
		if (!buffer.getFormat().equals(inputFormat)) {
		    if (setInputFormat(buffer.getFormat()) == null)
			return BUFFER_PROCESSED_FAILED;
		}
		
		data = buffer.getData();
	    } else
		if (data == null)
		    return BUFFER_PROCESSED_FAILED;
	    
	    if (outWidth > 0 && outHeight > 0) {
		if (outWidth != lastOutWidth || outHeight != lastOutHeight) {
		    outWidth &= ~1;
		    lastOutWidth = outWidth;
		    lastOutHeight = outHeight;
		    
		    srSetOutputSize(outWidth, outHeight);
		    if (debug) {
			System.err.println("SunRayRenderer srSetOutputSize() "
					+ outWidth + "x" + outHeight);
		    }
		}
		return (srDraw(data))?
		    BUFFER_PROCESSED_OK : BUFFER_PROCESSED_FAILED;
	    } else
		return BUFFER_PROCESSED_FAILED;
	}
    }

    public Format [] getBaseInputFormats() {
	if (debug) {
	    System.err.println("SunRayRenderer getBaseInputFormats() entered");
	}
	Format [] formats = new Format[1];
	formats[0] = new YUVFormat();
	return formats;
    }

    public Format [] getBaseOutputFormats() {
	if (debug) {
	    System.err.println("SunRayRenderer getBaseOutputFormats() entered");
	}
	Format [] formats = new Format[0];
	return formats;
    }

    protected synchronized void repaint() {
	if (debug) {
	    System.err.println("SunRayRenderer repaint() entered");
	}
	if (!isStarted() && data != null)
	    doProcess(null, true);
    }

    /****************************************************************
     * INNER CLASSES
     ****************************************************************/

    /*
     * This class establishes a thread that invokes the native code
     * to check for X events (move, resize, expose, clip region change)
     */
    class EventThread extends MediaThread {
	SunRayRenderer renderer;
	private    boolean    checkEvents = true;

	public EventThread(SunRayRenderer renderer) {
	    super("SunRayRenderer Event Thread");
	    this.renderer = renderer;
	    useVideoPriority();
	}

	public void stopChecking() {
	    checkEvents = false;
	}

	public void run() {
	    while(checkEvents) {
		// srEventCheck will return true for resize or repaint
		if (renderer.srEventCheck()) {
		    renderer.repaint();
		}
		try {
		    sleep(5);
		} catch (InterruptedException ex) {
		}
	    }
	    renderer = null;
	}
 
    }		// end of EventThread

}
