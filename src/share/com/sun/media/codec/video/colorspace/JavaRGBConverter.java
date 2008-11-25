/*
 * @(#)JavaRGBConverter.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.video.colorspace;

import javax.media.Format;
import javax.media.format.*;
import javax.media.Buffer;
import com.sun.media.BasicCodec;
import java.awt.Dimension;

public class JavaRGBConverter extends RGBConverter {

    private static final String PLUGIN_NAME = "RGB Converter";
    
    public JavaRGBConverter() {
	inputFormats = new Format [] { new RGBFormat() };
	outputFormats = new Format [] { new RGBFormat() };
    }

    public String getName() {
	return PLUGIN_NAME;
    }
    
    protected void componentToComponent(Object inData, int inPS, int inLS, int inBPP,
					int inRed, int inGreen, int inBlue,
					boolean inPacked, int inEndian,
					Object outData, int outPS, int outLS, int outBPP,
					int outRed, int outGreen, int outBlue,
					boolean outPacked, int outEndian,
					int width, int height, boolean flip) {
	int srcPtr = 0;
	int dstPtr = 0;
	int srcInc = inLS - (width * inPS);
	int dstInc = outLS - (width * outPS);
	int x, y;
	if (flip) {
	    // Write the output bottom-up.
	    dstPtr = outLS * (height - 1);
	    dstInc = - ((3 * outLS) - (width * outPS));
	}

	if (inPacked && outPacked) {
	    int [] in = (int[])inData;
	    int [] out = (int[])outData;
	    if (inRed == outRed && inGreen == outGreen && inBlue == outBlue) {
		for (y = 0; y < height; y++) {
		    for (x = 0; x < width; x++) {
			out[dstPtr] = in[srcPtr];
			srcPtr += inPS;
			dstPtr += outPS;
		    }
		    srcPtr += srcInc;
		    dstPtr += dstInc;
		}
	    } else {
		int inrs = getShift(inRed);
		int ings = getShift(inGreen);
		int inbs = getShift(inBlue);
		int outrs = getShift(outRed);
		int outgs = getShift(outGreen);
		int outbs = getShift(outBlue);
		for (y = 0; y < height; y++) {
		    for (x = 0; x < width; x++) {
			int inPixel = in[srcPtr];
			int outPixel = (((inPixel >> inrs) & 0xFF) << outrs) |
			               (((inPixel >> ings) & 0xFF) << outgs) |
			               (((inPixel >> inbs) & 0xFF) << outbs) ;
			out[dstPtr] = outPixel;
			srcPtr += inPS;
			dstPtr += outPS;
		    }
		    srcPtr += srcInc;
		    dstPtr += dstInc;
		}
	    }
	} else if (inPacked && !outPacked) {
	    int [] in = (int[])inData;
	    byte [] out = (byte[])outData;
	    int redShift = getShift(inRed);
	    int greenShift = getShift(inGreen);
	    int blueShift = getShift(inBlue);
	    for (y = 0; y < height; y++) {
		for (x = 0; x < width; x++) {
		    int pixel = in[srcPtr];
		    byte red = (byte) ((pixel >> redShift) & 0xFF);
		    byte green = (byte) ((pixel >> greenShift) & 0xFF);
		    byte blue = (byte) ((pixel >> blueShift) & 0xFF);
		    out[dstPtr + outRed - 1] = red;
		    out[dstPtr + outGreen - 1] = green;
		    out[dstPtr + outBlue - 1] = blue;
		    srcPtr += inPS;
		    dstPtr += outPS;
		}
		srcPtr += srcInc;
		dstPtr += dstInc;
	    }
	} else if (!inPacked && outPacked) {
	    byte [] in = (byte[])inData;
	    int [] out = (int[])outData;
	    int redShift = getShift(outRed);
	    int greenShift = getShift(outGreen);
	    int blueShift = getShift(outBlue);
	    for (y = 0; y < height; y++) {
		for (x = 0; x < width; x++) {
		    byte red = in[srcPtr + inRed - 1];
		    byte green = in[srcPtr + inGreen - 1];
		    byte blue = in[srcPtr + inBlue - 1];
		    int pixel = ((red & 0xFF) << redShift) |
			        ((green & 0xFF) << greenShift) |
			        ((blue & 0xFF) << blueShift);
		    out[dstPtr] = pixel;
		    srcPtr += inPS;
		    dstPtr += outPS;
		}
		srcPtr += srcInc;
		dstPtr += dstInc;
	    }
	} else if (!inPacked && !outPacked) {
	    byte [] in = (byte[])inData;
	    byte [] out = (byte[])outData;
	    for (y = 0; y < height; y++) {
		for (x = 0; x < width; x++) {
		    out[dstPtr + outRed - 1] = in[srcPtr + inRed - 1];
		    out[dstPtr + outGreen - 1] = in[srcPtr + inGreen - 1];
		    out[dstPtr + outBlue - 1] = in[srcPtr + inBlue - 1];
		    srcPtr += inPS;
		    dstPtr += outPS;
		}
		srcPtr += srcInc;
		dstPtr += dstInc;
	    }
	}
    }

    protected void componentToSixteen(Object inData, int inPS, int inLS, int inBPP,
				      int inRed, int inGreen, int inBlue,
				      boolean inPacked, int inEndian,
				      Object outData, int outPS, int outLS, int outBPP,
				      int outRed, int outGreen, int outBlue,
				      boolean outPacked, int outEndian,
				      int width, int height, boolean flip) {
	int srcPtr = 0;
	int dstPtr = 0;
	int srcInc = inLS - (width * inPS);
	int dstInc = outLS - (width * outPS);
	int x, y;
	int outrs = getShift(outRed) - 3;
	int outgs = getShift(outGreen) - ((outGreen == 0x07E0)? 2 : 3);
	int inrs = getShift(inRed);
	int ings = getShift(inGreen);
	int inbs = getShift(inBlue);
	int outfs = 0;
	int outss = 0;
	
	if (!outPacked) {
	    if (outEndian == RGBFormat.BIG_ENDIAN) {
		outfs = 8;
		outss = 0;
	    } else {
		outfs = 0;
		outss = 8;
	    }
	}

	if (flip) {
	    // Write the output bottom-up.
	    dstPtr = outLS * (height - 1);
	    dstInc = - ((3 * outLS) - (width * outPS));
	}

	if (inPacked && outPacked) {
	    int [] in = (int[]) inData;
	    short [] out = (short[]) outData;

	    for (y = 0; y < height; y++) {
		for (x = 0; x < width; x++) {
		    int pixel = in[srcPtr];
		    out[dstPtr] = (short) ((((pixel >> inrs) << outrs) & outRed) |
			                   (((pixel >> ings) << outgs) & outGreen) |
			                   (((pixel >> inbs) & 0xFF) >> 3));
		    srcPtr += inPS;
		    dstPtr += outPS;
		}
		srcPtr += srcInc;
		dstPtr += dstInc;
	    }
	} else if (!inPacked && outPacked) {
	    byte [] in = (byte[]) inData;
	    short [] out = (short[]) outData;

	    for (y = 0; y < height; y++) {
		for (x = 0; x < width; x++) {
		    out[dstPtr] = (short)
			(((in[srcPtr + inRed - 1] << outrs) & outRed) |
			 ((in[srcPtr + inGreen - 1] << outgs) & outGreen) |
			 ((in[srcPtr + inBlue - 1] & 0xFF) >> 3));
		    srcPtr += inPS;
		    dstPtr += outPS;
		}
		srcPtr += srcInc;
		dstPtr += dstInc;
	    }
	} else if (!inPacked && !outPacked) {
	    byte [] in = (byte[]) inData;
	    byte [] out = (byte[]) outData;

	    for (y = 0; y < height; y++) {
		for (x = 0; x < width; x++) {
		    int pixel = (((in[srcPtr + inRed - 1] << outrs) & outRed) |
				 ((in[srcPtr + inGreen - 1] << outgs) & outGreen) |
				 ((in[srcPtr + inBlue - 1] & 0xFF) >> 3));
		    out[dstPtr] = (byte) (pixel >> outfs);
		    out[dstPtr + 1] = (byte) (pixel >> outss);
		    srcPtr += inPS;
		    dstPtr += outPS;
		}
		srcPtr += srcInc;
		dstPtr += dstInc;
	    }
	} else { // inPacked && !outPacked
	    int [] in = (int[]) inData;
	    byte [] out = (byte[]) outData;

	    for (y = 0; y < height; y++) {
		for (x = 0; x < width; x++) {
		    int pixel = in[srcPtr];
		    pixel = (int) ((((pixel >> inrs) << outrs) & outRed) |
				   (((pixel >> ings) << outgs) & outGreen) |
				   (((pixel >> inbs) & 0xFF) >> 3));
		    out[dstPtr] = (byte) (pixel >> outfs);
		    out[dstPtr + 1] = (byte) (pixel >> outss);
		    srcPtr += inPS;
		    dstPtr += outPS;
		}
		srcPtr += srcInc;
		dstPtr += dstInc;
	    }
	}
    }

    protected void sixteenToComponent(Object inData, int inPS, int inLS, int inBPP,
				      int inRed, int inGreen, int inBlue,
				      boolean inPacked, int inEndian,
				      Object outData, int outPS, int outLS, int outBPP,
				      int outRed, int outGreen, int outBlue,
				      boolean outPacked, int outEndian,
				      int width, int height, boolean flip) {
	int srcPtr = 0;
	int dstPtr = 0;
	int srcInc = inLS - (width * inPS);
	int dstInc = outLS - (width * outPS);
	int x, y;
	if (flip) {
	    // Write the output bottom-up.
	    dstPtr = outLS * (height - 1);
	    dstInc = - ((3 * outLS) - (width * outPS));
	}

	int inrs = getShift(inRed) - 3;
	int ings = getShift(inGreen) - ((inGreen == 0x07E0)? 2 : 3);
	int outrs = getShift(outRed);
	int outgs = getShift(outGreen);
	int outbs = getShift(outBlue);
	
	if (inPacked && outPacked) {
	    short [] in = (short[]) inData;
	    int [] out = (int[]) outData;
	    
	    for (y = 0; y < height; y++) {
		for (x = 0; x < width; x++) {
		    int pixel = (int) (in[srcPtr] & 0xFFFF);
		    int outpixel = (((pixel & inRed) >> inrs) << outrs) |
			           (((pixel & inGreen) >> ings) << outgs) |
			           (((pixel & inBlue) << 3) << outbs) ;
		    out[dstPtr] = outpixel;
		    srcPtr += inPS;
		    dstPtr += outPS;
		}
		srcPtr += srcInc;
		dstPtr += dstInc;
	    }
	} else if (!inPacked && outPacked) {
	    byte [] in = (byte[]) inData;
	    int [] out = (int[]) outData;
	    int fbshift, sbshift;

	    if (inEndian == RGBFormat.BIG_ENDIAN) {
		fbshift = 8;
		sbshift = 0;
	    } else {
		fbshift = 0;
		sbshift = 8;
	    }
	    
	    for (y = 0; y < height; y++) {
		for (x = 0; x < width; x++) {
		    int pixel = ((in[srcPtr] & 0xFF) << fbshift) |
			        ((in[srcPtr+1] & 0xFF) << sbshift);
		    int outpixel = (((pixel & inRed) >> inrs) << outrs) |
			           (((pixel & inGreen) >> ings) << outgs) |
			           (((pixel & inBlue) << 3) << outbs) ;
		    out[dstPtr] = outpixel;
		    srcPtr += inPS;
		    dstPtr += outPS;
		}
		srcPtr += srcInc;
		dstPtr += dstInc;
	    }		    
	} else if (inPacked && !outPacked) {
	    short [] in = (short[]) inData;
	    byte [] out = (byte[]) outData;
	    
	    for (y = 0; y < height; y++) {
		for (x = 0; x < width; x++) {
		    int pixel = (int) (in[srcPtr]);
		    out[dstPtr + outRed - 1] = (byte)((pixel & inRed) >> inrs);
		    out[dstPtr + outGreen - 1] = (byte)((pixel & inGreen) >> ings);
		    out[dstPtr + outBlue - 1] = (byte)((pixel & inBlue) << 3);
		    srcPtr += inPS;
		    dstPtr += outPS;
		}
		srcPtr += srcInc;
		dstPtr += dstInc;
	    }
	} else {
	    byte [] in = (byte[]) inData;
	    byte [] out = (byte[]) outData;
	    int fbshift, sbshift;

	    if (inEndian == RGBFormat.BIG_ENDIAN) {
		fbshift = 8;
		sbshift = 0;
	    } else {
		fbshift = 0;
		sbshift = 8;
	    }

	    for (y = 0; y < height; y++) {
		for (x = 0; x < width; x++) {
		    int pixel = ((in[srcPtr] & 0xFF) << fbshift) |
			        ((in[srcPtr+1] & 0xFF) << sbshift);
		    out[dstPtr + outRed - 1] = (byte)((pixel & inRed) >> inrs);
		    out[dstPtr + outGreen - 1] = (byte)((pixel & inGreen) >> ings);
		    out[dstPtr + outBlue - 1] = (byte)((pixel & inBlue) << 3);
		    srcPtr += inPS;
		    dstPtr += outPS;
		}
		srcPtr += srcInc;
		dstPtr += dstInc;
	    }
	}
    }

    protected void sixteenToSixteen(Object inData, int inPS, int inLS, int inBPP,
				    int inRed, int inGreen, int inBlue,
				    boolean inPacked, int inEndian,
				    Object outData, int outPS, int outLS, int outBPP,
				    int outRed, int outGreen, int outBlue,
				    boolean outPacked, int outEndian,
				    int width, int height, boolean flip) {
	int srcPtr = 0;
	int dstPtr = 0;
	int srcInc = inLS - (width * inPS);
	int dstInc = outLS - (width * outPS);
	int x, y;
	int shift = 0;
	int infs = 0;  // input first shift (for endian)
	int inss = 0;  // input second shift 
	int outfs = 0; // output first shift
	int outss = 0; // output second shift

	if (flip) {
	    // Write the output bottom-up.
	    dstPtr = outLS * (height - 1);
	    dstInc = - ((3 * outLS) - (width * outPS));
	}

	if (!inPacked) {
	    if (inEndian == RGBFormat.BIG_ENDIAN) {
		infs = 8;
		inss = 0;
	    } else {
		infs = 0;
		inss = 8;
	    }
	}

	if (!outPacked) {
	    if (outEndian == RGBFormat.BIG_ENDIAN) {
		outfs = 8;
		outss = 0;
	    } else {
		outfs = 0;
		outss = 8;
	    }
	}

	if (inRed != outRed || inGreen != outGreen) {
	    if (inRed > outRed)
		shift = 1; // shift red/green to the right
	    else
		shift = -1; // shift red/green to the left
	}

	if (inPacked && outPacked) {
	    short [] in = (short[]) inData;
	    short [] out = (short[]) outData;

	    if (shift == 0) {
		for (y = 0; y < height; y++) {
		    for (x = 0; x < width; x++) {
			out[dstPtr] = in[srcPtr];
			srcPtr += inPS;
			dstPtr += outPS;
		    }
		    srcPtr += srcInc;
		    dstPtr += dstInc;
		}
	    } else if (shift == 1) {
		for (y = 0; y < height; y++) {
		    for (x = 0; x < width; x++) {
			int pixel = in[srcPtr];
			out[dstPtr] = (short) (((pixel >> 1) & (outGreen | outRed)) |
			                       (pixel & outBlue));
			srcPtr += inPS;
			dstPtr += outPS;
		    }
		    srcPtr += srcInc;
		    dstPtr += dstInc;
		}
	    } else {
		for (y = 0; y < height; y++) {
		    for (x = 0; x < width; x++) {
			int pixel = in[srcPtr];
			out[dstPtr] = (short) (((pixel & (inGreen | inRed)) << 1) |
			                       (pixel & outBlue));
			srcPtr += inPS;
			dstPtr += outPS;
		    }
		    srcPtr += srcInc;
		    dstPtr += dstInc;
		}
	    }
	} else if (!inPacked && outPacked) {
	    byte [] in = (byte[]) inData;
	    short [] out = (short[]) outData;

	    if (shift == 0) {
		for (y = 0; y < height; y++) {
		    for (x = 0; x < width; x++) {
			int pixel = ((in[srcPtr] & 0xFF) << infs) |
			            ((in[srcPtr + 1] & 0xFF) << inss);
			out[dstPtr] = (short) pixel;
			srcPtr += inPS;
			dstPtr += outPS;
		    }
		    srcPtr += srcInc;
		    dstPtr += dstInc;
		}
	    } else if (shift == 1) {
		for (y = 0; y < height; y++) {
		    for (x = 0; x < width; x++) {
			int pixel = ((in[srcPtr] & 0xFF) << infs) |
			            ((in[srcPtr + 1] & 0xFF) << inss);
			out[dstPtr] = (short) (((pixel >> 1) & (outGreen | outRed)) |
					       (pixel & outBlue));
			srcPtr += inPS;
			dstPtr += outPS;
		    }
		    srcPtr += srcInc;
		    dstPtr += dstInc;
		}
	    } else {
		for (y = 0; y < height; y++) {
		    for (x = 0; x < width; x++) {
			int pixel = ((in[srcPtr] & 0xFF) << infs) |
			            ((in[srcPtr + 1] & 0xFF) << inss);
			out[dstPtr] = (short) (((pixel & (inGreen | inRed)) << 1) |
					       (pixel & outBlue));
			srcPtr += inPS;
			dstPtr += outPS;
		    }
		    srcPtr += srcInc;
		    dstPtr += dstInc;
		}
	    }
	} else if (!inPacked && !outPacked) {
	    byte [] in = (byte[]) inData;
	    byte [] out = (byte[]) outData;

	    if (shift == 0) {
		if (inEndian == outEndian) {
		    for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
			    out[dstPtr] = in[srcPtr];
			    out[dstPtr + 1] = in[srcPtr + 1];
			    srcPtr += inPS;
			    dstPtr += outPS;
			}
			srcPtr += srcInc;
			dstPtr += dstInc;
		    }
		} else {
		    for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
			    int pixel = ((in[srcPtr] & 0xFF) << infs) |
				        ((in[srcPtr + 1] & 0xFF) << inss);
			    out[dstPtr] = (byte) (pixel >> outfs);
			    out[dstPtr + 1] = (byte) (pixel >> outss);
			    srcPtr += inPS;
			    dstPtr += outPS;
			}
			srcPtr += srcInc;
			dstPtr += dstInc;
		    }
		}
	    } else if (shift == 1) {
		for (y = 0; y < height; y++) {
		    for (x = 0; x < width; x++) {
			int pixel = ((in[srcPtr] & 0xFF) << infs) |
			            ((in[srcPtr + 1] & 0xFF) << inss);
			pixel = ((pixel >> 1) & (outGreen | outRed)) |
			        (pixel & outBlue);
			out[dstPtr] = (byte) (pixel >> outfs);
			out[dstPtr + 1] = (byte) (pixel >> outss);
			srcPtr += inPS;
			dstPtr += outPS;
		    }
		    srcPtr += srcInc;
		    dstPtr += dstInc;
		}
	    } else {
		for (y = 0; y < height; y++) {
		    for (x = 0; x < width; x++) {
			int pixel = ((in[srcPtr] & 0xFF) << infs) |
			            ((in[srcPtr + 1] & 0xFF) << inss);
			pixel = ((pixel >> 1) & (outGreen | outRed)) |
			        (pixel & outBlue);
			out[dstPtr] = (byte) (pixel >> outfs);
			out[dstPtr + 1] = (byte) (pixel >> outss);
			srcPtr += inPS;
			dstPtr += outPS;
		    }
		    srcPtr += srcInc;
		    dstPtr += dstInc;
		}
	    }		
	} else {  // inPacked && !outPacked
	    short [] in = (short[]) inData;
	    byte [] out = (byte[]) outData;

	    if (shift == 0) {
		for (y = 0; y < height; y++) {
		    for (x = 0; x < width; x++) {
			int pixel = in[srcPtr];
			out[dstPtr] = (byte) (pixel >> outfs);
			out[dstPtr + 1] = (byte) (pixel >> outss);
			srcPtr += inPS;
			dstPtr += outPS;
		    }
		    srcPtr += srcInc;
		    dstPtr += dstInc;
		}
	    } else if (shift == 1) {
		for (y = 0; y < height; y++) {
		    for (x = 0; x < width; x++) {
			int pixel = in[srcPtr];
			pixel = ((pixel >> 1) & (outGreen | outRed)) |
			        (pixel & outBlue);
			out[dstPtr] = (byte) (pixel >> outfs);
			out[dstPtr + 1] = (byte) (pixel >> outss);
			srcPtr += inPS;
			dstPtr += outPS;
		    }
		    srcPtr += srcInc;
		    dstPtr += dstInc;
		}
	    } else {
		for (y = 0; y < height; y++) {
		    for (x = 0; x < width; x++) {
			int pixel = in[srcPtr];
			pixel = ((pixel >> 1) & (outGreen | outRed)) |
			        (pixel & outBlue);
			out[dstPtr] = (byte) (pixel >> outfs);
			out[dstPtr + 1] = (byte) (pixel >> outss);
			srcPtr += inPS;
			dstPtr += outPS;
		    }
		    srcPtr += srcInc;
		    dstPtr += dstInc;
		}
	    }
	}
    }
}
