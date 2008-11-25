/*
 * @(#)RtspUtil.java	1.12 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.content.rtsp;

import java.net.*;
import java.util.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.rtp.*;

import com.sun.media.*;
import com.sun.media.sdp.*;
import com.sun.media.rtsp.*;
import com.sun.media.rtsp.protocol.*;


public class RtspUtil implements RtspListener{
    // timer (in ms):
    private final int TIMER_1 = 60000; // used for describe msg
    private final int TIMER_2 = 30000; // used for all other messages

    private RtspManager rtspManager;
    private RTPManager mgrs[];
    private String mediaTypes[];
    private long sequenceNumber;
    private int numberOfTracks;    
    private String userAgent;
    private RtspUrl rtspUrl;
    private String mediaControls[];
    private int server_ports[];
    private int client_ports[];
    private String session_ids[];
    private Message message;    
    private int connectionId;
    private String url;
    private double startPos;
    private String processError;
    private long duration; 
    
    private Vector listeners;    

    private ReceiveStreamListener parent;

    public RtspUtil( ReceiveStreamListener parent) {
	this.parent= parent;
	
        listeners = new Vector();
	
	rtspManager = new RtspManager(false);
		
        sequenceNumber = (long)(Math.random() * 1000);

	userAgent = "User-Agent: JMF RTSP Player " + BasicPlayer.VERSION; 
	
        rtspManager.addListener(this);
    }

    public void setUrl( String url) {
	this.url = url;	
    }

    public void setStartPos( double startPos) {	
	this.startPos = startPos;
    }

    public RTPManager getRTPManager( int i) {
	return mgrs[ i];
    }

    public RTPManager[] getRTPManagers() {
	return mgrs;
    }

    public String getMediaType( int i) {
	return mediaTypes[ i];
    }

    public String[] getMediaTypes() {
	return mediaTypes;
    }

    public long getDuration() {
	return duration;
    }

    public void removeTrack( int trackId) {
	Log.comment( "track removed: " + mediaTypes[ trackId]);

	mgrs[ trackId].removeTargets( "media track not supported");
	mgrs[ trackId].dispose();	
	
	numberOfTracks--;

	if( trackId  + 1 > mgrs.length) {
	    int length= mgrs.length - trackId - 1;
	
	    System.arraycopy( mgrs, trackId + 1, mgrs, trackId, length);
	}
    }
    
    public boolean createConnection() {
	boolean realized= true;
	
	try {
            rtspUrl = new RtspUrl(url);
	} catch( MalformedURLException e) {
	    processError= "Invalid RTSP URL: " + url;
	    return false;
	}

        String ipAddress = getServerIpAddress();

	if( ipAddress == null) {
            Log.error( "Invalid server address:" + url);	    
	    processError= "Invalid Server adress: " + url;
	    return false;
	}
	
        connectionId = rtspManager.createConnection(ipAddress, rtspUrl.getPort());

        if (connectionId < 0) {
            switch (connectionId) {
                case - 2:
                    processError = "Unknown RTSP Host!";
                    break;
                case - 3:
                    processError = "Can't connect to RTSP Server!";
                    break;
                default:
                    processError = "Unknown reason";
                break;
            }

            realized = false;
        }

        return realized;
    }

    public void closeConnection() {
        rtspManager.closeConnection(connectionId);
    }
	    
    public boolean rtspSetup() {
        String msg = "DESCRIBE rtsp://" + rtspUrl.getHost() + "/" + rtspUrl.getFile() +
                     " RTSP/1.0\r\n" + "CSeq: " + sequenceNumber + "\r\n" +
                     "Accept: application/sdp\r\n" + userAgent + "\r\n\r\n";

        sendMessage(msg);

        boolean timeout = waitForResponse(TIMER_1);

        if (timeout) {
            sendStatusMessage(StatusMessage.TIMEOUT, "Timeout received.");

            return false;
        }

	if( !responseOk()) {
	    return false;
	}

        setDuration();

	// create the RTP Managers:
	numberOfTracks = getNumTracks();

	client_ports = new int[ numberOfTracks];

        mgrs = new RTPManager[ numberOfTracks];
	
	// get control strings and media types:
	mediaControls = new String[ numberOfTracks];
	mediaTypes= new String[ numberOfTracks];
	String dynamicPayloads[]= new String[ numberOfTracks];

	for( int i = 0; i < numberOfTracks; i++) {	             
	    mgrs[ i] = createSessionManager( i);

	    mediaTypes[ i] = getCurMediaType( i);
	    mediaControls[ i] = getMediaAttributeValue( i, "control");	    
	    dynamicPayloads[ i] = getMediaAttributeValue( i, "rtpmap");

	    // Add the dynamic payloads if there's one.
	    if (mediaTypes[i] != null && dynamicPayloads[i] != null) {
		addDynamicPayload(mgrs[i], mediaTypes[i], dynamicPayloads[i]);
	    }
	}

	// get the content base:
        String contentBase = getContentBase();

	session_ids = new String[ numberOfTracks];

	server_ports = new int[ numberOfTracks];

	// setup the individual tracks:	    
        for( int i = 0; i < numberOfTracks; i++) {
	    if( i == 0) {            	      	   
                msg = "SETUP " + contentBase + mediaControls[ i] + " RTSP/1.0\r\n" + 
                      "CSeq: " + sequenceNumber + "\r\n" +
                      "Transport: RTP/AVP;unicast;client_port=" + client_ports[ i] +
                      "-" + (client_ports[ i] + 1) + "\r\n" + userAgent + "\r\n\r\n";
	    } else {
                msg = "SETUP " + contentBase + mediaControls[ i] + " RTSP/1.0\r\n" + 
                      "CSeq: " + sequenceNumber + "\r\n" +
                       "Transport: RTP/AVP;unicast;client_port=" + client_ports[ i] +
                       "-" + (client_ports[ i] + 1) + "\r\n" + "Session: " +
                       session_ids[ 0] + "\r\n" + userAgent + "\r\n\r\n";
	    }

            sendMessage(msg);

            timeout = waitForResponse(TIMER_2);

            if (timeout) {
                Log.error("ERROR: Timeout received (1).");
		processError= "Server is not responding";		
                return false;
            }

	    if( !responseOk()) {
	        return false;
	    }	    

	    String sessionId= getSessionId();

	    if( sessionId == null) {
		processError= "Invalid session ID";
		return false;
	    }
	    
            session_ids[ i] = sessionId;

            int pos = session_ids[ i].indexOf(';');

            if (pos > 0) {
                session_ids[ i] = session_ids[ i].substring(0, pos);
            }

	    int serverPort= getServerDataPort();

	    if( serverPort == -1) {
		processError= "Invalid server data port";
		return false;
	    }

            server_ports[ i] = serverPort;	    
	}

        return true;
    }

    private boolean responseOk() {
	boolean result= false;
	
        int statusCode= getStatusCode();
	
        if (statusCode == com.sun.media.rtsp.protocol.StatusCode.OK) {
	    result= true;            
        } else { 
            processError =  "Message from RTSP Server - " + getStatusText(statusCode);
        }
	
        return result;
    }
    
    private String getSessionId() {
	String id= null;

	try {
            ResponseMessage responseMsg = (ResponseMessage) message.getParameter();

            SessionHeader hdr = (SessionHeader) responseMsg.getResponse().getHeader(
                    Header.SESSION).parameter;

	     id= hdr.getSessionId();
	} catch( Exception e) {
	}

	return id;
    }
    
    private int getServerDataPort() {
	int port= -1;

	try {
            ResponseMessage responseMsg = (ResponseMessage) message.getParameter();
	    
            TransportHeader transport_hdr =
                    (TransportHeader) responseMsg.getResponse()
	                .getHeader( Header.TRANSPORT).parameter;

	    port= transport_hdr.getServerDataPort();
	} catch( Exception e) {
	}

	return port;
    }
    
    public boolean rtspStart() {
        String msg;

	// start all tracks in one compound statement:
        msg = "PLAY rtsp://" + rtspUrl.getHost() + "/" +
                rtspUrl.getFile() + " RTSP/1.0\r\n" + "CSeq: " +
                sequenceNumber + "\r\n" + "Range: npt=" +
                (startPos / 1000000000) + "-\r\n" + "Session: " +
                 session_ids[ 0] + "\r\n" + userAgent + "\r\n\r\n";

        sendMessage(msg);

        boolean timeout = waitForResponse(TIMER_2);

        if (timeout) {
	    processError= "Server is not responding";
            return false;
        }

	int code= getStatusCode();

	if( code == -1) {
	    processError= "Received invalid status code";
	    return false;
	}
	
        if (getStatusCode() == com.sun.media.rtsp.protocol.StatusCode.SESSION_NOT_FOUND) {
	    for( int i = 0; i < numberOfTracks; i++) {
		mgrs[ i].removeTargets( "session not found");
		mgrs[ i].dispose();

                return false;
            }
	}

        return true;
    }

    public void rtspStop() {
        String msg = "PAUSE rtsp://" + rtspUrl.getHost() +
		     "/" + rtspUrl.getFile() +
                     " RTSP/1.0\r\n" + "CSeq: " + sequenceNumber + "\r\n" +
                     "Session: " + session_ids[ 0] + "\r\n" + userAgent +
		     "\r\n\r\n";

        sendMessage(msg);

        boolean timeout = waitForResponse(TIMER_2);

        if (timeout) {
            sendStatusMessage(StatusMessage.TIMEOUT, "Timeout received.");

            return;
        }
    }

    public void rtspTeardown() {
        String msg = "TEARDOWN rtsp://" + rtspUrl.getHost() + "/" +
                rtspUrl.getFile() + " RTSP/1.0\r\n" + "CSeq: " +
                sequenceNumber + "\r\n" + "Session: " + session_ids[ 0] +
                "\r\n" + userAgent + "\r\n\r\n";

        sendMessage(msg);
        
        boolean timeout= waitForResponse( TIMER_2);

        if (timeout) {
            sendStatusMessage( StatusMessage.TIMEOUT,
                               "Timeout received.");

            return;
         }
    }

    public RTPManager createSessionManager( int index) {
	RTPManager rtpManager= RTPManager.newInstance();

        if (rtpManager == null) {
            return null;
        }

        rtpManager.addReceiveStreamListener((ReceiveStreamListener) parent);

        try {
            // create the local Session Address:
            InetAddress localHost = InetAddress.getLocalHost();

            // SessionAddress localAddress = new SessionAddress( localHost,
	    // client_port);

	    SessionAddress localAddress= new SessionAddress();
	    
            // initialize the RTP Manager:
            rtpManager.initialize( localAddress);

	    client_ports[ index] = localAddress.getDataPort();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
	}

        return rtpManager;
    }

    /**
     * Parse the dynamic payload string and add the payload type to the
     * RTPManager.
     */
    private void addDynamicPayload(RTPManager mgr, String typeStr, 
	String dpStr) {

	// Parse the dynamic payload.
	int payload;
	Format fmt;
	int c;
	String tmpStr;

	// Skip leading spaces.
	c = 0;
	while (dpStr.length() > 0 && dpStr.charAt(c) == ' ') c++;
	if (c > 0)
	    dpStr = dpStr.substring(c);

	// Parse the payload number.
	c = 0;
	while (dpStr.length() > 0 && dpStr.charAt(c) != ' ') c++;
	if (c < 0)
	    return;

	tmpStr = dpStr.substring(0, c);
	dpStr = dpStr.substring(c);

	Integer integer = Integer.valueOf(tmpStr);
	if (integer == null)
	    return;

	// Check the payload value.
	payload = integer.intValue();
	if (payload < 96 || payload > 127)
	    return;

	// Skip leading spaces.
	c = 0;
	while (dpStr.length() > 0 && dpStr.charAt(c) == ' ') c++;
	if (c > 0)
	    dpStr = dpStr.substring(c);

	if (dpStr.length() == 0)
	    return;
		
	// Parse the format string.
	c = 0;
	while (dpStr.length() > 0 && dpStr.charAt(c) != '/') c++;
	if (c > 0)
	    tmpStr = dpStr.substring(0, c);
	else
	    tmpStr = dpStr;

	if (tmpStr.length() == 0)
	    return;

	// Append "/rtp" to generate a JMF RTP encoding.
	tmpStr = tmpStr.toLowerCase() + "/rtp";

	// Add the payload type to format mapping the the manager.

	if ("video".equalsIgnoreCase(typeStr)) {
	    mgr.addFormat(new VideoFormat(tmpStr), payload);
	    Log.comment("Add RTP dynamic payload for video: " + payload + " : " + tmpStr);
	} if ("audio".equalsIgnoreCase(typeStr)) {
	    mgr.addFormat(new AudioFormat(tmpStr), payload);
	    Log.comment("Add RTP dynamic payload for audio: " + payload + " : " + tmpStr);
	}
    }

    public int getStatusCode() {
	int code= -1;

	try { 
            ResponseMessage responseMsg = (ResponseMessage) message.getParameter();
	    code= responseMsg.getResponse().getStatusLine().getCode();
	} catch( Exception e) {
	}

	return code;
    }

    private String getStatusText( int code) {
	return StatusCode.getStatusText( code);
    }

    private void setDuration() {
        duration= 0;

        ResponseMessage msg = (ResponseMessage) message.getParameter();

        double start_time = 0.0;
        double end_time = 0.0;

        SdpParser sdp = msg.getResponse().sdp;

        if (sdp != null) {
            MediaAttribute attribute = sdp.getSessionAttribute("range");

            if (attribute != null) {
                String value=  attribute.getValue();

                if (value.startsWith("npt")) {
                    int start = value.indexOf('=') + 1;
                    int end = value.indexOf('-');

                    String startTime = value.substring(start, end).trim();

                    String endTime = value.substring(end + 1).trim();

                    end_time = new Double(endTime).doubleValue();

                    duration = (long)(end_time * 1000000000);
                }
            }
        }
    }
    
    private int getNumTracks() {
	int numTracks = 0;
	
	ResponseMessage msg = (ResponseMessage) message.getParameter();

        SdpParser sdp = msg.getResponse().sdp;

	if (sdp != null) {
	    numTracks = sdp.getMediaDescriptions().size();
	}

	return numTracks;
    }

    private MediaDescription getMediaDescription( String mediaName) {
        MediaDescription description = null;

	try {
            ResponseMessage msg = (ResponseMessage) message.getParameter();
	
            SdpParser sdp = msg.getResponse().sdp;

            description = sdp.getMediaDescription(mediaName);
	} catch( Exception e) {
	}

        return description;
    }
    
    private String getCurMediaType( int i) {
	String type = null;

	try {
	    ResponseMessage msg = (ResponseMessage) message.getParameter();

            SdpParser sdp = msg.getResponse().sdp;

	    MediaDescription md = (MediaDescription) sdp.getMediaDescriptions().elementAt( i);

            type = md.name;
	} catch( Exception e) {
	}

	return type;
    }    

    static public String getMediaAttribute( MediaDescription md,
                                      String attribute) {

        String mediaAttribute= "";

        if( md != null) {
            MediaAttribute ma = md.getMediaAttribute( "control");

	    if( ma != null) {
                mediaAttribute= ma.getValue();
	    }
	}

        return mediaAttribute;
    }
      
    private String getMediaAttributeValue( int i, String attribute) {
	String value = null;

	try {
	    ResponseMessage msg = (ResponseMessage) message.getParameter();

            SdpParser sdp = msg.getResponse().sdp;

	    MediaDescription md = (MediaDescription) sdp.getMediaDescriptions().elementAt( i);

	    MediaAttribute ma= md.getMediaAttribute( attribute);

	    value= ma.getValue();
	} catch( Exception e) {
	}

	return value;
    }

    private String getTrackID(String mediaName) {
        String trackId = null;

	try {
            ResponseMessage msg = (ResponseMessage) message.getParameter();

            SdpParser sdp = msg.getResponse().sdp;

            MediaDescription description = sdp.getMediaDescription(mediaName);

            MediaAttribute attribute = description.getMediaAttribute("control");

            trackId = attribute.getValue();
        } catch( Exception e) {
	}

        return trackId;
    }

    private String getContentBase() {
        String contentBase= "";

	try {
            ResponseMessage responseMsg = (ResponseMessage) message.getParameter();

            Header header = responseMsg.getResponse().getHeader( Header.CONTENT_BASE);

            ContentBaseHeader cbh = (ContentBaseHeader)header.parameter;

            contentBase = cbh.getContentBase();
	} catch( Exception e) {
	}

        return contentBase;
    }
    
    private void sendMessage(String message) {
        responseReceived = false;

        boolean success = rtspManager.sendMessage(connectionId, message);

        if (!success) {
            String ipAddress = getServerIpAddress();

            connectionId = rtspManager.createConnection(ipAddress, rtspUrl.getPort());

            rtspManager.sendMessage(connectionId, message);
        }
    }

    boolean responseReceived;
    boolean dataReceived;

    Object responseSync = new Object();

    private synchronized boolean waitForResponse(int time) {
        boolean timeout = false;

        try {
            synchronized (responseSync) {

		if (!responseReceived) {
		    responseSync.wait(time);
		}

                if (responseReceived) {
                    sequenceNumber++;
                } else {
                    timeout = true;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return timeout;
    }    

    private void processRtspRequest(int connectionId, Message message) {
        if (message.getType() == MessageType.OPTIONS) {
            OptionsMessage msg = (OptionsMessage) message.getParameter();

            sendResponse(connectionId, msg.getRequest());
        }
    }
    
    private void processRtspResponse(int connectionId, Message message) {
        this.message = message;

        responseReceived = true;

        synchronized (responseSync) {
            responseSync.notify();
        }
    }    

    private void sendResponse(int connectionId, Request msg) {
        String type = null;

        Header header = msg.getHeader(Header.CSEQ);

        if( header != null) {
            CSeqHeader cSeqHeader = (CSeqHeader) header.parameter;

            String message = "RTSP/1.0 200 OK\r\n" + "CSeq: " +
                    cSeqHeader.getSequenceNumber() + "\r\n\r\n";

            sendMessage(message);
	}
    }
    
    private void sendStatusMessage(int code, String message) {
        for (int i = 0; i < listeners.size(); i++) {
            ((RtspAppListener) listeners.elementAt(i)).postStatusMessage(code,
                    message);
        }
    }

    public void addListener(RtspAppListener listener) {
        listeners.addElement(listener);
    }

    public void removeListener(RtspAppListener listener) {
        listeners.removeElement(listener);
    }

    public int getNumberOfTracks() {
	return numberOfTracks;
    }

    public int[] getServerPorts() {
	return server_ports;
    }

    public void rtspMessageIndication(int connectionId, Message message) {
	if (message.getType() == MessageType.RESPONSE) {
            processRtspResponse(connectionId, message);
        } else {
            processRtspRequest(connectionId, message);
        }
    }

    public String getServerIpAddress() {
        String ipAddress = null;

	try {
	    if( rtspUrl == null) {
		rtspUrl= new RtspUrl( url);
	    }
	    
            String host = rtspUrl.getHost();

	    if( host != null) {		
                ipAddress = InetAddress.getByName(host).getHostAddress();
	    }
	} catch( MalformedURLException e) {
	    // System.err.println( "Malformed URL Exception: " + e);
	} catch (UnknownHostException e) {
            // System.err.println( "Host unknown: " + name);
        }
	
        return ipAddress;
    }
    
    public void rtspConnectionTerminated(int connectionId) {
        // System.out.println( "RtspPlayer::rtspConnectionTerminated");
    }

    public String getProcessError() {
	return processError;
    }

    public void setProcessError( String error) {
	processError= error;
    }    
}
