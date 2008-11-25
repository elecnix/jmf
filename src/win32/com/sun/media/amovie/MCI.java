/*
 * @(#)MCI.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.amovie;


public class MCI {
    
    private String errString;
    private String retString;
    private int hwndCallback = 0;

    public native int getDeviceId(String name);

    public native String getErrorString(int errId);

    public native boolean sendString(String command);

    static {
	com.sun.media.JMFSecurityManager.loadLibrary("jmmci");
    }

    public void ssendString(String command) {
	boolean ret = sendString(command);
	//System.out.println(errString);
	//System.out.println(retString);
	if (!ret)
	    throw new Error(errString);
    }

    public String getErrorMessage() {
	return errString;
    }

    public String getReturnString() {
	return retString;
    }
}
