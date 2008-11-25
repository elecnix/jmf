/*
 * @(#)VideoPanel.java	1.11 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import java.awt.*;
import java.awt.event.*;
import javax.media.*;
import javax.media.bean.playerbean.*;
import javax.media.control.*;

import com.sun.media.util.JMFI18N;


public class VideoPanel extends JMPanel implements MouseListener, ActionListener, ComponentListener {

    private MediaPlayer     mediaPlayer = null;
    private Component       compVisual = null;
    private double          dScale = 1.0;

    private PopupMenu       menuZoom = null;
    private MenuItem        menuItemZoom_1_2 = null;
    private MenuItem        menuItemZoom_1_1 = null;
    private MenuItem        menuItemZoom_2_1 = null;
    private MenuItem        menuItemZoom_4_1 = null;

    public static final String MENU_ZOOM     = JMFI18N.getResource ( "mediaplayer.menu.zoom" );
    public static final String MENU_ZOOM_1_2 = JMFI18N.getResource ( "mediaplayer.zoom.1:2" );
    public static final String MENU_ZOOM_1_1 = JMFI18N.getResource ( "mediaplayer.zoom.1:1" );
    public static final String MENU_ZOOM_2_1 = JMFI18N.getResource ( "mediaplayer.zoom.2:1" );
    public static final String MENU_ZOOM_4_1 = JMFI18N.getResource ( "mediaplayer.zoom.4:1" );


    public VideoPanel ( MediaPlayer mediaPlayer ) {
        super ( null );

        this.setBackground ( Color.black );
        this.addComponentListener ( this );
        createPopupMenuZoom ();
        setMediaPlayer ( mediaPlayer );
    }

    public void setMediaPlayer ( MediaPlayer mediaPlayer ) {
        int             i;
        MonitorControl  mc;
        Control         controls [];
        Panel           mainPanel;
        Panel           currentPanel;
        Panel           newPanel;
        Component       compControl;

        this.removeAll ();
        if ( compVisual != null ) // previous one
            compVisual.removeMouseListener ( this );

        this.mediaPlayer = mediaPlayer;
        compVisual = mediaPlayer.getVisualComponent ();
        if ( compVisual == null ) {
            mc = (MonitorControl) mediaPlayer.getControl ( "javax.media.control.MonitorControl" );
            if ( mc != null ) {
                controls = mediaPlayer.getControls ();
                mainPanel = new Panel ( new BorderLayout() );
                currentPanel = mainPanel;
                for ( i = 0;  i < controls.length;  i++ ) {
                    if ( !(controls[i] instanceof MonitorControl) )
                        continue;
                    mc = (MonitorControl) controls[i];
                    mc.setEnabled ( true );
                    compControl = mc.getControlComponent();
                    if ( compControl == null )
                        continue;
                    currentPanel.add ( compControl, BorderLayout.CENTER );
                    newPanel = new Panel ( new BorderLayout() );
                    currentPanel.add ( newPanel, BorderLayout.SOUTH );
                    currentPanel = newPanel;
                }
                compVisual = mainPanel;
                this.add ( mainPanel );
            }
        }
        else {
            this.add ( mediaPlayer );
        }

        if ( compVisual == null )
            return;

        resizeVisualComponent ();
        addPopupMenuZoom ();
    }

    public Component getMediaPlayer () {
        return ( mediaPlayer );
    }

    public Component getVisualComponent () {
        return ( mediaPlayer.getVisualComponent() /*compVisual*/ );
    }

    public void setZoom ( double dScale ) {
        this.dScale = dScale;
    }

    public double getZoom () {
        return ( dScale );
    }

    public Dimension getPreferredSize () {
        Dimension   dim;

        if ( compVisual == null  ||  mediaPlayer == null ) {
            dim = new Dimension ( 320, 0 );
        }
        else {
//            dim = mediaPlayer.getPreferredSize ();
            dim = compVisual.getPreferredSize ();
            dim.width = (int)(dim.width * dScale);
            dim.height = (int)(dim.height * dScale);
        }
        return ( dim );
    }

    public void addMenuZoomActionListener ( ActionListener listener ) {
        if ( menuItemZoom_1_2 != null )
            menuItemZoom_1_2.addActionListener ( listener );
        if ( menuItemZoom_1_1 != null )
            menuItemZoom_1_1.addActionListener ( listener );
        if ( menuItemZoom_2_1 != null )
            menuItemZoom_2_1.addActionListener ( listener );
        if ( menuItemZoom_4_1 != null )
            menuItemZoom_4_1.addActionListener ( listener );
    }

    public void removeMenuZoomActionListener(ActionListener listener) {
        if ( menuItemZoom_1_2 != null )
            menuItemZoom_1_2.removeActionListener ( listener );
        if ( menuItemZoom_1_1 != null )
            menuItemZoom_1_1.removeActionListener ( listener );
        if ( menuItemZoom_2_1 != null )
            menuItemZoom_2_1.removeActionListener ( listener );
        if ( menuItemZoom_4_1 != null )
            menuItemZoom_4_1.removeActionListener ( listener );
    }

    private void createPopupMenuZoom () {
        if ( menuZoom != null )
            return;

        menuZoom = new PopupMenu ( MENU_ZOOM );

        menuItemZoom_1_2 = new MenuItem ( MENU_ZOOM_1_2 );
        menuZoom.add ( menuItemZoom_1_2 );
        menuItemZoom_1_2.addActionListener ( this );

        menuItemZoom_1_1 = new MenuItem ( MENU_ZOOM_1_1 );
        menuZoom.add ( menuItemZoom_1_1 );
        menuItemZoom_1_1.addActionListener ( this );

        menuItemZoom_2_1 = new MenuItem ( MENU_ZOOM_2_1 );
        menuZoom.add ( menuItemZoom_2_1 );
        menuItemZoom_2_1.addActionListener ( this );

        menuItemZoom_4_1 = new MenuItem ( MENU_ZOOM_4_1 );
        menuZoom.add ( menuItemZoom_4_1 );
        menuItemZoom_4_1.addActionListener ( this );
    }

    private void addPopupMenuZoom () {
        if ( compVisual == null )
            return;
        createPopupMenuZoom ();
	compVisual.add ( menuZoom );
    }

    public void addNotify() {
	super.addNotify();
	if (compVisual != null)
	    compVisual.addMouseListener(this);
    }

    public void removeNotify() {
	super.removeNotify();
	if (compVisual != null)
	    compVisual.removeMouseListener(this);
    }

    public void resizeVisualComponent () {
        Dimension   dimThis;

        dimThis = this.getSize ();
        if ( mediaPlayer != null  &&  mediaPlayer.getParent() == this )
            mediaPlayer.setBounds ( 0, 0, dimThis.width, dimThis.height );
        else if ( compVisual != null  &&  compVisual.getParent() == this )
            compVisual.setBounds ( 0, 0, dimThis.width, dimThis.height );
        this.validate ();
    }

     public void actionPerformed ( ActionEvent event ) {
        String      strCmd;

        strCmd = event.getActionCommand ();
        if ( strCmd.equals(MENU_ZOOM_1_2) ) {
            dScale = 0.5;
        }
        else if ( strCmd.equals(MENU_ZOOM_1_1) ) {
            dScale = 1.0;
        }
        else if ( strCmd.equals(MENU_ZOOM_2_1) ) {
            dScale = 2.0;
        }
        else if ( strCmd.equals(MENU_ZOOM_4_1) ) {
            dScale = 4.0;
        }
//        mediaPlayer.setZoomTo ( "" + dScale + "f" );
        this.invalidate ();
    }

    public void mousePressed ( MouseEvent event ) {
        if ( event.isPopupTrigger() ) {
            menuZoom.show ( compVisual, event.getX(), event.getY() );
	}
    }

    public void mouseReleased ( MouseEvent event ) {
        if ( event.isPopupTrigger() ) {
            menuZoom.show ( compVisual, event.getX(), event.getY() );
	}
    }

    public void mouseClicked ( MouseEvent event ) {
        if ( event.isPopupTrigger() )
            menuZoom.show ( compVisual, event.getX(), event.getY() );
    }

    public void mouseEntered ( MouseEvent event ) {
    }

    public void mouseExited ( MouseEvent event ) {
    }

    public void componentResized ( ComponentEvent event ) {
        resizeVisualComponent ();
    }

    public void componentMoved ( ComponentEvent event ) {
    }

    public void componentShown ( ComponentEvent event ) {
    }

    public void componentHidden ( ComponentEvent event ) {
    }

}


