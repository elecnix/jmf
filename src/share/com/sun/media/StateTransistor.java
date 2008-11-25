/*
 * @(#)StateTransistor.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media;

import javax.media.Time;

/**
 * StateTransistor is an interface with the functionality of performing
 * the actual state transitions: DoPrefetch, DoRealize, etc.
 */

public interface StateTransistor {

    /**
     * This function performs the steps of realizing a module or a Player.
     * @return true if successful.
     */
    public boolean doRealize();

    /**
     * Called when realize fails.
     */
    public void doFailedRealize();

    /**
     * Called when the realize() is aborted, i.e. deallocate() was called
     * while realizing.  Release all resources claimed previously by the
     * realize() call.
     */
    public void abortRealize();

    /**
     * This function performs the steps to prefetch a module or Player.
     * @return true if successful.
     */
    public boolean doPrefetch();

    /**
     * Called when prefetch fails.
     */
    public void doFailedPrefetch();

    /**
     * Called when the prefetch() is aborted, i.e. deallocate() was called
     * while prefetching.  Release all resources claimed previously by the
     * prefetch call.
     */
    public void abortPrefetch();

    /**
     * This function performs the steps to start a module or Player.
     */
    public void doStart();

    /**
     * This function performs the steps to stop a module or Player,
     * and return to the prefetched state.
     */
    public void doStop();

    /**
     * This function performs the steps to deallocate a module or Player,
     * and return to the realized state.
     */
    public void doDealloc();

    /**
     * This function performs the steps to close a module or Player.
     */
    public void doClose();

    /**
     * This function notifies the module that the media time has changed.
     */
    public void doSetMediaTime(Time t);

    /**
     * This function notifies the module that the playback rate has changed.
     */
    public float doSetRate(float r);

}

