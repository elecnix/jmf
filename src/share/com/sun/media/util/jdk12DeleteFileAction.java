/*
 * @(#)jdk12DeleteFileAction.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.security.PrivilegedAction;
import java.lang.reflect.Constructor;
import java.io.*;

public
class jdk12DeleteFileAction  implements java.security.PrivilegedAction {

    public static Constructor cons;
    private File file;
    private static Boolean TRUE = new Boolean(true);
    private static Boolean FALSE = new Boolean(false);

    static {
	try {
	    cons = jdk12DeleteFileAction.class.getConstructor(new Class[] {
		File.class});
	} catch (Throwable e) {
	}
    }


    public jdk12DeleteFileAction(File file) {
	
	try {
	    this.file = file;
	} catch (Throwable e) {
	}
    }

    public Object run() {
	try {
	    if (file != null) {
		if (file.delete())
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
