/*
 * @(#)HelperDiag.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.customizer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.event.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;


/**
 *  This dialog class provides the help information:
 *
 *  @version 2.0
 */

public class HelperDiag extends JDialog  {
    JEditorPane helpPane = null;
    int anch = 0;
    String[] helpcontent = new String[6];
    int FILELEN = 0;

    public HelperDiag(Customizer parent, int anch) {
	super(parent, I18N.getResource("HelperDiag.TITLE"), false);
	this.anch = anch;

	try {
	    loadHTML();
	    helpPane = new JEditorPane("text/html",helpcontent[anch]);
	    helpPane.setBackground(Color.lightGray);
	    helpPane.setEditable(false);
	    // setResizable(true);
	    this.getContentPane().setLayout(new BorderLayout());
	    this.getContentPane().add("Center", new JScrollPane(helpPane));
	} catch (Exception ex) {
	    System.out.println(I18N.getResource("HelperDiag.ERROR"));
	    ex.printStackTrace();
	}

	this.addWindowListener(new CloseAdapter(parent, this));
    }

    public void setHelpPage(int idx) {
	helpPane.setText(helpcontent[idx]);
    }

    private void loadHTML() throws Exception {
	InputStream fin = getHelpFileStream();
	if ( fin == null){
	    // System.out.println("Can't find CustomizerHelp.html");
	    // return;
	    // System.exit(0);
	    throw new IOException(I18N.getResource("HelperDiag.ERROR"));
	}
	BufferedReader br = new BufferedReader(new InputStreamReader((fin)));
	char[] rawhtml = new char[FILELEN];
	int[] idx = new int[6];

	StringBuffer[] sb = new StringBuffer[6];
	for ( int i = 0; i < 6; i++ )
	    sb[i] = new StringBuffer();

	int len = br.read(rawhtml, 0, FILELEN);
	br.close();
	int j = 0;

	for ( int i = 0; i < len-1; i++) {
	    if ( rawhtml[i] == '<' && rawhtml[i+1] == 'a')
		idx[j++] = i;
	}

	for ( int i = 0; i < 5; i++ ) {
	    sb[i].append(rawhtml, idx[i], idx[i+1]-idx[i]);
	    helpcontent[i] = sb[i].toString();
	}
	sb[5].append(rawhtml, idx[5], len - idx[5]);
	helpcontent[5] = sb[5].toString();
    }

    private InputStream  getHelpFileStream() throws Exception {
	String helpfile = "CustomizerHelp.html";
	String retstr = helpfile;
	String classpath = System.getProperty("java.class.path");
	String pathsep = System.getProperty("path.separator");
	StringTokenizer st = new StringTokenizer(classpath, pathsep);
	while (st.hasMoreTokens()) {
	    String path = st.nextToken();
	    if (path.endsWith(".jar")) {
		ZipFile zipfile = new ZipFile(path);
		ZipEntry entry = zipfile.getEntry(helpfile);
		if ( entry != null ) {
		    FILELEN = (int)entry.getSize();
		    return zipfile.getInputStream(entry);
		}
	    }
	}
	return null;
    }
    
}

class CloseAdapter extends WindowAdapter {
    JDialog target;
    Customizer parent;
    
    public CloseAdapter(Customizer parent, JDialog t) {
	this.parent = parent;
	target = t;
    }
    
    public void windowClosing(WindowEvent e) { 
	parent.dismissHelp();
	target.dispose();
    }
    
}



