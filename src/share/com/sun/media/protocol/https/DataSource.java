/*
 * @(#)DataSource.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.https;

import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.io.IOException;
import java.lang.reflect.Method;
import javax.media.protocol.SourceCloneable;
import com.sun.media.JMFSecurityManager;

public class DataSource extends com.sun.media.protocol.DataSource implements SourceCloneable {

    private static SecurityManager securityManager;

    static {
	    securityManager = System.getSecurityManager();

	    // IE and Netscape browsers support https
	    // TODO: See if you need to check version # of IE and Netscape
            // to determine if https is supported.
	    // Netscape Browser?
	    boolean netscape = false;
	    boolean ie = false;
	    boolean msjvm = false;

	    String javaVendor =
		System.getProperty("java.vendor", "Sun").toLowerCase();
	    
	    if (javaVendor.indexOf("icrosoft") > 0) { // microsoft JVM
		msjvm = true;
	    }


	    if (securityManager != null) {
		netscape = (securityManager.toString().indexOf("netscape") != -1);
		ie = (securityManager.toString().indexOf("com.ms.security") != -1);
	    }


	    if (ie || msjvm) {
		try {
		    Class clsFactory = 
			Class.forName("com.ms.net.wininet.WininetStreamHandlerFactory");
		    // Note that Class.forName will either work or throw and Exception,
		    // it will not return null.
		    if ( clsFactory != null ) {
			URL.setURLStreamHandlerFactory(
						       (URLStreamHandlerFactory)clsFactory.newInstance());
		    }
		} catch (Throwable t) {
		}
	    } else if (!netscape) {
		// 1) Need JDK1.2 or higher. 2) JSSE should have been installed
	
		if (! JMFSecurityManager.isJDK12() ) {
		    throw new UnsatisfiedLinkError("Fatal Error: DataSource for https protocol needs JDK1.2 or higher VM");
		}
		try {
		    Class sslproviderC = Class.forName("com.sun.net.ssl.internal.ssl.Provider");
		    
		    
		    Object provider = sslproviderC.newInstance();

		    Class securityC = Class.forName("java.security.Security");
		    Class providerC = Class.forName("java.security.Provider");
		    Class systemC = Class.forName("java.lang.System");

		    Method addProviderM = securityC.getMethod("addProvider",
							new Class[] {
			                                  providerC
		                                        });

		    Method setPropertyM = systemC.getMethod("setProperty",
							new Class[] {
			                                  String.class,
			                                  String.class,
		                                        });
		    
		    if ( ( addProviderM != null) && (setPropertyM != null) ) {
			addProviderM.invoke(securityC, new Object[] {provider});
			setPropertyM.invoke(systemC, new Object[] {
                                           "java.protocol.handler.pkgs",
                                           "com.sun.net.ssl.internal.www.protocol"
			                   });
		    }

		} catch (Exception e) {
		    throw new UnsatisfiedLinkError("Fatal Error:Java Secure Socket Extension classes are not present");
		} catch (Error e) {
		    throw new UnsatisfiedLinkError("Fatal Error:Java Secure Socket Extension classes are not present");

		}
	    }
    }

    public javax.media.protocol.DataSource createClone() {
	DataSource ds = new com.sun.media.protocol.https.DataSource();
	ds.setLocator(getLocator());
	if (connected) {
	    try {
		ds.connect();
	    } catch (IOException e) {
		return null;
	    }
	}
	return ds;
    }
}
