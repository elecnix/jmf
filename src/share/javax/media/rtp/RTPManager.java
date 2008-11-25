/*
 * @(#)RTPManager.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.rtp;

import javax.media.protocol.*;
import javax.media.format.*;
import java.net.*;
import java.util.*;
import java.io.*;
import javax.media.rtp.event.*;
import javax.media.rtp.rtcp.*;
import javax.media.Controls;
import javax.media.Format;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Hashtable;
import com.sun.media.Log;


/**
 * The interface implemented by the RTPManager. This is the
 * starting point for creating, maintaining and closing an RTP
 * session.
 * <p>
 * <b>1. Unicast Session</b>
 * <br>
 * The following code fragment illustrates how to create a unicast
 * session:
 * <pre>
 * import java.net.*;
 * import javax.media.rtp.*;
 *
 * // create the RTP Manager
 * RTPManager rtpManager = RTPManager.newInstance();
 * 
 * // create the local endpoint for the local interface on
 * // any local port
 * SessionAddress localAddress = new SessionAddress();
 * 
 * // initialize the RTPManager
 * rtpManager.initialize( localAddress);
 *
 * // add the ReceiveStreamListener if you need to receive data
 * // and do other application specific stuff
 * // ...
 * 
 * // specify the remote endpoint of this unicast session 
 * // the address string and port numbers in the following lines
 * // need to be replaced with your values.
 * InetAddress ipAddress = InetAddress.getByName( "168.1.2.3");
 * 
 * SessionAddress remoteAddress = new SessionAddress( ipAddress, 3000);
 *
 * // open the connection
 * rtpManager.addTarget( remoteAddress);
 * 
 * // create a send stream for the output data source of a processor
 * // and start it
 * DataSource dataOutput = createDataSource();
 *
 * SendStream sendStream = rtpSession.createSendStream( dataOutput, 1);
 * sendStream.start();
 * 
 * // send data and do other application specific stuff,
 * // ...
 * 
 * // close the connection if no longer needed.
 * rtpManager.removeTarget( remoteAddress, "client disconnected.");
 * 
 * // call dispose at the end of the life-cycle of this RTPManager so
 * // it is prepared to be garbage-collected.
 * rtpManager.dispose();
 * </pre> 
 * <b>2. Multi-Unicast Session</b>
 * <br>
 * Creating multi-unicast sessions is similar to the example above. After
 * creating and starting the SendStream new remote endpoints may be added
 * by subsequent addTarget calls:
 * <pre>
 *   addTarget( remoteAddress2);
 *   addTarget( remoteAddress3);
 * </pre>
 *
 * <b>3. Multicast Session</b>
 * <br>
 * Creating and participating in multicast sessions also works similar
 * to the unicast example. Instead of specifying local and remote endpoints
 * a multicast session address needs to be created and passed into the
 * initialize and addTarget calls. Everything else follows the unicast
 * example.
 * <pre>
 * //...
 *
 * // create a multicast address for 224.1.1.0 and ports 3000/3001
 * IPAddress ipAddress = InetAddress.getByName( "224.1.1.0");
 * 
 * SessionAddress multiAddress = new SessionAddress( ipAddress, 3000);
 * 
 * // initialize the RTPManager
 * rtpManager.initialize( multiAddress);
 * 
 * // add the target
 * rtpManager.addTarget( multiAddress);
 * 
 * // ...
 */

public abstract class RTPManager implements Controls
{
    private static boolean jdkInit = false;
    private static Method forName3ArgsM;
    private static Method getSystemClassLoaderM;
    private static ClassLoader systemClassLoader;
    private static Method getContextClassLoaderM;
 

    /**
     * This method is used to add a dynamic payload to format
     * mapping to the RTPManager. The RTPManager maintains
     * all static payload numbers and their correspnding formats as
     * mentioned in the Audio/Video profile document. Using the plugin
     * packethandler interface, a user may plugin his own packetizer or
     * depacketizer to handle RTP streams of a proprietary format using
     * dynamic payload numbers as specified in the AV profile. Before
     * streaming dynamic payloads, a Format object needs to
     * be created for each of the dynamic payload types and associated 
     * with a dynamic payload number.
     * @param format The Format to be associated with this dynamic
     * payload number.
     * @param payload The RTP payload number
     * @see Format 
     */
    abstract public void
    addFormat(Format format,
              int payload);

    /**
     * Adds a ReceiveStreamListener. This listener listens to all the
     * events that notify state transitions for a particular
     * ReceiveStream.
     */
    abstract public void
    addReceiveStreamListener( ReceiveStreamListener listener);

    /**
     * Adds a RemoteListener to the session. This listener listens
     * to all remote RTP events. Currently, these include
     * ReceiverReportEvent, ReceiveSenderReportEvent and
     * RemoteCollisionEvent. This interface would be usefuly for an RTCP
     * monitor that does not wish to receive any particular stream
     * transitionEvents but just wants to monitor the session quality
     * and statistics.
     */
    abstract public void
    addRemoteListener( RemoteListener listener);

    /**
     * Adds a SendStreamListener. This listener listens to all the
     * events that notify state transitions for a particular
     * SendStream.
     */
    abstract public void
    addSendStreamListener(SendStreamListener listener);

   /**
     * Adds a SessionListener. A SessionListener will receive
     * events that pertain to the Session as a whole. Currently,
     * these include the NewParticipantEvent and
     * LocalCollisionEvent. Events are notified in the
     * update(SessionEvent) method which must be implemented by all
     * SessionListeners. 
     * @classname SessionListener
     */
    abstract public void
    addSessionListener(SessionListener listener);

    /**
     * Closes all open streams associated with the endpoint defined 
     * by remoteAddress.
     * <P>
     *
     * @param remoteAddress The RTP session address of a remote end
     * point for this session. i.e. the IP address/port of a remote
     * host
     *
     * @param reason A string that RTCP will send out to other
     * participants  as the reason the local participant  has quit the
     * session.This RTCP packet will go out with the default SSRC of the
     * session. If supplied as null, a default reason will be supplied
     * by the RTPManager. 
     * <P>
     */
    abstract public void
    removeTarget( SessionAddress remoteAddress, String reason)
                throws InvalidSessionAddressException;

    /**
     * Closes the open streams associated with all remote endpoints
     * that have been added previously by subsequent addTarget() calls. <p>
     *
     * @param reason A string that RTCP will send out to other
     * participants  as the reason the local participant  has quit the
     * session.This RTCP packet will go out with the default SSRC of the
     * session. If supplied as null, a default reason will be supplied
     * by the RTPManager. <P>
     */
    abstract public void
    removeTargets(String reason);

    /**
     * This method is used to create a sending stream within the RTP
     * session. For each time the call is made, a new sending stream
     * will be created. This stream will use the SDES items as entered
     * in the initialize() call for all its RTCP messages. Each stream
     * is sent out with a new SSRC (Synchronisation SouRCe
     * identifier), but from the same participant  i.e. local
     * participant. <BR>
     *
     * @param dataSource This is the PushOutputDataSource or
     * PullOutputDataSource which is the output data source of the
     * Processor. This data source may contain more than one
     * stream. The stream which is used in creating this RTP
     * stream is specified in the next parameter of stream.<BR>
     *
     * @param streamIndex The index of the sourcestream from which
     * data is sent out on this RTP stream. An index of 1 would indicate the first
     * sourcestream of this data source should be used to create the RTP
     * stream. If the index is set to zero, it would indicate a RTP
     * mixer operation is desired. i.e. all the streams of this
     * data source must be mixed into one single stream from one single
     * SSRC. <BR>
     * 
     * Note: The RTP payload that is used to send this stream is found
     * from the format set on the SourceStream of the data source
     * supplied. <BR>
     * If the sourcestream has no format set or has a
     * format for which a packetizer plugin cannot be found in the session
     * manager's database, an UnsupportedFormatException will be thrown
     * by the RTPManager. <BR>
     * 
     * Note on PullDataSources supplied to the RTPManager:
     * In most cases, it is expected that the data source supplied to the
     * RTPManager for stream creation would be a
     * PushDataSource. In cases that the data source is a PullDataSource,
     * it MUST have a format set on its SourceStreams. This is the only
     * way for RTPManger to determine the RTP payload to use in
     * the header of the stream as well as the bitrate to pulldata from
     * this data source. <BR>
     *
     * @return The SendStream created by the RTPManager.<BR>
     *    
     * @exception UnsupportedFormatException
     * (javax.media.format.UnsupportedFormatException ). This
     * exception is thrown if the format is not set on the sourcestream
     * or a RTP payload cannot be located for the format set on the
     * sourcestream.
     * @exception IOException
     * Thrown for two possible reasons which will be specified in the
     * message part of the exception
     * 1) If the session was initiated with zero rtcpBandwidthFraction which
     * implied that this participant could not send out any RTP/RTCP
     * data or control messages. i.e. it could not also create any send
     * streams and was just a passive listener for this session.
     * 2) If there was any problem opening the sending sockets 
     * @classname SendStream 
     * 
     */
    abstract public SendStream
    createSendStream(DataSource dataSource,
                     int streamIndex) throws
    UnsupportedFormatException, IOException;

    /**
     * Releases all objects allocated in the course of the session and prepares
     * the RTPManager to be garbage-collected. This method should be called at
     * the end of any RTP session.
     */
    abstract public void dispose();

    /**
     * Returns a vector of all the active (data sending)
     * participants. These participants may be remote and/or the local participant.
     */
    abstract public Vector
    getActiveParticipants();

    /**
     * Returns all the participants of this session.
     */
    abstract public Vector
    getAllParticipants();

    /**
     * This method will provide access to overall data and control
     * messsage reception statistics for this session. Statistics on
     * data from individual sources is available from the
     * getSourceReceptionStats() method of the  ReceiveStream interface.
     * @return The GlobalReceptionStats for this session
     * @classname ReceiveStream
     */
    abstract public GlobalReceptionStats
    getGlobalReceptionStats();

     /**
     * This method will provide access to overall data and control
     * messsage transmission statistics for this session. Statistics on
     * data from individual sources is available from the
     * getSourceTransmissionStats() method of the SendStream interface.
     * @return The GlobalTransmissionStats for this session
     * @classname SendStream
     */
    abstract public GlobalTransmissionStats
    getGlobalTransmissionStats();

    /**
     * Retrieves the local participant.
     */
    abstract public LocalParticipant
    getLocalParticipant();

    /**
     * Returns all the passive participants. These participants will
     * include the local participant and some remote participants that
     * do not send any data.
     */
    abstract public Vector
    getPassiveParticipants();

    /** Returns the ReceiveStreams created by the
     * RTPManager. These are streams formed when the RTPManager
     * detects a new source of RTP data. 
     * ReceiveStreams returned are a snapshot of the current state in the
     * RTPManager and the ReceiveStreamListener interface may be used
     * to get notified of additional streams.
     */   
    abstract public Vector
    getReceiveStreams();

    /**
     * Returns a Vector of all the remote participants in the
     * session.This vector is simply a snapshot of the current state in
     * the RTPManager. The SessionListener interface can be
     * used to get notified of additional participants for the
     * Session. <P>
     */
    abstract public Vector
    getRemoteParticipants();

    /** Returns the SendStreams created by the
     * RTPManager.
     * SendStreams returned are a snapshot of the current state in the
     * RTPSesion and the SendStreamListener interface may be used
     * to get notified of additional streams.
     */   
    abstract public Vector
    getSendStreams();

    /**
     * Initializes the session. Once this method has been called, the
     * session  is "initialized" and this method cannot be called again. <P>
     *
     * @param localAddress Encapsulates the *local* control and data
     * addresses to be used for the session. If either InetAddress
     * contained in  this parameter is null, a default local address
     * will be chosen. The ports do not necessarily need to be specified
     * (i.e. they  may be the ANY_PORT constant); the RTPManager will pick
     * appropriate ports in that case. <P>
     * If the session joins a multicast group, the localAddress will be ignored.
     * The multicast address will be taken from the addTarget() call.
     */
    abstract public void
    initialize( SessionAddress localAddress)
                throws InvalidSessionAddressException, IOException;

    /**
     * Initializes the session. Once this method has been called, the
     * session  is "initialized" and this method cannot be called again. <P>
     *
     * @param localAddresses An array of local session adresses.
     * In most cases the address will contain a single session address,
     * but for multi-homed systems (systems with more than one IP interface) 
     * there may be several local adresses specified in this parameter. <p>
     *
     * @param sourceDescription An array of SourceDescription
     * objects  containing information to send in RTCP SDES packets
     * for the  local participant.  This information  can be changed by
     * calling setSourceDescription() on the local Participant
     * object.  
     *
     * @param rtcpBandwidthFraction The fraction of the session bandwidth
     * that the RTPManager must use when sending out RTCP reports. 
     *
     * @param rtcpSenderBandwidthFraction The fraction of the
     * rtcpBandwidthFraction that the RTPManager must use to send out RTCP Sender
     * reports from the local participant. The remaining fraction of the
     * rtcp_bw is used for sending out RTCP Receiver reports. <P>
     *
     * @param encryptionInfo the encryption information to be used in
     * this session.
     *
     * Note : The rtcpBandwidthFraction is set to zero for a
     * non-participating observer of this session. In this case 
     * the application will receive both RTP and RTCP messages, but will
     * not send out any RTCP feedback reports.
     * This is equivalent to setting the outgoing RTP/RTCP
     * bandwidth of this application to zero, implying that this
     * application may NOT send out any data or control streams and can
     * thus not make a call to createSendStream(). If it does, it will
     * receive an exception. Further, this application is NOT considered
     * a Participant since it does not send out any RTCP
     * information. Consequently, this client will NOT appear in the
     * list of Participants for this session.<P>
     *
     * Init called a second time or thereafter will return
     * without doing anything, since the session had already been
     * initialized. If parameters to init() are different from
     * before, the user must note that the new parameters will ignored
     * as a result of no action being performed. <p>
     *
     * @exception InvalidSessionAddressException This exception is
     * thrown if the local control and data addresses given in
     * parameter localAddress do not belong to one of the localhost
     * interfaces. 
     * @exception IOException
     *
     * @classname SessionAddress
     * @classname SourceDescription
     */
    abstract public void
    initialize( SessionAddress localAddresses[],
  	        SourceDescription sourceDescription[],
                double rtcpBandwidthFraction,
                double rtcpSenderBandwidthFraction,
                EncryptionInfo encryptionInfo)
        throws InvalidSessionAddressException, IOException;

    /**
     * Initializes the session. Once this method has been called, the
     * session  is "initialized" and this method cannot be called again. <P>
     *
     * @param connector An implementation of the RTPConnector interface that
     * allows the developer to connect the RTPManager to any type of transport.
     * By default, RTP is streamed over UDP. If an RTPConnector is present, the
     * RTPManager will use the connector's send and receive methods to send or
     * receive data.
     * Please note: the methods addTarget, removeTarget and removeTargets cannot
     * be used in conjunction with an RTPConnector since these tasks will be handled
     * directly by the connector object.
     *
     * @classname RTPConnector
     */
    
    abstract public void
    initialize( RTPConnector connector);

    /**
     * This method opens the session, causing RTCP reports to be
     * generated and callbacks to be made through the
     * SessionListener interface. This method must be called after
     * session initialization and prior to the creation of any streams
     * on a session.
     * 
     * @param remoteAddress The RTP session address of a remote end
     * point for this session. i.e. the IP address/port of a remote
     * host
     * @exception InvalidSessionAddressException This exception is
     * thrown if the remote control and data addresses given in
     * parameter localAddress are not valid session addresses.
     *  <P>
     * @classname SessionAddress
     *
     */
    abstract public void
    addTarget( SessionAddress remoteAddress) 
        throws InvalidSessionAddressException, IOException;

    /**
     * Removes a ReceiveStreamListener.
     * @classname ReceiveStreamListener
     */ 
    abstract public void
    removeReceiveStreamListener( ReceiveStreamListener listener);

    /**
     * Removes a RemoteListener.
     * @classname RTPRemoteListener
     */ 
    abstract public void
    removeRemoteListener( RemoteListener listener);

    /**
     * Removes a SendStreamListener.
     * @classname SendStreamListener
     */ 
    abstract public void
    removeSendStreamListener(SendStreamListener listener);

    /**
     * Removes a SessionListener.
     * @classname SessionListener
     */ 
    abstract public void
    removeSessionListener(SessionListener listener);

    /**
     * Create an <CODE>RTPManager</CODE> object for the underlying
     * implementation class.
     */
    public static RTPManager newInstance() {
        RTPManager rtpManager= null;

        Enumeration SessionList =
            getRTPManagerList().elements();

        while( SessionList.hasMoreElements()) {
            String protoClassName = (String)SessionList.nextElement();

            try {	    
                Class protoClass = getClassForName(protoClassName);
                
                rtpManager = (RTPManager)protoClass.newInstance();
            } catch (ClassNotFoundException e) {
                // System.out.println( "class def not found.");
            } catch (InstantiationException e) {
                String err = "Error instantiating class: " + protoClassName + " : " + e;
                Log.error(e);				
            } catch (IllegalAccessException e) {
                System.out.println( "illegal access.");
            } catch (Exception e) {		
                String err = "Error instantiating class: " + protoClassName + " : " + e;
                Log.error(e);		
            } catch (Error e) {
                String err = "Error instantiating class: " + protoClassName + " : " + e;
                Log.error(e);		
            }

            if( rtpManager != null) {
                break;
            }
        }

        return rtpManager;
    }

    /**
     * Build a list of <CODE>RTPManager</CODE> implementation classes.
     * The implemenation class must be named 'RTPSessionMgr' and is
     * required to extend from javax.media.rtp.RTPManager.
     * <p>
     * The first name in the list will always be:
     * <blockquote><pre>
     * media.rtp.RTPSessionMgr
     * </pre></blockquote>
     * <p>
     *
     * Each additional name looks like:
     * <blockquote><pre>
     * com.&lt;company&gt;.media.rtp.RTPSessionMgr
     * </pre></blockquote>
     * for every <CODE>&lt;company&gt;</CODE> in the
     * company-list.
     */
    static public Vector getRTPManagerList() {

        // The first element is the name of the protocol handler ...
        String sourceName =
            "media.rtp.RTPSessionMgr";

        return buildClassList(getProtocolPrefixList(), sourceName);
    }        

    // This is a Package private class
    // This is a Package private class
    static Class getClassForName(String className) 
                throws ClassNotFoundException {
        /**
         *  Note: if we don't want this functionality
         *  just replace it with Class.forName(className)
         */

        try {
            return Class.forName(className);
        } catch (Exception e) {
            if (!checkIfJDK12()) {
                throw new ClassNotFoundException(e.getMessage());
            }
        } catch (Error e) {
            if (!checkIfJDK12()) {
                throw e;
            }
        }

        /**
         *  In jdk1.2 application, when you have jmf.jar in the ext directory and
         *  you want to access a class that is not in jmf.jar but is in the CLASSPATH,
         *  you have to load it using the the system class loader.
         */
        try {
            return (Class) forName3ArgsM.invoke(Class.class, new Object[] {
                className, new Boolean(true), systemClassLoader});
        } catch (Throwable e) {
        }

        /**
         *  In jdk1.2 applet, when you have jmf.jar in the ext directory and
         *  you want to access a class that is not in jmf.jar but applet codebase,
         *  you have to load it using the the context class loader.
         */
        try {
            // TODO: may need to invoke RuntimePermission("getClassLoader") privilege
            ClassLoader contextClassLoader =
                (ClassLoader) getContextClassLoaderM.invoke(Thread.currentThread(), null);
            return (Class) forName3ArgsM.invoke(Class.class, new Object[] {
                className, new Boolean(true), contextClassLoader});
        } catch (Exception e) {
            throw new ClassNotFoundException(e.getMessage());
        } catch (Error e) {
            throw e;
        }
    }

    /**
     * Build a list of complete class names.
     *<p>
     * 
     * For each element of the prefix-list
     * the following element is added to the list:
     * <blockquote><pre>
     *    &lt;prefix&gt;.&lt;name&gt;
     * </pre></blockquote>
     * These are added to the list in the same order as the prefixes appear
     * in the prefix-list.
     * </ol>
     * 
     * @param prefixList The list of prefixes to prepend to the class name.
     * @param name The name of the class to build the list for.
     * @return A vector of class name strings.
     */
    static Vector buildClassList(Vector prefixList, String name) {
        
        // New list which has the name as the first element ...
        Vector classList = new Vector();

        // Try and instance one directly from the classpath
        // if it's there.
        // $jdr: This has been objected to as confusing,
        // the argument for it's inclusion is that it
        // gives the user (via the classpath) a way
        // of modifying the search list at run time
        // for all applications.
        classList.addElement(name);

        // ... for each prefix append the name and put it
        // in the class list ...
        Enumeration prefix = prefixList.elements();
        while( prefix.hasMoreElements()) {
            String prefixName = (String)prefix.nextElement();
            classList.addElement(prefixName + "." + name);
        }

        // ... done
        return classList;
    }

    static Vector getProtocolPrefixList() {
        return (Vector) javax.media.PackageManager.getProtocolPrefixList().clone();
    }

    private static boolean checkIfJDK12() {
        if (jdkInit)
            return (forName3ArgsM != null);
        jdkInit = true;
        try {
            forName3ArgsM = Class.class.getMethod("forName",
                                                  new Class[] {
                String.class, boolean.class, ClassLoader.class
                    });
            
            getSystemClassLoaderM = ClassLoader.class.getMethod("getSystemClassLoader", null);

            // TODO: may need to invoke RuntimePermission("getClassLoader") privilege
            systemClassLoader = (ClassLoader) getSystemClassLoaderM.invoke(ClassLoader.class, null);

            getContextClassLoaderM = Thread.class.getMethod("getContextClassLoader", null);

            return true;
        } catch (Throwable t) {
            forName3ArgsM = null;
            return false;
        }
    } 
}
  






