/*
 * @(#)Feedback.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp.rtcp;
/**
 * Encapsulates the feedback statistics contained in a RTCP report. In
 * the RTP spec, these are called the "report blocks" of the
 * sender/receiver reports. 
 */
import java.lang.*;

public interface Feedback 
{
    /**
     * Returns the SSRC of the data source for which this feedback
     * applies.  (Actually a 32-bit unsigned quantity) <P>
     */
    public long 
    getSSRC();
    /**
     * Returns the 'fraction lost' field. (Actually an 8-bit unsigned quantity) <P>
     */
    public int
    getFractionLost();
    /**
     * Returns the 'cumulative number of packets lost' field. (Actually
     * a 24-bit unsigned quantity)
     * <P> 
     */
    public long
    getNumLost();
    /**
     * Returns the 'extended highest sequence number received' field.
     * (Actually a 32-bit unsigned quantity) <P>
     */
    public long 
    getXtndSeqNum();
    /**
     * Returns the 'interarrival jitter' field.  (Actually a 32-bit
     * unsigned quantity) <P>
     */
    public long
    getJitter();
    /**
     * Returns the 'last SR (LSR)' field. (Actually a 32-bit unsigned quantity) <P>
     */
    public long
    getLSR();
    /**      
     * Returns the 'delay since last SR (DLSR)' field. (Actually a
     * 32-bit unsigned quantity) <P>
     */
    public long
    getDLSR();
}
