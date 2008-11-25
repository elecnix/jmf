/*
 * @(#)jdk12PropertyAction.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.security.PrivilegedAction;
import java.lang.reflect.Constructor;

public
class jdk12PropertyAction  implements java.security.PrivilegedAction {

    public static Constructor cons;
    private String name;

    static {
	try {
	    cons = jdk12PropertyAction.class.getConstructor(new Class[] {
		String.class});
	} catch (Throwable e) {
	}
    }


    public jdk12PropertyAction(String name) {
	
	try {
	    this.name = name;
	} catch (Throwable e) {
	}
    }

    public Object run() {
	try {
	    return System.getProperty(name);
	} catch (Throwable t) {
	    return null;
	}
    }

}
