/*
 * @(#)Seekable.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

/**
 * A media object will implement this interface
 * if it is capable of seeking to a particular position in the
 * stream. The most common classes that implement Seekable are SourceStreams in
 * DataSources and SourceTransferHandlers in DataSinks.
 * <p>
 * @see PullSourceStream
 * @see SourceTransferHandler
 * @see javax.media.DataSink
 * @version 1.11, 98/06/23.
 */

public interface Seekable {

    /**
     * Seek to the specified point in the stream.
     * @param where The position to seek to.
     * @return The new stream position.
     */
    long seek(long where);

    /**
     * Obtain the current point in the stream.
     */
    long tell();

   
    /**
     * Find out if this media object can position anywhere in the
     * stream. If the stream is not random access, it can only be repositioned
     * to the beginning.
     *
     * @return Returns <CODE>true</CODE> if the stream is random access, <CODE>false</CODE> if the stream can only
     * be reset to the beginning.
     */
    boolean isRandomAccess();

}
