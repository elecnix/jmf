/*
 * @(#)NativeDecoder.java	1.47 03/04/24
 *
 * Copyright (c) 1996-2003 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.video.vcm;

import javax.media.*;
import javax.media.control.*;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;

import com.sun.media.format.AviVideoFormat;
import com.sun.media.*;
import com.sun.media.util.*;
import com.sun.media.vfw.*;
import java.util.Vector;
import java.awt.*;

public final class NativeDecoder extends BasicCodec
implements com.sun.media.util.DynamicPlugIn {
    
    private static boolean loaded = false;
    static {
	com.sun.media.JMFSecurityManager.loadLibrary("jmvcm");
	loaded = true;
    }

    static Integer processLock = new Integer(1);

    protected VideoFormat inputFormat = null;
    protected VideoFormat outputFormat = null;

    /*
    protected String [] knownEncodings = {//VideoFormat.CINEPAK,
					  "msvc", "cram",
					  "mrle", "vdec",
					  "vgpx",
					  //"rgb",
					  "i263", "i420",
					  "yuy2", "yvu9", "y411",
					  //"m263",
					  //VideoFormat.INDEO32,
					  VideoFormat.INDEO41,
					  VideoFormat.INDEO50};
    */
    
    private static String [] supportedEncodings = null;

    private int vcmHandle = 0;

    private boolean debug = false;

    private BitMapInfo biIn = null;
    private BitMapInfo biOut = null;
    private boolean dropFrame = false;
    private boolean keepOutputRef = false;
    private NBA outputRef = null;

    public NativeDecoder() {
	// Workaround for Netscape bug, where if the 
	// Class.forName fails the first time, because
	// says native libraries couldn't be loaded,
	// it succeeds the second time and
	// throws an error later.
	if (!loaded) {
	    throw new UnsatisfiedLinkError("Cannot load native libraries");
	}
	inputFormats = new VideoFormat[1];
	inputFormats[0] = new VideoFormat(null);

	outputFormats = new VideoFormat[1];
	outputFormats[0] = new RGBFormat();
    }

    protected Format getInputFormat() {
	return inputFormat;
    }

    protected Format getOutputFormat() {
	return outputFormat;
    }

    private static synchronized void querySupportedEncodings() {
	ICInfo icinfo = new ICInfo();
	BitMapInfo biOut = new BitMapInfo();
	int i = 0;
	Vector listEncoding = new Vector();
	
	while (true) {
	    icinfo = new ICInfo();
	    boolean result = VCM.icInfoEnum("vidc", i, icinfo);
	    if (!result || i > 30)
		break;
	    else {
		if (!(icinfo.fccHandler == null ||
		      icinfo.fccHandler.equalsIgnoreCase(VideoFormat.MJPG) ||
		      icinfo.fccHandler.equalsIgnoreCase(VideoFormat.CINEPAK)
		      )) {
		    int handle = VCM.icOpen("vidc", icinfo.fccHandler, VCM.ICMODE_DECOMPRESS);
		    if (handle != 0) {
			if (icinfo.fccHandler.equalsIgnoreCase("ir41")) // hack
			    icinfo.fccHandler = "IV41";
			if (icinfo.fccHandler.equalsIgnoreCase("ir50")) // hack
			    icinfo.fccHandler = "IV50";
			if (icinfo.fccHandler.equalsIgnoreCase("ir32")) // hack
			    icinfo.fccHandler = "IV32";
			listEncoding.addElement(icinfo.fccHandler.toUpperCase());
			VCM.icClose(handle);
		    }
		}
	    }
	    i++;
	}
	listEncoding.addElement("YVU9");
	supportedEncodings = new String[listEncoding.size()];
	for (i = 0; i < supportedEncodings.length; i++) {
	    supportedEncodings[i] = (String) listEncoding.elementAt(i);
	}
    }
    
    public Format [] getSupportedInputFormats() {
	if (supportedEncodings == null) {
	    querySupportedEncodings();
	}
	inputFormats = new VideoFormat[supportedEncodings.length];
	for (int i = 0; i < supportedEncodings.length; i++) {
	    inputFormats[i] = new VideoFormat(supportedEncodings[i]);
	}

	return inputFormats;
    }

    public Format [] getSupportedOutputFormats(Format in) {
	if (in == null)
	    return outputFormats;
	
	if (matches(in, inputFormats) == null)
	    return new Format[0];

	VideoFormat video = (VideoFormat) in;
	Dimension size = video.getSize();
	if (size == null)
	    size = new Dimension(320, 240);
	RGBFormat [] tryFormats = new RGBFormat[] {
	    new RGBFormat(size, size.width * size.height * 3,
			  Format.byteArray,
			  video.getFrameRate(), // frame rate
			  24,
			  3, 2, 1,
			  3,
			  size.width * 3,
			  Format.TRUE,
			  Format.NOT_SPECIFIED), // endian

	    new RGBFormat(size, size.width * size.height * 3,
			  Format.byteArray,
			  video.getFrameRate(), // frame rate
			  24,
			  3, 2, 1,
			  3,
			  size.width * 3,
			  Format.FALSE, // flipped
			  Format.NOT_SPECIFIED), // endian

	    new RGBFormat(size, size.width * size.height,
			  Format.shortArray,
			  video.getFrameRate(), // frame rate
			  16,
			  0x07C00, 0x003e0, 0x0001F,
			  1, size.width,
			  Format.FALSE, // flipped
			  Format.NOT_SPECIFIED), // endian

	    new RGBFormat(size, size.width * size.height,
			  Format.shortArray,
			  video.getFrameRate(), // frame rate
			  16,
			  0x07C00, 0x003e0, 0x0001F,
			  1, size.width,
			  Format.TRUE,
			  Format.NOT_SPECIFIED) // endian


	};
	if (debug) System.err.println("Input format = " +
				      new BitMapInfo((VideoFormat)video));
	Vector supportedOuts = new Vector();
	int handle = VCM.icLocate("vidc", "RGB",
				  new BitMapInfo((VideoFormat) video),
				  null,
				  VCM.ICMODE_DECOMPRESS);
	if (handle != 0) {
	    for (int enum = 0; enum < tryFormats.length; enum++) {
		if ( VCM.icDecompressBegin(handle,
					   new BitMapInfo((VideoFormat) video),
					   new BitMapInfo(tryFormats[enum])) ) {
		    supportedOuts.addElement(tryFormats[enum]);
		    if (debug) System.err.println("VCM " + video.getEncoding() + " supports " + tryFormats[enum]);
		    VCM.icDecompressEnd(handle);
		} else {
		}
	    }
	    VCM.icClose(handle);
	}

	Format [] outs = new Format[supportedOuts.size()];
	for (int i = 0; i < outs.length; i++) {
	    outs[i] = (Format) supportedOuts.elementAt(i);
	}
	
	//if (debug) System.err.println("VCM.getSupportedOutputFormats: " + rgb);

	return outs;
    }
    
    public Format setInputFormat(Format in) {
	if (supportedEncodings == null) {
	    querySupportedEncodings();
	}
	String encoding = in.getEncoding();
	if (  encoding.equalsIgnoreCase(VideoFormat.CINEPAK) ||
	      encoding.equalsIgnoreCase(VideoFormat.MJPG) ||
	      encoding.equalsIgnoreCase("RGB")                  )
	    return null;

	if ( encoding.equalsIgnoreCase("mpg4") ||
	     encoding.equalsIgnoreCase("mp42") ||
	     encoding.equalsIgnoreCase("mp43") ||
	     encoding.equalsIgnoreCase("iv32")  )
	    keepOutputRef = true;
	else
	    keepOutputRef = false;
	
	if (in instanceof VideoFormat) {
	    if (matches(in, inputFormats) != null) {
		inputFormat = (VideoFormat) in;

		if (opened) {
		    try {
			close();
			Format [] ofs =
			    getSupportedOutputFormats(inputFormat);
			if (ofs.length < 1)
			    return null;
			else if (ofs.length == 1)
			    outputFormat = (RGBFormat) ofs[0];
			else {
			    for (int i = 0; i < ofs.length; i++) {
				if (((RGBFormat)outputFormat).getBitsPerPixel() ==
				    ((RGBFormat)ofs[i]).getBitsPerPixel() &&
				    ((RGBFormat)outputFormat).getFlipped() ==
				    ((RGBFormat)ofs[i]).getFlipped()) {
				    outputFormat = (VideoFormat) ofs[i];
				    break;
				}
			    }
			}
			open();
		    } catch (Exception e) {
			return null;
		    }
		}
		return in;
	    }
	}

	return null;
    }

    public Format setOutputFormat(Format out) {
	if (out instanceof RGBFormat) {
	    outputFormat = (RGBFormat) out;
	    if (debug) System.err.println("VCM.setOutputFormat : "  + outputFormat);
	    return out;
	}
	
	return null;
    }

    public void open() throws ResourceUnavailableException {

	if (inputFormat == null || outputFormat == null)
	    throw new ResourceUnavailableException("Formats not set!");

	Dimension size = inputFormat.getSize();
	//Object bi = inputFormat.getExtraData();
	Object bi = null;
	String inFourCC = inputFormat.getEncoding();

	biOut = new BitMapInfo();

	// Translate from AviVideoFormat to BitMapInfo
	if (inputFormat instanceof AviVideoFormat) {
	    bi = new BitMapInfo(inputFormat);
	    if (debug) System.err.println("VCM.open, BMI = " + bi);
	}


	if (bi != null && bi instanceof BitMapInfo) {
	    biIn = (BitMapInfo) bi;
	} else
	    biIn = new BitMapInfo(inFourCC, size.width, size.height);

	String cramHack = inFourCC;
	if (cramHack.equalsIgnoreCase("cram"))
	    cramHack = "msvc";

	vcmHandle = VCM.icLocate("vidc", "RGB",
				 biIn, null,
				 VCM.ICMODE_DECOMPRESS);

	if (debug) System.err.println("open.vcmHandle for " + inFourCC + " = " + vcmHandle);
	
	if (vcmHandle == 0)
	    throw new ResourceUnavailableException("Could not open VCM driver");

	BitMapInfo testOut = new BitMapInfo();
	//VCM.icDecompressGetFormat(vcmHandle, biIn, testOut);

	if (debug) System.err.println("open.preferred out = " + testOut);
	// biOut = testOut;

	
	// new trial
	biOut = new BitMapInfo(outputFormat);
	if (debug) System.err.println("open.requested out = " + biOut);
	
	boolean tryFlip = true;
	boolean result;
	
	result = VCM.icDecompressBegin(vcmHandle, biIn, biOut);

	if (result == false) {
	    if (debug) System.err.println("VCM.open : Could not set i/o format");
	    close();
	    throw new ResourceUnavailableException("Formats not supported");
	}
	if (debug) System.err.println("VCM.open success!");
	super.open();
    }

    public void close() {
	if (vcmHandle != 0)
	    VCM.icClose(vcmHandle);
	vcmHandle = 0;
	super.close();
    }

    public void reset() {
	if (vcmHandle != 0) {
	    VCM.icDecompressEnd(vcmHandle);
	    VCM.icDecompressBegin(vcmHandle, biIn, biOut);
	}
    }

    public int process(Buffer inBuffer, Buffer outBuffer) {
	if (isEOM(inBuffer)) {
	    propagateEOM(outBuffer);
	    return BUFFER_PROCESSED_OK;
	}

	Format outf = outBuffer.getFormat();
	outBuffer.setFormat(outputFormat);
	Object outBuf = validateData(outBuffer, 0, true);
	int bufType = 1;

	if (outputFormat.getDataType() == Format.shortArray) {
	    bufType = 2;
	}

	Object inBuf = getInputData(inBuffer);
	long flags = 0;
	int size = inBuffer.getLength();
	if (size < 2)
	    return BUFFER_PROCESSED_FAILED;
	Object header = inBuffer.getHeader();
	if (header != null && header instanceof Integer) {
	    flags = ((Integer)header).intValue();
	}

	if (dropFrame)
	    flags |= 0x20000000L;

	biIn.biSizeImage = size;
	// biOut.biHeight = -outputFormat.getSize().height;
	int result;
	synchronized (processLock) {
	    long outBytes = getNativeData(outBuf);
	    long inBytes = getNativeData(inBuf);
	    if (!keepOutputRef) {
		result = VCM.icDecompress(vcmHandle, flags,
					  biIn, inBuf, inBytes, biOut, outBuf, outBytes,
					  bufType);
	    } else {
		if (outputRef == null)
		    outputRef = new NBA(byte[].class, outputFormat.getMaxDataLength());
		result = VCM.icDecompress(vcmHandle, flags, biIn, inBuf, inBytes,
					  biOut, null,
					  outputRef.getNativeData(), bufType);
		if (outBytes != 0) {
		    outputRef.copyTo((NBA)outBuf);
		} else {
		    outputRef.copyTo((byte[])outBuf);
		}
	    }
	}
	if (dropFrame)
	    outBuffer.setDiscard(true);
	else
	    outBuffer.setLength(outputFormat.getMaxDataLength());

	if (result < 0)
	    return BUFFER_PROCESSED_FAILED;
	else
	    return BUFFER_PROCESSED_OK;
    }

    private Control [] controls = null;
    private DC dc = null;

    public Object [] getControls() {
	if (dc == null) {
	    dc = new DC();
	    controls = new Control[1];
	    controls[0] = dc;
	}

	return controls;
    }

    public String getName() {
	return "VCM Decoder";
    }

    void dropNextFrame(boolean on) {
	dropFrame = on;
    }

    public Format [] getBaseInputFormats() {
	Format [] formats = new Format[1];

	formats[0] = new VideoFormat(null);
	return formats;
    }

    public Format [] getBaseOutputFormats() {
	Format [] formats = new Format[1];
	formats[0] = new RGBFormat();
	return formats;
    }
    
    /****************************************************************
     * Decoder Control
     ****************************************************************/
    
    class DC implements FrameProcessingControl, QualityControl {

	public Component getControlComponent() {
	    return null;
	}
	
	public boolean setMinimalProcessing(boolean on) {
	    dropNextFrame(true);
	    return true;
	}
	
	/**
	 * Informs the codec that it is behind by some number of frames
	 * and that it needs to either speed up by dropping quality or by
	 * dropping frames as it sees fit. The value <code>framesBehind</code>
	 * can either be positive, zero or negative. A negative value indicates
	 * that the codec is ahead by that many frames and can possibly improve
	 * quality if its not at maximum. This method needs to be called before a
	 * call to <code>process</code>. The value is remembered by the codec
	 * until it is explicitly changed again.
	 */
	public void setFramesBehind(float framesBehind) {
	    if (framesBehind > 0)
		dropNextFrame(true);
	    else
		dropNextFrame(false);
	}

        public int getFramesDropped() {
            return 0;       ///XXX not implemented
        }

	/**
	 * Set the quality for the decoding or encoding. This value may have
	 * different
	 * effects depending on the type of compression. A higher quality
	 * setting will result in better quality of the resulting bits, e.g.
	 * better image quality for video.  There is usually a tradeoff between
	 * CPU usage and the quality; in that higher quality requires higher
	 * CPU usage.  This value is
	 * only a hint and the codec can choose to ignore it. The actual value
	 * that was set is returned<p>.
	 * It should be in the range of 0.0 to 1.0.
	 * @see #getQuality
	 */
	public float setQuality(float quality) {
	    return 1.0f;
	}

	/**
	 * Returns the current value of the compression quality parameter.
	 */
	public float getQuality() {
	    return 1.0f;
	}

	/**
	 * Return the default compression quality recommended for
	 * this codec.
	 */
	public float getPreferredQuality() {
	    return 1.0f;
	}

	public boolean isTemporalSpatialTradeoffSupported() {
	    return true;
	}
    }
}


