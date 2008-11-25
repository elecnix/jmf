/*
 * @(#)GlobalReceptionStats.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;
/**
 * Interface GlobalReceptionStats---
 * encapsulates data reception statistics as well as RTCP statistics
 * prepared by the RTPSM.
 */
public interface GlobalReceptionStats{
    /**
     * The total number of RTP and RTCP packets received on the RTP
     * Session socket before any packet validation
     */
    public int
    getPacketsRecd();
    /**
     * The total number of bytes received on the RTP session socket
     * before any validation of packets.
     */
    public int
    getBytesRecd();
    /**
     * The total number of RTP data packets that failed the RTP header
     * validation check such as RTP version number or length consistency
     */
    public int
    getBadRTPkts();
    /**
     * The total number of local collisions as seen by the RTPSM
     */
    public int
    getLocalColls();
    /**
     * The total number of remote collisions as seen by the RTPPSM
     */
    public int
    getRemoteColls();
    /**
     * The total number of packets looped as seen by the RTPSM
     */
    public int
    getPacketsLooped();
    /**
     * The number of packets that failed to get transmitted. In the
     * current implementation, this implies RTCP packets that failed
     * transmission
     */
    public int
    getTransmitFailed();
    /**
     * The total number of RTCP packets received on the RTP Session
     * control socket before any header validation
     */
    public int
    getRTCPRecd();
    /**
     * The total number of Sender Reports received on the RTCP socket
     */
    public int
    getSRRecd();
    /**
     * The total number of RTCP packets that failed the RTCP header
     * validity check such as RTP version number or length consistency
     */
    public int
    getBadRTCPPkts();
    /**
     * The total number of individual RTCP packets types that were not
     * implemented or not recognized by the RTPSM.
     */
    public int
    getUnknownTypes();
    /**
     * The total number of invalid ReceiverReports received by the
     * RTPSM. Invalidity is due to length inconsistency
     */
    public int
    getMalformedRR();
     /**
     * The total number of invalid SDES packets received by the
     * RTPSM. Invalidity is due to length inconsistency
     */
    public int
    getMalformedSDES();
     /**
     * The total number of invalid BYE RTCP packets received by the
     * RTPSM. Invalidity is due to length inconsistency
     */
    public int
    getMalformedBye();
     /**
     * The total number of invalid Sender Reports received by the
     * RTPSM. Invalidity is due to length inconsistency
     */
    public int
    getMalformedSR();
}
    
