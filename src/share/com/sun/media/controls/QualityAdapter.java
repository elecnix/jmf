/*
 * @(#)QualityAdapter.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import javax.media.control.QualityControl;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import com.sun.media.ui.SliderComp;

public class QualityAdapter implements QualityControl, ActionListener {

    protected float preferredValue;
    protected float minValue;
    protected float maxValue;
    protected float value;
    protected boolean settable;
    protected boolean isTSsupported;
    protected SliderComp sliderComp = null;
    private   float scale = 100f;

    public QualityAdapter(float preferred, float min, float max,
			  boolean settable) {
        this(preferred,min,max,false,settable);
    }

    public QualityAdapter(float preferred, float min, float max, boolean isTSsupported,
			  boolean settable) {
	preferredValue = preferred;
	minValue = min;
	maxValue = max;
	value = preferred;
	this.settable = settable;
	this.isTSsupported = isTSsupported;
    }

    public float getQuality() {
	return value;
    }

    public float setQuality(float newValue) {
	if (newValue < minValue)
	    newValue = minValue;
	else if (newValue > maxValue)
	    newValue = maxValue;

//	if (newValue != value) {
	    value = newValue;
	    if (sliderComp != null) {
		sliderComp.setValue(value * scale);
	    }
//	}

	if (settable)
	    return value;
	else
	    return -1;
    }

    public float getPreferredQuality() {
	return preferredValue;
    }

    public boolean isTemporalSpatialTradeoffSupported() {
	return isTSsupported;
    }

    protected String getName() {
	return "Quality";
    }

    public Component getControlComponent() {
	if (sliderComp == null) {

	    sliderComp = new SliderComp(getName(),
					minValue * scale,
					maxValue * scale,
					value * scale);
	    sliderComp.setActionListener(this);
	}
	return sliderComp;
    }

    public void actionPerformed(ActionEvent ae) {
	float newValue = sliderComp.getFloatValue() / scale;
	setQuality(newValue);
    }
}
