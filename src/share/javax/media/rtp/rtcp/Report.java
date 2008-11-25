/*
 * @(#)Report.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp.rtcp;

import java.util.*;
import javax.media.rtp.*;

/**
 * Encapsulates a generic RTCP report; this is the parent interface
 * for the SenderReport and ReceiverReport interfaces. <P>
 */
public interface Report 
{
    /**
     * Returns the Participant that sent this Report.  May be the
     * local  participant, in which case this report was generated locally. <P>
     */
    public Participant
    getParticipant();
    /**
     * Gets the SSRC from which this report originated.   <P>
     */
    public long 
    getSSRC();
    /**
     * Returns a Vector of Feedback objects.  These are the report
     * blocks that  come in both sender and receiver reports.  Note that
     * although a remote participant may report on  all sources it is
     * receiving, this call is only obligated to return feedback for
     * sources  originating from the local participant in this session.
     * Thus, if the local client is not sending  any streams in this
     * session, or if the SSRC sending this report isn't providing
     * feedback on  the local stream, this call would return a empty vector. <P>
     */
    public Vector
    getFeedbackReports();
    /**
     * Returns a Vector of SourceDescription objects.  Each such
     * object is one  field from the SDES packet type of this Report
     */
    public Vector
    getSourceDescription();
    
}
