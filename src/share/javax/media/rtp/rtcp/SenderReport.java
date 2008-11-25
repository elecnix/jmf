/*
 * @(#)SenderReport.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp.rtcp;

import javax.media.rtp.*;

/**
 * Contains the methods specific to an RTCP sender report (SR).
 */
public interface SenderReport extends Report {
    /**
     * Returns the RTPStream associated with this sender report.This method may
     * return NULL if this SR was received before RTP data packets have
     * arrived and an ReceiveStream could be created. <P>
     * Note that RTCPReceiverReports don't expose this method.  This is
     * because RR's  come from passive listeners that are represented by
     * an Participant with no ReceiveStreams.  <P>
     */
    public RTPStream
    getStream();
    /**
     * Returns the sender's packet count.  <P>
     */ 
    public long
    getSenderPacketCount();
    /** 
     * Returns the sender's byte count. <P>
     */
    public long
    getSenderByteCount();
    /**
     * Returns the most significant word of the NTP timestamp.  This is
     * returned as  a long because it's a 32-bit *unsigned* quantity,
     * the full range of which is only  representable by a long.   <P>
     */
    public long
    getNTPTimeStampMSW();
    /**
     * Returns the least significant word of the NTP timestamp.  This is
     * returned as  a long because it's a 32-bit *unsigned* quantity,
     * the full range of which is only  representable by a long
     */
    public long
    getNTPTimeStampLSW();
    /**
     * Returns the RTP Timestamp.  This is returned as a long because
     * it's a 32-bit  *unsigned* quantity, the full range of which is
     * only  representable by a long.    <P>
     */
    public long
    getRTPTimeStamp();
    /**
     * Returns a Feedback object corresponding to the feedback for
     * this sender  which the *local* participant is sending out in RTCP
     * reports. This allows an application to  quickly obtain the
     * reception statistics we are generating for this source.  Note
     * that this  method does not directly relate to the bits received
     * in  RTCP sender reports. This Feedback object is also
     * available via the local participant (but not as easily) because
     * it is  part of the RTCP reports it is sending out.  By calling
     * getReports() on the local  participant we could retrieve an
     * RTCPReport containing this same Feedback. <P>
     */
    public Feedback
    getSenderFeedback();
    
}
