/*
 * @(#)DataSource.java	1.31 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.file;

import java.security.*;

import java.util.Hashtable;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.io.*;
import java.net.*;
import javax.media.*;
import javax.media.protocol.*;
import com.sun.media.*;
import com.sun.media.util.*;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;

public class DataSource  extends PullDataSource implements SourceCloneable {

    private RandomAccessFile raf;
    private boolean connected = false;
    private long length = -1;
    private String contentType = null;
    private PullSourceStream[] pssArray = new PullSourceStream[1];

    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege=false;
    private Method m[] = new Method[1];
    private Class cl[] = new Class[1];
    private Object args[][] = new Object[1][0];

    private static Hashtable mimeTable;

//     private String listOfAllowedMediaFileExtensions =
//      "au wav aif aiff mid midi rmf gsm mpa mp2 mp3 g728 g729 g729a mov avi mpg mpv viv mvr swf spl ra ram";


    static {
	try {
	    jmfSecurity = JMFSecurityManager.getJMFSecurity();
	    securityPrivelege = true;

 	    mimeTable = MimeManager.getDefaultMimeTable();
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
	    System.err.println("medialocator is null");
	    throw(new IOException(this + ": connect() failed"));
	}

	//////////////////////////////////////////////////////////////////////
	// Getting the URL only to get the content type.
	// Is there a better way?
	URL url;
	try {
	    url = locator.getURL();
	} catch (MalformedURLException e) {
	    System.err.println(getLocator() +
			       ": Don't know how to deal with non-URL locator yet!");
	    throw(new IOException(this + ": connect() failed"));
	}
	
	String fileName = getFileName(locator);

	// For applets, check to see if the media file has a file extension
	// If not, throw an IOException with the following message:
	// "For security reasons, from an applet, cannot read a media file with no extension"
	// If there is a file extension, make sure it is registered in the
	// mimetable.
	// If not throw an IOException.

	if (jmfSecurity != null) {
	    int i = fileName.lastIndexOf(".");
	    if (i != -1) {
		String ext = fileName.substring(i+1).toLowerCase();
		if (!mimeTable.containsKey(ext)) {
		    // Treat aif as a special case due to bug in IE VM
		    if (!ext.equalsIgnoreCase("aif")) 
			throw new IOException("Permission Denied: From an applet cannot read media file with extension " + ext);
		}
	    } else {
		throw new IOException("For security reasons, from an applet, cannot read a media file with no extension");
	    }
	}


	try {
	    if ( /*securityPrivelege && */ (jmfSecurity != null) ) {
		try {
		    if (jmfSecurity.getName().startsWith("jmf-security")) {
			jmfSecurity.requestPermission(m, cl, args, JMFSecurity.READ_FILE);
			m[0].invoke(cl[0], args[0]);
		    } else if (jmfSecurity.getName().startsWith("internet")) {
			PolicyEngine.checkPermission(PermissionID.FILEIO);
			PolicyEngine.assertPermission(PermissionID.FILEIO);
		    }
		} catch (Throwable e) {
		    if (JMFSecurityManager.DEBUG) {
			System.err.println("Unable to get read file" +
					   " privilege  " + e);
		    }
		    jmfSecurity.permissionFailureNotification(JMFSecurity.READ_FILE);
		    throw new IOException("No permissions to read file");
		}
	    }



	    if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
		try {
		    Constructor cons = jdk12RandomAccessFileAction.cons;

		    raf = (RandomAccessFile) jdk12.doPrivM.invoke(
					 jdk12.ac,
					 new Object[] {
			        cons.newInstance(
					 new Object[] {
				           fileName, "r"
                                         })
				});
		    
		} catch (Throwable e) {
		    throw new IOException(JMFI18N.getResource("error.filenotfound"));
		}
	    } else {
		raf = new RandomAccessFile(fileName, "r");
 	    }

	    length = raf.length();
	    if (length < 0)
		length = SourceStream.LENGTH_UNKNOWN;
	    PullSourceStream pss = new RAFPullSourceStream();
	    pssArray[0] = pss;
	    

	    // TODO: jdk1.2 check if you need permission to call getContentType
	    URLConnection urlC = url.openConnection();
	    try {
		contentType = urlC.getContentType();
	    } catch (Throwable t) {
		contentType = null;
	    }
	    contentType = ContentType.getCorrectedContentType(contentType,
							      locator.getRemainder());

		/*
		 * shivak : if we get a "unknown" type or if it is "mpeg" (for MPEG1)
		 * we parse the stream for MPEG 1/2 types and return the content type
		 */
		/*
		disabled for JMF 2.1.

		if ( contentType.equals("content/unknown") ||
		     contentType.equals("video/mpeg") ) {
			contentType = MediaStreamParser.parseStream(raf,contentType) ;
		}
		*/

	    contentType = ContentDescriptor.mimeTypeToPackageName(contentType);

	    // How do I close the URLConnection??
	    //////////////////////////////////////////////////////////////////////

	    connected = true;
	} catch (Throwable e) {
	    throw new IOException(JMFI18N.getResource("error.filenotfound"));
	}
    }



    public void disconnect() {
	try {
	    if (raf != null) {
		raf.close();
	    }
	} catch (IOException e) {
	}
	if ( pssArray != null ) {
	    pssArray[0] = null;
	}
	connected = false;
    }

    public void start() throws IOException {
    }

    public void stop() throws IOException {
    }

    public void setLocator (MediaLocator ml) {

	// If it's file protocol, we'll try to strip out special characters
	// in the URL syntax:
	// %xx = the ASCII represented by the hexadecimal number "xx".
	if (ml != null && ml.getProtocol() != null && ml.getProtocol().equals("file")) {
	    int idx;
	    MediaLocator saved = ml;
	    String file = ml.getRemainder();
	    boolean changed = false;

	    if (file == null) {
		super.setLocator(ml);
		return;
	    }
	    try {
		idx = 0;
		while ((idx = file.indexOf("%", idx)) >= 0) {
		    if (file.length() > idx + 2) {
			byte [] bytes = new byte[1];
			try {
			    bytes[0] = (byte)Integer.valueOf(
						file.substring(idx + 1, idx + 3), 16).intValue();
			    file = file.substring(0, idx) + new String(bytes) +
				file.substring(idx + 3);
			    changed = true;
			} catch (NumberFormatException ne) {
			}
		    }
		    idx++;
		}
		if (changed)
	    	    ml = new MediaLocator(ml.getProtocol() + ":" + file);
	    } catch (Exception e) {
		ml = saved;
	    }
	}

	super.setLocator(ml);
    }

    public PullSourceStream[] getStreams() {
	return pssArray;
    }

    public Time getDuration() {
	return Duration.DURATION_UNKNOWN;
    }

    public Object[] getControls() {
	return new Object[0];

    }

    public Object getControl(String controlType) {
	return null;
    }

    public javax.media.protocol.DataSource createClone() {
	DataSource ds = new com.sun.media.protocol.file.DataSource();
	ds.setLocator(getLocator());
	if (connected) {
	    try {
		ds.connect();
	    } catch (IOException e) {
		return null;
	    }
	}
	return ds;
    }


    class RAFPullSourceStream implements PullSourceStream, Seekable {
	
	public long seek(long where) {
	    try {
		raf.seek(where);
		return tell();
	    } catch (IOException e) {
		System.out.println("seek: " + e);
		return -1;
	    }

	}

	public long tell() {
	    try {
		return raf.getFilePointer();
	    } catch (IOException e) {
		System.out.println("tell: " + e);
		return -1;
	    }
	}

	public boolean isRandomAccess() {
	    return true;
	}

	public boolean willReadBlock() {
	    return false;
	}

	public int read(byte[] buffer, int offset, int length)
	    throws IOException {
	    return raf.read(buffer, offset, length);
	}

	// TODO
	public ContentDescriptor getContentDescriptor() {
	    // System.out.println("in getContentDescriptor"); // TODO
	    return null;
	}

	public long getContentLength() {
	    return length;
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

    }

    public static String getFileName(MediaLocator locator) {
	try {
	    URL url = locator.getURL();

	    String fileName = locator.getRemainder();
	    // Parse filename to de-normalize it.
	    String saved = fileName;
	    try {
		// Change %xy to a character
		int idx = 0;
		while ((idx = fileName.indexOf("%", idx)) >= 0) {
		    if (fileName.length() > idx + 2) {
			byte [] bytes = new byte[1];
			try {
			    bytes[0] = (byte)Integer.valueOf(
							     fileName.substring(idx + 1, idx + 3), 16).intValue();
			    fileName = fileName.substring(0, idx) +
				new String(bytes) +
				fileName.substring(idx + 3);
			} catch (NumberFormatException ne) {
			}
		    }
		    idx++;
		}
		
		// Change | to :
		idx = 0;
		while ((idx = fileName.indexOf("|")) >= 0) {
		    if (idx > 0) {
			fileName = fileName.substring(0, idx) + ":" +
			    fileName.substring(idx + 1);
		    } else {
			fileName = fileName.substring(1);
		    }
		}
		
		while (fileName.startsWith("///")) {
		    fileName = fileName.substring(2);
		}
		
		if (System.getProperty("os.name").startsWith("Windows")) {
		    while (fileName.charAt(0) == '/' &&
			   fileName.charAt(2) == ':') {
			fileName = fileName.substring(1);
		    }
		}
	    } catch (Exception e) {
		fileName = saved;
	    }
	    return fileName;
	} catch (Throwable t) {
	    return null;
	}
    }

}
