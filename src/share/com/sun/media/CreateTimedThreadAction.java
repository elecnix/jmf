/*
 * @(#)CreateTimedThreadAction.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

/**
 * The reason this class is in this package and not util is because
 * it is used to create threads like StopTimeThread, TimedStartThread
 * in BasicController which are package private classes.
 * These cannot be instantiated from the util package
 */

public
class CreateTimedThreadAction implements java.security.PrivilegedAction {
	
    private Class objclass;
    private Class baseClass;
    private Object arg1;
    private long nanoseconds;
    static Constructor cons;

     static {
 	try {
 	    cons = CreateTimedThreadAction.class.getConstructor(new Class[] {
 		Class.class, Class.class, Object.class, long.class});
 	} catch (Throwable e) {
 	}
     }

    public CreateTimedThreadAction(Class objclass, Class baseClass,
				   Object arg1, long nanoseconds) {
	
	try {
	    this.objclass = objclass;
	    this.baseClass = baseClass;
	    this.arg1 = arg1;
	    this.nanoseconds = nanoseconds;
	} catch (Throwable e) {
	}
	
    }
    
    public Object run() {
	try {
	    Constructor cons = objclass.getConstructor(new Class[] {baseClass, long.class});
	    Object object = cons.newInstance(new Object[] {arg1,
					       new Long(nanoseconds)});
	    return object;
	} catch (Throwable e) {
	    return null;
	}
    }
}
