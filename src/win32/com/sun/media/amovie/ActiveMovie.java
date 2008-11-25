/*
 * @(#)ActiveMovie.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.amovie;

import javax.media.*;
import javax.media.protocol.*;

public class ActiveMovie implements Runnable {
    
    private int pGraph = 0;
    private int aStream = 0;
    private int filterPin = 0;
    private byte [] jbuffer;

    private boolean paused = false;
    private boolean donePaused = true;

    private Thread spinner = null;
    private Integer semaphore = new Integer(0);
    
    private boolean realized = false;
    private int     streamType = 3;

    private AMController controller;
    private PullSourceStream stream = null;
    private boolean seekable;
    private boolean randomAccess;
    private long readLocation = 0;
    private long streamLocation = 0;
    private boolean controllerRealized = false;
    private int cacheBuffer = 0;
    private int cacheTotalSize = 0;
    private int cacheAllocated = 0;

    private boolean deallocated = false;
    
    // volume constants
    public static final int MIN_VOLUME = -10000;
    public static final int MAX_VOLUME = 0;
    
    ActiveMovie(AMController controller, String file) {
	this.controller = controller;
        realized = openFile(file);
    }
    
    ActiveMovie(AMController controller, PullSourceStream source,
		boolean randomAccess, long contentLength) {

	this.controller = controller;
	this.stream  = source;
	this.seekable = source instanceof Seekable;
	this.randomAccess = randomAccess;

	// boolean seekable;
	this.jbuffer = new byte[65536];
	
	//seekable = (source instanceof Seekable) &&
	//         ((Seekable)source).isRandomAccess();

	initiateSpin();
	if (seekable)
	    seek(0);
	controller.canRead(64 * 1024);
	int size = controller.read(jbuffer, 0, 64 * 1024);

	if (!randomAccess && size > 0)
	    addToCache(jbuffer, 0, size);
	if (size > 0) {
	    //System.err.println("Content Length = " + contentLength);
	    if (!randomAccess && contentLength > 0)
		contentLength += 600 * 1024;
	    streamType = getStreamType(jbuffer, size);
	    streamLocation += size;
	    seek(0);

	    realized = openStream(seekable, randomAccess, streamType,
				  contentLength);

	} else
	    realized = false;
    }

    public void setSeekable(boolean seekable) {
	if (seekable) {
	    this.randomAccess = seekable;
	    this.seekable = seekable;
	}
	setNSeekable(seekable);
    }
    
    native void setNSeekable(boolean seekable);

    private void initiateSpin() {
	spinner = new Thread( this );
	spinner.start();
    }

    public void run() {
	// We need to block the thread if ActiveMovie is paused.
	while (true) {
	    synchronized (this) {
		while (paused) {
		    if (!donePaused) {
			donePaused = true;
			notifyAll();
		    }
		    try {
			wait();
		    } catch (InterruptedException e) {}
		}
	    }
	    
	    doNRequest(jbuffer);
	    
	    try {
		spinner.sleep(50);
	    } catch (Exception e) {
		System.err.println("Exception in run()" + e);
	    }
	}

    }

    void doneRealize() {
	controllerRealized = true;
    }
    
    boolean isRealized() {
	return realized;
    }

    boolean hasVideo() {
	return (streamType & 2) == 2;
    }

    boolean hasAudio() {
	return (streamType & 1) == 1;
    }

    native void amRun();

    native void amPause();

    native void amStop();

    native void amStopWhenReady();

    void stopDataFlow(boolean stop) {
	if (filterPin != 0)
	    stopDataFlow(filterPin, stop);
    }

    native void stopDataFlow(int filterPin, boolean stop);

    native double getDuration();

    native double getCurrentPosition();

    native void setCurrentPosition(double pos);

    native void setStopTime(double time);

    native int getBitRate();

    native double getFrameRate();

    native int getVideoWidth();

    native int getVideoHeight();

    native void setOwner(int owner);

    native void setVisible(int visible);

    native void setWindowPosition(int left, int top, int right, int bottom);

    native int getVolume();

    native void setVolume(int volume);

    native void setRate(double rate);

    native double getRate();

    native long getTime(); 

    native boolean waitForCompletion();

    native int  getStreamType(byte [] array, int size);
    
    native void doNRequest(byte [] array);

    // Called from native code
    public int canRead(int nBytes) {
	return controller.canRead(nBytes);
    }

    // Called from native code
    public long canSeek(long seekTo) {
	return controller.canSeek(seekTo);
    }

    public int read(byte [] array, int offset, int length) {
	int totalRead = 0;
	if (deallocated)
	    return -1;
	if (cacheTotalSize > 0 && !randomAccess) {
	    if (readLocation < cacheTotalSize && streamLocation == cacheTotalSize) {
		totalRead = (int) (cacheTotalSize - readLocation);
		if (totalRead > length)
		    totalRead = length;
		getFromCache((int) readLocation,
			     array, offset, totalRead);
		readLocation += totalRead;
		if (totalRead == length) {
		    return totalRead;
		} else {
		    length -= totalRead;
		    offset += totalRead;
		}
	    }
	}
	
	int actualRead = 0;
	int remaining = length;
	
	while (totalRead < length) {
	    if (canRead(remaining) > 0)
		actualRead = controller.read(array, offset, remaining);
	    else
		actualRead = -1;
	    if (actualRead == -1) {
		// EOS
		// cacheTotalSize = 0;
		if (totalRead > 0)
		    return totalRead;
		else
		    return -1;
	    } else if (actualRead == -2) {
		return -2;
	    } else if (actualRead > 0) {
		remaining -= actualRead;
		totalRead += actualRead;

		// Cache the data if the controller is not realized yet.
		if (!controllerRealized && !randomAccess) {
		    if (streamLocation == cacheTotalSize) {
			addToCache(array, offset, actualRead);
		    }
		}

		offset += actualRead;
		streamLocation += actualRead;
		readLocation = streamLocation;
		if (streamLocation > cacheTotalSize &&
		    controllerRealized)

		    cacheTotalSize = 0;
	    }
	}
	
	if (actualRead > 0)
	    return totalRead;
	else
	    return actualRead;
    }

    public long seek(long seekTo) {
	if (deallocated)
	    return 0;
	if (seekTo < cacheTotalSize && !randomAccess) {
	    readLocation = seekTo;
	    return seekTo;
	} else if (seekable && (randomAccess || (seekTo == 0))) {
	    long seeked = controller.seek(seekTo);
	    streamLocation = seekTo;
	    return seeked;
	} else {
	    // Couldn't seek
	    return -1;
	}
    }

    /****************************************************************
     * Cache Stuff
     ****************************************************************/
    
    private void addToCache(byte [] buffer, int offset, int size) {
	if (cacheBuffer == 0) {
	    cacheBuffer = nCreateCache(384 * 1024);
	    cacheAllocated = 384 * 1024;
	}
	
	if ((cacheTotalSize + size) > cacheAllocated)
	    return;

	nAddToCache(cacheBuffer, cacheTotalSize, buffer, offset, size);
	cacheTotalSize += size;
    }

    private void getFromCache(int location, byte [] buffer, int offset, int size) {
	nGetFromCache(cacheBuffer, location, buffer, offset, size);
    }

    private native int nCreateCache(int cacheSize);

    private native void nAddToCache(int cacheBuffer, int cacheOffset,
				   byte [] buffer, int bufOffset, int size);

    private native void nGetFromCache(int cacheBuffer, int cacheOffset,
				     byte [] buffer, int bufOffset, int size);

    private native void nFreeCache(int cacheBuffer);

    /****************************************************************
     * End Cache Stuff
     ****************************************************************/
    
    void dispose() {
	if (spinner != null) {
	    spinner.stop();
	    spinner = null;
	}

	dispose0();				      // native call
	
	if (cacheBuffer != 0) {
	    nFreeCache(cacheBuffer);
	    cacheBuffer = 0;
	}
    }

    protected void finalize() {
        dispose();
    }

    public synchronized void pause() {
	// System.err.println("In ActiveMovie.pause()");
	if (paused) return;
	donePaused = false;
	paused = true;
	// Block for the donePaused to clear.  This is
	// done so that the last read could be completed before
	// it returns.
	if (!donePaused) {
	    try {
		wait(250);
		donePaused = true;
	    } catch (InterruptedException e) {}
	}
    }

    public void restart() {
	// Restart the paused thread.
	// System.err.println("In ActiveMovie.restart()");
	deallocated = false;
	stopDataFlow(false);
	unPause();
    }

    private void unPause() {
	if (!paused) return;
	synchronized (this) {
	    donePaused = true;
	    paused = false;
	    notifyAll();
	}
    }

    public void kill() {
	deallocated = true;
	unPause();
	stopDataFlow(true);
	amStop();
    }

    native void dispose0();

    native boolean openFile(String file);
    native boolean openStream(boolean seekable,
			      boolean randomAccess,
			      int streamType,
			      long contentLength);
    static native int findWindow(String name);
}
