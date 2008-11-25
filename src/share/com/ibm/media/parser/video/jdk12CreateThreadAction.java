/*
 * @(#)jdk12CreateThreadAction.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.parser.video;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import java.security.PrivilegedAction;

public class jdk12CreateThreadAction  implements java.security.PrivilegedAction {

    private Class threadclass;
    private String name = null;
    public static Constructor cons;
    public static Constructor conswithname;

    static {
	try {
	    cons = jdk12CreateThreadAction.class.getConstructor(new Class[] {
		Class.class});
	    conswithname = jdk12CreateThreadAction.class.getConstructor(new Class[] {
		Class.class, String.class});
	} catch (Throwable e) {
	}
    }

    public jdk12CreateThreadAction(Class threadclass, String name) {
	
	try {
	    this.threadclass = threadclass;
	    this.name = name;
	} catch (Throwable e) {
	}
	
    }

    public jdk12CreateThreadAction(Class threadclass) {
	this(threadclass, null);
    }
    
    public Object run() {
	try {
	    Object object = threadclass.newInstance();
	    if (name != null) {
		((Thread) object).setName(name);
	    }
	    return object;
	} catch (Throwable e) {
// 	    System.out.println("audiodevice run: throws " + e);
	    return null;
	}
    }
}
