/*
 * @(#)RTPConnector.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp;

import javax.media.protocol.PushSourceStream;
import java.io.IOException;


/**
 * A programmer may abstract the underlying transport mechanism for
 * RTP control and data from the RTPManager. This is done via the 
 * RTPConnector object.  An implementation of the RTPConnector must 
 * be created and handed over to RTPManager during initialization.  
 * The RTPManager will then use it to handle the sending and receiving
 * of the data and control packets.
 *
 * This replaces the deprecated RTPSocket interface.
 *
 * @see RTPManager
 */
public interface RTPConnector
{
    /**
     * Returns an input stream to receive the RTP data. 
     *
     * @return an input stream for reading data from the RTP data channel.
     * @exception IOException if an I/O error occurs when creating the input
     * stream.
     */
    public PushSourceStream getDataInputStream() throws IOException;

    /**
     * Returns an output stream to send the RTP data.
     * 
     * @return an output stream for writing data to the RTP data channel.
     * @exception IOException if an I/O error occurs when creating the output
     * stream.
     */
    public OutputDataStream getDataOutputStream() throws IOException;

    /**
     * Returns an input stream to receive the RTCP data.
     *
     * @return an input stream for reading data from the RTCP data channel.
     * @exception IOException if an I/O error occurs when creating the input
     * stream.
     */
    public PushSourceStream getControlInputStream() throws IOException;

    /**
     * Returns an output stream to send the RTCP data.
     * 
     * @return an output stream for writing data to the RTCP data channel.
     * @exception IOException if an I/O error occurs when creating the output
     * stream.
     */
    public OutputDataStream getControlOutputStream() throws IOException;

    /**
     * Close all the RTP, RTCP streams.
     */
    public void close();

    /**
     * Set the receive buffer size of the RTP data channel.
     * This is only a hint to the implementation.  The actual implementation
     * may not be able to do anything to this.
     *
     * @param size the size to which to set the receive buffer of the
     *  RTP data channel.
     */
    public void setReceiveBufferSize( int size) throws IOException;

    /**
     * Get the receive buffer size set on the RTP data channel.
     * Return -1 if the receive buffer size is not applicable for 
     * the implementation.
     *
     * @param size of the receive buffers.
     */
    public int getReceiveBufferSize();

    /**
     * Set the send buffer size of the RTP data channel.
     * This is only a hint to the implementation.  The actual implementation
     * may not be able to do anything to this.
     *
     * @param size the size to which to set the send buffer of the
     *  RTP data channel.
     */
    public void setSendBufferSize( int size) throws IOException;

    /**
     * Get the send buffer size set on the RTP data channel.
     * Return -1 if the send buffer size is not applicable for 
     * the implementation.
     *
     * @param size of the send buffers.
     */
    public int getSendBufferSize();

    /**
     * Return the RTCP bandwidth fraction.  This value is used to
     * initialize the RTPManager.  Check RTPManager for more detauls.
     * Return -1 to use the default values.
     *
     * @see RTPManager#initialize
     */
    public double getRTCPBandwidthFraction();

    /**
     * Return the RTCP sender bandwidth fraction.  This value is used to
     * initialize the RTPManager.  Check RTPManager for more detauls.
     * Return -1 to use the default values.
     *
     * @see RTPManager#initialize
     */
    public double getRTCPSenderBandwidthFraction();
}

