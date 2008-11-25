/*
 * @(#)CusRegistry.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.customizer;

import java.util.*;
import java.io.*;
import javax.media.CaptureDeviceInfo;
import javax.media.cdm.*;


public class CusRegistry {
    // Default Hashtable in RegistryLib.class in jmf.jar
    private Hashtable defaultHash = null;
    // Hashtable loaded from jmf.properties, including all the native plugins
    private Hashtable fullHash = null;
    // Name of the properties file, including path.
    private static String filename = null;
    // Version number of the serialized file format. Will need
    // to be incremented if the format changes so that an old
    // implementation will expect errors with a new format.
    static final int versionNumber = 200;
    private static final String CLASSPATH = "java.class.path";
    private String classpath = null;
    
    private String workDir = null;
    private String javacpath = null;
    
    public CusRegistry() {
	defaultHash = null;
	fullHash = null;
	workDir = null;
	javacpath = null;
    }
    
    public  void setWorkDir(String wdir) {
	workDir = wdir;
    }

    public String getWorkDir() {
	return workDir;
    }

    public void setJavacPath(String cpath) {
	javacpath = cpath;
    }

    public String getJavacPath() {
	return javacpath;
    }
    
    public boolean loadRegistry(){
	InputStream ris = null;
	boolean loadDef = false;
	
	defaultHash = new Hashtable();
	fullHash = new Hashtable();

	// Load the default registry from RegistryLib
	ris = findJMFPropertiesFile();
	boolean loadFull = readRegistry(fullHash, ris);

	if ( loadFull ) {
	    return true;
	}

	try {
	    byte[] data;
	    Class c = Class.forName("com.sun.media.util.RegistryLib");
	    data = (byte[]) c.getMethod("getData", null).invoke(c,null);
	    if(data != null) {
		ris = new ByteArrayInputStream(data);
		loadDef = readRegistry(defaultHash, ris);
	    }
	} catch ( Exception ex) {
	    ex.printStackTrace();
	    ris = null;
	    loadDef = false;
	}

	return loadDef;
    }
    
    public Hashtable getDefaultRegistry() {
	return defaultHash;
    }

    public Hashtable getFullRegistry() {
	return fullHash;
    }
    
    // ---------------------
    public void removePlugIn(Hashtable newhash, String pname, int type) {
	String key = "PIM." + type + "_" + pname + ".in";
	newhash.remove(key);
	
	key = "PIM." + type + "_" + pname + ".out";
	newhash.remove(key);
	
	// System.out.println("remove " + key);
    }
    
    // ----------------------------
    public void removeCaptureD(Hashtable newhash, String cname){
	String key1 = "CDM.nDevices";
	Object oo;
	
	if ( newhash.get(key1) != null ) {
	    int totalnum = ((Integer)(newhash.get(key1))).intValue();
	    int found = -1;
	    int j = 0;
	    String key;
	    CaptureDeviceInfo info;

	    for ( int i = 0 ; i < totalnum; i++) {
		oo = newhash.get("CDM." + i);
		if ( oo != null ) {
		    info = (CaptureDeviceInfo)oo;
		    if ( info.getLocator().getProtocol().equalsIgnoreCase(cname) ) {
			newhash.put(key1, new Integer(totalnum-1));
			newhash.remove("CDM." + i);
			found = i;
			break;
		    }
		}
	    }

	    for ( int i = found+1; i < totalnum; i++) {
		oo = newhash.get("CDM." + i);
		j = i - 1;
		key = "CDM." + j;
		newhash.put(key, oo);
	    }

	    if ( found >= 0) {
		totalnum --;
		key = "CDM." + totalnum;
		newhash.remove(key);
	    }
	}
	
    }
    
    // -----------------------------------
    private final InputStream findJMFPropertiesFile() {
	String pfileName = findJMF();
	InputStream ris = null;

	if ( pfileName != null ) {
	    try {
		ris = new FileInputStream(pfileName);
	    } catch (Exception ex) {
		ris = null;
	    }
	}

	return ris;
    }

    public  final String findJMF() {
	
	// Search the directories in CLASSPATH for the first available
	// jmf.properties
	classpath = System.getProperty(CLASSPATH);
	if (classpath == null )
	    return null;
	
	StringTokenizer tokens = new StringTokenizer(classpath, File.pathSeparator);
	String dir;
	String strJMF = "jmf.properties";
	File file = null;
	InputStream ris = null;
	filename = null;
	
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
		file = new File(dir);
		if ( file.exists() && file.length() > 0 ) {
		    // ris = new FileInputStream(file);
		    filename = dir;
		    System.out.println("Found jmf.properties in " + dir);
		    break;
		}
	    } catch (Throwable t) {
		filename = null;
		return null;
	    }
	} // end of while
	
	return filename;

	//if ( filename == null || file == null )
	//    return null;
	// return ris;
    }
    
    // -------------------------
    private final boolean readRegistry(Hashtable hash, InputStream ris) {
	
	if ( ris == null )
	    return false;

	if ( hash == null )
	    hash = new Hashtable();

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
		    cnfe.printStackTrace();
		    failed = true;
		} catch (OptionalDataException ode) {
		    ode.printStackTrace();
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
	
	System.out.println("Registry Size = " + hash.size());
	return true;
    }

    // -----------------------------
    public boolean saveRegistry(Hashtable newhash){
	String tmpfilename = workDir + File.separator + "new_jmf.properties";
	try {
	    File tmpF = new File(tmpfilename);
	    
	    if ( tmpF.exists() ) {
		tmpF.delete();
	    }

	    FileOutputStream fos = new FileOutputStream(tmpfilename);
	    ObjectOutputStream oos = new ObjectOutputStream(fos);
	    
	    int tableSize = newhash.size();
	    oos.writeInt(tableSize);
	    oos.writeInt(versionNumber);
	    for (Enumeration e = newhash.keys(); e.hasMoreElements() ;) {
		String key = (String) e.nextElement();
		Object value = newhash.get(key);
		
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
	} catch ( Exception ex) {
	    ex.printStackTrace();
	    return false;
	}
	
	return true;
    }
    
}


