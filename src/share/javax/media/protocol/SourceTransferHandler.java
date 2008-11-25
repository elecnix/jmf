/*
 *  @(#)SourceTransferHandler.java	1.3 02/08/21
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
 * Implements the callback from a <CODE>PushSourceStream</CODE>.
 * In JMF 2.0, if a transfer handler is contained in a DataSink, it
 * can implement the Seekable interface if it wishes to be seekable.
 * @see Seekable
 * @see javax.media.DataSink
 * @see PushSourceStream
 * @version 1.3, 02/08/21.
 */

public interface SourceTransferHandler {

    /**
     * Transfer new data from a <CODE>PushSourceStream</CODE>.
     *
     * @param stream The stream that is providing the data.
     */
     public void transferData(PushSourceStream stream);
}
