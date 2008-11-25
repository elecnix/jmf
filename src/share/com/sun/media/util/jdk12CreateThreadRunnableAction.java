/*
 * @(#)jdk12CreateThreadRunnableAction.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import java.security.PrivilegedAction;

public
class jdk12CreateThreadRunnableAction  implements java.security.PrivilegedAction {

    private Class threadclass;
    private Runnable runnable;
    private String name = null;

    public static Constructor cons;
    public static Constructor conswithname;

    static {
	try {
	    cons = jdk12CreateThreadRunnableAction.class.getConstructor(new Class[] {
		Class.class, Runnable.class});
	    conswithname = jdk12CreateThreadRunnableAction.class.getConstructor(new Class[] {
		Class.class, Runnable.class, String.class});
	} catch (Throwable e) {
	}
    }

    public jdk12CreateThreadRunnableAction(Class threadclass,
						Runnable run, String name) {
	
	try {
	    this.threadclass = threadclass;
	    runnable = run;
	    this.name = name;
	} catch (Throwable e) {
	}
	
    }

    public jdk12CreateThreadRunnableAction(Class threadclass, Runnable run) {
	this(threadclass, run, null);
    }
    
    public Object run() {
	try {
	    Constructor cons = threadclass.getConstructor(new Class[] {Runnable.class});

	    Object object = cons.newInstance(new Object[] {runnable});
	    if (name != null) {
		((Thread) object).setName(name);
	    }
	    return object;
	} catch (Throwable e) {
	    return null;
	}
    }
}


