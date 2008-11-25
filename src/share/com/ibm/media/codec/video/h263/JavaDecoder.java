package com.ibm.media.codec.video.h263;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.ibm.media.codec.video.*;
import java.awt.Dimension;
import javax.media.rtp.*;


public class JavaDecoder extends VideoCodec {


    ////////////////////////////////////////////////////////////////////////////
    // Constants
    // CIF/QCIF sizes
//    static final int CIFW = 352;
//   static final int CIFH = 288;
//    static final int QCIFW = 176;
//    static final int QCIFH = 144;
//    static final int [] widths = {0, 128, 176, 352, 704, 1408,0,0};
//    static final int [] heights = {0, 96, 144, 288, 576, 1152,0,0};

    // RGB bit masks
    static final private int rMask = 0x000000ff;
    static final private int gMask = 0x0000ff00;
    static final private int bMask = 0x00ff0000;
    static final private boolean DEBUG = false;
    ////////////////////////////////////////////////////////////////////////////
    // Variables


    private H263Decoder javaDecoder;
    private FrameBuffer outputFrame;

    static public final int [] widths = {0, 128, 176, 352, 704, 1408,0,0};
    static public final int [] heights = {0, 96, 144, 288, 576, 1152,0,0};
    private int videoWidth=176;   // defualt size
    private int videoHeight=144;
    private boolean FormatSizeInitFlag=false;
    private int payloadLength=4;

    // Check if the native h263peg decoder is there.  If it is, disable
    // this renderer by return null from setInputFormat.
    static boolean nativeAvail = false;
    static {
	if (plugInExists("com.sun.media.codec.video.vh263.NativeDecoder", PlugInManager.CODEC)) {
	    try {
		JMFSecurityManager.loadLibrary("jmutil");
		JMFSecurityManager.loadLibrary("jmvh263");
		nativeAvail = true;
	    } catch (Throwable t) { }
	}
    }


    public JavaDecoder() {
        supportedInputFormats = new VideoFormat[] {new VideoFormat(VideoFormat.H263),new VideoFormat(VideoFormat.H263_RTP) };
        defaultOutputFormats  = new VideoFormat[] {new RGBFormat() };
        PLUGIN_NAME = "H.263 Decoder";

    }

    protected  Format[] getMatchingOutputFormats(Format in) {
      	VideoFormat     ivf  = (VideoFormat) in;
	Dimension       inSize = ivf.getSize();
        int             maxDataLength=ivf.getMaxDataLength();

        if ( (ivf.getEncoding()).equals(VideoFormat.H263_RTP) ) {
          supportedOutputFormats= new  VideoFormat[] {

                new RGBFormat (new Dimension(videoWidth,videoHeight),
                videoWidth * videoHeight, int[].class,
	        ivf.getFrameRate(),			       
                32,
      	        rMask, gMask, bMask,
                1,videoWidth,
		Format.FALSE, // flipped
		Format.NOT_SPECIFIED // endian
                ) /*,

                new RGBFormat (null,
                Format.NOT_SPECIFIED, int[].class,
                32,
      	        rMask, gMask, bMask,
                1,Format.NOT_SPECIFIED )*/ };

        }

        else {
          supportedOutputFormats= new  VideoFormat[] {
                new RGBFormat (new Dimension(inSize),
                inSize.width * inSize.height, int[].class,
                ivf.getFrameRate(),
                32,
      	        rMask, gMask, bMask,
                1,inSize.width,
		Format.FALSE, // flipped
		Format.NOT_SPECIFIED // endian
                ) };
        }

        return  supportedOutputFormats;
    }


    /**
     * Set the data input format.
     * @return false if the format is not supported.
     */
    public Format setInputFormat(Format format) {
	if (nativeAvail)
	    return null;
	if (super.setInputFormat(format) != null) {
	    reset();
	    return format;
	} else
	    return null;
    }

    public void open() throws ResourceUnavailableException {
        initDecoder();
    }

    public void close() {
        javaDecoder=null;
    }

    public void reset() {
       initDecoder();
    }

    // called when video resize is detected, by checkFormat()
    protected void videoResized() {
        initDecoder();
    }

    protected void initDecoder() {
        javaDecoder = new H263Decoder(true);
    }


   public int process(Buffer inputBuffer, Buffer outputBuffer) {


      boolean rtpData = false;

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

        if ( (inData[inputOffset+payloadLength] == 0) && (inData[inputOffset+payloadLength+1] == 0) &&
	                                                ((inData[inputOffset+payloadLength+2] & 0xfc) == 0x80)) {
              int s = (inData[inputOffset+payloadLength+4] >> 2) & 0x7;


              if ( (videoWidth!=widths[s]) || (videoHeight!=heights[s])  ) {

                 videoWidth=widths[s];
                 videoHeight=heights[s];


                 outputFormat = new RGBFormat (new Dimension(videoWidth,videoHeight),
                    videoWidth * videoHeight, int[].class,
                    ivf.getFrameRate(),
                    32,
      	            rMask, gMask, bMask,
                    1,videoWidth,
                    Format.FALSE, // flipped
		    Format.NOT_SPECIFIED // endian
                  );


                  outMaxLength = videoWidth*videoHeight;

                  if (FormatSizeInitFlag)
                    videoResized();       // allocate a new decoder only after it was actually used
               }

               FormatSizeInitFlag=true;

        }

        if (false == FormatSizeInitFlag) {
          return BUFFER_PROCESSED_FAILED;
        }

      }


    int[] outData = validateIntArraySize(outputBuffer,outMaxLength );

      /*
       *  <PATCH> check for insufficient input:
       *  The decoder might read up to 8 additional bytes before checking for EOS
       *  It does not bother the native code, but can cause ArrayOutOfBounds
       *  in Java code
       */
      if ( (inLength+8+inputOffset)>inData.length) {
        if (DEBUG)
         System.out.println("allocating more data for H.263");
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

      if (rtpData)
        inLength-=payloadLength;  // this is the length of the bitstream

      boolean ret = decodeData(inputBuffer,inLength,outputBuffer,rtpData);
      if (ret) {
           updateOutput(outputBuffer,outputFormat, outMaxLength, 0);
           return BUFFER_PROCESSED_OK;
       }
       else {
       if (DEBUG)
         System.out.println("[JavaDecoder] : returning OUTPUT_BUFFER_NOT_FILLED; ");
         return OUTPUT_BUFFER_NOT_FILLED;
       }


    }



    boolean decodeData(Buffer inputBuffer,int inputLength,Buffer outputBuffer,boolean rtpData) {

        int ret;
        int [] outData = (int [])outputBuffer.getData();
        byte[] inputData = (byte [])inputBuffer.getData();


        if (inputLength <= 0) {
            return false;
        }

        javaDecoder.initBitstream();
        int inputOffset=inputBuffer.getOffset();
        if (rtpData) {

//          RTPHeader rtpHeader= (RTPHeader) inputBuffer.getHeader();
            if (DEBUG) {
        	  System.out.println("[javadecoder:decodeData] inputBuffer.getTimeStamp()=" + inputBuffer.getTimeStamp());
//	          System.out.println("[javadecoder:decodeData] rtpHeader.getMarker()=" + inputBuffer.getHeader().getMarker());
                  }
          ret = javaDecoder.DecodeRtpPacket(inputData,inputOffset+payloadLength,inputLength,inputData,inputOffset,inputBuffer.getTimeStamp());
          if(ret ==  H263Decoder.H263_RC_PICTURE_FORMAT_NOT_INITED) {
          if (DEBUG)
            System.out.println("[javadecoder:decodeData] FORMAT_NOT_INITED returing false");
            return false;
            }
        }
        else {
           ret = javaDecoder.DecodePicture(inputData,inputOffset,true);
        }


        if (ret == H263Decoder.H263_RC_PICTURE_FORMAT_NOT_SUPPORTED) {
        if (DEBUG)
            System.out.println("[javadecoder:decodeData] throwing exception - format is not supported ");
            throw new RuntimeException("Currently this picture format is not supported!");
        }

        if (ret == H263Decoder.H263_RC_PICTURE_DONE) {
              int outWidth  = outputFormat.getSize().width;
              int outHeight = outputFormat.getSize().height;
              outputFrame = javaDecoder.CurrentFrame;
	      YCbCrToRGB.convert(outputFrame.Y,outputFrame.Cb,outputFrame.Cr,outData,outputFrame.width,outputFrame.height,outWidth,outHeight,255,4);
              return true;
            }
        else {
        if (DEBUG)
          System.out.println("[javadecoder:decodeData] ret != H263Decoder.H263_RC_PICTURE_DONE returning false");
          return false;
        }


    }


    static public int getPayloadHeaderLength(byte[] input,int offset) {

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

    public boolean checkFormat(Format format) {

      if ( (format.getEncoding()).equals(VideoFormat.H263_RTP) ) {
        return true;
      }
      else {
        return super.checkFormat(format);
      }

    }


}

