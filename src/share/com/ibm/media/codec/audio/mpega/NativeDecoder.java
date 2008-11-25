/*
 * @(#)NativeDecoder.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio.mpega;


import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;

/** GSM to PCM java decoder
 *  @author Shay Ben-David bendavid@haifa.vnet.ibm.com
 **/
public class NativeDecoder extends com.ibm.media.codec.audio.AudioCodec {

    ////////////////////////////////////////////////////////////////////////////
    // Variables

    /** <FONT COLOR="#FF0000">
    *  Licensed Materials - Property of IBM                         <br><br>
    *  "Restricted Materials of IBM"                                <br><br>
    *  5746-SM2                                                     <br><br>
    *  (c) Copyright IBM Corporation 1997,1998 All Rights Reserved       <br><br>
    *  US Government Users Restricted Rights - Use, duplication or
    *  disclosure restricted by GSA ADP Schedule Contract with
    *  IBM Corporation.</FONT>
    *
    **/
    public static final String a_copyright_notice="(c) Copyright IBM Corporation 1997,1998.";

    /* native functions prototype */
    private static native int mpeg_initialization (long[] pdata, int mpeg_quality);
    private static native int mpeg_audio_decode (long pdata,
                      byte[] in_buf_byte, int in_buf_offset, int in_buf_len,
                      byte[] out_buf_byte, int out_buf_offset, int out_buf_len,
                      int one_frame_flag, int[] num_samples_done,
                      int[]in_bytes_read, int[] samp_freq, int[] stereo);
    private static native int mpeg_terminate (long pdata);

    /* define MPEG constants */
     // maximum frame size is at layer 2, bitrate 384, sampling freq 32 KHz
    static final int MAX_MPEG_STREAM_FRAME_SIZE = 1728;
    static final int MAX_PCM_FRAME_SIZE         = 2304;   // stereo in Layer II
    static final int INTERNAL_BUFFER_LEN        = 50 * MAX_MPEG_STREAM_FRAME_SIZE;   // 50 is arbitray, > 1 second

    /* define decoding mode: one/many frames per call */
    static final int DECODE_ONE_FRAME_ONLY = 0;
    static final int DECODE_MANY_FRAMES    = 1;

    /* controling the audio quality (valid only for 44.1 KHz sampling rate */
    static final int MPEG_CD_QUALITY       = 0;
    static final int MPEG_HIGH_QUALITY     = 1;
    static final int MPEG_MEDIUM_QUALITY   = 2;

    /* Return codes and error codes (do not change these values) */
    static final int MPEG_NOERROR        = 0;
    static final int MPEG_ERROR          = 1;

    /* variables */
    private int mpegAudioQuality = MPEG_CD_QUALITY;   // default: CD
    private int one_frame_flag = DECODE_MANY_FRAMES;  // default: all the frames
    // pointer to the MPEG decoder inner data structure
    private long[] pdata                = new long[1];

    private int[] samp_freq             = new int[1];
    private int[] stereo                = new int[1];
    private int[] in_bytes_read         = new int[1];
    private int[] num_samples_done      = new int[1];

    private byte[] internalBuffer       = new byte[INTERNAL_BUFFER_LEN+20];   // +10 is enough, to prevent over-read inside the dll
    private int internalBufferDataLen   = 0;


    ////////////////////////////////////////////////////////////////////////////
    // Methods
    public NativeDecoder() {
       	supportedInputFormats = new AudioFormat[] { new AudioFormat(AudioFormat.MPEG)   };
        defaultOutputFormats  = new AudioFormat[] { new AudioFormat(AudioFormat.LINEAR) };
        PLUGIN_NAME="MPEG Decoder";
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
        try {
	    JMFSecurityManager.loadLibrary("jmutil");
	    JMFSecurityManager.loadLibrary("jmmpega");
            internalBufferDataLen = 0;
            mpeg_initialization(pdata,mpegAudioQuality);
            return;

	} catch (Throwable t) {
            System.out.println("Unable to load "+PLUGIN_NAME+"\n"+t);

	}
        throw new ResourceUnavailableException("Unable to load "+PLUGIN_NAME);
    }

    public void reset() {
        close();
        try {
            open();
        } catch (Exception e) {
        }
    }


    public void close() {
        mpeg_terminate (pdata[0]);
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
     int outLength=44100*40;  /* default output is 1 second of 44.1Khz stereo */ // *40 is arbitray
     int rc, returnResult = 0;
     byte[] inpData = (byte[]) inputBuffer.getData();
     byte[] outData = validateByteArraySize(outputBuffer, outLength);

     /* fill input data */
     if (INTERNAL_BUFFER_LEN - internalBufferDataLen > inpLength) {
         System.arraycopy(inpData, inputBuffer.getOffset(),
                          internalBuffer, internalBufferDataLen, inpLength);
         internalBufferDataLen += inpLength;
     } else {
         returnResult |= INPUT_BUFFER_NOT_CONSUMED;
     }

     /* decode */
     rc = mpeg_audio_decode (pdata[0],
                             internalBuffer, 0, internalBufferDataLen ,
                             outData, 0, outData.length,
                             one_frame_flag, num_samples_done,
                             in_bytes_read, samp_freq, stereo);
     if (rc != MPEG_NOERROR) {
         // System.out.println("MPEG Audio decoder error");
         return (returnResult | BUFFER_PROCESSED_FAILED);
     }

     /* if read too much, due to incomplete frame, do not crash */
     if (in_bytes_read[0] > internalBufferDataLen) {
         //if (in_bytes_read[0]-10 > internalBufferDataLen) {    // few bytes can be over-read in the dll in end-of-stream...
         //    System.out.println("PROBLEM in Mpeg Audio Decoder: too many bytes have been decoded");
         //}
         in_bytes_read[0] = internalBufferDataLen;
     }

     /* meanwhile, always shift data to buf start (not so efficient...) */
     System.arraycopy(internalBuffer, in_bytes_read[0],
                      internalBuffer, 0,
		      internalBufferDataLen - in_bytes_read[0]);

     /* update */
     internalBufferDataLen -= in_bytes_read[0];
     outLength = num_samples_done[0] << 1; // number of bytes
     if (outLength > 0) {
         updateOutput(outputBuffer, outputFormat, outLength, 0);
     } else {
         returnResult |= OUTPUT_BUFFER_NOT_FILLED;
     }

     return (returnResult | BUFFER_PROCESSED_OK);
   }

}


