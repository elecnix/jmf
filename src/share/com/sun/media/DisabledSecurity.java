/*
 * @(#)DisabledSecurity.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.lang.reflect.Method;

public class DisabledSecurity implements JMFSecurity {

    public static JMFSecurity security;

    static {
    	security = new DisabledSecurity();
    }


    private DisabledSecurity() {

    }

    // Disabled security and Netscape security can be invoked using
    // reflection. The jmf-security- prefix can be used to
    // treat them as a group. You don't have to check for netscape
    // or disabled.
    public String getName() {
	return "jmf-security-disabled";
    }

    public void requestPermission(Method[] m, Class[] c, Object[][] args,
				  int request) throws SecurityException {
      
         throw new SecurityException("DisabledSecurity : Cannot request permission");

    }

    // parameter not used
    public void requestPermission(Method[] m, Class[] c, Object[][] args, int request,
				  String parameter) throws SecurityException {
      requestPermission(m, c, args, request);
    }


    public boolean isLinkPermissionEnabled() {
           return false;
    }

   public void permissionFailureNotification(int permission) {
    }

    public void loadLibrary(String name) throws UnsatisfiedLinkError {
        throw new UnsatisfiedLinkError("Unable to get link privilege to " + name);
    }

}
