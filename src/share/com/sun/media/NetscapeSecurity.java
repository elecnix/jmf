/*
 * @(#)NetscapeSecurity.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.util.Hashtable;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class NetscapeSecurity implements JMFSecurity {

    public static JMFSecurity security;

    private static Method enablePrivilege;
    private static Class privilegeManager;
    //private static ClassLoader cl =null;
    private static boolean linkEnabled = true;

    private static Object [] readPropArgs = new Object[] {"UniversalPropertyRead"};
    private static Object [] readFileArgs = new Object[] {"UniversalFileRead"};
    private static Object [] writeFileArgs = new Object[] {"UniversalFileWrite"};
    private static Object [] deleteFileArgs = new Object[] {"UniversalFileDelete"};
    private static Object [] threadArgs = new Object[] {"UniversalThreadAccess"};
    private static Object [] threadGroupArgs = new Object[] {"UniversalThreadGroupAccess"};
    private static Object [] linkArgs = new Object[] {"UniversalLinkAccess"};
    private static Object [] connectArgs = new Object[] {"UniversalConnectWithRedirect"};
    public static Object [] windowArgs = new Object[] {"UniversalTopLevelWindow"};
    public static Object [] multicastArgs = new Object[] {"UniversalMulticast"};

    private static Hashtable table = new Hashtable();


    static {
	security = new NetscapeSecurity();

	try {
	    privilegeManager =
		Class.forName("netscape.security.PrivilegeManager");

	    enablePrivilege = privilegeManager.getMethod("enablePrivilege",
						 new Class[] {String.class});

            //cl =(Class.forName("javax.media.JMFSecurity")).getClassLoader();

	} catch (ClassNotFoundException  e) {
	    System.err.println("NetscapeSecurity: Cannot find class netscape.security.PrivilegeManager");
	} catch (Exception e) {
	    System.out.println(e);
	}

	table.put(new Integer(JMFSecurity.READ_PROPERTY), readPropArgs);
	table.put(new Integer(JMFSecurity.READ_FILE), readFileArgs);
	table.put(new Integer(JMFSecurity.WRITE_FILE), writeFileArgs);
	table.put(new Integer(JMFSecurity.DELETE_FILE), deleteFileArgs);
	table.put(new Integer(JMFSecurity.THREAD), threadArgs);
	table.put(new Integer(JMFSecurity.THREAD_GROUP), threadGroupArgs);
	table.put(new Integer(JMFSecurity.LINK), linkArgs);
	table.put(new Integer(JMFSecurity.CONNECT), connectArgs);
	table.put(new Integer(JMFSecurity.TOP_LEVEL_WINDOW), windowArgs);
	table.put(new Integer(JMFSecurity.MULTICAST), multicastArgs);
    }

    private Method methodArray[] = new Method[1];
    private Class classArray[] = new Class[1];
    private Object arguments[][] = new Object[1][0];


    private NetscapeSecurity() {
    }

    // Disabled security and Netscape security can be invoked using
    // reflection. The jmf-security- prefix can be used to
    // treat them as a group. You don't have to check for netscape
    // or disabled.
    public String getName() {
	return "jmf-security-netscape";
    }

    public void requestPermission(Method[] m, Class[] c, Object[][] args,
				  int request) throws SecurityException {

	//if ( (enablePrivilege == null) || (cl != null) ) {
        if (enablePrivilege == null) {
	    throw new SecurityException("Cannot request permission");
	}

	m[0] = enablePrivilege;
	c[0] = privilegeManager;


	Object value = table.get(new Integer(request));
	if (value == null) {
	    throw new SecurityException("Permission previously denied by user " + request);
	}

	args[0] = (Object[]) value;
    }

    // Netscape will not use this parameter.
    public void requestPermission(Method[] m, Class[] c, Object[][] args, int request,
				  String parameter) throws SecurityException {
	requestPermission(m, c, args, request);
    }


    public boolean isLinkPermissionEnabled()  {
	return linkEnabled;
    }

    public void permissionFailureNotification(int permission) {
         table.remove(new Integer(permission));
    }

    public void loadLibrary(String name) throws UnsatisfiedLinkError {
	try {
	    if (linkEnabled) {
		requestPermission(methodArray, classArray, arguments, JMFSecurity.LINK);
	    } else {
		throw new UnsatisfiedLinkError("No LINK privilege");
	    }
	    methodArray[0].invoke(classArray[0], arguments[0]);
	    System.loadLibrary(name);
	} catch (Exception e) {
	    linkEnabled = false;
	    // LINK permission should be removed from the interface as it is not useful
	    permissionFailureNotification(JMFSecurity.LINK);
	    throw new UnsatisfiedLinkError("Unable to get " + name +
				  " privilege  " + e);
	}
    }
}


