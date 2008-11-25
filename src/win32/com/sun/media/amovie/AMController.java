/*
 * @(#)AMController.java	1.22 02/12/17
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.amovie;

import javax.media.*;
import javax.media.protocol.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.Vector;
import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.control.FormatControl;
import com.sun.media.*;
import com.sun.media.util.LoopThread;
import com.sun.media.util.MediaThread;
import com.sun.media.util.jdk12;
import com.sun.media.ui.*;
import com.sun.media.controls.*;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;


/**
 * Controller for Active Movie based MPEG player.
 */
public class AMController extends BasicController
{

    /*************************************************************************
     * Constants
     *************************************************************************/
    
    final int MINVOLUME = -10000;

    /*************************************************************************
     * Variables
     *************************************************************************/

    // ActiveMovie wrapper
    ActiveMovie amovie = null;

    protected static final int TRYSET_DONE    = 0;
    protected static final int TRYSET_CANT    = 1;
    protected static final int TRYSET_PASTEOM = 2;

    private Integer closeLock = new Integer(0);
    private Integer sourceLock = new Integer(1);
    private boolean closed = false;
    private TimeBase amTimeBase = null;
    
    private int appletWindowHandle = 0;
    int pwidth = -1;
    int pheight = -1;
    int outWidth;
    int outHeight;
    private int width;
    private int height;
    boolean peerExists = false;
    private boolean muted = false;

    private boolean outputSizeSet = false;
    
    private EventThread eventThread = null;
    private boolean isFileStream = false;
    private boolean isRandomAccess = false;
    private boolean isSeekable = false;
    private boolean hasAudio = false;
    private boolean hasVideo = false;
    private boolean deallocated = false;
    private boolean sourceIsOn = false;
    private javax.media.protocol.DataSource source = null;
    private PullSourceStream stream = null;
    private SourceStream originalStream = null;
    private String mpegFile = null;
    private com.sun.media.content.video.mpeg.Handler player;
    private Component visualComponent = null;
    private boolean seekFailed = false;
    private Time timeWhenMediaStopped = null;
    private boolean mediaTimeChanged = false;
    private Time requestedMediaTime = new Time(0);
    private Time lastMediaTime = new Time(0);
    private int nextRead = 0;
    private boolean inEOM = false;

    private VideoFormat videoFormat = null;
    private AudioFormat audioFormat = null;
    
    private String id = "JavaActiveMovie_" + hashCode();

    // Caching related
    private boolean blockedOnRead = false;
    private Thread blockThread = null;
    private boolean abortingRealize = false;

    private boolean setRatePending = false;
    private float   setRateValue = (float) 1.0;
    
    private GainControl gc = null;
    private Control [] controls = null;
    private FormatControl audioControl = null;
    private FormatControl videoControl = null;

    private static boolean libraryLoaded = false;
    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege=false;
    private Method m[] = new Method[1];
    private Class cl[] = new Class[1];
    private Object args[][] = new Object[1][0];

    static {
	try {
	    jmfSecurity = JMFSecurityManager.getJMFSecurity();
	    securityPrivelege = true;
	} catch (SecurityException e) {
	}
    }

    /*************************************************************************
     * Methods
     *************************************************************************/

    public AMController(com.sun.media.content.video.mpeg.Handler handler) {
	String library = "jmam";
	if (!libraryLoaded) {
	    try {
		JMFSecurityManager.loadLibrary(library);
		libraryLoaded = true;
	    } catch (Throwable t) {
		throw new RuntimeException("Unable to load native MPEG library");
	    }

	}
	player = handler;
    }

    public void setSource(javax.media.protocol.DataSource s)
	throws IncompatibleSourceException {

	// Check for PullDataSource
	if (s instanceof PullDataSource)
	    source = s;
	else
	    throw new IncompatibleSourceException("MPEG Controller requires a PullDataSource");

	String excMessage = null;
	// Check if audio
	if (s.getContentType().equalsIgnoreCase("audio.mpeg") ||
	    s.getContentType().equalsIgnoreCase("audio/mpeg")) {
	    
	    // If there's an MP3 plugin, don't handle audio here
	    AudioFormat mp3 = new AudioFormat(AudioFormat.MPEGLAYER3);
	    AudioFormat linear = new AudioFormat(AudioFormat.LINEAR);
	    Vector v = PlugInManager.getPlugInList(mp3, linear, PlugInManager.CODEC);
	    if (v != null && v.size() > 0 &&
		!( v.size() == 1 && ((String)v.elementAt(0)).equals("com.ibm.media.codec.audio.ACMCodec"))) {
		// Some plug-in already handles MP3 and hence MP2 also
		// So this player shouldn't handle it
		excMessage = "Handler doesn't play MPEG Audio";
	    } else {
		// Handle only if local file && mp3
		MediaLocator ml = s.getLocator();
		if (ml == null)
		    excMessage = "No media locator, cannot handle stream";
		else {
		    String url = ml.toString();
		    if (url != null && url.endsWith(".mp2")) {
			// Don't handle MP2, since its handled by Unknown Handler
			excMessage = "Handler doesn't play MPEG Layer 2 audio";
		    } else {
			if (!ml.getProtocol().equals("file"))
			    excMessage = "Handler only plays local MPEG Audio files";
		    }
		}
	    }
	}
	if (excMessage != null)
	    throw new IncompatibleSourceException("MPEG Handler: " + excMessage);
    }

    public void setTimeBase(TimeBase tb) throws IncompatibleTimeBaseException {
	super.setTimeBase(tb);

	// We can't support any other time base
	if (tb != null && tb != amTimeBase) {
	    Log.warning("the mpeg handler cannot handle the given timebase.");
	    /**
	     Allow this to silently go through so addController will be
	     slightly more useful.
	     --ivg
	    throw new IncompatibleTimeBaseException("the mpeg handler cannot handle the given timebase.");
	     */
	}
	amTimeBase = tb;
    }

    public TimeBase getTimeBase() {
	if (amTimeBase == null)
	    amTimeBase = new AMTimeBase( this );
	return amTimeBase;
    }

    public boolean isConfigurable() {
	return false;
    }

    private void updateControls(ActiveMovie amovie) {
	if (hasAudio) {
	    audioFormat = new AudioFormat(AudioFormat.MPEG);
	}
	if (hasVideo) {
	    Dimension size = new Dimension(amovie.getVideoWidth(),
					   amovie.getVideoHeight());
	    videoFormat = new VideoFormat(VideoFormat.MPEG,
					  size,
					  Format.NOT_SPECIFIED,
					  Format.byteArray,
					  Format.NOT_SPECIFIED);
	}
    }

    public Control [] getControls() {
	int n = 0;
	if (hasAudio) n++;
	if (audioFormat != null) n++;
	if (videoFormat != null) n++;
	controls = new Control[n];
	n = 0;
	if (hasAudio) {
	    if (gc == null) {
		gc = new GCA();
	    }
	    controls[n++] = gc;
	}
	if (audioFormat != null) {
	    if (audioControl == null) {
		audioControl = new FormatAdapter(audioFormat,
						 new Format[] {audioFormat},
						 true,
						 false,
						 false);
	    }
	    controls[n++] = audioControl;
	}
	if (videoFormat != null) {
	    if (videoControl == null) {
		videoControl = new FormatAdapter(videoFormat,
						 new Format[] {videoFormat},
						 true,
						 false,
						 false);
	    }
	    controls[n++] = videoControl;
	}
	return controls;
    }
    
    private ActiveMovie createActiveMovie(javax.media.protocol.DataSource s) {
	URL url = null;
	if (s == null)
	    return null;
	MediaLocator ml = s.getLocator();
	
	if (ml != null) {
	    try {
		url = ml.getURL();
	    } catch (MalformedURLException e) {
		ml = null;			      // It's possible that its not a URL
						      // Bug reported after beta3.
	    }
	}
	
	if (ml != null && ml.getProtocol().equals("file")) {
	    int indexPipe;

	    mpegFile =
		com.sun.media.protocol.file.DataSource.getFileName(ml);
            isFileStream = true;
	    isRandomAccess = true;
	    isSeekable = true;
            ActiveMovie am = new ActiveMovie(this, mpegFile);
	    hasVideo = am.hasVideo();
	    hasAudio = am.hasAudio();
	    updateControls(am);
	    return am;
	} else {
	    // Can only handle file: for MPEG_AUDIO
	    if (s.getContentType().equalsIgnoreCase("audio.mpeg") ||
		s.getContentType().equalsIgnoreCase("audio/mpeg"))
		
		return null;
	    
	    // Its a data source other than file. Lets open a stream
	    if (s instanceof PullDataSource) {
		
		PullSourceStream [] streams = 
		    (PullSourceStream []) ((PullDataSource)s).getStreams();
		if (streams != null && streams.length > 0) {
		    stream = streams[0];
		    originalStream = stream;
		    if (stream instanceof Seekable) {
			isSeekable = true;
			// ((Seekable)stream).seek(0);
			// Is it a random access stream?
			if (((Seekable)stream).isRandomAccess())
			    isRandomAccess = true;
			if (ml != null && ml.getProtocol().startsWith("http") &&
			    url != null) {
			    isRandomAccess = false;
			}
		    }
		    ActiveMovie am = new ActiveMovie(this, (PullSourceStream)stream,
						     isRandomAccess,
						     originalStream.getContentLength());
		    hasVideo = am.hasVideo();
		    hasAudio = am.hasAudio();
		    updateControls(am);
		    return am;
		}
	    }	    
	    return null;
	}
    }

    /****************************************************************
     * Stream related calls made from native code
     ****************************************************************/
    
    public int canRead(int bytes) {
	return bytes;
    }

    public long canSeek(long seekTo) {
	// TODO: Pause the controller if seek will block
	return seekTo;
    }

    public long seek(long seekTo) {
	if (abortingRealize) {
	    //System.err.println("seek.0");
	    return 0;
	}

	synchronized (sourceLock) {
	    if (stream instanceof Seekable && sourceIsOn) {
		//System.err.println("seek.1");
		
		// Don't seek if already there.
		if (((Seekable)stream).tell() == seekTo)
		    return seekTo;
		
		if (((Seekable)stream).isRandomAccess()) {
		    long seeked = ((Seekable)stream).seek(seekTo);
		    //System.err.println("Seeked = " + seekTo);
		    return seeked;
		} else if (seekTo == 0)
		    return ((Seekable)stream).seek(seekTo);
	    }
	}
	//System.err.println("seek.2");
	return 0;
    }

    public int read(byte [] data, int offset, int length) {
	if (abortingRealize)
	    return -1;
	/*
	if (canRead(length) == -1)
	    return -1;
	*/
	if (nextRead == -2) {
	    nextRead = 0;
	    return -2;
	}
	synchronized (sourceLock) {
	    if (stream != null && sourceIsOn) {
		try {
		    int readBytes = stream.read(data, offset, length);
		    return readBytes;
		} catch (IOException ioe) {
		    sendEvent(new ConnectionErrorEvent(this, ioe.getMessage()));
		    return -1;
		}
	    } else
		return -1;
	}
    }
    
    protected boolean doRealize() {
	abortingRealize = false;

	if (amovie != null) {
	    if (amTimeBase instanceof AMTimeBase)
		amTimeBase = null;
	    amovie.kill();
	    amovie.dispose();
	    amovie = null;
	}

	startSource(true, true);

	synchronized (closeLock) {
	
	    amovie = createActiveMovie(source);
	    if (amovie == null)
		return false;
	
	    if (!amovie.isRealized()) {
		amovie = null;
		return false;
	    }

	    if (abortingRealize) {
		doDeallocate();
		amovie.dispose();
		amovie = null;
		return false;
	    }
	}

	try {
	    amovie.amStopWhenReady();
	    amovie.doneRealize();
	    if (amTimeBase == null)
		amTimeBase = new AMTimeBase( this );
	    setMediaLength((long)(amovie.getDuration() * 1e9));
	    amovie.setVisible(0);
	    startSource(false, false);
	} catch (Throwable t) {
	    return false;
	}
	return true;
    }

    protected void abortRealize() {
	// TODO: Abort downloading if caching is enabled
	// System.err.println("AMController.abortRealize()");
	abortingRealize = true;
	startSource(false, true);
    }

    protected void abortPrefetch() {
	// System.err.println("AMController.abortPrefetch()");
	startSource(false, true);
    }

    protected boolean doPrefetch() {
	if (amovie == null)
	    if (!doRealize())
		return false;

	// If activemovie was recreated, reattach the AM window to java panel
	if (amovie != null && visualComponent != null && peerExists) {
	    setOwner(visualComponent);
	}
	return true;
    }

    
    // Called from a separate thread called TimedStart thread.
    protected final void doStart() {
	GainControl gc;
	if (amovie == null)
	    doPrefetch();

	startSource(true, false);
	amovie.restart();

	if (setRatePending) {
	    amovie.setRate(setRateValue);
	    setRatePending = false;
	    if ((float) amovie.getRate() != setRateValue) {
		sendEvent(new RateChangeEvent(this, (float) amovie.getRate()));
	    }
	}
	
	// Restart the download thread.
	if (mediaTimeChanged) {
	    int returnVal = trySetMediaTime(requestedMediaTime);
	    if (returnVal == TRYSET_CANT) {
		// Couldn't set the media time.
		super.stop();
		sendEvent((StopEvent) new SeekFailedEvent(this, Started,
							  Prefetched,
							  getTargetState(),
							  getMediaTime()));
		return;
	    } else if (returnVal == TRYSET_PASTEOM) {
		// Seeking beyond EOM
		super.stop();
		sendEvent(new EndOfMediaEvent(this, Started, Prefetched,
					      getTargetState(), getMediaTime()));
		return;
	    }
	}
	// We've succeeded in setting the media time.
	mediaTimeChanged = false;
	
	if ((gc = player.getGainControl()) != null)
	    amovie.setVolume((int)(gc.getDB() * 100));
	amovie.amRun();
	if (!peerExists)
	    amovie.setVisible(0);
	if (gc != null)
	    muteChange(gc.getMute());	
	if (eventThread == null) {
	    if ( /*securityPrivelege  && */ (jmfSecurity != null) ) {
		String permission = null;
		try {
		    if (jmfSecurity.getName().startsWith("jmf-security")) {
			permission = "thread";
			jmfSecurity.requestPermission(m, cl, args,
						      JMFSecurity.THREAD);
			m[0].invoke(cl[0], args[0]);
			
			permission = "thread group";
			jmfSecurity.requestPermission(m, cl, args,
						      JMFSecurity.THREAD_GROUP);
			m[0].invoke(cl[0], args[0]);
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
		    
		    eventThread = (EventThread) jdk12.doPrivM.invoke(
                                           jdk12.ac,
					   new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               EventThread.class,
                                           })});
		} catch (Exception e) {
		}
	    } else {
		eventThread = new EventThread();
	    }
	}
	eventThread.setController(this);
	eventThread.start();
    }

    public void doStop() {
	// System.err.println("In doStop()");
	super.doStop();
	lastMediaTime = getMediaTime();
        if (amovie != null && !inEOM) {
	    amovie.amStop();
	    //amovie.amPause();
	    amovie.pause();
        }
	if (eventThread != null)
	    eventThread.pause();
	startSource(false, false);
	if (!isFileStream) 
	    nextRead = -2;
	sendEvent((StopEvent)new StopByRequestEvent(this, Started,
						    Prefetched,
						    getTargetState(),
						    getMediaTime()));
    }

    protected synchronized void doDeallocate() {
	// Stop the source
	startSource(false, false);
	
	// Restart from Time(0).
	timeWhenMediaStopped = getMediaTime();
	mediaTimeChanged = true;
	requestedMediaTime = new Time(0);
	lastMediaTime = timeWhenMediaStopped;

	// Kill all threads and ActiveMovie
	if (amovie != null) {
	    blockedOnRead = false;
 	    if (amovie.getVolume() == MINVOLUME)  // Is Mute on?
	        amovie.setVolume(MINVOLUME / 2); // TURN MUTE OFF
	    amovie.kill();

	    if (eventThread != null) {
	        eventThread.kill();
	        eventThread = null;
	    }
        }
    }

    public void finalize() {
	if (amovie != null)
	    doClose();
    }

    protected void doClose() {

	if (getState() == Controller.Realizing)
	    abortRealize();
	
	synchronized (closeLock) {
	    // Do nothing if already closed.
	    if (closed)
		return;
	    
	    // Stop all the threads and active movie.
	    doDeallocate();
	    
	    // Kill active movie
	    if (amovie != null) {
		amovie.dispose();
		amTimeBase = null;
		amovie = null;
	    }
	    // Disconnect the data source
	    if (source != null) {
		source.disconnect();
	    }
	    source = null;
	    closed = true; // Dont come back!
	}
    }

    public void setMediaTime(Time time) {
	super.setMediaTime(time);
	synchronized (this) {
	    requestedMediaTime = time;
	    mediaTimeChanged = true;
	}

	if (stream == null) {
	    amovie.restart();
	    if (trySetMediaTime(time) == TRYSET_DONE) 
		mediaTimeChanged = false;
	}
    }
    
    protected int trySetMediaTime(Time time) {
	if (amovie != null) {
	    long duration = getDuration().getNanoseconds();
	    long now = time.getNanoseconds();

	    // Dont know the duration and not seeking to zero?
	    if (getDuration() == DURATION_UNKNOWN && now != 0)
		return TRYSET_CANT;

	    // Seeking beyond duration?
	    if (now < 0)
		return TRYSET_CANT;
	    if (now > duration)
		return TRYSET_PASTEOM;

	    if (!isSeekable)
		return TRYSET_CANT;
	    
	    double nowSeconds = (double) now * 1e-9;
	    
	    if (isRandomAccess) {
		amovie.setCurrentPosition(nowSeconds);
		return TRYSET_DONE;
	    } else if (stream != null) {
		if (now != 0) {
		    return TRYSET_CANT;
		}
		// Seeking to zero should be ok.
		((Seekable)stream).seek(0);
		amovie.setCurrentPosition( 0 );
		return TRYSET_DONE;
	    }
	}
	return TRYSET_CANT;
    }

    public float doSetRate(float factor) {
	if (amovie == null)
	    return 1.0F;
	if (factor < 0.1)
	    factor = 0.1f;
	if (factor > 10.0)
	    factor = 10.0f;
	if ((float) amovie.getRate()  != factor) {
	    setRatePending = true;
	    setRateValue = factor;
	}

	return factor;
    }

    Component createVisualComponent() {
	Component c = null;
	Class visclass = null;
	
	//if (BuildInfo.getJavaVendor().indexOf("icrosoft") > 0) {
	    try {
		visclass = Class.forName("com.sun.media.amovie.MSVisualComponent");
	    } catch (Throwable t) {
	    }
	    //}

	if (visclass == null) {
	    try {
		visclass = Class.forName("com.sun.media.amovie.VisualComponent");
	    } catch (Throwable th) {
		return null;
	    }
	}

	Class params [] = { AMController.class };
	Constructor cons = null;
	try {
	    cons = visclass.getConstructor(params);
	    Object [] amparam = new AMController[1];
	    amparam[0] = this;
	    c = (Component) cons.newInstance(amparam);
	    return c;
	} catch (Throwable tr) {
	}
	return null;
    }
    
    public Component getVisualComponent() {
	if (amovie == null)
	    return null;
	if (visualComponent == null) {
	    if (amovie.getVideoWidth() == 0 ||
		amovie.getVideoHeight() == 0)
		visualComponent = null;
	    else {
		visualComponent = createVisualComponent();

		// Component resize listener
		visualComponent.addComponentListener(new ComponentAdapter() {
		    private int lastWidth = -1;
		    private int lastHeight = -1;
		    
		    public void componentResized(ComponentEvent ce) {
			if (amovie != null) {
			    Dimension csize = ce.getComponent().getSize();
			    if (csize.width == lastWidth &&
				csize.height == lastHeight)
				return;
			    lastWidth = csize.width;
			    lastHeight = csize.height;
			    outputSizeSet = true;
			    zoom(lastWidth, lastHeight);
			}
		    }
		} );
		// End resize listener
	    }
	}
	return visualComponent;
    }

    protected Time eomDuration = DURATION_UNKNOWN;
    
    public Time getDuration() {
	// If we've hit the end of media once, use that duration.
	if (eomDuration != DURATION_UNKNOWN)
	    return eomDuration;

	if (source instanceof Duration) {
	    Time d = ((Duration)source).getDuration();
	    if (d != null && d != Duration.DURATION_UNKNOWN &&
		d != Duration.DURATION_UNBOUNDED)
		return d;
	}
	
	if (amovie == null)
	    return Duration.DURATION_UNKNOWN;
	else {
	    double amduration = amovie.getDuration(); // Get the duration in secs
	    if (isRandomAccess)
		return new Time((long) (amduration * 1E+9)); // To nanoseconds
	    else
		return DURATION_UNKNOWN;
	}
    }

    /**
     * Sets the parent for the activemovie window.
     */
    void setOwner(Component parent) {
	try {
	    if (amovie != null && parent != null) {
		appletWindowHandle = com.sun.media.util.WindowUtil.getWindowHandle(parent);
		if (appletWindowHandle == 0) {
		    throw new NullPointerException("null peer");
		}
		amovie.setOwner(appletWindowHandle);
		amovie.setVisible(1);
		parent.getPreferredSize();
		amovie.setWindowPosition(0, 0, outWidth, outHeight);
	    }
	} catch (Throwable t) {
	}
    }

    void sendEOM() {
	if (amovie != null) {
	    amovie.amPause();
	    amovie.pause();
	}
	inEOM = true; // ay: To prevent hanging in doStop after EOM
	super.stop();
	inEOM = false;
	Time earlier = eomDuration;
	eomDuration = new Time(getMediaTime().getNanoseconds());
	startSource(false, false);
	sendEvent(new EndOfMediaEvent(this, Started, Prefetched,
				      getTargetState(), getMediaTime()));
	if (earlier == DURATION_UNKNOWN)
	    sendEvent(new DurationUpdateEvent(this, eomDuration));
	
	if (stream instanceof CachedStream && isRandomAccess == false) {
	    isRandomAccess = true;
	    if (amovie != null)
		amovie.setSeekable(true);
	}
    }

    public boolean startSource(boolean on, boolean regardless) {
	if (sourceIsOn == on)
	    return true;
	synchronized (sourceLock) {
	    if (regardless) {
		try {
		    if (on) {
			source.start();
			if (amovie != null)
			    amovie.stopDataFlow(false);
		    } else {
			if (amovie != null)
			    amovie.stopDataFlow(true);
			source.stop();
		    }
		} catch (Exception ge) {
		    // System.err.println("Couldn't stop the data source");
		    return false;
		}
		sourceIsOn = on;
	    }
	    return true;
	}
    }

    private void zoomChanged() {
       if (amovie == null)
	 return;
       int width = amovie.getVideoWidth();
       int height = amovie.getVideoHeight();
       if (peerExists)
	   amovie.setWindowPosition(0, 0, outWidth, outHeight);
       if (pwidth != width || pheight != height) {
	   pwidth = width;
	   pheight = height;
	   //sendSizeChangeEvent(pwidth, pheight, 1.0F);
       }
    }

    private void zoom(int width, int height) {
	outWidth = width;
	if (outWidth < 120)
	    outWidth = 120;
	outHeight = height;
	if (outHeight < 1)
	    outHeight = 1;
	zoomChanged();
    }
    
    public boolean audioEnabled() {
	if (amovie != null) {
	    return amovie.hasAudio();
	} else
	    return true;
    }

    public boolean videoEnabled() {
	if (amovie != null)
	    return amovie.hasVideo();
	else
	    return true;
    }

    public void gainChange(float g) {
       	if (amovie != null && !muted && gc != null) {
	    float dB = gc.getDB();
	    if (dB > 0.0f) dB = 0.0f;
	    if (dB < -70f) dB = -100f; // silence is -10000 for active movie
	    amovie.setVolume( (int)(dB * 100));
	}
    }

    public void muteChange(boolean state) {
	if (amovie != null) {
	    if (state) {
		muted = true;
		amovie.setVolume(MINVOLUME);
	    } else {
		muted = false;
		try {
		    float dB = gc.getDB();
		    if (dB > 0) dB = 0;
		    if (dB < -70f) dB = -100f;
		    amovie.setVolume( (int)(dB * 100) );
		} catch (Exception e) {
		}
	    }
	}
    }

    /*************************************************************************
     * INNER CLASSES
     *************************************************************************/

    class AMTimeBase extends MediaTimeBase {

	private AMController controller;
	
	public AMTimeBase(AMController controller) {
	    this.controller = controller;
	}

	public long getNanoseconds() {
	    return getMediaTime();
	}
	
	public long getMediaTime() {
	    long time = 0;
	    if (controller.amovie != null)
		time = controller.amovie.getTime() * 1000;
	    return time;
	}
    }
    
    class GCA extends GainControlAdapter {

        GCA() {
	    super(1.0f);
	}

	public void setMute(boolean mute) {
	    super.setMute(mute);
	    muteChange(mute);
	}

	public float setLevel(float g) {
	    float level = super.setLevel(g);
	    gainChange(g);
	    return level;
	}
    }
}

/**
 * This class used to be an inner class, which is the correct thing to do.
 * Changed it to a package private class because of jdk1.2 security.
 * For jdk1.2 and above applets, EventThread is created in a
 * privileged block using jdk12CreateThreadAction. jdk12CreateThreadAction
 * class is unable to create and instantiate an inner class 
 * in AMController class
 */
class EventThread extends LoopThread {
    
    private AMController amController;
    EventThread() {
	setName("JMF AMController Event Thread: " + getName());
    }
    
    void setController(AMController c) {
	amController = c;
    }
    
    public boolean process() {
	if (amController.amovie == null)
	    return false;
	boolean result = amController.amovie.waitForCompletion();
	if (result) {
	    amController.sendEOM();
	    pause();
	}
	try {
	    sleep(100);
	} catch (InterruptedException ie) {
	}
	return true;
    }
}

