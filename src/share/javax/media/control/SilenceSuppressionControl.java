/*
 * @(#)SilenceSuppressionControl.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

/**
 * This interface is a Control for specifying the parameters for silence
 * suppression.
 * @since JMF 2.0
 */
public interface SilenceSuppressionControl extends javax.media.Control {
    /**
     * Returns if silence suppression was enabled.
     * Decoders returns if silence suppression is supported.
     * @return if silence suppression was enabled
     */
    public boolean getSilenceSuppression();

    /**
     * Sets the silence suppression mode.
     * Decoders ignore this method.
     * @param newSilenceSuppression the requested silence suppression
     * mode
     * @return the actual silence suppression mode that was set
     */
    public boolean setSilenceSuppression(boolean newSilenceSuppression);

    /**
     * Returns if silence suppression is supported
     * @return if silence suppression is supported
     */
    public boolean isSilenceSuppressionSupported();

}

