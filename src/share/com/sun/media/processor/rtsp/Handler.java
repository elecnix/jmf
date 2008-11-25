/* 
 * @(#)Handler.java	1.22 02/08/21
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

package com.sun.media.processor.rtsp;

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
import com.sun.media.rtsp.*;

import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.Format;
import javax.media.format.FormatChangeEvent;
import javax.media.control.TrackControl;
import javax.media.control.BufferControl;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;


public class Handler extends BasicProcessor implements ReceiveStreamListener {

    // states:
    private final int INITIALIZED = 0;
    private final int REALIZED = 1;
    private final int PLAYING = 2;
    private final int PAUSING = 3;

    private DataSource data_sources[];
    
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

    private boolean first_pass = true;

    com.sun.media.content.rtsp.RtspUtil rtspUtil;
    
    public Handler() {
        framePositioning = false;

	rtspUtil = new com.sun.media.content.rtsp.RtspUtil( this);
	
	locators = new Vector();
    }

    String sessionError = "cannot create and initialize the RTP Session.";
    
    protected synchronized boolean doConfigure() {
        boolean configured = super.doConfigure();

	if (configured) {
	    configured = initRtspSession();
	}

	return configured;
    }    
    
    private boolean initRtspSession() {
        boolean realized = false;
	
	MediaLocator ml=  (MediaLocator) locators.elementAt( 0);

	rtspUtil.setUrl( ml.toString());
	
        String ipAddress = rtspUtil.getServerIpAddress();

        if (ipAddress == null) {
            System.out.println( "Invalid server address.");

            realized = false;
        } else {
	    rtspUtil.setUrl( ml.toString());

	    realized= rtspUtil.createConnection();

	    if( realized) {
                realized = rtspUtil.rtspSetup();

                try {
                    InetAddress destaddr = InetAddress.getByName(ipAddress);

		    int server_ports[]= rtspUtil.getServerPorts();
		    
		    for( int i = 0; i < rtspUtil.getNumberOfTracks(); i++) {
                        SessionAddress remoteAddress =
                                new SessionAddress(destaddr, server_ports[ i]);

			
                        rtspUtil.getRTPManager( i).addTarget( remoteAddress);

			// Set 3/4 sec worth of buffering.
			BufferControl bc = (BufferControl)rtspUtil.getRTPManager( i).getControl("javax.media.control.BufferControl");

			String mediaType= rtspUtil.getMediaType( i);
			
			if( mediaType.equals( "audio")) {
			    bc.setBufferLength(250);
			    bc.setMinimumThreshold(125);
			} else if( mediaType.equals( "video")) {
			    bc.setBufferLength(1500);
			    bc.setMinimumThreshold(250);
			}
		    }
                } catch (Exception e) {
                    Log.error(e.getMessage());

                    return realized;
                }
            }
        }

        if (realized) {
            state = REALIZED;

	    int size= rtspUtil.getNumberOfTracks();

	    data_sources = new DataSource[ size];
	    formats= new Format[ size];
	    
	    // Start the server.
            if (!rtspUtil.rtspStart()) {
		if( first_pass &&
		    rtspUtil.getStatusCode() ==
		    com.sun.media.rtsp.protocol.StatusCode.SESSION_NOT_FOUND) {

		    first_pass= false;

		    return initRtspSession();
		}
		
		return false;
	    }
	    
	    // Wait for the initial filling of the data buffers and
	    // the RTP players to fully realized.
            waitForData();

	    // Now the players are all started.  
	    // Stop the server momentarily.  Rewind the media
	    // so the next start will start from the beginning again.
	    // This is sort of inefficient but will get by the initial
	    // buffering problem.
            // rtspStop();
            // startPos = 0;

	    // Now we need to flush the datasources to make sure
	    // the rewind starts from the very beginning.

	    // for( int i = 0; i < numberOfTracks; i++) {
		// data_sources[ i].flush();
	    // }
        }
	
        return realized;
    }

    private synchronized boolean waitForData() {                
        try {
            synchronized (dataLock) {
                while (!dataReady) {
                    dataLock.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return dataReady;
    }

    // is this code 'getProperty()' used at all? - Marc
    /*
    private String getProperty(String prop) {
	String value = null;
	if ( (jmfSecurity != null) ) {
	    try {
		if (jmfSecurity.getName().startsWith("jmf-security")) {
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.READ_PROPERTY);
		    m[0].invoke(cl[0], args[0]);
		} else if (jmfSecurity.getName().startsWith("internet")) {
		    PolicyEngine.checkPermission(PermissionID.PROPERTY);
		    PolicyEngine.assertPermission(PermissionID.PROPERTY);
		}
	    } catch (Throwable e) {
		if (JMFSecurityManager.DEBUG) {
		    System.err.println("Unable to get read property " +
				       " privilege  " + e);
		}
		jmfSecurity.permissionFailureNotification(JMFSecurity.READ_PROPERTY);
		// securityPrivelege = false;
	    }
	}
	try {
	    if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
		Constructor cons = jdk12PropertyAction.cons;
		value = (String) jdk12.doPrivM.invoke(
				     jdk12.ac,
				     new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               prop
                                           })});
	    } else {
		value = System.getProperty(prop);
	    }
	} catch (Throwable e) {
	}
	return value;
    }
    */

    protected void completeConfigure() {
	state = javax.media.Processor.Configured;
	super.completeConfigure();
    }

    protected void doFailedConfigure() {
	closeSessions();
	super.doFailedConfigure();
    }

    private void closeSessions() {
	RTPManager mgrs[]= rtspUtil.getRTPManagers();
	
	for (int i = 0; i < mgrs.length; i++) {
	    if (mgrs[ i] != null) {
                mgrs[ i].removeTargets( "Closing session from the RTP Handler");
                mgrs[ i].dispose();
            } 

            mgrs[ i] = null;
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
	processor.close();
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

    private void sendMyEvent(ControllerEvent e) {
	super.sendEvent(e);
    }

    public void update( ReceiveStreamEvent event) {
	RTPManager mgr = (RTPManager)event.getSource();
	int idx;

	// return if the data sources have not been allocated yet.
	// this may happen if data is coming in from a different
	// source before the PLAY message has been issued.
	if( data_sources == null) {
	    return;
	}
	
	RTPManager mgrs[]= rtspUtil.getRTPManagers();
	
	for (idx = 0; idx < mgrs.length; idx++) {
	    if (mgrs[idx] == mgr) {
		break;
	    }
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
	}

	if (event instanceof NewReceiveStreamEvent) {
	    if (data_sources[idx] != null) {
		// We've already gotten a source from this session.
		return;
	    }

	    ReceiveStream stream = null;

	    try {
		// get a handle over the ReceiveStream
		stream =((NewReceiveStreamEvent)event).getReceiveStream();
		data_sources[idx] = stream.getDataSource();

		RTPControl ctl = (RTPControl)data_sources[idx].getControl("javax.media.rtp.RTPControl");
		if (ctl != null) {
		    formats[idx] = ctl.getFormat();
		    
		    if (formats[idx] instanceof AudioFormat) {
			audioEnabled = true;
		    }
		    
		    if (formats[idx] instanceof VideoFormat) {
			videoEnabled = true;
		    }
		}

		/* this can't be done
		if (source instanceof RTPSocket) {
		    ((RTPSocket)source).setChild(data_sources[idx]);
		} else {
		    ((com.sun.media.protocol.rtp.DataSource)source).
			setChild((com.sun.media.protocol.rtp.DataSource)data_sources[idx]);
		}
		*/

		for (int i = 0; i < data_sources.length; i++) {
		    // Return if not all sessions had yielded a source.
		    if (data_sources[i] == null) {
			return;
		    }
		}
		
		// We've received all the sources, let create the processor.

		DataSource mixDS;

		try {
		    mixDS = javax.media.Manager.createMergingDataSource(data_sources);
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

		if (!waitForConfigure(processor)) {
		    return;
		}
		
		// We are done generating the internal processor.
		synchronized(dataLock) {
                    dataReady = true;
                    dataLock.notifyAll();
		}
	    } catch (Exception e){
	        System.err.println("NewReceiveStreamEvent exception " + e.getMessage());
		return;
	    }        
	}
    }
  
    public void setSource(javax.media.protocol.DataSource source)
        throws IOException, IncompatibleSourceException {

        super.setSource(source);
	
        if (source instanceof com.sun.media.protocol.rtsp.DataSource){
            MediaLocator ml = source.getLocator();

	    locators.addElement(ml);
        } else {
            throw new IncompatibleSourceException();
        }
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

    public void updateStats() {
        if ( processor != null) {
            ((BasicProcessor) processor).updateStats();
        }
    }

    /**
     */
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
