/*
 * @(#)StatusMessage.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp;

public abstract class StatusMessage
{
    // status messages:
    public final static int INVALID_ADDRESS = 1;
    public final static int SERVER_DOWN     = 2;
    public final static int TIMEOUT         = 3;
    public final static int NOT_FOUND       = 4;
    public final static int PLAYING         = 5;
    public final static int PAUSING         = 6;
    public final static int END_REACHED     = 7;
    public final static int READY           = 8;
}

