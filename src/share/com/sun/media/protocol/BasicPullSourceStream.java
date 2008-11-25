/*
 * @(#)BasicPullSourceStream.java	1.13 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.protocol;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.io.*;
import java.net.*;
import javax.media.*;
import javax.media.protocol.*;
import com.sun.media.util.*;
import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;
import com.sun.media.IESecurity;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;

public class BasicPullSourceStream implements PullSourceStream, Seekable  {

    /**
     * the stream which PullSourceStream's inheriting classes handles
     */
    protected InputStream stream;

    /**
     * the current location in the stream
     */
    protected long location;

    /**
     * a flag to indicate EOF reached
     */
    protected boolean eofReached;

    protected long  contentLength;
    protected URL url;
    protected URLConnection urlC;

    private boolean needConnectPermission;
    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege = false;
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

    public BasicPullSourceStream(URL url,
				 InputStream stream,
				 long contentLength,
				 boolean needConnectPermission
				 ) throws IOException {

	this.needConnectPermission = needConnectPermission;
	// System.out.println("bpss: needConnectPermission is " + needConnectPermission);
        if (stream != null) {
	    this.stream = stream; // stream is opened
	    this.contentLength = contentLength;
        } else {
	    // Check with Doron if we can remove the else block.
	    // TODO: if not, add security stuff.
	    try {
		urlC = url.openConnection();
		this.contentLength = urlC.getContentLength();
		this.stream = urlC.getInputStream();
		if (this.stream == null)
		    throw new IOException("Got null input stream from url connection");
	    } catch (IOException ie) {
   	        throw new IOException("error in connection");
	    }
        }

        location = 0;
   	eofReached = false;
        this.url = url;
    }

    /**
     * Get the current content type for this stream.
     *
     * @returns  The current ContentDescriptor for this stream.
     */

    public ContentDescriptor getContentDescriptor() {
        return null;
    }

    /**
     * Find out if the end of the stream has been reached.
     *
     * @returns  true if there is no more data.
     */
    public boolean endOfStream() {
        return eofReached;
    }

    /**
     * willReadBlock indicates whether there data available now.
     *
     * @returns true if read would block, false if not.
     */
    public boolean willReadBlock() {
        try {
            return (stream.available() == 0);
        } catch (IOException e) {
            System.err.println("Exception PullSourceStream::willReadBlock " +
			       e.toString());
            return true;
        }
    }

    /**
     * read will perform a blocking read from stream.  If
     * buffer is null up to length bytes are read and discarded.
     *
     * @param buffer  buffer to read into
     * @param offset  offset in the buffer to put data
     * @param length  bytes to read
     * @returns bytes read or -1 for end of media
     * @exception     IOException
     */
    public int read(byte buffer[], int offset, int length) throws IOException {
        int bytesRead;
        int len = length;
        int off = offset;
        do {
	    bytesRead = stream.read(buffer, off, len);
            if (bytesRead == -1) {
		eofReached = true;
		int totalBytesRead = length - len;
		return (totalBytesRead > 0) ? totalBytesRead : -1;
            } else {
    	    	location += bytesRead;
                len -= bytesRead;
                off += bytesRead;
            }
        } while (len != 0);

        return length;

    }

    /**
     * Obtain the collection of objects that control the object that implements
     *	this interface. If no controls are supported, a zero length array
     *  is returned.
     *
     * @returns  the collection of object controls
     */
    // CURRENTLLY DOES NOTHING
    public Object[] getControls() {
        Object[] objects = new Object[0];

        return objects;
    }

    /**
     * Obtain the object that implements the specified Class or Interface The
     *	full class or interface name must be used. If the control is not
     *  supported then null is returned.
     *
     * @returns  the object that implements the control, or null.
     */
    // CURRENTLLY DOES NOTHING
    public Object getControl(String controlType) {
        return null;
    }


    /**
     * Seek to the specified point in the stream.
     *
     * @param where  the position to seek to
     * @returns      the new stream position
     */
    public long seek(long where) {
        long oldLocation = location;
	location = where;
       	try {
	    if (where < oldLocation) {
		reopenStream();
    	       	eofReached = false;
		return skip(stream, where);
            } else  {
		return skip(stream, (where - oldLocation));
	    }
        } catch (IOException e) {
// 	    System.err.println("Exception in PullSourceStream::seek(long) " +
// 			       e.toString());
            //System.exit(0);
            return 0; // dummy
        }
    }

    void reopenStream() {
	// reopen the stream and go to new location
	// System.out.println("$$$ bpss: seek back");
	try {
	    if (stream!=null) {
		stream.close();
	    }
	    // System.out.println("bpss: seek: needConnectPermission " +
	    //	   needConnectPermission);
	    if (needConnectPermission) {
		if ( /*securityPrivelege && */ (jmfSecurity != null) ) {
		    try {
			if (jmfSecurity.getName().startsWith("jmf-security")) {
			    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.CONNECT);
			    m[0].invoke(cl[0], args[0]);
			} else if (jmfSecurity.getName().startsWith("internet")) {
			    PolicyEngine.checkPermission(PermissionID.NETIO);
			    PolicyEngine.assertPermission(PermissionID.NETIO);
			}
		    } catch (Throwable e) {
			if (JMFSecurityManager.DEBUG) {
			    System.err.println("Unable to get connect " +
					       " privilege  " + e);
			}
			securityPrivelege = false;
			throw new IOException(JMFI18N.getResource("error.connectionerror") +
					      e.getMessage());
		    }
		}
	    }
	    
	    urlC = url.openConnection(); // This will not throw Security Exceptions
	    
	    try {
		if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
		    Constructor cons = jdk12ConnectionAction.cons;
		    stream = (InputStream) jdk12.doPrivM.invoke(
								jdk12.ac,
								new Object[] {
			cons.newInstance( new Object[] {urlC} )
			    }
								);
		    
		} else {
		    // stream=url.openStream();
		    stream = urlC.getInputStream();
		}
	    } catch (Exception e) {
		System.err.println("Unable to re-open a URL connection " + e);
		throw new IOException(JMFI18N.getResource("error.connectionerror") +
				      e.getMessage());
	    }
	} catch (IOException ex) {
	}
	
    }
    
    /**
     * Obtain the current point in the stream.
     *
     * @returns the current point in the stream
     */
    public long tell() {
        return location;
    }

    /**
     * Find out if this source can position anywhere in the stream.
     * If the stream is not random access, it can only be repositioned to the beginning.
     *
     * @returns returns true if the stream is random access, false if the
     * 			stream can only be reset to the beginning.
     */
    public boolean isRandomAccess() {
        return true;
    }

    /**
     * close the stream which PullSourceStream handles
     *
     * @exception  if an I/O error occurs
     */
    public void close()  {
	try {
	    stream.close();
	    stream = null;
        } catch (Exception e) {
	    System.out.println("BasicPullSourceStream close - IOException");
        }

    }

    public long getContentLength() {
	return contentLength;
    }


    private long skip(InputStream istream, long amount) throws IOException {
	long remaining = amount;
	while (remaining > 0) {
	    long actual = istream.skip(remaining);
	    remaining -= actual;
	}
	return amount;
    }
}
