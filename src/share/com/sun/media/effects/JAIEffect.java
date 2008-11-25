/*
 * @(#)JAIEffect.java	1.3E
 *
 * Copyright 1998-1999 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.media.effects;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.PlanarImage;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;
import javax.media.util.ImageToBuffer;
import javax.media.*;
import javax.media.format.RGBFormat;
import com.sun.media.JMFSecurityManager;

public class JAIEffect extends com.sun.media.BasicCodec implements Effect {

    static {
	// 1) Need JDK1.2 or higher. 2) JAI should have been installed
	
	if (! JMFSecurityManager.isJDK12() ) {
	    throw new UnsatisfiedLinkError("Fatal Error: JAI Effects need JDK1.2 or higher VM");
	}
	try {
	    Class.forName("javax.media.jai.PlanarImage");
	} catch (ClassNotFoundException e) {
	    throw new UnsatisfiedLinkError("Fatal Error: JAI classes are not present");
	}
    }

    private BufferToImage frameConverter = null;
    private PlanarImage jaiImage;

    private boolean debug = false;

    private Control[] controls;
    private JAIControl control;

    private static float[] edge1Matrix = 
             new float[] { -1.0F,       -1.0F,        -1.0F,
                          -1.0F,        8.0F,        -1.0F,
                          -1.0F,        -1.0F,        -1.0F};


    private static float[] edge2Matrix = 
             new float[] {  0.0F,       -1.0F,         0.0F,
                          -1.0F,        4.0F,        -1.0F,
                           0.0F,        -1.0F,         0.0F};

    private static float[] sharp1Matrix = 
             new float[] { 0.0F,       -1.0F,        0.0F,
                          -1.0F,        5.0F,        -1.0F,
                          0.0F,        -1.0F,        0.0F};

    private static float[] sharp2Matrix = 
             new float[] { -1.0F,       -1.0F,        -1.0F,
                          -1.0F,        9.0F,        -1.0F,
                          -1.0F,        -1.0F,        -1.0F};


    private static float[] embossMatrix = 
             new float[] {-1.0F,       -2.0F,         0.0F,
                          -2.0F,        0.0F,         2.0F,
                           0.0F,        2.0F,         1.0F};

    private String kernelNames[] = {
			     "Edge1", "Edge2",
			     "Sharp1", "Sharp2",
			     "Emboss",
 			     "None" // Last one is always None
                             };
    private float[][] matrices = new float[kernelNames.length -1][];

    private KernelJAI[] kernels = new KernelJAI[kernelNames.length -1];

    public JAIEffect() {
	
	matrices[0] = edge1Matrix;
	matrices[1] = edge2Matrix;
	matrices[2] = sharp1Matrix;
	matrices[3] = sharp2Matrix;
	matrices[4] = embossMatrix;

	inputFormats = new Format[] {
	    new RGBFormat(null,
			  Format.NOT_SPECIFIED,
			  Format.byteArray,
			  Format.NOT_SPECIFIED,
			  24,
			  3, 2, 1,
			  3, Format.NOT_SPECIFIED,
			  Format.TRUE,
			  Format.NOT_SPECIFIED)
	};

	outputFormats = new Format[] {
	    new RGBFormat(null,
			  Format.NOT_SPECIFIED,
			  Format.byteArray,
			  Format.NOT_SPECIFIED,
			  24,
			  3, 2, 1,
			  3, Format.NOT_SPECIFIED,
			  Format.TRUE,
			  Format.NOT_SPECIFIED)
	};

    }

    public String getName() {
	return "Java Advanced Imaging Effects";
    }


    public Format [] getSupportedOutputFormats(Format input) {
	if (input == null) {
	    return outputFormats;
	}
	
	if (matches(input, inputFormats) != null) {
	    return new Format[] { outputFormats[0].intersects(input) };
	} else {
	    return new Format[0];
	}
    }


    public Format setOutputFormat(Format output) {
	if (output == null || matches(output, outputFormats) == null)
	    return null;
	RGBFormat incoming = (RGBFormat) output;
	
	Dimension size = incoming.getSize();
	int maxDataLength = incoming.getMaxDataLength();
	int lineStride = incoming.getLineStride();
	float frameRate = incoming.getFrameRate();
	int flipped = incoming.getFlipped();
	int endian = incoming.getEndian();

	if (size == null)
	    return null;
	if (maxDataLength < size.width * size.height * 3)
	    maxDataLength = size.width * size.height * 3;
	if (lineStride < size.width * 3)
	    lineStride = size.width * 3;
	if (flipped != Format.FALSE)
	    flipped = Format.FALSE;
	
	outputFormat = outputFormats[0].intersects(new RGBFormat(size,
						        maxDataLength,
							null,
							frameRate,
							Format.NOT_SPECIFIED,
							Format.NOT_SPECIFIED,
							Format.NOT_SPECIFIED,
							Format.NOT_SPECIFIED,
							Format.NOT_SPECIFIED,
							lineStride,
							Format.NOT_SPECIFIED,
							Format.NOT_SPECIFIED));

	return outputFormat;
    }

    public int process(Buffer inBuffer, Buffer outBuffer) {
	try {
	if (frameConverter == null) {
	    frameConverter = new BufferToImage((VideoFormat) inBuffer.getFormat());
	}

        // Convert the Buffer to an AWT Image.
        Image frameImage = frameConverter.createImage(inBuffer);

        // Derive a JAI image from the AWT image.
	PlanarImage jaiImage = JAI.create("AWTImage", frameImage);

	int index;
	boolean emboss = false;
	if (control != null) {
	    index = control.getEffectIndex();
	    if (control.getEffectName().equals("None")) {
		outBuffer.setData(inBuffer.getData());
		outBuffer.setFormat(inBuffer.getFormat());
		outBuffer.setFlags(inBuffer.getFlags());
		outBuffer.setLength(inBuffer.getLength());
		return BUFFER_PROCESSED_OK;
	    }
	    if (control.getEffectName().equals("Emboss")) {
		emboss = true; // Special case
	    }
	} else
	    index = 0;

	if (kernels[index] == null) {
            kernels[index] = new KernelJAI(3, 3, matrices[index]);
	}

	jaiImage = JAI.create("convolve", jaiImage, kernels[index]);

	if (emboss) { // add 128 to make it brighter
            double[] constants = new double[] {128., 128., 128.};
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(jaiImage);
            pb.add(constants);
            jaiImage = JAI.create("addconst", pb, null);
        }

	// Now convert the image to a buffer
 	BufferedImage bim = jaiImage.getAsBufferedImage();

	Buffer out = ImageToBuffer.createBuffer(bim, 15.F);
	if (out == null) {
	    if (debug) {
		System.out.println("ImageToBuffer returned null");
	    }
	    return BUFFER_PROCESSED_FAILED;
	}

	outBuffer.setData(out.getData());
	outBuffer.setFormat(out.getFormat());
	outBuffer.setFlags(out.getFlags());
	outBuffer.setLength(out.getLength());
	} catch (Exception e) {
	    System.err.println(e);
	    return BUFFER_PROCESSED_FAILED;
	} catch (Error e) {
	    System.err.println(e);
	    return BUFFER_PROCESSED_FAILED;
	}
	return BUFFER_PROCESSED_OK;
    }


    public Object[] getControls() {
	if (controls == null) {
	    controls = new Control[1];
	    controls[0] = getControl();
	}
	return controls;
    }

    public Object getControl(String controlType) {
	// TODO: check if this right
	if (controlType.equals("com.sun.media.effects.JAIControl")) {
	    return getControl();
	} else
	    return null;
    }

    private Control getControl() {
	if (control == null) {
	    control = new JAIControl();
	    control.setConvolutionChoices(kernelNames);
	}
	int index = control.getEffectIndex();
	return control;
    }

class JAIControl implements Control {
    private Panel panel;
    private Choice c;

    JAIControl() {
	panel = new Panel();
    }

    void setConvolutionChoices(String[] s) {
	c = new Choice();
	for (int i = 0; i < s.length; i++) {
	    c.add(s[i]);
	}
	panel.add(c);
    }

    int getEffectIndex() {
	return c.getSelectedIndex();
    }

    String getEffectName() {
	return c.getSelectedItem();
    }

    public Component getControlComponent() {
	return panel;
    }

}

}
