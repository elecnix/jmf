/*
 * @(#)IESecurity.java	1.12 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.util.Hashtable;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;


/**
 *
 * public final synchronized class com/ms/security/PermissionID extends java.lang.Object 
 * public static final com/ms/security/PermissionID SYSTEM;
 * public static final com/ms/security/PermissionID FILEIO;
 * public static final com/ms/security/PermissionID NETIO;
 * public static final com/ms/security/PermissionID THREAD;
 * public static final com/ms/security/PermissionID PROPERTY;
 * public static final com/ms/security/PermissionID EXEC;
 * public static final com/ms/security/PermissionID REFLECTION;
 * public static final com/ms/security/PermissionID PRINTING;
 * public static final com/ms/security/PermissionID SECURITY;
 * public static final com/ms/security/PermissionID REGISTRY;
 * public static final com/ms/security/PermissionID CLIENTSTORE;
 * public static final com/ms/security/PermissionID UI;
 * public static final com/ms/security/PermissionID SYSSTREAMS;
 * public static final com/ms/security/PermissionID USERFILEIO;
 * public static final com/ms/security/PermissionID MULTIMEDIA;
 */

public class IESecurity implements JMFSecurity {

    public static JMFSecurity security;
    public static boolean jview=false;

    private static Class cls=null;
    private static Method dummyMethodRef=null;
    public static final boolean DEBUG = false;


    static {
	security = new IESecurity();
        cls = security.getClass();
        try {
   	  dummyMethodRef = cls.getMethod("dummyMethod",new Class[0]);
        } catch (Exception e) {

        }
    }

    private IESecurity() {
    }

    public String getName() {
	return "internetexplorer";
    }

     public static void dummyMethod() {

    }


    public void requestPermission(Method[] m, Class[] c, Object[][] args,
				  int request) throws SecurityException {

//         if (!jview)
// 	    throw new SecurityException("IESecurity : Cannot request permission");

        
        m[0] = dummyMethodRef;
	c[0] = cls;
	args[0] = null;
        

    }

    // Netscape will not use this parameter.
    public void requestPermission(Method[] m, Class[] c, Object[][] args, int request,
				  String parameter) throws SecurityException {
	requestPermission(m, c, args, request);
    }


    public boolean isLinkPermissionEnabled() {
       return jview;
       //return true;
    }

    public void permissionFailureNotification(int permission) {
    }

    public void loadLibrary(String name) throws UnsatisfiedLinkError {
// 	if (jview)
// 	    System.loadLibrary(name);
// 	else
// 	    throw new UnsatisfiedLinkError("Unable to get link privilege to " + name);

	try {
	    try {
		if (!jview) {
		    PolicyEngine.assertPermission(PermissionID.SYSTEM);
		}
	    } catch (Throwable t) {
	    }
	    // System.out.println("Call System.loadLibrary " + name);
	    System.loadLibrary(name);
	} catch (Exception e) {
	    if (DEBUG)
		System.err.println("IESecurity: Unable to load library " + name);
	    throw new UnsatisfiedLinkError("Unable to get link privilege to " + name);
	} catch (Error e) {
	    if (DEBUG)
		System.err.println("IESecurity: Unable to load library " + name);
	    throw new UnsatisfiedLinkError("Unable to get link privilege to " + name);
	}
    }

}


