/*
 * @(#)DevicePushSourceStream.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.protocol.device;

import javax.media.protocol.*;
import javax.media.Format;
import javax.media.format.*;

// public SHOULD BE REMOVED
public class DevicePushSourceStream implements PushSourceStream, Runnable {
  
    /**
     * The Handler to service data transfers to this stream
     */
    private SourceTransferHandler handler;

    /**
     * The thread which responsible on generating the transferData call
     */
    Thread triggerThread;
  
    /**
     * Indicates the stream was started
     */
    boolean started = false;
  
    static {
	com.sun.media.JMFSecurityManager.loadLibrary("jmdevice");
    }
  
    /**
     * Obtain the format that this object is set to.
     *
     * @return the current format.
     */
    public Format getFormat() {
    
	// TEMPORARY IMPLEMENTATION
	return new AudioFormat(AudioFormat.LINEAR, 22050, 16, 1);
    }
   
    /**
     * Obtain the collection of objects that
     * control the object that implements this interface.
     * <p>
     *
     * If no controls are supported, a zero length
     * array is returned.
     *
     * @return the collection of object controls
     */
    public Object[] getControls() {
    
	// no controls implemented
	return new Object[0];
    }
  
    /**
     * Obtain the object that implements the specified
     * <code>Class</code> or <code>Interface</code>
     * The full class or interface name must be used.
     * <p>
     * 
     * If the control is not supported then <code>null</code>
     * is returned.
     *
     * @return the object that implements the control,
     * or <code>null</code>.
     */
    public Object getControl(String controlType) {
	// no control implemented
	return null;
    }
  
    /**
     * Get the current content type for this stream.
     *
     * @return The current <CODE>ContentDescriptor</CODE> for this stream.
     */
    public ContentDescriptor getContentDescriptor() {

	// temporary implemantation
	return new ContentDescriptor("raw");
    }
  
    /**
     * Get the size, in bytes, of the content on this stream.
     * LENGTH_UNKNOWN is returned if the length is not known.
     *
     * @return The content length in bytes.
     */
    public long getContentLength() {
	return LENGTH_UNKNOWN;
    }
  
    /**
     * Find out if the end of the stream has been reached.
     *
     * @return Returns <CODE>true</CODE> if there is no more data.
     */
    public boolean endOfStream() {
	// currently we just return false but it should be changed 
	return false;
    }

    /**
     * Read from the stream without blocking.
     * Returns -1 when the end of the media
     * is reached.
     *
     * @param buffer The buffer to read bytes into.
     * @param offset The offset into the buffer at which to begin writing data.
     * @param length The number of bytes to read.
     * @return The number of bytes read or -1
     * when the end of stream is reached.
     */
    public synchronized native int read(byte[] buffer, int offset, int length);
  
    /**
     * Determine the size of the buffer needed for the data transfer.
     * This method is provided so that a transfer handler
     * can determine how much data, at a minimum, will be
     * available to transfer from the source.
     * Overflow and data loss is likely to occur if this much
     * data isn't read at transfer time.
     *
     * @return The size of the data transfer.
     */
    public int getMinimumTransferSize() {
	/* NOT IMPLEMENTED YET */
	return 128;
    }
  
    /**
     * Register an object to service data transfers to this stream.
     * <p>
     * If a handler is already registered when
     * <CODE>setTransferHandler</CODE> is called,
     * the handler is replaced;
     * there can only be one handler at a time.
     * 
     * @param transferHandler The handler to transfer data to.
     */
    public void setTransferHandler(SourceTransferHandler transferHandler) {
	handler = transferHandler;
    }

    /**
     * Runnable's method implementation
     */
    public void run() {
	while (started) {
	    handler.transferData(this);
	    while (!isBufferFilled()) {
		try {
		    synchronized(this) {
			wait();
		    }
		}
		catch (InterruptedException e) {
		    System.out.println("Exception: " + e);
		}
	    }
	}
	started = true; // this is a temporary solution for signaling the thead exit
    }

    /**
     * Starts a thread to initiate trasferData called on the Handler. 
     * The thread will wait untill the buffer sent to the device is 
     * full and than will call trasferData again
     */
    void start() {
	triggerThread = new Thread(this);
	started = true;
	triggerThread.start();
    }	

    /**
     * Stops the thread
     */
    void stop() {
	started = false;
	while (!started); // TEMPORARY SOLUTION, TO BE REPLACED
    }
   
    /**
     * Checks a the bufferFilled flag in the native code
     */
    private native boolean isBufferFilled();
  
}
