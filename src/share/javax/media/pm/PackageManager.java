/*
 * @(#)PackageManager.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.pm;

import com.sun.media.util.Registry;
import java.util.Vector;
import java.io.IOException;



public final class PackageManager extends javax.media.PackageManager {

    static private Vector protocolPrefixList = null;
    static private Vector contentPrefixList = null;

    static private Registry r = null;

    static {
	// Set Defaults
	protocolPrefixList = new Vector();	
	protocolPrefixList.addElement("javax");
	protocolPrefixList.addElement("com.sun");
	protocolPrefixList.addElement("com.ibm");
	
	contentPrefixList = new Vector();
	contentPrefixList.addElement("javax");
	contentPrefixList.addElement("com.sun");
	contentPrefixList.addElement("com.ibm");
	
	try {
	    r = new Registry();
	    Vector temp = (Vector) r.get("PM.protocolPrefixList");
	    if (temp != null)
		protocolPrefixList = temp;
	    temp = (Vector) r.get("PM.contentPrefixList");
	    if (temp != null)
		contentPrefixList = temp;
	} catch (Exception e) {
	    System.err.println("PackageManager: error reading registry");
	}
    }

    /*************************************************************************
     * Protocol Prefix List
     *************************************************************************/
    
    public synchronized static Vector getProtocolPrefixList() {
	return protocolPrefixList;
    }

    public synchronized static void setProtocolPrefixList(Vector list) {
	protocolPrefixList = list;
	if (!protocolPrefixList.contains("javax"))
	    protocolPrefixList.addElement("javax");
    }

    public synchronized static void commitProtocolPrefixList() {
	if (r != null) {
	    r.set("PM.protocolPrefixList", protocolPrefixList);
	    try {
		r.commit();
	    } catch (IOException ioe) {
		System.err.println("Could not commit protocolPrefixList");
	    }
	}
    }

    /*************************************************************************
     * Content Prefix List
     *************************************************************************/
    
    public synchronized static Vector getContentPrefixList() {
	return contentPrefixList;
    }

    public synchronized static void setContentPrefixList(Vector list) {
	contentPrefixList = list;
	if (!contentPrefixList.contains("javax"))
	    contentPrefixList.addElement("javax");
    }

    public synchronized static void commitContentPrefixList() {
	if (r != null) {
	    r.set("PM.contentPrefixList", contentPrefixList);
	    try {
		r.commit();
	    } catch (IOException ioe) {
		System.err.println("Could not commit contentPrefixList");
	    }
	}
    }
}
