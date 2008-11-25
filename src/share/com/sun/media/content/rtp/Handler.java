/* 
 * @(#)Handler.java	1.51 02/08/21
 * 
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.media.content.rtp;

import java.io.*;
import java.awt.*;
import java.net.*;
import java.awt.event.*;
import java.util.Vector;

import javax.media.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.rtp.rtcp.*;
import javax.media.protocol.*;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.Format;
import javax.media.format.FormatChangeEvent;
import javax.media.control.BufferControl;

import com.sun.media.util.*;
import com.sun.media.*;
import com.sun.media.ui.*;
import com.sun.media.controls.*;
import com.sun.media.rtp.*;
import com.sun.media.protocol.BufferListener;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;


/**
 * MediaPlayer extends BasicPlayer and uses MediaEngine to play media.
 */

public class Handler extends BasicPlayer 
	implements ReceiveStreamListener, BufferListener
{
    RTPSessionMgr mgrs[] = null;
    DataSource sources[] = null;
    Player players[] = null;
    Format formats[] = null;
    Format formatChanged[] = null;
    boolean realized[] = null;
    boolean dataReady[] = null;
    Vector locators = null;	// Media Locators.
    ControllerListener listener = new PlayerListener(this);

    boolean playersRealized = false;
    Object realizedSync = new Object();
    Object closeSync = new Object();
    Object dataSync = new Object();
    Object stateLock = new Object();
    private boolean closed = false;
    private boolean audioEnabled = false;
    private boolean videoEnabled = false;
    private boolean prebuffer = false;
    private boolean dataAllReady = false;

    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege=false;
    private Method m[] = new Method[1];
    private Class cl[] = new Class[1];
    private Object args[][] = new Object[1][0];    

    public Handler() {
        framePositioning = false;
	bufferControl = new BC();
	stopThreadEnabled = true;
    }

    String sessionError = "cannot create and initialize the RTP session.";
    
    protected boolean doRealize() {

        super.doRealize();

        try {
            
            if (source instanceof RTPSocket) {
                // this constructor will take care of initliasing and
                // starting the session as well as updating the encodings
                // from the RTPControl
		
		mgrs = new RTPSessionMgr[1];

                mgrs[1] = new RTPSessionMgr((RTPSocket)source);
		mgrs[1].addReceiveStreamListener(this);

		sources = new DataSource[1];
		players = new Player[1];
		formats = new Format[1];
		realized = new boolean[1];
		dataReady = new boolean[1];
		formatChanged = new Format[1];

		sources[0] = source;
		dataReady[0] = false;

            } else {
		RTPMediaLocator rml;
		InetAddress ipAddr;
		SessionAddress localAddr = new SessionAddress();
		SessionAddress destAddr;

		mgrs = new RTPSessionMgr[locators.size()];
		sources = new DataSource[locators.size()];
		players = new Player[locators.size()];
		formats = new Format[locators.size()];
		realized = new boolean[locators.size()];
		dataReady = new boolean[locators.size()];
		formatChanged = new Format[locators.size()];

		for (int i = 0; i < locators.size(); i++) {
		    rml = (RTPMediaLocator)locators.elementAt(i);
		    realized[i] = false;

		    mgrs[i] = (RTPSessionMgr) RTPManager.newInstance();
		    mgrs[i].addReceiveStreamListener(this);

		    ipAddr = InetAddress.getByName(rml.getSessionAddress());

		    if( ipAddr.isMulticastAddress()) {
			// local and remote address pairs are identical:
			localAddr= new SessionAddress( ipAddr,
						   rml.getSessionPort(),
						   rml.getTTL());

			destAddr = new SessionAddress( ipAddr,
						   rml.getSessionPort(),
						   rml.getTTL());
			    
		    } else {
			localAddr= new SessionAddress(InetAddress.getLocalHost(),
			  		           rml.getSessionPort());

                        destAddr = new SessionAddress(ipAddr,
						   rml.getSessionPort());
		    }
			
		    mgrs[ i].initialize( localAddr);
		    
		    if (prebuffer) {
			BufferControl bc = (BufferControl)mgrs[i].getControl("javax.media.control.BufferControl");
			bc.setBufferLength(bufferControl.getBufferLength());
			bc.setMinimumThreshold(bufferControl.getMinimumThreshold());
		    }
		    
    		    mgrs[i].addTarget(destAddr);
		}
	    }
            
        } catch (Exception e){
            Log.error("Cannot create the RTP Session: " + e.getMessage());
	    processError = sessionError;
            return false;
        }

	// dont realize this meta player until our player is realized
	try{
	    synchronized (realizedSync) {
		while (!playersRealized && !isInterrupted() && !closed)
		    realizedSync.wait();
	    }
	} catch (Exception e) {}

	// If realize is being interrupted, return failure from realize.
	if (closed || isInterrupted()) {
	    resetInterrupt();
	    processError = "no RTP data was received."; 
	    return false;
	}

        return true;
    }


    protected void completeRealize() {
	state = Realized;
	super.completeRealize();
    }


    protected void doStart() {
	super.doStart();

	synchronized (dataSync) {
	    if (prebuffer) {
		dataAllReady = false;
		for (int i = 0; i < dataReady.length; i++) {
		    dataReady[i] = false;
		    ((com.sun.media.protocol.rtp.DataSource)sources[i]).flush();
		    ((com.sun.media.protocol.rtp.DataSource)sources[i]).prebuffer();
		}
		// Wait atmost 3 secs for data prebuffering.
		if (!dataAllReady && !closed) {
		    try {
			dataSync.wait(3000);
		    } catch (Exception e) {}
		}
		//System.err.println("data prebuffered: " + dataAllReady);
	    }
	}

	// Start the component players.
	for (int i = 0; i < players.length; i++) {
	    try {
		if (players[i] != null)
		    waitForStart(players[i]);
		    //players[i].start();
	    } catch (Exception e) {}
	}
    }


    protected void doStop() {
	super.doStop();

	synchronized (dataSync) {
	    if (prebuffer) {
		dataSync.notify();
	    }
	}

	// Stop the component players.
	for (int i = 0; i < players.length; i++) {
	    try {
		if (players[i] != null)
		    waitForStop(players[i]);
		    //players[i].stop();
	    } catch (Exception e) {}
	}
    }


    protected void doDeallocate() {
	for (int i = 0; i < players.length; i++) {
	    try {
		if (players[i] != null)
		    players[i].deallocate();
	    } catch (Exception e) {}
	}
	synchronized (realizedSync) {
	    realizedSync.notify();
	}
    }


    protected void doFailedRealize() {

	synchronized (closeSync) {

	    for (int i = 0; i < mgrs.length; i++) {
		if (mgrs[i] != null) {
		    mgrs[i].removeTargets( "Closing session from the RTP Handler");
		    mgrs[i].dispose();
		    mgrs[i] = null;
		}
	    }
	}

	super.doFailedRealize();
    }


    protected void doClose() {

	closed = true;

	synchronized (realizedSync) {
	    realizedSync.notify();
	}

	synchronized (dataSync) {
	    dataSync.notifyAll();
	}

	stop();
	for (int i = 0; i < players.length; i++) {
	    try {
		if (players[i] != null)
		    players[i].close();
	    } catch (Exception e) {}
	}

	// close the RTP session.
	synchronized (closeSync) {
	    for (int i = 0; i < mgrs.length; i++) {
		if (mgrs[i] != null) {
                    mgrs[i].removeTargets( "Closing session from the RTP Handler");
                    mgrs[i].dispose();
		    mgrs[i] = null;
		}
	    }
	}

	super.doClose();
    }

    public void setTimeBase(TimeBase tb) throws IncompatibleTimeBaseException {
    }

    protected TimeBase getMasterTimeBase() {
	return new SystemTimeBase();
    }

    public float setRate(float rate) {
	if (getState() < Realized) {
	    throwError(new NotRealizedError("Cannot set rate on an unrealized Player."));
	}

	// Cannot play any rate other than 1.0.
	return 1.0f;
    }

    public void setStopTime(Time t) {
	controllerSetStopTime(t);
    }

    protected void stopAtTime() {
	controllerStopAtTime();
    }

    public synchronized void addController(Controller newController)
	throws IncompatibleTimeBaseException {
	int playerState = getState();
	
	if (playerState == Started) {
	    throwError(new ClockStartedError("Cannot add controller to a started player"));
	}
	
	if ( (playerState == Unrealized) || (playerState == Realizing) ) {
	    throwError(new NotRealizedError("A Controller cannot be added to an Unrealized Player"));
	}

	throw new IncompatibleTimeBaseException();
    }

    protected boolean audioEnabled() {
	return audioEnabled;
    }


    protected boolean videoEnabled() {
	return videoEnabled;
    }


    private void sendMyEvent(ControllerEvent e) {
	super.sendEvent(e);
    }


    public void update( ReceiveStreamEvent event) {

	RTPSessionMgr mgr = (RTPSessionMgr)event.getSource();
	int idx;

	for (idx = 0; idx < mgrs.length; idx++) {
	    if (mgrs[idx] == mgr)
		break;
	}

	if (idx >= mgrs.length) {
	    // Something's wrong.
	    System.err.println("Unknown manager: " + mgr);
	    return;
	}

	if( event instanceof RemotePayloadChangeEvent) {
     
	    Log.comment("Received an RTP PayloadChangeEvent");
	    RTPControl ctl = (RTPControl)sources[idx].getControl("javax.media.rtp.RTPControl");

	    if (ctl != null)
		formatChanged[idx] = ctl.getFormat();

	    // payload has changed. we need to close the old player and
	    // create a new player  
	    if (players[idx] != null) {

		// stop player
		stop();

		// Now, close the rtp player.
		// Wait till the old player is closed
		waitForClose(players[idx]);
	    }

	    try {
		// when the player was closed, its datasource was
		// disconnected. Now we must reconnect the datasource before
		// a player can be created for it.
		sources[idx].connect();
		players[idx] = javax.media.Manager.createPlayer(sources[idx]);
        
		if (players[idx] == null) {
		    Log.error("Could not create player for the new RTP payload.");
		    return;
		}
		players[idx].addControllerListener(listener);
		players[idx].realize();

	    }catch (Exception e){
		Log.error("Could not create player for the new payload.");
	    }

	} // payload change event
    
	if (event instanceof NewReceiveStreamEvent){

	    if (players[idx] != null) {
		// We've already created a player for this.  So this must
		// be the second stream in the same session.  We won't
		// deal with it.
		return;
	    }

	    ReceiveStream stream = null;
	    try {
		stream =((NewReceiveStreamEvent)event).getReceiveStream();
		sources[idx] = stream.getDataSource();

		RTPControl ctl = (RTPControl)sources[idx].getControl("javax.media.rtp.RTPControl");
		if (ctl != null){
		    formats[idx] = ctl.getFormat();
		    if (formats[idx] instanceof AudioFormat)
			audioEnabled = true;
		    else if (formats[idx] instanceof VideoFormat)
			videoEnabled = true;
		}

		if (source instanceof RTPSocket)
		    ((RTPSocket)source).setChild(sources[idx]);
		else
		    ((com.sun.media.protocol.rtp.DataSource)source).
		      setChild((com.sun.media.protocol.rtp.DataSource)sources[idx]);
		// create a player by passing datasource to the Media Manager
		players[idx] = javax.media.Manager.createPlayer(sources[idx]);
		if (players[idx] == null)
		    return;

		players[idx].addControllerListener(listener);
		players[idx].realize();

		if (prebuffer)
		    ((com.sun.media.protocol.rtp.DataSource)sources[idx]).setBufferListener(this);

	    } catch (Exception e) {
		Log.error("NewReceiveStreamEvent exception " + e.getMessage());
		return;
	    }
        
	}// instanceof newReceiveStreamEvent
    }


    private void waitForStart(Player p) {
	(new StateWaiter()).waitForStart(p, true);
    }

  
    private void waitForStop(Player p) {
	(new StateWaiter()).waitForStart(p, false);
    }


    private void waitForClose(Player p) {
	(new StateWaiter()).waitForClose(p);
    }

  
    class StateWaiter implements ControllerListener {

	boolean closeDown = false;
	Object stateLock = new Object();

	public void waitForStart(Player p, boolean startOn) {
	    p.addControllerListener(this);
	    if (startOn)
		p.start();
	    else
		p.stop();
	    synchronized (stateLock) {
		while (((startOn && p.getState() != Started) ||
		        (!startOn && p.getState() == Started)) &&
		       !closeDown) {
		   try {
			stateLock.wait(1000);
		   } catch (InterruptedException ie) {
			break;
		   }
		}
	    }
	    p.removeControllerListener(this);
	}

	public void waitForClose(Player p) {
	    p.addControllerListener(this);
	    p.close();
	    synchronized (stateLock) {
		while (!closeDown) {
		   try {
			stateLock.wait(1000);
		   } catch (InterruptedException ie) {
			break;
		   }
		}
	    }
	    p.removeControllerListener(this);
	}

	public void controllerUpdate(ControllerEvent ce) {
	    if (ce instanceof ControllerClosedEvent ||
		ce instanceof ControllerErrorEvent) {
		closeDown = true;
	    }
	    synchronized (stateLock) {
		stateLock.notify();
	    }
	}
    }


    public void setSource(javax.media.protocol.DataSource source)
        throws IOException, IncompatibleSourceException
    {
            super.setSource(source);
            if (source instanceof com.sun.media.protocol.rtp.DataSource){
                MediaLocator ml = source.getLocator();
		String mlStr = ml.getRemainder();
		String str;
		int start, idx;

		start = 0;
		while (mlStr.charAt(start) == '/')
		    start++;
	 	locators = new Vector();
		RTPMediaLocator rml;
		try {
		    while (start < mlStr.length() &&
		 	    (idx = mlStr.indexOf("&", start)) != -1) {
			str = mlStr.substring(start, idx);
                	rml = new RTPMediaLocator("rtp://" + str);
			locators.addElement(rml);
			start = idx+1;
		    }
		    if (start != 0)
			str = mlStr.substring(start);
		    else
			str = mlStr;
                    rml = new RTPMediaLocator("rtp://" + str);
		    locators.addElement(rml);

                } catch (Exception e) {
		    throw new IncompatibleSourceException();
		}

		if (locators.size() > 1)
		    prebuffer = true;

            } else if (!(source instanceof RTPSocket)) {
                throw new IncompatibleSourceException();
	    }
		
            // we will set the dynamic encoding for the one
            // encoding we dont use from static RTP payloads
            RTPControl ctl = (RTPControl)
                        source.getControl("javax.media.rtp.RTPControl");
                if (ctl != null)
                    ctl.addFormat(new AudioFormat(AudioFormat.DVI_RTP,
                                     	          44100,
                                                  4,
                                                  1),
                                      18);
    }


    private void invalidateComp(){
	controlComp = null;
	controls = null;
    }


    /**
     * Obtain the visiual component from the media engine.
     */
    public Component getVisualComponent() {
        /**
         * Call the superclass method to ensure that restrictions
         * on player methods are enforced
         */
        super.getVisualComponent();
	for (int i = 0; i < players.length; i++) {
	    if (players[i] != null && players[i].getVisualComponent() != null)
		return players[i].getVisualComponent();
	}
        return null;
    }


    /**
     * Return the list of controls from its slave controllers plus the
     * ones that this player supports.
     * @return the list of controls supported by this player.
     */
    public Control [] getControls() {
	if (controls != null)
	    return controls;

	// build the list of controls.  It is the total of all the
	// controls from each controllers plus the ones that are maintained
	// by the player itself (e.g. playbackControl).

	Vector cv = new Vector();

	if (cachingControl != null)
	    cv.addElement(cachingControl);

	if (bufferControl != null)
	    cv.addElement(bufferControl);

	Control c;
	Object cs[];
	Controller ctrller;
	int i, size = players.length;
	for (i = 0; i < size; i++) {
	    ctrller = (Controller)players[i];
	    cs = ctrller.getControls();
	    if (cs == null) continue;
	    for (int j = 0; j < cs.length; j++) {
		cv.addElement(cs[j]);
	    }
	}

	Control ctrls[];
	size = cv.size();
	ctrls = new Control[size];

	for (i = 0; i < size; i++) {
	    ctrls[i] = (Control)cv.elementAt(i);
	}

	// If the player has already been realized, we'll save what
	// we've collected this time.  Then next time, we won't need
	// to go through this expensive search again. 
	if (getState() >= Realized)
	    controls = ctrls;

	return ctrls;
    }
    

    public void updateStats() {
	for (int i = 0; i < players.length; i++) {
	    if (players[i] != null)
		((BasicPlayer)players[i]).updateStats();
	}
    }


    public void minThresholdReached(DataSource ds) {
	boolean ready = true;
	synchronized (dataSync) {
	    for (int i = 0; i < sources.length; i++) {
		if (sources[i] == ds)
		    dataReady[i] = true;
		else if (!dataReady[i])
		    ready = false;
	    }
	    if (!ready)
		return;

	    dataAllReady = true;
	    dataSync.notify();
	}
    }


    class PlayerListener implements ControllerListener {

      Handler handler;

      public PlayerListener(Handler handler) {
	this.handler = handler;
      }

      public synchronized void controllerUpdate(ControllerEvent ce) {

	Player p = (Player)ce.getSourceController();
	int idx;

	if (p == null)
	    return;

	for (idx = 0; idx < players.length; idx++) {
	    if (players[idx] == p)
		break;
	}

	if (idx >= players.length) {
	    // Something's wrong.
	    System.err.println("Unknown player: " + p);
	    return;
	}

	if (ce instanceof RealizeCompleteEvent) {

	    // if this is a payload change, add this player as
	    // controller and start the meta player  
	    if (formatChanged[idx] != null){
		try {

		    // now send a FCE
		    // invalidate control component
		    invalidateComp();
                 
		    FormatChangeEvent f = new FormatChangeEvent(
                                      (Controller)handler,
                                      formats[idx],
                                      formatChanged[idx]);
			((com.sun.media.content.rtp.Handler)handler).sendMyEvent(f);
		    formats[idx] = formatChanged[idx];
		    formatChanged[idx] = null;

		    // now start the meta player
		    //start();

		}catch (Exception e){
		    e.getMessage();
		}
	    }

	    realized[idx] = true;

	    // Check to see if all the players are realized.
	    for (int i = 0; i < realized.length; i++) {
		if (!realized[i])
		    return;
	    }

	    // The meta player is considered to be realized if all
	    // the component players are all realized.
	    synchronized (realizedSync){
		playersRealized = true;
		realizedSync.notifyAll();
	    }

	}

	if (ce instanceof ControllerErrorEvent) {
		players[idx].removeControllerListener( this );
		Log.error("RTP Handler internal error: " + ce);
		players[idx] = null;
	}

      }// end of controllerUpdate

    } // end of PlayerListener;


    /**
     * BufferControl for the renderer.
     */
    class BC implements BufferControl, Owned {

	long len = -1;
	long min = -1;

	public long getBufferLength() {
	    if (len < 0)
		return (prebuffer ? 750L : 125L);
	    return len;	
	} 

	public long setBufferLength(long time) {
	    len = time;
	    Log.comment("RTP Handler buffer length set: " + len);
	    return len;
	}
  
	public long getMinimumThreshold() {
	    if (min < 0)
		return (prebuffer ? 125L : 0);
	    return min;	
	}

	public long setMinimumThreshold(long time) {
	    min = time;
	    Log.comment("RTP Handler buffer minimum threshold: " + min);
	    return min;
	}
  
	public void setEnabledThreshold(boolean b) {
	}
  
	public boolean getEnabledThreshold() {
	    return (getMinimumThreshold() > 0);
	}

	public java.awt.Component getControlComponent() {
	    return null;
	}

	public Object getOwner() {
	    return Handler.this;
	}
    }
        
    
}// end of Handler 




