/*
 * @(#)OPICapture.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.sunvideoplus;

import java.awt.*;
import java.util.*;
import java.io.IOException;
import javax.media.*;
import javax.media.protocol.SourceStream;

public class OPICapture {

    private static boolean available = false;

    private static Integer OPILock = new Integer(1);

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
	    com.sun.media.JMFSecurityManager.loadLibrary("jmopi");
	    if (opiInitialize() && cacheFieldIDs())
		available = true;
	    else
		System.err.println("OPICapture initialize failed");
	} catch (Exception e) {
		System.err.println("OPICapture initialize failed: " + e);
	} catch (UnsatisfiedLinkError ule) {
		System.err.println("OPICapture initialize failed: " + ule);
	}
    }
 
    static public boolean isAvailable() {
	return available;
    }

    public OPICapture(SourceStream stream) {
	this.stream = stream;
    }

    public boolean connect(int devnum) {
	return connect(devnum, 1);
    }

    public boolean connect(int devnum, int port) {
	if (available)
	    synchronized (OPILock) {
		return opiConnect(devnum, port);
	    }
	else
	    return false;
    }

    public boolean setPort(int port) {
	if (available)
	    synchronized (OPILock) {
		return opiSetPort(port);
	    }
	else
	    return false;
    }

    public boolean setSignal(String signal) {
	if (available)
	    synchronized (OPILock) {
		return opiSetSignal(signal);
	    }
	else
	    return false;
    }

    public boolean setCompress(String compress) {
	if (available)
	    synchronized (OPILock) {
		return opiSetCompress(compress);
	    }
	else
	    return false;
    }

    public boolean setScale(int scale) {
	if (available)
	    synchronized (OPILock) {
		return opiSetScale(scale);
	    }
	else
	    return false;
    }

    public boolean setFrameRate(int fps) {
	if (available)
	    synchronized (OPILock) {
		return opiSetFrameRate(fps);
	    }
	else
	    return false;
    }

    public boolean setQuality(int quality) {
	if (available)
	    synchronized (OPILock) {
		return opiSetQuality(quality);
	    }
	else
	    return false;
    }

    public boolean setBitRate(int bitrate) {
	if (available)
	    synchronized (OPILock) {
		return opiSetBitRate(bitrate);
	    }
	else
	    return false;
    }

    public boolean start() {
	if (available)
	    synchronized (OPILock) {
		return opiStart();
	    }
	else
	    return false;
    }

    public int read(byte[] buf, int len) throws IOException {
	int rlen = 0;
	if (available) {
	    synchronized (OPILock) {
		rlen = opiRead(buf, len);
	    }
	    if (rlen < 0) {
		throw (new IOException("OPICapture read() failed"));
	    }
	} else {
	    throw (new IOException("OPICapture shared library not available"));
	}
	return rlen;
    }

    public int getWidth() {
	if (available)
	    synchronized (OPILock) {
		return opiGetWidth();
	    }
	else
	    return 0;
    }

    public int getHeight() {
	if (available)
	    synchronized (OPILock) {
		return opiGetHeight();
	    }
	else
	    return 0;
    }

    public int getLineStride() {
	if (available)
	    synchronized (OPILock) {
		return opiGetLineStride();
	    }
	else
	    return 0;
    }

    public boolean stop() {
	if (available)
	    synchronized (OPILock) {
		return opiStop();
	    }
	else
	    return false;
    }

    public boolean disconnect() {
	if (available)
	    synchronized (OPILock) {
		return opiDisconnect();
	    }
	else
	    return false;
    }

    public void finalize() {
	if (available)
	    synchronized (OPILock) {
		opiDisconnect();
	    }
    }

    private static native boolean cacheFieldIDs();

    private static native boolean opiInitialize();
    
    private native boolean opiLittleEndian();
    
    private native boolean opiConnect(int devnum, int port);

    private native boolean opiSetPort(int port);
    
    private native boolean opiSetSignal(String signal);

    private native boolean opiSetCompress(String compress);

    private native boolean opiSetScale(int scale);
    
    private native boolean opiSetFrameRate(int fps);
    
    private native boolean opiSetQuality(int quality);
    
    private native boolean opiSetBitRate(int bitrate);
    
    private native boolean opiStart();

    private native int opiRead(byte[] buf, int len);

    private native int opiGetWidth();

    private native int opiGetHeight();

    private native int opiGetLineStride();

    private native boolean opiStop();

    private native boolean opiDisconnect();

}

