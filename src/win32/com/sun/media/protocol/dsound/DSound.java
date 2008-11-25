
package com.sun.media.protocol.dsound;

import javax.media.format.AudioFormat;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;
import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;

public class DSound {

    private static boolean opened = false;
    private static AudioFormat format = null;
    private static int bufSize = 2048;
    private static long directSound = 0;
    private long dsBuffer = 0;

    static boolean loaded = false;

    static {
	if (!loaded) {
	    try {
		JMFSecurityManager.checkCapture();
		JMFSecurityManager.loadLibrary("jmdaudc");
		loaded = true;
	    } catch (UnsatisfiedLinkError e) {
		loaded = false;
		throw e;
	    }
	}
    }
	
    public DSound(AudioFormat format, int bufferSize) {
	this.format = format;
	this.bufSize = bufferSize;
    }
    
    public static boolean isOpen() {
	return opened;
    }

    public static boolean isFormatSupported(AudioFormat format, int bufSize) {
	return true;
    }

    public void open() {
	
	dsBuffer = nOpen((int) format.getSampleRate(),
			 format.getSampleSizeInBits(),
			 format.getChannels(),
			 bufSize);
	if (dsBuffer == 0)
	    throw new Error("Couldn't create capture buffer");
    }

    public AudioFormat getFormat() {
	return format;
    }

    public int getBufferSize() {
	return bufSize;
    }

    public void start() {
	nStart(dsBuffer);
    }

    public void stop() {
	nStop(dsBuffer);
    }

    public void flush() {
	nFlush(dsBuffer);
    }

    public synchronized void close() {
	if (dsBuffer != 0)
	    nClose(dsBuffer);
	dsBuffer = 0;
    }

    public int read(byte [] data, int offset, int len) {
	return nRead(dsBuffer, data, offset, len);
    }

    private native long nOpen(int sampleRate, int sampleSize,
			     int channels, int bufferSize);

    private synchronized native void nStart(long dsBuffer);

    private synchronized native void nStop(long dsBuffer);

    private synchronized native void nFlush(long dsBuffer);

    private synchronized native int nRead(long dsBuffer,
					  byte [] data, int offset, int len);

    private synchronized native void nClose(long dsBuffer);
}
