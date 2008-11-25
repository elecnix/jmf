/*
 * @(#)RateRange.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

/**
 * Describes the speed at which data flows.
 *
 * @version 1.4, 02/08/21.
 * 
 */
public class RateRange implements java.io.Serializable {

    // $jdr: Java needs number range objects.
    float minimum;
    float maximum;
    float current;
    boolean exact;

    RateRange() {
	super();
    }
    
    /**
     * Copy constructor.
     *
    */
    public RateRange(RateRange r) {
	minimum = r.minimum;
	maximum = r.maximum;
	current = r.current;
	exact = r.exact;
    }
    
    /**
     * Constructor using required values.
     *
     * @param init The initial value for this rate.
     * @param min The minimum value that this rate can take.
     * @param max The maximum value that this rate can take.
     * @param isExact Set to <CODE>true</CODE> if the source rate does not vary when using this 
     * rate range.
     */
    public RateRange(float init, float min, float max, boolean isExact) {
	minimum = min;
	maximum = max;
	current = init;
	exact = isExact;
    }


    /**
     * Set the current rate. Returns the rate that was actually set.
     * This implementation just returns the specified rate, 
     * subclasses should return the rate that was actually
     * set.
     * @param rate The new rate.
     */
    public float setCurrentRate(float rate) {
	current = rate;
	return current;
    }

    /**
     * Get the current rate.
     *
     * @return The current rate.
     *
     */
    public float getCurrentRate() {
	return current;
    }

    /**
     * Get the minimum rate supported by this range.
     *
     * @return The minimum rate.
     */
    public float getMinimumRate() {
	return minimum;
    }

    /**
     * Get the maximum rate supported by this range.
     *
     * @return The maximum rate.
     */
    public float getMaximumRate() {
	return maximum;
    }
    
    /**
     * Determine whether or not a particular value is within the range of
     * supported rates.
     *
     * @param The rate to test.
     * @return Returns <CODE>true</CODE> if the specified rate is supported.
     */
    public boolean inRange(float rate) {
	return (minimum < rate) && (rate < maximum);
    }

    
    /**
     * Determine whether or not the source will maintain a constant
     * speed when using this rate. If the
     * rate varies, synchronization is usually impractical.
     *
     * @return Returns <CODE>true</CODE> if the source will maintain a constant speed at this rate.
     */
    public boolean isExact() {
	return exact;
    }

}
