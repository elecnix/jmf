/*
 * @(#)VCapability.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.v4l;

/**
 * Class that mirrors the video_capability structure of Video4Linux
 */
public class VCapability {

    /** Name of the device */
    public String name = null;
    /** Type of the device. Values are VID_TYPE_* */
    public int type = 0;
    /** Number of channels (ports). */
    public int channels = 0;
    /** Number of audio devices. */
    public int audios = 0;
    /** Max video width that can be captured. */
    public int maxwidth = 0;
    /** Max video height that can be captured. */
    public int maxheight = 0;
    /** Min video width that can be captured. */
    public int minwidth = 0;
    /** Min video height that can be captured. */
    public int minheight = 0;

    public static final int VID_TYPE_CAPTURE = 1;
    public static final int VID_TYPE_TUNER   = 2;
    
    public VCapability() {
    }

    /** Returns true if the driver can capture to memory. */
    public boolean hasCapture() {
	return (type & VID_TYPE_CAPTURE) > 0;
    }

    /** Returns true if the driver has a TV Tuner. */
    public boolean hasTuner() {
	return (type & VID_TYPE_TUNER) > 0;
    }
}
