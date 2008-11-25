/*
 * @(#)JavaSoundSourceStream.java	1.40 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.javasound;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.io.IOException;
import javax.media.*;
import javax.media.protocol.*;
import javax.media.control.*;
import javax.sound.sampled.*;
import com.sun.media.Log;
import com.sun.media.CircularBuffer;
import com.sun.media.protocol.*;
import com.sun.media.util.LoopThread;
import com.sun.media.util.Arch;
import com.sun.media.util.jdk12;
import com.sun.media.ui.AudioFormatChooser;
import com.sun.media.renderer.audio.device.JavaSoundOutput;
import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;


/**
 * JavaSound capture stream.
 */
public class JavaSoundSourceStream extends BasicSourceStream 
	implements PushBufferStream {

    DataSource dsource;
    TargetDataLine dataLine = null;
    javax.media.format.AudioFormat format;
    javax.media.format.AudioFormat devFormat;
    boolean reconnect = false;

    int bufSize;
    BufferTransferHandler transferHandler;
    boolean started = false;
    AudioFormatChooser afc;
    BufferControl bc;
    CircularBuffer cb = new CircularBuffer(1);
    PushThread pushThread = null;


    //PortControl portControl = null;

    // Defaults if nothing is specified.
    static int DefRate = 44100;
    static int DefBits = 16;
    static int DefChannels = 2;
    static int DefSigned = javax.media.format.AudioFormat.SIGNED;
    static int DefEndian = (Arch.isBigEndian() ? 
				javax.media.format.AudioFormat.BIG_ENDIAN :
				javax.media.format.AudioFormat.LITTLE_ENDIAN);
    static int OtherEndian = (Arch.isBigEndian() ? 
				javax.media.format.AudioFormat.BIG_ENDIAN :
				javax.media.format.AudioFormat.LITTLE_ENDIAN);
    static javax.media.Format supported[];
    static protected CaptureDeviceInfo deviceList[];

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

		// The first one is the default.
		new javax.media.format.AudioFormat(
			javax.media.format.AudioFormat.LINEAR,
			44100,
			16, //javax.media.format.AudioFormat.NOT_SPECIFIED,
			2,
			DefEndian,
			DefSigned),
		new javax.media.format.AudioFormat(
			javax.media.format.AudioFormat.LINEAR,
			44100,
			16, //javax.media.format.AudioFormat.NOT_SPECIFIED,
			1,
			DefEndian,
			DefSigned),
		new javax.media.format.AudioFormat(
			javax.media.format.AudioFormat.LINEAR,
			22050,
			16, //javax.media.format.AudioFormat.NOT_SPECIFIED,
			2,
			DefEndian,
			DefSigned),
		new javax.media.format.AudioFormat(
			javax.media.format.AudioFormat.LINEAR,
			22050,
			16, //javax.media.format.AudioFormat.NOT_SPECIFIED,
			1,
			DefEndian,
			DefSigned),
		new javax.media.format.AudioFormat(
			javax.media.format.AudioFormat.LINEAR,
			11025,
			16, //javax.media.format.AudioFormat.NOT_SPECIFIED,
			2,
			DefEndian,
			DefSigned),
		new javax.media.format.AudioFormat(
			javax.media.format.AudioFormat.LINEAR,
			11025,
			16, //javax.media.format.AudioFormat.NOT_SPECIFIED,
			1,
			DefEndian,
			DefSigned),
		new javax.media.format.AudioFormat(
			javax.media.format.AudioFormat.LINEAR,
			8000,
			16, //javax.media.format.AudioFormat.NOT_SPECIFIED,
			2,
			DefEndian,
			DefSigned),
		new javax.media.format.AudioFormat(
			javax.media.format.AudioFormat.LINEAR,
			8000,
			16, //javax.media.format.AudioFormat.NOT_SPECIFIED,
			1,
			DefEndian,
			DefSigned),
	};

	deviceList = new CaptureDeviceInfo [] {
		   	    new CaptureDeviceInfo("JavaSound audio capture",
			    new MediaLocator("javasound://44100"), 
			    supported)
		     };
    }


    public JavaSoundSourceStream(DataSource ds) {
	super(new ContentDescriptor(ContentDescriptor.RAW), LENGTH_UNKNOWN);
	dsource = ds;


	bc = new BC(this);

	controls = new javax.media.Control[2];
	controls[0] = new FC(this);
	controls[1] = bc;
    }


    /**
     * Parse the javasound media locator which specifies the format.
     * A valid media locator is of the form:
     * javasound://[rate]/[sizeInBits]/[channels]/[big|little]/[signed|unsigned]
     */
    static public Format parseLocator(MediaLocator ml) {

	int rate, bits, channels, endian, signed;

	String rateStr = null, bitsStr = null, channelsStr = null;
	String endianStr = null, signedStr = null;

	// Parser the media locator to extract the requested format.

	String remainder = ml.getRemainder();
	if (remainder != null && remainder.length() > 0) {
	    while (remainder.length() > 1 && remainder.charAt(0) == '/')
		remainder = remainder.substring(1);
	    // Now see if there's a sample rate specified.
	    int off = remainder.indexOf('/');
	    if (off == -1) {
		if (!remainder.equals(""))
		    rateStr = remainder;
	    } else {
		rateStr = remainder.substring(0, off);
		remainder = remainder.substring(off + 1);
		// Now see if there's a sample size specified
		off = remainder.indexOf('/');
		if (off == -1) {
		    if (!remainder.equals(""))
			bitsStr = remainder;
		} else {
		    bitsStr = remainder.substring(0, off);
		    remainder = remainder.substring(off + 1);
		    // Now see if there's a channels specified
		    off = remainder.indexOf('/');
		    if (off == -1) {
			if (!remainder.equals(""))
			    channelsStr = remainder;
		    } else {
			channelsStr = remainder.substring(0, off);
			remainder = remainder.substring(off + 1);
			// Now see if there's endian specified.
			off = remainder.indexOf('/');
			if (off == -1) {
			    if (!remainder.equals(""))
				endianStr = remainder;
			} else {
			    endianStr = remainder.substring(0, off);
			    if (!remainder.equals(""))
			        signedStr = remainder.substring(off + 1);
			}
		   }
		}
	    }
	}

	// Sample Rate
	rate = DefRate;
	if (rateStr != null) {
	    try {
		Integer integer = Integer.valueOf(rateStr);
		if (integer != null)
		    rate = integer.intValue();
	    } catch (Throwable t) { }

	    // Range check.
	    if (rate <= 0 || rate > 96000) {
		Log.warning("JavaSound capture: unsupported sample rate: " + rate);
		rate = DefRate;
		Log.warning("        defaults to: " + rate);
	    }
	}

	// Sample Size
	bits = DefBits;
	if (bitsStr != null) {
	    try {
		Integer integer = Integer.valueOf(bitsStr);
		if (integer != null)
		    bits = integer.intValue();
	    } catch (Throwable t) { }

	    // Range check.
	    if (bits != 8 && bits != 16) {
		Log.warning("JavaSound capture: unsupported sample size: " + bits);
		bits = DefBits;
		Log.warning("        defaults to: " + bits);
	    }
	}

	// # of channels
	channels = DefChannels;
	if (channelsStr != null) {
	    try {
		Integer integer = Integer.valueOf(channelsStr);
		if (integer != null)
		    channels = integer.intValue();
	    } catch (Throwable t) { }

	    // Range check.
	    if (channels != 1 && channels != 2) {
		Log.warning("JavaSound capture: unsupported # of channels: " + channels);
		channels = DefChannels;
		Log.warning("        defaults to: " + channels);
	    }
	}

	// Endian
    	endian = DefEndian;
	if (endianStr != null) {
	    if (endianStr.equalsIgnoreCase("big"))
    		endian = javax.media.format.AudioFormat.BIG_ENDIAN;
	    else if (endianStr.equalsIgnoreCase("little"))
		endian = javax.media.format.AudioFormat.LITTLE_ENDIAN;
	    else {
		Log.warning("JavaSound capture: unsupported endianess: " + endianStr);
		Log.warning("        defaults to: big endian");
	    }
	}

	// Signed
    	signed = DefSigned;
	if (signedStr != null) {
	    if (signedStr.equalsIgnoreCase("signed"))
		signed = javax.media.format.AudioFormat.SIGNED;
	    else if (signedStr.equalsIgnoreCase("unsigned"))
    		signed = javax.media.format.AudioFormat.UNSIGNED;
	    else {
		Log.warning("JavaSound capture: unsupported signedness: " + signedStr);
		Log.warning("        defaults to: signed");
	    }
	}

	javax.media.format.AudioFormat fmt;
	fmt = new javax.media.format.AudioFormat(
			javax.media.format.AudioFormat.LINEAR,
			rate, bits, channels, endian, signed);

	return fmt;
    }


    /**
     * Set the capture format.
     */
    public Format setFormat(javax.media.Format fmt) {

	if (started) {
	    Log.warning("Cannot change audio capture format after started.");
	    return format;
	}

	if (fmt == null)
	    return format;

	javax.media.Format f = null;
	for (int i = 0; i < supported.length; i++) {
	    if (fmt.matches(supported[i]) &&
		(f = fmt.intersects(supported[i])) != null) {
		break;
	    }
	}

	if (f == null)
	    return format;

	try {
	    if (devFormat != null) {

		if (!devFormat.matches(f) && !JavaSoundOutput.isOpen()) {
		    // Can't change format if JavaSound is already opened
		    // for rendering.

		    //System.err.println("The capture format has changed.");
		    format = (javax.media.format.AudioFormat)f;
		    disconnect();
		    connect();
		}
	    } else {
		format = (javax.media.format.AudioFormat)f;
		connect();
	    }
	} catch (IOException e) {
	    return null;
	}

	if (afc != null)
	   afc.setCurrentFormat(format);

	return format;
    }


    public boolean isConnected() {
	return devFormat != null;
    }


    /**
     * Connect to the device.  It in turn calls openDev to do the
     * real work.
     */
    public void connect() throws IOException {

	// Return if it's already connected.
	if (isConnected())
	    return;

	if (JavaSoundOutput.isOpen()) {
	    Log.warning("JavaSound is already opened for rendering.  Will capture at the default format.");
	    format = null;
	}

	openDev();

	if (pushThread == null) {

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
		    // TODO: Do the right thing if permissions cannot 
		    // be obtained.
		    // User should be notified via an event
		}
	    }
	
	    if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
		try {
		    Constructor cons = jdk12CreateThreadAction.cons;
		
		    pushThread = (PushThread) jdk12.doPrivM.invoke(
                                           jdk12.ac,
					   new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               PushThread.class,
                                           })});
		} catch (Exception e) {
		    // System.out.println("Exception: creating pushThread");
		}
	    } else {
		pushThread = new PushThread();
	    }

	    pushThread.setSourceStream(this);
	}

	if (reconnect)
	    Log.comment("Capture buffer size: " + bufSize);
	devFormat = format;
	reconnect = false;
    }


    /**
     * Open the capture device.
     */
    void openDev() throws IOException {

	DataLine.Info info;
	javax.sound.sampled.AudioFormat afmt = null;

	if (format != null) {

	    afmt = JavaSoundOutput.convertFormat(format);
	    int chnls = (format.getChannels() == 
			    javax.media.format.AudioFormat.NOT_SPECIFIED ?
					1 : format.getChannels()); 
	    int size = (format.getSampleSizeInBits() == 
			    javax.media.format.AudioFormat.NOT_SPECIFIED ?
					16 : format.getSampleSizeInBits()); 
	    int frameSize = (size * chnls)/8;

	    if (frameSize  == 0)
		frameSize = 1;

	    bufSize = (int)(format.getSampleRate() * frameSize *
					bc.getBufferLength() / 1000);

	    info = new DataLine.Info(TargetDataLine.class, afmt, bufSize);
	} else {
	    info = new DataLine.Info(TargetDataLine.class,
				null, AudioSystem.NOT_SPECIFIED);
	}

	if (!AudioSystem.isLineSupported(info)) {
	    Log.error("Audio not supported: " + info + "\n");
	    throw new IOException("Cannot open audio device for input.");
	}

	try {

	    dataLine = (TargetDataLine)AudioSystem.getLine(info);

	    if (format != null) {
		dataLine.open(afmt, bufSize);
	    } else {
		dataLine.open();
		format = JavaSoundOutput.convertFormat(dataLine.getFormat());
	    }

	    bufSize = dataLine.getBufferSize();

	    //dataLine.start();

	} catch (Exception e) {
	    Log.error("Cannot open audio device for input: " + e);
	    throw new IOException(e.getMessage());
	}
    }

    public void disconnect() {
	if (dataLine == null)
	    return;
	/*
	dataLine.drain();
	*/
	dataLine.stop();
	dataLine.close();

	dataLine = null;
	devFormat = null;
	if (pushThread != null) {
	    pushThread.kill();
	    pushThread = null;
	}
    }


    /**
     * Start capturing.
     */
    public void start() throws IOException {
	if (dataLine == null)
	    throw new IOException("A JavaSound input channel cannot be opened.");
	if (started)
	    return;

	// Check if the GUI control has specified a new format.
	if (afc != null) {
	    Format f;
	    if ((f = afc.getFormat()) != null && !f.matches(format)) {
		if (setFormat(f) == null) {
		    //System.err.println("The chosen format is not supported.");
		}
	    }
	    afc.setEnabled(false);
	}

	// Disconnect the source if the reconnect flag is on.
	if (reconnect)
	    disconnect();

	// Connect the source if it's not already connected.
	if (!isConnected())
	    connect();

	// Flush the old data.
	synchronized (cb) {
	    while (cb.canRead()) {
		cb.read();
		cb.readReport();
	    }
	    cb.notifyAll();
	}

	pushThread.start();
	dataLine.flush();
	dataLine.start();
	started = true;
    }


    /**
     * Stop the capture.
     */
    public void stop() throws IOException {
	if (!started)
	    return;

	pushThread.pause();
	if (dataLine != null)
	    dataLine.stop();
	started = false;
	if (afc != null && !JavaSoundOutput.isOpen())
	    afc.setEnabled(true);
    }


    public Format getFormat() {
	return format;
    }


    public java.lang.Object[] getControls() {
	return controls;
    }


    static public Format[] getSupportedFormats() {
	return supported;
    }


    static public CaptureDeviceInfo [] listCaptureDeviceInfo() {
	return deviceList;
    }


    public void setTransferHandler(BufferTransferHandler th) {
	transferHandler = th;
    }


    public boolean willReadBlock() {
	return !started;
    }

    public void read(Buffer in) {

	Buffer buffer;
	Object data;

	synchronized (cb) {
	    while (!cb.canRead()) {
		try {
		   cb.wait();
		} catch (Exception e) {}
	    }
	    buffer = cb.read();
	}

	// Swap data with the input buffer and my own buffer.
	data = in.getData();
	in.copy(buffer);
	buffer.setData(data);

	synchronized (cb) {
	    cb.readReport();
	    cb.notify();
	}
    }


    /****************************************************************
     * INNER CLASS
     ****************************************************************/


    /**
     * Format control
     */
    class FC implements FormatControl, Owned {

	JavaSoundSourceStream jsss;

	public FC(JavaSoundSourceStream jsss) {
	    this.jsss = jsss;
	}

	public Object getOwner() {
	    return dsource;
	}

	public Format getFormat() {
	    return format;
	}

	public Format setFormat(Format fmt) {
	    return jsss.setFormat(fmt);
	}

	public Format [] getSupportedFormats() {
	    return supported;
	}

	public boolean isEnabled() {
	    return true;
	}

	public void setEnabled(boolean enabled) {
	}

	public java.awt.Component getControlComponent() {
	    if (afc == null) {
		afc = new AudioFormatChooser(supported, format);
		afc.setName("JavaSound");
		if (started || dataLine == null || JavaSoundOutput.isOpen())
		    afc.setEnabled(false);
	    }
	    return afc;
	}
    }


    static int DefaultMinBufferSize = 16;	// millisecs.
    static int DefaultMaxBufferSize = 4000;	// millisecs.
    long bufLenReq = 125;			// 1/8 sec.

    /**
     * BufferControl for the renderer.
     */
    class BC implements BufferControl, Owned {

	JavaSoundSourceStream jsss;

	BC(JavaSoundSourceStream js) {
	   jsss = js;
	} 
  
	public long getBufferLength() {
	    return bufLenReq;
	} 

	public long setBufferLength(long time) {
	    if (time < DefaultMinBufferSize)
		bufLenReq = DefaultMinBufferSize;
	    else if (time > DefaultMaxBufferSize)
		bufLenReq = DefaultMaxBufferSize;
	    else
		bufLenReq = time;
	    Log.comment("Capture buffer length set: " + bufLenReq);
	    reconnect = true;
	    return bufLenReq;
	}
  
	public long getMinimumThreshold() {
	    return 0;
	}

	public long setMinimumThreshold(long time) {
	    return 0;
	}
  
	public void setEnabledThreshold(boolean b) {
	}
  
	public boolean getEnabledThreshold() {
	    return false;
	}

	public java.awt.Component getControlComponent() {
	    return null;
	}

	public Object getOwner() {
	    return dsource;
	}
    }
}


/**
 * The thread that pushes the data.
 */

/**
 * This class used to be an inner class, which is the correct thing to do.
 * Changed it to a package private class because of jdk1.2 security.
 * For jdk1.2 and above applets, PushThread is created in a
 * privileged block using jdk12CreateThreadAction. jdk12CreateThreadAction
 * class is unable to create and instantiate an inner class 
 * in JavaSoundSourceStream class
 */

class PushThread extends LoopThread {
    
    private JavaSoundSourceStream sourceStream;
    private SystemTimeBase systemTimeBase = new SystemTimeBase();
    private long seqNo = 0;
    
    public PushThread() {
	setName("JavaSound PushThread");
    }
    
    void setSourceStream(JavaSoundSourceStream ss) {
	sourceStream = ss;
    }
    
    protected boolean process() {
	
	Buffer buffer;
	byte data[];
	int len;
	CircularBuffer cb = sourceStream.cb;
	BufferTransferHandler transferHandler = sourceStream.transferHandler;
	synchronized (cb) {
	    while (!cb.canWrite()) {
		try {
		    cb.wait();
		} catch (Exception e) {}
	    }
	    buffer = cb.getEmptyBuffer();
	}
	
	if (buffer.getData() instanceof byte[])
	    data = (byte[])buffer.getData();
	else
	    data = null;
	
	if (data == null || data.length < sourceStream.bufSize) {
	    data = new byte[sourceStream.bufSize];
	    buffer.setData(data);
	}
	
	len = sourceStream.dataLine.read(data, 0, sourceStream.bufSize);
	
	buffer.setOffset(0);
	buffer.setLength(len);
	buffer.setFormat(sourceStream.format);
	buffer.setFlags(buffer.FLAG_SYSTEM_TIME | buffer.FLAG_LIVE_DATA);
	buffer.setSequenceNumber(seqNo++);

	// We are assuming very little capture latency.
	buffer.setTimeStamp(systemTimeBase.getNanoseconds());
	
	synchronized (cb) {
	    cb.writeReport();
	    cb.notify();
	
	    if (transferHandler != null)
		transferHandler.transferData(sourceStream);
	}
	
	return true;
    }
}

