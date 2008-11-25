/*
 * @(#)PanelMediaSource.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.export;

import java.awt.*;
import java.awt.event.*;

import javax.media.*;
import javax.media.protocol.*;

import com.sun.media.util.JMFI18N;

import jmapps.util.*;
import jmapps.ui.*;
import jmapps.jmstudio.*;


public class PanelMediaSource extends JMPanel implements ActionListener, TextListener {

    private JMAppsCfg             cfgJMApps = null;

    private TextField               textSource;
    private Button                  buttonBrowseFile;
    private Button                  buttonBrowseCapture;

    private CaptureDialog           dlgCapture = null;
    private CaptureControlsDialog   dlgCaptureControls = null;
    private boolean                 boolSettingCaptureSource = false;

    private String                  strContentTypeDefault = null;

    private DataSource              dataSource = null;
    
    public PanelMediaSource ( JMAppsCfg cfgJMApps ) {
        super ();

        this.cfgJMApps = cfgJMApps;
        try {
            init ();
        }
        catch ( Exception exception ) {
            exception.printStackTrace ();
        }
    }

    public void setSourceUrl ( String strSourceUrl ) {
        if ( strSourceUrl != null  &&  strSourceUrl.trim().length() > 0 )
            textSource.setText ( strSourceUrl );
    }

    public String getSourceUrl () {
        String    strSourceUrl;

        strSourceUrl = textSource.getText ();
        return ( strSourceUrl );
    }

    public void setDataSource(DataSource ds) {
	    dataSource = ds;
    }

    public void setCaptureDlg (CaptureDialog cdlg) {
	this.dlgCapture = cdlg;
    }

    public void setJMStudioCfg ( JMAppsCfg cfgJMApps ) {
        String  strSourceUrl = null;

        this.cfgJMApps = cfgJMApps;
        if ( cfgJMApps != null )
            strSourceUrl = cfgJMApps.getLastTransmitRtpSource ();
        if ( strSourceUrl != null )
            textSource.setText ( strSourceUrl );
    }

    public void SaveData () {
        String    strSourceUrl;

        strSourceUrl = textSource.getText ();
        if ( cfgJMApps != null  &&  strSourceUrl != null )
            cfgJMApps.setLastTransmitRtpSource ( strSourceUrl );
    }

    public Processor createProcessor ()
    {
        String          strSourceUrl;
        MediaLocator    mediaSource;
        Processor       processor = null;
        boolean         boolResult;
        String          strContentType;


        strContentTypeDefault = null;
        dlgCaptureControls = null;

        if ( dataSource == null ) {
            if ( dlgCapture != null ) {
                dataSource = dlgCapture.createCaptureDataSource ();
                // dlgCapture = null;
            }
            else {
                strSourceUrl = getSourceUrl ();
                try {
                    mediaSource = new MediaLocator ( strSourceUrl );
                    dataSource = Manager.createDataSource ( mediaSource );
                }
                catch ( Exception exception ) {
                    MessageDialog.createErrorDialog ( getFrame(),
                        JMFI18N.getResource("jmstudio.error.datasource.createfor")
                        + " \'" + strSourceUrl + "\'. ", exception );
                }
            }
        }

        if ( dataSource == null ) {
            setCursor ( Cursor.getDefaultCursor() );
            return ( null );
        }

        if ( dataSource instanceof CaptureDevice ) {
            dlgCaptureControls = new CaptureControlsDialog ( getFrame(), dataSource );
        }

        strContentType = dataSource.getContentType();
        if ( strContentType != null )
            strContentTypeDefault = (new ContentDescriptor(strContentType)).toString ();

        try {
            processor = Manager.createProcessor ( dataSource );
        }
        catch ( Exception exception ) {
            MessageDialog.createErrorDialog ( this.getFrame(),
                        JMFI18N.getResource("jmstudio.error.processor.create"),
                        exception );
            exception.printStackTrace ();
            setCursor ( Cursor.getDefaultCursor() );
            return ( null );
        }

        return ( processor );
    }

    public String getDefaultContentType () {
        return ( strContentTypeDefault );
    }

    public CaptureControlsDialog getCaptureControlsDialog () {
        return ( dlgCaptureControls );
    }

    private void init () throws Exception {
        Panel    panel;
        Panel    panelDescription;
        Panel    panelEntry;
        Panel    panelButtons;
        Panel    panelButtons1;
        Panel    panelButtons2;
        Label    label;


        this.setLayout ( new BorderLayout(12,12) );

        panelDescription = new Panel ( new GridLayout(0,1) );
        this.add ( panelDescription, BorderLayout.NORTH );

        panelDescription.add ( new Label(JMFI18N.getResource("jmstudio.export.source.label1")) );
        panelDescription.add ( new Label(JMFI18N.getResource("jmstudio.export.source.label2")) );

        panel = new Panel ( new BorderLayout(6,6) );
        this.add ( panel, BorderLayout.CENTER );

        panelEntry = new Panel ( new BorderLayout(6,6) );
        panel.add ( panelEntry, BorderLayout.NORTH );

        textSource = new TextField ();
        textSource.addTextListener ( this );
        panelEntry.add ( textSource, BorderLayout.CENTER );

        panelButtons1 = new Panel ( new BorderLayout(6,6) );
        panel.add ( panelButtons1, BorderLayout.CENTER );
        panelButtons2 = new Panel ( new BorderLayout(6,6) );
        panelButtons1.add ( panelButtons2, BorderLayout.NORTH );
        panelButtons = new Panel ( new GridLayout(1,0,6,6) );
        panelButtons2.add ( panelButtons, BorderLayout.WEST );

        buttonBrowseFile = new Button ( JMFI18N.getResource("jmstudio.export.source.file") );
        buttonBrowseFile.addActionListener ( this );
        panelButtons.add ( buttonBrowseFile );

        buttonBrowseCapture = new Button ( JMFI18N.getResource("jmstudio.export.source.capture") );
        buttonBrowseCapture.addActionListener ( this );
        panelButtons.add ( buttonBrowseCapture );
    }


    public void actionPerformed ( ActionEvent event ) {
        String        strCmd;

        strCmd = event.getActionCommand ();
        if ( strCmd.equals(buttonBrowseFile.getLabel()) ) {
            browseFile ();
        }
        else if ( strCmd.equals(buttonBrowseCapture.getLabel()) ) {
            browseCapture ();
        }
    }

    public void textValueChanged ( TextEvent event ) {
        Object        objectSource;

        objectSource = event.getSource ();
        if ( objectSource == textSource ) {
            if ( boolSettingCaptureSource == true )
                boolSettingCaptureSource = false;
            else
                dlgCapture = null;
            dataSource = null;
        }
    }

    private void browseFile () {
        FileDialog    dlgFile;
        String          strFile;
        String        strDir;
        int           nIndex;

        strFile = textSource.getText ();
        try {
            dlgFile = new FileDialog ( getFrame(),
                        JMFI18N.getResource("jmstudio.export.source.filedialog"),
                        FileDialog.LOAD );
        }
        catch ( Exception exception ) {
            MessageDialog.createErrorDialog ( getFrame(),
                        JMFI18N.getResource("jmstudio.error.fgiledialog") );
            return;
        }

        nIndex = strFile.indexOf ( ':' );
        if ( nIndex > 0  &&  strFile.substring(0,nIndex).equalsIgnoreCase("file") ) {
            strFile = strFile.substring ( nIndex + 1 );
            dlgFile.setFile ( strFile );
        }
        dlgFile.show ();

        strFile = dlgFile.getFile ();
        strDir = dlgFile.getDirectory ();
        if ( strFile != null  &&  strFile.length() > 0 ) {
            strFile = "file:" + strDir + strFile;
            textSource.setText ( strFile );
            dlgCapture = null;
        }
    }

    private void browseCapture () {
        String     strAction;

        if ( dlgCapture == null )
            dlgCapture = new CaptureDialog ( getFrame(), cfgJMApps );
        dlgCapture.show ();
        strAction = dlgCapture.getAction ();
        if ( strAction.equals(dlgCapture.ACTION_OK) ) {
            boolSettingCaptureSource = true;
            textSource.setText ( dlgCapture.toString() );
        }
        else { // canceled
            dlgCapture = null;
        }
    }

}


