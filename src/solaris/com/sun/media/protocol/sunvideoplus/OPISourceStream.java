/*
 * @(#)OPISourceStream.java	1.24 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.sunvideoplus;

import java.awt.*;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.Vector;
import java.util.Date;
import javax.media.*;
import javax.media.protocol.*;
import com.sun.media.protocol.*;
import javax.media.format.*;
import javax.media.format.VideoFormat;
import javax.media.format.JPEGFormat;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;
import javax.media.control.*;
import com.sun.media.controls.*;
import com.sun.media.ui.*;
import com.sun.media.util.MediaThread;

import java.security.*;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import com.sun.media.util.jdk12;
import com.sun.media.util.jdk12CreateThreadAction;
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
 * The SourceStream can be accessed with the URL sunvideoplus:
 * The URL has been extended to allow selecting some of the options:
 * sunvideoplus://card/port/compression/size/signal where
 *	card = the sunvideoplus card to use (default 0) when multiple
 *		cards are installed.
 *	port = port to use (default 1), s-vhs, 1, or 2.
 *	compression = h261, h263, rgb, yuv or jpeg (default h261).
 *	size = fcif (full), cif, or qcif (default cif). Actual frame size
 *		depends on whether the camera is NTSC or PAL.
 *	signal = source signal (default NTSC), NTSC or PAL
 ****************************************************************/

public class OPISourceStream
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
    OPICapture	   opiCap = null;
    private VideoFormat    capFormat = null;
    private javax.media.Format[] supported = null;
    SystemTimeBase systemTimeBase = new SystemTimeBase();

    static private javax.media.Format[] supportedNTSC;
    static private javax.media.Format[] supportedPAL;
    static private javax.media.Format[] supportedCommon;

    // Following only needed while the control frame hack is present...
    private static final boolean CONTROL_PANEL_HACK = false;
			   // set to false to eliminate the
			   // control frame at connect.
    private Frame	   controlFrame = null;
    // Preceding only needed while the control frame hack is present...

    private static Integer OPILock = new Integer(0);
    Integer readLock = new Integer(1);

    private boolean        started = false;
    private boolean        connected = false;
    private boolean        connectedOK = false;
    private boolean	   inUse = false;

    private int            cardNo = 0;
    private static String [] VALID_PORTS = { "S-VHS", "1", "2" };
    private static final int DEFAULT_PORT = 1;
    private int		   portNo = DEFAULT_PORT;
    
    // Exclude YUV until com/sun/media/video/colorspace/NativeYUVToRGB
    // supports interleaved, specifically YUV_YUYV.
    private static String [] VALID_COMPRESS =
				{ "H261", "H263", "RGB",
						  "YUV",
						  "JPEG" };
    private static String [] VIDEO_COMPRESS = {
				javax.media.format.VideoFormat.H261,
				javax.media.format.VideoFormat.H263,
				javax.media.format.VideoFormat.RGB,
				javax.media.format.VideoFormat.YUV,
				javax.media.format.VideoFormat.JPEG
    };			// must match order of VALID_COMPRESS
    private static final int DEFAULT_COMPRESS = 0;
    private static final int H261_COMPRESS = 0;	// Must match H261 above
    private static final int H263_COMPRESS = 1;	// Must match H263 above
    private static final int RGB_COMPRESS = 2;	// Must match RGB above
    private static final int YUV_COMPRESS = 3;	// Must match RGB above
    private int		   compressNo = DEFAULT_COMPRESS;

    private static String [] VALID_SIGNAL = { "NTSC", "PAL" };
    private static final int DEFAULT_SIGNAL = 0;
    private static final int PAL_SIGNAL = 1;	// Must match PAL above
    private int		   signalNo = DEFAULT_SIGNAL;

    // NOTE that sizes must be largest to smallest for some logic to work
    private static String [] VALID_SIZES = { "FCIF", "CIF", "QCIF" };
    private static float [] VALID_SIZES_FLOAT = { 1.0f, 0.5f, 0.25f };
    private static int [] VALID_SCALE = { 1, 2, 4 };
    private static final int DEFAULT_SIZE = 1;
    private static float SIZE_GRANULARITY = 0.25f;
    private int		   sizeNo = DEFAULT_SIZE;

    private static final int DEFAULT_RATE = 30;
    private int		   rateNo = DEFAULT_RATE;

    private static final int DEFAULT_QUALITY = 50;
    private int		   qualityNo = DEFAULT_QUALITY;

    private static final int LO_BITRATE = 100;
    private static final int HI_BITRATE = 2500;
    private static final int DEFAULT_BITRATE = 2000;
    private int		   bitrateNo = DEFAULT_BITRATE;
    private static final int H263_LO_BITRATE = 1;
    private static final int H263_HI_BITRATE = 200;
    private static final int H263_DEFAULT_BITRATE = 60;

    private LocalPortControl portControl = null;
    private SignalControl signalControl = null;
    private RateControl rateControl = null;
    private LocalQualityControl qualityControl = null;
    private BitRateControl bitrateControl = null;
    private LocalFormatControl formatControl = null;

    private float preferredFrameRate = 30.0f;
    private PushThread pt = null;
    
    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege=false;
    private Method mSecurity[] = new Method[1];
    private Class clSecurity[] = new Class[1];
    private Object argsSecurity[][] = new Object[1][0];

    public OPISourceStream(DataSource ds) {
	super(new ContentDescriptor(ContentDescriptor.RAW),
	      LENGTH_UNKNOWN);
	this.dataSource = ds;
	this.locator = ds.getLocator();
	cardNo = 0;
	String remainder = locator.getRemainder();
	if (remainder != null && remainder.length() > 0) {
	    while (remainder.length() > 1 && remainder.charAt(0) == '/')
		remainder = remainder.substring(1);
	    String cardStr, portStr, compStr, signalStr, sizeStr;
	    portStr = null;		// assume no port specified
	    compStr = null;		// assume no compress specified
	    signalStr = null;		// assume no signal specified
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
			remainder = remainder.substring(off + 1);
			// Now see if there's a signal specified
			off = remainder.indexOf('/');
			if (off == -1) {
			    sizeStr = remainder;
			} else {
			    sizeStr = remainder.substring(0, off);
			    signalStr = remainder.substring(off + 1);
			}
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
	    if (signalStr != null && signalStr.length() > 0) {
		for (int i = 0; i < VALID_SIGNAL.length; i++) {
		    if (VALID_SIGNAL[i].equalsIgnoreCase(signalStr)) {
			signalNo = i;
		    }
		}
		signalToFormat(signalNo);
	    } else {
		signalToFormat(-1);
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
				getSizeDimension(),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				getRate());
	opiCap = new OPICapture(this);

	portControl = new LocalPortControl(this, VALID_PORTS, portNo);
	signalControl = new SignalControl(this, VALID_SIGNAL, signalNo);
	rateControl = new RateControl(this, (float)DEFAULT_RATE, 1f, 30f);
	qualityControl = new LocalQualityControl(this,
						((float)DEFAULT_QUALITY/100f),
						0.0f, 1.0f);
	bitrateControl = new BitRateControl(this, DEFAULT_BITRATE,
						LO_BITRATE, HI_BITRATE);

	formatControl = new LocalFormatControl(this);

	controls = new Object[5];
	controls[0] = portControl;
	controls[1] = rateControl;
	controls[2] = qualityControl;
	controls[3] = bitrateControl;
	controls[4] = formatControl;
    }

    public Object getDataType() {
	return Format.byteArray;
    }

    public void setTransferHandler(BufferTransferHandler th) {
	transferHandler = th;
    }

    public void connect() throws IOException {
	synchronized (OPILock) {
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

	    // Following only needed while the control frame hack is present...
	    if (CONTROL_PANEL_HACK)
		doControlPanelHack();
	    // Preceding only needed while the control frame hack is present...
	}
    }

    private boolean doConnect() {
	//	System.err.println("OPISourceStream.doConnect");
	if (!opiCap.connect(cardNo, portNo))
	    return false;
	setSize(sizeNo);		// set the scale
	setSignal(signalNo);		// set the signal format
	setCompress(compressNo);	// set the compression

	data = new byte[maxDataSize];	// prime the data field for push
	nextSequence = 1;		// reset in case it's a reconnect
	return true;
    }

    synchronized void disconnect() {
	//	System.err.println("OPISourceStream.disconnect");
	if (started) {
	    try {
		stop();
	    } catch (IOException ioe) {
	    }
	}
	synchronized (OPILock) {
	    connected = false;
	    opiCap.disconnect();
	    if (pt != null)
		pt.timeToQuit();
	    pt = null;
	    inUse = false;

	    // Following only needed while the control frame hack is present...
	    if(CONTROL_PANEL_HACK && controlFrame != null) {
		controlFrame.setVisible(false);
		controlFrame.removeAll();
		controlFrame.dispose();
		controlFrame = null;
	    }
	    // Preceding only needed while the control frame hack is present...
	}
    }

    void start() throws IOException {
	//	System.err.println("OPISourceStream.start");
	if (started)
	    return;
	if (!opiCap.start()) {
	    //	System.err.println("OPISourceStream.start failed");
	    throw (new IOException("OPIStart failed"));
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
			System.err.println("OPISourceStream: exception when creating thread");
		    }
		} else {
		    pt = new PushThread(this);
		}

		if (pt != null) {
		    pt.start();
		}
	    }
	    
	    if (signalControl != null) signalControl.setEnabled(false);

	    if (formatControl != null) formatControl.getControlComponent().
							setEnabled(false);
	}
    }

    void stop() throws IOException {
	//	System.err.println("OPISourceStream.stop");
	started = false;
	opiCap.stop();

	if (signalControl != null) signalControl.setEnabled(true);

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
	//	System.err.println("OPISourceStream.getFormat");
	return capFormat;
    }

    public Format[] getSupportedFormats() {
	//	System.err.println("OPISourceStream.getSupportedFormats");
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
	//	System.err.println("OPISourceStream.read");
	if (!started) {
	    buffer.setDiscard(true);
	    length = 0;
	    //	return 0;
	    return;
	}
	synchronized (readLock) {
	    int copyLength = length;
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
		buffer.setSequenceNumber(nextSequence++);
		buffer.setDiscard(false);
		buffer.setTimeStamp(timeStamp);
		buffer.setFlags(buffer.getFlags() |
				buffer.FLAG_SYSTEM_TIME |
				buffer.FLAG_KEY_FRAME |
				buffer.FLAG_LIVE_DATA);
		buffer.setFormat(capFormat);
	    } else
		buffer.setDiscard(true);
	    length = 0;
	    //	return copyLength;
	    return;
	}
    }

    public CaptureDeviceInfo getCaptureDeviceInfo() {
	// TODO - more useful descriptor of device
	return new CaptureDeviceInfo("SunVideoPlus device " + cardNo,
							locator, supported);
    }

    private void setSizes(int inWidth, int inHeight,
			    int outWidth, int outHeight, int scale) {
	for (int i = 0; i < VALID_SCALE.length; i++) {
	    if (scale == VALID_SCALE[i]) {
		if (sizeNo != i)
		    setSize(i);
		break;
	    }
	}
    }

    public void setRGBFormat(int inWidth, int inHeight,
			    int outWidth, int outHeight,
			    int scanLine, int scale) {
	if (inWidth <= 0) inWidth = 640;	// default to NTSC
	if (inHeight <= 0) inHeight = 480;	// default to NTSC
	if (outWidth <= 0) outWidth = inWidth / VALID_SCALE[sizeNo];
	if (outHeight <= 0) outHeight = inHeight / VALID_SCALE[sizeNo];

	Dimension dim = new java.awt.Dimension(outWidth, outHeight);
	// media engine doesn't like NOT_SPECIFIED
	if (scanLine == Format.NOT_SPECIFIED)
	    scanLine = 2 * outWidth;
	maxDataSize = scanLine * outHeight;
	capFormat = new RGBFormat(new java.awt.Dimension(outWidth, outHeight),
					maxDataSize, Format.byteArray,
				        getRate(), // frame rate
					16,
					0xF800, 0x7E0, 0x1F,
					2, scanLine,
					Format.FALSE, // flipped
					Format.NOT_SPECIFIED); // endian

	setSizes(inWidth, inHeight, outWidth, outHeight, scale);

	if (formatControl != null)
	    formatControl.setCurrentFormat(capFormat);
    }

    public void setYUVFormat(int inWidth, int inHeight,
			    int outWidth, int outHeight,
			    int scanLine, int scale) {
	if (inWidth <= 0) inWidth = 640;	// default to NTSC
	if (inHeight <= 0) inHeight = 480;	// default to NTSC
	if (outWidth <= 0) outWidth = inWidth / VALID_SCALE[sizeNo];
	if (outHeight <= 0) outHeight = inHeight / VALID_SCALE[sizeNo];

	// media engine doesn't like NOT_SPECIFIED
	scanLine = 2 * outWidth;
	maxDataSize = scanLine * outHeight;

	// The image coming from the Osprey card is YVYU.
	capFormat = new YUVFormat(new java.awt.Dimension(outWidth, outHeight),
					maxDataSize, Format.byteArray,
				        getRate(), // frame rate
					YUVFormat.YUV_YUYV, scanLine,
					scanLine, 0, 3, 1);
	setSizes(inWidth, inHeight, outWidth, outHeight, scale);

	if (formatControl != null)
	    formatControl.setCurrentFormat(capFormat);
    }

    public void setH261Format(int inWidth, int inHeight,
			int outWidth, int outHeight,
			int quality, int scale) {
	setVideoFormat(VideoFormat.H261, inWidth, inHeight,
			outWidth, outHeight, quality, scale);
    }

    public void setH263Format(int inWidth, int inHeight,
			int outWidth, int outHeight,
			int quality, int scale) {
	setVideoFormat(VideoFormat.H263, inWidth, inHeight,
			outWidth, outHeight, quality, scale);
    }

    public void setJpegFormat(int inWidth, int inHeight,
			int outWidth, int outHeight,
			int quality, int scale) {
	if (inWidth <= 0) inWidth = 640;	// default to NTSC
	if (inHeight <= 0) inHeight = 480;	// default to NTSC
	if (outWidth <= 0) outWidth = inWidth / VALID_SCALE[sizeNo];
	if (outHeight <= 0) outHeight = inHeight / VALID_SCALE[sizeNo];

	if (quality > 60)
	    maxDataSize = 3 * outWidth * outHeight;
	else
	    maxDataSize = 2 * outWidth * outHeight;
	// Note that quality is changed from the 1-100 range to 1-255
	capFormat = new JPEGFormat(
				new java.awt.Dimension(outWidth, outHeight),
				maxDataSize, Format.byteArray,
				getRate(),
				qualityNo / 3,
				VideoFormat.NOT_SPECIFIED);
	setSizes(inWidth, inHeight, outWidth, outHeight, scale);

	if (formatControl != null)
	    formatControl.setCurrentFormat(capFormat);
    }

    public void setVideoFormat(String format, int inWidth, int inHeight,
			int outWidth, int outHeight,
			int quality, int scale) {
	if (inWidth <= 0) inWidth = 640;	// default to NTSC
	if (inHeight <= 0) inHeight = 480;	// default to NTSC
	if (outWidth <= 0) outWidth = inWidth / VALID_SCALE[sizeNo];
	if (outHeight <= 0) outHeight = inHeight / VALID_SCALE[sizeNo];

	if (quality > 60)
	    maxDataSize = 3 * outWidth * outHeight;
	else
	    maxDataSize = 2 * outWidth * outHeight;
	capFormat = new VideoFormat(format,
				new java.awt.Dimension(outWidth, outHeight),
					maxDataSize, Format.byteArray,
					getRate()); // frame rate

	setSizes(inWidth, inHeight, outWidth, outHeight, scale);

	if (formatControl != null)
	    formatControl.setCurrentFormat(capFormat);
    }

    public void setMpegFormat(int inWidth, int inHeight,
			int outWidth, int outHeight,
			int quality, int scale) {
	if (inWidth <= 0) inWidth = 640;	// default to NTSC
	if (inHeight <= 0) inHeight = 480;	// default to NTSC
	if (outWidth <= 0) outWidth = inWidth / VALID_SCALE[sizeNo];
	if (outHeight <= 0) outHeight = inHeight / VALID_SCALE[sizeNo];

	if (quality > 60)
	    maxDataSize = 3 * outWidth * outHeight;
	else
	    maxDataSize = 2 * outWidth * outHeight;
	capFormat = new VideoFormat(VideoFormat.MPEG,
				new java.awt.Dimension(outWidth, outHeight),
					maxDataSize, Format.byteArray,
					getRate()); // frame rate

	setSizes(inWidth, inHeight, outWidth, outHeight, scale);

	if (formatControl != null)
	    formatControl.setCurrentFormat(capFormat);
    }

    private void setPort(int port) {
	portNo = port;
	opiCap.setPort(portNo);
    }
 
    private void setSignal(int signal) {
	signalNo = signal;
	opiCap.setSignal(VALID_SIGNAL[signalNo]);
	signalToFormat(signalNo);
    }

    private void signalToFormat(int s) {
	if (s == -1) {
	    // support both NTSC and PAL until a choice is made
	    supported = new javax.media.Format[supportedCommon.length
						+ supportedNTSC.length
						+ supportedPAL.length];
	    System.arraycopy(supportedCommon, 0, supported, 0,
							supportedCommon.length);
	    System.arraycopy(supportedNTSC, 0, supported,
				supportedCommon.length, supportedNTSC.length);
	    System.arraycopy(supportedNTSC, 0, supported,
				supportedCommon.length + supportedNTSC.length,
							supportedPAL.length);
	} else if (signalNo == PAL_SIGNAL) {
	    // support PAL formats
	    supported = new javax.media.Format[supportedCommon.length
						+ supportedPAL.length];
	    System.arraycopy(supportedCommon, 0, supported, 0,
							supportedCommon.length);
	    System.arraycopy(supportedPAL, 0, supported,
				supportedCommon.length, supportedNTSC.length);
	} else {
	    // support NTSC formats
	    supported = new javax.media.Format[supportedCommon.length
						+ supportedNTSC.length];
	    System.arraycopy(supportedCommon, 0, supported, 0,
							supportedCommon.length);
	    System.arraycopy(supportedNTSC, 0, supported,
				supportedCommon.length, supportedNTSC.length);
	}
	if (formatControl != null)
	    formatControl.setSupportedFormats(supported);
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
	opiCap.setCompress(VALID_COMPRESS[compressNo]);
	if (compress == H263_COMPRESS) {
	    bitrateControl.setRange(H263_DEFAULT_BITRATE,
					H263_LO_BITRATE, H263_HI_BITRATE);
	    setBitRate(H263_DEFAULT_BITRATE);
	} else {
	    bitrateControl.setRange(DEFAULT_BITRATE,
					LO_BITRATE, HI_BITRATE);
	    setBitRate(DEFAULT_BITRATE);
	}
	// also test for YUV when it is supported
	if ((compress == RGB_COMPRESS) || (compress == YUV_COMPRESS)) {
	    qualityControl.setEnabled(false);
	    bitrateControl.setEnabled(false);
	} else {
	    qualityControl.setEnabled(true);
	    bitrateControl.setEnabled(true);
	}
    }
 
    int getSize() {
	return sizeNo;
    }

    Dimension getSizeDimension() {
	int scale = VALID_SCALE[sizeNo];
	int w = 640;
	int h = 480;
	if (signalNo == PAL_SIGNAL) {
	    w = 768;
	    h = 576;
	}
	if ((compressNo == H263_COMPRESS) || (compressNo == H261_COMPRESS)) {
	    w = 704;
	    h = 576;
	}
	return new Dimension(w / scale, h / scale);
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
	int prevsize = sizeNo;
	sizeNo = size;
	// size may be rejected for current compression
	if (!opiCap.setScale(VALID_SCALE[sizeNo]))
	    sizeNo = prevsize;
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
	opiCap.setScale(VALID_SCALE[sizeNo]);
    }

    float getQuality() {
	return ((float) qualityNo / 100f);
    }
 
    void setQuality(float quality) {
	qualityNo = (int) ((quality * 100f) + 0.5f);
	opiCap.setQuality(qualityNo);
	if ((capFormat != null) && (capFormat instanceof JPEGFormat)) {
	    // Note that quality is changed from the 1-100 range to 1-255
	    capFormat = new JPEGFormat(
			(capFormat == null ? null : capFormat.getSize()), 
			maxDataSize, Format.byteArray, getRate(),
			qualityNo / 3,
			Format.NOT_SPECIFIED);
	    if (formatControl != null)
		formatControl.setCurrentFormat(capFormat);
	}

    }
 
    int getBitRate() {
	return bitrateNo;
    }
 
    void setBitRate(int bitrate) {
	bitrateNo =  bitrate;
	opiCap.setBitRate(bitrateNo);
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
	opiCap.setFrameRate(rateNo);

	// Adjust frame rate in format
	if (capFormat != null) {
	    if (VideoFormat.JPEG.equals(capFormat.getEncoding())) {
		capFormat = new JPEGFormat(capFormat.getSize(), 
			maxDataSize, Format.byteArray, getRate(),
			qualityNo / 3, JPEGFormat.NOT_SPECIFIED);
	    } else if (VideoFormat.H261.equals(capFormat.getEncoding())
			|| VideoFormat.H263.equals(capFormat.getEncoding())
			|| VideoFormat.MPEG.equals(capFormat.getEncoding())) {
		capFormat = new VideoFormat(capFormat.getEncoding(),
			capFormat.getSize(),
			maxDataSize, Format.byteArray,
			getRate()); // frame rate
	    } else if (VideoFormat.YUV.equals(capFormat.getEncoding())) {
		capFormat = new YUVFormat(capFormat.getSize(),
			maxDataSize, Format.byteArray,
			getRate(), // frame rate
			YUVFormat.YUV_YUYV,
			((YUVFormat)capFormat).getStrideY(),
			((YUVFormat)capFormat).getStrideUV(),
			0, 3, 1);
	    } else if (VideoFormat.RGB.equals(capFormat.getEncoding())) {
		capFormat = new RGBFormat(capFormat.getSize(), 
			maxDataSize, Format.byteArray,
			getRate(), // frame rate
			16,
			0xF800, 0x7E0, 0x1F, 2,
			((RGBFormat)capFormat).getLineStride(),
			Format.FALSE, // flipped
			Format.NOT_SPECIFIED); // endian
	    }
	    formatControl.setCurrentFormat(capFormat);
	}
    }
 
    public Format setFormat(javax.media.Format fmt) {

	javax.media.Format f = null;
	for (int i = 0; i < supported.length; i++) {
	    if (fmt.matches(supported[i]) &&
		(f = fmt.intersects(supported[i])) != null) {
		break;
	    }
	}

	if (f != null) {
	    VideoFormat format = (javax.media.format.VideoFormat)f;
	    if (format.getEncoding().equals(format.H261)) {
		setCompress("H261");
	    } else if (format.getEncoding().equals(format.H263)) {
		setCompress("H263");
	    } else if (format.getEncoding().equals(format.JPEG)) {
		setCompress("Jpeg");
	    } else if (format.getEncoding().equals(format.YUV)) {
		setCompress("YUV");
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
	controlFrame = new Frame("OPI Controls");
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
	p.add(bitrateControl.getControlComponent());
	controlFrame.add(p, "South");
	controlFrame.pack();
	controlFrame.setVisible(true);
    }
    // Preceding only needed while the control frame hack is present...


    /****************************************************************
     * Define the static values for supported formats
     ****************************************************************/

    static {

        try {
            jmfSecurity = JMFSecurityManager.getJMFSecurity();
            securityPrivelege = true;
        } catch (SecurityException e) {
        }

	supportedCommon = new javax.media.Format[] {

	    // H.26x
		    // H.261 formats
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.H261,
				new Dimension(352, 288),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.H261,
				new Dimension(176, 144),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),

		    // H.263 formats
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.H263,
				new Dimension(352, 288),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.H263,
				new Dimension(176, 144),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),

	};

	supportedNTSC = new javax.media.Format[] {

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

		    // YUV formats
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.YUV,
				new Dimension(640, 480),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.YUV,
				new Dimension(320, 240),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.YUV,
				new Dimension(160, 120),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),

	};

	supportedPAL = new javax.media.Format[] {

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

		    // YUV formats
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.YUV,
				new Dimension(768, 576),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.YUV,
				new Dimension(384, 288),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),
		    new javax.media.format.VideoFormat(
				javax.media.format.VideoFormat.YUV,
				new Dimension(192, 144),
				javax.media.format.VideoFormat.NOT_SPECIFIED,
				Format.byteArray,
				javax.media.format.VideoFormat.NOT_SPECIFIED),

	};
    }

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
	OPISourceStream stream;
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

	public LocalPortControl(OPISourceStream stream, String[] ports,
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
	    System.err.println("OPISourceStream.setPort() invalid port: "
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

    class SignalControl extends AtomicControlAdapter
    implements Owned, ItemListener {
	OPISourceStream stream;
	String [] validSignals = null;
	int currentSignal;
	Panel panel = null;
	Label labelSignal = null;
	Choice comboSignal = null;

	public SignalControl(OPISourceStream stream, String [] valid,
				int current) {
	    super(new Choice(), false, null);
	    this.stream = stream;
	    validSignals = valid;
	    currentSignal = current;
	}


	/****************************************************************
	 * SignalControl
	 ****************************************************************/
 
	public String getSignal() {
	    return validSignals[currentSignal];
	}

	public String [] getValidSignals() {
	    return validSignals;
	}

	public void setSignal(String signal) {
	    for (int i = 0; i < validSignals.length; i++) {
		if (validSignals[i].equalsIgnoreCase(signal)) {
		    currentSignal = i;
		    stream.setSignal(currentSignal);
		    informListeners();
		    return;
		}
	    }
	    System.err.println(
			"OPISourceStream.setSignal() invalid Signal: "
			+ signal);
	}

	public Component getControlComponent() {
	    if ( panel == null) {
		panel = new Panel(new BorderLayout(6, 6));
		labelSignal = new Label("Signal:", Label.RIGHT);
		panel.add(labelSignal, BorderLayout.WEST);
		comboSignal = (Choice)super.getControlComponent();
		comboSignal.addItemListener(this);
		panel.add(comboSignal, BorderLayout.CENTER);
		for (int i = 0; i < validSignals.length; i++) {
		    comboSignal.add(validSignals[i]);
		}
		comboSignal.select(validSignals[currentSignal]);
		comboSignal.addItemListener(this);
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
	 * ItemListener
	 ****************************************************************/
 
	public void itemStateChanged(ItemEvent ie) {
	    if (ie.getStateChange() == ItemEvent.SELECTED) {
		String signal = comboSignal.getSelectedItem();
		setSignal(signal);
	    }
	}

    }

    class RateControl extends FrameRateAdapter implements Owned {
	OPISourceStream stream;

	public RateControl(OPISourceStream stream,
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
	OPISourceStream stream;

	public LocalQualityControl(OPISourceStream stream,
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

    class BitRateControl extends BitRateAdapter implements Owned {
	OPISourceStream stream;

	public BitRateControl(OPISourceStream stream,
				int def, int low, int hi) {
	    super(def, low, hi, true);
	    this.stream = stream;
	}

	public void setRange(int def, int low, int hi) {
	     min = low;
	     max = hi;
	     super.setBitRate(def);
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
	 * BitRateAdapter
	 ****************************************************************/

	public int getBitRate() {
	    return stream.getBitRate();
	}

	public int setBitRate(int newValue) {
	    if (newValue < min) newValue = min;
	    if (newValue > max) newValue = max;
	    stream.setBitRate(newValue);
	    return super.setBitRate(stream.getBitRate());
	}

	protected String getName() {
	    return "Bit Rate (KB)";
	}

    }

    class LocalFormatControl implements FormatControl, Owned {

	OPISourceStream stream;
	private VideoFormatChooser vfc = null;
	private Panel fPanel = null;

	public LocalFormatControl(OPISourceStream svpss) {
	    this.stream = svpss;
	}

	public void setCurrentFormat(VideoFormat fmt) {
	    if (vfc != null)
		vfc.setCurrentFormat((VideoFormat) fmt);
	}

	public void setSupportedFormats(Format[] fmts) {
	    if (vfc != null) {
		Vector vf = new Vector();
		for (int i = 0; i < fmts.length; i++) {
		    if (fmts[i] instanceof VideoFormat)
			vf.addElement(fmts[i]);
		}
		vfc.setSupportedFormats(vf);
	    }
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
	    return stream.setFormat(fmt);
	}

	public Format [] getSupportedFormats() {
	    return stream.getSupportedFormats();
	}

	public boolean isEnabled() {
	    return OPICapture.isAvailable();
	}

	public void setEnabled(boolean enabled) {
	}

	public java.awt.Component getControlComponent() {
	    if (vfc == null) {
		vfc = new VideoFormatChooser(stream.getSupportedFormats(),
					(VideoFormat) stream.getFormat());
		if (started || !OPICapture.isAvailable())
		    vfc.setEnabled(false);
		fPanel = new FormatPanel(vfc);
	    }
	    return fPanel;
	}
    }

    class FormatPanel extends Panel {

	private VideoFormatChooser vfc = null;

	FormatPanel(VideoFormatChooser vfc) {
		this.vfc = vfc;
		setLayout(new BorderLayout());
		//	Signal
		add(signalControl.getControlComponent(), BorderLayout.NORTH);
		//	VideoFormatChooser
		add(vfc, BorderLayout.CENTER);
	}

	public void setEnabled(boolean enabled) {
	    vfc.setEnabled(enabled);
	    signalControl.getControlComponent().setEnabled(enabled);
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
    OPISourceStream stream;
    boolean quit = false;

    public PushThread(OPISourceStream stream) {
	super("OPISourceStream PushThread");
	this.stream = stream;
	useVideoPriority();
    }

    public void run() {
	SystemTimeBase timeBase = new SystemTimeBase();
	long now = 0;
	long time = 0;
	byte [] data = null;
	//System.err.println("In PushThread.run()");
	while (stream.getConnected() && !quit) {
	    try {
		sleep(10);
		time += 10;
	    } catch (Exception e) {
	    }
	    if (stream.getStarted() && !quit) {
		synchronized(stream.readLock) {
		    data = stream.getData();
		    if (data.length < stream.maxDataSize) {
			data = new byte[stream.maxDataSize];
			stream.setData(data);
		    }
		    int result = 0;
		    try {
			result = stream.opiCap.read(data, data.length);
		    } catch (IOException ioe) {
			if (stream.getStarted()) {
			    System.err.println(
					       "OPISourceStream PushThread read() failed: "
					       + ioe.getMessage());
			}
			result = -1;
		    }
		    stream.timeStamp = stream.systemTimeBase.getNanoseconds();
		    if (result > 0) {
			stream.pushData(result);
		    }
		}
	    }
	}
	// Stop the capturing process. 
    }

    // Avoids having multiple PushThreads active when a disconnect and
    // connect occur during the sleep.
    public void timeToQuit() {
	quit = true;
    }
}
