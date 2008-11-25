/*
 * @(#)CaptureParms.java	1.4 03/04/25
 *
 * Copyright (c) 1996-2003 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.vfw;

class CaptureParms {
    
    int dwRequestMicroSecPerFrame;
    boolean fMakeUserHitOKToCapture; 
    int wPercentDropForError;
    boolean fYield;
    int dwIndexSize; 
    int wChunkGranularity;
    boolean fUsingDOSMemory; 
    int wNumVideoRequested;
    boolean fCaptureAudio; 
    int wNumAudioRequested;
    int vKeyAbort;
    boolean fAbortLeftMouse; 
    boolean fAbortRightMouse;
    boolean fLimitEnabled;
    int wTimeLimit; 
    boolean fMCIControl;
    boolean fStepMCIDevice;
    int dwMCIStartTime; 
    int dwMCIStopTime;
    boolean fStepCaptureAt2x; 
    int wStepCaptureAverageFrames;
    int dwAudioBufferSize; 
    boolean fDisableWriteCache;
    int AVStreamMaster;

    CaptureParms() {
    }

    public String toString() {
	return
	    "\tdwRequestMicroSecPerFrame = " + dwRequestMicroSecPerFrame + "\n" +
	    "\tfMakeUserHitOKToCapture = " + fMakeUserHitOKToCapture + "\n" +
	    "\twPercentDropForError = " + wPercentDropForError + "\n" +
	    "\tfYield = " + fYield + "\n" +
	    "\tdwIndexSize = " + dwIndexSize + "\n" +
	    "\twChunkGranularity = " + wChunkGranularity + "\n" +
	    "\tfUsingDOSMemory = " + fUsingDOSMemory + "\n" +
	    "\twNumVideoRequested = " + wNumVideoRequested + "\n" +
	    "\tfCaptureAudio = " + fCaptureAudio + "\n" +
	    "\twNumAudioRequested = " + wNumAudioRequested + "\n" +
	    "\tvKeyAbort = " + vKeyAbort + "\n" +
	    "\tfAbortLeftMouse = " + fAbortLeftMouse + "\n" +
	    "\tfAbortRightMouse = " + fAbortRightMouse + "\n" +
	    "\tfLimitEnabled = " + fLimitEnabled + "\n" +
	    "\twTimeLimit = " + wTimeLimit + "\n" +
	    "\tfMCIControl = " + fMCIControl + "\n" +
	    "\tfStepMCIDevice = " + fStepMCIDevice + "\n" +
	    "\tdwMCIStartTime = " + dwMCIStartTime + "\n" +
	    "\tdwMCIStopTime = " + dwMCIStopTime + "\n" +
	    "\tfStepCaptureAt2x = " + fStepCaptureAt2x + "\n" +
	    "\twStepCaptureAverageFrames = " + wStepCaptureAverageFrames + "\n" +
	    "\tdwAudioBufferSize = " + dwAudioBufferSize + "\n" +
	    "\tfDisableWriteCache = " + fDisableWriteCache + "\n" +
	    "\tAVStreamMaster = " + AVStreamMaster + "\n";
    }

}
