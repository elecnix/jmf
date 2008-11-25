/*
 * @(#)GlobalTransmissionStats.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;
/**
 * Interface GlobalTransmissionStats---
 * encapsulates data transmission statistics as well as RTCP statistics
 * prepared by the RTPSM for all its sending streams.
 */
public interface GlobalTransmissionStats{
    /**
     * The total number of RTP packets transmitted on the RTP
     * Session socket.
     */
    public int
    getRTPSent();
    /**
     * The total number of bytes sent on the RTP session socket (RTP
     * data only) 
     */
    public int
    getBytesSent();
    /**
     * The total number of RTCP packets sent out by the RTPSM.
     */
    public int
    getRTCPSent();
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
     * The number of packets that failed to get transmitted for any
     * reason. This would include RTP and RTCP packets that failed to
     * get transmitted.
     */
    public int
    getTransmitFailed();
}
    
