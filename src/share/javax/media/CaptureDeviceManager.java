/*
 * @(#)CaptureDeviceManager.java	1.16 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import javax.media.format.*;
import java.util.Vector;
import java.lang.reflect.*;
import java.io.IOException;

/**
 * <CODE>CaptureDeviceManager</CODE> is a manager class that provides access to
 * a list of the capture devices available on a system.
 * <CODE>CaptureDeviceManager</CODE> uses a registry and query mechanisms
 * to locate devices and return <CODE>CaptureDeviceInfo</CODE> objects for available
 * devices. The <CODE>CaptureDeviceManager</CODE> is also used to register new capture devices.
 * @since JMF 2.0
 */

public class CaptureDeviceManager {

    private static CaptureDeviceManager cdm = null;

    private static Method mGetDeviceList = null;
    private static Method mGetDevice = null;
    private static Method mAddDevice = null;
    private static Method mRemoveDevice = null;
    private static Method mCommit = null;

    static {
	// Look for javax.media.cdm.CaptureDeviceManager
	try {
	    Class classCDM = Class.forName("javax.media.cdm.CaptureDeviceManager");
	    if (classCDM != null) {
		Object tryCDM = classCDM.newInstance();
		if (tryCDM instanceof CaptureDeviceManager) {
		    cdm = (CaptureDeviceManager) tryCDM;
 		    mGetDeviceList =
 			PackageManager.getDeclaredMethod(classCDM,
                              "getDeviceList",
			       new Class[] {Format.class});

 		    mGetDevice =
 			PackageManager.getDeclaredMethod(classCDM,
                              "getDevice",
			       new Class[] {String.class});

 		    mCommit =
 			PackageManager.getDeclaredMethod(classCDM,
                              "commit", null);

 		    mAddDevice =
 			PackageManager.getDeclaredMethod(classCDM,
                              "addDevice",
			       new Class[] {CaptureDeviceInfo.class});
					       
 		    mRemoveDevice =
 			PackageManager.getDeclaredMethod(classCDM,
                              "removeDevice",
			       new Class[] {CaptureDeviceInfo.class});
		}
	    }
	} catch (ClassNotFoundException cnfe) {
	    System.err.println(cnfe);
	} catch (InstantiationException ie) {
	    System.err.println(ie);
	} catch (IllegalAccessException iae) {
	    System.err.println(iae);
	} catch (SecurityException se) {
	    System.err.println(se);
	} catch (NoSuchMethodException e) {
	    System.err.println(e);
	}
    }

    private static Object runMethod(Method m, Object [] params) {
	try {
	    return m.invoke(null, params);
	} catch (IllegalAccessException iae) {
	    System.err.println(iae);
	} catch (IllegalArgumentException iare) {
	    System.err.println(iare);
	} catch (InvocationTargetException ite) {
	    System.err.println(ite);
	}
	return null;
    }

    /**
     * Gets a <CODE>CaptureDeviceInfo</CODE> object that corresponds to the specified device.
     * @param deviceName A <CODE>String</CODE> that contains the name of the device for 
     * which you want to get a <CODE>CaptureDeviceInfo</CODE> object.
     * For example: "SunVideo". 
     * @return A <CODE>CaptureDeviceInfo</CODE> object that corresponds to the specified device name.
     * Returns null if the specified device could
     * not be found.
     */
    public static CaptureDeviceInfo getDevice(java.lang.String deviceName) {
	if (cdm != null && mGetDevice != null) {
	    Object params[] = new Object[1];
	    params[0] = deviceName;
	    return (CaptureDeviceInfo) runMethod(mGetDevice, params);
	} else
	    return null;
    }

    /**
     * Gets a list of <CODE>CaptureDeviceInfo</CODE> objects that correspond to devices 
     * that can capture data in the specified <CODE>Format</CODE>. 
     * If no <CODE>Format</CODE> is specified, this method  returns a list of  
     * <CODE>CaptureDeviceInfo</CODE> objects for all of the available capture devices.
     * 
     * @return A <CODE>Vector</CODE> that contains <CODE>CaptureDeviceInfo</CODE> objects 
     * for the devices that support the specified <CODE>Format</CODE>.
     */
    public static Vector getDeviceList(Format format) {
	if (cdm != null && mGetDeviceList != null) {
	    Object params[] = new Object[1];
	    params[0] = format;
	    Vector returnVal = (Vector) runMethod(mGetDeviceList, params);
	    if (returnVal == null)
		return new Vector(1);
	    else
		return returnVal;
	} else
	    return new Vector(1);
    }

    /**
     * Adds a <CODE>CaptureDeviceInfo</CODE> object for a new capture device
     * to the list of devices maintained by the
     * <CODE>CaptureDeviceManager</CODE>. This information is not
     * stored permanently in any registry unless <CODE>commit</CODE> is called.
     * @param newDevice A <CODE>CaptureDeviceInfo</CODE> object that identifies the new device.
     * @see #commit
     * @return <CODE>true</CODE> if the object is added successfully, <CODE>false</CODE> if it is not.
     */
    public static boolean addDevice(CaptureDeviceInfo newDevice) {
	if (cdm != null && mAddDevice != null) {
	    Object params[] = new Object[1];
	    params[0] = newDevice;
	    Object result = runMethod(mAddDevice, params);
	    if (result != null)
		return ((Boolean)result).booleanValue();
	    else
		return false;
	} else
	    return false;
    }

    /**
     * Removes a <CODE>CaptureDeviceInfo</CODE> object from the list of devices maintained by the
     * <CODE>CaptureDeviceManager</CODE>. The change is not
     * stored permanently in any registry unless <CODE>commit</CODE> is called.
     * @param device A <CODE>CaptureDeviceInfo</CODE> object that identifies the device to remove.
     * @see #commit
     * @return <CODE>true</CODE> if the object is removed successfully, <CODE>false</CODE> if it is not.
     */
    public static boolean removeDevice(CaptureDeviceInfo device) {
	if (cdm != null && mRemoveDevice != null) {
	    Object params[] = new Object[1];
	    params[0] = device;
	    Object result = runMethod(mRemoveDevice, params);
	    if (result != null)
		return ((Boolean)result).booleanValue();
	    else
		return false;
	} else
	    return false;
    }

    /**
     * Permanently stores information about the list of devices in the registry.
     * Commit must be called to save changes made to the device list by calling
     * <CODE>addDevice</CODE> or <CODE>removeDevice</CODE>.
     * @exception IOException If the registry could not be committed to disk due
     * to an IO error.
     */
    public static void  commit() throws IOException {
	if (cdm != null && mCommit != null) {
	    runMethod(mCommit, null);
	}
    }
}


