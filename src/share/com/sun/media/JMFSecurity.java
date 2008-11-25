/*
 * @(#)JMFSecurity.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.lang.reflect.Method;

// TODO: remove LINK permission but keep isLinkPermissionEnabled()
public interface JMFSecurity {
    int READ_PROPERTY = (1 << 0);
    int READ_FILE = (1 << 1);
    int WRITE_FILE = (1 << 2);
    int DELETE_FILE = (1 << 3);
    int THREAD = (1 << 4);
    int THREAD_GROUP = (1 << 5);
    int LINK = (1 << 6);
    int CONNECT = (1 << 7);
    int TOP_LEVEL_WINDOW = (1 << 8);
    int MULTICAST = (1 << 9);
    
    String getName();

    void requestPermission(Method[] m, Class[] c, Object[][] args, int request) throws SecurityException;

    void requestPermission(Method[] m, Class[] c, Object[][] args, int request, String parameter) throws SecurityException;

    boolean isLinkPermissionEnabled();
    void    permissionFailureNotification(int permission);

    // The implementor of this method should catch all Exceptions and
    // throw UnsatisfiedLinkError
    void loadLibrary(String name) throws UnsatisfiedLinkError;

}
