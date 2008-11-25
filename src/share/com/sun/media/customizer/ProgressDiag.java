/*
 * @(#)ProgressDiag.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.customizer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
/**
 *  This dialog class report the progress of customizing, including:
 *    how many classes have been loaded from source jar file
 *    how many classes have been written to target jar file
 *
 *  @version 2.0
 */

public class ProgressDiag extends JDialog implements ActionListener {
    String srcname;
    String dstname;
    JLabel srcfilesize, srcclasscount;
    JLabel dstfilesize, dstclasscount;
    JLabel note;
    JProgressBar pb;
    JButton doneButton;
    boolean twojars;

    public ProgressDiag(Frame parent, String srcname, String dstname, boolean twojars) {
	super(parent, I18N.getResource("ProgressDiag.DTITLE"), true);
	this.srcname = srcname;
	this.dstname = dstname;
	this.twojars = twojars;

	init();
	// pack();
    }

    public void updateValue(int val) {
	if ( val >= pb.getMinimum() && val <= pb.getMaximum())
	    pb.setValue(val);
	// repaint();
    }

    public void updateNote(String str) {
	note.setText(str);
    }

    public void updateSourceInfo(long fsize, int cno) {
	srcfilesize.setText(new Long(fsize).toString());
	srcclasscount.setText(new Integer(cno).toString());
    }

    public void enableDone() {
	doneButton.setEnabled(true);
    }

    public void sentErr(String msg) {
	JOptionPane.showMessageDialog(this, msg, I18N.getResource("ProgressDiag.ERR"), JOptionPane.ERROR_MESSAGE);
	this.dispose();
    }

    public void updateTargetInfo(long fsize, int cno) {
	dstfilesize.setText(new Long(fsize).toString());
	dstclasscount.setText(new Integer(cno).toString());
    }

    void init() {
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	int w = 420;
	int h = 350;
	this.setLocation(screenSize.width/2 - w/2,
			 screenSize.height/2 - h/2);
	this.setSize(w,h);
	setResizable(false);

	if ( srcname.length() > 40 )
	    srcname = nameOnly(srcname);

	BorderLayout borderlayout = new BorderLayout();
	//JPanel holder = new JPanel();
	//this.getContentPane().setLayout(borderlayout);
	//this.getContentPane().add("Center", holder);

	GridLayout grid31 = new GridLayout(3,1);
	JPanel srcPane = new JPanel(grid31);
	JPanel dstPane = new JPanel(grid31);
	JPanel barPane = new JPanel(grid31);
        srcPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("ProgressDiag.SRCJAR")));
        dstPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), I18N.getResource("ProgressDiag.TJAR")));

	// build srcPane, left alignment
	FlowLayout leftflow = new FlowLayout(FlowLayout.LEFT);
	JPanel srcnamePane = new JPanel(leftflow);
	srcnamePane.add(new JLabel(I18N.getResource("ProgressDiag.FN")));
	JLabel srcn = new JLabel(srcname);
	srcn.setForeground(Color.black);
	srcnamePane.add(srcn);
	JPanel srcsizePane = new JPanel(leftflow);
	srcsizePane.add(new JLabel(I18N.getResource("ProgressDiag.FS")));
	srcfilesize = new JLabel(" ");
	srcfilesize.setForeground(Color.black);
	srcsizePane.add(srcfilesize);
	JPanel srcclscountPane = new JPanel(leftflow);
	srcclscountPane.add(new JLabel(I18N.getResource("ProgressDiag.NOC")));
	srcclasscount = new JLabel(" ");
	srcclasscount.setForeground(Color.black);
	srcclscountPane.add(srcclasscount);
	srcPane.add(srcnamePane);
	srcPane.add(srcsizePane);
	srcPane.add(srcclscountPane);


	// build dstPane, left alignment
	JLabel dstn;
	
	if ( !twojars) {
	    if ( dstname.length() > 40 ) 
		dstname = nameOnly(dstname);
	    dstn = new JLabel(dstname);
	} else {
	    File df1 = new File(dstname);
	    String pp = df1.getParent() + File.separator;
	    String fn = df1.getName();
	    String newdst = pp + "core_" + fn + " & " + "plugin_" + fn;
	    if ( newdst.length() > 40 ) 
		newdst = nameOnly(newdst);
	    dstn = new JLabel(newdst);
	}

	JPanel dstnamePane = new JPanel(leftflow);
	dstnamePane.add(new JLabel(I18N.getResource("ProgressDiag.FN")));
	// JLabel dstn = new JLabel(dstname);
	dstn.setForeground(Color.black);
	dstnamePane.add(dstn);
	JPanel dstsizePane = new JPanel(leftflow);
	dstsizePane.add(new JLabel(I18N.getResource("ProgressDiag.FS")));
	dstfilesize = new JLabel(" ");
	dstfilesize.setForeground(Color.black);
	dstsizePane.add(dstfilesize);
	JPanel dstclscountPane = new JPanel(leftflow);
	dstclscountPane.add(new JLabel(I18N.getResource("ProgressDiag.NOC")));
	dstclasscount = new JLabel(" ");
	dstclasscount.setForeground(Color.black);
	dstclscountPane.add(dstclasscount);
	dstPane.add(dstnamePane);
	dstPane.add(dstsizePane);
	dstPane.add(dstclscountPane);

	// build barPane, center alignment
	JPanel notePane = new JPanel();
	JPanel buttonPane = new JPanel();
	note = new JLabel(I18N.getResource("ProgressDiag.LOAD"));
	note.setForeground(Color.black);
	notePane.add(note);
	doneButton = new JButton(I18N.getResource("ProgressDiag.DONE"));
	doneButton.addActionListener(this);
	doneButton.setEnabled(false);
	buttonPane.add(doneButton);

	pb = new JProgressBar();
	// pb.setSize(2*w/3, 25);
	JPanel pbPane = new JPanel();
	pbPane.add(pb);
	barPane.add(notePane);
	barPane.add(pbPane);
	barPane.add(buttonPane);

	// put them together
	JPanel holder = new JPanel(new GridLayout(2,1));
	holder.add(srcPane);
	holder.add(dstPane);
	this.getContentPane().setLayout(borderlayout);
	this.getContentPane().add("Center", holder);
	this.getContentPane().add("South", barPane);
	// holder.add(barPane);
    }

    public void actionPerformed(ActionEvent evt) {
	this.dispose();
    }

    private String nameOnly(String srcStr) {
	String ret = null;
	if (srcStr == null )
	    return null;

	int i = srcStr.lastIndexOf(File.separatorChar);
	ret = srcStr.substring(i+1);
	if ( ret != null && ret.length() > 40 ) 
	    ret = ret.substring(0, 40);
	return ret;
    }
}

	
    

    
