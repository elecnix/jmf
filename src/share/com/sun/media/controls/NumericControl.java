/*
 * @(#)NumericControl.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.controls;

import com.sun.media.*;

/**
 * A control that represents the state by a numeric value. The value can be any
 * number represented as a float as long as it falls within the range and
 * has the specified granularity/accuracy. For example, if the lower limit is
 * 0 and the upper limit is 255 and the granularity is 1, then it can be assigned
 * any integer value from 0 to 255.
 */
public interface NumericControl extends AtomicControl {

    /**
     * The smallest value assignable to this control.
     */
    public float getLowerLimit();

    /**
     * The largest value assignable to this control.
     */
    public float getUpperLimit();

    /**
     * Returns the value that the control currently represents.
     */
    public float getValue();

    /**
     * Sets the value on the control and returns the value that was actually
     * set.
     */
    public float setValue(float value);

    /**
     * Returns the value that is the default for this control.
     */
    public float getDefaultValue();

    /**
     * Sets the default value for the control.
     */
    public float setDefaultValue(float value);

    /**
     * Returns the granularity of the numeric value.
     */
    public float getGranularity();

    /**
     * ???
     */
    public boolean isLogarithmic();

    /**
     * ???
     */
    public float getLogarithmicBase();
}
