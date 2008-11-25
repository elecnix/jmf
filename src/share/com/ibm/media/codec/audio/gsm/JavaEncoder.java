/*
 * @(#)JavaEncoder.java	1.13 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio.gsm;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;

/**
 * RTP GSM encoder plugin wrapper, which uses Java methods to do the encoding.
 * @author Shay Ben-David bendavid@haifa.vnet.ibm.com
 */
public class JavaEncoder extends com.ibm.media.codec.audio.BufferedEncoder {

     ////////////////////////////////////////////////////////////////////////////
    // Variables

   /** <FONT COLOR="#FF0000">
    *  Licensed Materials - Property of IBM                         <br><br>
    *  "Restricted Materials of IBM"                                <br><br>
    *  5648-B81                                                     <br><br>
    *  (c) Copyright IBM Corporation 1997,1999 All Rights Reserved  <br><br>
    *  US Government Users Restricted Rights - Use, duplication or
    *  disclosure restricted by GSA ADP Schedule Contract with
    *  IBM Corporation.</FONT>
    *
    **/
    public static final String a_copyright_notice="(c) Copyright IBM Corporation 1997,1999.";

  protected GsmEncoder encoder;

  // the default RTP packet size for the outputformat in units of samples.
  // for gsm, 160 audio samples are compressed to 33 bytes. We want the
  //default millisec perpacket to be 20ms --> 160 audio samples --> 33 octets.
  private int sample_count = 160;
  
    private long currentSeq = (long) (System.currentTimeMillis() * Math.random
());

    // current timestamp on RTP format packets.
    //private long timestamp = (long) (System.currentTimeMillis() * Math.random());
    private long timestamp = 0L;

  // the buffer used by the RTP packetization method
  byte[] pendingBuffer = null;
  
     ////////////////////////////////////////////////////////////////////////////
    // Methods

    public JavaEncoder() {
       	supportedInputFormats = new AudioFormat[] {
	                                new AudioFormat(AudioFormat.LINEAR,
					        Format.NOT_SPECIFIED,
						16,
						1,
						AudioFormat.LITTLE_ENDIAN,
						AudioFormat.SIGNED,
                                                Format.NOT_SPECIFIED,
                                                Format.NOT_SPECIFIED,
                                                Format.byteArray) };

	defaultOutputFormats  = new AudioFormat[] {
	    new AudioFormat(AudioFormat.GSM) /*,
	    new AudioFormat(AudioFormat.GSM_RTP)*/};

        PLUGIN_NAME="GSM Encoder";
        historySize = 320;
	pendingFrames = 0;
	// for RTP, set default packetsize to 160 audiosamples for a
	// default ms/packet of 20ms. This works out to 33 octets.
	//note if this value changes in setPacketSize(), sample count
	// needs to be updated as well.
	packetSize = 33;

    }

    protected  Format[] getMatchingOutputFormats(Format in) {

        AudioFormat af =(AudioFormat) in;

        supportedOutputFormats = new AudioFormat[] {
                new AudioFormat(
                    AudioFormat.GSM,
                    af.getSampleRate(),
                    16,
                    af.getChannels(),
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    264,
                    Format.NOT_SPECIFIED,
                    Format.byteArray
                )/*,

                new AudioFormat(
                    AudioFormat.GSM_RTP,
		    af.getSampleRate(),
                    Format.NOT_SPECIFIED,
		    af.getChannels(),
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    264,
                    Format.NOT_SPECIFIED,
                    Format.byteArray
                )  */

        };
        return  supportedOutputFormats;
    }

    public void open() throws ResourceUnavailableException{
        encoder=new GsmVadEncoder();
        encoder.gsm_encoder_reset();
    }

    public void codecReset() {
        encoder.gsm_encoder_reset();
    }


    public void close() {
        encoder=null;
    }

  public int process(Buffer inputBuffer, Buffer outputBuffer){
    // let the buffered encoder process the input buffer. this will
    // encode the data for us.Only RTP packetization will be desired
    // at this time.
    int retVal = super.process(inputBuffer, outputBuffer);
    
    // if the output format is not RTP, just return the from here.
    if (!outputFormat.getEncoding().equals(AudioFormat.GSM_RTP))
      return retVal;

    // if it is RTP, packetize the data.
    if (outputFormat.getEncoding().equals(AudioFormat.GSM_RTP)){

      // before we proceed for packetization, check for failure in
      // encoding and EOM
      if (retVal == BUFFER_PROCESSED_FAILED)
	return retVal;
      if (isEOM(inputBuffer) ) {
	propagateEOM(outputBuffer);
	return BUFFER_PROCESSED_OK;
      }

      // Now, if there are no pending frames, we are beginning
      // packetization of a new buffer.get a handle over the buffer to
      // be packetized
      if (pendingFrames == 0)
	pendingBuffer = (byte[])outputBuffer.getData();

      // start packetizing one frame at a time (160 samples)
      // the size of outputdata depends on the packet size set.
      byte[] outData = new byte[packetSize];
      outputBuffer.setData(outData);
      updateOutput(outputBuffer, outputFormat,packetSize, 0);
      outputBuffer.setSequenceNumber(currentSeq++);
      outputBuffer.setTimeStamp(timestamp);
      timestamp+=sample_count;

      System.arraycopy(pendingBuffer,
		       regions[pendingFrames],
		       outData,
		       0,
		       packetSize);

      if (pendingFrames + 1== frameNumber[0]){
	pendingFrames = 0;
	pendingBuffer = null;
	return BUFFER_PROCESSED_OK;
      }else
	pendingFrames++;

      return INPUT_BUFFER_NOT_CONSUMED;

    }//end of GSM_RTP

    return retVal;
  }

  protected int calculateOutputSize(int inputSize) {
    return calculateFramesNumber(inputSize) * 33 ;
  }

  protected int calculateFramesNumber(int inputSize) {
    return inputSize / 320;
  }



    protected boolean codecProcess(byte[] inpData,int readPtr,
                                    byte[] outData,int writePtr,
  			            int inpLength,
				    int[]  readBytes,int[] writeBytes,
                                    int[]  frameNumber,
				    int[] regions,int[] regionsTypes) {

        int inCount = 0;
        int outCount = 0;
        int channels=inputFormat.getChannels();
        boolean isStereo = ( channels == 2);

        int frames = inpLength/(320);

        regions[0]=writePtr;

        for (int frameCounter = 0; frameCounter<frames ; frameCounter++) {
              encoder.gsm_encode_frame(inpData, readPtr , outData,writePtr);
              readPtr += 320;
              inCount += 320;

              outCount += 33;
              writePtr += 33;

	      //regions [frameCounter+1]= outCount + writePtr;
	      regions [frameCounter +1] = writePtr;
              regionsTypes[frameCounter  ]= 0;

	      //System.out.println(inCount+" "+outCount+" "+inpLength);
        }

        readBytes [0]=inCount;
        writeBytes[0]=outCount;
        frameNumber[0]=frames;

        return true;
    }




}



