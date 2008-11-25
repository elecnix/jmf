/*
 * @(#)JavaDecoder.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio.alaw;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.sun.media.controls.*;

/** A-Law to PCM java decoder
 *  @Author Shay Ben-David bendavid@haifa.vnet.ibm.com
 **/
public class JavaDecoder extends com.ibm.media.codec.audio.AudioCodec {

    ////////////////////////////////////////////////////////////////////////////
    // Variables
    ////////////////////////////////////////////////////////////////////////////

    static private final byte[] lutTableL = new byte[256];

    static private final byte[] lutTableH = new byte[256];


    ////////////////////////////////////////////////////////////////////////////
    // Methods


    public JavaDecoder() {
	supportedInputFormats = new AudioFormat[] { new AudioFormat(AudioFormat.ALAW) };
        defaultOutputFormats  = new AudioFormat[] { new AudioFormat(AudioFormat.LINEAR) };
        PLUGIN_NAME="A-Law Decoder";
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
                )
                };
        return  supportedOutputFormats;
    }


    /** Initializes the codec.  **/
    public void open() {
     initTables();
    }


    /** decode the buffer  **/
    public int process(Buffer inputBuffer, Buffer outputBuffer) {

      if (!checkInputBuffer(inputBuffer) ) {
         return BUFFER_PROCESSED_FAILED;
      }

      if (isEOM(inputBuffer) ) {
         propagateEOM(outputBuffer);
         return BUFFER_PROCESSED_OK;
      }

      int channels = ((AudioFormat)outputFormat).getChannels();
      byte[] inData =(byte[]) inputBuffer.getData();
      byte[] outData = validateByteArraySize(outputBuffer, inData.length * 2);

      int inpLength=inputBuffer.getLength();
      int outLength=2*inpLength;

      int inOffset=0;
      int outOffset=0;
      for (int i=0;i<inpLength;i++) {
        int temp=inData[inOffset++]&0xff;
        outData[outOffset++]=lutTableL[temp];
        outData[outOffset++]=lutTableH[temp];
      }

      updateOutput(outputBuffer,outputFormat, outLength, 0);

	return BUFFER_PROCESSED_OK;
    }


   private void initTables (){
      for (int i=0;i<256;i++) {
        int input     = i ^ 0x55;
        int mantissa  = (input & 0xf ) << 4;
        int segment   = (input & 0x70) >> 4;
        int value     = mantissa+8;

        if (segment>=1)
         value+=0x100;
        if (segment>1)
         value <<= (segment - 1);

        if ( (input & 0x80)==0 )
         value = -value;

        lutTableL[i]=(byte)value;
        lutTableH[i]=(byte)(value>>8);

      }
   }

    public java.lang.Object[] getControls() {
        if (controls==null) {
             controls=new Control[1];
             controls[0]=new com.sun.media.controls.SilenceSuppressionAdapter(this,false,false);
	}
        return (Object[])controls;
    }

}

