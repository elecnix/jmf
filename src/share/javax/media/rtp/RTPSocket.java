/*
 * @(#)RTPSocket.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package javax.media.rtp;

import java.io.IOException;

/**
 * A programmer may abstract the underlying transport mechanism for
 * RTP control and data from the RTPSM. This is done via the RTPSocket
 * object. A RTPSocket datasource must be created and handed over to
 * Manager. The Manager will take care of creating a player for the content
 * type defined for this datasource. When hiding the underlying
 * transport protocol from the RTPSessionManager, the user is
 * responsible for streaming control and data to and from this
 * RTPSocket. 
 * Basically, every RTPSocket is a JMF compliant datasource and is used
 * for streaming the data channel of an RTP session to the RTPSM. 
 * Specifically, the RTPSocket is an instance of an RTPPushDataSource. It
 * also has a control counterpart RTPPushDataSource which is accessed via 
 * methods of the DataChannel interface. Connecting, disconnecting,
 * starting and stopping the RTPSocket will take care of calling these
 * methods on the control RTPPushDataSource as well.The programmer will
 * still need to set the source and destination streams for
 * the data and control source.
 *
 * @deprecated This inferface has been replaced with the RTPConnector interface.
 *
 * @see RTPPushDataSource
 * @see DataChannel
 */
public class RTPSocket extends RTPPushDataSource implements DataChannel{
    RTPPushDataSource controlsource = null;

    public RTPSocket(){
	controlsource = new RTPPushDataSource();
    }
    public RTPPushDataSource getControlChannel(){
	return controlsource;
    }
    /**
     * Set your content type as well as content type of the control
     * source
     */
     public void setContentType(String contentType){
	 super.setContentType(contentType);
	 controlsource.setContentType(contentType);
     }
    /**
     * connect yourself and the control source
     */

    public void connect() throws IOException{
	super.connect();
	controlsource.connect();
    }
    /**
     * disconnect yourself and the control source
     */
    public void disconnect(){
	super.disconnect();
	controlsource.disconnect();	
    }
    /**
     * Start yourself and the control source
     */
    public  void start() throws IOException{
	super.start();
	controlsource.start();
    }
    /**
     * Stop yourself and the control source
     */
    public  void stop() throws IOException{
	super.stop();
	controlsource.stop();
    }
    
}
    
	
	
