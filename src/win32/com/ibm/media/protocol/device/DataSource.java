/*
 * @(#)DataSource.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.protocol.device;

import java.io.*;

/**
 * This is a DataSource that reprents an audio capture device
 */
public class DataSource extends javax.media.protocol.PushDataSource {
  
    /**
     * The actual stream 
     */
    DevicePushSourceStream stream;

    /**
     * The streams array 
     */
    DevicePushSourceStream[] streams;

    static {
	com.sun.media.JMFSecurityManager.loadLibrary("jmdevice");
    }
  
    /**
     * A no-argument constructor required by pre 1.1 implementations
     * so that this class can be instantiated by
     * calling <CODE>Class.newInstance</CODE>.
     *
     */
    public DataSource() {
	super();
    }
  
    /**
     * Get a string that describes the content-type of the media
     * that the source is providing.
     * <p>
     * It is an error to call <CODE>getContentType</CODE> if the source is
     * not connected.
     *
     * @return The name that describes the media content.
     */
    public String getContentType() {
	// tempotary implementation
	return "raw";
    }
  
    /**
     * Open a connection to the source described by
     * the <CODE>MediaLocator</CODE>.
     * <p>
     *
     * The <CODE>connect</CODE> method initiates communication with the source.
     *
     * @exception IOException Thrown if there are IO problems
     * when <CODE>connect</CODE> is called.
     */
    public void connect() throws IOException {
	System.out.println("connecting DataSource");
	// currently we don't use the mediaLocator... we use default device
	stream = new DevicePushSourceStream();
	streams = new DevicePushSourceStream[1];
	streams[0] = stream;

	connectDevice();
    }

    /**
     * Connect to the actual device
     */
    native void connectDevice();
  
  
    /**
     * Close the connection to the source described by the locator.
     * <p>
     * The <CODE>disconnect</CODE> method frees resources used to maintain a
     * connection to the source.
     * If no resources are in use, <CODE>disconnect</CODE> is ignored.
     * If <CODE>stop</CODE> hasn't already been called,
     * calling <CODE>disconnect</CODE> implies a stop.
     *
     */
    public void disconnect() {
	disconnectDevice();
    }
  
    /**
     * Disconnect from the actual device
     */
    native void disconnectDevice();
  
  
    /**
     * Initiate data-transfer. The <CODE>start</CODE> method must be
     * called before data is available.
     *(You must call <CODE>connect</CODE> before calling <CODE>start</CODE>.)
     *
     * @exception IOException Thrown if there are IO problems with the source
     * when <CODE>start</CODE> is called.
     */
    public void start() throws IOException {
	/* instantiate a buffer and start the actual device, the read() method
	   calls start() on the device */
	stream.start();
    }

    /**
     * Stop the data-transfer.
     * If the source has not been connected and started,
     * <CODE>stop</CODE> does nothing.
     */
    public void stop() throws IOException {

	// stop the stream's thread and the device itself 
	stream.stop();
	stopDevice();
    }

    /**
     * Stops the actual device
     */
    native void stopDevice();

    /**
     * Get the collection of streams that this source
     * manages. The collection of streams is entirely
     * content dependent. The <code>ContentDescriptor</code>
     * of this <CODE>DataSource</CODE> provides the only indication of
     * what streams can be available on this connection.
     *
     * @return The collection of streams for this source.
     */
    public javax.media.protocol.PushSourceStream[] getStreams() {
	// return the steams the actual device provides
	return streams;
    }
  
    /**
     * Get the duration of the media represented
     * by this object.
     * The value returned is the media's duration
     * when played at the default rate.
     * If the duration can't be determined  (for example, the media object is presenting live
     * video)  <CODE>getDuration</CODE> returns <CODE>DURATION_UNKNOWN</CODE>.
     *
     * @return A <CODE>Time</CODE> object representing the duration or DURATION_UNKNOWN.
     */
    public javax.media.Time getDuration() {
	return DURATION_UNKNOWN;
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
  
}




