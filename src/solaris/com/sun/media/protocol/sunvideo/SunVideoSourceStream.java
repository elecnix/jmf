/*
 * @(#)SunVideoSourceStream.java	1.30 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.sunvideo;

import java.security.*;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.awt.*;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.Date;
import javax.media.*;
import javax.media.protocol.*;
import com.sun.media.protocol.*;
import javax.media.format.*;
import javax.media.format.VideoFormat;
import javax.media.format.JPEGFormat;
import javax.media.format.RGBFormat;
import javax.media.control.*;
import com.sun.media.controls.*;
import com.sun.media.ui.*;
import com.sun.media.util.jdk12;
import com.sun.media.util.MediaThread;
import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;

// Following only needed while the control frame hack is present...
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
// Preceding only needed while the control frame hack is present...

/****************************************************************
 * SourceStream for the DataSource
 *
 * The SourceStream can be accessed with the URL sunvideo:
 * The URL has been extended to allow selecting some of the options:
 * sunvideo://card/port/compression/size where
 *	card = the sunvideo card to use (default 0) when multiple
 *		cards are installed.
 *	port = port to use (default 1), s-vhs, 1, or 2.
 *	compression = rgb or jpeg (default jpeg).
 *	size = 1, 1/2, or 1/4 (default 1/2). Actual frame size
 *		depends on whether the camera is NTSC or PAL.
 ****************************************************************/

class SunVideoSourceStream
extends BasicSourceStream
implements PushBufferStream, Owned {

    private DataSource     dataSource = null;
    private MediaLocator   locator = null;
    int            maxDataSize = 230400;
    BufferTransferHandler  transferHandler = null;
    private byte []        data = null;
    private int            length = 0;
    private long	   nextSequence = 1;
    long	   timeStamp = 0;
    XILCapture	   svCap = null;
    SystemTimeBase systemTimeBase = new SystemTimeBase();

    private VideoFormat    capFormat = null;
    static private javax.media.Format[] supported;

    // Following only needed while the control frame hack is present...
    private static final boolean CONTROL_PANEL_HACK = false;
			   // set to false to eliminate the
			   // control frame at connect.
    private Frame	   controlFrame = null;
    // Preceding only needed while the control frame hack is present...

    private static Integer SunVideoLock = new Integer(0);
    Integer readLock = new Integer(1);

    private boolean        started = false;
    private boolean        connected = false;
    private boolean        connectedOK = false;
    private boolean	   inUse = false;

    private int            cardNo = 0;
    private static String [] VALID_PORTS = { "S-VHS", "1", "2" };
    private static final int DEFAULT_PORT = 1;
    private int		   portNo = DEFAULT_PORT;
    
    private static String [] VALID_COMPRESS = { "JPEG", "RGB" };
    private static String [] VIDEO_COMPRESS = {
				javax.media.format.VideoFormat.JPEG,
				javax.media.format.VideoFormat.RGB
    };		// must match order of VALID_COMPRESS
    private static final int DEFAULT_COMPRESS = 0;
    private static final int RGB_COMPRESS = 1;	// Must match RGB above
    private int		   compressNo = DEFAULT_COMPRESS;

    // NOTE that sizes must be largest to smallest for some logic to work
    private static String [] VALID_SIZES = { "1", "1/2", "1/4" };
    private static Dimension [] DEFAULT_DIMENSIONS = {
						new Dimension(640, 480),
						new Dimension(320, 240),
						new Dimension(160, 120),
    };		// must match order of VALID_SIZES
    private static float [] VALID_SIZES_FLOAT = { 1.0f, 0.5f, 0.25f };
    private static int [] VALID_SCALE = { 1, 2, 4 };
    private static final int DEFAULT_SIZE = 1;
    private static float SIZE_GRANULARITY = 0.25f;
    private int		   sizeNo = DEFAULT_SIZE;

    private static final int DEFAULT_RATE = 30;
    private int		   rateNo = DEFAULT_RATE;

    private static final int DEFAULT_QUALITY = 50;
    private int		   qualityNo = DEFAULT_QUALITY;

    private LocalPortControl portControl = null;
    private RateControl rateControl = null;
    private LocalQualityControl qualityControl = null;
    private LocalFormatControl formatControl = null;

    private float preferredFrameRate = 30.0f;
    private PushThread pt = null;
    long ptDelay = 1000/rateNo;
    
    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege=false;
    private Method mSecurity[] = new Method[1];
    private Class clSecurity[] = new Class[1];
    private Object argsSecurity[][] = new Object[1][0];

    static {
        try {
            jmfSecurity = JMFSecurityManager.getJMFSecurity();
            securityPrivelege = true;
        } catch (SecurityException e) {
        }

	supported = new javax.media.Format[] {

	    // NTSC
		    // JPEG formats
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.JPEG,
				new Dimension(320, 240),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.JPEG,
				new Dimension(160, 120),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),

		    // RGB formats
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.RGB,
				new Dimension(640, 480),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.RGB,
				new Dimension(320, 240),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.RGB,
				new Dimension(160, 120),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),

	    // PAL
		    // JPEG formats
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.JPEG,
				new Dimension(384, 288),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.JPEG,
				new Dimension(192, 144),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),

		    // RGB formats
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.RGB,
				new Dimension(768, 576),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.RGB,
				new Dimension(384, 288),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.RGB,
				new Dimension(192, 144),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),

	};
    }

    public SunVideoSourceStream(DataSource ds) {
	super(new ContentDescriptor(ContentDescriptor.RAW),
	      LENGTH_UNKNOWN);
	this.dataSource = ds;
	this.locator = ds.getLocator();
	cardNo = 0;
	String remainder = locator.getRemainder();
	if (remainder != null && remainder.length() > 0) {
	    while (remainder.length() > 1 && remainder.charAt(0) == '/')
		remainder = remainder.substring(1);
	    String cardStr, portStr, compStr, sizeStr;
	    portStr = null;		// assume no port specified
	    compStr = null;		// assume no compress specified
	    sizeStr = null;		// assume no size specified
	    // Now see if there's a port specified.
	    int off = remainder.indexOf('/');
	    if (off == -1) {
		cardStr = remainder;
	    } else {
		cardStr = remainder.substring(0, off);
		remainder = remainder.substring(off + 1);
		// Now see if there's a compression specified
		off = remainder.indexOf('/');
		if (off == -1) {
		    portStr = remainder;
		} else {
		    portStr = remainder.substring(0, off);
		    remainder = remainder.substring(off + 1);
		    // Now see if there's a size specified
		    off = remainder.indexOf('/');
		    if (off == -1) {
			compStr = remainder;
		    } else {
			compStr = remainder.substring(0, off);
			sizeStr = remainder.substring(off + 1);
		    }
		}
	    }
	    try {
		Integer integer = Integer.valueOf(cardStr);
		if (integer != null) {
		    cardNo = integer.intValue();
		}
	    } catch (Throwable t) {
	    }
	    if (portStr != null && portStr.length() > 0) {
		for (int i = 0; i < VALID_PORTS.length; i++) {
		    if (VALID_PORTS[i].equalsIgnoreCase(portStr)) {
			portNo = i;
		    }
		}
	    }
	    if (compStr != null && compStr.length() > 0) {
		for (int i = 0; i < VALID_COMPRESS.length; i++) {
		    if (VALID_COMPRESS[i].equalsIgnoreCase(compStr)) {
			compressNo = i;
		    }
		}
	    }
	    if (sizeStr != null && sizeStr.length() > 0) {
		for (int i = 0; i < VALID_SIZES.length; i++) {
		    if (VALID_SIZES[i].equalsIgnoreCase(sizeStr)) {
			sizeNo = i;
		    }
		}
	    }
	}
	capFormat = new javax.media.format.VideoFormat(
				VIDEO_COMPRESS[compressNo],
				DEFAULT_DIMENSIONS[sizeNo],
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				getRate());
	svCap = new XILCapture(this);

	portControl = new LocalPortControl(this, VALID_PORTS, portNo);
	rateControl = new RateControl(this, (float)DEFAULT_RATE, 1f, 30f);
	qualityControl = new LocalQualityControl(this,
						((float)DEFAULT_QUALITY/100f),
						0.01f, 0.62f);

	formatControl = new LocalFormatControl(this);

	controls = new Object[4];
	controls[0] = portControl;
	controls[1] = rateControl;
	controls[2] = qualityControl;
	controls[3] = formatControl;
    }

    public Object getDataType() {
	return Format.byteArray;
    }

    public void setTransferHandler(BufferTransferHandler th) {
	transferHandler = th;
    }

    public void connect() throws IOException {
	synchronized (SunVideoLock) {
	    if (inUse) {
		throw new IOException("Capture device in use");
	    } else
		inUse = true;
	    connected = false;
	    if (!doConnect()) {
		inUse = false;
		throw new IOException("Could not connect to capture device");
	    }
	    connected = true;
	}

	// Following only needed while the control frame hack is present...
	if (CONTROL_PANEL_HACK)
	    doControlPanelHack();
	// Preceding only needed while the control frame hack is present...
    }

    private boolean doConnect() {
	//	System.err.println("SunVideoSourceStream.doConnect");
	if (!svCap.connect(cardNo, portNo))
	    return false;
	setSize(sizeNo);		// set the scale
	setCompress(compressNo);	// set the compression

	data = new byte[maxDataSize];	// prime the data field for push
	nextSequence = 1;		// reset in case it's a reconnect
	return true;
    }

    synchronized void disconnect() {
	//	System.err.println("SunVideoSourceStream.disconnect");
	if (started) {
	    try {
		stop();
	    } catch (IOException ioe) {
	    }
	}
	synchronized (SunVideoLock) {
	    connected = false;
	    svCap.disconnect();
	    pt = null;
	    inUse = false;
	}

	// Following only needed while the control frame hack is present...
	if(CONTROL_PANEL_HACK && controlFrame != null) {
	    controlFrame.setVisible(false);
	    controlFrame.removeAll();
	    controlFrame.dispose();
	    controlFrame = null;
	}
	// Preceding only needed while the control frame hack is present...
    }

    void start() throws IOException {
	//	System.err.println("SunVideoSourceStream.start");
	if (started)
	    return;
	if (!svCap.start()) {
	    //	System.err.println("SunVideoSourceStream.start failed");
	    throw (new IOException("SunVideoStart failed"));
	}
	synchronized (this) {
	    started = true;
	    
	    // Start the video call back polling thread
	    if (pt == null) {
		if ( /*securityPrivelege  && */ (jmfSecurity != null) ) {
		    String permission = null;
		    try {
			if (jmfSecurity.getName().startsWith("jmf-security")) {
			    permission = "thread";
			    jmfSecurity.requestPermission(mSecurity, clSecurity, argsSecurity,
							  JMFSecurity.THREAD);
			    mSecurity[0].invoke(clSecurity[0], argsSecurity[0]);
			    
			    permission = "thread group";
			    jmfSecurity.requestPermission(mSecurity, clSecurity, argsSecurity,
							  JMFSecurity.THREAD_GROUP);
			    mSecurity[0].invoke(clSecurity[0], argsSecurity[0]);
			} else if (jmfSecurity.getName().startsWith("internet")) {
			    PolicyEngine.checkPermission(PermissionID.THREAD);
			    PolicyEngine.assertPermission(PermissionID.THREAD);
			}
		    } catch (Throwable e) {
       			if (JMFSecurityManager.DEBUG) {
        	            System.err.println( "Unable to get " + permission +
	        		     	        " privilege  " + e);
                        }
			securityPrivelege = false;
			// TODO: Do the right thing if permissions cannot be obtained.
			// User should be notified via an event
		    }
		}

		if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
		    try {
			Constructor cons = jdk12CreateThreadAction.cons;

			pt = (PushThread) jdk12.doPrivM.invoke(
                                           jdk12.ac,
					   new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               PushThread.class,
                                               this
                                           })});


		    } catch (Exception e) {
			System.err.println("SunVideoSourceStream: exception when creating thread");
		    }
		} else {
		    pt = new PushThread(this);
		}

		if (pt != null) {
		    pt.start();
		}
	    }

	    if (formatControl != null) formatControl.getControlComponent().
							setEnabled(false);
	    
	}
    }

    void stop() throws IOException {
	//	System.err.println("SunVideoSourceStream.stop");
	started = false;
	svCap.stop();

	if (portControl != null) portControl.setEnabled(true);

	if (formatControl != null) formatControl.getControlComponent().
							setEnabled(true);
    }

    public void finalize() {
	if (connected)
	    disconnect();
    }

    boolean getStarted() {
	return started;
    }

    boolean getConnected() {
	return connected;
    }

    public Format getFormat() {
	//	System.err.println("SunVideoSourceStream.getFormat");
	return capFormat;
    }

    public Format[] getSupportedFormats() {
	//	System.err.println("SunVideoSourceStream.getSupportedFormats");
	return supported;
    }

    byte [] getData() {
	return data;
    }

    void setData(byte [] buf) {
	data = buf;
    }

    void pushData(int length) {
	this.length = length;
	if (transferHandler != null)
	    transferHandler.transferData(this);
    }

    public boolean willReadBlock() {
	return true;
    }


    public void read(Buffer buffer) {
	//	System.err.println("SunVideoSourceStream.read");
	if (!started) {
	    buffer.setDiscard(true);
	    length = 0;
	    return;
	}
	synchronized (readLock) {
	    if (length > 0) {
		byte [] outgoingData = data;
		Object incomingData = buffer.getData();
		if (incomingData instanceof byte[] &&
		    ((byte[])incomingData).length >= maxDataSize) {
		    data = (byte []) incomingData;
		} else {
		    data = new byte[maxDataSize];
		}
		buffer.setOffset(0);
		buffer.setData(outgoingData);
		buffer.setLength(length);
		buffer.setDiscard(false);
		buffer.setSequenceNumber(nextSequence++);
		buffer.setTimeStamp(timeStamp);
		buffer.setFlags(buffer.getFlags() |
				buffer.FLAG_SYSTEM_TIME |
				buffer.FLAG_KEY_FRAME |
				buffer.FLAG_LIVE_DATA);
		buffer.setFormat(capFormat);
	    } else
		buffer.setDiscard(true);
	    length = 0;
	}
    }

    public CaptureDeviceInfo getCaptureDeviceInfo() {
	// TODO - more useful descriptor of device
	return new CaptureDeviceInfo("SunVideo", locator, supported);
    }

    public void setRGBFormat(int inWidth, int inHeight,
			    int outWidth, int outHeight, int scanLine) {
	if (inWidth <= 0) inWidth = 640;	// default to NTSC
	if (inHeight <= 0) inHeight = 480;	// default to NTSC
	if (outWidth <= 0) outWidth = inWidth / VALID_SCALE[sizeNo];
	if (outHeight <= 0) outHeight = inHeight / VALID_SCALE[sizeNo];

	Dimension dim = new java.awt.Dimension(outWidth, outHeight);
	// media engine doesn't like NOT_SPECIFIED
	if (scanLine == Format.NOT_SPECIFIED)
	    scanLine = 3 * outWidth;
	maxDataSize = scanLine * outHeight;
	capFormat = new RGBFormat(dim, maxDataSize, Format.byteArray,
				  getRate(), // frame rate
				  24,
				  3, 2, 1, 3, scanLine,
				  Format.FALSE, // flipped
				  Format.NOT_SPECIFIED); // endian
	formatControl.setCurrentFormat(capFormat);
    }

    public void setJpegFormat(int inWidth, int inHeight,
			int outWidth, int outHeight, int quality) {
	if (inWidth <= 0) inWidth = 640;	// default to NTSC
	if (inHeight <= 0) inHeight = 480;	// default to NTSC
	if (outWidth <= 0) outWidth = inWidth / VALID_SCALE[sizeNo];
	if (outHeight <= 0) outHeight = inHeight / VALID_SCALE[sizeNo];

	Dimension dim = new java.awt.Dimension(outWidth, outHeight);
	if (quality > 60)
	    maxDataSize = 3 * outWidth * outHeight;
	else
	    maxDataSize = 2 * outWidth * outHeight;
	capFormat = new JPEGFormat(dim, maxDataSize, Format.byteArray,
				getRate(), qualityNo, JPEGFormat.NOT_SPECIFIED);

	formatControl.setCurrentFormat(capFormat);
    }

    public void setMpegFormat(int inWidth, int inHeight,
			int outWidth, int outHeight, int quality) {
	if (inWidth <= 0) inWidth = 640;	// default to NTSC
	if (inHeight <= 0) inHeight = 480;	// default to NTSC
	if (outWidth <= 0) outWidth = inWidth / VALID_SCALE[sizeNo];
	if (outHeight <= 0) outHeight = inHeight / VALID_SCALE[sizeNo];

	Dimension dim = new java.awt.Dimension(outWidth, outHeight);
	if (quality > 60)
	    maxDataSize = 3 * outWidth * outHeight;
	else
	    maxDataSize = 2 * outWidth * outHeight;
	capFormat = new VideoFormat(VideoFormat.MPEG, dim,
				    maxDataSize, Format.byteArray,
				    getRate()); // frame rate

	formatControl.setCurrentFormat(capFormat);
    }

    private void setPort(int port) {
	portNo = port;
	svCap.setPort(portNo);
    }
    
    private void setCompress(String compress) {
	if (compress != null && compress.length() > 0) {
	    for (int i = 0; i < VALID_COMPRESS.length; i++) {
		if (VALID_COMPRESS[i].equalsIgnoreCase(compress)) {
		    compressNo = i;
		    if (connected)
			setCompress(compressNo);
		}
	    }
	}
    }
    
    private void setCompress(int compress) {
	compressNo = compress;
	svCap.setCompress(VALID_COMPRESS[compressNo]);
	if (compress == RGB_COMPRESS) {
	    qualityControl.setEnabled(false);
	} else {
	    qualityControl.setEnabled(true);
	}
    }
 
    int getSize() {
	return sizeNo;
    }

    void setSize(Dimension size) {
	int scale = 1;

	// Handle both NTSC and PAL sizes
	if (size.width > 384)
	    scale = 1;
	else if (size.width >= 320)
	    scale = 2;
	else
	    scale = 4;

	for (int i = 0; i < VALID_SIZES.length; i++) {
	    if (VALID_SCALE[i] == scale) {
		sizeNo = i;
		if (connected)
		    setSize(sizeNo);
	    }
	}
    }

    void setSize(int size) {
	sizeNo = size;
	svCap.setScale(VALID_SCALE[sizeNo]);
    }

    float getSizeFloat() {
	return VALID_SIZES_FLOAT[sizeNo];
    }

    float [] getSizesFloat() {
	return VALID_SIZES_FLOAT;
    }

    // Assumes VALID_SIZES_FLOAT is largest to smallest
    void setSize(float size) {
	if (size > VALID_SIZES_FLOAT[0]) {
	    sizeNo = 0;
	} else {
	    for (int i = 1; i < VALID_SIZES_FLOAT.length; i++) {
		sizeNo = i;
		if (size > VALID_SIZES_FLOAT[i]) {
		    // Allow for the cases where size is not an exact match
		    if ((VALID_SIZES_FLOAT[i - 1] - size) <
					    (size - VALID_SIZES_FLOAT[i])) {
			sizeNo = i - 1;
			break;
		    }
		    break;
		}
	    }
	}
	svCap.setScale(VALID_SCALE[sizeNo]);
    }

    float getQuality() {
	return ((float) qualityNo / 100f);
    }
 
    void setQuality(float quality) {
	qualityNo = (int) ((quality * 100f) + 0.5f);
	svCap.setQuality(qualityNo);
	if ((capFormat != null) && (capFormat instanceof JPEGFormat)) {
	    capFormat = new JPEGFormat(
			(capFormat == null ? null : capFormat.getSize()), 
			maxDataSize, Format.byteArray, getRate(),
			qualityNo, JPEGFormat.NOT_SPECIFIED);
	}
    }
 
    float getRate() {
	if (rateNo == 30)
	    return 29.97f;	// NTSC standard broadcast frame rate
	return (float) rateNo;
    }
 
    void setRate(float rate) {
	rateNo = (int) (rate + 0.5);
	if (rateNo <= 0)
	    rateNo = 1;
	ptDelay = 1000 / rateNo;
	//System.err.println("SunVideoSourceStream.setRate() rate: " + rateNo);

	// Adjust frame rate in format
	if (capFormat != null) {
	    if (VideoFormat.JPEG.equals(capFormat.getEncoding())) {
		capFormat = new JPEGFormat(capFormat.getSize(), 
			maxDataSize, Format.byteArray, getRate(),
			qualityNo, JPEGFormat.NOT_SPECIFIED);
	    } else if (VideoFormat.RGB.equals(capFormat.getEncoding())) {
		capFormat = new RGBFormat(capFormat.getSize(), 
			maxDataSize, Format.byteArray,
			getRate(), // frame rate
			24,
			3, 2, 1, 3,
			((RGBFormat)capFormat).getLineStride(),
			Format.FALSE, // flipped
			Format.NOT_SPECIFIED); // endian
	    }else if (VideoFormat.MPEG.equals(capFormat.getEncoding())) {
		capFormat = new VideoFormat(VideoFormat.MPEG,
			capFormat.getSize(), 
			maxDataSize, Format.byteArray,
			getRate()); // frame rate

	    }
	    formatControl.setCurrentFormat(capFormat);
	}
    }
 
    public Format setFormat(javax.media.Format fmt) {

	if (fmt.equals(capFormat))
	    return capFormat;

	//System.err.println("SunVideoSourceStream.setFormat() format: " + fmt);
	javax.media.Format f = null;
	for (int i = 0; i < supported.length; i++) {
	    if (fmt.matches(supported[i]) &&
		(f = fmt.intersects(supported[i])) != null) {
		break;
	    }
	}

	if (f != null) {
	    VideoFormat format = (javax.media.format.VideoFormat)f;
	    if (format.getEncoding().equals(format.JPEG)) {
		setCompress("Jpeg");
	    } else {
		setCompress("RGB");
	    }
	    if (format.getFrameRate() !=
				javax.media.format.VideoFormat.NOT_SPECIFIED) {
		// rateControl will call back to setRate
		rateControl.setFrameRate(format.getFrameRate());
	    }
	    setSize(format.getSize());
	    if (!connected) {
		capFormat = format;
	    }
	}

	return capFormat;
    }

    // Following only needed while the control frame hack is present...
    private void doControlPanelHack() {
	if (controlFrame != null) {
	    controlFrame.setVisible(true);
	    return;
	}
	controlFrame = new Frame("SunVideo Controls");
	controlFrame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
		controlFrame.setVisible(false);
	    }
	});
	controlFrame.setLayout(new BorderLayout());
	Panel p = new Panel();
	p.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
	//	p.add(new LabelComponent("Port"));
	p.add(portControl.getControlComponent());
	//	p.add(new LabelComponent("Format"));
	p.add(formatControl.getControlComponent());
	controlFrame.add(p, "North");
	p = new Panel();
	p.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
	p.add(rateControl.getControlComponent());
	p.add(qualityControl.getControlComponent());
	controlFrame.add(p, "South");
	controlFrame.pack();
	controlFrame.setVisible(true);
    }
    // Preceding only needed while the control frame hack is present...

    /****************************************************************
     * Owned
     ****************************************************************/

    public Object getOwner() {
	return dataSource;
    }


    /****************************************************************
     * INNER CLASSES
     ****************************************************************/

    class LocalPortControl extends AtomicControlAdapter
    implements Owned, PortControl, ControlChangeListener, ItemListener {
	SunVideoSourceStream stream;
	String [] validPorts = null;
	int currentPort = 0;
	Panel panel = null;
	Label labelPort = null;
	Choice comboPort = null;

	/* Must match the order of VALID_PORTS = S-VHS, 1, 2 */
	int [] PC_PORTS = {	PortControl.SVIDEO,
				PortControl.COMPOSITE_VIDEO,
				PortControl.COMPOSITE_VIDEO_2
	};

	public LocalPortControl(SunVideoSourceStream stream, String[] ports,
				int current) {
	    super(new Choice(), false, null);
	    this.stream = stream;
	    this.validPorts = ports;
	    this.currentPort = current;
	    addControlChangeListener(this);
	}

	public synchronized CaptureDeviceInfo getCaptureDeviceInfo() {
	    return stream.getCaptureDeviceInfo();
	}

	public String getPort() {
	    return validPorts[currentPort];
	}

	public void setPort(String port) {
	    for (int i = 0; i < validPorts.length; i++) {
		if (validPorts[i].equalsIgnoreCase(port)) {
		    currentPort = i;
		    stream.setPort(currentPort);
		    informListeners();
		    return;
		}
	    }
	    System.err.println("SunVideoSourceStream.setPort() invalid port: "
				+ port);
	}

	public Component getControlComponent() {
	    if ( panel == null) {
		panel = new Panel(new BorderLayout(6, 6));
		labelPort = new Label("Port:", Label.RIGHT);
		panel.add(labelPort, BorderLayout.WEST);
		comboPort = (Choice)super.getControlComponent();
		comboPort.addItemListener(this);
		panel.add(comboPort, BorderLayout.CENTER);
		for (int i = 0; i < validPorts.length; i++) {
		    comboPort.add(validPorts[i]);
		}
		comboPort.select(validPorts[currentPort]);
		comboPort.addItemListener(this);
	    }

	    return panel;
	}

	/****************************************************************
	 * Owned
	 ****************************************************************/

	public Object getOwner() {
	    return stream.getOwner();
	}

	/****************************************************************
	 * PortControl
	 ****************************************************************/
 
	public int getPorts() {
	    return PC_PORTS[currentPort];
	}
 
	public int getSupportedPorts() {
	    int ports = 0;
	    for (int i = 0; i < PC_PORTS.length; i++) {
		ports |= PC_PORTS[i];
	    }
	    return ports;
	}
 
	public int setPorts(int ports) {
	    int port = -1;
	    for (int i = 0; i < PC_PORTS.length; i++) {
		if (ports == PC_PORTS[i]) {
		    currentPort = i;
		    stream.setPort(currentPort);
		    informListeners();
		    break;
		}
	    }

	    return PC_PORTS[currentPort];
	}

	/****************************************************************
	 * ControlChangeListener
	 ****************************************************************/
 
	public void controlChanged(ControlChangeEvent ce) {
	    if (comboPort != null) {
		comboPort.select(validPorts[currentPort]);
	    }
	}

	/****************************************************************
	 * ItemListener
	 ****************************************************************/
    
	public void itemStateChanged(ItemEvent ie) {
	    if (ie.getStateChange() == ItemEvent.SELECTED) {
		String port = comboPort.getSelectedItem();
		setPort(port);
	    }
	}

    }

    class RateControl extends FrameRateAdapter implements Owned {
	SunVideoSourceStream stream;

	public RateControl(SunVideoSourceStream stream,
				float def, float low, float hi) {
	    super(def, low, hi, true);
	    this.stream = stream;
	}

	/****************************************************************
	 * Owned
	 ****************************************************************/

	public Object getOwner() {
	    return stream.getOwner();
	}

	/****************************************************************
	 * FrameRateAdapter
	 ****************************************************************/

	public float getFrameRate() {
	    return stream.getRate();
	}

	public float setFrameRate(float rate) {
	    if (rate < min) rate = min;
	    if (rate > max) rate = max;
	    stream.setRate(rate);
	    return super.setFrameRate(stream.getRate());
	}

    }

    class LocalQualityControl extends QualityAdapter implements Owned {
	SunVideoSourceStream stream;

	public LocalQualityControl(SunVideoSourceStream stream,
				float def, float low, float hi) {
	    super(def, low, hi, true);
	    this.stream = stream;
	}

	// Until the adapter has a setEnabled method
	public void setEnabled(boolean b) {
	}

	/****************************************************************
	 * Owned
	 ****************************************************************/

	public Object getOwner() {
	    return stream.getOwner();
	}

	/****************************************************************
	 * QualityAdapter
	 ****************************************************************/

	public float getQuality() {
	    return stream.getQuality();
	}

	public float setQuality(float quality) {
	    if (quality > maxValue) quality = maxValue;
	    if (quality < minValue) quality = minValue;
	    stream.setQuality(quality);
	    return super.setQuality(stream.getQuality());
	}

	public boolean isTemporalSpatialTradeoffSupported() {
	    return false;
	}

    }

    class LocalFormatControl implements FormatControl, Owned {

	private SunVideoSourceStream stream;
	private VideoFormatChooser vfc;

	public LocalFormatControl(SunVideoSourceStream svss) {
	    this.stream = svss;
	}

	public void setCurrentFormat(Format fmt) {
	    if (vfc != null)
		vfc.setCurrentFormat((VideoFormat) fmt);
	}

	/****************************************************************
	 * Owned
	 ****************************************************************/

	public Object getOwner() {
	    return stream.getOwner();
	}

	/****************************************************************
	 * FormatControl
	 ****************************************************************/

	public Format getFormat() {
	    return stream.getFormat();
	}

	public Format setFormat(Format fmt) {
	    Format newfmt = stream.setFormat(fmt);
	    setCurrentFormat(newfmt);
	    return newfmt;
	}

	public Format [] getSupportedFormats() {
	    return stream.getSupportedFormats();
	}

	public boolean isEnabled() {
	    return XILCapture.isAvailable();
	}

	public void setEnabled(boolean enabled) {
	}

	public java.awt.Component getControlComponent() {
	    if (vfc == null) {
		vfc = new VideoFormatChooser(stream.getSupportedFormats(),
					(VideoFormat) stream.getFormat());
		if (started || !XILCapture.isAvailable())
		    vfc.setEnabled(false);
	    }
	    return vfc;
	}
    }

}

/**
 * This class used to be an inner class, which is the correct thing to do.
 * Changed it to a package private class because of jdk1.2 security.
 * For jdk1.2 and above applets, PushThread is created in a
 * privileged block using jdk12CreateThreadAction. jdk12CreateThreadAction
 * class is unable to create and instantiate an inner class 
 * in PushThread class
 */

class PushThread extends MediaThread {
    SunVideoSourceStream stream;

    public PushThread(SunVideoSourceStream stream) {
	super("SunVideoSourceStream PushThread");
	this.stream = stream;
	useVideoPriority();
    }

    public void run() {
	long prevtime = 0;
	long prevcktime = 0;		// for timing only
	long now = 0;
	long time = 0;
	long delay = stream.ptDelay;
	long prevdelay = stream.ptDelay;
	long prevPtDelay = stream.ptDelay;
	int frames = 0;
	byte [] data = null;
	//System.err.println("In PushThread.run()");
	while (stream.getConnected()) {
	    try {
		sleep(delay);
		//	yield();
		time += delay;
	    } catch (Exception e) {
	    }
	    if (stream.getStarted()) {
		synchronized(stream.readLock) {
		    data = stream.getData();
		    if (data.length < stream.maxDataSize) {
			data = new byte[stream.maxDataSize];
			stream.setData(data);
		    }
		    int result = 0;
		    try {
			result = stream.svCap.read(data, data.length);
		    } catch (IOException ioe) {
			if (stream.getStarted()) {
			    System.err.println(
					       "SunVideoSourceStream PushThread read() failed: "
					       + ioe.getMessage());
			}
			result = -1;
		    }
		    stream.timeStamp = stream.systemTimeBase.getNanoseconds();
		    if (result > 0) {
			stream.pushData(result);
		    }
		}
		frames++;			// for timing only
		// If just after a start or frame rate change,
		// reset time corrections
		if (prevtime == 0 || prevPtDelay != stream.ptDelay) {
		    prevtime = now;
		    prevcktime = now;
		    delay = prevdelay = prevPtDelay = stream.ptDelay;
		    frames = 0;
		} else {
		    long diff = now - prevtime;
		    long delta = diff - stream.ptDelay;
		    if (diff > stream.ptDelay) {
			delay = prevdelay - (delta > stream.ptDelay ? 1 : delta);
			if (delay < 5)
			    delay = 5;
		    } else if (diff < stream.ptDelay) {
			delay = prevdelay + (-delta > stream.ptDelay ? 1 : -delta);
		    }
		    prevdelay = delay;
		    //	if (frames >= rateNo) {
		    //	    long fdiff = now - prevcktime;
		    //	    System.err.println(
		    //		"SunVideoSourceStream PushThread "
		    //			+ frames + " frames in "
		    //			+ fdiff + " ms = "
		    //			+ (frames*1000f/fdiff) + " fps ("
		    //			+ (fdiff/frames) + " ms/f)");
		    //	    frames = 0;
		    //	    prevcktime = now;
		    //	}
		}
		prevtime = now;
	    } else {
		// Stopped, reset time corrections
		prevtime = 0;
		delay = prevdelay = prevPtDelay = stream.ptDelay;
	    }
	}
	// Stop the capturing process. 
    }
}

