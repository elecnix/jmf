/*
 * @(#)SunAudioOutput.java	1.15 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.audio.device;

import java.io.*;
import javax.media.format.AudioFormat;
import com.sun.media.*;
import com.sun.media.util.LoopThread;
import com.sun.media.util.jdk12;
import com.sun.media.renderer.audio.SunAudioRenderer;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;

public class SunAudioOutput extends InputStream implements AudioOutput {
    protected sun.audio.AudioStream audioStream;

    protected int bufLength;
    protected byte buffer[];
    protected static int EOM = -1;
    protected boolean paused = false;
    protected boolean started = false;
    protected boolean flushing = false;
    private boolean startAfterWrite = false;
    protected AudioFormat format;
    private int SUN_MAGIC = 0x2e736e64;     // au file magic number
    private int HDR_SIZE = 24;              // minimum au header file size
    private int FILE_LENGTH = 0;            // file length (optional)
    private int SAMPLE_RATE = 8000;
    private int ENCODING = 1;               // ULAW
    private int CHANNELS = 1;

    // in == out implies buffer is empty.
    // (in + 1) % buffer.length == out implies buffer is full.
    int in = 0;
    int out = 0;
    boolean eom = false;

    /*private*/ int samplesPlayed = 0;
    private boolean isMuted = false;
    private double gain = 0;
    private byte silence[];
    //private boolean fConvertToULAW = false;
    /** padding length of silence at the end of the media (default constant) **/
    private static final int END_OF_MEDIA_PADDING_LENGTH=800;
    /** padding length of silence at the end of the media **/
    private int endOfMediaPaddingLength;
    private byte[] conversionBuffer ;

    //private boolean AudioPlayerStoppingPhase=false;
    //private FileOutputStream IN;

    /*private*/ static final int SLEEP_TIME=50;
    //protected static final int SUN_AUDIO_INTERNAL_DELAY=400; // for debuging purposes
    //protected static final int SUN_AUDIO_INTERNAL_DELAY=0; // for debuging purposes
    protected boolean internalDelayUpdate=false;
    private SunAudioPlayThread timeUpdatingThread=null;
    protected int sunAudioInitialCount=0;
    protected int sunAudioFinalCount=0;
    protected int silenceCount = 0;

    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege=false;
    private Method m[] = new Method[1];
    private Class cl[] = new Class[1];
    private Object args[][] = new Object[1][0];

    static {
	try {
	    jmfSecurity = JMFSecurityManager.getJMFSecurity();
	    securityPrivelege = true;
	} catch (SecurityException e) {
	}
    }

    public SunAudioOutput() {
    }

    public boolean initialize(AudioFormat format, int length) {

	this.format = format;
	//bufLength = length;
	//bufLength = 8000;	// hardcode the number to 8000.
        bufLength = 12000;	// hardcode the number to 12000.
	buffer = new byte[bufLength];

    	silence = new byte[bufLength];
	for (int i = 0; i < bufLength; i++)
		silence[i] = 127;

	if ( /*securityPrivelege &&*/ (jmfSecurity != null) ) {
	    String permission = null;

	    try {
		if (jmfSecurity.getName().startsWith("jmf-security")) {
		    permission = "thread";
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.THREAD);
		    m[0].invoke(cl[0], args[0]);
		    
		    permission = "thread group";
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.THREAD_GROUP);
		    m[0].invoke(cl[0], args[0]);
		} else if (jmfSecurity.getName().startsWith("internet")) {
		    PolicyEngine.checkPermission(PermissionID.THREAD);
		    PolicyEngine.assertPermission(PermissionID.THREAD);
		}
	    } catch (Throwable e) {
		if (JMFSecurityManager.DEBUG) {
		    System.err.println("Unable to get " + permission +
				       " privilege  " + e);
		}
		securityPrivelege = false;
		// TODO: Do the right thing if permissions cannot be obtained.
		// User should be notified via an event
	    }
	}
	if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
	    try {
		Constructor cons = jdk12CreateThreadAction.cons;
		
		timeUpdatingThread = (SunAudioPlayThread) jdk12.doPrivM.invoke(
								   jdk12.ac,
								   new Object[] {
		    cons.newInstance(
				     new Object[] {
			SunAudioPlayThread.class,
			    })});
	    } catch (Exception e) {
	    }
	} else {
	    timeUpdatingThread = new SunAudioPlayThread();
	}

	timeUpdatingThread.setStream(this);
        setPaddingLength(END_OF_MEDIA_PADDING_LENGTH); // defualt size

	// BB
	ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
	DataOutputStream tempData = new DataOutputStream(tempOut);
	try {
		tempData.writeInt(SUN_MAGIC);
		tempData.writeInt(HDR_SIZE);
		tempData.writeInt(FILE_LENGTH);
		tempData.writeInt(ENCODING);
		tempData.writeInt(SAMPLE_RATE);
		tempData.writeInt(CHANNELS);
	} catch (Exception e) {}

	byte[] buf = tempOut.toByteArray();

	write(buf, 0, buf.length);

     String encoding = format.getEncoding();
     int sampleRate = (int)format.getSampleRate();

     if (!( (format.getChannels() == 1) &&
         (sampleRate == 8000) &&
         (encoding.equals(AudioFormat.ULAW))  ) ) {

          System.out.println("AudioPlay:Unsupported Audio Format");
	  return false;

    }


	try {
		audioStream = new sun.audio.AudioStream(this);
	} catch (Exception e) {
		System.err.println("Exception: " + e);
		audioStream = null;
		return false;
	}
        return true;
    }


    public void finalize() throws Throwable {
	super.finalize();
	dispose();
    }

    public void pause() {

        //System.out.println("SunAudioOutput pause ");
         if (audioStream != null) {
            timeUpdatingThread.pause();
	    sun.audio.AudioPlayer.player.stop(audioStream);
	 }
	 paused = true;
    }

    public synchronized void resume() {

        //System.out.println("SunAudioOutput resume ");
        if ( (audioStream != null) && (!started || paused) )  {
                started=true;
                //System.out.println("start the player "+dataAvailable());
		sun.audio.AudioPlayer.player.start(audioStream);
                timeUpdatingThread.start();
	}

	paused = false;

    }



    // $$ AudioRenderer's abortPrefetch() calls this method
    public synchronized void dispose() {

         if (audioStream != null) {
              timeUpdatingThread.kill();
	      sun.audio.AudioPlayer.player.stop(audioStream);
	}
	 buffer = null;
    }


    public void drain() {

      int remain;
      int len;

      synchronized (this) {

	remain = endOfMediaPaddingLength;
        // pad the end of the media with silence
        // (used to drain sun.audio.AudioPlayer.player)

	while (remain > 0) {
	    len = write(silence,0,remain);
	    remain -= len;
	}

	// drain the JMF buffer.
	while (in != out && !paused) {
	    try {
		wait();
	    } catch (InterruptedException e) { }
	}


	// We'll need to drain longer on the Mac.
	if (SunAudioRenderer.runningOnMac) {
	    try {
		Thread.sleep(SunAudioRenderer.DEVICE_LATENCY/1000000L);
	    } catch (InterruptedException e) {}
	}
      }
    }


    // Clean the buffer.
    public synchronized void flush() {
	//System.out.println("SunAudioOutput flush ");
	in = 0;
	out = 0;
        sunAudioInitialCount = sunAudioFinalCount = samplesPlayed;
        flushing = true;
	notifyAll();
    }

    public long getMediaNanoseconds() {
	/*
	double samples = (double)samplesPlayed;
	//samples = samples/(double)format.getSampleRate();
	// SunAudioOutput plays at 8 Khz
	samples = samples/(double)8000;
	// System.out.println("AudioPlay.getTick() " + samples);
	return (audioStream == null ? 0 : (long)(samples * 1000000000L) );
	*/

	return (audioStream == null ? 0 : samplesPlayed * 125000L);
    }

    public void setMute(boolean m) {
	// System.out.println("AudioPlay.setMute()");
	isMuted = m;
    }


    public boolean getMute() {
	    return isMuted;
    }

    public void setGain(double g) {
    }

    public double getGain() {
	return 0f;
    }

    public float setRate(float r) {
	return 1.0f;
    }

    public float getRate() {
	return 1.0f;
    }

    public int dataAvailable() {
	if (in == out)
	    return 0;
	else {
	    if (in > out)
		return in - out;
	    else
		return bufLength - (out - in);
	}
    }


    public int bufferAvailable() {
	if (SunAudioRenderer.runningOnMac)
	    return 0;
	return bufLength - dataAvailable() - 1;
    }


    // Read a byte of data.  Block if there is no data to read.
    public synchronized int read() {
	// Block if the buffer is empty.
	while (in == out) {
	    if (eom) {
		eom = false;
		return EOM;
	    }
	    try {
		wait();
	    } catch (InterruptedException e) {
	    }
	}
	int ret = buffer[out++] & 0xFF;
	if (out >= buffer.length) {
	    out = 0;
	}
	return ret;
    }



     // $$ Non blocking read
    public synchronized int read(byte b[], int off, int len) {
	//System.out.println("AP:needs: " + len + " available: " + dataAvailable());
 	//System.out.println("AP: read3: " + Thread.currentThread() + ": " +
 	//		   Thread.currentThread().getPriority() +
 	//		   ": paused, avail: " + paused + ": " + available());

        int inputLength=len;

	if (len <= 0) {
	    return -1;
	}


        if ( (len>4) && (!internalDelayUpdate) ) {
          //System.out.println("sunAudioInternalDelay "+len);
          internalDelayUpdate=true;
          timeUpdatingThread.setInternalDelay(len);
        }

	if (dataAvailable() == 0) {
            //System.out.println("underflow - no data: " + inputLength);
            System.arraycopy(silence, 0, b, off, inputLength);
            //timeUpdatingThread.resetSampleCountTime();
            //sunAudioInitialCount=sunAudioFinalCount;
	    silenceCount += inputLength;
            return inputLength;
	}


	// This read will not block
	int c = read();
	if (c < 0) {
	    return -1;
	}
	b[off] = (byte) c;

  	int rlen = 1;

	if ( in != out ) {
	  int avail, need, size;

	  len--; // 1 byte read and copied.
	  if (out < in) {
	    avail = (in - out);
	    if (avail > len)
	      avail = len;
 	    System.arraycopy(buffer, out, b, off+1, avail);
	    out += avail;
	    rlen += avail;
	  } else if ( out > in ) {
	    avail = bufLength - out;
	    if (avail >= len) {
	      avail = len;
	      System.arraycopy(buffer, out, b, off+1, avail);

	      out += avail;
	      if (out >= bufLength)
		out = 0;
	      rlen += avail;
	    } else {
	      System.arraycopy(buffer, out, b, off+1, avail);
	      out += avail;
	      if (out >= bufLength)
		out = 0;
	      int copied = avail;
	      rlen += avail;
	      need = (len - avail);
	      avail = (in - out);
	      if (need <= avail)
		size = need;
	      else
		size = avail;
	      System.arraycopy(buffer, 0, b, off+1+copied, size);
	      out += size;
	      rlen += size;
	    }
	  }
	}

	// Notify if there's any waiting writer.
	if (isMuted) {

	    //System.err.println("muted -- fill with silence");
            System.arraycopy(silence, 0, b, off, inputLength);

	} else {

	    if (rlen<inputLength) { 

		// pad the rest of the buffer with silence 
 		// but don't update the sample count

		//System.out.println("underflow - pad with silence: " + (inputLength-rlen));
		System.arraycopy(silence, 0, b, off+rlen, inputLength-rlen);
		silenceCount += (inputLength-rlen);

	    } else if (silenceCount > 0) {

		// There were some silence filled in before.  We'll
		// need to compensate for that.
		if (silenceCount > rlen) {
		    silenceCount -= rlen;
		    rlen = 0;
		} else {
		    rlen -= silenceCount;
		    silenceCount = 0;
		}
	    }
        }

        timeUpdatingThread.resetSampleCountTime();
        sunAudioInitialCount=sunAudioFinalCount;
        sunAudioFinalCount+=rlen;
        notifyAll();

        return inputLength;

    }



    // Write an array of bytes to the buffer.  Block until there's
    // enough space in the buffer.
    public synchronized int write(byte data[], int off, int len) {

     //System.out.println("SunAudioOutput.write , try len"+len+" in "+in+" out "+out);
     //System.out.println("abc: SunAudioOutput.write "+len);

        flushing = false;
	if (len <= 0)
	    return 0;


	// Block if the buffer is full.
	while ((in + 1) % buffer.length == out) {
	    try {
		wait();
	    } catch (InterruptedException e) {
	    }
	}

       if (flushing) {
	    return 0;
	}

 	int wlen = 0;

	if (true) {
	  int canWrite, actualWrite, actualWrite1, length1;
	  if (in < out) {
	    canWrite = out - in -1;
	    actualWrite = (canWrite < len) ? canWrite : len;
 	    System.arraycopy(data, off, buffer, in, actualWrite);
	    in += actualWrite;
	    wlen += actualWrite;
	  } else {
	    if (out == 0)
	      length1 = bufLength - in - 1;
	    else
	      length1 = bufLength - in;

	    if (length1 >= len) {
	      actualWrite = len;
	      System.arraycopy(data, off, buffer, in, actualWrite);
	      in += actualWrite;
	      if (in >= bufLength)
		in = 0;
	      wlen += actualWrite;
	    } else {
	      actualWrite = length1;
              System.arraycopy(data, off, buffer, in, actualWrite);
	      in += actualWrite;
	      if (in >= bufLength)
		in = 0;
	      wlen += actualWrite;
	      len -= actualWrite;
	      actualWrite1 = actualWrite;

	      if (out > 0) {
		canWrite = out -in -1;
		actualWrite = (canWrite < len) ? canWrite : len;
		System.arraycopy(data, off+actualWrite1,
				 buffer, 0, actualWrite);
		wlen += actualWrite;
		in = actualWrite;
	      }
	    }
	  }
	}

	// Notify the waiting reader.
	notifyAll();

        //System.out.println("before wlen "+wlen+" in "+in+" out "+out);

        //saveInput(data,off,wlen);

 	return wlen;
    }





  /*
  private void saveInput(byte [] indata,int offset, int length) {
	try {
	    if (IN == null)
		IN = new FileOutputStream("audio.pcm");
	    IN.write(indata, offset, length);
	    IN.flush();
	 } catch (Exception e) {
	    System.out.println("Frame not saved: "+e);
	 }
    }

   */

  protected void setPaddingLength(int paddingLength) {
      //System.out.println("SunAudioOutput setPaddingLength "+ paddingLength);
      endOfMediaPaddingLength = paddingLength;
      if (endOfMediaPaddingLength > silence.length)
          endOfMediaPaddingLength = silence.length;
   }


}


/**
 * This class used to be an inner class, which is the correct thing to do.
 * Changed it to a package private class because of jdk1.2 security.
 * For jdk1.2 and above applets, SunAudioPlayThread is created in a
 * privileged block using jdk12CreateThreadAction. jdk12CreateThreadAction
 * class is unable to create and instantiate an inner class 
 * in SunAudioOutput class
 */

class SunAudioPlayThread extends LoopThread {
    
    long initialTime;
    long currentTime;
    int samplesUpdated;
    int sunAudioInternalDelay=-1;
    SunAudioOutput sunAudioOutput;
    
    SunAudioPlayThread() {
	setName(getName() + ": " + this);
    }
    
    
    void setStream(SunAudioOutput s) {
	sunAudioOutput = s;
    }
    
    public void resetSampleCountTime() {
	initialTime=System.currentTimeMillis();
    }
    
    public synchronized void start() {
	currentTime=System.currentTimeMillis();
	super.start();
    }
    
    public void setInternalDelay(int delay) {
	if (delay>=0)
	    sunAudioInternalDelay=delay;
	
	// SunAudioOutput.this.setPaddingLength(sunAudioInternalDelay*2);
	sunAudioOutput.setPaddingLength(sunAudioInternalDelay*2);
    }
    
    protected boolean process() {
	
	try {
	    Thread.sleep(sunAudioOutput. SLEEP_TIME);  // constant updating delay
	}
	catch (InterruptedException e) {
	    //System.out.println("SunAudioPlayThread interrupted");
	}
	
	if (sunAudioInternalDelay>=0) {
	    currentTime = System.currentTimeMillis();
	    samplesUpdated = (int) ((currentTime-initialTime)*8L);
	    if ( (samplesUpdated>=0) && (!sunAudioOutput.paused) ){
		int tmpSamplesPlayed= sunAudioOutput.sunAudioInitialCount+samplesUpdated;
		if ( (tmpSamplesPlayed > sunAudioOutput.samplesPlayed) &&
		     ( tmpSamplesPlayed<=sunAudioOutput.sunAudioFinalCount) ) {
		    if ( (tmpSamplesPlayed-sunAudioInternalDelay) > sunAudioOutput.samplesPlayed) {
			sunAudioOutput.samplesPlayed=tmpSamplesPlayed-sunAudioInternalDelay;
		    }
		}
	    }
	}
	return true;
    }
}
