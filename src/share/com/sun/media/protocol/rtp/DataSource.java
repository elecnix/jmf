/*
 * @(#)DataSource.java	1.15 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.rtp;

import java.io.*;
import javax.media.protocol.*;
import com.sun.media.*;
import com.sun.media.protocol.*;
import javax.media.*;
import javax.media.rtp.*;
import com.sun.media.rtp.RTPSessionMgr;
import com.sun.media.rtp.RTPControlImpl;
import com.sun.media.rtp.SSRCInfo;
import com.sun.media.rtp.RTPSourceStream;
import com.sun.media.protocol.BufferListener;


public class DataSource extends BasicPushBufferDataSource 
	implements Streamable, RTPSource
{
    static int SSRC_UNDEFINED = 0;  // RTPMediaLocator.SSRC_UNDEFINED

    private RTPSourceStream[] srcStreams = null;
    private boolean stopped = true;
    Player streamplayer = null;
    RTPSessionMgr mgr = null;
    RTPControl rtpcontrol = null;
    DataSource childsrc = null;
    int ssrc = SSRC_UNDEFINED;
    
    
    public void setMgr(RTPSessionMgr mgr){
	//System.out.println("manager being set to " + mgr);
	this.mgr = mgr;
    }

    public RTPSessionMgr getMgr(){
	return mgr;
    }

    public void setChild(DataSource source){
	childsrc = source;
    }

    public DataSource()
    {
	srcStreams = new RTPSourceStream[1];
	rtpcontrol = new MyRTPControl();
	//setContentType(ContentDescriptor.RAW);
	setContentType("rtp");
    }

    /**
     * Obtain the collection of streams that this source
     * manages. The collection of streams is entirely
     * content dependent. The mime-type of this
     * DataSource provides the only indication of
     * what streams can be available on this connection.
     *
     * @return collection of streams for this source.
     */
    public PushBufferStream[] getStreams() {
 	if (!connected)
 	    return null;
 	return srcStreams;
    }

    public void setPlayer(Player player){
	streamplayer = player;
    }

    public Player getPlayer(){
	return streamplayer;
    }

    public void setSourceStream(RTPSourceStream stream){
	if (srcStreams != null)
	    srcStreams[0] = stream;
    }

    public void setBufferListener(BufferListener listener) {
	srcStreams[0].setBufferListener(listener);
    }

    public void setLocator(MediaLocator mrl){
	super.setLocator(mrl);
    }

    public void setBufferWhenStopped(boolean flag) {
	srcStreams[0].setBufferWhenStopped(flag);
    }

    public void prebuffer() {
	started = true;
	srcStreams[0].prebuffer();
    } 

    /**
     * A method to flush the data buffers int the DataSource.
     */
    public void flush() {
	srcStreams[0].reset();
    }

    public void setSSRC(int ssrc) {
	this.ssrc = ssrc;
    }

    public int getSSRC() {
	return ssrc;
    }

    public String getCNAME() {
	if (mgr == null)
	    return null;
	SSRCInfo info = mgr.getSSRCInfo(ssrc);
	if (info != null)
	    return info.getCNAME();
	return null;
    }

    /**
     * Initiates data-transfer. Start must be called before
     * data is available. Connect must be called before start.
     * @exception IOException thrown if the source has IO trouble
     * at startup time.
     */
    public void start() throws IOException {
	super.start();
	if (childsrc != null)
	    childsrc.start();
	if (srcStreams != null){
	    for (int i = 0; i < srcStreams.length; i++)
		((com.sun.media.rtp.RTPSourceStream)srcStreams[i]).start();
	}
    }

    /**
     * Stops data-transfer.
     * If the source has not already been connected and started,
     * stop does nothing.
     */
    public void stop() throws IOException {
	super.stop();
	// stop your child source as well
	if (childsrc != null)
	    childsrc.stop();
	if (srcStreams != null){
	    for (int i = 0; i < srcStreams.length; i++)
		((com.sun.media.rtp.RTPSourceStream)srcStreams[i]).stop();
	}
    }

    public void setContentType(String contentType){
	this.contentType = contentType;
    }

    public boolean isStarted(){
	return started;
    }

    /**
     * Opens a connection to the source described by
     * the URL.<p>
     * Connect initiates communmication with the source.
     *
     * @exception IOException thrown if the connect has IO trouble.
     */
    public void connect() throws IOException {
	// start the RTPSessionManager by calling startSession()
	// this will throw an IOException if there is any problem
	/*
	if (mgr != null){
	    mgr.startSession();
	}
	*/
	if (srcStreams != null){
	  for (int i = 0; i < srcStreams.length; i++){
	    if (srcStreams[i] != null)
	      ((com.sun.media.rtp.RTPSourceStream)srcStreams[i]).connect();
	  }
	}
	connected = true;
    }
    
    /**
     * Close the connection to the source described by the URL. <p>
     * Disconnect frees resources used to maintain a connection
     * to the source. If no resources are in use, disconnect
     * is ignored.
     * Implies a stop, if stop hasn't already been called.
     */
    public void disconnect() {
	// once we have disconnected, set boolean to false
	// If this datasource was created by using the RTPAPI and not
	// via Manager, we dont want to disconnect this source i.e. we
	// dont want to closeSession() on RTPSM. In this case, the
	// datasource will not have a manager set to it. In this case,
	// the datasource cannot really be disconnected until the
	// session manager is closed by using the RTPAPI
	if (srcStreams != null){
	    for (int i = 0; i < srcStreams.length; i++)
		((com.sun.media.rtp.RTPSourceStream)srcStreams[i]).close();
	}
	/*
	if (mgr != null){
	    // close the RTPSourceStream
	    mgr.removeDataSource(this);
	    mgr.closeSession();
	    mgr = null;
	     // to fix bug 4174773, multiple stream problem 9/18/98
            started = false;
	    connected = false;
	    return;
	}
	*/
    }
    
    
    /**
     * Returns an zero length array because no controls
     * are supported.
     *
     * @return a zero length <code>Object</code> array.
     */
    public Object[] getControls() {
	// return a one element array of rtpcontrol object
	RTPControl[] controls = new RTPControl[1];
	controls[0] = rtpcontrol;
	return controls;	    
    }

    public void setControl(Object control){
	rtpcontrol = (RTPControl)control;
    }

    /**
     * Returns <code>null</code> because no controls are implemented.
     *
     * @return <code>null</code>.
     */
    public Object getControl(String type) {
	Class cls;
	try {
	    cls = Class.forName(type);
	} catch (ClassNotFoundException e) {
	    return null;
	}
	Object cs[] = getControls();
	for (int i = 0; i < cs.length; i++) {
	    if (cls.isInstance(cs[i]))
		return cs[i];
	}
	return null;
    }
    
    public boolean isPrefetchable() {
	return false;
    } 


    class MyRTPControl extends RTPControlImpl {

	public int getSSRC() {
	    return ssrc;
	}

	public String getCNAME() {
	    if (mgr == null)
		return null;
	    SSRCInfo info = mgr.getSSRCInfo(ssrc);
	    if (info != null)
		return info.getCNAME();
	    return null;
	}
    }
}

