/*
 * @(#)NativeEncoder.java	1.26 03/04/24
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

/*
 *  Licensed Materials - Property of IBM
 *  "Restricted Materials of IBM"
 *  5648-B81
 *  (c) Copyright IBM Corporation 1997,1998 All Rights Reserved
 *  US Government Users Restricted Rights - Use, duplication or
 *  disclosure restricted by GSA ADP Schedule Contract with
 *  IBM Corporation.
 *
 */

package com.ibm.media.codec.video.h263;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.sun.media.controls.*;
import com.ibm.media.codec.video.*;
import java.awt.Dimension;
import javax.media.rtp.*;
import javax.media.control.*;
import java.awt.*;
import java.awt.event.*;

public class NativeEncoder extends VideoCodec {


    ////////////////////////////////////////////////////////////////////////////
    // Constants

    /**
     *  Licensed Materials - Property of IBM
     *  "Restricted Materials of IBM"
     *  5648-B81
     *  (c) Copyright IBM Corporation 1997,1999 All Rights Reserved
     *  US Government Users Restricted Rights - Use, duplication or
     *  disclosure restricted by GSA ADP Schedule Contract with
     *  IBM Corporation.
     *
     **/
    public static final String a_copyright_notice="(c) Copyright IBM Corporation 1997, 1999.";
    ////////////////////////////////////////////////////////////////////////////
    // Variables

    static final int [] widths = {0, 128, 176, 352, 704, 1408,0,0};
    static final int [] heights = {0, 96, 144, 288, 576, 1152,0,0};
    private int videoWidth=176;   // defualt size
    private int videoHeight=144;

    int nativeFormat=0;  // native representation of format

    // Variables & constants needed for the RTP

    // final static int   DEFAULT_RTP_MTU = 576; //XXX
    // set default to 1024 - RTP size (12) - UDP size (8) - IP size (20)
    final static int   DEFAULT_RTP_MTU = 984;
    final static int   MAX_RTP_MTU = 1456;
    final static int   DEFAULT_MAX_OUTPUT_LENGTH = 40960; // should be ported from the native dll export
    public int         maxOutputLength;
    public int         targetOutputLength;
    long               timeStamp = Buffer.TIME_UNKNOWN;   // change to random ?
    long               sequenceNumber = 0; // change to random ?
    long               deltaFrames = (long) (1000000.0/29.97);    // in Nano Seconds
    boolean            useRtp = false;
    Control[]          controls=null;
    float              sourceFrameRate=0;
    float              targetFrameRate=0;
    int                minBitRate=5000;
    int                maxBitRate=1000000;
    int                useBitRate=20000;
    int                iFramePeriod=15;

    int                frameDecimation=1;
    int                frame2skip=0;

    boolean initCompleted=false;
    boolean settingsChanged = false;
    boolean dropFrame = false;
    boolean okToDrop = false;

    EncodeControl encodeControl = null;

    ////////////////////////////////////////////////////////////////////////////
    // Native interface

//    private native boolean initNativeEncoder(int nativeFormat,boolean rtpSessionFlag);
      private native boolean initNativeEncoder(int nativeFormat,
                                             int MTUPacketSize,
					     float sourceFrameRate,
					     float targetFrameRate,
				             int targetBitRate,
                                             int IFramePeriod);


     /* new Init to push all controls into PUSER
       nativeFormat
       MTUPacketSize(0 for non RTP sessions)
       sourceFrameRate
       targetFrameRate
       targetBitRate
       IFramePeriod
*/

    private native boolean setFramesBehind(int numOfFrames);
    private native boolean setQuality(float quality);

    private native boolean encodeFrameNative(Buffer in, Buffer out);

    private native boolean closeNativeEncoder();


    // java variables to be accessed by native

    private int nativeData = 0;

    private int        prevTr =0; //previous temporal reference
    private int        tr;          // current temporal reference
    public boolean     frameDone = true ; // we start a new frame
    private int        outputLength;
    private boolean    interFlag;




    // Constructor
    public NativeEncoder() {
///       System.out.println("[h263Encoder:Constructor]");
        supportedInputFormats = new VideoFormat[] {
	                                new YUVFormat(YUVFormat.YUV_420)
			        };

        defaultOutputFormats  = new VideoFormat[] {
	                                new VideoFormat(VideoFormat.H263),
					new VideoFormat(VideoFormat.H263_RTP)
			        };

        PLUGIN_NAME = "H.263 Encoder";
    }


    public Format setInputFormat(Format format) {
        YUVFormat     ivf  = (YUVFormat) super.setInputFormat(format);
        if (ivf==null)
            return null;

	Dimension       inSize = ivf.getSize();
        if (inSize==null)
            return null;
	if (ivf.getOffsetU() > ivf.getOffsetV())
	    return null;
        videoWidth  =   inSize.width;
        videoHeight =   inSize.height;

        sourceFrameRate=ivf.getFrameRate();

        deltaFrames = (long) (1000000.0/ivf.getFrameRate());    // in Nano Seconds

	if (opened) {
	    VideoFormat newOut;
	    newOut = 
                new VideoFormat (
				 outputFormat.getEncoding(),
				 new Dimension(inSize),
				 Format.NOT_SPECIFIED,
				 Format.byteArray,
				 ivf.getFrameRate());
	    close();
	    setOutputFormat(newOut);
	    try {
		open();
	    } catch (ResourceUnavailableException re) {
		return null;
	    }
	}
        return  format;

    }


    /** Hagai
      * overides VideoCodec.setOutputFormat in order to init RTP variables
     */
    public Format setOutputFormat(Format format) {
	VideoFormat f = (VideoFormat)super.setOutputFormat(format);
	if (f.getMaxDataLength() == Format.NOT_SPECIFIED) {
	    if (f.getEncoding().equals(VideoFormat.H263_RTP)) {
		useRtp = true;
		maxOutputLength = MAX_RTP_MTU;
		targetOutputLength = DEFAULT_RTP_MTU;
	    } else {
		useRtp = false;
		maxOutputLength = DEFAULT_MAX_OUTPUT_LENGTH;
		targetOutputLength = DEFAULT_MAX_OUTPUT_LENGTH;
	    }
	    f = new VideoFormat(f.getEncoding(),f.getSize(),maxOutputLength,Format.byteArray,f.getFrameRate());
	    targetFrameRate=f.getFrameRate();
	    frameDecimation= (int)(sourceFrameRate/targetFrameRate);
	    useBitRate = (int) ((targetFrameRate * f.getSize().width *
				 f.getSize().height) / 5);
	    f = (VideoFormat)super.setOutputFormat(f);

	}
	return f;  // How do we define who is responsible for initiating the MTU
    }


    protected  Format[] getMatchingOutputFormats(Format in) {
      	VideoFormat     ivf  = (VideoFormat) in;
	Dimension       inSize = ivf.getSize();
	
        if (inSize==null)
            return null;

	videoWidth  =   inSize.width;
        videoHeight =   inSize.height;

	// Amith - allow only default frame rate
        supportedOutputFormats= new  VideoFormat[2];
        for (int i=0;i<2;) {
            float useFrameRate=ivf.getFrameRate();
            if (i==2)
                useFrameRate /= 2.0F;

            if (i==4)
                useFrameRate /= 3.0F;

            supportedOutputFormats[i++]=
                new VideoFormat (
                    VideoFormat.H263,
                    new Dimension(inSize),
                    Format.NOT_SPECIFIED,
                    Format.byteArray,
		    useFrameRate);

            supportedOutputFormats[i++]=
		    new VideoFormat (              //Hagai
                    VideoFormat.H263_RTP,
                    new Dimension(inSize),
                    Format.NOT_SPECIFIED,
                    Format.byteArray,
		    useFrameRate);
	}

        return  supportedOutputFormats;
    }


    public void open() throws ResourceUnavailableException {

	// Validate sizes here.

        // Native format
        //
        //	|FRAME FORMAT		width		height	    image_format |
	//	|- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - |
	//	|SQCIF			 128		 96		  0	 |
	//	|QCIF (PAL)		 176		 144		  1      |
	//	|CIF (PAL)		 352		 288              2	 |


        ///===add support for non standard frame size here==
        if ( (videoWidth==128) && (videoHeight==96 ) ) {
             nativeFormat=0;
        } else if ( (videoWidth==176) && (videoHeight==144) ) {
             nativeFormat=1;
        } else if ( (videoWidth==352) && (videoHeight==288) ) {
             nativeFormat=2;
        } else  {
	    Log.error("Class: " + this);
	    Log.error("  can only encode in sizes: 128x96, 176x144, 352x288.");
	    throw new ResourceUnavailableException("could not load jmvh263");
	}

        try {
            JMFSecurityManager.loadLibrary("jmutil");
            JMFSecurityManager.loadLibrary("jmh263enc");
            //initFrame();
//            initEncoder();
	    super.open();
	    if (encodeControl != null)
		encodeControl.open(controls);
            return;
            }
	catch (Throwable e) {
            System.out.println(e);
        }

	//System.out.println("[h263encoder::open]could not load jmvh263");
	throw new ResourceUnavailableException("could not load jmvh263");
    }


    synchronized public void  close() {
//       System.out.println("[h263Encoder:close]");
	if (encodeControl != null)
	    encodeControl.close();
        closeNativeEncoder();
	super.close();
    }

    protected void finalize() {
	if (encodeControl != null) {
	    encodeControl.frame.dispose();
	    encodeControl = null;
	}
    }

    private synchronized void  closeNative() {
        closeNativeEncoder();
    }


    public final synchronized void  reset() {
//       System.out.println("[h263Encoder:reset]");
       initEncoder();
       settingsChanged = false;
    }


    // called when video resize is detected, by checkFormat()
    protected void videoResized() {
//   System.out.println("[h263Encoder:videoResized]");
        initEncoder();
    }


    protected void initEncoder() {
       if (maxOutputLength != outputFormat.getMaxDataLength() ) {
           VideoFormat f=outputFormat;
           outputFormat = new VideoFormat(f.getEncoding(),f.getSize(),maxOutputLength,Format.byteArray,f.getFrameRate());
//           System.out.println("of "+outputFormat);
       }
/*
       System.out.println("[h263Encoder:initEncoder]");
       System.out.println("nativeFormat="+ nativeFormat);
       System.out.println("sourceFrameRate="+ sourceFrameRate);
       System.out.println("targetFrameRate="+ targetFrameRate);
       System.out.println("useBitRate="+ useBitRate);
       System.out.println("iFramePeriod="+ iFramePeriod);
       System.out.println("maxOutputLength="+ maxOutputLength);
       System.out.println("targetOutputLength="+ targetOutputLength);
       System.out.println("useRTP="+useRtp);
*/
       closeNative();
//       System.out.println("[h263Encoder:initEncoder] : closed");
//       initNativeEncoder(nativeFormat,useRtp);

       if (useRtp)
          initNativeEncoder(nativeFormat,
                         targetOutputLength, //MTUPacketSize
			 sourceFrameRate,
			 targetFrameRate,
			 useBitRate, //targetBitRate,
			 iFramePeriod );//IFramePeriod
        else
                  initNativeEncoder(nativeFormat,
                         0, // "No RTP"
			 sourceFrameRate,//sourceFrameRate,
			 targetFrameRate,//targetFrameRate,
			 useBitRate, //targetBitRate,
			 iFramePeriod);//IFramePeriod



//       System.out.println("[h263Encoder:initEncoder] : done");
    }


   synchronized public int  process(Buffer inputBuffer, Buffer outputBuffer) {

      if (!initCompleted) {
//          System.out.println("init encoder");
          initCompleted=true;
          initEncoder();
      }

      // Drop frame when necessary.
      if (okToDrop && dropFrame) {
	  dropFrame = false;
	  outputBuffer.setDiscard(true);
	  if (settingsChanged)
	      reset();
	  return BUFFER_PROCESSED_OK;
      }

      okToDrop = false;

//    System.out.println("[h263Encoder:process]");
       if ( frameDone==true) {
//          System.out.println(frame2skip);
          frame2skip++;
          if (frame2skip!=frameDecimation) {
              updateOutput(outputBuffer,outputFormat, 0 , 0);
//              outputBuffer.setDiscard(true);
              return OUTPUT_BUFFER_NOT_FILLED;
          }
          frame2skip=0;

          if (!checkInputBuffer(inputBuffer) ) {
              return BUFFER_PROCESSED_FAILED;
              }

	  if (isEOM(inputBuffer) ) {
	      propagateEOM(outputBuffer);
	      okToDrop = true;
	      if (settingsChanged)
		  reset();
	      return BUFFER_PROCESSED_OK;
	  }
       }
       VideoFormat ivf=(VideoFormat) inputBuffer.getFormat();
       int inLength=inputBuffer.getLength();
       int inMaxLength=ivf.getMaxDataLength();
       int outMaxLength=outputFormat.getMaxDataLength();
       int inputOffset=inputBuffer.getOffset();
       
       if (outMaxLength < MAX_RTP_MTU)
	   outMaxLength = MAX_RTP_MTU;

      byte[] inData =(byte[]) inputBuffer.getData();
      byte[] outData = validateByteArraySize(outputBuffer,outMaxLength );

      boolean ret = encodeFrameNative(inputBuffer,outputBuffer);
//      if (frameDone == false)
//      System.out.println("[NativeEncoder:process] tr=" +tr + " outputLength= "+ outputLength + " timeStamp=" + inputBuffer.getTimeStamp() );

      if (outputLength>=0)
	      outputBuffer.setLength(outputLength);

      // update the output buffer fileds that are needed within the RTP header.
      if (useRtp) {
           outputBuffer.setSequenceNumber(sequenceNumber);

      sequenceNumber++;

//      RTPHeader header = (RTPHeader)outputBuffer.getHeader(); // this should have been done elsewhere ??
//      if (null == header)
//               header = new RTPHeader();

        if (true == frameDone) {


                timeStamp = inputBuffer.getTimeStamp();
                // The above libe should be repaced with the line bellow if there is a possibility that the input
                // Buffer does not have a timeStamp
                // timeStamp += ((long)((tr-prevTr)&0x000000ff))*deltaFrames;
                prevTr= tr;
 	  	int flags =  outputBuffer.getFlags();
                flags |= Buffer.FLAG_RTP_MARKER;
 	        outputBuffer.setFlags(flags);
                }
        else    {
                }
	  // No need to set the time stamp.
          //outputBuffer.setTimeStamp(timeStamp);
      } else {  // set key frame for intra frames
          int flags =  outputBuffer.getFlags();
          if (!interFlag)
              flags |= Buffer.FLAG_KEY_FRAME;
          else
	      flags &= ~Buffer.FLAG_KEY_FRAME;
          outputBuffer.setFlags(flags);
      }


      // note that encoder update output length
      updateOutput(outputBuffer,outputFormat, outputBuffer.getLength() , 0);
      // Amith - added this because outputFormat is defined in both BasicCodec
      //         and VideoCodec
      outputBuffer.setFormat(outputFormat);
      
      if (true == frameDone)
        if (outputLength !=-1) {
	    okToDrop = true;
	    if (settingsChanged)
	        reset();
            return BUFFER_PROCESSED_OK;
        } else {
//            System.out.println("[NativeEncoder:process]Last Gob Does not fit intop MTU");
            return (OUTPUT_BUFFER_NOT_FILLED);
        }
      else
        if (outputLength ==-1) { // a gob doesn't fit in an MTU
//             System.out.println("[NativeEncoder:process] A gob doesn't fit in an MTU");
             return (INPUT_BUFFER_NOT_CONSUMED|OUTPUT_BUFFER_NOT_FILLED);
         }
      else
            return INPUT_BUFFER_NOT_CONSUMED;
    } // end of process


    public java.lang.Object[] getControls() {
        if (controls==null) {
             controls=new Control[8];
             controls[0]=new H263Adapter(this, false, false,false,false,false,0,1000,false);
             controls[1]=new BitRateAdapter(this,useBitRate,minBitRate,maxBitRate,true);
             controls[2]=new KeyFrameAdapter(this,iFramePeriod,true);
             controls[3]=new QualityAdapter(this,1.0F,0.0F,1.0F,false,true);
             controls[4]=new FrameRateAdapter(this,targetFrameRate,
	                                           sourceFrameRate/3,
						   sourceFrameRate,true);
             controls[5]=new FrameProcessingAdapter(this);
             controls[6]=new PacketSizeAdapter(this,targetOutputLength,true);
	     encodeControl = new EncodeControl(controls);
	     controls[7]= encodeControl;
        }

        return (Object[])controls;
    }
}


class EncodeControl implements javax.media.Control {

    Control [] controls;
    Button button;
    Frame frame = null;
    
    public EncodeControl(Control [] controls) {
	open(controls);
    }

    public void open(Control [] controls) {
	this.controls = controls;
	createFrame();
    }

    public void close() {
	synchronized (this) {
	    controls = null;
	    if (frame != null) {
		frame.removeAll();
		frame.setVisible(false);
	    }
	}
    }
    
    private void createFrame() {
	synchronized (this) {
	    if (frame != null)
		frame.removeAll();
	    else
		frame = new Frame("H.263 Control");
	}
        frame.setLayout(new com.sun.media.controls.VFlowLayout(1));
        frame.add(new Label( "H.263 Control",Label.CENTER) );
        frame.add(new Label( " "));

        for (int i=0;i<controls.length;i++) {
	   if (controls[i] != null && controls[i] != this)
		frame.add(controls[i].getControlComponent() );
        }

        //System.out.println(c.length);
        frame.pack();
	frame.addWindowListener( new WindowAdapter() {
	    public void windowClosing(WindowEvent we) {
		frame.setVisible(false);
	    }
	} );
    }

    public Component getControlComponent() {
	if (button == null) {
	    button = new Button("H.263 Encoding Controls");
	    button.setName("H.263 Control");
	    button.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent ae) {
		    createFrame();
		    frame.setVisible( true );
		}
	    } );
	}
	return button;
    }
}


class BitRateAdapter extends com.sun.media.controls.BitRateAdapter implements Owned{
    NativeEncoder owner;

    public BitRateAdapter(NativeEncoder owner,int initialBitRate, int minBitRate,
                          int maxBitRate, boolean settable) {

         super(initialBitRate, minBitRate ,maxBitRate, settable);
         this.owner=owner;
    }

    public java.lang.Object getOwner() {
        return (Object) owner;
    }

    public int setBitRate(int newValue) {

        owner.useBitRate=super.setBitRate(newValue);
	owner.settingsChanged = true;
        return owner.useBitRate;

    }

}


class KeyFrameAdapter extends com.sun.media.controls.KeyFrameAdapter implements Owned{
    NativeEncoder owner;

    public KeyFrameAdapter(Codec owner,int preferredInterval, boolean settable) {

         super(preferredInterval, settable);
         this.owner=(NativeEncoder)owner;
    }
    public int setKeyFrameInterval(int newValue) {

        owner.iFramePeriod=super.setKeyFrameInterval(newValue);
	owner.settingsChanged = true;
        return owner.iFramePeriod;
    }


    public java.lang.Object getOwner() {
        return (Object) owner;
    }

}


class QualityAdapter extends com.sun.media.controls.QualityAdapter implements Owned{
    Codec owner;

    public QualityAdapter(Codec owner,float preferred, float min, float max,boolean isTSsupported,
			  boolean settable) {

         super( preferred, min, max, isTSsupported,settable);
         this.owner=owner;
    }

    public java.lang.Object getOwner() {
        return (Object) owner;
    }

}


class FrameRateAdapter extends com.sun.media.controls.FrameRateAdapter {

     public FrameRateAdapter(Object owner, float initialFrameRate, float minFrameRate,
			    float maxFrameRate, boolean settable) {

         super( owner, initialFrameRate, minFrameRate, maxFrameRate,settable);
    }

    public float setFrameRate(float frameRate) {
        NativeEncoder owner=(NativeEncoder)super.owner;

	int skipFrames=(int)(owner.sourceFrameRate / frameRate);
        float useFrameRate= owner.sourceFrameRate / skipFrames;

        if (useFrameRate>max)
            useFrameRate=max;

        if (useFrameRate<min)
            useFrameRate=min;

        owner.targetFrameRate=super.setFrameRate(useFrameRate);
	owner.settingsChanged = true;

        return owner.targetFrameRate;
    }

}


class PacketSizeAdapter extends com.sun.media.controls.PacketSizeAdapter {

    public PacketSizeAdapter(Codec owner, int packetSize, boolean settable) {
         super( owner, packetSize, settable);
    }

    public int setPacketSize(int numBytes) {
        NativeEncoder owner=(NativeEncoder)super.owner;

        if ((numBytes<200)  ||
            (numBytes>owner.MAX_RTP_MTU) ||
            (!owner.useRtp) )
            return owner.targetOutputLength;

        // int useBytes=super.setPacketSize(numBytes); // Just returns current!
        int useBytes=numBytes;
        //System.out.println("[H263-RTP]new Packet size "+useBytes);
        owner.targetOutputLength=useBytes;
        if (owner.targetOutputLength>owner.maxOutputLength)
            owner.maxOutputLength=owner.targetOutputLength;
	owner.settingsChanged = true;

	// set the value in the adapter so it is returned by getPacketSize
	packetSize=useBytes;
        return useBytes;
    }

}


class FrameProcessingAdapter implements FrameProcessingControl,Owned {
     NativeEncoder owner;
     public FrameProcessingAdapter(NativeEncoder owner) {
        this.owner=owner;
    }

    public boolean setMinimalProcessing(boolean newMinimal) {
//        minimal = newMinimal;
//        return minimal;
        return true;
    }

    public void setFramesBehind(float frames) {
        if (frames >= 1)
            owner.dropFrame = true;
        else
            owner.dropFrame = false;
    }

    public int getFramesDropped() {
        return 0;       ///XXX not implemented
    }

    public Component getControlComponent() {
        return new Label("Frame processing");
    }

    public Object getOwner() {
        return (Object)owner;
    }
}
