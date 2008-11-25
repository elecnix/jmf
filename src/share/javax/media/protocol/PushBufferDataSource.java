/*
 *@(#)PushBufferDataSource.java	1.3 02/08/21 
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package javax.media.protocol;

/**
 * Abstracts a data source that manages data in the form of push
 * streams. The streams from this datasource contain Buffer objects
 * i.e. PushBufferStreams.  <BR>
 * 
 * @see javax.media.Manager
 * @see javax.media.Player
 * @see DataSource
 * @see javax.media.Buffer
 * @see javax.media.protocol.PushBufferStream
 * 
*/

public abstract class PushBufferDataSource extends DataSource {

    /**
     * Get the collection of streams that this source
     * manages. The collection of streams is entirely
     * content dependent. The <code>ContentDescriptor</code>
     * of this <CODE>DataSource</CODE> provides the only indication of
     * what streams can be available on this connection.
     *
     * @return The collection of streams for this source.
     */
    public abstract PushBufferStream[] getStreams();

}
