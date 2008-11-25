/*
 * @(#)BitMapInfo.java	1.22 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.vfw;

import javax.media.Format;
import javax.media.format.*;
import com.sun.media.format.*;
import java.awt.Dimension;

public class BitMapInfo {

    public int biWidth = 0;
    public int biHeight = 0;
    public int biPlanes = 1;
    public int biBitCount = 24;
    // equivalent of biCompression
    public String fourcc = "";
    public int biSizeImage = 0;
    public int biXPelsPerMeter = 0;
    public int biYPelsPerMeter = 0;
    public int biClrUsed = 0;
    public int biClrImportant = 0;
    public int extraSize = 0;
    public byte [] extraBytes = null;
    
    public BitMapInfo() {

    }

    public BitMapInfo(String fourcc, int width, int height) {
	this.biWidth = width;
	this.biHeight = height;
	this.fourcc = fourcc;
	if (fourcc.equals("RGB"))
	    biSizeImage = width * height * 3;
	if (fourcc.equals("MSVC"))
	    this.fourcc = "CRAM";
    }

    public BitMapInfo(String fourcc, int width, int height,
		      int planes, int bitcount, int sizeImage,
		      int clrused, int clrimportant) {
	this(fourcc, width, height);

	biPlanes = planes;
	biBitCount = bitcount;
	biSizeImage = sizeImage;
	biClrUsed = clrused;
	biClrImportant = clrimportant;
    }

    public BitMapInfo(VideoFormat format) {
	Dimension size = format.getSize();
	if (size == null)
	    size = new Dimension(320, 240);
	Class arrayType = format.getDataType();
	int elSize = (arrayType == Format.byteArray) ? 1 :
	    ((arrayType == Format.intArray) ? 4 : 2);

	biWidth = size.width;
	biHeight = size.height;
	biPlanes = 1;
	biSizeImage = format.getMaxDataLength() * elSize;
	fourcc = format.getEncoding();
	if (fourcc.equalsIgnoreCase("msvc")) {
	    fourcc = "CRAM";
	}
	if (format instanceof AviVideoFormat) {
	    AviVideoFormat avif = (AviVideoFormat) format;
	    biPlanes = avif.getPlanes();
	    biBitCount = avif.getBitsPerPixel();
	    biXPelsPerMeter = avif.getXPelsPerMeter();
	    biYPelsPerMeter = avif.getYPelsPerMeter();
	    biClrUsed = avif.getClrUsed();
	    biClrImportant = avif.getClrImportant();
	    extraBytes = avif.getCodecSpecificHeader();
	    if (extraBytes != null)
		extraSize = extraBytes.length;
	} else if (format instanceof RGBFormat) {
	    RGBFormat rgb = (RGBFormat) format;
	    fourcc = "RGB";
	    biBitCount = rgb.getBitsPerPixel();
	    if (rgb.getFlipped() == Format.FALSE)
		biHeight = -biHeight;
	} else if (format instanceof YUVFormat) {
	    YUVFormat yuv = (YUVFormat) format;
	    switch (yuv.getYuvType()) {
	    case YUVFormat.YUV_420:
		if (yuv.getOffsetU() < yuv.getOffsetV())
		    fourcc = "I420";
		else
		    fourcc = "YV12";
	    case YUVFormat.YUV_YUYV:
		if (yuv.getOffsetY() == 0 && yuv.getOffsetU() == 1)
		    fourcc = "YUY2";
		else if (yuv.getOffsetY() == 0 && yuv.getOffsetU() == 3)
		    fourcc = "YVYU";
		else if (yuv.getOffsetU() == 0)
		    fourcc = "UYVY";
	    }
	    if (fourcc.equalsIgnoreCase("yv12") ||
		fourcc.equalsIgnoreCase("i420") ||
		fourcc.equalsIgnoreCase("y411") ) {
		biBitCount = 12;
	    } else if (fourcc.equalsIgnoreCase("yuy2")) {
		biBitCount = 16;
	    }
	}
    }

    public VideoFormat createVideoFormat(Class arrayType) {
	return createVideoFormat(arrayType, Format.NOT_SPECIFIED);
    }

    public VideoFormat createVideoFormat(Class arrayType, float frameRate) {
	VideoFormat format;
	if (fourcc.equalsIgnoreCase(VideoFormat.RGB)) {
	    // Assume its one of byte[], short[] and int[]
	    int elSize = (arrayType == Format.byteArray) ? 1 :
		          ((arrayType == Format.intArray) ? 4 : 2);
	    // Size of the array
	    int maxDataLength = biSizeImage / elSize;
	    int rm = Format.NOT_SPECIFIED, gm = Format.NOT_SPECIFIED, bm = Format.NOT_SPECIFIED;
	    if (biBitCount == 16) {
		rm = 0x7C00; gm = 0x03E0; bm = 0x001F;
	    } else if (biBitCount == 32) {
		if (elSize == 4)
		    { rm = 0xFF0000; gm = 0x00FF00; bm = 0x0000FF; }
		else
		    { rm = 3; gm = 2; bm = 1; }
	    } else if (biBitCount == 24) {
		rm = 3; gm = 2; bm = 1;
	    }
	    int bytesPerLine = biWidth * biBitCount / 8;
	    int lineStride = bytesPerLine / elSize;
	    int pixelStride = lineStride / biWidth;
	    int actualHeight = biHeight;
	    int flipped = Format.TRUE;
	    
	    if (biHeight < 0) {
		actualHeight = -actualHeight;
		flipped = Format.FALSE;
	    }
	    
	    format = new RGBFormat(new java.awt.Dimension(biWidth, actualHeight),
				   maxDataLength, arrayType,
				   frameRate,
				   biBitCount,
				   rm, gm, bm, pixelStride, lineStride,
				   /*flipped */ flipped,
				   RGBFormat.LITTLE_ENDIAN);
	    /*	} else if (fourcc.equalsIgnoreCase("yvu9")) {
	    int ySize = biWidth * biHeight;
	    System.err.println("ySize = " + ySize +
			       "\nbiHeight = " + biHeight +
			       "\nbiWidth = " + biWidth +
			       "\nbiSizeImage = " + biSizeImage);
	    format = new YUVFormat(new java.awt.Dimension(biWidth, biHeight),
				   biSizeImage, Format.byteArray,
				   frameRate,
				   YUVFormat.YUV_YVU9,
				   biWidth, biWidth / 4,
				   0, ySize + ySize / 16, ySize);
	    */
	} else if (fourcc.equalsIgnoreCase("yuy2")) {
	    int ySize = biWidth * biHeight;
	    
	    format = new YUVFormat(new java.awt.Dimension(biWidth, biHeight),
				   biSizeImage, // max size
				   byte[].class, // type
				   frameRate,
				   YUVFormat.YUV_YUYV, // yuv type
				   biWidth * 2, biWidth * 2,
				   0, 1, 3);
	    
	} else if (fourcc.equalsIgnoreCase("i420")) {
	    int ySize = biWidth * biHeight;
	    
	    format = new YUVFormat(new java.awt.Dimension(biWidth, biHeight),
				   biSizeImage, // max size
				   byte[].class, // type
				   frameRate,
				   YUVFormat.YUV_420, // yuv type
				   biWidth, biWidth / 2,
				   0, ySize, ySize + ySize / 4);
	} else if (fourcc.equalsIgnoreCase("yv12")) {
	    int ySize = biWidth * biHeight;
	    
	    format = new YUVFormat(new java.awt.Dimension(biWidth, biHeight),
				   biSizeImage, // max size
				   byte[].class, // type
				   frameRate,
				   YUVFormat.YUV_420, // yuv type
				   biWidth, biWidth / 2,
				   0, ySize + ySize / 4, ySize);
	    
	} else {
	    format = new AviVideoFormat(fourcc,
					new java.awt.Dimension(biWidth, biHeight),
					biSizeImage, arrayType,
					frameRate, // Format.NOT_SPECIFIED,
					biPlanes, biBitCount,
					biSizeImage,
					biXPelsPerMeter,
					biYPelsPerMeter,
					biClrUsed,
					biClrImportant,
					extraBytes);
	}
	return format;
    }

    public String toString() {
	String s = "Size = " + biWidth + " x " + biHeight + "\t" +
	    "Planes = " + biPlanes + "\t" +
	    "BitCount = " + biBitCount + "\t" +
	    "FourCC = " + fourcc + "\t" +
	    "SizeImage = " + biSizeImage + "\n" +
	    "ClrUsed = " + biClrUsed + "\n" +
	    "ClrImportant = " + biClrImportant + "\n" +
	    "ExtraSize = " + extraSize + "\n";
	if (extraSize > 0) {
	    for (int i = 0; i < extraSize; i++)
		s += "\t" + i + " = " + extraBytes[i] + "\n";
	}
	return s;
    }
}
