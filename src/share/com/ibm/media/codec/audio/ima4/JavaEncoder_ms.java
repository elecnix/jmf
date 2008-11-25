/*
 *  @(#)JavaEncoder_ms.java	1.9 02/08/21
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
import com.sun.media.format.*;
import com.sun.media.*;

public class JavaEncoder_ms extends com.ibm.media.codec.audio.BufferedEncoder {


    ////////////////////////////////////////////////////////////////////////////
    // Variables
    ////////////////////////////////////////////////////////////////////////////


    // state of the ima4 decoder
    private IMA4State ima4stateL,ima4stateR;

    // *** encoding control ?
    private int inputframeSizeInBytes =  1010;

    ////////////////////////////////////////////////////////////////////////////
    // Methods


    public JavaEncoder_ms() {
	supportedInputFormats = new AudioFormat[] {
	new AudioFormat(
               AudioFormat.LINEAR,
               AudioFormat.NOT_SPECIFIED,
               16,
               AudioFormat.NOT_SPECIFIED,
               AudioFormat.LITTLE_ENDIAN,
               AudioFormat.SIGNED,
               AudioFormat.NOT_SPECIFIED,//inputframeSizeInBytes*8,
               AudioFormat.NOT_SPECIFIED,
               Format.byteArray
               )};
        defaultOutputFormats  = new AudioFormat[] { new WavAudioFormat(AudioFormat.IMA4_MS) };
        PLUGIN_NAME="IMA4 MS Encoder";


    }


    protected  Format[] getMatchingOutputFormats(Format in) {
      AudioFormat af =(AudioFormat) in;
      int outFrameSizeInBits =
                ( (inputframeSizeInBytes-2) *2 + 4*8) * af.getChannels();

      int wSamplesPerBlock = inputframeSizeInBytes / 2;

      supportedOutputFormats = new AudioFormat[] {
                new WavAudioFormat(
                    AudioFormat.IMA4_MS,
                    af.getSampleRate(),
                    4,
                    af.getChannels(),
                    outFrameSizeInBits,
                       ((int)af.getSampleRate()) * 2 / 8 *
		       outFrameSizeInBits / inputframeSizeInBytes,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    Format.byteArray,
                    new byte[] { (byte)(wSamplesPerBlock&0xff),
		                 (byte)(wSamplesPerBlock>>8)
			       }


                ) };

        historySize = inputframeSizeInBytes*af.getChannels();

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
        return calculateFramesNumber(inputSize) *  ((inputframeSizeInBytes-4) * 4 +2) ;
    }

    protected int calculateFramesNumber(int inputSize) {
//System.out.println("frame number "+inputSize+" => "+inputSize / 128);
        return inputSize / inputframeSizeInBytes;
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

        final int frames = inpLength/(channels*1010);

      int iterations = (inputframeSizeInBytes-2)>>1 ;

        regions[0]=writePtr;

        // MSDVI stereo format is a mess !
        // header left  (same as mono header)
        // header right (same as mono header)
        // 4 bytes left  (8 samples)
        // 4 bytes right (8 samples)
        // 4 bytes left  (8 samples)
        // 4 bytes right (8 samples)
        //    :      :      :
        //    :      :      :
        // 4 bytes left  (8 samples)
        // 4 bytes right (8 samples)

        // ima4stateL,R are stored between chunks encoding.

//        System.out.println("iter "+iterations +" frames "+frames);
        for (int frameCounter = 0; frameCounter<frames ; frameCounter++) {
            //LEFT or mono


            int valprev= (inpData[readPtr+(inCount++)] & 0xff);
            valprev   |= (inpData[readPtr+(inCount++)] << 8);

            ima4stateL.valprev=valprev;
            // validate index legality
            if (ima4stateL.index>88) {
                ima4stateL.index=88;
            } else if (ima4stateL.index<0) {
                ima4stateL.index=0;
            }


            outData[writePtr+(outCount++)]=(byte)(valprev);
            outData[writePtr+(outCount++)]=(byte)(valprev>>8);
            outData[writePtr+(outCount++)]=(byte)(ima4stateL.index);
            outCount++;

            if (isStereo) {
                valprev= (inpData[readPtr+(inCount++)] & 0xff);
                valprev   |= (inpData[readPtr+(inCount++)] << 8);

                ima4stateR.valprev=valprev;
                // validate index legality
                if (ima4stateR.index>88) {
                    ima4stateR.index=88;
                } else if (ima4stateR.index<0) {
                    ima4stateR.index=0;
                }


                outData[writePtr+(outCount++)]=(byte)(valprev);
                outData[writePtr+(outCount++)]=(byte)(valprev>>8);
                outData[writePtr+(outCount++)]=(byte)(ima4stateR.index);
                outCount++;

            }

            for (int loop=0 ; loop<iterations/8 ; loop++ ) {

                IMA4.encode(inpData,inCount+readPtr,outData,outCount+writePtr,8,ima4stateL,stride);
                outCount += (8 >> 1);

                if (isStereo) {
                    IMA4.encode(inpData,inCount+readPtr+2,outData,outCount+writePtr,8,ima4stateR,stride);
                    outCount += (8 >> 1);
                    inCount  += (8 << 2);

                } else {
                    inCount  += (8 << 1);
                }
            }


              regions     [frameCounter+1]= outCount + writePtr;
              regionsTypes[frameCounter  ]= 0;


        }
        readBytes [0]=inCount;
        writeBytes[0]=outCount;
        frameNumber[0]=frames;


        return true;
    }

}
