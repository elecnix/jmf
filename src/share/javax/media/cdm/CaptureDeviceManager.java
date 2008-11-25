/*
 * @(#)CaptureDeviceManager.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.cdm;

import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;
import javax.media.*;
import javax.media.format.*;
import com.sun.media.util.Registry;

/**
 * This is a manager class that fetches a list of devices available
 * on the system. It uses a registry mechanism and/or query mechanism
 * to locate devices and return CaptureDeviceInfo objects for each available
 * device. The CaptureDeviceManager can also be used to register new devices
 * by adding a device and calling commit().<P>
 * @since JMF 2.0
 */

public class CaptureDeviceManager extends javax.media.CaptureDeviceManager {

    static Vector devices = null;
    
    static {
	readFromRegistry();
    }

    public CaptureDeviceManager() {
	readFromRegistry();
    }
    
    /**
     * Returns a list of CaptureDeviceInfo objects for each of the registered devices.
     * @return a list of CaptureDeviceInfo objects for each of the registered devices.
     */
    public static synchronized Vector getDeviceList() {
	return getDeviceList(null);
    }

    /**
     * Returns the CaptureDeviceInfo corresponding to the specified name of the
     * device. For example: "SunVideo". Returns null if the device could
     * not be found.
     * @return the CaptureDeviceInfo corresponding to the specified name of theo     * device.
     */
    public static synchronized CaptureDeviceInfo getDevice(java.lang.String deviceName) {
	if (devices == null)
	    return null;
	
	Enumeration enum = devices.elements();
	while (enum.hasMoreElements()) {
	    CaptureDeviceInfo cdi = (CaptureDeviceInfo) enum.nextElement();
	    if (cdi.getName().equals(deviceName))
		return cdi;
	}
	
	return null;
    }

    /**
     * Returns a list of CaptureDeviceInfo objects corresponding to devices that can capture
     * data with the specified data format. If format is null then it returns all registered
     * objects
     * @return a list of  CaptureDeviceInfo objects for the given data format.
     */
    public static synchronized Vector getDeviceList(Format format) {
	if (devices == null)
	    return null;

	if (format == null)
	    return devices;
	
	Vector newList = new Vector();
	
	Enumeration enum = devices.elements();
	while (enum.hasMoreElements()) {
	    CaptureDeviceInfo cdi = (CaptureDeviceInfo) enum.nextElement();
	    Format [] formats = cdi.getFormats();
	    for (int i = 0; i < formats.length; i++) {
		if (formats[i].matches(format)) {
		    newList.addElement(cdi);
		    break;
		}
	    }
	}
	
	if (newList.size() > 0)
	    return newList;
	else
	    return null;
    }

    /**
     * Adds a CaptureDeviceInfo object to the list. This information is not
     * stored permanently in any registry. If a similar CaptureDeviceInfo
     * object is already registered, then the new device is not added.
     * @see #commit
     * @return true if the object could be added, false otherwise.
     */
    public static synchronized boolean addDevice(CaptureDeviceInfo newDevice) {
	if (newDevice != null) {
	    if (devices == null)
		devices = new Vector();
	    else {
		Enumeration enum = devices.elements();
		while (enum.hasMoreElements()) {
		    CaptureDeviceInfo cdi = (CaptureDeviceInfo) enum.nextElement();
		    if (cdi.equals(newDevice)) {
			return false;
		    }
		}
	    }
	    
	    devices.addElement(newDevice);
	    return true;
	}
	
	return false;
    }

    /**
     * Removes a CaptureDeviceInfo object from the list.
     * @see #commit
     */
    public static synchronized boolean removeDevice(CaptureDeviceInfo device) {
	if ( devices == null ||
	     device == null ||
	     device.getName() == null)
	    return false;

	Enumeration enum = devices.elements();
	while (enum.hasMoreElements()) {
	    CaptureDeviceInfo cdi = (CaptureDeviceInfo) enum.nextElement();
	    if (cdi == device) {
		devices.removeElement(cdi);
		return true;
	    }
	}
	
	return false;
    }

    /**
     * Permanently store information about the list of devices in a registry.
     * @return false if the commit fails.
     */
    public static synchronized void commit() throws IOException {
	Registry r = new Registry();

	r.removeGroup("CDM");

	if (devices == null) {
	    r.set("CDM.nDevices", new Integer(0));
	} else {
	    r.set("CDM.nDevices", new Integer(devices.size()));

	    int i = 0;
	    Enumeration enum = devices.elements();
	    while (enum.hasMoreElements()) {
		CaptureDeviceInfo cdi = (CaptureDeviceInfo) enum.nextElement();
		r.set("CDM." + i, cdi);

		i++;
	    }
	}
	try {
	    r.commit();
	} catch (Exception e) {
	    System.err.println("Exception on commit = " + e);
	}
    }

    private static synchronized void readFromRegistry() {
	if (devices != null)
	    return;
	devices = new Vector();
	Registry r = new Registry();
	Object size = r.get("CDM.nDevices");
	if (size == null || ((Integer)size).intValue() == 0)
	    return;
	for (int i = 0; i < ((Integer)size).intValue(); i++) {
	    Object cdi = r.get("CDM." + i);
	    if (cdi != null && cdi instanceof CaptureDeviceInfo)
		devices.addElement(cdi);
	}
    }
}


