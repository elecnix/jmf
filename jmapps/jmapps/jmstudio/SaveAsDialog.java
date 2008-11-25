/*
 * @(#)SaveAsDialog.java	1.14 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.jmstudio;

import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.datasink.*;

import com.sun.media.util.JMFI18N;
import com.sun.media.ui.TabControl;
import com.sun.media.ui.PlayerWindow;
import com.sun.media.ui.AudioFormatChooser;
import com.sun.media.ui.VideoFormatChooser;

import jmapps.util.*;
import jmapps.ui.*;


/**
* This class is used to create modeless SaveAs dialog. Instanciating the class
* opens the dialog. It uses TabControl to allow user specify the parameters
* for each audio and video track. Classes TrackPanelAudio and TrackPanelVideo
* are used to compose the pages of the TabControl. When user presses "OK" button
* it calls system FileDialog. After specifying the target file, it creates
* Progress dialog, that allows to monitor the progress of saving media to the
* file, pause, resume and abort the process.
*/
public class SaveAsDialog extends JMDialog implements ControllerListener,
                                                DataSinkListener, ItemListener {

    private JMAppsCfg       cfgJMApps;
    private String          inputURL;
    private DataSource      dataSource = null;
    private Processor       processor = null;
    private DataSink        dataSink = null;
    private TrackControl    arrTrackControls [];
    private int             nAudioTrackCount = 0;
    private int             nVideoTrackCount = 0;
    private String          strContentType = null;
    private String          strContentTypeExt = null;
    private boolean         boolSaving = false;
    private ProgressDialog  dlgProgress = null;
    private ProgressThread  threadProgress = null;
    private Format          captureFormat = null;

    private TabControl      tabControl;
    private Hashtable       hashtablePanelsAudio = new Hashtable ();
    private Hashtable       hashtablePanelsVideo = new Hashtable ();
    private Button          buttonSave;
    private Button          buttonCancel;
    private Choice          comboContentType;

    private Image           imageAudioEn = null;
    private Image           imageAudioDis = null;
    private Image           imageVideoEn = null;
    private Image           imageVideoDis = null;

    private String          strFailMessage = null;

    private ContentDescriptor []       supportedCDs;

    private static final String    STR_MSVIDEO = JMFI18N.getResource("jmstudio.saveas.type.msvideo");
    private static final String    STR_QUICKTIME = JMFI18N.getResource("jmstudio.saveas.type.quicktime");
    private static final String    STR_AIFF = JMFI18N.getResource("jmstudio.saveas.type.aiff");
    private static final String    STR_GSM = JMFI18N.getResource("jmstudio.saveas.type.gsm");
    private static final String    STR_WAVE = JMFI18N.getResource("jmstudio.saveas.type.wave");
    private static final String    STR_BASIC_AUDIO = JMFI18N.getResource("jmstudio.saveas.type.basicaudio");
    private static final String    STR_MPEG_AUDIO = JMFI18N.getResource("jmstudio.saveas.type.mpegaudio");

    /**
    * This constructor creates object SaveAsDialog, fills it with controls
    * does the layout, displays it on the screen, and returns.
    * The dialog stays on the screen untill user presses button "OK" or "Cancel".
    * @param    frame      parent frame
    * @param    inputURL   source of the media
    * @param    format     possible capture format
    */
    public SaveAsDialog ( Frame frame, String inputURL, Format format, JMAppsCfg cfgJMApps ) {
        super ( frame, JMFI18N.getResource("jmstudio.saveas.title"), false );

        this.cfgJMApps = cfgJMApps;
        this.inputURL = inputURL;
        this.captureFormat = format;

        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
    * This constructor creates object SaveAsDialog, fills it with controls
    * does the layout, displays it on the screen, and returns.
    * The dialog stays on the screen untill user presses button "OK" or "Cancel".
    * @param    frame      parent frame
    * @param    inputURL   source of the media
    * @param    format     possible capture format
    */
    public SaveAsDialog ( Frame frame, DataSource dataSource, JMAppsCfg cfgJMApps ) {
        super ( frame, JMFI18N.getResource("jmstudio.saveas.title"), false );

        this.cfgJMApps = cfgJMApps;
        this.dataSource = dataSource;
        this.inputURL = "Capture";

        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
    * This method is called from the constructor. It performs all required
    * initialization, creates all controls, does the layout and puts the dialog
    * on the screen.
    * @exception    Exception
    */
    private void init () throws Exception {
        int             i;
        Frame           frame;
        Point           point;
        Panel           panel;
        Panel           panelButtons;
        JMPanel         panelBorder;
        MediaLocator    mediaSource;
        Format          format;
        boolean         boolResult;
        Dimension       dim;


        imageAudioEn = ImageArea.loadImage ( "audio.gif", this, true );
        imageAudioDis = ImageArea.loadImage ( "audio-disabled.gif", this, true );
        imageVideoEn = ImageArea.loadImage ( "video.gif", this, true );
        imageVideoDis = ImageArea.loadImage ( "video-disabled.gif", this, true );

        frameOwner.setCursor ( new Cursor(Cursor.WAIT_CURSOR) );

        if (dataSource == null) {
            try {
                mediaSource = new MediaLocator ( inputURL );
                dataSource = Manager.createDataSource ( mediaSource );
                // If its a capture datasource, set the format as specified by JMStudio
                if (captureFormat != null && dataSource instanceof CaptureDevice) {
                    FormatControl [] fcs = ((CaptureDevice)dataSource).getFormatControls();
                        if (fcs != null && fcs.length > 0) {
                            fcs[0].setFormat(captureFormat);
                        }
                }
            }
            catch ( Exception exception ) {
                MessageDialog.createErrorDialog ( frameOwner,
						  JMFI18N.getResource("jmstudio.error.datasource.createfor")
						  + " \'" + inputURL + "\'.", exception );
                frameOwner.setCursor ( Cursor.getDefaultCursor() );
                throw exception;
            }
        }

        strContentType = dataSource.getContentType ();
        try {
            processor = Manager.createProcessor ( dataSource );
        }
        catch ( NoPlayerException exception ) {
            MessageDialog.createErrorDialog ( frameOwner,
                        JMFI18N.getResource("jmstudio.error.processor.create"),
                        exception );
            frameOwner.setCursor ( Cursor.getDefaultCursor() );
            throw exception;
        }
        processor.addControllerListener ( this );

        // wait for processor to be configured
        boolResult = waitForState ( processor, Processor.Configured );
        if ( boolResult == false ) {
//            MessageDialog.createErrorDialog ( frameOwner,
//                        JMFI18N.getResource("jmstudio.error.processor.configure")
//                        + "\n" + strFailMessage );
            frameOwner.setCursor ( Cursor.getDefaultCursor() );
            return;
        }

        supportedCDs = processor.getSupportedContentDescriptors();

        arrTrackControls = processor.getTrackControls ();
        for ( i = 0;  i < arrTrackControls.length;  i++ ) {
            format = arrTrackControls[i].getFormat ();
            if ( format instanceof VideoFormat )
                nVideoTrackCount++;
            if ( format instanceof AudioFormat )
                nAudioTrackCount++;
        }

        this.setLayout ( new BorderLayout() );

        panelBorder = new JMPanel ( new BorderLayout(6,6) );
        panelBorder.setEmptyBorder ( 6, 6, 6, 6 );
        panelBorder.setBackground ( Color.lightGray );
        this.add ( panelBorder, BorderLayout.CENTER );

        panel = createPanelGeneral ();
        panelBorder.add ( panel, BorderLayout.NORTH );

        panel = createPanelProperties ();
        panelBorder.add ( panel, BorderLayout.CENTER );

        panel = new JMPanel ( new FlowLayout(FlowLayout.CENTER) );
        panelBorder.add ( panel, BorderLayout.SOUTH );
        panelButtons = createButtonPanel ( new String[] { ACTION_SAVE, ACTION_CANCEL } );
        panel.add ( panelButtons );

        changeContentType ();

        this.pack ();
        dim = this.getSize ();
        dim.width += 64;
        this.setSize ( dim );

        this.addWindowListener ( this );
        this.setResizable ( false );
        this.setVisible ( true );

        frameOwner.setCursor ( Cursor.getDefaultCursor() );
    }

    /**
    * This method is used by method init() to create the panel that contains
    * TabControl and its pages TrackPanelAudio or TrackPanelVideo for each track.
    * @return       created panel
    * @exception    Exception
    */
    private Panel createPanelProperties () throws Exception {
        int                     i;
        int                     nIndexAudio;
        int                     nIndexVideo;
        int                     nCount;
        Panel                   panel;
        TrackPanelAudio         panelAudio;
        TrackPanelVideo         panelVideo;
        Format                  format;
        String                  strTitle;
        String                  strAudio = JMFI18N.getResource("jmstudio.saveas.audio");
        String                  strVideo = JMFI18N.getResource("jmstudio.saveas.video");
        JMAppsCfg.TrackData     dataTrack;


        tabControl = new TabControl ( TabControl.ALIGN_TOP );

        nIndexAudio = 0;
        nIndexVideo = 0;
        nCount = arrTrackControls.length;
        for ( i = 0;  i < nCount;  i++ ) {
            format = arrTrackControls[i].getFormat ();
            if ( format instanceof AudioFormat ) {
                nIndexAudio++;
                if ( nAudioTrackCount < 2 )
                    strTitle = new String ( strAudio );
                else
                    strTitle = new String ( strAudio + " " + nIndexAudio );

                panelAudio = new TrackPanelAudio ( arrTrackControls[i], this );
                tabControl.addPage ( panelAudio, strTitle, imageAudioEn );
                hashtablePanelsAudio.put ( strTitle, panelAudio );

                if ( cfgJMApps != null ) {
                    dataTrack = cfgJMApps.getLastSaveFileTrackData ( strTitle );
                    if ( dataTrack != null )
                        panelAudio.setDefaults ( dataTrack.boolEnable, dataTrack.format );
                }
            }
            else if ( format instanceof VideoFormat ) {
                nIndexVideo++;
                if ( nVideoTrackCount < 2 )
                    strTitle = new String ( strVideo );
                else
                    strTitle = new String ( strVideo + " " + nIndexVideo );

                panelVideo = new TrackPanelVideo ( arrTrackControls[i], this );
                tabControl.addPage ( panelVideo, strTitle, imageVideoEn );
                hashtablePanelsVideo.put ( strTitle, panelVideo );

                if ( cfgJMApps != null ) {
                    dataTrack = cfgJMApps.getLastSaveFileTrackData ( strTitle );
                    if ( dataTrack != null )
                        panelVideo.setDefaults ( dataTrack.boolEnable, dataTrack.format );
                }
            }
        }

        return ( tabControl );
    }

    /**
    * This method is used by method init() to create the panel that contains
    * the choice of the media type for output.
    * @return       created panel
    * @exception    Exception
    */
    private Panel createPanelGeneral () throws Exception {
        Panel           panelGeneral;
        Panel           panelFormat;
        Label           label;
        String          strValue;


        panelGeneral = new Panel ( new GridLayout(0,1,4,4) );

        panelFormat = new Panel ( new BorderLayout() );
        panelGeneral.add ( panelFormat );

        label = new Label ( JMFI18N.getResource("jmstudio.saveas.format") );
        panelFormat.add ( label, BorderLayout.WEST );
        comboContentType = new Choice ();
        comboContentType.addItemListener ( this );

        for ( int i = 0; i < supportedCDs.length; i++ ) {
            if (!(supportedCDs[i] instanceof FileTypeDescriptor))
                continue;
            String ct = supportedCDs[i].getContentType();
            strValue = transCDToDesc ( ct );
            if ( nAudioTrackCount < 1 ) {
                if ( strValue.equals(STR_AIFF)
                        ||  strValue.equals(STR_GSM)
                        ||  strValue.equals(STR_WAVE)
                        ||  strValue.equals(STR_BASIC_AUDIO)
                        ||  strValue.equals(STR_MPEG_AUDIO) )
                    continue;
            }
            comboContentType.addItem ( strValue );
        }

        strValue = null;
        if ( cfgJMApps != null )
            strValue = cfgJMApps.getLastSaveFileContentType ();
        if ( strValue == null )
            strValue = transCDToDesc ( ContentDescriptor.mimeTypeToPackageName(strContentType) );
        comboContentType.select ( strValue );
        panelFormat.add ( comboContentType, BorderLayout.CENTER );

        return ( panelGeneral );
    }

    private String transCDToDesc(String strContentType) {
        if ( strContentType.equals(ContentDescriptor.mimeTypeToPackageName(FileTypeDescriptor.MSVIDEO)) )
            return ( STR_MSVIDEO );
        else if ( strContentType.equals(ContentDescriptor.mimeTypeToPackageName(FileTypeDescriptor.QUICKTIME)) )
            return ( STR_QUICKTIME );
        else if ( strContentType.equals(ContentDescriptor.mimeTypeToPackageName(FileTypeDescriptor.AIFF)) )
            return ( STR_AIFF );
        else if ( strContentType.equals(ContentDescriptor.mimeTypeToPackageName(FileTypeDescriptor.GSM)) )
            return ( STR_GSM );
        else if ( strContentType.equals(ContentDescriptor.mimeTypeToPackageName(FileTypeDescriptor.WAVE)) )
            return ( STR_WAVE );
        else if ( strContentType.equals(ContentDescriptor.mimeTypeToPackageName(FileTypeDescriptor.BASIC_AUDIO)) )
            return ( STR_BASIC_AUDIO );
        else if ( strContentType.equals(ContentDescriptor.mimeTypeToPackageName(FileTypeDescriptor.MPEG_AUDIO)) )
            return ( STR_MPEG_AUDIO );
        else
            return strContentType;
    }


    /**
    * This method is called, when the user presses button "OK". It opens system
    * FileDialog. After the user chooses the target file it initiates the saving
    * process, puts the Progress dialog on the screen, and launches the thread
    * to monitor the progress and update the Progress dialog.
    */
    private void doSave () {
        int                     i;
        MediaLocator            mediaDest;
        DataSource              dataSource;
        Object                  arrControls[];
        MonitorControl          monitorControl;
        boolean                 boolResult;
        String                  strFileContentType = null;
        AudioFormat             formatAudio;
        FileDialog              dlgFile;
        String                  strDirName = null;
        String                  strFileName = null;
        Enumeration             enumKeys;
        String                  strPanel;
        TrackPanelVideo        panelVideo;
        TrackPanelAudio        panelAudio;
        int                     nMediaDuration;
        Component               monitor = null;
        String                  strValue;
        TrackControl            trackControl;
        JMAppsCfg.TrackData   dataTrack;


        dlgFile = new FileDialog ( frameOwner, JMFI18N.getResource("jmstudio.saveas.filedialog"), FileDialog.SAVE );
        i = inputURL.indexOf ( ':' );
        if ( i > 2 )
            strFileName = inputURL.substring ( i + 1 );
        else
            strFileName = new String ( inputURL );

        if ( strFileName.indexOf('/') == 0  &&  strFileName.indexOf(':') == 2 )
            strFileName = strFileName.substring ( 1 );

        i = strFileName.lastIndexOf ( '.' );
        if ( i >= 0  &&  strContentTypeExt != null )
            strFileName = strFileName.substring ( 0, i + 1 ) + strContentTypeExt;
        else if ( i < 0 && strContentTypeExt != null )
            strFileName = "Unknown." + strContentTypeExt;

        strDirName = cfgJMApps.getLastSaveFileDir ();
        i = Math.max ( strFileName.lastIndexOf('/'), strFileName.lastIndexOf('\\') );
        if ( i > -1 ) {
            if ( strDirName == null )
                strDirName = strFileName.substring ( 0, i );
            strFileName = strFileName.substring ( i + 1 );
        }
        if ( strDirName != null )
            dlgFile.setDirectory ( strDirName );
        dlgFile.setFile ( strFileName );

        dlgFile.setVisible ( true );
        strFileName = dlgFile.getFile ();
        if ( strFileName == null  ||  strFileName.length() == 0 ) {
            // user pressed cancel on FileDialog
            return;
        }
        strDirName = dlgFile.getDirectory ();
        if ( strDirName != null ) {
            strFileName = strDirName.trim() + strFileName.trim();
            cfgJMApps.setLastSaveFileDir ( strDirName );
        }
        else
            strFileName = strFileName.trim();

        // looks like here is a good place to save default values for the next run
        if ( cfgJMApps != null ) {
            strValue = comboContentType.getSelectedItem ();
            cfgJMApps.setLastSaveFileContentType ( strValue );
        }

        setCursor ( new Cursor(Cursor.WAIT_CURSOR) );
        try {
            // configure processor
//            System.err.println("Output content type = " + strContentType);
            processor.setContentDescriptor( new FileTypeDescriptor(strContentType) );

            // go through parameters ...
            enumKeys = hashtablePanelsVideo.keys ();
            while ( enumKeys.hasMoreElements() ) {
                strPanel = (String) enumKeys.nextElement ();
                panelVideo = (TrackPanelVideo) hashtablePanelsVideo.get ( strPanel );
                panelVideo.updateTrack ();

                if ( cfgJMApps != null ) {
                    dataTrack = cfgJMApps.createTrackDataObject();
                    trackControl = panelVideo.getTrackControl ();
                    dataTrack.boolEnable = trackControl.isEnabled ();
                    dataTrack.format = trackControl.getFormat ();
                    cfgJMApps.setLastSaveFileTrackData ( dataTrack, strPanel );
                }
            }
            enumKeys = hashtablePanelsAudio.keys ();
            while ( enumKeys.hasMoreElements() ) {
                strPanel = (String) enumKeys.nextElement ();
                panelAudio = (TrackPanelAudio) hashtablePanelsAudio.get ( strPanel );
                panelAudio.updateTrack ();

                if ( cfgJMApps != null ) {
                    dataTrack = cfgJMApps.createTrackDataObject();
                    trackControl = panelAudio.getTrackControl ();
                    dataTrack.boolEnable = trackControl.isEnabled ();
                    dataTrack.format = trackControl.getFormat ();
                    cfgJMApps.setLastSaveFileTrackData ( dataTrack, strPanel );
                }
            }

            boolResult = waitForState ( processor, Processor.Realized );
            if ( boolResult == false ) {
//                MessageDialog.createErrorDialogModeless ( frameOwner,
//                                JMFI18N.getResource("jmstudio.error.processor.realize")
//                                + " " + strFailMessage );
                setCursor ( Cursor.getDefaultCursor() );
                processor.close ();
                this.dispose ();
                return;
            }
/*
            Control swc = processor.getControl("javax.media.control.StreamWriterControl");
            if (swc != null) {
                ((StreamWriterControl)swc).setStreamSizeLimit(1000000);
            }
*/
//            if ( strContentType == null )
                strFileContentType = "file:";
//            else
//                strFileContentType = "file." + strContentType + ":";

            dataSource = processor.getDataOutput ();
	    int sepIndex = strFileName.lastIndexOf(java.io.File.separatorChar);
	    String fileNameOnly;
	    if (sepIndex >= 0) {
		fileNameOnly = strFileName.substring(sepIndex+1,
						     strFileName.length());
	    } else {
		fileNameOnly = strFileName;
	    }
	    if (fileNameOnly.indexOf(".") == -1) {
		strFileName += ("." + strContentTypeExt);
	    }
            mediaDest = new MediaLocator ( strFileContentType + strFileName );
            dataSink = Manager.createDataSink ( dataSource, mediaDest );
            boolSaving = true;
            monitorControl = (MonitorControl)
            processor.getControl("javax.media.control.MonitorControl");
            if (monitorControl != null)
                monitor = monitorControl.getControlComponent();

            Time duration = processor.getDuration();
            nMediaDuration = (int)duration.getSeconds();

            dataSink.addDataSinkListener(this);
	    try {
		dataSink.open ();
	    } catch (Exception e) {
		processor.close();
		throw e;
	    }
//            if (captureFormat != null)
//                new PlayerWindow(processor, "Transcoding Processor", true, false);
//            else
                dataSink.start ();
            processor.start ();
//            dataSource.start ();

            if ( nMediaDuration > 0  &&  duration != Duration.DURATION_UNBOUNDED
                                     &&  duration != Duration.DURATION_UNKNOWN ) {
                dlgProgress = new ProgressDialog ( frameOwner,
                                JMFI18N.getResource("jmstudio.saveprogress.title"),
                                0, nMediaDuration, this );
            }
            else {
                dlgProgress = new ProgressDialog ( frameOwner,
                                JMFI18N.getResource("jmstudio.saveprogress.title"),
                                JMFI18N.getResource("jmstudio.saveprogress.label"),
                                monitor, this );
            }
            dlgProgress.setVisible ( true );

            threadProgress = new ProgressThread ( processor, dlgProgress );
            threadProgress.start ();

        }
        catch ( Exception exception ) {
//            stopSaving ();
            boolSaving = false;
            //exception.printStackTrace ();
            MessageDialog.createErrorDialogModeless ( frameOwner, null, exception );
        }

        setCursor ( Cursor.getDefaultCursor() );
        this.dispose ();
    }

    /**
    * This method overwrites the ActionListener method to process events
    * from buttons, track pages, and Progress dialog.
    * @param    event    action event
    */
    public void actionPerformed ( ActionEvent event ) {
        String        strCmd;
        Object        objectSource;

        strCmd = event.getActionCommand ();
        if ( strCmd.equals(ACTION_CANCEL) ) {
            stopSaving();
            this.dispose ();
        }
        else if ( strCmd.equals(ACTION_SAVE) ) {
            doSave ();
        }
        else if ( (strCmd.equals(ProgressDialog.ACTION_ABORT) ||
                            strCmd.equals(ProgressDialog.ACTION_STOP) )
                            && boolSaving == true ) {
            stopSaving ();
        }
        else if ( strCmd.equals(ProgressDialog.ACTION_PAUSE)  &&  boolSaving == true ) {
            processor.stop ();
            dlgProgress.setPauseButtonText ( ProgressDialog.ACTION_RESUME );
            threadProgress.pauseThread ();
        }
        else if ( strCmd.equals(ProgressDialog.ACTION_RESUME)  &&  boolSaving == true ) {
            processor.start ();
            dlgProgress.setPauseButtonText ( ProgressDialog.ACTION_PAUSE );
            threadProgress.resumeThread ();
        }
        else if ( strCmd.equals(AudioFormatChooser.ACTION_TRACK_ENABLED) ) {
            objectSource = event.getSource ();
            if ( objectSource instanceof TrackPanelAudio )
                tabControl.setPageImage ( (Panel)objectSource, imageAudioEn );
        }
        else if ( strCmd.equals(AudioFormatChooser.ACTION_TRACK_DISABLED) ) {
            objectSource = event.getSource ();
            if ( objectSource instanceof TrackPanelAudio )
                tabControl.setPageImage ( (Panel)objectSource, imageAudioDis );
        }
        else if ( strCmd.equals(VideoFormatChooser.ACTION_TRACK_ENABLED) ) {
            objectSource = event.getSource ();
            if ( objectSource instanceof TrackPanelVideo )
                tabControl.setPageImage ( (Panel)objectSource, imageVideoEn );
        }
        else if ( strCmd.equals(VideoFormatChooser.ACTION_TRACK_DISABLED) ) {
            objectSource = event.getSource ();
            if ( objectSource instanceof TrackPanelVideo )
                tabControl.setPageImage ( (Panel)objectSource, imageVideoDis );
        }
    }

    /**
    * This method overwrites the ItemListener method to monitor the users choice
    * of the media type, and notify track pages about the change.
    * @param    event    item state changed event
    */
    public void itemStateChanged ( ItemEvent event ) {
        Object              objectSource;

        objectSource = event.getSource ();
        if ( objectSource == comboContentType ) {
            changeContentType ();
        }
    }

    /**
    * If the user closes dialog using system menu, it does the cleanup.
    * @param    event    window event
    */
    public void windowClosing ( WindowEvent event ) {
        stopSaving();
        this.dispose ();
    }

    /**
    * This method looks for ControllerErrorEvent, and displays the Error dialog.
    * @param    event    controller event
    */
    public void controllerUpdate ( ControllerEvent event ) {
        if ( event instanceof ControllerErrorEvent ) {
            strFailMessage = ((ControllerErrorEvent)event).getMessage ();

            if ( boolSaving == true ) {
                stopSaving ();
                MessageDialog.createErrorDialogModeless ( frameOwner,
                                JMFI18N.getResource("jmstudio.error.processor.savefile")
                                + "\n" + JMFI18N.getResource("jmstudio.error.controller")
                                + "\n" + strFailMessage );
            }
            else {
                MessageDialog.createErrorDialogModeless ( frameOwner,
                                JMFI18N.getResource("jmstudio.error.controller")
                                + "\n" + strFailMessage );
            }
        }
        else if ( event instanceof EndOfMediaEvent ) {
            if ( boolSaving == true )
                stopSaving();
        }
    }

    /**
     * This method monitors the process of saving file for end of file, and
     * possible errors. It also does a cleanup, when one of those events occurs.
     * @param    event    file write event
     */
    public void dataSinkUpdate ( DataSinkEvent event ) {
        if ( event instanceof EndOfStreamEvent ) {
            closeDataSink();
//            MessageDialog.createInfoDialog ( frameOwner, "File has been saved." );
        }
        else if ( event instanceof DataSinkErrorEvent ) {
            stopSaving ();
            MessageDialog.createErrorDialogModeless ( frameOwner,
                        JMFI18N.getResource("jmstudio.error.processor.writefile") );
        }
    }

    private void closeDataSink() {
	synchronized (this) {
	    if (dataSink != null)
		dataSink.close();
	    dataSink = null;
	}
    }

    /**
     * This method cleans up after the completion of the file save procedure.
     */
    private void stopSaving () {
        boolSaving = false;

        if ( threadProgress != null ) {
            threadProgress.terminateNormaly ();
            threadProgress = null;
        }
        if ( processor != null ) {
            processor.stop ();
            processor.close ();
        }
        if ( dlgProgress != null ) {
            dlgProgress.dispose ();
            dlgProgress = null;
        }
    }

    /**
    * This method waits untill the processor enter the specified state, or
    * some failure occurs.
    * @param    nState    the state of processor (see Processor and Player constants)
    * @return   true if the state was reached, false otherwise
    */

    Object stateLock = new Object();
    boolean stateFailed = false;
    
    private synchronized boolean waitForState(Processor p, int state) {
        p.addControllerListener(new StateListener());
        stateFailed = false;
	
        if (state == Processor.Configured) {
            p.configure();
        }
        else if (state == Processor.Realized) {
            p.realize();
        }

        while (p.getState() < state && !stateFailed) {
	    synchronized (stateLock) {
            try {
                stateLock.wait();
            }
            catch (InterruptedException ie) {
                return false;
            }
	    }
        }

	    return ( !stateFailed );
    }

    /**
    * This method is called whenever user makes the choice of the target media
    * type. It notifies all track pages about the change.
    */
    private void changeContentType () {
        Enumeration         enumPanels;
        TrackPanelVideo    panelVideo;
        TrackPanelAudio    panelAudio;
        String              strValue;


        strValue = comboContentType.getSelectedItem ();
        if ( strValue.equals(STR_MSVIDEO) ) {
            strContentType = FileTypeDescriptor.MSVIDEO;
            strContentTypeExt = "avi";
        }
        else if ( strValue.equals(STR_QUICKTIME) ) {
            strContentType = FileTypeDescriptor.QUICKTIME;
            strContentTypeExt = "mov";
        }
        else if ( strValue.equals(STR_AIFF) ) {
            strContentType = FileTypeDescriptor.AIFF;
            strContentTypeExt = "aif";
        }
        else if ( strValue.equals(STR_GSM) ) {
            strContentType = FileTypeDescriptor.GSM;
            strContentTypeExt = "gsm";
        }
        else if ( strValue.equals(STR_WAVE) ) {
            strContentType = FileTypeDescriptor.WAVE;
            strContentTypeExt = "wav";
        }
        else if ( strValue.equals(STR_BASIC_AUDIO) ) {
            strContentType = FileTypeDescriptor.BASIC_AUDIO;
            strContentTypeExt = "au";
        }
        else if ( strValue.equals(STR_MPEG_AUDIO) ) {
            strContentType = FileTypeDescriptor.MPEG_AUDIO;
            strContentTypeExt = "mp3";
        }
        else {
            strContentType = strValue;
            strContentTypeExt = "movie";
        }

//        System.err.println("ChangeContentType = " + strContentType);
        if ( processor.setContentDescriptor(new FileTypeDescriptor(strContentType)) == null) {
            System.err.println ( "Error setting content descriptor on processor" );
        }
        enumPanels = hashtablePanelsVideo.elements ();
        while ( enumPanels.hasMoreElements() ) {
            panelVideo = (TrackPanelVideo) enumPanels.nextElement ();
            panelVideo.setContentType ( strContentType );
        }
        enumPanels = hashtablePanelsAudio.elements ();
        while ( enumPanels.hasMoreElements() ) {
            panelAudio = (TrackPanelAudio) enumPanels.nextElement ();
            panelAudio.setContentType ( strContentType );
        }

    }


    class StateListener implements ControllerListener {

        public void controllerUpdate ( ControllerEvent ce ) {
            if (ce instanceof ControllerClosedEvent)
                stateFailed = true;
            if (ce instanceof ControllerEvent)
                synchronized (stateLock) {
                    stateLock.notifyAll();
                }
        }
    }

}


