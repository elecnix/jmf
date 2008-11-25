/*
 * @(#)CreateWorkThreadAction.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

/**
 * The reason this class is in this package and not util is because
 * it is used to create threads like SendEventQueue, RealizeWorkThread
 * in BasicController, StatsThread in BasicPlayer which are 
 * package private classes.
 * These cannot be instantiated from the util package
 */

public
class CreateWorkThreadAction implements java.security.PrivilegedAction {
	
    private Class objclass;
    Class baseClass;
    Object arg;
    static Constructor cons;

     static {
 	try {
 	    cons = CreateWorkThreadAction.class.getConstructor(new Class[] {
 		Class.class, Class.class, Object.class});
 	} catch (Throwable e) {
 	}
     }
    
    public CreateWorkThreadAction(Class objclass, Class baseClass, Object arg) {
	try {
	    this.objclass = objclass;
	    this.baseClass = baseClass;
	    this.arg = arg;
	} catch (Throwable e) {
	}
	
    }
    
    public Object run() {
	try {
	    Constructor cons = objclass.getConstructor(new Class[] {baseClass});
	    Object object = cons.newInstance(new Object[] {arg});
	    return object;
	} catch (Throwable e) {
	    return null;
	}
    }
}
