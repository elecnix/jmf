/*
 * @(#)WindowUtil.java	1.12 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.awt.*;
import com.sun.media.JMFSecurityManager;

public final class WindowUtil {
    private static int jawtAvail = 0;
    static {
	String javaVersion = null;
	String subver = null;
	int len = 0;
	try {
	    javaVersion = (String)System.getProperty("java.version");
	    if ( javaVersion.length() < 3)
		len = javaVersion.length();
	    else
		len = 3;
	    subver = javaVersion.substring(0,len);
	} catch (Throwable t) {
	    javaVersion = null;
	    subver=null;
	}
	
	if ( subver == null || subver.compareTo("1.3") <= 0) {
	    jawtAvail = 0;
	} else {
	    jawtAvail = 1;
	}
	
	try {
	    if (jawtAvail == 1)
		JMFSecurityManager.loadLibrary("jawt");
	} catch (Exception e) {
	    // Ignore the exception: most likely because jawt had
	    // already been loaded.
	} catch (UnsatisfiedLinkError e) {
	    // Ignore the exception: most likely because jawt had
	    // already been loaded.
	}

	try {
	    JMFSecurityManager.loadLibrary("jmutil");
	} catch (Exception e) {
	    System.err.println("Could not load library jmutil native module");
	    e.printStackTrace();
	} catch (UnsatisfiedLinkError ule) {
	    System.err.println("Could not load library jmutil native module");
	    ule.printStackTrace();
	}
    }
    
    public native static int getWindowHandle(Component c, int jawt);
    public native static boolean canUseXIL(boolean greenOnlyVM);
    public native static boolean isUltra();
    
    public static int getWindowHandle(Component c) {
	return getWindowHandle(c, jawtAvail);
    }
    
    public static int getJAWTAvail() {
	return jawtAvail;
    }
    
}
