/*
 * @(#)VideoSizingControl.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.controls;

import java.awt.*;
import javax.media.*;
import com.sun.media.*;

public interface VideoSizingControl extends Control {

    /**
     * Returns true if the video window can be scaled to any size.
     */
    boolean supportsAnyScale();

    /**
     * Sets the video window to an arbitrary size causing the video to be
     * scaled accordingly. Returns the actual size that the window was set to,
     * which might be different from the requested size due to constraints.
     */
    Dimension setVideoSize(Dimension size);

    /**
     * Returns the current size of the video window.
     */
    Dimension getVideoSize();

    /**
     * Returns the actual frame size of the video input stream.
     */
    Dimension getInputVideoSize();

    /**
     * Returns true if the video can be scaled to fixed zoom factors.
     * Use this only if <B>supportsAnyScale</B> returns false.
     * @see #getValidZoomFactors
     */
    boolean supportsZoom();

    /**
     * If the control supports fixed zoom factors then this method returns
     * a list of zoom factors that are available.
     */
    float[] getValidZoomFactors();

    /**
     * Returns a zoom control if available, else returns null.
     */
    NumericControl getZoomControl();

    /**
     * Returns true if the video can be clipped to a rectangular region
     * before it is displayed.
     */
    boolean supportsClipping();

    /**
     * Sets the clip region if the player supports clipping. Returns the
     * actual clip rectangle that was set.
     * @see #getClipRegion
     */
    Rectangle setClipRegion(Rectangle clip);

    /**
     * Returns the current clip rectangle in effect.
     * @see #setClipRegion
     */
    Rectangle getClipRegion();

    /**
     * Freezes the video window, i.e., continues reading/decoding the video but
     * does not update the video window. Audio, if available, will
     * continue playing.
     */
    BooleanControl getVideoMute();
}

