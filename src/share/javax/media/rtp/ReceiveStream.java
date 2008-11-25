/*
 * @(#)ReceiveStream.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;
/**
 * Interface RTPStream -- interface representing a receiving
 * stream within  an RTP session. ReceiveStreams are created by the RTP
 * subsystem,  as necessary, for each  independent stream of data
 * arriving from  remote participants on a session.The presence of new
 * ReceiveStreams is announced with the newReceiveStream() callback in
 * RTPReceiveStreamListener.  <P>
 */

public interface ReceiveStream 
extends RTPStream
{
    /**
     * Returns the reception statistics as computed by the RTPSM for
     * this ReceiveStream. i.e. all the reported statistics are for
     * data packets arriving only from the source of this ReceiveStream
     * i.e. the source with ssrcid of this stream source.  
     * @return RTPReceptionstats for data from this source.
     * Global reception stats are available from method
     * getGlobalReceptionStats of RTPSessionManager interface  
     * @Classname   RTPSessionManager
     */
    public ReceptionStats
    getSourceReceptionStats();
    
}
