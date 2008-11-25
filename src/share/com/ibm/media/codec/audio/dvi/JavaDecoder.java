/*
 * @(#)JavaDecoder.java	1.11 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.ibm.media.codec.audio.dvi;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.sun.media.controls.*;

/** DVI to PCM java decoder **/
public class JavaDecoder extends com.ibm.media.codec.audio.AudioCodec {

    ////////////////////////////////////////////////////////////////////////////
    // Variables
    ////////////////////////////////////////////////////////////////////////////


    // state of the ima4 decoder
    private DVIState dviState;

    ////////////////////////////////////////////////////////////////////////////
    // Methods


    public JavaDecoder() {
	// EP:removed DVI as supported format since currently we dont use
	// this decoder or dvi. it is only used for RTP and dont want
	// to advertise a format we dont know how to handle here.  
	supportedInputFormats = new AudioFormat[] { new AudioFormat (AudioFormat.DVI_RTP)};
        defaultOutputFormats  = new AudioFormat[] { new AudioFormat(AudioFormat.LINEAR) };
        PLUGIN_NAME="DVI Decoder";

    }


    protected  Format[] getMatchingOutputFormats(Format in) {
      AudioFormat af =(AudioFormat) in;

      supportedOutputFormats = new AudioFormat[] {
                new AudioFormat(AudioFormat.LINEAR,
                af.getSampleRate(),
                16,
                af.getChannels(),
                AudioFormat.LITTLE_ENDIAN, //isBigEndian(),
                AudioFormat.SIGNED)        //isSigned());
                };

        return  supportedOutputFormats;
    }


    /** Initializes the codec.  **/
    public void open() {
	    dviState=new DVIState();
 	 }


    /** Clean up **/
    public void close() {
	    dviState = null;
  	}


    /** decode the buffer  **/
    public int process(Buffer inputBuffer, Buffer outputBuffer) {
       int outLength=0;

       if (!checkInputBuffer(inputBuffer) ) {
           return BUFFER_PROCESSED_FAILED;
       }

       if (isEOM(inputBuffer) ) {
           propagateEOM(outputBuffer);
           return BUFFER_PROCESSED_OK;
       }

       int channels = ((AudioFormat)outputFormat).getChannels();
       byte[] inData =(byte[]) inputBuffer.getData();
       // if the encoding is DVI_RTP, the data of the buffer contains
       // the RTP header, payload header and dvi payload. The offset
       // of the buffer is set to point to the dvi payload header. The
       // length of the buffer is the length including the payload header.In
       // case of DVI, the payload header is 4 bytes in length. 

       // the actual dvi payload length is Buffer.getLength - size
       // of dvi payload header i.e. 4 bytes.
       byte[] outData = validateByteArraySize(outputBuffer,
					      (inData.length -4) * 4);
       // get the dviState values from the DVI payload header
       int offset = inputBuffer.getOffset();

       // first 16 bits is the predicted value
       int prevVal = (inData[offset++] << 8);
       prevVal  |= (inData[offset++] & 0xff);

       //next 8 bits is the index
       int index = (inData[offset++] & 0xff);

       // next 8 bits is reserved adn to be ignored
       offset++;
       dviState.valprev = prevVal;
       dviState.index = index;
       // data to be decoded: input offset must point to actual
       // payload offset and length.
       DVI.decode(inData,
		  offset,
		  outData,
		  0,
		  2 * (inputBuffer.getLength() - 4),
		  dviState);

       // make room for the worst case input
       outLength=4* (inputBuffer.getLength() - 4);
       updateOutput(outputBuffer,outputFormat, outLength, 0);

       return BUFFER_PROCESSED_OK;
    }

    public java.lang.Object[] getControls() {
        if (controls==null) {
             controls=new Control[1];
             controls[0]=new SilenceSuppressionAdapter(this,false,false);
	}
        return (Object[])controls;
    }

}


