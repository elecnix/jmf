/*
 * @(#)PlugIn.java	1.17 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import javax.media.Format;
import javax.media.Controls;
import javax.media.ResourceUnavailableEvent;

/**
 * The base interface for JMF plug-ins. A <code>PlugIn</code> is a media processing
 * unit that accepts data in a particular format and processes or presents the
 * data. Plug-ins are registered through the <code>PlugInManager</code>.
 *
 * @since JMF 2.0
 */
public interface PlugIn extends Controls {

    /** 
     * The input <CODE>Buffer</CODE> was converted successfully to output.
     */
    public static final int BUFFER_PROCESSED_OK = 0;

    /** 
     * The input <CODE>Buffer</CODE> could not be handled.
     */
    public static final int BUFFER_PROCESSED_FAILED = 1 << 0;

    /** 
     * The input <CODE>Buffer</CODE> chunk was not fully consumed.
     * The plug-in should update the offset + length fields of the 
     * <code>Buffer</code>.  The plug-in will be called later with the same 
     * input <CODE>Buffer</CODE>.
     */
    public static final int INPUT_BUFFER_NOT_CONSUMED = 1 << 1;

    /** 
     * The output <CODE>Buffer</CODE> chunk was not filled.  The plug-in should update 
     * the offset + length fields of the <code>Buffer</code>.  
     * The plug-in will be called later with the same output <CODE>Buffer</CODE>.
     */
    public static final int OUTPUT_BUFFER_NOT_FILLED  = 1 << 2;

    /**
     * The processing of the given buffer has caused the plugin
     * to terminate.  The plugin will not be able to continue
     * further processing.
     */
    public static final int PLUGIN_TERMINATED = 1 << 3;

    /**
     * Gets the name of this plug-in as a human-readable string.
     * @return A <code>String</code> that contains the descriptive name of the 
     * plug-in.
     */
    public String getName();

    /**
     * Opens the plug-in software or hardware component and acquires the
     * resources that the plug-in needs to operate. 
     * All required input and/or output formats have to be set on the plug-in
     * before <code>open</code> is called. 
     * Buffers should not
     * be passed into the plug-in without first calling this method.
     * @exception ResourceUnavailableException If all of the required resources cannot be
     * acquired. 
     */
    public void open() throws ResourceUnavailableException;

    /**
     * Closes the plug-in component and releases the resources it was using. No more data
     * will be accepted by the plug-in after <code>close</code> is called. A closed
     * plug-in can be reinstated by calling
     * <code>open</code> again.
     */
    public void close();

    /**
     * Resets the state of the plug-in. The <code>reset</code> method is typically called if the end of media 
     * is reached or the media
     * is repositioned.
     */
    public void reset();
}


