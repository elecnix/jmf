/*
 * @(#)Drainable.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * This interface would be implemented on a <code>Renderer</code> or
 * <code>Multiplexer</code> plugin if the plugin's internal buffered data 
 * can be drained.
 */
public interface Drainable {

    /**
     * Drains the queued data from a <code>Renderer</code>
     * or <code>Multiplexer</code> plugin by continuing the processing
     * until the internal buffer is empty.  The method blocks until
     * the draining is complete.<br>
     * If drain() is invoked on a stopped <code>Renderer</code> or
     * <code>Multiplexer</code> that has buffered data, the
     * method will block until the plugin is started and the data buffer
     * becomes empty.  If drain() is invoked by one thread, and another 
     * continues to fill the data queue, the operation will not complete.<br>
     * To abort a <code>drain</code> call, the <code>reset</code> and
     * <code>close</code> method on the plugin can be used. 
     */
    public void drain();
}
