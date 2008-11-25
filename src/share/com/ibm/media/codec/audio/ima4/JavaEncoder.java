/*
 *  @(#)JavaEncoder.java	1.9 02/08/21
 * 
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.ibm.media.codec.audio.ima4;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;

public class JavaEncoder extends com.ibm.media.codec.audio.BufferedEncoder {


    ////////////////////////////////////////////////////////////////////////////
    // Variables
    ////////////////////////////////////////////////////////////////////////////


    // state of the ima4 decoder
    private IMA4State ima4stateL,ima4stateR;


    ////////////////////////////////////////////////////////////////////////////
    // Methods


    public JavaEncoder() {
	supportedInputFormats = new AudioFormat[] { new AudioFormat(
               AudioFormat.LINEAR,
               AudioFormat.NOT_SPECIFIED,
               16,
               AudioFormat.NOT_SPECIFIED,
               AudioFormat.LITTLE_ENDIAN,
               AudioFormat.SIGNED,
               AudioFormat.NOT_SPECIFIED,
               AudioFormat.NOT_SPECIFIED,
               Format.byteArray
               )};
        defaultOutputFormats  = new AudioFormat[] { new AudioFormat(AudioFormat.IMA4) };
        PLUGIN_NAME="IMA4 Encoder";

        historySize = 256; // worst case: stereo * 2 bytes/sample * 64 samples

    }
    protected  Format[] getMatchingOutputFormats(Format in) {
      AudioFormat af =(AudioFormat) in;

      supportedOutputFormats = new AudioFormat[] {
                 new AudioFormat(
                AudioFormat.IMA4,
                af.getSampleRate(),
                16,
                af.getChannels(),
                Format.NOT_SPECIFIED, //isBigEndian(),
                Format.NOT_SPECIFIED, //isSigned());
                34 * 8 * af.getChannels(),
                AudioFormat.NOT_SPECIFIED,
                Format.byteArray
                ) };

        return  supportedOutputFormats;
    }


    /** Initializes the codec.  **/
    public void open() {
       ima4stateL=new IMA4State();
       ima4stateR=new IMA4State();

    }

    /** Clean up **/
    public void close() {
        ima4stateL = null;
        ima4stateR = null;
    }
    // reset IMA4 encoder state
    public void codecReset() {
        ima4stateL.index   = 0;
        ima4stateL.valprev = 0;
        ima4stateR.index   = 0;
        ima4stateR.valprev = 0;
    }

    protected int calculateOutputSize(int inputSize) {
//System.out.println("output number "+inputSize+" => "+calculateFramesNumber(inputSize) * 34);
        return calculateFramesNumber(inputSize) * 34 * 2;
    }

    protected int calculateFramesNumber(int inputSize) {
//System.out.println("frame number "+inputSize+" => "+inputSize / 128);
        return inputSize / 128;
    }

    protected boolean codecProcess(byte[] inpData,int readPtr,
                                    byte[] outData,int writePtr,
  			            int inpLength,
				    int[]  readBytes,int[] writeBytes,
                                    int[]  frameNumber,
				    int[] regions,int[] regiostypes) {

        int inCount = 0;
        int outCount = 0;
        int channels=inputFormat.getChannels();
        boolean isStereo = ( channels == 2);
        int stride = isStereo ? 2 : 0;

        final int frames = inpLength/(channels*128);

        regions[0]=writePtr;

        // IMA4 mono takes 64 little endian input stereo interleaved samples (256 bytes)
        // and convert them to:
        // LEFT: 2 bytes header followed by 32 bytes encoded data
        // followed by:
        // RIGHT: 2 bytes header followed by 32 bytes encoded data

        // ima4stateL,R are stored between chunks encoding.

        for (int frameCounter = 0; frameCounter<frames ; frameCounter++) {
            //LEFT or mono

            // validate index legality
            if (ima4stateL.index>88) {
                ima4stateL.index=88;
            } else if (ima4stateL.index<0) {
                ima4stateL.index=0;
            }

            ima4stateL.valprev &= 0xFFFFFF80 ;  // synchronize state of encoder and decoder

            int stateL = ima4stateL.valprev | ima4stateL.index;

            // state is now prevVal(9 most significant bits- signed )::index (7 least significant bits- unsigned)
            // note that state is sign extended !

            // store state in big endian
            outData[writePtr+(outCount++)]=(byte)(stateL>>8);
            outData[writePtr+(outCount++)]=(byte)(stateL);

            IMA4.encode(inpData,readPtr+inCount,outData,writePtr+outCount,64,ima4stateL,stride);

            outCount += (64>>1);


            //RIGHT
            if (isStereo) {
                // validate index legality
                if (ima4stateR.index>88) {
                    ima4stateR.index=88;
                } else if (ima4stateR.index<0) {
                    ima4stateR.index=0;
                }

                ima4stateR.valprev &= 0xFFFFFF80 ;  // synchronize state of encoder and decoder

                int stateR = ima4stateR.valprev | ima4stateR.index;

                // state is now prevVal(9 most significant bits- signed )::index (7 least significant bits- unsigned)
                // note that state is sign extended !

                // store state in big endian
                outData[writePtr+(outCount++)]=(byte)(stateR>>8);
                outData[writePtr+(outCount++)]=(byte)(stateR);

                IMA4.encode(inpData,readPtr+inCount+2,outData,writePtr+outCount,64,ima4stateR,stride);

                outCount += (64>>1);

                inCount += (64<<2);
            } else {
                inCount += (64<<1);

            }
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

