/*
 * @(#)SourceDescription.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp.rtcp;

import java.net.*;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import com.sun.media.*;
import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;
import com.sun.media.util.*;


/**
 * Class to encapsulate a source description item used in RTCP SDES reports. <P>
 */
public class SourceDescription implements java.io.Serializable {
    // possible values of the type variable
    public static final int SOURCE_DESC_CNAME = 1;
    public static final int SOURCE_DESC_NAME = 2;
    public static final int SOURCE_DESC_EMAIL = 3;
    public static final int SOURCE_DESC_PHONE = 4;
    public static final int SOURCE_DESC_LOC = 5;
    public static final int SOURCE_DESC_TOOL = 6;
    public static final int SOURCE_DESC_NOTE = 7;
    public static final int SOURCE_DESC_PRIV = 8;

    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege=false;
    private static Method m[] = new Method[1];
    private static Class cl[] = new Class[1];
    private static Object args[][] = new Object[1][0];

    static {
	try {
	    jmfSecurity = JMFSecurityManager.getJMFSecurity();
	    securityPrivelege = true;
	} catch (SecurityException e) {
	}
    }    
    
    /**
     * Constructor. <P>
     * the type of source description this is
     * description the actual source description string
     * frequency the frequency at which RTCP sends this source description
     * encrypted set to true if this SDES item is to be encrypted.
     * Please see individual variable description for current implementation status
     */
    public 
    SourceDescription(int type,			  
		      String  description,
		      int     frequency,
 		      boolean encrypted) {
	m_type = type;
	m_description = description;
	m_frequency = frequency;
	m_encrypted = encrypted;
    }
    
  // Instance Variables
    private int     m_type;        // what kind of user data is this
    private String  m_description = null; // the actual data 
    private int     m_frequency;   // how often RTCP sends it
    private boolean m_encrypted =  false;   // encrypt it?
    
    // Methods
    /**
     * Get the type of source description this is. <P>
     */
    public int 
    getType(){ 
	return m_type;
    }
  
    /**
     * Get the actual source description string. <P>
     */
    public String
    getDescription(){
	return m_description;
    }
    public void
    setDescription(String desc){
	m_description = desc;
    }
    /**
     * Get the frequency at which RTCP sends this source description. <P>
     * Currently, this frequency parameter will be ignored by the
     * implementation and SDES items are sent according to the following
     * algorithm... The CNAME is sent every RTCP reporting interval. All
     * other SDES items are sent once every 3 RTCP report intervals.  
     */
    public int 
    getFrequency(){
	return m_frequency;
    }
    /**
     * Should this source description be encrypted?.
     * Currently, we do not support any encryption schemes <P>
     */
    public boolean
    getEncrypted(){
	return m_encrypted;
    }

    /**
     * This function can be used to generate a CNAME using the scheme
     * described in RFC1889. This function is provided in order to facilitate CNAME
     * generation prior to actual stream creation. <P>
     *
     * @return The generated CNAME. <P>
     */
    public static String generateCNAME() {
	String hostname = null;
	InetAddress host;
	String cname = "";

	if ( jmfSecurity != null) {
	    String permission = null;
	    try {
		if (jmfSecurity.getName().startsWith("jmf-security")) {
		    permission = "connect";
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.CONNECT);
		    m[0].invoke(cl[0], args[0]);
		} else if (jmfSecurity.getName().startsWith("internet")) {
		    PolicyEngine.checkPermission(PermissionID.NETIO);
		    PolicyEngine.assertPermission(PermissionID.NETIO);
		}
	    } catch (Throwable e) {
		if (JMFSecurityManager.DEBUG) {
		    System.err.println("Unable to get " + permission +
				       " privilege  " + e);
		}
		jmfSecurity.permissionFailureNotification(JMFSecurity.CONNECT);
		// securityPrivelege = false;
	    }
	}

	try {
	    if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
		Constructor cons = jdk12InetAddressAction.cons;

		host = (InetAddress) jdk12.doPrivM.invoke(
                                           jdk12.ac,
 					  new Object[] {
 					  cons.newInstance(
 					   new Object[] {
					       null, //  static method
					       "getLocalHost",
					       null, // No parameters
                                           })});

		hostname = (String) jdk12.doPrivM.invoke(
                                           jdk12.ac,
 					  new Object[] {
 					  cons.newInstance(
 					   new Object[] {
					       host,
					       "getHostName",
					       null, // No parameters
                                           })});


	    } else {
		host = InetAddress.getLocalHost();
		hostname = host.getHostName();
	    }
	} catch (Throwable e) {
	    System.err.println("InitSession : UnknownHostExcpetion " +
			       e.getMessage());
	}

	cname = getProperty("user.name");
	
	if (cname == null) {
	    return hostname;
	} else {
	    return cname + "@" + hostname;
	}
    }

    private static String getProperty(String prop) {

	String value = null;
	if ( jmfSecurity != null) {
	    try {
		if (jmfSecurity.getName().startsWith("jmf-security")) {
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.READ_PROPERTY);
		    m[0].invoke(cl[0], args[0]);
		} else if (jmfSecurity.getName().startsWith("internet")) {
		    PolicyEngine.checkPermission(PermissionID.PROPERTY);
		    PolicyEngine.assertPermission(PermissionID.PROPERTY);
		}
	    } catch (Throwable e) {
		if (JMFSecurityManager.DEBUG) {
		    System.err.println("Unable to get read property " +
				       " privilege  " + e);
		}
		jmfSecurity.permissionFailureNotification(JMFSecurity.READ_PROPERTY);
		// securityPrivelege = false;
	    }
	}

	try {
	    if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
		Constructor cons = jdk12PropertyAction.cons;
		value = (String) jdk12.doPrivM.invoke(
				     jdk12.ac,
				     new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               prop
                                           })});
	    } else {
		value = System.getProperty(prop);
	    }
	} catch (Throwable e) {
	}
	
	return value;
    }    
}
