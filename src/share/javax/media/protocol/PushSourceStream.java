/*
 * @(#)PushSourceStream.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.protocol;

/**
 * Abstracts a read interface that pushes data.
 *
 * 
 * @see PushDataSource
 * @version 1.3, 02/08/21.
 */
import java.io.IOException;

public interface PushSourceStream extends SourceStream {

    /**
     * Read from the stream without blocking.
     * Returns -1 when the end of the media
     * is reached.
     *
     * @param buffer The buffer to read bytes into.
     * @param offset The offset into the buffer at which to begin writing data.
     * @param length The number of bytes to read.
     * @return The number of bytes read or -1
     * when the end of stream is reached.
     * @throws IOException Thrown if an error occurs while reading
     *
     */
    public int read(byte[] buffer, int offset, int length) throws IOException;
    
    /**
     * Determine the size of the buffer needed for the data transfer.
     * This method is provided so that a transfer handler
     * can determine how much data, at a minimum, will be
     * available to transfer from the source.
     * Overflow and data loss is likely to occur if this much
     * data isn't read at transfer time.
     *
     * @return The size of the data transfer.
     */
    public int getMinimumTransferSize();

    /**
     * Register an object to service data transfers to this stream.
     * <p>
     * If a handler is already registered when
     * <CODE>setTransferHandler</CODE> is called,
     * the handler is replaced;
     * there can only be one handler at a time.
     * 
     * @param transferHandler The handler to transfer data to.
     */
    public void setTransferHandler(SourceTransferHandler transferHandler);

}
