/*
 * @(#)VFWCapture.java	1.16 03/04/25
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.vfw;

import java.awt.*;
import java.util.*;
import javax.media.*;
import com.sun.media.vfw.*;

public class VFWCapture {

    private static int globalnID = 0x00022100;

    static {
	com.sun.media.JMFSecurityManager.loadLibrary("jmvfw");
	cacheFieldIDs();
    }
    
    public static synchronized int getNextID() {
	return globalnID++;
    }

    static native boolean cacheFieldIDs();

    static native int capSetWindowPos(int hWnd,
					     int x, int y,
					     int width, int height);
    
    public static native int capCreateCaptureWindow(String title,
						    int parentHandle,
						    int x, int y,
						    int width, int height,
						    int nID);

    public static native String capGetDriverDescriptionName(int driverID);

    static native String capGetDriverDescriptionDesc(int driverID);

    static native boolean capCaptureAbort(int hWnd);
    
    static native boolean capCaptureGetSetup(int hWnd,
						    CaptureParms cp);
    
    static native boolean capCaptureSetSetup(int hWnd,
						    CaptureParms cp);
    
    static native boolean capCaptureSequence(int hWnd);

    static native boolean capCaptureSequenceNoFile(int hWnd);

    static native boolean capCaptureStop(int hWnd);

    static native boolean capDlgVideoCompression(int hWnd);

    static native boolean capDlgVideoDisplay(int hWnd);

    public static native boolean capDlgVideoFormat(int hWnd);

    static native boolean capDlgVideoSource(int hWnd);

    public static native boolean capDriverConnect(int hWnd, int driverIndex);

    public static native boolean capDriverDisconnect(int hWnd);

    static native boolean capDriverGetCaps(int hWnd, CapDriverCaps cp);

    public static native String capDriverGetName(int hWnd);

    static native String capDriverGetVersion(int hWnd);

    static native boolean capGetAudioFormat(int hWnd, WaveFormatEx wfe);

    static native boolean capGetStatus(int hWnd, CapStatus cs);

    public static native boolean capGetVideoFormat(int hWnd, BitMapInfo bmi);

    static native boolean capOverlay(int hWnd, boolean overlay);

    static native boolean capPreview(int hWnd, boolean preview);

    static native boolean capPreviewRate(int hWnd, int millis);

    static native boolean capPreviewScale(int hWnd, boolean scale);

    static native boolean capSetAudioFormat(int hWnd, WaveFormatEx wfe);

    static native boolean capSetVideoFormat(int hWnd, BitMapInfo bmi);

    
    static native int createFrameCallback(int hWnd);

    static native void startFrameCallback(int hWnd, int cbHandle);

    static native void stopFrameCallback(int hWnd, int cbHandle);

    static native void destroyFrameCallback(int hWnd, int cbHandle);

    static native int getAvailableData(int hWnd, int cbHandle,
					      Object data, long dataBytes,
					      int dataLength,
					      long [] resultTimeStamp);

    static native void destroyWindow(int hWnd);

    static native int  createWindow(String title);

    // value = 0 for hide, 1 for minimal, 2 for normal
    static native void  showWindow(int hWnd, int value, int width, int height);

    static native int  peekWindowLoop(int window);
}


