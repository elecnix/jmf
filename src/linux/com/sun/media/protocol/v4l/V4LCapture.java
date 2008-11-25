/*
 * @(#)V4LCapture.java	1.6 03/04/23
 *
 * Copyright (c) 1996-2003 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.protocol.v4l;

import javax.media.Format;
import javax.media.format.*;
import java.awt.Dimension;

class V4LCapture {

    private int nativePeer = 0;

    private boolean supportsMMap = false;
    private boolean supportsRead = false;
    private int cardNo = 0;
    
    static {
	com.sun.media.JMFSecurityManager.loadLibrary("jmv4l");
	// System.loadLibrary("jmv4l");
    }
    
    V4LCapture(int cardNo) {
	nativePeer = nOpen(cardNo);
	if (nativePeer != 0)
	    this.cardNo = cardNo;
	else
	    throw new Error("Can't open video card " + cardNo);
    }

    void close() {
	synchronized (this) {
	    nClose();
	    nativePeer = 0;
	}
    }

    native int nOpen(int cardNo);

    native int nClose();
    
    native int getCapability(VCapability vcap);

    native int getChannel(VChannel vchan);

    native int setChannel(VChannel vchan);

    native int getPicture(VPicture vpict);

    native int setPicture(VPicture vpict);

    native int getTuner(VTuner vtuner);

    native int setTuner(VTuner vtuner);

    native int getFrequency();

    native int setFrequency(int freq);

    native int setFormat(int depth, int palette,
				int width, int height,
				float frameRate);

    native int start();

    native int stop();

    native int readNextFrame(byte [] buffer, int offset, int length);


    int formatToPalette(Format format) {
	if (format == null)
	    return 0;
	
	if (format instanceof RGBFormat) {
	    RGBFormat rgbf = (RGBFormat) format;
	    int bpp = rgbf.getBitsPerPixel();
	    switch (bpp) {
	    case 24:
		return VPicture.VIDEO_PALETTE_RGB24;
	    case 16:
		if (rgbf.getGreenMask() == (0x3F << 5))
		    return VPicture.VIDEO_PALETTE_RGB565;
		else
		    return VPicture.VIDEO_PALETTE_RGB555;
	    case 32:
		return VPicture.VIDEO_PALETTE_RGB32;
	    }
	    return 0;
	} else if (format instanceof YUVFormat) {
	    YUVFormat yuvf = (YUVFormat) format;
	    switch (yuvf.getYuvType()) {
	    case YUVFormat.YUV_YUYV:
		if (yuvf.getOffsetY() == 0)
		    return VPicture.VIDEO_PALETTE_YUV422;
		else
		    // Not necessarily, but because the inverse function supports only these two
		    return VPicture.VIDEO_PALETTE_UYVY;
	    case YUVFormat.YUV_420:
		return VPicture.VIDEO_PALETTE_YUV420P;
	    }
	}
	// Not one of the supported types
	return 0;
    }
	    



	    
    VideoFormat paletteToFormat(int palette, Dimension size) {
	int area = size.width * size.height;
	VideoFormat format;
	
	switch (palette) {
	case VPicture.VIDEO_PALETTE_RGB565 :
	    format = new RGBFormat(size, area * 2, Format.byteArray,
				   Format.NOT_SPECIFIED, 16,
				   0x1F << 11, 0x3F << 5, 0x1F,
				   2, size.width * 2,
				   Format.FALSE, RGBFormat.LITTLE_ENDIAN);
	    break;
	case VPicture.VIDEO_PALETTE_RGB24 :
	    format = new RGBFormat(size, area * 3, Format.byteArray,
				   Format.NOT_SPECIFIED, 24,
				   3, 2, 1,
				   3, size.width * 3,
				   Format.FALSE, Format.NOT_SPECIFIED);
	    break;
	case VPicture.VIDEO_PALETTE_RGB32 :
	    format = new RGBFormat(size, area * 4, Format.byteArray,
				   Format.NOT_SPECIFIED, 32,
				   1, 2, 3,
				   4, size.width * 4,
				   Format.FALSE, Format.NOT_SPECIFIED);
	    break;
	case VPicture.VIDEO_PALETTE_RGB555 :
	    format = new RGBFormat(size, area * 2, Format.byteArray,
				   Format.NOT_SPECIFIED, 16,
				   0x1F << 10, 0x1F << 5, 0x1F,
				   2, size.width * 2,
				   Format.FALSE, RGBFormat.LITTLE_ENDIAN);
	    break;
	case VPicture.VIDEO_PALETTE_YUV422 :
	case VPicture.VIDEO_PALETTE_YUYV :
	    format = new YUVFormat(size, area * 2, Format.byteArray,
				   Format.NOT_SPECIFIED, YUVFormat.YUV_YUYV,
				   size.width * 2, size.width * 2,
				   0, 1, 3);
	    break;
	case VPicture.VIDEO_PALETTE_UYVY :
	    format = new YUVFormat(size, area * 2, Format.byteArray,
				   Format.NOT_SPECIFIED, YUVFormat.YUV_YUYV,
				   size.width * 2, size.width * 2,
				   1, 0, 2);
	    break;
	case VPicture.VIDEO_PALETTE_YUV420P :
	    format = new YUVFormat(size, area + (area / 2), Format.byteArray,
				   Format.NOT_SPECIFIED, YUVFormat.YUV_420,
				   size.width, size.width / 2,
				   0, area, area + (area / 4));
	    break;

	// TODO : Rest of the formats

	default :
	    format = null;
	}

	return format;
    }

    int paletteToDepth(int palette) {
	int p2d [] = {
	    0,
	    8,  // VIDEO_PALETTE_GREY
	    8,  // VIDEO_PALETTE_HI240
	    16, // VIDEO_PALETTE_RGB565
	    24, // VIDEO_PALETTE_RGB24
	    32, // VIDEO_PALETTE_RGB32
	    16, // VIDEO_PALETTE_RGB555
	    16, // VIDEO_PALETTE_YUV422
	    16, // VIDEO_PALETTE_YUYV
	    16, // VIDEO_PALETTE_UYVY
	    12, // VIDEO_PALETTE_YUV420
	    12, // VIDEO_PALETTE_YUV411
	    12, // VIDEO_PALETTE_RAW
	    16, // VIDEO_PALETTE_YUV422P
	    12, // VIDEO_PALETTE_YUV411P
	    12, // VIDEO_PALETTE_YUV420P
	    12, // VIDEO_PALETTE_YUV410P
	    0, 0, 0, 0, 0, 0, 0, 0, 0, 0
	};
	return p2d[palette];
    }
    
//     public static void main(String [] args) {
// 	V4LCapture cap = new V4LCapture(0);
// 	VCapability vcap = new VCapability();
// 	cap.getCapability(vcap);
// 	cap.close();
// 	System.err.println("Card = " + vcap.name);
// 	System.err.println("Channels = " + vcap.channels);
//     }
}
