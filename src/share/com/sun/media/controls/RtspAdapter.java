/*
 * @(#)RtspAdapter.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import java.awt.*;
import javax.media.rtp.*;

public class RtspAdapter implements javax.media.control.RtspControl {
    private RTPManager managers[];
    private String mediaTypes[];
    

    public void setRTPManagers( RTPManager managers[]) {
	this.managers = managers;
    }

    public RTPManager[] getRTPManagers() {
	return managers;
    }

    public void setMediaTypes( String mediaTypes[]) {
	this.mediaTypes = mediaTypes;
    }

    public String[] getMediaTypes() {
	return mediaTypes;
    }
    
    public Component getControlComponent() {
	return null;
    }
}
