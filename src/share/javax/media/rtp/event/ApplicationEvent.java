/*
 * @(#)ApplicationEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp.event;

import javax.media.rtp.*;

/**
 * Event which informs the RTP listener that an RTCP 'APP' packet has
 * been received.   <P>
 *
 */
public class ApplicationEvent extends ReceiveStreamEvent{
    /**
     *  appSubtype The "subtype" value found in the APP packet
     * header. 
     */
    private int appSubtype;
    /**
     * The 4-character string found in the APP packet.
     * May be empty  but not null.
     */
    private String appString;
    /**
     * The application-specific data. May be a
     * zero-length  array but not null.  
     */
    private byte[] appData;
    
    public  ApplicationEvent(SessionManager from,
		     Participant participant,
		     ReceiveStream recvStream,
		     int appSubtype,
		     String appString,
		     byte appData[]){
	super(from, recvStream, participant);
	this.appSubtype = appSubtype;
	this.appString = appString;
	this.appData = appData;
    }
    /**
     *  appSubtype The "subtype" value found in the APP packet
     * header.
     */
    public int getAppSubType(){
	return appSubtype;
    }
    /**
     * The 4-character string found in the APP packet.
     * May be empty  but not null.
     */
    public String getAppString(){
	return appString;
    }
    /**
     * The application-specific data. May be a
     * zero-length  array but not null.
     */
    public byte[] getAppData(){
	return appData;
    }
}
    
	
