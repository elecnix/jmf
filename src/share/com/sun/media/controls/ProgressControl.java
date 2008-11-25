/*
 * @(#)ProgressControl.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import com.sun.media.*;

public interface ProgressControl extends GroupControl {

    /**
     * A StringControl that displays the instantaneous frame rate, if video
     * is present.
     */
    StringControl getFrameRate();

    /**
     * A StringControl that displays the instantaneous bandwidth of the
     * input stream.
     */
    StringControl getBitRate();

    /**
     * Displays the video properties such as size, compression type, etc.
     * which are specific to the incoming video stream.
     */
    StringControl getVideoProperties();

    StringControl getVideoCodec();

    StringControl getAudioCodec();

    /**
     * Displays the audio properties such as sampling rate, resolution,
     * compression type, etc. specific to the incoming audio stream.
     */
    StringControl getAudioProperties();
}
