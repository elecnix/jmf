/*
 * @(#)ResourceUnavailableException.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * ResourceUnavailableException is thrown if a class couldn't could allocate
 * the required resources for an operation.
 *
 * @see PlugIn
 * @since JMF 2.0
 */

public class ResourceUnavailableException extends MediaException {

    /**
     * Constructor.
     * 
     * @param reason  the reason the exception occured.
     */
    public ResourceUnavailableException(String reason) {
       super(reason);
    }
}
