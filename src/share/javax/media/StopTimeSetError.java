/*
 * @(#)StopTimeSetError.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * <CODE>StopTimeSetError</CODE> is thrown when the stop time 
 * has been set on a <I>Started</I>&nbsp;<CODE>Clock</CODE> and <code>setStopTime</code> is invoked
 * again.
 * 
 * @version 1.2, 02/08/21.
 */

public class StopTimeSetError extends MediaError {

    public StopTimeSetError(String reason) {
       super(reason);
    }
}
