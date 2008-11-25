/*
 * @(#)KeyFrameControl.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

/**
 * This interface is a Control for specifying the parameters for key frame
 * interval.
 * @since  2.0
 */
public interface KeyFrameControl extends javax.media.Control {

    /**
     * Set the desired interval between key frames, if such a parameter
     * is relevant to the encoder. <code>frames-1</code> indicates the number of
     * non-key frames that are encoded between key frames. 
     *
     * This value is
     * only a hint, and the encoder may choose to override this value if
     * needed. <p>
     * @param frames key frame interval.  It should be greater than or
     * equals to 1.
     * The key frame interval is specified in output frames.
     * <code>getPreferredKeyFrameInterval</code> returns the default.
     * @return the actual key frame interval set.
     */
    public int  setKeyFrameInterval(int frames);

    /**
     * Returns the current value of the keyFrameInterval.
     * The key frame interval is specified in output frames.
     * @return the current value of the keyFrameInterval.
     */
    public int   getKeyFrameInterval();

    /**
     * Returns the keyFrameInterval value preferred by the encoder.
     * The key frame interval is specified in output frames.
     * @return the keyFrameInterval value preferred by the encoder.
     */
    public int   getPreferredKeyFrameInterval();

}

