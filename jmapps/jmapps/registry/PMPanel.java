/*
 * @(#)PMPanel.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.registry;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import javax.media.PackageManager;

import com.sun.media.util.JMFI18N;

import jmapps.ui.*;


public class PMPanel extends JMPanel implements VectorEditor {

    private final static int PROTOCOL = 1;
    private final static int CONTENT = 2;

    VectorPanel panelProtocol;
    VectorPanel panelContent;

    Vector protocolValues = null;
    Vector contentValues  = null;


    public PMPanel() {
        super();

        setLayout ( new GridLayout(1,0,12,12) );

        panelProtocol = new VectorPanel ( JMFI18N.getResource("jmfregistry.protocolprefix.vector.label"),
                                                (VectorEditor)this, PROTOCOL );
        add ( panelProtocol );
        
        panelContent = new VectorPanel ( JMFI18N.getResource("jmfregistry.contentprefix.vector.label"),
                                                (VectorEditor)this, CONTENT );
        add ( panelContent );
    }

    /*************************************************************************
     * VectorEditor implementation
     *************************************************************************/

    public Vector getList(int code) {
        if (code == PROTOCOL) {
            protocolValues = (Vector) PackageManager.getProtocolPrefixList().clone();
            return protocolValues;
        }
        else {
            contentValues = (Vector) PackageManager.getContentPrefixList().clone();
            return contentValues;
        }
    }

    public void setList(int code, Vector list) {
        if (code == PROTOCOL)
            protocolValues = list;
        else
            contentValues = list;
    }

    public void commit(int code) {
        if (code == PROTOCOL) {
            PackageManager.setProtocolPrefixList(protocolValues);
            PackageManager.commitProtocolPrefixList();
        }
        else {
            PackageManager.setContentPrefixList(contentValues);
            PackageManager.commitContentPrefixList();
        }
    }

    public void selectedIndex(int code, int index) {
	// Nothing to to
    }

    public boolean addToList(int code, String value) {
        if (code == PROTOCOL) {
            if (protocolValues.indexOf(value) < 0)
                return true;
            else
                return false;
        }
        else {
            if (contentValues.indexOf(value) < 0)
                return true;
            else
                return false;
        }
    }
    
}


