/*
 * @(#)StreamWriterControl.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

/**
 * This interface is implemented by a Multiplexer or DataSink to
 * enable controlling the number of bytes generated as output. For instance,
 * if a QuickTime Multiplexer is generating a QuickTime file that's being
 * written to disk, the application can use this control to restrict the size
 * of the output file.
 */
public interface StreamWriterControl extends javax.media.Control {

    /**
     * Sets a limit on the number of bytes that a stream writer can
     * generate. This value can be
     * ignored or treated as an approximate limit by the object
     * generating the stream.
     * @return true if the stream writer could succesfully set the
     * stream size limit, false if the feature is not supported or
     * the numOfBytes parameter is invalid.
     */
    public boolean setStreamSizeLimit(long numOfBytes);

    /**
     * Returns the number of bytes written to the stream so far. If
     * unknown, it returns -1.
     */
    public long getStreamSize();
}
