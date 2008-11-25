/*
 * @(#)ColorControl.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.controls;

import com.sun.media.*;

/**
 * A group of controls that modify the color of the video output from this
 * player.
 */
public interface ColorControl extends GroupControl {

    /**
     * Returns a brightness control object for the video output.
     */
    NumericControl getBrightness();

    /**
     * Returns a contrast control object for the video output.
     */
    NumericControl getContrast();

    /**
     * Returns a color saturation control object for the video output.
     */
    NumericControl getSaturation();

    /**
     * Returns a hue control object for the video output.
     */
    NumericControl getHue();

    /**
     * Returns a grayscale control object for the video output. Grayscale
     * output can be turned on or off.
     */
    BooleanControl getGrayscale();
}
