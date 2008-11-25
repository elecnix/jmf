/*
 * @(#)ControlChangeEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.controls;

import javax.media.*;
import com.sun.media.*;

/**
 * This event contains information about which Control has changed.
 */
public class ControlChangeEvent {

    private Control c;

    /**
     * Creates a ControlChangeEvent with the specified control.
     */
    public ControlChangeEvent(Control c) {
	this.c = c;
    }

    /**
     * Returns the Control that generated this event.
     */
    public Control getControl() {
	return c;
    }
}
    
