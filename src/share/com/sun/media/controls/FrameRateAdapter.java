/*
 * @(#)FrameRateAdapter.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import javax.media.control.FrameRateControl;
import javax.media.Owned;
import com.sun.media.Reparentable;
import com.sun.media.ui.TextComp;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class FrameRateAdapter implements FrameRateControl, ActionListener, Reparentable {

    protected float value = 0.0f;
    protected float min   = 0f;
    protected float max   = 0f;
    protected TextComp textComp = null;
    protected boolean settable;
    protected Object owner = null;

    public FrameRateAdapter(float initialFrameRate, float minFrameRate,
			    float maxFrameRate, boolean settable) {
	value = initialFrameRate;
	min = minFrameRate;
	max = maxFrameRate;
	this.settable = settable;
    }

    public FrameRateAdapter(Object owner, float initialFrameRate, float minFrameRate,
			    float maxFrameRate, boolean settable) {
	this(initialFrameRate, minFrameRate, maxFrameRate, settable);
	this.owner = owner;
    }

    /**
     * Returns the current frame rate. Returns -1 if it is unknown.
     * @return the frame rate in frames per second.
     */
    public float getFrameRate() {
	return value;
    }

    /**
     * Sets the frame rate.
     * Returns -1 if it is unknown or it is not controllable.
     * @param newFrameRate the requested new frame rate
     * @return the actual frame rate in frames per second.
     */
    public float setFrameRate(float newFrameRate) {
	if (settable) {
	    if (newFrameRate < min)
		newFrameRate = min;
	    else if (newFrameRate > max)
		newFrameRate = max;
//	    if (newFrameRate != value) {
		value = newFrameRate;
		if (textComp != null) {
		    textComp.setValue(Float.toString(value));
		}
//	    }
	    return value;
	} else
	    return -1;
    }

    /**
     * Returns the maximum frame rate. Returns -1 if it is unknown.
     * @return the maximum frame rate in frames per second.
     */
    public float getMaxSupportedFrameRate() {
	return max;
    }

    /**
     * Returns the default frame rate. Returns -1 if it is unknown.
     * @return the default frame rate in frames per second.
     */
    public float getPreferredFrameRate() {
	return min;
    }


    protected String getName() {
	return "Frame Rate";
    }

    public void setEnabled(boolean enable) {
	if (textComp != null)
	    textComp.setEnabled(enable);
    }

    public Component getControlComponent() {
	if (textComp == null) {
	    textComp = new TextComp(getName(), value+"", 2, settable);
	    textComp.setActionListener(this);
	}
	return textComp;
    }

    public void actionPerformed(ActionEvent ae) {
    System.out.println("fra:");
	float newFrameRate = textComp.getFloatValue();
	setFrameRate(newFrameRate);
    }

    public Object getOwner() {
	if (owner == null)
	    return this;
	else
	    return owner;
    }

    public void setOwner(Object newOwner) {
	owner = newOwner;
    }
}


