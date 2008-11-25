/*
 * @(#)CloneableSourceStreamAdapter.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.protocol;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.io.*;
import java.util.*;
import java.lang.*;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.format.*;

import com.sun.media.*;
import com.sun.media.util.*;
import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;

public class CloneableSourceStreamAdapter {
  
    SourceStream master;
    SourceStream adapter = null;
    Vector slaves = new Vector();

    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege=false;
    private Method m[] = new Method[1];
    private Class cl[] = new Class[1];
    private Object args[][] = new Object[1][0];
    protected int numTracks = 0;
    protected Format [] trackFormats;


    static {
	try {
	    jmfSecurity = JMFSecurityManager.getJMFSecurity();
	    securityPrivelege = true;
	} catch (SecurityException e) {
	}
    }

    /**
     * Constructor
     */
    CloneableSourceStreamAdapter(SourceStream master) {
    
	this.master = master;

	// create the matching adapter according to the stream's type
	if (master instanceof PullSourceStream)
	    adapter = new PullSourceStreamAdapter();
	if (master instanceof PullBufferStream)
	    adapter = new PullBufferStreamAdapter();
	if (master instanceof PushSourceStream)
	    adapter = new PushSourceStreamAdapter();
	if (master instanceof PushBufferStream)
	    adapter = new PushBufferStreamAdapter();
    }


    /**
     * Return the stream adapter to be used by the Handler. There is only 
     * one adapter per stream since there is only one master stream.
     */
    SourceStream getAdapter() {
    
	return adapter;
    }

    /**
     * This method should be could only by the <CODE>CloneableDataSource</CODE>.
     *
     * @return a slave <CODE>SourceStream</CODE> which will either a 
     * <CODE>PushSourceStream</CODE> or a <CODE>PushBufferStream.
     */
    SourceStream createSlave() {

	SourceStream slave = null;

	if ((master instanceof PullSourceStream) ||
	    (master instanceof PushSourceStream))
	    slave = new PushSourceStreamSlave();
	if ((master instanceof PullBufferStream) || 
	    (master instanceof PushBufferStream))
	    slave = new PushBufferStreamSlave();
	slaves.addElement(slave);
    
	return slave;
    }

    void copyAndRead(Buffer b) throws IOException {

	if (master instanceof PullBufferStream) 
	    ((PullBufferStream)master).read(b);
    
	if (master instanceof PushBufferStream) 
	    ((PushBufferStream)master).read(b);
    
	for (Enumeration e = slaves.elements(); e.hasMoreElements();) {
	    Object stream = e.nextElement(); 
	    ((PushBufferStreamSlave)stream).setBuffer((Buffer)b.clone());
	    Thread.yield();
	}
    }

    int copyAndRead(byte[] buffer, int offset, int length) throws IOException {

	int totalRead = 0;
	if (master instanceof PullSourceStream)
	    totalRead = ((PullSourceStream)master).read(buffer, offset, length);
	if (master instanceof PushSourceStream)
	    totalRead = ((PushSourceStream)master).read(buffer, offset, length);
 
	for (Enumeration e = slaves.elements(); e.hasMoreElements();) {
	    Object stream = e.nextElement(); 
	    byte[] copyBuffer = new byte[totalRead];
	    System.arraycopy(buffer, offset, copyBuffer, 0, totalRead);
	    ((PushSourceStreamSlave)stream).setBuffer(copyBuffer);
	}
    
	return totalRead;
    }



    ////////////////////////////
    //
    // INNER CLASSES
    ////////////////////////////

    class SourceStreamAdapter implements SourceStream {

	public ContentDescriptor getContentDescriptor() {
      
	    return master.getContentDescriptor();
	}
    
	public long getContentLength() {
      
	    return master.getContentLength();
	}
    
	public boolean endOfStream() {
      
	    return master.endOfStream();
	}
    
	public Object[] getControls() {
      
	    return master.getControls();
	}
    
	public Object getControl(String controlType) {

	    return master.getControl(controlType);
	}
    }


    abstract class PushStreamSlave extends SourceStreamAdapter 
	implements SourceStreamSlave, Runnable {
	MediaThread notifyingThread;
	boolean connected = false;

	public synchronized void connect() {

	    if (connected)
		return;

	    connected = true;

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
		    // TODO: Do the right thing if permissions cannot 
		    // be obtained.
		    // User should be notified via an event
		}
	    }

	    if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
		try {
		    Constructor cons = jdk12CreateThreadRunnableAction.cons;
		    notifyingThread = (MediaThread) jdk12.doPrivM.invoke(
                                           jdk12.ac,
					   new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               MediaThread.class,
					       this
				   })});
		} catch (Exception e) { }
	    } else {
		notifyingThread = new MediaThread(this);
	    }
	    if (notifyingThread != null) {
		if (master instanceof PushBufferStream) {
		    if (((PushBufferStream)master).getFormat() instanceof VideoFormat)
			notifyingThread.useVideoPriority();
		    else
			notifyingThread.useAudioPriority();
		}
		notifyingThread.start(); // You dont need permission for start
	    }

	}

	public synchronized void disconnect() {
	    connected = false;
	    notifyAll();
	}
    }


    class PushSourceStreamSlave extends PushStreamSlave
    implements PushSourceStream, Runnable {
    
	SourceTransferHandler handler;
	private byte[] buffer;

	/**
	 * Set the buffer this stream can provide for the next read
	 */
	synchronized void setBuffer(byte[] buffer) {
	    this.buffer = buffer;
	    notifyAll();
	}
    
	public synchronized int read(byte[] buffer, int offset, int length) 
	    throws IOException {
      
	    if (length + offset > buffer.length)
		throw new IOException("buffer is too small");

	    // block till we have a buffer to read from
	    while (this.buffer == null && connected) { 
		try {
		    wait(50);
		}
		catch (InterruptedException e) {
		    System.out.println("Exception: " + e);
		}
	    }

	    if (!connected)
		throw new IOException("DataSource is not connected");
      
	    int copyLength = (length > this.buffer.length ? this.buffer.length : length);
	    System.arraycopy(this.buffer, 0, buffer, offset, copyLength);
	    this.buffer = null;

	    return copyLength;
	}
    
	public int getMinimumTransferSize() {
      
	    return ((PushSourceStream)master).getMinimumTransferSize();
	}
    
	public void setTransferHandler(SourceTransferHandler transferHandler) {

	    handler = transferHandler;
	}

	SourceTransferHandler getTransferHandler() {

	    return handler;
	}

	/**
	 * Implementation of Runnable inteface.
	 */
	public void run() {

	    while (!endOfStream() && connected) {
		try {
		    synchronized(this) {
			wait(); // till we will be notified that a read occured
		    }
		} catch (InterruptedException e) {
		    System.out.println("Exception: " + e);
		}

		if (connected && handler != null)
		    handler.transferData((PushSourceStream)this);
	    }
	}
    }
  

    class PushBufferStreamSlave extends PushStreamSlave 
    implements PushBufferStream, Runnable {

	BufferTransferHandler handler;
	private Buffer b;

	/**
	 * Set the buffer this stream can provide for the next read
	 */
	synchronized void setBuffer(Buffer b) {
	    this.b = b;
	    notifyAll();
	}

	public javax.media.Format getFormat() {
       
	    return ((PushBufferStream)master).getFormat();
	}
    
	public synchronized void read(Buffer buffer) throws IOException {
      
	    // block till we have a buffer to read from
	    while (b == null && connected) { 
		try {
		    wait(50);
		}
		catch (InterruptedException e) {
		    System.out.println("Exception: " + e);
		}
	    }

	    if (!connected)
		throw new IOException("DataSource is not connected");
      
	    buffer.copy(b);
	    b = null;
	}
    
	public int getMinimumTransferSize() {
      
	    return ((PushSourceStream)master).getMinimumTransferSize();
	}
    
	public void setTransferHandler(BufferTransferHandler transferHandler) {
      
	    handler = transferHandler;
	}
   
	BufferTransferHandler getTransferHandler() {
      
	    return handler;
	}

	/**
	 * Implementation of Runnable inteface.
	 */
	public void run() {

	    while (!endOfStream() && connected) {
		try {
		    synchronized(this) {
			wait(); // till we will be notified that a read occured
		    }
		} catch (InterruptedException e) {
		    System.out.println("Exception: " + e);
		}
		if (connected && handler != null)
		    handler.transferData((PushBufferStream)this);
	    }
	}
    }

  
    class PullSourceStreamAdapter extends SourceStreamAdapter 
    implements PullSourceStream {
    
	public boolean willReadBlock() {
      
	    return ((PullSourceStream)master).willReadBlock();
	}
    
	public int read(byte[] buffer, int offset, int length)
	    throws IOException {
      
	    return copyAndRead(buffer, offset, length);
	}
    }
    

    class PullBufferStreamAdapter extends SourceStreamAdapter 
    implements PullBufferStream {

	public boolean willReadBlock() {
      
	    return ((PullBufferStream)master).willReadBlock();
	}
    
	public void read(Buffer buffer)throws IOException {
      
	    copyAndRead(buffer);
	}
    
	public javax.media.Format getFormat() {
      
	    return ((PullBufferStream)master).getFormat();
	}
    }


    class PushSourceStreamAdapter extends SourceStreamAdapter 
    implements PushSourceStream, SourceTransferHandler {

	SourceTransferHandler handler;

	public int read(byte[] buffer, int offset, int length) throws IOException {

	    return copyAndRead(buffer, offset, length);
	}
    
	public int getMinimumTransferSize() {

	    return ((PushSourceStream)master).getMinimumTransferSize();
	}

	public void setTransferHandler(SourceTransferHandler transferHandler) {

	    handler = transferHandler;
	    ((PushSourceStream)master).setTransferHandler(this);
	}

	public void transferData(PushSourceStream stream) {

	    if (handler != null)
		handler.transferData(this);
	}
    }


    class PushBufferStreamAdapter extends SourceStreamAdapter 
    implements PushBufferStream, BufferTransferHandler {

	BufferTransferHandler handler;

	public javax.media.Format getFormat() {

	    return ((PushBufferStream)master).getFormat();
	}
    
	public void read(Buffer buffer) throws IOException {
      
	    copyAndRead(buffer);
	}

	public void setTransferHandler(BufferTransferHandler transferHandler) {

	    handler = transferHandler;
	    ((PushBufferStream)master).setTransferHandler(this);
	}
    
	public void transferData(PushBufferStream stream) {

	    if (handler != null)
		handler.transferData(this);
	}
    }
}





