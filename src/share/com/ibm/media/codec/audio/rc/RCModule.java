/*
 * @(#)RCModule.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio.rc;

import javax.media.*;
import javax.media.format.AudioFormat;
import javax.media.Format;
import com.sun.media.*;


import java.io.*;

public class RCModule extends com.ibm.media.codec.audio.AudioCodec {

    private RateConversion rateConversion = null;
    private Format lastInputFormat=null;
    private Format lastOutputFormat=null;
    private static boolean DEBUG=false;

    public RCModule() {
        supportedInputFormats = new AudioFormat[] {
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
				16,
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
				),
                new AudioFormat(
				AudioFormat.LINEAR,
				Format.NOT_SPECIFIED,
				8,
				1,
				Format.NOT_SPECIFIED,
				Format.NOT_SPECIFIED
				)	};

	/*
	  supportedInputFormats = new AudioFormat[] {
	  new AudioFormat(
	  AudioFormat.LINEAR,
	  11025,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED
	  ),
	  new AudioFormat(
	  AudioFormat.LINEAR,
	  11127,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED
	  ),
	  new AudioFormat(
	  AudioFormat.LINEAR,
	  16000,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED
	  ),
	  new AudioFormat(
	  AudioFormat.LINEAR,
	  22050,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED
	  ),
	  new AudioFormat(
	  AudioFormat.LINEAR,
	  22254,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED
	  ),
	  new AudioFormat(
	  AudioFormat.LINEAR,
	  22255,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED
	  ),
	  new AudioFormat(
	  AudioFormat.LINEAR,
	  32000,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED
	  ),
	  new AudioFormat(
	  AudioFormat.LINEAR,
	  44100,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED
	  ),
	  new AudioFormat(
	  AudioFormat.LINEAR,
	  48000,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED,
	  Format.NOT_SPECIFIED
	  )	};
	*/


        defaultOutputFormats  =  new AudioFormat[] {
// 	    new AudioFormat(
// 			    AudioFormat.ULAW,
// 			    8000,
// 			    8,
// 			    1,
// 			    Format.NOT_SPECIFIED,
// 			    Format.NOT_SPECIFIED
// 			    ),
		new AudioFormat(
				AudioFormat.LINEAR,
				8000,
				16,
				2,
				AudioFormat.LITTLE_ENDIAN,
				AudioFormat.SIGNED
				),
                new AudioFormat(
				AudioFormat.LINEAR,
				8000,
				16,
				1,
				AudioFormat.LITTLE_ENDIAN,
				AudioFormat.SIGNED
				)
		};
        PLUGIN_NAME="Rate Conversion";
    }


    public Format setInputFormat(Format format) {

	if (!isSampleRateSupported(format)) return null;

	//System.err.println("INput format is " + format);
	return super.setInputFormat(format);

    }



    protected  Format[] getMatchingOutputFormats(Format in) {

	if (!isSampleRateSupported(in)) return new Format[0];

	supportedOutputFormats = new AudioFormat[] {
// 	    new AudioFormat(
// 			    AudioFormat.ULAW,
// 			    8000,
// 			    8,
// 			    1,
// 			    Format.NOT_SPECIFIED,
// 			    Format.NOT_SPECIFIED
// 			    ),

		new AudioFormat(
				AudioFormat.LINEAR,
				8000,
				16,
				1,
				AudioFormat.LITTLE_ENDIAN,
				AudioFormat.SIGNED
				),
                new AudioFormat(
				AudioFormat.LINEAR,
				8000,
				16,
				2,
				AudioFormat.LITTLE_ENDIAN,
				AudioFormat.SIGNED
				)
		};

	return  supportedOutputFormats;
    }


    public void open() throws ResourceUnavailableException {
	//System.err.println("RCModule output is " + outputFormat);
    }

    public void reset() {
	if (null != rateConversion) {
	    rateConversion.reset();
	}
    }

    public void close() {
	if (null != rateConversion) {
	    rateConversion.close();
	}

	rateConversion = null;
    }


    public int process(Buffer inputBuffer, Buffer outputBuffer) {


	if (!checkInputBuffer(inputBuffer) ) {
            return BUFFER_PROCESSED_FAILED;
	}

	if (isEOM(inputBuffer) ) {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
	}

	int inputLength = inputBuffer.getLength();

	if ( (lastInputFormat != inputFormat) || (lastOutputFormat != outputFormat) ||
	     (rateConversion == null) ) {
	    if (false == initConverter((AudioFormat)inputFormat,
				       (AudioFormat)outputFormat,inputLength)) {
		return BUFFER_PROCESSED_FAILED;
	    }
	}

	int maxOutLength = rateConversion.getMaxOutputLength(inputLength);
	byte[] inputData = (byte[]) inputBuffer.getData();
	byte[] outData = validateByteArraySize(outputBuffer, maxOutLength);

	int outLength = rateConversion.process(inputData,inputBuffer.getOffset(),
					       inputLength,outData,outputBuffer.getOffset());

	updateOutput(outputBuffer, outputFormat, outLength, outputBuffer.getOffset());
	//System.err.println(outputBuffer.getFormat());
	return BUFFER_PROCESSED_OK;

    }


    private boolean isSampleRateSupported(Format format) {
	try {
	    int sampleRate = (int) (((AudioFormat)format).getSampleRate());

	    // these are the only supported rates
	    if ( !( (sampleRate == 11025) || (sampleRate == 11127) ||
		    (sampleRate == 16000) || (sampleRate == 22050) ||
		    (sampleRate == 22254) || (sampleRate == 22255) ||
		    (sampleRate == 32000) || (sampleRate == 44100) ||
		    (sampleRate == 48000) ) ) {
		if (DEBUG) {
		    System.out.println("RCModule - input format sampling rate isn't supported");
		}
		return false; // input sampling rate doesn't match
	    }

	}
	catch (Throwable t) { // in case that the format isn't instance of AudioFormat
	    return false;
	}

	/*
	  int numberOfInputChannels=((AudioFormat)format).getChannels();
	  int inputSampleSize=((AudioFormat)format).getSampleSizeInBits();

	  if ( ( (numberOfInputChannels != 1) && (numberOfInputChannels != 2) ) || ( (inputSampleSize != 16) && (inputSampleSize != 8) ) ) {
          System.out.println("input format doesn't match");
          false; // input format doesn't match
	  }
	*/

	return true;

    }


    private boolean initConverter(AudioFormat inFormat,AudioFormat outFormat,int inputLength) {

	lastInputFormat=inFormat;
	lastOutputFormat=outFormat;

	boolean isSigned=false;
	int numberOfInputChannels=inFormat.getChannels();
	int numberOfOutputChannels=outFormat.getChannels();
	int inputSampleSize=inFormat.getSampleSizeInBits();
	int sampleRate=(int)inFormat.getSampleRate();
	boolean ulawOutput=false;


	if (sampleRate == 8000) {
	    return false;
	}

	int pcmType = RateConversion.RATE_CONVERSION_LITTLE_ENDIAN_FORMAT;

	if (inFormat.getEndian() == AudioFormat.BIG_ENDIAN) {
	    pcmType = RateConversion.RATE_CONVERSION_BIG_ENDIAN_FORMAT;
	}

	if (8 == inputSampleSize) {
	    pcmType = RateConversion.RATE_CONVERSION_BYTE_FORMAT;
	}

        if (inFormat.getSigned()==AudioFormat.SIGNED) {
	    isSigned=true;
        }


        if (rateConversion != null) {
	    close();
        }


        if (outFormat.getEncoding()==AudioFormat.ULAW) {
	    ulawOutput=true;
        }

	rateConversion = new RateConversion();
        if (RateConversion.RATE_CONVERSION_OK != rateConversion.init(inputLength,
								     sampleRate,
								     8000,
								     numberOfInputChannels,
								     numberOfOutputChannels,
								     pcmType,
								     isSigned,
								     ulawOutput) ) {
	    rateConversion = null;
	    return false;

	}


	return true;
    }

}


