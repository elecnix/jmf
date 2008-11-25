/*
 * @(#)DataChannel.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp;


/**
 * This is an interface that must be implemented by an object that
 * will provide a RTP data and control channel. e.g. the RTPSocket
 * object. The control channel of this object is an RTPPushDataSource and
 * can be accessed via this interface.
 *
 * @deprecated This inferface has been replaced with the RTPConnector interface.
 *
 * @see RTPPushDataSource
 * @see RTPSocket 
 */
public interface DataChannel {
    /**
     * Retrieves the control channel datasource of the object
     * implementing this interface.
     * @returns RTPPushDataSource the datasource corresponding to the
     * RTP Control Channel.
     */
    public RTPPushDataSource getControlChannel();
}
