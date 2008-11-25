/*
 * @(#)DataSource.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.protocol.ds;

import java.io.*;

import javax.media.*;
import javax.media.protocol.*;
import com.sun.media.JMFSecurityManager;

/**
 * This class is a DataSource for video capture with DirectShow
 */
public class DataSource extends PushBufferDataSource {
  
    DSSourceStream[] streams = new DSSourceStream[1];
    DSSourceStream stream = null;
    boolean connected = false;
    boolean started = false;
    
    static {
	JMFSecurityManager.loadLibrary("jmds");
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
    
	if (connected)
	    return;

	MediaLocator locator = getLocator();
	String remainder = locator.getRemainder();
	int deviceNo = -1;

	if (remainder.startsWith("//")) {
	  remainder = remainder.substring(2);
	  try {
	    deviceNo = Integer.valueOf(remainder).intValue();
	  }
	  catch (NumberFormatException e) {
	    deviceNo = -1;
	  }
	}
	if (deviceNo == -1) {
	  System.out.println("Illegal locator, using default device");
	  System.out.println("Use: ds://<DeviceNumber> for specific device");
	  deviceNo = 0;
	}
	if (!buildDSGraph(deviceNo)) {
	  System.out.println("Error building DirectShow Filter Graph");
	  throw new IOException("Error building DirectShow Filter Graph");
	}
	stream = new DSSourceStream(); // should we have here parameters???
	connected = true;
    }

    /**
     * Build the DirectShow capture filter graph
     *
     * @return  true if succeed, false otherwise
     */
    private native boolean buildDSGraph(int deviceNo);
  
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
    
	if (!connected)
	    return;
    
	destroyDSGraph();
	stream = null;
	connected = false;
    }

    /**
     * Destroy the DirectShow capture filter graph
     */
    private native void destroyDSGraph();
  
    /**
     * Initiate data-transfer. The <CODE>start</CODE> method must be
     * called before data is available.
     *(You must call <CODE>connect</CODE> before calling <CODE>start</CODE>.)
     *
     * @exception IOException Thrown if there are IO problems with the source
     * when <CODE>start</CODE> is called.
     */
    public void start() throws IOException {
    
	if (started)
	    return;

	stream.start(); // start a thread to initiate the Processor's read() calls
	if (!startDSGraph()) {
	  System.out.println("Error while starting DirectShow Filter Graph");
	  throw new IOException("Error while starting DirectShow Filter Graph");
	}
	started = true;
    }

    /**
     * Start the DirectShow capture filter graph
     *
     * @return true  if succeed, false otherwise
     */
    private native boolean startDSGraph();
  
    /**
     * Stop the data-transfer.
     * If the source has not been connected and started,
     * <CODE>stop</CODE> does nothing.
     */
    public void stop() throws IOException {

	if (!started)
	    return;

	stream.stop();
	if (!stopDSGraph()) {
	  System.out.println("Error stopping DirectShow Filter Graph");
	  throw new IOException("Error stopping DirectShow Filter Graph");
	}
    
	started = false;
    }

    /**
     * Stop the DirectShow capture filter graph
     *
     * @return true  if succeed, false otherwise
     */
    private native boolean stopDSGraph();
  
    /**
     * Get the collection of streams that this source
     * manages. The collection of streams is entirely
     * content dependent. The <code>ContentDescriptor</code>
     * of this <CODE>DataSource</CODE> provides the only indication of
     * what streams can be available on this connection.
     *
     * @return The collection of streams for this source.
     */
    public PushBufferStream[] getStreams() {
    
	streams[0] = stream;
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
    public Time getDuration() {

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
