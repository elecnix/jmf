/*
 *
 *
 * Copyright 1998-1999 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.ibm.media.codec.audio.mpegenc;


import java.io.*;
import java.util.*;
import java.awt.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;

/**
 *  Native Mpeg Encoder Java wrapper
 *
 *  @author Shay Ben-David bendavid@il.ibm.com
 **/
public class NativeEncoder extends com.ibm.media.codec.audio.AudioCodec {

    ////////////////////////////////////////////////////////////////////////////
    // Variables

    /** <FONT COLOR="#FF0000">
    *  Licensed Materials - Property of IBM                         <br><br>
    *  "Restricted Materials of IBM"                                <br><br>
    *  5746-SM2                                                     <br><br>
    *  (c) Copyright IBM Corporation 1997,1999 All Rights Reserved  <br><br>
    *  US Government Users Restricted Rights - Use, duplication or
    *  disclosure restricted by GSA ADP Schedule Contract with
    *  IBM Corporation.</FONT>
    *
    **/
    public static final String a_copyright_notice=
                        "(c) Copyright IBM Corporation 1997,1999.";

    Control[] controls=null;


    /* define MPEG constants */

     // maximum frame size is at layer 2, bitrate 384, sampling freq 32 KHz
    static final int MAX_MPEG_STREAM_FRAME_SIZE = 1728;
    static final int MAX_PCM_FRAME_SIZE         = 2304;   // stereo in Layer II

    static final int ENCD_MAX_FRAMES            = 9;

    static final int MAX_INPUT_SIZE   = ENCD_MAX_FRAMES * MAX_PCM_FRAME_SIZE * 2;
    static final int MAX_OUTPUT_SIZE  = ENCD_MAX_FRAMES * MAX_MPEG_STREAM_FRAME_SIZE;

    /* Return codes and error codes (do not change these values) */
    static final int MPEG_NOERROR        = 0;
    static final int MPEG_ERROR          = 1;

    static final int MPEG_FALSE          = 0;
    static final int MPEG_TRUE           = 1;

    static final int MPEG_STEREO         = 0;
    static final int MPEG_JOINT_STEREO   = 1;
    static final int MPEG_DUAL_CHANNEL   = 2;
    static final int MPEG_SINGLE_CHANNEL = 3;

    // allowed bit rates
     static final int[] layer1BitRate_mono= {
         32,  64,  96, 128, 160,
	192, 224, 256, 288, 320,
	352, 384, 416, 448 };

     static final int[] layer1BitRate_stereo= {
              64,  96, 128, 160,
	192, 224, 256, 288, 320,
	352, 384, 416, 448 };

     static final int[] layer2BitRate_mono= {
         32,  48,  56,  64,  80,
	 96, 112, 128, 160, 192 };

     static final int[] layer2BitRate_stereo= {
         64,  96, 112, 128, 160,
	192, 224, 256, 320, 384 };


/* variables */

    // pointer to the MPEG decoder inner data structure
    private int[] pdata                 = new int[1];
    private int[] psetup                = new int[1];
    private int   layer;

    private int   encodingType;
    private int   bitrate;
    private int   samplingFrequency;
    private int   endianess;
    private int   copyright;

    private int   original;
    private int   errorProtect;

    private int   nChannels;

    private int[] in_bytes         = new int[1];
    private int[] out_bytes        = new int[1];

    boolean isStarted;

    byte[] history=new byte[MAX_INPUT_SIZE];
    int historyLength=0;


    ////////////////////////////////////////////////////////////////////////////
    // Methods

    public NativeEncoder() {
        int[] SamplingFrequencies={32000, 44100,48000};

       	supportedInputFormats = new AudioFormat[3];

        for (int i=0;i<3;i++) {
            supportedInputFormats[i]=new AudioFormat(AudioFormat.LINEAR,
                                                SamplingFrequencies[i],
						16,
                                                Format.NOT_SPECIFIED,
						AudioFormat.LITTLE_ENDIAN,
						AudioFormat.SIGNED,
                                                Format.NOT_SPECIFIED,
                                                Format.NOT_SPECIFIED,
                                                byte[].class
                                );
        }

        defaultOutputFormats  = new AudioFormat[] { new AudioFormat(AudioFormat.MPEG) };
        PLUGIN_NAME="MPEG Audio Encoder";


    }

    /* native functions prototype */
    private static native int nativeOpen  (
                        int[] pdata,
                        int[] psetup
    );

    private static native int nativeInit  (
                        int[] pdata,
                        int[] psetup,
                        int   layer,
                        int   encodingType,
                        int   bitrate,
                        int   samplingFrequency,
                        int   endianess,
                        int   copyright,
                        int   original,
                        int   errorProtect
    );

    private static native int nativeGetBytesToRead (
                        int[] pdata,
                        int[] psetup
    );

    private static native int nativeEncode (
                        int[]  pdata,
                        int[]  psetup,
                        byte[] in_buf,
                        int    in_buf_offset,
			int    in_buf_len,
                        byte[] out_buf,
			int    out_buf_offset,
			int    out_buf_len,
                        int[]  in_bytes,
                        int[]  out_bytes
    );

    private static native int nativeHandleEOM (
                        int[]  pdata,
			int[]  pestup,
                        byte[] out_buf,
			int    out_buf_offset,
         	        int[]  out_bytes
    );

    private static native int nativeClose (
                        int[] pdata,
			int[] pestup
			                  );


    protected  Format[] getMatchingOutputFormats(Format in) {

        AudioFormat af =(AudioFormat) in;

        supportedOutputFormats = new AudioFormat[] {
            new AudioFormat(AudioFormat.MPEG,
	                    af.getSampleRate(),
                            Format.NOT_SPECIFIED,
                            af.getChannels(),
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED,
                            byte[].class
			   )
        };
        return supportedOutputFormats;
    }

    public Format setInputFormat(Format format) {
         AudioFormat af=(AudioFormat)super.setInputFormat(format);
         if (af==null) {
             return null;
         }
         nChannels=af.getChannels();
         samplingFrequency=(int)af.getSampleRate();
         endianess= (af.getEndian() == AudioFormat.BIG_ENDIAN) ? MPEG_TRUE : MPEG_FALSE;
         bitrate=128 * nChannels;
         layer=2;
         encodingType= (nChannels==1) ? MPEG_SINGLE_CHANNEL : MPEG_STEREO;
         copyright=MPEG_FALSE;
         original=MPEG_FALSE;
         errorProtect=MPEG_FALSE;

         return (Format)af;
    }

    public void open() throws ResourceUnavailableException{
        try {
	    JMFSecurityManager.loadLibrary("jmutil");
	    JMFSecurityManager.loadLibrary("jmmpegenc");
            nativeOpen(pdata, psetup);
            reset();
            openControlPanel();

            return;

	} catch (Throwable t) {
            String errMsg= "Unable to load "+PLUGIN_NAME+"\n"+t;
            t.printStackTrace();
            System.err.println(errMsg);
            throw new ResourceUnavailableException(errMsg);

	}
    }

    private void openControlPanel() {
        java.awt.Frame controlFrame=new java.awt.Frame(getName()+"  Control");
        controlFrame.setLayout(new com.sun.media.controls.VFlowLayout(1));
        controlFrame.add(new Label(getName()+"  Control",Label.CENTER) );
        controlFrame.add(new Label( " "));

        Control[] c=(Control[]) getControls();
        for (int i=0;i<c.length;i++) {
            controlFrame.add(c[i].getControlComponent() );
        }
        controlFrame.pack();
        controlFrame.show();
    }

    public void reset() {

        int rc = nativeInit  ( pdata, psetup,
                     layer, encodingType, bitrate, samplingFrequency,
                     endianess, copyright, original, errorProtect
        );

        if (rc!=MPEG_NOERROR) {
            throw new RuntimeException("MPEG encoder setting failed. rc="+rc);
        }

        historyLength=0;
        isStarted=false;

    }


    public void close() {
        nativeClose (pdata, psetup);
    }



   public int process(Buffer inputBuffer, Buffer outputBuffer) {
     isStarted=true;

     int inpLength=inputBuffer.getLength();
     int outLength;
     int rc, returnResult = 0;
     boolean fullFrame=true;

     byte[] inpData = (byte[]) inputBuffer.getData();
     byte[] outData = validateByteArraySize(outputBuffer, MAX_OUTPUT_SIZE);

     int inOffset=inputBuffer.getOffset();
     int outOffset=outputBuffer.getOffset();

     int neededinput=nativeGetBytesToRead(pdata,psetup);

     int inpBytes =  neededinput -  historyLength;

     if (inpBytes>inpLength) {
         inpBytes=inpLength;
         fullFrame=false;
     }

     System.arraycopy(inpData,inOffset,history,historyLength, inpBytes);

     historyLength += inpBytes;
     inOffset  += inpBytes;
     inpLength -= inpBytes;
     inputBuffer.setOffset(inOffset  );
     inputBuffer.setLength(inpLength );

     if (!fullFrame) {
         if (inputBuffer.isEOM() ) {
             nativeHandleEOM (pdata, psetup,outData,outOffset,out_bytes);
             outLength=out_bytes[0];
             updateOutput(outputBuffer, outputFormat, outLength, 0);
             return BUFFER_PROCESSED_OK;
         }

         return OUTPUT_BUFFER_NOT_FILLED;

     }

     rc = nativeEncode (pdata, psetup,
                        history,0,neededinput,
                        outData, outOffset, outData.length,
                        in_bytes, out_bytes);

     historyLength=0;

     if (rc != MPEG_NOERROR) {
         System.out.println("MPEG Audio process error "+rc);
         return (BUFFER_PROCESSED_FAILED);
     }

     outLength=out_bytes[0];
     int readInput=in_bytes[0];

     updateOutput(outputBuffer, outputFormat, outLength, 0);

     return ( INPUT_BUFFER_NOT_CONSUMED | BUFFER_PROCESSED_OK);
}

    //================== Control methods ===========================
    public  java.lang.Object[] getControls() {
        if (controls==null) {
             controls=new Control[1];
             controls[0]=new MpegAudioControlAdapter(this, layer, samplingFrequency,
	                 nChannels, bitrate, 0,0,0,0);
	}


        return (Object[])controls;
    }

    boolean setOriginal(int new_original) {
        original=new_original;
        reset();
        return true;
    }

    boolean setErrorProtect(int new_errorProtect) {
        errorProtect=new_errorProtect;
        reset();
        return true;
    }

    boolean setCopyright(int new_copyRight) {
        copyright=new_copyRight;
        reset();
        return true;
    }

    boolean setBitrate(int new_bitrate) {
        bitrate=new_bitrate;
        reset();
        return true;
    }

    boolean setLayer(int new_layer) {
        layer=new_layer;
        reset();
        return true;
    }

    boolean SetEncodingType(int new_encodingType) {
        encodingType=new_encodingType;
        reset();
        return true;
    }

    boolean isControlValid() {
        if ( (original != MPEG_FALSE ) &&
	     (original != MPEG_TRUE  )
	   )
            return false;

        if ( (errorProtect != MPEG_FALSE ) &&
	     (errorProtect != MPEG_TRUE  )
           )
            return false;

        if ( (copyright != MPEG_FALSE ) &&
	     (copyright != MPEG_TRUE  )
	   )
            return false;

        if ( (endianess != MPEG_FALSE ) &&
	     (endianess != MPEG_TRUE  )
	   )
            return false;

        if ( (samplingFrequency != 32000 ) &&
	     (samplingFrequency != 44100 ) &&
	     (samplingFrequency != 48000 )
           )
            return false;

        if ( (layer != 1 ) &&
	     (layer != 2 )
           )
            return false;

        if ( (nChannels != 1 ) &&
	     (nChannels != 2 )
           )
            return false;

        // no support for mpeg dual channel
        if ( (encodingType == MPEG_DUAL_CHANNEL ) )
            return false;


        if ( (encodingType == MPEG_SINGLE_CHANNEL ) &&
	     (nChannels != 1 )
           )
            return false;

        if ( ( (encodingType == MPEG_STEREO       ) ||
               (encodingType == MPEG_JOINT_STEREO ) ) &&
	     (nChannels != 2 )
           )
            return false;

        if (layer == 1) {
            if (nChannels == 1) {
                for (int i=0;i<layer1BitRate_mono.length;i++) {
                    if (bitrate==layer1BitRate_mono[i]) {
                        return true;
                    }
                }
                return false;
            }

            {
                for (int i=0;i<layer1BitRate_stereo.length;i++) {
                    if (bitrate==layer1BitRate_stereo[i]) {
                        return true;
                    }
                }
                return false;
            }
        }
        // layer 2
        if (nChannels == 1) {
            for (int i=0;i<layer2BitRate_mono.length;i++) {
                if (bitrate==layer2BitRate_mono[i]) {
                    return true;
                }
            }
            return false;
        }

        {
            for (int i=0;i<layer2BitRate_stereo.length;i++) {
                if (bitrate==layer2BitRate_stereo[i]) {
                    return true;
                }
            }
            return false;
        }


    }

}
