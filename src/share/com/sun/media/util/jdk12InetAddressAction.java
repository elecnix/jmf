/*
 * @(#)jdk12InetAddressAction.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.security.PrivilegedAction;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.UnknownHostException;

public
class jdk12InetAddressAction implements java.security.PrivilegedAction {
    public static Constructor cons;
    private InetAddress addr;
    private String method;
    private String arg;

    static {
	try {
	    cons = jdk12InetAddressAction.class.getConstructor(new Class[] {
	     InetAddress.class, String.class, String.class});
	} catch (Throwable e) {
	}
     }

    public jdk12InetAddressAction(InetAddress addr, String method, String arg) {
	this.addr = addr;
	this.method = method;
	this.arg = arg;
    }


    public Object run() {
	try {
	    if (method.equals("getLocalHost"))
		return InetAddress.getLocalHost();
	    else if (method.equals("getAllByName"))
		return InetAddress.getAllByName(arg);
	    else if (method.equals("getByName"))
		return InetAddress.getByName(arg);
	    else if (method.equals("getHostName"))
		return addr.getHostName();
	    else
		return null;
	} catch (Throwable t) {
	    return null;
	}
    }


}
