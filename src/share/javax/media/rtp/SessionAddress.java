/*
 * @(#)SessionAddress.java	1.12 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;

import java.net.InetAddress;
/**
 * Class to encapsulate a pair of internet address and a pair of ports
 * for use  in RTPSM methods.
 * <P> 
 */
public class SessionAddress implements java.io.Serializable {
    private InetAddress m_dataAddress;
    private InetAddress m_controlAddress;
    private int         m_dataPort;
    private int         m_controlPort;
    private int         ttl; // time to live, used in multicast sessions
    
    
    public static final int ANY_PORT = -1;
    
    /**
     * Constructor to create an SessionAddress given the data  internet
     * address and data port.   <P>
     *
     * @param dataAddress The data address.   <P>
     *
     * @param dataPort The data port. If this value is passed as the
     * constant  ANY_PORT, the object
     * will not represent a specific data port.  <P>
     */    
    public 
	SessionAddress( InetAddress dataAddress,
			int         dataPort) {        						      
	m_dataAddress = dataAddress;
	m_controlAddress = dataAddress;
	m_dataPort = dataPort;
	m_controlPort = dataPort + 1;
    }
    
    /**
     * Constructor to create an SessionAddress given the data  internet
     * address and data port.   <P>
     *
     * @param dataAddress The data address.   <P>
     *
     * @param dataPort The data port. If this value is passed as the
     * constant  ANY_PORT, the object
     * will not represent a specific data port.  <P>
     *
     * @param timeToLive The time-to-live parameter for multicast sessions <P>
     */        
    public 
	SessionAddress( InetAddress dataAddress,
			int         dataPort,
			int         timeToLive) {        						     
	m_dataAddress = dataAddress;
	m_controlAddress = dataAddress;
	m_dataPort = dataPort;
	m_controlPort = dataPort + 1;
	
	ttl= timeToLive;
    }    
    
    /**
     * Constructor to create an SessionAddress given both internet
     * address and ports.   <P>
     *
     * @param dataAddress The data address.   <P>
     *
     * @param dataPort The data port. If this value is passed as the
     * constant  ANY_PORT, the object
     * will not represent a specific data port.  <P>
     *
     * @param controlAddress The control address.  <P>
     *
     * @param controlPort The control port.  If this value is passed as
     * the  constant ANY_PORT, the
     * object will not represent a specific control port.  <P>
     *
     */    
    public 
	SessionAddress( InetAddress dataAddress,
			int         dataPort,
			InetAddress controlAddress,
			int         controlPort) {
	m_dataAddress = dataAddress;
	m_controlAddress = controlAddress;
	m_dataPort = dataPort;
	m_controlPort = controlPort;
    }
    /**
     * Constructor to create an "empty" RTPSesionAddress.  Equivalent
     * to calling SessionAddress(null, ANY_PORT, null, ANY_PORT).   <P>
     */
    public
	SessionAddress(){
	this(null, ANY_PORT, null, ANY_PORT);
    }
    
    /**
     * Returns the time-to-live value for mutlicast sessions.
     *
     */
    public int getTimeToLive() {
        return ttl;
    }    
    
    /**
     * Access method to get the data address of this SessionAddress.   <P>
     */
    public InetAddress
	getDataAddress(){
	return m_dataAddress;
    }
    
    /**
     * Access method to set the data address of this SessionAddress.   <P>
     */
    public void setDataHostAddress( InetAddress dataAddress) {
	m_dataAddress= dataAddress;
    }
    
    /**
     * Access method to set the data port of this SessionAddress.   <P>
     */
    public void setDataPort( int dataPort) {
	m_dataPort= dataPort;
    }
    
    /**
     * Returns the IP address string of the data address host.
     *
     */
    public String getDataHostAddress() {
        return m_dataAddress.getHostAddress();
    }
    /**
     * Access method to get the data port of this SessionAddress.   <P>
     */
    public int getDataPort(){
	return m_dataPort;
    }
    /**
     * Access method to get the control address of this SessionAddress.   <P>
     */
    public InetAddress getControlAddress(){
	return m_controlAddress;
    }
    
    /**
     * Access method to set the control address of this SessionAddress.   <P>
     */
    public void setControlHostAddress( InetAddress controlAddress) {
	m_controlAddress= controlAddress;
    }
    
    /**
     * Access method to set the control port of this SessionAddress.   <P>
     */
    public void setControlPort( int controlPort) {
	m_controlPort= controlPort;
    }    
    /**
     * Returns the IP address string of the control address host.
     *
     */
    public String getControlHostAddress() {
        return m_controlAddress.getHostAddress();
    }
    
    /**
     * Access method to get the control port of this SessionAddress.   <P>
     */
    public int getControlPort(){
	return m_controlPort;
    }
    public boolean equals(Object obj){
	if (obj instanceof SessionAddress){
	    SessionAddress otheraddr = (SessionAddress)obj;
	    InetAddress otherdest = otheraddr.getDataAddress();
	    InetAddress othercontl = otheraddr.getControlAddress();
	    int dport = otheraddr.getDataPort();
	    int cport = otheraddr.getControlPort();
	    if ( (otherdest.equals(m_dataAddress)) &&
		 (othercontl.equals(m_controlAddress)) &&
		 (dport == m_dataPort) &&
		 (cport == m_controlPort))
		return true;
	}
	return false;
    }
    public int hashCode(){
	return 1;
    }
    
    public String toString() {
	String s= "DataAddress: ";
	
	if( m_dataAddress != null)
	    s+= m_dataAddress.toString();
	else s+= "null";
	
	s+= "\nControlAddress: ";
	
	if( m_controlAddress != null)
	    s+= m_controlAddress.toString();
	else s+= "null";
	
	s+= ("\nDataPort: " + m_dataPort + "\nControlPort: " + m_controlPort);
	
	return s;
    }
}
