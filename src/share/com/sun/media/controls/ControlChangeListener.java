/*
 * @(#)ControlChangeListener.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.controls;

import com.sun.media.*;

/**
 * Listener for changes in the state of a Control.
 */
public interface ControlChangeListener {

    /**
     * Gets called whenever the state of a Control changes.
     */
    public void controlChanged(ControlChangeEvent e);

}
