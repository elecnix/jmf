/*
 * @(#)KeyFrameAdapter.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import javax.media.control.KeyFrameControl;
import com.sun.media.ui.TextComp;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class KeyFrameAdapter implements KeyFrameControl, ActionListener {

    int preferred;
    int value;
    boolean settable;
    TextComp textComp = null;

    public KeyFrameAdapter(int preferredInterval, boolean settable) {
	this.preferred = preferredInterval;
	this.settable = settable;
	this.value = preferred;
    }

    public int getKeyFrameInterval() {
	return value;
    }

    public int setKeyFrameInterval(int newValue) {

	if (settable) {
	    if (newValue < 1)
		newValue = 1;
//	    if (newValue != value) {
		value = newValue;
		if (textComp != null) {
		    textComp.setValue(Integer.toString(value));
		}
//	    }
	    return value;
	} else
	    return -1;
    }

    public int getPreferredKeyFrameInterval() {
	return preferred;
    }

    protected String getName() {
	return "Key Frames Every";
    }

    public Component getControlComponent() {
	if (textComp == null) {
	    textComp = new TextComp(getName(),
				    Integer.toString(value), 3,
				    settable);
	    textComp.setActionListener(this);

	}
	return textComp;
    }

    public void actionPerformed(ActionEvent ae) {
	int newValue = textComp.getIntValue();
	setKeyFrameInterval(newValue);
    }
}

