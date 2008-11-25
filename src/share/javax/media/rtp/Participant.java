/*
 * @(#)Participant.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp;
import java.util.*;

/**
 * This interface represents one participant in an RTP session.
 * Within a session,  a participant is defined to be the application
 * sending and  receiving data on that session.  Note  that this doesn't
 * necessarily  imply that participants are human. A single participant
 * may be the source of zero or more streams of RTP data packets, each
 * of which is represented by an RTPStream  object obtainable via the
 * getStreams().<P>
 *
 * RTPSM only creates instances of the two subclasses of this class,
 * RTPRemoteParticipant and RTPLocalParticipant.  Of all the
 * Participants  objects managed by the  RTPSessionManager, only one
 * is the local participant and thus an instance of
 * RTPLocalParticipant. <P>
 *
 * All the other Participants in the session (obtainable via
 * RTPSM.getRemoteParticipants()) are "remote" and are instances of
 * RTPRemoteParticipant; calls to getStreams() on  remote participants
 * return a Vector of ReceiveStream objects.  The client can determine
 * whether  an Participant is local doing a simple "instanceof" test. <P>
 *
 * Participants are identified by the CNAME string, which is
 * transmitted in every  RTCP packet.Since a participant may begin
 * sending data packets *before* the CNAME of the  participant has
 * arrived in an RTCP packet, it is possible that a data stream will
 * exist, for a  short time, as "unassociated" with any participant.
 * See ReceiveStream for more details.  <P>
 *
 * Note that this abstraction defines a participant *within* a
 * session; in real  life, "participants"
 * can be involved in more than one RTP session at a time. <P>
 *
 */
public interface Participant
{
    /**
     * Retrieves a Vector which is the set of all RTPStream objects for
     * this  participant.  Note that if the participant is not sending a
     * stream  of RTP packets (i.e. it is a  "passive" participant),this
     * method  will return an empty Vector. Note that if this call is
     * made  after  data has started coming in from a new SSRC from this
     * participant, but before an RTCP packet  has arrived to map that
     * SSRC to this  participant, that receive stream will not turn up
     * in this call; when and if said RTCP packet arrives and the orphan
     * stream can  be associated with an  Participant, the
     * recvStreamMapped callback will be issued to all RTPSessionListeners.<P>
     *
     * If this is the local participant, the returned Vector will
     * contain null. if it is a remote participant it
     * will contain ReceiveStream objects.    <P>
     *
     * @return the Vector of RTPStream objects. <P>
     */
    public Vector 
    getStreams();
    /**
     * Returns a Vector of RTCPReport objects, each representing the
     * most recent  RTCP report for an SSRC sent by this participant.  In
     * most cases,  this Vector will contain  exactly one report, be it an
     * SR or RR,  because most participants (passive receivers and
     * single-stream senders) only send out one SSRC.  The exceptions are
     * those  participants sending multiple  data streams, in which case
     * there will be one report per stream (i.e. per SSRC); these reports
     * would also  be accessible via the getReport() call on each
     * individual  RTPStream.    <P>
     */
    public Vector
    getReports();
    /**
     * Provides a quick method to get the CNAME of this participant.
     * This value  can also be obtained from the RTCPReport objects
     * returned through getReports() on this participant. <P>
     *
     * @return the CNAME identifying this participant.   <P>
     */
    public String 
    getCNAME();
    /** 
     * Returns a Vector of RTCPSourceDescription objects.  Each such
     * object is one  field from the most recent RTCP SDES packet for
     * this  participant. Since typically the most recent SDES packet
     * will not contain all of the RTCPSourceDescription objects
     * simultaneously, the returned Vector contains the most recent SDES type
     * received from this participant, possibly from many RTCP
     * reports. (Each of the returned SDES types therefore may not be
     * from the  latest RTCP report necessarliy). A convenient method
     * to get at the source description  without having to go through
     * calling  getReports(),  enumerating the Vector, and  then
     * calling getSourceDescription()  on the  RTCPReport.<P>
     * @return the Vector representing the latest SDES report from this
     * participant.  May be an empty Vector.    <P>
     */
    public Vector
    getSourceDescription();
    
}

