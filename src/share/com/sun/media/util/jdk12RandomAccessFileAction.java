/*
 * @(#)jdk12RandomAccessFileAction.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.util;

import java.security.PrivilegedAction;
import java.lang.reflect.Constructor;
import java.io.*;

public
class jdk12RandomAccessFileAction implements java.security.PrivilegedAction {

    public static Constructor cons;
    private String name;
    private String mode;

    static {
	try {
	    cons = jdk12RandomAccessFileAction.class.getConstructor(new Class[] {
	     String.class, String.class});
	} catch (Throwable e) {
	}
     }

    public jdk12RandomAccessFileAction(String name, String mode) {
	boolean rw = mode.equals("rw");
	if (!rw)
	    mode = "r";
	this.mode = mode;
	this.name = name;
    }

    public Object run() {
	try {
	    return new RandomAccessFile(name, mode);
	} catch (Throwable e) {
	    return null;
	}
    }
}

