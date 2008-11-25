/*
 * @(#)CapStatus.java	1.4 03/04/25
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.vfw;

class CapStatus {

    int uiImageWidth;
    int uiImageHeight;
    boolean fLiveWindow;
    boolean fOverlayWindow;
    boolean fScale;
    int ptScrollX, ptScrollY;
    boolean fUsingDefaultPalette;
    boolean fAudioHardware;
    boolean fCapFileExists;
    int dwCurrentVideoFrame;
    int dwCurrentVideoFramesDropped;
    int dwCurrentWaveSamples;
    int dwCurrentTimeElapsedMS;
    int hPalCurrent;
    boolean fCapturingNow;
    int dwReturn;
    int wNumVideoAllocated;
    int wNumAudioAllocated;

    CapStatus() {
    }
}
