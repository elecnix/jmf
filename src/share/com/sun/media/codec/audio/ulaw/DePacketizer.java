/*
 * 
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

package com.sun.media.codec.audio.ulaw;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import java.lang.Math;

public class DePacketizer extends com.sun.media.codec.audio.AudioCodec {

    public DePacketizer() {
	   inputFormats = new Format[] { new AudioFormat(AudioFormat.ULAW_RTP) };
    }


    public String getName() {
      return "ULAW DePacketizer";
    }


    public Format [] getSupportedOutputFormats(Format in) {

      if (in == null)
          return new Format[] { new AudioFormat(AudioFormat.ULAW) };

      if (matches(in, inputFormats) == null)
          return new Format[1];

      if (! (in instanceof AudioFormat) )
          return new Format[] { new AudioFormat(AudioFormat.ULAW) };

      AudioFormat af =(AudioFormat) in;
      return new Format[] {  new AudioFormat(
                AudioFormat.ULAW,
                af.getSampleRate(),
		af.getSampleSizeInBits(),
                af.getChannels())
          };
    }



    /** Initializes the codec.  **/
    public void open() {

    }

    /** Clean up **/
    public void close() {
	
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

	Object outData = outputBuffer.getData();
	outputBuffer.setData(inputBuffer.getData());
	inputBuffer.setData(outData);
	outputBuffer.setLength(inputBuffer.getLength());
	outputBuffer.setFormat(outputFormat);
	outputBuffer.setOffset(inputBuffer.getOffset());
	return BUFFER_PROCESSED_OK;
	
	
    }
    
}

