/*
 * @(#)InputSourceStream.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

import java.io.InputStream;
import java.io.IOException;

/**
 * Build a source stream out of an input stream.
 *
 * @see DataSource
 * @see SourceStream
 * @see java.io.InputStream
 *
 * @version 1.3, 02/08/21.
 *
*/
public
class InputSourceStream implements PullSourceStream {

    protected InputStream stream;
    protected boolean eosReached;
    ContentDescriptor contentType;

    /**
     * Construct an <CODE>InputSourceStream</CODE> from an input stream.
     *
     * @param s The input stream to build the source stream from.
     * @param type The content-type of the source stream.
     */
    public InputSourceStream(InputStream s, ContentDescriptor type) {
	stream = s;
	eosReached = false;
	contentType = type;
    }

    /**
     * Get the content type for this stream.
     *
     * @return content descriptor for the stream.
     */
    public ContentDescriptor getContentDescriptor() {
	return contentType;
    }

    /**
     * Obtain the content length
     *
     * @return content length for this stream.
     */
     public long getContentLength() {
	 return SourceStream.LENGTH_UNKNOWN;
     }
    
    /**
     * Query if the next read will block.
     * 
     * @return true if a read will block.
     */
    public boolean willReadBlock() {
	if( eosReached == true) {
	    return true;
	} else {
	    try {
		return stream.available() == 0;
	    } catch (IOException e) {
		return true;
	    }
	}
    }

    /**
     * Read a buffer of data.
     *
     * @param buffer The buffer to read data into.
     * @param offset The offset into the buffer for reading.
     * @param length The number of bytes to read.
     * @return The number of bytes read or -1 indicating end-of-stream.
     */
    public int read(byte[] buffer, int offset, int length) throws IOException {
	int bytesRead = stream.read(buffer, offset, length);
	if( bytesRead == -1) {
	    eosReached = true;
	}
	return bytesRead;
    }

    /**
     * Turn the stream off.
     *
     * @exception IOException Thrown if there is a problem closing the stream.
    */
    public void close() throws IOException {
	stream.close();
    }

    /**
     * Return if the end of stream has been reached.
     * @return true if the end of the stream has been reached.
     */
    // $jdr: This is a bug. Need to figure out
    // the "correct" way to determine, before a read
    // is done, if we're at EOS.
    public boolean endOfStream() {
	return eosReached;
    }

    /**
     * Returns an zero length array because no controls
     * are supported.
     *
     * @return a zero length <code>Object</code> array.
     */
    public Object[] getControls() {
	return new Object[0];
    }

    /**
     * Returns <code>null</code> because no controls are implemented.
     *
     * @return <code>null</code>.
     */
    public Object getControl(String controlName) {
	return null;
    }
     
}
