/*
 * @(#)ReceiverReportEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp.event;

import javax.media.rtp.*;
import javax.media.rtp.rtcp.*;

/**
 * Informs the RTP listener that an RTCP receiver report (RR) has
 * been  received.  The Participant sending the RR is available
 * by calling getParticipant() on the  receiverReport object. <P> 
 */
public class ReceiverReportEvent extends RemoteEvent{
    /**
     * receiverReport the RTCP receiver report.
     */
    private ReceiverReport report;
    public ReceiverReportEvent(SessionManager from,
			      ReceiverReport report){
	super(from);
	this.report = report;
    }
    /**
     * receiverReport the RTCP receiver report.
     */
    public ReceiverReport getReport(){
	return report;
    }
}
