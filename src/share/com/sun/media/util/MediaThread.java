/*
 * @(#)MediaThread.java	1.17 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;


import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;
import com.sun.media.NetscapeSecurity;
import com.sun.media.JDK12Security;
import com.sun.media.IESecurity;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import java.security.*;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;

import com.sun.media.BasicPlayer; // $$$$ REMOVE

/**
 *  Note: In netscape, if the user doesn't give Thread priority, you
 *  will not be able to reduce the priority also. All threads
 *  will run under same priority.
/**
 * A thread class where all JMF created threads should based on.
 */
public class MediaThread extends Thread {

    private static ThreadGroup threadGroup;
    static boolean securityPrivilege = true;
    private static final boolean debug = false;

    private static int controlPriority = Thread.MAX_PRIORITY - 1;
    private static int audioPriority = Thread.MAX_PRIORITY - 5;
    /* To be less than the Appletpriority */
    private static int videoPriority = Thread.NORM_PRIORITY - 2;
    private static int networkPriority = audioPriority + 1;
    private static int videoNetworkPriority = networkPriority - 1;

    // If you don't have threadgroup and thread permissions.
    private static int defaultMaxPriority = 4;

    static {
	JMFSecurity jmfSecurity = null;
	Method m[] = new Method[1];
	Class cl[] = new Class[1];
	Object args[][] = new Object[1][0];

	try {
	    // If you dont get thread and threadgroup access,
	    // using default thread group (null), else
	    // use root thread group
	    jmfSecurity = JMFSecurityManager.getJMFSecurity();
	    if ( jmfSecurity != null ) {
		if (jmfSecurity.getName().startsWith("jmf-security")) {
		    boolean haveBoth = true;

		    defaultMaxPriority = Thread.currentThread().getPriority();
		    try {
			jmfSecurity.requestPermission(m, cl, args, JMFSecurity.THREAD);
			m[0].invoke(cl[0], args[0]);
		    } catch (Throwable t) {
			jmfSecurity.permissionFailureNotification(JMFSecurity.THREAD);
			haveBoth = false;
		    }
		    if (haveBoth) {
			defaultMaxPriority =
			    Thread.currentThread().getThreadGroup().getMaxPriority();
			
			// System.out.println(" $$$ Now setting defaultMaxPriority as the current threads MAX priority which is " + defaultMaxPriority);
		    }

		    try {
			jmfSecurity.requestPermission(m, cl, args, JMFSecurity.THREAD_GROUP);
			m[0].invoke(cl[0], args[0]);
		    } catch (Throwable t) {
			jmfSecurity.permissionFailureNotification(JMFSecurity.THREAD_GROUP);
			haveBoth = false;
		    }
		    if (!haveBoth) {
			throw new Exception("No thread and or threadgroup permission");
		    }
		} else if (jmfSecurity.getName().startsWith("internet")) {
		    PolicyEngine.assertPermission(PermissionID.THREAD);
		} else if (jmfSecurity.getName().startsWith("jdk12")) {
		    Constructor cons = jdk12Action.getCheckPermissionAction();

		    defaultMaxPriority = Thread.currentThread().getPriority();
		    jdk12.doPrivContextM.invoke(
					 jdk12.ac,
					 new Object[] {
			    cons.newInstance(
					 new Object[] {
			                     JDK12Security.getThreadPermission()
                                         }),
				jdk12.getContextM.invoke(null, null)
  		       });

		    defaultMaxPriority =
			Thread.currentThread().getThreadGroup().getMaxPriority();


		    jdk12.doPrivContextM.invoke(
					 jdk12.ac,
					 new Object[] {
			cons.newInstance(
					 new Object[] {
			                     JDK12Security.getThreadGroupPermission()
                                         }),
			    jdk12.getContextM.invoke(null, null)
		    });

		} else if (jmfSecurity.getName().startsWith("default")) {
		    // TODO: even if class loader is not null
		    // we should check to see if the classes are signed
		    if (MediaThread.class.getClassLoader() != null) {
			// jmf from server
			throw new SecurityException();
		    }
		}

	    }
	} catch (Throwable e) {
	    // System.err.println("Permission to manipulate threads and/or thread groups not granted " + e + " : " + e.getMessage());
            securityPrivilege=false;
	    // System.out.println("defaultMaxPriority is " + defaultMaxPriority);

	    // TODO: tweak these based on testing
	    controlPriority = defaultMaxPriority;
	    audioPriority = defaultMaxPriority;
	    videoPriority = defaultMaxPriority - 1;
	    networkPriority = defaultMaxPriority;
	    videoNetworkPriority = defaultMaxPriority;

	    // TODO: Do the right thing if permissions cannot be obtained.
	    // User should be notified via an event
	}

        if (securityPrivilege) {
	    threadGroup = getRootThreadGroup();
	    // System.out.println("threadGroup is " + threadGroup);
        }
        else {
	    threadGroup = null;
	    // System.out.println("threadGroup is null");
        }
	
    }
    
    
    static private ThreadGroup getRootThreadGroup() {
	ThreadGroup current = null;
	try {
	    current = Thread.currentThread().getThreadGroup();
	    ThreadGroup g = current;
	    for (; g.getParent() != null; g = g.getParent());
	    // System.out.println("Root threadgroup is " + g);
	    return g;
	} catch (Exception e) {
	    return null; // current
	} catch (Error e) {
	    return null; // current;
	}
    }

    public MediaThread() {
	this("JMF thread");
    }
    
    public MediaThread(String name) {
	super(threadGroup, name);
    }
    
    
    public MediaThread(Runnable r) {
	this(r, "JMF thread");
    }
    
    public MediaThread(Runnable r, String name) {
	super(threadGroup, r,  name);
    }
    
    
    /**
     * This should be used for Manager, events threads etc. -- the mechanism
     * to maintain the players.
     */
    public void useControlPriority() {
	usePriority(controlPriority);
    }
    
    /**
     * This should be used for threads handling the audio medium.
     */
    public void useAudioPriority() {
	usePriority(audioPriority);
    }
    
    /**
     * This should be used for threads handling the video medium.
     */
    public void useVideoPriority() {
	usePriority(videoPriority);
    }
    
    /**
     * This should be used for threads handling network packets. e.g. RTP
     */
    public void useNetworkPriority() {
	usePriority(networkPriority);
    }

    public void useVideoNetworkPriority(){
	usePriority(videoNetworkPriority);
    }


    public static int getControlPriority() {
	return controlPriority;
    }

    public static int getAudioPriority() {
	return audioPriority;
    }

    public static int getVideoPriority() {
	return videoPriority;
    }

    public static int getNetworkPriority() {
	return networkPriority;
    }

    public static int getVideoNetworkPriority() {
	return videoNetworkPriority;
    }


    private void usePriority(int priority) {
	try {
	    setPriority(priority);
	} catch (Throwable t) {
	}
	if (debug) {
	    checkPriority("priority",
			  priority,
			  securityPrivilege, getPriority());
	}
    }

    private void checkPriority(String name, int ask, boolean priv, int got) {
	if (ask != got) {
	    System.out.println("MediaThread: " + name + " privilege? " + priv + "  ask pri: " + ask + " got pri:  " + got);
	}
    }
}




