/*
 * @(#)RTPSource.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol;

import javax.media.*;


/**
 * This is a special tagging interface to specify whether a DataSource
 * is intended comes from the RTPSessionMgr which gives some additional info.
 */ 
public interface RTPSource {

    /**
     * Get the ssrc.
     */
    public int getSSRC();

    /**
     * Get the cname.
     */
    public String getCNAME();

    /**
     * Prebuffer the data.
     */
    public void prebuffer();

    /**
     * Flush the data buffers.
     */
    public void flush();

    /**
     * Set the buffer listener.
     */
    public void setBufferListener(BufferListener listener);
}
