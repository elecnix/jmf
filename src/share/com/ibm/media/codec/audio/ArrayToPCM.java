/**
 * @(#)ArrayToPCM.java	1.8 02/08/21
 */

package com.ibm.media.codec.audio;

import javax.media.format.*;
import javax.media.format.*;

import com.sun.media.*;
import javax.media.*;

public class ArrayToPCM extends  BasicCodec{
    private static String ArrayToPCM="ArrayToPCM";


    public String getName() {
	return ArrayToPCM;
    }

    public ArrayToPCM() {
	inputFormats = new Format[] {new AudioFormat(AudioFormat.LINEAR,
						     Format.NOT_SPECIFIED,
						     Format.NOT_SPECIFIED,
						     Format.NOT_SPECIFIED,
						     Format.NOT_SPECIFIED,
						     Format.NOT_SPECIFIED,
						     Format.NOT_SPECIFIED,
						     Format.NOT_SPECIFIED,
						     Format.byteArray) };
	
	outputFormats = new Format[] {new AudioFormat(AudioFormat.LINEAR,
						      Format.NOT_SPECIFIED,
						      16,
						      Format.NOT_SPECIFIED,
						      Format.NOT_SPECIFIED,
						      Format.NOT_SPECIFIED,
						      Format.NOT_SPECIFIED,
						      Format.NOT_SPECIFIED,
						      Format.shortArray) };
    }

    public Format [] getSupportedOutputFormats(Format in) {
	if (! (in instanceof AudioFormat) )
	    return outputFormats;
	AudioFormat iaf=(AudioFormat) in;
	if (!(iaf.getEncoding().equals(AudioFormat.LINEAR)) ||
	    (iaf.getDataType()!=Format.byteArray) )
	    return new Format[0];

	AudioFormat oaf = new AudioFormat (AudioFormat.LINEAR,
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

	if (isEOM(inputBuffer) )
	    return BUFFER_PROCESSED_OK;

	byte[] inBuffer = (byte [])inputBuffer.getData();
	AudioFormat inFormat = (AudioFormat) inputBuffer.getFormat();
	boolean isSigned    = inFormat.getSigned() == AudioFormat.SIGNED ;
	boolean isBigEndian = inFormat.getEndian() == AudioFormat.BIG_ENDIAN;
	int sampleSize      = (inFormat.getFrameSizeInBits()+7)>>3;

	int inLength    = inputBuffer.getLength();
	// support only for 8 and 16 bits
	int samplesNumber = (sampleSize == 1) ? inLength : (inLength>>1);
	int outLength     = samplesNumber;

	short[] outBuffer = validateShortArraySize(outputBuffer, outLength);

	// == main
	int offset = isSigned ? 0 : 32768;

	int inOffset=0;
	int outOffset=0;


	if (sampleSize == 1) {
	    // signed + unsigned 8 bit
	    for (int i=samplesNumber-1;i>=0;i--) {
		outBuffer[i]=(short)((inBuffer[i]<<8) + offset);
	    }

	}  else {
	    // signed + unsigned 16 bit
	    if (isBigEndian) {
		for (int i=samplesNumber-1; i>=0; i--){
		    int sample1 = inBuffer[inOffset++] << 8;
		    int sample2 = inBuffer[inOffset++] & 0xff;
		    outBuffer[outOffset++] = (short)((sample1 | sample2) + offset);
		}
	    } else {
		for (int i=samplesNumber-1; i>=0; i--){
		    int sample1 = inBuffer[inOffset++] & 0Xff;
		    int sample2 = inBuffer[inOffset++] << 8;
		    outBuffer[outOffset++] = (short)((sample1 | sample2) + offset);
		}
	    }
	}


	// == epilog
	outputBuffer.setLength(samplesNumber);
	outputBuffer.setFormat(outputFormat);


	return BUFFER_PROCESSED_OK;
    }

    public static void main (String[] args) {
	Codec codec=new ArrayToPCM();
	Format[] ifmt=codec.getSupportedInputFormats();
	Format[] ofmt=codec.getSupportedOutputFormats(new AudioFormat(AudioFormat.LINEAR,
								      8000, 8, 2,
								      AudioFormat.LITTLE_ENDIAN,
								      AudioFormat.UNSIGNED));
	for (int i=0; i<ifmt.length ; i++) {
	    System.out.println(ifmt[i]);
	}
	System.out.println("* out *");

	for (int i=0; i<ofmt.length ; i++) {
	    System.out.println(ofmt[i]);
	}

    }
}
