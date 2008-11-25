/*
 * @(#)DefaultSecurity.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.util.Hashtable;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class DefaultSecurity implements JMFSecurity {

    public static JMFSecurity security;
    private static ClassLoader clsLoader=null;
    private static Class cls=null;
    private static Method dummyMethodRef=null;

    static {

    	security = new DefaultSecurity();
        try {
	    cls = security.getClass();
	    clsLoader = cls.getClassLoader();
	    dummyMethodRef = cls.getMethod("dummyMethod",new Class[0]);
        } catch (Exception e) {
	    System.out.println(e);
	}

    }

    public static void dummyMethod() {

    }

    private DefaultSecurity() {

    }

    public String getName() {
	return "default";
    }

    public void requestPermission(Method[] m, Class[] c, Object[][] args,
				  int request) throws SecurityException {

	//	if (clsLoader == null) {
	    m[0] = dummyMethodRef;
	    c[0] = cls;
	    args[0] = null;

	    // Dont throw SecurityException if clsLoader is not null
	    // as we may be able to get some permissions.
// 	}
// 	else {
// 	    throw new SecurityException("DefaultSecurity : Cannot request permission");
// 	}
    }

    // parameter not used
    public void requestPermission(Method[] m, Class[] c, Object[][] args, int request,
				  String parameter) throws SecurityException {
	requestPermission(m, c, args, request);
    }


    public boolean isLinkPermissionEnabled() {
         if (clsLoader == null)  {
           return true;
          }
         else {
           return false;
         }

    }

   public void permissionFailureNotification(int permission) {
    }

    public void loadLibrary(String name) throws UnsatisfiedLinkError {
	if (clsLoader == null) {
	    System.loadLibrary(name);
	}
	else {
	    throw new UnsatisfiedLinkError("Unable to get link privilege to " + name);
	}
    }

}


