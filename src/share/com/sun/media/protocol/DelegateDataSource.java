/*
 * @(#)DelegateDataSource.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol;

import javax.media.*;
import javax.media.protocol.*;
import com.sun.media.Log;
import java.io.IOException;


/**
 * This special DataSource is used to prebuild a streaming player
 * before the actual streaming DataSource is not available e.g. RTP.
 */ 
public class DelegateDataSource extends PushBufferDataSource implements Streamable {

    protected String contentType = ContentDescriptor.RAW;
    protected PushBufferDataSource master;
    protected DelegateStream streams[];

    protected boolean started = false;
    protected boolean connected = false;


    public DelegateDataSource(Format format[]) {
	streams = new DelegateStream[format.length];
	for (int i = 0; i < format.length; i++) {
	    streams[i] = new DelegateStream(format[i]);
	}
	try {
	    connect();
	} catch (IOException e) {}
    }

    public void setMaster(PushBufferDataSource ds) throws IOException {
	master = ds;

	PushBufferStream mstrms[] = ds.getStreams();
	for (int i = 0; i < mstrms.length; i++) {
	    for (int j = 0; j < streams.length; j++) {
		if (streams[j].getFormat().matches(mstrms[i].getFormat()))
		    streams[j].setMaster(mstrms[i]);
	    }
	}

	for (int i = 0; i < mstrms.length; i++) {
	    if (streams[i].getMaster() == null) {
		Log.error("DelegateDataSource: cannot not find a matching track from the master with this format: " + streams[i].getFormat());
	    }
	}

	if (connected)
	    master.connect();
	if (started)
	    master.start();
    }

    public javax.media.protocol.DataSource getMaster() {
	return master;
    }

    public PushBufferStream[] getStreams() {
	return streams;
    }

    public MediaLocator getLocator() {
	if (master != null)
	    return master.getLocator();
	return null;
    }

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
	if (master != null)
	    master.connect();
	connected = true;
    }

    public void disconnect() {
	try{
            if (started)
                stop();
        }catch (IOException e){}
	if (master != null)
	    master.disconnect();
	connected = false;
    }

    public void start() throws IOException {
	// we need to throw error if connect() has not been called
        if (!connected)
            throw new java.lang.Error("DataSource must be connected before it can be started");
        if (started)
            return;
	if (master != null)
	    master.start();
	started = true;
    }

    public void stop() throws IOException {
	if ((!connected) || (!started))
	    return;
	if (master != null)
	    master.stop();
	started = false;
    }

    public Object [] getControls() {
	if (master != null)
	    return master.getControls();
	return new Object[0];
    }

    public Object getControl(String controlType) {
	if (master != null)
	    return master.getControl(controlType);
	return null;
    }

    public Time getDuration() {
	if (master != null)
	    return master.getDuration();
	return Duration.DURATION_UNKNOWN;
    }

    public boolean isPrefetchable() {
	return false;
    }


    /////////////////////
    //
    // INNER CLASSES
    /////////////////////

    class DelegateStream implements PushBufferStream, BufferTransferHandler {

	Format format;
	PushBufferStream master;
	BufferTransferHandler th;

	public DelegateStream(Format format) {
	    this.format = format;
	}

	public void setMaster(PushBufferStream master) {
	    this.master = master;
	    master.setTransferHandler(this);
	}

	public PushBufferStream getMaster() {
	    return master;
	}

	public Format getFormat() {
	    if (master != null)
		return master.getFormat();
	    return format;
	}

	public ContentDescriptor getContentDescriptor() {
	    if (master != null)
		return master.getContentDescriptor();
	    return new ContentDescriptor(ContentDescriptor.RAW);
	}

	public long getContentLength() {
	    if (master != null)
		return master.getContentLength();
	    return LENGTH_UNKNOWN;
	}

	public boolean endOfStream() {
	    if (master != null)
		return master.endOfStream();
	    return false;
	}

	public void read(Buffer buffer) throws IOException {
	    if (master != null)
		master.read(buffer);
	    throw new IOException("No data available");
	}

	public void setTransferHandler(BufferTransferHandler transferHandler) {
	    th = transferHandler;
	}

	public void transferData(PushBufferStream stream) {
	    if (th != null)
		th.transferData(stream);
	}

	public Object [] getControls() {
	    if (master != null)
		return master.getControls();
	    return new Object[0];
	}

	public Object getControl(String controlType) {
	    if (master != null)
		return master.getControl(controlType);
	    return null;
	}
    }
}
