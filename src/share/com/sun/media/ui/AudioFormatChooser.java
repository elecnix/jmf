/*
 * @(#)AudioFormatChooser.java	1.18 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.ui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.*;
import javax.media.format.*;

import com.sun.media.util.JMFI18N;


public class AudioFormatChooser extends Panel implements ItemListener {

    public static final String    ACTION_TRACK_ENABLED = "ACTION_AUDIO_TRACK_ENABLED";
    public static final String    ACTION_TRACK_DISABLED = "ACTION_AUDIO_TRACK_DISABLED";

    private AudioFormat     formatOld;
    private Format          arrSupportedFormats [] = null;
    private Vector          vectorContSuppFormats = new Vector ();
    private boolean         boolDisplayEnableTrack;
    private ActionListener  listenerEnableTrack;
    private boolean         boolEnableTrackSaved = true;

    private Checkbox        checkEnableTrack;
    private Label           labelEncoding;
    private Choice          comboEncoding;
    private Label           labelSampleRate;
    private Choice          comboSampleRate;
    private Label           labelHz;
    private Label           labelBitsPerSample;
    private CheckboxGroup   groupBitsPerSample;
    private Checkbox        checkBits8;
    private Checkbox        checkBits16;
    private Label           labelChannels;
    private CheckboxGroup   groupChannels;
    private Checkbox        checkMono;
    private Checkbox        checkStereo;
    private Label           labelEndian;
    private CheckboxGroup   groupEndian;
    private Checkbox        checkEndianBig;
    private Checkbox        checkEndianLittle;
    private Checkbox        checkSigned;

    private boolean         boolEnable8 = false;
    private boolean         boolEnable16 = false;
    private boolean         boolEnableMono = false;
    private boolean         boolEnableStereo = false;
    private boolean         boolEnableEndianBig = false;
    private boolean         boolEnableEndianLittle = false;
    private boolean         boolEnableSigned = false;



    public AudioFormatChooser ( Format arrFormats[], AudioFormat formatDefault ) {
	    this ( arrFormats, formatDefault, false, null );
    }

    public AudioFormatChooser ( Format arrFormats[], AudioFormat formatDefault,
                                boolean boolDisplayEnableTrack,
                                ActionListener listenerEnableTrack ) {
        int    i;
        int    nCount;

        this.arrSupportedFormats = arrFormats;
        this.boolDisplayEnableTrack = boolDisplayEnableTrack;
        this.listenerEnableTrack = listenerEnableTrack;

        nCount = arrSupportedFormats.length;
        for ( i = 0;  i < nCount;  i++ ) {
            if ( arrSupportedFormats[i] instanceof AudioFormat )
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
        int            i;
        int            nSize;
        String         strEncoding;
        double         dSampleRate;
        String         strSampleRate;
        int            nBits;
        int            nChannels;
        int            nEndian;
        int            nSigned;
        Format         formatResult = null;
        AudioFormat    formatAudioNew;
        AudioFormat    formatAudio;
        Object         objectFormat;



        strEncoding = comboEncoding.getSelectedItem ();
        strSampleRate = comboSampleRate.getSelectedItem ();
        dSampleRate = Double.valueOf(strSampleRate).doubleValue();

        if ( checkBits8.getState() == true  &&  checkBits8.isEnabled() == true )
            nBits = 8;
        else if ( checkBits16.getState() == true  &&  checkBits16.isEnabled() == true )
            nBits = 16;
        else
            nBits = Format.NOT_SPECIFIED;

        if ( checkMono.getState() == true  &&  checkMono.isEnabled() == true )
            nChannels = 1;
        else if ( checkStereo.getState() == true  &&  checkStereo.isEnabled() == true )
            nChannels = 2;
        else
            nChannels = Format.NOT_SPECIFIED;

        if ( checkEndianBig.getState() == true  &&  checkEndianBig.isEnabled() == true )
            nEndian = AudioFormat.BIG_ENDIAN;
        else if ( checkEndianLittle.getState() == true  &&  checkEndianLittle.isEnabled() == true )
            nEndian = AudioFormat.LITTLE_ENDIAN;
        else
            nEndian = Format.NOT_SPECIFIED;

        if ( checkSigned.getState() == true )
            nSigned = AudioFormat.SIGNED;
        else
            nSigned = AudioFormat.UNSIGNED;

        formatAudioNew = new AudioFormat ( strEncoding, dSampleRate, nBits, nChannels, nEndian, nSigned );

        nSize = vectorContSuppFormats.size ();
        for ( i = 0;  i < nSize  &&  formatResult == null;  i++ ) {
            objectFormat = vectorContSuppFormats.elementAt ( i );
            if ( !(objectFormat instanceof AudioFormat) )
                continue;
            formatAudio = (AudioFormat) objectFormat;

            if ( !this.isFormatGoodForEncoding(formatAudio) )
                continue;
            if ( !this.isFormatGoodForSampleRate(formatAudio) )
                continue;
            if ( !this.isFormatGoodForBitSize(formatAudio) )
                continue;
            if ( !this.isFormatGoodForChannels(formatAudio) )
                continue;
            if ( !this.isFormatGoodForEndian(formatAudio) )
                continue;
            if ( !this.isFormatGoodForSigned(formatAudio) )
                continue;

            if ( formatAudio.matches(formatAudioNew) )
                formatResult = formatAudio.intersects ( formatAudioNew );
        }

        return ( formatResult );
    }

    public void setCurrentFormat ( AudioFormat formatDefault ) {
        if ( isFormatSupported(formatDefault) )
            this.formatOld = formatDefault;
        updateFields ( formatOld );
    }

    public void setSupportedFormats ( Format arrFormats[], AudioFormat formatDefault ) {
        int    i;
        int    nCount;

        this.arrSupportedFormats = arrFormats;

        nCount = arrSupportedFormats.length;
        vectorContSuppFormats.removeAllElements ();
        for ( i = 0;  i < nCount;  i++ ) {
            if ( arrSupportedFormats[i] instanceof AudioFormat )
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

    private void init () throws Exception {
        Panel         panel;
        Panel         panelGroup;
        Panel         panelLabel;
        Panel         panelData;
        Panel         panelEntry;
        Label         label;


        this.setLayout ( new BorderLayout(6,6) );
        panel = this;

        checkEnableTrack = new Checkbox ( JMFI18N.getResource("formatchooser.enabletrack"), true );
        checkEnableTrack.addItemListener ( this );
        if ( boolDisplayEnableTrack == true ) {
            panelGroup = new Panel ( new BorderLayout() );
            panel.add ( panelGroup, BorderLayout.NORTH );
            panelGroup.add ( checkEnableTrack, BorderLayout.WEST );
        }

        panelGroup = new Panel ( new BorderLayout(6,6) );
        panel.add ( panelGroup, BorderLayout.CENTER );
        panel = panelGroup;
        panelGroup = new Panel ( new BorderLayout() );
        panel.add ( panelGroup, BorderLayout.NORTH );

        panelLabel = new Panel ( new GridLayout(0,1,6,6) );
        panelGroup.add ( panelLabel, BorderLayout.WEST );
        panelData = new Panel ( new GridLayout(0,1,6,6) );
        panelGroup.add ( panelData, BorderLayout.CENTER );

        labelEncoding = new Label ( JMFI18N.getResource("formatchooser.encoding"), Label.LEFT );
        panelLabel.add ( labelEncoding );
        comboEncoding = new Choice ();
        comboEncoding.addItemListener ( this );
        panelData.add ( comboEncoding );

        labelSampleRate = new Label ( JMFI18N.getResource("formatchooser.samplerate"), Label.LEFT );
        panelLabel.add ( labelSampleRate );
        panelEntry = new Panel ( new BorderLayout(6,6) );
        panelData.add ( panelEntry );
        comboSampleRate = new Choice ();
        comboSampleRate.addItemListener ( this );
        panelEntry.add ( comboSampleRate, BorderLayout.CENTER );
        labelHz = new Label ( JMFI18N.getResource("formatchooser.hz") );
        panelEntry.add ( labelHz, BorderLayout.EAST );

        labelBitsPerSample = new Label ( JMFI18N.getResource("formatchooser.bitspersample"), Label.LEFT );
        panelLabel.add ( labelBitsPerSample );
        panelEntry = new Panel ( new GridLayout(1,0,6,6) );
        panelData.add ( panelEntry );
        groupBitsPerSample = new CheckboxGroup ();
        checkBits8 = new Checkbox ( JMFI18N.getResource("formatchooser.8bit"), groupBitsPerSample, false );
        checkBits8.addItemListener ( this );
        panelEntry.add ( checkBits8 );
        checkBits16 = new Checkbox ( JMFI18N.getResource("formatchooser.16bit"), groupBitsPerSample, false );
        checkBits16.addItemListener ( this );
        panelEntry.add ( checkBits16 );

        labelChannels = new Label ( JMFI18N.getResource("formatchooser.channels"), Label.LEFT );
        panelLabel.add ( labelChannels );
        panelEntry = new Panel ( new GridLayout(1,0,6,6) );
        panelData.add ( panelEntry );
        groupChannels = new CheckboxGroup ();
        checkMono = new Checkbox ( JMFI18N.getResource("formatchooser.mono"), groupChannels, false );
        checkMono.addItemListener ( this );
        panelEntry.add ( checkMono );
        checkStereo = new Checkbox ( JMFI18N.getResource("formatchooser.stereo"), groupChannels, false );
        checkStereo.addItemListener ( this );
        panelEntry.add ( checkStereo );

        labelEndian = new Label ( JMFI18N.getResource("formatchooser.endian"), Label.LEFT );
        panelLabel.add ( labelEndian );
        panelEntry = new Panel ( new GridLayout(1,0,6,6) );
        panelData.add ( panelEntry );
        groupEndian = new CheckboxGroup ();
        checkEndianBig = new Checkbox ( JMFI18N.getResource("formatchooser.endian.big"), groupEndian, false );
        checkEndianBig.addItemListener ( this );
        panelEntry.add ( checkEndianBig );
        checkEndianLittle = new Checkbox ( JMFI18N.getResource("formatchooser.endian.little"), groupEndian, false );
        checkEndianLittle.addItemListener ( this );
        panelEntry.add ( checkEndianLittle );

        panelGroup = new Panel ( new BorderLayout(6,6) );
        panel.add ( panelGroup, BorderLayout.CENTER );
        panel = panelGroup;
        panelGroup = new Panel ( new BorderLayout() );
        panel.add ( panelGroup, BorderLayout.NORTH );

        checkSigned = new Checkbox ( JMFI18N.getResource("formatchooser.signed"), true );
        checkSigned.addItemListener ( this );
        panelGroup.add ( checkSigned, BorderLayout.WEST );

        updateFields ( formatOld );
    }


    private void updateFields ( AudioFormat formatDefault ) {
        int            i;
        int            nSize;
        String         strEncoding;
        String         strEncodingPref = null;
        Object         objectFormat;
        AudioFormat    formatAudio;
        Vector         vectorEncoding = new Vector ();
        boolean        boolEnable;

        boolEnable = comboEncoding.isEnabled ();
        comboEncoding.setEnabled ( false );
        comboEncoding.removeAll ();

        nSize = vectorContSuppFormats.size ();
        for ( i = 0;  i < nSize;  i++ ) {
            objectFormat = vectorContSuppFormats.elementAt ( i );
            if ( !(objectFormat instanceof AudioFormat) )
                continue;
            formatAudio = (AudioFormat) objectFormat;

            strEncoding = formatAudio.getEncoding().toUpperCase();
            if ( vectorEncoding.contains(strEncoding) )
                continue;
            comboEncoding.addItem ( strEncoding );
            vectorEncoding.addElement ( strEncoding );
            if ( strEncodingPref == null )
                strEncodingPref = strEncoding;
        }

        if ( formatDefault != null  ) {
            strEncoding = formatDefault.getEncoding ();
            comboEncoding.select ( strEncoding );
        }
        else if ( strEncodingPref != null )
            comboEncoding.select ( strEncodingPref );
        else if ( comboEncoding.getItemCount() > 0 )
            comboEncoding.select ( 0 );

        updateFieldsFromEncoding ( formatDefault );
        comboEncoding.setEnabled ( boolEnable );
    }

    private void updateFieldsFromEncoding ( AudioFormat formatDefault ) {
        int            i;
        int            nSize;
        double         dSampleRate;
        String         strSampleRate;
        String         strSampleRatePref = null;
        Object         objectFormat;
        AudioFormat    formatAudio;
        Vector         vectorRates = new Vector ();
        boolean        boolEnable;

        boolEnable = comboSampleRate.isEnabled ();
        comboSampleRate.setEnabled ( false );
        comboSampleRate.removeAll ();

        nSize = vectorContSuppFormats.size ();
        for ( i = 0;  i < nSize;  i++ ) {
            objectFormat = vectorContSuppFormats.elementAt ( i );
            if ( !(objectFormat instanceof AudioFormat) )
                continue;
            formatAudio = (AudioFormat) objectFormat;
            if ( !isFormatGoodForEncoding(formatAudio) )
                continue;

            dSampleRate = formatAudio.getSampleRate ();
            strSampleRate = Double.toString ( dSampleRate );
            if ( vectorRates.contains(strSampleRate) )
                continue;
            comboSampleRate.addItem ( strSampleRate );
            vectorRates.addElement ( strSampleRate );
            if ( strSampleRatePref == null )
                strSampleRatePref = strSampleRate;
        }
        if ( formatDefault != null  &&  isFormatGoodForEncoding(formatDefault) )
            comboSampleRate.select ( Double.toString ( formatDefault.getSampleRate() ) );
        else if ( strSampleRatePref != null )
            comboEncoding.select ( strSampleRatePref );
        else if ( comboSampleRate.getItemCount() > 0 )
            comboSampleRate.select ( 0 );

        updateFieldsFromRate ( formatDefault );
        comboSampleRate.setEnabled ( boolEnable );
    }

    private void updateFieldsFromRate ( AudioFormat formatDefault ) {
        int            i;
        int            nSize;
        Object         objectFormat;
        AudioFormat    formatAudio;
        int            nBits;
        int            nBitsPref = Format.NOT_SPECIFIED;


        boolEnable8 = false;
        boolEnable16 = false;
        nSize = vectorContSuppFormats.size ();
        for ( i = 0;  i < nSize;  i++ ) {
            objectFormat = vectorContSuppFormats.elementAt ( i );
            if ( !(objectFormat instanceof AudioFormat) )
                continue;
            formatAudio = (AudioFormat) objectFormat;
            if ( !this.isFormatGoodForEncoding(formatAudio) )
                continue;
            if ( !this.isFormatGoodForSampleRate(formatAudio) )
                continue;

            nBits = formatAudio.getSampleSizeInBits ();
            if ( nBitsPref == Format.NOT_SPECIFIED )
                nBitsPref = nBits;

            if ( nBits == Format.NOT_SPECIFIED ) {
                boolEnable8 = true;
                boolEnable16 = true;
            }
            else if ( nBits == 8 )
                boolEnable8 = true;
            else if ( nBits == 16 )
                boolEnable16 = true;

        }
        checkBits8.setEnabled ( boolEnable8 );
        checkBits16.setEnabled ( boolEnable16 );

        if ( formatDefault != null
                &&  this.isFormatGoodForEncoding(formatDefault)
                &&  this.isFormatGoodForSampleRate(formatDefault) ) {
            nBits = formatDefault.getSampleSizeInBits ();
            if ( nBits == 8 )
                checkBits8.setState ( true );
            else if ( nBits == 16 )
                checkBits16.setState ( true );
        }
        else if ( nBitsPref != Format.NOT_SPECIFIED ) {
            if ( nBitsPref == 8 )
                checkBits8.setState ( true );
            else if ( nBitsPref == 16 )
                checkBits16.setState ( true );
        }
        else {
            if ( boolEnable8 == true )
                checkBits8.setState ( true );
            else
                checkBits16.setState ( true );
        }

        updateFieldsFromBits ( formatDefault );
    }

    private void updateFieldsFromBits ( AudioFormat formatDefault ) {
        int            i;
        int            nSize;
        Object         objectFormat;
        AudioFormat    formatAudio;
        int            nChannels;
        int            nChannelsPref = Format.NOT_SPECIFIED;


        boolEnableMono = false;
        boolEnableStereo = false;

        nSize = vectorContSuppFormats.size ();
        for ( i = 0;  i < nSize;  i++ ) {
            objectFormat = vectorContSuppFormats.elementAt ( i );
            if ( !(objectFormat instanceof AudioFormat) )
                continue;
            formatAudio = (AudioFormat) objectFormat;
            if ( !this.isFormatGoodForEncoding(formatAudio) )
                continue;
            if ( !this.isFormatGoodForSampleRate(formatAudio) )
                continue;
            if ( !this.isFormatGoodForBitSize(formatAudio) )
                continue;

            nChannels = formatAudio.getChannels ();
            if ( nChannelsPref == Format.NOT_SPECIFIED )
                nChannelsPref = nChannels;

            if ( nChannels == Format.NOT_SPECIFIED ) {
                boolEnableMono = true;
                boolEnableStereo = true;
            }
            else if ( nChannels == 1 )
                boolEnableMono = true;
            else
                boolEnableStereo = true;

        }
        checkMono.setEnabled ( boolEnableMono );
        checkStereo.setEnabled ( boolEnableStereo );

        if ( formatDefault != null
                &&  this.isFormatGoodForEncoding(formatDefault)
                &&  this.isFormatGoodForSampleRate(formatDefault)
                &&  this.isFormatGoodForBitSize(formatDefault) ) {
            nChannels = formatDefault.getChannels ();
            if ( nChannels == 1 )
                checkMono.setState ( true );
            else
                checkStereo.setState ( true );
        }
        else if ( nChannelsPref != Format.NOT_SPECIFIED ) {
            if ( nChannelsPref == 1 )
                checkMono.setState ( true );
            else
                checkStereo.setState ( true );
        }
        else {
            if ( boolEnableMono == true )
                checkMono.setState ( true );
            else
                checkStereo.setState ( true );
        }

        updateFieldsFromChannels ( formatDefault );
    }

    private void updateFieldsFromChannels ( AudioFormat formatDefault ) {
        int            i;
        int            nSize;
        Object         objectFormat;
        AudioFormat    formatAudio;
        int            nEndian;
        int            nEndianPref = Format.NOT_SPECIFIED;


        boolEnableEndianBig = false;
        boolEnableEndianLittle = false;

        nSize = vectorContSuppFormats.size ();
        for ( i = 0;  i < nSize;  i++ ) {
            objectFormat = vectorContSuppFormats.elementAt ( i );
            if ( !(objectFormat instanceof AudioFormat) )
                continue;
            formatAudio = (AudioFormat) objectFormat;
            if ( !this.isFormatGoodForEncoding(formatAudio) )
                continue;
            if ( !this.isFormatGoodForSampleRate(formatAudio) )
                continue;
            if ( !this.isFormatGoodForBitSize(formatAudio) )
                continue;
            if ( !this.isFormatGoodForChannels(formatAudio) )
                continue;

            nEndian = formatAudio.getEndian ();
            if ( nEndianPref == Format.NOT_SPECIFIED )
                nEndianPref = nEndian;

            if ( nEndian == Format.NOT_SPECIFIED ) {
                boolEnableEndianBig = true;
                boolEnableEndianLittle = true;
            }
            else if ( nEndian == AudioFormat.BIG_ENDIAN )
                boolEnableEndianBig = true;
            else
                boolEnableEndianLittle = true;

        }

        checkEndianBig.setEnabled ( boolEnableEndianBig );
        checkEndianLittle.setEnabled ( boolEnableEndianLittle );

        if ( formatDefault != null
                &&  this.isFormatGoodForEncoding(formatDefault)
                &&  this.isFormatGoodForSampleRate(formatDefault)
                &&  this.isFormatGoodForBitSize(formatDefault)
                &&  this.isFormatGoodForChannels(formatDefault) ) {
            nEndian = formatDefault.getEndian ();
            if ( nEndian == AudioFormat.BIG_ENDIAN )
                checkEndianBig.setState ( true );
            else
                checkEndianLittle.setState ( true );
        }
        else if ( nEndianPref != Format.NOT_SPECIFIED ) {
            if ( nEndianPref == AudioFormat.BIG_ENDIAN )
                checkEndianBig.setState ( true );
            else
                checkEndianLittle.setState ( true );
        }
        else {
            if ( boolEnableEndianBig == true )
                checkEndianBig.setState ( true );
            else
                checkEndianLittle.setState ( true );
        }

        if ( checkBits16.getState() != true ) {
            // endian doesn't matter
            boolEnableEndianBig = false;
            boolEnableEndianLittle = false;
            checkEndianBig.setEnabled ( boolEnableEndianBig );
            checkEndianLittle.setEnabled ( boolEnableEndianLittle );
        }

        updateFieldsFromEndian ( formatDefault );
    }

    private void updateFieldsFromEndian ( AudioFormat formatDefault ) {
        int            i;
        int            nSize;
        Object         objectFormat;
        AudioFormat    formatAudio;
        int            nSigned;
        int            nSignedPref = Format.NOT_SPECIFIED;
        boolean        boolSigned;
        boolean        boolUnsigned;


        boolSigned = false;
        boolUnsigned = false;

        nSize = vectorContSuppFormats.size ();
        for ( i = 0;  i < nSize;  i++ ) {
            objectFormat = vectorContSuppFormats.elementAt ( i );
            if ( !(objectFormat instanceof AudioFormat) )
                continue;
            formatAudio = (AudioFormat) objectFormat;
            if ( !this.isFormatGoodForEncoding(formatAudio) )
                continue;
            if ( !this.isFormatGoodForSampleRate(formatAudio) )
                continue;
            if ( !this.isFormatGoodForBitSize(formatAudio) )
                continue;
            if ( !this.isFormatGoodForChannels(formatAudio) )
                continue;
            if ( !this.isFormatGoodForEndian(formatAudio) )
                continue;

            nSigned = formatAudio.getSigned ();
            if ( nSignedPref == Format.NOT_SPECIFIED )
                nSignedPref = nSigned;

            if ( nSigned == Format.NOT_SPECIFIED ) {
                boolSigned = true;
                boolUnsigned = true;
            }
            else if ( nSigned == AudioFormat.SIGNED )
                boolSigned = true;
            else
                boolUnsigned = true;

        }
        boolEnableSigned = boolSigned && boolUnsigned;
        checkSigned.setEnabled ( boolEnableSigned );

        if ( formatDefault != null
                &&  this.isFormatGoodForEncoding(formatDefault)
                &&  this.isFormatGoodForSampleRate(formatDefault)
                &&  this.isFormatGoodForBitSize(formatDefault)
                &&  this.isFormatGoodForChannels(formatDefault)
                &&  this.isFormatGoodForEndian(formatDefault) ) {
            nSigned = formatDefault.getSigned ();
            if ( nSigned == AudioFormat.SIGNED )
                checkSigned.setState ( true );
            else
                checkSigned.setState ( false );
        }
        else if ( nSignedPref != Format.NOT_SPECIFIED ) {
            if ( nSignedPref == AudioFormat.SIGNED )
                checkSigned.setState ( true );
            else
                checkSigned.setState ( false );
        }
        else {
            if ( boolSigned == true )
                checkSigned.setState ( true );
            else
                checkSigned.setState ( false );
        }

        updateFieldsFromSigned ( formatDefault );
    }

    private void updateFieldsFromSigned ( AudioFormat formatDefault ) {
    }

    private boolean isFormatGoodForEncoding ( AudioFormat format ) {
        String     strEncoding;
        boolean    boolResult = false;

        strEncoding = comboEncoding.getSelectedItem ();
        if ( strEncoding != null ) {
            boolResult = format.getEncoding().equalsIgnoreCase(strEncoding);
        }
        return ( boolResult );
    }

    private boolean isFormatGoodForSampleRate ( AudioFormat format ) {
        double     dSampleRate;
        String     strSampleRate;
        boolean    boolResult = false;

        strSampleRate = comboSampleRate.getSelectedItem ();
        if ( strSampleRate != null ) {
            dSampleRate = Double.valueOf(strSampleRate).doubleValue();
            if ( format.getSampleRate() == Format.NOT_SPECIFIED )
                boolResult = true;
            else if ( format.getSampleRate() == dSampleRate )
                boolResult = true;
        }
        return ( boolResult );
    }

    private boolean isFormatGoodForBitSize ( AudioFormat format ) {
        int        nBits;
        boolean    boolResult = false;

        if ( checkBits8.getState() == true )
            nBits = 8;
        else if ( checkBits16.getState() == true )
            nBits = 16;
        else
            nBits = Format.NOT_SPECIFIED;

        if ( format.getSampleSizeInBits() == Format.NOT_SPECIFIED )
            boolResult = true;
        else if ( nBits == Format.NOT_SPECIFIED )
            boolResult = true;
        else if ( format.getSampleSizeInBits() == nBits )
            boolResult = true;
        else if ( format.getSampleSizeInBits() < 8 ) // Hack to allow 4bit and 0bit
            boolResult = true;

        return ( boolResult );
    }

    private boolean isFormatGoodForChannels ( AudioFormat format ) {
        int        nChannels;
        boolean    boolResult = false;

        if ( checkMono.getState() == true )
            nChannels = 1;
        else if ( checkStereo.getState() == true )
            nChannels = 2;
        else
            nChannels = Format.NOT_SPECIFIED;

        if ( format.getChannels() == Format.NOT_SPECIFIED )
            boolResult = true;
        else if ( nChannels == Format.NOT_SPECIFIED )
            boolResult = true;
        else if ( format.getChannels() == nChannels )
            boolResult = true;

        return ( boolResult );
    }

    private boolean isFormatGoodForEndian ( AudioFormat format ) {
        int        nEndian;
        boolean    boolResult = false;

        if ( checkEndianBig.getState() == true )
            nEndian = AudioFormat.BIG_ENDIAN;
        else if ( checkStereo.getState() == true )
            nEndian = AudioFormat.LITTLE_ENDIAN;
        else
            nEndian = Format.NOT_SPECIFIED;

        if ( format.getEndian() == Format.NOT_SPECIFIED )
            boolResult = true;
        else if ( nEndian == Format.NOT_SPECIFIED )
            boolResult = true;
        else if ( format.getEndian() == nEndian )
            boolResult = true;

        return ( boolResult );
    }

    private boolean isFormatGoodForSigned ( AudioFormat format ) {
        int        nSigned;
        boolean    boolResult = false;

        if ( checkSigned.getState() == true )
            nSigned = AudioFormat.SIGNED;
        else
            nSigned = AudioFormat.UNSIGNED;

        if ( format.getSigned() == Format.NOT_SPECIFIED )
            boolResult = true;
        else if ( nSigned == Format.NOT_SPECIFIED )
            boolResult = true;
        else if ( format.getSigned() == nSigned )
            boolResult = true;

        return ( boolResult );
    }

    private boolean isFormatSupported ( AudioFormat format ) {
        int            i;
        int            nCount;
        AudioFormat    formatAudio;
        boolean        boolSupported = false;

        if ( format == null )
            return ( boolSupported );

        nCount = vectorContSuppFormats.size ();
        for ( i = 0;  i < nCount  &&  boolSupported == false;  i++ ) {
            formatAudio = (AudioFormat) vectorContSuppFormats.elementAt ( i );
            if ( formatAudio.matches(format) )
                boolSupported = true;
        }
        return ( boolSupported );
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
        else if ( objectSource == comboSampleRate ) {
            updateFieldsFromRate ( formatOld );
        }
        else if ( objectSource == checkBits8  ||   objectSource == checkBits16 ) {
            updateFieldsFromBits ( formatOld );
        }
        else if ( objectSource == checkMono  ||   objectSource == checkStereo ) {
            updateFieldsFromChannels ( formatOld );
        }
        else if ( objectSource == checkEndianBig  ||   objectSource == checkEndianLittle ) {
            updateFieldsFromEndian ( formatOld );
        }
        else if ( objectSource == checkSigned ) {
            updateFieldsFromSigned ( formatOld );
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
        labelSampleRate.setEnabled ( boolEnable );
        comboSampleRate.setEnabled ( boolEnable );
        labelHz.setEnabled ( boolEnable );
        labelBitsPerSample.setEnabled ( boolEnable );
        checkBits8.setEnabled ( boolEnable && boolEnable8 );
        checkBits16.setEnabled ( boolEnable && boolEnable16 );
        labelChannels.setEnabled ( boolEnable );
        checkMono.setEnabled ( boolEnable && boolEnableMono );
        checkStereo.setEnabled ( boolEnable && boolEnableStereo );
        labelEndian.setEnabled ( boolEnable );
        checkEndianBig.setEnabled ( boolEnable && boolEnableEndianBig );
        checkEndianLittle.setEnabled ( boolEnable && boolEnableEndianLittle );
        checkSigned.setEnabled ( boolEnable && boolEnableSigned );
    }


}


