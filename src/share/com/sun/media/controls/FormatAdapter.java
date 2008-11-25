/*
 * @(#)FormatAdapter.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import javax.media.control.FormatControl;
import javax.media.*;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class FormatAdapter implements FormatControl, ActionListener {

    protected Format currentFormat;
    protected Format [] supportedFormats;
    protected boolean enabled;
    protected boolean formattable;
    protected boolean enableable;
    
    public FormatAdapter(Format format, Format [] supported,
			 boolean enabled,
			 boolean formattable,
			 boolean enableable) {
	this.currentFormat = format;
	this.supportedFormats = supported;
	this.enabled = enabled;
	this.formattable = formattable;
	this.enableable = enableable;
    }

    public Format getFormat() {
	return currentFormat;
    }

    public Format setFormat(Format newFormat) {
	if (formattable) {
	    currentFormat = newFormat;
	}
	return currentFormat;
    }

    public Format [] getSupportedFormats() {
	return supportedFormats;
    }

    public boolean isEnabled() {
	return enabled;
    }
    
    public void setEnabled(boolean newEnable) {
	if (enableable)
	    this.enabled = newEnable;
    }

    protected String getName() {
	return "Format";
    }

    public Component getControlComponent() {
	// TODO
	return null;
    }

    public void actionPerformed(ActionEvent ae) {
	// TODO
    }
}
