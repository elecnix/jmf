/*
 * @(#)BufferListener.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol;

import javax.media.*;


/**
 * This interface allows a DataSource to notify its listener on
 * the status of the data flow in the buffers.
 */ 
public interface BufferListener {

    /*
    public void overFlown(javax.media.protocol.DataSource ds);

    public void underFlown(javax.media.protocol.DataSource ds);
    */

    public void minThresholdReached(javax.media.protocol.DataSource ds);
}
