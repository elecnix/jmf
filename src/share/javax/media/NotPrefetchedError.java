/*
 * @(#)NotPrefetchedError.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * <code>NotPrefetchedError</code> is thrown when a method that
 * requires a <CODE>Controller</CODE> to be in the <I>Prefetched</I> state is called 
 * and the <CODE>Controller</CODE> has not been <i>Prefetched</i>.  
 * <p>
 * This typically happens
 * when <code>syncStart</code> is invoked on a <I>Stopped</I>&nbsp;<code>Controller</code>
 * that hasn't been <I>Prefetched</I>.
 *
 * @see Controller
 * @version 1.2, 02/08/21.
 */

public class NotPrefetchedError extends MediaError {

    public NotPrefetchedError(String reason) {
       super(reason);
    }
}
