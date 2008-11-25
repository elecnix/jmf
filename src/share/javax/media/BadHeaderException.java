/*
 * @(#)BadHeaderException.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <CODE>BadHeaderException</CODE> is thrown
 * by a <CODE>Demultiplexer</CODE> when <code>getTracks</code>
 * is invoked and the header information is incomplete
 * or inconsistent.
 * <p>
 */
public class BadHeaderException extends MediaException {

    /**
     * Creates a simple exception object.
     */
    public BadHeaderException() {
	super();
    }

    /**
     * Creates an exception object with a specific reason.
     * @param reason a user readable string that describes the reason for the exception.
     */
    public BadHeaderException(String reason) {
	super(reason);
    }
}
