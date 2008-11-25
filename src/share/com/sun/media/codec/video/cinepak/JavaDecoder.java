/*
 * @(#)JavaDecoder.java	1.10 02/08/21
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


public class JavaDecoder extends VideoCodec {


    ////////////////////////////////////////////////////////////////////////////
    // Constants

    // RGB bit masks
    static final private int rMask = 0x000000ff;
    static final private int gMask = 0x0000ff00;
    static final private int bMask = 0x00ff0000;

    ////////////////////////////////////////////////////////////////////////////
    // Variables

    private int [] refData = null;
    
    /** the cinepak java decoder **/
    private CineStore fOurStore;


    public JavaDecoder() {
	supportedInputFormats = new VideoFormat[] {new VideoFormat(VideoFormat.CINEPAK) };
        defaultOutputFormats  = new VideoFormat[] {new RGBFormat(
                null, Format.NOT_SPECIFIED,
		Format.intArray,
		Format.NOT_SPECIFIED, // frame rate
		32,
		rMask, gMask, bMask,
		1,Format.NOT_SPECIFIED,
		Format.FALSE, // flipped
		Format.NOT_SPECIFIED // endian
                ) };
        PLUGIN_NAME = "Cinepak Decoder";

    }

    protected  Format[] getMatchingOutputFormats(Format in) {
      	VideoFormat     ivf  = (VideoFormat) in;
	Dimension       inSize = ivf.getSize();
	int lineStride = (inSize.width + 3) & ~3;
	int rowStride = (inSize.height + 3) & ~3;
	
        supportedOutputFormats= new  VideoFormat[] {
	    new RGBFormat (new Dimension(inSize),
			   lineStride * rowStride, Format.intArray,
			   ivf.getFrameRate(),			       
			   32,
			   rMask, gMask, bMask
			   )
	};

        return  supportedOutputFormats;
    }


    public void open() throws ResourceUnavailableException {
        initDecoder();
    }

    public void close() {
        fOurStore=null;
    }

    public void reset() {
        // no need to init decoder as first frame is always a key frame
    }

    // called when video resize is detected, by checkFormat()
    protected void videoResized() {
        initDecoder();
    }

    protected void initDecoder() {
        fOurStore = new CineStore();
    }


   public int process(Buffer inputBuffer, Buffer outputBuffer) {

      if (!checkInputBuffer(inputBuffer) ) {
         return BUFFER_PROCESSED_FAILED;
      }

      if (isEOM(inputBuffer) ) {
         propagateEOM(outputBuffer);
         return BUFFER_PROCESSED_OK;
      }

      VideoFormat ivf=(VideoFormat) inputBuffer.getFormat();
      int inLength=inputBuffer.getLength();
      int inMaxLength=ivf.getMaxDataLength();
      int outMaxLength=outputFormat.getMaxDataLength();

      byte[] inData =(byte[]) inputBuffer.getData();
      int[] outData = validateIntArraySize(outputBuffer,outMaxLength );

      if (refData == null) {
	  refData = outData;
	  outputBuffer.setData(null);
	  outData = validateIntArraySize(outputBuffer, outMaxLength);
      }

      outputBuffer.setData(refData); // temporarily
      
      // the java decoder relies on output Buffer format
      outputBuffer.setFormat(outputFormat);

      fOurStore.DoFrame(inputBuffer, outputBuffer, fOurStore);

      System.arraycopy(refData, 0,
		       outData, 0,
		       outMaxLength); // make a copy

      outputBuffer.setData(outData);  // put it back in the buffer
      
      updateOutput(outputBuffer,outputFormat, outMaxLength, 0);

      return BUFFER_PROCESSED_OK;
    }



}

