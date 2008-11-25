package com.sun.media.codec.video.h261;



import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.ibm.media.codec.video.*;
import java.awt.Dimension;
//import javax.media.rtp.RTPHeader;



public class NativeDecoder extends VideoCodec {


////////////////////////////////////////////////////////////////////////////

// Constants
// CIF/QCIF sizes
//    static final int CIFW = 352;
//   static final int CIFH = 288;
//    static final int QCIFW = 176;
//    static final int QCIFH = 144;
      static public final int [] widths = {176, 352};
      static public final int [] heights = {144, 288};



    // initialize the native codec

    private native boolean initNativeDecoder();

    // free any buffers allocated by the native codec

    private native boolean freeNativeDecoder();

    private native void allocatePicture(int size);
    
    // Decode the compressed video and output a YUV frame

    private native boolean decodeNative(Buffer inputBuffer, Buffer outputBuffer,
					long outBytes);


    ////////////////////////////////////////////////////////////////////////////

    // Variables

    private int nativeData;

    // 32-bit flags

    int h261Flags;

    // End-of-frame marker bit

    int syncBit = 0;

    // QT workaround: Their picture start code is not byte aligned.

    int skipBytes = 0;

    int nframes = 0;

    // Flag a size change to the native code before it starts decoding.

    int sizeChanged = 0;

    private int width=352;
    private int height=288;
    private boolean formatSizeInitFlag=false;


    public NativeDecoder() {

	supportedInputFormats = new VideoFormat[]{new VideoFormat(VideoFormat.H261),
						  new VideoFormat(VideoFormat.H261_RTP)};

        defaultOutputFormats  = new VideoFormat[] {new YUVFormat() };
        PLUGIN_NAME = "H.261 Decoder";

    }



    protected  Format[] getMatchingOutputFormats(Format in) {

      	VideoFormat     ivf  = (VideoFormat) in;
	Dimension       inSize = ivf.getSize();
        int             inMaxDataLength=ivf.getMaxDataLength();
        //Dimension       outSize=(inSize); //XXX sbd: is it always the way ?
        int             outNumOfPixels;

        if ( (ivf.getEncoding()).equals(VideoFormat.H261_RTP) ) {
	    // Varify the sizes.
	    if (inSize != null) {
		Dimension  outSize=movieSizeTo261Size(inSize);
		width=outSize.width;
		height=outSize.height;
	  }
          outNumOfPixels=width*height;
          supportedOutputFormats= new  VideoFormat[] {
                new YUVFormat (
                new Dimension(width,height),
                (outNumOfPixels)+ ((outNumOfPixels) >> 1),
                Format.byteArray,
                ivf.getFrameRate(),			       
                YUVFormat.YUV_420 ,
                width,
                width>>1,
                0,
                outNumOfPixels,
                outNumOfPixels+(outNumOfPixels >>2)
                ) };
          }
        else {
          Dimension  outSize=movieSizeTo261Size(inSize);
          width=outSize.width;
          height=outSize.height;
          outNumOfPixels=outSize.width*outSize.height;
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
	    try {
		close();
		outputFormat = (VideoFormat) getMatchingOutputFormats(ret)[0];
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
            JMFSecurityManager.loadLibrary("jmh261");
            initNativeDecoder();
	    allocateBuffer();
	    super.open();
            return;

	} catch (Throwable e) {
            System.err.println("could not open "+PLUGIN_NAME+". reason:");
            System.err.println(e);
	}


        throw new ResourceUnavailableException("Could not open "+PLUGIN_NAME);
    }



    public void close() {
         freeNativeDecoder();
	 super.close();
    }



    public void reset() {
        // no need to init decoder as first frame is always a key frame
    }



    // called when video resize is detected, by checkFormat()

    protected void videoResized() {
        initNativeDecoder();
	allocateBuffer();
    }


    protected void allocateBuffer() {
	// Always allocate CIF size buffer in case size change occurs at
	// frame boundary.
	int size = 352 * 288;
	size = 4 * (size + size/2);
	allocatePicture(size);
    }


    public void findStartCode(Buffer inputBuffer) {

	byte [] data = (byte[]) inputBuffer.getData();
	int i = 1;
	while (data[i] == 0) {
	    i++;
	}

	skipBytes = i - 1;
	if (data[i] != 1) {
	    skipBytes--;
	    if ((data[i] & 128) != 0)
		h261Flags |= 0x20000000;
	    else if ((data[i] & 64) != 0)
		h261Flags |= 0x40000000;
	    else if ((data[i] & 32) != 0)
		h261Flags |= 0x60000000;
	    else if ((data[i] & 16) != 0)
		h261Flags |= 0x80000000;
	    else if ((data[i] & 8) != 0)
		h261Flags |= 0xa0000000;
	    else if ((data[i] & 4) != 0)
		h261Flags |= 0xc0000000;
	    else if ((data[i] & 2) != 0)
		//discard 7 bits
		h261Flags |= 0xe0000000;
	}

     	i = inputBuffer.getLength() -1;
       	while(data[i] == 0) {
	    i--;
	}


	if (i == 1) {
	    inputBuffer.setLength(i - 1);
	} else {
	    switch (data[i] &0xff) {
	    case 1:
		//should never happen
		break;
	    case 2:
		//discard 7 bits
		h261Flags |= 0x04000000;
		break;
	    case 4:
		h261Flags |= 0x08000000;
		break;
	    case 8:
		h261Flags |= 0x0c000000;
		break;
	    case 16:
		h261Flags |= 0x10000000;
		break;
	    case 32:
		h261Flags |= 0x14000000;
		break;
	    case 64:
		h261Flags |= 0x18000000;
		break;
	    case 128:
		h261Flags |= 0x1c000000;
		break;
	    }
	    inputBuffer.setLength(i-1);
	}

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
	int inLength = inputBuffer.getLength();
	int inMaxLength = ivf.getMaxDataLength();

	byte[] inData =(byte[]) inputBuffer.getData();

	if (ivf.getEncoding().equals(VideoFormat.H261_RTP)){
            rtpData = true;
	    // the RTP H261 Payload header is 4 bytes. Read it into
	    // flags starting at the offset pointed to by the
	    // inputBuffer (the offset points to the end of the RTP header.

            int inputOffset = inputBuffer.getOffset();
            // convert the 4 byte payload header into one int value
            h261Flags = (inData[inputOffset]<<24) + ((inData[inputOffset+1]&0xff)<<16) + ((inData[inputOffset+2]&0xff)<<8) + (inData[inputOffset+3]&0xff) ;

	    //syncBit = ((RTPHeader)(inputBuffer.getHeader())).getMarker();
	    if ( (inputBuffer.getFlags() & Buffer.FLAG_RTP_MARKER) != 0)
	      syncBit = 1;
	    else
	      syncBit = 0;

	    // ask the decoder to skip 4 bytes after the offset as
	    // well to get to the actual h261 compressed payload
	    skipBytes = 4;

            if ( (inData[inputOffset+skipBytes] == 0) && (inData[inputOffset+skipBytes+1] == 1) && ((inData[inputOffset+skipBytes+2] & 0xfc) == 0)) {
                int pictureFormat = (inData[inputOffset+skipBytes+3]>>3)&0x01;
                if ( (width!=widths[pictureFormat]) || (height!=heights[pictureFormat])  ) {
		    
                  width=widths[pictureFormat];
                  height=heights[pictureFormat];
                  int outNumOfPixels=width*height;
		  close();
                  outputFormat = new YUVFormat (
		      new Dimension(width,height),
		      (outNumOfPixels)+ ((outNumOfPixels) >> 1),
		      Format.byteArray,
		      ivf.getFrameRate(),			       
		      YUVFormat.YUV_420 ,
		      width,
		      width>>1,
		      0,
		      outNumOfPixels,
		      outNumOfPixels+(outNumOfPixels >>2)
		      );
		  try {
		      open();
		  } catch (Exception e) {
		      return BUFFER_PROCESSED_FAILED;
		  }
		}

                formatSizeInitFlag=true;
                //System.out.println("start code , width "+width+" height "+height);
            }

	/*
	-ivg
	 I've fixed the H261 decoder to initialize to the right size
	 so it wouldn't have to wait for the first start code.  But that
	 didn't work very well.  The decoder crashes.  So I'm keeping the
	 guard in for now.  Will have to look at it later.
            if (false == formatSizeInitFlag) {
              return BUFFER_PROCESSED_FAILED;
            }
	*/

	} else {

	    // format is not H261_RTP
	    h261Flags = 0x01000000;
	    syncBit = 1;
	    findStartCode(inputBuffer);
	}

	outputBuffer.setFormat(outputFormat);
	int outMaxLength = outputFormat.getMaxDataLength();
	Object outData = validateData(outputBuffer, outMaxLength * 2, true);
	long outBytes = getNativeData(outData);
	boolean ret = decodeNative(inputBuffer, outputBuffer, outBytes);
        if (ret && syncBit == 1) {
	    updateOutput(outputBuffer,outputFormat, outMaxLength, 0);
	    return BUFFER_PROCESSED_OK;
        } else {
	    return OUTPUT_BUFFER_NOT_FILLED;
        }

   }

    private Dimension movieSizeTo261Size(Dimension movieSize) {
       int width  = (movieSize.width  + 0xf) & 0xfffffff0;
       int height = (movieSize.height + 0xf) & 0xfffffff0;
       int formatCap = (width * height) >> 8;

       /* transform the number of macroblocks into format*/
       if (formatCap <= 99)
           return new Dimension(176,144);   // QCIF
       if (formatCap <= 396)
           return new Dimension(352,288);   // CIF

       return new Dimension(0,0);    // ERROR

    }


   public boolean checkFormat(Format format) {

      if ( (format.getEncoding()).equals(VideoFormat.H261_RTP) ) {
        return true;
      }
      else {
        return super.checkFormat(format);
      }

    }


}



