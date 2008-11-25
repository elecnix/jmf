/*
 * @(#)LoopThread.java	1.12 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

/**
 * LoopThread
 * A looping thread that implements a safe way of pausing and restarting.
 * Instead of using suspend() and resume() from the thread class, a pause() 
 * and start() method is provided.
 * To use it, you will:
 *   - subclass from it;
 *   - overwrite the process() callback;
 *   - call start() to initiate the thread.
 * @version 1.9, 98/09/28
 */
public abstract class LoopThread extends MediaThread {

    protected boolean paused = false, started = false;
    protected boolean killed = false;
    private boolean waitingAtPaused = false;

    /**
     * The pause flag determines the initial state.
     */
    public LoopThread() {
	setName("Loop thread");
    }

    /**
     * Set the paused state to true.  The thread will halt at the
     * beginning of the loop at the next iteration.  i.e., unlike
     * suspend(), the thread will NOT pause immediately.  It will execute
     * until the next waitHereIfPaused() is encountered.
     */
    public synchronized void pause() {
	paused = true;
    }

    /**
     * Same as pause except that it blocks until the loop has actually 
     * paused (at waitHereIfPaused).
     */
    public synchronized void blockingPause() {
	if (waitingAtPaused || killed) return;
	paused = true;
	waitForCompleteStop();
    }

    public boolean isPaused() {
	return paused;
    }

    /**
     * Wait until the loop has come to a complete stop.
     */
    public synchronized void waitForCompleteStop() {
	try {
	    while (!killed && !waitingAtPaused && paused)
		wait();
	} catch (InterruptedException e) { }
    }

    /**
     * Wait until the loop has come to a complete stop.
     * Time out after the given milli seconds.
     */
    public synchronized void waitForCompleteStop(int millis) {
	try {
	    if (!killed && !waitingAtPaused && paused)
		wait(millis);
	} catch (InterruptedException e) { }
    }

    /**
     * Resume the loop at the beginning of the loop where
     * waitHereIfPaused() is called.
     */
    public synchronized void start() {
	if (!started) {
	    super.start();
	    started = true;
	}
	paused = false;
	notifyAll();
    }

    public synchronized void kill() {
	killed = true;
	notifyAll();
    }

    /**
     * Put the thread in a wait() if the paused state is on.  Otherwise, it
     * will just proceed.
     */
    public synchronized boolean waitHereIfPaused() {
	if (killed)
	    return false;
	waitingAtPaused = true;
	if (paused)
	    notifyAll();	// notify the blocking pause.
	try {
	    while (!killed && paused)
		wait();
	} catch (InterruptedException e) {
	    System.err.println("Timer: timeLoop() wait interrupted " + e);
	}
	waitingAtPaused = false;
	if (killed)
	    return false;
	return true;
    }

    /**
     * process callback function.
     */
    protected abstract boolean process();

    /**
     * The run method for the Thread class.
     */
    public void run() {
	for (;;) {

	    // Wait here if pause() was invoked.  Until start() is called,
	    // the thread will wait here indefinitely.
	    if (!waitHereIfPaused())
		break;

	    // Invoke the callback function.
	    if (!process())
		break;
	}
    }

}
