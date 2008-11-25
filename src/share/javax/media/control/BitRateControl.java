/*
 * @(#)BitRateControl.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

/**
 * This interface is a Control for specifying the parameters for bit rate.
 * The bit rates are specified in bits per second. This Control can be used
 * to export the bit rate information for an incoming stream or to control
 * the encoding bit rate of a compressor.
 * @since JMF 2.0
 */
public interface BitRateControl extends javax.media.Control {

    /**
     * Returns the current bit rate of the owning object. If the stream
     * is of variable bit rate, then the value returned is an
     * instantaneous or average bit rate over a period of time.
     * The bit rates are specified in bits per second.
     * @return the bit rate
     */
    int getBitRate();

    /**
     * Sets the bit rate of the owning object. This is mostly relevant
     * in the case of encoders. If the bit rate cannot be controlled, then
     * the return value is -1.
     * The bit rates are specified in bits per second.
     * @return the bit rate that was actually set on the object, or -1
     *         if the bit rate is not controllable.
     */
    int setBitRate(int bitrate);

    /**
     * Returns the lowest bit rate that this object can encode the
     * data stream to.
     * The bit rates are specified in bits per second.
     * @return the lowest supported bit rate
     */
    int getMinSupportedBitRate();

    /**
     * Returns the highest bit rate that this object can encode the
     * data stream to.
     * The bit rates are specified in bits per second.
     * @return the maximal supported bit rate
     */
    int getMaxSupportedBitRate();
}
