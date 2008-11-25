/*
 * @(#)H261Control.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

/**
 * This interface is a Control for specifying the parameters for H.261 
 * video codec.
 * @since JMF 2.0
 */
public interface H261Control extends javax.media.Control {

    /**
     * Returns if still image transmission is supported
     * @return if still image transmission is supported
     */
    public boolean isStillImageTransmissionSupported() ;

    /**
     * Sets the still image transmission mode.
     * Decoders ignore this method.
     * @param newStillImageTransmission the requested still image transmission
     * mode
     * @return the actual still image transmission mode that was set
     */
    public boolean setStillImageTransmission(boolean newStillImageTransmission);

    /**
     * Returns if still image transmission was enabled.
     * Decoders returns if still image transmission is supported.
     * @return if still image transmission was enabled
     */
    public boolean getStillImageTransmission();

}
