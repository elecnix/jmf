/*
 * @(#)AudioPacketizer.java	1.5 99/04/01
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

package com.ibm.media.codec.audio;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.sun.media.controls.*;

/**
 * Implements an G723 Packetizer.
 */
public abstract class AudioPacketizer extends com.ibm.media.codec.audio.AudioCodec {

    ////////////////////////////////////////////////////////////////////////////
    // Variables

    protected byte [] history;

    protected int packetSize;

    protected int historyLength;

    protected int sample_count;

  //    protected long currentSeq = (long)
  //                             (System.currentTimeMillis() * Math.random() );

  // current timestamp on RTP format packets.
  //protected long timestamp =  (long)
  //                             (System.currentTimeMillis() * Math.random() );
  
  ////////////////////////////////////////////////////////////////////////////
      // Methods
      

    public synchronized int process(Buffer inputBuffer, Buffer outputBuffer) {

        int inpLength=inputBuffer.getLength();
        int outLength = packetSize;

        byte[] inpData = (byte[]) inputBuffer.getData();
        byte[] outData = validateByteArraySize(outputBuffer, outLength);


        if (inpLength+historyLength>=packetSize) {
            int copyFromHistory=Math.min(historyLength,packetSize);

            System.arraycopy(history, 0, outData, 0 ,copyFromHistory ) ;

            int remainingBytes=packetSize-copyFromHistory;
            System.arraycopy(inpData, inputBuffer.getOffset(), outData,historyLength,remainingBytes) ;

            historyLength -= copyFromHistory;
            inputBuffer.setOffset( inputBuffer.getOffset() + remainingBytes);
            inputBuffer.setLength( inpLength - remainingBytes);
	    
	    // EP: packetizers will not convert time to RTP
	    // units. This will be done in the RTP session manager and thus
	    // processor will use the media times as is.  
            //outputBuffer.setSequenceNumber(currentSeq++);
            //outputBuffer.setTimeStamp(timestamp);
            //timestamp+=sample_count;

            updateOutput(outputBuffer, outputFormat, outLength, 0);

            return INPUT_BUFFER_NOT_CONSUMED ;
        }

        if (inputBuffer.isEOM() ) { // last packet

            System.arraycopy(history, 0, outData, 0 ,historyLength ) ;
            System.arraycopy(inpData, inputBuffer.getOffset(), outData,historyLength,inpLength) ;

	    // EP: packetizers will not convert time to RTP
	    // units. This will be done in the RTP session manager and thus
	    // processor will use the media times as is. 
            //outputBuffer.setSequenceNumber(currentSeq++);
            //outputBuffer.setTimeStamp(timestamp);
            //timestamp+=sample_count;

            updateOutput(outputBuffer, outputFormat, inpLength + historyLength, 0);

            historyLength=0;

            return BUFFER_PROCESSED_OK;
        }

        //save remainder for next time
        System.arraycopy(inpData, inputBuffer.getOffset(), history, historyLength,inpLength) ;
        historyLength += inpLength;
        return OUTPUT_BUFFER_NOT_FILLED ;
    }

    public void reset() {
        historyLength=0;
    }


}


