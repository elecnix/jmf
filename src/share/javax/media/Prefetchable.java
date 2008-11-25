/*
 * @(#)Prefetchable.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * This interface would be implemented on a <code>Renderer</code> or
 * <code>Multiplexer</code> plugin if it has some
 * internal buffers that require prefetching to minimize startup
 * latency.
 */
public interface Prefetchable {

    /**
     * Returns the prefetch state of the <code>Renderer</code> or
     * <code>Multiplexer</code> plugin. If the plugin
     * implementing this interface has sufficient data to start
     * processing with minimum latency, then it returns true. If
     * it returns false, then the enclosing <code>Player</code> can call 
     * <code>process</code> on the plugin with some data. Once a 
     * plugin reaches its prefetched state, it remains in that state 
     * until a <code>reset</code> call.
     */
    public boolean isPrefetched();
}
