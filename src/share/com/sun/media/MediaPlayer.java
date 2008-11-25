
/*
 * @(#)MediaPlayer.java	1.13 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.io.*;
import java.awt.*;
import javax.media.*;


/**
 * MediaPlayer extends BasicPlayer and uses PlaybackEngine to play media.
 */

public class MediaPlayer extends BasicPlayer {

    protected PlaybackEngine engine;

    public MediaPlayer() {
	engine = new PlaybackEngine(this);
    }

    public void setSource(javax.media.protocol.DataSource source)
	throws IOException, IncompatibleSourceException {
	// Ask the engine to verify the source.
	engine.setSource(source);

	// Put the media engine under the management of this player.
	// BasicPlayer will be responsible to transition the engine
	// to the realized state.
	manageController(engine);

	super.setSource(source);
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
	return engine.getVisualComponent();
    }

    /**
     * Obtain the gain control from the media engine.
     */
    public GainControl getGainControl() {
	int state = getState();
	if (state < Realized) {
	    throwError(new NotRealizedError("Cannot get gain control on an unrealized player"));
	} 
	return engine.getGainControl();
    }

    /**
     * Obtain media time directly from the engine.
     */
    public Time getMediaTime() {
	// When add controller is used, we want to use the
	// less accurate clock but still allows the time base
	// to take consideration of the slave controllers.
	// Otherwise, we'll use the more accurate engine time.
	if (controllerList.size() > 1)
	    return super.getMediaTime();
	else
	    return engine.getMediaTime();
    }

    public long getMediaNanoseconds() {
	// When add controller is used, we want to use the
	// less accurate clock but still allows the time base
	// to take consideration of the slave controllers.
	// Otherwise, we'll use the more accurate engine time.
	if (controllerList.size() > 1)
	    return super.getMediaNanoseconds();
	else
	    return engine.getMediaNanoseconds();
    }

    /**
     * Obtain the time base from the media engine.
     */
    protected TimeBase getMasterTimeBase() {
	return engine.getTimeBase();
    }

    protected boolean audioEnabled() {
	return engine.audioEnabled();
    }

    protected boolean videoEnabled() {
	return engine.videoEnabled();
    }

    public void updateStats() {
	engine.updateRates();
    }
    
    public void setProgressControl(com.sun.media.controls.ProgressControl p){
	engine.setProgressControl(p);
    }
}
