/*
 * @(#)PullSourceStream2InputStream.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.util;

import java.io.*;
import javax.media.protocol.*;

/**
 * PullSourceStream2InputStream is an adaptor between a PullSourceStream
 * and an InputStream. In receives in its constructor a PullSourceStream 
 * and uses that stream in order to implement the InputStream methods
 */
public class PullSourceStream2InputStream extends InputStream {

    /**
     * The PullSourceStream to be used
     */
    PullSourceStream pss;

    /**
     * A byte array  of size 1, for the read() method
     */
    byte[] buffer = new byte[1];

    /**
     * Constructor
     */
    public PullSourceStream2InputStream(PullSourceStream pss) {

	this.pss = pss;
    }

    public int read() throws IOException {

	if (pss.endOfStream()) {
	    System.out.println("end of stream");
	    return -1;
	}

	pss.read(buffer, 0, 1);
	return buffer[0];    
    }
    
    public int read(byte b[]) throws IOException {

	return pss.read(b, 0, b.length);
    }

    public int read(byte b[], int off, int len) throws IOException {
    
	return pss.read(b, off, len);
    }

    public long skip(long n) throws IOException {
    
	byte[] buffer = new byte[(int)n];
	int read = read(buffer);

	return read;
    }

    public int available() throws IOException {
    
	// NOT IMPLEMENTED
	System.out.println("available was called");
    
	return 0;
    }

    public void close() throws IOException {
    
	// DO NOTHING (???)
    }

    public boolean markSupported() {
    
	return false;
    }

}
