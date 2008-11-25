/*
 * @(#)DataSource.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.sunvideoplus;

import java.awt.Dimension;
import java.awt.Frame;
import java.io.IOException;
import javax.media.*;
import javax.media.protocol.*;
import com.sun.media.protocol.BasicPushBufferDataSource;
import javax.media.format.*;
import javax.media.format.VideoFormat;
import javax.media.format.RGBFormat;
import javax.media.control.FormatControl;

public class DataSource extends BasicPushBufferDataSource
implements CaptureDevice, RateConfigureable {

    PushBufferStream [] streams = new PushBufferStream[0];
    OPISourceStream sourceStream = null;
    String contentType;
    Time duration;
    boolean connected = false;
    boolean started = false;

    //	static String ContentType = "raw";
    static String ContentType = ContentDescriptor.RAW;
    //	static String ContentType = VideoFormat.JPEG;

    public DataSource() {
	com.sun.media.JMFSecurityManager.checkCapture();
	contentType = ContentType;
	duration = DURATION_UNBOUNDED;
    }

    public PushBufferStream [] getStreams() {
	//	System.err.println("OPIDataSource.getStreams()");
	if (!connected) {
	    System.err.println(
		"DataSource needs to be connected before calling getStreams");
	    return null;
	}
	return streams;
    }

    public javax.media.CaptureDeviceInfo getCaptureDeviceInfo() {
	//	System.err.println("OPIDataSource.getCaptureDeviceInfo()");
	if (sourceStream == null) {
	    sourceStream = new OPISourceStream(this);
	    streams = new PushBufferStream[1];
	    streams[0] = sourceStream;
	}
	return sourceStream.getCaptureDeviceInfo();
    }

    public javax.media.control.FormatControl[] getFormatControls() {
	//	System.err.println("OPIDataSource.getFormatControls()");
	if (sourceStream == null) {
	    sourceStream = new OPISourceStream(this);
	    streams = new PushBufferStream[1];
	    streams[0] = sourceStream;
	}
	FormatControl[] fc = new FormatControl[1];
	fc[0] = (FormatControl)sourceStream.getControl(
					"javax.media.control.FormatControl");
	return fc;
    }

    public void connect() throws IOException {
	//	System.err.println("OPIDataSource.connect()");
	if (connected)
	    return;
	if (sourceStream == null) {
	    sourceStream = new OPISourceStream(this);
	    streams = new PushBufferStream[1];
	    streams[0] = sourceStream;
	}
	sourceStream.connect();
	connected = true;
	// TODO
	//	super.connect();
    }

    public void disconnect() {
	//	System.err.println("OPIDataSource.disconnect()");
	// TODO
	sourceStream.disconnect();
	connected = false;
	//	super.disconnect();
    }

    public void start() throws IOException {
	if (started)
	    return;
	//	System.err.println("OPIDataSource.start()");
	sourceStream.start();
	started = true;
	//	super.start();
    }

    public void stop() throws IOException {
	if (!started)
	    return;
	//	System.err.println("OPIDataSource.stop()");
	sourceStream.stop();
	started = false;
	//	super.stop();
    }

    public String getContentType() {
	//	System.err.println("OPIDataSource.getContentType() ");
	return contentType;
    }

    public Time getDuration() {
	//	System.err.println("OPIDataSource.getDuration()");
	return duration;
    }

    boolean getStarted() {
	//	System.err.println("OPIDataSource.getStarted()");
	return started;
    }

    public Object[] getControls() {
	//	System.err.println("OPIDataSource.getControls()");
	if (sourceStream != null)
	    return sourceStream.getControls();
	return null;
    }

    public Object getControl(String name) {
	//	System.err.println("OPIDataSource.getControl()");
	if (sourceStream != null)
	    return sourceStream.getControl(name);
	return null;
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
	    SourceStream ss [] = { sourceStream };
	    return ss;
	}
    }
}
