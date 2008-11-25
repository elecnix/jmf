/*
 * @(#)ExtBuffer.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import javax.media.Buffer;

public class ExtBuffer extends javax.media.Buffer {

    protected NBA nativeData = null;

    protected boolean nativePreferred = false;


    public void setNativeData(NBA nativeData) {
	this.nativeData = nativeData;
    }

    public NBA getNativeData() {
	return nativeData;
    }

    public boolean isNativePreferred() {
	return nativePreferred;
    }

    public void setNativePreferred(boolean prefer) {
	nativePreferred = prefer;
    }

    public Object getData() {
	if (nativeData != null)
	    return nativeData.getData();
	else
	    return data;
    }

    public void setData(Object data) {
	nativeData = null;
	this.data = data;
    }

    /**
     * Copy the attributes from the specified <CODE>Buffer</CODE> into this
     * <CODE>Buffer</CODE>
     * @param buffer The input <CODE>Buffer</code> the copy the attributes from.
     */
    public void copy(Buffer buffer, boolean swap) {
	super.copy(buffer, swap);
	if (buffer instanceof ExtBuffer) {
	    ExtBuffer fromBuf = (ExtBuffer) buffer;
	    if (swap) {
		NBA temp = fromBuf.nativeData;
		fromBuf.nativeData = nativeData;
		nativeData = temp;
		boolean prefer = fromBuf.nativePreferred;
		fromBuf.nativePreferred = nativePreferred;
		nativePreferred = prefer;
	    } else {
		nativeData = fromBuf.nativeData;
		nativePreferred = fromBuf.nativePreferred;
	    }

	}
    }
}

