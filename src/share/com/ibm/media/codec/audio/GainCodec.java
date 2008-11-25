/**
 * @(#)GainCodec.java	1.9 02/08/21
 */

package com.ibm.media.codec.audio;

import javax.media.format.*;
import javax.media.format.*;

import com.sun.media.*;
import javax.media.*;

public class GainCodec extends  BasicCodec {
    private static String GainCodec="GainCodec";

    public float gain = 2.0F;

    public void setGain(float newGain){
	gain=newGain;
    }

    public String getName() {
	return GainCodec;
    }

    public GainCodec() {
	inputFormats = new Format[] {new AudioFormat(AudioFormat.LINEAR,
						     Format.NOT_SPECIFIED,
						     16,
						     Format.NOT_SPECIFIED,
						     Format.NOT_SPECIFIED,
						     Format.NOT_SPECIFIED,
						     Format.NOT_SPECIFIED,
						     Format.NOT_SPECIFIED,
						     Format.shortArray)};

	outputFormats= new Format[] {new AudioFormat(AudioFormat.LINEAR,
						     Format.NOT_SPECIFIED,
						     16,
						     Format.NOT_SPECIFIED,
						     Format.NOT_SPECIFIED,
						     Format.NOT_SPECIFIED,
						     Format.NOT_SPECIFIED,
						     Format.NOT_SPECIFIED,
						     Format.shortArray)};
    }

    public Format [] getSupportedOutputFormats(Format in) {
	if (! (in instanceof AudioFormat) )
	    return outputFormats;

	AudioFormat iaf=(AudioFormat) in;
	if (!(iaf.getEncoding().equals(AudioFormat.LINEAR)) ||
	    (iaf.getDataType()!=Format.shortArray) )
	    return new Format[0];

	AudioFormat oaf = new AudioFormat(AudioFormat.LINEAR,
					  iaf.getSampleRate(),
					  16,
					  iaf.getChannels(),
					  AudioFormat.LITTLE_ENDIAN,
					  AudioFormat.SIGNED,
					  iaf.getFrameSizeInBits(),
					  iaf.getFrameRate(),
					  Format.shortArray);
	return new Format[] {oaf};
    }

    public int process(Buffer inputBuffer, Buffer outputBuffer){
	// == prolog

	if (isEOM(inputBuffer) ) {
	    propagateEOM(outputBuffer);
	    return BUFFER_PROCESSED_OK;
	}

	short[] inBuffer       = (short[])inputBuffer.getData();
	int     inLength       = inputBuffer.getLength();
	int     inOffset       = inputBuffer.getOffset();
	int     samplesNumber  = inLength;

	short[] outBuffer    = validateShortArraySize(outputBuffer, samplesNumber);

	// == main

	for (int i=0; i< samplesNumber;i++) {
	    int sample = inBuffer[inOffset + i];
	    sample = (int)(sample * gain);
	    if (sample>32767)    // saturate
		sample = 32767;
	    else if (sample < -32768)
		sample = -32768;
	    outBuffer[i]=(short) sample;
	}

	// == epilog

	updateOutput(outputBuffer,outputFormat, samplesNumber, 0);


	return BUFFER_PROCESSED_OK;
    }

    public static void main (String[] args) {


	GainCodec codec=new GainCodec();
	Format[] ifmt=codec.getSupportedInputFormats();
	Format[] ofmt=codec.getSupportedOutputFormats(null);//new AudioFormat(AudioFormat.LINEAR,8000,8,2,false,false,0xffff));
	Buffer inp=new Buffer();

	Buffer out=new Buffer();
	short[] buffer=new short[100];
	for (int i=0;i<100;i++) {
	    buffer[i]=(short)(i+20500);
	}
	inp.setData(buffer);
	inp.setLength(10);
	inp.setOffset(0);
	codec.setGain(1.6F);
	int rc=codec.process(inp,out);

	System.out.println("rc="+rc);
	short[] outbuf=(short[])out.getData();
	System.out.println("length="+out.getLength());
	System.out.println("offset="+out.getOffset());
	for (int i=0;i<outbuf.length;i++) {
	    System.out.println(i+" "+outbuf[i]);
	}
	inp.setLength(0);
	inp.setEOM(true);
	rc=codec.process(inp,out);
	System.out.println("rc="+rc);
	outbuf=(short[])out.getData();
	System.out.println("length="+out.getLength());
	System.out.println("offset="+out.getOffset());
	for (int i=0;i<outbuf.length;i++) {
	    System.out.println(i+" "+outbuf[i]);
	}


	/*
	  for (int i=0; i<ifmt.length ; i++) {
	  System.out.println(ifmt[i]);
	  }
	  System.out.println("* out *");

	  for (int i=0; i<ofmt.length ; i++) {
	  System.out.println(ofmt[i]);
	  }
	*/
    }
}
