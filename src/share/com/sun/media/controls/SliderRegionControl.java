/*
 * @(#)SliderRegionControl.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import com.sun.media.*;

/**
 * A SliderRegionControl can be used to highlight a section of the 
 * slider.
 */
public interface SliderRegionControl extends AtomicControl {

    /**
     *  Sets the long value for this control. Returns the actual long
     * that was set.
     */
    long setMaxValue(long value);

    /**
     * Returns the long value for this control.
     */
    long getMaxValue();

    /**
     *  Sets the long value for this control. Returns the actual long
     * that was set.
     */
    long setMinValue(long value);

    /**
     * Returns the long value for this control.
     */
    long getMinValue();

    boolean isEnable();
 
    void setEnable(boolean f);

}    
