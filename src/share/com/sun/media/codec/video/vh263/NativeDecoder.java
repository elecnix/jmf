/*
 * @(#)NativeDecoder.java	1.17 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.video.vh263;
import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.ibm.media.codec.video.*;
import java.awt.Dimension;
import javax.media.rtp.*;


public class NativeDecoder extends VideoCodec {


    ////////////////////////////////////////////////////////////////////////////
    // Native methods


    private static native boolean initNativeDecoderClass();

    private native boolean initNativeDecoder(int width,int height);

    private native boolean decodeFrameNative(Buffer in, Buffer out);

    private native boolean decodePacketNative(byte[] in,int inputOffset,int inputLength, byte[] out,byte[] payloadHeader,int payloadOffset,int sync, int h263_1998);

    private native boolean closeNativeDecoder();

    ////////////////////////////////////////////////////////////////////////////
    // Variables


    //// start: the following variables are accessed by the native code

    /** pointer to the native structure of H263 decoder **/
    private int nativeData;
    /** pointer to the native structure of the picture descriptor **/
    private int pictureDesc;

    private int PBFrameCap = 1;

    private int bsStart;

    private int nextGOB;

    private int pendingFrame = 0;

    private static int  MAX_SEQ = 65535;

    private long prevSeq = -1;

    /*
     * Generate debug messages
     */
    private static final boolean debug = false;

    //// end: the following variables are accessed by the native code

    static final int [] widths = {0, 128, 176, 352, 704, 1408,0,0};
    static final int [] heights = {0, 96, 144, 288, 576, 1152,0,0};
    private int videoWidth=176;   // defualt size
    private int videoHeight=144;
    //private int videoWidth=352;   // defualt size
    //private int videoHeight=288;

    private boolean FormatSizeInitFlag=false;
    private int payloadLength=4;



    public NativeDecoder() {
	supportedInputFormats = new VideoFormat[] {
				new VideoFormat(VideoFormat.H263),
				new VideoFormat(VideoFormat.H263_RTP),
				new VideoFormat(VideoFormat.H263_1998_RTP)
	};
        defaultOutputFormats  = new VideoFormat[] {new YUVFormat() };
        PLUGIN_NAME = "H.263 Decoder";

    }

    protected  Format[] getMatchingOutputFormats(Format in) {
      	VideoFormat     ivf  = (VideoFormat) in;
	Dimension       inSize = ivf.getSize();
        int             inMaxDataLength=ivf.getMaxDataLength();
        int             outNumOfPixels;


        if ( ( (ivf.getEncoding()).equals(VideoFormat.H263_RTP) )
        	|| ( (ivf.getEncoding()).equals(VideoFormat.H263_1998_RTP) ) ) {

	    if (inSize == null)
		outNumOfPixels = videoWidth * videoHeight;
	    else {
		outNumOfPixels = inSize.width * inSize.height;
		videoWidth = inSize.width;
		videoHeight = inSize.height;
	    }
      
          outNumOfPixels=videoWidth*videoHeight;
          supportedOutputFormats= new  VideoFormat[] {

                new YUVFormat (
                new Dimension(videoWidth,videoHeight),
                (outNumOfPixels)+ ((outNumOfPixels) >> 1),
                Format.byteArray,
                ivf.getFrameRate(),			       
                YUVFormat.YUV_420 ,
                videoWidth,
                videoWidth>>1,
                0,
                outNumOfPixels,
                outNumOfPixels+(outNumOfPixels >>2)
                )};

        }
        else {

         Dimension  outSize=movieSizeTo263Size(inSize);
         outNumOfPixels=outSize.width*outSize.height;
         videoWidth=outSize.width;
         videoHeight=outSize.height;
         supportedOutputFormats= new  VideoFormat[] {
                new YUVFormat (
                inSize,
                (outNumOfPixels)+ ((outNumOfPixels) >> 1),
                Format.byteArray,
                ivf.getFrameRate(),			       
                YUVFormat.YUV_420 ,
                outSize.width,
                outSize.width>>1,
                0,
                outNumOfPixels,
                outNumOfPixels+(outNumOfPixels >>2)
                ) };

        }

        return  supportedOutputFormats;
    }

    public Format setInputFormat(Format input) {
	Format ret = super.setInputFormat(input);
	if (ret == null)
	    return null;
	if (opened) {
	    close();
	    try {
		setOutputFormat(getMatchingOutputFormats(input)[0]);
		open();
	    } catch (Exception e) {
		return null;
	    }
	}
	return ret;
    }

    public void open() throws ResourceUnavailableException {
        try {
	    JMFSecurityManager.loadLibrary("jmutil");
            JMFSecurityManager.loadLibrary("jmvh263");
            initNativeDecoderClass();
            initDecoder();
	    super.open();
            return;
        } catch (Throwable e) {
	    //System.out.println(e);
        }

        throw new ResourceUnavailableException("could not load jmvh263");
    }

    public void close() {
        closeNativeDecoder();
	super.close();
    }

    public void reset() {
      //we do not initialize the decoder as the first frame is key frame
    }

    // called when video resize is detected, by checkFormat()
    protected void videoResized() {
        initDecoder();
    }

    protected void initDecoder() {
        closeNativeDecoder();		     // close the decoder if it was openned
        initNativeDecoder(videoWidth,videoHeight);
    }



    public int process(Buffer inputBuffer, Buffer outputBuffer) {


      boolean rtpData = false;
      boolean rtp1998Data = false;
      boolean ret=false;
      RTPHeader rtpHeader=null;
      int newWidth=videoWidth;
      int newHeight=videoHeight;

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
      int inputOffset=inputBuffer.getOffset();

      byte[] inData =(byte[]) inputBuffer.getData();

      if ( (ivf.getEncoding()).equals(VideoFormat.H263_RTP) ) {
        rtpData = true;
        payloadLength=getPayloadHeaderLength(inData,inputOffset);
        if ( (inData[inputOffset+payloadLength] == 0) && (inData[inputOffset+payloadLength+1] == 0) && ((inData[inputOffset+payloadLength+2] & 0xfc) == 0x80)) {
              int s = (inData[inputOffset+payloadLength+4] >> 2) & 0x7;

	      newWidth=widths[s];
	      newHeight=heights[s];
	      FormatSizeInitFlag=true;

	}
      }
      else if ( (ivf.getEncoding()).equals(VideoFormat.H263_1998_RTP) ) {
        rtp1998Data = true;
	int s = -1;
	int picOffset = -1;
	if (getSequenceDiff(prevSeq, inputBuffer.getSequenceNumber()) != 1) {
	    boolean discard = ((inData[inputOffset] & 0x04) == 0);
	    // Not part of the continuation, report lost packet
	    if (debug) {
		System.err.println(
			"NativeDecoder: Sequence out of order, expecting "
					+ (prevSeq+1) + " got "
					+ inputBuffer.getSequenceNumber()
					+ (discard ? " discard packet" : ""));
	    }
	    if (discard)
		return OUTPUT_BUFFER_NOT_FILLED;
	}
	prevSeq = inputBuffer.getSequenceNumber();
        payloadLength=get1998PayloadHeaderLength(inData,inputOffset);
	if (payloadLength > 5) {
	    // Use PIC header in payload header
	    if ( ((inData[inputOffset] & 0x02) == 0x02)
		&& ((inData[inputOffset+3] & 0xfc) == 0x80)) {
		picOffset = inputOffset + 3;
	    } else if ((inData[inputOffset+2] & 0xfc) == 0x80) {
		picOffset = inputOffset + 2;
	    }
	} else if ( ((inData[inputOffset] & 0x04) == 0x04)
		&& ((inData[inputOffset+payloadLength] & 0xfc) == 0x80)) {
	    picOffset = inputOffset + payloadLength;
	}

	if (picOffset >= 0) {
	    s = (inData[picOffset+2] >> 2) & 0x7;
	    if (s == 7) {
		// Extended PTYPE, picture size is in the extension
		// if UFEP = 001
		if (((inData[picOffset+3] >> 1) & 0x07) == 1) {
		    s = ((inData[picOffset+3] << 2) & 0x04) |
				((inData[picOffset+4] >> 6) & 0x03);
		} else {
		    s = -1;	// picture type not present
		}
	    }
	}

	if (s >= 0) {

              newWidth=widths[s];
	      newHeight=heights[s];
	      FormatSizeInitFlag=true;

	}
      }
      if ( (videoWidth!=newWidth) || (videoHeight!=newHeight)  ) {

        videoWidth=newWidth;
        videoHeight=newHeight;


        int outNumOfPixels=videoWidth*videoHeight;

        outputFormat = new YUVFormat (
		new Dimension(videoWidth,videoHeight),
		(outNumOfPixels)+ ((outNumOfPixels) >> 1),
		Format.byteArray,
		ivf.getFrameRate(),			       
		YUVFormat.YUV_420 ,
		videoWidth,
		videoWidth>>1,
		0,
		outNumOfPixels,
		outNumOfPixels+(outNumOfPixels >>2)
        );


        outMaxLength = outputFormat.getMaxDataLength();
        // outMaxLength = videoWidth * videoHeight;
        videoResized();

	FormatSizeInitFlag=true;

      }

      if ( (rtpData || rtp1998Data) && !FormatSizeInitFlag) {
        return BUFFER_PROCESSED_FAILED;
      }

      byte[] outData = validateByteArraySize(outputBuffer,outMaxLength );

      /*
       *  <PATCH> check for insufficient input:
       *  The decoder might read up to 8 additional bytes before checking for EOS
       *  It does not bother the native code, but can cause ArrayOutOfBounds
       *  in Java code
       */
      if ( (inLength+8+inputOffset)>inData.length) {
         //System.out.println("allocating more data for H.263");
         int newLength=(inLength > inMaxLength) ? inLength : inMaxLength;

         byte[] tempArray=new byte[inputOffset+newLength+8];
         System.arraycopy(inData,0,tempArray,0,inLength+inputOffset);
         inData=tempArray;
         inputBuffer.setData(tempArray);
         //inputBuffer.setOffset(0);

      }

      /*
       *  <PATCH> pad input with EOS
       */
      inData[inputOffset+inLength] = 0;
      inData[inputOffset+inLength+1] = 0;
      inData[inputOffset+inLength+2]= (byte) 0xfc;
      inLength += 3;
      inputBuffer.setLength(inLength);

      if (rtpData) {
        inLength-=payloadLength;  // this is the length of the bitstream
        //rtpHeader= (RTPHeader) inputBuffer.getHeader();
	int marker = 0;
	if ((inputBuffer.getFlags() & Buffer.FLAG_RTP_MARKER) != 0)
	  marker = 1;
        ret = decodePacketNative(inData,inputOffset+payloadLength,inLength,outData,inData,inputOffset,marker,0);
      } else
      if (rtp1998Data) {
        inLength-=payloadLength;  // this is the length of the bitstream
        //rtpHeader= (RTPHeader) inputBuffer.getHeader();
	int marker = 0;
	if ((inputBuffer.getFlags() & Buffer.FLAG_RTP_MARKER) != 0) {
	  marker = 1;
	} else {
	    inLength -= 3;	// skip the EOS pad
	}
        ret = decodePacketNative(inData,inputOffset+payloadLength,inLength,outData,inData,inputOffset,marker,1);
      }
      else {
        ret = decodeFrameNative(inputBuffer, outputBuffer);
      }

      if (ret) {
         updateOutput(outputBuffer,outputFormat, outMaxLength, 0);
	 outputBuffer.setFormat(outputFormat);
         return BUFFER_PROCESSED_OK;
       }
       else {
         return OUTPUT_BUFFER_NOT_FILLED;
       }


    }




    private Dimension movieSizeTo263Size(Dimension movieSize) {
       int width  = (movieSize.width  + 0xf) & 0xfffffff0;
       int height = (movieSize.height + 0xf) & 0xfffffff0;
       int formatCap = (width * height) >> 8;

       /* transform the number of macroblocks into format*/
       if (formatCap <= 48)
           return new Dimension(128,96);    // SQCIF
       if (formatCap <= 99)
           return new Dimension(176,144);   // QCIF
       if (formatCap <= 396)
           return new Dimension(352,288);   // CIF
       if (formatCap <= 1584)
           return new Dimension(704,576);   // 4CIF
       if (formatCap <= 6336)
           return new Dimension(1408,1152); // 16CIF

       return new Dimension(0,0);    // ERROR


    }

    private int getPayloadHeaderLength(byte[] input,int offset) {

       int l = 0;
       byte b = input[offset];

	if ( (b & 0x80) != 0) { //mode B or C
	    if ((b & 0x40) != 0) //mode C
		l = 12;
	    else //mode B
		l = 8;
	} else { //mode A
	    l = 4;
	}

	return l;
    }

    static public int get1998PayloadHeaderLength(byte[] input,int offset) {

       int l = 2 + ((input[offset]&0x01) << 5) | ((input[offset+1]&0xf8) >> 3);

	if ( (input[offset] & 0x02) != 0) { // Video Redundancy present
	    l++;
	}

	return l;
    }

    public boolean checkFormat(Format format) {

      if ( (format.getEncoding()).equals(VideoFormat.H263_RTP) ) {
        return true;
      }
      else if ( (format.getEncoding()).equals(VideoFormat.H263_1998_RTP) ) {
        return true;
      }
      else {
        return super.checkFormat(format);
      }

    }

    /*
     * the RTP sequence number is unsigned 16 bit counter that
     * wraps around. Allow for the case where it has wrapped.
     * @param p sequence number of the suspected previous packet
     * @param c sequence number of the current (or next) packet
     * @return int difference in sequence numbers
     */
    private int  getSequenceDiff(long p, long c) {
	if (c > p)
	    return (int) (c - p);
	if (c == p)
	    return 0;
	if (p > MAX_SEQ - 100 && c < 100) {
	    // Allow for the case where sequence number has wrapped.
	    return (int) ((MAX_SEQ - p) + c + 1);
	}
	return (int) (c - p);
    }

}

