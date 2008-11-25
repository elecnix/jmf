/*
 * @(#)VideoFormatChooser.java	1.21 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.datasink.*;

import com.sun.media.util.JMFI18N;


public class VideoFormatChooser extends Panel implements ItemListener, ActionListener {

    public static final String  ACTION_TRACK_ENABLED = "ACTION_VIDEO_TRACK_ENABLED";
    public static final String  ACTION_TRACK_DISABLED = "ACTION_VIDEO_TRACK_DISABLED";

    private VideoFormat         formatOld;
    private Format              arrSupportedFormats [] = null;
    private float               customFrameRates [] = null;
    private Vector              vectorContSuppFormats = new Vector ();
    private boolean             boolDisplayEnableTrack;
    private ActionListener      listenerEnableTrack;
    private boolean             boolEnableTrackSaved = true;

    private Checkbox            checkEnableTrack;
    private Label               labelEncoding;
    private Choice              comboEncoding;
    private Label               labelSize;
//    private Choice              controlSize;
    private VideoSizeControl    controlSize;
    private Label               labelFrameRate;
    private Choice              comboFrameRate;
    private Label               labelExtra;
    private Choice              comboExtra;
    private int                 nWidthLabel = 0;
    private int                 nWidthData = 0;

    private static final int MARGINH = 12;
    private static final int MARGINV = 6;


    private static final float [] standardCaptureRates = new float [] {
        15f, 1f, 2f, 5f, 7.5f, 10f, 12.5f, 20f, 24f, 25f, 30f
    };

    private static final String     DEFAULT_STRING = JMFI18N.getResource("formatchooser.default");


    public VideoFormatChooser ( Format arrFormats[], VideoFormat formatDefault,
				float [] frameRates ) {
        this ( arrFormats, formatDefault, false, null, frameRates );
    }

    public VideoFormatChooser ( Format arrFormats[], VideoFormat formatDefault ) {
        this ( arrFormats, formatDefault, false, null, null );
    }

    public VideoFormatChooser ( Format arrFormats[], VideoFormat formatDefault,
                                boolean boolDisplayEnableTrack,
                                ActionListener listenerEnableTrack) {
        this(arrFormats, formatDefault, boolDisplayEnableTrack, listenerEnableTrack, null);
    }
    
    public VideoFormatChooser ( Format arrFormats[], VideoFormat formatDefault,
                                boolean boolDisplayEnableTrack,
                                ActionListener listenerEnableTrack,
				                boolean capture) {
        this(arrFormats, formatDefault, boolDisplayEnableTrack,
                    listenerEnableTrack, capture? standardCaptureRates : null);
    }
    
    public VideoFormatChooser ( Format arrFormats[], VideoFormat formatDefault,
                                boolean boolDisplayEnableTrack,
                                ActionListener listenerEnableTrack,
                                float [] frameRates) {
        int    i;
        int    nCount;

        this.arrSupportedFormats = arrFormats;
        this.boolDisplayEnableTrack = boolDisplayEnableTrack;
        this.listenerEnableTrack = listenerEnableTrack;
        this.customFrameRates = frameRates;
	
        nCount = arrSupportedFormats.length;
        for ( i = 0;  i < nCount;  i++ ) {
            if ( arrSupportedFormats[i] instanceof VideoFormat )
                vectorContSuppFormats.addElement ( arrSupportedFormats[i] );
        }

        if ( isFormatSupported(formatDefault) )
            this.formatOld = formatDefault;
        else
            this.formatOld = null;

        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setEnabled ( boolean boolEnable ) {
        super.setEnabled ( boolEnable );

        if ( checkEnableTrack != null )
            checkEnableTrack.setEnabled ( boolEnable );
        enableControls ( boolEnable );
    }

    public Format getFormat () {
        int             i;
        int             nSize;
        String          strEncoding;
        Integer         integerBitsPerPixel;
        String          strBitsPerPixel;
        int             nYuvType;
        String          strYuvType = null;
        Object          objectFormat;
        VideoFormat     formatVideo = null;
        VideoFormat     formatVideoNew;
        RGBFormat       formatRGB;
        YUVFormat       formatYUV;


        strEncoding = comboEncoding.getSelectedItem ();

        nSize = vectorContSuppFormats.size ();
        for ( i = 0;  i < nSize;  i++ ) {
            objectFormat = vectorContSuppFormats.elementAt ( i );
            if ( !(objectFormat instanceof VideoFormat) )
                continue;
            formatVideo = (VideoFormat) objectFormat;

            if ( !this.isFormatGoodForEncoding(formatVideo) )
                continue;
            if ( !this.isFormatGoodForVideoSize(formatVideo) )
                continue;
            if ( !this.isFormatGoodForFrameRate(formatVideo) )
                continue;

            if ( strEncoding.equalsIgnoreCase(VideoFormat.RGB)  &&  formatVideo instanceof RGBFormat ) {
                formatRGB = (RGBFormat) formatVideo;
                integerBitsPerPixel = new Integer ( formatRGB.getBitsPerPixel() );
                strBitsPerPixel = integerBitsPerPixel.toString ();
                if ( !(comboExtra.getSelectedItem().equals(strBitsPerPixel)) )
                    continue;
            }
            else if ( strEncoding.equalsIgnoreCase(VideoFormat.YUV)  &&  formatVideo instanceof YUVFormat ) {
                formatYUV = (YUVFormat) formatVideo;
                nYuvType = formatYUV.getYuvType ();
                strYuvType = getYuvType ( nYuvType );
                if ( strYuvType == null  ||  !(comboExtra.getSelectedItem().equals(strYuvType)) )
                    continue;
            }

            break;
        }
        if ( i >= nSize )
            return ( null );

        if ( formatVideo.getSize() == null ) {
            formatVideoNew = new VideoFormat ( null, controlSize.getVideoSize(), Format.NOT_SPECIFIED, null, -1f );
            formatVideo = (VideoFormat) formatVideoNew.intersects ( formatVideo );
        }
        if (customFrameRates != null && formatVideo != null) {
            formatVideoNew = new VideoFormat ( null, null, Format.NOT_SPECIFIED, null, getFrameRate() );
            formatVideo = (VideoFormat) formatVideoNew.intersects ( formatVideo );
        }

        return ( formatVideo );
    }

    public float getFrameRate() {
        String selection = comboFrameRate.getSelectedItem();
        if (selection != null) {
            if ( selection.equals(DEFAULT_STRING) )
                return ( Format.NOT_SPECIFIED );
            try {
                float fr = Float.valueOf(selection).floatValue();
                return fr;
            }
            catch (NumberFormatException nfe) {
            }
        }
        return ( Format.NOT_SPECIFIED );
    }
		
	
    public void setCurrentFormat ( VideoFormat formatDefault ) {
        if ( isFormatSupported(formatDefault) )
            this.formatOld = formatDefault;
        updateFields ( formatOld );
    }

    public void setFrameRate(float frameRate) {
        for (int i = 0; i < comboFrameRate.getItemCount(); i++) {
            float value = Float.valueOf(comboFrameRate.getItem(i)).floatValue();
            if (Math.abs(frameRate - value) < 0.5) {
                comboFrameRate.select(i);
                return;
            }
        }
    }

    public void setSupportedFormats ( Format arrFormats[], VideoFormat formatDefault ) {
        int    i;
        int    nCount;

        this.arrSupportedFormats = arrFormats;

        vectorContSuppFormats.removeAllElements ();
        nCount = arrSupportedFormats.length;
        for ( i = 0;  i < nCount;  i++ ) {
            if ( arrSupportedFormats[i] instanceof VideoFormat )
                vectorContSuppFormats.addElement ( arrSupportedFormats[i] );
        }
        if ( isFormatSupported(formatDefault) )
            this.formatOld = formatDefault;
        else
            this.formatOld = null;
        setSupportedFormats ( vectorContSuppFormats );
    }

    public void setSupportedFormats ( Vector vectorContSuppFormats ) {
        this.vectorContSuppFormats = vectorContSuppFormats;

        if ( vectorContSuppFormats.isEmpty() ) {
            checkEnableTrack.setState ( false );
            checkEnableTrack.setEnabled ( false );
            onEnableTrack ( true );
            return;
        }
        else {
            checkEnableTrack.setEnabled ( true );
            checkEnableTrack.setState ( boolEnableTrackSaved );
            onEnableTrack ( true );
        }

        if ( !isFormatSupported(this.formatOld) )
            this.formatOld = null;

        updateFields ( formatOld );
    }

    public void setTrackEnabled ( boolean boolEnable ) {
        boolEnableTrackSaved = boolEnable;
        if ( checkEnableTrack == null )
            return;
        checkEnableTrack.setState ( boolEnable );
        onEnableTrack ( true );
    }

    public boolean isTrackEnabled () {
        boolean    boolEnabled;

        boolEnabled = checkEnableTrack.getState ();
        return ( boolEnabled );
    }

    public Dimension getPreferredSize () {
        Dimension   dim;
        Dimension   dimControl;
        Dimension   dimLabel;

        dim = new Dimension ();
        if ( boolDisplayEnableTrack == true ) {
            dimControl = checkEnableTrack.getPreferredSize();
            dim.width = Math.max ( dim.width, dimControl.width );
            dim.height += dimControl.height + MARGINV;
        }

        dimLabel = labelEncoding.getPreferredSize ();
        nWidthLabel = Math.max ( nWidthLabel, dimLabel.width );
        dimControl = comboEncoding.getPreferredSize ();
        nWidthData = Math.max ( nWidthData, dimControl.width );
        dim.height += Math.max ( dimLabel.height, dimControl.height ) + MARGINV;

        dimLabel = labelSize.getPreferredSize ();
        nWidthLabel = Math.max ( nWidthLabel, dimLabel.width );
        dimControl = controlSize.getPreferredSize ();
        nWidthData = Math.max ( nWidthData, dimControl.width );
        dim.height += Math.max ( dimLabel.height, dimControl.height ) + MARGINV;

        dimLabel = labelFrameRate.getPreferredSize ();
        nWidthLabel = Math.max ( nWidthLabel, dimLabel.width );
        dimControl = comboFrameRate.getPreferredSize ();
        nWidthData = Math.max ( nWidthData, dimControl.width );
        dim.height += Math.max ( dimLabel.height, dimControl.height ) + MARGINV;

        dimLabel = labelExtra.getPreferredSize ();
        nWidthLabel = Math.max ( nWidthLabel, dimLabel.width );
        dimControl = comboExtra.getPreferredSize ();
        nWidthData = Math.max ( nWidthData, dimControl.width );
        dim.height += Math.max ( dimLabel.height, dimControl.height );

        dim.width = Math.max ( dim.width, nWidthLabel + MARGINH + nWidthData );
        return ( dim );
    }

    public void doLayout () {
        Dimension   dimControl;
        Dimension   dimLabel;
        Dimension   dimThis;
        int         nLabelOffsetX;
        int         nDataOffsetX;
        int         nOffsetY;

        getPreferredSize ();
        nOffsetY = 0;
        nLabelOffsetX = 0;
        nDataOffsetX = nWidthLabel + MARGINH;
        dimThis = this.getSize ();

        if ( boolDisplayEnableTrack == true ) {
            dimControl = checkEnableTrack.getPreferredSize();
            checkEnableTrack.setBounds ( nLabelOffsetX, nOffsetY, dimControl.width, dimControl.height );
            nOffsetY += dimControl.height + MARGINV;
        }

        dimLabel = labelEncoding.getPreferredSize ();
        dimControl = comboEncoding.getPreferredSize ();
        labelEncoding.setBounds ( nLabelOffsetX, nOffsetY, nWidthLabel, dimLabel.height );
        comboEncoding.setBounds ( nDataOffsetX, nOffsetY, dimThis.width - nDataOffsetX, dimControl.height );
        nOffsetY += Math.max ( dimLabel.height, dimControl.height ) + MARGINV;

        dimLabel = labelSize.getPreferredSize ();
        dimControl = controlSize.getPreferredSize ();
        labelSize.setBounds ( nLabelOffsetX, nOffsetY, nWidthLabel, dimLabel.height );
        controlSize.setBounds ( nDataOffsetX, nOffsetY, dimThis.width - nDataOffsetX, dimControl.height );
        nOffsetY += Math.max ( dimLabel.height, dimControl.height ) + MARGINV;

        dimLabel = labelFrameRate.getPreferredSize ();
        dimControl = comboFrameRate.getPreferredSize ();
        labelFrameRate.setBounds ( nLabelOffsetX, nOffsetY, nWidthLabel, dimLabel.height );
        comboFrameRate.setBounds ( nDataOffsetX, nOffsetY, dimThis.width - nDataOffsetX, dimControl.height );
        nOffsetY += Math.max ( dimLabel.height, dimControl.height ) + MARGINV;

        dimLabel = labelExtra.getPreferredSize ();
        dimControl = comboExtra.getPreferredSize ();
        labelExtra.setBounds ( nLabelOffsetX, nOffsetY, nWidthLabel, dimLabel.height );
        comboExtra.setBounds ( nDataOffsetX, nOffsetY, dimThis.width - nDataOffsetX, dimControl.height );
        nOffsetY += Math.max ( dimLabel.height, dimControl.height ) + MARGINV;
    }

    private void init () throws Exception {
        int             i;
        String          strValue;
        VideoFormat     format;
        VideoSize       sizeVideo;


        this.setLayout ( null );

        checkEnableTrack = new Checkbox ( JMFI18N.getResource("formatchooser.enabletrack"), true );
        checkEnableTrack.addItemListener ( this );
        if ( boolDisplayEnableTrack == true ) {
            this.add ( checkEnableTrack );
        }

        labelEncoding = new Label ( JMFI18N.getResource("formatchooser.encoding"), Label.RIGHT );
        this.add ( labelEncoding );
        comboEncoding = new Choice ();
        comboEncoding.addItemListener ( this );
        this.add ( comboEncoding );

        labelSize = new Label ( JMFI18N.getResource("formatchooser.videosize"), Label.RIGHT );
        this.add ( labelSize );
//        controlSize = new Choice ();
        if ( formatOld == null ) {
            controlSize = new VideoSizeControl ();
        }
        else {
            sizeVideo = new VideoSize ( formatOld.getSize() );
            controlSize = new VideoSizeControl ( sizeVideo );
        }
//        controlSize.addItemListener ( this );
        controlSize.addActionListener ( this );
        this.add ( controlSize );

        labelFrameRate = new Label ( JMFI18N.getResource("formatchooser.framerate"), Label.RIGHT );
        this.add ( labelFrameRate );
        comboFrameRate = new Choice ();
        comboFrameRate.addItemListener ( this );
        this.add ( comboFrameRate );

        labelExtra = new Label ( "Extra:", Label.RIGHT );
        labelExtra.setVisible ( false );
        this.add ( labelExtra );
        comboExtra = new Choice ();
        comboExtra.setVisible ( false );
        this.add ( comboExtra );

        updateFields ( formatOld );
    }

    private void updateFields ( VideoFormat formatDefault ) {
        int            i;
        int            nSize;
        String         strEncoding;
        String         strEncodingPref = null;
        Object         objectFormat;
        VideoFormat    formatVideo;
        Vector         vectorEncoding = new Vector ();
        boolean        boolEnable;

        boolEnable = comboEncoding.isEnabled ();
        comboEncoding.setEnabled ( false );
        comboEncoding.removeAll ();

        nSize = vectorContSuppFormats.size ();
        for ( i = 0;  i < nSize;  i++ ) {
            objectFormat = vectorContSuppFormats.elementAt ( i );
            if ( !(objectFormat instanceof VideoFormat) )
                continue;
            formatVideo = (VideoFormat) objectFormat;

            strEncoding = formatVideo.getEncoding().toUpperCase();
            if ( strEncodingPref == null )
                strEncodingPref = strEncoding;

            if ( vectorEncoding.contains(strEncoding) )
                continue;
            comboEncoding.addItem ( strEncoding );
            vectorEncoding.addElement ( strEncoding );
        }

        if ( formatDefault != null  ) {
            strEncoding = formatDefault.getEncoding().toUpperCase();
            comboEncoding.select ( strEncoding );
        }
        else if ( strEncodingPref != null ) {
            comboEncoding.select ( strEncodingPref );
        }
        else if ( comboEncoding.getItemCount() > 0 )
            comboEncoding.select ( 0 );

        updateFieldsFromEncoding ( formatDefault );
        comboEncoding.setEnabled ( boolEnable );
    }

    private void updateFieldsFromEncoding ( VideoFormat formatDefault ) {
        int             i;
        int             nSize;
        VideoSize       sizeVideo;
        VideoSize       sizeVideoPref = null;
        boolean         boolVideoSizePref = false;
        Object          objectFormat;
        VideoFormat     formatVideo;
//        Vector          vectorSizes = new Vector ();
        Dimension       formatVideoSize;
        boolean         boolEnable;

        boolEnable = controlSize.isEnabled ();
        controlSize.setEnabled ( false );
        controlSize.removeAll ();

        nSize = vectorContSuppFormats.size ();
        for ( i = 0;  i < nSize;  i++ ) {
            objectFormat = vectorContSuppFormats.elementAt ( i );
            if ( !(objectFormat instanceof VideoFormat) )
                continue;
            formatVideo = (VideoFormat) objectFormat;
            if ( !this.isFormatGoodForEncoding(formatVideo) )
                continue;
            formatVideoSize = formatVideo.getSize();
            if (formatVideoSize == null)
//                sizeVideo = new Dimension(-1, -1);
                sizeVideo = null;
            else
                sizeVideo = new VideoSize ( formatVideoSize );
            if ( boolVideoSizePref == false ) {
                boolVideoSizePref = true;
                sizeVideoPref = sizeVideo;
            }

//            if ( vectorSizes.contains(sizeVideo) )
//                continue;
//            controlSize.addItem ( sizeVideo.toString() );
            controlSize.addItem ( sizeVideo );
//            vectorSizes.addElement ( sizeVideo );
        }

        if ( formatDefault != null  &&  this.isFormatGoodForEncoding(formatDefault) ) {
            formatVideoSize = formatDefault.getSize();
            if (formatVideoSize == null)
                sizeVideo = null;
//                sizeVideo = new Dimension(-1, -1);
            else
                sizeVideo = new VideoSize ( formatVideoSize );
//            controlSize.select ( sizeVideo.toString() );
            controlSize.select ( sizeVideo );
        }
        else if ( boolVideoSizePref == true ) {
//            controlSize.select ( sizeVideoPref.toString() );
            controlSize.select ( sizeVideoPref );
        }
        else if ( controlSize.getItemCount() > 0 )
            controlSize.select ( 0 );

        updateFieldsFromSize ( formatDefault );
        controlSize.setEnabled ( boolEnable );
    }

    private void updateFieldsFromSize ( VideoFormat formatDefault ) {
        int            i;
        int            nSize;
        Float          floatFrameRate;
        Float          floatFrameRatePref = null;
        Object         objectFormat;
        VideoFormat    formatVideo;
        Vector         vectorRates = new Vector ();
        boolean        boolEnable;

        boolEnable = comboFrameRate.isEnabled ();
        comboFrameRate.setEnabled ( false );
        if (customFrameRates == null)
            comboFrameRate.removeAll ();
        else if (comboFrameRate.getItemCount() < 1) {
            // fill in with custom frame rates
            for (i = 0; i < customFrameRates.length; i++)
                comboFrameRate.addItem(Float.toString(customFrameRates[i]));
        }

        nSize = vectorContSuppFormats.size ();
        for ( i = 0;  i < nSize;  i++ ) {
            objectFormat = vectorContSuppFormats.elementAt ( i );
            if ( !(objectFormat instanceof VideoFormat) )
                continue;
            formatVideo = (VideoFormat) objectFormat;
            if ( !this.isFormatGoodForEncoding(formatVideo) )
                continue;
            if ( !this.isFormatGoodForVideoSize(formatVideo) )
                continue;

            if (customFrameRates != null) {
                continue;
            }

            floatFrameRate = new Float ( formatVideo.getFrameRate() );
            if ( floatFrameRatePref == null )
                floatFrameRatePref = floatFrameRate;

            if ( vectorRates.contains(floatFrameRate) )
                continue;
            if ( floatFrameRate.floatValue() == Format.NOT_SPECIFIED )
                comboFrameRate.addItem ( DEFAULT_STRING );
            else
                comboFrameRate.addItem ( floatFrameRate.toString() );
            vectorRates.addElement ( floatFrameRate );
        }

        if ( formatDefault != null  &&  customFrameRates == null
                                    &&  this.isFormatGoodForEncoding(formatDefault)
                                    &&  this.isFormatGoodForVideoSize(formatDefault) ) {
            floatFrameRate = new Float ( formatDefault.getFrameRate() );
            if ( floatFrameRate.floatValue() == Format.NOT_SPECIFIED )
                comboFrameRate.select ( DEFAULT_STRING );
            else
                comboFrameRate.select ( floatFrameRate.toString() );
        }
        else if ( floatFrameRatePref != null ) {
            if ( floatFrameRatePref.floatValue() == Format.NOT_SPECIFIED )
                comboFrameRate.select ( DEFAULT_STRING );
            else
                comboFrameRate.select ( floatFrameRatePref.toString() );
        }
        else if ( comboFrameRate.getItemCount() > 0 )
            comboFrameRate.select ( 0 );

        updateFieldsFromRate ( formatDefault );
        comboFrameRate.setEnabled ( boolEnable );
    }

    private void updateFieldsFromRate ( VideoFormat formatDefault ) {
        int            i;
        int            nSize;
        String         strEncoding;
        Integer        integerBitsPerPixel;
        int            nYuvType;
        String         strYuvType = null;
        Object         objectFormat;
        VideoFormat    formatVideo;
        RGBFormat      formatRGB;
        YUVFormat      formatYUV;
        Vector         vectorExtra = new Vector ();
        boolean        boolRGB = false;
        boolean        boolYUV = false;
        boolean        boolEnable;


        strEncoding = comboEncoding.getSelectedItem ();
        if ( strEncoding == null )
            return;

        if ( strEncoding.equalsIgnoreCase(VideoFormat.RGB) ) {
            labelExtra.setText ( JMFI18N.getResource("formatchooser.bitsperpixel") );
            labelExtra.setVisible ( true );
            comboExtra.setVisible ( true );
            boolRGB = true;
        }
        else if ( strEncoding.equalsIgnoreCase(VideoFormat.YUV) ) {
            labelExtra.setText ( JMFI18N.getResource("formatchooser.yuvtype") );
            labelExtra.setVisible ( true );
            comboExtra.setVisible ( true );
            boolYUV = true;
        }
        else {
            labelExtra.setVisible ( false );
            comboExtra.setVisible ( false );
            return;
        }

        boolEnable = comboExtra.isEnabled ();
        comboExtra.setEnabled ( false );
        comboExtra.removeAll ();

        nSize = vectorContSuppFormats.size ();
        for ( i = 0;  i < nSize;  i++ ) {
            objectFormat = vectorContSuppFormats.elementAt ( i );
            if ( !(objectFormat instanceof VideoFormat) )
                continue;
            formatVideo = (VideoFormat) objectFormat;
            if ( !this.isFormatGoodForEncoding(formatVideo) )
                continue;
            if ( !this.isFormatGoodForVideoSize(formatVideo) )
                continue;
            if ( !this.isFormatGoodForFrameRate(formatVideo) )
                continue;

            if ( boolRGB == true  &&  formatVideo instanceof RGBFormat ) {
                formatRGB = (RGBFormat) formatVideo;
                integerBitsPerPixel = new Integer ( formatRGB.getBitsPerPixel() );
                if ( !(vectorExtra.contains(integerBitsPerPixel)) ) {
                    comboExtra.addItem ( integerBitsPerPixel.toString() );
                    vectorExtra.addElement ( integerBitsPerPixel );
                }
            }
            else if ( boolYUV == true  &&  formatVideo instanceof YUVFormat ) {
                formatYUV = (YUVFormat) formatVideo;
                nYuvType = formatYUV.getYuvType ();
                strYuvType = getYuvType ( nYuvType );
                if ( strYuvType != null  &&  !(vectorExtra.contains(strYuvType)) ) {
                    comboExtra.addItem ( strYuvType );
                    vectorExtra.addElement ( strYuvType );
                }
            }

        }
        if ( formatDefault != null  &&  this.isFormatGoodForEncoding(formatDefault)
                                    &&  this.isFormatGoodForVideoSize(formatDefault)
                                    &&  this.isFormatGoodForFrameRate(formatDefault) ) {
            if ( boolRGB == true  &&  formatDefault instanceof RGBFormat ) {
                formatRGB = (RGBFormat) formatDefault;
                integerBitsPerPixel = new Integer ( formatRGB.getBitsPerPixel() );
                comboExtra.select ( integerBitsPerPixel.toString() );
            }
            else if ( boolYUV == true  &&  formatDefault instanceof YUVFormat ) {
                formatYUV = (YUVFormat) formatDefault;
                nYuvType = formatYUV.getYuvType ();
                strYuvType = getYuvType ( nYuvType );
                if ( strYuvType != null )
                    comboExtra.select ( strYuvType );
            }
            else if ( comboExtra.getItemCount() > 0 )
                comboExtra.select ( 0 );
        }
        else if ( comboExtra.getItemCount() > 0 )
            comboExtra.select ( 0 );

        comboExtra.setEnabled ( boolEnable );
    }

    private boolean isFormatGoodForEncoding ( VideoFormat format ) {
        String     strEncoding;
        boolean    boolResult = false;

        strEncoding = comboEncoding.getSelectedItem ();
        if ( strEncoding != null ) {
            boolResult = format.getEncoding().equalsIgnoreCase(strEncoding);
        }
        return ( boolResult );
    }

    private boolean isFormatGoodForVideoSize ( VideoFormat format ) {
        VideoSize  sizeVideo;
        boolean    boolResult = false;
        Dimension  formatVideoSize;


        sizeVideo = controlSize.getVideoSize ();
        formatVideoSize = format.getSize();
        if ( formatVideoSize == null )
            boolResult = true;
        else
            boolResult = sizeVideo.equals(formatVideoSize);

        return ( boolResult );
    }

    private boolean isFormatGoodForFrameRate ( VideoFormat format ) {
        String     strFrameRate;
        float      fFrameRate1;
        float      fFrameRate2;
        boolean    boolResult = false;

        // If custom frame rates (VFW capture), always true
        if (customFrameRates != null)
            return true;

        strFrameRate = comboFrameRate.getSelectedItem ();
        if ( strFrameRate.equals(DEFAULT_STRING) )
            return true;

        fFrameRate2 = format.getFrameRate();
        if ( fFrameRate2 == Format.NOT_SPECIFIED )
            return true;

        if ( strFrameRate != null ) {
            fFrameRate1 = Float.valueOf(strFrameRate).floatValue();
            boolResult = (fFrameRate1 == fFrameRate2);
        }
        return ( boolResult );
    }


    private boolean isFormatSupported ( VideoFormat format ) {
        int            i;
        int            nCount;
        VideoFormat    formatVideo;
        boolean        boolSupported = false;

        if ( format == null )
            return ( boolSupported );

        nCount = vectorContSuppFormats.size ();
        for ( i = 0;  i < nCount  &&  boolSupported == false;  i++ ) {
            formatVideo = (VideoFormat) vectorContSuppFormats.elementAt ( i );
            if ( formatVideo.matches(format) )
                boolSupported = true;
        }
        return ( boolSupported );
    }

    public void actionPerformed ( ActionEvent event ) {
        if ( event.getActionCommand().equals(VideoSizeControl.ACTION_SIZE_CHANGED) ) {
            updateFieldsFromSize ( formatOld );
        }
    }

    public void itemStateChanged ( ItemEvent event ) {
        Object     objectSource;

        objectSource = event.getSource ();
        if ( objectSource == checkEnableTrack ) {
            boolEnableTrackSaved = checkEnableTrack.getState ();
            onEnableTrack ( true );
        }
        else if ( objectSource == comboEncoding ) {
            updateFieldsFromEncoding ( formatOld );
        }
        else if ( objectSource == controlSize ) {
            updateFieldsFromSize ( formatOld );
        }
        else if ( objectSource == comboFrameRate ) {
            updateFieldsFromRate ( formatOld );
        }
    }

    private void onEnableTrack ( boolean notifyListener ) {
        boolean        boolEnable;
        ActionEvent    event;

        boolEnable = checkEnableTrack.getState ();
        enableControls ( boolEnable  &&  this.isEnabled() );

        if ( notifyListener == true  &&  listenerEnableTrack != null ) {
            if ( boolEnable == true )
                event = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, ACTION_TRACK_ENABLED );
            else
                event = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, ACTION_TRACK_DISABLED );
            listenerEnableTrack.actionPerformed ( event );
        }
    }

    private void enableControls ( boolean boolEnable ) {
        labelEncoding.setEnabled ( boolEnable );
        comboEncoding.setEnabled ( boolEnable );
        labelSize.setEnabled ( boolEnable );
        controlSize.setEnabled ( boolEnable );
        labelFrameRate.setEnabled ( boolEnable );
        comboFrameRate.setEnabled ( boolEnable );
        labelExtra.setEnabled ( boolEnable );
        comboExtra.setEnabled ( boolEnable );
    }

    private String getYuvType ( int nType ) {
        String    strType = null;

        if ( (nType & YUVFormat.YUV_420) == YUVFormat.YUV_420 )
            strType = JMFI18N.getResource("formatchooser.yuv.4:2:0");
        else if ( (nType & YUVFormat.YUV_422) == YUVFormat.YUV_422 )
            strType = JMFI18N.getResource("formatchooser.yuv.4:2:2");
        else if ( (nType & YUVFormat.YUV_YUYV) == YUVFormat.YUV_YUYV )
            strType = JMFI18N.getResource("formatchooser.yuv.YUYV");
        else if ( (nType & YUVFormat.YUV_111) == YUVFormat.YUV_111 )
            strType = JMFI18N.getResource("formatchooser.yuv.1:1:1");
        else if ( (nType & YUVFormat.YUV_411) == YUVFormat.YUV_411 )
            strType = JMFI18N.getResource("formatchooser.yuv.4:1:1");
        else if ( (nType & YUVFormat.YUV_YVU9) == YUVFormat.YUV_YVU9 )
            strType = JMFI18N.getResource("formatchooser.yuv.YVU9");
        else
            strType = null;

        return ( strType );
    }

}


class VideoSize extends Dimension {

    public VideoSize () {
        super ();
    }

    public VideoSize ( int nWidth, int nHeight ) {
        super ( nWidth, nHeight );
    }

    public VideoSize ( Dimension dim ) {
        super ( dim );
    }

    public boolean equals ( Dimension dim ) {
        boolean     boolResult = true;

        if ( dim == null )
            boolResult = false;
        if ( boolResult == true )
            boolResult = (this.width == dim.width);
        if ( boolResult == true )
            boolResult = (this.height == dim.height);
        return ( boolResult );
    }

    public String toString () {
        return ( "" + this.width + " x " + this.height );
    }

}


class VideoSizeControl extends Panel implements ItemListener, ComponentListener {

    private Choice          comboSize;
    private Panel           panelCustom;
    private TextField       textWidth;
    private TextField       textHeight;
    private Label           labelX;
    private Hashtable       htSizes = new Hashtable ();
    private VideoSize       sizeVideoDefault = null;

    private ActionListener  listener;

    public static final String  ACTION_SIZE_CHANGED = "Size Changed";


    static final String     CUSTOM_STRING = JMFI18N.getResource("formatchooser.custom");

    public VideoSizeControl () {
        this ( null );
    }

    public VideoSizeControl ( VideoSize sizeVideoDefault ) {
        super ();

        this.sizeVideoDefault = sizeVideoDefault;
        init ();
    }
/*
    public Dimension getPreferredSize () {
        Dimension   dim;
        Dimension   dimFieldWidth;
        Dimension   dimFieldHeight;

        dim = labelX.getPreferredSize();
        dimFieldWidth = textWidth.getPreferredSize();
        dimFieldHeight = textHeight.getPreferredSize();

        dim.height = Math.max ( dim.height, dimFieldWidth.height );
        dim.height = Math.max ( dim.height, dimFieldHeight.height );
        dim.width += 2 * Math.max ( dimFieldWidth.width, dimFieldHeight.width );

        return ( dim );
    }
*/
    public void setEnabled ( boolean boolEnable ) {
        super.setEnabled ( boolEnable );

        comboSize.setEnabled ( boolEnable );
        textWidth.setEnabled ( boolEnable );
        textHeight.setEnabled ( boolEnable );
        labelX.setEnabled ( boolEnable );

        if ( boolEnable == true )
            updateFields ();
    }

    public void addActionListener ( ActionListener listener ) {
        this.listener = listener;
    }

    public VideoSize getVideoSize () {
        String      strItem;
        VideoSize   sizeVideo;
        Object      objSize;
        int         nWidth;
        int         nHeight;

        strItem = comboSize.getSelectedItem ();
        objSize = htSizes.get ( strItem );
        if ( objSize == null  ||  !(objSize instanceof VideoSize)
                                ||  strItem.equals(CUSTOM_STRING) ) {
            try {
                nWidth = Integer.valueOf(textWidth.getText()).intValue();
            }
            catch ( Exception exception ) {
                nWidth = 0;
            }
            try {
                nHeight = Integer.valueOf(textHeight.getText()).intValue();
            }
            catch ( Exception exception ) {
                nHeight = 0;
            }
            sizeVideo = new VideoSize ( nWidth, nHeight );
        }
        else {
            sizeVideo = (VideoSize) objSize;
        }
        return ( sizeVideo );
    }

    public void addItem ( VideoSize sizeVideo ) {
        String  strItem;

        if ( sizeVideo == null ) {
            sizeVideo = new VideoSize ( -1, -1 );
            strItem = CUSTOM_STRING;
        }
        else {
            strItem = sizeVideo.toString ();
        }

        if ( htSizes.containsKey(strItem) )
            return;

        comboSize.addItem ( strItem );
        htSizes.put ( strItem, sizeVideo );

        if ( comboSize.getItemCount() == 1 )
            updateFields ();
    }

    public void removeAll () {
        comboSize.removeAll ();
        htSizes = new Hashtable ();
        updateFields ();
    }

    public void select ( VideoSize sizeVideo ) {
        if ( sizeVideo == null )
            comboSize.select ( CUSTOM_STRING );
        else
            comboSize.select ( sizeVideo.toString() );
        updateFields ();
    }

    public void select ( int nIndex ) {
        comboSize.select ( nIndex );
        updateFields ();
    }

    public int getItemCount () {
        return ( comboSize.getItemCount() );
    }

    private void init () {
        Label   label;

        setLayout ( new GridLayout(0,1,4,4) );

        comboSize = new Choice ();
        comboSize.addItem ( CUSTOM_STRING );
        comboSize.addItemListener ( this );
        this.add ( comboSize );

        panelCustom = new Panel (null);
        panelCustom.addComponentListener ( this );
        this.add ( panelCustom );

        if ( sizeVideoDefault == null )
            textWidth = new TextField ( 3 );
        else
            textWidth = new TextField ( "" + sizeVideoDefault.width, 3 );
        panelCustom.add ( textWidth, BorderLayout.CENTER );

        labelX = new Label ( "x", Label.CENTER );
        panelCustom.add ( labelX, BorderLayout.WEST );
        if ( sizeVideoDefault == null )
            textHeight = new TextField ( 3 );
        else
            textHeight = new TextField ( "" + sizeVideoDefault.height, 3 );
        panelCustom.add ( textHeight, BorderLayout.CENTER );

        updateFields ();
    }

    private void updateFields () {
        String      strItem;
        boolean     boolEnable;
        VideoSize   sizeVideo;


        strItem = comboSize.getSelectedItem ();
        if ( strItem == null  ||  strItem.equals(CUSTOM_STRING) ) {
            boolEnable = true;
        }
        else {
            sizeVideo = (VideoSize) htSizes.get ( strItem );
            textWidth.setText ( "" + sizeVideo.width );
            textHeight.setText ( "" + sizeVideo.height );
            boolEnable = false;
        }

        textWidth.setEnabled ( boolEnable );
        textHeight.setEnabled ( boolEnable );
        labelX.setEnabled ( boolEnable );
    }

    private void resizeCustomFields () {
        Dimension   dimPanel;
        Dimension   dimLabelX;
        int         nWidth;

        dimPanel = panelCustom.getSize();
        dimLabelX = labelX.getPreferredSize();
        nWidth = (dimPanel.width - dimLabelX.width) / 2;
        textWidth.setBounds ( 0, 0, nWidth, dimPanel.height );
        labelX.setBounds ( nWidth, 0, dimLabelX.width, dimPanel.height );
        textHeight.setBounds ( nWidth + dimLabelX.width, 0, nWidth, dimPanel.height );
    }

    public void itemStateChanged ( ItemEvent event ) {
        Object          objectSource;
        ActionEvent     eventAction;

        objectSource = event.getSource ();
        if ( objectSource != comboSize )
            return;
        updateFields ();
        if ( listener != null ) {
            eventAction = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, ACTION_SIZE_CHANGED );
            listener.actionPerformed ( eventAction );
        }
    }

    public void componentResized ( ComponentEvent event ) {
        resizeCustomFields ();
    }

    public void componentMoved ( ComponentEvent event ) {
    }

    public void componentShown ( ComponentEvent event ) {
    }

    public void componentHidden ( ComponentEvent event ) {
    }

}


