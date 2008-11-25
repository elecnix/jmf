/*
 * @(#)JMFI18N.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.util.*;
import com.sun.media.util.locale.*;

public class JMFI18N {
    static public java.util.ResourceBundle bundle = null;
    static public java.util.ResourceBundle bundleApps = null;

    static public String getResource(String key){
	//$$ Testing - cania
	//$$ java.util.Locale.setDefault(Locale.GERMANY);
	Locale currentLocale = java.util.Locale.getDefault();
	
	if (bundle == null) {
	    try {
		bundle = java.util.ResourceBundle.getBundle("com.sun.media.util.locale.JMFProps", currentLocale);
	    } catch(java.util.MissingResourceException e){
		System.out.println("Could not load Resources");
		System.exit(0);
	    }
	    try {
		bundleApps = java.util.ResourceBundle.getBundle("com.sun.media.util.locale.JMFAppProps", currentLocale);
	    } catch (java.util.MissingResourceException me) {
	    }
	}
	String value = "";
        try {
            value = (String) bundle.getObject(key);
        } catch (java.util.MissingResourceException e) {
	    if (bundleApps != null) {
		try {
		    value = (String) bundleApps.getObject(key);
		} catch (java.util.MissingResourceException mre) {
		    System.out.println("Could not find " + key);
		}
	    } else
		System.out.println("Could not find " + key);
	}
        return value;
    }
}
    
