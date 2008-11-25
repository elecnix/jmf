/*
 * @(#)JMStudio.java	1.43 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

import java.util.*;
import java.net.*;
import java.lang.reflect.Method;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.media.*;
import javax.media.util.*;
import javax.media.format.*;
import javax.media.control.*;
import javax.media.protocol.*;
import javax.media.bean.playerbean.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;

import com.sun.media.util.JMFI18N;
import com.sun.media.rtp.RTPSessionMgr;

import jmapps.ui.*;
import jmapps.util.*;
import jmapps.rtp.*;
import jmapps.jmstudio.*;


public class JMStudio extends PlayerFrame implements ItemListener,
                                    ReceiveStreamListener {

    private Menu                    menuRecentUrl;
    private Menu                    menuAE;
    private Menu                    menuVE;
    private MenuItem                menuFileClose;
    private MenuItem                menuFileExport;
    private MenuItem                menuCapture;
    private CheckboxMenuItem        menuAutoPlay;
    private CheckboxMenuItem        menuAutoLoop;
    private CheckboxMenuItem        menuKeepAspect;
    private MenuItem                menuFullScreen;
    private MenuItem                menuSnapShot;
    private MenuItem                menuPlugins;
    private MenuItem                menuCaptureControl;
    private MenuItem                menuRtpSessionControl;
    private MenuItem                menuTransmissionStats;

    private Dimension               dimFrameSizeBeforeFullScreen = null;
    private Window                  windowFullScreen = null;
    private MouseListener           listenerMouseFullScreen;

    private Control                 controlPlugins = null;
    private Component               componentPlugins = null;
    private FrameGrabbingControl    controlGrabber = null;

    private FileDialog              dlgOpenFile = null;
    private JMFRegistry             jmfRegistry = null;

    private Vector                  vectorRtpFrames = null;
    private SnapFrame               frameSnap = null;
    private TransmissionStatsDialog dlgTransmissionStats = null;
    private SessionControlDialog    dlgSessionControl = null;

    private String                  strOptionalTitle = "";
    private DataSource              dataSourceCurrent = null;
    private String                  nameCaptureDeviceAudio = null;
    private String                  nameCaptureDeviceVideo = null;
    private String                  audioEffect = null;
    private String                  videoEffect = null;
    private CaptureControlsDialog   dlgCaptureControls = null;

    private RTPSessionMgr           mngrSessionRtp = null;
    private Vector                  vectorMngrSessions = null;
    private Vector                  vectorStreams = null;
    private Vector                  vectorStreamLabels = null;

    boolean killed = false;
    boolean recvRTP = false;

    public static final String APPNAME = JMFI18N.getResource ( "jmstudio.appname" );

    private static final String MENU_FILE                   = JMFI18N.getResource ( "jmstudio.menu.file" );
    private static final String MENU_FILE_NEWWINDOW         = JMFI18N.getResource ( "jmstudio.menu.file.newwindow" );
    private static final String MENU_FILE_OPENFILE          = JMFI18N.getResource ( "jmstudio.menu.file.openfile" );
    private static final String MENU_FILE_OPENURL           = JMFI18N.getResource ( "jmstudio.menu.file.openurl" );
    private static final String MENU_FILE_OPENRTP           = JMFI18N.getResource ( "jmstudio.menu.file.openrtp" );
    private static final String MENU_FILE_CAPTURE           = JMFI18N.getResource ( "jmstudio.menu.file.capture" );
    private static final String MENU_FILE_RECENTURL         = JMFI18N.getResource ( "jmstudio.menu.file.recent" );
    private static final String MENU_FILE_CLOSE             = JMFI18N.getResource ( "jmstudio.menu.file.close" );
    private static final String MENU_FILE_EXPORT            = JMFI18N.getResource ( "jmstudio.menu.file.export" );
    private static final String MENU_FILE_TRANSMIT          = JMFI18N.getResource ( "jmstudio.menu.file.transmit" );
    private static final String MENU_FILE_PREFERENCES       = JMFI18N.getResource ( "jmstudio.menu.file.preferences" );
    private static final String MENU_FILE_EXIT              = JMFI18N.getResource ( "jmstudio.menu.file.exit" );

    private static final String MENU_PLAYER                 = JMFI18N.getResource ( "jmstudio.menu.player" );
    private static final String MENU_PLAYER_AUTOPLAY        = JMFI18N.getResource ( "jmstudio.menu.player.autoplay" );
    private static final String MENU_PLAYER_AUTOLOOP        = JMFI18N.getResource ( "jmstudio.menu.player.autoloop" );
    private static final String MENU_PLAYER_KEEPASPECT      = JMFI18N.getResource ( "jmstudio.menu.player.keepaspect" );
    private static final String MENU_PLAYER_FULLSCREEN      = JMFI18N.getResource ( "jmstudio.menu.player.fullscreen" );
    private static final String MENU_PLAYER_SNAPSHOT        = JMFI18N.getResource ( "jmstudio.menu.player.snapshot" );
    private static final String MENU_PLAYER_PLUGINS         = JMFI18N.getResource ( "jmstudio.menu.player.plugins" );
    private static final String MENU_PLAYER_CAPTURE         = JMFI18N.getResource ( "jmstudio.menu.player.capturecontrols" );
    private static final String MENU_PLAYER_RTPSESSION      = JMFI18N.getResource ( "jmstudio.menu.player.rtpsession" );
    private static final String MENU_PLAYER_TRANSMISSION    = JMFI18N.getResource ( "jmstudio.menu.player.transmission" );

    private static final String MENU_HELP                   = JMFI18N.getResource ( "jmstudio.menu.help");
    private static final String MENU_HELP_ABOUT             = JMFI18N.getResource ( "jmstudio.menu.help.about");

    private static Vector       vectorFrames = new Vector (); // keeps count of all open JMStudio frames
    private static JMAppsCfg    cfgJMApps = null;
    private static double       dDefaultScale = 1.0;


    public JMStudio () {
        super ( null, APPNAME );
        updateMenu ();
	killed = false;
	recvRTP = false;
    }

    public void addNotify () {
        super.addNotify ();
    }

    public void pack () {
        super.pack ();
    }

    protected void initFrame () {
        createMenu ();
        super.initFrame ();
    }

    private void createMenu () {
        MenuBar         menu;
        Menu            menuFile;
        Menu            menuPlayer;
        Menu            menuHelp;
        MenuShortcut    shortcut;
        MenuItem        itemMenu;
        Vector          vector;
        boolean         boolValue;


        menu = new MenuBar ();
        this.setMenuBar ( menu );

        // menu File
        menuFile = new Menu ( MENU_FILE );
        menu.add ( menuFile );

        shortcut = new MenuShortcut ( KeyEvent.VK_N );
        itemMenu = new MenuItem ( MENU_FILE_NEWWINDOW, shortcut );
        itemMenu.addActionListener ( this );
        menuFile.add ( itemMenu );

        shortcut = new MenuShortcut ( KeyEvent.VK_O );
        itemMenu = new MenuItem ( MENU_FILE_OPENFILE, shortcut );
        itemMenu.addActionListener ( this );
        menuFile.add ( itemMenu );

        shortcut = new MenuShortcut ( KeyEvent.VK_U );
        itemMenu = new MenuItem ( MENU_FILE_OPENURL, shortcut );
        itemMenu.addActionListener ( this );
        menuFile.add ( itemMenu );

        shortcut = new MenuShortcut ( KeyEvent.VK_R );
        itemMenu = new MenuItem ( MENU_FILE_OPENRTP, shortcut );
        itemMenu.addActionListener ( this );
        menuFile.add ( itemMenu );

        shortcut = new MenuShortcut ( KeyEvent.VK_P );
        menuCapture = new MenuItem ( MENU_FILE_CAPTURE, shortcut );
        menuCapture.addActionListener ( this );
        menuFile.add ( menuCapture );
        vector = CaptureDeviceManager.getDeviceList ( null );
        if ( vector == null  ||  vector.size() < 1 )
            menuCapture.setEnabled ( false );
        else
            menuCapture.setEnabled ( true );

        menuRecentUrl = new Menu ( MENU_FILE_RECENTURL );
        updateRecentUrlMenu ();
        menuFile.add ( menuRecentUrl );

        shortcut = new MenuShortcut ( KeyEvent.VK_W );
        menuFileClose = new MenuItem ( MENU_FILE_CLOSE, shortcut );
        menuFileClose.addActionListener ( this );
        menuFile.add ( menuFileClose );

        menuFile.addSeparator ();

        shortcut = new MenuShortcut ( KeyEvent.VK_E );
        menuFileExport = new MenuItem ( MENU_FILE_EXPORT, shortcut );
        menuFileExport.addActionListener ( this );
        menuFile.add ( menuFileExport );

        shortcut = new MenuShortcut ( KeyEvent.VK_T );
        itemMenu = new MenuItem ( MENU_FILE_TRANSMIT, shortcut );
        itemMenu.addActionListener ( this );
        menuFile.add ( itemMenu );

        menuFile.addSeparator ();

        itemMenu = new MenuItem ( MENU_FILE_PREFERENCES );
        itemMenu.addActionListener ( this );
        menuFile.add ( itemMenu );

        shortcut = new MenuShortcut ( KeyEvent.VK_X );
        itemMenu = new MenuItem ( MENU_FILE_EXIT, shortcut );
        itemMenu.addActionListener ( this );
        menuFile.add ( itemMenu );

        // menu Player
        menuPlayer = new Menu ( MENU_PLAYER );
        menu.add ( menuPlayer );
        // It's a funny thing that we have to make this kind of a call at all,
        // less in this particular place. But if we don't do that here,
        // Windows will screw up the frame insets and therefore the layout.
        this.addNotify ();

        if ( cfgJMApps != null )
            boolValue = cfgJMApps.getAutoPlay ();
        else
            boolValue = false;
        menuAutoPlay = new CheckboxMenuItem ( MENU_PLAYER_AUTOPLAY, boolValue );
        menuAutoPlay.addItemListener ( this );
        menuPlayer.add ( menuAutoPlay );

        if ( cfgJMApps != null )
            boolValue = cfgJMApps.getAutoLoop ();
        else
            boolValue = true;
        menuAutoLoop = new CheckboxMenuItem ( MENU_PLAYER_AUTOLOOP, boolValue );
        menuAutoLoop.addItemListener ( this );
        menuPlayer.add ( menuAutoLoop );

        if ( cfgJMApps != null )
            boolValue = cfgJMApps.getKeepAspectRatio ();
        else
            boolValue = false;
        menuKeepAspect = new CheckboxMenuItem ( MENU_PLAYER_KEEPASPECT, boolValue );
        menuKeepAspect.addItemListener ( this );
        menuPlayer.add ( menuKeepAspect );

        menuPlayer.addSeparator ();

        shortcut = new MenuShortcut ( KeyEvent.VK_F );
        menuFullScreen = new MenuItem ( MENU_PLAYER_FULLSCREEN, shortcut );
        menuFullScreen.addActionListener ( this );
        menuPlayer.add ( menuFullScreen );

        shortcut = new MenuShortcut ( KeyEvent.VK_S );
        menuSnapShot = new MenuItem ( MENU_PLAYER_SNAPSHOT, shortcut );
        menuSnapShot.addActionListener ( this );
        menuPlayer.add ( menuSnapShot );

        menuPlayer.addSeparator ();

//        shortcut = new MenuShortcut ( KeyEvent.VK_V );
        menuPlugins = new MenuItem ( MENU_PLAYER_PLUGINS /*, shortcut*/ );
        menuPlugins.addActionListener ( this );
        menuPlayer.add ( menuPlugins );

//        shortcut = new MenuShortcut ( KeyEvent.VK_A );
        menuCaptureControl = new MenuItem ( MENU_PLAYER_CAPTURE /*, shortcut*/ );
        menuCaptureControl.addActionListener ( this );
        menuPlayer.add ( menuCaptureControl );

//        shortcut = new MenuShortcut ( KeyEvent.VK_I );
        menuRtpSessionControl = new MenuItem ( MENU_PLAYER_RTPSESSION /*, shortcut*/ );
        menuRtpSessionControl.addActionListener ( this );
        menuPlayer.add ( menuRtpSessionControl );

//        shortcut = new MenuShortcut ( KeyEvent.VK_M );
        menuTransmissionStats = new MenuItem ( MENU_PLAYER_TRANSMISSION /*, shortcut*/ );
        menuTransmissionStats.addActionListener ( this );
        menuPlayer.add ( menuTransmissionStats );

	// Add effects menus
	menuPlayer.addSeparator();
	Vector videoEffects = getEffectList(new VideoFormat(null));
	Vector audioEffects = getEffectList(new AudioFormat(null));
	if (videoEffects.size() > 0) {
	    menuVE = new Menu("Insert Video Effect");
	    fillEffectList(menuVE, videoEffects);
	    menuPlayer.add(menuVE);
	}
	if (audioEffects.size() > 0) {
	    menuAE = new Menu("Insert Audio Effect");
	    fillEffectList(menuAE, audioEffects);
	    menuPlayer.add(menuAE);
	}
        // menu Help
        menuHelp = new Menu ( MENU_HELP );
        menu.add ( menuHelp );

        shortcut = new MenuShortcut ( KeyEvent.VK_H );
        itemMenu = new MenuItem ( MENU_HELP_ABOUT, shortcut );
        itemMenu.addActionListener ( this );
        menuHelp.add ( itemMenu );
    }

    public void actionPerformed ( ActionEvent event ) {
        String      strCmd;
        Object      objSource;
        String      nameUrl;
        Frame       frame;
        Component   component;


        strCmd = event.getActionCommand ();
        objSource = event.getSource ();
        if ( strCmd == null  &&  objSource instanceof MenuItem )
            strCmd = ((MenuItem)objSource).getActionCommand ();

        if ( strCmd == null )
            return;

        if ( strCmd.equals(MENU_FILE_NEWWINDOW) ) {
            createNewFrame ();
        }
        else if ( strCmd.equals(MENU_FILE_OPENFILE) ) {
            openFile ();
        }
        else if ( strCmd.equals(MENU_FILE_OPENURL) ) {
            openUrl ();
        }
        else if ( strCmd.equals(MENU_FILE_OPENRTP) ) {
            openRtp ();
        }
        else if ( strCmd.equals(MENU_FILE_CAPTURE) ) {
            captureMedia ();
        }
        else if ( strCmd.equals(MENU_FILE_RECENTURL) ) {
            if ( objSource instanceof MenuItem ) {
                nameUrl = ((MenuItem)objSource).getLabel ();
                open ( nameUrl );
            }
        }
        else if ( strCmd.equals(MENU_FILE_CLOSE) ) {
            killCurrentPlayer ();
//            setPlaceholder ();
        }
        else if ( strCmd.equals(MENU_FILE_EXPORT) ) {
            exportMedia ();
        }
        else if ( strCmd.equals(MENU_FILE_TRANSMIT) ) {
            transmitMedia ();
        }
        else if ( strCmd.equals(MENU_FILE_PREFERENCES) ) {
            if ( jmfRegistry == null )
                jmfRegistry = new JMFRegistry ();
            jmfRegistry.setVisible ( true );
            jmfRegistry.addWindowListener(
                new WindowAdapter () {
                    public void windowClosing ( WindowEvent event ) {
                        Vector  vector;
                        vector = CaptureDeviceManager.getDeviceList ( null );
                        if ( vector == null  ||  vector.size() < 1 )
                            menuCapture.setEnabled ( false );
                        else
                            menuCapture.setEnabled ( true );
                    }
                }
            );
        }
        else if ( strCmd.equals(MENU_FILE_EXIT) ) {
            closeAll ();
        }

        else if ( strCmd.equals(MENU_PLAYER_AUTOPLAY) ) {
        }
        else if ( strCmd.equals(MENU_PLAYER_AUTOLOOP) ) {
        }
        else if ( strCmd.equals(MENU_PLAYER_KEEPASPECT) ) {
        }
        else if ( strCmd.equals(MENU_PLAYER_FULLSCREEN) ) {
            setFullScreen ( true );
        }
        else if ( strCmd.equals(MENU_PLAYER_SNAPSHOT) ) {
            doSnapShot ();
        }
        else if ( strCmd.equals(MENU_PLAYER_PLUGINS) ) {
            if ( componentPlugins != null ) {
                componentPlugins.setVisible ( true );
            }
            else {
                if ( controlPlugins != null  &&  controlPlugins instanceof Component ) {
                    componentPlugins = (Component)controlPlugins;
                    componentPlugins.setVisible ( true );
                    component = componentPlugins;
                    while ( component != null ) {
                        if ( component instanceof Frame ) {
                            frame = (Frame) component;
                            JMFrame.autoPosition ( frame, this );
                            break;
                        }
                        component = component.getParent ();
                    }
                }
            }
        }
        else if ( strCmd.equals(MENU_PLAYER_CAPTURE) ) {
            if ( dlgCaptureControls != null ) {
                dlgCaptureControls.setVisible ( true );
                dlgCaptureControls.toFront ();
            }
        }
        else if ( strCmd.equals(MENU_PLAYER_RTPSESSION) ) {
            if ( dlgSessionControl != null ) {
                dlgSessionControl.setVisible ( true );
                dlgSessionControl.toFront ();
            }
        }
        else if ( strCmd.equals(MENU_PLAYER_TRANSMISSION) ) {
            if ( dlgTransmissionStats != null ) {
                dlgTransmissionStats.setVisible ( true );
                dlgTransmissionStats.toFront ();
            }
        }
        else if ( strCmd.equals(MENU_HELP_ABOUT) ) {
            AboutDialog.createDialog ( this );
        }
        else {
            super.actionPerformed ( event );
        }
    }

    public void itemStateChanged ( ItemEvent event ) {
        Object      objSource;

        objSource = event.getSource ();
        if ( objSource == menuAutoPlay ) {
            if ( cfgJMApps != null )
                cfgJMApps.setAutoPlay ( menuAutoPlay.getState() );
        }
        else if ( objSource == menuAutoLoop ) {
            if ( mediaPlayerCurrent != null )
                mediaPlayerCurrent.setPlaybackLoop ( menuAutoLoop.getState() );
            if ( cfgJMApps != null )
                cfgJMApps.setAutoLoop ( menuAutoLoop.getState() );
        }
        else if ( objSource == menuKeepAspect ) {
            if ( mediaPlayerCurrent != null ) {
                mediaPlayerCurrent.setFixedAspectRatio ( menuKeepAspect.getState() );
                if ( panelVideo != null )
                    panelVideo.resizeVisualComponent ();
            }
            if ( cfgJMApps != null )
                cfgJMApps.setKeepAspectRatio ( menuKeepAspect.getState() );
        }
    }

    public void windowClosing ( WindowEvent event ) {
	killCurrentPlayer();
        this.dispose ();
    }

    public void windowClosed ( WindowEvent event ) {
        int     nIndex;
        Point   pointLocation;

        super.windowClosed ( event );

        if ( frameSnap != null )
            frameSnap.dispose ();
        if ( vectorFrames.contains(this) ) {
            pointLocation = this.getLocation ();
            nIndex = vectorFrames.indexOf ( this );
            if ( cfgJMApps != null )
                cfgJMApps.setJMStudioFrameLocation ( pointLocation, nIndex );
            vectorFrames.removeElement ( this );
        }
        if ( vectorFrames.size() < 1 )
            exitApllication ();
    }

    public synchronized void update ( ReceiveStreamEvent event) {
        ReceiveStream   stream;
        RTPSessionMgr   mngrSession;
        DataSource      dataSource;
        PlayerFrame     frame;

        if ( event instanceof NewReceiveStreamEvent ) {
	    recvRTP = true;
            mngrSession = (RTPSessionMgr)event.getSource();
            stream =((NewReceiveStreamEvent)event).getReceiveStream ();
            dataSource = stream.getDataSource ();

	    // this call is not valid for the new RTP interface
            //strOptionalTitle = mngrSession.getSessionAddress().getDataAddress().getHostAddress()
            //                     + ":" + mngrSession.getSessionAddress().getDataPort();

	    strOptionalTitle = hostAddress + ":" + port;
	    
            mngrSessionRtp = null;
            if ( vectorRtpFrames != null  &&  vectorMngrSessions != null
                        &&  vectorMngrSessions.size() > 0
                        &&  vectorMngrSessions.firstElement() == mngrSession ) {
                frame = new PlayerFrame ( this, strOptionalTitle );
                vectorRtpFrames.addElement ( frame );
                frame.open ( dataSource );
                frame.setVisible ( true );
            }
            else {
                open ( dataSource, false );

                vectorMngrSessions = new Vector ();
                vectorMngrSessions.addElement ( mngrSession );
                vectorStreams = new Vector ();
                vectorStreams.addElement ( stream );

                dlgSessionControl = new SessionControlDialog ( this, mngrSession );
                updateMenu ();

                vectorRtpFrames = new Vector ();
            }
        }
    }

    protected void processRealizeComplete ( RealizeCompleteEvent event ) {
        String      strMediaLocation;
        Dimension   dimVideo;
        Dimension   dimControlPanel;

        killCurrentView ();
	
        this.setCursor ( cursorNormal );

        // Get the visual component
        panelVideo = new VideoPanel ( mediaPlayerCurrent );
        panelVideo.setZoom ( dDefaultScale );
        panelVideo.addMenuZoomActionListener ( this );
        // Get the control component
	compControl = mediaPlayerCurrent.getControlPanelComponent ();

        // The bounds of the components will be set by LayoutManager,
        // but the components might start draw themselves in another thread
        // causing the garbage on the screen.
        // So we set here their bounds to avoid it.
        dimVideo = panelVideo.getPreferredSize ();
	//        panelVideo.setBounds ( 0, 0, dimVideo.width, dimVideo.height );
	if ( compControl != null ) {
            dimControlPanel = compControl.getPreferredSize ();
            compControl.setBounds ( 0, dimVideo.height, dimVideo.width, dimControlPanel.height );
        }
	
        panelContent.add ( panelVideo, BorderLayout.CENTER );
	//        panelVideo.repaint ();
	if ( compControl != null) {
            panelContent.add ( compControl, BorderLayout.SOUTH );
            compControl.repaint ();
	}
	
        // Plug-In Viewer - Implementation specific control
        controlPlugins = mediaPlayerCurrent.getControl ( "com.sun.media.JMD" );
        controlGrabber = (FrameGrabbingControl) mediaPlayerCurrent.getControl ( "javax.media.control.FrameGrabbingControl" );


	// Wait for visual to show up
	Component compVis = panelVideo.getVisualComponent();
	if (compVis != null) {
	    while (!compVis.isVisible()) {
		try {
		    Thread.sleep(10);
		} catch (InterruptedException ie) {
		}
	    }
	}

	// Ask the player to prefetch data and prepare to start.
	mediaPlayerCurrent.prefetch ();
	
	strMediaLocation = mediaPlayerCurrent.getMediaLocation ();
	if ( strMediaLocation == null  ||  strMediaLocation.trim().length() < 1 )
            strMediaLocation = strOptionalTitle;
	setTitle ( strMediaLocation + " - " + APPNAME );
	
	updateMenu ();
    }
    
    protected void processPrefetchComplete ( PrefetchCompleteEvent event ) {
        long    lWait;

        if ( menuAutoPlay.getState() ) {
            if ( mediaPlayerCurrent != null  &&  mediaPlayerCurrent.getTargetState() != Controller.Started ) {
                mediaPlayerCurrent.start();
            }
        }
    }

    protected void processFormatChange ( FormatChangeEvent event ) {
        killCurrentView ();

        // Get the visual component
        panelVideo = new VideoPanel ( mediaPlayerCurrent );
        panelVideo.setZoom ( dDefaultScale );
        panelVideo.addMenuZoomActionListener ( this );
        panelContent.add ( panelVideo, BorderLayout.CENTER );

        // Get the control component
        compControl = mediaPlayerCurrent.getControlPanelComponent ();
        if ( compControl != null) {
            panelContent.add ( compControl, BorderLayout.SOUTH );
        }
    }

    private void openFile () {
        String          nameFile = null;
        String          nameUrl;
        MediaPlayer     mediaPlayer;
        boolean         boolResult;

        if ( dlgOpenFile == null )
            dlgOpenFile = new FileDialog ( this, MENU_FILE_OPENFILE, FileDialog.LOAD );

        if ( cfgJMApps != null )
            nameFile = cfgJMApps.getLastOpenFile ();
        if ( nameFile != null )
            dlgOpenFile.setFile ( nameFile );

        dlgOpenFile.show ();
        nameFile = dlgOpenFile.getFile ();
        if ( nameFile == null )
            return;

        nameFile = dlgOpenFile.getDirectory() + nameFile;
        if ( cfgJMApps != null )
            cfgJMApps.setLastOpenFile ( nameFile );

        nameUrl = "file:" + nameFile;
        open ( nameUrl );
    }

    private void openUrl () {
        OpenUrlDialog   dlgOpenUrl;
        String          nameUrl = null;
        MediaPlayer     mediaPlayer;
        boolean         boolResult;
        String          strAction;

        if ( cfgJMApps != null )
            nameUrl = cfgJMApps.getLastOpenUrl ();

        dlgOpenUrl = new OpenUrlDialog ( this, nameUrl );
	    dlgOpenUrl.show ();
        strAction = dlgOpenUrl.getAction ();
        if ( !strAction.equals(JMDialog.ACTION_OPEN) )
            return;
        nameUrl = dlgOpenUrl.getUrl ();
        if ( nameUrl == null )
            return;

        if ( cfgJMApps != null )
            cfgJMApps.setLastOpenUrl ( nameUrl );
        open ( nameUrl );
    }

    private String hostAddress;
    private String port;
    RTPTimer rtptimer = null;

    class RTPTimer extends Thread {
	JMStudio outer = null;
	public RTPTimer(JMStudio outer) {
	    this.outer = outer;
	}
	
	public void run() {
	    MessageDialog dlg = null;
	    String newtime;
	    String answer = "";
	    Image image = null;
	    
	    try {
		Thread.sleep(7000);
	    } catch (InterruptedException ie) {
		return;
	    }
	    
	    //System.out.println("killed = " + killed);
	    //System.out.println("recvRTP = " + recvRTP);
	    
	    if ( !killed && !recvRTP) {
		// popup a info diag
		image = ImageArea.loadImage ( "iconInfo.gif" );
		dlg = new MessageDialog(outer, "Waiting for data", "7 seconds elasped", image, false,false);
		dlg.setLocationCenter ();
		dlg.show ();		
	    }
	    
	    int count = 7;
	    while ( !killed && !recvRTP && !Thread.currentThread().isInterrupted() && count < 60 ) {
		try {
		    Thread.sleep(1000);
		} catch (InterruptedException ie) {
		    if ( dlg != null ) {
			dlg.dispose();
			dlg = null;
		    }
		    return;
		}
		
		count ++;
		newtime = new Integer(count).toString() + " seconds elasped";
		if ( dlg != null )
		    dlg.getTextView().setText(newtime);
	    } // end of while
	    
	    if ( !killed && !recvRTP && !Thread.currentThread().isInterrupted()) {
		
		answer = MessageDialog.createYesNoDialog (outer, "Waing for data", "You have been waiting for 60 secs. Continue to wait?");
		
	    }

	    if ( !killed && !recvRTP && !Thread.currentThread().isInterrupted() && answer.equals("No")) {
		if ( dlg != null ) {
		    dlg.dispose();
		    dlg = null;
		}

		outer.killCurrentPlayer();
		return;
	    }

	    count = 60;
	    while ( !killed && !recvRTP && !Thread.currentThread().isInterrupted()) {
		try {
		    Thread.sleep(1000);
		} catch (InterruptedException ie) {
		    if ( dlg != null ) {
			dlg.dispose();
			dlg = null;
		    }
		    return;
		}
		
		count ++;
		newtime = new Integer(count).toString() + " seconds elasped";
		if ( dlg != null )
		    dlg.getTextView().setText(newtime);
	    } // end of while


	} // end of run()
    }
    
    private void openRtp () {
        OpenRtpDialog   dlgOpenRtp;
        String          strAction;
        String          strAddress;
        String          strPort;
        String          strTtl;


        dlgOpenRtp = new OpenRtpDialog ( this, cfgJMApps );
	    dlgOpenRtp.show ();
        strAction = dlgOpenRtp.getAction ();
        if ( !strAction.equals(JMDialog.ACTION_OPEN) )
            return;

        strAddress = dlgOpenRtp.getAddress ();
	
        strPort = dlgOpenRtp.getPort ();
        strTtl = dlgOpenRtp.getTtl ();

	hostAddress= strAddress;
	port= strPort;
	
	// Get rid of current player
	killCurrentPlayer();

	rtptimer = new RTPTimer(this);
	killed = false;
	recvRTP = false;
	//System.out.println("in openRtp recvRTP = " + recvRTP);
	rtptimer.start();

        mngrSessionRtp = JMFUtils.createSessionManager ( strAddress, strPort, strTtl , this );
	
        if ( mngrSessionRtp == null ) {
            MessageDialog.createErrorDialog ( this, JMFI18N.getResource("jmstudio.error.sessionmngr.create") );
	    killed = true;
	    if ( rtptimer != null && rtptimer.isAlive()) {
		rtptimer.interrupt();
		rtptimer = null;
	    }
            return;
        }

        updateMenu ();
    }

    public void open ( String nameUrl ) {
        MediaPlayer     mediaPlayer;
        boolean         boolResult;

        mediaPlayer = jmapps.util.JMFUtils.createMediaPlayer ( nameUrl, (Frame)this, audioEffect, videoEffect );
	
        dataSourceCurrent = null;
        boolResult = open ( mediaPlayer, true );
        if ( boolResult == true ) {
            addToRecentUrlList ( nameUrl );
        }
    }

    public void open ( DataSource dataSource ) {
	open (  dataSource, true  );
    }

    public void open ( DataSource dataSource, boolean killPrevious ) {
        MediaPlayer     mediaPlayer;
        boolean         boolResult;
	
        mediaPlayer = jmapps.util.JMFUtils.createMediaPlayer ( dataSource, (Frame)this );
        boolResult = open ( mediaPlayer, killPrevious );
        if ( boolResult == true )
            dataSourceCurrent = dataSource;
    }

    public boolean open ( MediaPlayer mediaPlayer ) {
	return open ( mediaPlayer, true );
    }

    public boolean open ( MediaPlayer mediaPlayer, boolean killPrevious ) {

        if ( mediaPlayer == null )
            return ( false );

	if (killPrevious) 
            killCurrentPlayer ();
	    

        this.setCursor ( cursorWait );
        mediaPlayerCurrent = mediaPlayer;
	killed = false;
	
        mediaPlayer.setPlaybackLoop ( menuAutoLoop.getState() );
        mediaPlayer.setFixedAspectRatio ( menuKeepAspect.getState() );
        mediaPlayer.setPopupActive ( false );
        mediaPlayer.setControlPanelVisible ( false );
        mediaPlayer.addControllerListener ( this );
        mediaPlayer.realize();

        updateMenu ();
        return ( true );
    }

    private void exportMediaOld () {
        int             i;
        String          nameUrl;
        Format          format;
        AudioFormat     formatAudioDevice = null;
        VideoFormat     formatVideoDevice = null;
        FormatControl   fcs [];
        DataSource      dataSource;
        SaveAsDialog    dlgSaveAs;

        nameUrl = mediaPlayerCurrent.getMediaLocation ();

        if ( dataSourceCurrent != null ) {
            if (dataSourceCurrent instanceof CaptureDevice) {
                fcs = ((CaptureDevice)dataSourceCurrent).getFormatControls();
                for ( i = 0;  i < fcs.length; i++ ) {
                    format = fcs[i].getFormat();
                    if ( format instanceof AudioFormat )
                        formatAudioDevice = (AudioFormat) format;
                    else if ( format instanceof VideoFormat )
                        formatVideoDevice = (VideoFormat) format;
                }
            }
	    }

	    if ( nameUrl != null  &&  nameUrl.length() > 1  &&  closeCapture() ) {
            if ( nameCaptureDeviceAudio != null  ||  nameCaptureDeviceVideo != null ) {
                dataSource = JMFUtils.createCaptureDataSource ( nameCaptureDeviceAudio,
    								formatAudioDevice, nameCaptureDeviceVideo,
    								formatVideoDevice );
                if ( dataSource == null )
                    System.err.println ( "DataSource is null" );
                dlgSaveAs = new SaveAsDialog ( JMStudio.this, dataSource, cfgJMApps );
            }
            else {
                dlgSaveAs = new SaveAsDialog ( JMStudio.this, nameUrl, null, cfgJMApps);
            }
	    }
    }

    private void exportMedia () {
        int             i;
        int             nCount;
        String          nameUrl;
        Format          format;
        AudioFormat     formatAudioDevice = null;
        VideoFormat     formatVideoDevice = null;
        FormatControl   fcs [];
        DataSource      dataSource;
        SaveAsDialog    dlgSaveAs;
        RTPSessionMgr   mngrSession;
        SessionAddress  addrSession;
        InetAddress     addrInet;
        Control         arrControls [];
        String          strResult;

        nameUrl = mediaPlayerCurrent.getMediaLocation ();

        if ( dataSourceCurrent != null  &&  dataSourceCurrent instanceof CDSWrapper) {
            dataSource = dataSourceCurrent;
            dataSourceCurrent = null;
            killCurrentPlayer();
//            setPlaceholder();
            dlgSaveAs = new SaveAsDialog ( JMStudio.this, dataSource, cfgJMApps );
        }
        else if ( nameUrl != null  &&  nameUrl.trim().length() > 0 ) {
            dlgSaveAs = new SaveAsDialog ( JMStudio.this, nameUrl, null, cfgJMApps);
        }
        else if ( vectorMngrSessions.size() > 0  &&  vectorStreams.size() > 0 ) {
            mngrSession = (RTPSessionMgr) vectorMngrSessions.firstElement();

	    // this code is not valid for the RTP interface:
            // addrSession = mngrSession.getSessionAddress();
            // addrInet = addrSession.getDataAddress();	    
            // nameUrl = "rtp://" + addrInet.getHostAddress() + ":" + addrSession.getDataPort();

	    addrInet= null;
	    
	    try {	       
	        addrInet= InetAddress.getByName( hostAddress);
	    } catch( UnknownHostException e) {
		e.printStackTrace();
	    }
	    
	    nameUrl = "rtp://" + hostAddress + ":" + port;
	    
            arrControls = mediaPlayerCurrent.getControls();
            nCount = arrControls.length;
            for ( i = 0;  i < nCount;  i++ ) {
                if ( arrControls[i] instanceof TrackControl ) {
                    format = ((TrackControl)arrControls[i]).getFormat();
                    if ( format instanceof AudioFormat ) {
                        nameUrl = nameUrl + "/audio";
                        break;
                    }
                    else if ( format instanceof VideoFormat ) {
                        nameUrl = nameUrl + "/video";
                        break;
                    }
                }
            }
	    
            if ( !addrInet.isMulticastAddress() ) {
                strResult = MessageDialog.createOKCancelDialog ( this, JMFI18N.getResource("jmstudio.query.savertp.unicast") );
                if ( !strResult.equals(MessageDialog.ACTION_OK) )
                    return;
                killCurrentPlayer();
            }

            dlgSaveAs = new SaveAsDialog ( JMStudio.this, nameUrl, null, cfgJMApps);
        }
    }

    private void captureMedia () {
        CaptureDialog       dialogCapture;
        DataSource          dataSource;
        CaptureDeviceInfo   cdi;

        nameCaptureDeviceAudio = null;
        nameCaptureDeviceVideo = null;

        dialogCapture = new CaptureDialog ( this, cfgJMApps );
        dialogCapture.show ();
        if (dialogCapture.getAction() == CaptureDialog.ACTION_CANCEL)
            return;

        cdi = dialogCapture.getAudioDevice();
        if ( cdi != null  &&  dialogCapture.isAudioDeviceUsed() )
            nameCaptureDeviceAudio = cdi.getName();
        cdi = dialogCapture.getVideoDevice();
        if ( cdi != null  &&  dialogCapture.isVideoDeviceUsed() )
            nameCaptureDeviceVideo = cdi.getName();
        dataSource = JMFUtils.createCaptureDataSource ( nameCaptureDeviceAudio,
                                                dialogCapture.getAudioFormat(),
                                                nameCaptureDeviceVideo,
                                                dialogCapture.getVideoFormat() );

        if ( dataSource != null ) {

            if (dataSource instanceof CaptureDevice
                            &&  dataSource instanceof PushBufferDataSource) {
                DataSource cdswrapper = new CDSWrapper((PushBufferDataSource)dataSource);
                dataSource = cdswrapper;
                try {
                    cdswrapper.connect();
                }
                catch (IOException ioe) {
                    dataSource = null;
                    nameCaptureDeviceAudio = null;
                    nameCaptureDeviceVideo = null;
                    MessageDialog.createErrorDialog ( this,
                            JMFI18N.getResource("jmstudio.error.captureds") );
                }
    	    }

            open ( dataSource );
            if ( dataSource != null ) {
                dlgCaptureControls = new CaptureControlsDialog (  JMStudio.this, dataSource );
                if ( dlgCaptureControls.isEmpty() ) {
                    dlgCaptureControls = null;
                }
                else {
//                    dlgCaptureControls.setVisible ( true );
                }
            }
        }
        else {
            nameCaptureDeviceAudio = null;
            nameCaptureDeviceVideo = null;
            MessageDialog.createErrorDialog ( this,
                            JMFI18N.getResource("jmstudio.error.captureds") );
        }
    }

    private void transmitMedia () {
        TransmitWizard      dlgTransmit;
	    String              urlString = null;
        String              strAction;
        MediaPlayer         mediaPlayer;
        Processor           processorTransmit;
        boolean             boolResult;
        DataSource          dataSource = null;

        if ( dataSourceCurrent != null  &&  dataSourceCurrent instanceof CDSWrapper) {
            dataSource = dataSourceCurrent;
            dataSourceCurrent = null;
            killCurrentPlayer();
//            setPlaceholder();
            urlString = "Capture";
        }
        else if ( mediaPlayerCurrent != null ) {
    	    urlString = mediaPlayerCurrent.getMediaLocation ();
        }

        dlgTransmit = new TransmitWizard ( this, urlString, dataSource, cfgJMApps );
        dlgTransmit.show ();
        strAction = dlgTransmit.getAction ();
        if ( !strAction.equals(TransmitWizard.ACTION_FINISH) )
            return;

        processorTransmit = dlgTransmit.getProcessor ();
        if ( processorTransmit == null )
            return;

        strOptionalTitle = JMFI18N.getResource ( "jmstudio.playerwindow.transcoding" );
        mediaPlayer = jmapps.util.JMFUtils.createMediaPlayer ( processorTransmit, (Frame)this );
        boolResult = open ( mediaPlayer );
        if ( boolResult == true ) {
            vectorMngrSessions = dlgTransmit.getMngrSessions ();
            vectorStreams = dlgTransmit.getStreams ();
            vectorStreamLabels = dlgTransmit.getStreamLabels ();

            dlgTransmissionStats = new TransmissionStatsDialog ( this, vectorMngrSessions, vectorStreamLabels );
            this.updateMenu ();
        }
    }

    private void setFullScreen ( boolean boolFullScreen ) {
        Dimension   dimScreen;
        Dimension   dimPrefSize;
        Rectangle   rectVideo;


        if ( panelVideo == null )
            return;

        if ( boolFullScreen == true  &&  panelVideo.getParent() != windowFullScreen ) {
            dimFrameSizeBeforeFullScreen = this.getSize ();

            dimScreen = Toolkit.getDefaultToolkit().getScreenSize();
            if ( windowFullScreen == null ) {
                windowFullScreen = new Window ( this );
                windowFullScreen.setLayout( null );
                windowFullScreen.setBackground ( Color.black );
            }
            windowFullScreen.setBounds ( 0, 0, dimScreen.width, dimScreen.height );
            panelContent.remove ( panelVideo );
            dimPrefSize = panelVideo.getPreferredSize ();
            if ( compControl != null) {
                panelContent.remove ( compControl );
            }

            rectVideo = new Rectangle ( 0, 0, dimScreen.width, dimScreen.height );
            if ( (float)dimPrefSize.width/dimPrefSize.height >= (float)dimScreen.width/dimScreen.height ) {
                rectVideo.height = (dimPrefSize.height * dimScreen.width) / dimPrefSize.width;
                rectVideo.y = (dimScreen.height - rectVideo.height) / 2;
            }
            else {
                rectVideo.width = (dimPrefSize.width * dimScreen.height) / dimPrefSize.height;
                rectVideo.x = (dimScreen.width - rectVideo.width) / 2;
            }

            Toolkit.getDefaultToolkit().sync();
            windowFullScreen.add ( panelVideo );
            windowFullScreen.setVisible ( true );
            panelVideo.setBounds ( rectVideo );
            windowFullScreen.validate ();

            listenerMouseFullScreen = new MouseAdapter () {
                public void mouseClicked ( MouseEvent event ) {
                    setFullScreen ( false );
                }
            };
            panelVideo.getVisualComponent().addMouseListener ( listenerMouseFullScreen );
        }
        else if ( boolFullScreen == false  &&  panelVideo.getParent() == windowFullScreen ) {
            //this.setVisible ( false );
            panelVideo.getVisualComponent().removeMouseListener ( listenerMouseFullScreen );
            Toolkit.getDefaultToolkit().sync();
            windowFullScreen.setVisible ( false );
            windowFullScreen.remove ( panelVideo );
            panelContent.add ( panelVideo, BorderLayout.CENTER );
            if ( compControl != null) {
                panelContent.add ( compControl, BorderLayout.SOUTH );
            }
            if ( dimFrameSizeBeforeFullScreen != null ) {
                this.setSize ( dimFrameSizeBeforeFullScreen );
                this.validate ();
            }
            this.setVisible ( true );
        }
    }

    private void doSnapShot () {
        Buffer          bufferFrame;
        BufferToImage   bufferToImage;
        Image           image;

	    bufferFrame = controlGrabber.grabFrame ();
		bufferToImage = new BufferToImage ( (VideoFormat)bufferFrame.getFormat() );
	    image = bufferToImage.createImage ( bufferFrame );
	    if ( image == null )
            return;

		if ( frameSnap == null )
		    frameSnap = new SnapFrame ( image, this );
        else
            frameSnap.setImage ( image );
		frameSnap.setTitle ( this.getTitle() + " - " + JMFI18N.getResource("jmstudio.snapshot") );
    }

    protected void killCurrentView () {

        if ( componentPlugins != null ) {
            componentPlugins.setVisible ( false );
            componentPlugins = null;
        }
        controlGrabber = null;
        super.killCurrentView ();
    }

    protected void killCurrentPlayer () {
        int                     i;
        int                     nCount;
        RTPStream               streamRtp;
        RTPSessionMgr           mngrSession;
        PlayerFrame             frame;

        // Whatever it is, kill it
        nameCaptureDeviceAudio = null;
        nameCaptureDeviceVideo = null;
        if ( dlgCaptureControls != null )
            dlgCaptureControls.dispose ();
        dlgCaptureControls = null;

        if ( dlgTransmissionStats != null )
            dlgTransmissionStats.dispose ();
        dlgTransmissionStats = null;
        if ( dlgSessionControl != null )
            dlgSessionControl.dispose ();
        dlgSessionControl = null;

        if ( vectorRtpFrames != null ) {
            nCount = vectorRtpFrames.size();
            for ( i = 0;  i < nCount;  i++ ) {
                frame = (PlayerFrame) vectorRtpFrames.elementAt ( i );
                frame.dispose();
            }
            vectorRtpFrames.removeAllElements();
            vectorRtpFrames = null;
        }

        if ( vectorStreams != null ) {
            nCount = vectorStreams.size();
            for ( i = 0;  i < nCount;  i++ ) {
                streamRtp = (RTPStream) vectorStreams.elementAt ( i );
                if ( streamRtp instanceof SendStream )
                    ((SendStream)streamRtp).close ();
            }
            vectorStreams.removeAllElements();
            vectorStreams = null;
        }
        if ( vectorMngrSessions != null ) {
            nCount = vectorMngrSessions.size();
            for ( i = 0;  i < nCount;  i++ ) {
                mngrSession = (RTPSessionMgr) vectorMngrSessions.elementAt ( i );
                if ( mngrSessionRtp == mngrSession )
                    mngrSessionRtp = null;

		mngrSession.removeTargets( "Transmission terminated.");
		mngrSession.dispose();
            }
            vectorMngrSessions.removeAllElements();
            vectorMngrSessions = null;
        }
        if ( mngrSessionRtp != null ) {
	    mngrSessionRtp.removeTargets( "Transmission terminated.");
	    mngrSessionRtp.dispose();
            mngrSessionRtp = null;
        }

        super.killCurrentPlayer ();

        if (dataSourceCurrent != null) {
            if (dataSourceCurrent instanceof CDSWrapper) {
                ((CDSWrapper)dataSourceCurrent).close();
            }
        }
        dataSourceCurrent = null;

        setTitle ( APPNAME );

	killed = true;
	if ( rtptimer != null && rtptimer.isAlive()) {
	    rtptimer.interrupt();
	    rtptimer = null;
	}

        updateMenu ();
    }

    private boolean closeCapture () {
        String  strAction;
        String  strMessage;

        if ( mediaPlayerCurrent == null )
            return true;

        // Sun's implementation specific capture devices
        if ( dataSourceCurrent != null) {
            strMessage = JMFI18N.getResource ( "jmstudio.query.erooropencapture.closepreview" );
            strAction = MessageDialog.createOKCancelDialog ( this, strMessage );
            if ( strAction != null  &&  strAction.equals(JMDialog.ACTION_OK) ) {
                killCurrentPlayer ();
//                setPlaceholder ();
                return true;
            }
            else
                return false;
        }
        else // (probably) not a capture device
            return true;
    }

    public void updateMenu () {
        boolean     boolEnable;

        boolEnable = (mediaPlayerCurrent != null);

        menuFileExport.setEnabled ( boolEnable );
        menuFileClose.setEnabled ( boolEnable  ||  mngrSessionRtp != null  || (vectorMngrSessions != null && !vectorMngrSessions.isEmpty()) );
        menuKeepAspect.setEnabled ( boolEnable && panelVideo != null  &&  panelVideo.getVisualComponent() != null );
        menuFullScreen.setEnabled ( boolEnable && panelVideo != null  &&  panelVideo.getVisualComponent() != null );
        menuSnapShot.setEnabled ( boolEnable  &&  (controlGrabber != null) );
        menuPlugins.setEnabled ( boolEnable  &&  (controlPlugins != null) );
        menuCaptureControl.setEnabled ( boolEnable  &&  (dlgCaptureControls != null) );
        menuTransmissionStats.setEnabled ( boolEnable  &&  (dlgTransmissionStats != null) );
        menuRtpSessionControl.setEnabled ( boolEnable  &&  (dlgSessionControl != null) );
    }

    private void addToRecentUrlList ( String strUrl ) {
        int     nPos;
        String  strUrlType;

        if ( cfgJMApps == null )
            return;

        nPos = strUrl.lastIndexOf ( "." );
        if ( strUrl.substring(0,4).equalsIgnoreCase("rtp:") )
            strUrlType = "RTP";
        else if ( nPos < 1  ||  nPos == strUrl.length() - 1 )
            strUrlType = "Other";
        else
            strUrlType = strUrl.substring(nPos+1).toUpperCase();
        cfgJMApps.addRecentUrls ( strUrlType, strUrl );

        updateRecentUrlMenu ();
    }

    private void updateRecentUrlMenu () {
        int             i;
        int             nSize;
        Enumeration     enumUrlTypes;
        Object          objUrlType;
        Menu            menuUrlType;
        MenuItem        menuItem;
        Vector          vectorUrls;
        Object          objUrl;


        if ( cfgJMApps == null )
            return;

        menuRecentUrl.removeAll ();
        enumUrlTypes = cfgJMApps.getRecentUrlTypes ();
        if ( enumUrlTypes == null )
            return;

        while ( enumUrlTypes.hasMoreElements() ) {
            objUrlType = enumUrlTypes.nextElement();
            menuUrlType = new Menu ( objUrlType.toString() );
            menuRecentUrl.add ( menuUrlType );

            vectorUrls = cfgJMApps.getRecentUrls ( objUrlType.toString() );
            if ( vectorUrls == null )
                continue;

            nSize = vectorUrls.size ();
            for ( i = 0;  i < nSize;  i++ ) {
                objUrl = vectorUrls.elementAt ( i );
                menuItem = new MenuItem ( objUrl.toString() );
                menuItem.setActionCommand ( MENU_FILE_RECENTURL );
                menuItem.addActionListener ( this );
                menuUrlType.add ( menuItem );
            }
        }
    }

    // This method loads HTTP Proxy information from the appletviewer settings.
    static void initProps() {
	Properties props = new Properties(System.getProperties());

	props = new Properties(props);

	File theUserPropertiesFile;
	String sep = File.separator;
	theUserPropertiesFile = new File(System.getProperty("user.home") +
					 sep + ".hotjava" +
					 sep + "properties");

	try {
	    FileInputStream in = new FileInputStream(theUserPropertiesFile);
	    props.load(new BufferedInputStream(in));
	    in.close();
	} catch (Exception e) {
	}

	System.setProperties(props);
    }

    public static void main ( String args[] ) {
        int         i;
        String      strMedia;
        JMStudio    jmStudio;

        jmapps.ui.MessageDialog.titleDefault = APPNAME;
        cfgJMApps = new JMAppsCfg ();
	try {
	    initProps();
	} catch (Throwable t) {
	    System.out.println("Unable to read Http Proxy information from the appletviewer settings");
	}
	
        for ( i = 0;  i < args.length;  i++ ) {
            if ( args[i].equals("-x") ) {
                if ( args.length > i + 1 ) {
                    try {
                        dDefaultScale = Double.valueOf(args[i+1]).doubleValue();
                        i++;
                    }
                    catch ( Exception exception ) {
                        dDefaultScale = 1.0;
                    }
                }
                continue;
            }

            strMedia = args[i];
            if ( strMedia.indexOf(":") < 2 ) {
                if ( strMedia.indexOf("/") != 0 )
                    strMedia = "/" + strMedia;
                strMedia = "file:" + strMedia;
            }
            jmStudio = createNewFrame ();
            jmStudio.open ( strMedia );
        }

        if ( vectorFrames.size() < 1 ) {
            jmStudio = createNewFrame ();
        }
    }

    public static JMStudio createNewFrame () {
        int         nIndex;
        Point       point;
        Dimension   dim;
        Dimension   dimScreen;
        JMStudio    jmStudio;

        jmStudio = new JMStudio ();
        if ( cfgJMApps != null ) {
            nIndex = vectorFrames.size ();
            point = cfgJMApps.getJMStudioFrameLocation ( nIndex );
            dim = jmStudio.getSize ();
            dimScreen = Toolkit.getDefaultToolkit().getScreenSize();
            if ( point.x + dim.width > dimScreen.width )
                point.x = dimScreen.width - dim.width;
            if ( point.y + dim.height > dimScreen.height )
                point.y = dimScreen.height - dim.height;
            jmStudio.setLocation ( point );
            jmStudio.setVisible ( true );
            jmStudio.invalidate ();
            jmStudio.pack ();
        }

        vectorFrames.addElement ( jmStudio );
        return ( jmStudio );
    }

    public static void closeAll () {
        int         i;
        JMStudio    jmStudio;

        i = vectorFrames.size();
        while ( i > 0 ) {
            i--;
            jmStudio = (JMStudio) vectorFrames.elementAt ( i );
	    jmStudio.killCurrentPlayer();
            jmStudio.dispose();
        }
    }

    public static void exitApllication () {
        cleanUp ();
        System.exit ( 0 );
    }

    private static void cleanUp () {
        if ( cfgJMApps != null )
            cfgJMApps.save();
    }

    private Vector getEffectList(Format format) {
	Vector v = PlugInManager.getPlugInList(format,
					       null,
					       PlugInManager.EFFECT);
	return v;
    }

    private void fillEffectList(Menu menu, Vector list) {
	boolean first = true;
	for (int i = 0; i < list.size(); i++) {
	    String className = (String) list.elementAt(i);
	    try {
		Class cClass = Class.forName(className);
		Codec codec = (Codec) cClass.newInstance();
		String name = codec.getName();
		if (first) {
		    first = false;
		    CheckboxMenuItem mi = new CheckboxMenuItem("None");
		    mi.setName("");
		    menu.add(mi);
		    mi.setState(true);
		    addEffectListener(mi);
		}
		CheckboxMenuItem mi = new CheckboxMenuItem(name);
		mi.setName(className);
		menu.add(mi);
		mi.setState(false);
		addEffectListener(mi);
	    } catch (Throwable t) {
	    }
	}
    }

    private void addEffectListener(CheckboxMenuItem mi) {
	mi.addItemListener( new EffectListener(mi) );
    }

    class WaitOnVis extends Thread {

	VideoPanel vp;
	MediaPlayer mp;
	
	public WaitOnVis(VideoPanel vp, MediaPlayer mp) {
	    this.vp = vp;
	    this.mp = mp;
	}

	public void run() {
	    
	    Component compVis = vp.getVisualComponent();
	    if (compVis != null) {
		while (!compVis.isVisible()) {
		    try {
			Thread.sleep(10);
		    } catch (InterruptedException ie) {
		    }
		    System.err.println("sleeping");
		}
	    }
	    mp.prefetch();
	}
    }

    class EffectListener implements ItemListener {
	CheckboxMenuItem mi;
	
	public EffectListener(CheckboxMenuItem mi) {
	    this.mi = mi;
	}

	public void itemStateChanged(ItemEvent ie) {
	    boolean state = mi.getState();
	    Menu menu = (Menu) mi.getParent();
	    for (int i = 0; i < menu.getItemCount(); i++) {
		if (menu.getItem(i) != mi) {
		    ((CheckboxMenuItem)menu.getItem(i)).setState(false);
		}
	    }
	    if (state == false)
		((CheckboxMenuItem)menu.getItem(0)).setState(true);
	    String name = mi.getName();
	    if (name == null || name.length() < 1)
		name = null;
	    if (menu == menuAE)
		audioEffect = name;
	    else
		videoEffect = name;
	}
    }
}
