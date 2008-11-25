/*
 * @(#)Handler.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.datasink.rtp;

import java.net.MalformedURLException;
import com.sun.media.rtp.*;
import com.sun.media.datasink.*;
import javax.media.*;
import javax.media.rtp.*;
import javax.media.rtp.rtcp.*;
import javax.media.protocol.*;
import java.io.IOException;
import java.awt.Component;
import java.net.InetAddress;

public class Handler extends BasicDataSink {

    private RTPMediaLocator rtpmrl = null;
    RTPManager rtpmanager = null;
    PushBufferDataSource source = null;
    SendStream rtpsendstream = null;

    public Object getControl(String controlType) {
	return null;
    }

    public Object[] getControls() {
	return new Object[0];
    }

    public Component getControlComponent() {
	return null;
    }
  
    public void setSource(DataSource source)
	throws IOException, IncompatibleSourceException {
    
	if ( ! (source instanceof PushBufferDataSource) ) {
	    throw new IncompatibleSourceException("Only supports PushBufferDataSource");
	}
	this.source = (PushBufferDataSource) source;
	PushBufferStream [] streams = this.source.getStreams();
	int numStreams = streams.length;
	System.out.println("streams is " + streams + " : " + numStreams);

	if ( (streams == null) || (numStreams <= 0) )
	    throw new IOException("source " + source + " doesn't have any streams");
    }

    /**
     * Set the output <code>MediaLocator</code>.
     * This method should only be called once; an error is thrown if
     * the locator has already been set.
     * @param output <code>MediaLocator</code> that describes where 
     *            the output goes.
     */
    public void setOutputLocator(MediaLocator output) {
	if (rtpmrl == null) {
	    System.out.println("sink: setOutputLocator " + output);
	    // need to validate the RTPMedialocator
	    try{
		rtpmrl = new RTPMediaLocator(output.toString());
	    }catch (MalformedURLException e){
		rtpmrl = null;
	    }
	} else {
	    throw new Error("setOutputLocator cannot be called more than once");
	}
    }
    
    /**
     * Get the output <code>MediaLocator</code> that describes where
     * the output of this DataSink goes.
     * @return the output <code>MediaLocator</code> for this 
     * <code>DataSink</code>.
     */
    public MediaLocator getOutputLocator() {
	return rtpmrl;
    }

    /**
     * Initiate data-transfer. 
     * You must call open before calling start
     *
     * @exception IOException Thrown if there are IO problems with the source
     * when start is called.
     */
    public void start() throws IOException {
	rtpsendstream.start();
    }
  
    /**
     * Stop the data-transfer.
     * If the source has not been connected and started,
     * <CODE>stop</CODE> does nothing.
     */
    public void stop() throws IOException {
	rtpsendstream.stop();
    }
  
    /**
     * Open a connection to the destination described by
     * the output MediaLocator.
     * <p>
     *
     * The open method establishes a channel with the destination
     as described in the medialocator.
     *
     * @exception IOException Thrown if there are IO problems
     * when open is called.
     * @exception SecurityException thrown if there is any security
     * violation while attempting to access the destination as described
     * by the medialocator  
     */
    public void open() throws IOException, SecurityException {
	// create, initialise and start the session manager here
	if (rtpmrl == null)
	    throw new IOException ("No Valid RTP MediaLocator");
	try {
	    String address = rtpmrl.getSessionAddress();
	    int port = rtpmrl.getSessionPort();
	    int ttl = rtpmrl.getTTL();
	    rtpmanager = RTPManager.newInstance();

	    // create our local Session Address
	    SessionAddress localaddr = new SessionAddress();
	    InetAddress destaddr = InetAddress.getByName(address);
	    SessionAddress sessaddr = new SessionAddress(destaddr, port, ttl);

	    rtpmanager.initialize(localaddr);
	    rtpmanager.addTarget(sessaddr);
	    rtpsendstream = rtpmanager.createSendStream(source,0);

	} catch (Exception  e) {
	    throw new IOException (e.getMessage());
	}
    } 

    /**
     * Close the connection to the source described by the locator.
     * <p>
     * The close method frees resources used to maintain a
     * connection to the destination described in the medialocator.
     * If no resources are in use, close is ignored.
     * If stop hasn't already been called,
     * calling close implies a stop.
     * The DataSink may no longer be used after being closed.
     */
    public void close() {
	if (rtpmanager != null) {
	    rtpmanager.removeTargets("DataSink closed");
	    rtpmanager.dispose();
	}
    }

    /**
     * Get a string that describes the content-type of the media
     * that the datasink is consuming.
     * @return The name that describes the media content.
     */
    public String getContentType() {
	return "RTP";
    }
  
}
