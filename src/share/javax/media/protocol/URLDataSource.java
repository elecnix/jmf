/*
 * @(#)URLDataSource.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

import java.io.*;
import java.net.*;
import javax.media.MediaLocator;
import javax.media.Duration;
import javax.media.Time;

/**
 * A default data-source created directly from
 * a <code>URL</code> using <code>URLConnection</code>.
 **/
public class URLDataSource extends PullDataSource {

    protected URLConnection conn;
    protected ContentDescriptor contentType;
    protected URLSourceStream[] sources;
    protected boolean connected;

    /**
     * Implemented by subclasses.
     */
    protected URLDataSource() {
    }
    
    /**
     * Construct a <CODE>URLDataSource</CODE>
     * directly from a <code>URL</code>.
     */
    public URLDataSource(URL url) throws IOException {
	setLocator(new MediaLocator(url));
	connected = false;
    }

    /*
     * Get the single-element array for this source.
     *
     * @return The single-element array of source stream.
     */
    public PullSourceStream[] getStreams() {
	// $jdr: Is this necessary? See getContentType().
	if( !connected) {
	    throw new java.lang.Error("Unconnected source.");
	}
	return sources;
    }

    /**
     * Initialize the connection with the source.
     *
     * @exception IOException Thrown if there are problems setting
     * up the connection.
     */
    public void connect() throws IOException {
	
	// Make the connect.
	conn = getLocator().getURL().openConnection();
	conn.connect();
	connected = true;

	// Figure out the content type.
	String mimeType = conn.getContentType();
	if( mimeType == null) {
	    mimeType = ContentDescriptor.CONTENT_UNKNOWN;
	}
	contentType = new ContentDescriptor(
			   ContentDescriptor.mimeTypeToPackageName(mimeType));

	// Create a source stream.
	sources = new URLSourceStream[1];
	sources[0] = new URLSourceStream(conn, contentType);
	
    }

    /**
     * Return the content type name.
     * 
     * @return The content type name.
     */
    public String getContentType() {
       // $jdr: We could probably get away with
       // not doing anything here, and connecting on
       // creation, given that this protocol is pretty
       // "connection-less".
       if( !connected) {
	   throw new java.lang.Error("Source is unconnected.");
       }
       return contentType.getContentType();
    }

    /**
     * Disconnect the source.
     */
    public void disconnect() {
	if(connected) {
	    try {
		sources[0].close();
	    } catch(IOException e) {
		// There really isn't anything we can do
		// if close throws an exception.
		// so we'll eat it.
	    }
	    connected = false;
	}
    }

    public void start() throws IOException {
       // Nothing to do here either.
    }

    /**
     * Stops the 
     *
     */
    public void stop() throws IOException {
       // sure.
    }

    /**
     * Returns <code>Duration.DURATION_UNKNOWN</code>.
     * The duration is not available from an <code>InputStream</code>.
     *
     * @return <code>Duration.DURATION_UNKNOWN</code>.
     */ 
    public Time getDuration() {
	return Duration.DURATION_UNKNOWN;
    }

    /**
     * Returns an empty array, because this source
     * doesn't provide any controls.
     *
     * @return empty <code>Object</code> array.
     */
    public Object[] getControls() {
	return new Object[0];
    }

    /**
     * Returns null, because this source doesn't provide
     * any controls.
     */
    public Object getControl(String controlName) {
	return null;
    }


    /**
     * Create a PullSourceStream from a URLConnection.
     * 
     **/
    class URLSourceStream extends InputSourceStream {
	
	protected URLConnection conn;
	
	public URLSourceStream(URLConnection conn, ContentDescriptor type)
	    throws IOException {
	    /* Initialize with the stream. */
	    super(conn.getInputStream(), type);
	    this.conn = conn;
	}
	
	/**
	 * Obtain the number of bytes available on this stream.
	 *
	 * @return the content length for this stream.
	 */
	public long getContentLength() {
	    /* See if we actually know the content length */
	    long len = conn.getContentLength();
	    len =  (len == -1) ? SourceStream.LENGTH_UNKNOWN : len;
	    
	    return len;
	}
    }

}
