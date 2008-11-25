/*
 * @(#)SettingsPanel.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.registry;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.IOException;

import com.sun.media.util.JMFI18N;
import com.sun.media.util.Registry;

import jmapps.ui.*;


public class SettingsPanel extends JMPanel implements ActionListener {

    private Checkbox    checkAllowCaching;
    // private TextField   textAllowFileRead;
    private Checkbox    checkAllowFileWrite;
    private Checkbox    checkAllowCapture;
    private TextField   textMaxCacheSize;
    private TextField   textCacheDir;
    private Checkbox    checkAllowLogging;
    private TextField   textLoggingDir;

    private Button      buttonCommit;
    private Button      buttonRestore;


    public SettingsPanel() {
        Panel   panel;
        Panel   panelButtons;
        Panel   panelData;
        Panel   panelEntry;
        Label   label;

        setLayout( new BorderLayout(6,6) );

        panel = new Panel ( new BorderLayout(6,6) );
        this.add ( panel, BorderLayout.NORTH );
        panelData = new Panel ( new GridLayout(0,1,6,6) );
        panel.add ( panelData, BorderLayout.WEST );


//         panelEntry = new Panel ( new BorderLayout(6,6) );
//         panelData.add ( panelEntry );
//         label = new Label ( JMFI18N.getResource("jmfregistry.settings.allowfileread") );
//         panelEntry.add ( label, BorderLayout.WEST );
//         textAllowFileRead = new TextField ( 32 );
//         panelEntry.add ( textAllowFileRead, BorderLayout.CENTER );

        panelEntry = new Panel ( new BorderLayout(6,6) );
        panelData.add ( panelEntry );
        checkAllowFileWrite = new Checkbox ( JMFI18N.getResource("jmfregistry.settings.allowfilewrite") );
        panelEntry.add ( checkAllowFileWrite, BorderLayout.WEST );

        panelEntry = new Panel ( new BorderLayout(6,6) );
        panelData.add ( panelEntry );
        checkAllowCapture = new Checkbox ( JMFI18N.getResource("jmfregistry.settings.allowcapture") );
        panelEntry.add ( checkAllowCapture, BorderLayout.WEST );

        panelEntry = new Panel ( new BorderLayout(6,6) );
        panelData.add ( panelEntry );
        checkAllowCaching = new Checkbox ( JMFI18N.getResource("jmfregistry.settings.allowcaching") );
        panelEntry.add ( checkAllowCaching, BorderLayout.WEST );

        panelEntry = new Panel ( new BorderLayout(6,6) );
        panelData.add ( panelEntry );
        label = new Label ( JMFI18N.getResource("jmfregistry.settings.cachedirectory") );
        panelEntry.add ( label, BorderLayout.WEST );
        textCacheDir = new TextField ( 32 );
        panelEntry.add ( textCacheDir, BorderLayout.CENTER );

        panelEntry = new Panel ( new BorderLayout(6,6) );
        panelData.add ( panelEntry );
        label = new Label ( JMFI18N.getResource("jmfregistry.settings.maxcache") );
        panelEntry.add ( label, BorderLayout.WEST );
        textMaxCacheSize = new TextField ();
        panelEntry.add ( textMaxCacheSize, BorderLayout.CENTER );

        panelEntry = new Panel ( new BorderLayout(6,6) );
        panelData.add ( panelEntry );
        checkAllowLogging = new Checkbox ( JMFI18N.getResource("jmfregistry.settings.allowlogging") );
        panelEntry.add ( checkAllowLogging, BorderLayout.WEST );

        panelEntry = new Panel ( new BorderLayout(6,6) );
        panelData.add ( panelEntry );
        label = new Label ( JMFI18N.getResource("jmfregistry.settings.loggingdirectory") );
        panelEntry.add ( label, BorderLayout.WEST );
        textLoggingDir = new TextField ();
        panelEntry.add ( textLoggingDir, BorderLayout.CENTER );

        panel = new Panel ( new FlowLayout(FlowLayout.CENTER) );
        this.add ( panel, BorderLayout.SOUTH );
        panelButtons = new Panel ( new GridLayout(1,0,12,12) );
        panel.add ( panelButtons );

        buttonCommit = new Button ( JMFI18N.getResource("jmfregistry.settings.commit") );
        buttonCommit.addActionListener( this );
        panelButtons.add ( buttonCommit );

        buttonRestore = new Button ( JMFI18N.getResource("jmfregistry.settings.restore") );
        buttonRestore.addActionListener( this );
        panelButtons.add ( buttonRestore );

        doRestore();
    }

    public void actionPerformed(ActionEvent ae) {
        Object source = ae.getSource();
        if (source == buttonCommit)
            doCommit();
        if (source == buttonRestore)
            doRestore();
    }

    private void doCommit() {
        boolean allowCaching = checkAllowCaching.getState();
        Registry.set("secure.allowCaching", new Boolean(allowCaching));
        boolean allowFileWrite = checkAllowFileWrite.getState();
        Registry.set("secure.allowSaveFileFromApplets", new Boolean(allowFileWrite));
        boolean allowCapture = checkAllowCapture.getState();
        Registry.set("secure.allowCaptureFromApplets", new Boolean(allowCapture));

        Registry.set("secure.cacheDir", textCacheDir.getText());
	// Registry.set("secure.additionalFileExtensions", textAllowFileRead.getText());

        boolean allowLogging = checkAllowLogging.getState();
        Registry.set("allowLogging", new Boolean(allowLogging));
        Registry.set("secure.logDir", textLoggingDir.getText());

        try {
            int cacheSize = Integer.parseInt(textMaxCacheSize.getText());
            Registry.set("secure.maxCacheSizeMB", new Integer(cacheSize));
        }
        catch (Throwable t) {
            MessageDialog.createErrorDialog ( getFrame(),
                                JMFI18N.getResource("jmfregistry.appname"),
                                JMFI18N.getResource("jmfregistry.error.cachesize") );
        }

        try {
            Registry.commit();
        }
        catch (IOException ioe) {
            MessageDialog.createErrorDialog ( getFrame(),
                                JMFI18N.getResource("jmfregistry.appname"),
                                JMFI18N.getResource("jmfregistry.error.commit") );
        }
    }

    private void doRestore() {
        Boolean allowCaching = (Boolean) Registry.get("secure.allowCaching");
        Boolean allowFile = (Boolean) Registry.get("secure.allowSaveFileFromApplets");
        Boolean allowCapture = (Boolean) Registry.get("secure.allowCaptureFromApplets");
        String  cacheDir = (String) Registry.get("secure.cacheDir");
        Integer cacheSize = (Integer) Registry.get("secure.maxCacheSizeMB");
        Boolean allowLogging = (Boolean) Registry.get("allowLogging");
        String  loggingDir = (String) Registry.get("secure.logDir");

        // String  fileExtensions = (String) Registry.get("secure.additionalFileExtensions");

        checkAllowCaching.setState ( (allowCaching != null  &&  ((Boolean)allowCaching).booleanValue()) );
        checkAllowFileWrite.setState ( (allowFile != null  &&  ((Boolean)allowFile).booleanValue()) );
        checkAllowCapture.setState ( (allowCapture != null  &&  ((Boolean)allowCapture).booleanValue()) );
        checkAllowLogging.setState ( (allowLogging != null  &&  ((Boolean)allowLogging).booleanValue()) );
        if (cacheDir != null)
            textCacheDir.setText(cacheDir);
        if (cacheSize != null)
            textMaxCacheSize.setText(cacheSize.toString());
//         if (fileExtensions != null)
//             textAllowFileRead.setText(fileExtensions);
        if (loggingDir != null)
            textLoggingDir.setText(loggingDir);
    }

}


