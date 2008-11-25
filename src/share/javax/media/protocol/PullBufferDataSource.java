/*
 * @(#)PullBufferDataSource.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

/**
 * Abstracts a media data-source that contains one or more
 * <code>PullBufferStreams</code> and delivers data as
 * <code>Buffer</code> objects.
 *
 * @see javax.media.Manager
 * @see javax.media.Player
 * @see javax.media.Buffer
 * @see DataSource
 * @see javax.media.protocol.PullBufferStream
 * @since JMF 2.0
 */


public abstract class PullBufferDataSource extends DataSource {

    /**
     * Get the collection of streams that this source
     * manages. The collection of streams is entirely
     * content dependent. The  MIME type of this
     * <CODE>DataSource</CODE> provides the only indication of
     * what streams can be available on this connection.
     * Each of the streams returned by this datasource is a PullBufferStream
     * @return The collection of streams for this source.
     */
    public abstract PullBufferStream[] getStreams();

}
