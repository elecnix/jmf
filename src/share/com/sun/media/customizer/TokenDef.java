/*
 * @(#)TokenDef.java	1.2 00/02/09
 *
 * Copyright 1996-2000 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.media.customizer;

/** 
 *  This class defines all the tokens used in CustomDB.java
 *
 *  @version 2.0
 */

public interface TokenDef {
    // general
    public static final int GUI = 1;
    public static final int STUDIO = 2;
    public static final int SINKFILE = 3;
    public static final int SINKRTP = 4;
    public static final int RTP = 5;
    public static final int RTSP = 6;
    public static final int NOTPB = 7;
    public static final int PLYRBEAN = 8;
    public static final int MPLYR = 9;
    public static final int CLNMGDS = 10;

    // audio renderer
    public static final int JSRENDER = 15;
    public static final int SUNARENDER = 16;
    public static final int DARENDER = 17;
    // video renderer
    public static final int AWTRNDR = 20;
    public static final int JPEGRNDR = 21;
    public static final int XILRNDR = 22;
    public static final int XLIBRNDR = 23;
    public static final int SUNRAYRNDR = 24;
    public static final int GDIRNDR = 25;
    public static final int DDRNDR = 26;

    // capture DataSource
    public static final int DSJS = 30;
    public static final int VFW = 31;
    public static final int SVDO = 32;
    public static final int SVDOPLS = 33;
    public static final int DSOUND = 34;

    //protocol DataSource 
    public static final int DSHTTP = 36;
    public static final int DSFILE = 37;
    public static final int DSFTP = 38;
    public static final int DSHTTPS = 39;

    // Parser
    public static final int WAVPSR = 40;
    public static final int AUPSR = 41;
    public static final int AIFFPSR = 42;
    public static final int GSMPSR = 43;
    public static final int QTPSR = 44;
    public static final int AVIPSR = 45;
    public static final int MPEGPSR = 46;

    // Mux
    public static final int AVIMUX = 50;
    public static final int GSMMUX = 51;
    public static final int MPEGMUX = 52;
    public static final int AIFFMUX = 53;
    public static final int WAVMUX = 54;
    public static final int AUMUX = 55;
    public static final int QTMUX = 56;
    
    // audio codec
    public static final int MPAJD = 60;
    public static final int MPAND = 61;
    public static final int MPANE = 62;
    public static final int MPAPRTP = 63;
    public static final int MPADRTP = 64;
    public static final int ULAWJD = 65;
    public static final int ULAWJE = 66;
    public static final int ULAWPRTP = 67;
    public static final int ULAWDRTP = 68;
    public static final int GSMDRTP = 69;
    public static final int GSMPRTP = 70;
    public static final int MSGSMJD = 71;
    public static final int GSMJD = 72;
    public static final int MSGSMND = 73;
    public static final int GSMND = 74;
    public static final int MSGSMJE = 75;
    public static final int GSMJE = 76;
    public static final int MSGSMNE = 77;
    public static final int GSMNE = 78;
    public static final int ALAWJD = 79;
    public static final int MSIMA4JD = 80;
    public static final int IMA4JD = 81;
    public static final int MSIMA4JE = 82;
    public static final int IMA4JE = 83;
    public static final int DVIJD = 84;
    public static final int DVIDRTP = 85;
    public static final int DVIJE = 86;
    public static final int DVIPRTP = 87;
    public static final int G723PRTP = 88;
    public static final int G723JD = 89;
    public static final int G723DRTP = 90;
    public static final int G723ND = 91;
    public static final int G723NE = 92;
    public static final int MSADPCMJD = 93;
    public static final int ACM = 94;

    // video codec
    public static final int CINEPAKJD = 100;
    public static final int CINEPAKND = 101;
    public static final int CINEPAKPRONE = 102;
    public static final int H263NE = 103;
    public static final int H263PRTP = 104;
    public static final int H263ND = 105;
    public static final int H263DRTP = 106;
    public static final int H263JD = 107;
    public static final int H261ND = 108;
    public static final int H261DRTP = 109;
    public static final int JPEGDRTP = 110;
    public static final int JPEGPRTP = 111;
    public static final int JPEGND = 112;
    public static final int JPEGNE = 113;
    public static final int MPEGDRTP = 114;
    public static final int MPEGPRTP = 115;
    public static final int MPEGND = 116;
    public static final int IV32ND = 117;
    public static final int VCMNE = 118;
    public static final int VCMND = 119;

    // special players 
    public static final int MVR = 125;
    public static final int MIDI = 128;
    public static final int RMF = 129;
    public static final int CDAUDIO = 130;

    public static final int SMPEGPLY = 131;
    public static final int WMPEGPLY = 132;
    public static final int MPEG2PLY = 133;

}



    
