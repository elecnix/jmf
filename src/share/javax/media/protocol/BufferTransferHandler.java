/*
 * @(#)BufferTransferHandler.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

/**
 * Implements the callback from a <CODE>PushBufferStream</CODE>.
 * A <code>PushBufferStream</code> needs to notify the data handler
 * when data is available to be pushed. When the handler connects to
 * a <code>PushBufferStream</code> it should register a BufferTransferHandler
 * object with the stream using the <code>setTransferHandler</code> method.
 *
 * @see PushBufferStream#setTransferHandler
 * @since JMF 2.0
 */

public interface BufferTransferHandler {

    /**
     * Notification from the <code>PushBufferStream</code> to the
     * handler that data is available to be read from <code>stream</code>.
     * The data can be read by this handler in the same thread or can be
     * read later.
     *
     * @param stream The stream that is providing the data.
     */
     public void transferData(PushBufferStream stream);
}
