/*
 * @(#)ImageToBuffer.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.util;

import java.awt.Image;
import java.awt.image.*;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Vector;
import java.awt.image.*;
import javax.media.PlugInManager;
import javax.media.PlugIn;
import javax.media.Codec;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.*;
import javax.media.ResourceUnavailableException;

/**
 * Utility class to convert an AWT <code>Image</code> object to a JMF
 * <code>Buffer</code> object. The output buffer will contain the image
 * data in an array of ints in the default ColorModel of the AWT toolkit.
 * The <code>getFormat</code> method on the returned buffer will contain
 * the exact format that the data is in, including the RGB mask values.
 */
public class ImageToBuffer {

    private static ImageObserver iobs = null;

    /**
     * Creates a JMF <code>Buffer</code> object for the given AWT Image
     * and frameRate. If the frameRate parameter of the output format is
     * not relevant to your needs, then any value can be specified as the
     * frameRate. If you are generating live video data that is to be
     * presented by JMF, then you should specify a reasonable value between
     * 1 and 60 for the frameRate.<P>The output buffer will have unspecified
     * values for the <code>flags, sequenceNumber, header and timeStamp</code>
     * fields. Only the <code>format, data, offset and length</code> will
     * contain valid values.
     * @param image an AWT Image that is fully prepared and has a known size.
     * @param frameRate the frameRate at which these buffer objects are being
     * generated.
     * @return a JMF Buffer object that contains the image data in RGB format,
     * or null if the image could not be converted succesfully.
     */
    public static Buffer createBuffer(Image image, float frameRate) {
	int width, height, scan;
	int [] data;
	int redMask, greenMask, blueMask;
	Dimension size;
	PixelGrabber pg;
	ColorModel cm;
	DirectColorModel dcm;
	Buffer buffer;
	
	if (image == null)
	    return null;

	// Need image observer to get the image size
	if (iobs == null) {
	    iobs = new ImageObserver() {
		public boolean imageUpdate(Image im, int info,
					   int x, int y, int w, int h) {
		    return false;
		}
	    } ;
	}

	// Get the size
	width = image.getWidth(iobs);
	height = image.getHeight(iobs);
	if (width < 1 || height < 1)
	    return null;
	scan = (width + 3) & ~3;
	size = new Dimension(width, height);
	data = new int[scan * height];

	// Get the pixels into an int array
	pg = new PixelGrabber(image, 0, 0, width, height,
			      data, 0, scan);
	try {
	    pg.grabPixels();
	} catch (InterruptedException ie) {
	    return null;
	}

	cm = pg.getColorModel();
	if (!(cm instanceof DirectColorModel)) // Cant do if not DCM
	    return null;
	dcm = (DirectColorModel) cm;
	redMask = dcm.getRedMask();
	greenMask = dcm.getGreenMask();
	blueMask = dcm.getBlueMask();
	if ( (redMask | greenMask | blueMask) != 0xFFFFFF )
	    return null;

	// Format for creating the buffer
	RGBFormat rgb = new RGBFormat(size,
				      scan * height,
				      Format.intArray,
				      frameRate,
				      32,
				      redMask, greenMask, blueMask,
				      1, scan,
				      RGBFormat.FALSE,
				      Format.NOT_SPECIFIED);

	buffer = new Buffer();
	buffer.setOffset(0);
	buffer.setLength(scan * height);
	buffer.setHeader(null);
	buffer.setFlags(0);
	buffer.setData(data);
	buffer.setFormat(rgb);
	buffer.setTimeStamp(-1);
	buffer.setSequenceNumber(0);
	return buffer;
    }
}


