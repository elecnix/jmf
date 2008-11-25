/* 
 * @(#)Handler.java	1.6 02/08/21
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

package com.sun.media.content.application.x_jmx;

import java.io.*;
import java.awt.*;
import java.net.*;
import java.util.Vector;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.Format;
import javax.media.renderer.VisualContainer;

import com.sun.media.*;
import com.sun.media.util.*;
import com.sun.media.ui.*;
import com.sun.media.controls.*;


/**
 * MediaPlayer extends BasicPlayer and uses MediaEngine to play media.
 */

public class Handler extends BasicPlayer 
{
    Player players[] = null;
    Player master = null;
    boolean realized[] = null;
    Vector locators = new Vector();	// Media Locators.
    ControllerListener listener = new PlayerListener(this);

    boolean playersRealized = false;
    Object realizedSync = new Object();
    private boolean closed = false;
    private boolean audioEnabled = false;
    private boolean videoEnabled = false;


    public Handler() {
        framePositioning = true;
    }

    String sessionError = "Cannot create a Player for: ";

    protected boolean doRealize() {

        super.doRealize();

	MediaLocator ml = null;

        try {
            
	    players = new Player[locators.size()];
	    realized = new boolean[locators.size()];

	    for (int i = 0; i < locators.size(); i++) {
		ml = (MediaLocator)locators.elementAt(i);
		players[i] = Manager.createPlayer(ml);
		players[i].addControllerListener(listener);
		realized[i] = false;
		players[i].realize();
	    }

        } catch (Exception e) {
            Log.error(sessionError + ml);
	    processError = sessionError + ml;
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
	    processError = "Realize interrupted";
	    return false;
	}

	try {
	    master = players[0];
	    for (int i = 1; i < players.length; i++) {
		master.addController(players[i]);
	    }
	} catch (IncompatibleTimeBaseException e) {
	    processError = "AddController failed";
	    return false;
	}

	manageController(master);

        return true;
    }


    protected void completeRealize() {
	state = Realized;
	super.completeRealize();
    }


    protected void doStart() {
	super.doStart();
    }


    protected void doStop() {
	super.doStop();
    }


    protected void doDeallocate() {
	synchronized (realizedSync) {
	    realizedSync.notify();
	}
    }


    protected void doClose() {

	closed = true;

	synchronized (realizedSync) {
	    realizedSync.notify();
	}

	stop();

	super.doClose();
    }


    protected TimeBase getMasterTimeBase() {
	return master.getTimeBase();
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


    public void setSource(javax.media.protocol.DataSource source)
        throws IOException, IncompatibleSourceException
    {
	super.setSource(source);

	if (!(source instanceof PullDataSource))
	    throw new IncompatibleSourceException();

	PullSourceStream pss[] = ((PullDataSource)source).getStreams();

	if (pss.length != 1)
	    throw new IncompatibleSourceException();

	source.start();

	int len = (int)pss[0].getContentLength(); 

	if (len == SourceStream.LENGTH_UNKNOWN)
	    throw new IncompatibleSourceException();

	byte barray[] = new byte[len];
	String content;

	try {

	    len = pss[0].read(barray, 0, len);
	    content = new String(barray);

	} catch (Exception e) {
	    throw new IncompatibleSourceException();
	}

	char ch;
	int start = 0, idx;
	int size = content.length();
	String str;
	String relPath = null;

	ch = content.charAt(start);

	while (start < size) {

	    while (ch == ' ' || ch == '\n') {
		start++;
		if (start >= size)
		    break;
		ch = content.charAt(start);
	    }

	    if (start >= size)
		break;
	
	    idx = start;

	    do {
		idx++;
		if (idx >= size)
		    break;
		ch = content.charAt(idx);
	    } while (ch != '\n');

	    str = content.substring(start, idx);

	    if (str.indexOf(':') == -1) {

	        // Probably didn't specify a protocol or
		// absolute path here.
		// We'll assume it's relative path and use
		// the first part of the media locator to
		// generate the name.

		if (relPath == null) {
		    MediaLocator loc = source.getLocator();
		    if (loc == null)
			throw new IncompatibleSourceException();
		    relPath = loc.toString();
		    int i = relPath.lastIndexOf('/');
		    if (i < 0)
			i = relPath.lastIndexOf(File.separator);
		    relPath = relPath.substring(0, i + 1);
		}
		str = relPath + str;
	    }

	    locators.addElement(new MediaLocator(str));
	    start = idx;
	}

	if (locators.size() < 1) 
	    throw new IncompatibleSourceException();
    }


    private void invalidateComp(){
	controlComp = null;
	controls = null;
    }


    private Container container = null;
    
    public Component getVisualComponent() {
	Vector visuals = new Vector(1);

	for( int i = 0; i < players.length; i++) {
	    Component comp= players[i].getVisualComponent();

	    if( comp != null) {
		visuals.addElement( comp);
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

    
    public void updateStats() {
	for (int i = 0; i < players.length; i++) {
	    if (players[i] != null)
		((BasicPlayer)players[i]).updateStats();
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
	    Log.error("Meta Handler internal error: " + ce);
	    players[idx] = null;
	}

      }// end of controllerUpdate

    } // end of PlayerListener;

}// end of Handler 

