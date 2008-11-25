/*
 * @(#)NotConfiguredError.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>NotConfiguredError</code> is thrown when a method that
 * requires a <CODE>Processor</CODE> to be in the <I>Configured</I> state 
 * is called and the <CODE>Processor</CODE> has not yet been <i>Configured</i>.  
 * <p>
 * This typically happens when <code>getTrackControls</code> is invoked on an
 * <I>Unrealized</I>&nbsp;<code>Processor</code>.
 *
 * @see Processor
 * @since JMF 2.0
 */

public class NotConfiguredError extends MediaError {

    /**
     * Constructor.
     * 
     * @param reason  the reason the exception occured.
     */
    public NotConfiguredError(String reason) {
       super(reason);
    }
}
