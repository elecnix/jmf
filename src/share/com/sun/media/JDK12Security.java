/*
 * @(#)JDK12Security.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.util.Hashtable;
import java.lang.reflect.*;
import java.lang.reflect.InvocationTargetException;

import java.security.*;


public class JDK12Security implements JMFSecurity {

    public static final JMFSecurity security;
    private static Class cls=null;
    private static Method dummyMethodRef=null;

    private static Permission threadPermission = null;
    private static Permission threadGroupPermission = null;
    private static Permission connectPermission = null;
    private static Permission multicastPermission = null;
    private static Permission readAllFilesPermission = null;

    private static Constructor filepermcons;

    static {

    	security = new JDK12Security();
        try {
	    cls = security.getClass();
	    dummyMethodRef = cls.getMethod("dummyMethod",new Class[0]);


	    Class rtperm = Class.forName("java.lang.RuntimePermission");
	    Class socketperm = Class.forName("java.net.SocketPermission");
	    Class fileperm = Class.forName("java.io.FilePermission");
	    filepermcons = fileperm.getConstructor(new Class[] {String.class, String.class});


	    Constructor cons = rtperm.getConstructor(new Class[] {String.class});
	    threadPermission = (Permission) cons.newInstance(new Object[] {"modifyThread"});
	    // System.out.println("threadPermission is " + threadPermission);

	    threadGroupPermission = (Permission) cons.newInstance(new Object[] {"modifyThreadGroup"});
	    // System.out.println("threadGroupPermission is " + threadGroupPermission);


	    cons = socketperm.getConstructor(new
	     Class[] {
		String.class, String.class
		     });
	    connectPermission = (Permission) cons.newInstance(
                     new Object[] {"*", "connect"});
	    // System.out.println("connectPermission is " + connectPermission);

	    multicastPermission = (Permission) cons.newInstance(
                     new Object[] {"*", "accept,connect"});
	    // System.out.println("multicastPermission is " + multicastPermission);

	    // I don't think there is a way of saying
	    // give permission to read a file called jmf.properties
	    // no matter what the path. Check

// 	    readAllFilesPermission = (Permission) cons.newInstance(
// 		   new Object[] {"ALL FILES", "read"});
// 	    System.out.println("readAllFilesPermission is " + readAllFilesPermission);


        } catch (Exception e) {
	}

    }

    public static Permission getReadFilePermission(String name) {
	try {
 	    return  (Permission) filepermcons.newInstance(
				     new Object[] {name, "read"});
	} catch (Exception e) {
	    return null;
	}
    }

    public static Permission getWriteFilePermission(String name) {
	try {
 	    return  (Permission) filepermcons.newInstance(
				     new Object[] {name, "read, write"});
	} catch (Exception e) {
	    return null;
	}
    }


    public static void dummyMethod() {

    }

    private JDK12Security() {

    }

    public String getName() {
	return "jdk12";
    }

    public static Permission getThreadPermission() {
	return threadPermission;
    }


    public static Permission getThreadGroupPermission() {
	return threadGroupPermission;
    }

    public static Permission getConnectPermission() {
	return connectPermission;
    }

    public static Permission getMulticastPermission() {
	return multicastPermission;
    }

    public static Permission getReadAllFilesPermission() {
	return readAllFilesPermission;
    }

    public void requestPermission(Method[] m, Class[] c, Object[][] args,
				  int request) throws SecurityException {


	m[0] = dummyMethodRef;
	c[0] = cls;
	args[0] = null;

	//throw new SecurityException("DefulatSecurity : Cannot request permission");
    }

    // parameter not used
    public void requestPermission(Method[] m, Class[] c, Object[][] args, int request,
				  String parameter) throws SecurityException {
	requestPermission(m, c, args, request);
    }


     public boolean isLinkPermissionEnabled() {
       return true;
    }

    public void permissionFailureNotification(int permission) {
    }

    // Note: this method may be called by user code and will load
    // the library using the permissions of this class.
    // We need to revisit this case and maybe remove this method.
    public void loadLibrary(final String name) throws UnsatisfiedLinkError {

	AccessController.doPrivileged(
	    new java.security.PrivilegedAction() {
	      public Object run()  {
		  System.loadLibrary(name);
		  return null;
	      }
	}
      );
   }

}


