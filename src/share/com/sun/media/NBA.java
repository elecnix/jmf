/*
 * @(#)NBA.java	1.6 03/04/25
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

public final class NBA {

    private long data;
    private int size;
    private Class type = null;
    private Object javaData = null;
    private int atype = 1;

    static {
	try {
	    JMFSecurityManager.loadLibrary( "jmutil");
	} catch (Throwable t) {
	}
    }

    public NBA(Class type, int size) {
	this.type = type;
	this.size = size;
	if (type == short[].class) {
	    atype = 2;
	    size *= 2;
	} else if (type == int[].class) {
	    atype = 4;
	    size *= 4;
	} else if (type == long[].class) {
	    atype = 8;
	    size *= 8;
	}
	
	//System.err.println("NBA.constructor allocating " + size);
	data = nAllocate(size);
	if (data == 0)
	    throw new OutOfMemoryError("Couldn't allocate native buffer");
    }

    protected synchronized final void finalize() {
	if (data != 0)
	    nDeallocate(data);
	data = 0;
    }

    public synchronized Object getData() {
	if (javaData == null) {
	    if (type == byte[].class)
		javaData = new byte[size];
	    else if (type == short[].class)
		javaData = new short[size];
	    else if (type == int[].class)
		javaData = new int[size];
	    else if (type == long[].class)
		javaData = new long[size];
	    else {
		System.err.println("NBA: Don't handle this data type");
		return null;
	    }
	}
	//Thread.dumpStack();
	//System.err.println("NBA.getData");
	nCopyToJava(data, javaData, size, atype);
	return javaData;
    }

    public synchronized Object clone() {
	NBA cl = new NBA(type, size);
	nCopyToNative(data, cl.data, size);
	return cl;
    }

    public synchronized void copyTo(NBA nba) {
	if (nba.size >= size) {
	    nCopyToNative(data, nba.data, size);
	}
    }

    public synchronized void copyTo(byte [] javadata) {
	if (javadata.length >= size) {
	    nCopyToJava(data, javadata, size, atype);
	}
    }

    public synchronized long getNativeData() {
	return data;
    }

    public int getSize() {
	return size;
    }

    private native long nAllocate(int size);

    private native void nDeallocate(long data);

    private native void nCopyToNative(long indata, long outdata, int size);

    private native void nCopyToJava(long indata, Object outdata, int size, int atype);
}
