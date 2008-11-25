/*
 * @(#)JavaDecoder_ms.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio.ima4;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.sun.media.controls.*;


public class JavaDecoder_ms extends com.ibm.media.codec.audio.AudioCodec {

    ////////////////////////////////////////////////////////////////////////////
    // Variables
    ////////////////////////////////////////////////////////////////////////////


    // state of the ima4 decoder
    private IMA4State ima4state;


    ////////////////////////////////////////////////////////////////////////////
    // Methods


    public JavaDecoder_ms() {
	supportedInputFormats = new AudioFormat[] { new AudioFormat(AudioFormat.IMA4_MS) };
        defaultOutputFormats  = new AudioFormat[] { new AudioFormat(AudioFormat.LINEAR) };
        PLUGIN_NAME="IMA4 MS Decoder";
    }

    protected  Format[] getMatchingOutputFormats(Format in) {
      AudioFormat af =(AudioFormat) in;

      // sbd: check for correct frame size
      int fs=af.getFrameSizeInBits();
      int channels= af.getChannels();

      if ( ( fs % (8*4*channels) ) != 0 )
           return new Format[0];

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

      int blockSize=((AudioFormat)inputFormat).getFrameSizeInBits() >> 3;

		outLength = decodeJavaMSIMA4(inData, outData, inputBuffer.getLength(),
                                 outData.length, channels,blockSize );


      updateOutput(outputBuffer,outputFormat, outLength, 0);

	return BUFFER_PROCESSED_OK;
    }

    // java decoding methods //
    ///////////////////////////


    private int decodeJavaMSIMA4(byte[] inBuffer, byte[] outBuffer, int lenIn, int lenOut,int nChannels, int blockSize){
	switch (nChannels) {
	case 1: //mono
	    return decodeMSIMA4mono  (inBuffer,outBuffer,lenIn,lenOut,blockSize);
	case 2: //stereo
	    return decodeMSIMA4stereo(inBuffer,outBuffer,lenIn,lenOut,blockSize);
	default:
	    throw new RuntimeException("MSIMA4: Can only handle 1 or 2 channels\n");

	}
    }

    private int decodeMSIMA4mono(byte[] inBuffer, byte[] outBuffer, int lenIn, int lenOut,int blockSize){
	int inCount  = 0;
	int outCount = 0;

	lenIn=( lenIn / blockSize) *  blockSize;

	while (inCount<lenIn) {
	    // MSDVI header is 2 bytes for previous value,
	    // 1 byte for index
	    // 1 byte spare
	    // note that previous value is also written to the output
	    int prevVal=  (inBuffer[inCount++] & 0xff);
	    prevVal |= (inBuffer[inCount++] << 8);


	    int index = (inBuffer[inCount++] & 0xff);

	    if (index>88)
		index=88;

	    inCount++;

	    outBuffer[outCount++] = (byte)(prevVal>>8);
	    outBuffer[outCount++] = (byte)(prevVal   );


	    ima4state.valprev=prevVal ;
	    ima4state.index=index;

	    IMA4.decode(inBuffer,inCount,outBuffer,outCount,(blockSize-4)<<1,ima4state,0);

	    inCount += blockSize-4;

	    outCount += (blockSize-4)<<2;
	}

	return outCount;

    }

    private int decodeMSIMA4stereo(byte[] inBuffer, byte[] outBuffer, int lenIn, int lenOut,int blockSize){
	int inCount = 0;
	int outCount = 0;

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

	lenIn=( lenIn / blockSize) *  blockSize;

	for (int i=0;i<outBuffer.length;i++)
	    outBuffer[i]=(byte)0;

	while (inCount<lenIn  ) {
	    int storedinCount = inCount;
	    int storedoutCount = outCount;
	    //LEFT
	    int prevValL=  (inBuffer[inCount++] & 0xff);
	    prevValL |= (inBuffer[inCount++] << 8);

	    int indexL = (inBuffer[inCount++] & 0xff);

	    if (indexL>88)
		indexL=88;

	    inCount++;

	    outBuffer[outCount++] = (byte)(prevValL>>8);
	    outBuffer[outCount++] = (byte)(prevValL   );

	    outCount += 2;
	    inCount += 4;

	    ima4state.valprev=prevValL;
	    ima4state.index=indexL;

	    for (int i=blockSize-8;i>0;i-=8) {
		IMA4.decode(inBuffer,inCount,outBuffer,outCount,8,ima4state,2);

		inCount +=8;
		outCount += 32;
	    }

	    //RIGHT
	    inCount  = storedinCount + 4;
	    outCount = storedoutCount + 2;


	    int prevValR=  (inBuffer[inCount++] & 0xff);
	    prevValR |= (inBuffer[inCount++] << 8);

	    int indexR = (inBuffer[inCount++] & 0xff);

	    if (indexR>88)
		indexR=88;

	    inCount++;

	    outBuffer[outCount++] = (byte)(prevValR>>8);
	    outBuffer[outCount++] = (byte)(prevValR   );
	    ima4state.valprev=prevValR;
	    ima4state.index=indexR;

	    outCount += 2;
	    inCount += 4;

	    for (int i=blockSize-8;i>0;i-=8) {
		IMA4.decode(inBuffer,inCount,outBuffer,outCount,8,ima4state,2);

		inCount +=8;
		outCount += 32;
	    }



	    inCount = storedinCount + blockSize;
	    outCount = storedoutCount +  ((blockSize-8)<<2) + 4;


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

