/*
 * @(#)RawParser.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.parser;

import java.io.IOException;
import javax.media.*;
import javax.media.Buffer;
import javax.media.protocol.*;
import javax.media.format.*;
import com.sun.media.*;


public abstract class RawParser extends BasicPlugIn implements Demultiplexer {

    static final String NAME = "Raw parser";

    protected DataSource source;
    ContentDescriptor supported[];

    public String getName() {
	return NAME;
    }

    public RawParser() {
	supported = new ContentDescriptor[1];
	supported[0] = new ContentDescriptor(ContentDescriptor.RAW);
    }

    /**
     * Lists the possible input formats supported by this plug-in.
     */
    public ContentDescriptor [] getSupportedInputContentDescriptors() {
	return supported;
    }

    public boolean isPositionable() {
	return source instanceof Positionable;
    }

    public boolean isRandomAccess() {
	return source instanceof Positionable &&
	    ((Positionable)source).isRandomAccess();
    }

    public Track [] getTracks() {
	return null;
    }

    public Time getMediaTime() {
	return Time.TIME_UNKNOWN;
    }

    public Time getDuration() {
	return (source == null ? Duration.DURATION_UNKNOWN :  source.getDuration());
    }

    /**
     * Resets the state of the plug-in. Typically at end of media or when media
     * is repositioned.
     */
    public void reset() {
    }

    public Time setPosition(Time when, int round) {
	if (source instanceof Positionable)
	    return ((Positionable)source).setPosition(when, round);
	return when;
    }

    public Object[] getControls() {
	return source.getControls(); 
    }

}

