/*
 * @(#)NoPlayerException.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
  * A <code>NoPlayerException</code>  is thrown when a <code>Manager</code> 
  * can't find a <code>Player</code> for a
  * particular <CODE>URL</CODE> or <CODE>MediaLocator</CODE>.
  *
  * @version 1.3, 02/08/21.
 */

public class NoPlayerException extends MediaException {

   public NoPlayerException() {
       super();
   }
    
    public NoPlayerException(String reason) {
	super(reason);
    }
}
