/*
 * @(#)SenderReportEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp.event;

import javax.media.rtp.*;
import javax.media.rtp.rtcp.*;

/**
 * Informs the RTP listener that an RTCP sender report (SR) has been
 * received. The Participant sending the SR is available by
 * calling  getParticipant() on the senderReport  object.  The
 * RTPStream representing the stream being reported on is available
 * by calling  getStream() on the senderReport object.  <P>
 *
 */
public class SenderReportEvent extends RemoteEvent{
    /**
     * The RTCP sender report
     */
    private SenderReport report;
    public SenderReportEvent(SessionManager from,
				 SenderReport report){
	super(from);
	this.report = report;
    }
    /**
     * The RTCP sender report
     */
    public SenderReport getReport(){
	return report;
    }
}

