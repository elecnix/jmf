/*
 * @(#)DirectAudioRenderer.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.audio;

import java.util.*;
import java.io.*;
import javax.media.*;
import javax.media.format.*;
import javax.media.format.AudioFormat;
import com.sun.media.*;
import com.sun.media.util.*;
import com.sun.media.controls.*;
import com.sun.media.renderer.audio.device.AudioOutput;


/**
 * DirectAudioRenderer to render audio to a device directly.
 * This is used in conjunction DirectAudioOutput.
 * @version
 */

public class DirectAudioRenderer extends AudioRenderer {

    static String NAME = "DirectAudioRenderer";

    public DirectAudioRenderer() {
        super();

	//
	// This line advertizes all the formats that are supported
	// by this renderer.
	// Here we assume the renderer can support all linear/PCM formats.
	//
	supportedFormats = new Format[] {

            new AudioFormat(
                AudioFormat.LINEAR,
		44100,				// Sample rate
		AudioFormat.NOT_SPECIFIED,	// Sample size
		2,	// # of channels
		AudioFormat.LITTLE_ENDIAN,
		AudioFormat.NOT_SPECIFIED
            ),
            new AudioFormat(
                AudioFormat.LINEAR,
		44100,				// Sample rate
		AudioFormat.NOT_SPECIFIED,	// Sample size
		1,	// # of channels
		AudioFormat.LITTLE_ENDIAN,
		AudioFormat.NOT_SPECIFIED
            ),

            new AudioFormat(
                AudioFormat.LINEAR,
		22050,				// Sample rate
		AudioFormat.NOT_SPECIFIED,	// Sample size
		AudioFormat.NOT_SPECIFIED,	// # of channels
		AudioFormat.LITTLE_ENDIAN,
		AudioFormat.NOT_SPECIFIED
            ),

            new AudioFormat(
                AudioFormat.LINEAR,
		11025,		 		// Sample rate
		AudioFormat.NOT_SPECIFIED,	// Sample size
		AudioFormat.NOT_SPECIFIED,	// # of channels
		AudioFormat.LITTLE_ENDIAN,
		AudioFormat.NOT_SPECIFIED
            ),

            new AudioFormat(
                AudioFormat.LINEAR,
		8000,				// Sample rate
		AudioFormat.NOT_SPECIFIED,	// Sample size
		AudioFormat.NOT_SPECIFIED,	// # of channels
		AudioFormat.LITTLE_ENDIAN,
		AudioFormat.NOT_SPECIFIED
            ),
	};

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


    /**
     * Close the renderer and the device.
     */
    public void close() {
        super.close();
    }


    // --- hsy
    public void flush() {
	device.flush();
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


    /**
     * Set the rate of the renderer.
     */
    public float setRate(float factor) {
	// Assume the rate cannot be changed for this renderer.
	return super.setRate(1.0f);
    }


    ///////////////////////////
    //
    // INNER CLASS
    ///////////////////////////


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
	    if (renderer != null && renderer.device != null) {
		renderer.device.setGain(g);
	    }
	    return level;
	}

    }


    /**
     * native method declaraions
     */
    private native boolean nOpen(int rate, int sizeInBits, int channels, int bufSize);
    private native void nClose();
    private native void nPause();
    private native void nResume();
    private native void nDrain();
    private native void nFlush();
    private native void nSetGain(float g);
    private native void nSetMute(boolean m);
    private native long nGetSamplesPlayed();
    private native int nBufferAvailable();
    private native int nWrite(byte data[], int off, int len);

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
    class DirectAudioOutput implements AudioOutput {

	private int bufSize;
	private AudioFormat format;
	private double gain;
	private boolean muted;

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
	    bufSize = (int)(format.getSampleRate() * format.getSampleSizeInBits() * format.getChannels() / 8 / 32);
	    // bufSize = 32;

	    return nOpen((int)format.getSampleRate(),
			format.getSampleSizeInBits(),
			format.getChannels(),
			bufSize);
	}


	/**
	 * Close the device.
	 */
	public void dispose() {
	    nClose();
	}


	public void finalize() throws Throwable {
	    super.finalize();
	    dispose();
	}


	/**
	 * Pause the device.
	 */
	public void pause() {
	    nPause();
	}


	/**
	 * Resume the device.
	 */
	public void resume() {
	    nResume();
	}


	/**
	 * Drain the device.
	 */
	public void drain() {
	    nDrain();
	}


	/**
	 * Flush the device.
	 */
	public void flush() {
	    nFlush();
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
	    long samples = nGetSamplesPlayed();
	    long t = (long)(1000000L * samples / format.getSampleRate()) * 1000L;
	    return t;
	}


	/**
	 * Return the audio gain in units of db.
	 */
	public double getGain() {
	    return gain;
	}


	public void setGain(double g) {
	    nSetGain(gainControl.getLevel());
	    gain = g;
	}


	/**
	 * Mute/unmute the device.
	 */
	public void setMute(boolean m) {
	    nSetMute(m);
	    muted = m;
	}


	/**
	 * Return the mute state of the device.
	 */
	public boolean getMute() {
	    return muted;
	}


	/**
	 * Set the playback rate of the device.
	 */
	public float setRate(float r) {
	    return 1.0f;
	}


	/**
	 * Get the playback rate of the device.
	 */
	public float getRate() {
	    return 1.0f;
	}


	/**
	 * Return the amount of data in bytes that can be sent to the
	 * device via the "write" call without blocking it.
	 */ 
	public int bufferAvailable() {
	    return nBufferAvailable();
	}


	/**
	 * Send the audio data to the device.  This should be a blocking call.
	 */
	public int write(byte data[], int off, int len) {
	  // nDrain();
	    return nWrite(data, off, len);
	}
    }

}
