/*
 * @(#)SSRCInUseException.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;

import java.lang.Exception;
/**
 * Exception thrown from initSession(), startSession(),
 * setDestinationAddress() if any of the InetAddress passed as
 * parameters are not local host address in case of local interfaces,
 * or cannot be resolved in case of remote addresses. <P>
 */
public class SSRCInUseException extends SessionManagerException{
    public SSRCInUseException(){
	super();
    }
    public SSRCInUseException(String reason){
	super(reason);
    }
}
