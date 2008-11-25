/*
 * @(#)Handler.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.content.video.mpeg;


import java.io.*;
import java.awt.*;
import java.util.*;
import java.net.*;
import javax.media.*;
import javax.media.protocol.*;
import com.sun.media.*;
import com.sun.media.codec.video.jmpx.Jmpx;
import com.sun.media.ui.*;
import com.sun.media.controls.*;


/**
 * A MPEG player implementation using mpx.
 */
public class Handler extends BasicPlayer {

    private final static int ZoomedIn = 1;
    private final static int ZoomedOut = -1;
    private boolean reloaded = true;
    private int type;
    protected Jmpx jmpx;
    boolean streaming = false;

    public Handler() {
	this(Jmpx.MpxStrm_Unknown);
    }

    public Handler(int type) {
	this.type = type;
	jmpx = new Jmpx(type);
	manageController(jmpx);
    }

    public void doClose() {
	super.doClose();
	jmpx = null;
    }

    public void setSource(javax.media.protocol.DataSource source)
	throws IOException, IncompatibleSourceException {

	super.setSource(source);
	jmpx.setSource(source);
    }

    protected TimeBase getMasterTimeBase() {
	return jmpx.getTimeBase();
    }

    public Component getVisualComponent() {
	super.getVisualComponent();
	return jmpx.getVisualComponent();
    }

    public void updateStats() {
    }

    protected boolean audioEnabled() {
	if (jmpx.getStreamType() == Jmpx.MpxStrm_VSEQ)
	    return false;
	return true;
    }

    protected boolean videoEnabled() {
	if (jmpx.getStreamType() == Jmpx.MpxStrm_ASEQ)
	    return false;
	return true;
    }

    public void muteChange(boolean m) {
	jmpx.setMute(m);
    }

    public void gainChange(GainChangeEvent gce) {
	jmpx.setGain(gce.getLevel());
    }

    public void mute(boolean m) {
	if (jmpx.isInitialized())
	    jmpx.mute(m);
    }

    public boolean isMuted() {
	return jmpx.isMuted();
    }

    /**
     * Set the audio mode.
     */
    public void setAudioMode(int m) {
	if (jmpx.isInitialized())
	    jmpx.setAudioMode(m);
    }

    /**
     * Set the audio quality.
     */
    public void setAudioQuality(int q) {
	if (jmpx.isInitialized())
	    jmpx.setAudioQuality(q);
    }

    /**
     * Events from mpx are handled here.
     */
    private static int sizeChangeWidth = -1;

    public void processEvent(ControllerEvent evt) {
	super.processEvent(evt);
    }

}


