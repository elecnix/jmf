/*
 * @(#)ProcessJAR.java	1.15 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.customizer;

import java.util.*;
import java.util.zip.*;
import java.io.*;
import javax.swing.JProgressBar;
import javax.media.PlugInManager;
import javax.media.CaptureDeviceManager;
import javax.media.CaptureDeviceInfo;
import javax.media.cdm.*;


/**
  * The purpose of this class is to do the real customization work:
  *         open the source JMF JAR file,
  *         filter out those plugins not selected by the user,
  *         create the target JAR file and write out all necessary classes.
  *   An object of this class will run on a separate thread.
  * @version 2.0
  *
  */

public class ProcessJAR extends Thread implements TokenDef {
    public static final String[] sollibs = {
	"libCvidPro.so",
	"libjmcvid.so",
	"libjmdaud.so",
	"libjmfCVIDPro.so",
	"libjmg723.so",
	"libjmgsm.so",
	"libjmh261.so",
	"libjmh263enc.so",
	"libjmiv32.so",
	"libjmjpeg.so",
	"libjmmpa.so",
	"libjmmpegv.so",
	"libjmmpx.so",
	"libjmmpx2.so",
	"libjmopi.so",
	"libjmsunray.so",
	"libjmvh263.so",
	"libjmxil.so",
	"libjmxlib.so",
	"libjsound.so",
	"soundbank.gm",
	"sound.jar",
    };

    public static final String[] win32libs = {
	"jmacm.dll",
	"jmam.dll",
	"jmcvid.dll",
	"jmdaud.dll",
	"jmdaudc.dll",
	"jmddraw.dll",
	"jmg723.dll",
	"jmgdi.dll",
	"jmgsm.dll",
	"jmh261.dll",
	"jmh263enc.dll",
	"jmiv32.dll",
	"jmjpeg.dll",
	"jmmci.dll",
	"jmmpa.dll",
	"jmmpegv.dll",
	"jmvcm.dll",
	"jmvfw.dll",
	"jmvh263.dll",
	"jsound.dll",
	"soundbank.gm",
	"sound.jar",
    };

    public static final Hashtable cdmap;
    static {
	cdmap = new Hashtable();
	
	cdmap.put("dsound", "com/sun/media/protocol/dsound/DataSource.class");
	cdmap.put("javasound", "com/sun/media/protocol/javasound/DataSource.class");
	cdmap.put("vfw", "com/sun/media/protocol/vfw/DataSource.class");
	cdmap.put("sunvideo", "com/sun/media/protocol/sunvideo/DataSource.class");
	cdmap.put("sunvideoplus", "com/sun/media/protocol/sunvideoplus/DataSource.class");
    }
    
    String srcJARname;
    String dstJARname;
    String workDir;
    int[] selected;
    ProgressDiag progressDlg;
    CusRegistry theRegistry;
    int release;
    boolean twojars;
    
    public ProcessJAR(String srcJARname, String dstJARname, int[] selected,ProgressDiag progressDlg, CusRegistry theRegistry, int release, boolean twojars) {
	this.srcJARname = srcJARname;
	this.dstJARname = dstJARname;
	this.selected = selected;
	this.progressDlg = progressDlg;
	this.theRegistry = theRegistry;
	this.workDir = this.theRegistry.getWorkDir();
	this.release = release;
	this.twojars = twojars;
    }
  
    
    /**
     * run():
     *   The method of the thread to do the real job:
     *
     *       Open source JAR file:
     *           Open it for reading
     *           make sure all mediaFormat/Codecs classes are there to pick
     *           display class name/class count/file size while processing
     *
     *       Process the request by marking necessary MediaFormat/Codec classes
     *
     *       Create destinaton JAR file:
     *           create the file,  abort if any error
     *           for each class from source
     *             if this class is needed, write it to the destination JAR/ZIP file
     *             otherwise ignore it
     *
     *       When done, enable DONE button of the progress bar
     *
     * @Param N/A
     * @return N/A
     *
     */
    public void run() {
	// Assuming 35% for reading, 15% for updating the registry and 50% for writing, can be adjusted if needed.
	File srcFile, dstFile, dstFile2 = null;
	long srcfilesize, filesizeSoFar;
	int classCountSoFar;
	int progressSoFar = 0;
	ZipFile srcZF = null;
	ZipEntry srcZE;
	
	FileOutputStream fos, fos2;
	ZipOutputStream zos = null, zos2 = null;
	InputStream is = null;
	
	double readRate, writeRate;
	//System.out.println("in ProcessJAR run...");
	//System.out.println("srcname = " + srcJARname);
	//System.out.println("dstname = " + dstJARname);
	//for ( int i = 0; i < selected.length; i++)
	//System.out.println(" " + selected[i]);
	
	// let dialog thread execute first
	try {
	    sleep(30);
	} catch (Exception ex) {
	}
	
	srcFile = new File(srcJARname);
	
	// We will use the file size as the base for the initial segment
	// of the progress bar
	srcfilesize = srcFile.length();
	readRate = 35.0/srcfilesize;
	
	// Open the source file as a ZipFile
	try {
	    srcZF = new ZipFile(srcFile);
	}
	catch (ZipException e) {
	    errorMessage(e.getMessage());
	    return;
	}
	catch (IOException e) {
	    errorMessage(e.getMessage());
	    return;
	}
	
	if ( CustomDB.getSize() == 0 ) 
	    CustomDB.buildDB();
	else 
	    CustomDB.clearAllMarks();
	
	Vector orglist = new Vector();
	Enumeration se = srcZF.entries();
	String clsname;
	long entrysize;
	
	classCountSoFar = 0;
	filesizeSoFar = 0;
	
	// read classes from source jar
	while (se.hasMoreElements()) {
	    srcZE = (ZipEntry)se.nextElement();
	    
	    clsname = srcZE.getName();
	    if (!(clsname.endsWith(".class") || clsname.endsWith(".gif")) ){
		// System.out.println(clsname);
		continue;
	    }
	    orglist.addElement(srcZE);
	    
	    entrysize = srcZE.getCompressedSize();
	    classCountSoFar++;
	    filesizeSoFar += entrysize;
	    
	    if ( classCountSoFar % 5 == 0 ) {
		progressSoFar = (int)(readRate*filesizeSoFar);
		progressDlg.updateValue(progressSoFar);
		progressDlg.updateSourceInfo(filesizeSoFar, classCountSoFar);
		myyield();
	    }
	}
	
	
	progressDlg.updateSourceInfo(srcfilesize, classCountSoFar);
	myyield();
	
	// Finish loading source jar file, start to write target
	int notwanted = CustomDB.markAllClasses(selected);
	writeRate = 50.0/(classCountSoFar - notwanted);
	
	progressDlg.updateNote(I18N.getResource("ProcessJAR.REGISTRY"));
	myyield();
	
	updateRegistry();
	
	// Create the destination file
	if ( !twojars ) {
	    dstFile = new File(dstJARname);
	    try {
		fos = new FileOutputStream(dstFile);
		zos = new ZipOutputStream(fos);
	    } catch (IOException e) {
		try {
		    srcZF.close();
		}
		catch (IOException ex) {
		}
		dstFile.delete();
		errorMessage(e.getMessage());
		return;
	    }
	} else {
	    dstFile = new File(dstJARname);
	    String pp = dstFile.getParent() + File.separator;
	    String fn = dstFile.getName();
	    // System.out.println("pp = " + pp);
	    // System.out.println("fn = " + fn);
	    
	    dstFile = new File(pp + "core_" + fn);
	    dstFile2 = new File(pp + "plugin_" + fn);
	    try {
		fos = new FileOutputStream(dstFile);
		fos2 = new FileOutputStream(dstFile2);
		zos = new ZipOutputStream(fos);
		zos2 = new ZipOutputStream(fos2);
	    } catch (IOException e) {
		try {
		    srcZF.close();
		}
		catch (IOException ex) {
		}
		dstFile.delete();
		dstFile2.delete();
		errorMessage(e.getMessage());
		return;
	    }
	}

	classCountSoFar = 0;
	filesizeSoFar = 0;
	byte buffer[] = new byte[4096];  // Read/Write buffer
	int len;
	DBItem dbentry;
	
	progressDlg.updateNote(I18N.getResource("ProcessJAR.WRT"));
	myyield();
	
	// Traverse all classes from source JAR/ZIp file
	int NNN = orglist.size();
	if ( !twojars ) {
	    for (int nn = 0 ; nn < NNN; nn++ ) {
		srcZE = (ZipEntry)(orglist.elementAt(nn));
		
		clsname = srcZE.getName();
		entrysize = srcZE.getCompressedSize();
		
		if ( (dbentry=(DBItem)CustomDB.get(clsname)) == null || dbentry.isMarked()) { 
		    ++classCountSoFar;
		    // filesizeSoFar += entrysize;
		    ZipEntry newZE;
		    String rClsName = "com/sun/media/util/RegistryLib.class";
		    
		    try {
			if ( clsname.equals(rClsName)) {
			    String fullName = rClsName.replace('/', File.separatorChar);
			    fullName = workDir + File.separator + fullName;
			    is = new FileInputStream(fullName);
			} else {
			    is = srcZF.getInputStream(srcZE);
			}
			
			newZE = new ZipEntry(clsname);
			zos.putNextEntry(newZE);
			while (true) {
			    if (is.available() == 0)
				break;
			    len = is.read(buffer);
			    if (len == -1)
				break;
			    zos.write(buffer, 0, len);
			}
			zos.closeEntry();
			is.close();
			if (clsname.equals("com/sun/media/util/RegistryLib.class")) {
			    System.out.println("Default Registry replaced");
			    entrysize = newZE.getCompressedSize();
			}
		    } catch (IOException e) {
			try {
			    srcZF.close();
			}catch (IOException ex) {
			}
			
			dstFile.delete();
			errorMessage(e.getMessage());
			return;
		    }
		    
		    filesizeSoFar += entrysize;
		}
		
		if ( classCountSoFar % 5 == 0 ) {
		    progressSoFar = (int)(50 + writeRate * classCountSoFar);
		    progressDlg.updateValue(progressSoFar);
		    progressDlg.updateTargetInfo(filesizeSoFar, classCountSoFar);
		    myyield();
		}
		
	    } // end of for nn
	 		
	    // Done
	    try {
		zos.close();
		srcZF.close();
	    } catch ( IOException ex) {
		dstFile.delete();
		errorMessage(ex.getMessage());
		return;
	    }
	    
	} else { // generate two jars
	    for (int nn = 0 ; nn < NNN; nn++ ) {
		srcZE = (ZipEntry)(orglist.elementAt(nn));
		
		clsname = srcZE.getName();
		entrysize = srcZE.getCompressedSize();
		dbentry=(DBItem)CustomDB.get(clsname);

		if ( dbentry == null ) { // core classes
		    ++classCountSoFar;
		    // filesizeSoFar += entrysize;
		    ZipEntry newZE;
		    
		    try {
			is = srcZF.getInputStream(srcZE);
						
			newZE = new ZipEntry(clsname);
			zos.putNextEntry(newZE);
			while (true) {
			    if (is.available() == 0)
				break;
			    len = is.read(buffer);
			    if (len == -1)
				break;
			    zos.write(buffer, 0, len);
			}
			zos.closeEntry();
			is.close();
		    } catch (IOException e) {
			try {
			    srcZF.close();
			}catch (IOException ex) {
			}
			
			dstFile.delete();
			errorMessage(e.getMessage());
			return;
		    }
		    
		} else if ( dbentry.isMarked()) { // plugable classes
		    ++classCountSoFar;
		    // filesizeSoFar += entrysize;
		    ZipEntry newZE;
		    String rClsName = "com/sun/media/util/RegistryLib.class";
		    
		    try {
			if ( clsname.equals(rClsName)) {
			    String fullName = rClsName.replace('/', File.separatorChar);
			    fullName = workDir + File.separator + fullName;
			    is = new FileInputStream(fullName);
			} else {
			    is = srcZF.getInputStream(srcZE);
			}
			
			newZE = new ZipEntry(clsname);
			zos2.putNextEntry(newZE);
			while (true) {
			    if (is.available() == 0)
				break;
			    len = is.read(buffer);
			    if (len == -1)
				break;
			    zos2.write(buffer, 0, len);
			}
			zos2.closeEntry();
			is.close();
			if (clsname.equals("com/sun/media/util/RegistryLib.class")) {
			    System.out.println("Default Registry replaced");
			    entrysize = newZE.getCompressedSize();
			}
		    } catch (IOException e) {
			try {
			    srcZF.close();
			}catch (IOException ex) {
			}
			
			dstFile2.delete();
			errorMessage(e.getMessage());
			return;
		    }

		}
		
		filesizeSoFar += entrysize;

		if ( classCountSoFar % 5 == 0 ) {
		    progressSoFar = (int)(50 + writeRate * classCountSoFar);
		    progressDlg.updateValue(progressSoFar);
		    progressDlg.updateTargetInfo(filesizeSoFar, classCountSoFar);
		    myyield();
		}
		
	    }
	    
	
	    // Done
	    try {
		zos.close();
		zos2.close();
		srcZF.close();
	    } catch ( IOException ex) {
		dstFile.delete();
		dstFile2.delete();
		errorMessage(ex.getMessage());
		return;
	    }
	} // end of else twojars

	// ??
	long dstfilesize = dstFile.length();
	if ( twojars ) {
	    dstfilesize += dstFile2.length();
	}
	progressDlg.updateValue(100);
	progressDlg.updateTargetInfo(dstfilesize, classCountSoFar);
	progressDlg.enableDone();
	
	// Process native libs
	if ( release >= 2 ) {
	    System.out.println("***Needed native modules***");
	}
	
	if ( release == 2 ) { //SPP
	    String libname = null;
	    for (int i = 0; i < sollibs.length; i++) {
		libname = sollibs[i];
		if ( (dbentry=(DBItem)CustomDB.get(libname)) == null || dbentry.isMarked()) { 
		    System.out.println(libname);
		}
	    }
	    System.out.println("libjmutil.so");
	}
	
	if (release == 3 ) {
	    String libname = null;
	    for (int i = 0; i < win32libs.length; i++) {
		libname = win32libs[i];
		if ( (dbentry=(DBItem)CustomDB.get(libname)) == null || dbentry.isMarked()) { 
		    System.out.println(libname);
		}
	    }
	    System.out.println("jmutil.dll");
	}
	
	
    }
    
    
    private void errorMessage(String msg) {
	progressDlg.sentErr(msg);
	myyield();
    }
    
    private void myyield() {
	try {
	    sleep(15);
	} catch (Exception ex) {
	}
    }
    

    private void updateRegistry() {
	Vector v;
	Hashtable newDefHash, newFullHash, theHash;
	boolean b_def, b_full;
	int len;
	String pin, newpin;
	
	
	newFullHash = (Hashtable)(theRegistry.getFullRegistry().clone());
	if ( newFullHash != null && newFullHash.size() > 0 )
	    b_full = true;
	else
	    b_full = false;

	if ( b_full ) {
	    newDefHash = newFullHash;
	    b_def = false;
	} else {
	    newDefHash = (Hashtable)(theRegistry.getDefaultRegistry().clone());
	    
	    if ( newDefHash != null && newDefHash.size() > 0 )
		b_def = true;
	    else
		b_def = false;
	}

	// update DEMUX
	v = PlugInManager.getPlugInList(null, null, PlugInManager.DEMULTIPLEXER);
	len = v.size();
	
	for (int i = 0; i < len; i++) {
	    pin = (String)v.elementAt(i);
	    pin.trim();
	    newpin = pin.replace('.', '/') + ".class";
	    DBItem dbentry = (DBItem)CustomDB.get(newpin);
	    if ( dbentry != null && !dbentry.isMarked()) {
		if ( b_full) {
		    theRegistry.removePlugIn(newFullHash, pin,PlugInManager.DEMULTIPLEXER); 
		} else if ( b_def) {
		    theRegistry.removePlugIn(newDefHash, pin,PlugInManager.DEMULTIPLEXER); 
		}
	    }
	}
	
	// update CODEC
	v = PlugInManager.getPlugInList(null, null, PlugInManager.CODEC);
	len = v.size();
	
	for (int i = 0; i < len; i++) {
	    pin = (String)v.elementAt(i);
	    pin.trim();
	    newpin = pin.replace('.', '/') + ".class";
	    DBItem dbentry = (DBItem)CustomDB.get(newpin);
	    if ( dbentry != null && !dbentry.isMarked()) {
		// PlugInManager.removePlugIn(pin,PlugInManager.CODEC);
		if ( b_full) {
		    theRegistry.removePlugIn(newFullHash,pin,PlugInManager.CODEC);
		} else if ( b_def) {
		    theRegistry.removePlugIn(newDefHash,pin,PlugInManager.CODEC);
		}
	    }
	}
	// update MUX
	v = PlugInManager.getPlugInList(null, null, PlugInManager.MULTIPLEXER);
	len = v.size();
	
	for (int i = 0; i < len; i++) {
	    pin = (String)v.elementAt(i);
	    pin.trim();
	    newpin = pin.replace('.', '/') + ".class";
	    DBItem dbentry = (DBItem)CustomDB.get(newpin);
	    if ( dbentry != null && !dbentry.isMarked()){
		// PlugInManager.removePlugIn(pin,PlugInManager.MULTIPLEXER);
		if ( b_full) {
		    theRegistry.removePlugIn(newFullHash, pin,PlugInManager.MULTIPLEXER); 
		} else if ( b_def) {
		    theRegistry.removePlugIn(newDefHash, pin,PlugInManager.MULTIPLEXER);
		}
	    }
	}

	// update RENDERER
	v = PlugInManager.getPlugInList(null, null, PlugInManager.RENDERER);
	len = v.size();
	
	for (int i = 0; i < len; i++) {
	    pin = (String)v.elementAt(i);
	    pin.trim();
	    newpin = pin.replace('.', '/') + ".class";
	    DBItem dbentry = (DBItem)CustomDB.get(newpin);
	    if ( dbentry != null && !dbentry.isMarked()){ 
		// PlugInManager.removePlugIn(pin,PlugInManager.RENDERER);
		if (b_full) { 
		    theRegistry.removePlugIn(newFullHash, pin,PlugInManager.RENDERER);
		} else if (b_def) {
		    theRegistry.removePlugIn(newDefHash, pin,PlugInManager.RENDERER);
		}
	    }
	}
	
	// update Capture Devices
	v = CaptureDeviceManager.getDeviceList(null);
	len = v.size();
	String dname = null;

	for ( int i = 0; i < len; i++){
	    CaptureDeviceInfo info = (CaptureDeviceInfo)v.elementAt(i);
	    dname = info.getLocator().getProtocol();
	    
	    if ( !isCDSelected(dname)) {
		if ( b_full) { 
		    theRegistry.removeCaptureD(newFullHash, dname);
		} else if (b_def) {
		    theRegistry.removeCaptureD(newDefHash, dname);
		}
	    }
	} 

	
	// generate RegistyLib.java
	if ( b_full ) {
	    writeRegistryLib(newFullHash) ;
	} else if ( b_def) {
	    writeRegistryLib(newDefHash);
	}
	
	// compile RegistryLib
	try {
	    String[] cmdarr = new String[4];
	    cmdarr[0] = theRegistry.getJavacPath() + File.separator + "javac";
	    cmdarr[1] = "-d";
	    cmdarr[2] = workDir;
	    cmdarr[3] = workDir + File.separator + "RegistryLib.java";
	    
	    Process proc = Runtime.getRuntime().exec(cmdarr);
	    proc.waitFor();
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	// save the full registry to disk
	if (release >= 2 && b_full)
	    theRegistry.saveRegistry(newFullHash);

	progressDlg.updateValue(42);
	myyield();
	
    }
    
    private boolean isCDSelected(String dname) {
	DBItem dbentry = null;
	String pname;

	// System.out.println("dname = " + dname);
	pname = (String)(cdmap.get(dname));
	dbentry = (DBItem)CustomDB.get(pname);

	if ( dbentry != null && dbentry.isMarked())
	    return true;
	
	return false;
    }
    
    private void writeRegistryLib(Hashtable newhash) {
	byte[] properties;
	int i,j;
	
	properties = null;
	
	// write the newhash to a ByteOutputStream
	try {
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    ObjectOutputStream oos = new ObjectOutputStream(bos);
	    int tableSize = newhash.size();
	    oos.writeInt(tableSize);
	    oos.writeInt(theRegistry.versionNumber);
	    for (Enumeration e = newhash.keys(); e.hasMoreElements() ;) {
		String key = (String) e.nextElement();
		Object value = newhash.get(key);
		
		oos.writeUTF(key);    // write key as UTF chars.
		
		oos.writeObject(value);
		oos.flush();
	    }
	    oos.close();
	    properties = bos.toByteArray();
	} catch (Exception ex) {
	    ex.printStackTrace();
	    System.out.println("Sth is wrong when saving the registry");
	    return;
    }
	
	
	// write RegistryLib
	try {
	    String registryname = workDir + File.separator + "RegistryLib.java";
	    DataOutputStream ds = new DataOutputStream(new FileOutputStream(registryname));
	    ds.writeBytes("/* Generated by RegistryGen.\n   DO NOT EDIT.*/\n\n");
	    ds.writeBytes("package com.sun.media.util;\n\n");
	    ds.writeBytes("public abstract class ");
	    ds.writeBytes("RegistryLib");
	    ds.writeBytes(" {\n\n");
	    if (properties.length > 0) {
		ds.writeBytes("   public static byte[] getData(){\n");
		ds.writeBytes("       int i;\n");
		ds.writeBytes("       byte[] b= new byte["+properties.length+"];\n");
		ds.writeBytes("       for (i=0;i<b.length;i++)\n");
		// switch back between 255 and 0
		ds.writeBytes("          b[i] = (byte)(s.charAt(i)-1);\n");
		ds.writeBytes("       return b;\n");
		ds.writeBytes("    }\n");
	    } else {
		ds.writeBytes("   public static byte[] getData(){\n");
		ds.writeBytes("       return null;\n");
		ds.writeBytes("    }\n");
	    }
	    
	    ds.writeBytes("    private static String s = \n        ");
	    ds.writeBytes("\"");
	    int len= properties.length;
	    for (j = 0; j < len; j++) {
		// switch between 255 and 0 since 0 is more common
		ds.writeBytes( ("\\"+byte2oct((byte)(1+properties[j]))) );
		if ((j%16)==15) {
		    ds.writeBytes("\"+\n        \""  );
		}
	    }
	    ds.writeBytes("\";\n\n"  );
	    ds.writeBytes("}\n"); //trailer
	    ds.close();
	} catch (Exception ex) {
	    System.out.println("Sth wrong when writing RegsitryLib");
	}
	
  }
    
  
    // convert byte to its octal presentation (always 3 characters)
    private static String byte2oct(byte b) {
	int i=b&0xff;
	int dig3=i%8;
	int dig2=(i/8)%8;
	int dig1=i/64;
    return (""+dig1+""+dig2+""+dig3);
    }
}



    

