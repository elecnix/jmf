/*
 * @(#)BasicPullParser.java	1.20 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.parser;

import java.io.IOException;
import javax.media.Demultiplexer;
import javax.media.IncompatibleSourceException;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushDataSource;
import javax.media.protocol.PullDataSource;
import javax.media.protocol.SourceStream;
import javax.media.protocol.PullSourceStream;
import javax.media.protocol.Positionable;
import javax.media.protocol.Seekable;
import javax.media.protocol.CachedStream;
import javax.media.protocol.ContentDescriptor;
import javax.media.Format;
import com.sun.media.BasicPlugIn;

public abstract class BasicPullParser extends BasicPlugIn implements Demultiplexer {

    protected DataSource source;
    protected SourceStream[] streams;
    private Format[] outputFormats;
    private byte[] b = new byte[1];
    private byte[] intArray = new byte[4];
    private byte[] shortArray = new byte[2];
    private final int TEMP_BUFFER_LENGTH = 2048;
    private byte[] tempBuffer = new byte[TEMP_BUFFER_LENGTH];
    private long currentLocation = 0;
    protected boolean seekable = false;
    protected boolean positionable = false;
    protected CachedStream cacheStream;
    private Object sync = new Object(); // synchronizing variable


    public void setSource(DataSource source)
	throws IOException, IncompatibleSourceException {

	if (!(source instanceof PullDataSource)) {
	    throw new IncompatibleSourceException("DataSource not supported: " + source);
	} else {
	    streams = ((PullDataSource) source).getStreams();
	}


	if ( streams == null) {
	    throw new IOException("Got a null stream from the DataSource");
	}

	if (streams.length == 0) {
	    throw new IOException("Got a empty stream array from the DataSource");
	}

	this.source = source;
	this.streams = streams;
	
	positionable =  (streams[0] instanceof Seekable);
	seekable =  positionable && ((Seekable) streams[0]).isRandomAccess();

	if (!supports(streams))
	    throw new IncompatibleSourceException("DataSource not supported: " + source);

	try {
	    cacheStream = (CachedStream) streams[0];
	} catch (ClassCastException e) {
	    // System.out.println("bpparser: cacheStream is null");
	    cacheStream = null;
	}
    }


    /**
     * A parser may support pull only or push only or both
     * pull and push streams.
     * Some parsers may have other requirements.
     * A quicktime parser imposes an additional requirement that
     * isSeekable() and isRandomAccess() be true
     *
     * Override this if the Parser has additional requirements
     * from the PullSourceStream
     */
    protected boolean supports(SourceStream[] streams) {
	return ( (streams[0] != null) &&
		 (streams[0] instanceof PullSourceStream) );
	    
    }


    public boolean isPositionable() {
	return positionable;
    }

    // multiple streams??
    //     public boolean isSeekable() {
    // 	return (streams[0] instanceof Seekable);
    //     }

    public boolean isRandomAccess() {
	return seekable;
    }


    /**
     * Read numBytes from offset 0
     */
    public int readBytes(PullSourceStream pss, byte[] array,
			      int numBytes) throws IOException {

	return readBytes(pss, array, 0, numBytes);
    }

    // TODO: when working on the quicktime parser, you can
    // decide whether to remove the PullSourceStream arg. from
    // the read and skip methods. Use streams[0]
    public int readBytes(PullSourceStream pss, byte[] array,
			 int offset,
			 int numBytes) throws IOException {
	//TODO	synchronized(array) {

	// Is this if check too much overhead?
	// Can we rely on parsers to call this method with valid args?
	if (array == null) {
	    throw new NullPointerException();
	} else if ((offset < 0) || (offset > array.length) || (numBytes < 0) ||
		   ((offset + numBytes) > array.length) || ((offset + numBytes) < 0)) {
	    throw new IndexOutOfBoundsException();
	} else if (numBytes == 0) {
	    return 0;
	}

	int remainingLength = numBytes;
	int actualRead = 0;

	remainingLength = numBytes;
	while (remainingLength > 0) {

		actualRead = pss.read(array, offset, remainingLength);
		if (actualRead == -1) {// End of stream
		    if (offset == 0) {
			// Note: Using this as we don't have EndOfMediaException
			throw new IOException("BasicPullParser: readBytes(): Reached end of stream while trying to read " + numBytes + " bytes");
		    } else {
			// System.out.println("readBytes: ASKED for " + numBytes +
			// " GOT " + offset +
			// "NEXT read will be EOM");
			return offset;
		    }
		} else if (actualRead == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
		    return com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD;
		} else if (actualRead < 0) {
		    throw new IOException("BasicPullParser: readBytes() read returned " + actualRead);
		}
		remainingLength -= actualRead;
		// System.out.println("  remainingLength is " + remainingLength);
		offset += actualRead;
		synchronized(sync) {
		    currentLocation += actualRead;
		}
	}
	return numBytes;
	// System.out.println("Finished reading " + numBytes);
	//TODO	}
    }
    
    
    public /*protected*/ int readInt(PullSourceStream pss) throws IOException {
	return readInt(pss, true);
    }


    public /*protected*/ int readShort(PullSourceStream pss) throws IOException {
	return readShort(pss, true);
    }



    public int readByte(PullSourceStream pss) throws IOException {
	readBytes(pss, b, 1);
	return (int) b[0];
    }

    protected int readInt(PullSourceStream pss,
			  boolean isBigEndian) throws IOException {
	int result;

	readBytes(pss, intArray, 4);
	if (isBigEndian) {
	    result = ((intArray[0] & 0xFF) << 24) | 
		     ((intArray[1] & 0xFF) << 16) | 
		     ((intArray[2] & 0xFF) << 8) |
		     (intArray[3] & 0xFF);
	} else {
	    result = ((intArray[3] & 0xFF) << 24) |
		     ((intArray[2] & 0xFF) << 16) |
		     ((intArray[1] & 0xFF) << 8) |
		     (intArray[0] & 0xFF);
	}
	return result;
    }


    protected int parseIntFromArray(byte[] array, int offset,
			  boolean isBigEndian) throws IOException {
	int result;

	if (isBigEndian) {
	    result = ((array[offset+0] & 0xFF) << 24) | 
		     ((array[offset+1] & 0xFF) << 16) | 
		     ((array[offset+2] & 0xFF) << 8) |
		     (array[offset+3] & 0xFF);
	} else {
	    result = ((array[offset+3] & 0xFF) << 24) |
		     ((array[offset+2] & 0xFF) << 16) |
		     ((array[offset+1] & 0xFF) << 8) |
		     (array[offset+0] & 0xFF);
	}
	return result;
    }


    // Note: can use parseShortFromArray method except that
    // there is the overhead of the if call
    protected short readShort(PullSourceStream pss,
			  boolean isBigEndian) throws IOException {
	int result;

	readBytes(pss, shortArray, 2);
	if (isBigEndian) {
	    result = ((shortArray[0] &0xFF) << 8) |
		     (shortArray[1] &0xFF);
	} else {
	    result = ((shortArray[1] &0xFF) << 8) |
		     (shortArray[0] & 0xFF);
	}
	return (short) result;
    }


    public final static  short parseShortFromArray(byte[] array, boolean isBigEndian)
	throws IOException {
	if (array.length < 2)
	    throw new IOException("Unexpected EOF");

	int result;

	if (isBigEndian) {
	    result = ((array[0] &0xFF) << 8) |
		     (array[1] &0xFF);
	} else {
	    result = ((array[1] &0xFF) << 8) |
		     (array[0] & 0xFF);
	}
	return (short) result;

    }

    protected String readString(PullSourceStream pss) throws IOException {
	readBytes(pss, intArray, 4);
	return new String(intArray);
    }


    // TODO: REPEAT: when working on the quicktime parser, you can
    // decide whether to remove the PullSourceStream arg. from
    // the read and skip and getLocation methods. Use streams[0]

    // TODO: use the boolean seekable
    public /*protected*/ void skip(PullSourceStream pss, int numBytes) throws IOException {

	// System.out.println("skip : " + numBytes);
	// System.out.println("seekable is " + (pss instanceof Seekable));
	if ( (pss instanceof Seekable) && ((Seekable)pss).isRandomAccess() ) {
	    long current = ((Seekable)pss).tell();
	    long newPos = current + numBytes;
	    // System.out.println("bpp:skip " + numBytes + " : " + current + " : " + newPos);
	    ((Seekable)pss).seek( newPos );
	    if (newPos != ((Seekable)pss).tell()) {
		// System.out.println("DEBUG: seek to " + newPos + " failed");
		// Is this the correct thing to do?
		throw new IOException("Seek to " + newPos + " failed");
	    }
            synchronized(sync) {
                currentLocation += numBytes;
            }
	    return;
	}
	// NOTE: readBytes with null as second arg. should do the job.
	// But there is a bug in the PullSourceStream implementation
	// readBytes(pss, null, numBytes);

	int remaining = numBytes;
	int bytesRead;
	while (remaining > TEMP_BUFFER_LENGTH) {
	    // System.out.println("Calling readBytes " + TEMP_BUFFER_LENGTH);
	    bytesRead = readBytes(pss, tempBuffer, TEMP_BUFFER_LENGTH);
	    if (bytesRead != TEMP_BUFFER_LENGTH) {
		throw new IOException("BasicPullParser: End of Media reached while trying to skip " + numBytes);
	    }
	    remaining -= TEMP_BUFFER_LENGTH;
	}
	if (remaining > 0) {
	    // System.out.println("Calling readBytes " + remaining);
	    bytesRead = readBytes(pss, tempBuffer, remaining);
	    if (bytesRead != remaining) {
		throw new IOException("BasicPullParser: End of Media reached while trying to skip " + numBytes);
	    }
	}
	synchronized(sync) {
	    currentLocation += numBytes;
	}
    }

    // TODO: use the boolean seekable
    public /*protected*/ final long getLocation(PullSourceStream pss) {
	synchronized(sync) {
	    if ( (pss instanceof Seekable) )
		return ((Seekable)pss).tell();
	    else
		return currentLocation;
	}
    }


    /**
     * Lists the possible input formats supported by this plug-in.
     */
    public abstract ContentDescriptor [] getSupportedInputContentDescriptors();


    /**
     * Opens the plug-in software or hardware component and acquires
     * necessary resources. If all the needed resources could not be
     * acquired, it throws a ResourceUnavailableException. Data should not
     * be passed into the plug-in without first calling this method.
     */
    public void open() {
	// throws ResourceUnavailableException;
    }

    /**
     * Closes the plug-in component and releases resources. No more data
     * will be accepted by the plug-in after a call to this method. The
     * plug-in can be reinstated after being closed by calling
     * <code>open</code>.
     */
    public void close() {
	if (source != null) {
	    try {
		source.stop();
		source.disconnect();
	    } catch (IOException e) {
		// Internal error?
	    }
	    source = null;
	}
    }

    /**
     * This get called when the player/processor is started.
     */
    public void start() throws IOException {
 	if (source != null)
 	    source.start();
    }

    /**
     * This get called when the player/processor is stopped.
     */
    public void stop() {
	if (source != null) {
	    try {
		source.stop();
	    } catch (IOException e) {
		// Internal errors?
	    }
	}
    }

    /**
     * Resets the state of the plug-in. Typically at end of media or when media
     * is repositioned.
     */
    public void reset() {
    }

}
