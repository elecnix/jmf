/*
 * @(#)ExportWizard.java	1.16 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.export;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.control.*;
import javax.media.protocol.*;
import javax.media.datasink.*;
import javax.media.rtp.*;

import com.sun.media.util.JMFI18N;
import com.sun.media.ui.PlayerWindow;

import jmapps.ui.*;
import jmapps.util.*;
import jmapps.jmstudio.*;


public class ExportWizard extends WizardDialog implements ControllerListener, DataSinkListener {

    protected PanelMediaSource          panelSource;
    protected PanelMediaTargetType      panelTargetType;
    protected PanelMediaTargetFormat    panelTargetFormat;
    protected PanelMediaTargetFile      panelTargetFile;
    protected PanelMediaTargetNetwork   panelTargetNetwork;

    protected String                    strTitle = JMFI18N.getResource("jmstudio.export.title");

    protected Processor                 processor = null;
    protected String                    strTargetType = null;
    protected boolean                   boolChangedProcessor = false;
    protected boolean                   boolChangedTargetType = false;

    protected String                    strFailMessage = null;

    protected DataSink                  dataSinkSave = null;
    protected ProgressDialog            dlgProgressSave = null;
    protected ProgressThread            threadProgressSave = null;

    protected TransmitPlayerWindow      playerTransmit = null;
    protected CaptureControlsDialog     dlgCaptureControls = null;

    protected Vector                    vectorWindowsLeft = new Vector ();

    private JMAppsCfg                   cfgJMApps = null;


    public ExportWizard ( String strTitle, Frame frame, String strSourceUrl, JMAppsCfg cfgJMApps ) {
    	super ( frame, strTitle, true, "logo.gif" );

        this.strTitle = strTitle;
        this.setTitle ( strTitle );

        this.cfgJMApps = cfgJMApps;
        panelSource.setJMStudioCfg ( cfgJMApps );
        panelSource.setSourceUrl ( strSourceUrl );
        panelTargetNetwork.setJMStudioCfg ( cfgJMApps );
    }

    public ExportWizard ( Frame frame, String strSourceUrl, JMAppsCfg cfgJMApps ) {
    	this ( JMFI18N.getResource("jmstudio.export.title"), frame, strSourceUrl, cfgJMApps );
    }

    public Vector getWindowsLeft () {
        return ( vectorWindowsLeft );
    }

    protected void init () throws Exception {
    	super.init ();

    	panelSource = new PanelMediaSource ( cfgJMApps );
    	panelTargetType = new PanelMediaTargetType ();
    	panelTargetFormat = new PanelMediaTargetFormat ();
    	panelTargetFile = new PanelMediaTargetFile ();
    	panelTargetNetwork = new PanelMediaTargetNetwork ( cfgJMApps );
    	this.setSize ( 480, 480 );
    	this.setLocation ( 100, 100 );
    }

    protected Panel getFirstPage () {
    	return ( panelSource );
    }

    protected Panel getLastPage () {
    	String	strTargetType;
    	Panel	panel = null;

    	strTargetType = panelTargetType.getType ();
    	if ( strTargetType.equals(PanelMediaTargetType.TYPE_FILE) )
            panel = panelTargetFile;
    	else if ( strTargetType.equals(PanelMediaTargetType.TYPE_NETWORK) )
    	    panel = panelTargetNetwork;
    	else if ( strTargetType.equals(PanelMediaTargetType.TYPE_SCREEN) )
    	    panel = panelTargetFormat;

    	return ( panel );
    }

    protected Panel getNextPage ( Panel panelPage ) {
    	Panel	panelPageNext = null;
    	String	strTargetType;

    	if ( panelPage == null ) {
    	    panelPageNext = getFirstPage ();
    	}
    	else if ( panelPage == panelSource ) {
    	    panelPageNext = panelTargetType;
    	}
    	else if ( panelPage == panelTargetType ) {
    	    panelPageNext = panelTargetFormat;
    	}
    	else if ( panelPage == panelTargetFormat ) {
    	    strTargetType = panelTargetType.getType ();
    	    if ( strTargetType.equals(PanelMediaTargetType.TYPE_FILE) )
    	    	panelPageNext = panelTargetFile;
    	    else if ( strTargetType.equals(PanelMediaTargetType.TYPE_NETWORK) )
    	    	panelPageNext = panelTargetNetwork;
    	    else if ( strTargetType.equals(PanelMediaTargetType.TYPE_SCREEN) )
    	    	panelPageNext = null;
    	}
    	else {
    	    panelPageNext = null;
    	}

    	return ( panelPageNext );
    }

    protected Panel getPrevPage ( Panel panelPage ) {
    	Panel	panelPagePrev = null;

    	if ( panelPage == null )
    	    panelPagePrev = getLastPage ();
    	else if ( panelPage == panelTargetNetwork )
    	    panelPagePrev = panelTargetFormat;
    	else if ( panelPage == panelTargetFile )
    	    panelPagePrev = panelTargetFormat;
    	else if ( panelPage == panelTargetFile )
    	    panelPagePrev = panelTargetFormat;
    	else if ( panelPage == panelTargetFormat )
    	    panelPagePrev = panelTargetType;
    	else if ( panelPage == panelTargetType )
    	    panelPagePrev = panelSource;
    	else
    	    panelPagePrev = null;

    	return ( panelPagePrev );
    }

    protected boolean onPageDone ( Panel panelPage ) {
    	if ( panelPage == panelSource ) {
            setCursor ( new Cursor(Cursor.WAIT_CURSOR) );
            processor = panelSource.createProcessor ();
            if ( processor != null ) {
                processor.addControllerListener ( this );
                configureProcessor ();
            }
            setCursor ( Cursor.getDefaultCursor() );

            if ( processor == null ) {
                panelSource.setDataSource ( null );
                this.setTitle ( strTitle );
                return ( false );
            }
            boolChangedProcessor = true;
            this.setTitle ( strTitle + " " + panelSource.getSourceUrl() );
            panelSource.SaveData ();
    	}
    	else if ( panelPage == panelTargetType ) {
            if ( strTargetType == null  ||  !panelTargetType.getType().equals(strTargetType) ) {
                strTargetType = panelTargetType.getType ();
                boolChangedTargetType = true;
            }
    	}
    	else if ( panelPage == panelTargetNetwork ) {
            if ( panelTargetNetwork.checkValidFields(true) == false )
                return ( false );
            else
                panelTargetNetwork.saveData ();
    	}
        return ( true );
    }

    protected boolean onPageActivate ( Panel panelPage ) {
        String    strContentType;

    	if ( panelPage == panelSource ) {
            this.setTitle ( strTitle );
        }
    	else if ( panelPage == panelTargetFormat ) {
            if ( boolChangedProcessor == true  ||  boolChangedTargetType == true ) {
                setCursor ( new Cursor(Cursor.WAIT_CURSOR) );
                strContentType = panelSource.getDefaultContentType ();
                panelTargetFormat.setProcessor ( processor, strContentType, strTargetType );
                boolChangedProcessor = false;
                boolChangedTargetType = false;
                setCursor ( Cursor.getDefaultCursor() );
            }
    	}
    	else if ( panelPage == panelTargetFile ) {
    	    // ...
    	}
    	else if ( panelPage == panelTargetNetwork ) {
            panelTargetNetwork.setTracks ( panelTargetFormat.getEnabledVideoTracks(),
                                           panelTargetFormat.getEnabledAudioTracks() );
    	}
        return ( true );
    }

    protected boolean onFinish () {
        boolean    boolResult;

        setCursor ( new Cursor(Cursor.WAIT_CURSOR) );
        if ( strTargetType == null )
            boolResult = false;
        else if ( strTargetType.equals(PanelMediaTargetType.TYPE_FILE) )
            boolResult = doSaveFile ();
        else if ( strTargetType.equals(PanelMediaTargetType.TYPE_NETWORK) )
            boolResult = doTransmit ();
        else if ( strTargetType.equals(PanelMediaTargetType.TYPE_SCREEN) )
            boolResult = doPreview ();
        else
            boolResult = false;
        setCursor ( Cursor.getDefaultCursor() );

        return ( boolResult );
    }

    private void configureProcessor () {
        boolean         boolResult;

        if ( processor == null )
            return;

        // wait for processor to be configured
        boolResult = waitForState ( processor, Processor.Configured );
        if ( boolResult == false ) {
//            MessageDialog.createErrorDialog ( frameOwner, JMFI18N.getResource("jmstudio.error.processor.configure") + " " + strFailMessage );
            MessageDialog.createErrorDialog ( frameOwner, strFailMessage );
            destroyProcessor ();
        }
    }

    private void realizeProcessor () {
        boolean         boolResult;

        if ( processor == null )
            return;

        // wait for processor to be configured
        boolResult = waitForState ( processor, Processor.Realized );
        if ( boolResult == false ) {
//            MessageDialog.createErrorDialog ( frameOwner, JMFI18N.getResource("jmstudio.error.processor.realize") + " " + strFailMessage );
            MessageDialog.createErrorDialog ( frameOwner, strFailMessage );
            destroyProcessor ();
        }
    }

    private void destroyProcessor () {
        if ( processor == null )
            return;
	processor.removeControllerListener(this);
        processor.close ();
        processor = null;
        panelSource.setDataSource ( null );
    }

    Object stateLock = new Object();
    boolean stateFailed = false;

    private synchronized boolean waitForState(Processor p, int state) {
	StateListener sl;
	p.addControllerListener(sl = new StateListener());
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
	p.removeControllerListener(sl);
        return ( !stateFailed );
    }


    private boolean doSaveFile () {
        int                 i;
        DataSource          dsOutput;
        MediaLocator        mediaDest;
        Object              arrControls[];
        int                 nMediaDuration;
        String              strFileName;
        MonitorControl      monitorControl = null;
        Component           monitor = null;

        if ( processor == null )
            return ( false );

        panelTargetFormat.updateProcessorFormat ();
        realizeProcessor ();
        if ( processor == null )
            return ( false );

        dsOutput = processor.getDataOutput ();
        if ( dsOutput == null ) {
            MessageDialog.createErrorDialog ( frameOwner, JMFI18N.getResource("jmstudio.error.processor.creatednooutput") );
            destroyProcessor ();
            return ( false );
        }

        try {
            strFileName = panelTargetFile.getFileName ();
            mediaDest = new MediaLocator ( "file:" + strFileName );
            dataSinkSave = javax.media.Manager.createDataSink ( dsOutput, mediaDest );
        }
        catch ( Exception exception ) {
            stopSaving ();
            MessageDialog.createErrorDialog ( frameOwner, exception );
            return ( false );
        }

        try {
            dataSinkSave.addDataSinkListener( this );
            monitorControl = (MonitorControl)
            processor.getControl("javax.media.control.MonitorControl");
            if (monitorControl != null)
                monitor = monitorControl.getControlComponent();

            Time duration = processor.getDuration();
            nMediaDuration = (int)duration.getSeconds();

            dataSinkSave.open ();
            dataSinkSave.start ();
            processor.start ();
            //dataSource.start ();

            if ( nMediaDuration > 0
                    &&  duration != Duration.DURATION_UNBOUNDED
                    &&  duration != Duration.DURATION_UNKNOWN )
                dlgProgressSave = new ProgressDialog ( frameOwner,
						       JMFI18N.getResource("jmstudio.saveprogress.title"),
						       0, nMediaDuration, this );
            else
                dlgProgressSave = new ProgressDialog ( frameOwner,
						       JMFI18N.getResource("jmstudio.saveprogress.title"),
						       JMFI18N.getResource("jmstudio.saveprogress.label"),
						       monitor, this );

            dlgProgressSave.setVisible ( true );

            threadProgressSave = new ProgressThread ( processor, dlgProgressSave );
            threadProgressSave.start ();

            vectorWindowsLeft.addElement ( dlgProgressSave );
        }
        catch ( Exception exception ) {
            stopSaving ();
            MessageDialog.createErrorDialog ( frameOwner, exception );
        }

        return ( true );
    }

    private boolean doTransmit () {
        int                     i;
        DataSource              dsOutput;
        MediaLocator            mediaDest;
        Object                  arrControls[];
        int                     nMediaDuration;
        String                  strFileName;
        PushBufferStream        arrStreams [];
        boolean                 boolSucceeded;
        boolean                 arrAudioEnabled [];
        boolean                 arrVideoEnabled [];
        String                  arrVideoAddresses [];
        String                  arrAudioAddresses [];
        String                  arrVideoPorts [];
        String                  arrAudioPorts [];
        String                  arrVideoTtls [];
        String                  arrAudioTtls [];
        SessionManager          managerSession;
        SendStream              streamSend;
        TrackControl            arrTrackControls [];
        Format                  format;
        int                     nVideoIndex;
        int                     nAudioIndex;
        String                  strAddress;
        String                  strPort;
        String                  strTtl;
        String                  strStreamLabel;
        String                  strAudio = JMFI18N.getResource("jmstudio.export.targetnw.audio");
        String                  strVideo = JMFI18N.getResource("jmstudio.export.targetnw.video");


        if ( processor == null )
            return ( false );

	panelSource.setCaptureDlg(null);

        panelTargetFormat.updateProcessorFormat ();
        realizeProcessor ();
        if ( processor == null )
            return ( false );

        dsOutput = processor.getDataOutput ();
        if ( dsOutput == null ) {
            MessageDialog.createErrorDialog ( frameOwner, JMFI18N.getResource("jmstudio.error.processor.creatednooutput") );
            destroyProcessor ();
            return ( false );
        }
        if ( !(dsOutput instanceof PushBufferDataSource) ) {
            MessageDialog.createErrorDialog ( frameOwner, JMFI18N.getResource("jmstudio.error.processor.outputincorrect") );
            destroyProcessor ();
            return ( false );
        }

        arrStreams = ((PushBufferDataSource)dsOutput).getStreams ();
        if (arrStreams == null) {
            MessageDialog.createErrorDialog ( frameOwner, JMFI18N.getResource("jmstudio.error.processor.outputempty") );
            destroyProcessor ();
            return ( false );
        }

        arrVideoEnabled = panelTargetFormat.getEnabledVideoTracks ();
        arrAudioEnabled = panelTargetFormat.getEnabledAudioTracks ();

        arrVideoAddresses = panelTargetNetwork.getVideoAddresses ();
        arrAudioAddresses = panelTargetNetwork.getAudioAddresses ();
        arrVideoPorts = panelTargetNetwork.getVideoPorts ();
        arrAudioPorts = panelTargetNetwork.getAudioPorts ();
        arrVideoTtls = panelTargetNetwork.getVideoTtls ();
        arrAudioTtls = panelTargetNetwork.getAudioTtls ();

        arrTrackControls = processor.getTrackControls ();
        nVideoIndex = 0;
        nAudioIndex = 0;

        boolSucceeded = false;

        for ( i = 0;  i < arrStreams.length;  i++ ) {
            format = arrStreams[i].getFormat ();
            if ( format instanceof VideoFormat ) {
                while ( (nVideoIndex < arrVideoEnabled.length) &&
			arrVideoEnabled[nVideoIndex] == false )
                    nVideoIndex++;

		if (nVideoIndex >= arrVideoEnabled.length)
		    continue;


                strAddress = arrVideoAddresses[nVideoIndex];
                strPort = arrVideoPorts[nVideoIndex];
                strTtl = arrVideoTtls[nVideoIndex];
                nVideoIndex++;
                strStreamLabel = strVideo + " " + nVideoIndex;
            } else if ( format instanceof AudioFormat ) {
                while ( (nAudioIndex < arrAudioEnabled.length) &&
			arrAudioEnabled[nAudioIndex] == false )
                    nAudioIndex++;

		if (nAudioIndex >= arrAudioEnabled.length)
		    continue;

                strAddress = arrAudioAddresses[nAudioIndex];
                strPort = arrAudioPorts[nAudioIndex];
                strTtl = arrAudioTtls[nAudioIndex];
                nAudioIndex++;
                strStreamLabel = strAudio + " " + nAudioIndex;
            } else
                continue;

            try {
                if ( strAddress.equals("...")  ||  strPort.equals("") )
                    continue;
                managerSession = JMFUtils.createSessionManager ( strAddress, strPort, strTtl, null );
                if ( managerSession == null ) {
                    MessageDialog.createErrorDialog ( frameOwner, JMFI18N.getResource("jmstudio.error.processor.createsessionmanager") );
                    destroyProcessor ();
                    return ( false );
                }
                streamSend = managerSession.createSendStream ( dsOutput, i );
                streamSend.start();
                if ( !boolSucceeded ) {
                    createTransmitWindow ();
                }
                boolSucceeded = true;
                addTransmitSessionManager ( managerSession, streamSend, strStreamLabel );
            } catch ( Exception exception ) {
                MessageDialog.createErrorDialog ( frameOwner, exception );
                destroyProcessor ();
                return ( false );
            }
        }

        dlgCaptureControls = panelSource.getCaptureControlsDialog ();
        if ( dlgCaptureControls != null  &&  !dlgCaptureControls.isEmpty() ) {
            dlgCaptureControls.setVisible ( true );
        }

        if ( !boolSucceeded ) {
            MessageDialog.createErrorDialog ( frameOwner, JMFI18N.getResource("jmstudio.error.processor.transmittracks") );
            destroyProcessor ();
        }

        return ( boolSucceeded );
    }

    protected void createTransmitWindow () {

        if ( processor == null )
            return;

        playerTransmit = new TransmitPlayerWindow ( processor );
        playerTransmit.addWindowListener ( this );
        vectorWindowsLeft.addElement ( playerTransmit );
    }

    protected void addTransmitSessionManager ( SessionManager mngrSession,
                        SendStream streamSend, String strStreamLabel ) {

        if ( playerTransmit == null )
            return;

        playerTransmit.addSessionManager ( mngrSession, streamSend, strStreamLabel );
    }



    private boolean doPreview () {
        DataSource    dsOutput;
        Player        player;
        PlayerWindow  windowPlayer;

        if ( processor == null )
            return ( false );

        panelTargetFormat.updateProcessorFormat ();
        realizeProcessor ();
        if ( processor == null )
            return ( false );

        dsOutput = processor.getDataOutput ();
        if ( dsOutput == null ) {
            MessageDialog.createErrorDialog ( frameOwner, JMFI18N.getResource("jmstudio.error.processor.creatednooutput") );
            destroyProcessor ();
            return ( false );
        }

        // Create a Player with the data source
        // Create a PlayerWindow for the Player (this will start the player)
        // Start the processor
        try {
            player = javax.media.Manager.createPlayer ( dsOutput );
            if ( player != null ) {
                windowPlayer = new PlayerWindow ( player, JMFI18N.getResource("jmstudio.playerwindow.preview") );
                vectorWindowsLeft.addElement ( windowPlayer );
                windowPlayer = new PlayerWindow ( processor, JMFI18N.getResource("jmstudio.playerwindow.transcoding") );
                vectorWindowsLeft.addElement ( windowPlayer );
            }
            else {
                MessageDialog.createErrorDialog ( frameOwner, JMFI18N.getResource("jmstudio.error.player.create") );
                destroyProcessor ();
                return ( false );
            }
        }
        catch ( Exception exception ) {
            MessageDialog.createErrorDialog ( frameOwner, JMFI18N.getResource("jmstudio.error.player.create"), exception );
            destroyProcessor ();
            return ( false );
        }

        return ( true );
    }

    public void actionPerformed ( ActionEvent event ) {
        String        strCmd;
        Object        objectSource;

        strCmd = event.getActionCommand ();
        if ( (strCmd.equals(ProgressDialog.ACTION_ABORT)
                        ||  strCmd.equals(ProgressDialog.ACTION_STOP))
                        &&  dataSinkSave != null ) {
            stopSaving ();
        }
        else if ( strCmd.equals(ProgressDialog.ACTION_PAUSE) &&  dataSinkSave != null ) {
            processor.stop ();
            dlgProgressSave.setPauseButtonText ( ProgressDialog.ACTION_RESUME );
            threadProgressSave.pauseThread ();
        }
        else if ( strCmd.equals(ProgressDialog.ACTION_RESUME) &&  dataSinkSave != null ) {
            processor.start ();
            dlgProgressSave.setPauseButtonText ( ProgressDialog.ACTION_PAUSE );
            threadProgressSave.resumeThread ();
        }
        else
            super.actionPerformed ( event );
    }

    public void controllerUpdate ( ControllerEvent event ) {
        if ( event instanceof ControllerErrorEvent ) {
            strFailMessage = JMFI18N.getResource ( "jmstudio.error.controller" )
                    + "\n" + ((ControllerErrorEvent)event).getMessage ();
//            MessageDialog.createErrorDialogModeless ( frameOwner, strFailMessage );
        }
        else if (event instanceof EndOfMediaEvent) {
            if ( strTargetType.equals(PanelMediaTargetType.TYPE_FILE) )
                stopSaving();
        }
    }

    public void dataSinkUpdate(DataSinkEvent event) {
        if ( event instanceof EndOfStreamEvent ) {
            closeDataSink();
//            MessageDialog.createInfoDialog ( frameOwner, "File has been saved." );
        } else if ( event instanceof DataSinkErrorEvent ) {
            stopSaving();
            MessageDialog.createErrorDialog ( frameOwner, JMFI18N.getResource("jmstudio.error.processor.writefile") );
        }
    }

    public void windowClosed ( WindowEvent event ) {
        Object          objSource;

        objSource = event.getSource ();
        if ( objSource == playerTransmit ) {
            if ( dlgCaptureControls != null ) {
                dlgCaptureControls.dispose ();
                dlgCaptureControls = null;
            }
            playerTransmit = null;
        }
        if ( getAction().equals(WizardDialog.ACTION_CANCEL) )
            destroyProcessor ();
    }

    private void stopSaving () {
        if ( threadProgressSave != null ) {
            threadProgressSave.terminateNormaly ();
            threadProgressSave = null;
        }
        if ( processor != null ) {
            processor.stop ();
            destroyProcessor ();
        }
        if ( dlgProgressSave != null ) {
            dlgProgressSave.dispose ();
            dlgProgressSave = null;
        }
    }

    private synchronized void closeDataSink() {
        if (dataSinkSave != null) {
            dataSinkSave.close ();
            dataSinkSave = null;
        }
    }

    class StateListener implements ControllerListener {
        public void controllerUpdate(ControllerEvent ce) {
            if ( ce instanceof ControllerClosedEvent )
                stateFailed = true;
            if ( ce instanceof ControllerEvent ) {
                synchronized (stateLock) {
                    stateLock.notifyAll();
                }
            }
        }
    }

}


