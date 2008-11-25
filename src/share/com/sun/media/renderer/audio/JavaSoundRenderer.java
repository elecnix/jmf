/*
 * @(#)JavaSoundRenderer.java	1.34 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package com.sun.media.renderer.audio;

import java.util.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Constructor;
import javax.media.*;
import javax.media.format.*;
import javax.media.format.AudioFormat;
import com.sun.media.*;
import com.sun.media.util.*;
import com.sun.media.controls.*;
import com.sun.media.renderer.audio.device.*;
import com.ibm.media.codec.audio.ulaw.JavaDecoder;


/**
 * JavaSoundRenderer
 * @version
 */

public class JavaSoundRenderer extends AudioRenderer implements ExclusiveUse {

    static String NAME = "JavaSound Renderer";

    Codec ulawDecoder;
    javax.media.Format ulawOutputFormat;
    javax.media.Format ulawFormat, linearFormat;
    static int METERHEIGHT = 4;
    static boolean available = false;
    
    static {
	String javaVersion = null;
	String subver = null;
	int len;

	try {
	    javaVersion = (String)System.getProperty("java.version");
            if ( javaVersion.length() < 3 )
		len = javaVersion.length();
	    else 
		len = 3;
	    subver = javaVersion.substring(0,len);
	} catch (Throwable t) {
	    javaVersion = null;
	    subver = null;
	}

	if ( (subver == null) || (subver.compareTo("1.3") <  0)) {
	    try {
		JMFSecurityManager.loadLibrary("jmutil");
		JMFSecurityManager.loadLibrary("jsound");
		available = true;
	    } catch (Throwable t) {
	    }
	} else {
	    available = true;
	}
    }

    public JavaSoundRenderer() {
        super();

	if (!available)
	    throw new UnsatisfiedLinkError("No JavaSound library");
	
	ulawFormat = new javax.media.format.AudioFormat(
				AudioFormat.ULAW);
 	linearFormat = new javax.media.format.AudioFormat(
				AudioFormat.LINEAR);
	supportedFormats = new javax.media.Format[2];
	supportedFormats[0] = linearFormat;
	supportedFormats[1] = ulawFormat;

	gainControl = new GCA(this);
	peakVolumeMeter = new PeakVolumeMeter(this);
    }

    public String getName() {
	return NAME;
    }


    public void open() throws ResourceUnavailableException {
	if (device == null && inputFormat != null) {
	    if (!initDevice(inputFormat))
		throw new ResourceUnavailableException("Cannot intialize audio device for playback");
	    device.pause();
	}
    }

    public boolean isExclusive() {
	// JavaSound can mix audio
	return false;
    }
    
    protected boolean initDevice(javax.media.format.AudioFormat in) {

	javax.media.Format newInput = in;

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

	return super.initDevice((javax.media.format.AudioFormat)newInput);
    }

    protected AudioOutput createDevice(javax.media.format.AudioFormat format) {
	return new JavaSoundOutput();
    }

    Buffer decodeBuffer = null;

    public int processData(Buffer buffer) {

	if (!checkInput(buffer))
	    return BUFFER_PROCESSED_FAILED;

	// Processing linear data
	if (ulawDecoder == null) {
	    try {
		((PeakVolumeMeter)peakVolumeMeter).processData(buffer);
	    } catch (Throwable t) {
		t.printStackTrace();
	    }
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
	    try {
		((PeakVolumeMeter)peakVolumeMeter).processData(decodeBuffer);
	    } catch (Throwable t) {
		System.err.println(t);
	    }
	    return super.doProcessData(decodeBuffer);
	}

	return BUFFER_PROCESSED_FAILED;
    }

    public Object [] getControls() {
	Control c[] = new Control[] { 
	    gainControl,
	    bufferControl,
	    peakVolumeMeter
	};
	return c;
    }

    class GCA extends GainControlAdapter {

	AudioRenderer renderer;

	protected GCA(AudioRenderer r) {
	    super(false);
	    renderer = r;
	}

	public void setMute(boolean mute) {
	    if (renderer != null && renderer.device != null)
		renderer.device.setMute(mute);
	    super.setMute(mute);
	}

	public float setLevel(float g) {
	    float level = super.setLevel(g);
	    if (renderer != null && renderer.device != null)
		renderer.device.setGain(getDB());
	    return level;
	}
    }

    class PeakVolumeMeter implements Control, Owned {

	int averagePeak = 0;
	int lastPeak = 0;

	Panel component = null;
	Checkbox cbEnabled = null;
	Canvas canvas = null;
	AudioRenderer renderer;
	long lastResetTime;
	Graphics cGraphics = null;
	
	public PeakVolumeMeter(AudioRenderer r) {
	    this.renderer = r;
	     lastResetTime = System.currentTimeMillis();
	}

	public Object getOwner() {
	    return renderer;
	}

	public Component getControlComponent() {
	    if (component == null) {
		canvas = new Canvas() {
		    public Dimension getPreferredSize() {
			return new Dimension(102, METERHEIGHT);
		    }
		};
		cbEnabled = new Checkbox("Peak Volume Meter", false);
		component = new Panel();
		component.add(cbEnabled);
		component.add(canvas);
		canvas.setBackground(Color.black);
	    }
	    return component;
	}

	public void processData(Buffer buf) {
	    AudioFormat af = (AudioFormat) buf.getFormat();
	    int index = 0;
	    int peak = 0;
	    int inc = 2;
	    if (component == null)
		return;
	    if (!cbEnabled.getState())
		return;
	    byte [] data = (byte[]) buf.getData();
	    boolean signed;
	    if (buf.isDiscard())
		return;
	    if (buf.getLength() <= 0)
		return;
	    if (af.getEndian() == AudioFormat.LITTLE_ENDIAN)
		index = 1;
	    
	    signed = af.getSigned() == AudioFormat.SIGNED;
	    if (af.getSampleSizeInBits() == 8)
		inc = 1;
	    if (signed) {
		for (int i = index; i < buf.getLength(); i += (inc * 5)) {
		    int d = data[i];
		    if (d < 0)
			d = -d;
		    if (d > peak)
			peak = d;
		}
		peak = (peak * 100) / 127;
	    } else {
		for (int i = index; i < buf.getLength(); i += (inc * 5)) {
		    if ((data[i] & 0xFF) > peak)
			peak = (data[i] & 0xFF);
		}
		peak = (peak * 100) / 255;
	    }
	    averagePeak = (peak + averagePeak) / 2;
	    long currentTime = System.currentTimeMillis();
	    if (currentTime > lastResetTime + 100) {
		lastResetTime = currentTime;
		updatePeak(averagePeak);
		averagePeak = peak;
	    }
	}

	private void updatePeak(int newPeak) {
	    if (canvas == null)
		return;
	    if (cGraphics == null) {
		cGraphics = canvas.getGraphics();
	    }
	    if (cGraphics == null)
		return;
	    if (newPeak > 99)
		newPeak = 99;
	    cGraphics.setColor(Color.green);
	    if (newPeak < 80) {
		cGraphics.drawLine(1, 1, newPeak + 1, 1);
		cGraphics.drawLine(1, 2, newPeak + 1, 2);
	    } else {
		cGraphics.drawLine(1, 1, 80 + 1, 1);
		cGraphics.drawLine(1, 2, 80 + 1, 2);
		cGraphics.setColor(Color.yellow);
		if (newPeak < 90) {
		    cGraphics.drawLine( 80 + 1, 1, newPeak + 1, 1);
		    cGraphics.drawLine( 80 + 1, 2, newPeak + 1, 2);
		} else {
		    cGraphics.drawLine( 80 + 1, 1, 90 + 1, 1);
		    cGraphics.drawLine( 80 + 1, 2, 90 + 1, 2);
		    cGraphics.setColor(Color.red);
		    cGraphics.drawLine( 90 + 1, 1, newPeak + 1, 1);
		    cGraphics.drawLine( 90 + 1, 2, newPeak + 1, 2);
		}
	    }
	    cGraphics.setColor(Color.black);
	    cGraphics.drawLine( newPeak + 2, 1, 102, 1);
	    cGraphics.drawLine( newPeak + 2, 2, 102, 2);
	    lastPeak = newPeak;
	}
    }
}
