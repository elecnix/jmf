/*
 * @(#)jdk12MulticastSocketJoinGroupAction.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.security.PrivilegedAction;
import java.lang.reflect.Constructor;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.io.IOException;


public
class jdk12MulticastSocketJoinGroupAction implements java.security.PrivilegedAction {

    public static Constructor cons;
    private MulticastSocket s;
    private InetAddress a;

    static {
	try {
	    cons = jdk12MulticastSocketJoinGroupAction.class.getConstructor(new Class[] {
	     MulticastSocket.class,
	     InetAddress.class});
	} catch (Throwable e) {
	}
     }


    public jdk12MulticastSocketJoinGroupAction(MulticastSocket s, InetAddress a) {
	this.s = s;
	this.a = a;
    }

    public Object run() {
	try {
	    s.joinGroup(a);
	    return s; // No Error
	} catch (Throwable t) {
	    return null; // Error
	}
    }
}
