/*
 * @(#)jdk12.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.util;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;




// This class will compile under 1.1 also as reflection alone is used
public class jdk12 {

    // Should be able to use final for a one time initialization

    public static Class ac;
    public static Class accontextC;
    public static Class permissionC;
    public static Class privActionC;
    public static Method checkPermissionM;
    public static Method doPrivM;
    public static Method doPrivContextM;
    public static Method getContextM;


    static {
	try {
	    ac = Class.forName("java.security.AccessController");
	    accontextC = Class.forName("java.security.AccessControlContext");
	    permissionC = Class.forName("java.security.Permission");
	    privActionC = Class.forName("java.security.PrivilegedAction");
	    // System.out.println("ac is " + ac);
	    checkPermissionM = ac.getMethod("checkPermission",
					  new Class[] {
		    permissionC
		});

		doPrivM = ac.getMethod("doPrivileged",
				       new Class[] {
		    privActionC
		});

		getContextM = ac.getMethod("getContext", null);
		// System.out.println("getContextM is " + getContextM);


		doPrivContextM = ac.getMethod("doPrivileged",
				       new Class[] {
		    privActionC,
                    accontextC
		});
		// System.out.println("doPrivContextM is " + doPrivContextM);
	} catch (Throwable t) {
	    // This shouldn't happen on jdk1.2
	    // System.err.println("Ok if thrown on non-jdk1.2 VM: " + t);
	}
    }


}


