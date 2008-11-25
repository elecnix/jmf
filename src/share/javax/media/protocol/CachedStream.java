/*
 * @(#)CachedStream.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

/**
 * This interface is implemented by a <code>SourceStream</code> that caches
 * downloaded data for fast delivery to the handler. It provides control
 * over enabling/disabling the buffering and provides a way to query if
 * a certain number of bytes are available to be read from the stream.
 * <P>
 * Buffering here refers to jitter-buffering. Buffering is normally enabled.
 * It enables smooth playback even if the incoming data rate fluctuates.
 * The amount of buffering is implementation dependant.
 * <P>
 * When parsing the header of a media file, the parser
 * may want to read a few bytes to check for say a valid
 * magic header; if the magic header is illegal, the
 * parser can throw a BadHeaderException. The parser
 * may build several tables and extract media track
 * information.
 * <P>
 * The parser typically doen't need to wait for the buffers to be
 * filled. 
 * So it is desirable to turn off buffering when parsing the
 * header and turn it on after the header is read.
 * This won't have any effect on playback smoothness
 * since playback cannot start until the header is parsed.
 *
 * @see PullSourceStream
 * @see PushSourceStream
 * @since JMF 2.0
 */
public interface CachedStream {

    /**
     * This method is used by the parser to turn off buffering
     * while the header is being read. Buffering is on by default.
     */
    void setEnabledBuffering(boolean b);

    /**
     * Tells if buffering is currently enabled or not.
     */
    boolean getEnabledBuffering();
    
    /**
     * Will seek to offset and read numBytes block?
     * Will return true unless numBytes bytes can be
     * read in one <code>read</code> call.
     */
    boolean willReadBytesBlock(long offset, int numBytes);
    
    /**
     * Will read numBytes block?
     * Will return true unless numBytes bytes can be
     * read in one <code>read</code> call.
     */
    boolean willReadBytesBlock(int numBytes);

    /**
     * Aborts a read if it is in progress.
     */
    void abortRead();

}
