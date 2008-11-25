/*
 * @(#)Jmpx.java	1.20 00/10/16
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.media.codec.video.jmpx;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.applet.Applet;
import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;
import javax.media.format.*;
import com.sun.media.*;
import com.sun.media.util.*;
import com.sun.media.controls.*;
import com.sun.media.renderer.video.*;
import javax.media.renderer.VideoRenderer;

import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;

/* import javax.media.renderer.video.Java2DWrap; */

/**
 * A utility class to drive the mpx mpeg decoder.
 */
public final class Jmpx extends BasicController implements Runnable {

    // A pipe to read commands from mpx.  mpxCmdPipe is for
    // use by mpx.  cmdPipe is for use by the Jmpx client.
    private PipedInputStream cmdPipe;
    protected PipedOutputStream mpxCmdPipe;
    protected boolean killCommandThread = false;
    private Vector listeners;
    private MpxThread mpxThread;
    private DataThread dataThread;
    private int audioRate = 0, audioChannels = 0;
    private boolean sourceIsOn = false;
    private Component monitor;
    private long lastStatTime = 0;
    private VideoRenderer renderer = null;
    private VideoFormat videoformat = null;
    
    protected JmpxAudio audio = null;

    public static boolean BIG_ENDIAN = true;
    public static boolean LITTLE_ENDIAN = false;
    
    private native boolean initMPX(Component c);
    private native void sendMPX(byte command[]);
    private native void broadcastMPX(byte command[]);
    private native void setStream(SourceStream m);
    private native int checkMpegFile(String file);
    private native int getMediaDuration(String file);
    private native int checkMpegStream(SourceStream m);
    private native boolean bigEndian();
    
    // Stream type
    public final static int	MpxStrm_11172 = (1<<0);
    public final static int	MpxStrm_VSEQ = (1<<1);
    public final static int	MpxStrm_ASEQ = (1<<2);
    public final static int     MpxStrm_Unknown = 0;

    // Audio modes and quality.
    public final static int	MpxAudioLeft = 01;
    public final static int	MpxAudioRight = 02;
    public final static int	MpxAudioStereo = 03;
    public final static int	MpxAudioLowQ = 010;
    public final static int	MpxAudioMedQ = 020;
    public final static int	MpxAudioHiQ = 030;

    // MPEG server commands
    protected final static int	MCMD_NULL = 0;
    protected final static int	MCMD_EXIT = 1;
    protected final static int	MCMD_OPENSRC = 2;
    protected final static int	MCMD_CLOSESRC = 3;
    protected final static int	MCMD_REENTER = 4;
    protected final static int	MCMD_PLAYCTR = 5;
    protected final static int	MCMD_PRESCTR = 6;
    protected final static int	MCMD_STREAM = 7;
    protected final static int	MCMD_SENDSTAT = 8;
    protected final static int	MCMD_STATUS = 9;
    protected final static int	MCMD_ACK = 10;
    protected final static int	MCMD_SETRSRC = 11;
    protected final static int	MCMD_CAPTURE = 12;
    protected final static int	MCMD_CDOP = 13;
    protected final static int	MCMD_TEST = 0xff;
    protected final static int	MCMD_QSIZE = 30;
    protected final static int	MCMD_QSTATS = 31;

    // Float to int multiplier
    protected final static double	MULF = (double)65535.0;	

    /* Command Flags */
    protected final static int	MCFL_SNDACK = (1<<0);
    protected final static int	MCFL_NOACK = (1<<1);
    protected final static int	MCFL_ORGMPX = (1<<2);
    protected final static int	MCFL_MPXRSV1 = (1<<16);

    /*	 MCMD_OPENSRC	type	*/
    protected final static int	MSC_NONE = 0;
    protected final static int	MSC_FNAME = 1;
    protected final static int	MSC_CDFILE = 2;
    protected final static int	MSC_NETWORK = 3;
    protected final static int	MSC_FDSCP = 4;
    protected final static int	MSC_JAVASTRM = 5;

    /*	 MCMD_REENTER	flags	*/
    protected final static int	MRE_FOFS = (1<<0);
    protected final static int	MRE_RELOFS = (1<<1);
    protected final static int	MRE_ASOPEN = (1<<2);
    protected final static int	MRE_STRMS = (1<<3);
    protected final static int	MRE_SEEKVSEQ = (1<<4);

    /*	 MCMD_PLAYCTR	action	*/
    public final static int	PC_PLAY = (1<<0);
    public final static int	PC_FWDSPEED = (1<<1);
    public final static int	PC_FWDSTEP = (1<<2);
    public final static int	PC_PAUSE = (1<<3);
    public final static int	PC_AUDMSK = 
    		(PC_PLAY | PC_PAUSE | PC_FWDSTEP);

    /* 	 MCMD_STREAM	stream */
    protected final static int	STRM_IGNOREID = 0x80;
    protected final static int	STRM_SBCOFF = 0x40;
    protected final static int	STRM_AUTOSBC = 0x20;
    protected final static int	STRM_IDBITS = 0x3f;

    /*	 MCMD_PRESCTR   which */
    protected final static int	PCTR_VMD = (1<<0);
    protected final static int	PCTR_AMD = (1<<1);
    protected final static int	PCTR_AVOL = (1<<2);
    protected final static int	PCTR_LUM = (1<<3);
    protected final static int	PCTR_SAT = (1<<4);
    protected final static int	PCTR_GAM = (1<<5);
    protected final static int	PCTR_LSG = 
    		(PCTR_LUM | PCTR_SAT | PCTR_GAM);

    /*	 MCMD_PRESCTR	vmd	Video Display Mode */
    protected final static int	VDM_NONE = 0;
    protected final static int	VDM_COL = 1;
    protected final static int	VDM_COLB = 2;
    protected final static int	VDM_COL8 = 3;

    protected static final int TRYSET_DONE = 0;
    protected static final int TRYSET_CANT = 1;
    protected static final int TRYSET_PASTEOM = 2;
    
    protected MediaThread cmdInputThread;
    protected int ackSeq;
    protected int strmType;
    protected float fwdSpeed = 1.0f;
    protected int width, height;
    protected int outWidth, outHeight;
    protected int zoom;
    protected float java_zoom;
    protected int cmap;
    protected boolean interleaved;
    protected boolean muted = false;
    protected boolean reloaded = false;
    protected float fps, kbps, loc, oldLoc;
    protected long time, size;
    protected long frames;
    private   long peer = 0;	/* Native Jmpx */
    protected int  possibleLength = -1;
    protected int  possibleWidth = -1;
    protected int  possibleHeight = -1;

    // Fast Blitter
    //protected boolean useFastBlt = true;
    //protected Class blitterClass = null;
    //protected int screenDepth;
    //protected Blitter blitter = null;
    //protected RGBFormat rgbFormat = null;
    //protected RGBData   rgbData   = null;
    //protected VidData   lastFrame = null;

    protected int useJavaRenderer = 1;

    // must be called rgbBuffer for native library to find it
    protected Object rgbBuffer = null;

    protected FrameRateControl frameRateControl = null;
    protected BitRateControl bitRateControl = null;
    protected GainControl gainControl = null;
    protected Control [] controls;
    protected int needYUVBuffer = 0;
    protected int needXBGR = 1;
    
/*
    protected YUVData yuvFrame = null;
    protected IndexColorData pseudoFrame = null;
    protected YUVToPseudo ditherer = null;
*/
    protected boolean windowAvailable = false;
    protected javax.media.protocol.DataSource source;
    protected TimeBase timeBase;

    protected String filename;
    protected SourceStream sourceStream;
    protected byte [] tempArray = new byte[65536];
    
    protected MemoryImageSource sourceImage = null;
    protected Image displayImage = null;
    int rMask = 0xFF;
    int gMask = 0xFF00;
    int bMask = 0xFF0000;

    protected Time knownDuration = Duration.DURATION_UNKNOWN;
    protected Time eomDuration = Duration.DURATION_UNKNOWN;
    protected Time requestedMediaTime = new Time(0);
    protected boolean mediaTimeChanged = false;
    protected boolean outputSizeSet = false;

    // CACHE
    protected int contentLength = -1;
    
    /* protected Object image = null; */


    /* some JMFSecurity stuff */
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
    }

    public Jmpx() {
	this(MpxStrm_Unknown);
    }


    public Jmpx(int type) {
	super();

	ackSeq = 0;
	strmType = type; 
    	width = 320; height = 240;
	outWidth = 320; outHeight = 240;
    	zoom = 1;
	java_zoom = (float) 1.0;
    	cmap = 0;
    	interleaved = true;
    	fps = 0; kbps = 0; loc = 0;
    	time = 0; size = 0;
    	frames = 0;
    	listeners = new Vector();
    }


    public void setSource(javax.media.protocol.DataSource source)
	throws IOException, IncompatibleSourceException {
	if (!(source instanceof javax.media.protocol.PullDataSource) && 
	    !(source instanceof javax.media.protocol.PushDataSource)) {
	    System.out.println("Unsupported data source: " + source);
	    throw new IncompatibleSourceException(this + ": doesn't support " + source);
	} else
	    this.source = source;
    }


    protected boolean isConfigurable() {
	return false;
    }


    public void finalize() throws Throwable {
	super.finalize();
	if (isInitialized())
	    close();
    }

    protected int readFromStream(int offset, int nbytes) {
	if (tempArray == null)
	    tempArray = new byte[65536];
	return readFromStream(tempArray, offset, nbytes);
    }

    protected int readFromStream(byte buf[], int offset, int nbytes) {
	if (sourceStream == null) {
	    sendEvent(new ConnectionErrorEvent(this, "No source stream specified!")); 
	    return -1;
	}

	try {
	    if (sourceStream instanceof PullSourceStream)
		return ((PullSourceStream)sourceStream).read(buf, offset, nbytes);

	    else if (sourceStream instanceof PushSourceStream)
		return readFromPushStream(buf, offset, nbytes);

	} catch (IOException e) {
	    // The stream is failing.  Throw an error event.
	    sendEvent(new ConnectionErrorEvent(this, e.getMessage())); 
	}

	return -1;
    }


    protected int readFromPushStream(byte buf[], int offset, int nbytes) 
	throws IOException {
	PushSourceStream pss = (PushSourceStream)sourceStream;
	boolean starved = false, paused = false;
	int n;
	long now;

	synchronized (this) {

	    while ((n = pss.read(buf, offset, nbytes)) == 0) {
		// Check to see if there's data available.

		if (!starved) {
		    // Try to wait for a second to see if data will arrive.
		    try {
			wait(1000);
		    } catch (InterruptedException e) {}

		    starved = true;

		} else {
		    // We have already waited for one sec and no data.
		    // We'll need to pause the player and throw the 
		    // restarting event.
		    super.stop();
		    pause();
		    sendEvent(new RestartingEvent(this, Started, 
				Prefetching, Started, getMediaTime()));
		    paused = true;

		    try {
			wait();
		    } catch (InterruptedException e) {}
		}
	    }
	}

	if (paused) {
	    // restart the player if the player had been paused.
	    sendEvent(new StartEvent(this, Prefetched, Started,
			     Started, getMediaTime(),
			     getTimeBase().getTime()));
	}

	// -1 signifies the end of stream.
	if (n == -1)
	    return 0;
	return n;
    }


    protected synchronized void notifyPushDataArrival() {
	notifyAll();
    }

    
    protected boolean doRealize() {

    	try {
    	    JMFSecurityManager.loadLibrary("jmutil");
    	    JMFSecurityManager.loadLibrary("jmmpx");
	} catch (UnsatisfiedLinkError e) {
	    return false;
	}

	// Initialize the segv trace.
	// Ema: The Segv handler causes a crash in Sol 2.6. Until we
	// get a new segv handler that works on 2.6, we will remove
	// this debug facility.  
	//new Segv();

	// First thing, check the byte order
	if (bigEndian()) {
	    BIG_ENDIAN = true;
	    LITTLE_ENDIAN = false;
	} else {
	    BIG_ENDIAN = false;
	    LITTLE_ENDIAN = true;
	}
	    
	if (source == null)
	    return false;

	mediaTimeChanged = false;

	startSource(true);
	
	// Determine the source type. 
	sourceStream = null;
	filename = null;
	if (source instanceof PullDataSource) {
	    PullDataSource pds = (PullDataSource)source;
	    URL url = null;
	    String protocol = null;
	    MediaLocator ml = pds.getLocator();
	    if (ml != null) {
		protocol = ml.getProtocol();
		try {
		    url = ml.getURL();
		} catch (MalformedURLException mue) {
		    url = null; // Could be a locator that's not a valid URL
		}
	    }
	    if (source instanceof com.sun.media.protocol.file.DataSource &&
	        protocol != null && protocol.equals("file") && url != null) {
		// It's file protocol.  We'll just allow mpx to read
		// directly from the file to maximize performance.
		filename = url.getFile();
	    } else { 
		// simply use the source stream from the DataSource.
		// We support only the first stream.
		SourceStream ss[] = pds.getStreams();
		if (ss.length >= 1)
		    sourceStream = ss[0];
		contentLength = (int) sourceStream.getContentLength();
		if (contentLength <= 0)
		    contentLength = -1;
	    }
	} else {
	    PushDataSource pds = (PushDataSource)source;
	    // We support only the first stream.
	    SourceStream ss[] = pds.getStreams();
	    if (ss.length >= 1) {
		sourceStream = ss[0];
		((PushSourceStream)sourceStream).setTransferHandler(new PushStreamHandler());
	    }
	}

	// Something's wrong with the DataSource.
	if (sourceStream == null && filename == null) {
	    System.err.println("No source stream");
	    return false;
	}

	// Check the stream type.
	if (!checkStreamType()) {
	    System.err.println("The data is not an MPEG stream.\n"); 
	    return false;
	}

	reloaded = true;

	// Assign the time base.
	timeBase = new MPXTimeBase();

	// Create the graphics components.
	monitor = createMonitor(width, height);

/*
	if (useFastBlt) {
	    // Check if we have a fast blitter
	    useFastBlt = false;
	    try {
		String javaVendor = "Sun";
		try {
		    // Check for 2.6 and (netscape or green), disable xil.
		    JMFSecurity.enablePrivilege.invoke(JMFSecurity.privilegeManager,
						       JMFSecurity.readPropArgs);
		    javaVendor =
			System.getProperty("java.vendor", "Sun").toLowerCase();
		} catch (Exception anyE) {
		}
		int threads = com.sun.media.renderer.video.VideoRenderer.getThreadType();
		int badOS = com.sun.media.renderer.video.VideoRenderer.getBadOS();
		if (!(badOS == 1 &&
		      (javaVendor.indexOf("etscape") > 0 || threads == 1))) {
		    blitterClass = Class.forName("com.sun.media.blitter.xil.Blitter");
		    if (blitterClass != null) {
			Blitter tryBlt = (com.sun.media.Blitter) blitterClass.newInstance();
			useFastBlt = true;
			tryBlt.close();
			tryBlt = null;
		    }
		}
	    } catch (Exception e) {
	    } catch (Error er) {
	    }
	    if (!useFastBlt) {
		try {
		    blitterClass = Class.forName("com.sun.media.blitter.xlib.Blitter");
		    if (blitterClass != null) {
			Blitter tryBlt = (com.sun.media.Blitter) blitterClass.newInstance();
			useFastBlt = true;
			tryBlt.close();
			tryBlt = null;
		    }
		} catch (Exception e) {
		} catch (Error er) {
		}
	    }
	    if (!useFastBlt) {
		try {
		    blitterClass = Class.forName("com.sun.media.blitter.directx.Blitter");
		    if (blitterClass != null) {
			Blitter tryBlt = (com.sun.media.Blitter) blitterClass.newInstance();
			useFastBlt = true;
			tryBlt.close();
			tryBlt = null;
		    }
		} catch (Exception e) {
		} catch (Error er) {
		}
	    }
	    if (!useFastBlt) {
		try {
		    blitterClass = Class.forName("com.sun.media.blitter.gdi.Blitter");
		    if (blitterClass != null) {
			Blitter tryBlt = (com.sun.media.Blitter) blitterClass.newInstance();
			useFastBlt = true;
			tryBlt.close();
			tryBlt = null;
		    }
		} catch (Exception e) {
		} catch (Error er) {
		}
	    }
	}
*/
	/*
	if (Toolkit.getDefaultToolkit().getColorModel() instanceof IndexColorModel) {
	    screenDepth = 8;
	    if (useFastBlt)
		needYUVBuffer = 1;
	} else {
	    screenDepth = 24;
	    needYUVBuffer = 0;
	}
	*/
	//screenDepth = 24;
	startSource(false);

	return true;
    }


    protected void abortRealize() {
	startSource(false);
    }


    protected boolean doPrefetch() {

	if (isInitialized())
	    return true;

	if (strmType != Jmpx.MpxStrm_VSEQ) {
	    // Try to grab the audio device.
	    boolean gotAudio;

	    // If the rate and channels are not yet known, initialize
	    // it to some format first to claim the audio device.  The
	    // native code will later re-initialize it to the valid format.
	    if (audioRate == 0 || audioChannels == 0)
		gotAudio = setAudio(22050, 1);
	    else
		gotAudio = setAudio(audioRate, audioChannels);

	    if (!gotAudio) {
		sendEvent(new AudioDeviceUnavailableEvent(this));
		if (strmType == Jmpx.MpxStrm_ASEQ)
		    return false;
	    }
	}

    	// Initialize and connect the command pipes
    	cmdPipe = new PipedInputStream();
    	mpxCmdPipe = new PipedOutputStream();

    	try {
	   cmdPipe.connect(mpxCmdPipe);
	} catch (IOException e) {
	   System.err.println("Failed to connect the command pipes " + e);
	   return false;
	}

// // 	mpxThread = new MpxThread(this);
// // 	dataThread = new DataThread(this);

// // 	// Call a native method to start the mpx process.
// // 	if (!initMPX(monitor))
// // 	    return false;

// // 	cmdInputThread = new MediaThread(this);
// // 	cmdInputThread.setName("Jmpx command input thread");

// // 	// The decoding threads are running at Minmum priority.
// // 	// Set this thread to one level over that.
// // 	cmdInputThread.useControlPriority();
// // 	cmdInputThread.start();


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
// 		} else if (jmfSecurity.getName().startsWith("internet")) {
// 		    PolicyEngine.checkPermission(PermissionID.THREAD);
// 		    PolicyEngine.assertPermission(PermissionID.THREAD);
// 		}
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
		Constructor cons = jdk12CreateThreadRunnableAction.cons;
		
		mpxThread = (MpxThread) jdk12.doPrivM.invoke(
                                           jdk12.ac,
					   new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               MpxThread.class,
                                               this
                                           })});


		dataThread = (DataThread) jdk12.doPrivM.invoke(
                                           jdk12.ac,
					   new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               DataThread.class,
                                               this
                                           })});
		
	    // Call a native method to start the mpx process.
	    if (!initMPX(monitor))
		return false;

	    cmdInputThread = (MediaThread) jdk12.doPrivM.invoke(
                                           jdk12.ac,
					   new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               MediaThread.class,
                                               this
                                           })});

	    cmdInputThread.setName("Jmpx command input thread");
	    
	    // The decoding threads are running at Minmum priority.
	    // Set this thread to one level over that.
	    cons = jdk12PriorityAction.cons;
	    jdk12.doPrivM.invoke(
				     jdk12.ac,
				     new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               cmdInputThread,
                                               new Integer(cmdInputThread.getControlPriority())
                                           })});

	    cmdInputThread.start();
	    } catch (Exception e) {
	    }
	} else {
	    mpxThread = new MpxThread(this);
	    dataThread = new DataThread(this);
	    
	    // Call a native method to start the mpx process.
	    if (!initMPX(monitor))
		return false;
	    
	    cmdInputThread = new MediaThread(this);
	    cmdInputThread.setName("Jmpx command input thread");
	    
	    // The decoding threads are running at Minmum priority.
	    // Set this thread to one level over that.
	    cmdInputThread.useControlPriority();
	    cmdInputThread.start();
	}
	return true;
    }


    protected void abortPrefetch() {
	// Free up mpx.
	closeMPX();
	reloaded = true;
	startSource(false);
    }


    protected void doStart() {

	// Reset EOM threshold flag. This is not thread safe 
	oldLoc = 0f;
	
	if (timeBase instanceof MPXTimeBase)
	    ((MPXTimeBase)timeBase).reset();
	startSource(true);

	if (reloaded) {
	    if (filename != null)
		openFile(filename);
	    else if (sourceStream != null)
		openStream(sourceStream);
	    reloaded = false;
	}

	if (!mediaTimeChanged) {
	    play();
	    return;
	}

	// Can we set the media time ?
	int returnVal = trySetMediaTime(requestedMediaTime);
	if (returnVal == TRYSET_DONE) {
	    play();
	    mediaTimeChanged = false;
	} else if (returnVal == TRYSET_CANT) {
	    super.stop();
	    sendEvent((StopEvent) new SeekFailedEvent(this, Started, Prefetched,
							  getTargetState(),
							  getMediaTime()));
	} else if (returnVal == TRYSET_PASTEOM) {
	    super.stop();
	    sendEvent(new EndOfMediaEvent(this, Started, Prefetched,
					  getTargetState(), getMediaTime()));
	}
    }


    public void stop() {
	super.stop();
	pause();
	startSource(false);
	if (timeBase instanceof MPXTimeBase)
	    ((MPXTimeBase)timeBase).reset();
	sendEvent((StopEvent)new StopByRequestEvent(this, Started,
						    Prefetched,
						    getTargetState(),
						    getMediaTime()));
    }


    public boolean startSource(boolean on) {
	if (sourceIsOn == on)
	    return true;
	try {
	    if (on)
		source.start();
	    else
		source.stop();
	} catch (Exception ge) {
	    System.err.println("Couldn't stop the data source");
	    return false;
	}
	sourceIsOn = on;
	return true;
    }


    protected void closeMPX() {
	if (isInitialized()) {
	    // Stop reading from the command pipe.
	    killCommandThread = true;

	    // Command mpx to exit.  Then free the associated native data
	    // structures.
	    exit();

	    try {
		mpxCmdPipe.flush();
		mpxCmdPipe.close();
		cmdPipe.close();
	    } catch (IOException e) {
		System.err.println("closing command pipe: " + e);
	    }
	}
    }


    public synchronized void doClose() {

	if (isInitialized()) {
	    windowAvailable = false;
	    closeMPX();

	    // finalize is not called as soon as it should.  We'll attempt
	    // to clear references to these resource-consuming objects
	    // in hope that the garbage collector will collect these
	    // right away...
/*
	    if (blitter != null) {
		blitter.close();
		blitter = null;
	    }
	    rgbBuffer = null;
	    yuvFrame = null;
	    pseudoFrame = null;
	    ditherer = null;
*/
	    mpxThread = null;
	    dataThread = null;
	    cmdInputThread = null;
	    monitor = null;

	}
	if (source != null) {
	    try {
		source.disconnect();
	    } catch (Exception e) {}
	}
    }


    public void setTimeBase(TimeBase tb) throws IncompatibleTimeBaseException {
	super.setTimeBase(tb);

	// We can't support any other time base except the ones
	// are based on JavaSound rendering.
	if (tb != null && tb != timeBase) {
	    /**
	     Allow this to silently go through so addController will be
	     slightly more useful.
	     --ivg
	    throw new IncompatibleTimeBaseException("the mpeg handler cannot handle the given timebase.");
	     */
	}
	timeBase = tb;
    }


    public TimeBase getTimeBase() {
	super.getTimeBase();
	return timeBase;
    }


    /**
     * This is base on a rough calculation.
     * Based on the current time and the offset into the media,
     * we can estimate the new offset given a new media time.
     */
    public void setMediaTime(Time t) {

	if ((getState() == Unrealized) || (getState() == Realizing))
	    throw new NotRealizedError("Cannot set media time on an unrealized controller");
	Time currentMediaTime = getMediaTime();
	requestedMediaTime = t;
	mediaTimeChanged = true;
	super.setMediaTime(t);
    }


    protected int trySetMediaTime(Time t) {

	Time duration = getDuration();
	double secGoto = t.getSeconds();
	double secDuration = duration.getSeconds();

	// Do we know the duration?
	if (duration != DURATION_UNKNOWN) {
	    // Are we seeking outside the media's duration?
	    if (secGoto > secDuration)
		return TRYSET_PASTEOM;
	} else if (secGoto != 0)
	    return TRYSET_CANT;
	
	// Streaming are handled differently.
	if (sourceStream != null) {
	    if (sourceStream instanceof Seekable) {
		Seekable seeker = (Seekable) sourceStream;
		if (secGoto == 0.0) {
		    seeker.seek(0); // seek into the stream
		    seek(0);
		    return TRYSET_DONE;
		} else {
		    if (seeker.isRandomAccess() && getContentLength() > 0) {
			double newLoc = secGoto / secDuration;
			long   seekLoc = (long)(newLoc * getContentLength());

			seeker.seek(seekLoc);
			seek(newLoc);
			return TRYSET_DONE;
		    } else
			return TRYSET_CANT;
		}
	    } else
		return TRYSET_DONE;			      // Not seekable. just ignore the seek. ???
	} else {				      // File seek
	    
	    // Handles the obvious case.
	    if (secGoto == 0.0) {
		seek(0);
		return TRYSET_DONE;
	    }
	    
	    double newloc = secGoto / secDuration;
	    // System.err.println("Newloc = " + newloc);
	    // Sanity check
	    if (newloc > 1.0) newloc = 1.0;
	    
	    seek(newloc);
	    return TRYSET_DONE;
	}
    }


    /**
     * Get the current media time in nanosecond.
     */
    public long getMediaNanoseconds() {
	return getMediaTime().getNanoseconds();
    }


    public int getContentLength() {
	return contentLength;
    }
    

    /**
     * This is again based on a very rough estimation calculated from
     * the current time and the current offset into the media.
     */
    public Time getDuration() {
	// If we've hit the EOM then we know the duration for sure
	if (eomDuration != Duration.DURATION_UNKNOWN)
	    return eomDuration;

	if (source instanceof Duration) {
	    Time d = ((Duration)source).getDuration();
	    if (d != null && d != Duration.DURATION_UNKNOWN &&
		d != Duration.DURATION_UNBOUNDED)
		return d;
	}
	
	// If it's not a file, we can't estimate the duration
	/*
	if (sourceStream != null) {
	    return knownDuration;
	}
	*/
	
	if (mediaTimeChanged)
	    return knownDuration;
	
	long jtime = getMediaNanoseconds();
	double loc = getLoc();

	// Can't even give an estimation until we've played atleast 3 secs.
	if (loc == 0.0 || jtime < (long) 3E+9)
	    return knownDuration;

	long nanoseconds = (long)(jtime/loc);
	long knownNanoseconds = knownDuration.getNanoseconds();

	
	// If the difference is more than a second
	if (Math.abs(nanoseconds - knownNanoseconds) > 5E+9) {
	    knownDuration = new Time(nanoseconds);
	    sendEvent(new DurationUpdateEvent(this, knownDuration));
	}
	
	return knownDuration;
    }


    public Component getVisualComponent() {
	if (strmType == Jmpx.MpxStrm_ASEQ)
	    return null;
	return monitor;
    }


    public Control [] getControls() {

	if (strmType != Jmpx.MpxStrm_ASEQ && frameRateControl == null) {
	    frameRateControl = new FrameRateAdapter(this, 0f, 0f, 30f, false) {
		    
		public float setFrameRate(float rate) {
		    this.value = rate;
		    return this.value;
		}
		    
		public Component getControlComponent() {
		    return null;
		}

		public Object getOwner() {
		    return Jmpx.this;
		}
	    };
	}

	if (strmType != Jmpx.MpxStrm_VSEQ && gainControl == null) {
	    gainControl = new GCA(this);
	}

	if (bitRateControl == null) {
	    bitRateControl = new BitRateAdapter(0, -1, -1, false) {
	    
	        public int setBitRate(int rate) {
		    this.value = rate;
		    return this.value;
		}
		    
		public Component getControlComponent() {
		    return null;
		}
	    };
	}

	int size = 0;

	if (frameRateControl != null) size++;
	if (gainControl != null) size++;
	if (bitRateControl != null) size++;

	controls = new Control[size];

	size = 0;
	if (frameRateControl != null)
	    controls[size++] = frameRateControl;
	if (gainControl != null)
	    controls[size++] = gainControl;
	if (bitRateControl != null)
	    controls[size++] = bitRateControl;

	return controls;
    }


    public void play() {
	if (fwdSpeed == 1.0f)
	    setAction(PC_PLAY);
	else
	    setAction(PC_FWDSPEED);
    }


    public void pause() {
	setAction(PC_PAUSE);
    }


    protected float doSetRate(float r) {
	if (strmType == MpxStrm_ASEQ)
	   return 1.0f;
	if (r < 0.1f) r = 0.1f;
	else if (r > 10.0f) r = 10.0f;
	fwdSpeed = r;
	return fwdSpeed;
    }


    public boolean checkStreamType() { 
	// Return if the stream type is already determined.
	if (strmType != MpxStrm_Unknown) {
	    /* for an mp2 file */
	    possibleLength = -1;
	    if (filename != null)
		getMediaDuration(filename);
	    if (possibleLength > 0)
		eomDuration = new Time((double) possibleLength);
	    return true;
	}

	possibleLength = -1;
	
	if (filename != null) {
	    if ((strmType = checkMpegFile((String)filename)) == 0)
		return false;
	} else if (sourceStream != null) {
	    if ((strmType = checkMpegStream(sourceStream)) == 0)
		return false;
	    // Reset the stream back to 0.  This is essential for video-only
	    // mpeg stream to work.
	    if (sourceStream instanceof Seekable)
		((Seekable)sourceStream).seek(0);
	} else
	    return false;

	if (possibleWidth != -1 && possibleHeight != -1) {
	    width = possibleWidth;
	    height = possibleHeight;
	    outWidth = width;
	    outHeight = height;
	}
	if (possibleLength > 0)
	    eomDuration = new Time((double) possibleLength);
	return true;
    }


    protected boolean initAudio() {
	return (audio != null);
    }


    protected void closeAudio() {
	if (audio != null) {
	    audio.pause();
	    audio.flush();
	    audio.dispose();
	    audio = null;
	}
    }


    protected boolean setAudio(int rate, int channels) {

	boolean isMuted = false;
	float gain = -1f;

	if (audio != null) {
	    // Return if there's no change in format.  Otherwise, 
	    // re-initialize the audio.
	    if (audioRate == rate && audioChannels == channels) {
		audio.flush();
		audio.resetSamples();
		return true;
	    }
	    isMuted = audio.getMute();
	    gain = (float)audio.getGain();
	    audio.pause();
	    audio.flush();
	    audio.dispose();
	}

	/*
	System.err.println("Jmpx.setAudio(): " + "\n" +
		"    rate = " + rate + "\n" +
		"    channels = " + channels);
	*/

	AudioFormat format = new AudioFormat(
			AudioFormat.LINEAR,
			rate, 
			16,
			channels,
			BIG_ENDIAN ? AudioFormat.BIG_ENDIAN : AudioFormat.LITTLE_ENDIAN,
			AudioFormat.SIGNED);
	audioRate = rate;
	audioChannels = channels;

        audio = new JmpxAudio();

	if (!audio.initialize(format)) {
	    audio = null;
	    return false;
	}

	audio.resume();

	if (timeBase instanceof MPXTimeBase)
	    ((MPXTimeBase)timeBase).reset();

	//--- added by hsy
	if ( gainControl != null ) {
	    audio.setMute(gainControl.getMute());
	    if ( gainControl.getDB() != -1)
		audio.setGain(gainControl.getDB());
	} else {
	    if (isMuted)
		audio.setMute(isMuted);
	    if (gain != -1)
		audio.setGain(gain);
	}

	return true;
    }


    public boolean isInitialized() {
	return (peer != 0);
    }


    public boolean isMuted() {
	return muted;
    }


    public int getWidth() {
	return width;
    }


    public int getHeight() {
	return height;
    }


    public int getStreamType() {
	return strmType;
    }


    public int getDisplayWidth() {
	if (useJavaRenderer == 1)
	    return outWidth;
	else
	    return width * zoom;
    }


    public int getDisplayHeight() {
	if (useJavaRenderer == 1)
	    return outHeight;
	else
	    return height * zoom;
    }


    /**
     * Get the current location into the media. (0.0 - 1.0)
     */
    public double getLoc() {
	return loc;
    }


    public void setMute(boolean m) {
	if (audio != null)
	    audio.setMute(m);
    }


    public void setGain(float g) {
	if (audio != null)
	    audio.setGain(g);
    }


    private synchronized void exit() {
	MpxCmd	cmd = new MpxCmd();
	cmd.packInt(MpxCmd.FLAGS, MCFL_SNDACK | MpxCmd.FLAGS_PAT);
	cmd.packInt(MpxCmd.ID, MCMD_EXIT);
	sendMPX(cmd.getBytes());
	peer = 0;
    }


    private synchronized void openFile(String path) {

	MpxCmd	cmd = new MpxCmd();
	cmd.packInt(MpxCmd.ID, MCMD_OPENSRC);
	cmd.packInt(MpxCmd.PARAMS, 0);
	cmd.packInt(MpxCmd.PARAMS+1, 0);
	cmd.packInt(MpxCmd.PARAMS+2, strmType);
	cmd.packInt(MpxCmd.PARAMS+3, 0);
	cmd.packInt(MpxCmd.PARAMS+4, MRE_FOFS);
	cmd.packInt(MpxCmd.PARAMS+5, MSC_FNAME);
	cmd.packStr(MpxCmd.PARAMS+6, path);
	sendMPX(cmd.getBytes());
    }


    public synchronized void openStream(SourceStream is) {

	// Tell mpx to read from the give InputStream.
	setStream(is);

	MpxCmd	cmd = new MpxCmd();
	cmd.packInt(MpxCmd.ID, MCMD_OPENSRC);
	cmd.packInt(MpxCmd.PARAMS, 0);
	cmd.packInt(MpxCmd.PARAMS+1, 0);
	cmd.packInt(MpxCmd.PARAMS+2, strmType);
	cmd.packInt(MpxCmd.PARAMS+3, 0);
	cmd.packInt(MpxCmd.PARAMS+4, MRE_FOFS);
	cmd.packInt(MpxCmd.PARAMS+5, MSC_JAVASTRM);
	sendMPX(cmd.getBytes());
    }


    /**
     * Set the action to one of the following types:
     * 	PC_PLAY, PC_FWDSPEED, PC_FWDSTEP, PC_PAUSE
     */
    public synchronized void setAction(int act) {
	MpxCmd	cmd = new MpxCmd();
	cmd.packInt(MpxCmd.ID, MCMD_PLAYCTR);
	cmd.packInt(MpxCmd.PARAMS, act);
	cmd.packInt(MpxCmd.PARAMS+1, (int)(fwdSpeed * MULF));
	sendMPX(cmd.getBytes());
    }


    /**
     * Seek to an offset into the media.  loc is given in 0.0 to 1.0.
     */
    public synchronized void seek(double loc) {
	MpxCmd	cmd = new MpxCmd();
	cmd.packInt(MpxCmd.ID, MCMD_REENTER);
	cmd.packInt(MpxCmd.PARAMS, 0);
	cmd.packInt(MpxCmd.PARAMS+1, (int)(loc * MULF));
	cmd.packInt(MpxCmd.PARAMS+2, strmType);
	cmd.packInt(MpxCmd.PARAMS+3, 0);
	cmd.packInt(MpxCmd.PARAMS+4, MRE_FOFS);
	sendMPX(cmd.getBytes());
    }


    public synchronized void flush() {
	MpxCmd	cmd = new MpxCmd();
	cmd.packInt(MpxCmd.ID, MCMD_REENTER);
	cmd.packInt(MpxCmd.PARAMS, 0);
	cmd.packInt(MpxCmd.PARAMS+1, 0);
	cmd.packInt(MpxCmd.PARAMS+2, strmType);
	cmd.packInt(MpxCmd.PARAMS+3, 0x2020);
	cmd.packInt(MpxCmd.PARAMS+4, MRE_ASOPEN);
	sendMPX(cmd.getBytes());
    }


    public synchronized void mute(boolean m) {
	MpxCmd	cmd = new MpxCmd();
	cmd.packInt(MpxCmd.ID, MCMD_STREAM);
	if (m)
	    cmd.packInt(MpxCmd.PARAMS, STRM_IGNOREID | STRM_SBCOFF);
	else
	    cmd.packInt(MpxCmd.PARAMS, 0);
	sendMPX(cmd.getBytes());
	muted = m;
    }


    public synchronized void muteAll(boolean m) {
	MpxCmd	cmd = new MpxCmd();
	cmd.packInt(MpxCmd.ID, MCMD_STREAM);
	if (m)
	    cmd.packInt(MpxCmd.PARAMS, STRM_IGNOREID | STRM_SBCOFF);
	else
	    cmd.packInt(MpxCmd.PARAMS, 0);
	broadcastMPX(cmd.getBytes());
    }


    public synchronized void zoom(int z) {
	if (useJavaRenderer != 1) {
	    MpxCmd	cmd = new MpxCmd();
	    int	arg;
	    cmd.packInt(MpxCmd.ID, MCMD_PRESCTR);
	    cmd.packInt(MpxCmd.PARAMS, PCTR_VMD);
	    if (interleaved)
		arg = VDM_COLB;
	    else
		arg = VDM_COL;
	    arg <<= 8;
	    arg |= z;
	    cmd.packInt(MpxCmd.PARAMS+1, arg);
	    sendMPX(cmd.getBytes());
	} else {
	    java_zoom = (float) z;
	}
    }


    public synchronized void zoomIn() {
	if (useJavaRenderer == 1) {
	    if (java_zoom >= (float) 2.0)
		return;
	    java_zoom = java_zoom * 2;
	    updateSizeChanged(width, height);
	} else {
	    zoom(zoom + 1);
	}
    }


    public synchronized void zoomOut() {
	if (useJavaRenderer == 1) {
	    if (java_zoom <= (float) 0.25)
		return;
	    java_zoom = java_zoom / (float) 2.0;
	    updateSizeChanged(width, height);
	} else {
	    if (zoom <= 1)
		return;
	    zoom(zoom - 1);
	}
    } 


    public void updateSizeChanged(int width, int height) {
	sendEvent(new SizeChangeEvent(this, width, height, 1.0f));
    }
    

    public synchronized void setInterleave(boolean l) {
	MpxCmd	cmd = new MpxCmd();
	int	arg;
	cmd.packInt(MpxCmd.ID, MCMD_PRESCTR);
	cmd.packInt(MpxCmd.PARAMS, PCTR_VMD);
	if (l)
	    arg = VDM_COLB;
	else
	    arg = VDM_COL;
	arg <<= 8;
	if (useJavaRenderer == 1)
	    arg |= 1;
	else
	    arg |= zoom;
	cmd.packInt(MpxCmd.PARAMS+1, arg);
	sendMPX(cmd.getBytes());
    }


    public synchronized void setAudioMode(int m) {
	MpxCmd	cmd = new MpxCmd();
	cmd.packInt(MpxCmd.ID, MCMD_PRESCTR);
	cmd.packInt(MpxCmd.PARAMS, PCTR_AMD);
	if ((m & MpxAudioStereo) == MpxAudioStereo)
	    cmd.packInt(MpxCmd.PARAMS+2, 070);
	else if ((m & MpxAudioRight) == MpxAudioRight)
	    cmd.packInt(MpxCmd.PARAMS+2, 060);
	else
	    cmd.packInt(MpxCmd.PARAMS+2, 050);
	sendMPX(cmd.getBytes());
    }


    public synchronized void setAudioQuality(int q) {
	MpxCmd	cmd = new MpxCmd();
	cmd.packInt(MpxCmd.ID, MCMD_PRESCTR);
	cmd.packInt(MpxCmd.PARAMS, PCTR_AMD);
	if ((q & MpxAudioHiQ) == MpxAudioHiQ)
	    cmd.packInt(MpxCmd.PARAMS+2, 04);
	else if ((q & MpxAudioMedQ) == MpxAudioMedQ)
	    cmd.packInt(MpxCmd.PARAMS+2, 05);
	else
	    cmd.packInt(MpxCmd.PARAMS+2, 06);
	sendMPX(cmd.getBytes());
    }


    public synchronized void setGamma(double g) {
	MpxCmd	cmd = new MpxCmd();
	cmd.packInt(MpxCmd.ID, MCMD_PRESCTR);
	cmd.packInt(MpxCmd.PARAMS, PCTR_GAM);
	cmd.packInt(MpxCmd.PARAMS+6, (int)(g * MULF));
	sendMPX(cmd.getBytes());
    }


    public synchronized void sendAck() {
	MpxCmd	cmd = new MpxCmd();
	cmd.packInt(MpxCmd.FLAGS, MCFL_SNDACK | MpxCmd.FLAGS_PAT);
	cmd.packInt(MpxCmd.ID, MCMD_ACK);
	cmd.packInt(MpxCmd.SEQUENCE, ackSeq);
	sendMPX(cmd.getBytes());
    }


    public void addJmpxListener(JmpxListener cl) {
	listeners.addElement(cl);
    }


    public void removeJmpxListener(JmpxListener cl) {
	listeners.removeElement(cl);
    }


    public void updateListeners(JmpxEvent evt) {
	for (int i = 0; i < listeners.size(); i++) {
	    ((JmpxListener)listeners.elementAt(i)).jmpxUpdate(evt);
	}
    }


    protected void listenCommand() {
	// byte	command[] = new byte[512];
	byte	command[] = new byte[128];
	int	len = 0;
	try {
	    if (!killCommandThread)
		len = cmdPipe.read(command);
	    //System.err.println("command len is " + len);
	} catch (IOException e) {
	    return;
	}
	if (len > 0)
	    processCommand(command, len);
    }


    protected int processCommand(byte cmd[], int len) {
	int	cb[];
	int	i;
	int	flags, type, seq, id;

	// From the byte array convert into integer array.
	// Because byte is signed, simply cb[i] = cmd[i*4]
	// won't do.
	len /= 4;
	cb = new int[len];
	if (BIG_ENDIAN) {
	    for (i = 0; i < len; i++) {
		cb[i] = 0;
		cb[i] |= cmd[i*4] & 0x7f;
		cb[i] |= cmd[i*4] & 0x80;
		cb[i] <<= 8;
		cb[i] |= cmd[i*4+1] & 0x7f;
		cb[i] |= cmd[i*4+1] & 0x80;
		cb[i] <<= 8;
		cb[i] |= cmd[i*4+2] & 0x7f;
		cb[i] |= cmd[i*4+2] & 0x80;
		cb[i] <<= 8;
		cb[i] |= cmd[i*4+3] & 0x7f;
		cb[i] |= cmd[i*4+3] & 0x80;
	    }
	} else {
	    for (i = 0; i < len; i++) {
		cb[i] = 0;
		cb[i] |= cmd[i*4+3] & 0x7f;
		cb[i] |= cmd[i*4+3] & 0x80;
		cb[i] <<= 8;
		cb[i] |= cmd[i*4+2] & 0x7f;
		cb[i] |= cmd[i*4+2] & 0x80;
		cb[i] <<= 8;
		cb[i] |= cmd[i*4+1] & 0x7f;
		cb[i] |= cmd[i*4+1] & 0x80;
		cb[i] <<= 8;
		cb[i] |= cmd[i*4+0] & 0x7f;
		cb[i] |= cmd[i*4+0] & 0x80;
	    }
	}

	// Check for sync pattern
	for (i = 0; i < 4; i++) {
	    if (cb[i] != MpxCmd.SyncPat[i])
		return 0;
	}

	// Check for version #
	if (cb[MpxCmd.VERSION] != MpxCmd.Version)
	    return 0;

	// Parse command flags
	if ((cb[MpxCmd.FLAGS] >>> 16) != 0xcccc)
	    return 0;
	else
	    flags = cb[MpxCmd.FLAGS] & 0xffff;

	// Ignore if not from mpx.
	if ((flags & MCFL_ORGMPX) == 0)
	    return 0;

	if ((cb[MpxCmd.TYPE] >>> 16) != 0xdddd)
	    return 0;
	else
	    type = cb[MpxCmd.TYPE] & 0xffff;

	if (type != 1 && type != 2)
	    return 0;

	// Check for command size.
	if ((type == 1 && len != 32) || (type == 2 && len != 128))
	    return 0;

	seq = cb[MpxCmd.SEQUENCE];
	id = cb[MpxCmd.ID];

	switch (id & 0xff) {
	case /*MCMD_QSIZE*/30:
	    width = cb[MpxCmd.PARAMS];
	    height = cb[MpxCmd.PARAMS+1];
	    if (useJavaRenderer != 1)
		zoom = cb[MpxCmd.PARAMS+2];
	    cmap = cb[MpxCmd.PARAMS+3];

	    updateSize(width, height);
	    sendEvent(new SizeChangeEvent(this, width, height, 1.0f));

	    break;

	case /*MCMD_QSTATS*/31:
	    long now = System.currentTimeMillis();
	    float delta = (float)(now - lastStatTime)/1000f;
	    long frameCount;

	    lastStatTime = now;
	    size = cb[MpxCmd.PARAMS];
	    loc = (float)(cb[MpxCmd.PARAMS+1]/MULF);
	    time = cb[MpxCmd.PARAMS+2];
	    frameCount = (cb[MpxCmd.PARAMS+3] + cb[MpxCmd.PARAMS+4] +
					cb[MpxCmd.PARAMS+5]);
	    fps = (cb[MpxCmd.PARAMS+3] + cb[MpxCmd.PARAMS+4] +
					cb[MpxCmd.PARAMS+5])/delta;
	    fps = ((int)(fps * 10f))/10f;
	    kbps = ((cb[MpxCmd.PARAMS+6] * 8)/delta) / 1000;
	    kbps = ((int)(kbps * 10))/10f;
	    frames += cb[MpxCmd.PARAMS+3] + cb[MpxCmd.PARAMS+4] +
					cb[MpxCmd.PARAMS+5];
	    //update progress controls
	    if (frameRateControl != null)
		frameRateControl.setFrameRate(fps);
	    if (bitRateControl != null)
		bitRateControl.setBitRate((int)(kbps * 1000));

	    if (loc >= 1.0 && (frames != 0 || strmType == MpxStrm_ASEQ)) {
		pause();
		super.stop();
		startSource(false);
		if (oldLoc < 1.0) {
		    eomDuration = getMediaTime();
		    knownDuration = eomDuration;
		    sendEvent(new EndOfMediaEvent(this, Started, Prefetched,
						  getTargetState(), getMediaTime()));
		    sendEvent( new DurationUpdateEvent(this, eomDuration) );
		}
	    }
	    oldLoc = loc;
	    getDuration();
	    break;

	case /*MCMD_ACK*/10:
	    if (listeners.size() > 0) {
		JmpxAckNotifyEvent ack = new JmpxAckNotifyEvent();
		ack.sequence = seq;
		updateListeners(ack);
	    }
	    break;

	default:
	}

	return id;
    }


    public void run() {
	while (!killCommandThread) {
	    listenCommand();
	}
    }

    public synchronized void resizeWindow(int w, int h) {
	if (monitor == null) return;
	width = w;
	height = h;
	if (videoformat instanceof RGBFormat) {
	    rgbBuffer = new int[width * height];
	    videoformat = new RGBFormat(new Dimension(width, height),
					width * height,
					Format.intArray,
					30f, 32,
					rMask, gMask, bMask,
					1, width,
					RGBFormat.FALSE,
					RGBFormat.BIG_ENDIAN);

	    outWidth = width;
	    outHeight = height;
	    monitor.setSize(outWidth, outHeight);
	} else {
	    // have to redefine the YUV format
	    videoformat = new YUVFormat(new Dimension(width, height),
				    width * height * 2,
				    Format.byteArray, Format.NOT_SPECIFIED,
				    YUVFormat.YUV_422,
				    width, width / 2, 0, width * height,
				    width * height + width * height / 4);
	    rgbBuffer = new byte[width * height * 2];
	    monitor.setSize(outWidth, outHeight);
	    //System.err.println("Jmpx resized: " + outWidth + "x" + outHeight);
	}
/*
	width = w;
	height = h;
	if (!outputSizeSet) {
	    outWidth = w;
	    outHeight = h;
	}
	monitor.setSize(outWidth, outHeight);
	if (useFastBlt) {
	    if (screenDepth == 8) {
		YUVFormat yuvFormat = new YUVFormat(width, height, width * height * 2, 1,
						    YUVFormat.YUV411);
		yuvFrame = new YUVData(yuvFormat);
		rgbBuffer = (byte []) yuvFrame.getBuffer();
		IndexColorFormat icf = new IndexColorFormat(width, height, width * height, 1,
							    0, null);
		pseudoFrame = new IndexColorData(icf);
		
		if (ditherer == null) {
		    ditherer = new YUVToPseudo();
		    ditherer.initialize( null , monitor);
		}
	    } else {
		rgbFormat = new RGBFormat(width, height, width * height, 4,
					  0x000000FF,
					  0x0000FF00,
					  0x00FF0000,
					  4);
		rgbData = new RGBData(rgbFormat);
		rgbBuffer = (int []) rgbData.getBuffer();
	    }
	} else {
	    rgbBuffer = new int[w * h];
	
	    
	    DirectColorModel cm = new DirectColorModel(24, 0x000000FF,
						       0x0000FF00,
						       0x00FF0000);
	    sourceImage = new MemoryImageSource(w, h, cm, (int [])rgbBuffer, 0, w);
	    sourceImage.setAnimated(true);
	    sourceImage.setFullBufferUpdates(true);
	    displayImage = (monitor).createImage(sourceImage);
	    monitor.prepareImage(displayImage, monitor);    
	}
	
	
	updateSizeChanged(width, height);
	String audio = "";
	if (strmType == MpxStrm_11172)  {
	    audio = JMFI18N.getResource("jmpx.MPEG1-Audio");
	}
	if (strmType == MpxStrm_VSEQ) {
	    audio = JMFI18N.getResource("mediaplayer.N/A");
	}
*/
    }


    public Component getImage(int w, int h) {
	if (width != w || height != h || rgbBuffer == null) {
	    resizeWindow(w, h);
	}
	return monitor;
    }

    private Buffer outBuffer = new Buffer();
    
    public synchronized void displayImage() {
	if (monitor == null)
	    return;
	synchronized (monitor) {
	    if (videoformat instanceof RGBFormat) {
		outBuffer.setData((int[]) rgbBuffer);
		outBuffer.setLength(width * height);
	    } else {
		outBuffer.setData((byte[]) rgbBuffer);
		// assumes YUV 422
		outBuffer.setLength(width * height + width * height/2);
	    }
	    outBuffer.setFormat(videoformat);
	    outBuffer.setTimeStamp(Buffer.TIME_UNKNOWN);
	    outBuffer.setFlags(0);
	    outBuffer.setHeader(null);

	    renderer.process(outBuffer);
	}
    }
/*
	synchronized (monitor) {
	    if (!windowAvailable) {
		lastFrame = null;
		return;
	    }
	    
	    if (useFastBlt) {    
		if (screenDepth == 8) {
		    if (ditherer == null) {
			ditherer = new YUVToPseudo();
			ditherer.initialize( null , monitor);
		    }
		    ditherer.convert(yuvFrame, pseudoFrame);
		    if (blitter == null) {
			try {
			    blitter = (com.sun.media.Blitter) blitterClass.newInstance();
			} catch (Exception e) {
			    return;
			}
			if (! blitter.setComponent(monitor))
			    blitter = null;
			else if (! blitter.setBuffer(pseudoFrame))
			    blitter = null;
			else if (! blitter.setOutputSize(outWidth, outHeight))
			    blitter = null;
		    }
		    lastFrame = pseudoFrame;
		} else {
		    
		    if (blitter == null) {
			try {
			    blitter = (com.sun.media.Blitter) blitterClass.newInstance();
			} catch (Exception e) {
			    return;
			}
			if (! blitter.setComponent(monitor))
			    blitter = null;
			else if (! blitter.setBuffer(rgbData))
			    blitter = null;
			else if (! blitter.setOutputSize(outWidth, outHeight))
			    blitter = null;
		    }
		    lastFrame = rgbData;
		}
		
		if (blitter != null)
		    blitter.draw(lastFrame, 0, 0);
		else
		    System.err.println("Couldn't use Fast blitter");
	    } else {
		Graphics g = (monitor).getGraphics();
		if (g != null && displayImage != null) {
		    sourceImage.newPixels(0, 0, width, height);
		    g.drawImage(displayImage,
				0, 0, 
				outWidth, 
				outHeight, //dest
				0, 0, width, height,	      // source
				monitor);
		}
	    }
	}
    }
*/

    private Component createMonitor(int width, int height) {
	Dimension size = new Dimension(width, height);
	// try YUV first
	videoformat = new YUVFormat(size, width * height * 2,
				    Format.byteArray, Format.NOT_SPECIFIED,
				    YUVFormat.YUV_422,
				    width, width / 2, 0, width * height,
				    width * height + width * height / 4);
	renderer = (VideoRenderer) SimpleGraphBuilder.findRenderer(videoformat);
	if (renderer != null) {
	    // found a YUV renderer
	    needYUVBuffer = 1;
	    rgbBuffer = new byte [ width * height * 2];
	    return renderer.getComponent();
	}

	needXBGR = 1;
	if (LITTLE_ENDIAN) {
	    rMask = 0xFF0000;
	    bMask = 0xFF;
	}
        ColorModel cm = Toolkit.getDefaultToolkit().getColorModel();
	if (cm instanceof DirectColorModel) {
	    DirectColorModel dcm = (DirectColorModel) cm;
	    if (dcm.getPixelSize() > 16) {
	        rMask = dcm.getRedMask();
	        gMask = dcm.getGreenMask();
	        bMask = dcm.getBlueMask();
	    }
	    if (rMask != 0xFF && !LITTLE_ENDIAN) {
		needXBGR = 0;
	    }
	}
	videoformat = new RGBFormat(size,
				  size.width * size.height,
				  Format.intArray,
				  30f, 32,
				  rMask, gMask, bMask,
				  1, (size.width + 7) & ~0x7,
				  RGBFormat.FALSE,
				  RGBFormat.NOT_SPECIFIED);

	rgbBuffer = new int[width * height];

	renderer = (VideoRenderer) SimpleGraphBuilder.findRenderer(videoformat);
	if (renderer == null) {
	    System.err.println("No renderer");
	    return null;
	} else
	    return renderer.getComponent();
    }

    private void updateSize(int width, int height) {

    }
    
    /*************************************************************************
     * INNER CLASSES
     *************************************************************************/

    class MPXTimeBase extends MediaTimeBase {

	public long getMediaTime() {
	    return (audio != null ? audio.getMediaNanoseconds() : 0);
	}

	protected void reset() {
	    if (audio == null || getState() != Controller.Started)
		mediaStopped();
	    else
		mediaStarted();
	}
    }

    /**
     * Rendering surface for the MPEG player.
     */
    class Monitor extends Canvas {

	public Monitor(int w, int h) {
	    super.setSize(w, h);
/*
	    addComponentListener( new ComponentAdapter() {

		private int lastWidth = -1;
		private int lastHeight = -1;
		
		public void componentResized(ComponentEvent ce) {
		    Dimension csize = getSize();
		    if (csize.width == lastWidth && csize.height == lastHeight)
			return;
		    lastWidth = csize.width;
		    lastHeight = csize.height;
		    outWidth = lastWidth;
		    outHeight = lastHeight;
		    Jmpx.this.outputSizeSet = true;
		    if (blitter != null) {
			blitter.setOutputSize(outWidth, outHeight);
		    }
		}
	    } );
*/
	}

	public void updateSize(int w, int h) {
	    if (Jmpx.this.outputSizeSet)
		return;
	    Component parent = this;
	    while (parent.getParent() != null)
		parent = parent.getParent();
	    super.setSize(w, h);
	    if (parent != null)
		parent.validate();
	    repaint();
	}

	public Dimension getMinimumSize() {
	    return new Dimension(1, 1);
	}

	public Dimension getPreferredSize() {
	    return new Dimension(Jmpx.this.getWidth(), Jmpx.this.getHeight());
	}

	// This happens when the component's peer is removed.
	// This is a panic situation.  We need to recreate an
	// instance of Jmpx to handle it.
	public synchronized void removeNotify() {
	    windowAvailable = false;
/*
	    if (blitter != null)
		blitter.close();
	    blitter = null;
	    ditherer = null;
*/
	    super.removeNotify();
	}

	public synchronized void addNotify() {
	    super.addNotify();
	    windowAvailable = true;
	}

	public synchronized void paint(Graphics g) {
	    if (Jmpx.this.getTargetState() == Controller.Started)
		return;
/*
	    if (blitter != null && lastFrame != null)
	    	blitter.draw(lastFrame, 0, 0);
*/
	}

	public void update(Graphics g) {
	    if (Jmpx.this.getTargetState() == Controller.Started)
		return;
/*
	    if (blitter != null && lastFrame != null)
		blitter.draw(lastFrame, 0, 0);
*/
	}

    }


    /**
     * Handles push source stream.
     */
    class PushStreamHandler implements SourceTransferHandler {
	public void transferData(PushSourceStream stream) {
	    notifyPushDataArrival();	
	}
    }


    class GCA extends GainControlAdapter {

	Jmpx jmpx;

	protected GCA(Jmpx jmpx) {
	    super(false);
	    this.jmpx = jmpx;
	}

	public void setMute(boolean mute) {
	    if (audio != null)
		audio.setMute(mute);
	    super.setMute(mute);
	}

	public float setLevel(float g) {
	    if (jmpx == null)
		return g;

	    float level = super.setLevel(g);
	    if (audio != null)
		audio.setGain(getDB());
	    return level;
	}
    }
}


/**
 * A thread class for use by the Jmpx native code to instantiate
 * green threads.
 */
class JmpxThread extends Thread {

    Jmpx jmpx;
    long clientData;
    long mpxData;
    private PipedOutputStream cmdPipe;

    JmpxThread(Jmpx jmpx) {
	this.jmpx = jmpx;
	clientData = 0;
	mpxData = 0;
	cmdPipe = jmpx.mpxCmdPipe;
	setPriority(Thread.MIN_PRIORITY);
    }

    void replyCommand(byte[] cmd) {
	try {
	    cmdPipe.write(cmd, 0, cmd.length);
	    cmdPipe.flush();
	} catch (IOException e) {
	    //System.err.println("replyCommand() failed: " + e);
	}
    }
}


//
// This is the mpx decoding thread that's started from the initMPX()
// native call.
//
class MpxThread extends JmpxThread {

    static {
    	JMFSecurityManager.loadLibrary("jmutil");
    	JMFSecurityManager.loadLibrary("jmmpx");
    }

    native public void run();

    MpxThread(Jmpx jmpx) {
	super(jmpx);
	setName("mpx decoding thread");
	setPriority(Thread.MIN_PRIORITY);
    }
}


//
// This is the mpx data server thread that's started from the initMPX()
// native call.
//
class DataThread extends JmpxThread {

    private boolean paused = false;
    private boolean donePaused = true;

    static {
    	JMFSecurityManager.loadLibrary("jmutil");
    	JMFSecurityManager.loadLibrary("jmmpx");
    }

    native public void run();

    DataThread(Jmpx jmpx) {
	super(jmpx);
	setName("mpx data thread");
	setPriority(Thread.MIN_PRIORITY);
    }

    protected synchronized void pause() {
	donePaused = false;
	paused = true;
	// Block for the donePaused to clear.  This is done
	// so that the last read could be completed before it
	// returns.
	if (!donePaused) {
	    try {
		wait(250);
		donePaused = true;
	    } catch (InterruptedException e) {}
	}
    }

    protected synchronized void restart() {
	// Restart the paused thread.
	paused = false;
	donePaused = true;
	notifyAll();
    }

    protected synchronized void checkPause() {
	// Check to see if the thread is paused.  Block if so.
	while (paused) {
	    if (!donePaused) {
		donePaused = true;
		notifyAll();
	    }
	    try {
		wait();
	    } catch (InterruptedException e) {}
	}
    }

    protected synchronized void checkRead() {
	// In case pause() is called in the middle of the read,
	// this will unblock the pause() call after the read.
	if (paused) {
	   donePaused = true;
	   notifyAll();
	}
    }
}


// A semaphore class to be used by the native code for synchronization.
class Semaphore {

    private int count;

    Semaphore(int count) {
	this.count = count;
    }

    protected synchronized void reset(int count) {
	this.count = count;
	notifyAll();
    }

    protected synchronized boolean decr() {
	try {
	    while (count <= 0)
		wait();
	} catch (InterruptedException e) {
	    return false;
	}
	count--;
	return true;
    }

    protected synchronized boolean tryDecr() {
	if (count <= 0)
	    return false;
	count--;
	return true;
    }

    protected synchronized boolean incr() {
	count++;
	notifyAll();
	return true;
    }
}


/*
 * Mpx Command class to package bytes to send across the pipe.
 */
class MpxCmd {

    public final static int	SyncPat[] = {0x1, 0x2, 0x3, 0x4};
    public final static int	Version = 0xaaaa0001;

    // Command header indexes.
    public final static int	SYNC1 = 0;
    public final static int	SYNC2 = 1;
    public final static int	SYNC3 = 2;
    public final static int	SYNC4 = 3;
    public final static int	VERSION = 4;
    public final static int	CHANNEL = 5;
    public final static int	SEQUENCE = 6;
    public final static int	FLAGS = 7;
    public final static int	TYPE = 8;
    public final static int	ID = 9;
    public final static int	PARAMS = 10;

    protected final static int CHANNEL_PAT = 0xbbbb0000;
    protected final static int SEQUENCE_PAT = 0x00000000;
    protected final static int FLAGS_PAT = 0xcccc0000;
    protected final static int TYPE_PAT = 0xdddd0002;

    protected byte	cmdbuf[];

    public MpxCmd() {
	// Fill in the command header bytes.
	cmdbuf = new byte[512];
	packInt(SYNC1, SyncPat[0]);	// sync 1
	packInt(SYNC2, SyncPat[1]);	// sync 2
	packInt(SYNC3, SyncPat[2]);	// sync 3
	packInt(SYNC4, SyncPat[3]);	// sync 4
	packInt(VERSION, Version);	// version
	packInt(CHANNEL, CHANNEL_PAT);	// channel
	packInt(SEQUENCE, SEQUENCE_PAT); // sequence #
	packInt(FLAGS, FLAGS_PAT); 	// flags
	packInt(TYPE, TYPE_PAT);	// type
    }

    public byte[] getBytes() {
	return cmdbuf;
    }

    protected void packInt(int start, int k) {
	if (Jmpx.BIG_ENDIAN) {
	    cmdbuf[start*4 + 3] = (byte)k;
	    cmdbuf[start*4 + 2] = (byte)(k >>> 8);
	    cmdbuf[start*4 + 1] = (byte)(k >>> 16);
	    cmdbuf[start*4 + 0] = (byte)(k >>> 24);
	} else {
	    cmdbuf[start*4 + 0] = (byte)k;
	    cmdbuf[start*4 + 1] = (byte)(k >>> 8);
	    cmdbuf[start*4 + 2] = (byte)(k >>> 16);
	    cmdbuf[start*4 + 3] = (byte)(k >>> 24);
	}
    }
    
    protected void packStr(int start, String str) {
	int	i;
	for (i = 0; i < str.length(); i++) {
	    cmdbuf[start*4 + i] = (byte)str.charAt(i); 
	}
    }
}

