/*
 * @(#)CachedPullSourceStream.java	1.29 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

// TODO: Handle write errors (file system full)


package com.sun.media.protocol;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.io.*;
import java.net.*;
import javax.media.*;
import javax.media.protocol.*;
import com.sun.media.util.*;
import com.sun.media.Log;
import com.sun.media.JMFSecurity;
import com.sun.media.JDK12Security;
import com.sun.media.IESecurity;
import com.sun.media.JMFSecurityManager;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;

import java.security.*;

public class CachedPullSourceStream implements Runnable, PullSourceStream,
					       Seekable, CachedStream {
    private InputStream stream;
    private RandomAccessFile readRAF = null;
    private RandomAccessFile writeRAF = null;
    private String fileName;
    private int bufferSize = 2048; // TODO: choose appropriate value
    private byte[] buffer = new byte[bufferSize];
    private boolean eosReached = false;
    private boolean ioException = false;
    private long length;
    private File file;
    private String protocol;
    private boolean readAborted = false;
    private boolean paused = false;
    private boolean abort = false;
    private MediaThread downloadThread;
    private long contentLength;
    private int highMarkFactor = 10;


    private boolean blockRead = true;
    private static int MAX_HIGH_MARK = 2000000;
    private static int DEFAULT_HIGH_MARK = 1000000;
    private static int MIN_HIGH_MARK = (8 * 1024);
    private int highMark = DEFAULT_HIGH_MARK;
    private int lowMark = 0;

    // TODO: remove enabled as it is not needed
     private boolean enabled = true;
//    private boolean startDownloadCalled = false;
    private boolean jitterEnabled = true;

    private DownloadProgressListener listener = null;
    private int numKiloBytesUpdateIncrement = -1;
    private boolean closed = true;

    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege=false;
    private Method m[] = new Method[1];
    private Class cl[] = new Class[1];
    private Object args[][] = new Object[1][0];

    private int maxCacheSize = Integer.MAX_VALUE;

    static {
	try {
	    jmfSecurity = JMFSecurityManager.getJMFSecurity();
	    securityPrivelege = true;
	} catch (SecurityException e) {
	}
    }

    public CachedPullSourceStream(InputStream stream, String fileName,
			   long contentLength, String protocol)
	throws IOException {
	this.stream = stream;
	this.contentLength = contentLength;
	this.fileName = fileName;
	this.protocol = protocol;

	/**
	 * We don't want to do caching unless we have read, write and
	 * delete privileges. We don't need thread and thread group
	 * permissions for caching
	 */
	if ( /* securityPrivelege &&*/ (jmfSecurity != null) ) {
	    String permission = null;
	    int permissionid = 0;
	    try {
		if (jmfSecurity.getName().startsWith("jmf-security")) {
		    try {
			// Note: the thread and thread_group permissions would have
			// been asked by MediaThread static initializer
			permission = "thread";
			jmfSecurity.requestPermission(m, cl, args, JMFSecurity.THREAD);
			m[0].invoke(cl[0], args[0]);
			
			permission = "thread group";
			jmfSecurity.requestPermission(m, cl, args, JMFSecurity.THREAD_GROUP);
			m[0].invoke(cl[0], args[0]);
		    } catch (Throwable t) {
			// Ignore Security Errors/Exceptions.
		    }

		    permission = "read file";
		    permissionid = JMFSecurity.READ_FILE;
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.READ_FILE);
		    m[0].invoke(cl[0], args[0]);
		    
		    
		    permission = "write file";
		    permissionid = JMFSecurity.WRITE_FILE;
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.WRITE_FILE);
		    m[0].invoke(cl[0], args[0]);
		    
		    permission = "delete file";
		    permissionid = JMFSecurity.DELETE_FILE;
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.DELETE_FILE);
		    m[0].invoke(cl[0], args[0]);
		    
		} else if (jmfSecurity.getName().startsWith("internet")) {
		    PolicyEngine.checkPermission(PermissionID.FILEIO);
		    PolicyEngine.assertPermission(PermissionID.FILEIO);
		    try {
			PolicyEngine.checkPermission(PermissionID.THREAD);
			PolicyEngine.assertPermission(PermissionID.THREAD);
		    } catch (Throwable t) {
		    }
		}
	    } catch (Exception e) {
		if (JMFSecurityManager.DEBUG) {
		    System.err.println("Unable to get " + permission +
				       " privilege  " + e);
		}
		if (permissionid > 0)
		    jmfSecurity.permissionFailureNotification(permissionid);
		securityPrivelege = false;
		// TODO: Do the right thing if permissions cannot be obtained.
		// User should be notified via an event
	    }
	}


         if (!securityPrivelege) {
           //System.out.println("JMF Fatal Error: don't have security privelege to use file caching");
            throw new IOException("No security privilege for caching");
         }

	 createFilesAndThread(fileName);

	 Object cdir = com.sun.media.util.Registry.get("secure.maxCacheSizeMB");

	 if ( (cdir != null) && (cdir instanceof Integer) ) {
	     int size = ((Integer) cdir).intValue();
	     if (size < 1)
		 size = 1;
	     maxCacheSize = size * 1000000; //bytes;
	 }

	 highMark = getHighMark(contentLength);
	 closed = false;
    }

    // Temporary method
    private int getHighMark(long contentLength) {
	if (contentLength <= 0)
	    return DEFAULT_HIGH_MARK;

	long tryHighMark = contentLength / highMarkFactor;

	if (tryHighMark < MIN_HIGH_MARK)
	    tryHighMark = MIN_HIGH_MARK;
	else if (tryHighMark > MAX_HIGH_MARK)
	    tryHighMark = MAX_HIGH_MARK;
	return (int) tryHighMark;
    }


    public void setEnabledBuffering(boolean b) {
	jitterEnabled = b;
    }

    public boolean getEnabledBuffering() {
	return jitterEnabled;
    }

    // Choose good name
    private void createFilesAndThread(String fileName) throws IOException {
	try {
	    file = new File(fileName);
	    // TODO: throw IOException if file exists
// 		// Highly unlikely as we use random numbers
// 	    if (file.exists()) {
// 		throw new IOException("Cache file " + fileName + " exists");
// 	    }

	    String parent = file.getParent();
	    File parentFile = null;
	    if (parent != null) {
		parentFile = new File(parent);
	    }


	    if (securityPrivelege && 
		(jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
		Constructor cons;
		if (parentFile != null) {
		    cons = jdk12MakeDirectoryAction.cons;
		    Boolean success;
		    success = (Boolean) jdk12.doPrivM.invoke(
					 jdk12.ac,
					 new Object[] {
			        cons.newInstance(
					 new Object[] {
				           parentFile
                                         })
				});
		    if ((success == null) || !success.booleanValue()) {
			throw new IOException("Unable to create directory " + parentFile);
		    }
		}

		cons = jdk12RandomAccessFileAction.cons;

		writeRAF = (RandomAccessFile) jdk12.doPrivM.invoke(
					 jdk12.ac,
					 new Object[] {
			        cons.newInstance(
					 new Object[] {
				           file.getPath(), "rw"
                                         })
				});

		if (writeRAF == null) {
		    throw new IOException("Cannot create cache file");
		}

		readRAF = (RandomAccessFile) jdk12.doPrivM.invoke(
					 jdk12.ac,
					 new Object[] {
			        cons.newInstance(
					 new Object[] {
				           file.getPath(), "r"
                                         })
				});

		if (readRAF == null) {
		    throw new IOException("Cannot create cache file");
		}

		cons = jdk12CreateThreadRunnableAction.cons;
		
		downloadThread = (MediaThread) jdk12.doPrivM.invoke(
                                           jdk12.ac,
					   new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               MediaThread.class,
                                               this
                                           })});

		downloadThread.setName("download");
		cons = jdk12PriorityAction.cons;
		jdk12.doPrivM.invoke(
				     jdk12.ac,
				     new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               downloadThread,
                                               new Integer(downloadThread.getVideoPriority())
                                           })});

	    } else {
		if (parentFile != null) {
		    if (!parentFile.exists() && !parentFile.mkdirs()) {
 			throw new IOException("Unable to create directory " + parentFile);
		    }
		}
		writeRAF = new RandomAccessFile(file, "rw");
		readRAF = new RandomAccessFile(file, "r");
		
		downloadThread = new MediaThread(this, "download");
		downloadThread.useVideoPriority();
	    }

	} catch (Throwable e) {
	    throw new IOException(e.getMessage());
	}
    }

    private synchronized void setLength(long length) {
	this.length = length;
    }

    
    private synchronized long getLength() {
	return length;
    }
    
    
    public void run() {
	int totalBytesRead = 0;
	int nextUpdate = numKiloBytesUpdateIncrement;
	int debugIndex = 1;

	if (ioException)
	    return;

	while (!eosReached) {

	    if (abort) {
		// System.out.println("run: abort true: download thread exit1");
		return;
	    }

	    try {
		// System.out.println("available is " + stream.available());

		/**
		 * The JDK designers could have made the available()
		 * more useful by returning -1 if End of Media (EOM) has
		 * been reached. The only way to check for EOM is
		 * to call read which is a blocking call
		 * Because of this limitation, if the contentLength is
		 * not known, then the available() call is not made
		 * (because it will always return 0 on EOM) prior to
		 * calling read.
		 */

		// For some reason https streams return available() always as 0
		// So don't call available() for https streams
		if ( (contentLength > 0) && (!protocol.equals("https")) ) {
		    while (stream.available() == 0) {
			synchronized(this) {
			    try {
				wait(25);
			    } catch (InterruptedException e) {
			    }
			}
			if (abort) {
			    // System.out.println("abort true: download thread exit2");
			    return;
			}
		    }
		}

		while (paused) {
		    // System.out.println("DOWNLOAD THREAD PAUSED");
		    synchronized(this) {
			// pause is triggered by user, so wait a longer time
			// to see if the user wants to resume download
			try {
			    wait(1000);
			} catch (InterruptedException e) {
			}
			if (abort) {
			    // System.out.println("abort true: download thread exit3");
			    return;
			}
		    }
		}
		// This read will never block if content length is known
		int bytesRead = stream.read(buffer, 0, buffer.length);
		if (bytesRead != -1) {
		    if ( (getLength() + bytesRead) > maxCacheSize ) {
			Log.warning("MAX CACHESIZE of " +
					   maxCacheSize + " reached ");
			contentLength = totalBytesRead;
			eosReached = true; // Is this correct?
		    }
		    writeRAF.write(buffer, 0, bytesRead);
		    totalBytesRead += bytesRead;
		    // 			long length = writeRAF.length();
		    long length = totalBytesRead;
		    setLength(length);

		    if (length == contentLength) {
			eosReached = true;
			// System.out.println("TOTAL DOWNLOAD: " + totalBytesRead);
		    }
		    // TODO: support multiple listeners??
		    if (listener != null) {
			if (totalBytesRead >= nextUpdate) {
			    listener.downloadUpdate();
			    nextUpdate += numKiloBytesUpdateIncrement;
			}
		    }

		} else {
		    //setLength(writeRAF.length());
		    setLength(totalBytesRead);
		    contentLength = totalBytesRead;
		    eosReached = true;
		}
		loadUpdate();
	    } catch (IOException e) {
		Log.warning(e + " : Check if you have enough space in the cache directory");
		ioException = true;
		eosReached = true;
		blockRead = false;
		break;
	    }
	}

	if (listener != null) {
	    listener.downloadUpdate();
	}

	if (writeRAF != null) {
	    try {
		writeRAF.close();
		writeRAF = null;
	    } catch (IOException e) {
	    }
	    writeRAF = null;
	}

    }


    // TODO: In the current implementation, once abortDownload() is called
    // presumably in response to player.close(), a subsequent
    // startDownload will have no effect. If we want startDownload to start
    // a new download thread if there is no download thread running, we can
    // easily do that.
    void startDownload() {
// 	synchronized(this) {
// 	    startDownloadCalled = true;
// 	}
	if (enabled) {
	    if (downloadThread != null) {
		downloadThread.start();
	    }
	}
    }

    void pauseDownload() {
	if ( (downloadThread != null) && !downloadThread.isAlive() )
	    return;
	if (enabled) {
	    synchronized(this) {
		if (!paused) {
		    paused = true;
		    // System.out.println("setting paused to true and notify");
		    notify();
		}
	    }
	}
    }


    void resumeDownload() {
	if ( (downloadThread != null) && !downloadThread.isAlive() )
	    return;
	if (enabled) {
	    synchronized(this) {
		if (paused) {
		    paused = false;
		    // System.out.println("setting paused to false and notify");
		    notify();
		}
	    }
	}
    }


    public void abortDownload() {
	// System.out.println("abortDownload");
	abort = true;
    }


    public void abortRead() {
	synchronized (this) {
	    readAborted = true;
	}
    }

    public long seek(long where) {
	int debugTime = 0;
	// TODO: IMPORTANT: check for race condition. try better solution
	synchronized(this) {
	    readAborted = false;
	}
	try {
// 	    drainCondition(where);
// 	    if ( !jitterEnabled  || !blockRead) {
	    if (!jitterEnabled || !drainCondition(where)) {
		if (where <= getLength()) {  // <
		    return doSeek(where);
		}
	    }
		
	    while (true) {
		if (eosReached) {
		    if ( where <= getLength() ) { // <
			return doSeek(where);
		    } else {
			return -1; // Attempt to seek past EOF
		    }
		}
		    
		if (jitterEnabled) {
		    synchronized(this) {
			while (blockRead) {
			    try {
				wait(100);
			    } catch (InterruptedException e) {
			    }
			    if (readAborted) {
// 				System.out.println("seek changed. discard: " +
// 						   Thread.currentThread().getName());
				readAborted = false;
				return com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD;
			    }
			}
		    }
		}

		if (readAborted) {
// 		    System.out.println("seek changed. discard: " +
// 				       Thread.currentThread().getName());
		    readAborted = false;
		    return com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD;
		}

		if (where <= getLength()) { // <
		    return doSeek(where);
		}
		    
		try {
		    Thread.currentThread().sleep(250);
		} catch (InterruptedException e) {
		}
	    }
	} finally {
	    // System.out.println("seek to " + where + " success");
	    if (jitterEnabled)  // TODO: remove after test
		drainCondition(where);
	}
    }

    private long getWriteReadPtrOffset() {
	// System.out.println("getLength, tell is " + getLength() + " : " + tell());
	return getLength() - tell();
    }

    private synchronized void loadUpdate() {
// 	System.out.println("loadUpdate: " + blockRead + " : " +
// 			   getWriteReadPtrOffset() + " : " + highMark);
	if (blockRead) {
	    if ( ( eosReached || ( getWriteReadPtrOffset() >= highMark) ) ) {
// 		System.out.println("loadUpdate: setting blockRead to false " +
// 				   getLength() + " : " + tell());
		blockRead = false;
		synchronized(this) {
		    notify();
		}
	    }
	}
    }

    private synchronized boolean drainCondition() {
	return drainCondition(tell());
    }

    private synchronized boolean drainCondition(long offset) {
	offset = getLength() - offset;
	if ( eosReached ) {
	    if (blockRead) {
		blockRead = false;
		notify();
	    }
	    return false;
	}

	if (blockRead) {
	    if (offset < highMark) {
		return true;
	    } else {
		blockRead = false;
		notify();
		return false;
	    }
	} else {
	    if (offset < lowMark) {
		blockRead = true;
		return true;
	    } else {
		return false;
	    }
	}
    }

    public boolean willReadBytesBlock(long offset, int numBytes) {
	if (jitterEnabled && drainCondition(offset)) {
	    return true;
	}
	return ( (offset + numBytes) >  getLength());
    }

    public boolean willReadBytesBlock(int numBytes) {
	return willReadBytesBlock(tell(), numBytes);
    }


    private int waitUntilSeekWillSucceed(long where) throws IOException {
	boolean debugPrint = true;

	// TODO: check for race condition. try better solution
 	if (!jitterEnabled || !drainCondition(where)) {

	    if (where <= getLength()) { // <
		return 0;
	    }
	}

	while (true) {
	    if (eosReached) {
		if ( where <= getLength() ) { // <
		    return 0;
		} else {
		    // throw new IOException("Attempt to read past EOM");
		    return -1;
		}
	    }


	    if (jitterEnabled) {
		synchronized(this) {
		    while (blockRead) {
			try {
			    wait(100);
			} catch (InterruptedException e) {
			}
			if (readAborted) {
			    return com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD;
			}
		    }
		}
	    }


	    if (readAborted) {
		return com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD;
	    }

	    if (where <= getLength()) { // <
		return 0;
	    }
	    // TODO: replace sleep with wait/notify
	    try {
//		if (debugPrint) {
// 		    System.out.println("waitUntilSeekWillSucceed: " + where + " : " +
// 				       getLength() + " eos? " + eosReached);
//		    debugPrint = false;
//		}
		Thread.currentThread().sleep(250);
	    } catch (InterruptedException e) {
	    }
	}
    }


    /**
     * need to synchronize because a close (datasource.disconnect) can
     * come anytime
     */
    public long tell() {
	synchronized(this) {
	    if (closed)
		return -1;
	    try {
		return readRAF.getFilePointer();
	    } catch (IOException e) {
		// System.out.println(Thread.currentThread() + " tell: " + e);
		// e.printStackTrace();
		return -1;
	    }
	}
    }
	
    /**
     * need to synchronize because a close (datasource.disconnect) can
     * come anytime
     */
    private synchronized long doSeek(long where) {
	if (closed)
	    return -1;
	try {
	    readRAF.seek(where);
	    return readRAF.getFilePointer();
	} catch (IOException e) {
	    return -1;
	}
    }

    /**
     * need to synchronize because a close (datasource.disconnect) can
     * come anytime
     */
    public synchronized int doRead(byte[] buffer, int offset, int length)
	throws IOException {

	if (closed)
	    return -1;
	try {
	    // 	    System.out.println("before reading " + length);
	    // 	    System.out.println("to read " + length + " from " + tell() +
	    // 			       " getLength() is " + getLength());
	    int actual = readRAF.read(buffer, offset, length);
	    // 	    System.out.println("actual read is " + actual);
	    return actual;
	    // return readRAF.read(buffer, offset, length);
	} catch (ArrayIndexOutOfBoundsException e) {
	    // TODO: remove this catch block
	    // System.out.println("warning:aioubexception: reas: buffer_length, offset, length " + buffer.length + " : " + offset + " : " + length);
	    e.printStackTrace();
	    return com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD;
	}

    }


    private synchronized void doClose() {
	try {
	    closed = true;
	    if (readRAF != null) {
		readRAF.close();
	    }
	    if (writeRAF != null) {
		writeRAF.close();
	    }
	    if (file == null)
		return;
	    
	    deleteFile(file);
	    file = null;
	    
	} catch (IOException e) {
	}
    }


    public boolean isRandomAccess() {
	if (enabled)
	    return true;
	try {
	    Seekable s = (Seekable) stream;
	    return s.isRandomAccess();
	} catch (ClassCastException e) {
	    return false;
	}
    }

    public boolean willReadBlock() {
	return false;
    }


    public int read(byte[] buffer, int offset, int length)
	throws IOException {

	try {
	    int result = waitUntilSeekWillSucceed(tell() + length);
	    if (result == -1)
		return -1; // EOS
	    if (result != com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
		return doRead(buffer, offset, length);
	    } else {
		// System.out.println(" read returns " + result);
		return result;
	    }
	} finally {
	    if (jitterEnabled)  // TODO: remove after test
		drainCondition(); // to update blockRead
	}
    }

    // TODO
    public ContentDescriptor getContentDescriptor() {
	return null;
    }

    public boolean endOfStream() {
	return false; // TODO
    }

    public Object[] getControls() {
	return new Object[0];
    }

    public Object getControl(String controlType) {
	    return null;
    }

    // TODO: Make only the relevant sections synchronized
    void  close() {
	if (!abort)
	    abortDownload();
	// Note: downloadThread is never set to null.
	// It is null only when it is not initialized
	if (downloadThread != null) {
	    // Wait for download thread to die
	    for (int i = 0; i < 20; i++) {
		if ( !downloadThread.isAlive() ) {
		    // System.out.println("downloadThread is dead");
		    break;
		}
		try {
		    Thread.currentThread().sleep(100);
		} catch (InterruptedException e) {
		}
	    }
	    // The thread didn't quit
	    // in 2 seconds when abort is true.
	    // This generally won't happen if the contentLength
	    // is known; in this case stream.read will never block
	    // as we call available() first.
	    // This may happen if the contentLength is not known
	    // and if the stream.read() call blocks for more than
	    // 2 seconds. In this case we have have no choice but
	    // to kill the download thread

	    // Commenting out code below as stopping a Thread is
	    // deprecated
// 	    if (downloadThread.isAlive()) {
// 		// System.out.println("Killing downloadThread");
// 		downloadThread.stop();
// 	    }
	}
	doClose();
    }


    /**
     * Get the total number of bytes in the media being downloaded. Returns
     * <code>LENGTH_UNKNOWN</code> if this information is not available.
     *
     * @return The media length in bytes, or <code>LENGTH_UNKNOWN</code>.
     */ 
     public long getContentLength() {
 	return contentLength;
     }

    /**
     * Get the total number of bytes of media data that have been downloaded so far.
     *
     * @return The number of bytes downloaded.
     */
    long getContentProgress() {
	return length;
    }


    // TODO: create a list of DownloadProgressListener
    void addDownloadProgressListener(DownloadProgressListener l,
					    int numKiloBytes) {
	listener = l;
	if (numKiloBytes <= 0)
	    numKiloBytes = 1024;
	numKiloBytesUpdateIncrement = numKiloBytes * 1024;
    }


    // TODO: remove the listener from the list
    void removeDownloadProgressListener(DownloadProgressListener l) {
	listener = null;
    }

    long getStartOffset() {
	return 0L;
    }


    long getEndOffset() {
	return length;
    }

    private boolean deleteFile(File file) {

	boolean fileDeleted=false;
	try {
	    if ( /*securityPrivelege &&*/ (jmfSecurity != null) ) {
		try {
		    if (jmfSecurity.getName().startsWith("jmf-security")) {
			jmfSecurity.requestPermission(m, cl, args, JMFSecurity.DELETE_FILE);
			m[0].invoke(cl[0], args[0]);
		    } else if (jmfSecurity.getName().startsWith("internet")) {
			PolicyEngine.checkPermission(PermissionID.FILEIO);
			PolicyEngine.assertPermission(PermissionID.FILEIO);
		    }
		} catch (Exception e) {
		    if (JMFSecurityManager.DEBUG) {
			System.err.println("Unable to get DELETE_FILE " +
					   " privilege  " + e);
		    }
		    securityPrivelege = false;
		    // TODO: Do the right thing if permissions cannot be obtained.
		    // User should be notified via an event, if applicable
		}
	    }
 	    if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
		Constructor cons = jdk12DeleteFileAction.cons;
		Boolean success;
		success = (Boolean) jdk12.doPrivM.invoke(
					 jdk12.ac,
					 new Object[] {
			        cons.newInstance(
					 new Object[] {
				           file
                                         })
				});
		fileDeleted = success.booleanValue();
	    } else {
		fileDeleted = file.delete();
	    }
	} catch (Throwable e) {
	}
        return fileDeleted;
    }

    boolean isDownloading() {
	if (eosReached)
	    return false;
	return (length != LENGTH_UNKNOWN);
    }

}

