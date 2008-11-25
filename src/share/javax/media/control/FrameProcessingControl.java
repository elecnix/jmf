/*
 * @(#)FrameProcessingControl.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

/**
 * This interface is a Control for specifying the parameters for frame 
 * processing.
 * @since JMF 2.0
 */
public interface FrameProcessingControl extends javax.media.Control {

    /**
     * Sets the number of output frames the codec is lagging behind.
     * This is a hint to do minimal processing for the next
     * <code> numFrames </code> frames in order to catch up.
     * @param numFrames  the number of frames the codec is lagging behind
     */
    public void setFramesBehind(float numFrames);

    /**
     * Sets the minimal processing mode. Minimal processing is doing only
     * the needed calculations in order to keep the codec state, without
     * outputting anything.
     * Returns false if miminal processing is not set.
     * @param newMinimalProcessing new minimal processign mode.
     * @return the actual mode set.
     *
     **/
    public boolean setMinimalProcessing(boolean newMinimalProcessing);

    /**
     * Returns the number of output frames that were dropped during encoding
     * since the last call to this method.
     * @return the number of output frames that were dropped during encoding
     * since the last call to this method.
     */
    public int getFramesDropped();

}
