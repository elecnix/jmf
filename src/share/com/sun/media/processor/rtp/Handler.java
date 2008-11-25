/* 
 * @(#)Handler.java	1.17 02/08/21
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

package com.sun.media.processor.rtp;

import java.io.*;
import java.awt.*;
import java.util.Vector;
import javax.media.*;
import com.sun.media.*;
import com.sun.media.ui.*;
import com.sun.media.controls.*;
import com.sun.media.rtp.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.rtp.rtcp.*;
import java.net.*;
import javax.media.protocol.*;
import java.awt.event.*;
import com.sun.media.util.*;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.Format;
import javax.media.format.FormatChangeEvent;
import javax.media.control.TrackControl;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;


public class Handler extends BasicProcessor implements ReceiveStreamListener{

    RTPSessionMgr mgrs[] = null;
    DataSource sources[] = null;
    Processor processor = null;
    Format formats[] = null;
    Vector locators = null;

    Object dataLock = new Object();
    boolean dataReady = false;

    private boolean closed = false;
    private boolean audioEnabled = false;
    private boolean videoEnabled = false;

    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege=false;
    private Method m[] = new Method[1];
    private Class cl[] = new Class[1];
    private Object args[][] = new Object[1][0];    


    public Handler() {
        framePositioning = false;
    }

    String sessionError = "cannot create and initialize the RTP Session.";
    
    protected synchronized boolean doConfigure() {

        super.doConfigure();
 
        try {
            if (source instanceof RTPSocket) {
                // this constructor will take care of initliasing and
                // starting the session as well as updating the encodings
                // from the RTPControl
		
		mgrs = new RTPSessionMgr[1];

                mgrs[1] = new RTPSessionMgr((RTPSocket)source);
		mgrs[1].addReceiveStreamListener(this);

		sources = new DataSource[1];
		sources[0] = source;
		formats = new Format[1];
		dataReady = false;

            } else {

		RTPMediaLocator rml;
		InetAddress ipAddr;
		SessionAddress localAddr = new SessionAddress();
		SessionAddress destAddr;

		mgrs = new RTPSessionMgr[locators.size()];
		sources = new DataSource[locators.size()];
		formats = new Format[locators.size()];
		dataReady = false;

		for (int i = 0; i < locators.size(); i++) {
		    rml = (RTPMediaLocator)locators.elementAt(i);

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
			localAddr= new SessionAddress( InetAddress.getLocalHost(),
			  		           rml.getSessionPort());

                        destAddr = new SessionAddress( ipAddr,
						   rml.getSessionPort());
		    }
			
		    mgrs[ i].initialize( localAddr);
    		    mgrs[i].addTarget(destAddr);
		}
	    }
            
        } catch (Exception e){
            Log.error("Cannot create the RTP Session: " + e.getMessage());
	    processError = sessionError;
            return false;
        }

	// dont configure this meta player until the internal processor
	// is configured
	try{
	    synchronized (dataLock) {
		while (!dataReady && !isInterrupted() && !closed)
		    dataLock.wait();
	    }
	} catch (Exception e) {}

	// If configure is being interrupted, return failure from configure.
	if (closed || isInterrupted()) {
	    resetInterrupt();
	    processError = "no RTP data was received.";
	    return false;
	}
        
        return true;

    }// end of doConfigure()


    protected void completeConfigure() {
	state = javax.media.Processor.Configured;
	super.completeConfigure();
    }


    protected void doFailedConfigure() {
	closeSessions();
	super.doFailedConfigure();
    }


    Object closeSync = new Object();

    private void closeSessions() {
	synchronized (closeSync) {
	    for (int i = 0; i < mgrs.length; i++) {
		if (mgrs[i] != null) {
		    mgrs[i].removeTargets( "Closing session from the RTP Handler");
		    mgrs[i].dispose();
		    mgrs[i] = null;
		}
	    }
	}
    }


    protected boolean doRealize() {
	return waitForRealize(processor);
    }


    protected void completeRealize() {
	state = Realized;
	super.completeRealize();
    }


    protected void doFailedRealize() {
	closeSessions();
	super.doFailedRealize();
    }


    protected void doStart() {
	super.doStart();
	waitForStart(processor);
    }


    protected void doStop() {
	super.doStop();
	waitForStop(processor);
    }


    protected void doDeallocate() {
	processor.deallocate();
	synchronized (dataLock) {
	    dataLock.notifyAll();
	}
    }

    protected void doClose() {
	closed = true;

	synchronized (dataLock) {
	    dataLock.notify();
	}

	stop();

	if( processor != null) {
	    processor.close();
	}
	
	closeSessions();

	super.doClose();
    }

    public void setTimeBase(TimeBase tb) throws IncompatibleTimeBaseException {
    }

    protected TimeBase getMasterTimeBase() {
	return new SystemTimeBase();
    }

    protected boolean audioEnabled(){
	return audioEnabled;
    }

    protected  boolean videoEnabled(){
	return videoEnabled;
    }

    private void sendMyEvent(ControllerEvent e){
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

	if (event instanceof RemotePayloadChangeEvent) {

	    Log.comment("Received an RTP PayloadChangeEvent");
	    Log.error("The RTP processor cannot handle mid-stream payload change.\n");
	    sendEvent(new ControllerErrorEvent(this, "Cannot handle mid-stream payload change."));
	    close();

	}// payload change event
    
	if (event instanceof NewReceiveStreamEvent){

	    if (sources[idx] != null) {
		// We've already gotten a source from this session.
		return;
	    }

	    ReceiveStream stream = null;
	    try{
		// get a handle over the ReceiveStream
		stream =((NewReceiveStreamEvent)event).getReceiveStream();
		sources[idx] = stream.getDataSource();

		RTPControl ctl = (RTPControl)sources[idx].getControl("javax.media.rtp.RTPControl");
		if (ctl != null){
		    formats[idx] = ctl.getFormat();
		    if (formats[idx] instanceof AudioFormat)
			audioEnabled = true;
		    if (formats[idx] instanceof VideoFormat)
			videoEnabled = true;
		}

		if (source instanceof RTPSocket)
		    ((RTPSocket)source).setChild(sources[idx]);
		else
		    ((com.sun.media.protocol.rtp.DataSource)source).
			setChild((com.sun.media.protocol.rtp.DataSource)sources[idx]);

		for (int i = 0; i < sources.length; i++) {
		    // Return if not all sessions had yielded a source.
		    if (sources[i] == null)
			return;
		}

		// We've received all the sources, let create the processor.

		DataSource mixDS;

		try {
		    mixDS = javax.media.Manager.createMergingDataSource(sources);
		} catch (Exception e) {
		    System.err.println("Cannot merge data sources.");
		    return;
		}

		try {
		    processor = javax.media.Manager.createProcessor(mixDS);
		} catch (Exception e) {
		    System.err.println("Cannot create the mix processor.");
		    return;
		}

		if (!waitForConfigure(processor))
		    return;

		// We are done generating the internal processor.
		synchronized(dataLock) {
                    dataReady = true;
                    dataLock.notifyAll();
		}

	    } catch (Exception e){
		System.err.println("NewReceiveStreamEvent exception " +
		                     e.getMessage());
		  return;
	    }
        
	}// instanceof newReceiveStreamEvent
    }
  

    public void setSource(javax.media.protocol.DataSource source)
        throws IOException, IncompatibleSourceException {

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

    private void invalidateComp() {
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
        return processor.getVisualComponent();
    }

    /**
     * Return the list of controls from its slave controllers plus the
     * ones that this player supports.
     * @return the list of controls supported by this player.
     */
    public Control [] getControls() {
	return processor.getControls();
    }
    
    public void updateStats(){
        if ( processor != null) {
            ((BasicProcessor) processor).updateStats();
        }
    }

    public TrackControl[] getTrackControls() throws NotConfiguredError {
	super.getTrackControls();
	return processor.getTrackControls();
    }

    public ContentDescriptor[] getSupportedContentDescriptors() 
	throws NotConfiguredError {
	super.getSupportedContentDescriptors();
	return processor.getSupportedContentDescriptors();
    }
    
    public ContentDescriptor setContentDescriptor(ContentDescriptor ocd) 
	throws NotConfiguredError {
	super.setContentDescriptor(ocd);
	return processor.setContentDescriptor(ocd);
    }
    
    public ContentDescriptor getContentDescriptor() 
	throws NotConfiguredError {
	super.getContentDescriptor();
	return processor.getContentDescriptor();
    }
    
    public DataSource getDataOutput() throws NotRealizedError {
	super.getDataOutput();
	return processor.getDataOutput();
    }

    private boolean waitForConfigure(Processor p) {
	return (new StateWaiter()).waitForConfigure(p);
    }

    private boolean waitForRealize(Processor p) {
	return (new StateWaiter()).waitForRealize(p);
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

	public boolean waitForConfigure(Processor p) {
	    p.addControllerListener(this);
	    p.configure();
	    synchronized (stateLock) {
		while (p.getState() != javax.media.Processor.Configured && !closeDown) {
		   try {
			stateLock.wait(1000);
		   } catch (InterruptedException ie) {
			break;
		   }
		}
	    }
	    p.removeControllerListener(this);
	    return !closeDown;
	}

	public boolean waitForRealize(Processor p) {
	    p.addControllerListener(this);
	    p.realize();
	    synchronized (stateLock) {
		while (p.getState() != Realized && !closeDown) {
		   try {
			stateLock.wait(1000);
		   } catch (InterruptedException ie) {
			break;
		   }
		}
	    }
	    p.removeControllerListener(this);
	    return !closeDown;
	}

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
}




