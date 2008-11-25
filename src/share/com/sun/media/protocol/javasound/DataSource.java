/*
 * @(#)DataSource.java	1.12 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.javasound;

import java.io.IOException;
import javax.media.*;
import javax.media.protocol.*;
import javax.media.format.*;
import javax.media.format.AudioFormat;
import javax.media.control.FormatControl;
import com.sun.media.protocol.BasicPushBufferDataSource;


public class DataSource extends BasicPushBufferDataSource implements javax.media.protocol.CaptureDevice, RateConfigureable {

    PushBufferStream [] streams = new PushBufferStream[0];
    JavaSoundSourceStream sourceStream = null;
    String contentType;
    Time duration;
    boolean started = false;

    //	static String ContentType = "raw";
    static String ContentType = ContentDescriptor.RAW;

    public DataSource() {
	com.sun.media.JMFSecurityManager.checkCapture();
	contentType = ContentType;
	duration = DURATION_UNBOUNDED;
	sourceStream = new JavaSoundSourceStream(this);
	streams = new PushBufferStream[1];
	streams[0] = sourceStream;
    }

    static public CaptureDeviceInfo [] listCaptureDeviceInfo() {
	return JavaSoundSourceStream.listCaptureDeviceInfo();
    }

    public CaptureDeviceInfo getCaptureDeviceInfo() {
	return (JavaSoundSourceStream.listCaptureDeviceInfo())[0];
    }

    public FormatControl[] getFormatControls() {
	FormatControl fc[] = new FormatControl[1];
	fc[0] = (FormatControl)sourceStream.getControl("javax.media.control.FormatControl");
	return fc;
    }

    public PushBufferStream [] getStreams() {
	//	System.err.println("JavaSoundDataSource.getStreams()");
	if (streams == null)
	    System.err.println(
		"DataSource needs to be connected before calling getStreams");
	return streams;
    }

    public void connect() throws IOException {
	//	System.err.println("JavaSoundDataSource.connect()");
	if (sourceStream.isConnected())
	    return;

	if (getLocator() != null)
	    sourceStream.setFormat(JavaSoundSourceStream.parseLocator(getLocator()));
	sourceStream.connect();
    }

    public void disconnect() {
	//	System.err.println("JavaSoundDataSource.disconnect()");
	sourceStream.disconnect();
    }

    public void start() throws IOException {
	//	System.err.println("JavaSoundDataSource.start()");
	sourceStream.start();
    }

    public void stop() throws IOException {
	//	System.err.println("JavaSoundDataSource.stop()");
	sourceStream.stop();
    }

    public String getContentType() {
	//	System.err.println("JavaSoundDataSource.getContentType() ");
	return contentType;
    }

    public Time getDuration() {
	//	System.err.println("JavaSoundDataSource.getDuration()");
	return duration;
    }

    boolean getStarted() {
	//	System.err.println("JavaSoundDataSource.getStarted()");
	return started;
    }

    public Object[] getControls() {
	//	System.err.println("JavaSoundDataSource.getControls()");
	Object o[] = sourceStream.getControls();
	return o;
    }

    public Object getControl(String name) {
	//	System.err.println("JavaSoundDataSource.getControl(): " + name);
	return sourceStream.getControl(name);
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
