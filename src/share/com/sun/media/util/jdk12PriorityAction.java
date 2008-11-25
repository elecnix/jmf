/*
 * @(#)jdk12PriorityAction.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.security.PrivilegedAction;
import java.lang.reflect.Constructor;

public class jdk12PriorityAction  implements java.security.PrivilegedAction {

    private Thread t;
    private int priority;
    public static Constructor cons;

    static {
	try {
	    cons = jdk12PriorityAction.class.getConstructor(new Class[] {
		Thread.class, int.class});
	} catch (Throwable e) {
	}
    }

    public jdk12PriorityAction (Thread t, int priority) {
	this.t = t;
	this.priority = priority;
    }

    public Object run() {
	try {
	    t.setPriority(priority);
	    return null;
	} catch (Throwable t) {
// 	    System.err.println("jdk12PriorityAction: run throws " + t +
// 			       " : " + t.getMessage());
	    return null;
	}
    }

}

