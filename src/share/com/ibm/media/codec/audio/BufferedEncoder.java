/*
 * @(#)BufferedEncoder.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio;

import javax.media.format.*;
import javax.media.format.*;
import javax.media.*;
import com.sun.media.*;


public abstract class BufferedEncoder extends AudioCodec {


    //actual read and write bytes by the native code
    protected int[] readBytes=new int[1];

    protected int[] writeBytes=new int[1];

    protected int[] frameNumber=new int[1];

    protected Buffer history = new Buffer();

    protected int[] regions;

    protected int[] regionsTypes;

    protected int pendingFrames;

    protected int packetSize=-1;

    public int getPacketSize() {
        return packetSize;
    }
    public int setPacketSize(int newPacketSize) {
        //sbd: add packet validity here
        packetSize=newPacketSize;
        return packetSize;
    }

    public void reset() {
        history.setLength(0);
        codecReset();
    }

    protected abstract void  codecReset();

    protected int historySize;

    public int process(Buffer inputBuffer, Buffer outputBuffer) {

        if (pendingFrames>0) {
	  //System.out.println("packetizing");
	  return BUFFER_PROCESSED_OK;
        }

        if (!checkInputBuffer(inputBuffer) ) {
            return BUFFER_PROCESSED_FAILED;
        }

        if (isEOM(inputBuffer) ) {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }

        int inpOffset=inputBuffer.getOffset();
	int inpLength=inputBuffer.getLength();
	int outLength = 0;
        int outOffset = 0;
        byte[] inpData = (byte[]) inputBuffer.getData();
        byte[] outData = validateByteArraySize(outputBuffer, calculateOutputSize(inpData.length + historySize) );
        int historyLength = history.getLength();
        byte[] historyData = validateByteArraySize(history, historySize);
        int framesNumber= calculateFramesNumber(inpData.length + historySize);

        if ( (regions == null) || (regions.length < framesNumber+1) )
            regions = new int[framesNumber + 1];

        if ( (regionsTypes == null) || (regionsTypes.length < framesNumber) )
            regionsTypes = new int[framesNumber];

        if (historyLength != 0) {
            int bytesToCopy = (historyData.length - historyLength);
            if (bytesToCopy > inpLength) {
                bytesToCopy = inpLength;
	    }

            System.arraycopy(inpData, inpOffset,
			     historyData,historyLength,
			     bytesToCopy);

            codecProcess(historyData, 0,
			 outData, outOffset,
			 historyLength + bytesToCopy ,
			 readBytes, writeBytes, frameNumber, regions, regionsTypes);

	    if (readBytes[0] <= 0) {
		if (writeBytes[0] <= 0) {
		    //System.err.println("Returning output buffer not filled");
		    return OUTPUT_BUFFER_NOT_FILLED;
		} else {
		    updateOutput(outputBuffer, outputFormat, writeBytes[0], 0);
		    //System.err.println("Returning OK");
		    return BUFFER_PROCESSED_OK;
		}
	    }		 

	    //System.out.println("1: "+inpLength+" "+readBytes[0]+" "+writeBytes[0]);

            outOffset += writeBytes[0];
            outLength += writeBytes[0];

            inpOffset += (readBytes[0]-historyLength);
            inpLength += (historyLength-readBytes[0]);

        }


        codecProcess(inpData,inpOffset, outData,outOffset,inpLength,readBytes,writeBytes,frameNumber,regions, regionsTypes);
        //System.out.println("2: "+inpLength+" "+readBytes[0]+" "+writeBytes[0]);


        // debug
        //for (int i=0; i<frameNumber[0];i++ ) {
	// System.out.println(i+" "+regions[i]+" - "+regions[i+1]+" type "+regionsTypes[i]);
        //}


        outLength += writeBytes[0];

        inpOffset += readBytes[0];
        inpLength -= readBytes[0];

        System.arraycopy (inpData,inpOffset,historyData,0,inpLength);
        history.setLength(inpLength);

        updateOutput(outputBuffer, outputFormat, outLength, 0);


        return BUFFER_PROCESSED_OK;
    }

    protected abstract int calculateOutputSize(int inputSize);

    protected abstract int calculateFramesNumber(int inputSize);

    protected abstract boolean codecProcess(byte[] inpData,int readPtr,
                                    byte[] outData,int writePtr,
  			            int inpLength,
				    int[]  readBytes,int[] writeBytes,
                                    int[] frameNumber,
				    int[] regions,int[] regiostypes);


}



