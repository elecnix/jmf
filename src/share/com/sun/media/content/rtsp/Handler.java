/*
 * @(#)Handler.java	1.74 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.content.rtsp;


import java.io.*;
import java.awt.*;
import java.net.*;
import java.util.Vector;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.rtp.rtcp.*;
import javax.media.control.BufferControl;
import javax.media.renderer.*;

import com.sun.media.*;
import com.sun.media.controls.RtspAdapter;
import com.sun.media.protocol.DelegateDataSource;
import com.sun.media.rtsp.*;

import com.sun.media.protocol.rtp.DataSource;
import com.sun.media.protocol.BufferListener;

/**
 * MediaPlayer extends BasicPlayer and uses MediaEngine to play media.
 */

public class Handler extends BasicPlayer implements ReceiveStreamListener,
    TimerListener, BufferListener {

    // states:
    private final int INITIALIZED = 0;
    private final int REALIZED = 1;
    private final int PLAYING = 2;
    private final int PAUSING = 3;

    private RtspUtil rtspUtil;
    private Player players[];
    private Vector playerList;
    private boolean dataReceived;
    private DataSource data_sources[];
    private boolean track_ready[];
    private String url;

    private Object readySync = new Object();

    // synchronization variable used in
    // waitForState().
    private Object stateSync = new Object();

    // used to wait for a state until the state is
    // reached or something failed. In this case
    // the wait loop will be aborted.
    private boolean waitFailed;
    
    private int state;

    private boolean first_pass = true;
    
    public Handler() {
	rtspUtil = new RtspUtil( this);

        framePositioning = false;

        playerList = new Vector();

        state = INITIALIZED;

	stopThreadEnabled = true;
    }

    protected synchronized boolean doRealize() {
        boolean realized = super.doRealize();

        if (realized) {
            realized = initRtspSession();

	    if( !realized) {
		processError= rtspUtil.getProcessError();
	    } else {
		long duration= rtspUtil.getDuration();

                if( duration > 0) {		
		    sendEvent(new DurationUpdateEvent(this, new Time(duration)));
		}
	    }
        }

        return realized;
    }

    private boolean initRtspSession() {
        boolean realized = false;

	rtspUtil.setUrl( url);

        String ipAddress = rtspUtil.getServerIpAddress();
	
        if (ipAddress == null) {
            Log.error( "Invalid server address");
	    
	    rtspUtil.setProcessError( "Invalid server address");

            realized = false;
        } else {
	    realized = rtspUtil.createConnection();

	    if( realized) {
                realized = rtspUtil.rtspSetup();

                try {
                    InetAddress destaddr = InetAddress.getByName(ipAddress);

		    int numberOfTracks= rtspUtil.getNumberOfTracks();
		    int server_ports[]= rtspUtil.getServerPorts();
		    
		    for( int i = 0; i < numberOfTracks; i++) {
                        SessionAddress remoteAddress =
                                new SessionAddress(destaddr, server_ports[ i]);

			RTPManager mgr= rtspUtil.getRTPManager( i);
			
                        mgr.addTarget( remoteAddress);

			// Set 3/4 sec worth of buffering.
			BufferControl bc = (BufferControl)mgr.getControl("javax.media.control.BufferControl");

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
	    
	    players= new Player[ size];	    
	    data_sources = new DataSource[ size];
    	    track_ready = new boolean[ size];
	
	    dataReceived= false;

	    // Start the server.
            if (!rtspUtil.rtspStart()) {
		if( first_pass &&
		    rtspUtil.getStatusCode() ==
		    com.sun.media.rtsp.protocol.StatusCode.SESSION_NOT_FOUND) {

		    first_pass= false;
		    
		    playerList= new Vector();

		    return initRtspSession();
		}
		
		return false;
	    }

	    // Wait for the initial filling of the data buffers and
	    // the RTP players to fully realized.
            waitForData();

	    if( playerList.size() > 0) {
	        // Now the players are all started.  
	        // Stop the server momentarily.  Rewind the media
	        // so the next start will start from the beginning again.
	        // This is sort of inefficient but will get by the initial
	        // buffering problem.
	        rtspStop();
	        rtspUtil.setStartPos( 0);

	        // Now we need to flush the datasources to make sure
	        // the rewind starts from the very beginning.

	        for( int i = 0; i < rtspUtil.getNumberOfTracks(); i++) {
		    data_sources[ i].flush();
	        }
	    } else {
		rtspUtil.setProcessError( "Media tracks not supported");
		realized= false;
	    }
        }

        return realized;
    }

    // This method should not be called with RTSP:
    public boolean doPrefetch() {
        boolean prefetched;

        prefetched = super.doPrefetch();

        return prefetched;
    }

    public void doStart() {
        if (state >= REALIZED && state != PLAYING) {

	    for( int i = 0; i < rtspUtil.getNumberOfTracks(); i++) {
		track_ready[ i] = (rtspUtil.getRTPManager( i) == null ? true : false);
		data_sources[ i].prebuffer();
	    }

            boolean success = rtspUtil.rtspStart();

	    // We are waiting for audio and video to be prebuffered
	    // before actually starting the playback.  We'll wait for
	    // at most 3 seconds.
	    synchronized (readySync) {
		boolean ready = true;
		
		for( int i = 0; i < rtspUtil.getNumberOfTracks(); i++) {
		    if( !track_ready[ i]) {
			ready = false;
			break;
		    }
		}
		
		if (!ready) {
		    try {
			readySync.wait(3000);
		    } catch (Exception e) { }
		}
	    }

            if (success) {
                super.doStart();

                startPlayers();

                state = PLAYING;

		// this timer controls the end of media event which is
		// required for looping.
		long duration= rtspUtil.getDuration();

                if( duration > 0) {		
                    timer = new Timer(this,
                            duration + 500000000 - getMediaTime().getNanoseconds());

                    timer.start();
		}
            }
        }
    }

    public void doSetMediaTime(Time now) {
	super.doSetMediaTime(now);

        rtspUtil.setStartPos( now.getNanoseconds());
	
	// Now we need to flush the datasources.
	for( int i = 0; i < rtspUtil.getNumberOfTracks(); i++) {
	    data_sources[ i].flush();
	}
    }

    private Timer timer;

    public Time getMediaTime() {
        Time time = super.getMediaTime();

        return time;
    }

    public void timerExpired() {
        timer = null;
	
        processEndOfMedia();
    }

    public void doStop() {
	if( state == PLAYING) {	    
            super.doStop();

            if (timer != null) {
                timer.stopTimer();
		timer.removeListener(this);

                timer = null;
            }

	    stopPlayers();

	    rtspStop();

            state = PAUSING;
	}
    }

    public void rtspStop() {
	rtspUtil.setStartPos( getMediaTime().getNanoseconds());
	rtspUtil.rtspStop();
    }
    
    public void doClose() {
	stopPlayers();

        closePlayers();
		
        if (timer != null) {
            timer.stopTimer();
	    timer.removeListener(this);	    

            timer = null;
        }

	if( state == PLAYING) {	    
            rtspUtil.rtspTeardown();
	}

	state = INITIALIZED;	

        rtspUtil.closeConnection();	

	for( int i = 0; i < rtspUtil.getNumberOfTracks(); i++) {
	    RTPManager mgr= rtspUtil.getRTPManager( i);
	    mgr.removeTargets( "server down.");
	    mgr.dispose();
	}

        super.doClose();
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

    public boolean audioEnabled() {
        boolean enabled = true;

        return enabled;
    }

    public boolean videoEnabled() {
        boolean enabled = true;

        return enabled;
    }

    public void updateStats() {

    }

    protected TimeBase getMasterTimeBase() {
        return new SystemTimeBase();
    }

    public synchronized void update(ReceiveStreamEvent event) {
        // find the sourceRTPSM for this event
        RTPManager source = (RTPManager) event.getSource();

        // create a new player if a new recvstream is detected
        if (event instanceof NewReceiveStreamEvent) {
            // get a handle over the ReceiveStream
            ReceiveStream stream = ((NewReceiveStreamEvent) event).getReceiveStream();

            Participant part = stream.getParticipant();

            int numberOfTracks= rtspUtil.getNumberOfTracks();
	       
	    for( int i = 0; i < numberOfTracks; i++) {
	        if( source == rtspUtil.getRTPManager( i)) {
	            DataSource ds = (com.sun.media.protocol.rtp.DataSource)stream.getDataSource();
		    
	            try {
	      	        players[ i] = javax.media.Manager.createPlayer(ds);
   		    } catch (Exception e) {
		        System.err.println("Failed to create a player " +
                                           "from the given Data Source: " + e);
		    }
		    
		    try {
                        waitFailed= false;

                        players[ i].addControllerListener(new StateListener());			
 		        players[ i].realize();

		        waitForState(players[ i], Player.Realized);
		    } catch( Exception e) {
		        // System.out.println( failed to realize.");
		    }

	            if( players[ i].getState() == Player.Realized) {
                        playerList.addElement(players[ i]);
						
       	                ds.setBufferListener(this);

		        data_sources[ i]= ds;
		    } else {
			players[ i].close();			
	      	        players[ i]= null;
			
		        rtspUtil.removeTrack( i);
		    }

	            break;
	        }		
	    }

	    // Notify when all the players are realized.
            if (playerList.size() == rtspUtil.getNumberOfTracks()) {
                dataReceived = true;

		synchronized (this) {
                    notifyAll();
                }
            }
        } else if (event instanceof ByeEvent) {
        }
    }

    public void minThresholdReached(javax.media.protocol.DataSource ds) {
	synchronized (readySync) {
	    for( int i = 0; i < rtspUtil.getNumberOfTracks(); i++) {
	        if (ds == data_sources[ i]) {		    
		    track_ready[ i] = true;
		    break;
		}
	    }

	    boolean all_ready = true;
	    
	    for( int i = 0; i < rtspUtil.getNumberOfTracks(); i++) {
	        if (!track_ready[ i]) {
		    all_ready = false;
		    break;
		}
	    }

	    if( all_ready) {
		readySync.notifyAll();
	    }
	}
    }

    public long getMediaNanoseconds() {
        long value= super.getMediaNanoseconds();

        return value;
    }

    public Time getDuration() {

	long t = rtspUtil.getDuration();

	if (t <= 0)
	    return Duration.DURATION_UNKNOWN;

        return new Time(t);
    }
    
    private synchronized void waitForState(Player p, int state) {       			
        while (p.getState() < state && !waitFailed) {
            synchronized (stateSync) {
                try {
                    stateSync.wait();
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    class StateListener implements ControllerListener {
        public void controllerUpdate(ControllerEvent ce) {
            if (ce instanceof ControllerClosedEvent) {
            }

            if (ce instanceof ResourceUnavailableEvent) {
                waitFailed = true;

                synchronized (stateSync) {
                    stateSync.notify();
                }
            }

            if (ce instanceof RealizeCompleteEvent) {
                synchronized (stateSync) {
                    stateSync.notify();
                }				
            }

            if (ce instanceof ControllerEvent) {
                synchronized (stateSync) {
                    stateSync.notify();
                }
            }

            if (ce instanceof EndOfMediaEvent) {
            }
        }
    }

    private synchronized boolean waitForData() {                
        try {
            synchronized (this) {
                while (!dataReceived) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return dataReceived;
    }

    private Container container = null;
    
    public Component getVisualComponent() {
	Vector visuals = new Vector(1);

	for( int i = 0; i < rtspUtil.getNumberOfTracks(); i++) {
	    if( players[ i] != null) {
	        Component comp= players[ i].getVisualComponent();

	        if( comp != null) {
		    visuals.addElement( comp);
	        }
	    }
	}

	if( visuals.size() == 0) {
	    return null;
	} else if( visuals.size() == 1) {
	    return (Component) visuals.elementAt( 0);
	} else {
	    return createVisualContainer( visuals);
	}
    }

    protected Component createVisualContainer(Vector visuals) {
	Boolean hint = (Boolean) Manager.getHint(Manager.LIGHTWEIGHT_RENDERER);
	
	if (container == null) {
	    if (hint == null || hint.booleanValue() == false) {
		container = new HeavyPanel(visuals);
	    } else {
		container = new LightPanel(visuals);
	    }
	
	    container.setLayout( new FlowLayout() );
	    container.setBackground(Color.black);
	    
	    for (int i = 0; i < visuals.size(); i++) {
		Component c = (Component)visuals.elementAt(i);
		container.add(c);
		c.setSize(c.getPreferredSize());
	    }
	}
	
	return container;
    }
    
    class HeavyPanel extends java.awt.Panel implements VisualContainer {
	public HeavyPanel(Vector visuals) {
	}
    }

    class LightPanel extends java.awt.Container implements VisualContainer {
	public LightPanel(Vector visuals) {
	}
    }
    
    public GainControl getGainControl() {
        GainControl gainControl = null;

        for (int i = 0; i < playerList.size(); i++) {
            Player player = (Player) playerList.elementAt(i);

            gainControl = player.getGainControl();

            if (gainControl != null) {
                break;
            }
        }

        return gainControl;
    }

    public Control[] getControls() {
        int size = 0;
	
        for (int i = 0; i < playerList.size(); i++) {
            Control[] controls = ((Player)(playerList.elementAt(i))).getControls();

            size += controls.length;
        }

	size++;
	
        Control rtspControls[] = new Control[size];

	RtspAdapter rtspAdapter= new RtspAdapter();

	rtspAdapter.setRTPManagers( rtspUtil.getRTPManagers());
	rtspAdapter.setMediaTypes( rtspUtil.getMediaTypes());
	
        int counter = 0;

       	rtspControls[ counter++] = rtspAdapter;
	
        for (int i = 0; i < playerList.size(); i++) {
            Control[] controls = ((Player)(playerList.elementAt(i))).getControls();

            for (int k = 0; k < controls.length; k++) {
                rtspControls[counter++] = controls[k];
            }
        }

        return rtspControls;
    }

    private void startPlayers() {
        for (int i = 0; i < playerList.size(); i++) {
            Player player = (Player) playerList.elementAt(i);

            player.start();
        }
    }

    private void stopPlayers() {
        for (int i = 0; i < playerList.size(); i++) {
            Player player = (Player) playerList.elementAt(i);

            player.stop();
        }
    }

    private void closePlayers() {
        for (int i = 0; i < playerList.size(); i++) {
            Player player = (Player) playerList.elementAt(i);

            player.close();
        }
    }

    public void setSource(javax.media.protocol.DataSource source)
            throws IOException, IncompatibleSourceException {

        if (source instanceof com.sun.media.protocol.rtsp.DataSource) {
            MediaLocator ml = source.getLocator();

            try {
                url = ml.toString();
            } catch (Exception e) {
                throw new IncompatibleSourceException();
            }
        } else {
            throw new IncompatibleSourceException();
        }
    }
}
