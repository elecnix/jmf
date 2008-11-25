/*
 * @(#)MediaException.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
  * A <code>MediaException</code> indicates an unexpected error
  * condition in a JMF method.
  *
  * @version 1.13, 98/06/23
 */

public class MediaException extends Exception {

    public MediaException() {
	super();
    }
    
    public MediaException(String reason) {
	super(reason);
    }
}
