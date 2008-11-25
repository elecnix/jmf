/*
 * @(#)PlayerFrame.java	1.10 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import java.net.*;
import java.lang.reflect.Method;
import java.awt.*;
import java.awt.event.*;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.format.*;
import javax.media.bean.playerbean.*;

import com.sun.media.util.JMFI18N;


public class PlayerFrame extends JMFrame implements ActionListener,
                                        ContainerListener, ControllerListener {

    protected MediaPlayer       mediaPlayerCurrent = null;
    protected JMPanel           panelContent;
    protected Component         compControl = null;
    protected VideoPanel        panelVideo = null;
    protected ImageArea         areaImagePlaceholder = null;
    protected Component         progressBarCache = null;

    protected Cursor            cursorNormal = new Cursor ( Cursor.DEFAULT_CURSOR );
    protected Cursor            cursorWait = new Cursor ( Cursor.WAIT_CURSOR );

    protected boolean           boolErrorClose = false;
    protected boolean           boolMediaClosed = true;


    public PlayerFrame ( Frame frameOwner, String strTitle ) {
        super ( frameOwner, strTitle );
    }

    protected void initFrame () {
        super.initFrame ();
        this.setLayout ( new BorderLayout() );
        panelContent = new JMPanel ( new BorderLayout() );
        this.add ( panelContent, BorderLayout.CENTER );
        panelContent.addContainerListener ( this );
        setPlaceholder ();
    }

    public void open ( String nameUrl ) {
        MediaPlayer     mediaPlayer;
        boolean         boolResult;

        mediaPlayer = jmapps.util.JMFUtils.createMediaPlayer ( nameUrl, (Frame)this, null, null );
        boolResult = open ( mediaPlayer );
    }

    public void open ( DataSource dataSource ) {
        MediaPlayer     mediaPlayer;
        boolean         boolResult;

        mediaPlayer = jmapps.util.JMFUtils.createMediaPlayer ( dataSource, (Frame)this );
        boolResult = open ( mediaPlayer );
    }

    public boolean open ( MediaPlayer mediaPlayer ) {

        if ( mediaPlayer == null )
            return ( false );

        killCurrentPlayer ();
        this.setCursor ( cursorWait );

        mediaPlayerCurrent = mediaPlayer;
        mediaPlayer.setPopupActive ( false );
        mediaPlayer.setControlPanelVisible ( false );
        mediaPlayer.addControllerListener ( this );
        mediaPlayer.realize();

        return ( true );
    }

    public void setPlaceholder () {
        Image   image;

        if ( areaImagePlaceholder == null ) {
            image = ImageArea.loadImage ( "playerPlaceholder.gif", (Component)this, true );
            if ( image != null ) {
                areaImagePlaceholder = new ImageArea ( image );
                areaImagePlaceholder.setInsets ( 0, 0, 0, 0 );
            }
        }

        if ( areaImagePlaceholder != null ) {
            panelContent.removeAll ();
            panelContent.add ( areaImagePlaceholder, BorderLayout.CENTER );
        }
    }

    protected void killCurrentView () {
        int         i;
        Component   component;

        panelVideo = null;
        compControl = null;

        i = panelContent.getComponentCount();
        while ( i > 0 ) {
            i--;
            component = panelContent.getComponent ( i );
            if ( component == areaImagePlaceholder )
                continue;
            panelContent.remove ( component );
        }

    }

    protected void killCurrentPlayer () {
        int                     i;
        int                     nCount;

        killCurrentView ();
//        progressBarCache = null;

        if ( mediaPlayerCurrent != null ) {
            boolMediaClosed = false;
            mediaPlayerCurrent.close ();
            if ( boolErrorClose == false ) {
                while ( boolMediaClosed == false ) {
                    try {
                        Thread.sleep ( 50 );
                    }
                    catch ( Exception exception ) {
                    }
                }
            }
            mediaPlayerCurrent.removeControllerListener ( this );
            mediaPlayerCurrent = null;
        }
    }

    public void actionPerformed ( ActionEvent event ) {
        String      strCmd;
        Object      objSource;

        strCmd = event.getActionCommand ();
        objSource = event.getSource ();
        if ( strCmd == null  &&  objSource instanceof MenuItem )
            strCmd = ((MenuItem)objSource).getActionCommand ();

        if ( strCmd == null )
            return;

        if ( strCmd.equals(VideoPanel.MENU_ZOOM_1_2) ) {
            pack ();
        }
        else if ( strCmd.equals(VideoPanel.MENU_ZOOM_1_1) ) {
            pack ();
        }
        else if ( strCmd.equals(VideoPanel.MENU_ZOOM_2_1) ) {
            pack ();
        }
        else if ( strCmd.equals(VideoPanel.MENU_ZOOM_4_1) ) {
            pack ();
        }
    }

    public synchronized void controllerUpdate ( ControllerEvent event ) {
        if ( event instanceof RealizeCompleteEvent ) {
            processRealizeComplete ( (RealizeCompleteEvent) event );
        }
        else if ( event instanceof PrefetchCompleteEvent ) {
            processPrefetchComplete ( (PrefetchCompleteEvent) event );
        }
        else if ( event instanceof ControllerErrorEvent ) {
            processControllerError ( (ControllerErrorEvent) event );
        }
        else if ( event instanceof ControllerClosedEvent ) {
            processControllerClosed ( (ControllerClosedEvent) event );
        }
        else if ( event instanceof DurationUpdateEvent ) {
            Time t = ((DurationUpdateEvent)event).getDuration();
        }
        else if ( event instanceof CachingControlEvent ) {
            processCachingControl ( (CachingControlEvent) event );
        }
        else if ( event instanceof StartEvent ) {
        }
        else if ( event instanceof MediaTimeSetEvent ) {
        }
        else if ( event instanceof TransitionEvent ) {
        }
        else if ( event instanceof RateChangeEvent ) {
        }
        else if ( event instanceof StopTimeChangeEvent ) {
        }
        else if ( event instanceof FormatChangeEvent ) {
            processFormatChange ( (FormatChangeEvent) event );
        }
        else if ( event instanceof SizeChangeEvent ) {
        }
        else if ( event.getClass().getName().endsWith("ReplaceURLEvent") ) {
            processReplaceURL ( event );
        }

    }

    protected void processRealizeComplete ( RealizeCompleteEvent event ) {
        killCurrentView ();

        this.setCursor ( cursorNormal );

        panelVideo = new VideoPanel ( mediaPlayerCurrent );
        panelVideo.setZoom ( 1.0 );
        panelVideo.addMenuZoomActionListener ( this );
        panelContent.add ( panelVideo, BorderLayout.CENTER );

        compControl = mediaPlayerCurrent.getControlPanelComponent ();
        if ( compControl != null) {
            panelContent.add ( compControl, BorderLayout.SOUTH );
        }

        mediaPlayerCurrent.prefetch ();
	}

    protected void processPrefetchComplete ( PrefetchCompleteEvent event ) {
        mediaPlayerCurrent.start();
    }

    protected void processControllerError ( ControllerErrorEvent event ) {
        String      strMessage;

        this.setCursor ( cursorNormal );
        strMessage = JMFI18N.getResource ( "jmstudio.error.controller" )
                    + "\n" + event.getMessage();
        MessageDialog.createErrorDialogModeless ( this, strMessage );
        if ( boolMediaClosed == true ) {
            boolErrorClose = true;
            killCurrentPlayer ();
            boolErrorClose = false;
        }
        boolMediaClosed = true;
    }

    protected void processControllerClosed ( ControllerClosedEvent event ) {
        boolMediaClosed = true;
        this.setCursor ( cursorNormal );
    }

    protected void processCachingControl ( CachingControlEvent event ) {
        CachingControl      controlCaching;
        Component           progressBarCacheNew = null;

        controlCaching = event.getCachingControl ();
        if ( controlCaching == null )
            return;

        if ( progressBarCacheNew == null )
            progressBarCacheNew = controlCaching.getControlComponent();
        if ( progressBarCacheNew == null )
            progressBarCacheNew = controlCaching.getProgressBarComponent();
        if ( progressBarCacheNew == null )
            return;
        if ( progressBarCacheNew == progressBarCache )
            return;

        if ( mediaPlayerCurrent != null  &&  mediaPlayerCurrent.getState() >= Player.Realized )
            return;

        killCurrentView ();
        progressBarCache = progressBarCacheNew;
        panelContent.add ( progressBarCache, BorderLayout.CENTER );
        this.setCursor ( cursorNormal );
    }

    protected void processFormatChange ( FormatChangeEvent event ) {
        killCurrentView ();

        // Get the visual component
        panelVideo = new VideoPanel ( mediaPlayerCurrent );
        panelVideo.setZoom ( 1.0 );
        panelVideo.addMenuZoomActionListener ( this );
        panelContent.add ( panelVideo, BorderLayout.CENTER );

        // Get the control component
        compControl = mediaPlayerCurrent.getControlPanelComponent ();
        if ( compControl != null) {
            panelContent.add ( compControl, BorderLayout.SOUTH );
        }
    }

    protected void processReplaceURL ( ControllerEvent event ) {
        Class   classReplaceURLEvent;
        Class   classEvent;
        Method  methodGetUrl;
        URL     url;

        killCurrentPlayer ();

        try {
            classReplaceURLEvent = Class.forName ( "com.ibm.media.ReplaceURLEvent" );
            classEvent = event.getClass ();
            classReplaceURLEvent.isAssignableFrom ( classEvent );
            methodGetUrl = classEvent.getMethod ( "getURL", null );
            url = (URL) methodGetUrl.invoke ( event, null );
            open ( url.toString() );
        }
        catch (Exception e) {
        }
        catch (Error e) {
        }
    }

    public void componentAdded ( ContainerEvent event ) {
        if ( event.getChild() != areaImagePlaceholder
                        &&  areaImagePlaceholder != null
                        &&  areaImagePlaceholder.getParent() == panelContent )
            panelContent.remove ( areaImagePlaceholder );
        this.pack();
    }

    public void componentRemoved ( ContainerEvent event ) {
        if ( panelContent.getComponentCount() < 1
                        &&  event.getChild() != areaImagePlaceholder )
            setPlaceholder ();
        else
            this.pack ();
    }


    public void windowClosed ( WindowEvent event ) {
        killCurrentPlayer ();
    }

    public void windowClosing ( WindowEvent event ) {
        this.setVisible ( false );
    }

}


