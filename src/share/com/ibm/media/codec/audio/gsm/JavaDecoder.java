/*
 * @(#)JavaDecoder.java	1.9 02/08/21
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
 * RTP GSM decoder plugin wrapper, which uses Java methods to do the decoding.
 * @author Shay Ben-David bendavid@haifa.vnet.ibm.com
 */
public class JavaDecoder extends com.ibm.media.codec.audio.AudioCodec {

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

     protected GsmDecoder decoder;

     ////////////////////////////////////////////////////////////////////////////
    // Methods
    public JavaDecoder() {
       	supportedInputFormats = new AudioFormat[] { new AudioFormat(AudioFormat.GSM),
						    new AudioFormat(AudioFormat.GSM_RTP)};
        defaultOutputFormats  = new AudioFormat[] { new AudioFormat(AudioFormat.LINEAR) };
        PLUGIN_NAME="GSM Decoder";
    }

    protected  Format[] getMatchingOutputFormats(Format in) {

        AudioFormat af =(AudioFormat) in;

        supportedOutputFormats = new AudioFormat[] {
                new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                16,
                af.getChannels(),
                AudioFormat.LITTLE_ENDIAN, //isBigEndian(),
                AudioFormat.SIGNED //isSigned());
                )                };
        return  supportedOutputFormats;
    }

    public void open() throws ResourceUnavailableException{
        decoder=new GsmDecoder();
        decoder.decoderInit();
    }

    public void reset() {
        resetDecoder();
    }


    public void close() {
        freeDecoder();
    }


    public int process(Buffer inputBuffer, Buffer outputBuffer) {

        if (!checkInputBuffer(inputBuffer) ) {
            return BUFFER_PROCESSED_FAILED;
        }

        if (isEOM(inputBuffer) ) {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }

        int inpLength=inputBuffer.getLength();
        int outLength = calculateOutputSize(inputBuffer.getLength() );

        byte[] inpData = (byte[]) inputBuffer.getData();
        byte[] outData = validateByteArraySize(outputBuffer, outLength);


        decode(inpData, inputBuffer.getOffset(), outData, 0, inpLength);

        updateOutput(outputBuffer, outputFormat, outLength, 0);
        return BUFFER_PROCESSED_OK;
    }

    protected void freeDecoder() {
        decoder = null;
    }

    protected void resetDecoder() {
        decoder.decoderInit();
    }

    protected int calculateOutputSize(int inputSize) {
        return inputSize/33*320;
    }

    protected void decode(byte[] inpData,int readPtr,byte[] outData,int writePtr,int inpLength) {
        int numberOfFrames = (inpLength/33);

        for ( int n=1 ; n<=numberOfFrames ; n++,writePtr += 320,readPtr += 33) {
                decoder.decodeFrame(inpData, readPtr , outData,writePtr);
        }
    }

    public java.lang.Object[] getControls() {
        if (controls==null) {
             controls=new Control[1];
             controls[0]=new SilenceSuppressionAdapter(this,true,false);
	}
        return (Object[])controls;
    }


}



