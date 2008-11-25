/*
 * @(#)AudioOutput.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.audio.device;

import javax.media.format.AudioFormat;


public interface AudioOutput {

    /**
     * Initialize the audio output.
     */
    public boolean initialize(AudioFormat format, int bufferSize);

    /**
     * Close the device.
     * Cannot call this close since it clashes InputStream.close
     * for SunAudioOutput which also implements InputStream.
     */
    public void dispose();

    /**
     * Pause the device.
     */
    public void pause();

    /**
     * Resume the device.
     */
    public void resume();

    /**
     * Drain the device.
     */
    public void drain();

    /**
     * Flush the device.
     */
    public void flush();

    /**
     * Return the time as measured from the samples consumed since the
     * device has opened.
     */
    public long getMediaNanoseconds();

    /**
     * Set the audio gain of the device.
     */
    public void setGain(double g);

    /**
     * Return the audio gain of the device.
     */
    public double getGain();

    /**
     * Mute the audio device.
     */
    public void setMute(boolean m);

    /**
     * Return if the device is muted.
     */
    public boolean getMute();

    /**
     * set the playback rate.
     */
    public float setRate(float rate);

    /**
     * get the playback rate.
     */
    public float getRate();

    /**
     * Obtains the number of bytes of data that can be written to 
     * the device without blocking.
     */
    public int bufferAvailable();

    /**
     * Write data to the device.
     */
    public int write(byte data[], int off, int len);

}
