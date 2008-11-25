/*
 * @(#)Renderer.java	1.16 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import javax.media.format.*;

/**
 * A <code>Renderer</code> is a media processing unit that renders input media
 * to a pre-defined destination, such as the screen or system speaker. It has
 * one input and no outputs--rendering is the final stage of the media processing pipeline.  
 * <p>
 * If a <code>Renderer</code> implements the <code>Clock</code> interface,
 * it can be used by a <code>Player</code> as
 * the master time base for synchronization.   In this case, the
 * <code>Renderer</code> should update the media time and time-base time as 
 * it processes the media. 
 * @since JMF 2.0
 */
public interface Renderer extends javax.media.PlugIn {

    /**
     * Lists the input formats supported by this <CODE>Renderer</CODE>.
     * @return An array of <CODE>Format</CODE> objects that represent 
     * the input formats supported by this <CODE>Renderer</CODE>.
     */
    public Format [] getSupportedInputFormats();

    /**
     * Sets the <CODE>Format</CODE> of the input data.
     * @return The <CODE>Format</CODE> that was set. This is typically the 
     * supported <CODE>Format</CODE> that most closely matches the specified <CODE>Format</CODE>. 
     * If possible, the format fields that were not specified are set to the preferred values
     * in the returned <CODE>Format</CODE>. Returns null if the specified <CODE>Format</CODE> is 
     * not supported. 
     */
    public Format setInputFormat(Format format);

    /**
     * Initiates the rendering process. When <CODE>start</CODE> is called, the renderer begins
     * rendering any data available in its internal buffers.
     */
    public void start();

    /**
     * Halts the rendering process. 
     */
    public void stop();

    /**
     * Processes the data and renders it to the output device
     * represented by this <CODE>Renderer</CODE>.
     * @return <CODE>BUFFER_PROCESSED_OK</CODE> if the processing is successful.  Other
     * possible return codes are defined in <CODE>PlugIn</CODE>. 
     * @see PlugIn
     */
    public int process(Buffer buffer);

}

