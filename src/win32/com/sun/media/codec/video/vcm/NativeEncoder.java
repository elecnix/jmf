/*
 * @(#)NativeEncoder.java	1.23 03/04/24
 *
 * Copyright (c) 1996-2003 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.video.vcm;

import javax.media.*;
import javax.media.Format;
import javax.media.format.*;
import javax.media.control.QualityControl;
import com.sun.media.*;
import com.sun.media.controls.QualityAdapter;
import com.sun.media.vfw.*;
import java.util.Vector;
import java.awt.*;


public final class NativeEncoder extends BasicCodec {

    protected RGBFormat inputFormat = null;
    protected VideoFormat outputFormat = null;

    static {
	com.sun.media.JMFSecurityManager.loadLibrary("jmvcm");
    }

    private static String [] supportedEncodings = null;
    private static BitMapInfo [] supportedOutBMIs = null;
    
    private int vcmHandle = 0;

    private boolean debug = false;

    private BitMapInfo biIn = null;
    private BitMapInfo biOut = null;
    private BitMapInfo biPrev = null;
    private boolean dropFrame = false;

    private boolean hasQuality = false;
    private boolean hasCrunch = false;
    private boolean hasTemporal = false;
    private boolean hasFastCompress = false;

    private boolean begun = false;
    private QualityControl qc = null;
    private Control [] controls = null;
    private float quality = 9000f;
    final static int AVIF_KEYFRAME = 0x00000010;
    
    
    public NativeEncoder() {
	inputFormats = new VideoFormat[1];
	inputFormats[0] = new RGBFormat();

	outputFormats = new VideoFormat[1];
	outputFormats[0] = new VideoFormat(null);
    }

    protected Format getInputFormat() {
	return inputFormat;
    }

    protected Format getOutputFormat() {
	return outputFormat;
    }

    public String getName() {
	return "VCM Encoder";
    }

    private static synchronized void querySupportedEncodings() {

	ICInfo icinfo = new ICInfo();
	BitMapInfo biOut = new BitMapInfo();
	int i = 0;
	Vector listEncoding = new Vector();
	Vector listBMI = new Vector();
	
	while (true) {
	    boolean result = VCM.icInfoEnum("vidc", i, icinfo);
	    if (!result || i > 30)
		break;
	    else {
		int handle = VCM.icOpen("vidc", icinfo.fccHandler, VCM.ICMODE_COMPRESS);
		if (handle != 0) {
		    BitMapInfo biIn = new BitMapInfo("RGB", 320, 240);
		    biOut = new BitMapInfo("RGB", 320, 240);
		    VCM.icCompressGetFormat(handle, biIn, biOut);
		    if (biOut.fourcc.equalsIgnoreCase(icinfo.fccHandler) &&
			!(icinfo.fccHandler.equalsIgnoreCase(VideoFormat.MJPG) ||
			  icinfo.fccHandler.equalsIgnoreCase("dmb1"))) {
			listEncoding.addElement(biOut.fourcc);
			listBMI.addElement(biOut);
		    }
		    VCM.icClose(handle);
		}
	    }
	    i++;
	}

	supportedEncodings = new String[listEncoding.size()];
	supportedOutBMIs = new BitMapInfo[listEncoding.size()];
	for (i = 0; i < supportedEncodings.length; i++) {
	    supportedEncodings[i] = (String) listEncoding.elementAt(i);
	    supportedOutBMIs[i] = (BitMapInfo) listBMI.elementAt(i);
	}
    }

    public Format [] getSupportedInputFormats() {
	inputFormats[0] = new RGBFormat(null,
					Format.NOT_SPECIFIED,
					Format.byteArray,
					Format.NOT_SPECIFIED, // frame rate
					24,
					3, 2, 1,
					3,
					Format.NOT_SPECIFIED,
					Format.TRUE,
					Format.NOT_SPECIFIED); // endian

	return inputFormats;
    }

    public Format [] getSupportedOutputFormats(Format input) {
	if (input == null || !(input instanceof RGBFormat))
	    return new VideoFormat[] {
	           new VideoFormat(null)
	    };
	if (input.matches(inputFormats[0])) {
	    if (supportedEncodings == null) {
		querySupportedEncodings();
	    }

	    Dimension frameSize = ((VideoFormat)input).getSize();
	    if (frameSize == null)
		frameSize = new Dimension(320, 240);
	    outputFormats = new VideoFormat[supportedEncodings.length];
	    
	    for (int i = 0; i < supportedEncodings.length; i++) {
		int area = frameSize.width * frameSize.height;
		int outSize = area * supportedOutBMIs[i].biBitCount / 8;
		if (outSize < area)
		    outSize = area;
		outputFormats[i] = new VideoFormat(supportedEncodings[i],
						   frameSize,
						   outSize,
						   Format.byteArray,
						   ((VideoFormat)input).getFrameRate());
	    }
	    return outputFormats;  
	} else
	    return new Format[0];
    }

    public Format setInputFormat(Format in) {
	if (in instanceof RGBFormat) {
	    if (matches(in, inputFormats) != null) {
		inputFormat = (RGBFormat) in;
		if (begun) {
		    close();
		    try {
			// Update outputsize
			Dimension frameSize = ((RGBFormat)in).getSize();
			int area = frameSize.width * frameSize.height;
			int outSize = area * 24 / 8;
			outputFormat =
			    new VideoFormat(outputFormat.getEncoding(),
					    frameSize,
					    outSize,
					    Format.byteArray,
					    inputFormat.getFrameRate());
			
			open();
		    } catch (Exception e) {
			return null;
		    }
		}
		return in;
	    } else {
		//System.err.println("VCMEncoder didn't match:\n" + in);
	    }
	}

	return null;
    }

    public Format setOutputFormat(Format out) {
	if (out instanceof VideoFormat) {
	    outputFormat = (VideoFormat) out;
	    if (debug) System.err.println("VCM.setOutputFormat : "  + outputFormat);
	    return out;
	}
	
	return null;
    }

    public synchronized void open() throws ResourceUnavailableException {
	
	if (inputFormat == null || outputFormat == null)
	    throw new ResourceUnavailableException("Unknown formats");
	
	// Size restriction 4x4
	Dimension size = inputFormat.getSize();
	if (size == null || (size.width % 4) != 0 || (size.height % 4) != 0) {
	    Log.error("Class: " + this);
	    Log.error("  can only encode in sizes of multiple of 4 pixels.");
	    throw new ResourceUnavailableException("Wrong size.");
	}

	if (vcmHandle != 0)
	    close();

	biIn = new BitMapInfo(inputFormat);
	biOut = new BitMapInfo(outputFormat);

	vcmHandle = VCM.icLocate("vidc", VideoFormat.RGB,
				 biIn, biOut,
				 VCM.ICMODE_COMPRESS);
	if (vcmHandle == 0) {
	    throw new ResourceUnavailableException("Couldn't create compressor");
	}
	
	ICInfo icinfo = new ICInfo();
	VCM.icGetInfo(vcmHandle, icinfo);

	// Get properties of the compressor
	int flags = icinfo.dwFlags;
	//	System.err.println("Flags = " + flags);
	hasQuality = (flags & VCM.VIDCF_QUALITY) != 0;
	hasCrunch  = (flags & VCM.VIDCF_CRUNCH) != 0;
	hasTemporal = (flags & VCM.VIDCF_TEMPORAL) != 0;
	hasFastCompress = (flags & VCM.VIDCF_FASTC) != 0;

	begun = false;


	BitMapInfo testBMI = new BitMapInfo();
	VCM.icCompressGetFormat(vcmHandle, biIn, testBMI);
	biOut = testBMI;
	
	if (!VCM.icCompressBegin(vcmHandle, biIn, biOut)) {
	    VCM.icClose(vcmHandle);
	    throw new ResourceUnavailableException("Couldn't create compressor");
	} else {
	    begun = true;
	}
	seqNo = (seqNo + 3) & ~3;
    }

    public synchronized void close() {
	if (vcmHandle != 0) {
	    if (begun)
		VCM.icCompressEnd(vcmHandle);
	    VCM.icClose(vcmHandle);
	    begun = false;
	}
	vcmHandle = 0;
    }

    public Object [] getControls() {
	//System.err.println("NativeEncoder.getControls$$$$$$$");
	if (controls == null) {
	    //System.err.println("Has quality = " + hasQuality);
	    if (qc == null) {
		qc = new QualityAdapter(quality, 0f, 10000f, true) {
		    public float setQuality(float newValue) {
			quality = newValue;
			return super.setQuality(newValue);
		    }
		    
		    protected String getName() {
			return "Video Encoder Quality";
		    }
		};
		controls = new Control[1];
		controls[0] = qc;
	    } else {
		controls = new Control[0];
	    }
	}
	//System.err.println(controls);
	return controls;
    }
    

    int seqNo = 0;
    int keyFrameInterval = 4;
    int [] outFlags = new int[1];
    int [] ckid = new int[1];
    byte [] dataPrev = null;
    
    public int process(Buffer inBuffer, Buffer outBuffer) {
	if (isEOM(inBuffer)) {
	    propagateEOM(outBuffer);
	    return BUFFER_PROCESSED_OK;
	}

	int inFlags = 0;
	int outBufFlags = 0;
	
	Object outData = outBuffer.getData();

	if (outData == null || !(outData instanceof byte[]) ||
	    ((byte[])outData).length < outputFormat.getMaxDataLength())
	    outData = new byte[outputFormat.getMaxDataLength()];
	outBuffer.setData(outData);
	outBuffer.setFormat(outputFormat);

	if (dataPrev == null && hasTemporal) {
	    dataPrev = new byte[inputFormat.getMaxDataLength()];
	}

	if ((seqNo % keyFrameInterval) == 0) {
	    inFlags |= VCM.ICCOMPRESS_KEYFRAME;
	}
	
	seqNo++;
	int returnVal = VCM.icCompress(vcmHandle,
				       inFlags,
				       biOut,
				       outData,
				       biIn,
				       inBuffer.getData(),
				       ckid,
				       outFlags,
				       seqNo,
				       outputFormat.getMaxDataLength(),
				       (int) quality,
				       biIn,
				       dataPrev);

	if ((outFlags[0] & AVIF_KEYFRAME) != 0) 
	    outBufFlags |= Buffer.FLAG_KEY_FRAME;

	Object tempdata = dataPrev;
	if (hasTemporal) {
	    dataPrev = (byte[]) inBuffer.getData();
	    inBuffer.setData(tempdata);
	}
	
	if (returnVal < 0) {
	    return BUFFER_PROCESSED_FAILED;
	}
	outBuffer.setFormat(outputFormat);
	outBuffer.setSequenceNumber(seqNo);
	outBuffer.setLength(biOut.biSizeImage);
	outBufFlags = (outBuffer.getFlags() & ~Buffer.FLAG_KEY_FRAME) |
	    outBufFlags |
	    Buffer.FLAG_NO_SYNC;
	outBuffer.setFlags(outBufFlags);
	return BUFFER_PROCESSED_OK;
    }
}


