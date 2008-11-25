/*
 * @(#)NotRealizedError.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * <code>NotRealizedError</code> is thrown when a method that
 * requires a <code>Controller</code> to be in the <i>Realized</i> state is called 
 * and the <code>Controller</code> is not <i>Realized</i>.
 * <p>
 * 
 * For example, this can happen when
 * <code>getComponents</code> is called on an <i>Unrealized</i>&nbsp;
 * <code>Player</code>.
 *
 * @see Controller
 * @see Player
 * @version 1.2, 02/08/21.
 */

public class NotRealizedError extends MediaError {

    public NotRealizedError(String reason) {
       super(reason);
    }
}
