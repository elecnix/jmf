
package com.sun.media.protocol.dsound;

import javax.media.format.AudioFormat;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;
import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;

public class DSoundST extends DSound implements Runnable {

    private static final int REQ_OPEN = 1;
    private static final int REQ_FLUSH = 2;
    private static final int REQ_START = 3;
    private static final int REQ_STOP = 4;
    private static final int REQ_CLOSE = 5;
    private static final int REQ_READ = 6;

    protected boolean failure = false;
    protected Thread requestThread = null;
    protected Integer requestLock = new Integer(0);
    protected int request = 0;
    protected int result;
    protected byte [] data;
    protected int offset;
    protected int len;

    
    public DSoundST(AudioFormat format, int bufferSize) {
	super(format, bufferSize);
    }

    private void waitForRequest(int req) {
	synchronized (requestLock) {
	    request = req;
	    requestLock.notifyAll();
	    while (request > 0) {
		try {
		    requestLock.wait(100);
		} catch (InterruptedException ie) {
		}
	    }
	}
    }

    public void open() {
	if (requestThread == null) {
	    requestThread = new Thread(this);
	    requestThread.start();
	}

	waitForRequest(REQ_OPEN);

	if (failure)
	    throw new Error("Error opening DSound for capture");
    }

    public void start() {
	waitForRequest(REQ_START);
    }

    public void stop() {
	waitForRequest(REQ_STOP);
    }

    public void flush() {
	waitForRequest(REQ_FLUSH);
    }

    public void close() {
	waitForRequest(REQ_CLOSE);
    }

    public int read(byte [] data, int offset, int len) {
	this.data = data;
	this.offset = offset;
	this.len = len;
	waitForRequest(REQ_READ);
	return result;
    }

    public void run() {
	boolean done = false;
	while (!done) {
	    synchronized (requestLock) {
		while (request == 0) {
		    try {
			requestLock.wait();
		    } catch (InterruptedException ie) {
		    }
		}
	    }
	    if (request > 0) {
		switch (request) {
		case REQ_OPEN:
		    try {
			super.open();
		    } catch (Error e) {
			failure = true;
		    }
		    break;
		case REQ_START:
		    super.start();
		    break;
		case REQ_STOP:
		    super.stop();
		    break;
		case REQ_READ:
		    result = super.read(data, offset, len);
		    break;
		case REQ_FLUSH:
		    super.flush();
		    break;
		case REQ_CLOSE:
		    super.close();
		    done = true;
		}
	    }
	    synchronized (requestLock) {
		request = 0;
		requestLock.notifyAll();
	    }
	}
    }
}
