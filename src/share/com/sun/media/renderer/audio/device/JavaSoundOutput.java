/*
 * @(#)JavaSoundOutput.java	1.27 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.audio.device;

import java.io.*;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.Encoding;
import com.sun.media.*;
//import javax.sound.sampled.FloatControl.*;
//import javax.sound.sampled.BooleanControl.*;


public class JavaSoundOutput implements AudioOutput {

    static Mixer mixer = null;
    static Object initSync = new Object();

    protected SourceDataLine dataLine;

    protected FloatControl gc;
    protected FloatControl rc;
    protected BooleanControl mc;
    protected boolean paused = true;

    protected int bufSize;
    protected javax.media.format.AudioFormat format;


    public JavaSoundOutput() {
    }


    public boolean initialize(javax.media.format.AudioFormat format, int bufSize) {

      synchronized (initSync) {

	DataLine.Info info;
	javax.sound.sampled.AudioFormat afmt;

	afmt = convertFormat(format);

	//System.err.println("Render buffer size = " + bufSize);

	info = new DataLine.Info(SourceDataLine.class, afmt, bufSize);

	try {

	    if (!AudioSystem.isLineSupported(info)) {
		Log.warning("DataLine not supported: " + format);
		return false;
	    }

	    dataLine = (SourceDataLine)AudioSystem.getLine(info);
	    dataLine.open(afmt, bufSize);

	} catch (Exception e) {
	    Log.warning("Cannot open audio device: " + e);
	    return false;
	}

	this.format = format;
	this.bufSize = bufSize;

	if (dataLine == null) {
	    Log.warning("JavaSound unsupported format: " + format);
	    return false;
	}

	try {
	    gc = (FloatControl)dataLine.getControl(FloatControl.Type.MASTER_GAIN);
	    mc = (BooleanControl)dataLine.getControl(BooleanControl.Type.MUTE);
	} catch (Exception e) {
	    Log.warning("JavaSound: No gain control");
	}

	try {
	    rc = (FloatControl)dataLine.getControl(FloatControl.Type.SAMPLE_RATE);
	} catch (Exception e) {
	    Log.warning("JavaSound: No rate control");
	}

	return true;
      }
    }


    // $$ AudioRenderer's abortPrefetch() calls this method
    public void dispose() {
	dataLine.close();
    }


    public void finalize() throws Throwable {
	super.finalize();
	dispose();
    }


    public void pause() {
	if (dataLine != null)
	    dataLine.stop();
	paused = true;
    }


    public void resume() {
	if (dataLine != null)
	    dataLine.start();
	paused = false;
    }


    // Remove this method
    public void drain() {
	dataLine.drain();
    }


    // Clean the buffer.
    public void flush() {
	dataLine.flush();
    }


    public javax.media.format.AudioFormat getFormat() {
	return format;
    }

    long lastPos = 0;
    long originPos = 0;
    long totalCount = 0;

    public long getMediaNanoseconds() {
	if (dataLine == null || format == null)
	    return 0;

	long pos = dataLine.getFramePosition();

	if (pos < lastPos) {
	    // Wraps around.
	    totalCount += lastPos - originPos; 
	    originPos = pos;
	}

	lastPos = pos;

	return (long)(((totalCount + pos - originPos) * 1000 / format.getSampleRate()) * 1000000);
    }

    public void setGain(double g) {
	if (gc != null)
	    gc.setValue((float)g);
    }


    public double getGain() {
	return (gc != null ? gc.getValue() : 0);
    }


    public void setMute(boolean m) {
	if (mc != null)
	    mc.setValue(m);
    }


    public boolean getMute() {
	return (mc != null ? mc.getValue() : false);
    }


    public float setRate(float r) {
	if (rc == null)
	    return 1.0f;

	float rate = (float)(r * format.getSampleRate());

	if (rate > rc.getMaximum() || rate < rc.getMinimum())
	    return getRate();

	rc.setValue(rate);

	return r;
    }


    public float getRate() {
	if (rc == null)
	    return 1.0f;
	return (float)(rc.getValue() / format.getSampleRate());
    }


    public int bufferAvailable() {
	return dataLine.available();
    }


    public int write(byte data[], int off, int len) {
	return dataLine.write(data, off, len);
    }


    public static boolean isOpen() {
	Mixer mixer = AudioSystem.getMixer(null);
	Line lines[] = mixer.getSourceLines();

	return (lines != null && lines.length > 0);
    }


    /**
     * Convert a JavaSound format to a JMF format.
     */
    public static javax.media.format.AudioFormat convertFormat(
		javax.sound.sampled.AudioFormat fmt) {
	Encoding type = fmt.getEncoding();

	String encoding;
	if (type == Encoding.PCM_SIGNED ||
	    type == Encoding.PCM_UNSIGNED)
	    encoding = javax.media.format.AudioFormat.LINEAR;
	else if (type == Encoding.ALAW)
	    encoding = javax.media.format.AudioFormat.ALAW;
	else if (type == Encoding.ULAW)
	    encoding = javax.media.format.AudioFormat.ULAW;
	else
	    encoding = null;

	return new javax.media.format.AudioFormat(
		encoding,
		fmt.getSampleRate(),
		fmt.getSampleSizeInBits(),
		fmt.getChannels(),
		(fmt.isBigEndian() ?
			javax.media.format.AudioFormat.BIG_ENDIAN :
			javax.media.format.AudioFormat.LITTLE_ENDIAN),
		(type == Encoding.PCM_SIGNED ? 
			javax.media.format.AudioFormat.SIGNED :
			javax.media.format.AudioFormat.UNSIGNED));
    }


    /**
     * Convert a JMF format to a JavaSound format.
     */
    public static javax.sound.sampled.AudioFormat convertFormat(
		javax.media.format.AudioFormat fmt) {

	return new javax.sound.sampled.AudioFormat(
			(fmt.getSampleRate() == 
				javax.media.format.AudioFormat.NOT_SPECIFIED ?
					8000f : (float)fmt.getSampleRate()),
			(fmt.getSampleSizeInBits() == 
				javax.media.format.AudioFormat.NOT_SPECIFIED ?
					16 : fmt.getSampleSizeInBits()), 
			(fmt.getChannels() == 
				javax.media.format.AudioFormat.NOT_SPECIFIED ?
					1 : fmt.getChannels()),
			(fmt.getSigned() == 
				javax.media.format.AudioFormat.SIGNED ?
					true : false),
			(fmt.getEndian() == 
				javax.media.format.AudioFormat.BIG_ENDIAN ?
					true : false));
    }
}

