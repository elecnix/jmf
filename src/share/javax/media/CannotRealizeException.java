/*
 * @(#)CannotRealizeException.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>CannotRealizeException</code> is thrown when <code>Manager</code> 
 * cannot realize a <code>Player</code> or <code>Processor</code>
 * via the <code>createRealizedPlayer</code> and 
 * <code>createRealizedProcessor</code> calls.
 *
 * @version 2.0, 98/05/18.
 * @since JMF 2.0
 */

public class CannotRealizeException extends MediaException {

   public CannotRealizeException() {
       super();
   }
    
    public CannotRealizeException(String reason) {
	super(reason);
    }
}
