/*
 * @(#)jdk12ConnectionAction.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.security.PrivilegedAction;
import java.lang.reflect.Constructor;
import java.net.URLConnection;

public
class jdk12ConnectionAction  implements java.security.PrivilegedAction {

    public static Constructor cons;
    private URLConnection urlC;

    static {
	try {
	    cons = jdk12ConnectionAction.class.getConstructor(new Class[] {
		URLConnection.class});
	} catch (Throwable e) {
	}
    }


    public jdk12ConnectionAction(URLConnection urlC) {
	
	try {
	    this.urlC = urlC;
	} catch (Throwable e) {
	}
    }

    public Object run() {
	try {
	    return urlC.getInputStream();
	} catch (Throwable t) {
	    return null;
	}
    }

}
