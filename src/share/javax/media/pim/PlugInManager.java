/*
 * @(#)PlugInManager.java	1.53 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.pim;

import java.lang.reflect.Method;
import javax.media.Format;
import javax.media.*;
import com.sun.media.util.Registry;
import com.sun.media.util.Resource;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.*;
import javax.media.protocol.ContentDescriptor;
import com.sun.media.util.DynamicPlugIn;

/**
 * This is a PlugIn manager used to search for installed plug-ins and
 * to register new plug-ins. <P>
 * <H3>Plug-in types</H3>
 * Plug-ins can be of different types such as codecs, demultiplexers, 
 * renderers, etc.
 * <P>The recognized plug-in types are listed below. Other custom plug-in types
 * can also be registered.<P>
 * "demultiplexer"<BR>"codec"<BR>"renderer"<BR>"multiplexer"<BR>"effect"
 * @since JMF 2.0
 */

public class 
PlugInManager extends javax.media.PlugInManager {

    private static Hashtable    lists         = null;
    private static PlugInVector plugins       = null;
    private static Registry     r             = null;
    private static Format       emptyFormat[] = new Format[0];

    private static boolean jdkInit = false;
    private static Method forName3ArgsM;
    private static Method getSystemClassLoaderM;
    private static ClassLoader systemClassLoader;
    private static Method getContextClassLoaderM;

    
    static {
	readFromRegistry();
    }


    /****************************************************************
     * PlugInManager implementation
     ****************************************************************/

    public 
    PlugInManager() {
	readFromRegistry();
    }

    
    private static boolean checkIfJDK12() {
	if (jdkInit)
	    return (forName3ArgsM != null);
	jdkInit = true;
	try {
	    forName3ArgsM = Class.class.getMethod("forName",
						  new Class[] {
		String.class, boolean.class, ClassLoader.class
		    });
	    
	    getSystemClassLoaderM = ClassLoader.class.getMethod("getSystemClassLoader", null);

	    // TODO: may need to invoke RuntimePermission("getClassLoader") privilege
	    systemClassLoader = (ClassLoader) getSystemClassLoaderM.invoke(ClassLoader.class, null);

	    getContextClassLoaderM = Thread.class.getMethod("getContextClassLoader", null);

	    return true;
	} catch (Throwable t) {
	    forName3ArgsM = null;
	    return false;
	}
    }


    // This is a Package private class
    static Class getClassForName(String className) 
                throws ClassNotFoundException {
	/**
	 *  Note: if we don't want this functionality
	 *  just replace it with Class.forName(className)
	 */

	try {
	    return Class.forName(className);
	} catch (Exception e) {
	    if (!checkIfJDK12()) {
		throw new ClassNotFoundException(e.getMessage());
	    }
	} catch (Error e) {
	    if (!checkIfJDK12()) {
		throw e;
	    }
	}

	/**
	 *  In jdk1.2 application, when you have jmf.jar in the ext directory and
	 *  you want to access a class that is not in jmf.jar but is in the CLASSPATH,
	 *  you have to load it using the the system class loader.
	 */
	try {
	    return (Class) forName3ArgsM.invoke(Class.class, new Object[] {
		className, new Boolean(true), systemClassLoader});
	} catch (Throwable e) {
	}

	/**
	 *  In jdk1.2 applet, when you have jmf.jar in the ext directory and
	 *  you want to access a class that is not in jmf.jar but applet codebase,
	 *  you have to load it using the the context class loader.
	 */
	try {
	    // TODO: may need to invoke RuntimePermission("getClassLoader") privilege
	    ClassLoader contextClassLoader =
		(ClassLoader) getContextClassLoaderM.invoke(Thread.currentThread(), null);
	    return (Class) forName3ArgsM.invoke(Class.class, new Object[] {
		className, new Boolean(true), contextClassLoader});
	} catch (Exception e) {
	    throw new ClassNotFoundException(e.getMessage());
	} catch (Error e) {
	    throw e;
	}
    }


    /**
     * Builds a list of plug-ins that satisfy the specified input and
     * output formats. Either or both of the formats could be null if
     * they are to be ignored. So, if <code>input</code> is null, then
     * it returns a list of plug-ins that match the output format and
     * vice-versa.
     */

    public synchronized static Vector 
    getPlugInList( Format input, Format output, int type ) {
	ListInfo li = (ListInfo) lists.get(new Integer(type));

	if (li == null)
	    return new Vector(1);
	Vector pluginList = li.classNames;
	Vector result = new Vector(5);
	// Do we need light weight?
	boolean blw = false;
	Boolean lw = (Boolean) Manager.getHint(Manager.LIGHTWEIGHT_RENDERER);
	if (lw != null && lw.booleanValue())
	    blw = true;
	
	Enumeration eClassNames = pluginList.elements();
	while (eClassNames.hasMoreElements()) {
	    ClassNameInfo cni = (ClassNameInfo) eClassNames.nextElement();
	    PlugInInfo pii = (PlugInInfo) vectorFindPlugIn(type, cni.className,
							   cni.hashValue);
	    if (pii != null) {
		if (  match(pii.inputFormats, input) &&
		      match(pii.outputFormats, output)  ) {
		    if (blw) {
			if (cni.className.indexOf("com.sun.media.renderer.video") == 0)
			    if (!cni.className.equals("com.sun.media.renderer.video.LightWeightRenderer"))
				continue;
		    }
		    result.addElement(cni.className);
		}
	    }
	}

	return result;
    }


    /**
     * Sets the search order for the list of plug-ins. This list is valid
     * for the duration of the session only, unless <code>commit</code> is
     * called.
     * @see #commit
     */

    public synchronized static void 
    setPlugInList(Vector pluginList, int type) {
	// parameter validity check
	if (pluginList == null || type < 1)
	    return;

	// Verify that all the classnames are valid and skip those that aren't
	Vector validNames = new Vector(5);
	Enumeration eClassNames = pluginList.elements();
	while (eClassNames.hasMoreElements()) {
	    String        className = (String) eClassNames.nextElement();
	    ClassNameInfo cni       = new ClassNameInfo(className);

	    // Is the class name and type in the plugins database?
	    if (vectorFindPlugIn(type, cni.className, cni.hashValue) != null) {
		// Add it to the valid names
		validNames.addElement(cni);
	    }
	}

	// Put this type and ordered list of class names in the lists hashtable
	lists.put(new Integer(type), new ListInfo(type, validNames));

	// Purge the run-time supported format database from the
	// MediaEngine since the plugin list is modified.
	Resource.purgeDB();
    }


    /**
     * Commits any changes made to the plug-in list. Also required
     * when a new plug-in is added or a plug-in is removed.
     */

    public synchronized static void 
    commit() throws java.io.IOException {
	r.removeGroup("PIM");

	r.set("PIM.lists", lists.clone());

	Enumeration eListInfo = lists.elements();
	while (eListInfo.hasMoreElements()) {
	    // Get the list info
	    ListInfo li = (ListInfo) eListInfo.nextElement();
	    if (li == null) {
		System.err.println("li is null????");
		continue;
	    }
	    int type = li.type;
	    Vector classNames = li.classNames;

	    // Enumerate each of the classes in the list
	    Enumeration eClassNames = classNames.elements();
	    while (eClassNames.hasMoreElements()) {
		ClassNameInfo cni = (ClassNameInfo) eClassNames.nextElement();

		// Retrieve the plugin's info
		PlugInInfo pii = (PlugInInfo) vectorFindPlugIn(type, 
						 cni.className, cni.hashValue);
		if (pii == null) {
		    System.err.println("pii is null for " + cni.className);
		    continue;
		}
		// Save the input and output formats.
		r.set("PIM." + type + "_" + cni.className + ".in", 
		      pii.inputFormats);
		r.set("PIM." + type + "_" + cni.className + ".out", 
		      pii.outputFormats);
	    }
	}

	r.commit();

	// Destroy the permanent supported table resource database 
	// since the plugin registry is permanently changed.
	Resource.destroy();
    }


    /**
     * Registers a new plug-in. This plug-in is automatically appended
     * to the list of plug-ins. Returns false if the new plug-in could not
     * be registered. The reason could be that a plug-in by that name
     * already exists. <code>commit</code> has to be called to make this
     * addition permanent.
     * @param className class name of the plug-in class
     * @param in list of supported input formats
     * @param out list of supported output formats
     * @param type the plug-in type
     * @param dynamic true if the input and output formats
     * need to be determined at runtime by the plug-in rather
     * than from the registry.
     */

    public synchronized static boolean 
    addPlugIn(String className, Format in[], Format out[], int type) {

	// Search for the class name in the list
	Integer typeInt = new Integer(type);
	ListInfo li = (ListInfo) lists.get(typeInt);
	Vector v;
	ClassNameInfo cni;

	if (li != null) {
	    v = li.classNames;
	    Enumeration eClassNames = v.elements();
	    while (eClassNames.hasMoreElements()) {
		cni = (ClassNameInfo) eClassNames.nextElement();
		// Return false if the class already exists in this list
		if (cni.className.equals(className))
		    return false;
	    }
	} else {
	    // This is the first item in this list.
	    v = new Vector(5);
	}
	// TODO : remove this
	// Additional verification to make sure such a class exists
	try {
	    Class piClass = getClassForName(className);
	} catch (Throwable t) {
	    // Sorry dude. You cant fool me with random class names
	    return false;
	}

	// Now we aren't checking to see if the plugin already exists
	// in the plugins hashtable. We'll just overwrite any older def.

	// Append it to the list for this type
	cni = new ClassNameInfo(className);
	v.addElement(cni);
	// Set the list on the type

	lists.put(typeInt, new ListInfo(type, v));

	if (in == null)
	    in = emptyFormat;
	if (out == null)
	    out = emptyFormat;

	vectorInsertPlugIn(new PlugInInfo(type, className,
					  cni.hashValue, in, out));

	// Purge the run-time supported format database from the
	// MediaEngine since the plugin list is modified.
	Resource.purgeDB();

	return true;
    }


    /**
     * Removes an existing plug-in from the registry. Returns false if
     * a plug-in by that name couldn't be found. <code>commit</code> has
     * to be called to make this change permanent.
     */

    public synchronized static boolean 
    removePlugIn(String className, int type) {

	// First remove it from its type list if it exists.
	Integer       typeInt = new Integer(type);
	ListInfo      li      = (ListInfo) lists.get(typeInt);
	ClassNameInfo cni = null;
	PlugInInfo    pii = null;

	if (li != null) {
	    Vector v = li.classNames;
	    Enumeration eClassNames = v.elements();

            while (eClassNames.hasMoreElements()) {
                cni = (ClassNameInfo) eClassNames.nextElement();

		if (cni.className.equals(className)) {
		    v.removeElement(cni);
		    break;
		}
	    }
	}

	if (cni != null) {
	    pii = vectorRemovePlugIn(type, className, cni.hashValue);

	    // Purge the run-time supported format database from the
	    // MediaEngine since the plugin list is modified.
	    Resource.purgeDB();
	}

	return(pii != null);
    }


    public synchronized static Format[] 
    getSupportedInputFormats(String className, int type) {
	ClassNameInfo cni = new ClassNameInfo(className);
	PlugInInfo pii = (PlugInInfo) vectorFindPlugIn(type, className,
						       cni.hashValue);
	if (pii == null)
	    return emptyFormat;
	return pii.inputFormats;
    }


    public synchronized static Format[] 
    getSupportedOutputFormats(String className, int type) {
	ClassNameInfo cni = new ClassNameInfo(className);
	PlugInInfo pii = (PlugInInfo) vectorFindPlugIn(type, className,
						       cni.hashValue);
	if (pii == null)
	    return emptyFormat;
	return pii.outputFormats;
    }


    /****************************************************************
     * Local Methods
     ****************************************************************/

    static private void 
    vectorInsertPlugIn(PlugInInfo pii) {
	plugins.addElement(pii);
    }


    static private PlugInInfo 
    vectorFindPlugIn(int type, String className, long hashValue) {
	return(plugins.lookup(type, className, hashValue));
    }


    static private PlugInInfo 
    vectorRemovePlugIn(int type, String className, long hashValue) {
	PlugInInfo pii = vectorFindPlugIn(type, className, hashValue);

	if (pii != null) {
	    plugins.removeElement(pii);
	    return pii;
	}
	return null;
    }


    // Check if a certain format is compatible with any in an array of formats.
    private synchronized static boolean 
    match(Format [] formats, Format tomatch) {
	if (tomatch == null || formats == null)
	    return true;
	for (int i = 0; i < formats.length; i++) {
	    if (formats[i].matches(tomatch))
		return true;
	}
	return false;
    }


    private static void 
    readFromRegistry() {
	// If its already initialized, dont read the registry.
	if (lists != null)
	    return;

	// Create a hashtable for the plugins info.
	plugins = new PlugInVector(40);

	// Read the plug-in lists for the different types all stored
	// into one hashtable whose key is the type.
	r = new Registry();
        Object list = r.get("PIM.lists");
	if (list != null && list instanceof Hashtable) {
	    lists = (Hashtable) list;
	} else {
	    // No plugin lists were found.
            // This should happen only during compilation of RegistryLib
	    lists = new Hashtable(8);
            System.out.println("No plugins found");
	    //registerDefaultPlugIns();
	    return;
	}

	Enumeration eListInfo = lists.elements();
	while (eListInfo.hasMoreElements()) {
	    // Get the list info
	    ListInfo li = (ListInfo) eListInfo.nextElement();

	    int type = li.type;
	    Vector classNames = li.classNames;
	    Vector validClassNames = new Vector(5);

	    // Enumerate each of the classes in the list
	    Enumeration eClassNames = classNames.elements();
	    while (eClassNames.hasMoreElements()) {
		ClassNameInfo cni = (ClassNameInfo) eClassNames.nextElement();
		// Make the key using the type and the classname
		String key = type + "_" + cni.className;

		// Try to get the input and output formats. If something
		// fails during deserialization, the objects will be null
		Object f1 = r.get("PIM." + key + ".in");
		Object f2 = r.get("PIM." + key + ".out");

		// If the format arrays are valid, add it to the plugins hashtable
		if (  f1 != null && f1 instanceof Format[] &&
		      f2 != null && f2 instanceof Format[]   ) {

		    PlugInInfo pii = new PlugInInfo(type, cni.className, 
			          cni.hashValue, (Format[]) f1, (Format[]) f2);
		    vectorInsertPlugIn(pii);
		    // This plugin is valid, add to the plugin list for this type
		    validClassNames.addElement(cni);
		}
	    }
	    // Add to the list of plugin lists
	    lists.put(new Integer(type), new ListInfo(type, validClassNames));
	}
    }

}


class 
ListInfo implements Serializable {

    public int    type;
    public Vector classNames;

    public 
    ListInfo(int type, Vector classNames) {
	this.type       = type;
	this.classNames = classNames;
    }
}


class 
PlugInInfo {

    public int    type;
    public String className;
    public long   hashValue;
    public Format inputFormats[];
    public Format outputFormats[];
    
    public 
    PlugInInfo(int type, String className, long hashValue,
	       Format inputFormats[], Format outputFormats[]) {
	this.type          = type;
	this.className     = className;
	this.hashValue     = hashValue;
	this.inputFormats  = inputFormats;
	this.outputFormats = outputFormats;
    }
}


class 
ClassNameInfo implements Serializable {

    public String className;
    public long   hashValue;

    public
    ClassNameInfo(String className) {
	this.className = className;
	hashValue = makeHashValue(className);
    }


    public final static long
    makeHashValue(String className) {
        final int MAXCHARS = 22;
 
        long h     = 0;
        char val[] = className.toCharArray();
        int  len   = val.length;
        int  off   = (val.length < MAXCHARS) ? 0 : val.length - MAXCHARS;
 
        for (int i = off; i < len; i++) {
            h = (h * 37) + val[off++];
        }
 
        return(h);
    }
}


/* Would have like to extend the Vector class, but the insertElementAt()
 * and elementAt() methods are final there.
 */

class 
PlugInVector {

    private Vector elements = null;


/**
 * Constructs an empty plugin vector with the specified initial capacity.
 *   
 * @param initialCapacity the initial capacity of the vector.
 */

    public 
    PlugInVector(int initialCapacity) {
        elements = new Vector(initialCapacity, 0);
    }


/**
 * Add the new PlugInInfo instance into the sorted (by hashValue)
 * vector of PlugInInfo objects.
 *
 * @param obj the component to be added.
 *
 * XXX: it's remotely possibly that we are going to get duplicate hash values.
 */

    public final synchronized void 
    addElement(PlugInInfo pii) {
	int i;

	for (i = 0; i < elements.size(); i++) {
	    PlugInInfo current = (PlugInInfo) elements.elementAt(i);

	    if (current.hashValue == pii.hashValue &&
		!current.className.equals(pii.className)) {
	        System.err.println("Problem adding " + pii.className +
				   " to plugin table.");
		System.err.println(" Already hash value of " + pii.hashValue +
				   " in plugin table for class name of " +
				   current.className);
		break;
	    } else if (current.hashValue > pii.hashValue) {
		break;
	    }
        }

	elements.insertElementAt(pii, i);
    }


/**
 * Removes the first occurrence of the given PlugInInfo instance from
 * the vector of plugins.
 *
 * @param obj the component to be removed.
 *
 * @return <code>true</code> if the argument was a component of this
 * vector; <code>false</code> otherwise.
 */

    public final synchronized boolean 
    removeElement(PlugInInfo pii) {
        return(elements.removeElement(pii));
    }


/**
 * Search to see if a PlugInInfo object with the given type and classname
 * is present in the vector of plugins.
 *
 * @param type the type of the plugin.
 * @param className the class name of the plugin.
 * @param hashValue the hash value for the class name.
 *
 * @return the first PlugIniInfo instance with the given type and classname 
 * if found, otherwise null.
 */

    public final synchronized PlugInInfo 
    lookup(int type, String className, long hashValue) {
	PlugInInfo current   = null;
	int        low       = 0;
        int        high      = elements.size();
        int        mid       = 0;

	while (low <= high) {
	    mid     = (low + high) / 2;
	    current = (PlugInInfo) elements.elementAt(mid);

	    if (hashValue < current.hashValue) {
	        high = mid - 1;
	    } else if (hashValue > current.hashValue) {
		low = mid + 1;
	    } else {

		if (type == current.type &&
		    className.equals(current.className)) {
		    return(current);
		} else {

/* It's remotely possible that there are two (or more) elements in this
 * plugin vector with the same hash value. If that's the case, then search
 * for them (in both directions). Note for code simplicity sake, it repeats
 * the test with the 'mid'th plugin element.
 */

                    int i = 0;
                    do {
			current = (PlugInInfo) elements.elementAt(mid-i);
                        if (hashValue != current.hashValue) {
                            break;
                        }   
                        if (type == current.type &&
                            className.equals(current.className)) {
                    	    return(current);
                        }
                        i++; 
                    } while (mid-i >= 0);
 
                    i = 0;
                    do {
			current = (PlugInInfo) elements.elementAt(mid+i);
                        if (hashValue != current.hashValue) {
                            break; 
                        }    
                        if (type == current.type &&
                            className.equals(current.className)) {
                            return(current);
			}
                        i++;
                    } while (mid+i < elements.size()); 

		    // break from the outer loop if a match
		    // cannot be found.
		    break;
                } 
	    }
	}

	return(null);
    }
}
