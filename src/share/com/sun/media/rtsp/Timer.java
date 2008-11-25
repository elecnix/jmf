/*
 * @(#)Timer.java	1.2 00/04/18
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.media.rtsp;

import java.util.*;

public class Timer extends Thread implements Runnable {
    private Vector listeners;
    private long duration;
    private boolean stopped;

    public Timer(TimerListener listener, long duration) {
        listeners = new Vector();

        this.duration = duration / 1000000;

        addListener(listener);

        stopped = false;
    }

    public void reset() {
    }

    public void stopTimer() {
        // System.out.println( "stop timer");

        stopped = true;

        synchronized (this) {
            notify();
        }
    }

    public void run() {
        // System.out.println( "start timer for " + duration + "ms");

        synchronized (this) {
            try {
                wait(duration);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!stopped) {
            for (int i = 0; i < listeners.size(); i++) {
                TimerListener listener = (TimerListener) listeners.elementAt(i);

                listener.timerExpired();
            }
        }
    }

    public void addListener(TimerListener listener) {
        listeners.addElement(listener);
    }

    public void removeListener(TimerListener listener) {
        listeners.removeElement(listener);
    }
}
