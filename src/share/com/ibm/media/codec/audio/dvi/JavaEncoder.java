/*
 * @(#)JavaEncoder.java	1.12 99/04/01
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


package com.ibm.media.codec.audio.dvi;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import java.io.*;
import java.lang.Math;

/**
 * Implements an DVI codec which uses Java methods to do the decoding.
 */
public class JavaEncoder extends com.ibm.media.codec.audio.AudioPacketizer {

    ////////////////////////////////////////////////////////////////////////////
    // Variables
    ////////////////////////////////////////////////////////////////////////////
    private Buffer pcmBuffer=new Buffer();


    // state of the ima4 decoder
    private DVIState dviState=new DVIState();

    private long currentSeq = 0;

    ////////////////////////////////////////////////////////////////////////////
    // Methods

    public JavaEncoder() {

        packetSize=240;
       	supportedInputFormats = new AudioFormat[] {
	        new AudioFormat(
                    AudioFormat.LINEAR,
                    Format.NOT_SPECIFIED,
                    16,
		    1,
                    AudioFormat.LITTLE_ENDIAN,
                    AudioFormat.SIGNED,
      	            AudioFormat.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    Format.byteArray
                )
        } ;
        defaultOutputFormats  = new AudioFormat[] {
	  new AudioFormat(AudioFormat.DVI_RTP,
			  AudioFormat.NOT_SPECIFIED,
			  4, // 4 bits per sample
			  1,
			  AudioFormat.NOT_SPECIFIED,        //isBigEndian(),
			  AudioFormat.NOT_SPECIFIED,        //isSigned());
			  AudioFormat.NOT_SPECIFIED,
			  AudioFormat.NOT_SPECIFIED,
			  Format.byteArray)
	      };

        PLUGIN_NAME="DVI Encoder";


    }


    protected  Format[] getMatchingOutputFormats(Format in) {
      AudioFormat af =(AudioFormat) in;

      supportedOutputFormats = new AudioFormat[] {
	  new AudioFormat(AudioFormat.DVI_RTP,
			  af.getSampleRate(),
			  4, // 4 bits per sample
			  1,
			  AudioFormat.NOT_SPECIFIED,        //isBigEndian(),
			  AudioFormat.NOT_SPECIFIED,        //isSigned());
			  AudioFormat.NOT_SPECIFIED,
			  AudioFormat.NOT_SPECIFIED,
			  Format.byteArray)
	      };
      return  supportedOutputFormats;

    }

  public Format setOutputFormat(Format out){
    // depending on the freq of output format, we must set the packet
    // size to be equivalent to 60ms (will be changed to 40 or 20ms) 
    Format f = super.setOutputFormat(out);
    AudioFormat af = (AudioFormat)f;
    if (af.getSampleRate() == 8000)
      packetSize = 240;
    else if (af.getSampleRate() == 11025)
      packetSize = 330;
    else if (af.getSampleRate() == 22050)
      packetSize = 660;
    else if (af.getSampleRate() == 44100)
      packetSize = 1320;
    return f;
  }


    /** decode the buffer  **/
    public int process(Buffer inputBuffer, Buffer outputBuffer) {

       int rc = super.process(inputBuffer,pcmBuffer);
       if ( (rc & OUTPUT_BUFFER_NOT_FILLED) != 0)
          return rc;

       byte[] pcmData =(byte[]) pcmBuffer.getData();
       int inpLength =  pcmBuffer.getLength();


       int outLength = pcmBuffer.getLength()/4;
       byte[] outData = validateByteArraySize(outputBuffer,outLength + 4);

       // dump the DVI flags from dviState into the first 4 bytes of
       // every packet sent out. The 4 bytes comes from the size of
       // the dvi payload header in case of RTP
       outData[0] = (byte) (dviState.valprev >> 8);
       outData[1] = (byte) dviState.valprev;
       outData[2] = (byte) dviState.index;
       outData[3] = 0;

       DVI.encode(pcmData,
		  0,
		  outData,
		  4,
		  inpLength >> 1,
		  dviState);

       pcmBuffer.setOffset(0);
       pcmBuffer.setLength(0);

       outputBuffer.setSequenceNumber(currentSeq++);
       outputBuffer.setTimeStamp(pcmBuffer.getTimeStamp());

       updateOutput(outputBuffer,
		    outputFormat,
		    outLength+4,
		    0);

       return rc;
    }


    public void open() throws ResourceUnavailableException{
        dviState=new DVIState();

        setPacketSize(packetSize);
        reset();
    }

    public void reset() {
        super.reset();
        dviState.valprev=0;
        dviState.index=0;
        pcmBuffer.setOffset(0);
        pcmBuffer.setLength(0);

    }


    public  java.lang.Object[] getControls() {
        if (controls==null) {
             controls=new Control[2];
             controls[0]=new PacketSizeAdapter(this,packetSize,true);
             controls[1]=new com.sun.media.controls.SilenceSuppressionAdapter(this,false,false);
	}
        return (Object[])controls;
    }

    public synchronized void setPacketSize(int newPacketSize) {
        packetSize=newPacketSize*4;  // this is pcmBuffer packetSize

        sample_count = packetSize/2;

        if (history==null) {
            history=new byte[packetSize];
            return;
        }

        if (packetSize > history.length ) {
            byte[] newHistory=new byte[packetSize];
            System.arraycopy(history,0,newHistory,0,historyLength);
            history=newHistory;
        }
    }


}

class PacketSizeAdapter extends com.sun.media.controls.PacketSizeAdapter {
    public PacketSizeAdapter(Codec newOwner, int newPacketSize, boolean newIsSetable) {
        super(newOwner,newPacketSize,newIsSetable);
    }

    public int setPacketSize(int numBytes) {

        int numOfPackets=numBytes *2;

        if (numOfPackets < 10) {
            numOfPackets=10;
        }

        if (numOfPackets > 4000) {
            numOfPackets= 4000;
        }
        packetSize= numOfPackets /2;

        ((JavaEncoder)owner).setPacketSize(packetSize);

        return packetSize;
    }

}
