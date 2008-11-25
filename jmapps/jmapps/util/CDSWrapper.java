/*
 * @(#)CDSWrapper.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.util;

import javax.media.*;
import javax.media.protocol.*;
import java.io.IOException;
import javax.media.control.FormatControl;

public class CDSWrapper extends PushBufferDataSource
    implements CaptureDevice {

    private PushBufferDataSource inputDS = null;
    private boolean firstConnect = true;
    private boolean firstDisconnect = true;
    
    public CDSWrapper(PushBufferDataSource incoming) {
	this.inputDS = incoming;
    }

    public void connect() throws IOException {
	if (firstConnect) {
	    inputDS.connect();
	    firstConnect = false;
	}
    }

    public void disconnect() {
	// Don't disconnect input first time
	if (firstDisconnect) {
	    firstDisconnect = false;
	} else {
	    close();
	}
    }

    public void close() {
	// If its been connected atleast once, disconnect the input
	if (!firstConnect) {
	    inputDS.disconnect();
	}
    }

    public MediaLocator getLocator() {
	return inputDS.getLocator();
    }
    
    public String getContentType() {
	return inputDS.getContentType();
    }

    public void start() throws IOException {
	inputDS.start();
    }

    public void stop() throws IOException {
	inputDS.stop();
    }

    /****************************************************************
     * PushBufferDataSource
     ****************************************************************/
    
    public PushBufferStream [] getStreams() {
	return inputDS.getStreams();
    }

    /****************************************************************
     * CaptureDevice
     ****************************************************************/
    
    public FormatControl[] getFormatControls() {
	return ((CaptureDevice)inputDS).getFormatControls();
    }

    public CaptureDeviceInfo getCaptureDeviceInfo() {
	return ((CaptureDevice)inputDS).getCaptureDeviceInfo();
    }

    /****************************************************************
     * Controls
     ****************************************************************/

    public Object [] getControls() {
	return inputDS.getControls();
    }

    public Object getControl(String ctype) {
	return inputDS.getControl(ctype);
    }
    
    /****************************************************************
     * Duration
     ****************************************************************/

    public Time getDuration() {
	return inputDS.getDuration();
    }
}

