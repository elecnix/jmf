/*
 * @(#)MediaProcessor.java	1.15 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.io.*;
import java.awt.*;
import javax.media.*;
import javax.media.format.*;
import javax.media.control.*;
import javax.media.protocol.*;

/**
 * MediaProcessor extends BasicProcessor and uses ProcessEngine to process
 * media.
 */

public class MediaProcessor extends BasicProcessor {

    protected ProcessEngine engine;

    public MediaProcessor() {
	engine = new ProcessEngine(this);
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
	super.getGainControl();		// check for valid states.
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

    /**
     * Return the tracks in the media. This method can only be called after 
     * the Processor has been configured. 
     */
    public TrackControl[] getTrackControls() throws NotConfiguredError {
	return engine.getTrackControls();
    }

    /** 
     * Return all the content-types which this Processor's output supports. 
     */
    public ContentDescriptor[] getSupportedContentDescriptors() 
	throws NotConfiguredError {
	return engine.getSupportedContentDescriptors();
    }
    
    /**
     * Set the output content-type. 
     */
    public ContentDescriptor setContentDescriptor(ContentDescriptor ocd) 
	throws NotConfiguredError {
	return engine.setContentDescriptor(ocd);
    }
    
    /**
     * Return the output content-type.
     */
    public ContentDescriptor getContentDescriptor() 
	throws NotConfiguredError {
	return engine.getContentDescriptor();
    }
    
    /** 
     * Return the output DataSource of the Processor. 
     */
    public DataSource getDataOutput() throws NotRealizedError {
	return engine.getDataOutput();
    }
    
    public void updateStats() {
	engine.updateRates();
    }
}

