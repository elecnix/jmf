/*
 * @(#)Registry.java	1.30 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.io.*;
import com.sun.media.JMFSecurity;
import com.sun.media.IESecurity;
import com.sun.media.JMFSecurityManager;
import javax.media.*;
import java.lang.*;
import java.util.*;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;

/**
 * Class where objects can be registered with a unique key (string).
 * The Registry tries to find a "jmf.properties" file in the CLASSPATH.
 * If it does, then it reads all the keys and corresponding objects.
 * The structure of the serialized file is:
 *     Number of items in the table: integer
 *     Version number              : integer
 *     Key for item 1              : UTF
 *     Value of item 1             : Object
 *     Key for item 2              : UTF
 *     Value of item 2             : Object
 *     and so on......
 */
public class Registry {

    // Hashtable that stores all the properties.
    private static Hashtable hash = null;
    // Name of the properties file, including path.
    private static String filename = null;
    // Version number of the serialized file format. Will need
    // to be incremented if the format changes so that an old
    // implementation will expect errors with a new format.
    private static final int versionNumber = 200;
    private static boolean securityPrivelege=false;
    private static JMFSecurity jmfSecurity = null;
    private static Method m[] = new Method[1];
    private static Class cl[] = new Class[1];
    private static Object args[][] = new Object[1][0];
    private static final String CLASSPATH = "java.class.path";
    private static String userhome = null;
    private static String classpath = null;

    private static boolean jdkInit = false;
    private static Method forName3ArgsM;
    private static Method getSystemClassLoaderM;
    private static ClassLoader systemClassLoader;
    private static Method getContextClassLoaderM;

    /**
     * Static code block to read in the registry file and initialize
     * the hash table.
     */
    static {
	hash = new Hashtable();

	try {
	    jmfSecurity = JMFSecurityManager.getJMFSecurity();
	    securityPrivelege = true;
	} catch (SecurityException e) {
	}

	if ( /* securityPrivelege && */ (jmfSecurity != null) ) {
	    String permission = null;
	    try {
		if (jmfSecurity.getName().startsWith("jmf-security")) {
		    permission = "read property";
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.READ_PROPERTY);
		    m[0].invoke(cl[0], args[0]);


		    permission = "read file";
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.READ_FILE);
		    m[0].invoke(cl[0], args[0]);
		} else if (jmfSecurity.getName().startsWith("internet")) {
 		    PolicyEngine.checkPermission(PermissionID.PROPERTY);
 		    PolicyEngine.checkPermission(PermissionID.FILEIO);
		    PolicyEngine.assertPermission(PermissionID.PROPERTY);
		    PolicyEngine.assertPermission(PermissionID.FILEIO);
		}
	    } catch (Throwable e) {
		if (JMFSecurityManager.DEBUG) {
		    System.err.println("Registry: Unable to get " + permission +
				       " privilege  " + e.getMessage());
		}
		securityPrivelege = false;
	    }
	}

	if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
	    try {
		Constructor cons = jdk12PropertyAction.cons;
		classpath = (String) jdk12.doPrivM.invoke(
				     jdk12.ac,
				     new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               CLASSPATH,
                                           })});

		userhome = (String) jdk12.doPrivM.invoke(
				     jdk12.ac,
				     new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               "user.home",
                                           })});

	    } catch (Throwable e) {
		securityPrivelege = false;
	    }
	} else {
	    try {
		if (securityPrivelege) {
		    classpath = System.getProperty(CLASSPATH);
		    userhome = System.getProperty("user.home");		    
		}
	    } catch (Exception e) {
		filename = null;
		securityPrivelege = false;
	    }
	}

	if (classpath == null) {
	    securityPrivelege = false;
	}
	InputStream registryInputStream = null;

	String jmfDir = getJMFDir();
	if (jmfDir != null) {
	    classpath = classpath + File.pathSeparator + jmfDir;
	}
	
        if (securityPrivelege) {
	    registryInputStream = findJMFPropertiesFile();
          if (registryInputStream == null) {
            securityPrivelege=false; // there is no access to jmf.properties
          }
        }

	if (!readRegistry(registryInputStream)) {
	    hash = new Hashtable();
	}

    }

    private static String getJMFDir() {
	try {
	    String pointerFile;
	    if (File.separator.equals("/")) {
		if (userhome == null)
		    return null;
		pointerFile = userhome;
	    } else {
		JMFSecurityManager.loadLibrary("jmutil");
		pointerFile = nGetUserHome() + File.separator + "java";
	    }
	    pointerFile += File.separator + ".jmfdir";
	    File f = new File(pointerFile);
	    FileInputStream fis = getRegistryStream(f);
	    BufferedReader br = new BufferedReader(new InputStreamReader(fis));
	    String dir = br.readLine();
	    br.close();
	    return dir;
	} catch (Throwable t) {
	}
	return null;
    }

    private static native String nGetUserHome();
    
    /**
     * Add or modify a property. The key and the value should be non-null.
     * Returns false if it couldn't add/modify the property.
     */
    public static final synchronized boolean set(String key, Object value) {
	if (key != null && value != null) {
	    if (jmfSecurity != null && key.indexOf("secure.") == 0)
		return false;
	    hash.put(key, value);
	    return true;
	} else
	    return false;
    }

    /**
     * Returns the value corresponding to the specified key. Returns null
     * if no such property is found.
     */
    public static final synchronized Object get(String key) {
	if (key != null)
	    return hash.get(key);
	else
	    return null;
    }

    /**
     * Removes a property from the hashtable. Returns false
     * if the property was not found.
     */
    public static final synchronized boolean remove(String key) {
	if (key != null) {
	    if (hash.containsKey(key)) {
		hash.remove(key);
		return true;
	    }
	}

	return false;
    }

    /**
     * Removes an entire set of properties with the keys starting with
     * the value "keyStart".
     */
    public static final synchronized void removeGroup(String keyStart) {
	Vector keys = new Vector();
	if (keyStart != null) {
	    Enumeration e = hash.keys();
	    while (e.hasMoreElements()) {
		String key = (String) e.nextElement();
		if (key.startsWith(keyStart))
		    keys.addElement(key);
	    }
	}

	for (int i = 0; i < keys.size(); i++) {
	    hash.remove(keys.elementAt(i));
	}
    }
    
    /**
     * Writes all the properties in the hashtable to the
     * jmf.properties file. If the file is non-existent or the writing
     * failed for some reason, it throws an IOException.
     */
    public static final synchronized boolean commit()
    throws IOException {

	if (jmfSecurity != null) {
	    throw new SecurityException("commit: Permission denied");
	}

	if (filename == null)
	    throw new IOException("Can't find registry file");

	FileOutputStream fos = new FileOutputStream(filename);
	ObjectOutputStream oos = new ObjectOutputStream(fos);

	int tableSize = hash.size();
	oos.writeInt(tableSize);
	oos.writeInt(versionNumber);
	for (Enumeration e = hash.keys(); e.hasMoreElements() ;) {
	    String key = (String) e.nextElement();
	    Object value = hash.get(key);

	    oos.writeUTF(key);			      // write key as UTF chars.

            /*
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ObjectOutputStream tempOOS = new ObjectOutputStream(baos);
	    tempOOS.writeObject(value);
	    tempOOS.flush();
	    byte [] serObject = baos.toByteArray();
	    oos.writeObject(serObject);
	    tempOOS.close();
            */

            oos.writeObject(value);
            oos.flush();
	}
	oos.close();
	return true;
    }

   private static final synchronized InputStream findJMFPropertiesFile() {

	// Search the directories in CLASSPATH for the first available
	// jmf.properties
	StringTokenizer tokens = new StringTokenizer(classpath, File.pathSeparator);
	String dir;
	String strJMF = "jmf.properties";
	File file = null;
	InputStream ris = null;

	while (tokens.hasMoreTokens()) {
	    dir = tokens.nextToken();
	    String caps = dir.toUpperCase();
	    // If its not a directory, then we need to get rid of
	    // the file name and get the directory.
	    try {
		if (caps.indexOf(".ZIP") > 0 || caps.indexOf(".JAR") > 0 ) {
		    int sep = dir.lastIndexOf(File.separator);
		    if (sep == -1 && ! File.separator.equals("/")) // if someone uses a slash in DOS
			                                           // instead of a backslash
			sep = dir.lastIndexOf("/");

		    if (sep == -1) {		      // no separator
			sep = dir.lastIndexOf(":");	      // is there a ":" ?
			if (sep == -1) {		      // no ":"
			    dir = strJMF;		      // its just a xxx.jar or xxx.zip
			} else {			      // there is a ":"
			    dir = dir.substring(0, sep) + ":" + strJMF;
			}
		    } else {
			dir = dir.substring(0, sep) + File.separator + strJMF;
		    }
		} else
		    dir = dir + File.separator + strJMF;
	    } catch (Exception e) {
		dir = dir + File.separator + strJMF;
	    }
	    try {
		// System.out.println("Registry: dir is " + dir);
		file = new File(dir);
		//		if (file.exists()) {
		ris = getRegistryStream(file);
		if (ris != null) {
		    filename = dir;
		    break;
		}
	    } catch (Throwable t) {
		filename = null;
		return null;
	    }
	}
	try {
	    if (filename == null || file == null)
		return null;
// 	    if (file.length() == 0) { // TODO: Ask AMITH if you need this
// 		return false;
//             }
	} catch (Throwable t) {
	    return null;
	}


	return ris;
    }


    private static FileInputStream getRegistryStream(File file) throws IOException {
	try {

	    if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
		Constructor cons = jdk12ReadFileAction.cons;
		return (FileInputStream) jdk12.doPrivM.invoke(
				     jdk12.ac,
				     new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               file.getPath()
                                           })});
	    
	    } else {
		if (!file.exists()) {
		    // System.out.println("file doesnt exist");
		    return null;
		} else {
		    return new FileInputStream(file.getPath());
		}
	    }
	} catch (Throwable t) {
	    return null;
	}
    }


   private static final synchronized boolean readRegistry(InputStream ris) {

       // read from jmf.properties first
       if (realReadRegistry(ris)) {
	   return true;
       }
       
       
       // something wrong when reading from jmf.properties or 
       // can not find jmf.properties in the classpath
       // Anyway try to load from RegistryLib.class
       byte[] data;
       try {
	   Class c = Class.forName("com.sun.media.util.RegistryLib");
	   data = (byte[]) c.getMethod("getData", null).invoke(c,null);
       } catch (Exception e) {
	   // System.out.println("no data in RegistryLib");
	   return false;
       }
       if(data==null) {
	   // System.out.println("no data in RegistryLib");
	   return false;
       }
       
       InputStream def_ris = new ByteArrayInputStream(data);
       
       return (realReadRegistry(def_ris));
   }
    
    private static final boolean realReadRegistry(InputStream ris) {
	if ( ris == null)
	    return false;
	
	try {
	    // Inner class with skipHeader so that the protected method
	    // readStreamHeader can be called.
	    ObjectInputStream ois = new ObjectInputStream(ris);
	    
	    int tableSize = ois.readInt();
	    int version = ois.readInt();
	    if (version > 200) {
		System.err.println("Version number mismatch.\nThere could be" +
				   " errors in reading the registry");
	    }
	    hash = new Hashtable();
	    for (int i = 0; i < tableSize; i++) {
		String key = ois.readUTF();
		boolean failed = false;
		byte [] serObject;
		try {
                    /*
		      serObject = (byte[])ois.readObject();
		      ByteArrayInputStream bais = new ByteArrayInputStream(serObject);
		      ObjectInputStream tois = new ObjectInputStream(bais);
		      Object value = tois.readObject();
		      hash.put(key, value);
		      tois.close();
                    */
		    
                    Object value = ois.readObject();
		    hash.put(key, value);
		} catch (ClassNotFoundException cnfe) {
		    failed = true;
		} catch (OptionalDataException ode) {
		    failed = true;
		}
	    }
	    ois.close();
            ris.close();
	} catch (IOException ioe) {
	    System.err.println("IOException in readRegistry: " + ioe);
	    return false;
	} catch (Throwable t) {
	    return false;
	}

        return true;
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
    // Currently used VideoCodecChain in this package
    // Not used in this class as we can use the generic Class.forName
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

    /*************************************************************************
     * INNER CLASSES
     *************************************************************************/

    public static void main(String [] args) {
	//Registry r = new Registry();
	Format f1 = new Format("crap", Integer.class);
	Class vfclass;
	Object bad = new Integer(5);
	try {
	    vfclass = Class.forName("javax.media.format.H261Format");
	    bad = vfclass.newInstance();
	} catch (ClassNotFoundException cnfe) {
	    System.err.println("H261Format not found in main()");
	} catch (InstantiationException ie) {
	} catch (IllegalAccessException iae) {
	}
	Format f2 = new Format("crap2", Boolean.class);

	set("obj1", f1);
	set("obj2", bad);
	set("obj3", f2);
	try {
	    commit();
	} catch (IOException ioe) {
	    System.err.println("main: IO error in commit " + ioe);
	}
    }
}

