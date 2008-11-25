/*
 * @(#)BooleanControl.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.controls;

import com.sun.media.*;

/**
 * This control represents a toggle state. The state at any time can either
 * be true or false.
 */
public interface BooleanControl extends AtomicControl {

    /**
     * Returns the current state of the control.
     */
    public boolean getValue();

    /**
     * Sets the state of the control.
     */
    public boolean setValue(boolean value);
}
