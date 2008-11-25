/*
 * @(#)jdk12Util.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

public class jdk12Util {
     public static Constructor getCheckPermissionAction() {

	 try {
	     return CheckPermissionAction.class.getConstructor(new Class[] {
                 Permission.class});
	     
	 } catch (Throwable e) {
	     System.out.println("getCheckThreadPermissionAction: " + e);
	 }
     }
}


class CheckPermissionAction implements java.security.PrivilegedAction {

    Permission permission;
    public CheckPermissionAction(Permission p) {
	permission = p;
    }

    public Object run() {
	AccessController.checkPermission(permission);
	return null; // nothing to return
    }

}

