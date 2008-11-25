/*
 * @(#)PackageManager.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;
import java.util.Vector;
import java.lang.reflect.*;

/**
 * A <CODE>PackageManager</CODE> maintains a persistent store of
 * package-prefix lists. A package prefix specifies the
 * prefix for a complete class name. A factory uses
 * a package-prefix list to find a class that
 * might belong to any of the packages that are referenced
 * in the prefix list.<p>
 *
 * The <code>Manager</code> uses package-prefix lists
 * to find protocol handlers and content handlers for time-based
 * media.<p>
 *
 * The current version of a package-prefix list is obtained with
 * the <code>get&lt;package-prefix&gt;List</code> method. This method returns the prefix
 * list in use; any changes to the list take effect immediately.
 *
 * Unless it is made persistent with
 * <code>commit&lt;package-prefix&gt;List</code>, a package-prefix list is only valid 
 * while the <code>Manager</code> is referenced.
 * 
 * The <code>commit&lt;package-prefix&gt;List</code> method ensures that any changes made 
 * to a package-prefix list are still visible the next time that
 * the <code>Manager</code> is referenced.
 *
 * @see Manager
 * @version 1.6, 02/08/21.
 */


public class PackageManager {

    static private PackageManager pm = null;

    static private Method mGetProtocolPrefixList = null;
    static private Method mSetProtocolPrefixList = null;
    static private Method mCommitProtocolPrefixList = null;
    static private Method mGetContentPrefixList = null;
    static private Method mSetContentPrefixList = null;
    static private Method mCommitContentPrefixList = null;

    static final private Class [] sigNone = new Class[0];
    static final private Class [] sigVector = { Vector.class };
    
    /**
     * The package prefix used when searching for protocol
     * handlers.
     *
     * @see Manager
     */
    static private Vector protoPrefixList;

    static {
	// Default value
	protoPrefixList = new Vector();
	protoPrefixList.addElement("javax");
	protoPrefixList.addElement("com.sun");

	try {
	    Class pmClass = Class.forName("javax.media.pm.PackageManager");
	    if (pmClass != null) {
		Object tryPM = pmClass.newInstance();
		if (tryPM instanceof PackageManager) {
		    pm = (PackageManager) tryPM;

		    mGetProtocolPrefixList =
			getDeclaredMethod(pmClass, "getProtocolPrefixList",
						  sigNone);
		    mSetProtocolPrefixList =
			getDeclaredMethod(pmClass, "setProtocolPrefixList",
						  sigVector);
		    mCommitProtocolPrefixList =
			getDeclaredMethod(pmClass, "commitProtocolPrefixList",
						  sigNone);
		    mGetContentPrefixList =
			getDeclaredMethod(pmClass, "getContentPrefixList",
						  sigNone);
		    mSetContentPrefixList =
			getDeclaredMethod(pmClass, "setContentPrefixList",
						  sigVector);
		    mCommitContentPrefixList =
			getDeclaredMethod(pmClass, "commitContentPrefixList",
						  sigNone);
		}
	    }
	} catch (ClassNotFoundException cnfe) {
	    System.err.println(cnfe);
	} catch (InstantiationException ie) {
	    System.err.println(ie);
	} catch (IllegalAccessException iae) {
	    System.err.println(iae);
	} catch (SecurityException se) {
	    System.out.println("PackageManager: SecurityException: " + se);
	    System.err.println(se);
	} catch (NoSuchMethodException nsme) {
	}
    }

    /* Private method to invoke a method using reflection API */
    private static Object runMethod(Method m, Object [] params) {
	try {
	    return m.invoke(null, params);
	} catch (IllegalAccessException iae) {
	} catch (IllegalArgumentException iare) {
	} catch (InvocationTargetException ite) {
	}
	return null;
    }
    
    /**
     * Get the current value of the protocol package-prefix list.
     * <p>
     * @return The protocol package-prefix list.
     */
    static public Vector getProtocolPrefixList() {
	if (pm != null && mGetProtocolPrefixList != null) {
	    return (Vector) runMethod(mGetProtocolPrefixList, null);
	} else
	    return protoPrefixList;
    }

    /**
     * Set the protocol package-prefix list.
     * This is required for changes to take effect.
     *
     * @param list The new package-prefix list to use.
     */
    static public void setProtocolPrefixList(Vector list) {
	if (pm != null && mSetProtocolPrefixList != null) {
	    Object [] params = new Object[1];
	    params[0] = list.clone();
	    runMethod(mSetProtocolPrefixList, params);
	    protoPrefixList = getProtocolPrefixList();
	} else
	    protoPrefixList = (Vector)list.clone();
    }

    /**
     * Make changes to the protocol package-prefix list persistent.
     * <p>
     * This method throws a <code>SecurityException</code> if the calling thread
     * does not have access to system properties.
     *
     */
    static public void commitProtocolPrefixList() {
	if (pm != null && mCommitProtocolPrefixList != null) {
	    runMethod(mCommitProtocolPrefixList, null);
	}
    }

    /*************************************************************************
     * Content Prefix List
     *************************************************************************/

    /**
     * The package prefix used when searching for content
     * handlers.
     *
     * @see Manager
     */
    static private Vector contentPrefixList;

    static {
	// Default value
	contentPrefixList = new Vector();
	contentPrefixList.addElement("javax");
	contentPrefixList.addElement("com.sun");
    }

    /**
     * Get the current value of the content package-prefix list.
     * Any changes made to this list take effect immediately.
     * <p>
     *
     * @return The content package-prefix list.
     */
    static public Vector getContentPrefixList() {
	if (pm != null && mGetContentPrefixList != null) {
	    return (Vector) runMethod(mGetContentPrefixList, null);
	} else
	    return contentPrefixList;
    }

    /**
     * Set the current value of the content package-prefix list.
     * This is required for changes to take effect.
     *
     * @param list The content package-prefix list to set.
     */
    static public void setContentPrefixList(Vector list) {
	if (pm != null && mSetContentPrefixList != null) {
	    Object [] params = new Object[1];
	    params[0] = list.clone();
	    runMethod(mSetContentPrefixList, params);
	    contentPrefixList = getContentPrefixList();
	} else
	    contentPrefixList = (Vector)list.clone();
    }
    
    /**
     * Make changes to the content prefix-list persistent.
     * <p>
     * This method throws a <code>SecurityException</code> if the calling thread
     * does not have access to system properties.
     *
     */
    static public void commitContentPrefixList() {
	if (pm != null && mCommitContentPrefixList != null) {
	    runMethod(mCommitContentPrefixList, null);
	}
    }

    /**
     * netscape browser throws SecurityException when you call
     * Class.getDeclaredMethod or Class.getDeclaredMethods
     * There is a privilege called UniversalMemberAccess which should
     * allow this call. But Netscape has disabled this privilege; they
     * mention this in in their security faq. So, we call getMethod and
     * using getDeclaringClass method, check to see if it the subclass
     * overrided it or not.
     * Note: this code is used in all cases, not just netscape
     */

    static Method getDeclaredMethod(Class c, String name, Class[] params)
	throws NoSuchMethodException {
	
	Method m = c.getMethod(name, params);
	if (m.getDeclaringClass() == c) {
	    return m;
	} else {
	    return null;
	}
    }
}
