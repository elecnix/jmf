/*
 * @(#)StateHelper.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.util;

import javax.media.*;


/**
 * A utility class to perform synchronous state transition for
 * Players and Processors.
 */
public class StateHelper implements javax.media.ControllerListener {

    Player player = null;
    boolean configured = false;
    boolean realized = false;
    boolean prefetched = false;
    boolean eom = false;
    boolean failed = false;
    boolean closed = false;
    
    public StateHelper(Player p) {
	player = p;
	p.addControllerListener(this);
    }


    public boolean configure() {
	return configure(Integer.MAX_VALUE);
    }


    /**
     * Configure the player.  This method will block until
     * either the player has been configured or the configure has failed.
     */
    public boolean configure(int timeOutMillis) {
	long startTime = System.currentTimeMillis();
	synchronized (this) {
	    if (player instanceof Processor)
		((Processor)player).configure();
	    else
		return false;
	    while (!configured && !failed) {
		try {
		    wait(timeOutMillis);
		} catch (InterruptedException ie) {
		}
		if (System.currentTimeMillis() - startTime > timeOutMillis)
		    break;
	    }
	}
	return configured;
    }

    public boolean realize() {
	return realize(Integer.MAX_VALUE);
    }

    /**
     * Realize the player.  This method will block until
     * either the player has been realized or the realize has failed.
     */
    public boolean realize(int timeOutMillis) {
	long startTime = System.currentTimeMillis();
	synchronized (this) {
	    player.realize();
	    while (!realized && !failed) {
		try {
		    wait(timeOutMillis);
		} catch (InterruptedException ie) {
		}
		if (System.currentTimeMillis() - startTime > timeOutMillis)
		    break;
	    }
	}
	return realized;
    }


    /**
     * Prefetch the player.  This method will block until
     * either the player has been configured or the configure has failed.
     */
    public boolean prefetch(int timeOutMillis) {
	long startTime = System.currentTimeMillis();
	synchronized (this) {
	    player.prefetch();
	    while (!prefetched && !failed) {
		try {
		    wait(timeOutMillis);
		} catch (InterruptedException ie) {
		}
		if (System.currentTimeMillis() - startTime > timeOutMillis)
		    break;
	    }
	}
	return prefetched && !failed;
    }


    /**
     * Start the player and play till the end of the media.  This 
     * method will block until either the player has finished playing
     * the media or the playback has failed at some point.
     */
    public boolean playToEndOfMedia(int timeOutMillis) {
	long startTime = System.currentTimeMillis();
	eom = false;
	synchronized (this) {
	    player.start();
	    while (!eom && !failed) {
		try {
		    wait(timeOutMillis);
		} catch (InterruptedException ie) {
		}
		if (System.currentTimeMillis() - startTime > timeOutMillis)
		    break;
	    }
	}
	return eom && !failed;
    }


    /**
     * Close the player.
     */
    public void close() {
	synchronized (this) {
	    player.close();
	    while (!closed) {
		try {
		    wait(100);
		} catch (InterruptedException ie) {
		}
	    }
	}
	player.removeControllerListener(this);
    }


    public synchronized void controllerUpdate(ControllerEvent ce) {
	if (ce instanceof RealizeCompleteEvent) {
	    realized = true;
	} else if (ce instanceof ConfigureCompleteEvent) {
	    configured = true;
	} else if (ce instanceof PrefetchCompleteEvent) {
	    prefetched = true;
	} else if (ce instanceof EndOfMediaEvent) {
	    eom = true;
	} else if (ce instanceof ControllerErrorEvent) {
	    failed = true;
	} else if (ce instanceof ControllerClosedEvent) {
	    closed = true;
	} else {
	    return;
	}
	notifyAll();
    }
}
