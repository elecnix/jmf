/*
 * @(#)PropertySheet.java	1.38 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.*;
import javax.media.format.*;
import javax.media.control.*;
import javax.media.protocol.*;

import com.sun.media.*;
import com.sun.media.util.JMFI18N;


public class PropertySheet extends Dialog implements WindowListener, ActionListener {

    private Player              player;
    private Vector              vectorControlBitRate = new Vector(1);
    private Vector              vectorLabelBitRate = new Vector(1);
    private FrameRateControl    controlFrameRate = null;
    
    private Vector              vectorTrackFormats = new Vector ();
    private int                 nAudioTrackCount = 0;
    private int                 nVideoTrackCount = 0;
    private Vector              vectorMiscControls = new Vector ();

    private Button        buttonClose;
    private Label         labelDuration = null;
    private Label         labelPosition = null;
    private Label         labelBitRate = null;
    private Label         labelFrameRate = null;

    private ColumnList    columnListAudio;
    private ColumnList    columnListVideo;
    

    private static final String         STR_UNKNOWN = JMFI18N.getResource("propertysheet.unknown");
    private static final String         STR_UNBOUNDED = JMFI18N.getResource("propertysheet.unbounded");


    public PropertySheet ( Frame parent, Player player ) {
        super ( parent, JMFI18N.getResource("propertysheet.title"), false );

        this.player = player;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void init () throws Exception {
        Panel         panel;
        Dimension     dim;


        this.setLayout ( new BorderLayout(5,5) );
        this.setBackground ( Color.lightGray );

        panel = createPanelButtons ();
        this.add ( panel, BorderLayout.SOUTH );

        panel = createPanelProperties ();
        this.add ( panel, BorderLayout.CENTER );

        // ...
        Canvas    canvas;
        canvas = new Canvas ();
        this.add ( canvas, BorderLayout.NORTH );
        canvas = new Canvas ();
        this.add ( canvas, BorderLayout.EAST );
        canvas = new Canvas ();
        this.add ( canvas, BorderLayout.WEST );

        this.pack ();
        this.addWindowListener ( this );
        this.setResizable ( false );
        dim = this.getPreferredSize ();
        if ( dim.width > 480 )
            dim.width = 480;
        this.setBounds ( 100, 100, dim.width, dim.height );
        this.repaint ();
    }

    private Panel createPanelProperties () throws Exception {
        TabControl    tabControl;
        Panel         panel;
        int           i;
        int           nIndexAudio;
        int           nIndexVideo;
        int           nCount;
        Object        format;
        String        strTitle;

        tabControl = new TabControl ( TabControl.ALIGN_TOP );

        panel = createPanelGeneral ();
        tabControl.addPage ( panel, JMFI18N.getResource("propertysheet.tab.general") );

        if ( nVideoTrackCount > 0 ) {
            panel = createPanelVideo ( vectorTrackFormats );
            tabControl.addPage ( panel, JMFI18N.getResource("propertysheet.tab.video") );
        }

        if ( nAudioTrackCount > 0 ) {
            panel = createPanelAudio ( vectorTrackFormats );
            tabControl.addPage ( panel, JMFI18N.getResource("propertysheet.tab.audio") );
        }

        if ( !(vectorMiscControls.isEmpty()) ) {
            panel = createPanelMisc ();
            tabControl.addPage ( panel, JMFI18N.getResource("propertysheet.tab.misc") );
        }

        update ();
        return ( tabControl );
    }

    private Panel createPanelGeneral () throws Exception {
        int                 i;
        Panel               panelGeneral;
        Panel               panelLabels;
        Panel               panelData;
        Panel               panel;
        Label               label;
        UrlLabel            labelUrl;
        Control             arrControls [];
        Format              format;
        BasicPlayer         playerBasic;
        MediaLocator        mediaLocator;
        String              strValue = null;
        BitRateControl      controlBitRateTemp;
        FrameRateControl    controlFrameRateTemp;


        panelGeneral = new Panel ( new BorderLayout() );

        panel = new Panel ( new BorderLayout(8,4) );
        panelGeneral.add ( panel, BorderLayout.NORTH );

        panelLabels = new Panel ( new GridLayout(0,1,4,4) );
        panel.add ( panelLabels, BorderLayout.WEST );
        panelData = new Panel ( new GridLayout(0,1,4,4) );
        panel.add ( panelData, BorderLayout.CENTER );

        if ( player instanceof BasicPlayer ) {
            playerBasic = (BasicPlayer) player;

            mediaLocator = playerBasic.getMediaLocator ();
            if (mediaLocator != null)
                strValue = mediaLocator.toString();
            if ( strValue != null ) {
                label = new Label ( JMFI18N.getResource("propertysheet.general.medialocation"), Label.RIGHT );
                panelLabels.add ( label );
                labelUrl = new UrlLabel ( strValue );
                panelData.add ( labelUrl );
            }

            strValue = playerBasic.getContentType ();
            if ( strValue != null ) {
                strValue = (new ContentDescriptor(strValue)).toString();
                label = new Label ( JMFI18N.getResource("propertysheet.general.contenttype"), Label.RIGHT );
                panelLabels.add ( label );
                label = new Label ( strValue );
                panelData.add ( label );
            }
        }

        label = new Label ( JMFI18N.getResource("propertysheet.general.duration"), Label.RIGHT );
        panelLabels.add ( label );
        labelDuration = new Label ();
        panelData.add ( labelDuration );

        label = new Label ( JMFI18N.getResource("propertysheet.general.position"), Label.RIGHT );
        panelLabels.add ( label );
        labelPosition = new Label ();
        panelData.add ( labelPosition );

        nAudioTrackCount = 0;
        nVideoTrackCount = 0;
        arrControls = player.getControls();
        for ( i = 0;  i < arrControls.length;  i++ ) {
            if ( arrControls[i] == null )
                continue;

            if ( arrControls[i] instanceof FormatControl ) {
                if ( !(arrControls[i] instanceof Owned
                        &&  ( ((Owned)arrControls[i]).getOwner() instanceof SourceStream
                        ||  ((Owned)arrControls[i]).getOwner() instanceof DataSource )) ) {
                    format = ((FormatControl)arrControls[i]).getFormat ();
                    vectorTrackFormats.addElement ( format );
                    if ( format instanceof AudioFormat )
                        nAudioTrackCount++;
                    else if ( format instanceof VideoFormat )
                        nVideoTrackCount++;
                }
            }

            if ( arrControls[i] instanceof TrackControl ) {
                // Can't think of anything to do.  The FormatControl
                // clause has already taken care of the work and I don't
                // want TrackControl to fall into the "else" clause.
                // --ivg
            }
            else if ( arrControls[i] instanceof BitRateControl ) {
                controlBitRateTemp = (BitRateControl) arrControls[i];
                if ( controlBitRateTemp instanceof Owned
                        &&  ((Owned)controlBitRateTemp).getOwner() instanceof Controller ) {
                    vectorControlBitRate.addElement(controlBitRateTemp);
                    label = new Label ( JMFI18N.getResource("propertysheet.general.bitrate"), Label.RIGHT );
                    panelLabels.add ( label );
                    labelBitRate = new Label ();
		    vectorLabelBitRate.addElement(labelBitRate);
                    panelData.add ( labelBitRate );
                }
                else {
                    vectorMiscControls.addElement ( arrControls[i] );
                }
            }
            else if ( arrControls[i] instanceof FrameRateControl ) {
                controlFrameRateTemp = (FrameRateControl) arrControls[i];
                if ( controlFrameRateTemp instanceof Owned
                        &&  ((Owned)controlFrameRateTemp).getOwner() instanceof Controller ) {
                    controlFrameRate = controlFrameRateTemp;
                    label = new Label ( JMFI18N.getResource("propertysheet.general.framerate"), Label.RIGHT );
                    panelLabels.add ( label );
                    labelFrameRate = new Label ();
                    panelData.add ( labelFrameRate );
                }
                else {
                    vectorMiscControls.addElement ( arrControls[i] );
                }
            }
            else if ( arrControls[i] instanceof GainControl ) {
                // ignore it
            }
            else if ( arrControls[i] instanceof MonitorControl ) {
                // ignore it
            }
            else if ( arrControls[i] instanceof Owned
                        &&  ( ((Owned)arrControls[i]).getOwner() instanceof CaptureDevice
                        ||  ((Owned)arrControls[i]).getOwner() instanceof SourceStream
                        ||  ((Owned)arrControls[i]).getOwner() instanceof DataSource ) ) {
                // ignore it
            }
            else if ( arrControls[i] instanceof CachingControl ) {
                // ignore it
            }
            else {
                vectorMiscControls.addElement ( arrControls[i] );
            }

        }
        return ( panelGeneral );
    }

    private Panel createPanelVideo ( Vector vectorFormats ) throws Exception {
        int             i;
        int             nCount;
        int             nTrackIndex;
        Panel           panelVideo;
        Dimension       dimSize;
        float           fValue;
        VideoFormat     formatVideo;
        Object          objectFormat;
        String          arrValues [];
        String          arrColumnNames [] = {
                            JMFI18N.getResource("propertysheet.video.track"),
                            JMFI18N.getResource("propertysheet.video.encoding"),
                            JMFI18N.getResource("propertysheet.video.size"),
                            JMFI18N.getResource("propertysheet.video.framerate"),
                        };

        panelVideo = new Panel ( new BorderLayout() );

        columnListVideo = new ColumnList ( arrColumnNames );
        arrValues = new String [arrColumnNames.length];
        nCount = vectorFormats.size ();
        nTrackIndex = 0;
        for ( i = 0;  i < nCount;  i++ ) {
            objectFormat = vectorFormats.elementAt ( i );
            if ( !(objectFormat instanceof VideoFormat) )
                continue;
            formatVideo = (VideoFormat) objectFormat;
            nTrackIndex++;

            arrValues[0] = new String ( "" + nTrackIndex );
            arrValues[1] = formatVideo.getEncoding();

            dimSize = formatVideo.getSize ();
            if ( dimSize == null )
                arrValues[2] = new String ( STR_UNKNOWN );
            else
                arrValues[2] = new String ( "" + dimSize.width + " x " + dimSize.height );

            fValue = formatVideo.getFrameRate ();
            if ( fValue == Format.NOT_SPECIFIED )
                arrValues[3] = STR_UNKNOWN;
            else
                arrValues[3] = "" + fValue;

            columnListVideo.addRow ( arrValues );
        }
        columnListVideo.setColumnWidthAsPreferred ();
        panelVideo.add ( columnListVideo, BorderLayout.CENTER );
        return ( panelVideo );
    }

    private Panel createPanelAudio ( Vector vectorFormats ) throws Exception {
        int             i;
        int             nCount;
        int             nTrackIndex;
        Panel           panelAudio;
        int             nValue;
        double          dValue;
        AudioFormat     formatAudio;
        Object          objectFormat;
        String          arrValues [];
        String          arrColumnNames [] = {
                            JMFI18N.getResource("propertysheet.audio.track"),
                            JMFI18N.getResource("propertysheet.audio.encoding"),
                            JMFI18N.getResource("propertysheet.audio.samplerate"),
                            JMFI18N.getResource("propertysheet.audio.bitspersample"),
                            JMFI18N.getResource("propertysheet.audio.channels"),
                        };

        panelAudio = new Panel ( new BorderLayout() );

        columnListAudio = new ColumnList ( arrColumnNames );
        arrValues = new String [arrColumnNames.length];
        nCount = vectorFormats.size ();
        nTrackIndex = 0;
        for ( i = 0;  i < nCount;  i++ ) {
            objectFormat = vectorFormats.elementAt ( i );
            if ( !(objectFormat instanceof AudioFormat) )
                continue;
            formatAudio = (AudioFormat) objectFormat;
            nTrackIndex++;

            arrValues[0] = new String ( "" + nTrackIndex );
            arrValues[1] = formatAudio.getEncoding();

            dValue = formatAudio.getSampleRate ();
            if ( dValue == Format.NOT_SPECIFIED )
                arrValues[2] = STR_UNKNOWN;
            else
                arrValues[2] = "" + dValue;

            nValue = formatAudio.getSampleSizeInBits ();
            if ( nValue == Format.NOT_SPECIFIED )
                arrValues[3] = STR_UNKNOWN;
            else
                arrValues[3] = "" + nValue;

            nValue = formatAudio.getChannels ();
            if ( nValue == Format.NOT_SPECIFIED )
                arrValues[4] = STR_UNKNOWN;
            else if ( nValue == 1 )
               arrValues[4] = "" + nValue + " (" + JMFI18N.getResource("propertysheet.audio.channels.mono") + ")";
            else if ( nValue == 2)
               arrValues[4] = "" + nValue + " (" + JMFI18N.getResource("propertysheet.audio.channels.stereo") + ")";
            else
               arrValues[4] = "" + nValue;

            columnListAudio.addRow ( arrValues );
        }
        columnListAudio.setColumnWidthAsPreferred ();
        panelAudio.add ( columnListAudio, BorderLayout.CENTER );
        return ( panelAudio );
    }

    private Panel createPanelMisc () throws Exception {
        int           i;
        int           nSize;
        Panel         panel;
        Panel         panelMisc;
        Panel         panelNext;
        Panel         panelControl;
        Control       control;
        Component     comp;

        panelMisc = new Panel ( new BorderLayout(6,6) );
        panel = panelMisc;
        nSize = vectorMiscControls.size ();
        for ( i = 0;  i < nSize;  i++ ) {
            control = (Control) vectorMiscControls.elementAt ( i );
            comp = control.getControlComponent();
	    if ( comp != null  &&  comp.getParent() == null ) {
                panelControl = new Panel ( new BorderLayout(6,6) );
                panelControl.add ( comp, BorderLayout.WEST );
                panelNext = new Panel ( new BorderLayout(6,6) );
                panelNext.add ( panelControl, BorderLayout.NORTH );
                panel.add ( panelNext, BorderLayout.CENTER );
                panel = panelNext;
            }
        }
        return ( panelMisc );
    }

    private Panel createPanelButtons () throws Exception {
        Panel    panelButtons;
        Panel    panel;

        panelButtons = new Panel ( new FlowLayout(FlowLayout.RIGHT) );

        panel = new Panel ( new GridLayout(1,0,6,6) );
        panelButtons.add ( panel );

        buttonClose = new Button ( JMFI18N.getResource("propertysheet.close") );
        buttonClose.addActionListener ( this );
        panel.add ( buttonClose );

        return ( panelButtons );
    }

    public void actionPerformed ( ActionEvent e ) {
        String  strCmd;

        strCmd = e.getActionCommand ();
        if ( strCmd.equals(buttonClose.getLabel()) ) {
            this.setVisible ( false );
        }
    }

    public void windowOpened ( WindowEvent e ) {
    }

    public void windowClosing ( WindowEvent e ) {
        this.setVisible ( false );
    }

    public void windowClosed ( WindowEvent e ) {
    }

    public void windowIconified ( WindowEvent e ) {
    }

    public void windowDeiconified ( WindowEvent e ) {
    }

    public void windowActivated ( WindowEvent e ) {
    }

    public void windowDeactivated ( WindowEvent e ) {
    }

    void update () {
        updateBitRate ();
        updateFrameRate ();
	updateMediaTime ();
        updateDuration ();
    }

    void updateDuration () {
	Time    timeDuration;

        if ( labelDuration != null ) {
            timeDuration = player.getDuration ();
	    labelDuration.setText ( formatTime(timeDuration) );
        }
    }

    void updateBitRate () {
	int    bitRate;

        if (vectorLabelBitRate.size() > 0) {
	    for (int i = 0; i < vectorLabelBitRate.size(); i++) {
		Label labelBitRate = (Label)
		    vectorLabelBitRate.elementAt(i);
		BitRateControl controlBitRate = (BitRateControl)
		    vectorControlBitRate.elementAt(i);
		bitRate = controlBitRate.getBitRate();
		labelBitRate.setText(Float.toString(bitRate/1000.0f) + " " +
				     JMFI18N.getResource("propertysheet.kbps"));
	    }
        }
    }

  void updateFrameRate () {
    float    frameRate;
    
    if ( labelFrameRate != null  &&  controlFrameRate != null ) {
      frameRate = controlFrameRate.getFrameRate ();
      labelFrameRate.setText ( Float.toString(frameRate) + " " + JMFI18N.getResource("propertysheet.fps") );
    }
  }
  
  void clearBRFR() {
    if ( labelFrameRate != null ) 
      labelFrameRate.setText("0.0 " + JMFI18N.getResource("propertysheet.fps") );
    
    if ( labelBitRate != null )
      labelBitRate.setText("0.0 " + JMFI18N.getResource("propertysheet.kbps") );
  }
  
  void updateMediaTime () {
    Time    timeMedia;
    
        if ( labelPosition != null ) {
	  timeMedia = player.getMediaTime ();
	  labelPosition.setText ( formatTime(timeMedia) );
	}
    }

    private String formatTime ( Time time ) {
	long    nano;
	int     hours;
	int     minutes;
	int     seconds;
	int     hours10;
	int     minutes10;
	int     seconds10;
	long    nano10;
        String  strTime = new String ( STR_UNKNOWN );

	if ( time == null  ||  time == Time.TIME_UNKNOWN  ||  time == javax.media.Duration.DURATION_UNKNOWN )
	    return ( strTime );

	if ( time == javax.media.Duration.DURATION_UNBOUNDED )
	    return ( STR_UNBOUNDED );

	nano = time.getNanoseconds();
	seconds = (int) (nano / Time.ONE_SECOND);
	hours = seconds / 3600;
	minutes = ( seconds - hours * 3600 ) / 60;
	seconds = seconds - hours * 3600 - minutes * 60;
	nano = (long) ((nano % Time.ONE_SECOND) / (Time.ONE_SECOND/100));

        hours10 = hours / 10;
        hours = hours % 10;
        minutes10 = minutes / 10;
        minutes = minutes % 10;
        seconds10 = seconds / 10;
        seconds = seconds % 10;
        nano10 = nano / 10;
        nano = nano % 10;

        strTime = new String ( "" + hours10 + hours + ":" + minutes10 + minutes + ":" + seconds10 + seconds + "." + nano10 + nano );
	return ( strTime );
    }


}


class UrlLabel extends Label implements ComponentListener {
    private String    strLabel = null;

    public UrlLabel ( String strLabel ) {
        super ( strLabel );

        this.strLabel = strLabel;
        this.addComponentListener ( this );
    }

    public synchronized void setText ( String strLabel ) {
        this.strLabel = strLabel;
        setInternalLabel ();
    }

    public String getText () {
        return ( this.strLabel );
    }

    public String toString () {
        return ( this.strLabel );
    }


    private void setInternalLabel () {
        Rectangle    rect;
        Font         font;
        FontMetrics  fontMetrics;
        int          nWidth;
        String       strLabel;
        int          nIndex1;
        int          nIndex2;


        rect = this.getBounds ();
        font = this.getFont ();
        fontMetrics = this.getFontMetrics ( font );
        strLabel = this.strLabel;
        nWidth = fontMetrics.stringWidth ( strLabel );
        nIndex1 = this.strLabel.lastIndexOf ( java.io.File.separatorChar );
        nIndex2 = nIndex1;
        while ( nIndex2 >= 0  &&  nWidth > rect.width ) {
            nIndex2 = this.strLabel.lastIndexOf ( java.io.File.separatorChar, nIndex2 - 1 );
            if ( nIndex2 < 0 )
                strLabel = "..." + this.strLabel.substring ( nIndex1 );
            else
                strLabel = this.strLabel.substring ( 0, nIndex2 + 1 ) + "..." + this.strLabel.substring ( nIndex1 );
            nWidth = fontMetrics.stringWidth ( strLabel );
        }

        super.setText ( strLabel );
    }

    public void componentResized ( ComponentEvent event ) {
        setInternalLabel ();
    }

    public void componentMoved ( ComponentEvent event ) {
    }

    public void componentShown ( ComponentEvent event ) {
    }

    public void componentHidden ( ComponentEvent event ) {
    }

}


