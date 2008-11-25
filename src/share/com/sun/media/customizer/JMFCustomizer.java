/*
 * @(#)JMFCustomizer.java	1.11 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

import java.awt.*;
import java.io.File;
import com.sun.media.customizer.*;
/**
 *  This is a wrapper class to quickly invoke customizer
 *
 *  @version 2.0
 */

public class JMFCustomizer {
    public static void main (String[] args) {
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	int w = 540;
	int h = 400;
	int x, y;
	x = screenSize.width/2 - w/2;
	y = screenSize.height/2 - h/2;
	
	String workDir = null;
	String javacpath = null;

	workDir = (String)System.getProperty("user.dir");
	javacpath = (String)System.getProperty("java.home");
	String endjavac = javacpath.substring(javacpath.length() -3);
	if ( endjavac.equalsIgnoreCase("jre")) {
	  int idx = javacpath.length() - 3;
	  javacpath = javacpath.substring(0, idx) + "bin";
	} else {
	  javacpath = javacpath + File.separator + "bin";
	}

	if ( args.length > 0 ) 
	  try {
	    for ( int i = 0; i < args.length; i++) {
	      if ( args[i].equals("-d") )
		workDir = args[i+1];
	      if ( args[i].equals("-p") )
		javacpath = args[i+1];
	    }
	  } catch (Exception ex) {
	    System.out.println("Usuage: java JMFCustomizer [-d <working-dir>] [-p <javac-path>]");
	    System.exit(0);
	  }
	
	File tmpf = new File(workDir);
	if (!tmpf.isDirectory() && !tmpf.canWrite()){
	  System.out.println("Working directory " + workDir + " either is not a directory or don't have write permission.");
	  System.exit(0);
	}

	workDir += File.separator + "cuswork";
	tmpf = new File(workDir);
	if (!tmpf.exists()) {
	  tmpf.mkdirs();
	}
	
	System.out.println("Working directory is " + workDir);
	
	CusRegistry theRegistry = new CusRegistry();
	if ( theRegistry.loadRegistry() ) {
	  theRegistry.setWorkDir(workDir);
	  theRegistry.setJavacPath(javacpath);
	  System.out.println("javac path = " + javacpath);
	  new Customizer(I18N.getResource("JMFCustomizer.Title"), x, y, w, h, theRegistry);
	} else {
	  System.out.println("Failed to load jmf registry");
	}
    }
  
  static void deleteWorkDir(File droot) {

  }

}	
