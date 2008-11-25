/*
 * @(#)DataSource.java	1.11 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.vfw;

import java.awt.Dimension;
import java.awt.Frame;
import java.io.IOException;
import javax.media.*;
import javax.media.protocol.*;
import javax.media.control.FormatControl;
import com.sun.media.controls.FormatAdapter;
import com.sun.media.protocol.BasicPushBufferDataSource;
import javax.media.format.*;
import com.sun.media.format.AviVideoFormat;
import com.sun.media.vfw.*;
import java.util.Vector;

public class DataSource
    extends BasicPushBufferDataSource
    implements CaptureDevice, RateConfigureable {

    private PushBufferStream [] streams = null;
    private VFWSourceStream vfwStream = null;
    private static String ContentType = ContentDescriptor.RAW;
    private FormatControl fc = null;
    protected Control [] controls = null;
    
    public DataSource() {
	com.sun.media.JMFSecurityManager.checkCapture();
	contentType = ContentType;
	duration = DURATION_UNBOUNDED;
    }


    public void setLocator(MediaLocator loc) {
	super.setLocator(loc);
	if (vfwStream == null) {
	    vfwStream = new VFWSourceStream(getLocator());
	}
    }

    public PushBufferStream [] getStreams() {
	if (streams == null)
	    System.err.println("DataSource needs to be connected before calling getStreams");
	return streams;
    }

    public FormatControl [] getFormatControls() {
	if (vfwStream == null)
	    return new FormatControl[0];
	else
	    return new FormatControl[] {(FormatControl) vfwStream};
    }

    public CaptureDeviceInfo getCaptureDeviceInfo() {
	if (vfwStream == null)
	    return null;
	else
	    return vfwStream.getCaptureDeviceInfo();
    }


    public void connect() throws IOException {
	if (streams == null) {
	    streams = new PushBufferStream[1];
	    if (vfwStream == null)
		vfwStream = new VFWSourceStream(getLocator());
	    streams[0] = vfwStream;
	}

        vfwStream.connect(null);

	super.connect();
    }

    public void disconnect() {
	vfwStream.disconnect();
	super.disconnect();
    }

    public void start() throws IOException {
	vfwStream.start();
	super.start();
    }

    public void stop() throws IOException {
	vfwStream.stop();
	super.stop();
    }

    boolean getStarted() {
	return started;
    }

    public Object [] getControls() {
	if (vfwStream != null)
	    return vfwStream.getControls();
	else
	    return new Control[0];
    }

    /**
     * Return the control based on a control type for the PlugIn.
     */
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


    /**
     * Methods for the RateConfigurable interface.
     */
    public RateConfiguration[] getRateConfigurations() {
	RateConfiguration config [] = { new OneRateConfig() };
	return config;
    }

    public RateConfiguration setRateConfiguration(RateConfiguration config) {
	return config;
    }

    /////////////////////////////////////
    //
    // Inner class RateConfiguration
    /////////////////////////////////////
    class OneRateConfig implements RateConfiguration {
	public RateRange getRate() {
	    return new RateRange(1.0f, 1.0f, 1.0f, true);
	}

	public SourceStream[] getStreams() {
	    SourceStream ss [] = { vfwStream };
	    return ss;
	}
    }
}
