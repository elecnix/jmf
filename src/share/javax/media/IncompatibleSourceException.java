/*
 * @(#)IncompatibleSourceException.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * An <CODE>IncompatibleSourceException</CODE> is thrown
 * by a <CODE>MediaHandler</CODE> when <code>setSource</code>
 * is invoked and the <code>MediaHandler</code> cannot
 * support the <code>DataSource</code>.
 * <p>
 **/

public class IncompatibleSourceException extends MediaException {

    public IncompatibleSourceException() {
	super();
    }
    
    public IncompatibleSourceException(String reason) {
	super(reason);
    }
}
