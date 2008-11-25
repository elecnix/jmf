/*
 * @(#)NativeEncoder.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.video.cinepakpro;

import javax.media.*;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.format.RGBFormat;
import javax.media.control.*;

import java.awt.Dimension;
import java.awt.Component;

import com.sun.media.*;
import com.sun.media.util.*;

public class NativeEncoder extends BasicCodec
{
    // I/O formats
	private RGBFormat inputFormat = null;
	private VideoFormat outputFormat = null;

    // Have we loaded the native library?
    private static boolean loaded = false;

    // Assume we can load it.
    private static boolean canLoad = true;

    // Pointer to native structure
    private int peer = 0;

    private boolean newframe = true;

    private int current_offset = 0;
    
    int returnVal = 0;
	
	// housekeepers from PrivateGlobals
	private	boolean		bCcontextAllocated = false;

	
     //****************************************************************
     //* Codec Methods
     //****************************************************************/

    // Initialize default formats.
    public 
NativeEncoder() 
{
	inputFormats = new RGBFormat[1];
	inputFormats[0] = new RGBFormat(
				null,			// any size
				Format.NOT_SPECIFIED,	// maxDataLen
				Format.intArray,
				Format.NOT_SPECIFIED,	// any frame rate.
				32,
				0x00FF0000,
				0x0000FF00,
				0x000000FF);

	outputFormats = new VideoFormat[1];
	outputFormats[0] = new VideoFormat(VideoFormat.CINEPAK);
}

    protected Format getInputFormat() 
{
		
	return inputFormat;
}

    protected Format getOutputFormat() 
	{
		return outputFormat;
   }

    // Return supported output formats
    public Format [] getSupportedOutputFormats(Format in) 
	{
		if (in == null)
		    return outputFormats;
	
		// Make sure the input is RGB video format
		if (!verifyInputFormat(in))
		    return new Format[0];
	
		Format out [] = new Format[ 1 ];
		out[0] = computeOutputFormat(in);
			
		return out;
    }

    private 
boolean 
verifyInputFormat( Format input ) 
{
	if (!(input instanceof RGBFormat))
	{
	    return false;
	}
			
	RGBFormat rgb = (RGBFormat) input;
	if ( //rgb.getDataType() != Format.intArray ||
	     rgb.getBitsPerPixel() != 32 ||
	     rgb.getRedMask() != 0x00FF0000 ||
	     rgb.getGreenMask() != 0x0000FF00 ||
	     rgb.getBlueMask() != 0x000000FF ||
	     rgb.getSize() == null ||
	     rgb.getLineStride() < rgb.getSize().width ||
	     rgb.getPixelStride() != 1 )
	{
	    return false;
	}

	return true;
}

    public 
Format 
setInputFormat( Format input ) 
{
	if (!verifyInputFormat(input))
	    return null;
		
	inputFormat = (RGBFormat) input;
	
	return inputFormat;
}

    public 
Format 
setOutputFormat( Format output ) 
{
	if (matches(output, outputFormats) == null)
	{
	    return null;
	}
	outputFormat = (VideoFormat) output;
	
	return outputFormat;
}

    private 
final 
VideoFormat 
computeOutputFormat( Format in ) 
{
	// Calculate the properties
	RGBFormat rgb = (RGBFormat) in;
	Dimension size = rgb.getSize();
	int maxDataLength = size.width * size.height * 3;
	VideoFormat cinepak = new VideoFormat(
								VideoFormat.CINEPAK,
					   			size, 
								maxDataLength,
					   			Format.byteArray,
								rgb.getFrameRate() );
		
	return cinepak;
}

    public void open() throws ResourceUnavailableException 
	{
		if (!canLoad)
	    	throw new ResourceUnavailableException("Unable to load" +
						   " native CinepakPro converter");

		if (!loaded) 
		{
	    	try 
			{
				//JMFSecurityManager.loadLibrary( "CvidPro" );
				JMFSecurityManager.loadLibrary("jmutil");
				JMFSecurityManager.loadLibrary("jmfCVIDPro");
				loaded = true;
	    	} 
			catch (Throwable t) 
			{
				canLoad = false;
				throw new ResourceUnavailableException("Unable to load " +
								       "native cinepak encoder");
	    	}
		}

		if (inputFormat == null || outputFormat == null)
		    throw new ResourceUnavailableException("Formats not set " +
							   "on the cinepak encoder");
	
		if (peer != 0)
		    close();

		Dimension size = inputFormat.getSize();
				
		try 
		{
	    	peer = initCinepakEncoder(
							// source image info
						size.width, 
						size.height, 
						inputFormat.getLineStride(),
						inputFormat.getBitsPerPixel(),		// should be 24
						9,									// BGR888 packed 24 bpp 
															// TODO:  make smarter based on pixel stride, etc.
							// data rate params
						300000,								// 300 KB/s
						1000.0/inputFormat.getFrameRate(),	// 15 fps
						30,									// keyframes every 2s
						
							//job flags - don't recommend turning any other than B&W on
						false,								// fraction intercodebooks
						false,								// logging
						false,								// black and white
						
							// user knobs
						2.1,								// K/I ratio
						false,								// adapt K/I
						3500,								// nat key insertion threshold
						13									// ratio of smooths to skips is 13/8 or about 8:5
							);	
		} 
		catch (Throwable t) 
		{
		}
	
		if (peer == 0)
		{
		    throw new ResourceUnavailableException("Unable to initialize cinepak encoder");
		}
    }

    public synchronized void close() {
	if (peer != 0)
	    freeCinepakEncoder(peer);
	peer = 0;
    }

    public void reset() {
	// Anything to do?
    }

    public synchronized int process(Buffer inBuffer, Buffer outBuffer) 
	{
		Object header = null;
		
		if (isEOM(inBuffer)) 
		{
		    propagateEOM(outBuffer);
		    return BUFFER_PROCESSED_OK;
		}
			
		Format inFormat = inBuffer.getFormat();
		Format outFormat = outBuffer.getFormat();
		int [] inData = (int[]) inBuffer.getData();
		boolean flipped = ((RGBFormat) inFormat).getFlipped() == Format.TRUE;
	
		if (outputFormat.getEncoding().equals(VideoFormat.CINEPAK))
		{
		    byte [] outData = (byte[]) outBuffer.getData();
		    if (	outData == null 
				||	(outData.length < outputFormat.getMaxDataLength()) 
				)
			{
//				int	iMaxDataSize = 50000;		// make it 500k to be safe
			
				outData = new byte[outputFormat.getMaxDataLength()];
//				outData = new byte[ iMaxDataSize ];
				
				outBuffer.setData(outData);
		    }
		    
		    if (outFormat == null)
			{
				outBuffer.setFormat(outputFormat);
			}
		    
		    if (peer == 0) 
			{
				try
				{
			    	open();
				} 
				catch (ResourceUnavailableException re) 
				{
			    	return BUFFER_PROCESSED_FAILED;
				}
		    }
		    
		    Dimension size = inputFormat.getSize();
		    returnVal = encodeCinepakFrame(
							peer,
				   			inData,
							0,
						   	outData );
				   
		    if (returnVal > 0)
			{
				outBuffer.setLength(returnVal);
				outBuffer.setOffset(0);
				inBuffer.setLength(0);
				outBuffer.setTimeStamp(inBuffer.getTimeStamp());

				if ( wasKeyFrame( peer ) ) {
				    outBuffer.setFlags( Buffer.FLAG_KEY_FRAME );
				} else {
				    outBuffer.setFlags( outBuffer.getFlags() &
							~Buffer.FLAG_KEY_FRAME);
				}

				return BUFFER_PROCESSED_OK;
			}
			
		    outBuffer.setDiscard(true);
		    return BUFFER_PROCESSED_FAILED;
		}// if outputformat is CINEPAK
	
		return BUFFER_PROCESSED_FAILED;	
	}// end of process()

    public void finalize() {
	close();
    }

    public String getName() 
	{
		return "CinepakPro Encoder by CTi";
    }

    //****************************************************************
    // * Native Methods
    //****************************************************************/

    // Initializes the native encoder
	//
	//	returns 
	//		a positive encoder instance handle which is needed for every subsequent call
	//			or
	//		a negative error code
	//		
	//		0 == some kinda really gross error...
	//
    private native int initCinepakEncoder(
								// source image info
							int 	width_pixels, 		// width of source image in pixels
							int 	height_pixels, 		// height of source image in pixels
							int		rowPitch_bytes,		// pitch of rows in bytes
							int		colorDepth_bpp,		// color depth in bits per pixel
							int		dibType,			// code for color organization
							
								// parameters
									// data rate controls
							int		dataRate_kBps,		// average data rate in kilobytes/s
							double	frameDuration_ms,	// frame duration in milliseconds.
														// 	if changing frame rates, re-init
							int		forceKeyFrameEvery_f,
														// forces key frames to be inserted every x frames
							
									// job flags
							boolean	doFractionInterCodeBook,	// true to turn on fractional refining of inter codebooks
							boolean	doLogRequested,				// true to enable logging
							boolean	doBlackAndWhite,			// true to enable B&W only encoding
									
									// user controls
							double	initKIRatio,		// starting ratio of sizes of Key to Interframes
														//	permanent ratio if doUseAdaptiveKIRatio is false
							boolean	doUseAdaptiveKIRatio, //or starting point if true
							int		NatKeyThreshold,	// amount of change required to force a natural keyframe
							int		SpatialTemporalHint	// hint value to influence better spatial resolution or
														// 	faster codebook changes
								);
		
    
    //*
    // * Encodes the RGB data and returns the output length (positive)
     //* Returns zero if it couldn't encode, or a negative value to indicate
     //* the error.
     //
	 //		note that key/interframe information is lost!
	 //
	 //		so call wasKeyFrame() immed after to find out if it was a keyframe
	 //
    private native int encodeCinepakFrame(
								int		instance,			// returned from initCinepakEncoder() 
								int [] 	inData, 			// source pixels in format described to init()
								int 	dataStart,			// offset in inData where the image really starts
				  				byte [] outData				// destination of output data -- compressed frame
									);
	private native boolean wasKeyFrame( int instance );


    // Frees any native structures
	//	
	//	returns error code
	//
	//		0 == success, noErr
	//
    private native int freeCinepakEncoder(int instance);

    private Control [] controls = null;
    private DC dc = null;

    public Object [] getControls() 
	{
		if (dc == null) 
		{
	    	dc = new DC();
		    controls = new Control[1];
		    controls[0] = dc;
		}

		return controls;
    }
    
    /****************************************************************
     * Inner Classes
     ****************************************************************/
    
    class DC implements FrameProcessingControl, QualityControl  {

		public
		int
		getFramesDropped( )
		{
			return 0;
		}

	public Component getControlComponent() 
	{
	    return null;
	}
	
	public boolean setMinimalProcessing(boolean on) {
	    //dropNextFrame(true);
	    return false;
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
	   /*
	    if (framesBehind > 0)
		dropNextFrame(true);
	    else
		dropNextFrame(false);
	   */
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
