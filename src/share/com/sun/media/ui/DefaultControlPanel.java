/*
 * @(#)DefaultControlPanel.java	1.55 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.*;
import com.sun.media.controls.*;
import com.sun.media.util.*;
import javax.media.format.*;
import javax.media.format.*;
import javax.media.format.*;
import javax.media.control.*;


public class DefaultControlPanel extends BufferedPanelLight
    implements ActionListener, ItemListener, ControllerListener, GainChangeListener, ComponentListener {

    /*************************************************************************
     * Variables
     *************************************************************************/

    static final Color     colorBackground = new Color(192, 192, 192);

    private static final String    MENU_PROPERTIES = JMFI18N.getResource("mediaplayer.properties");
    private static final String    MENU_RATE_1_4 = JMFI18N.getResource("mediaplayer.rate.1:4");
    private static final String    MENU_RATE_1_2 = JMFI18N.getResource("mediaplayer.rate.1:2");
    private static final String    MENU_RATE_1_1 = JMFI18N.getResource("mediaplayer.rate.1:1");
    private static final String    MENU_RATE_2_1 = JMFI18N.getResource("mediaplayer.rate.2:1");
    private static final String    MENU_RATE_4_1 = JMFI18N.getResource("mediaplayer.rate.4:1");
    private static final String    MENU_RATE_8_1 = JMFI18N.getResource("mediaplayer.rate.8:1");
    private static final String    MENU_MEDIA = JMFI18N.getResource("mediaplayer.menu.media");
    private static final String    MENU_AUDIO = JMFI18N.getResource("mediaplayer.menu.audio");
    private static final String    MENU_VIDEO = JMFI18N.getResource("mediaplayer.menu.video");


    Player                 player;
    Frame                  parentFrame = null;

    Container              container = null;
    TransparentPanel       panelLeft;
    TransparentPanel       panelRight;
    TransparentPanel       panelProgress;
    boolean                boolAdded = false;

    ButtonComp   buttonPlay = null;
    ButtonComp             buttonStepBack = null;
    ButtonComp             buttonStepFwd = null;
    AudioButton            buttonAudio = null;
    ButtonComp             buttonMedia = null;
    ProgressSlider         progressSlider = null;
    CheckboxMenuItem    menuItemCheck = null;
    PopupMenu              menuPopup = null;

    WindowListener wl = null;
    private boolean firstTime = true;
    private boolean started = false;
    private Integer localLock = new Integer(0);
    
    GainControlComponent audioControls = null;
    PropertySheet 	 propsSheet = null;
      //    PopupMenu 		 rateMenu = null;

    // Controls
    javax.media.control.FramePositioningControl    controlFrame = null;
    ProgressControl            progressControl = null;
    GainControl                gainControl = null;
    SliderRegionControl        regionControl = null;
    String                     urlName = null;
      
    long                       lFrameStep = 0;

    private CheckboxMenuItem   menuRate_1_4 = null;
    private CheckboxMenuItem   menuRate_1_2 = null;
    private CheckboxMenuItem   menuRate_1_1 = null;
    private CheckboxMenuItem   menuRate_2_1 = null;
    private CheckboxMenuItem   menuRate_4_1 = null;
    private CheckboxMenuItem   menuRate_8_1 = null;
      
    private Vector             vectorTracksAudio = new Vector ();
    private Vector             vectorTracksVideo = new Vector ();
      
    private int pausecnt = -1;
    private boolean resetMediaTimeinPause = false;

    /*************************************************************************
     * Methods
     *************************************************************************/

    public DefaultControlPanel(Player player) {
        this.player = player;
        try {
            init ();
        }
        catch ( Exception exception ) {
            exception.printStackTrace ();
        }
    }

    public void addNotify () {
        boolean                        boolLightweight = true;
        Container                      containerParent;
        java.awt.peer.ComponentPeer    compPeer;


        if ( boolAdded == false ) {

            containerParent = this.getParent ();
            while ( containerParent != null  &&  boolLightweight == true ) {
                compPeer = containerParent.getPeer ();
                containerParent = containerParent.getParent ();
                if ( containerParent == null )
                    break;
                if ( compPeer != null  &&  !(compPeer instanceof java.awt.peer.LightweightPeer) )
                    boolLightweight = false;
            }

            if ( container != null ) {
                container.remove ( panelLeft );
                container.remove ( panelRight );
                container.remove ( panelProgress );
                if ( container != this )
                    this.remove ( container );
            }

            if ( boolLightweight == true ) {
                container = this;
            }
            else {
                container = new BufferedPanel ( new BorderLayout() );
                container.setBackground(colorBackground);
                ((BufferedPanel)container).setBackgroundTile ( BasicComp.fetchImage("texture3.gif") );
                this.add ( container, BorderLayout.CENTER );
            }
            container.add ( panelLeft, BorderLayout.WEST );
            container.add ( panelRight, BorderLayout.EAST );
            container.add ( panelProgress, BorderLayout.CENTER );

            boolAdded = true;
        }

        setVisible ( true );
        super.addNotify ();
        this.validate ();
    }

    public void removeNotify () {
//        cleanUp();
        super.removeNotify ();
        if ( boolAdded == true ) {
//            container.remove ( panelLeft );
//            container.remove ( panelRight );
//            container.remove ( panelProgress );
//            if ( container != this )
//                this.remove ( container );
            boolAdded = false;
        }
    }

    // added by hsy for FlashDefaultControlPanel 
    protected void removePlayButton() {
        panelLeft.remove(buttonPlay);
    }

    private void init () throws Exception {
        Time                duration;
        Dimension           dim;
        int                 i;
        int                 nCount;
        Control             arrControls [];
        TrackControl        trackControl;
        Format              format;
        VideoFormat         formatVideo;
        float               frameRate;
        MenuItem            menuItem;
        boolean             boolEnable;


        getPlayerControls();
        if (gainControl != null)
            gainControl.addGainChangeListener ( this );

        setBackground(colorBackground);
        setLayout ( new BorderLayout() );
        this.addComponentListener ( this );

        container = this;
        panelLeft = new TransparentPanel ( new GridLayout(1,0) );
        container.add ( panelLeft, BorderLayout.WEST );
        panelRight = new TransparentPanel ( new GridLayout(1,0) );
        container.add ( panelRight, BorderLayout.EAST );
        panelProgress = new TransparentPanel ( new BorderLayout() );
        container.add ( panelProgress, BorderLayout.CENTER );

        buttonPlay = new ButtonComp ( "Play",
                                      "play.gif", "play-active.gif", "play-pressed.gif", "play-disabled.gif",
                                      "pause.gif", "pause-active.gif", "pause-pressed.gif", "pause-disabled.gif" );
        buttonPlay.setActionListener ( this );
        panelLeft.add ( buttonPlay );

//        if ( player instanceof com.sun.media.BasicPlayer
//                &&  ((com.sun.media.BasicPlayer)player).isFramePositionable() ) {
        if ( controlFrame != null ) {
            buttonStepBack = new ButtonComp ( "StepBack",
                                          "step-back.gif", "step-back-active.gif", "step-back-pressed.gif", "step-back-disabled.gif",
                                          "step-back.gif", "step-back-active.gif", "step-back-pressed.gif", "step-back-disabled.gif" );
            buttonStepBack.setActionListener ( this );
            buttonStepBack.setContMousePress ( true );
            panelLeft.add ( buttonStepBack );

            buttonStepFwd = new ButtonComp ( "StepForward",
                                          "step-fwd.gif", "step-fwd-active.gif", "step-fwd-pressed.gif", "step-fwd-disabled.gif",
                                          "step-fwd.gif", "step-fwd-active.gif", "step-fwd-pressed.gif", "step-fwd-disabled.gif" );
            buttonStepFwd.setActionListener ( this );
            buttonStepFwd.setContMousePress ( true );
            panelLeft.add ( buttonStepFwd );
        }

        if (gainControl != null) {
            buttonAudio = new AudioButton ( gainControl );
            buttonAudio.setActionListener ( this );
            panelRight.add ( buttonAudio );
        }
	
        buttonMedia = new ButtonComp ( "Media",
                                      "media.gif", "media-active.gif", "media-pressed.gif", "media-disabled.gif",
                                      "media.gif", "media-active.gif", "media-pressed.gif", "media-disabled.gif" );
        buttonMedia.setActionListener ( this );
        panelRight.add ( buttonMedia );

        progressSlider = new ProgressSlider( "mediatime", this, player );
        progressSlider.setActionListener ( this );
        panelProgress.add ( progressSlider, BorderLayout.CENTER );
        duration = player.getDuration ();
        if (duration == Duration.DURATION_UNBOUNDED  ||  duration == Duration.DURATION_UNKNOWN )
            progressSlider.setEnabled(false);

        updateButtonState ();

	// Add GUI controls for audio
/*
	if (gainControl != null) {
	    audioControls = new GainControlComponent(gainControl);
	    add(audioControls);
	    minimumWidth += audioControls.getPreferredSize().width;
	}
*/

        validate();
        dim = this.getPreferredSize ();
        this.setSize ( dim );
        setVisible ( true );
        setBackgroundTile ( BasicComp.fetchImage("texture3.gif") );
        player.addControllerListener ( this );

        arrControls = player.getControls ();
        nCount = arrControls.length;
        for ( i = 0;  i < nCount;  i++ ) {
            if ( !(arrControls[i] instanceof TrackControl) )
                continue;
            trackControl = (TrackControl) arrControls[i];
            format = trackControl.getFormat ();

            if ( format instanceof AudioFormat ) {
                vectorTracksAudio.addElement ( trackControl );
            }
            else if ( format instanceof VideoFormat ) {
                vectorTracksVideo.addElement ( trackControl );
                formatVideo = (VideoFormat) format;
                frameRate = formatVideo.getFrameRate ();
                lFrameStep = (long)(Time.ONE_SECOND / frameRate);
            }
        }

        menuPopup = new PopupMenu ( MENU_MEDIA );
        buttonMedia.setPopupMenu ( menuPopup );

        nCount = vectorTracksAudio.size ();
	// The audio icon for the first enabled track is
	// enabled. For all the remaining tracks, the
	// audio icon is set to muted state.
	boolean aTrackAudioIconEnabled = false;
        if ( nCount > 1 ) {
            for ( i = 0;  i < nCount;  i++ ) {
                trackControl = (TrackControl)vectorTracksAudio.elementAt ( i );
		boolEnable = false;
		if (!aTrackAudioIconEnabled && trackControl.isEnabled()) {
		    aTrackAudioIconEnabled = true;
		    boolEnable = true;
		}
                menuItemCheck = new CheckboxMenuItem ( MENU_AUDIO + " " + i, boolEnable );
                muteAudioTrack ( trackControl, !boolEnable );
                menuItemCheck.addItemListener ( this );
                menuPopup.add ( menuItemCheck );
            }
            menuPopup.addSeparator ();
        }
/*
        nCount = vectorTracksVideo.size ();
        if ( nCount > 1 ) {
            for ( i = 0;  i < nCount;  i++ ) {
                menuItemCheck = new CheckboxMenuItem ( "Video " + i, true );
                menuItemCheck.addItemListener ( this );
                menuPopup.add ( menuItemCheck );
            }
            menuPopup.addSeparator ();
        }
*/
        menuRate_1_4 = new CheckboxMenuItem ( MENU_RATE_1_4, false );
        menuRate_1_4.addItemListener ( this );
        menuPopup.add ( menuRate_1_4 );
        menuRate_1_2 = new CheckboxMenuItem ( MENU_RATE_1_2, false );
        menuRate_1_2.addItemListener ( this );
        menuPopup.add ( menuRate_1_2 );
        menuRate_1_1 = new CheckboxMenuItem ( MENU_RATE_1_1, true );
        menuRate_1_1.addItemListener ( this );
        menuPopup.add ( menuRate_1_1 );
        menuRate_2_1 = new CheckboxMenuItem ( MENU_RATE_2_1, false );
        menuRate_2_1.addItemListener ( this );
        menuPopup.add ( menuRate_2_1 );
        menuRate_4_1 = new CheckboxMenuItem ( MENU_RATE_4_1, false );
        menuRate_4_1.addItemListener ( this );
        menuPopup.add ( menuRate_4_1 );
        menuRate_8_1 = new CheckboxMenuItem ( MENU_RATE_8_1, false );
        menuRate_8_1.addItemListener ( this );
        menuPopup.add ( menuRate_8_1 );
//        menuPopup.addSeparator ();

//        menuItem = new MenuItem ( MENU_PROPERTIES );
//        menuItem.addActionListener ( this );
//        menuPopup.add ( menuItem );
    }

    private void updateButtonState () {
        if ( player == null ) {
            buttonPlay.setEnabled ( false );
        }
        else {
            buttonPlay.setEnabled ( true );
            if ( player.getState() == Controller.Started )
                buttonPlay.setValue ( true );
            else
                buttonPlay.setValue ( false );
        }
    }

    public void minicleanUp() {
        synchronized (localLock) {
            firstTime = true;
        }
    }

    public void dispose() {
        synchronized (localLock) {

	    if (player == null)
		return;

            if (propsSheet != null) {
                propsSheet.dispose();
                propsSheet = null;
            }
            if (progressSlider != null) {
                progressSlider.dispose();
                progressSlider = null;
            }
            if (audioControls != null) {
                remove(audioControls);
                audioControls = null;
            }
	    if (buttonAudio != null) {
		buttonAudio.dispose();
		buttonAudio = null;
	    }

            player = null;
            gainControl = null;
	    controlFrame = null;

            if (parentFrame != null && wl != null) {
                parentFrame.removeWindowListener(wl);
                parentFrame = null;
                wl = null;
            }

    	    vectorTracksAudio.removeAllElements();
    	    vectorTracksVideo.removeAllElements();

	    /**
	     * Don't ask why, the following few lines of code
	     * will allow the Solaris production VM to garbage-collect
	     * the DefaultControlPanel.
	     */
	    if (menuItemCheck != null)
		menuItemCheck.removeItemListener(this);
            menuRate_1_4.removeItemListener(this);
            menuRate_1_2.removeItemListener(this);
            menuRate_8_1.removeItemListener(this);
            menuRate_4_1.removeItemListener(this);
            menuRate_2_1.removeItemListener(this);
            menuRate_1_1.removeItemListener(this);
	    buttonMedia.setPopupMenu(null);
        }
    }

    private void getPlayerControls() {
        Control    control;

        if (player == null)
            return;
        gainControl = player.getGainControl();

        control = player.getControl ( "javax.media.control.FramePositioningControl" );
        if ( control != null  &&  control instanceof javax.media.control.FramePositioningControl )
            controlFrame = (javax.media.control.FramePositioningControl) control;
/*
        Control [] controls = player.getControls();
        for (int i = 0; i < controls.length; i++) {
            Control ctl = controls[i];
            if (ctl instanceof GainControl) {
                gainControl = (GainControl)ctl;
            }
        }
*/
    }

    public void actionPerformed(ActionEvent ae) {
        String    command;

        command = ae.getActionCommand();
        if ( command.equalsIgnoreCase(buttonPlay.getLabel()) ) {
            playStop();
        }
        if ( buttonAudio != null  &&  command.equalsIgnoreCase(buttonAudio.getLabel()) ) {
            audioMute ();
        }
        else if ( command.equalsIgnoreCase(buttonMedia.getLabel())
                  ||  command.equalsIgnoreCase(MENU_PROPERTIES) ) {
            showPropsSheet();
        }
        else if ( buttonStepBack != null  &&  command.equalsIgnoreCase(buttonStepBack.getLabel()) ) {
            playStep ( false );
        }
        else if ( buttonStepFwd != null  &&  command.equalsIgnoreCase(buttonStepFwd.getLabel()) ) {
            playStep ( true );
        }
    }

    public void itemStateChanged ( ItemEvent event ) {
        ItemSelectable  item;
        Object          objectItem;
        String          strItem;
        int             nIndex;
        TrackControl    trackControl;
        boolean         boolEnabled;
        int             nState;
    

        item = event.getItemSelectable ();
        nState = event.getStateChange ();
        objectItem = event.getItem ();
        if ( item == menuRate_1_4  &&  nState == ItemEvent.SELECTED ) {
            menuRate_1_4.setState ( false ); // will be set only if successfull, i.e. on RateChangeEvent
            player.setRate ( (float)0.25 );
        }
        else if ( item == menuRate_1_2  &&  nState == ItemEvent.SELECTED ) {
            menuRate_1_2.setState ( false );
            player.setRate ( (float)0.5 );
        }
        else if ( item == menuRate_1_1  &&  nState == ItemEvent.SELECTED ) {
            menuRate_1_1.setState ( false );
            player.setRate ( (float)1.0 );
        }
        else if ( item == menuRate_2_1  &&  nState == ItemEvent.SELECTED ) {
            menuRate_2_1.setState ( false );
            player.setRate ( (float)2.0 );
        }
        else if ( item == menuRate_4_1  &&  nState == ItemEvent.SELECTED ) {
            menuRate_4_1.setState ( false );
            player.setRate ( (float)4.0 );
        }
        else if ( item == menuRate_8_1  &&  nState == ItemEvent.SELECTED ) {
            menuRate_8_1.setState ( false );
            player.setRate ( (float)8.0 );
        }
        else if ( objectItem instanceof String ) {
            strItem = (String) objectItem;
            if ( strItem.substring(0,5).equalsIgnoreCase(MENU_AUDIO) ) {
                nIndex = Integer.valueOf(strItem.substring(6)).intValue();
                trackControl = (TrackControl)vectorTracksAudio.elementAt ( nIndex );
                boolEnabled = event.getStateChange() == ItemEvent.SELECTED;
                muteAudioTrack ( trackControl, !boolEnabled );
            }
            else if ( strItem.substring(0,5).equalsIgnoreCase(MENU_VIDEO) ) {
            }
        }
    }

    void update() {
        if ( propsSheet == null || player == null )
            return;
	
        if ( player.getState() == Controller.Started ) {
            pausecnt = -1;
            propsSheet.update();
        }
        else {
            if (pausecnt < 5 ) {
                pausecnt ++;
                propsSheet.update();
            }
            else if ( pausecnt == 5 ) {
                pausecnt++;
                propsSheet.clearBRFR();
            }
            else if ( resetMediaTimeinPause ) {
                resetMediaTimeinPause = false;
                propsSheet.updateMediaTime();
            }
        }
	
    }

    void resetPauseCount() {
        pausecnt = -1;
    }

      
    private void playStop() {
        long    lDuration;
        long    lMedia;

        boolean state = buttonPlay.getValue();

        synchronized (localLock) {
            if (player == null || buttonPlay == null)
                return;
            if (state) {
                if (player.getTargetState() == Controller.Started) {
                    // Ignore
                }
                else {
                    buttonPlay.setEnabled(false);

                    lDuration = player.getDuration().getNanoseconds();
                    lMedia = player.getMediaNanoseconds();
                    if ( lMedia >= lDuration )
                        player.setMediaTime ( new Time(0) );
                    player.start();
                }
            }
            else {
                if (player.getTargetState() == Controller.Started) {
                    buttonPlay.setEnabled(false);
                    player.stop();
                }
                else {
                    // Ignore
                }
            }
        }
    }

    private void audioMute() {
        boolean boolState;

        if ( gainControl == null )
            return;

        boolState = buttonAudio.getValue();
        gainControl.setMute ( boolState );
    }

    private void playStep ( boolean boolFwd ) {
        long            lValue;

        if ( controlFrame == null )
            return;

        if (player.getTargetState() == Controller.Started) {
            buttonPlay.setEnabled(false);
            player.stop();
        }
        controlFrame.skip ( (boolFwd?1:(-1)) );
    }

    public void controllerUpdate(ControllerEvent ce) {
        long    lDuration;
        long    lMedia;

        synchronized (localLock) {
            if (player == null)
                return;
            if (ce instanceof StartEvent) {
                buttonPlay.setValue(true);
                buttonPlay.setEnabled(true);
                if ( buttonStepFwd != null )
                    buttonStepFwd.setEnabled ( true );
                if ( buttonStepBack != null )
                    buttonStepBack.setEnabled ( true );
            }
            else if (ce instanceof StopEvent ||
		     ce instanceof ResourceUnavailableEvent) {
                buttonPlay.setValue(false);
                buttonPlay.setEnabled(true);

                Thread.yield ();
                lDuration = player.getDuration().getNanoseconds();
                lMedia = player.getMediaNanoseconds();
                if ( buttonStepFwd != null ) {
                    if ( lMedia < lDuration - 1 )
                        buttonStepFwd.setEnabled ( true );
                    else
                        buttonStepFwd.setEnabled ( false );
                }
                if ( buttonStepBack != null ) {
                    if ( lMedia > 0 )
                        buttonStepBack.setEnabled ( true );
                    else
                        buttonStepBack.setEnabled ( false );
                }
            }
            else if (ce instanceof DurationUpdateEvent) {
                Time duration = player.getDuration();
                if ( duration == Duration.DURATION_UNKNOWN  ||  duration == Duration.DURATION_UNBOUNDED )
                    progressSlider.setEnabled(false);
                else
                    progressSlider.setEnabled(true);

                if (propsSheet != null)
                    propsSheet.updateDuration();
            }
            else if (ce instanceof MediaTimeSetEvent) {
                Thread.yield ();
                lDuration = player.getDuration().getNanoseconds();
                lMedia = player.getMediaNanoseconds();
                if ( buttonStepFwd != null ) {
                    if ( lMedia < lDuration - 1 )
                        buttonStepFwd.setEnabled ( true );
                    else
                        buttonStepFwd.setEnabled ( false );
                }
                if ( buttonStepBack != null ) {
                    if ( lMedia > 0 )
                        buttonStepBack.setEnabled ( true );
                    else
                        buttonStepBack.setEnabled ( false );
                }

                resetMediaTimeinPause = true;
            }
            else if (ce instanceof RateChangeEvent) {
                float    fRate;

                menuRate_1_4.setState ( false );
                menuRate_1_2.setState ( false );
                menuRate_1_1.setState ( false );
                menuRate_2_1.setState ( false );
                menuRate_4_1.setState ( false );
                menuRate_8_1.setState ( false );

                fRate = player.getRate ();
                if ( fRate < 0.5 ) {
                    menuRate_1_4.removeItemListener ( this );
                    menuRate_1_4.setState ( true );
                    menuRate_1_4.addItemListener ( this );
                }
                else if ( fRate < 1.0 ) {
                    menuRate_1_2.removeItemListener ( this );
                    menuRate_1_2.setState ( true );
                    menuRate_1_2.addItemListener ( this );
                }
                else if ( fRate > 4.0 ) {
                    menuRate_8_1.removeItemListener ( this );
                    menuRate_8_1.setState ( true );
                    menuRate_8_1.addItemListener ( this );
                }
                else if ( fRate > 2.0 ) {
                    menuRate_4_1.removeItemListener ( this );
                    menuRate_4_1.setState ( true );
                    menuRate_4_1.addItemListener ( this );
                }
                else if ( fRate > 1.0 ) {
                    menuRate_2_1.removeItemListener ( this );
                    menuRate_2_1.setState ( true );
                    menuRate_2_1.addItemListener ( this );
                }
                else {
                    menuRate_1_1.removeItemListener ( this );
                    menuRate_1_1.setState ( true );
                    menuRate_1_1.addItemListener ( this );
                }
            }
        }
    }

    public void gainChange ( GainChangeEvent event ) {
        boolean    boolMute;

        boolMute = gainControl.getMute ();
        buttonAudio.setValue ( boolMute );
    }

    public void componentResized(ComponentEvent e) {
        this.validate ();
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }


    /*************************************************************************
     * Overridden Component methods
     *************************************************************************/

    public void paint(Graphics g) {
        if (firstTime) {
            findFrame();
        }
        super.paint(g);
    }

    protected void findFrame() {
        synchronized (localLock) {
            if (firstTime) {
                firstTime = false;
                Component c = getParent();
                while (!(c instanceof Frame) && c != null) {
                    c = c.getParent();
                }
                if (c instanceof Frame) {
                    parentFrame = (Frame)c;
                    ((Frame)c).addWindowListener(wl = new WindowAdapter() {
                        public void windowClosing(WindowEvent we) {
                            minicleanUp();
                        }
                    } );

                }
            }
        }
    }

    public Insets getInsets () {
        Insets    insets;

        insets = new Insets (1,0,0,0);
        return ( insets );
    }

    private void showPropsSheet() {
        Point       point;
        Dimension   dim;

        if (propsSheet == null) {
            try {
                propsSheet = new PropertySheet ( parentFrame, player );
                if ( this.isShowing() ) {
                    point = this.getLocationOnScreen ();
                    dim = this.getSize ();
                    point.y += dim.height;
                    propsSheet.setLocation ( point );
                }
            }
            catch (Exception e) {
                propsSheet = null;
            }
        }

        if (propsSheet != null) {
            propsSheet.setVisible(true);
        }

    }

    private void muteAudioTrack ( TrackControl trackControl, boolean boolMute ) {
        int               i;
        int               nCount;
        Object            arrControls [];

        arrControls = trackControl.getControls ();
        nCount = arrControls.length;
        for ( i = 0;  i < nCount;  i++ ) {
            if ( arrControls[i] instanceof GainControl )
                ((GainControl)arrControls[i]).setMute ( boolMute );
        }
    }

}



class TransparentPanel extends Container implements ComponentListener {

    public TransparentPanel () {
        super ();
        this.addComponentListener ( this );
    }

    public TransparentPanel ( LayoutManager mgrLayout ) {
        super ();
        this.setLayout ( mgrLayout );
        this.addComponentListener ( this );
    }

    public void componentResized(ComponentEvent e) {
        this.doLayout ();
        this.repaint ();
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }


}


class AudioButton extends ButtonComp {

    private GainControl    gainControl;
    private GainSlider     sliderGain = null;

    public AudioButton ( GainControl gainControl ) {
        super ( "Audio", "audio.gif", "audio-active.gif", "audio-pressed.gif", "audio-disabled.gif",
                         "mute.gif", "mute-active.gif", "mute-pressed.gif", "mute-disabled.gif" );
        this.gainControl = gainControl;
        this.setMousePopup ( true );
    }

    protected void processMousePopup () {
        Dimension    dim;
        Point        point;

        if ( this.isShowing()  &&  gainControl.getLevel() >= 0 ) {
            if ( sliderGain == null )
                sliderGain = new GainSlider ( gainControl, getFrame() );
            dim = this.getSize ();
            point = this.getLocationOnScreen ();
            point.y += dim.height;
            sliderGain.setLocation ( point );
            sliderGain.setVisible ( !sliderGain.isVisible() );
        }
    }

    private Frame getFrame () {
        Component    parent;
        Frame        frame = null;

        parent = this;
        while ( parent != null  &&  !(parent instanceof Frame ) ) {
            parent = parent.getParent ();
        }

        if ( parent != null  &&  parent instanceof Frame )
            frame = (Frame) parent;
        return ( frame );
    }

    public void dispose() {
	gainControl = null;
	if (sliderGain != null) {
	    sliderGain.dispose();
	    sliderGain = null;
	}
    }
}


class GainSlider extends Window implements GainChangeListener, MouseListener,
                                           MouseMotionListener, FocusListener {

    private GainControl         gainControl;
    private Image               imageGrabber = null;
    private Dimension           dimGrabber = new Dimension ();
    private Button              buttonFocus;
    private boolean             boolFocus = false;
    private boolean             pressed = false;
    private PopupThread         threadPopup = null;
    private Image               imageBackground = null;

    private static final int    WIDTH = 80;
    private static final int    HEIGHT = 20;

    public GainSlider ( GainControl gainControl ) {
        this ( gainControl, new Frame() );
    }

    public GainSlider ( GainControl gainControl, Frame frame ) {
        super ( frame );

        this.gainControl = gainControl;
        try {
            init ();
        }
        catch ( Exception exception ) {
        }
    }

    public void dispose() {
	gainControl = null;
    }

    public void addNotify () {
        Insets  insets;

        super.addNotify ();
        insets = this.getInsets ();
        this.setSize ( WIDTH + insets.left + insets.right, HEIGHT + insets.top + insets.bottom );
    }

    private void init () throws Exception {
        gainControl.addGainChangeListener ( this );
        this.addMouseListener ( this );
        this.addMouseMotionListener ( this );
        this.setLayout ( null );
        imageBackground = BasicComp.fetchImage ( "texture3.gif" );

        buttonFocus = new Button ( "Focus" );
        buttonFocus.setBounds ( -100, -100, 80, 24 );
        this.add ( buttonFocus );
        buttonFocus.addFocusListener ( this );

        imageGrabber = BasicComp.fetchImage ( "grabber.gif" );
        this.setBackground ( Color.lightGray );
        this.setSize ( WIDTH, HEIGHT );
    }

    public void setVisible ( boolean boolVisible ) {
        super.setVisible ( boolVisible );

        if ( boolVisible == true ) {
            buttonFocus.requestFocus ();
            if ( threadPopup != null )
                threadPopup.stopNormaly ();

            threadPopup = new PopupThread ( this );
            threadPopup.resetCounter ( 3 );
            threadPopup.start ();
        }
        else if ( threadPopup != null ) {
            threadPopup.stopNormaly ();
        }
    }

    public void update ( Graphics g ) {
        Rectangle       rectClient;
        Image           image;
        Graphics        graphics;

        rectClient = this.getBounds ();
        image = createImage ( rectClient.width, rectClient.height );
        if ( image != null )
            graphics = image.getGraphics ();
        else
            graphics = g;

        paint ( graphics );

        if ( image != null )
            g.drawImage ( image, 0, 0, this );
    }

    public void paint ( Graphics graphics ) {
        int         x, y;
        Dimension   dimSize;
        int         widthGrabber;
        float       levelGain;
        Insets      insets;
        Rectangle   rect;


        paintBackground ( graphics );

        dimSize = this.getSize ();
        insets = this.getInsets ();
        rect = new Rectangle ( insets.left, insets.top,
                                dimSize.width - insets.left - insets.right,
                                dimSize.height - insets.top - insets.bottom );

        graphics.setColor ( this.getBackground() );
        graphics.draw3DRect ( rect.x, rect.y, rect.width - 1, rect.height - 1, true );
        graphics.draw3DRect ( rect.x + 4, rect.y + (rect.height / 2) - 2, rect.width - 9, 3, false );

        if ( dimGrabber.width < 1 )
            dimGrabber.width = imageGrabber.getWidth ( this );
        if ( dimGrabber.height < 1 )
            dimGrabber.height = imageGrabber.getHeight ( this );

        levelGain = gainControl.getLevel ();
        x = rect.x + (int)(2 + levelGain * (rect.width - 5 - dimGrabber.width));
        y = rect.y + (rect.height - dimGrabber.height) / 2;
        graphics.drawImage ( imageGrabber, x, y, this );
    }

    private void paintBackground ( Graphics graphics ) {
        Dimension    dimSize;
        Rectangle    rectTile;
        Rectangle    rectClip;

        dimSize = getSize ();
        if ( imageBackground == null ) {
            graphics.setColor ( getBackground() );
            graphics.fillRect ( 0, 0, dimSize.width, dimSize.height );
        }
        else {
            rectTile = new Rectangle ( 0, 0, imageBackground.getWidth(this), imageBackground.getHeight(this) );
            rectClip = graphics.getClipBounds ();

            while ( rectTile.y < dimSize.height ) {
                while ( rectTile.x < dimSize.width ) {
                    if ( rectClip == null  ||  rectClip.intersects(rectTile) ) {
                        graphics.drawImage ( imageBackground, rectTile.x, rectTile.y, this );
                    }
                    rectTile.x += rectTile.width;
                }
                rectTile.x = 0;
                rectTile.y += rectTile.height;
            }
        }
    }

    public void gainChange ( GainChangeEvent event ) {
        repaint ();
    }

    public void mouseClicked ( MouseEvent event ) {
    }

    public void mousePressed ( MouseEvent event ) {
        Point    pointMouse;

        if ( threadPopup != null )
            threadPopup.resetCounter ( 3 );
        pointMouse = event.getPoint ();
        setLevelToMouse ( pointMouse );
	pressed = true;
    }

    public void mouseReleased ( MouseEvent event ) {
	pressed = false;
	if (boolFocus == false)
	    this.setVisible(false);
    }

    public void mouseEntered ( MouseEvent event ) {
        boolFocus = true;
        if ( threadPopup != null )
            threadPopup.stopNormaly ();
    }

    public void mouseExited ( MouseEvent event ) {
        if ( boolFocus == true && !pressed)
            this.setVisible ( false );
        boolFocus = false;
    }

    public void mouseDragged ( MouseEvent event ) {
        Point    pointMouse;

        if ( threadPopup != null )
            threadPopup.resetCounter ( 3 );
        pointMouse = event.getPoint ();
        setLevelToMouse ( pointMouse );
    }

    public void mouseMoved ( MouseEvent event ) {
    }

    public void focusLost ( FocusEvent event ) {
    }

    public void focusGained ( FocusEvent event ) {
    }

    private void setLevelToMouse ( Point pointMouse ) {
        int         nPos;
        int         nWidth;
        Dimension   dimSize;
        Insets      insets;
        float       levelGain;

	if (gainControl == null)
	    return;

        dimSize = this.getSize ();
        insets = this.getInsets ();

        nPos = pointMouse.x - 2 - insets.left;
        nWidth = dimSize.width - insets.left - insets.right - 5;
        if ( nPos > nWidth )
            nPos = nWidth;
        if ( nPos < 0 )
            nPos = 0;
        levelGain = (float)nPos / nWidth;
        gainControl.setMute ( false );
        gainControl.setLevel ( levelGain );
    }

}

class PopupThread extends Thread {

    private Window        window;
    private int           nTimeCounter = 3;
    private boolean       boolRun = true;


    public PopupThread ( Window window ) {
        this.window = window;
    }

    public void resetCounter ( int nTimeCounter ) {
        this.nTimeCounter = nTimeCounter;
    }

    public void stopNormaly () {
        boolRun = false;
    }

    /**
    *
    */
    public void run () {
        while ( boolRun ) {
            if ( nTimeCounter < 1 )
                window.setVisible ( false );
            try {
                sleep ( 1000 );
            }
            catch ( Exception exception ) {
            }
            nTimeCounter--;
        }
    }
}


