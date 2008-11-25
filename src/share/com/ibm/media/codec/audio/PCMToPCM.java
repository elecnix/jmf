/*
 * @(#)PCMToPCM.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.sun.media.controls.*;


public class PCMToPCM extends com.ibm.media.codec.audio.AudioCodec {

  private Format lastInputFormat=null;
  private Format lastOutputFormat=null;
  private int bias=0;
  private int signMask=0;
  private int inputSampleSize=8;
  private int outputSampleSize=8;
  private int numberOfInputChannels=1;
  private int numberOfOutputChannels=1;
  private boolean channels2To1=false;
  private boolean channels1To2=false;
  private boolean channels2To2=false;


  private int inputLsbOffset;
  private int inputMsbOffset;

  private int outputLsbOffset;
  private int outputMsbOffset;




    ////////////////////////////////////////////////////////////////////////////
    // Methods


    public PCMToPCM() {
        supportedInputFormats = new AudioFormat[] {
            new AudioFormat(
                AudioFormat.LINEAR,
                Format.NOT_SPECIFIED,
                16,
                1,
                Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED
                ),
            new AudioFormat(
                AudioFormat.LINEAR,
                Format.NOT_SPECIFIED,
                16,
                2,
                Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED
                ),
            new AudioFormat(
                AudioFormat.LINEAR,
                Format.NOT_SPECIFIED,
                8,
                1,
                Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED
                ),
            new AudioFormat(
                AudioFormat.LINEAR,
                Format.NOT_SPECIFIED,
                8,
                2,
                Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED
                )	};  // support 1/2 channels and 8/16 bit samples


        defaultOutputFormats  = new AudioFormat[] { new AudioFormat(AudioFormat.LINEAR) };
        PLUGIN_NAME="PCM to PCM converter";
    }


    protected  Format[] getMatchingOutputFormats(Format in) {

        AudioFormat af =(AudioFormat) in;
	int otherChnl = (af.getChannels() == 1 ? 2 : 1);

        supportedOutputFormats = new AudioFormat[] {
	  // Converting to little endian, signed
            new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                16,
                af.getChannels(),
                AudioFormat.LITTLE_ENDIAN,
                AudioFormat.SIGNED,
		16 * af.getChannels(),
		af.getFrameRate(),
		af.getDataType()
                ),
            new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                16,
                otherChnl,
                AudioFormat.LITTLE_ENDIAN,
                AudioFormat.SIGNED,
		16 * otherChnl,
		af.getFrameRate(),
		af.getDataType()
                ),
	  // Converting to big endian, signed
            new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                16,
                af.getChannels(),
                AudioFormat.BIG_ENDIAN,
                AudioFormat.SIGNED,
		16 * af.getChannels(),
		af.getFrameRate(),
		af.getDataType()
                ),
            new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                16,
                otherChnl,
                AudioFormat.BIG_ENDIAN,
                AudioFormat.SIGNED,
		16 * otherChnl,
		af.getFrameRate(),
		af.getDataType()
                ),
	  // Converting to little endian, unsigned
            new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                16,
                af.getChannels(),
                AudioFormat.LITTLE_ENDIAN,
                AudioFormat.UNSIGNED,
		16 * af.getChannels(),
		af.getFrameRate(),
		af.getDataType()
                ),
            new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                16,
                otherChnl,
                AudioFormat.LITTLE_ENDIAN,
                AudioFormat.UNSIGNED,
		16 * otherChnl,
		af.getFrameRate(),
		af.getDataType()
                ),
	  // Converting to big endian, unsigned
            new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                16,
                af.getChannels(),
                AudioFormat.BIG_ENDIAN,
                AudioFormat.UNSIGNED,
		16 * af.getChannels(),
		af.getFrameRate(),
		af.getDataType()
                ),
            new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                16,
                otherChnl,
                AudioFormat.BIG_ENDIAN,
                AudioFormat.UNSIGNED,
		16 * otherChnl,
		af.getFrameRate(),
		af.getDataType()
                ),
	  // Converting to 8-bit, signed
            new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                8,
                af.getChannels(),
                Format.NOT_SPECIFIED,
                AudioFormat.SIGNED,
		8 * af.getChannels(),
		af.getFrameRate(),
		af.getDataType()
                ),
            new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                8,
                otherChnl,
                Format.NOT_SPECIFIED,
                AudioFormat.SIGNED,
		8 * otherChnl,
		af.getFrameRate(),
		af.getDataType()
                ),
	  // Converting to 8-bit, unsigned
            new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                8,
                af.getChannels(),
                Format.NOT_SPECIFIED,
                AudioFormat.UNSIGNED,
		8 * af.getChannels(),
		af.getFrameRate(),
		af.getDataType()
                ),
            new AudioFormat(
                AudioFormat.LINEAR,
                af.getSampleRate(),
                8,
                otherChnl,
                Format.NOT_SPECIFIED,
                AudioFormat.UNSIGNED,
		8 * otherChnl,
		af.getFrameRate(),
		af.getDataType()
                )   };


        return  supportedOutputFormats;
    }




  public int process(Buffer inputBuffer, Buffer outputBuffer) {


    if (!checkInputBuffer(inputBuffer) ) {
            return BUFFER_PROCESSED_FAILED;
    }

    if (isEOM(inputBuffer) ) {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
    }

    if ( (lastInputFormat != inputFormat) || (lastOutputFormat != outputFormat)  ){
        initConverter((AudioFormat)inputFormat,(AudioFormat)outputFormat);
    }

    int inpLength=inputBuffer.getLength();
    int outLength = calculateOutputSize(inputBuffer.getLength() );

    byte[] inpData = (byte[]) inputBuffer.getData();
    byte[] outData = validateByteArraySize(outputBuffer, outLength);

    convert(inpData, inputBuffer.getOffset(),inpLength, outData, outputBuffer.getOffset());

    updateOutput(outputBuffer, outputFormat, outLength, outputBuffer.getOffset());
    return BUFFER_PROCESSED_OK;

  }


  private int calculateOutputSize(int inputLength) {

    int outputLength=inputLength;

    if ( (inputSampleSize==8) && (outputSampleSize==16) ) {
       outputLength*=2;
    }

    if ( (inputSampleSize==16) && (outputSampleSize==8) ) {
       outputLength/=2;
    }

    if ( (numberOfInputChannels==1) && (numberOfOutputChannels==2) ) {
       outputLength*=2;
    }

    if ( (numberOfInputChannels==2) && (numberOfOutputChannels==1) ) {
       outputLength/=2;
    }

    return outputLength;

  }

  private void initConverter(AudioFormat inFormat,AudioFormat outFormat) {

     lastInputFormat=inFormat;
     lastOutputFormat=outFormat;


     numberOfInputChannels=inFormat.getChannels();
     numberOfOutputChannels=outFormat.getChannels();

     inputSampleSize=inFormat.getSampleSizeInBits();
     outputSampleSize=outFormat.getSampleSizeInBits();

     if ( (inFormat.getEndian()==AudioFormat.BIG_ENDIAN) || (8 == inputSampleSize) ) {
       inputLsbOffset = 1;
       inputMsbOffset=0;
     }
     else {
       inputLsbOffset = -1;
       inputMsbOffset=1;
     }


     int outputEndianess = outFormat.getEndian();
     if (outputEndianess==Format.NOT_SPECIFIED) {
         outputEndianess=inFormat.getEndian(); /* if the output endianess is not specified assume the input endianess */
     }

     if ( (outputEndianess==AudioFormat.BIG_ENDIAN) || (8 == outputSampleSize) ) {
       outputLsbOffset = 1;
       outputMsbOffset=0;
     }
     else {
       outputLsbOffset = -1;
       outputMsbOffset=1;
     }


     if (inFormat.getSigned()==AudioFormat.SIGNED) {
       signMask=0xffffffff;
     }
     else {
    	signMask=0x0000ffff;
     }

     if ( (inFormat.getSigned()==outFormat.getSigned() ) || (outFormat.getSigned()==Format.NOT_SPECIFIED) ) {
        bias=0; /* if the output sign is not specified assume the input sign */
     }
     else {
        bias=32768;
     }


     if ( (numberOfInputChannels==2) && (numberOfOutputChannels==1)  ) {
        channels2To1=true;
     }
     else
        channels2To1=false;

     if ( (numberOfInputChannels==1) && (numberOfOutputChannels==2)  ) {
        channels1To2=true;
     }
     else
        channels1To2=false;


     if ( (numberOfInputChannels==2) && (numberOfOutputChannels==2)  ) {
        channels2To2=true;
     }
     else
        channels2To2=false;

  }

  private void convert(byte[] input,int inputOffset,int inputLength,byte[] outData,int outputOffset) {

      int sample1=0;
      int sample2=0;
      int i;

      outputOffset+=outputMsbOffset;

      for(i=inputOffset+inputMsbOffset;i<(inputLength+inputOffset); ) {

      if (8 == inputSampleSize) {
           sample1 = input[i++]<<8;

           if (numberOfInputChannels==2) {
              sample2 = input[i++]<<8;
           }
      }
      else {
          sample1  = (input[i] << 8) + (0xff & input[i+inputLsbOffset]);
          i+=2;

           if (numberOfInputChannels==2) {
              sample2  = (input[i] << 8) + (0xff & input[i+inputLsbOffset]);
              i+=2;
           }
      }


      if (channels2To1)   // 2->1 , downmix the channels
       sample1 = ( (sample1&signMask) + (sample2&signMask) ) >> 1;

      // convert to signed samples

      sample1 = (int)( (short)(sample1+bias) );

      if (channels2To2) {    // 2->2
        sample2 = (int)( (short)(sample2+bias) );
      }

      if (channels1To2)     // 1->2 , duplicate the channel
         sample2 = sample1;



       // write the samples to the output

       if (8 == outputSampleSize) {
         outData[outputOffset++]=(byte)(sample1>>8);

         if (numberOfOutputChannels==2) {
           outData[outputOffset++]=(byte)(sample2>>8);
         }

       }
       else {
           outData[outputOffset+outputLsbOffset]=(byte) sample1;
           outData[outputOffset]=(byte) (sample1>>8);
           outputOffset+=2;
           if (numberOfOutputChannels==2) {
              outData[outputOffset+outputLsbOffset]=(byte) sample2;
              outData[outputOffset]=(byte) (sample2>>8);
              outputOffset+=2;
           }
        }

      }
    }

  }
