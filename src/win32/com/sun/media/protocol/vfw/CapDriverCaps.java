/*
 * @(#)CapDriverCaps.java	1.3 03/04/25
 *
 * Copyright (c) 1996-2003 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.vfw;

class CapDriverCaps {

    int wDeviceIndex;
    boolean fHasOverlay;
    boolean fHasDlgVideoSource;
    boolean fHasDlgVideoFormat;
    boolean fHasDlgVideoDisplay;
    boolean fCaptureInitialized;
    boolean fDriverSuppliesPalettes;
    int hVideoIn;
    int hVideoOut;
    int hVideoExtIn;
    int hVideoExtOut;

    CapDriverCaps() {
    }
}


