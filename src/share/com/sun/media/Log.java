/*
 * @(#)Log.java	1.10 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import com.sun.media.JMFSecurity;
import com.sun.media.JDK12Security;
import com.sun.media.IESecurity;
import com.sun.media.JMFSecurityManager;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;


/**
 * A public static class to generate and write to jmf.log.
 */
public class Log {

    public static boolean isEnabled = true;
    private static DataOutputStream log = null;
    private static String fileName = "jmf.log";
    private static int indent = 0;

    // Security
    private static JMFSecurity jmfSecurity = null;
    private static Method m[] = new Method[1];
    private static Class cl[] = new Class[1];
    private static Object args[][] = new Object[1][0];
    private static boolean ieSec = false;
    private static String permission = null;
    private static int permissionid = 0;


    static {

      // This is to guard against the log file being opened twice at
      // the same time.
      synchronized (fileName) {

       // Check so we won't run this twice.
       if (isEnabled && log == null) {

	Object llog = com.sun.media.util.Registry.get("allowLogging");

	// Check the registry file to see if logging is turned on.
	if (llog != null && llog instanceof Boolean) {
	    if (! ((Boolean)llog).booleanValue())
		isEnabled = false;
	}

	// Check security to open the file.
	if (isEnabled) {
	    try {
		jmfSecurity = JMFSecurityManager.getJMFSecurity();
		if (jmfSecurity == null) {
		    // allow log writing for applications.
		} else if (jmfSecurity.getName().startsWith("jmf-security")) {
		    permission = "write file";
		    permissionid = JMFSecurity.WRITE_FILE;
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.WRITE_FILE);
		    m[0].invoke(cl[0], args[0]);
		    
		    permission = "delete file";
		    permissionid = JMFSecurity.DELETE_FILE;
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.DELETE_FILE);
		    m[0].invoke(cl[0], args[0]);
		    permission = "read system property";
		    permissionid = JMFSecurity.READ_PROPERTY;
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.READ_PROPERTY);
		    m[0].invoke(cl[0], args[0]);
		} else if (jmfSecurity.getName().startsWith("internet")) {
		    PolicyEngine.checkPermission(PermissionID.FILEIO);
		    PolicyEngine.assertPermission(PermissionID.FILEIO);
		    ieSec = true;
		}
	    } catch (Exception e) { 
		isEnabled = false;
	    }
	}

	if (isEnabled) {
            isEnabled = false;
	    try {
		String dir;
		Object ldir = com.sun.media.util.Registry.get("secure.logDir");
		if (ldir != null && ldir instanceof String && !("".equals(ldir)))
		    dir = (String)ldir;
		else
		    dir = (String)System.getProperty("user.dir");

		String file = dir + File.separator + fileName;
		log = new DataOutputStream(new FileOutputStream(file));
		if (log != null) {
		    System.err.println("Open log file: " + file);
		    isEnabled = true;
		    writeHeader();
		}
	    } catch (Exception e) {
		System.err.println("Failed to open log file.");
	    }
	}

       } // Don't need to run this twise.
      }  // synchronized (fileName)
    }


    // Request write permission to the log file.
    private static synchronized boolean requestPerm() {
	try {
	    if (!ieSec) {
		permission = "write file";
		permissionid = JMFSecurity.WRITE_FILE;
		jmfSecurity.requestPermission(m, cl, args, JMFSecurity.WRITE_FILE);
		m[0].invoke(cl[0], args[0]);
	    } else {
		PolicyEngine.checkPermission(PermissionID.FILEIO);
		PolicyEngine.assertPermission(PermissionID.FILEIO);
	    }
	} catch (Exception e) { 
	    return false;
	}
	return true;
    }


    private static synchronized void writeHeader() {

	if (jmfSecurity != null && !requestPerm())
	    return;

	write("#\n# JMF " + BasicPlayer.VERSION + "\n#\n");

	String os = null, osver = null, osarch = null;
	String java = null, jver = null;
	try {
	    os = System.getProperty("os.name");	
	    osarch = System.getProperty("os.arch");	
	    osver = System.getProperty("os.version");	
	    java = System.getProperty("java.vendor");
	    jver = System.getProperty("java.version");	
	} catch (Throwable e) {
	    // Can't get the info.  No big deal.
	    return;
	}

	if (os != null)
	    comment("Platform: " + os + ", " + osarch + ", " + osver);
	if (java != null)
	    comment("Java VM: " + java + ", " + jver);
	write("");
    }


    public static synchronized void comment(Object str) {
	if (isEnabled) {

	    if (jmfSecurity != null && !requestPerm())
		return;

	    try {
		log.writeBytes("## " + str + "\n");
	    } catch (IOException e) {}
	}
    }


    public static synchronized void warning(Object str) {
	if (isEnabled) {

	    if (jmfSecurity != null && !requestPerm())
		return;

	    try {
		log.writeBytes("!! " + str + "\n");
	    } catch (IOException e) {}
	}
    }


    public static synchronized void profile(Object str) {
	if (isEnabled) {

	    if (jmfSecurity != null && !requestPerm())
		return;

	    try {
		log.writeBytes("$$ " + str + "\n");
	    } catch (IOException e) {}
	}
    }


    static boolean errorWarned = false;

    public static synchronized void error(Object str) {
	if (isEnabled) {

	    if (jmfSecurity != null && !requestPerm())
		return;

	    if (!errorWarned) {
		System.err.println("An error has occurred.  Check jmf.log for details.");
		errorWarned = true;
	    }

	    try {
		log.writeBytes("XX " + str + "\n");
	    } catch (IOException e) {}
	} else {
	    System.err.println(str);
	}
    }


    public static synchronized void dumpStack(Throwable e) {
	if (isEnabled) {

	    if (jmfSecurity != null && !requestPerm())
		return;

	    e.printStackTrace(new PrintWriter(log, true));
	    write("");
	} else
	    e.printStackTrace();
    }


    public static synchronized void write(Object str) {
	if (isEnabled) {

	    if (jmfSecurity != null && !requestPerm())
		return;

	    try {
		for (int i = indent; i > 0; i--)
		    log.writeBytes("    ");
		log.writeBytes(str + "\n");
	    } catch (IOException e) {}
	}
    }


    public static synchronized void setIndent(int i) {
	indent = i;
    }


    public static synchronized void incrIndent() {
	indent++;
    }


    public static synchronized void decrIndent() {
	indent--;
    }


    public static int getIndent() {
	return indent;
    }
}
