/*
 * @(#)VFWSourceStream.java	1.46 03/04/24
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.vfw;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.IOException;
import javax.media.*;
import javax.media.protocol.*;
import com.sun.media.protocol.*;
import com.sun.media.*;
import javax.media.format.*;
import javax.media.control.*;
import com.sun.media.vfw.*;
import com.sun.media.ui.VideoFormatChooser;
import com.sun.media.util.WindowUtil;
import com.sun.media.controls.FrameRateAdapter;
import java.util.Vector;


/****************************************************************
 * SourceStream for the DataSource
 ****************************************************************/

public class VFWSourceStream
extends BasicSourceStream
implements PushBufferStream, Runnable,
    ActionListener, FormatControl, Owned {
    
    private MediaLocator   locator = null;
    private int            capHandle = 0;
    Integer                cbHandleLock = new Integer(0);
    private int            cbHandle = 0;
    private VideoFormat    capFormat = null;
    private float          capFrameRate = 0;
    private int            capPreviewRate = 33; // in millis
    private int            capWidth = -1;
    private int            capHeight = -1;
    private byte []        data = null;
    private int            maxDataSize = 1;
    BufferTransferHandler  transferHandler = null;
    private int            length = 0;
    private int            nativeParentWindow = 0; // Native window
    private static String STRING_CF = "Custom Format...";
    private static String STRING_SD = "Video Source...";

    private Integer        lockInit = new Integer(1);
    private Integer        vfwReqLock = new Integer(2);
    private int            vfwRequest = -1;

    private static final int REQ_START = 1;
    private static final int REQ_STOP  = 2;
    private static final int REQ_FORMATDIALOG = 3;
    private static final int REQ_SOURCEDIALOG = 4;
    private static final int REQ_DISCONNECT = 5;
    private static final int REQ_UPDATECAPTURESETUP = 6;
    private static final int REQ_MONITOR = 7;
    
    private boolean        started = false;
    private boolean        connected = false;
    private boolean        connectedOK = false;
    private Frame          capFrame = null;

    public static int      MAX_CARDS = 10;
    public static boolean  DEBUG = false;
    private int            cardNo = 0;
    
    private static boolean [] inUse = new boolean[MAX_CARDS];
    private static Integer VFWLock = new Integer(0);
    Integer readLock = new Integer(1);
    Integer waitForStop = new Integer(2);
    boolean doneReading = false;
    
    boolean waitingToStop = false;
    long lastTimeStamp = 0;
    long startTimeStamp = 0;
    long [] resultTimeStamp = new long[1];
    SystemTimeBase systemTimeBase = new SystemTimeBase();
    
    private Panel controlPanel = null;
    private Control [] controls = null;

    private static final String DEFAULT_PORT = "default";
    private boolean hasDlgVideoFormat = false;
    private boolean hasDlgVideoSource = false;
    private boolean hasOverlay = false;
    private boolean monitorEnabled = false;
    private float defaultFrameRate = 15.0f;
    private float requestedFrameRate = 15.0f;
    private PushThread pt = null;
    private TransferDataThread tdt = null;
    private VideoFormat defaultFormat = null;
    private VideoFormat requestedFormat = null;
    private VideoFormat fullFormat = null;
    private CaptureDeviceInfo cdi = null;
    CircularBuffer bufferQ = new CircularBuffer(8);

    private float FRAME_RATES [] = {
	1f, 2f, 5f, 7.5f, 10f, 12.5f, 15f, 20f, 24f, 25f, 30f
    };

    static {
	for (int i = 0; i < MAX_CARDS; i++)
	    inUse[i] = false;
    }
    
    public VFWSourceStream(MediaLocator ml) {
	super(new ContentDescriptor(ContentDescriptor.RAW),
	      LENGTH_UNKNOWN);
	this.locator = ml;
	cardNo = getCardNo(ml);
    }

    CaptureDeviceInfo getCaptureDeviceInfo() {
	if (cdi == null) {
	    String url = "vfw://" + cardNo;

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
		cdi = autoDetect(cardNo);
	    }
	}

	return cdi;
    }

    public static CaptureDeviceInfo autoDetect(int cardNo) {
	CaptureDeviceInfo cdi = null;
	try {
	    cdi = new VFWDeviceQuery(cardNo);
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

    static int getCardNo(MediaLocator locator) {
	int cardNo = 0;
	String remainder = locator.getRemainder();
	if (remainder != null && remainder.length() > 0) {
	    while (remainder.length() > 1 && remainder.charAt(0) == '/')
		remainder = remainder.substring(1);
	    try {
		Integer integer = Integer.valueOf(remainder);
		if (integer != null)
		    cardNo = integer.intValue();
	    } catch (Throwable t) {
	    }
	}
	return cardNo;
    }
    
    public Object getDataType() {
	return Format.byteArray;
    }

    public void setTransferHandler(BufferTransferHandler th) {
	transferHandler = th;
    }

    public void connect(Format format) throws IOException {
	synchronized (VFWLock) {
	    /*
	    try {
		String prop = System.getProperty("vfw.popup");
		if (prop != null)
		    disableFormatDialog = true;
	    } catch (Throwable t) {
		if (t instanceof ThreadDeath)
		    throw (ThreadDeath)t;
	    }
	    */
	    if (connected)
		return;
	    if (inUse[cardNo]) {
		//System.err.println("Card in use");
		throw new IOException("Capture device in use");
	    } else
		inUse[cardNo] = true;
	    connected = false;
	    if (format != null)
		requestedFormat = (VideoFormat) format;
	    
	    //System.err.println("Requested format is " + requestedFormat);
	    Thread initThread = new Thread(this, "VFW Request Thread");
	    initThread.start();
	    
	    if (!connected) {
		//synchronized (lockInit) {
		    try {
			//System.err.println("Waiting to finish connect");
			while (!connected) {
			    Thread.currentThread().sleep(50);
			    Thread.currentThread().yield();
			}
		    } catch (InterruptedException ie) {
		    }
		    //}
	    }

	    if (!connectedOK) {
		inUse[cardNo] = false;
		connected = false;
		throw new IOException("Could not connect to capture device");
	    } else
		updateComponents();
	}
    }

    private boolean doConnect() {
	//System.err.println("Before creating window");
	nativeParentWindow = VFWCapture.createWindow("Cap Parent");
	//System.err.println("After creating window");
	capHandle = VFWCapture.capCreateCaptureWindow("Test",
							  nativeParentWindow,
							  0, 0, 320, 240,
							  VFWCapture.getNextID());
	if (capHandle == 0) {
	    VFWCapture.destroyWindow(nativeParentWindow);
	    return false;
	}
	
	cbHandle = VFWCapture.createFrameCallback(capHandle);
	    
	if (cbHandle == 0 || !VFWCapture.capDriverConnect(capHandle, cardNo)) {
	    //System.err.println("capDriverConnect failed");
	    if (cbHandle != 0)
		VFWCapture.destroyFrameCallback(capHandle, cbHandle);
	    VFWCapture.destroyWindow(capHandle);
	    //capFrame.dispose();
	    VFWCapture.destroyWindow(nativeParentWindow);
	    return false;
	}

	CapDriverCaps cdc = new CapDriverCaps();
	VFWCapture.capDriverGetCaps(capHandle, cdc);

	CaptureParms cp = new CaptureParms();
	VFWCapture.capCaptureGetSetup(capHandle, cp);
	//System.err.println("Capture Params = " + cp);
	// Allow windows events to occur
	cp.fYield = true;
	// Dont capture audio by default
	cp.fCaptureAudio = false;
	// Dont abort on mouse clicks
	cp.fAbortLeftMouse = false;
	cp.fAbortRightMouse = false;
	// Dont prompt user to start
	cp.fMakeUserHitOKToCapture = false;

	if (cdc.fHasDlgVideoFormat) {
	    hasDlgVideoFormat = true;
	}
	if (requestedFormat == null) {
	    Dimension size = new Dimension(320, 240);
	    requestedFormat = new RGBFormat(size,
					    size.width * size.height * 3,
					    Format.byteArray,
					    requestedFrameRate,
					    24,
					    3, 2, 1,
					    3, size.width * 3,
					    RGBFormat.TRUE,
					    RGBFormat.NOT_SPECIFIED);
		
	}

	// Default frame rate requested is 15 fps.
	if (cp.dwRequestMicroSecPerFrame == 0)
	    cp.dwRequestMicroSecPerFrame = 66667;
	defaultFrameRate = 1000000F / cp.dwRequestMicroSecPerFrame;
	if (requestedFormat.getFrameRate() > 0 &&
	    requestedFormat.getFrameRate() < 61)
	    requestedFrameRate = requestedFormat.getFrameRate();
	cp.dwRequestMicroSecPerFrame = (int) (1000000F / requestedFrameRate);
	//FRAME_RATES[0] = requestedFrameRate;
	cp.wNumVideoRequested = 15;
	VFWCapture.capCaptureSetSetup(capHandle, cp);
	VFWCapture.capCaptureGetSetup(capHandle, cp);
	if (cp.dwRequestMicroSecPerFrame != 0)
	    capFrameRate = 1000000F / cp.dwRequestMicroSecPerFrame;
	else
	    capFrameRate = requestedFrameRate;

	if (cdc.fHasDlgVideoSource)
	    hasDlgVideoSource = true;
	if (cdc.fHasOverlay) {
	    VFWCapture.capOverlay(capHandle, monitorEnabled);
	    hasOverlay = true;
	} else {
	    VFWCapture.capPreview(capHandle, monitorEnabled);
	    VFWCapture.capPreviewRate(capHandle, 33);
	    hasOverlay = false;
	}
	
	VFWCapture.capSetWindowPos(capHandle, 0, 0, 320, 240);
	BitMapInfo bmi = new BitMapInfo();
	
	VFWCapture.capGetVideoFormat(capHandle, bmi);

	defaultFormat = bmi.createVideoFormat(Format.byteArray, capFrameRate);
	bmi = new BitMapInfo(requestedFormat);
	//System.err.println("Setting bmi = " + bmi);
	VFWCapture.capSetVideoFormat(capHandle, bmi);
	VFWCapture.capGetVideoFormat(capHandle, bmi);
	//System.err.println("Result of setting = " + bmi);
	capFormat = bmi.createVideoFormat(Format.byteArray);

	VideoFormat frOnly = new VideoFormat(null, null,
					     Format.NOT_SPECIFIED,
					     null,
					     capFrameRate);

	fullFormat = (VideoFormat) capFormat.intersects(frOnly);
	maxDataSize = bmi.biSizeImage;
	data = new byte[maxDataSize];

	return true;
    }

    void updateCaptureSetup() {
	CaptureParms cp = new CaptureParms();
	VFWCapture.capCaptureGetSetup(capHandle, cp);
	if (requestedFrameRate < 1)
	    requestedFrameRate = 1;
	cp.dwRequestMicroSecPerFrame = (int) (1000000F / requestedFrameRate);
	VFWCapture.capCaptureSetSetup(capHandle, cp);
	VFWCapture.capCaptureGetSetup(capHandle, cp);
	if (cp.dwRequestMicroSecPerFrame != 0)
	    capFrameRate = 1000000F / cp.dwRequestMicroSecPerFrame;
	updateFormatChooser();
    }

    void updateFormatChooser() {
    	if (formatChooser != null) {
	    formatChooser.setCurrentFormat(capFormat);
	    formatChooser.setFrameRate(capFrameRate);
	}
    }

    void requestUpdateCaptureSetup() {
	synchronized (vfwReqLock) {
	    vfwRequest = REQ_UPDATECAPTURESETUP;
	    vfwReqLock.notifyAll();
	}
    }
    
    synchronized void disconnect() {
	if (started) {
	    try {
		stop();
	    } catch (IOException ioe) {
	    }
	}
	connected = false;
	synchronized (vfwReqLock) {
	    vfwRequest = REQ_DISCONNECT;
	    vfwReqLock.notifyAll();
	}
	
	while (pt != null) {
	    try {
		wait(50);
	    } catch (InterruptedException ie) {
	    }
	}
	inUse[cardNo] = false;
	synchronized (bufferQ) {
	    bufferQ.notifyAll();
	}
	synchronized (readLock) {
	    readLock.notifyAll();
	}
    }

    private void doDisconnect() {
	VFWCapture.capCaptureAbort(capHandle);
	VFWCapture.stopFrameCallback(capHandle, cbHandle);
	synchronized (cbHandleLock) {
	    //VFWCapture.destroyFrameCallback(capHandle, cbHandle);
	    cbHandle = 0;
	}
	if (hasOverlay)
	    VFWCapture.capOverlay(capHandle, false);
	else
	    VFWCapture.capPreview(capHandle, false);
	VFWCapture.capDriverDisconnect(capHandle);
	synchronized (this) {
	    VFWCapture.destroyWindow(capHandle);
	    //capFrame.dispose();
	    VFWCapture.destroyWindow(nativeParentWindow);
	    notifyAll();
	}
    }

    void start() throws IOException {
	//System.err.println("@@@@ Incoming start 1/2");
	//if (started)
	//    return;
	//System.err.println("@@@@ Incoming start 2/2");
	started = true;
	updateComponents();
	synchronized (this) {
	    if (!connected) {
		started = false;
		return;
	    }
	    synchronized (bufferQ) {
		bufferQ.reset();
	    }
	    // Start the video call back polling thread
	    if (pt == null) {
		pt = new PushThread(this);
		pt.start();
	    }
	    synchronized (pt) {
		pt.buffer = null;
		pt.data = null;
		pt.dataBytes = 0;
	    }
	    
	    if (tdt == null) {
		tdt = new TransferDataThread(this);
		tdt.start();
	    }
	    // Send a request to the vfw thread to start capture
	    synchronized (vfwReqLock) {
		vfwRequest = REQ_START;
		vfwReqLock.notifyAll();
	    }
	}
    }

    private void handleFormatChange() {
	// Check if capture format changed
	VideoFormat newFormat = null;
	VideoFormat frOnly = null;
	
	// If we have a new format request....
	if ( requestedFormat != null ) {
	    //System.err.println("handleFormatChange: req = " + requestedFormat);
	    BitMapInfo bmi = new BitMapInfo(requestedFormat);
	    //System.err.println("handleFormatChange: try bmi = " + bmi);
	    // Try setting the format
	    VFWCapture.capSetVideoFormat(capHandle, bmi);
	    // What did it really set it to?
	    VFWCapture.capGetVideoFormat(capHandle, bmi);
	    //System.err.println("handleFormatChange: got bmi = " + bmi);
	    // This is the final format
	    capFormat = bmi.createVideoFormat(Format.byteArray);
	    if (requestedFormat.getFrameRate() > 0 &&
                requestedFormat.getFrameRate() < 61)
                requestedFrameRate = requestedFormat.getFrameRate();
	    requestedFormat = null;
	    maxDataSize = bmi.biSizeImage;	    
	    data = new byte[maxDataSize];
	    capWidth = bmi.biWidth;
	    capHeight = bmi.biHeight;
	}

	// Did the frame rate change in the format chooser?
	if (formatChooser != null) {
	    float chooserFrameRate = formatChooser.getFrameRate();
	    if (Math.abs(chooserFrameRate - capFrameRate) > 0.45)
		requestedFrameRate = chooserFrameRate;
	}

	if (requestedFrameRate != Format.NOT_SPECIFIED) {
	    updateCaptureSetup();
	    requestedFrameRate = Format.NOT_SPECIFIED;
	}

	frOnly = new VideoFormat(null,
				 null,
				 Format.NOT_SPECIFIED,
				 null,
				 capFrameRate);

	newFormat = (VideoFormat) capFormat.intersects(frOnly);

	if ( fullFormat == null ||
	     !newFormat.equals(fullFormat)) {
	    fullFormat = newFormat;
	}
	updateFormatChooser();
    }
    
    private void doStartCapture() {
	handleFormatChange();
	// Start the capture
	CapStatus cs = new CapStatus();
	VFWCapture.capGetStatus(capHandle, cs);
	if (cs.fCapturingNow)
	    return;
	VFWCapture.startFrameCallback(capHandle, cbHandle);
	VFWCapture.capCaptureSequenceNoFile(capHandle);
	startTimeStamp = systemTimeBase.getNanoseconds();
    }

    void stop() throws IOException {
	started = false;

	synchronized (vfwReqLock) {
	    vfwRequest = REQ_STOP;
	    waitingToStop = true;
	    vfwReqLock.notifyAll();
	}
	
	synchronized (waitForStop) {
	    while (waitingToStop) {
		try {
		    waitForStop.wait();
		} catch (InterruptedException ie) {
		}
	    }
	}
	
	updateComponents();
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

    int getCapHandle() {
	return capHandle;
    }

    int getCBHandle() {
	return cbHandle;
    }

    byte [] getData() {
	return data;
    }

    void pushData(int length) {
	this.length = length;
	if (transferHandler != null)
	    transferHandler.transferData(this);
    }

    Object checkDataAllocation(Buffer buffer) {
	Object data;
	if (buffer instanceof ExtBuffer &&
	    ((ExtBuffer)buffer).isNativePreferred()) {

	    data = ((ExtBuffer)buffer).getNativeData();
	    if (data == null || ((NBA)data).getSize() < maxDataSize)
		data = new NBA(byte[].class, maxDataSize);
	    ((ExtBuffer)buffer).setNativeData((NBA) data);
	} else {
	    data = buffer.getData();
	    if (data == null || !(data instanceof byte[]) ||
		((byte[])data).length < maxDataSize) {
		data = new byte[maxDataSize];
		buffer.setData(data);
	    }
	}
	return data;
    }

    public void read(Buffer buffer) {
	synchronized (readLock) {
	    synchronized (bufferQ) {
		if (bufferQ.canRead()) {
		    // Get the available captured buffer
		    Buffer captureBuffer = bufferQ.read();
		    buffer.copy(captureBuffer, true); // swap data objects
		    buffer.setOffset(0);
		    buffer.setFormat(fullFormat);
		    buffer.setFlags(Buffer.FLAG_KEY_FRAME | Buffer.FLAG_SYSTEM_TIME |
				    Buffer.FLAG_LIVE_DATA);
		    /*
		    byte [] outgoingData = (byte[]) captureBuffer.getData();
		    captureBuffer.setData(checkDataAllocation(buffer.getData()));
		    // Copy attributes of captured buffer to outgoing
		    buffer.setData(outgoingData);
		    buffer.setOffset(0);
		    buffer.setLength(captureBuffer.getLength());
		    buffer.setTimeStamp(captureBuffer.getTimeStamp());
		    buffer.setFormat(fullFormat);
		    buffer.setFlags(Buffer.FLAG_KEY_FRAME | Buffer.FLAG_SYSTEM_TIME);
		    */
		    // Release buffer to buffer queue
		    bufferQ.readReport();
		} else
		    buffer.setDiscard(true);
	    }
	    doneReading = true;
	    readLock.notifyAll();
	}
    }

    public Object [] getControls() {
	if (controls == null) {
	    controls = new Control[2];

	    controls[0] = (FormatControl)this;
	    controls[1] = new MC();
	}
	return controls;
    }

    /****************************************************************
     * FormatControl
     ****************************************************************/

    public Format getFormat() {
	if (fullFormat == null)
	    fullFormat = (VideoFormat) getSupportedFormats()[0];
	return fullFormat;
    }

    public Format [] getSupportedFormats() {
	if (cdi == null)
	    cdi = getCaptureDeviceInfo();
	if (cdi == null)
	    return new Format[0];
	return cdi.getFormats();
    }

    public Format setFormat(Format in) {
	if (!(in instanceof VideoFormat))
	    return null;
	Format matched = BasicPlugIn.matches(in, getSupportedFormats());
	if (matched != null)
	    requestedFormat = (VideoFormat) in;
	if (connected && !started) {
	    handleFormatChange();
	    return fullFormat;
	} else {
	    return matched;
	}
    }

    public void setEnabled(boolean value) {
	// ignore
    }

    public boolean isEnabled() {
	return true;
    }

    Choice formatChoice = null;
    Choice frameRateChoice = null;
    Panel controlComponent = null;
    Button formatDialog = null;
    Button sourceDialog = null;
    VideoFormatChooser formatChooser = null;

    private void updateComponents() {
	if (formatChoice != null) 
	    formatChoice.setEnabled( !started );
	if (formatChooser != null)
	    formatChooser.setEnabled( connected && !started );
	if (formatDialog != null)
	    formatDialog.setEnabled( connected && !started );
	if (sourceDialog != null)
	    sourceDialog.setEnabled( connected && !started );
	if (frameRateChoice != null)
	    frameRateChoice.setEnabled( !started );
	if (cbMonitorEnable != null)
	    cbMonitorEnable.setEnabled( !started );
    }

    public Component getControlComponent() {
	if (controlComponent == null) {
	    controlComponent = new Panel( new BorderLayout() );
	    controlComponent.setName("Capture Control");
	    Format [] formats = getSupportedFormats();
	    formatChooser = new VideoFormatChooser(formats, (VideoFormat) getFormat(),
						   FRAME_RATES);
	    controlComponent.add("North", formatChooser);

	    formatDialog = new Button(STRING_CF);
	    
	    sourceDialog = new Button(STRING_SD);
	    sourceDialog.addActionListener(this);

	    Panel southPanel = new Panel();
	    southPanel.add(formatDialog);
	    southPanel.add(sourceDialog);

	    controlComponent.add("South", southPanel);
	    
	    updateComponents();
	    updateFormatChooser();
	    formatDialog.addActionListener(this);
	}
	return controlComponent;
    }

    public Object getOwner() {
	return this;
    }

    /****************************************************************
     * ActionListener
     ****************************************************************/
    
    public synchronized void actionPerformed(ActionEvent ae) {
	String source = ae.getActionCommand();
	if (source.equals(STRING_CF)) {
	    synchronized (vfwReqLock) {
		vfwRequest = REQ_FORMATDIALOG;
		vfwReqLock.notifyAll();
	    }
	} else if (source.equals(STRING_SD)) {
	    synchronized (vfwReqLock) {
		vfwRequest = REQ_SOURCEDIALOG;
		vfwReqLock.notifyAll();
	    }
	}
    }
    
    /****************************************************************
     * this.Thread to do VFW requests.
     ****************************************************************/
    
    public void run() {
	boolean wmQuit = false;
	int peekResult = 1;
	
	if (doConnect()) {
	    synchronized (lockInit) {
		connected = true;
		connectedOK = true;
		//System.err.println("Notifying doneConnect");
		lockInit.notifyAll();
	    }
	} else {
	    synchronized (lockInit) {
		connected = true;
		connectedOK = false;
		lockInit.notifyAll();
		return;
	    }
	}
	
	while (connected) {
	    while (vfwRequest < 0) {
		try {
		    Thread.currentThread().yield();
		    Thread.currentThread().sleep(50);
		} catch (InterruptedException ie) {
		}
		//System.err.println("Peeking..");
		
		peekResult = VFWCapture.peekWindowLoop(nativeParentWindow);
		if (peekResult == 0)
		    wmQuit = true;
		
	    }
	    int cpVFWRequest = vfwRequest;
	    vfwRequest = -1;
	    switch (cpVFWRequest) {
		case REQ_STOP:
		    if (DEBUG) System.err.println("capture - Stopping");
		    VFWCapture.stopFrameCallback(capHandle, cbHandle);
		    VFWCapture.capCaptureStop(capHandle);
		    
		    // Wait for atmost 5 seconds for capture to stop
		    int i = 0;
		    CapStatus cs = new CapStatus();
		    while (i < 100) {
			VFWCapture.capGetStatus(capHandle, cs);
			if (!cs.fCapturingNow) {
			    if (DEBUG) System.err.println("capture - Stopped");
			    break;
			}
			try {
			    Thread.currentThread().sleep(50);
			} catch (InterruptedException ie) {
			}
			
			i++;
		    }
		    waitingToStop = false;
		    synchronized (waitForStop) {
			waitForStop.notifyAll();
		    }
		    break;
		case REQ_START:
		    if (DEBUG) System.err.println("capture - Starting");
		    doStartCapture();
		    if (DEBUG) System.err.println("capture - Started");
		    break;
		case REQ_DISCONNECT:
		    if (DEBUG) System.err.println("capture - Disconnecting");
		    break;
		case REQ_FORMATDIALOG:
		    // Minimize monitor if its not enabled
		    if (!monitorEnabled)
			VFWCapture.showWindow(nativeParentWindow, 1, -1, -1);

		    // Invoke the dialog
		    VFWCapture.capDlgVideoFormat(capHandle);

		    // Monitor back to previous state
		    setMonitorEnabled(monitorEnabled);
		    
		    BitMapInfo bmi = new BitMapInfo();
		    VFWCapture.capGetVideoFormat(capHandle, bmi);
		    
		    Format newFormat =
			bmi.createVideoFormat(Format.byteArray, requestedFrameRate);
		    if (!newFormat.equals(capFormat)) {
			requestedFormat = (VideoFormat) newFormat;
			maxDataSize = requestedFormat.getMaxDataLength();
			data = new byte[maxDataSize];
		    }
		    updateFormatChooser();
		    break;
		case REQ_SOURCEDIALOG:
		    // Minimize monitor if its not enabled
		    if (!monitorEnabled)
			VFWCapture.showWindow(nativeParentWindow, 1, -1, -1);

		    // Invoke the dialog
		    VFWCapture.capDlgVideoSource(capHandle);

		    // Monitor back to previous state
		    setMonitorEnabled(monitorEnabled);
		    break;
		case REQ_UPDATECAPTURESETUP:
		    updateCaptureSetup();
		    break;
	        case REQ_MONITOR:
		    if (hasOverlay) {
			VFWCapture.capOverlay(capHandle, monitorEnabled);
		    } else {
			VFWCapture.capPreview(capHandle, monitorEnabled);
			VFWCapture.capPreviewRate(capHandle, capPreviewRate);
		    }
	    }
	}

	doDisconnect();
	if (DEBUG) System.err.println("capture - Disconnected");
	
	while (!wmQuit) {
	    try {
		Thread.currentThread().yield();
		Thread.currentThread().sleep(50);
	    } catch (InterruptedException ie) {
	    }
	    //System.err.println("Peeking...");
	    peekResult = VFWCapture.peekWindowLoop(nativeParentWindow);
	    if (peekResult == 0)
		wmQuit = true;
	}
	pt = null;
    }

    private Checkbox cbMonitorEnable = null;
    private Panel monitorComponent = null;
    private TextField textPreviewRate = null;
    
    Component getMonitorControlComponent() {
	if (monitorComponent == null) {
	    monitorComponent = new Panel();
	    monitorComponent.setLayout( new BorderLayout() );
	    cbMonitorEnable = new Checkbox("Video Monitor");
	    cbMonitorEnable.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent ie) {
		    setMonitorEnabled(cbMonitorEnable.getState());
		}
	    } );
	    Panel panelPreviewRate = new Panel( new BorderLayout() );
	    panelPreviewRate.add("East", new Label("frames/sec"));
	    
	    textPreviewRate = new TextField("30");
	    textPreviewRate.addActionListener( new ActionListener() {
		public void actionPerformed(ActionEvent ae) {
		    try {
			float previewRate = Float.valueOf(textPreviewRate.getText()).floatValue();
			if (previewRate < 0.1f)
			    previewRate = 0.1f;
			else if (previewRate > 30f)
			    previewRate = 30f;
			setMonitorPreviewRate(previewRate);
		    } catch (Exception e) {
		    }
		}
	    } );
	    panelPreviewRate.add("Center", textPreviewRate);
	    monitorComponent.add("West", cbMonitorEnable);
	    monitorComponent.add("Center", panelPreviewRate);
	}
	return monitorComponent;
    }

    boolean setMonitorEnabled(boolean value) {
	if (value) {
	    if (nativeParentWindow != 0)
		VFWCapture.showWindow(nativeParentWindow, 2,
				      capWidth, capHeight);
	    monitorEnabled = true;
	} else {
	    if (nativeParentWindow != 0)
		VFWCapture.showWindow(nativeParentWindow, 0, -1, -1);
	    monitorEnabled = false;
	}
	synchronized (vfwReqLock) {
	    vfwRequest = REQ_MONITOR;
	    vfwReqLock.notifyAll();
	}
	return value;
    }

    float setMonitorPreviewRate(float rate) {
	if (rate > 30)
	    rate = 30f;
	else if (rate < 0.1)
	    rate = 0.1f;
	int millis = (int) (1000 / rate);
	capPreviewRate = millis;
	synchronized (vfwReqLock) {
	    vfwRequest = REQ_MONITOR;
	    vfwReqLock.notifyAll();
	}
	return rate;
    }
    
    /****************************************************************
     * INNER CLASSES
     ****************************************************************/

    class MC implements MonitorControl, Owned {

	public MC() {
	}

	public Component getControlComponent() {
	    return getMonitorControlComponent();
	}

	public float setPreviewFrameRate(float value) {
	    return setMonitorPreviewRate(value);
	}

	public boolean setEnabled(boolean value) {
	    return setMonitorEnabled(value);
	}

	public Object getOwner() {
	    return VFWSourceStream.this;
	}
    }

    class PushThread extends Thread {
	VFWSourceStream stream;

	public PushThread(VFWSourceStream stream) {
	    super("VFW VideoPollThread");
	    this.stream = stream;
	}
	
	/*
	public void run2() {
	    int handle = getCapHandle();
	    int cbHandle = getCBHandle();
	    int time = 0;
	    byte [] data = null; //getData();
	    //System.err.println("In PushThread.run()");
	    while (getConnected()) {
		try {
		    sleep(10);
		    yield();
		    time += 10;
		} catch (Exception e) {
		}
		if (getStarted()) {
		    data = checkDataAllocation(data);
		    synchronized (VFWSourceStream.this.cbHandleLock) {
			if ((cbHandle = getCBHandle()) != 0) {
			    int result = VFWCapture.getAvailableData(
							     handle,
							     cbHandle,
							     data,
							     data.length,
							     resultTimeStamp);
			    if (result > 0) {
				lastTimeStamp = startTimeStamp +
				    (resultTimeStamp[0] * 1000000);
				synchronized (bufferQ) {
				    if (!bufferQ.canWrite()) {
					// Discard an old one
					bufferQ.read();
					bufferQ.readReport();
				    }
				    Buffer incoming = bufferQ.getEmptyBuffer();
				    byte [] temp = data;
				    data = checkDataAllocation(incoming.getData());
				    incoming.setData(temp);
				    incoming.setOffset(0);
				    incoming.setTimeStamp(lastTimeStamp);
				    incoming.setLength(result);
				    bufferQ.writeReport();
				    bufferQ.notifyAll();
				}
			    }
			}
		    }
		}
	    }
	}
	*/
	
	public Object data = null;
	public long dataBytes = 0;
	public Buffer buffer = null;

	public void run() {
	    int handle = getCapHandle();
	    int cbHandle = getCBHandle();
	    int time = 0;
	    //System.err.println("In PushThread.run()");
	    while (getConnected()) {
		try {
		    sleep(10);
		    yield();
		    time += 10;
		} catch (Exception e) {
		}
		synchronized (this) {
		    if (getStarted()) {
			
			if (buffer == null) {
			    synchronized (bufferQ) {
				if (!bufferQ.canWrite()) {
				    // Discard an old one
				    bufferQ.read();
				    bufferQ.readReport();
				}
				buffer = bufferQ.getEmptyBuffer();
			    }
			    data = checkDataAllocation(buffer);
			    if (data instanceof NBA)
				dataBytes = ((NBA)data).getNativeData();
			    else
				dataBytes = 0;
			}
			
			
			synchronized (VFWSourceStream.this.cbHandleLock) {
			    if ((cbHandle = getCBHandle()) != 0) {
				int result = VFWCapture.getAvailableData(
							     handle,
							     cbHandle,
							     data,
							     dataBytes,
							     maxDataSize,
							     resultTimeStamp);
				if (result > 0) {
				    lastTimeStamp = startTimeStamp +
					(resultTimeStamp[0] * 1000000);
				    synchronized (bufferQ) {
					buffer.setOffset(0);
					buffer.setTimeStamp(lastTimeStamp);
					buffer.setLength(result);
					bufferQ.writeReport();
					bufferQ.notifyAll();
					buffer = null;
				    }
				}
			    }
			}
		    }
		}
	    }
	}
    }

    class TransferDataThread extends Thread {
	VFWSourceStream stream;

	public TransferDataThread(VFWSourceStream stream) {
	    super("VFW TransferDataThread");
	    this.stream = stream;
	}

	public void run() {
	    while (getConnected()) {
		synchronized (bufferQ) {
		    while (!bufferQ.canRead() && getConnected()) {
			try {
			    bufferQ.wait(250);
			} catch (InterruptedException ie) {
			}
		    }
		}

		synchronized (stream.readLock) {
		    stream.doneReading = false;
		}

		if (bufferQ.canRead() && getConnected() && getStarted()) {
		    pushData(1);
		}

		synchronized (stream.readLock) {
		    if (!doneReading) {
			try {
			    stream.readLock.wait(1000);
			} catch (InterruptedException ie2) {
			}
		    }
		}
	    }
	}
    }
}

