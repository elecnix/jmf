/*
 * @(#)PacketSizeControl.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

/**
 * This interface is a Control for specifying the parameters for packet size.
 * This control is useful for specifying the MTU of RTP channel.
 * @since JMF 2.0
 */
public interface PacketSizeControl extends javax.media.Control {

    /**
     * Sets the desired maximum data size on the data that is output by this
     * encoder. This parameter is to be used as a means to convey the
     * preferred size of individual data units (packets) that are output
     * by this encoder. Returns the actual packet size that was set.
     * @param numBytes The number of bytes the maximum packet size
     * is set to
     * @return the actual packet size in bytes set by the encoder
     */
    public int setPacketSize(int numBytes);

    /**
     * Retrieve the maximum packet size used by this encoder.
     * @return Maximum packet size in bytes used by this encoder.
     */
    public int getPacketSize();

}

