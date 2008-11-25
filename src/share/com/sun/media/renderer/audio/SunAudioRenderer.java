/*
 * @(#)SunAudioRenderer.java	1.16 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.audio;

import java.util.*;
import java.io.*;
//import java.lang.reflect.Constructor;
import javax.media.*;
import javax.media.format.*;
import javax.media.format.AudioFormat;
import com.sun.media.*;
import com.sun.media.util.*;
import com.sun.media.controls.*;
import com.sun.media.renderer.audio.device.*;


/**
 * SunAudioRenderer
 * @version
 */

public class SunAudioRenderer extends AudioRenderer {

    static String NAME = "SunAudio Renderer";

    static public String vendor = null;
    static public String version = null;
    static public boolean runningOnMac = false;
    static public boolean useSystemTime = false;

    static {
	try {
	    vendor = System.getProperty("java.vendor");
	    version = System.getProperty("java.version");
	    if (vendor != null) {
		vendor = vendor.toUpperCase();
		if (vendor.startsWith("APPLE") && version.startsWith("1.1")) {
		    runningOnMac = true;
		    useSystemTime = true;
		}
	    }
	} catch (Throwable e) {
	    // Non-fatal error.  No need to do anything.
	}
    }

    private BasicClock clock = null;
    private long startMediaTime = 0;

    public SunAudioRenderer() {
        super();

	if (useSystemTime) {
	    timeBase = new SystemTimeBase();
	    clock = new BasicClock();
	}

	supportedFormats = new Format[1];
        supportedFormats[0] = new AudioFormat(
                AudioFormat.ULAW,
                8000,
                8,
                1,
                Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED
            );

	gainControl = new MCA(this);
    }

    public String getName() {
	return NAME;
    }


    public void open() throws ResourceUnavailableException {
	if (!grabDevice()) {
	    throw new ResourceUnavailableException("AudioRenderer: Failed to initialize audio device.");
	}
    }

    public void close() {
        super.close();
    }


    protected AudioOutput createDevice(AudioFormat format) {
	return (new SunAudioOutput());
    }


    private static synchronized boolean grabDevice() {

	/*
	  The following lines cause JMF to crash on the Mac.
	  Check to make sure we are not running on the Mac.
	*/

	if (runningOnMac) {
	    if (!sun.audio.AudioPlayer.player.isAlive()) {
                System.out.println("Audio device is busy");
                return false;
            }
	}
	return true;
    }


    /**
     * Specialized clock methods to use system timebase instead
     * of audio timebase for the mac.
     */
    public void setTimeBase(TimeBase master) throws IncompatibleTimeBaseException {
	if (useSystemTime) {
	    if (!(master instanceof SystemTimeBase)) {
		Log.warning("AudioRenderer cannot be controlled by time bases other than its own: " + master);
		/**
		Silently allows the time base to be set to make
		addController slightly more useful.
	 	-ivg
		throw new IncompatibleTimeBaseException();
		*/
	    }
	    clock.setTimeBase(master);
	} else {
	    super.setTimeBase(master);
	}
    }


    public void syncStart(Time at) {
	super.syncStart(at);
	if (useSystemTime)
	    clock.syncStart(at);
    }


    public void stop() {
	super.stop();
	if (useSystemTime)
	    clock.stop();
    }


    public void setStopTime(Time t) {
	if (useSystemTime)
	    clock.setStopTime(t);
	else
	    super.setStopTime(t);
    }


    public Time getStopTime() {
	if (useSystemTime)
	    return clock.getStopTime();
	else
	    return super.getStopTime();
    }


    public void setMediaTime(Time now) {
	if (useSystemTime) {
	    clock.setMediaTime(now);
	    startMediaTime = now.getNanoseconds();
	} else
	    super.setMediaTime(now);
    }


    public Time getMediaTime() {
	return (useSystemTime ? clock.getMediaTime() : super.getMediaTime());
    }


    static public long DEVICE_LATENCY = (runningOnMac ? 7000000000L : 0);

    public long getMediaNanoseconds() {
	if (useSystemTime) {
	    long t = clock.getMediaNanoseconds();
	    if (t - startMediaTime < DEVICE_LATENCY) {
		return startMediaTime;
	    }
	    return t - DEVICE_LATENCY;
	}
	return super.getMediaNanoseconds();
    }


    public Time getSyncTime() {
	return (useSystemTime ? clock.getSyncTime() : super.getSyncTime());
    }


    public TimeBase getTimeBase() {
	return (useSystemTime ? clock.getTimeBase() : super.getTimeBase());
    }


    public Time mapToTimeBase(Time t) throws ClockStoppedException {
	return (useSystemTime ? clock.mapToTimeBase(t) : super.mapToTimeBase(t));
    }


    public float getRate() {
	return (useSystemTime ? clock.getRate() : super.getRate());
    }


    public float setRate(float factor) {
	// sun.audio does not support rate change.
	return super.setRate(1.0f);
    }



    class MCA extends GainControlAdapter {

	AudioRenderer renderer;

	protected MCA(AudioRenderer r) {
	    super(false);
	    renderer = r;
	}

	public void setMute(boolean mute) {
	    if (renderer != null && renderer.device != null)
		renderer.device.setMute(mute);
	    super.setMute(mute);
	}

        public float getLevel() {
	  return -1f; // this is a dummy negative value for GainControlComponent usage
       }

    }
}
