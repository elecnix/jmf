/*
 * @(#)GeneralPane.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.customizer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;
import java.util.zip.ZipFile;

/** 
 *  This class defines the general page
 *
 *  @version 2.0
 */

public class GeneralPane  extends JPanel implements ActionListener {
    public static final int MFILE = 0;
    public static final int RTPREC = 1;
    public static final int CAPTURE = 2;
    public static final int PLAY = 3;
    public static final int RTPTRANS = 4;
    public static final int TRANSCODE = 5;
    public static final int GUI = 6;
    public static final int STUDIO = 7;
    public static final int BEAN = 8;
    
    JFrame parent;
    String srcJARname = "jmf.jar";
    String dstJARname = "custom.jar";
    JButton bsrc, bdst;
    JTextField srcFile, dstFile;
    JCheckBox[] funcs = new JCheckBox[9];
    JCheckBox twoJar;
    boolean[] resultFuncs = new boolean[9];
    
    int release = 0;

    public GeneralPane(JFrame parent) {
	this.parent = parent;
	
	// create JAR file panel
	bsrc = new JButton(I18N.getResource("GeneralPane.Browse"));
	bdst = new JButton(I18N.getResource("GeneralPane.Browse"));
	
	bsrc.addActionListener(this);
	bdst.addActionListener(this);
	
	srcFile = new JTextField(srcJARname, 20);
	dstFile = new JTextField(dstJARname, 20);
	
	JPanel srcPane = new JPanel();
	JPanel dstPane = new JPanel();
	JPanel twoJarPane = new JPanel();
	
	srcPane.add(new JLabel(I18N.getResource("GeneralPane.SrcJar")));
	srcPane.add(srcFile);
	srcPane.add(bsrc);
	
        dstPane.add(new JLabel(I18N.getResource("GeneralPane.DstJar")));
	dstPane.add(dstFile);
	dstPane.add(bdst);
	
	twoJar = new JCheckBox(I18N.getResource("GeneralPane.twoJar"),false);
	twoJarPane.add(twoJar);
	
        JPanel jarPane = new JPanel(new GridLayout(3,1));
	jarPane.add(srcPane);
	jarPane.add(dstPane);
	jarPane.add(twoJarPane);
	
	// create main functionality panel
	JPanel funcPane = createFuncPane();
	
	this.setLayout(new BorderLayout());
	this.add("South", jarPane);
	this.add("Center", funcPane);
    }
    
    public void actionPerformed(ActionEvent evt) {
	if (evt.getSource() == bsrc) {
	    JFileChooser fd = new JFileChooser();
	    fd.setCurrentDirectory(new File("."));
	    fd.setSelectedFile(new File(srcJARname));
	    int result = fd.showOpenDialog(parent);
	    if ( result == JFileChooser.APPROVE_OPTION) {
		srcJARname = (fd.getSelectedFile() != null)?fd.getSelectedFile().getAbsolutePath():"nothing";
		srcFile.setText(srcJARname);
	    }

	} else if ( evt.getSource() == bdst) {
	    JFileChooser fd = new JFileChooser();
	    fd.setCurrentDirectory(new File("."));
	    fd.setSelectedFile(new File(dstJARname));
	    int result = fd.showSaveDialog(parent);
	    if ( result == JFileChooser.APPROVE_OPTION) {
		dstJARname = (fd.getSelectedFile() != null)?fd.getSelectedFile().getAbsolutePath():"nothing";
		dstFile.setText(dstJARname);
	    }
	}
    }

    public String getSrcJARName() {
        srcJARname = srcFile.getText();
	return srcJARname;
    }
    
    public String getDstJARName() {
        dstJARname = dstFile.getText();
	return dstJARname;
    }
    
    private JPanel createFuncPane() {
	funcs[MFILE] = new JCheckBox(I18N.getResource("GeneralPane.MFILES"), true);
	funcs[RTPREC] = new JCheckBox(I18N.getResource("GeneralPane.RTPRECV"), false);
	funcs[CAPTURE] = new JCheckBox(I18N.getResource("GeneralPane.CAPTURE"), false);
	funcs[PLAY] = new JCheckBox(I18N.getResource("GeneralPane.PLAY"), true);
	funcs[RTPTRANS] = new JCheckBox(I18N.getResource("GeneralPane.RTPTRANS"), false);
	funcs[TRANSCODE] = new JCheckBox(I18N.getResource("GeneralPane.TRANSCODE"), false);
	funcs[GUI] = new JCheckBox(I18N.getResource("GeneralPane.GUI"), true);
	funcs[STUDIO] = new JCheckBox(I18N.getResource("GeneralPane.DEMO"), false);
	funcs[BEAN] = new  JCheckBox(I18N.getResource("GeneralPane.BEAN"), false);

	// Source
	JPanel srcpanel = new JPanel(new GridLayout(3,1));
        srcpanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),I18N.getResource("GeneralPane.MSOURCE") ));
	for ( int i = 0; i < 3; i++)
	    srcpanel.add(funcs[i]);

	// Sink
	JPanel snkpanel = new JPanel(new GridLayout(3,1));
        snkpanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("GeneralPane.MSINK")));
	for ( int i = 3; i < 6; i++)
	    snkpanel.add(funcs[i]);

	// opt component
	JPanel optpanel = new JPanel(new GridLayout(3,1));
        optpanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("GeneralPane.OPTCOMP")));
	for ( int i = 7; i < 9; i++)
	    optpanel.add(funcs[i]);

	JPanel panel = new JPanel(new GridLayout(1,3));
	panel.add(srcpanel);
	panel.add(snkpanel);
	panel.add(optpanel);

	return (panel);
    }
    
    public boolean[] getState(){
	for (int i = 0; i < 9; i++) {
	    if (funcs[i].isEnabled() && funcs[i].isSelected())
		resultFuncs[i] = true;
	    else 
		resultFuncs[i] = false;
	} 
	return (resultFuncs);
    }

    public boolean checking(boolean[] func, JFrame parent) {
	    boolean vv;
	    
	    // at least 1 source and 1 sink
	    vv = func[0] || func[1] || func[2];
	    vv &= func[3] || func[4] || func[5];
            srcJARname = srcFile.getText(); 
            dstJARname = dstFile.getText();

	    if ( !vv) {
		JOptionPane.showMessageDialog(parent, I18N.getResource("GeneralPane.s1s1"), I18N.getResource("GeneralPane.WRONGSEL"), JOptionPane.ERROR_MESSAGE);
                return (false);
            }

	    // check the source jar file
            if (srcJARname == null || srcJARname.length() <= 0 ) {
		JOptionPane.showMessageDialog(parent, I18N.getResource("GeneralPane.INPUTSRCJAR") ,I18N.getResource("GeneralPane.BADSRCJAR") , JOptionPane.ERROR_MESSAGE);
                return (false);
            }

	    File srcFileHandle = new File(srcJARname);
	    if ( !srcFileHandle.exists() || srcFileHandle.length() == 0 ) {
		JOptionPane.showMessageDialog(parent, I18N.getResource("GeneralPane.SRCJARNOTEXIST"),I18N.getResource("GeneralPane.BADSRCJAR") , JOptionPane.ERROR_MESSAGE);
                return (false);
            }

	    try {
		ZipFile zp = new ZipFile(srcFileHandle);
		if ( zp.getEntry("com/sun/media/codec/video/cinepakpro/NativeEncoder$DC.class") != null ) {
		    release = 2; // SPP
		} else if ( zp.getEntry("com/ibm/media/codec/audio/ACMCodec.class") != null) {
		    release = 3; // WPP
		} else {
		    release = 1; // AJ
		}

		zp.close();
	    } catch ( Exception ex) {}
	    

	    // check the dst jar file
            if (dstJARname == null || dstJARname.length() <= 0 ) {
		JOptionPane.showMessageDialog(parent,I18N.getResource("GeneralPane.INPUTDSTJAR") , I18N.getResource("GeneralPane.BADTARJAR"), JOptionPane.ERROR_MESSAGE);
                return (false);
            }

	    if (srcJARname.equalsIgnoreCase(dstJARname)) {
		JOptionPane.showMessageDialog(parent,I18N.getResource("GeneralPane.SRCDSTJAR") , I18N.getResource("GeneralPane.BADTARJAR") , JOptionPane.ERROR_MESSAGE);
                return (false);
            }

	    // no error
	    return true;
    }

    public int getRelease() {
	return release;
    }

    public boolean genTwoJars() {
      return (twoJar.isEnabled() && twoJar.isSelected());
    }
}

