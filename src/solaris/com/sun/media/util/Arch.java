/*
 * @(#)Arch.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

/**
 * A class that contains platform specific information.
 */
public class Arch {

    public final static int SPARC = 1;
    public final static int UNIX  = 2;
    public final static int WIN32 = 4;
    public final static int SOLARIS = 8;
    public final static int LINUX = 16;
    public final static int X86 = 32;
    
    /**
     * Returns true if the byte ordering is big endian
     */
    public static boolean isBigEndian() {
	return true;
    }

    /**
     * Returns true if the byte ordering is little endian.
     */
    public static boolean isLittleEndian() {
	return false;
    }

    /**
     * Returns the byte alignment for integers. For example on win32, an int
     * can be aligned to any byte in memory, so the return value would be 1.
     * Return value for solaris is 4.
     */
    public static int getAlignment() {
	return 4;
    }

    public static int getArch() {
	return SOLARIS | UNIX | SPARC;
    }
}
    
