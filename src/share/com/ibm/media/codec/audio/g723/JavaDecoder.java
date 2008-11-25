/*
 * @(#)JavaDecoder.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio.g723;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.sun.media.controls.*;

/**
 * Implements an G723 codec which uses native methods to do the decoding.
 */
public class JavaDecoder extends com.ibm.media.codec.audio.AudioCodec {

    ////////////////////////////////////////////////////////////////////////////
    // Variables


    protected G723Dec decoder;
     ////////////////////////////////////////////////////////////////////////////
    // Methods
    public JavaDecoder() {
       	supportedInputFormats = new AudioFormat[] { new AudioFormat(AudioFormat.G723),
						    new AudioFormat(AudioFormat.G723_RTP)};
        defaultOutputFormats  = new AudioFormat[] { new AudioFormat(AudioFormat.LINEAR) };
        PLUGIN_NAME="G723 Decoder";
    }

    protected  Format[] getMatchingOutputFormats(Format in) {

        AudioFormat af =(AudioFormat) in;

        supportedOutputFormats = new AudioFormat[] {
                new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                16,
                1,
                AudioFormat.LITTLE_ENDIAN, //isBigEndian(),
                AudioFormat.SIGNED //isSigned());
                )                };
        return  supportedOutputFormats;
    }

    public void open() throws ResourceUnavailableException{
        decoder=new G723Dec();
        decoder.decoderOpen();
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

        // get offset for both RTP and non RTP streams
        decode(inpData, inputBuffer.getOffset(), outData, 0, inpLength);

        updateOutput(outputBuffer, outputFormat, outLength, 0);
        return BUFFER_PROCESSED_OK;
    }

    protected void initDecoder() {
        decoder.decoderReset();
    }

    protected void freeDecoder() {
 	decoder=null;
    }

    protected void resetDecoder() {
        decoder.decoderReset();
    }

    protected int calculateOutputSize(int inputSize) {
        return inputSize/24*480;
    }

    protected void decode(byte[] inpData,int readPtr,
                          byte[] outData,int writePtr,
			  int inpLength) {
        int numberOfFrames = (inpLength/24);
        int frameSize =24;
        int n;

        // loop for all G.723 frames in the current block ("Frame")
        for (n=0; n<numberOfFrames ; n++, readPtr += frameSize, writePtr += 2*240) {
            decoder.decodeFrame(inpData, readPtr,outData,writePtr);
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



