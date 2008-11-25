/*
 * @(#)DirectAudioRenderer.java	1.18 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.audio;

import java.util.*;
import java.io.*;
import javax.media.*;
import javax.media.format.*;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import com.sun.media.*;
import com.sun.media.util.*;
import com.sun.media.controls.*;
import com.sun.media.renderer.audio.device.AudioOutput;

import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;
import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;


/**
 * DirectAudioRenderer to render audio to a device directly.
 * This is used in conjunction DirectAudioOutput.
 * @version
 */

public class DirectAudioRenderer extends AudioRenderer
implements Runnable, ExclusiveUse {

    static String NAME = "DirectSound Renderer";

    public static final int REQ_OPEN = 1;
    public static final int REQ_START = 2;
    public static final int REQ_STOP = 3;
    public static final int REQ_FLUSH = 4;
    public static final int REQ_DRAIN = 5;
    public static final int REQ_CLOSE = 6;
    public static final int REQ_SETGAIN = 7;
    public static final int REQ_SETMUTE = 8;
    public static final int REQ_SETRATE = 9;
    public static final int REQ_WRITE = 10;
    public static final int REQ_AVAILABLE = 11;
    public static final int REQ_TIME = 12;

    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege=false;
    private Method m[] = new Method[1];
    private Class cl[] = new Class[1];
    private Object args[][] = new Object[1][0];

    private Format ulawFormat, linearFormat;
    private Codec ulawDecoder;
    private Format ulawOutputFormat;
    
    static {
	try {
	    jmfSecurity = JMFSecurityManager.getJMFSecurity();
	    securityPrivelege = true;
	} catch (SecurityException e) {
	}
    }

    public DirectAudioRenderer() {
        super();

	//
	// This line advertizes all the formats that are supported
	// by this renderer.
	// Here we assume the renderer can support all linear/PCM formats.
	//
	supportedFormats = new Format[2];
	ulawFormat = new AudioFormat(AudioFormat.ULAW);
 	linearFormat = new AudioFormat(AudioFormat.LINEAR);

        supportedFormats[0] = new AudioFormat(
                AudioFormat.LINEAR,
		AudioFormat.NOT_SPECIFIED,	// Sample rate
		AudioFormat.NOT_SPECIFIED,	// Sample size
		AudioFormat.NOT_SPECIFIED,	// # of channels
		AudioFormat.NOT_SPECIFIED,
		AudioFormat.NOT_SPECIFIED
            );
	supportedFormats[1] = ulawFormat;

	gainControl = new MCA(this);
    }

    public String getName() {
	return NAME;
    }


    public void open() throws ResourceUnavailableException {
	if (device == null && inputFormat != null) {
	    if (!initDevice(inputFormat))
		throw new ResourceUnavailableException("Cannot intialize audio device for playback");
	}
    }

    protected boolean initDevice(AudioFormat in) {
	Format newInput = in;

	// Free the old ulaw decoder if there's one.
	if (ulawDecoder != null) {
	    ulawDecoder.close();
	    ulawDecoder = null;
	}

	// Initialize a ulaw decoder if the input format is ulaw.
	Format outs[] = new Format[1];
	if (ulawFormat.matches(in)) {

	    ulawDecoder = SimpleGraphBuilder.findCodec(in, linearFormat, null, outs);
	    if (ulawDecoder != null) {
		ulawOutputFormat = newInput = outs[0];
	    } else
		return false;
	}

	devFormat = in;

	return super.initDevice((AudioFormat)newInput);
    }

    public boolean isExclusive() {
	return false;
    }

    /**
     * Close the renderer and the device.
     */
    public void close() {
        super.close();
    }

    Buffer decodeBuffer = null;

    public int processData(Buffer buffer) {
	if (!checkInput(buffer))
	    return BUFFER_PROCESSED_FAILED;

	// Process linear data
	if (ulawDecoder == null) {
	    return super.doProcessData(buffer);
	}

	// Pre-processing ulaw data, then feed it into JavaSound.
	if (decodeBuffer == null) {
	    decodeBuffer = new Buffer();
	    decodeBuffer.setFormat(ulawOutputFormat);
	}

	decodeBuffer.setLength(0);
	decodeBuffer.setOffset(0);
	decodeBuffer.setFlags(buffer.getFlags());
	decodeBuffer.setTimeStamp(buffer.getTimeStamp());
	decodeBuffer.setSequenceNumber(buffer.getSequenceNumber());

	int rc = ulawDecoder.process(buffer, decodeBuffer);

	if (rc == BUFFER_PROCESSED_OK) {
	    return super.doProcessData(decodeBuffer);
	}

	return BUFFER_PROCESSED_FAILED;
    }	


    /**
     * Create the device - DirectAudioOutput.
     */
    protected AudioOutput createDevice(AudioFormat format) {
	return (new DirectAudioOutput());
    }


    /**
     * Grab the audio device without opening it.
     */
    private static synchronized boolean grabDevice() {
	// If the device can be claimed without being opened,
	// then fill in this method.
	return true;
    }

    public void run() {
	((Runnable) device).run();
    }

    Thread chpThread() {
	Thread thread = null;
	
	if ( /*securityPrivelege && */ (jmfSecurity != null) ) {
	    String permission = null;
	    try {
		if (jmfSecurity.getName().startsWith("jmf-security")) {
		    permission = "thread";
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.THREAD);
		    m[0].invoke(cl[0], args[0]);
		    
		    permission = "thread group";
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.THREAD_GROUP);
		    m[0].invoke(cl[0], args[0]);
		} else if (jmfSecurity.getName().startsWith("internet")) {
		    PolicyEngine.checkPermission(PermissionID.THREAD);
		    PolicyEngine.assertPermission(PermissionID.THREAD);
		}
	    } catch (Throwable e) {
		if (JMFSecurityManager.DEBUG) {
		    System.err.println("Unable to get " + permission +
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
		
		thread = (Thread) jdk12.doPrivM.invoke(
					   jdk12.ac,
					   new Object[] {
					       cons.newInstance(
							new Object[] {
							    Thread.class,
							    this
							})});
		
		thread.setName("DirectSound Request Thread");
		
		cons = jdk12PriorityAction.cons;
		jdk12.doPrivM.invoke(
				     jdk12.ac,
				     new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               thread,
                                               new Integer(Thread.MAX_PRIORITY)
                                           })});
	    } catch (Exception e) {
		e.printStackTrace();
	    }
 	} else {
	    thread = new Thread(this);
	    thread.setName("DirectSound Request Thread");
	    thread.setPriority(Thread.MAX_PRIORITY);
 	}
	return thread;
    }


    /****************************************************************
     * INNER CLASSES
     ****************************************************************/
    
    /**
     * Set the gain for this renderer.
     */
    class MCA extends GainControlAdapter {

	AudioRenderer renderer;

	protected MCA(AudioRenderer r) {
	    super(1.0f);
	    renderer = r;
	}

	/**
	 * Muting the device.
	 */
	public void setMute(boolean mute) {
	    if (renderer != null && renderer.device != null)
		renderer.device.setMute(mute);
	    super.setMute(mute);
	}

	/**
	 * Set the gain of the device.
	 */
	public float setLevel(float g) {
	    float level = super.setLevel(g);
	    g = getDB();
	    if (renderer != null && renderer.device != null) {
		renderer.device.setGain(g);
	    }
	    return level;
	}

    }


    /**
     * native method declaraions
     */
    private native int nOpen(int rate, int sizeInBits, int channels, int bufSize);
    private native void nClose(int peer);
    private native void nPause(int peer);
    private native void nResume(int peer);
    private native void nDrain(int peer);
    private native void nFlush(int peer);
    private native void nSetGain(int peer, float g);
    private native void nSetMute(int peer, boolean m);
    private native long nGetSamplesPlayed(int peer);
    private native int nBufferAvailable(int peer);
    private native int nWrite(int peer, byte data[], int off, int len,
			      boolean swapBytes, boolean signChange);
    private native void nCheckUnderflow(int peer);
    private native boolean nSetFrequency(int peer, int frequency);

    static boolean loaded = false;
    private int nativeData = 0;		// Pointer to the native decoder.

    static {
	if (!loaded) {
	    try {
		JMFSecurityManager.loadLibrary("jmdaud");
		loaded = true;
	    } catch (UnsatisfiedLinkError e) {
		loaded = false;
	    }
	}
    }


    /**
     * This is the low-level audio device wrapper class.
     * It assumes the native code is written in a library call jmdaud.
     */
    class DirectAudioOutput implements AudioOutput, Runnable {

	private int bufSize;
	private AudioFormat format;
	private float gain;
	private boolean muted;
	private int request = 0;
	private Integer reqLock = new Integer(0);
	private boolean response = false;
	private Thread reqThread = null;
	private float rate = 1.0f;
	private boolean swapBytes = false;
	private boolean signChange = false;
	private float reqRate = 1.0f;
	private byte[] writeData;
	private int writeOffset;
	private int writeLen;
	private int writeResponse;
	private long nanoseconds;
	private int bufferAvailable;
	private boolean started = false;
	private Integer writeLock = new Integer(1);
	
	public DirectAudioOutput() {
	}

	/**
	 * Initialize the audio device to the specified format.
	 */
	public boolean initialize(AudioFormat format, int bufSize) {

	    // Native libraries cannot be loaded.
	    if (!loaded)
		return false;

	    // The higher level class (DirectAudioRenderer) has already 
	    // screen the format.  It should be one of the supported
	    // formats as advertized.
	    this.format = format;

	    // 1/32 of a second.
	    this.bufSize = (int)(format.getSampleRate() * format.getSampleSizeInBits() * format.getChannels() / 8 / 32);

	    if (reqThread == null) {
		reqThread = chpThread();
		reqThread.start();
	    }

	    if (format.getEndian() == AudioFormat.BIG_ENDIAN &&
		format.getSampleSizeInBits() == 16)
		swapBytes = true;
	    else
		swapBytes = false;
	    if (format.getSigned() == AudioFormat.SIGNED &&
		format.getSampleSizeInBits() == 8)
		signChange = true;
	    else
		signChange = false;
	    makeRequest(REQ_OPEN);
	    waitForResponse();
	    return nativeData != 0;
	}

	private void makeRequest(int request) {
	    synchronized (reqLock) {
		this.request = request;
		response = false;
		reqLock.notifyAll();
	    }
	}

	private void waitForResponse() {
	    synchronized (reqLock) {
		while (response == false) {
		    try {
			reqLock.wait(50);
		    } catch (InterruptedException ie) {
		    }
		}
	    }
	}

	public void run() {
	    boolean done = false;
	    int reqCopy;
	    //Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
	    while (!done) {
		synchronized (reqLock) {
		    try {
			if (request == 0)
			    reqLock.wait(50);
		    } catch (InterruptedException ie) {
		    }
		    reqCopy = request;
		    request = 0;
		}
		//System.err.println("reqCopy = " + reqCopy);
		switch (reqCopy) {
		case REQ_OPEN:
		    nativeData = nOpen((int)format.getSampleRate(),
				       format.getSampleSizeInBits(),
				       format.getChannels(),
				       bufSize);
		    //System.err.println("Trying to open " + nativeData);
		    break;

		case REQ_START:
		    nResume(nativeData);
		    break;
		    
		case REQ_STOP:
		    nPause(nativeData);
		    break;
		    
		case REQ_FLUSH:
		    nFlush(nativeData);
		    break;
		    
		case REQ_DRAIN:
		    nDrain(nativeData);
		    break;
		    
		case REQ_SETGAIN:
		    nSetGain(nativeData, gain);
		    break;
		case REQ_SETMUTE:
		    nSetMute(nativeData, muted);
		    break;
		    
		case REQ_SETRATE:
		    doSetRate(reqRate);
		    break;
		    
		case REQ_WRITE:
		    writeResponse = nWrite(nativeData,
					   writeData, writeOffset, writeLen,
					   swapBytes, signChange);
		    break;
		    
		case REQ_TIME:
		    {
			long samples = nGetSamplesPlayed(nativeData);
			nanoseconds = (long)(1000000L * samples /
					     format.getSampleRate()) * 1000L;
		    }
		    break;
		    
		case REQ_AVAILABLE:
		    bufferAvailable = nBufferAvailable(nativeData);
		    break;
		    
		case REQ_CLOSE:
		    synchronized (writeLock) {
			nClose(nativeData);
			nativeData = 0;
			done = true;
		    }
		    break;
		default:
		    //System.err.println("Idle");
		    if (started) {
			synchronized (writeLock) {
			    nCheckUnderflow(nativeData);
			}
		    }
		}
		if (reqCopy > 0) {
		    synchronized (reqLock) {
			response = true;
			reqLock.notifyAll();
		    }
		}
	    }
	    reqThread = null;
	}
	
	/**
	 * Close the device.
	 */
	public synchronized void dispose() {
	    if (nativeData != 0) {
		makeRequest(REQ_CLOSE);
		waitForResponse();
	    }
	}

	/**
	 * Pause the device.
	 */
	public synchronized void pause() {
	    makeRequest(REQ_STOP);
	    waitForResponse();
	    started = false;
	}


	/**
	 * Resume the device.
	 */
	public synchronized void resume() {
	    makeRequest(REQ_START);
	    waitForResponse();
	    started = true;
	}


	/**
	 * Drain the device.
	 */
	public synchronized void drain() {
	    makeRequest(REQ_DRAIN);
	    waitForResponse();
	    started = false;
	}


	/**
	 * Flush the device.
	 */
	public synchronized void flush() {
	    makeRequest(REQ_FLUSH);
	    waitForResponse();
	    started = false;
	}


	/**
	 * Retrieve the audio format.
	 */
	public AudioFormat getFormat() {
	    return format;
	}


	/**
	 * Return the amount of audio samples played so far in units of
	 * nanoseconds.  This method should return a number that's montonically
	 * increasing.  It should get the samples played so far from
	 * the audio device translate that into nanoseconds.
	 */
	public long getMediaNanoseconds() {
	    long samples = nGetSamplesPlayed(nativeData);
	    nanoseconds = (long)(1000000L * samples /
				 format.getSampleRate()) * 1000L;
	    //makeRequest(REQ_TIME);
	    //waitForResponse();
	    return nanoseconds;
	}


	/**
	 * Return the audio gain in units of db.
	 */
	public double getGain() {
	    return gain;
	}


	public synchronized void setGain(double g) {
	    gain = (float) g;
	    makeRequest(REQ_SETGAIN);
	    waitForResponse();
	}


	/**
	 * Mute/unmute the device.
	 */
	public synchronized void setMute(boolean m) {
	    muted = m;
	    makeRequest(REQ_SETMUTE);
	    waitForResponse();
	}


	/**
	 * Return the mute state of the device.
	 */
	public boolean getMute() {
	    return muted;
	}

	public float doSetRate(float r) {
	    // DirectSound currently handles only between 100 Hz and 100KHz
	    if (r * format.getSampleRate() > 100000) {
		r = (float) 100000 / (float) format.getSampleRate();
	    }

	    if (r < 0.1f)
		r = 0.1f;

	    int sampFreq = (int) (r * format.getSampleRate());
	    if (nSetFrequency(nativeData, sampFreq))
		rate = r;
	    else {
		rate = 1.0f;
		r = 1.0f;
	    }
	    return r;
	}

	/**
	 * Set the playback rate of the device.
	 */
	public synchronized float setRate(float r) {
	    reqRate = r;
	    makeRequest(REQ_SETRATE);
	    waitForResponse();
	    return rate;
	}

	/**
	 * Get the playback rate of the device.
	 */
	public float getRate() {
	    return rate;
	}

	/**
	 * Return the amount of data in bytes that can be sent to the
	 * device via the "write" call without blocking it.
	 */
	public synchronized int bufferAvailable() {
	    makeRequest(REQ_AVAILABLE);
	    waitForResponse();
	    return bufferAvailable;
	}


	/**
	 * Send the audio data to the device.  This should be a blocking call.
	 */
	public int write(byte data[], int off, int len) {
	    writeData = data;
	    writeOffset = off;
	    writeLen = len;
	    synchronized (writeLock) {
		writeResponse = nWrite(nativeData,
				       writeData, writeOffset, writeLen,
				       swapBytes, signChange);
	    }
	    //makeRequest(REQ_WRITE);
	    //waitForResponse();
	    return writeResponse;
	}
    }
}
