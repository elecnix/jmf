/*
 * @(#)SessionManagerException.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;

import java.lang.Exception;

/**
 * The RTPSM Exception class which must be subclassed by all exception
 * classes of the SessionManager <P>
 */
public class SessionManagerException extends Exception{
    public SessionManagerException(){
	super();
    }
    public SessionManagerException(String reason){
	super(reason);
    }
}


