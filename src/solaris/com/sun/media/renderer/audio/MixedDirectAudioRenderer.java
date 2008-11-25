/*
 * @(#)MixedDirectAudioRenderer.java	1.10 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.audio;

import java.util.*;
import java.io.*;
import javax.media.*;
import javax.media.format.*;
import javax.media.format.AudioFormat;
import com.sun.media.*;
import com.sun.media.util.*;
import com.sun.media.controls.*;

// for security checking when creating mixing thread
import java.security.*;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;
import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;


public class MixedDirectAudioRenderer implements Renderer, Runnable {
  protected static final int MIXLEN = 160;
  protected static final int MAXNUM = 16;
  protected static String NAME = "MixedDirectAudioRenderer";
  protected static  String name = "com.sun.media.renderer.audio.DirectAudioRenderer";
  protected static BufSlot[] buffers = null;
  protected static int nextBufPtr = 0;
  protected static byte[] mixpcm;
  protected static DirectAudioRenderer renderer = null;
  protected static MediaThread mixThread = null;
  protected static Object locks;
  protected static boolean opened = false;
  protected static boolean started = false;
  protected static boolean isBigEndian;
  protected static boolean is16Bit;
  protected static boolean isSigned;
  protected static AudioFormat curFormat = null;
  protected static int numstreams = 0;
  protected static int numSofar = 0;
  
  // for security checking when creating mixing thread
  private static JMFSecurity jmfSecurity = null;
  private static boolean securityPrivelege=false;
  private Method m[] = new Method[1];
  private Class cl[] = new Class[1];
  private Object args[][] = new Object[1][0];

  static {
    // for security checking when creating mixing thread
    try {
      jmfSecurity = JMFSecurityManager.getJMFSecurity();
      securityPrivelege = true;
    } catch (SecurityException e) {
    }
    
    try {
      renderer = (DirectAudioRenderer)SimpleGraphBuilder.createPlugIn(
						    name,
						    PlugInManager.RENDERER);
      
      GainControl gainControl = (GainControl)renderer.getControl("javax.media.GainControl");
      gainControl.setDB(3.72f);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    
    buffers = new BufSlot[MAXNUM];
    for ( int i = 0; i < MAXNUM; i++) {
      buffers[i] = new BufSlot();
    }
    mixpcm = new byte[MIXLEN];
    
    locks = new Object();
    nextBufPtr = 0;
    opened = false;
    started = false;
    numstreams = 0;
    numSofar = 0;
    curFormat = null;
    isBigEndian = true;
    is16Bit = true;
    isSigned = true;
  }
  
  //-----------------------------
  // This is an extra method of Renderer 
  // can only be called when the player is closed
  public static void closeRenderer() {
    synchronized (locks ) {
      for ( int i = 0; i < MAXNUM; i++ ) {
	buffers[i].allocated = false;
	buffers[i].validLen = 0;
	buffers[i].instop = true;
	buffers[i].consumed = true;
      }
      
      numstreams = 0;
      
      try {
	renderer.drain();
	renderer.stop();
	renderer.close();
      } catch (Exception ex) {
	ex.printStackTrace();
      }

      started = false;
      opened = false;
      
      mixThread.interrupt();
      // mixThread = null;

      locks.notifyAll();
    }
  }

  // -------- starting from here are all the instance var & methods
  // ***************************************************************
  protected int curBuf;
  
  // -----------------------------
  public MixedDirectAudioRenderer() {
    curBuf = 0;
  }

  
  // ---- methods from PlugIn
  public String getName() {
    return NAME;
  }

  public void open() {
    synchronized (locks ) {
      if ( !opened) {
	opened = true;
	try {
	  renderer.open();
	} catch (Exception ex) {
	  ex.printStackTrace();
	}
      }
      
      boolean found = false;
      int count = 0;
      
      while ( count < MAXNUM ) {
	if ( nextBufPtr >= MAXNUM )
	  nextBufPtr = 0;
	
	if ( !buffers[nextBufPtr].allocated ) {
	  found = true;
	  curBuf = nextBufPtr;
	  nextBufPtr ++;
	  break;
	}
	count++;
	nextBufPtr++;
      }
      
      if (!found) {
	System.out.println("not enough buffer available!");
	curBuf = -1;
      } else {
	buffers[curBuf].allocated = true;
	buffers[curBuf].validLen = 0;
	buffers[curBuf].instop = true;
	buffers[curBuf].consumed = true;
      }
      
      // todo: do we need to notify here
      locks.notifyAll();
    }
  }

  public void close() {
    synchronized (locks) {
      buffers[curBuf].allocated = false;
      buffers[curBuf].validLen = 0;
      buffers[curBuf].instop = true;
      buffers[curBuf].consumed = true;

      /*** if DAR frequently been closed & opened,might cause core dump 
	at DirectAudioRenderer.nWrite or NPE at device.bufferAvailable
	in AudioRenderer.java

	boolean needToClose = true;
	for ( int i = 0; i < MAXNUM; i++ )
	if ( buffers[i].allocated ) {
	needToClose = false;
	break;
	}
	
	if ( needToClose ) {
	started = false;
	opened = false;
	renderer.drain();
	renderer.close();
	}
      ***/
      // todo: do we need notify here?
      locks.notifyAll();

    }
    
    // renderer.close();

  }
  
  // todo:  how to mapp?
  public void reset() {
    synchronized ( locks ) {
      renderer.reset();
      renderer.start();
    }
  }
  
 
  // ---- methods from Renderer
  public Format[] getSupportedInputFormats() {
    return renderer.getSupportedInputFormats();
  }

  public void start() {
    synchronized ( locks ) {
      if ( !started ) {
	started = true;
	renderer.start();
      }
      
      if ( mixThread == null ) {
	// security checking when creating mixing thread
	if ( /*securityPrivelege && */ (jmfSecurity != null) ) {
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
	  }
	}
	
	if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
	  try {
	    Constructor cons = jdk12CreateThreadRunnableAction.cons;
	    
	    mixThread = (MediaThread) jdk12.doPrivM.invoke(
							   jdk12.ac,
							   new Object[] {
	      cons.newInstance(
			       new Object[] {
		MediaThread.class,
		  this
		  })});
	    
	    cons = jdk12PriorityAction.cons;
	    jdk12.doPrivM.invoke(
				 jdk12.ac,
				 new Object[] {
	      cons.newInstance(
			       new Object[] {
		mixThread,
		  new Integer(mixThread.getAudioPriority())
		  })});
	    
	  } catch (Exception e) {
	  }
	  
	} else {
	  mixThread = new MediaThread(this);
	  mixThread.useAudioPriority();
	}
	
	mixThread.setName("Mixing thread");
	mixThread.start();
	
	// mixThread = new MediaThread(this);
	// mixThread.useAudioPriority();
	// mixThread.start();
      }
      
      if ( buffers[curBuf].instop ) {
	numstreams ++;
	// TODO: this is for reduce mixing latency. flush() is not part
        // of renderer api. DAUD on win32 doesn't has this method
	// if ( numstreams > 1 )
	//   renderer.flush();
	
	buffers[curBuf].instop = false;
	locks.notifyAll();
      }
    }

  }
  
  public void stop() { 
    synchronized (locks ) {
      if ( !buffers[curBuf].instop ) {
	numstreams --;
	buffers[curBuf].instop = true;
	locks.notifyAll();
      }
    }
    
    // renderer.stop();
  }

  public int process(Buffer buffer) {
    synchronized( locks ) {
      while ( !buffers[curBuf].consumed ) {
	try {
	  locks.wait();
	} catch (Exception ex) {}
      }

      int len = buffer.getLength();
      // System.out.println("in MDAR process len = " + len + " curBuf = " + curBuf);
      System.arraycopy((byte[])buffer.getData(), 0, buffers[curBuf].data, 0, len);
      buffers[curBuf].validLen = len;
      
      buffers[curBuf].consumed = false;
      numSofar ++;
      locks.notifyAll();
    }

    return PlugIn.BUFFER_PROCESSED_OK;
  }
  
  public Format setInputFormat(Format format) {
    curFormat = (AudioFormat) format;
    isBigEndian = (curFormat.getEndian() == AudioFormat.BIG_ENDIAN);
    is16Bit = (curFormat.getSampleSizeInBits() == 16);
    isSigned = (curFormat.getSigned() == AudioFormat.SIGNED);
    // System.out.println("curFormat = " + curFormat);
    return renderer.setInputFormat(format);
  }
  
  // -------- methods from Controls
  public Object getControl(String controlType) {
    return renderer.getControl(controlType);
  }
  
  public Object[] getControls() {
    return renderer.getControls();
  }
  

  // ------------------------------------
  // main method to mix all the buffer together and 
  // renderer the mixed buffer
  // *NOTE*: We assume all the buffers are in the SAME format.
  // current implementation ma02y only supports 16bit/signed data
  public void run() {
    short[] sumpcm = new short[MIXLEN/2];
    int len = 0;
    int maxlen = 0;
    Buffer tbuffer = new Buffer();
    tbuffer.setFormat(curFormat);

    int index = 0;
    boolean single; 

    while ( !Thread.currentThread().isInterrupted()) {
      try {
	synchronized (locks) {
	  while ( numstreams == 0 || numSofar < numstreams ) { 
	    try {
	      locks.wait();
	    } catch (InterruptedException ex) {
	      mixThread = null;
	      return;
	    }
	  }
	  
	  index = MAXNUM+1;
	  single = true;
	  for ( int i = 0; i < MAXNUM; i++ )
	    if ( buffers[i].allocated && !buffers[i].instop) {
	      index = i;
	      break;
	    }

	  for ( int i = index+1; i < MAXNUM; i++ )
	    if ( buffers[i].allocated  && !buffers[i].instop) {
	      single = false;
	      break;
	    }

	  if ( single && index < MAXNUM ) { // single sound
	    len = buffers[index].validLen;
	    System.arraycopy(buffers[index].data, 0, mixpcm, 0, len);
	    tbuffer.setLength(len);
	    tbuffer.setData(mixpcm);
	    tbuffer.setOffset(0);
	    
	    buffers[index].consumed = true;
	    
	  } else if ( index < MAXNUM ) { // mul sounds
	    maxlen = 0;
	    if ( is16Bit) {
	      for ( int i = 0; i < sumpcm.length; i++)
		sumpcm[i] = 0;
	    } else {
	      for ( int i = 0; i < mixpcm.length; i++)
		mixpcm[i] = 0;
	    }
	    
	    for ( int i = index; i < MAXNUM; i++ ) {
	      if ( !buffers[i].allocated  || buffers[i].instop )
		continue;
	      len = buffers[i].validLen;
	      if ( is16Bit)
		add16PCMData(sumpcm, buffers[i].data, len);
	      else
		add8PCMData(mixpcm, buffers[i].data, len);
	      
	      if ( len > maxlen)
		maxlen = len;
	      
	      buffers[i].consumed = true;
	    }
	    
	    if ( is16Bit) 
	      convertShort2Byte(sumpcm, mixpcm, maxlen);
	    tbuffer.setData(mixpcm);
	    tbuffer.setLength(maxlen);
	    tbuffer.setOffset(0);
	    tbuffer.setFormat(curFormat);
	    
	  }

	  if ( index < MAXNUM )
	    renderer.process(tbuffer);
	  
	  numSofar = 0;
	  locks.notifyAll();
	} // synchronized
	
	// if ( index < MAXNUM )
	// renderer.process(tbuffer);
	
	Thread.currentThread().yield();
      } catch ( Exception ex) {
	ex.printStackTrace();
      }
    } // while

    mixThread = null;
    return;

  }

  // ---------------------------
  void add16PCMData(short[] sum, byte[] data, int len) {

    int t = 0;
    short s1, s2, s;
    if ( isBigEndian  && isSigned) { // BigEndian & Signed
      for ( int i = 0; i < len/2; i++) {
	t = i << 1;
	s1 = sum[i];
	s2 = (short)((data[t+1] & 0xFF) | ((data[t] << 8 ) & 0xFF00));
	s = (short)(s1 + s2);
	if ( s1 > 0 && s2 > 0 && s < 0 ) {
	  s = (short)(0x7fff);
	} else if ( s1 < 0 && s2 < 0 && s > 0) {
	  s = (short)(0x8000);
	} 
	  
	sum[i] = s;
      }
    } else if (!isBigEndian && isSigned) { // little Endian & Signed
      for ( int i = 0; i < len/2; i++) {
	t = i << 1 ;
	s1 = sum[i];
	s2 = (short)((data[t] & 0xFF) | ((data[t+1] << 8 ) & 0xFF00));
	s = (short)(s1 + s2);
	if ( s1 > 0 && s2 > 0 && s < 0 ) {
	  s = (short)(0x7fff);
	} else if ( s1 < 0 && s2 < 0 && s > 0) {
	  s = (short)(0x8000);
	} 
	
	sum[i] = s;
      }
    }
  }
  
  // --------------------------
  void convertShort2Byte(short[] src, byte[] dst, int maxlen) {
    int t = 0;
    // int len = dst.length/2;
    int len = maxlen/2;
    if ( isBigEndian) { // BigEndian
      for ( int i = 0; i < len; i++ ) {
	t = i << 1;
	dst[t] = (byte)((src[i] >> 8) & 0xff);
	dst[t+1] = (byte)(src[i] & 0xFF);
      }
    } else { // little Endian
      for ( int i = 0; i < len; i++ ) {
	t = i << 1;
	dst[t] = (byte)(src[i] & 0xFF);
	dst[t+1] = (byte)((src[i] >> 8) & 0xff );
      }
    }
  }

  // ----------------------------------------------
  void add8PCMData(byte[] sum, byte[] data, int len) {
    if ( isSigned) { // Signed
      byte s1, s2, s;
      for ( int i = 0; i < len; i++) {
	s1 = sum[i];
	s2 = data[i];
	s = (byte)(s1 + s2);
	if ( s1 > 0 && s2 > 0 && s < 0 ) {
	  s = (byte)(0x7f);
	} else if ( s1 < 0 && s2 < 0 && s > 0) {
	  s = (byte)(0x80);
	} 
	
	sum[i] = s;
      }
    } 
  }
  
}
    
