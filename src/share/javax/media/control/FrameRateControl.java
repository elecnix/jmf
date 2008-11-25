/*
 * @(#)FrameRateControl.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

/**
 * This interface is a Control for specifying the parameters for frame rate.
 * @since JMF 2.0
 */
public interface FrameRateControl extends javax.media.Control {

    /**
     * Returns the current output frame rate. Returns -1 if it is unknown.
     * @return the output output frame rate in frames per second.
     */
    public float getFrameRate();

    /**
     * Sets the frame rate.
     * Returns -1 if it is unknown or it is not controllable.
     * @param newFrameRate the requested new frame rate
     * @return the actual frame rate in frames per second.
     */
    public float setFrameRate(float newFrameRate);

    /**
     * Returns the maximum output frame rate. Returns -1 if it is unknown.
     * @return the maximum output frame rate in frames per second.
     */
    public float getMaxSupportedFrameRate();

    /**
     * Returns the default output frame rate. Returns -1 if it is unknown.
     * @return the default output frame rate in frames per second.
     */
    public float getPreferredFrameRate();



}

