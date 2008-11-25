/*
 * @(#)JMFSecurityManager.java	1.31 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.util.Date;
import java.lang.reflect.Method;
public class JMFSecurityManager {

    private static JMFSecurity security = null;
    private static JMFSecurity enabledSecurity = null;
    private static SecurityManager securityManager;
    private static int count = 0;
    public static final boolean DEBUG = false;

    // TIMEBOMB CODE HAS BEEN DISABLED FOR FCS

    private static boolean jdk12 = false;
    static {
// 	Date today = new Date();
// 	// System.out.println("Today is " + today);
// 	Date expire = new Date(99, 11, 25); // 11 ==> December

//    	{ // TESTING BLOCK: UNCOMMENT AND MODIFIY 'today' TO VERIFY THAT
//            // TIME BOMB CODE WORKS PROPERLY
//    	    today = new Date(99, 11, 23); // dec 23
//  	    System.out.println("Today is set to " + today);
//    	}

// 	long currentTime = today.getTime();
// 	long expireTime = expire.getTime();


	// Note: I am doing this for applications too.
	// They will get a warning which informs them to upgrade to FCS

// 	if (currentTime > expireTime) {
	if (false) {
	    System.err.println("WARNING: jmf2.0 beta license has expired. Please install jmf2.0 FCS");
	    security = DisabledSecurity.security;
	} else {
	    securityManager = System.getSecurityManager();
	    
	    boolean jdk11=false;
	    boolean msjvm=false;
	    
	    
	    try {
		String javaVersion = System.getProperty("java.version");
		
		//System.out.println("javaVersion "+javaVersion);
		
		if (!javaVersion.equals(""))  {
		    // verify that the version string starts with a number
		    /*
		      char c = javaVersion.charAt(0);
		      if ( (c >= '0') && (c <= '9') ) { // string starts with a number
		      if (javaVersion.compareTo("1.2") < 0)  {
		      jdk11 = true;
		      }
		      else  {
		      jdk12 = true;
		      }
		      }
		    */
		    if (javaVersion.startsWith("1.1")) {
			if (DEBUG)
			    System.out.println("JDK 1.1.x");
			jdk11 = true;
		    }
		    else {
			char c = javaVersion.charAt(0);
			// string starts with a number
			if ( (c >= '0') && (c <= '9') ) { 
			    if (javaVersion.compareTo("1.2") >= 0)  {
				if (DEBUG)
				    System.out.println("JDK 1.2 and up");
				jdk12 = true;
			    }
			}
		    }
		}
		
		String javaVendor =
		    System.getProperty("java.vendor", "Sun").toLowerCase();
		
		if (javaVendor.indexOf("icrosoft") > 0) { // microsoft JVM
		    msjvm = true;
		}
		
		
	    } catch (Throwable t) {
		System.out.println(t);
	    }
	    
	    

	    // TODO: For FCS, cleanup this if block -- handle jdk1.2 and up
	    // as a separate case no matter what the security manager
	    if (securityManager != null) { // check the security manager
		if (DEBUG)
		    System.out.println(securityManager);
		
		if ( securityManager.toString().indexOf("netscape") != -1 ) {
		    // Netscape's security manager
		    security = NetscapeSecurity.security;
		} else if ( ( securityManager.toString().indexOf("com.ms.security") != -1 ) || msjvm ){
		    // Internet Explorer security manager
		    security = IESecurity.security;
// 		    if (JMFSecurityManager.class.getClassLoader() == null) {
// 			security = IESecurity.security;
// 		    } else {
// 			// TODO For FCS: Handle remote with cab case.
// 			// Remote classes.
// 			security = DisabledSecurity.security;
// 		    }
		    //} else if ( ((securityManager.toString().indexOf("Applet")) != -1) &&
		} else if ( (securityManager.toString().indexOf("sun.applet.AppletSecurity") != -1) ||
			    (securityManager.toString().indexOf("sun.plugin.ActivatorSecurityManager") != -1) ) {
		    // appletviewer
		    if (jdk11) { // JDK 1.1
			//securityManager = null;
			//security = null;
			security = com.sun.media.DefaultSecurity.security;
		    }
		    
		    if (jdk12) { // verify that the version is 1.2 and up
			//securityManager = null;
			security = JDK12Security.security;
		    }
		    
		} else if ( securityManager.toString().indexOf("java.lang.SecurityManager") != -1) {
		    
		    // JDk 1.2 security manager
		    if (jdk12) { // verify that the version is 1.2 and up
			//securityManager = null;
			security = JDK12Security.security;
		    }
		}
		
		else { // TODO
		    if (jdk12) {
			security = JDK12Security.security;
		    } else
			security = com.sun.media.DefaultSecurity.security;
		}
	    }
	    
	    else {  // no security manager
		// 	    if (msjvm) { // jview (for now act like Internet Explorer security manager)
		// 		security = IESecurity.security;
		//                 IESecurity.jview = true;
		// 	    }
	    }
	    if ( DEBUG && (security != null) ) {
		System.out.println("Security: " + security.getName());
	    }
	    
	    
	    enabledSecurity=security;
	    
	    //        if ( (security != null) &&
	    // 	    security.getName().endsWith("netscape") ) {
	    // 	   try {
	    // 	       Method m[] = new Method[1];
	    // 	       Class cl[] = new Class[1];
	    // 	       Object args[][] = new Object[1][0];
	    
	    // 	       security.requestPermission(m, cl, args, JMFSecurity.READ_PROPERTY);
	    // 	       m[0].invoke(cl[0], args[0]);
	    // 	   } catch (Throwable t) {
	    // 	       security.permissionFailureNotification(JMFSecurity.READ_PROPERTY);
	    // 	   }
	    //        }
	    // sun.applet.AppletSecurity
	}
    }
	



    public static JMFSecurity getJMFSecurity() throws SecurityException {
	return security;
    }

    public static boolean isLinkPermissionEnabled() {
	if (security == null)
	    return true;
	else {
	    return security.isLinkPermissionEnabled();
	}
    }

    public static void loadLibrary(String name) throws UnsatisfiedLinkError {
	try {
	    JMFSecurity s = getJMFSecurity();
	    if (s != null) {
		s.loadLibrary(name);
	    } else {
		System.loadLibrary(name);
	    }
	} catch (Throwable t) {
	    throw new UnsatisfiedLinkError("JMFSecurityManager: " + t);
	}
    }


    public static synchronized void disableSecurityFeatures() {
       security=DisabledSecurity.security;
       count++;       
    }

    public static synchronized void enableSecurityFeatures() {
      count--;
      if (count <= 0)
	security=enabledSecurity;
    }

    private static final String STR_NOPERMCAPTURE =
                   "No permission to capture from applets";
    private static final String STR_NOPERMFILE =
                   "No permission to write files from applets";

    public static void checkCapture() {
	// If its not an applet, no need to check permissions
	if (security == null)
	    return;
	// Check the secure.... flag if its true
	Object captureFromApplets =
	    com.sun.media.util.Registry.get("secure.allowCaptureFromApplets");
	if (captureFromApplets == null ||
	    !(captureFromApplets instanceof Boolean) ||
	    ((Boolean)captureFromApplets).booleanValue() == false)
	    throw new RuntimeException(STR_NOPERMCAPTURE);
    }

    public static void checkFileSave() {
	// If its not an applet, no need to check permissions
	if (security == null)
	    return;
	// Check the secure.... flag if its true
	Object saveFromApplets =
	    com.sun.media.util.Registry.get("secure.allowSaveFileFromApplets");
	if (saveFromApplets == null ||
	    !(saveFromApplets instanceof Boolean) ||
	    ((Boolean)saveFromApplets).booleanValue() == false)
	    throw new RuntimeException(STR_NOPERMFILE);
    }

    public static boolean isJDK12() {
	return jdk12;
    }
}
