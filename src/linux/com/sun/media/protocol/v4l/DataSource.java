/*
 * @(#)DataSource.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.v4l;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.control.FormatControl;
import java.io.IOException;

public class DataSource extends PushBufferDataSource implements CaptureDevice {

    protected Object [] controls = new Object[0];
    protected boolean started = false;
    protected String contentType = "raw";
    protected boolean connected = false;
    protected Time duration = DURATION_UNBOUNDED;
    protected V4LSourceStream [] streams = null;
    protected V4LSourceStream stream = null;
    
    public DataSource() {
    }

    // connect is called right after setLocator()
    // This is where we initialize the capture device and get its capabilities
    public void connect() throws IOException {
	// Dont connect a second time
	if (connected)
	    return;

	// Create a new V4LSourceStream array
	streams = new V4LSourceStream[1];
	// Try to open the stream for the specified locator
	// The locator has information such as cardno, format, size...
	try {
	    stream = new V4LSourceStream(getLocator());
	    controls = stream.getControls();
	} catch (Exception ex) {
	    throw new IOException(ex.toString());
	} catch (Error er) {
	    throw new IOException(er.toString());
	}
	streams[0] = stream;
	
	connected = true;
    }

    public String getContentType() {
	if (!connected){
            throw new Error("DataSource not connected yet!");
        }
	return contentType;
    }

    public CaptureDeviceInfo getCaptureDeviceInfo() {
	return stream.getCaptureDeviceInfo();
    }

    public FormatControl [] getFormatControls() {
	return stream.getFormatControls();
    }
    
    public void disconnect() {
	if (!connected)
	    return;

	synchronized (stream) {	    
	    try {
		if (started)
		    stop();
	    } catch (IOException e) {}
	    stream.close();
	    
	    connected = false;
	}
    }

    public void start() throws IOException {
	// we need to throw error if connect() has not been called
        if (!connected)
            throw new java.lang.Error("DataSource must be connected before it can be started");
	synchronized (stream) {
	    if (started)
		return;
	    started = true;
	    stream.start(true);
	}
    }

    public void stop() throws IOException {
	synchronized (stream) {
	    if ((!connected) || (!started))
		return;
	    started = false;
	    stream.start(false);
	}
    }

    public Object [] getControls() {
	return controls;
    }

    public Object getControl(String controlType) {
       try {
          Class  cls = Class.forName(controlType);
          Object cs[] = getControls();
          for (int i = 0; i < cs.length; i++) {
             if (cls.isInstance(cs[i]))
                return cs[i];
          }
          return null;

       } catch (Exception e) {   // no such controlType or such control
         return null;
       }
    }

    public Time getDuration() {
	// Unbounded
	return duration;
    }

    public PushBufferStream [] getStreams() {
	return streams;
    }
    
}
