/*
 * @(#)CreateSourceThreadAction.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

/**
 * The reason this class is in this package and not util is because
 * it is used to create SourceThread in BasicSourceModule
 * which is a package private classes.
 * These cannot be instantiated from the util package
 */

public
class CreateSourceThreadAction implements java.security.PrivilegedAction {
	
    private Class sourceThreadClass;
    private BasicSourceModule bsm;
    private Object myoc;
    private int i;
    static Constructor cons;

     static {
 	try {
 	    cons = CreateSourceThreadAction.class.getConstructor(new Class[] {
 		Class.class, BasicSourceModule.class, Object.class, int.class});
 	} catch (Throwable e) {
 	}
     }

    public CreateSourceThreadAction(Class sourceThreadClass, BasicSourceModule bsm,
				    Object myoc, int i) {
	
	try {
	    this.sourceThreadClass = sourceThreadClass;
	    this.bsm = bsm;
	    this.myoc = myoc;
	    this.i = i;
	} catch (Throwable e) {
	}
    }
    
    public Object run() {
	try {
	    Constructor cons = sourceThreadClass.getConstructor(new Class[]
				{BasicSourceModule.class,
                                 myoc.getClass(),
                                 int.class});

	    Object object = cons.newInstance(new Object[] {
		bsm, myoc, new Integer(i)});
	    return object;
	} catch (Throwable e) {
	    return null;
	}
    }
}
