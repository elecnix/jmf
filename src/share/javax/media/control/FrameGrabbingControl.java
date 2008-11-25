/*
 * @(#)FrameGrabbingControl.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

import javax.media.Control;
import javax.media.Buffer;


/**
 * The <code>FrameGrabbingControl</code> is the interface to grab a
 * still video frame from the video stream.  This control can be
 * exported by a <code>Renderer</code> or a <code>Player</code>
 * via the <code>getControl</code> method. 
 * <p>
 * The frame returned is in raw decoded format.  The <code>ImageConverter</code>
 * class can be used to convert it into Java image format.
 * @since JMF 2.0
 */
public interface FrameGrabbingControl extends javax.media.Control {

    /**
     * Grab the current frame from the video stream.  
     * <p>
     * If the <code>Player</code>
     * or <code>Renderer</code> is in the <i>Started</i> state, the 
     * exact frame returned is not well-defined.  
     * <p>
     * The frame returned is in raw decoded format.  
     * The <code>ImageConverter</code>
     * class can be used to convert it into Java image format. 
     * @return a copy of the current frame; null if the operation fails for any
     *  reason.
     */
    public Buffer grabFrame();
}


