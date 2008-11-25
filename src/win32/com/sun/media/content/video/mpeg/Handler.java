/*
 * @(#)Handler.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.content.video.mpeg;

import javax.media.*;
import javax.media.protocol.*;
import java.io.*;
import java.awt.*;
import java.util.*;
import java.net.*;
import com.sun.media.*;
import com.sun.media.util.*;
import com.sun.media.ui.*;
import com.sun.media.amovie.*;
import com.sun.media.controls.*;

/**
 * A MPEG player implementation for windows that runs on top of Active Movie.
 */

public class Handler extends BasicPlayer {

    /*************************************************************************
     * Variables
     *************************************************************************/
    
    private DefaultControlPanel controlPanel = null;

    protected AMController amController = null;

    private boolean justDeallocated = false;

    //private TimeBase masterTimeBase = mixerTimeBase;

    /*************************************************************************
     * Methods
     *************************************************************************/
    
    public Handler() {
    }

    public void setSource(DataSource source)
	throws IOException, IncompatibleSourceException {
	//if (BuildInfo.usePureJava()) {
	//  throw new IncompatibleSourceException(this + " : does not support mpeg on this platform.");
	//}
	super.setSource(source);
	try {
	    if (amController == null) {
		amController = new AMController(this);
		amController.setSource(source);
		manageController(amController);
	    }
	} catch (Exception e) {
	    throw new IncompatibleSourceException(e.getMessage());
	}
    }

    protected TimeBase getMasterTimeBase() {
	return amController.getTimeBase();
    }
    
    public void updateStats() {
    }
    
    /**
     * Get the Component this player will output its visual media to.  If
     * this player has no visual component (e.g. audio only)
     * getVisualComponent() will return null.
     *
     * @return the media display component.
    */
    public Component getVisualComponent() {
	if (state == Unrealized || state == Realizing)
	    throw new javax.media.NotRealizedError("Cannot get visual component from an unrealized player.");
	return amController.getVisualComponent();
    }


    /**
     * Get the object for controlling audio gain. Returns null
     * if this player does not have a GainControl (e.g. no audio).
     *
     * @return the GainControl object for this player.
    */
    public GainControl getGainControl() {
	return super.getGainControl();
    }

    /**
     * Get the Component with the default user interface for controlling
     * this player.
     * If this player has no default control panel null is
     * returned.
     *
     * @return the default control panel GUI.
     */
    public Component getControlPanelComponent() {
	return super.getControlPanelComponent();
    }

    protected boolean connectNodes() {
	//addControls(amController.getControls());
	return true;
    }

    protected void createGainControl() {
	// called by MediaPlayer.doRealize()
    }

    protected void doDeallocate() {
	super.doDeallocate();
	justDeallocated = true;
    }

    protected synchronized boolean doRealize() {
	boolean returnVal = super.doRealize();

	if (!returnVal)
	    return false;
	/*
	if (audioEnabled()) {
	    gainControl = new GainCA(false);
	    gainControl.addGainChangeListener( this );
	}
	// set rate control to null if player plays only AUDIO since
	// we dont support audio rate control  
	if ((rateControl != null) && (audioEnabled()) && (!videoEnabled()))
	    rateControl = null;
	
	if (regionControl != null)
	    regionControl.setEnable(false);
	Time dur = amController.getDuration();
	if (dur != null)
	    setMediaLength(dur.getNanoseconds());
	*/
	return true;
    }

    protected boolean doPrefetch() {
	if (justDeallocated) {
	    setMediaTime(new Time(0));
	    justDeallocated = false;
	}
	return super.doPrefetch();
    }
    
    public void doClose() {
	/*
	if (controlPanel != null)
	    controlPanel.dispose();
	controlPanel = null;
	*/
	super.doClose();
	amController = null;
    }

    public boolean audioEnabled() {
	return amController.audioEnabled();
    }

    public boolean videoEnabled() {
	return amController.videoEnabled();
    }
    
    /*
    public void gainChange(GainChangeEvent gce) {
       	amController.gainChange(gce);
    }

    public void muteChange(boolean state) {
	amController.muteChange(state);
    }

    public StringControl getFrameRate() {
	return frameRate;			      // from MediaPlayer
    }

    public StringControl getBitRate() {
	return bitRate;				      // from MediaPlayer
    }

    public StringControl getAudioProps() {
	return audioProps;			      // from MediaPlayer
    }

    public StringControl getVideoProps() {
	return videoProps;			      // from MediaPlayer
    }

    public StringControl getAudioCodec() {
	return audioCodec;			      // from MediaPlayer
    }

    public StringControl getVideoCodec() {
	return videoCodec;			      // from MediaPlayer
    }
    */
    /*************************************************************************
     * INNER CLASSES
     *************************************************************************/

    /*
    class GainCA extends GainControlAdapter {

	public GainCA(boolean mute) {
	    super(mute);
	}

	public void setMute(boolean m) {
	    muteChange(m);
	    super.setMute(m);
	}
    }
    */
}
