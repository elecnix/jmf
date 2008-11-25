/*
 * @(#)NativeEncoder.java	1.18 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio.g723;

import javax.media.Buffer;
import javax.media.ResourceUnavailableException;
import javax.media.Format;
import javax.media.format.*;
import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;


public class NativeEncoder extends  com.ibm.media.codec.audio.BufferedEncoder {

    ////////////////////////////////////////////////////////////////////////////
    // Variables
    ////////////////////////////////////////////////////////////////////////////

    // variable used by native code to store a pointer to the C++ class
    int nativeData;

    ////////////////////////////////////////////////////////////////////////////
    // Native methods

    // initialize the native codec
    private native void initNative();

    // free any buffers allocated by the native codec
    private native void freeNative();

    // free any buffers allocated by the native codec
    private native void resetNative();

    protected native boolean  codecProcess(byte[] inpData,int readPtr,
                                    byte[] outData,int writePtr,
  			            int inpLength,
				    int[]  readBytes,int[] writeBytes,
                                    int[]  frameNumber,
				    int[] regions,int[] regionsTypes) ;

//    private native boolean encodeNative(byte[] inpBuffer,int readPtr,
//                                        byte[] outBuffer,int writePtr,
//					int inpLength);

    // for G723, RTP, we will send out 240 samples compressed in a single packet
    private int sample_count = 240;

    private long currentSeq = (long) (System.currentTimeMillis() * Math.random
());

    // current timestamp on RTP format packets.
    //private long timestamp = (long) (System.currentTimeMillis() * Math.random());
    private long timestamp = 0L;

  // the buffer used by the RTP packetization method
  byte[] pendingBuffer = null;
    ////////////////////////////////////////////////////////////////////////////
    // Methods


    public NativeEncoder() {
       	supportedInputFormats = new AudioFormat[] {
	                        new AudioFormat(AudioFormat.LINEAR,
					        Format.NOT_SPECIFIED,
						16,
						1,
						AudioFormat.LITTLE_ENDIAN,
						AudioFormat.SIGNED,
                                                Format.NOT_SPECIFIED,
                                                Format.NOT_SPECIFIED,
                                                Format.byteArray
                                ) };

        defaultOutputFormats  = new AudioFormat[] {
	                        new AudioFormat(AudioFormat.G723)  /*,
			        new AudioFormat(AudioFormat.G723_RTP,
						8000,
						Format.NOT_SPECIFIED,
						1)*/};

        PLUGIN_NAME="G723 Encoder";
        historySize = 480;
	pendingFrames = 0;
	// for RTP, set default packetsize to 160 audiosamples for a
	// default ms/packet of 20ms. This works out to 33 octets.
	//note if this value changes in setPacketSize(), sample count
	// needs to be updated as well.
	packetSize = 24;
    }

    protected  Format[] getMatchingOutputFormats(Format in) {

        AudioFormat af =(AudioFormat) in;

        supportedOutputFormats = new AudioFormat[] {
                new AudioFormat(
                    AudioFormat.G723,
                    af.getSampleRate(),
                    Format.NOT_SPECIFIED,
                    1,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    24*8,
                    Format.NOT_SPECIFIED,
                    Format.byteArray
                ) /*,

                new AudioFormat(
                    AudioFormat.G723_RTP,
                    8000,
                    Format.NOT_SPECIFIED,
		    1,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    24*8,
                    Format.NOT_SPECIFIED,
                    Format.byteArray
                )   */

        };
        return  supportedOutputFormats;
    }


    /** Initializes the codec.  **/
    public void open() throws ResourceUnavailableException {
    	try {
            JMFSecurityManager.loadLibrary("jmutil");
            JMFSecurityManager.loadLibrary("jmg723");
            initNative();
            return;

        } catch (Throwable t) {
            // can't load native implementation
            System.err.println("can not load "+PLUGIN_NAME);
            System.err.println("reason : "+t);
            throw new ResourceUnavailableException("can not load "+PLUGIN_NAME);
	}


    }

    /** Clean up **/
    public void close() {
        freeNative();
    }

    public void codecReset() {
        resetNative();
    }
  public int process(Buffer inputBuffer, Buffer outputBuffer){
    // let the buffered encoder process the input buffer. this will
    // encode the data for us.Only RTP packetization will be desired
    // at this time.
    int retVal = super.process(inputBuffer, outputBuffer);

    // if it is RTP, packetize the data.
    if (outputFormat.getEncoding().equals(AudioFormat.G723_RTP)){

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

      // start packetizing one frame at a time (240 samples)
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

    }//end of G723_RTP

    return retVal;
  }
/* sbd:// removed RTP support
    public int process(Buffer inputBuffer, Buffer outputBuffer) {

        if (!checkInputBuffer(inputBuffer) ) {
            return BUFFER_PROCESSED_FAILED;
        }

        if (isEOM(inputBuffer) ) {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }

	int inpLength = 0;
	int outLength = 0;
	byte[] inpData = null;
	byte[] outData = null;

	if (outputFormat.getEncoding().equals(AudioFormat.G723_RTP)){
	    // 480 bytes is the size of the input buffer we can handle
	    // at one time. If the input frame size is less than or
	    // equal to this, we will not be filling output buffer in a
	    // single process() run.
	    // If the input frame size is greter, we will not be
	    // consuming the input buffer in a single process run
	    inpLength = sample_count * inputFormat.getSampleSizeInBits()/8;

	    if (inputBuffer.getLength() <= inpLength){
		inpData = (byte[]) inputBuffer.getData();

		outLength = calculateOutputSize(inpLength);
		outData = validateByteArraySize(outputBuffer, outLength);

		// length of data returned by decoding method
		int outDecoded = calculateOutputSize( inputBuffer.getLength());

		// entire input buffer is going to be encoded
		encodeNative(inpData, inputBuffer.getOffset(),
			     outData, outputBuffer.getOffset(),
			     inputBuffer.getLength());

		// set output buffer's length and offset to reflect
		// compressed data.
		outputBuffer.setOffset(outputBuffer.getOffset() + outDecoded);

		updateOutput(outputBuffer, outputFormat, outLength,
			     outputBuffer.getOffset());

		if (outputBuffer.getOffset() >= outputBuffer.getLength()){
		    outputBuffer.setSequenceNumber(currentSeq++);
		    outputBuffer.setTimeStamp(timestamp);
		    timestamp+=sample_count;
		    return BUFFER_PROCESSED_OK;
		}
		return OUTPUT_BUFFER_NOT_FILLED;
	    }// end of if (inputBuffer.getLength <= inpLength){
	    if (inputBuffer.getLength() > inpLength){
		// currently, our input frame size will not be larger
		// than 480 bytes since our inputFormat specifies this
		// as a requirement. Do we need to put this in if
		// frameSizeInBits() becomes a multiple of 160 samples?
	    }
	}
	inpLength=inputBuffer.getLength();
	outLength = calculateOutputSize(inputBuffer.getLength() );

        inpData = (byte[]) inputBuffer.getData();
        outData = validateByteArraySize(outputBuffer, outLength);

        encodeNative(inpData,inputBuffer.getOffset(), outData,0,inpLength);

        updateOutput(outputBuffer, outputFormat, outLength, 0);
        return BUFFER_PROCESSED_OK;
    }
     */

    protected int calculateOutputSize(int inputSize) {
        return calculateFramesNumber(inputSize) * 24 ;
    }

    protected int calculateFramesNumber(int inputSize) {
        return inputSize / 480;
    }

}

