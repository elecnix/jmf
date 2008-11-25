/*
 * @(#)V4LSourceStream.java	1.7 03/04/30
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.protocol.v4l;

import java.awt.Dimension;
import java.awt.Component;
import java.util.Vector;
import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.control.FormatControl;
import java.io.IOException;

public class V4LSourceStream implements PushBufferStream, FormatControl, Runnable {

    protected CaptureDeviceInfo cdi = null;
    protected FormatControl [] formatControls = null;
    protected ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW);
    protected Format [] supportedFormats = null;
    protected int maxDataLength;
    protected byte [] data;
    protected int deviceNo = 0;
    protected int seqNo = 0;
    protected VideoFormat currentFormat = null;
    protected VideoFormat requestedFormat = null;
    protected int requestedPort = -1;
    
    protected boolean started = false;
    protected boolean stopped = true;
    protected Thread thread;
    protected float frameRate = 30f;
    protected BufferTransferHandler transferHandler;
    protected Control [] controls = new Control[0];

    protected V4LCapture capture;

    SystemTimeBase systemTimeBase = new SystemTimeBase();

    
    public V4LSourceStream(MediaLocator locator) {
	// Get the device number and preferred format from the locator
	VideoFormat locatorFormat = parseLocator(locator);
	// Make sure we can find/create a CaptureDeviceInfo object
	if (getCaptureDeviceInfo() == null)
	    throw new Error("No such capture device!");
	// Pick the default format
	requestedFormat = (VideoFormat) getSupportedFormats()[0];
	// Update the format if any changes are specified in the locator
	if (locatorFormat != null)
	    requestedFormat = (VideoFormat) locatorFormat.intersects(requestedFormat);

	// Create and open the capture device
	try {
	    capture = new V4LCapture(deviceNo);
	} catch (Throwable t) {
	    throw new Error("Couldn't initialize capture device");
	}

	// Set the port (channel)
	if (requestedPort != -1)
	    setPort(requestedPort);

	// Set the capture format. This can be changed until the start() call
	setFormat(requestedFormat);
	//handleFormatChange();
    }

    public CaptureDeviceInfo getCaptureDeviceInfo() {
	if (cdi == null) {
	    String url = "v4l://" + deviceNo;

	    // Check if device is registered with CaptureDeviceManager
	    Vector cdiList =
		CaptureDeviceManager.getDeviceList(new VideoFormat(null));
	    if (cdiList != null && cdiList.size() > 0) {
		for (int i = 0; i < cdiList.size(); i++) {
		    CaptureDeviceInfo tempCDI =
			(CaptureDeviceInfo) cdiList.elementAt(i);
		    if (tempCDI.getLocator().toString().equalsIgnoreCase(url)) {
			cdi = tempCDI;
			break;
		    }
		}
	    }
	    
	    // If we couldn't find it registered in CaptureDeviceManager
	    if (cdi == null) {
		cdi = autoDetect(deviceNo);
	    }
	}

	if (cdi != null) {
	    supportedFormats = cdi.getFormats();
	}
	
	return cdi;
    }

    protected CaptureDeviceInfo autoDetect(int cardNo) {
	CaptureDeviceInfo cdi = null;
	try {
	    cdi = new V4LDeviceQuery(cardNo);
	    if ( cdi != null && cdi.getFormats() != null &&
		 cdi.getFormats().length > 0) {
		// Commit it to disk. Its a new device
		if (CaptureDeviceManager.addDevice(cdi)) {
		    CaptureDeviceManager.commit();
		}
	    }
	} catch (Throwable t) {
	    if (t instanceof ThreadDeath)
		throw (ThreadDeath)t;
	}
	
	return cdi;
    }

    public FormatControl [] getFormatControls() {
	if (formatControls == null) {
	    formatControls = new FormatControl[1];
	    formatControls[0] = (FormatControl) this;
	}
	return formatControls;
    }

    protected VideoFormat parseLocator(MediaLocator loc) {
	deviceNo = getDeviceNo(loc);

	// TODO : add other parameters to the locator, such as size, channel,
	//        and format
	
	return null;
    }

    // Parse the locator to get the device number
    protected int getDeviceNo(MediaLocator locator) {
	int deviceNo = 0;
	String remainder = locator.getRemainder();
	if (remainder != null && remainder.length() > 0) {
	    while (remainder.length() > 1 && remainder.charAt(0) == '/')
		remainder = remainder.substring(1);
	    try {
		Integer integer = Integer.valueOf(remainder);
		if (integer != null)
		    deviceNo = integer.intValue();
	    } catch (Throwable t) {
	    }
	}
	return deviceNo;
    }

    protected void setPort(String portMatch) {
	portMatch = portMatch.toLowerCase();
	VCapability vcap = new VCapability();
	capture.getCapability(vcap);
	VChannel vchan;
	// Search through all channels to find the first matching
	// channel name that contains portMatch
	for (int i = 0; i < vcap.channels; i++) {
	    vchan = new VChannel(i);
	    capture.getChannel(vchan);
	    if (vchan.name != null &&
		vchan.name.toLowerCase().indexOf(portMatch) >= 0) {
		capture.setChannel(vchan);
		return;
	    }
	}
    }

    protected void setPort(int portNo) {
	// TODO
    }

    /****************************************************************
     * FormatControl
     ****************************************************************/

    public Format [] getSupportedFormats() {
	return supportedFormats;
    }

    public Format setFormat(Format inf) {
	if (com.sun.media.BasicPlugIn.matches(inf, getSupportedFormats()) == null)
	    return null;
	requestedFormat = (VideoFormat) inf;
	if (!started)
	    return handleFormatChange();
	return requestedFormat;
    }

    protected Format handleFormatChange() {
	VideoFormat inf = requestedFormat;
	requestedFormat = null;
	int palette = capture.formatToPalette(inf);
	float frameRate = inf.getFrameRate();
	if (frameRate < 0)
	    frameRate = 30f;
	VPicture vpict = new VPicture();
	capture.getPicture(vpict);
	vpict.palette = palette;
	vpict.depth = capture.paletteToDepth(palette);
	capture.setPicture(vpict);
	if (capture.setFormat(vpict.depth, palette,
			      inf.getSize().width,
			      inf.getSize().height,
			      frameRate) < 0) {
	    return null;
	}
	currentFormat = inf;
	return currentFormat;
    }

    public void setEnabled(boolean value) {
	// ignore
    }

    public boolean isEnabled() {
	return true;
    }
    
    public Component getControlComponent() {
	// TODO
	return null;
    }
    
    /***************************************************************************
     * SourceStream
     ***************************************************************************/
    
    public ContentDescriptor getContentDescriptor() {
	return cd;
    }

    public long getContentLength() {
	return LENGTH_UNKNOWN;
    }

    public boolean endOfStream() {
	return false;
    }

    /***************************************************************************
     * PushBufferStream
     ***************************************************************************/

    public Format getFormat() {
	return currentFormat;
    }

    public void read(Buffer buffer) throws IOException {
	int retVal = 0;
	if (!started)
	    throw new IOException("Not started");
	maxDataLength = currentFormat.getMaxDataLength();
	Object outdata = buffer.getData();
	if (!(outdata instanceof byte[]) || ((byte[])outdata).length < maxDataLength) {
	    outdata = new byte[maxDataLength];
	    buffer.setData(outdata);
	}
	synchronized (this) {
	    int count = 0;
	    while ( (retVal = capture.readNextFrame((byte[]) outdata, 0, maxDataLength) ) < 0 && count < 20 ) {
		try {
		    Thread.sleep(10);
		} catch (InterruptedException ie) {
		}
		count++;
	    }
	    buffer.setFormat(currentFormat);
	    buffer.setSequenceNumber( seqNo );
	    buffer.setLength(maxDataLength);
	    buffer.setFlags(Buffer.FLAG_KEY_FRAME | Buffer.FLAG_SYSTEM_TIME |
			    Buffer.FLAG_LIVE_DATA);
	    buffer.setHeader( null );
	    buffer.setTimeStamp(systemTimeBase.getNanoseconds());
	    seqNo++;
	}
    }

    public void setTransferHandler(BufferTransferHandler transferHandler) {
	synchronized (this) {
	    this.transferHandler = transferHandler;
	    notifyAll();
	}
    }

    void start(boolean started) {
	synchronized ( this ) {
	    // Check if current state is same as requested state
	    if (this.started == started)
		return;
	    this.started = started;
	    if (started) {
		if (requestedFormat != null)
		    handleFormatChange();
		thread = new Thread(this);
		capture.start();
		thread.start();
	    } else {
		stopped = true;
		capture.stop();
	    }
	    notifyAll();
	}
    }

    synchronized void close() {
	if (capture != null) {
	    if (started)
		start(false);
	    capture.close();
	    capture = null;
	}
    }

    /***************************************************************************
     * Runnable
     ***************************************************************************/

    public void run() {
	stopped = false;
	while (started) {
	    synchronized (this) {
		while (transferHandler == null && started) {
		    try {
			wait(200);
		    } catch (InterruptedException ie) {
		    }
		} // while
	    }

	    if (started && transferHandler != null) {
		transferHandler.transferData(this);
		try {
		    Thread.currentThread().sleep( 1 );
		} catch (InterruptedException ise) {
		}
	    }
	} // while (started)
	stopped = true;
    } // run

    // Controls
    
    public Object [] getControls() {
	return controls;
    }

    public Object getControl(String controlType) {
       try {
          Class  cls = Class.forName(controlType);
          Object cs[] = getControls();
          for (int i = 0; i < cs.length; i++) {
             if (cls.isInstance(cs[i]))
                return cs[i];
          }
          return null;

       } catch (Exception e) {   // no such controlType or such control
         return null;
       }
    }
}
