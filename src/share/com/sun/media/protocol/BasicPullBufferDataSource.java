/*
 * @(#)BasicPullBufferDataSource.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol;

import javax.media.Time;
import javax.media.MediaLocator;
import javax.media.protocol.*;
import java.io.IOException;

public abstract class BasicPullBufferDataSource extends PullBufferDataSource {

    protected Object [] controls = new Object[0];
    protected boolean started = false;
    protected String contentType = "content/unknown";
    protected boolean connected = false;
    protected Time duration = DURATION_UNKNOWN;
    
    public String getContentType() {
	if (!connected){
            System.err.println("Error: DataSource not connected");
            return null;
        }
	return contentType;
    }

    public void connect() throws IOException {
	 if (connected)
            return;
	 connected = true;
    }

    public void disconnect() {
	try{
            if (started)
                stop();
        }catch (IOException e){}
	connected = false;
    }

    public void start() throws IOException {
	// we need to throw error if connect() has not been called
        if (!connected)
            throw new java.lang.Error("DataSource must be connected before it can be started");
        if (started)
            return;
	started = true;
    }

    public void stop() throws IOException {
	if ((!connected) || (!started))
	    return;
	started = false;
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
	return duration;
    }
}
