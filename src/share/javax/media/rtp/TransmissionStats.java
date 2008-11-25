/*
 * @(#)TransmissionStats.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;

/**
 * Interface TransmissionStats---
 * encapsulates data transmission statistics as well as RTCP statistics
 * prepared by the RTPSM.A "PDU" is a a Protocol Data Unit and
 * represents a single RTP packet. 
 */
public interface TransmissionStats
{
    /**
     * Get the total number of PDU sent out by this source. This will
     * be the total numer of RTP data packets sent out by this source.  
     */
    public int
    getPDUTransmitted();

    /**
     * Get the total number of bytes or octets of data transmitted by
     * this source.
     */
    public int
    getBytesTransmitted();
    /**
     * Get the total number of RTCP packets sent out by this source
     * i.e total number of Sender Reports sent  out by this source.
     */
    public int
    getRTCPSent();
}
