/*
 * @(#)RTPControl.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;

import java.lang.*;
import java.util.*;
import javax.media.Format;
import javax.media.Control;
import java.awt.Component;
/**
 * This interface is typically implemented by a RTP datasource. This
 * interface will be used to control access to various RTP information
 * via the datasource. e.g. dynamic payload-->EncodingName mapping can
 * be established using this control. Reception statistics and current
 * format being streamed on this datasource can also be queried. 
 */
public interface RTPControl extends Control{
    /**
     * This method is used to add a dynamic payload ---> Format
     * mapping in the RTPSessionManager. The RTPSessionManager maintains
     * all static payload numbers and their correspnding Formats as
     * mentioned in the Audio/Video profile document. Using the plugin
     * packethandler interface, a user may plugin his own packetizer or
     * depacketizer to handle RTP streams of a proprietary format using
     * dynamic payload numbers as specified in the AV profile. Before
     * streaming payload of a dynamic nature, a Format object needs to
     * be created for the dynamic payload and associated with a dynamic
     * payload number.
     */
    public  void addFormat(Format fmt,
			   int payload);
    /**
     * Query the RTP datasource for its data reception statistics
     */
    public ReceptionStats getReceptionStats();
    /**
     * Query the RTP datasource for global RTP statistics
     */
    public GlobalReceptionStats getGlobalStats();
    /**
     * Retrieves the Format object for the current stream. If this datasource
     * has not received data as yet, null will be returned. This method
     * provides a way for the application writer to query the datasource
     * for the format it is currently streaming, since payload formats
     * can be switched during the course of an RTP session. 
     */
    public Format getFormat();
    /**
     * Retrieves a complete list of all the formats set on this
     * datasource. This is a collection of Format objects that
     * were added by using addFormat() method.
     */
    public Format[] getFormatList();
    /**
     * Retrieves a format corresponding to the payload as stored by
     * the session manager.  
     */
    public Format getFormat(int payload);
}

