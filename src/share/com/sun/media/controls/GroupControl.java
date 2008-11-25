/*
 * @(#)GroupControl.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.controls;

import javax.media.*;
import com.sun.media.*;

/**
 * A GroupControl is a parent to a set of smaller controls. This is a base
 * class interface for group controls such as VolumeControl, ColorControl,
 * PlaybackControl, etc.
 */
public interface GroupControl extends AtomicControl {

    /**
     * Returns any controls that might constitute this control.
     */
    public Control[] getControls();
}

