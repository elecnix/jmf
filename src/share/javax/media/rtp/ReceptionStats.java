/*
 * @(#)ReceptionStats.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;
/**
 * Interface ReceptionStats---
 * encapsulates data reception statistics as well as RTCP statistics
 * prepared by the RTPSM.A "PDU" is a a Protocol Data Unit and
 * represents a single RTP packet.
 */
public interface ReceptionStats
{

    /**
     * The difference between the number of packets expected as
     * determined by the RTP sequence number and the count of packets
     * actually received and validated.
     */
    public int
    getPDUlost();
    /**
     * The total number of valid packets received from the selected
     * source
     */
    public int
    getPDUProcessed();
    /**
     * The total number of data packets that came in out of order as
     * per the RTP sequence number
     */
    public int
    getPDUMisOrd();
    /**
     * The total number of RTP data packets that have failed to be
     * within an acceptable sequence number range for an established
     * SSRC Id.
     */
    public int
    getPDUInvalid();
    /**
     * The total number of RTP data packets that match the sequence
     * number of another already in the input queue.
     */
    public int
    getPDUDuplicate();

}
