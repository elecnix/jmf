/*
 * @(#)NativeDecoder.java	1.22 03/04/24
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.video.cinepak;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.ibm.media.codec.video.*;
import java.awt.Dimension;
import com.sun.media.util.Arch;

public final class NativeDecoder extends VideoCodec {


    static {
	JMFSecurityManager.loadLibrary("jmutil");
	JMFSecurityManager.loadLibrary("jmcvid");
    }

    ////////////////////////////////////////////////////////////////////////////
    // Constants

    // RGB bit masks
    private int rMask = 0x000000ff;
    private int gMask = 0x0000ff00;
    private int bMask = 0x00ff0000;
    private NBA refData;

    private native boolean initNative(int bitsPerPixel,
				     int rMask, int gMask, int bMask);
    private native boolean freeNative();
    private native boolean decodeNative(int bytesPerPixel,
					Object inData, long outDataBytes,
					Object outCopy, long outCopyBytes,
					int outMaxLength);
    


    ////////////////////////////////////////////////////////////////////////////
    // Variables
    /** reference to the native data structure **/
    private int nativeData;
    /** reference to the color map conversion **/
    private int [] colorMap = null;

    public NativeDecoder() {
	supportedInputFormats = new VideoFormat[] {new VideoFormat(VideoFormat.CINEPAK) };
        defaultOutputFormats  = new VideoFormat[] {
	        new RGBFormat(
                null, Format.NOT_SPECIFIED, Format.intArray,
		Format.NOT_SPECIFIED, // frame rate
		32,
		0xff, 0xff00, 0xff0000,
		1,Format.NOT_SPECIFIED,
		Format.FALSE, // flipped
		Format.NOT_SPECIFIED // endian
                ) ,

	        new RGBFormat(
                null, Format.NOT_SPECIFIED, Format.intArray,
		Format.NOT_SPECIFIED, // frame rate
		32,
		0xff0000, 0xff00, 0xff,
		1,Format.NOT_SPECIFIED,
		Format.FALSE, // flipped
		Format.NOT_SPECIFIED // endian
		) ,

	        new RGBFormat(
                null, Format.NOT_SPECIFIED, Format.shortArray,
		Format.NOT_SPECIFIED, // frame rate
		16,
		0xf800, 0x07e0, 0x001f,
		1,Format.NOT_SPECIFIED,
		Format.FALSE, // flipped
		Format.NOT_SPECIFIED // endian
		),

	        new RGBFormat(
                null, Format.NOT_SPECIFIED, Format.shortArray,
		Format.NOT_SPECIFIED, // frame rate
		16,
		0x7c00, 0x03e0, 0x001f,
		1,Format.NOT_SPECIFIED,
		Format.FALSE, // flipped
		Format.NOT_SPECIFIED // endian
		) ,

	        new RGBFormat(
                null, Format.NOT_SPECIFIED, Format.byteArray,
		Format.NOT_SPECIFIED, // frame rate
		24,
		1, 2, 3,
		3,Format.NOT_SPECIFIED,
		Format.FALSE, // flipped
		Format.NOT_SPECIFIED // endian
		) ,

	        new RGBFormat(
                null, Format.NOT_SPECIFIED, Format.byteArray,
		Format.NOT_SPECIFIED, // frame rate
		24,
		3, 2, 1,
		3,Format.NOT_SPECIFIED,
		Format.FALSE, // flipped
		Format.NOT_SPECIFIED // endian
		) };


        PLUGIN_NAME = "Cinepak Decoder";

    }

    protected  Format[] getMatchingOutputFormats(Format in) {
      	VideoFormat     ivf  = (VideoFormat) in;
	Dimension       inSize = ivf.getSize();
        int lineStride = (inSize.width + 3) & 0xFFFFFFFC;
	int rowStride = (inSize.height + 3) & 0xFFFFFFFC;
        int length = lineStride * rowStride;

        supportedOutputFormats= new  VideoFormat[] {
	        new RGBFormat(
                inSize, length, Format.intArray,
                ivf.getFrameRate(),			       
		32,
		0xff, 0xff00, 0xff0000,
		1,lineStride,
		Format.FALSE, // flipped
		Format.NOT_SPECIFIED // endian
		) ,

	        new RGBFormat(
                inSize, length, Format.intArray,
                ivf.getFrameRate(),			       
		32,
		0xff0000, 0xff00, 0xff,
		1,lineStride,
		Format.FALSE, // flipped
		Format.NOT_SPECIFIED // endian
		) ,

	        new RGBFormat(
                inSize, length, Format.shortArray,
                ivf.getFrameRate(),			       
		16,
		0xf800, 0x07e0, 0x001f,
		1,lineStride,
		Format.FALSE, // flipped
		Format.NOT_SPECIFIED // endian
		) ,

	        new RGBFormat(
                inSize, length, Format.shortArray,
                ivf.getFrameRate(),			       
		16,
		0x7c00, 0x03e0, 0x001f,
		1,lineStride,
		Format.FALSE, // flipped
		Format.NOT_SPECIFIED // endian
		) ,

	        new RGBFormat(
                inSize, length * 3 , Format.byteArray,
                ivf.getFrameRate(),			       
		24,
		1, 2, 3,
		3, lineStride * 3,
		Format.FALSE,
		Format.NOT_SPECIFIED
		) ,

	        new RGBFormat(
                inSize, length * 3 , Format.byteArray,
                ivf.getFrameRate(),			       
		24,
		3, 2, 1,
		3, lineStride * 3,
		Format.FALSE,
		Format.NOT_SPECIFIED
		) };

        return  supportedOutputFormats;
    }


    public void open() throws ResourceUnavailableException {
      	try {
//             JMFSecurityManager.loadLibrary("jmutil");
//             JMFSecurityManager.loadLibrary("jmcvid");
            initDecoder();
	    super.open();
            return;
	} catch (Throwable e) {
	}

        System.err.println("could not open "+PLUGIN_NAME);
        throw new ResourceUnavailableException("could not open "+PLUGIN_NAME);
    }

    public void close() {
         freeNative();
	 super.close();
    }

    public void reset() {
    }

    public Format setInputFormat(Format input) {
	Format ret = super.setInputFormat(input);
	if (ret == null)
	    return null;
	if (opened) {
	    Dimension size = ((VideoFormat)input).getSize();
	    RGBFormat oldFormat = (RGBFormat) outputFormat;
	    int lineStride = ((size.width + 3) & 0xFFFFFFFC) * oldFormat.getPixelStride();
	    int rowStride = (size.height + 3) & 0xFFFFFFFC;
	    int length = lineStride * rowStride;
	    
	    outputFormat = new RGBFormat(size,
					 length,
					 oldFormat.getDataType(),
					 ((VideoFormat)input).getFrameRate(),
					 oldFormat.getBitsPerPixel(),
					 oldFormat.getRedMask(),
					 oldFormat.getGreenMask(),
					 oldFormat.getBlueMask(),
					 oldFormat.getPixelStride(),
					 lineStride,
					 oldFormat.getFlipped(),
					 oldFormat.getEndian()
					 );
	}
	return ret;
    }

    // called when video resize is detected, by checkFormat()
    protected void videoResized() {
        initDecoder();
    }

    protected void initDecoder() {
        RGBFormat ovf = (RGBFormat)outputFormat;
        rMask = ovf.getRedMask();
        gMask = ovf.getGreenMask();
        bMask = ovf.getBlueMask();
        int bitsPerPixel = ovf.getBitsPerPixel() ;
	// Translate from position to mask
	if (bitsPerPixel == 24 && ovf.getDataType() == Format.byteArray) {
	    int bypp = bitsPerPixel / 8;
	    if (Arch.isLittleEndian()) {
		rMask = 0xFF << ((rMask - 1) * 8);
		gMask = 0xFF << ((gMask - 1) * 8);
		bMask = 0xFF << ((bMask - 1) * 8);
	    } else {
		rMask = 0xFF << ((rMask - 1) * 8);
		gMask = 0xFF << ((gMask - 1) * 8);
		bMask = 0xFF << ((bMask - 1) * 8);
	    }		
	}
        initNative(bitsPerPixel, rMask, gMask, bMask);
    }


    public int process(Buffer inputBuffer, Buffer outputBuffer) {
        Object outData;

        if (!checkInputBuffer(inputBuffer) ) {
            return BUFFER_PROCESSED_FAILED;
        }

        if (isEOM(inputBuffer) ) {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }

        VideoFormat ivf = (VideoFormat) inputBuffer.getFormat();
        int inLength = inputBuffer.getLength();
        int inMaxLength = ivf.getMaxDataLength();
        int outMaxLength = outputFormat.getMaxDataLength();
        Class outputClass = outputFormat.getDataType() ;
	outputBuffer.setFormat(outputFormat);

        byte[] inData = (byte[]) inputBuffer.getData();

	outData = validateData(outputBuffer, 0, true);

        int bytesPerPixel = ((RGBFormat)outputFormat).getBitsPerPixel() / 8;
	long outDataBytes = 0;

	if (outData instanceof NBA)
	    outDataBytes = ((NBA)outData).getNativeData();
	//System.err.println("outDataBytes = " + outDataBytes);
	
	if (refData == null || refData.getSize() < outMaxLength) {
	    refData = new NBA(outputFormat.getDataType(),
			      outMaxLength);
	}
	//System.err.println("outMaxLength = " + outMaxLength);
	decodeNative(bytesPerPixel, inData, refData.getNativeData(), outData, outDataBytes, outMaxLength);
        updateOutput(outputBuffer, outputFormat, outMaxLength, 0);

        return BUFFER_PROCESSED_OK;
    }



}

