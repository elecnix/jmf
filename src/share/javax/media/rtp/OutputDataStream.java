/*
 * @(#)OutputDataStream.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp;

import javax.media.protocol.*;


/**
 * The interface which is opposite in functionality to a
 * PushSourceStream. A PushSourceStream in JMF will be the originator of
 * data which can be copied or read using the read() method of this
 * interface. The OutputDataStream is an interface via which data can be
 * written on a stream using the write() method. In RTP, the
 * OutputDataStream is used in the RTPPushDataSource. This is a two way
 * data source and the outgoing (to the network) channel is a
 * OutputDataStream. Data written on the OutputDataStream of a
 * RTPPushDataSource will be transmitted over the underlying network
 * protocol.
 *
 */
public interface OutputDataStream{
    /**
     * Write data to the underlying network. Data is copied from the
     * buffer starting af offset. Number of bytes copied is length
     * @param buffer The buffer from which data is to be sent out on
     * the network.
     * @param offset The offset at which data from buffer is copied
     * over
     * @param length The number of bytes of data copied over to the network.
     */     
    public abstract int write(byte[] buffer,
			      int offset,
			      int length);
}
