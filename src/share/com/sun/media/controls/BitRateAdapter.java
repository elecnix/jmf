/*
 * @(#)BitRateAdapter.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import javax.media.control.BitRateControl;
import com.sun.media.ui.TextComp;
import javax.media.*;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class BitRateAdapter implements BitRateControl, ActionListener {

    protected int value;
    protected int min;
    protected int max;
    protected boolean settable;
    protected TextComp textComp;

    public BitRateAdapter(int initialBitRate, int minBitRate, int maxBitRate,
			  boolean settable) {
	value = initialBitRate;
	min = minBitRate;
	max = maxBitRate;
	this.settable = settable;
    }

    public int getBitRate() {
	return value;
    }

    public int setBitRate(int newValue) {
	if (settable) {
	    if (newValue < min)
		newValue = min;
	    if (newValue > max)
		newValue = max;
//	    if (newValue != value) {
            value = newValue;
            if (textComp != null)
                textComp.setValue(Integer.toString(newValue));
//	    }
	    return value;
	} else
	    return -1;
    }

    public int getMinSupportedBitRate() {
	return min;
    }

    public int getMaxSupportedBitRate() {
	return max;
    }

    protected String getName() {
	return "Bit Rate";
    }

    public Component getControlComponent() {
	if (textComp == null) {
	    textComp = new TextComp(getName(), Integer.toString(value),
				    7, settable);
	    textComp.setActionListener(this);
	}
	return textComp;
    }

    public void actionPerformed(ActionEvent ae) {
	if (textComp != null) {
	    setBitRate(textComp.getIntValue());
	}
    }

}
