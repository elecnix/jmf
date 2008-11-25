/*
 * @(#)DrawingSurfaceJAWT.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.awt.*;
import com.sun.media.JMFSecurityManager;

public class DrawingSurfaceJAWT {
    public static final int valid = 0;
    public static final int pawt = 1;
    public static final int pds = 2;
    public static final int pwinid = 3;
    public static final int pdisp = 4;

    private static boolean avail = true;
    static {
	try {
	    Toolkit.getDefaultToolkit();
	    JMFSecurityManager.loadLibrary("jmfjawt");
	    // System.out.println("in dsJAWT after loading jmfjawt");
	    avail = true;
	} catch(Throwable t) {
	   t.printStackTrace();
	   avail = false;
	}

    }

    public static native int getWindowHandle(Component c);
    public static native boolean lockAWT(int dsObj);
    public static native void unlockAWT(int dsObj);
    public static native void freeResource(int awtObj, int dsObj);
    public native int getAWT();
    public native int getDrawingSurface(Component c, int awtObj);
    public native int getDrawingSurfaceWinID(int dsObj);
    public native int getDrawingSurfaceDisplay(int dsObj);

    int[] winfo = null;

    public DrawingSurfaceJAWT() {
	if ( !avail ) {
	    throw new RuntimeException("can't load jmfjawt native module");
	}

	// System.out.println("in DrawingSurfaceJAWT constructor");
	winfo = new int[5];
	for ( int i = 0; i < 5; i++)
	    winfo[i] = 0;

    }

    public int[] getWindowInfo(Component cc) {
	int value = 0;
	value  = getAWT();
	if ( value == 0 ) {
	    winfo[valid] = 0;
	    return winfo;
	}
	winfo[pawt] = value;
	
	value = getDrawingSurface(cc, winfo[pawt]);
	if ( value == 0 ) {
	    winfo[valid] = 0;
	    return winfo;
	}

	winfo[pds] = value;
	
	value = getDrawingSurfaceWinID(winfo[pds]);
	if ( value == 0 ) {
	    winfo[valid] = 0;
	    return winfo;
	}
	    
	winfo[pwinid] = value;

	value = getDrawingSurfaceDisplay(winfo[pds]);
	if ( value == 0 ) {
	    winfo[valid] = 0;
	    return winfo;
	}

	winfo[pdisp] = value;
	winfo[valid] = 1;
	
	return winfo;
    }
   

}
