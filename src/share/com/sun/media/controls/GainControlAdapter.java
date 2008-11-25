/*
 * @(#)GainControlAdapter.java	1.10 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.controls;

import javax.media.*;
import java.util.*;
import com.sun.media.ui.*;
import java.awt.*;

public class GainControlAdapter implements GainControl {

    private Vector listeners = null;
    private boolean muteState;
    private Component component;

    // Default level is whatever that maps to 0db.
    // If we set it to 1.0f, then the gain won't go pass
    // 0db.
    private float DefLevel = 0.4f;
    private float dB = 0f;
    private float level = DefLevel;

    public GainControlAdapter() {
    }

    public GainControlAdapter(float defLevel) {
	DefLevel = defLevel;
	level = defLevel;
    }

    public GainControlAdapter(boolean mute) {
	this.muteState = mute;
	setLevel(DefLevel);
    }

    public void setMute(boolean mute) {
	if (muteState != mute) {
	    muteState = mute;
	    informListeners();
	}
    }

    public boolean getMute() {
	return muteState;
    }

    public float setDB(float gain) {
	if (dB != gain) {
	    dB = gain;
	    float mult = (float) Math.pow(10.0, dB/20.0);
	    level = mult * DefLevel;
	    if (level < 0.0)
		setLevel(0.0f);
	    else if (level > 1.0)
		setLevel(1.0f);
	    else {  
		setLevel(level);
		informListeners();
	    }
	}
	return dB;
    }

    public float getDB() {
	return dB;
    }

    public float setLevel(float level) {
	if (level < 0.0)
	    level = 0.0f;
	if (level > 1.0)
	    level = 1.0f;
	if (this.level != level) {
	    this.level = level;
	    float mult = level/DefLevel;
	    dB = (float) (Math.log((double)((mult==0.0)?0.0001:mult))/Math.log(10.0) * 20.0);
	    informListeners();
	}
	return this.level;
    }

    public float getLevel() {
	return level;
    }

    public synchronized void addGainChangeListener(GainChangeListener listener) {
	if (listener != null) {
	    if (listeners == null)
		listeners = new Vector();
	    listeners.addElement(listener);
	}
    }

    public synchronized void removeGainChangeListener(GainChangeListener listener) {
	if (listener != null && listeners != null)
	    listeners.removeElement(listener);
    }

    public java.awt.Component getControlComponent() {
	return null;
    }

    protected synchronized void informListeners() {
	if (listeners != null) {
	    GainChangeEvent gce = new GainChangeEvent(this, muteState, dB, level);
	    for (int i = 0; i < listeners.size(); i++) {
		GainChangeListener gcl = (GainChangeListener) listeners.elementAt(i);
		gcl.gainChange(gce);
	    }
	}
    }
}

