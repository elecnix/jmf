/*
 * @(#)WaveFormatEx.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.vfw;

public class WaveFormatEx {

    public int wFormatTag;
    public int nChannels;
    public int nSamplesPerSec;
    public int nAvgBytesPerSec;
    public int nBlockAlign;
    public int wBitsPerSample;
    public int cbSize;

    public WaveFormatEx() {
    }
}
