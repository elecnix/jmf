/*
 * @(#)JavaDecoder.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio.ima4;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.sun.media.controls.*;


public class JavaDecoder extends com.ibm.media.codec.audio.AudioCodec {

    ////////////////////////////////////////////////////////////////////////////
    // Variables
    ////////////////////////////////////////////////////////////////////////////


    // state of the ima4 decoder
    private IMA4State ima4state;


    ////////////////////////////////////////////////////////////////////////////
    // Methods


    public JavaDecoder() {
	supportedInputFormats = new AudioFormat[] { new AudioFormat(AudioFormat.IMA4) };
        defaultOutputFormats  = new AudioFormat[] { new AudioFormat(AudioFormat.LINEAR) };
        PLUGIN_NAME="IMA4 Decoder";
   }



    protected  Format[] getMatchingOutputFormats(Format in) {
      AudioFormat af =(AudioFormat) in;

      supportedOutputFormats = new AudioFormat[] {
                 new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                16,
                af.getChannels(),
                AudioFormat.BIG_ENDIAN, //isBigEndian(),
                AudioFormat.SIGNED //isSigned());
                )
                };

        return  supportedOutputFormats;
    }



    /** Initializes the codec.  **/
    public void open() {
       ima4state=new IMA4State();

    }

    /** Clean up **/
    public void close() {
       ima4state = null;
    }



    /** decode the buffer  **/
    public int process(Buffer inputBuffer, Buffer outputBuffer) {
      int outLength;

      if (!checkInputBuffer(inputBuffer) ) {
         return BUFFER_PROCESSED_FAILED;
      }

      if (isEOM(inputBuffer) ) {
         propagateEOM(outputBuffer);
         return BUFFER_PROCESSED_OK;
      }

      int channels = ((AudioFormat)outputFormat).getChannels();
      byte[] inData =(byte[]) inputBuffer.getData();
      byte[] outData = validateByteArraySize(outputBuffer, inData.length * 4);

      outLength = decodeJavaIMA4(inData, outData, inputBuffer.getLength(),
                                 outData.length, channels);

      updateOutput(outputBuffer,outputFormat, outLength, 0);

	return BUFFER_PROCESSED_OK;
    }


    // java decoding methods //
    ///////////////////////////

    /** IMA4 decoding: sends the buffer to either decodeIMA4mono or decodeIMA4stereo **/
            int decodeJavaIMA4(byte[] inData, byte[] outData, int lenIn, int lenOut,int nChannels){
      switch (nChannels) {
       case 1: //mono
          return decodeIMA4mono  (inData,outData,lenIn,lenOut,0x20); // 0x20 is IMA4 chunk size
       case 2: //stereo
          return decodeIMA4stereo(inData,outData,lenIn,lenOut,0x20);
       default:
          throw new RuntimeException("IMA4: Can only handle 1 or 2 channels\n");

      }
    }
    /** decode IMA4 mono packet **/
    private int decodeIMA4mono(byte[] inData, byte[] outData, int lenIn, int lenOut,int blockSize){
      int inCount  = 0;
      int outCount = 0;

      // IMA4 mono chunk format is 2 bytes header followed by 32 bytes encoded data

      lenIn = (lenIn / (blockSize+2) ) * (blockSize+2);

      while (inCount<lenIn) {
        int state = (inData[inCount++] << 8);
            state |=  (inData[inCount++] & 0xff);

        // state is now prevVal(9 most significant bits- signed )::index (7 least significant bits- unsigned)

        int index = state & 0x7F;

        if (index>88)
            index=88;

        ima4state.valprev=state & 0xFFFFFF80 ;
        ima4state.index=index;

        IMA4.decode(inData,inCount,outData,outCount,blockSize<<1,ima4state,0);

        inCount  += blockSize;
        outCount += blockSize<<2;
      }

      return outCount;
    }
    /** decode IMA4 stereo packet **/
    private int decodeIMA4stereo(byte[] inData, byte[] outData, int lenIn, int lenOut,int blockSize){
      int inCount = 0;
      int outCount = 0;

      // System.out.println( lenIn);
      lenIn = (lenIn / 2 /(blockSize+2) ) * (blockSize+2)*2;

      // IMA4 stereo chunk format is left IMA4 mono chunk followed by right IMA4 mono chunk
      while (inCount<lenIn) {
        //LEFT
        int stateL = (inData[inCount++] << 8);
            stateL |=  (inData[inCount++] & 0xff);

        int indexL = stateL & 0x7F;

        if (indexL>88)
            indexL=88;

        ima4state.valprev=stateL & 0xFFFFFF80 ;
        ima4state.index=indexL;


        IMA4.decode(inData,inCount,outData,outCount,blockSize<<1,ima4state,2);

        inCount += blockSize;

        //RIGHT
        int stateR = (inData[inCount++] << 8);
        stateR |=  (inData[inCount++] & 0xff);

        int indexR = stateR & 0x7F;

        if (indexR>88)
            indexR=88;

        ima4state.valprev=stateR & 0xFFFFFF80 ;
        ima4state.index=indexR;


        IMA4.decode(inData, inCount, outData, outCount+2, blockSize<<1, ima4state, 2);

        //loop counters
        inCount += blockSize;
        outCount += blockSize<<3;
      }



      return outCount;
    }


    public java.lang.Object[] getControls() {
        if (controls==null) {
             controls=new Control[1];
             controls[0]=new SilenceSuppressionAdapter(this,false,false);
	}
        return (Object[])controls;
    }
}

