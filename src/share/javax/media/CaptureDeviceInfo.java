/*
 * @(#)CaptureDeviceInfo.java	1.12 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import javax.media.format.*;
import java.io.Serializable;

/**
 * A <CODE>CaptureDeviceInfo</CODE> object contains information about a particular capture device.
 * @since JMF 2.0
 */
public class CaptureDeviceInfo implements Serializable {

    /**
     * A <CODE>String</CODE> that contains the name of a device.
     */
    protected String name;
    /**
     * A <CODE>MediaLocator</CODE> that identifies a device. 
     */
    protected MediaLocator locator = null;
    /**
     * The output formats supported by a device.
     */
    protected Format [] formats = null;

    /**
     * Constructs a <CODE>CaptureDeviceInfo</CODE> object with the specified name, media
     * locator, and array of <CODE>Format</CODE> objects.
     * @param name A <CODE>String</CODE> that contains the name of the device.
     * @param locator The <CODE>MediaLocator</CODE> that uniquely specifies the device.
     * @param formats An array of the output formats supported by the device.
     */
    public CaptureDeviceInfo(String name, MediaLocator locator, Format [] formats) {
	this.name = name;
	this.locator = locator;
	this.formats = formats;
    }

    /**
     * Constructs a <CODE>CaptureDeviceInfo</CODE> object with null attributes.
     */
    public CaptureDeviceInfo() {
    }

    /**
     * Gets a list of the output formats supported by this device.
     * @return A <CODE>Format</CODE> array that contains the output formats supported by this device.
     */
    public Format [] getFormats() {
	return formats;
    }

    /**
     * Gets the <code>MediaLocator</code> needed to create a <code>DataSource</code>
     * for this device through the <code>Manager</code>. The <CODE>MediaLocator</CODE> 
     * is unique--no two devices can use the same locator.
     *
     * @return The <CODE>MediaLocator</CODE> that uniquely specifies this device.
     */
    public MediaLocator getLocator() {
	return locator;
    }

    /**
     * Gets the name of this device.
     * The name might include the device name, vendor name, and a version number.
     * @return A <CODE>String</CODE> that describes this device.
     */
    public String getName() {
	return name;
    }

    public boolean equals(Object obj) {
	if (!(obj instanceof CaptureDeviceInfo))
	    return false;
	CaptureDeviceInfo cdi = (CaptureDeviceInfo) obj;
	return
	    name != null && locator != null && formats != null &&
	    name.equals(cdi.getName()) &&
	    locator.equals(cdi.getLocator()) &&
	    formats.equals(cdi.getFormats());
    }

    public String toString() {
	String result = name + " : " + locator + "\n";
	if (formats != null) {
	    for (int i = 0; i < formats.length; i++) {
		result += formats[i] + "\n";
	    }
	}
	return result;
    }
}
