/*
 * @(#)DSSourceStream.java	1.10 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.protocol.ds;

import java.io.*;
import java.awt.*;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.format.*;
import javax.media.format.*;

import com.sun.media.vfw.*;


public class DSSourceStream implements PushBufferStream, Runnable {

    private BufferTransferHandler transferHandler;

    private Thread triggerThread;

    private boolean started = false;

    private VideoFormat format = null; 
  
    /** 
     * Get the current content type for this stream.
     *
     * @return The current <CODE>ContentDescriptor</CODE> for this stream.
     */
    public ContentDescriptor getContentDescriptor() {
	return new ContentDescriptor(ContentDescriptor.RAW);
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
	return false;
    }
  
    /**
     * Get the format tupe of the data that this source stream provides.
     * @return A <CODE>Format</CODE> object that describes the data in this stream.
     */
    public Format getFormat() {
    
	if (format == null) {
	  BitMapInfo bmi = new BitMapInfo();
	  getVideoFormat(bmi);
	  float frameRate = getFrameRate();
	  Format directShowFormat = bmi.createVideoFormat(Format.byteArray,
							  frameRate);
	    if (directShowFormat instanceof VideoFormat)
		format = (VideoFormat)directShowFormat;
	    else
		return new Format("DirectShowUnknown");
	}
    
	return format;
    }
  
    /**
     * Fill the BitMapInfo object
     */
    private native void getVideoFormat(BitMapInfo bmi);

  /**
   * Get the DirectShow reported frame rate
   */
  private native float getFrameRate();
  
    /**
     * Read from the stream without blocking.
     * @throws IOException Thrown if an error occurs while reading
     *
     */
    public void read(Buffer buffer) throws IOException {
    
	//    System.out.println("read");
    
	Object data = buffer.getData();
	int length = format.getMaxDataLength();
    
	if (data == null || !(data instanceof byte[]) || ((byte[])data).length != length)
	    data = new byte[length];
    
	buffer.setFormat(format);    
	setBuffer((byte[])data, buffer.getOffset());
	try {
	    while (!isFilled())
		Thread.sleep(50);
	}
	catch (InterruptedException e) {
	    System.out.println("Exception: " + e);
	}
    
	buffer.setData(data);
	buffer.setOffset(0);
	buffer.setLength(length);
	buffer.setTimeStamp(Buffer.TIME_UNKNOWN);
    }
  
    /**
     * Sets a buffer
     */
    private native void setBuffer(byte[] data, int offset);
  
    /**
     * Checks if buffer was filled with data
     */
    private native boolean isFilled();
  
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
    public void setTransferHandler(BufferTransferHandler transferHandler) {
	this.transferHandler = transferHandler;
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
	return null;
    }

    /**
     * Start a thread to generate the transferData calls
     */
    void start() {
    
	if (started) 
	    return;

	triggerThread = new Thread(this);
	started = true;
	triggerThread.start();
    }

    /** 
     * Stops the triggerThread
     */
    void stop() {
    
	if (!started)
	    return;

	synchronized(this) {
	    started = false;
	    try {
		wait(); // wait till the triggerThread ended
	    }
	    catch (InterruptedException e) {
		System.out.println("Exception: " + e);
	    }
	}
    }
    

    /**
     * Runnable implementation
     */
    public void run() {

	while (started) {
	    transferHandler.transferData(this);
      
	    /* WE ASSUME transferData IS A BLOCKING CALL - THIS MAY NOT BE THE 
	       CASE FOR DIFFERENT IMPLEMENTATIONS OF Processors */
	}
	synchronized(this) {
	    notify(); // notify the stop method that the triggerThread has finished
	}
    }
  
}
