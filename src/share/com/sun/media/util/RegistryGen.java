/*
 * @(#)RegistryGen.java	1.63 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.io.*;
import java.util.*;
import javax.media.*;
import com.sun.media.util.DynamicPlugIn;
import javax.media.Format;


/**
 * A simple utility to create RegistryLib .
 *
 * To run:
 *	% javac RegistryGen.java
 *	% java RegistryGen [-d <destdir>] <registrylib>
 * The output is a file called registrylib.java at directory <destdir>
 * In case that registrylib contains a package it will be the registrylib's package
 *
 */

public class RegistryGen {
    static String arname;
    static String pkgname;
    static String destdir;
    static String names[];
    static DataOutputStream ds;
    static byte[] properties=null;
    private static String filename = null;
    private static Format [] emptyFormat = new Format[0];


    static {
	String fileSeparator = System.getProperty("file.separator");
	Registry.set("secure.allowCaching", new Boolean(true));
	Registry.set("secure.maxCacheSizeMB", new Integer(250)); // Megabytes
// 	if (fileSeparator.equals("/")) {
// 	    Registry.set("secure.cacheDir", "/tmp");
// 	} else {
// 	    Registry.set("secure.cacheDir", "C:" + fileSeparator + "temp");
// 	}
	Registry.set("secure.allowSaveFileFromApplets", new Boolean(false));
	Registry.set("secure.allowCaptureFromApplets", new Boolean(false));

	Registry.set("allowLogging", new Boolean(false));
	//For testing.
	//Registry.set("secure.logDir", "/tmp");

	try {
	    Registry.commit();
	} catch (Exception e) {
	    // ??
	}
    }

    /*
     * These are a list of Native PlugIns which are used by JMFPropertiesGen for
     * performance packs. This list is prepended to the Default PlugIns below.
     */
    static String [] nativePlugins = {

 	// Codecs

	"com.ibm.media.codec.video.mpeg.MpegVideo",
//	"com.ibm.media.codec.audio.mpega.NativeDecoder", //sbd: disable mpeg audio decoder

	"com.sun.media.codec.video.cinepak.NativeDecoder",
	"com.sun.media.codec.video.cinepakpro.NativeEncoder",
	"com.sun.media.codec.video.h261.NativeDecoder",
//      "com.sun.media.codec.video.h263.NativeDecoder",
	"com.sun.media.codec.video.vh263.NativeDecoder",
	"com.sun.media.codec.video.jpeg.NativeDecoder",
	//"com.sun.media.codec.video.IV32Decoder",

	"com.sun.media.codec.video.vcm.NativeDecoder",
	"com.sun.media.codec.video.vcm.NativeEncoder",

//	Disabled since we don't have right to ship them.
//
//	"com.sun.media.codec.audio.g729a.NativeEncoder",
//	"com.sun.media.codec.audio.g729a.NativeDecoder",
//	"com.sun.media.codec.audio.g729.NativeEncoder",
//	"com.sun.media.codec.audio.g729.NativeDecoder",
//	"com.sun.media.codec.audio.g728.NativeEncoder",
//	"com.sun.media.codec.audio.g728.NativeDecoder",

	"com.ibm.media.codec.audio.g723.NativeDecoder",
	"com.ibm.media.codec.audio.gsm.NativeDecoder",
	"com.ibm.media.codec.audio.gsm.NativeDecoder_ms",
	"com.ibm.media.codec.audio.ACMCodec",
	
	"com.sun.media.codec.video.jpeg.NativeEncoder",
	"com.ibm.media.codec.video.h263.NativeEncoder",
	"com.ibm.media.codec.audio.g723.NativeEncoder",
	"com.ibm.media.codec.audio.gsm.NativeEncoder",
	// "com.ibm.media.codec.audio.gsm.NativeEncoder_ms",
	"com.sun.media.codec.audio.mpa.NativeDecoder",
	"com.sun.media.codec.audio.mpa.NativeEncoder",

//	IBM's mp2 encoder is replaced by Sun's mp3 encoder.
//	"com.ibm.media.codec.audio.mpegenc.NativeEncoder",

	// Converters.
	"com.sun.media.codec.video.colorspace.YUVToRGB",
	//"com.sun.media.codec.video.colorspace.NativeRGBToRGB",  // Not yet implemented
	
	// Packetizers
	
	// Renderers.

	"com.sun.media.renderer.video.DDRenderer",
	"com.sun.media.renderer.video.GDIRenderer",
	"com.sun.media.renderer.video.SunRayRenderer",
	"com.sun.media.renderer.video.XILRenderer",
	"com.sun.media.renderer.video.XLibRenderer",
	
	// Commented out since if we put it here, then it will
	// take precedence over JavaSoundRenderer...
	// DONT ENABLE THIS WITHOUT MODIFYING JMFPropertiesGen
	// IT SPECIFICALLY TURNS THIS ON FOR WIN32 COMPILES
	//"com.sun.media.renderer.audio.DirectAudioRenderer",

	// Multiplexers.
    };

    /*
     * This is a list of the pure Java PlugIns, used for generating RegistryLib
     * and also the JMFProperties gen for the all-java package.
     */
    static String [] defaultPlugins = {
	
	// Parsers
	"com.ibm.media.parser.video.MpegParser",
	"com.sun.media.parser.audio.WavParser",
	"com.sun.media.parser.audio.AuParser",
	"com.sun.media.parser.audio.AiffParser",
	"com.sun.media.parser.audio.GsmParser",
	"com.sun.media.parser.RawStreamParser",
	"com.sun.media.parser.RawBufferParser",
	"com.sun.media.parser.RawPullStreamParser",
	"com.sun.media.parser.RawPullBufferParser",
	"com.sun.media.parser.video.QuicktimeParser",
	"com.sun.media.parser.video.AviParser",

//	Disabled since we don't have right to ship them.
//
//	"com.sun.media.parser.audio.G729AParser",
//	"com.sun.media.parser.audio.G729Parser",
//	"com.sun.media.parser.audio.G728Parser",
	
	// Codecs
	
//	Disabled since we don't have right to ship them.
//
//	"com.sun.media.codec.audio.g729a.JavaDecoder",
//	"com.sun.media.codec.audio.g729a.JavaEncoder",
//	"com.sun.media.codec.audio.g729.JavaDecoder",
//	"com.sun.media.codec.audio.g729.JavaEncoder",
//	"com.sun.media.codec.audio.g728.JavaDecoder",
//	"com.sun.media.codec.audio.g728.JavaEncoder",

	"com.sun.media.codec.audio.mpa.JavaDecoder",

	"com.sun.media.codec.video.cinepak.JavaDecoder",
	"com.ibm.media.codec.video.h263.JavaDecoder",
	"com.sun.media.codec.video.colorspace.JavaRGBConverter",
	"com.sun.media.codec.video.colorspace.JavaRGBToYUV",
	"com.ibm.media.codec.audio.PCMToPCM",
	"com.ibm.media.codec.audio.rc.RCModule",
	"com.sun.media.codec.audio.rc.RateCvrt",
	"com.sun.media.codec.audio.msadpcm.JavaDecoder",
	"com.ibm.media.codec.audio.ulaw.JavaDecoder",
	"com.ibm.media.codec.audio.alaw.JavaDecoder",
	"com.ibm.media.codec.audio.dvi.JavaDecoder",
	"com.ibm.media.codec.audio.g723.JavaDecoder",
	"com.ibm.media.codec.audio.gsm.JavaDecoder",
	"com.ibm.media.codec.audio.gsm.JavaDecoder_ms",
	"com.ibm.media.codec.audio.ima4.JavaDecoder",
	"com.ibm.media.codec.audio.ima4.JavaDecoder_ms",

	"com.ibm.media.codec.audio.ulaw.JavaEncoder",
	"com.ibm.media.codec.audio.dvi.JavaEncoder",
	"com.ibm.media.codec.audio.gsm.JavaEncoder",
	"com.ibm.media.codec.audio.gsm.JavaEncoder_ms",
	"com.ibm.media.codec.audio.ima4.JavaEncoder",
	"com.ibm.media.codec.audio.ima4.JavaEncoder_ms",
	
	// Packetizers
//	Disabled since we don't have right to ship them.
//
//	"com.sun.media.codec.audio.g729.Packetizer",
//	"com.sun.media.codec.audio.g729.DePacketizer",
//	"com.sun.media.codec.audio.g728.Packetizer",
//	"com.sun.media.codec.audio.g728.DePacketizer",

	"com.sun.media.codec.audio.ulaw.Packetizer",
	"com.sun.media.codec.audio.ulaw.DePacketizer",
	"com.sun.media.codec.audio.mpa.Packetizer",
	"com.sun.media.codec.audio.mpa.DePacketizer",
	"com.ibm.media.codec.audio.gsm.Packetizer",
	"com.ibm.media.codec.audio.g723.Packetizer",
	"com.sun.media.codec.video.jpeg.Packetizer",
	"com.sun.media.codec.video.jpeg.DePacketizer",
	"com.sun.media.codec.video.mpeg.Packetizer",
	"com.sun.media.codec.video.mpeg.DePacketizer",
	
	// Renderers.
	"com.sun.media.renderer.audio.JavaSoundRenderer",
	"com.sun.media.renderer.audio.SunAudioRenderer",
	"com.sun.media.renderer.video.AWTRenderer",
	"com.sun.media.renderer.video.LightWeightRenderer",
	"com.sun.media.renderer.video.JPEGRenderer",
	
	// Multiplexers.
	"com.sun.media.multiplexer.RawBufferMux",
	"com.sun.media.multiplexer.RawSyncBufferMux",
	"com.sun.media.multiplexer.RTPSyncBufferMux",
	"com.sun.media.multiplexer.audio.GSMMux",
	"com.sun.media.multiplexer.audio.MPEGMux",
	"com.sun.media.multiplexer.audio.WAVMux",
	"com.sun.media.multiplexer.audio.AIFFMux",
	"com.sun.media.multiplexer.audio.AUMux",
	"com.sun.media.multiplexer.video.AVIMux",
	"com.sun.media.multiplexer.video.QuicktimeMux",
    };


    static void printUsage() {
	System.err.println("java RegistryGen [-d <destdir>] [-j java] <registrylib> ");
	System.err.println("-j java: all-java");
    }

    static void writeClass() {
	int i,j;
	int accBytes=0;
	String name;

	try {

	    ds.writeBytes("/* Generated by RegistryGen.\n   DO NOT EDIT.*/\n\n");
	    if (pkgname != null) {
		ds.writeBytes("package ");
		ds.writeBytes(pkgname);
		ds.writeBytes(";\n\n");
	    }


	    ds.writeBytes("public abstract class ");
	    ds.writeBytes(arname);
	    ds.writeBytes(" {\n\n");

            /*
	    if (properties.length>0) {
		ds.writeBytes("public static byte[] properties = {\n");
		for (i = 0; i<properties.length-1; i++) {
		    ds.writeBytes("  "+properties[i]+",\n");
		}
		ds.writeBytes("  "+properties[i]+"    };\n");
	    }
	    else {
		ds.writeBytes("public static byte[] properties = null;\n");
	    }
            */

            if (properties.length>0) {
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
	   } catch (IOException e) {
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



    private static void registerPlugIn(String className) {
	try {
	    Class pic = Class.forName(className);
	    Object instance = pic.newInstance();
	    Format [] inputs = null;
	    Format [] outputs = null;
	    int type;

	    if (instance instanceof Demultiplexer) {
		type = PlugInManager.DEMULTIPLEXER;
		inputs = ((Demultiplexer)instance).getSupportedInputContentDescriptors();
		outputs = emptyFormat;
	    } else if (instance instanceof Codec) {
		type = PlugInManager.CODEC;
		inputs = ((Codec)instance).getSupportedInputFormats();
		outputs = ((Codec)instance).getSupportedOutputFormats(null);
	    } else if (instance instanceof Renderer) {
		type = PlugInManager.RENDERER;
		inputs = ((Renderer)instance).getSupportedInputFormats();
		outputs = emptyFormat;
	    } else if (instance instanceof Multiplexer) {
		type = PlugInManager.MULTIPLEXER;
		inputs = emptyFormat;
		outputs = ((Multiplexer)instance).getSupportedOutputContentDescriptors(null);
	    } else if (instance instanceof Effect) {
		type = PlugInManager.EFFECT;
		inputs = ((Effect)instance).getSupportedInputFormats();
		outputs = ((Effect)instance).getSupportedOutputFormats(null);
	    } else
		type = 0;
	    
	    if (instance instanceof DynamicPlugIn) {
		inputs = ((DynamicPlugIn)instance).getBaseInputFormats();
		outputs = ((DynamicPlugIn)instance).getBaseOutputFormats();
	    }
	    
	    if (type != 0) {
		boolean result = PlugInManager.addPlugIn(className, inputs, outputs, type);
	    }
	} catch (ClassNotFoundException cnfe) {
	} catch (Exception e) {
	} catch (UnsatisfiedLinkError erro) {
	}
    }

    private static void deletePlugins(int type) {
        Vector v = PlugInManager.getPlugInList(null,null,type);
        Enumeration eClassNames = v.elements();
        while (eClassNames.hasMoreElements()) {
	    String className = (String) eClassNames.nextElement();
            PlugInManager.removePlugIn(className,type);
	}
    }
    
    static void registerPlugIns(String[] plugins) {
	
	if (plugins==null)
	    return ;
	
        // delete the old plugin list before registration of the new list
        deletePlugins(PlugInManager.DEMULTIPLEXER);
        deletePlugins(PlugInManager.CODEC);
        deletePlugins(PlugInManager.RENDERER);
        deletePlugins(PlugInManager.MULTIPLEXER);
        deletePlugins(PlugInManager.EFFECT);
	
	for (int i = 0; i < plugins.length; i++)
	    registerPlugIn(plugins[i]);
	try {
	    PlugInManager.commit();
	} catch (Exception e) {
	    System.err.println("can't commit PlugInManager");
	}
	
    }
    

    static void registerCaptureDevices(javax.media.CaptureDeviceInfo cdis[]) {

	for (int i = 0; i < cdis.length; i++) {
	    //System.err.println("Register device: " + cdis[i].getName());
	    CaptureDeviceManager.addDevice(cdis[i]);
	}

	try {
	    CaptureDeviceManager.commit();
	} catch (java.io.IOException ioe) {
	    System.err.println("can't commit CaptureDeviceManager");
	}
    }

    
    private static boolean readProperties() {
	
        String classpath = null;
	try {
	    classpath = System.getProperty("java.class.path");
	} catch (Exception e) {
	    filename = null;
	    //System.err.println("Error: Couldn't read the CLASSPATH "+e);
	    return false;
	}

	// Search the directories in CLASSPATH for the first available jmf.properties
	
	StringTokenizer tokens = new StringTokenizer(classpath, File.pathSeparator);
	String dir;
	String strJMF = "jmf.properties";
	File file = null;
	
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
		    
		    if (sep == -1) {		              // no separator
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
		if (file.exists()) {
		    filename = dir;
		    break;
		}
	    } catch (Throwable t) {
		filename = null;
		return false;
	    }
	}
	
	try {
	    if (filename == null || file == null)
		return false;
	    if (file.length() == 0)
		return false;
	} catch (Throwable t) {
	    return false;
	}
	
	try {
	    FileInputStream fis = new FileInputStream(filename);
            DataInputStream dis = new DataInputStream(fis);
            int len=dis.available();
            properties=new byte[len];
            dis.read(properties,0,len);
            dis.close();
            fis.close();
	    
        } catch (IOException ioe) {
	    System.err.println("IOException in readProperties: " + ioe);
	    return false;
	}
	
        return true;
	
    }
    
    
    public static String[] findAllPlugInList(boolean allJava, String[] defaultList, String[] nativeList) {
	int i;
	String [] mergedList;
	
	if ( allJava ) {
	    // Use just the pure java plugins
	    mergedList = defaultList;
	} else {
	    int addDirectAudio = 0;
	    // Disable the DAR registry on win32 because its been
	    // moved to JMFInit (register only on installation)
	    if ( System.getProperty("os.name").startsWith("Solaris") ||
		 System.getProperty("os.name").startsWith("SunOS") ) {
		addDirectAudio = 1;
	    }
	    
	    // copy the native list first
	    mergedList = new String[nativeList.length +
				   addDirectAudio +
				   defaultList.length];
	    for (i = 0; i < nativeList.length; i++) {
		mergedList[i] = nativeList[i];
	    }
	    
	    // find the index of JavaSoundRenderer and put
	    // DirectAudioRenderer right behind it on solaris
	    // otherwise simply append the entire default list 
	    
	    if ( addDirectAudio == 0 ) {
		for (i = 0; i < defaultList.length; i++) {
		    mergedList[i + nativeList.length] =
			defaultList[i];
		}
	    } else { // on Solaris
		int idxJS = -1;
		for ( int j = 0; j < defaultList.length; j++ ) {
		    if ( defaultList[j].indexOf("JavaSoundRenderer") >= 0 ){
			idxJS = j;
			break;
		    }
		}
		
		if (idxJS >= 0 ) { // found JSRenderer
		    for ( int j = 0; j <= idxJS; j++)
			mergedList[nativeList.length+j] = defaultList[j];
		    
		    mergedList[nativeList.length+idxJS+1] = "com.sun.media.renderer.audio.DirectAudioRenderer"; 
		    
		    for ( int j = idxJS + 1; j < defaultList.length; j++)
			mergedList[nativeList.length+1+j] = defaultList[j];
		} else { // can not find JavaSound, then simply put DAR 
		    // at the end of defaultList
		    for ( int j = 0; j < defaultList.length; j++ )
			mergedList[nativeList.length+j] = defaultList[j];
		    mergedList[nativeList.length+defaultList.length] = "com.sun.media.renderer.audio.DirectAudioRenderer";
		}
	    }
	}
	
	return mergedList;
    }

    public static void main(String[] args) {
	int i, j, k;
	boolean allJava = false;
	
	// Parse the arguments.
	names = new String[args.length+1];
	for (i = 0, j = 0; i < args.length; i++) {
	    if (args[i].equals("-d")) {
		if (i++ >= args.length) {
		    printUsage();
		    return;
		}
		destdir = args[i];
	    } else if ( args[i].equals("-j")) {
		k = i+1;
		if ( k < args.length && args[k].equalsIgnoreCase("java")) {
		    allJava = true;
		    i++;
		} else {
		    allJava = false;
		}
	    }  else {
		names[j++] = args[i];
	    }
	}
	names[j] = null;
	
	if (j == 0) {
	    printUsage();
	    return;
	}

	// Determines the package and library name.
	i = names[0].lastIndexOf(".");
	if (i == -1) {
	    pkgname = null;
	    arname = names[0];
	} else {
	    pkgname = names[0].substring(0, i);
	    arname = names[0].substring(i + 1);
	}
	
	// Opens the destination file.
	String filename = null;
	try {
	    if (destdir == null)
		filename = arname + ".java";
	    else
		filename = destdir + File.separator + arname + ".java";
	    ds = new DataOutputStream(new FileOutputStream(filename));
	} catch (IOException e) {
	    System.err.println("Cannot open file: " + filename + e);
	}

	String[] mergedList = null;
	mergedList = findAllPlugInList(allJava, defaultPlugins, nativePlugins);
	
        registerPlugIns(mergedList);
        if (!readProperties()) {
	    System.err.println("Cannot read jmf.properties");
	    System.exit(0);
        }
	writeClass();
	System.exit(0);
    }



}

