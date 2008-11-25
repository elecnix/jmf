/*
 * @(#)BufferToImage.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.util;

import java.awt.Image;
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
 * This is a utility class to convert a video <code>Buffer</code> object to
 * an AWT <code>Image</code> object that you can then render using AWT methods.
 * <P>You can use this class in conjunction with the
 * <code>FrameGrabbingControl</code> to grab frames from the video renderer
 * and manipulate the image.
 * @see javax.media.control.FrameGrabbingControl
 * @since JMF 2.0
 */
public class BufferToImage {

    private VideoFormat format;

    private Codec converter = null;

    private RGBFormat prefFormat;

    private Buffer outputBuffer;

    private Dimension size;

    private boolean converterNotRequired = false;

    /**
     * Instantiates a Buffer to Image conversion object for the specified
     * format. The <code>createImage</code> method expects input buffers to have the
     * format that is specified here.
     * @param format the format of incoming <code>Buffer</code> objects.
     */
    public BufferToImage(VideoFormat format) {
	if (!(format instanceof YUVFormat ||
	      format instanceof RGBFormat)) {
	    // Cant do
	} else {
	    this.format = format;
	    size = format.getSize();
	    prefFormat = new RGBFormat(size,
				       size.width * size.height, // maxDataLength
				       Format.intArray,       // type
				       format.getFrameRate(),
				       32,  // bpp
				       Format.NOT_SPECIFIED,  // masks
				       Format.NOT_SPECIFIED,
				       Format.NOT_SPECIFIED, 
				       1, Format.NOT_SPECIFIED, // strides
				       RGBFormat.FALSE, // flipped
				       Format.NOT_SPECIFIED); // endian
	    if (format.matches(prefFormat)) {
		converterNotRequired = true;
		return;
	    }
	    Codec codec = findCodec(format, prefFormat);
	    if (codec != null)
		converter = codec;
	    outputBuffer = new Buffer();
	}
    }

    private Codec findCodec(VideoFormat input, VideoFormat output) {
	Vector codecList = PlugInManager.getPlugInList(input, output, PlugInManager.CODEC);
	if (codecList == null || codecList.size() == 0)
	    return null;
	for (int i = 0; i < codecList.size(); i++) {
	    String codecName = (String) codecList.elementAt(i);
	    Class codecClass = null;
	    Codec codec = null;
	    try {
		codecClass = Class.forName(codecName);
		if (codecClass != null)
		    codec = (Codec) codecClass.newInstance();
	    } catch (ClassNotFoundException cnfe) {
	    } catch (IllegalAccessException iae) {
	    } catch (InstantiationException ie) {
	    } catch (ClassCastException cce) {
	    }
	    if (codec == null)
		continue;
	    if (codec.setInputFormat(input) == null)
		continue;
	    Format [] outputs = codec.getSupportedOutputFormats(input);
	    if (outputs == null || outputs.length == 0)
		continue;
	    for (int j = 0; j < outputs.length; j++) {
		if (outputs[j].matches(output)) {
		    Format out = codec.setOutputFormat(outputs[j]);
		    if (out != null && out.matches(output)) {
			try {
			    codec.open();
			    return codec;
			} catch (ResourceUnavailableException rue) {
			}
		    }
		}
	    }
	}
	return null;
    }

    /**
     * Converts the input buffer to a standard AWT image and returns the image.
     * The buffer should contain video data of the format specified in the
     * constructor. If the input data is not valid or a suitable converter
     * couldn't be found, the method returns null.
     * @return an AWT Image if the conversion was completed succesfully, else
     * returns null.
     */
    public Image createImage(Buffer buffer) {
	// check for bad values
	if (  buffer == null ||
	      (converter == null && converterNotRequired == false) ||
	      prefFormat == null ||
	      buffer.getFormat() == null ||
	      !buffer.getFormat().matches(format) ||
	      buffer.getData() == null ||
	      buffer.isEOM() ||
	      buffer.isDiscard()  )
	    return null;

	int [] outputData;
	RGBFormat vf;
	
	try {
	    if (converterNotRequired) {
		outputData = (int[]) buffer.getData();
		vf = (RGBFormat) buffer.getFormat();
		outputBuffer = buffer;
	    } else {
		int retVal = converter.process(buffer, outputBuffer);
		if (retVal != PlugIn.BUFFER_PROCESSED_OK)
		    return null;
		outputData = (int[]) outputBuffer.getData();
		vf = (RGBFormat) outputBuffer.getFormat();
	    }
	} catch (Exception ex) {
	    System.err.println("Exception " + ex);
	    return null;
	}

	Image outputImage = null;
	BufferToImage waFor2DBug = null;

	try {
	    Class cl = Class.forName("com.sun.media.util.BufferToBufferedImage");
	    waFor2DBug = (BufferToImage) cl.newInstance();
	} catch (Exception e) {
	    //System.err.println("Not JDK 1.2");
	}

	if (waFor2DBug != null) {
	    outputImage = waFor2DBug.createImage(outputBuffer);
	} else {
	    int redMask = vf.getRedMask();
	    int greenMask = vf.getGreenMask();
	    int blueMask = vf.getBlueMask();
	    
	    
	    DirectColorModel dcm = new DirectColorModel(32,
							redMask,
							greenMask,
							blueMask);
	    
	    MemoryImageSource sourceImage = new MemoryImageSource(size.width,
								  size.height,
								  dcm,
								  outputData,
								  0,
								  size.width);
	    outputImage = Toolkit.getDefaultToolkit().createImage(sourceImage);
	}
	
	return outputImage;
    }
}


