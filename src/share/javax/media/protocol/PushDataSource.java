/*
 * @(#)PushDataSource.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

/**
 * Abstracts a data source that manages <CODE>PushDataStreams</CODE>.
 *
 * @see javax.media.Manager
 * @see javax.media.Player
 * @see DataSource
 * @version 1.3, 02/08/21.
*/

public abstract class PushDataSource extends DataSource {

    /**
     * Get the collection of streams that this source
     * manages. The collection of streams is entirely
     * content dependent. The <code>ContentDescriptor</code>
     * of this <CODE>DataSource</CODE> provides the only indication of
     * what streams can be available on this connection.
     *
     * @return The collection of streams for this source.
     */
    public abstract PushSourceStream[] getStreams();

}
