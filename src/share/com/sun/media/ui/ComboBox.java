/*
 * @(#)ComboBox.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package com.sun.media.ui;

import java.awt.*;
import java.awt.event.*;

public class ComboBox extends Panel implements ItemListener {

    /*************************************************************************
     * Variables
     *************************************************************************/
    
    TextField edit;
    Button bPullDown;
    PullDownList listWindow = null;
    List list = null;
    boolean mouseIn = false;

    /*************************************************************************
     * ComboBox Methods
     *************************************************************************/
    
    public ComboBox() {
	this(3);
    }
    
    public ComboBox(int cols) {
	setLayout( new BorderLayout() );
	
	edit = new TextField(cols);
	bPullDown = new Button("...");
	add("Center", edit);
	add("East", bPullDown);

	list = new List(6);
	list.setBackground(Color.white);
	list.addItemListener( this );

	bPullDown.addActionListener( new ActionListener() {
	    public void actionPerformed(ActionEvent ae) {
		pullDown();
	    }
	} );

	bPullDown.addMouseListener( new MouseAdapter() {
	    public void mouseEntered(MouseEvent me) {
		if (listWindow == null)
		    firstTime();
		mouseIn = true;
	    }

	    public void mouseExited(MouseEvent me) {
		mouseIn = false;
	    }
	} );
    }

    public void firstTime() {

	Component frame = this;
	while (!(frame instanceof Frame) && frame != null) {
	    frame = frame.getParent();
	}
	if (frame == null) {
	    System.out.println("No frame found in hierarchy");
	    System.exit(0);
	}
	listWindow = new PullDownList( (Frame) frame, list);
	listWindow.validate();
    }

    public void itemStateChanged(ItemEvent ie) {
	if (ie.getStateChange() == ItemEvent.SELECTED) {
	    String s = list.getSelectedItem();
	    edit.setText(s);
	    edit.selectAll();
	    pullDown();
	}
    }

    public void pullDown() {
	if (listWindow.isVisible() == false) {
	    listWindow.show(this);
	} else
	    listWindow.setVisible(false);
    }

    public void addActionListener(ActionListener al) {
	edit.addActionListener(al);
    }

    public void removeActionListener(ActionListener al) {
	edit.removeActionListener(al);
    }

    public void addItemListener(ItemListener il) {
	list.addItemListener(il);
    }

    public void removeItemListener(ItemListener il) {
	list.removeItemListener(il);
    }


    /*************************************************************************
     * Text Related Methods
     *************************************************************************/

     public String getText() {
	return edit.getText();
    }
    
     public void setEditable(boolean f) {
	edit.setEditable(f);
    }


    /*************************************************************************
     * List Related Methods
     *************************************************************************/
    
    public void add(String item) {
	list.add(item);
    }

    public void add(String item, int index) {
	list.add(item, index);
    }

    public void addItem(String item) {
	list.addItem(item);
    }

    public void addItem(String item, int index) {
	list.addItem(item, index);
    }

    public void delItem(int index) {
	list.delItem(index);
    }

    public String getSelectedItem() {
	return list.getSelectedItem();
    }

    public int getSelectedIndex() {
	return list.getSelectedIndex();
    }

    public void select(int index) {
	list.select(index);
	String s = list.getSelectedItem();
	edit.setText(s);
	edit.selectAll();
    }


    /*************************************************************************
     * Inner Classes
     *************************************************************************/

    class PullDownList extends Window {

	List list;
	int height = 100;
	int HEIGHT = 100;
	
	public PullDownList(Frame f, List list) {
	    super(f);
	    setLayout( null );
	    setBackground(Color.black);
	    this.list = list;
	    this.add(list);

	    list.addMouseListener( new MouseAdapter() {
		public void mouseEntered(MouseEvent e) {
		}
	    } );
	}

	public void show(Component parent) {
	    Point p = parent.getLocationOnScreen();
	    Dimension psize = parent.getSize();
	    height = list.getPreferredSize().height;
	    if (height == 0)
		height = HEIGHT;
	    list.setBounds(1, 1, psize.width-2, HEIGHT);
	    setBounds(p.x, p.y + psize.height, psize.width, HEIGHT + 2);
	    validate();
	    setVisible(true);
	}

	public List getList() {
	    return list;
	}
    }
}
 

