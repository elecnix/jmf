/*
 * @(#)VChannel.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.v4l;

/** Class that mirrors the video_channel structure of Video4Linux. */
public class VChannel {

    /** The channel number. */
    public int channel;
    /** The name for the channel. */
    public String name;
    /** The number of tuners. */
    public int tuners;
    /** Flags can be VIDEO_VC_* */
    public int flags;
    /** Type can be VIDEO_TYPE_* */
    public int type;
    /** PAL/NTSC/SECAM */
    public int norm;

    public static final int VIDEO_VC_TUNER = 1;
    public static final int VIDEO_VC_AUDIO = 2;

    public static final int VIDEO_TYPE_TV = 1;
    public static final int VIDEO_TYPE_CAMERA = 2;

    public static final int VIDEO_NORM_PAL = 0;
    public static final int VIDEO_NORM_NTSC = 1;
    
    public VChannel(int channel) {
	this.channel = channel;
    }

    public boolean hasTuner() {
	return (flags & VIDEO_VC_TUNER) > 0;
    }

    public boolean hasAudio() {
	return (flags & VIDEO_VC_AUDIO) > 0;
    }

    public boolean isTVInput() {
	return type == VIDEO_TYPE_TV;
    }

    public boolean isCameraInput() {
	return type == VIDEO_TYPE_CAMERA;
    }
}
    
