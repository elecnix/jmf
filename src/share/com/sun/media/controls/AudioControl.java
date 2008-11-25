/*
 * @(#)AudioControl.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import com.sun.media.*;

/**
 * AudioControl groups audio related controls.
 */
public interface AudioControl extends GroupControl {

    /**
     * The port to send the audio output to. Since this is quite
     * platform-dependent, it probably returns a control that has nothing
     * more than a Component.
     */
    AtomicControl getOutputPort();

    /**
     * Amplitude for a high-pass filter.
     */
    NumericControl getTreble();

    /**
     * Amplitude for a low-pass filter.
     */
    NumericControl getBass();

    /**
     * Left/Right balance control. -1.0 .. 1.0  ???
     */
    NumericControl getBalance();

}
