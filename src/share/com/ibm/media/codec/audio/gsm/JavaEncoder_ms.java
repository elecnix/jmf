/*
 * @(#)JavaEncoder_ms.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio.gsm;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.format.*;
import com.sun.media.*;

/**
 * MS GSM encoder plugin wrapper, which uses Java methods to do the encoding.
 * @author Shay Ben-David bendavid@haifa.vnet.ibm.com
 */
public class JavaEncoder_ms extends JavaEncoder {

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

    public JavaEncoder_ms() {
       	supportedInputFormats = new AudioFormat[] {
	                                new AudioFormat(AudioFormat.LINEAR,
					        Format.NOT_SPECIFIED,
						16,
						1,
						AudioFormat.LITTLE_ENDIAN,
						AudioFormat.SIGNED,
                                                Format.NOT_SPECIFIED,
                                                Format.NOT_SPECIFIED,
                                                Format.byteArray

						) };

        defaultOutputFormats  = new AudioFormat[] {new WavAudioFormat(AudioFormat.GSM_MS)};

        PLUGIN_NAME="MS GSM Encoder";
        historySize = 640;
    }

    protected  Format[] getMatchingOutputFormats(Format in) {

        AudioFormat af =(AudioFormat) in;

        supportedOutputFormats = new AudioFormat[] {
                new WavAudioFormat(
                    AudioFormat.GSM_MS,
                    af.getSampleRate(),
                    0,
                    af.getChannels(),
                    65*8,
                    (int) (af.getSampleRate() * af.getChannels() / 320 * 65),
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    Format.byteArray,
                    new byte[] { (byte)0x40, (byte)0x01}

                )
        };
        return  supportedOutputFormats;
    }

    public void open() throws ResourceUnavailableException{
        encoder=new GsmEncoder_ms();
        encoder.gsm_encoder_reset();
    }

    protected int calculateOutputSize(int inputSize) {
        return calculateFramesNumber(inputSize) * 65 ;
    }

    protected int calculateFramesNumber(int inputSize) {
        return inputSize / 640;
    }

    protected boolean codecProcess(byte[] inpData,int readPtr,
                                    byte[] outData,int writePtr,
  			            int inpLength,
				    int[]  readBytes,int[] writeBytes,
                                    int[]  frameNumber,
				    int[] regions,int[] regionsTypes) {

        int inCount = 0;
        int outCount = 0;
        int channels=inputFormat.getChannels();
        boolean isStereo = ( channels == 2);

        final int frames = inpLength/(640);

        regions[0]=writePtr;

        for (int frameCounter = 0; frameCounter<frames ; frameCounter++) {
              encoder.gsm_encode_frame(inpData, readPtr , outData,writePtr);
              readPtr += 640;
              inCount += 640;

              outCount += 65;
              writePtr += 65;

              regions     [frameCounter+1]= outCount + writePtr;
              regionsTypes[frameCounter  ]= 0;

//            System.out.println(inCount+" "+outCount+" "+inpLength);
        }

        readBytes [0]=inCount;
        writeBytes[0]=outCount;
        frameNumber[0]=frames;

        return true;
    }





}



