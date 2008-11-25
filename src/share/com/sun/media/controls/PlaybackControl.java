/*
 * @(#)PlaybackControl.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.controls;

import com.sun.media.*;

/**
 * Groups playback related controls.
 */
public interface PlaybackControl extends GroupControl {

    /**
     * Control to play the media.
     */
    BooleanControl getPlay();

    /**
     * Control to stop the media.
     */
    BooleanControl getStop();

    /**
     * Moves the media time forward by an xxx amount.
     */
    ActionControl  getStepForward();

    /**
     * Moves the media time back by an xxx amount.
     */
    ActionControl  getStepBackward();

    /**
     * Play rate control.
     */
    NumericControl getPlayRate();

    /**
     * Control to set the media time.
     */
    NumericControl getSeek();
}
