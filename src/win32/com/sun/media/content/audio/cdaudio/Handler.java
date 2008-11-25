/*
 * @(#)Handler.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.content.audio.cdaudio;


import java.io.*;
import java.awt.*;
import java.util.*;
import java.net.*;
import javax.media.*;
import javax.media.protocol.*;
import com.sun.media.*;
import com.sun.media.ui.*;
import com.sun.media.util.*;
import com.sun.media.protocol.*;
import com.sun.media.amovie.MCI;

/**
 * A CD player implementation using MCI.
 */
public class Handler extends com.sun.media.BasicPlayer {
    
    private int frames;
    private int minutes;
    private int seconds;
    private int hours;
    private int track;
    private int mins;
    private int secs, frms;
    private int hrs;
    private MCI mci;
    private boolean isPlaying = false;
    private byte [] buffer;
    private long nanos;
    private boolean muted;
    private EomThread eomThread;

    // $$$$$TODO - robsz$$$
    // Implement a nice resource management scheme. Something that will check
    // if anybody else is playing a cd, and if so, stop them. It occurs to me
    // that some scheme like that might be also used in managing resources of
    // other players, at least those subclassing from JavaMedia. Some kind of
    // static variable, which holds all the players, a function which can
    // lower or raise thread priority of a player, maybe some ability to set
    // the framerate?

    public Handler() {
	mci = new MCI();
    }
    
    public void doClose() {
	super.doClose();
	if (eomThread != null)
	    eomThread.kill();
    }

    public void setSource(javax.media.protocol.DataSource source) throws IncompatibleSourceException {
	if (source instanceof com.sun.media.protocol.file.DataSource) {
	    this.source = source;
	} else {
	    System.out.println("Unsupported data source: "+ source);
	    throw new IncompatibleSourceException(this+": doesn't support "+source);
	}
    }
    
    public String convertToMsf(int f, int s, int m) {
	if (f > 74) {
	    f %= 75;
	    s++;
	}
	if (s > 59) {
	    s %= 60;
	    m++;
	}
	String st = ""+m+":"+ s+":"+f;
	return st;
    }

    public void doStart() {
	if (eomThread != null) 
	    eomThread.start();
	mci.sendString("set cdaudio time format msf wait");

	String st = convertToMsf(buffer[36]+frms, secs+buffer[37], buffer[38]+mins);

	String et = convertToMsf(buffer[36]+buffer[40], buffer[37]+buffer[41]-1,
				 buffer[38]+buffer[42]);

	mci.ssendString("open cdaudio shareable wait");
	String comm = "play cdaudio from "+st+" to "+et;
	mci.ssendString(comm);
	mci.ssendString("close cdaudio wait");

	isPlaying = true;
    }

    public void doStop() {
	isPlaying = false;
	doSetMediaTime(getMediaTime());
	mci.ssendString("open cdaudio shareable wait");
	mci.ssendString("pause cdaudio wait");
	mci.ssendString("close cdaudio wait");
	if (eomThread != null)
	    eomThread.pause();
    }

    protected boolean doRealize() {
	PullSourceStream pss = (PullSourceStream)
	    (((PullDataSource)source).getStreams())[0];
        buffer = new byte[44];
	int count = 0;
	int tst;
	try {
	    count = pss.read(buffer, 0, 44);
	} catch(IOException e) {}
	if (count < 0) {
	    abortRealize();
	    return false;
	}
	frames = buffer[40];
	seconds = buffer[41];
	minutes = buffer[42];
	hours = buffer[43];
	track = (((int)buffer[22]) & 0xff);
	nanos = 60 * minutes + seconds; //seconds
	nanos *= 75; //frames
	nanos += frames;
	nanos *= 40; //nanos
	nanos = nanos * 333333;
	setMediaLength(nanos);
	//System.out.println("track "+track+" mins "+minutes+" secs "+seconds+
	//		   " frames "+frames+" nanos "+nanos);
	//System.out.println(track);
	track |=  ((((int) buffer[23]) & 0xff) << 8);
	//System.out.println(track);
	//System.out.println("Audio opened");
	mci.ssendString("open cdaudio shareable wait");
	mci.ssendString("stop cdaudio wait");
	mci.ssendString("close cdaudio wait");
	if (eomThread == null) {
	    eomThread = new EomThread(this);
	    eomThread.start();
	} else
	    eomThread.start();
	return true;
    }
    /*
    protected boolean createNodes() {
	return true;
    }

    protected boolean connectNodes() {
	return true;
    }
    */
    protected boolean audioEnabled() {
	return true;
    }

    protected boolean videoEnabled() {
	return false;
    }

    public void gainChange(GainChangeEvent gce) {
    }

    public void muteChange(boolean state) {
	if ( state != muted ) {
	    muted = state;
	    mci.ssendString("open cdaudio shareable wait");
	    if (state) 
		mci.ssendString("set cdaudio audio all off wait");
	    else 
		mci.ssendString("set cdaudio audio all on wait");
	    mci.ssendString("close cdaudio wait");
	}
    }

    public void updateStats() {

    }
    
    public Time getDuration() {
	//System.out.println("getDuration is called: "+nanos);
	return new Time(nanos);
    }

    public TimeBase getMasterTimeBase() {
	return null;
    }

    public void doSetMediaTime(Time time) {
	long now = time.getNanoseconds();
	secs =(int)( now / 1000000000);
	hrs = secs / 3600;
	mins = secs / 60;
	secs = secs % 60;
	now %= 1000000000;
	frms = (int) (now / (40 *333333));
	//System.out.println("Setting time to "+mins+":"+secs+":"+frms);
    }

    public void handleEom() {
	setTargetState(Prefetched);
	processEndOfMedia();
    }
    
    class EomThread extends LoopThread {
	private Handler handler;
	public EomThread(Handler h) {
	    handler = h;
	}

	public boolean process() {

	    long now = getMediaTime().getNanoseconds();
	    if (now >= nanos) {
		handler.stop();
		handler.handleEom();
	    } else
		try {
		    sleep(100);
		} catch (InterruptedException e) {}
	    return true;
	}
    }
}


