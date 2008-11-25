/*
 * @(#)VectorPanel.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.registry;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import com.sun.media.util.JMFI18N;

import jmapps.ui.*;


public class VectorPanel extends JMPanel implements ItemListener, ActionListener {

    VectorEditor        editor;
    int                 code;

    TextField           textAdd;
    Vector              values;
    List                listBox;

    Button              buttonAdd;
    Button              buttonUp;
    Button              buttonDown;
    Button              buttonRemove;
    Button              buttonRestore;
    Button              buttonCommit;


    public VectorPanel ( String title, VectorEditor ve, int code ) {
        super();

        int     i;
        int     nCount;
        Panel   panelButtons;
        Panel   panelValues;
        Panel   panelAdd;
        Panel   panel;
        Label   label;
        Object  objValue;

        this.editor = ve;
        this.code = code;

        setLayout ( new BorderLayout(6,6) );

        panelValues = new Panel ( new BorderLayout(6,6) );
        this.add ( panelValues, BorderLayout.CENTER );
        panel = new Panel ( new BorderLayout(6,6) );
        this.add ( panel, BorderLayout.EAST );
        panelButtons = new Panel ( new GridLayout(0,1,6,6) );
        panel.add ( panelButtons, BorderLayout.NORTH );

        panelAdd = new Panel ( new BorderLayout(6,6) );
        panelValues.add ( panelAdd, BorderLayout.NORTH );
        label = new Label ( title );
        panelAdd.add ( label, BorderLayout.NORTH );
        textAdd = new TextField ( 20 );
        textAdd.addActionListener ( this );
        panelAdd.add ( textAdd, BorderLayout.CENTER );

        listBox = new List ();
        listBox.addItemListener ( this );
        panelValues.add ( listBox, BorderLayout.CENTER );
        values = editor.getList ( code );
        nCount = values.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            objValue = values.elementAt ( i );
            listBox.add ( objValue.toString() );
        }

        buttonAdd = new Button(JMFI18N.getResource("jmfregistry.vector.add"));
        buttonAdd.addActionListener(this);
        panelButtons.add(buttonAdd);

        buttonUp = new Button(JMFI18N.getResource("jmfregistry.vector.moveup"));
        buttonUp.addActionListener(this);
        panelButtons.add(buttonUp);

        buttonDown = new Button(JMFI18N.getResource("jmfregistry.vector.movedown"));
        buttonDown.addActionListener(this);
        panelButtons.add(buttonDown);

        buttonRemove = new Button(JMFI18N.getResource("jmfregistry.vector.remove"));
        buttonRemove.addActionListener(this);
        panelButtons.add(buttonRemove);

        buttonRestore = new Button(JMFI18N.getResource("jmfregistry.vector.restore"));
        buttonRestore.addActionListener(this);
        panelButtons.add(buttonRestore);

        buttonCommit = new Button(JMFI18N.getResource("jmfregistry.vector.commit"));
        buttonCommit.addActionListener(this);
        panelButtons.add(buttonCommit);

        updateButtons ();
    }

    private void refresh() {
        int     i;
        int     nCount;
        Object  objValue;

        listBox.removeAll();
        nCount = values.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            objValue = values.elementAt ( i );
            listBox.add ( objValue.toString() );
        }
        repaint ();
    }

    private void addNew() {
        String value = textAdd.getText();
        if ( value != null  &&  value.length() > 0  &&  editor.addToList(code,value)  ) {
            values.addElement(value);
            refresh();
        }
        else {
            MessageDialog.createErrorDialog ( getFrame(),
                                JMFI18N.getResource("jmfregistry.appname"),
                                JMFI18N.getResource("jmfregistry.error.additem") );
        }
        updateButtons ();
    }

    public void actionPerformed(ActionEvent ae) {
        Object source = ae.getSource();
        if (source == textAdd || source == buttonAdd)
            addNew();
        else if (source == buttonUp)
            moveUp();
        else if (source == buttonDown)
            moveDown();
        else if (source == buttonRemove)
            remove();
        else if (source == buttonRestore)
            restore();
        else if (source == buttonCommit)
            commit();
        updateButtons ();
    }

    public void itemStateChanged ( ItemEvent event ) {
        int     index;

        index = listBox.getSelectedIndex ();
        editor.selectedIndex ( code, index );
        updateButtons ();
    }

    public void moveUp() {
        int     index;
        Object  element;

        index = listBox.getSelectedIndex ();
	if (index == 0)
	    return;
        element = values.elementAt(index);
        values.removeElementAt(index);
        values.insertElementAt(element, index - 1);
        refresh();
        editor.setList ( code, values );
        listBox.select ( index - 1 );
        updateButtons ();
    }

    public void moveDown() {
        int     index;
        Object  element;

        index = listBox.getSelectedIndex ();
	if (index == listBox.getItemCount() - 1)
	    return;
        element = values.elementAt(index);
        values.removeElementAt(index);
        values.insertElementAt(element, index + 1);
        refresh();
        editor.setList(code, values);
        listBox.select(index + 1);
        updateButtons ();
    }

    public void remove() {
        int     index;
        int     nCount;

        index = listBox.getSelectedIndex ();
        if ( index < 0 )
        {
            updateButtons ();
            return;
        }
            
        values.removeElementAt(index);
        refresh();
        editor.setList(code,values);
        nCount = values.size ();
        if ( index >= nCount )
            index--;

        if ( nCount > 0 ) {
            listBox.select ( index );
        }
        else {
            index = -1;
        }

        editor.selectedIndex ( code, index );
        updateButtons ();
    }

    public void restore() {
        System.err.println("restoring");
        values = editor.getList(code);
        System.err.println("value = " + values);
        refresh();
        updateButtons ();
    }

    public void commit() {
        editor.commit(code);
        updateButtons ();
    }

    private void updateButtons () {
        int     index;
        int     nCount;

        nCount = values.size();
        index = listBox.getSelectedIndex();

        if ( index < 1 )
            buttonUp.setEnabled ( false );
        else
            buttonUp.setEnabled ( true );

        if ( index >= nCount - 1  ||  index < 0 )
            buttonDown.setEnabled ( false );
        else
            buttonDown.setEnabled ( true );

        if ( index < 0 )
            buttonRemove.setEnabled ( false );
        else
            buttonRemove.setEnabled ( true );
    }

}


