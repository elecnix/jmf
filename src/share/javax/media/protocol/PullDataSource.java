/*
 * @(#)PullDataSource.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

/**
 * Abstracts a media data-source that  only supports
 * pull data-streams.
 *
 * @see javax.media.Manager
 * @see javax.media.Player
 * @see DataSource
 * @version 1.3, 02/08/21.
 */


public abstract class PullDataSource extends DataSource {

    /**
     * Get the collection of streams that this source
     * manages. The collection of streams is entirely
     * content dependent. The  MIME type of this
     * <CODE>DataSource</CODE> provides the only indication of
     * what streams can be available on this connection.
     *
     * @return The collection of streams for this source.
     */
    public abstract PullSourceStream[] getStreams();

}
