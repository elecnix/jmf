/*
 * @(#)TrackControl.java	1.20 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

import javax.media.*;
import javax.media.format.*;

/**
 *
 * The <code>TrackControl</code> interface is provided by the
 * <code>Processor</code> to query, control and manipulate the data
 * of individual media tracks.
 *
 * @see javax.media.Format
 * @see javax.media.Processor
 *
 * @since JMF 2.0
 */

public interface TrackControl extends FormatControl, javax.media.Controls {

    /**
     * Specify a chain of <code>Codec</code> plugins to be used on the track.
     * The <code>Processor</code> will try its best to insert the codecs in
     * the given order to the data flow.
     * <br>
     * <code>Effect</code> is a subclass of <code>Codec</code> and 
     * thus can be used also in this method.
     *
     * @param codecs an array of <code>Codec</code> plugins to be set.
     *
     * @exception javax.media.NotConfiguredError if the owning
     *    <code>Processor</code> is not configured.
     * @exception javax.media.UnsupportedPlugInException if the
     *    PlugIn cannot be set.
     */
    public void setCodecChain(Codec codecs[])
	throws UnsupportedPlugInException, NotConfiguredError;

    /**
     * Replace the default renderer of the <code>Processor</code> 
     * with the given one.
     *
     * @param renderer the <code>Renderer</code> plugin to be used.
     *
     * @exception javax.media.NotConfiguredError if the owning
     *    <code>Processor</code> is not configured.
     * @exception javax.media.UnsupportedPlugInException if the
     *    PlugIn cannot be set.
     */
    public void setRenderer(Renderer renderer)
	throws UnsupportedPlugInException, NotConfiguredError;

}


