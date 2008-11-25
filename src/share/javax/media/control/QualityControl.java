/*
 * @(#)QualityControl.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

/**
 * This interface is a Control for specifying the parameters for quality.
 * Quality is referenced by a float value of 0.0 for minimal quality
 * and 1.0 for maximum quality. There is usually a tradeoff between 
 * CPU usage and the quality:
 * higher quality requires higher CPU usage.
 *
 * @since JMF 2.0
 */
public interface QualityControl extends javax.media.Control {

    /**
     * Returns the current value of the decoding or encoding quality parameter.
     * @return the current decoding or encoding quality.
     */
    public float getQuality();

    /**
     * Set the quality for the decoding or encoding. This value may have
     * different
     * effects depending on the type of compression. A higher quality
     * setting will result in better quality of the resulting outputb, e.g.
     * better image quality for video.  There is usually a tradeoff between
     * CPU usage and the quality: higher quality requires higher
     * CPU usage.  This value is only a hint and the codec can choose
     * to ignore it. The actual value that was set is returned.
     * It should be in the range of 0.0 to 1.0.
     * @param newQuality the decoding or encoding quality.
     * @return the quality that is actually set.
     */
    public float setQuality(float newQuality);

    /**
     * Returns the default quality recommended for decoding or encoding.
     * @return the preferred decoding or encoding quality.
     */
    public float getPreferredQuality();

    /**
     * Returns if the encoder can increase frame rate with reduced resolution
     * and vica-versa.
     * @return if the encoder can increase frame rate with reduced resolution
     * and vica-versa.
     */
    public boolean isTemporalSpatialTradeoffSupported();

}

