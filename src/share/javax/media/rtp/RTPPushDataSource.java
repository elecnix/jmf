/*
 * @(#)RTPPushDataSource.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp;

import javax.media.protocol.*;
import java.io.*;
import javax.media.*;


/**
 * This is an abstraction for a RTP data handler. The source allows
 * the underlying network/transport protocol to be abstracted from the
 * RTP handler. The source is a TWO WAY datasource in that it has an
 * output data  stream (PushSourceStream) as well as an input data
 * stream (OutputDataStream). RTPDataSocket extends
 * javax.media.protocol.DataSource. Data flowing out of or in to this
 * source is a  stream of bytes in the form of a RTP packet and does not
 * contain any information dependant on the network/transport protocol
 * that will be used to transmit these bytes. Note: an RTPPushDataSource
 * can either handle the DataChannel of an RTP session or the control
 * channel of an RTP session. An RTPPushDataSource that handles the data
 * channel of an RTP session must implement the interface defined in
 * javax.media.rtp.DataChannel.
 *
 * @deprecated This inferface has been replaced with the RTPConnector interface.
 *
 * @see DataChannel
 */
public class RTPPushDataSource extends PushDataSource{
    /**
     * The output stream of this socket. Implements
     * javax.media.protocol.PushSourceStream. Data on this stream is
     * transferrred to the SourceTransferHandler
     */
    PushSourceStream outputstream;
    /**
     * The input stream of this socket. Data on this stream is sent
     * out on the wire using the underlying network/transport protocol
     * Implements javax.media.rtp.OutputDataStream
     * @className OutputDataStream
     */
    OutputDataStream inputstream;
    /**
     * The content type of this socket. needs to be set by the
     * creator of this datasocket before a player can be created for
     * this datasource.
     */
    String contentType = null;
    /**
     * Used to ensure that DataSource methods like getContentType()
     * cannot be called till this datasource has been connected.
     */
    private boolean connected = false;
    /**
     * Used to check if the datasource has been started. This will
     * initiate data transfer to and from this data source
     */
    private boolean started = false;
    /**
     * The actual datasource created by the RTPSM and passed to the handler
     */
    DataSource childsrc = null;
    /**
     * The RTPControl interface that is exported by this datasource
     * and will be used to configure information for this datasource
     */    
    private RTPControl rtpcontrol = null;
    
    public RTPPushDataSource(){
	// since we cannot access the RTPControlImpl class in the
	// javax.media packages,  we will load the class based on the
	// classname
	Class eClass = null;
	try{
	    eClass  =
		Class.forName("com.sun.media.rtp.RTPControlImpl");
	    rtpcontrol = (RTPControl) eClass.newInstance();
	}catch (Exception e){
	    
	    rtpcontrol = null;
	}
    }
    
    public void setChild(DataSource source){
	childsrc = source;
    }
    /**
     * Method to retrive the output stream of this two way data source
     */
    public PushSourceStream getOutputStream(){
	return outputstream;
    }
    
    /**
     * Method to retrieve the input stream of this two way data source
     */
     public OutputDataStream getInputStream(){
	return inputstream;
    }

    /**
     * Method to set the output stream of this two way data source
     */    
    public void setOutputStream(PushSourceStream outputstream){
	this.outputstream = outputstream;
    }
    
    /**
     * Method to set the input stream of this two way data source
     */ 
    public void setInputStream(OutputDataStream inputstream){
	this.inputstream = inputstream;
    }

    /**
     *Implementation of javax.media.protocol.DataSource.getContentType()
     */
    public String getContentType(){
	if (!connected){
	    System.err.println("Error: DataSource not connected");
	    return null;
	}
	return ContentDescriptor.mimeTypeToPackageName(contentType);
    }

    /**
     * Method used to set the content type of this two way data source
     */
    public void setContentType(String contentType){
	this.contentType = contentType;
    }
    /**
     * Set the boolean to true and dont do anything else in connect for now
     */
    public void connect() throws IOException{
	connected = true;
	if (childsrc != null)
	  childsrc.connect();
    }
    /**
     * Set the boolean to false and dont do anything else in connect
     * for now
     */
    public void disconnect(){
	connected = false;
	if (childsrc != null)
	  childsrc.disconnect();
    }
    /**
     * This two way data source does not have any medialocator and
     * datasource will throw an Uninitialized DataSource error incase
     * initCheck() is called on this datasource. So, here this method is
     * overrident to esnure that no errors are thrown and in the absence
     * of a MediaLocator, nothing is done for now
     */
    protected void initCheck(){
    }
    /**
     * Initiates data-transfer. Start must be called before
     * data is available. Connect must be called before start.
     *
     * @exception IOException thrown if the source has IO trouble
     * at startup time.
     */
    public  void start() throws IOException{
	if (!connected)
	    return;
	started = true;
	if (childsrc != null)
	    childsrc.start();
    }
    /**
     * Stops data-transfer.
     * If the source has not already been connected and started,
     * stop does nothing.
     */
    public  void stop() throws IOException{
	if ((!connected) && (!started))
	    return;
	started = false;
	if (childsrc != null)
	    childsrc.stop();
    }
    /**
     * Method to find out if the source had been started
     */
    public boolean isStarted(){
	return started;
    }
    /**
     * Returns an zero length array because no controls
     * are supported.
     *
     * @return a zero length <code>Object</code> array.
     */
    public Object[] getControls(){
	// return a one element array of rtpcontrol object
	RTPControl[] controls = new RTPControl[1];
	controls[0] = rtpcontrol;
	return controls;	
    }
    /**
     * Returns <code>null</code> because no controls are implemented.
     *
     * @return <code>null</code>.
     */
    public Object getControl(String controlName){
	if (controlName.equals("javax.media.rtp.RTPControl"))
	    return rtpcontrol;
	return null;
    }

    /**
     * Returns null since the duration of the streams of this
     * datasource are not known
     */
    public Time getDuration(){
	return null;
    }
    public PushSourceStream[] getStreams(){
	PushSourceStream[] outstream = new PushSourceStream[1];
	outstream[0]= outputstream;
	return outstream;
    }
}
