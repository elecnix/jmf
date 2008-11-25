/*
 * @(#)MimePanel.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.registry;

import java.util.*;
import java.awt.*;
import java.awt.List;
import java.awt.event.*;

import javax.media.*;

import com.sun.media.MimeManager;
import com.sun.media.util.JMFI18N;

import jmapps.ui.*;


public class MimePanel extends JMPanel implements ActionListener, ItemListener, TextListener {

    private Hashtable   htMimeTypes;

    private Button      buttonRestore;
    private Button      buttonCommit;
    private Button      buttonAdd;
    private Button      buttonRemove;

    private List        listBoxMimeType;
    private TextField   textMimeType;
    private List        listBoxMimeExt;
    private TextField   textMimeExt;


    public MimePanel() {
        super ();

        init ();
    }

    private void init () {
        Panel   panel;
        Panel   panelData;

        this.setLayout ( new BorderLayout(6,6) );

        panel = createPanelButtons ();
        this.add ( panel, BorderLayout.EAST );

        panelData = new Panel ( new GridLayout(1,0,12,12) );
        this.add ( panelData, BorderLayout.CENTER );

        panel = createPanelMimeTypes ();
        panelData.add ( panel );

        panel = createPanelMimeExt ();
        panelData.add ( panel );

        restore ();
        updateButtons ();
    }

    private Panel createPanelButtons () {
        Panel   panelButtons;
        Panel   panel;

        panelButtons = new Panel ( new FlowLayout(FlowLayout.LEFT) );

        panel = new Panel ( new GridLayout(0,1,12,12) );
        panelButtons.add ( panel );

        buttonAdd = new Button ( JMFI18N.getResource("jmfregistry.vector.add") );
        buttonAdd.addActionListener ( this );
        panel.add ( buttonAdd );

        buttonRemove = new Button ( JMFI18N.getResource("jmfregistry.vector.remove") );
        buttonRemove.addActionListener ( this );
        panel.add ( buttonRemove );

        buttonRestore = new Button ( JMFI18N.getResource("jmfregistry.vector.restore") );
        buttonRestore.addActionListener ( this );
//        panel.add ( buttonRestore );

        buttonCommit = new Button ( JMFI18N.getResource("jmfregistry.vector.commit") );
        buttonCommit.addActionListener ( this );
        panel.add ( buttonCommit );

        return ( panelButtons );
    }

    private Panel createPanelMimeTypes () {
        Panel   panel;
        Panel   panelTemp;
        Label   label;


        panel = new Panel ( new BorderLayout(6,6) );

        panelTemp = new Panel ( new BorderLayout(6,6) );
        panel.add ( panelTemp, BorderLayout.NORTH );

        label = new Label ( JMFI18N.getResource("jmfregistry.mime.type.label") );
        panelTemp.add ( label, BorderLayout.NORTH );

        textMimeType = new TextField ();
        textMimeType.addTextListener ( this );
        panelTemp.add ( textMimeType, BorderLayout.CENTER );

        listBoxMimeType = new List ();
        listBoxMimeType.addItemListener ( this );
        panel.add ( listBoxMimeType, BorderLayout.CENTER );

        return ( panel );
    }

    private Panel createPanelMimeExt () {
        Label   label;
        Panel   panel;
        Panel   panelTemp;


        panel = new Panel ( new BorderLayout(6,6) );

        panelTemp = new Panel ( new BorderLayout(6,6) );
        panel.add ( panelTemp, BorderLayout.NORTH );

        label = new Label ( JMFI18N.getResource("jmfregistry.mime.ext.label") );
        panelTemp.add ( label, BorderLayout.NORTH );

        textMimeExt = new TextField ();
        textMimeExt.addTextListener ( this );
        panelTemp.add ( textMimeExt, BorderLayout.CENTER );

        listBoxMimeExt = new List ();
        listBoxMimeExt.addItemListener ( this );
        panel.add ( listBoxMimeExt, BorderLayout.CENTER );

        return ( panel );
    }

    public void actionPerformed ( ActionEvent event ) {
        String          strCmd;
        Object          objSource;

        objSource = event.getSource ();
        strCmd = event.getActionCommand ();
        if ( strCmd.equals(buttonRestore.getActionCommand()) )
            restore ();
        if ( strCmd.equals(buttonCommit.getActionCommand()) )
            MimeManager.commit();
        else if ( objSource == buttonAdd )
            addExt ();
        else if ( objSource == buttonRemove )
            removeExt ();

        updateButtons ();
    }

    public void itemStateChanged ( ItemEvent event ) {
        Object      objSource;
        String      strValue;

        objSource = event.getSource ();
        if ( objSource == listBoxMimeType ) {
            strValue = listBoxMimeType.getSelectedItem ();
            textMimeType.setText ( strValue );
            fillExtList ();
        }
        else if ( objSource == listBoxMimeExt ) {
            strValue = listBoxMimeExt.getSelectedItem ();
            textMimeExt.setText ( strValue );
        }
        updateButtons ();
    }

    public void textValueChanged ( TextEvent event ) {
        updateButtons ();
    }

    private void restore () {
        Hashtable       htMimeExt;
        Enumeration     enumKeys;
        Object          objExt;
        Object          objType;
        String          strExt;
        String          strType;
        Vector          vectorExt;

        htMimeExt = MimeManager.getMimeTable ();
        htMimeTypes = new Hashtable ();

        enumKeys = htMimeExt.keys ();
        while ( enumKeys.hasMoreElements() ) {
            objExt = enumKeys.nextElement ();
            objType = htMimeExt.get ( objExt );
            strExt = objExt.toString ();
            strType = objType.toString ();

            if ( htMimeTypes.containsKey(strType) ) {
                vectorExt = (Vector)htMimeTypes.get ( strType );
            }
            else {
                vectorExt = new Vector ();
                htMimeTypes.put ( strType, vectorExt );
            }

            if ( !vectorExt.contains(strExt) )
                vectorExt.addElement ( strExt );
        }
        fillTypeList ();
    }

    private void fillTypeList () {
        Enumeration     enumKeys;
        Object          objType;

        listBoxMimeType.removeAll ();
        enumKeys = htMimeTypes.keys();
        while ( enumKeys.hasMoreElements() ) {
            objType = enumKeys.nextElement ();
            listBoxMimeType.add ( objType.toString() );
        }

//        if ( listBoxMimeType.getItemCount() > 0 )
//            listBoxMimeType.select ( 0 );
//        fillExtList ();
    }

    private void fillExtList () {
        int             i;
        int             nCount;
        String          strType;
        Vector          vectorExt;
        Object          objExt;

        listBoxMimeExt.removeAll ();
        strType = listBoxMimeType.getSelectedItem ();
        if ( strType == null )
            return;

        vectorExt = (Vector) htMimeTypes.get ( strType );
        nCount = vectorExt.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            objExt = vectorExt.elementAt ( i );
            listBoxMimeExt.add ( objExt.toString() );
        }

//        if ( listBoxMimeExt.getItemCount() > 0 )
//            listBoxMimeExt.select ( 0 );
    }

    private void addType () {
        String          strType;
        Vector          vectorExt;

        strType = textMimeType.getText().trim();
        if ( strType.length() < 1 )
            return;

        if ( !htMimeTypes.containsKey(strType) ) {
            vectorExt = new Vector ();
            htMimeTypes.put ( strType, vectorExt );
            listBoxMimeType.add ( strType );
        }

        selectItem ( listBoxMimeType, strType );
        fillExtList ();
    }

    private void removeType () {
        int             i;
        int             nCount;
        String          strType;
        String          strExt;
        Vector          vectorExt;
        boolean         boolResult = true;

        strType = textMimeType.getText().trim();
        if ( strType.length() < 1 )
            return;
        vectorExt = (Vector) htMimeTypes.get ( strType );

        nCount = vectorExt.size ();
        for ( i = nCount - 1;  i >= 0;  i-- ) {
            strExt = (String) vectorExt.elementAt ( i );
            boolResult = MimeManager.removeMimeType ( strExt );
            if ( boolResult == true ) {
                vectorExt.removeElement ( strExt );
                listBoxMimeExt.remove ( strExt );
            }
        }

        nCount = vectorExt.size ();
        if ( nCount > 0 ) {
            listBoxMimeExt.select ( 0 );
            MessageDialog.createErrorDialog ( new Frame(),
                                JMFI18N.getResource("jmfregistry.appname"),
                                JMFI18N.getResource("jmfregistry.error.removeitem") );
        }
        else {
            htMimeTypes.remove ( strType );
            i = getItemIndex ( listBoxMimeType, strType );
            listBoxMimeType.remove ( strType );
//            if ( i >= listBoxMimeType.getItemCount() )
//                i = listBoxMimeType.getItemCount() - 1;
//            if ( i >= 0 ) {
//                listBoxMimeType.select ( i );
//                fillExtList();
//            }
        }
    }

    private void addExt () {
        String          strType;
        String          strExt;
        Vector          vectorExt;
        boolean         boolResult = true;

        strExt = textMimeExt.getText().trim();
        if ( strExt.length() < 1 )
            return;
        strType = textMimeType.getText().trim();
        if ( strType.length() < 1 )
            return;

        if ( !(htMimeTypes.containsKey(strType)) )
            addType ();
        if ( !(htMimeTypes.containsKey(strType)) )
            return;

        vectorExt = (Vector) htMimeTypes.get ( strType );
        if ( !vectorExt.contains(strExt) ) {
            boolResult = MimeManager.addMimeType ( strExt, strType );
            if ( boolResult == true ) {
                vectorExt.addElement ( strExt );
                listBoxMimeExt.add ( strExt );
            }
            else {
                MessageDialog.createErrorDialog ( new Frame(),
                                JMFI18N.getResource("jmfregistry.appname"),
                                JMFI18N.getResource("jmfregistry.error.additem") );
            }
        }

        if ( boolResult == true )
            selectItem ( listBoxMimeExt, strExt );
    }

    private void removeExt () {
        int             i;
        String          strType;
        String          strExt;
        Vector          vectorExt;
        boolean         boolResult = true;

        strExt = textMimeExt.getText().trim();
        if ( strExt.length() < 1 )
            return;
        strType = textMimeType.getText().trim();
        if ( strType.length() < 1 )
            return;

        if ( !(htMimeTypes.containsKey(strType)) )
            return;

        vectorExt = (Vector) htMimeTypes.get ( strType );
        if ( !vectorExt.contains(strExt) )
            return;

        boolResult = MimeManager.removeMimeType ( strExt );
        if ( boolResult == true ) {
            vectorExt.removeElement ( strExt );
            i = getItemIndex ( listBoxMimeExt, strExt );
            listBoxMimeExt.remove ( strExt );
//            if ( i >= listBoxMimeExt.getItemCount() )
//                i = listBoxMimeExt.getItemCount() - 1;
//            if ( i >= 0 )
//                listBoxMimeExt.select ( i );
            if ( vectorExt.isEmpty() )
                removeType ();
        }
        else {
            MessageDialog.createErrorDialog ( new Frame(),
                        JMFI18N.getResource("jmfregistry.appname"),
                        JMFI18N.getResource("jmfregistry.error.removeitem") );
        }
    }

    private void selectItem ( List listBox, String strItemSel ) {
        int     i;

        i = getItemIndex ( listBox, strItemSel );
        if ( i >= 0 )
            listBox.select ( i );
    }

    private int getItemIndex ( List listBox, String strItemSel ) {
        int     i;
        int     nCount;
        String  strItem;

        nCount = listBox.getItemCount ();
        for ( i = 0;  i < nCount;  i++ ) {
            strItem = listBox.getItem ( i );
            if ( strItem.equals(strItemSel) )
                return ( i );
        }
        return ( -1 );
    }

    private void updateButtons () {
        String          strType;
        String          strExt;
        Vector          vectorExt;
        boolean         boolAddEnable;
        boolean         boolRemoveEnable;

        strExt = textMimeExt.getText().trim();
        strType = textMimeType.getText().trim();

        if ( strExt.length() > 1  &&  strType.length() > 1 ) {
            boolAddEnable = true;
            boolRemoveEnable = true;
        }
        else {
            boolAddEnable = false;
            boolRemoveEnable = false;
        }

        if ( htMimeTypes.containsKey(strType) ) {
            vectorExt = (Vector) htMimeTypes.get ( strType );
            if ( vectorExt.contains(strExt) )
                boolAddEnable = false;
            else
                boolRemoveEnable = false;
        }
        else {
            boolRemoveEnable = false;
        }

        buttonAdd.setEnabled ( boolAddEnable );
        buttonRemove.setEnabled ( boolRemoveEnable );
    }

}


