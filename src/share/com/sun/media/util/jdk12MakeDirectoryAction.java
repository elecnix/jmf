/*
 * @(#)jdk12MakeDirectoryAction.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.security.PrivilegedAction;
import java.lang.reflect.Constructor;
import java.io.*;

public
class jdk12MakeDirectoryAction implements java.security.PrivilegedAction {

    public static Constructor cons;
    private File file;
    private static Boolean TRUE = new Boolean(true);
    private static Boolean FALSE = new Boolean(false);
    static {
	try {
	    cons = jdk12MakeDirectoryAction.class.getConstructor(new Class[] {
	     File.class});
	} catch (Throwable e) {
	}
     }

    public jdk12MakeDirectoryAction(File file) {
	this.file = file;
    }

    public Object run() {
	try {
	    if (file != null) {
		if (file.exists() || file.mkdirs())
		    return TRUE;
		else
		    return FALSE;
	    } else {
		return FALSE;
	    }
	} catch (Throwable e) {
	    return null;
	}
    }
}
