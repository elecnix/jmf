/*
 * @(#)NativeDecoder.java	1.22 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.audio.mpa;


import javax.media.format.AudioFormat;
import javax.media.Format;
import javax.media.Buffer;
import javax.media.ResourceUnavailableException;
import com.sun.media.Log;
import com.sun.media.util.Arch;
import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;


public class NativeDecoder extends com.sun.media.codec.audio.AudioCodec {

    private int pendingDataSize = 0;
    private static final int OUTSIZE = 32 * 1024;
    private byte [] pendingData = new byte[OUTSIZE * 4];
    private MpegAudio mpa;
    private int nativeData = 0;
    private boolean expectingSameInputBuffer = false;
    private long accumTS = 0;
    private AudioFormat aFormat = null;
    
    public NativeDecoder() {
	inputFormats = new Format[] { 
	    /*
	    new AudioFormat(AudioFormat.MPEGLAYER3,
			    16000.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, // endian
			    AudioFormat.SIGNED),

	    new AudioFormat(AudioFormat.MPEGLAYER3,
			    22050.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, // endian
			    AudioFormat.SIGNED),

	    new AudioFormat(AudioFormat.MPEGLAYER3,
			    24000.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, // endian
			    AudioFormat.SIGNED),

	    new AudioFormat(AudioFormat.MPEGLAYER3,
			    32000.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, // endian
			    AudioFormat.SIGNED),

	    new AudioFormat(AudioFormat.MPEGLAYER3,
			    44100.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, // endian
			    AudioFormat.SIGNED),

	    new AudioFormat(AudioFormat.MPEGLAYER3,
			    48000.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, // endian
			    AudioFormat.SIGNED),
	    */
	    new AudioFormat(AudioFormat.MPEG,
			    16000.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, // endian
			    AudioFormat.SIGNED),

	    new AudioFormat(AudioFormat.MPEG,
			    22050.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, // endian
			    AudioFormat.SIGNED),

	    new AudioFormat(AudioFormat.MPEG,
			    24000.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, // endian
			    AudioFormat.SIGNED),

	    new AudioFormat(AudioFormat.MPEG,
			    32000.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, // endian
			    AudioFormat.SIGNED),

	    new AudioFormat(AudioFormat.MPEG,
			    44100.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, // endian
			    AudioFormat.SIGNED),

	    new AudioFormat(AudioFormat.MPEG,
			    48000.,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED,
			    Format.NOT_SPECIFIED, // endian
			    AudioFormat.SIGNED),


         };
    }

    public String getName() {
	return "MPEG Layer 2 Decoder";
    }

    public Format [] getSupportedOutputFormats(Format input) {
	if (input == null) {
	    return new Format[] { new AudioFormat(AudioFormat.LINEAR) };
	} else if (input instanceof AudioFormat) {
	    AudioFormat af = (AudioFormat) input;
	    AudioFormat output = new AudioFormat(AudioFormat.LINEAR,
						 af.getSampleRate(),
						 af.getSampleSizeInBits(),
						 af.getChannels(),
						 AudioFormat.BIG_ENDIAN,
					/*
					   The new version of the native
					   codec is adjusted to output 
					   BIG_ENDIAN on both platforms.
						 Arch.isBigEndian()?
						     AudioFormat.BIG_ENDIAN :
						     AudioFormat.LITTLE_ENDIAN,
					*/
						 AudioFormat.SIGNED);
	    outputFormats = new Format[] { output };
	} else {
	    outputFormats = new Format[0];
	}
	return outputFormats;
    }

    public synchronized void open() throws ResourceUnavailableException {
	if (nativeData != 0)
	    close();
      	try {
            JMFSecurityManager.loadLibrary("jmutil");
            JMFSecurityManager.loadLibrary("jmmpa");
            mpa = new MpegAudio();
	    nativeData = mpa.nOpen(new int[1]);
	    if (nativeData == 0)
		throw new ResourceUnavailableException("could not open " + getName());
	    pendingDataSize = 0;
	    expectingSameInputBuffer = false;
	    accumTS = 0;
	    aFormat = (AudioFormat)outputFormat;
	    //System.err.println("Input format is " + inputFormat);
	    //System.err.println("Output format is " + outputFormat);
            return;
	} catch (Throwable e) {
	    //System.err.println(e);
	}

        throw new ResourceUnavailableException("could not open " + getName());
    }

    public synchronized void close() {
	if (nativeData != 0) {
	    mpa.nClose(nativeData);
	    nativeData = 0;
	}
    }

    public synchronized void reset() {
	if (nativeData != 0) {
	    close();
	    try {
		open();
	    } catch (ResourceUnavailableException rue) {
		Log.error("MP2 Decoder: " + rue);
	    }
	}
    }

    int [] inRead = new int[1];
    int [] inReq  = new int[1];
    int [] outWritten = new int[1];
    
    public synchronized int process(Buffer in, Buffer out) {
	boolean inputEOM = false;
	if (isEOM(in)) {
	    if (pendingDataSize == 0) {
		propagateEOM(out);
		return BUFFER_PROCESSED_OK;
	    }
	    inputEOM = true;
	}

	Object inObject = in.getData();
	Object outObject = out.getData();

	if (outObject == null) {
	    outObject = new byte[OUTSIZE];
	    out.setData(outObject);
	}
	
	if (!(inObject instanceof byte[]) || !(outObject instanceof byte[]))
	    return BUFFER_PROCESSED_FAILED;

	byte [] inData = (byte[]) inObject;
	byte [] outData = (byte[]) outObject;
	int inOffset = in.getOffset();
	int inLength = in.getLength();
	int outdataSize = outData.length;
	int outOffset = 0;
	int pendingDataOffset = 0;

	if (!expectingSameInputBuffer) {
	    System.arraycopy(inData, inOffset, pendingData, pendingDataSize,
								inLength);
	    pendingDataSize += inLength;
	}
	
	inReq[0] = 21;
	if (nativeData != 0) {
	    while (true) {
		// Does the output buffer have enough space left?
		if (outOffset + 5000 > outdataSize)
		    break;
		if (pendingDataSize < inReq[0])
		    break;
		boolean returnVal = 
		    MpegAudio.nConvert(nativeData,
				       pendingData, pendingDataOffset, pendingDataSize,
				       outData, outOffset, outdataSize - outOffset,
				       inRead, outWritten,
				       pendingDataOffset, inReq);

		if (returnVal) {
		    // Decoded ok. There was some data written

                    //sbd: patch: if read more then available ignore last audio frame
                    if (inRead[0] > pendingDataSize) {
                        //System.out.println("mpa error: Read more than available !");
                        break;
		    }
		    outOffset += outWritten[0];
		    pendingDataOffset += inRead[0];
		    pendingDataSize -= inRead[0];
		    
		    if (inReq[0] == 0)
			inReq[0] = 21;

		    // For MP3, need header from next frame to remain in
		    // pendingData until EOM to deal with negative offset
		    // 9 = header with CRC + negative offset + 1 for pad
		    if (pendingDataSize < inRead[0] + 9)
			break;
		} else {
		    //System.err.println("NativeDecoder: error from nConvert!");
		    // If there was an error
		    // If there's some output data, break out of the loop
		    if (outOffset > 0)
			break;
		    // else we've decoded nothing. return false.
		    else {
			pendingDataSize = 0;
			if (inputEOM) {
			    // Ignore error if EOM buffer
			    outOffset = 0;
			    break;
			} else {
			    return BUFFER_PROCESSED_FAILED;
			}
		    }
		}
	    }
	}

	// Move the last chunk to the beginning of the pendingData buffer
	if (pendingDataOffset != 0)
	    System.arraycopy(pendingData, pendingDataOffset,
			     pendingData, 0, pendingDataSize);
	out.setLength(outOffset);
	out.setFormat(outputFormat);
	if (aFormat != null && accumTS != 0 && in.getTimeStamp() > 0)
	    out.setTimeStamp(in.getTimeStamp() + aFormat.computeDuration(accumTS));

	if (inputEOM) {
	    if (outOffset == 0) {
		// trying to process last bit of data and it isn't usable,
		// discard it to avoid a loop and propagate EOM.
		pendingDataSize = 0;
		propagateEOM(out);
		return BUFFER_PROCESSED_OK;
	    }
	    // processed remaining frame(s), return data and repeat the EOM.
	    expectingSameInputBuffer = true;
	    accumTS += out.getLength(); 
	    return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
	}

	if (pendingDataSize > 1024) {
	    expectingSameInputBuffer = true;
	    accumTS += out.getLength(); 
	    return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
	} else {
	    expectingSameInputBuffer = false;
	    accumTS = 0;
	    return BUFFER_PROCESSED_OK;
	}
    }

    
}
