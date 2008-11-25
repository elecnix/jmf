/*
 * @(#)PIMPanel.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.registry;

import java.awt.*;
import java.awt.event.*;
import javax.media.PlugInManager;
import java.util.Vector;
import java.util.Enumeration;
import com.sun.media.util.Registry;
import javax.media.format.*;
import java.io.IOException;
import javax.media.*;
import javax.media.renderer.*;

import com.sun.media.util.JMFI18N;
import com.sun.media.ui.TabControl;

import jmapps.ui.*;


public class PIMPanel extends JMPanel {

    TabControl tabs;
    
    public PIMPanel() {
        super();

        int             i;
        PerTypePanel    panelPerType;

        setLayout( new BorderLayout() );
        tabs = new TabControl ();
        add ( tabs, BorderLayout.CENTER );

        for ( i = 1;  i < PerTypePanel.pluginTypes.length;  i++ ) {
            panelPerType = new PerTypePanel ( i );
            tabs.addPage ( panelPerType, PerTypePanel.pluginTypes[i] );
        }
    }

}
    

