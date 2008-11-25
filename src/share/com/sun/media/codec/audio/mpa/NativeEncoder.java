/*
 * @(#)NativeEncoder.java	1.14 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.audio.mpa;


import javax.media.*;
import javax.media.format.AudioFormat;
import javax.media.ResourceUnavailableException;
import com.sun.media.util.Arch;
import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;
import com.sun.media.controls.BitRateAdapter;
import java.util.Hashtable;


public class NativeEncoder extends com.sun.media.codec.audio.AudioCodec {

    private int nativeData = 0;

    // These are initialized once in the native code.
    protected int INSIZE = 0;
    protected int OUTSIZE = 0;

    // Process buffer.  The encoder takes only byte array input of
    // size INSIZE.  So we'll have to accumulate enough
    // data before feeding it into the encoder.
    // The array cannot be initialize now since INSIZE has not been
    // initialized yet.
    byte encData[];
    int encDataLen = 0;

    BitRateControl bra;
    Control controls[];

    boolean settingsChanged = false;
    
    private int numFramesPerOutputBuffer = 2;
    private int numFramesInBuffer = 0;
    public native int nOpen(AudioFormat format);

    public native boolean nClose(int instance);

    public native int nConvert(int instance,
				  byte [] inBuf, int inOffset, int inLen,
				  byte [] outBuf, int outOffset);

    public native int nFlush(int instance, byte[] outBuf);

    private int outOffset = 0;

    static Hashtable bitRateTable = new Hashtable();

    static {
	bitRateTable.put(new Integer(32000), new Integer(48000));
	bitRateTable.put(new Integer(44100), new Integer(64000));
	bitRateTable.put(new Integer(48000), new Integer(64000));

	bitRateTable.put(new Integer(16000), new Integer(24000));
	bitRateTable.put(new Integer(22050), new Integer(32000));
	bitRateTable.put(new Integer(24000), new Integer(32000));
    }

    public NativeEncoder() {

	inputFormats = new Format[] {

/*
 - crashes the VM.  Disables for 2.1.1 beta.

	    new AudioFormat(AudioFormat.LINEAR,
				16000.,
				16,
			    	Format.NOT_SPECIFIED,
				Arch.isBigEndian()?
				     AudioFormat.BIG_ENDIAN :
				     AudioFormat.LITTLE_ENDIAN,
				AudioFormat.SIGNED),
*/

	    new AudioFormat(AudioFormat.LINEAR,
				22050.,
				16,
			    	Format.NOT_SPECIFIED,
				Arch.isBigEndian()?
				     AudioFormat.BIG_ENDIAN :
				     AudioFormat.LITTLE_ENDIAN,
				AudioFormat.SIGNED),
	    new AudioFormat(AudioFormat.LINEAR,
				24000.,
				16,
			    	Format.NOT_SPECIFIED,
				Arch.isBigEndian()?
				     AudioFormat.BIG_ENDIAN :
				     AudioFormat.LITTLE_ENDIAN,
				AudioFormat.SIGNED),
/*
 - The decoder weren't able to handle the data generated.  
   Probably buggy data.  Disables for 2.1.1 beta.

	    new AudioFormat(AudioFormat.LINEAR,
				32000.,
				16,
			    	Format.NOT_SPECIFIED,
				Arch.isBigEndian()?
				     AudioFormat.BIG_ENDIAN :
				     AudioFormat.LITTLE_ENDIAN,
				AudioFormat.SIGNED),
*/

	    new AudioFormat(AudioFormat.LINEAR,
				44100.,
				16,
			    	Format.NOT_SPECIFIED,
				Arch.isBigEndian()?
				     AudioFormat.BIG_ENDIAN :
				     AudioFormat.LITTLE_ENDIAN,
				AudioFormat.SIGNED),
	    new AudioFormat(AudioFormat.LINEAR,
				48000.,
				16,
			    	Format.NOT_SPECIFIED,
				Arch.isBigEndian()?
				     AudioFormat.BIG_ENDIAN :
				     AudioFormat.LITTLE_ENDIAN,
				AudioFormat.SIGNED),
	};

	outputFormats = new Format[] {
	    /* Disabling MP3 encoding 
	    new AudioFormat(AudioFormat.MPEGLAYER3,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    AudioFormat.BIG_ENDIAN,
			    Format.NOT_SPECIFIED),
	    */
	    new AudioFormat(AudioFormat.MPEG,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    AudioFormat.BIG_ENDIAN,
			    Format.NOT_SPECIFIED),
	};

 	bra = new BitRateControl(this, 64000, 24000, 128000);
    }

    public String getName() {
	return "MPEG Layer 2 Encoder";
    }

    public Format [] getSupportedOutputFormats(Format input) {
	if (input == null) {
	    return outputFormats;
	} else if (input instanceof AudioFormat) {
	    AudioFormat af = (AudioFormat) input;
	    Format outputs[] = new Format[] {
		        /* Disabling MP3 encoding
			new AudioFormat (AudioFormat.MPEGLAYER3,
					 af.getSampleRate(),
					 af.getSampleSizeInBits(),
					 af.getChannels(),
					 AudioFormat.BIG_ENDIAN,
					 AudioFormat.SIGNED),
		        */
			new AudioFormat (AudioFormat.MPEG,
					 af.getSampleRate(),
					 af.getSampleSizeInBits(),
					 af.getChannels(),
					 AudioFormat.BIG_ENDIAN,
					 AudioFormat.SIGNED),
		};
	    return outputs;
	}

	return new Format[0];
    }


    public int getBitRate() {
	return bra.getBitRate();
    }


    public  java.lang.Object[] getControls () {
	if (controls == null) {
	     controls = new Control[1];
	     controls[0] = bra;
	}
	return (Object[])controls;
    }


    public int isLayer3() {
	/* Disabling MP3
	if (outputFormat != null && 
	    outputFormat.getEncoding().equalsIgnoreCase(AudioFormat.MPEGLAYER3)) {
	    return 1;
	} else {
	    return 0;
	}
	*/
	return 0;
    }


    public synchronized void open() throws ResourceUnavailableException {
	if (nativeData != 0)
	    close();
	
      	try {
            JMFSecurityManager.loadLibrary("jmutil");
            JMFSecurityManager.loadLibrary("jmmpa");
	    nativeData = nOpen((AudioFormat)outputFormat);
	    if (nativeData == 0) {
		throw new ResourceUnavailableException("could not open " + getName());
	    }
	    encData = new byte[INSIZE*numFramesPerOutputBuffer];
	    return;
	} catch (Throwable e) {
	}
	throw new ResourceUnavailableException("could not open " + getName());
    }

    public synchronized void close() {
	if (nativeData != 0) {
	    nClose(nativeData);
	    nativeData = 0;
	}
    }

    public synchronized void reset() {
	if (nativeData != 0) {
	    close();
	    try {
		open();
	    } catch (ResourceUnavailableException rue) {
		System.err.println("MP2 Encoder: " + rue);
	    }
	}
    }


    public Format setOutputFormat(Format format) {
      if (matches(format, getSupportedOutputFormats(inputFormat)) == null)
          return null;
      if (!(format instanceof AudioFormat))
          return null;


      AudioFormat in = (AudioFormat) format;

      double sampleRate = in.getSampleRate();
      int channels = in.getChannels();

      int bitRate = computeBitRate((int) sampleRate, channels);
      bra.setBitRate(bitRate);

      outputFormat = new AudioFormat(
				     in.getEncoding(),
				     sampleRate,
				     in.getSampleSizeInBits(),
				     channels,
				     in.getEndian(),
				     in.getSigned(),
				     8, // in.getFrameSizeInBits(),
				     getBitRate() / 8,// in.getFrameRate(),
				     in.getDataType()
				     );
      return outputFormat;
    }

    public synchronized int process(Buffer in, Buffer out) {

	if (isEOM(in)) {
	    int len = nFlush(nativeData, (byte [])out.getData());
	    if (len > 0) {
		out.setOffset(0);
		out.setLength(len);
	    } else
		out.setLength(0);

	    out.setFormat(outputFormat);
	    out.setEOM(true);
	    if (settingsChanged) {
		reset();
		settingsChanged = false;
	    }
	    return BUFFER_PROCESSED_OK;
	}

	if (!(in.getData() instanceof byte[])) {
	    return BUFFER_PROCESSED_FAILED;
	}

	int inOffset = in.getOffset();
	int inLength = in.getLength();
	byte inData[] = (byte[])in.getData();
	byte outData[] = (byte[])out.getData();

	// Check the output buffer size.
	if (outData == null || outData.length < (OUTSIZE*numFramesPerOutputBuffer)) {
	    outData = new byte[OUTSIZE*numFramesPerOutputBuffer];
	    out.setData(outData);
	}

	int rtn = 0;

	if (nativeData != 0) {

	    //System.err.println("inOffset = " + inOffset);
	    while ((encDataLen < INSIZE*numFramesPerOutputBuffer) && 
                     (inOffset < inLength)) {
		encData[encDataLen] = inData[inOffset];
		inOffset++; 
		encDataLen++;
	    }

	    //System.err.println("inOffset = " + inOffset + " encDataLen = " + encDataLen + " inLength = " + inLength);
	    if (encDataLen >= INSIZE*numFramesPerOutputBuffer) { 
		// We have enough samples to encode.

		for (int i = 0; i < numFramesPerOutputBuffer; i++) {

		    int returnVal = 
			nConvert(nativeData, (byte[])encData, 
				 (INSIZE*i), ////// inOffset, 
				 INSIZE, (byte[])out.getData(), outOffset);
		    
		    if (returnVal < 0) {
			return BUFFER_PROCESSED_FAILED;
		    }

		    numFramesInBuffer++;
		    outOffset += returnVal;
		}
		encDataLen = 0;
		numFramesInBuffer = 0;
		if (outOffset == 0) {
		    rtn |= OUTPUT_BUFFER_NOT_FILLED;
		} else {
		    out.setOffset(0);
		    out.setLength(outOffset);
		    out.setFormat(outputFormat);
		    outOffset = 0;
		}
	    } else {
 		// We are not done with the output buffer.
 		rtn |= OUTPUT_BUFFER_NOT_FILLED;
 	    }
	}

	if (inOffset < inLength) {
	    // We are not done with the input buffer.
	    in.setOffset(inOffset);
	    rtn |= INPUT_BUFFER_NOT_CONSUMED;
	}




	if (rtn == BUFFER_PROCESSED_OK && settingsChanged) {
	    reset();
	    settingsChanged = false;
	}
	return rtn;
    }


    // Currently sampleRate supported is low (16k, 22.05k and 24k) and
    // high (32k, 44.1k and 48k);
    //     BitRates for sampleRates  16k, 22.05k and 24k
    //       8, 16, 24,  32,  40,  48,  56,  64,  80,  96, 112, 128, 144, 160

    //     BitRates for sampleRates 32k, 44.1k and 48k 
    //       32, 40, 48,  56,  64,  80,  96, 112, 128, 160, 192, 224, 256, 320

    // Choose 128k bitrate for 44100, stereo
    private int computeBitRate(int sampleRate, int channels) {

	Object o = bitRateTable.get(new Integer(sampleRate));

	int bitRate;

	if (o != null) {
	    bitRate = ((Integer)o).intValue();
	    bitRate *= channels;
	} else {
	    bitRate = 64000; // Default bitrate
	}

	return bitRate;
    }


    ////////////////////////////////////////
    //
    // INNER CLASS
    ////////////////////////////////////////

    class BitRateControl extends BitRateAdapter implements Owned {

	NativeEncoder encoder;

	public BitRateControl(NativeEncoder enc, int init, int min, int max) {
	    super(init, min, max, true);
	    encoder = enc;
	}

	public int setBitRate(int val) {
	    int rtn = super.setBitRate(val);
	    encoder.settingsChanged = true;
	    return rtn;
	}

	public Object getOwner() {
	    return encoder;
	}
    }
}
