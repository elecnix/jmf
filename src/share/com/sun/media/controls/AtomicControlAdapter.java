/*
 * @(#)AtomicControlAdapter.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import javax.media.*;
import com.sun.media.*;
import java.util.Vector;
import com.sun.media.controls.*;
import java.awt.*;

/**
 * An AtomicControl is one that can be treated as an individual control
 * and can have its own behaviour. It is a base class for other controls.
 */
public class AtomicControlAdapter implements AtomicControl {


    /*************************************************************************
     * VARIABLES
     *************************************************************************/
    
    protected Component component = null;
    private Vector listeners = null;
    protected boolean isdefault = false;
    protected Control parent = null;
    protected boolean enabled = true;

    /*************************************************************************
     * METHODS
     *************************************************************************/

    public AtomicControlAdapter(Component c, boolean def, Control parent) {
	component = c;
	isdefault = def;
	this.parent = parent;
    }
    
    /*************************************************************************
     * IMPLEMENTATION FOR AtomicControl
     *************************************************************************/
    
    /**
     * Returns true if this control is available on the default control
     * panel returned for the player in question.
     */
    public boolean isDefault() {
	return isdefault;
    }

    /**
     * Specify whether this control should be available on the control panel.
     * ???
     */
    public void setVisible(boolean visible) {
	// dummy
    }

    /**
     * Returns true if this control is available on the control panel. ???
     */
    public boolean getVisible() {
	return true;
    }

    /**
     * Set the enabled/disabled state of the control. Can be useful to
     * temporarily gray out a control due to some constraints.
     */
    public void setEnabled(boolean enabled) {
	this.enabled = enabled;
	if (component != null)
	    component.setEnabled(enabled);
	informListeners();
    }

    /**
     * Returns the enabled/disabled state of the control.
     */
    public boolean getEnabled() {
	return enabled;
    }

    public void setParent(Control p) {
	parent = p;
    }

    /**
     * Returns the control group to which this control belongs, if any. Otherwise
     * it returns null.
     */
    public Control getParent() {
	return parent;
    }

    /**
     * Add a listener that should be informed if any state of this control
     * changes.
     */
    public void addControlChangeListener(ControlChangeListener ccl) {
	if (listeners == null) {
	    listeners = new Vector();
	}
	if (ccl != null) {
	    listeners.addElement(ccl);
	}
    }

    /**
     * Remove an already added listener. Does nothing if the listener was not
     * previously added.
     */
    public void removeControlChangeListener(ControlChangeListener ccl) {
	if (listeners != null && ccl != null)
	    listeners.removeElement(ccl);
    }

    public void informListeners() {
	if (listeners != null) {
	    for (int i = 0; i < listeners.size(); i++) {
		ControlChangeListener ccl =
		    (ControlChangeListener) listeners.elementAt(i);
		ccl.controlChanged( new ControlChangeEvent(this) );
	    }
	}
    }

    /**
     * <B> Sun specific - </B> Returns the description string for this control. 
     */
    public String getTip() {
	return null;
    }
    
    /**
     * <B> Sun specific - </B>
     * Sets the description string for this control. Should be short since it
     * will be displayed as a tool tip when the mouse hovers over the control
     * for a few seconds.
     */
    public void setTip(String tip) {
	//dummy
    }

    public Component getControlComponent() {
	return component;
    }

    public boolean isReadOnly() {
	return false;
    }
}

