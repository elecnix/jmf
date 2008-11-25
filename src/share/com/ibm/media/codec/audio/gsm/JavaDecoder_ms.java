/*
 * @(#)JavaDecoder_ms.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio.gsm;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.sun.media.controls.*;


/**
 * MS GSM decoder plugin wrapper, which uses Java methods to do the decoding.
 * @author Shay Ben-David bendavid@haifa.vnet.ibm.com
 */
public class JavaDecoder_ms extends JavaDecoder {

     ////////////////////////////////////////////////////////////////////////////
    // Variables

   /** <FONT COLOR="#FF0000">
    *  Licensed Materials - Property of IBM                         <br><br>
    *  "Restricted Materials of IBM"                                <br><br>
    *  5648-B81                                                     <br><br>
    *  (c) Copyright IBM Corporation 1997,1999 All Rights Reserved  <br><br>
    *  US Government Users Restricted Rights - Use, duplication or
    *  disclosure restricted by GSA ADP Schedule Contract with
    *  IBM Corporation.</FONT>
    *
    **/
    public static final String a_copyright_notice="(c) Copyright IBM Corporation 1997,1999.";

     ////////////////////////////////////////////////////////////////////////////
    // Methods

    public JavaDecoder_ms() {
	super();
	supportedInputFormats = new AudioFormat[] { new AudioFormat(AudioFormat.GSM_MS) };
        PLUGIN_NAME="GSM MS Decoder";
    }

    public void open() throws ResourceUnavailableException{
        decoder=new GsmDecoder_ms();
        decoder.decoderInit();
    }


    protected int calculateOutputSize(int inputSize) {
        return inputSize/65 * 640;

    }

    protected void decode(byte[] inpData,int readPtr,byte[] outData,int writePtr,int inpLength) {
        int numberOfFrames = (inpLength/65);

        for ( int n=1 ; n<=numberOfFrames ; n++,writePtr += 640,readPtr += 65) {
            decoder.decodeFrame(inpData, readPtr , outData,writePtr);
        }
    }

    public java.lang.Object[] getControls() {
        if (controls==null) {
             controls=new Control[1];
             controls[0]=new SilenceSuppressionAdapter(this,false,false);
	}
        return (Object[])controls;
    }

}



