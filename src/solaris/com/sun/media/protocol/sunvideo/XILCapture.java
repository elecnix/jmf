/*
 * @(#)XILCapture.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.sunvideo;

import java.awt.*;
import java.util.*;
import java.io.IOException;
import javax.media.*;
import javax.media.protocol.SourceStream;

/*************************************************************************
* This class shares XILLock with com.sun.media.renderer.video.XILRenderer.
* XIL 1.2 (Solaris 2.5, 2.5.1) is not multithread safe. All calls to the
* XIL library must be single threaded, whether for capture or rendering.
* Because of this common lock, calls that will result in delays are not
* to be encouraged since this will result in erratic performance of the
* renderer. For example, setting Jpeg quality high with a high frame
* rate will result in jerky rendering of video other than the capture
* stream.
************************************************************************/
public class XILCapture {

    private static boolean available = false;

    /**
     * Maintained by native code.
     */
    private long peer;

    /**
     * Referenced by native code.
     */
    private SourceStream stream;

    static {
	try {
	    com.sun.media.JMFSecurityManager.loadLibrary("jmutil");
	    com.sun.media.JMFSecurityManager.loadLibrary("jmxil");
	    if (xilInitialize() && cacheFieldIDs())
		available = true;
	    else
		System.err.println("XILCapture initialize failed");
	} catch (Exception e) {
		System.err.println("XILCapture initialize failed: " + e);
	} catch (UnsatisfiedLinkError ule) {
		System.err.println("XILCapture initialize failed: " + ule);
	}
    }

    static public boolean isAvailable() {
	return available;
    }

    public XILCapture(SourceStream stream) {
	this.stream = stream;
    }

    public boolean connect(int devnum) {
	return connect(devnum, 1);
    }

    public boolean connect(int devnum, int port) {
	if (available)
	    synchronized (com.sun.media.renderer.video.XILRenderer.XILLock) {
		return xilConnect(devnum, port);
	    }
	else
	    return false;
    }

    public boolean setPort(int port) {
	if (available)
	    synchronized (com.sun.media.renderer.video.XILRenderer.XILLock) {
		return xilSetPort(port);
	    }
	else
	    return false;
    }

    public boolean setScale(int scale) {
	if (available)
	    synchronized (com.sun.media.renderer.video.XILRenderer.XILLock) {
		return xilSetScale(scale);
	    }
	else
	    return false;
    }

    public boolean setSkip(int skip) {
	if (available)
	    synchronized (com.sun.media.renderer.video.XILRenderer.XILLock) {
		return xilSetSkip(skip);
	    }
	else
	    return false;
    }

    public boolean setQuality(int quality) {
	if (available)
	    synchronized (com.sun.media.renderer.video.XILRenderer.XILLock) {
		return xilSetQuality(quality);
	    }
	else
	    return false;
    }

    public boolean setCompress(String compress) {
	if (available)
	    synchronized (com.sun.media.renderer.video.XILRenderer.XILLock) {
		return xilSetCompress(compress);
	    }
	else
	    return false;
    }

    public boolean start() {
	if (available)
	    synchronized (com.sun.media.renderer.video.XILRenderer.XILLock) {
		return xilStart();
	    }
	else
	    return false;
    }

    public int read(byte[] buf, int len) throws IOException {
	int rlen = 0;
	if (available) {
	    synchronized (com.sun.media.renderer.video.XILRenderer.XILLock) {
		rlen = xilRead(buf, len);
	    }
	    if (rlen < 0) {
		throw (new IOException("XILCapture read() failed"));
	    }
	} else {
	    throw (new IOException("XILCapture shared library not available"));
	}
	return rlen;
    }

    public int getWidth() {
	if (available)
	    synchronized (com.sun.media.renderer.video.XILRenderer.XILLock) {
		return xilGetWidth();
	    }
	else
	    return 0;
    }

    public int getHeight() {
	if (available)
	    synchronized (com.sun.media.renderer.video.XILRenderer.XILLock) {
		return xilGetHeight();
	    }
	else
	    return 0;
    }

    public int getLineStride() {
	if (available)
	    synchronized (com.sun.media.renderer.video.XILRenderer.XILLock) {
		return xilGetLineStride();
	    }
	else
	    return 0;
    }

    public boolean stop() {
	if (available)
	    synchronized (com.sun.media.renderer.video.XILRenderer.XILLock) {
		return xilStop();
	    }
	else
	    return false;
    }

    public boolean disconnect() {
	if (available)
	    synchronized (com.sun.media.renderer.video.XILRenderer.XILLock) {
		return xilDisconnect();
	    }
	else
	    return false;
    }

    public void finalize() {
	if (available)
	    synchronized (com.sun.media.renderer.video.XILRenderer.XILLock) {
		xilDisconnect();
	    }
    }

    private static native boolean cacheFieldIDs();

    private static native boolean xilInitialize();
    
    private native boolean xilLittleEndian();
    
    private native boolean xilConnect(int devnum, int port);

    private native boolean xilSetPort(int port);
    
    private native boolean xilSetScale(int scale);
    
    private native boolean xilSetSkip(int skip);
    
    private native boolean xilSetQuality(int quality);
    
    private native boolean xilSetCompress(String compress);

    private native boolean xilStart();

    private native int xilRead(byte[] buf, int len);

    private native int xilGetWidth();

    private native int xilGetHeight();

    private native int xilGetLineStride();

    private native boolean xilStop();

    private native boolean xilDisconnect();

}

