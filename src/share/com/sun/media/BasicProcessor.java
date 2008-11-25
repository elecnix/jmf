/*
 * @(#)BasicProcessor.java	1.10 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import javax.media.*;
import javax.media.format.*;
import javax.media.control.*;
import javax.media.protocol.*;

/**
 * BasicProcessor implements the bases of a javax.media.Processor.  It handles
 * all the Processor state transitions, event handling and management of
 * any Controller under its control.
 */

public abstract class BasicProcessor extends BasicPlayer implements Processor {

    static String NOT_CONFIGURED_ERROR = "cannot be called before the Processor is configured";

    /**
     * A processor is configurable.
     */
    protected boolean isConfigurable() {
	return true;
    }

    /**
     * Return the tracks in the media. This method can only be called after 
     * the Processor has been configured. 
     * A <code>NotConfiguredError</code> is thrown if 
     * <code>getTrackControls</code> is called and the 
     * Processor is in the <code>Unrealized</code> or 
     * <code>Configuring</code> states. 
     *
     * @return  An array of the tracks in the media.  An emtpy array is returned
     *   if there is no <code>TrackControl</code> available for this Processor.
     * @exception NotConfiguredError  if the Processor is Unrealized.
     */
    public TrackControl[] getTrackControls() throws NotConfiguredError {
	if (getState() < Processor.Configured)
	    throw new NotConfiguredError("getTrackControls " + NOT_CONFIGURED_ERROR);
	return new TrackControl[0];
    }

    /** 
     * Return all the content-types which this Processor's output supports. 
     * The Processor builds the <code>ContentDescriptor</code> array according 
     * to the input DataSource attached to the Processor and according 
     * to the codecs 
     * and multiplexers installed in the system.
     *
     * @return  An array of the the content-types supported by the Processor.
     * @exception NotConfiguredError if the Processor is Unrealized.
     */
    public ContentDescriptor[] getSupportedContentDescriptors() 
	throws NotConfiguredError {
	if (getState() < Processor.Configured)
	    throw new NotConfiguredError("getSupportedContentDescriptors " + NOT_CONFIGURED_ERROR);
	return new ContentDescriptor[0];
    }
    
    /**
     * Set the output content-type. 
     * If <code>setContentDescriptor</code> is not called, the 
     * output DataSource's output 
     * will be on individual tracks.
     *
     * @param outputContentDescriptor  the content-type of the output.
     * @exception NotConfiguredError if the Processor is Unrealized.
     * realized state and it does not support format changes after it has been
     * realized.
     */
    public ContentDescriptor setContentDescriptor(ContentDescriptor ocd) 
	throws NotConfiguredError {
	if (getState() < Processor.Configured)
	    throw new NotConfiguredError("setContentDescriptor " + NOT_CONFIGURED_ERROR);
	return ocd;
    }
    
    /**
     * Return the output content-type.
     *
     * @return  The current output content-type.
     * @exception NotConfiguredError if the Processor is Unrealized.
     */
    public ContentDescriptor getContentDescriptor() 
	throws NotConfiguredError {
	if (getState() < Processor.Configured)
	    throw new NotConfiguredError("getContentDescriptor " + NOT_CONFIGURED_ERROR);
	return null;
    }
    
    /** 
     * Return the output DataSource of the Processor. The output DataSource
     * is the output connection of the Processor through which it 
     * supplies the processed streams.
     *
     * @return  The output DataSource of the Processor.
     * @exception NotRealizedError if the Processor is not realized.
     */
    public DataSource getDataOutput() throws NotRealizedError {
	if (getState() < Realized)
	    throw new NotRealizedError("getDataOutput cannot be called before the Processor is realized"); 
	return null;
    }

}
