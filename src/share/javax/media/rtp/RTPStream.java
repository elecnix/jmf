/*
 * @(#)RTPStream.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp;

import java.lang.String;
import javax.media.rtp.rtcp.*;
import javax.media.protocol.DataSource;
import java.io.IOException;

/**
 * Interface RTPStream -- base interface representing a stream within
 * an RTP  session.  Objects do not implement RTPStream, but rather
 * ReceiveStream.   <P>
 */
public interface RTPStream 
{
    /**
     * Returns the Participant that "owns" this stream.  If this is
     * an  "orphaned" receive stream (see
     * RTPSessionListener.newReceiveStream), null is returned; the
     * recvStreamMapped() callback announces the fact that this call will
     * no longer return null for this  object. <P>
     */
    public Participant
    getParticipant();
    /**
     * Retrieves the latest RTCP sender report for this stream.  May be
     * null if  no RTCP sender report has been received (for
     * ReceiveStreams)<P>
     *
     * For receive streams, the returned report represents the latest
     * sender report  received from the remote source.  If the
     * associated remote participant contains multiple  ReceiveStreams,
     * the reports returned by invoking this method on each may not
     * necessarily contain  an RTCPFeedback array (obtainable via
     * getRTCPFeedbackReports()).  This is for the same reason  as noted above;
     * since the reports are coming from the same participant, they
     * would only differ in the sender portion and it would be redundant
     * to send the receiver portion in each.  <P>
     */
    public SenderReport
    getSenderReport();
    /**
     * Returns the SSRC of the stream.  <P>
     *
     * @return the stream's SSRC. <P>
     */
    public long 
    getSSRC();
    /**
     * Returns the datasource of the stream.
     * @returns the stream's datasource
     */
    public DataSource
    getDataSource();
     
    
}
