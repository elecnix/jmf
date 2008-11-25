/*
 * @(#)DataSource.java	1.27 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol;

import java.awt.Component;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.io.*;
import java.net.*;
import javax.media.Manager;
import javax.media.Time;
import javax.media.MediaLocator;
import javax.media.ExtendedCachingControl;
import javax.media.DownloadProgressListener;
import javax.media.protocol.*;
import com.sun.media.util.*;
import com.sun.media.JMFSecurity;
import com.sun.media.IESecurity;
import com.sun.media.JMFSecurityManager;
import com.sun.media.ui.CacheControlComponent;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;


public class DataSource  extends PullDataSource {
    protected boolean connected = false;
    private String contentType = null;
    private PullSourceStream[] pssArray = new PullSourceStream[1];
    private CachedPullSourceStream cachedStream = null;
    private long contentLength = SourceStream.LENGTH_UNKNOWN; // -1
    private InputStream inputStream;
    private String fileSeparator = System.getProperty("file.separator");
    private boolean downLoadThreadStarted = false;
    private boolean isEnabledCaching = false;
    private ExtendedCachingControl[] cachingControls = new ExtendedCachingControl[0];

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

    public String getContentType() {
	if (!connected)
	    return null;
	return contentType;
    }

    public void connect() throws IOException {
	if (connected)
	    return;

	MediaLocator locator = getLocator();
	if (locator == null) {
	    throw(new IOException(this + ": connect() failed"));
	}

	URL url;
	URLConnection urlC;
	try {
	    url = locator.getURL();
	    urlC = url.openConnection(); // This will not throw security exception
	    urlC.setAllowUserInteraction(true);
	} catch (MalformedURLException e) {
	    // System.err.println(getLocator() +
	    //	       ": Don't know how to deal with non-URL locator yet!");
	    throw(new IOException(this + ": connect() failed"));
	}
	String protocol = url.getProtocol();


	// Note that even if we don't have connect privileges we can play
	// media from the same server from which the applet is downloaded.
	// Try to see if you can getInputStream without asking for
	// connect privilege
	boolean needConnectPermission = true;
	try {
	    inputStream = urlC.getInputStream();
	    needConnectPermission = false;
	    // System.out.println("got getInputStream without asking for security needConnectPermission " + needConnectPermission);

	} catch (Throwable e) {
	    // System.err.println("Unable to getInputStream without asking for permission " + e);
	}

	if (inputStream == null) {
	    if ( /*securityPrivelege &&*/ (jmfSecurity != null) ) {
		try {
		    if (jmfSecurity.getName().startsWith("jmf-security")) {
			jmfSecurity.requestPermission(m, cl, args, JMFSecurity.CONNECT);
			m[0].invoke(cl[0], args[0]);
		    } else if (jmfSecurity.getName().startsWith("internet")) {
			PolicyEngine.checkPermission(PermissionID.NETIO);
			PolicyEngine.assertPermission(PermissionID.NETIO);
		    }
		} catch (Exception e) {
		    if (JMFSecurityManager.DEBUG) {
			System.err.println("Unable to get connect " +
					   " privilege  " + e);
		    }
		    jmfSecurity.permissionFailureNotification(JMFSecurity.CONNECT);
		    throw new IOException("Unable to get connect permission" + e.getMessage());
		    // securityPrivelege = false;
		}
	    }


	    try {
		if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
		    Constructor cons = jdk12ConnectionAction.cons;
		    inputStream = (InputStream) jdk12.doPrivM.invoke(
                                           jdk12.ac,
					   new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               urlC,
                                           })});

		} else {
		    inputStream = urlC.getInputStream();
		}
	    } catch (Throwable e) {
		// System.err.println("Unable to open a URL connection " + e);
		throw new IOException(JMFI18N.getResource("error.connectionerror") +
				      e.getMessage());
	    }
	}

	if (inputStream == null) {
	    throw new IOException(JMFI18N.getResource("error.connectionerror") +
				  "Unable to open a URL connection");
	}

	if (protocol.equals("ftp")) {
	    contentType = "content/unknown";
	    // The contentType will be obtained after the
	    // getCorrectedContentType call
	} else {
	    contentType = urlC.getContentType();
	    contentLength = urlC.getContentLength();
	    // System.out.println("contentLength is " + contentLength);
	}

	contentType = ContentType.getCorrectedContentType(contentType,
							  locator.getRemainder());
	contentType = ContentDescriptor.mimeTypeToPackageName(contentType);

	// System.out.println("contentType is " + contentType);
	
	boolean cachingRequested = ((Boolean) Manager.getHint(Manager.CACHING)).booleanValue();

	// Don't do caching for hotmedia or flash
	if (  contentType.endsWith(".mvr") ||
              contentType.endsWith("x_shockwave_flash") ||
              contentType.endsWith("futuresplash") ) {
	    // System.err.println("Caching not done for hotmedia or flash");
	    cachingRequested = false;
	}

	String filePrefix = null;
	if ( cachingRequested ) {
	    // user wants caching. check to see if caching is allowed
	    filePrefix = Manager.getCacheDirectory();
	    if (filePrefix != null) {
		Object allowCachingObj = com.sun.media.util.Registry.get("secure.allowCaching");
		if (allowCachingObj != null) {
		    isEnabledCaching = ((Boolean) allowCachingObj).booleanValue();
		}
	    }
	}

	if (isEnabledCaching) {
	    // TODO: remove file name extension, eg .mov from cache file
	    String fileName = filePrefix + fileSeparator +
		generateFileName(getLocator().getRemainder());
	    try {
		cachedStream = new
		    CachedPullSourceStream(inputStream, fileName, contentLength, protocol);
		pssArray[0] = cachedStream;
		cachingControls = new ExtendedCachingControl[1];
		cachingControls[0] = new CachingControl(cachedStream);
		com.sun.media.Log.comment("Caching in " + filePrefix);
	    }  catch(IOException e) {
		isEnabledCaching = false;
	    }
	}


	if (!isEnabledCaching) {
	    try {
		pssArray[0] = new BasicPullSourceStream(url,
							inputStream,
							contentLength,
							needConnectPermission
							);
		cachedStream=null;
	    } catch(Exception ie) {
		pssArray[0] = null;
		throw new IOException(JMFI18N.getResource("error.connectionerror") +
				      ie.getMessage());
	    }
        }
	connected = true;
    }

    public void disconnect() {
	if (!connected)
	    return;
	if (cachedStream != null) {
	    cachedStream.close();
	    cachedStream = null;
	}
	pssArray[0] = null;
	connected = false;
    }

    public void start() throws IOException {
	if (!connected)
	    return;
	// TODO: see if you need downLoadThreadStarted
	if (cachedStream != null) {
	    if (!downLoadThreadStarted) {
		cachedStream.startDownload();
		downLoadThreadStarted = true;
	    } else {
		cachedStream.resumeDownload();
	    }
	}
    }

    public void stop() throws IOException { // TODO
	if (!connected)
	    return;
// 	if (cachedStream != null) {
// 	    cachedStream.pauseDownload();
// 	}
    }

    public PullSourceStream[] getStreams() {
	return pssArray;
    }

    public Time getDuration() {
	return null;
    }

    public Object[] getControls() {
	return cachingControls;
    }

    public Object getControl(String controlType) {
	if ( (cachingControls.length > 0) &&
	     (controlType.equals("javax.media.CachingControl")) ) {
	    return cachingControls[0];
	} else {
	    return null;
	}
    }



    //   TODO: can be moved into a file-utils file
    // Generate a new file name by combining actual filename
    //  + a random number + extension.
    static public String generateFileName(String infile) {
 
        String filename, ext = null;
        int sepindex = 0;
        java.util.Random generator = new java.util.Random();
        int dotindex = infile.lastIndexOf('.');
        int suffix = generator.nextInt();
 
        //
        // if dotindex is not found, it implies extension
        // doesn't exist. Then set the dotindex to the
        // length of the input file, infile.
        if (dotindex != -1) {
            ext = new String(infile.substring(dotindex));
	} else {
            dotindex = infile.length();
	}

        sepindex = infile.lastIndexOf(File.separatorChar);
	// some URL's on Wintel use either slash. So should we.
	sepindex = Math.max(infile.lastIndexOf('/'), sepindex);
        //
        // If sepindex equals to -1, the input file name doesn't
        // have a separator. Copy the filename from 0 up to the
        // the extension.
	if (sepindex >= dotindex) {
	    dotindex = infile.length();
	    ext = null;
	}

        filename = infile.substring(sepindex + 1, dotindex);
	String in;
	if (ext != null)
	    in = new String(filename + suffix + ext);
	else
	    in = new String(filename + suffix);
 	return convertNonAlphaNumericToUnderscore(in);
    }

    // Convert all non-alpha-numeric characters other than periods
    // to underscores (_).
    private static String convertNonAlphaNumericToUnderscore(String in) {
	if (in == null)
	    return null;

	// ... run through each char and convert
	//             !([A-Za-z0--9])   ->  '_'
	int len = in.length();
	char nm[] = new char[len];
	in.getChars(0, len, nm, 0);
	for (int i = 0; i < len; i++) {
	    char c = nm[i];
	    if (!(c == '.' ||
		  'A' <= c && c <= 'Z' ||
		  'a' <= c && c <= 'z' ||
		  '0' <= c && c <= '9')) {
		nm[i] = '_';
	    }
	}
	return new String(nm);
    }

    class CachingControl implements javax.media.ExtendedCachingControl {

	private CacheControlComponent controlComponent;
	private Component progressBar;
	private CachedPullSourceStream cpss;

	CachingControl(CachedPullSourceStream cpss) {
	    // TODO: CacheControlComponent may need cleanup
	    this.cpss = cpss;
	    controlComponent = new CacheControlComponent(this, null);
	    progressBar = controlComponent.getProgressBar();
	}

	// What is the purpose of this method in CachingControl? ?
	// Should we implement it by returning true if the
	// dowload thread is alive?
	public boolean isDownloading() {
	    return cpss.isDownloading();
	}

	
	public long getContentLength() {
	    return contentLength;
	}

	public long getContentProgress() {
	    return cpss.getContentProgress();
	}

	public Component getProgressBarComponent() {
	    return progressBar;
	}

	public Component getControlComponent() {
	    return controlComponent;
	}

	public void pauseDownload() {
	    cpss.pauseDownload();
	}

	public void resumeDownload() {
	    cpss.resumeDownload();
	}

	public long getStartOffset() {
	    return cpss.getStartOffset();
	}
	
	
	public long getEndOffset() {
	    return cpss.getEndOffset();
	}

	
	public void setBufferSize(Time t) {
	    //TODO
	}
	
	public Time getBufferSize() {
	    return null; // TODO
	}
	
	
	public void addDownloadProgressListener(DownloadProgressListener l,
					 int numKiloBytes) {
	    cpss.addDownloadProgressListener(l, numKiloBytes);
	}
	
	public void removeDownloadProgressListener(DownloadProgressListener l) {
	    cpss.removeDownloadProgressListener(l);
	}
	
    }
}
