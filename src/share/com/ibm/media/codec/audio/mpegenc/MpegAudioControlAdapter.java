/*
 * @(#)MpegAudioControl.java	1.1 99/03/03
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.ibm.media.codec.audio.mpegenc;

import javax.media.*;
import javax.media.control.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Implementation for mpeg audio encoder control
 */
public class MpegAudioControlAdapter implements MpegAudioControl,
             BitRateControl, Owned {

    Component component=null;

    NativeEncoder owner=null;

    int layer;

    int bitrate;

    int error_protection;

    int nChannels;

    int joint_stereo;

    int original;

    int copyright;

    int layout;

    int samplingFrequency;
    
    int[] availableRates;
    /**
     * Indicates support for audio layer 1
     */
    public final static int LAYER_1   = 1<<0;

    /**
     * Indicates support for audio layer 2
     */
    public final static int LAYER_2  = 1<<1;

    /**
     * Indicates support for audio layer 3
     */
    public final static int LAYER_3  = 1<<2;

    /**
     * Returns the audio layer support capability.<br>
     * The returned value consists of a logical OR among the relevant flags.
     * @see #LAYER_1
     * @see #LAYER_2
     * @see #LAYER_3
     */
    public int getSupportedAudioLayers() {
        return LAYER_1 | LAYER_2;
    }

    /**
     * Indicates support for 16 KHz audio sampling rate
     */
    public final static int SAMPLING_RATE_16	= 1<<0;

    /**
     * Indicates support for 22.05 KHz audio sampling rate
     */
    public final static int SAMPLING_RATE_22_05 = 1<<1;
    /**
     * Indicates support for 24 KHz audio sampling rate
     */
    public final static int SAMPLING_RATE_24   	= 1<<2;
    /**
     * Indicates support for 32 KHz audio sampling rate
     */
    public final static int SAMPLING_RATE_32 	= 1<<3;
    /**
     * Indicates support for 44.1 KHz audio sampling rate
     */
    public final static int SAMPLING_RATE_44_1  = 1<<4;
    /**
     * Indicates support for 48 KHz audio sampling rate
     */
    public final static int SAMPLING_RATE_48   = 1<<5;

    /**
     * Returns the audio sampling rate support capability.<br>
     * The returned value consists of a logical OR among the relevant flags.
     * @see #SAMPLING_RATE_16
     * @see #SAMPLING_RATE_22_05
     * @see #SAMPLING_RATE_24
     * @see #SAMPLING_RATE_32
     * @see #SAMPLING_RATE_44_1
     * @see #SAMPLING_RATE_48
     */
    public int getSupportedSamplingRates() {
        return SAMPLING_RATE_32 | SAMPLING_RATE_44_1 | SAMPLING_RATE_48;
    }

    /**
     * Indicates support for single channel layout
     */
    public final static int SINGLE_CHANNEL  	= 1<<0;

    /**
     * Indicates support for two channels stereo layout
     */
    public final static int TWO_CHANNELS_STEREO  = 1<<1;

    /**
     * Indicates support for two channels dual layout
     */
    public final static int TWO_CHANNELS_DUAL   	= 1<<2;

    /**
     * Indicates support for three channels 2-1 layout
     * (Left, Right and single Surround)
     */
    public final static int THREE_CHANNELS_2_1   	= 1<<2;

    /**
     * Indicates support for three channels 3-0 layout
     * (Left, Center and Right)
     */
    public final static int THREE_CHANNELS_3_0   	= 1<<3;

    /**
     * Indicates support for four channels 2-0 2-0 layout
     * (Left and Right of first program, Left and Right of second program)
     */
    public final static int FOUR_CHANNELS_2_0_2_0	= 1<<4;

    /**
     * Indicates support for four channels 2-2 layout
     * (Left, Right, Left Surround and Right Surround)
     */
    public final static int FOUR_CHANNELS_2_2   	= 1<<5;

    /**
     * Indicates support for four channels 3-1 layout
     * (Left, Center, Right and single Surround)
     */
    public final static int FOUR_CHANNELS_3_1   	= 1<<6;

    /**
     * Indicates support for five channels 3-0 2-0 layout
     * (Left, Center and Right of first program, Left and Right of
     * second program)
     */
    public final static int FIVE_CHANNELS_3_0_2_0	= 1<<7;

    /**
     * Indicates support for five channels 3-2 layout
     * (Left, Center, Right, Left Surround and Right surround)
     */
    public final static int FIVE_CHANNELS_3_2		= 1<<8;

    /**
     * Returns the audio channel layout support capability.<br>
     * The returned value consists of a logical OR among the relevant flags.
     * @see #SINGLE_CHANNEL
     * @see #TWO_CHANNELS_STEREO
     * @see #TWO_CHANNELS_DUAL
     * @see #THREE_CHANNELS_2_1
     * @see #THREE_CHANNELS_3_0
     * @see #FOUR_CHANNELS_2_0_2_0
     * @see #FOUR_CHANNELS_2_2
     * @see #FIVE_CHANNELS_3_0_2_0
     * @see #FIVE_CHANNELS_3_2
     */
    public int getSupportedChannelLayouts() {
        return SINGLE_CHANNEL | TWO_CHANNELS_STEREO;
    }

    /**
     * Returns the low freuqency channel support capability.<br>
     */
    public boolean isLowFrequencyChannelSupported() {
        return false;
    }

    /**
     * Returns the multilingual mode support capability.<br>
     */
    public boolean isMultilingualModeSupported() {
        return false;
    }

    /**
     * Controls the MPEG Audio Layer.<br>
     * Returns the layer which was actually set.
     */
    public int setAudioLayer(int audioLayer) {
//        layer=owner.setAudioLayer(audioLayer);
        return layer;
    }

    /**
     * Returns the current MPEG Audio Layer.<br>
     */
    public int getAudioLayer() {
        return layer;
    }

    /**
     * Controls the MPEG Audio channel layout.<br>
     * Returns the channel layout which was actually set.
     */
    public int setChannelLayout(int channelLayout){
        return layout;
    }

    /**
     * Returns the current MPEG Audio channel layout.<br>
     */
    public int getChannelLayout(){
        return layout;
    }

    /**
     * Controls the Low Frequency Channel mode.
     * "true" is on; "false" is off. <br>
     * Returns true if Low Frequency Channel mode is actually turned on.
     */
    public boolean setLowFrequencyChannel(boolean on){
        return false;
    }

    /**
     * Returns true if Low Frequency Channel mode is turned on.
     */
    public boolean getLowFrequencyChannel(){
        return false;
    }

    /**
     * Controls the Multilingual mode.
     * "true" is on; "false" is off. <br>
     * Returns true if Multilingual mode is actually turned on.
     */
    public boolean setMultilingualMode(boolean on){
        return false;
    }

    /**
     * Returns true if Multilingual mode is turned on.
     */
    public boolean getMultilingualMode() {
        return false;
    }


    public java.lang.Object getOwner() {
        return (Object)owner;
    }

    /**
     * Returns the current bit rate of the owning object. If the stream
     * is of variable bit rate, then the value returned is an
     * instantaneous or average bit rate over a period of time.
     */
     public int getBitRate() {
         return bitrate;
     }

    /**
     * Sets the bit rate of the owning object. This is mostly relevant
     * in the case of encoders. If the bit rate cannot be controlled, then
     * the return value is -1.
     * @return the bit rate that was actually set on the object, or -1
     *         if the bit rate is not controllable.
     */
     public int setBitRate(int bitrate) {
//         bitrate=owner.setBitRate(bitrate);
         return bitrate;

     }

    /**
     * Returns the lowest bit rate that this object can encode the
     * data stream to.
     */
     public int getMinSupportedBitRate() {
         return 32000;
     }

    /**
     * Returns the highest bit rate that this object can encode the
     * data stream to.
     */
     public int getMaxSupportedBitRate() {
         return 448000;
     }

     String SAMPLING_FREQUENCY_STRING = " Sampling Frequency ";
     String ERROR_PROTECTION_STRING   = " Error Protection ";
     String ORIGINAL_STRING           = " Original ";
     String COPYRIGHT_STRING          = " Copyright ";
     String LAYER_STRING              = " Layer ";
     String LAYER_1_STRING            = " Layer 1 ";
     String LAYER_2_STRING            = " Layer 2 ";
     String MODE_STRING               = " Mode ";
     String MONO_STRING               = " Mono ";
     String STEREO_STRING             = " Stereo ";
     String JOINT_STEREO_STRING       = " Joint Stereo ";
     String BITRATE_STRING            = " Bitrate ";


     public java.awt.Component getControlComponent() {

         if (component==null) {
            Panel componentPanel=new Panel();
            componentPanel.setLayout(new com.sun.media.controls.VFlowLayout(0));

            Panel freqPanel=new Panel();
            freqPanel.setLayout(new BorderLayout() );
            freqPanel.add("West", new Label(SAMPLING_FREQUENCY_STRING,Label.CENTER) );
            freqPanel.add("East", new Label(samplingFrequency+"",Label.CENTER) );
            componentPanel.add(freqPanel);

            Panel errPanel=new Panel();
            errPanel.setLayout(new BorderLayout() );
            errPanel.add("West",new Label(ERROR_PROTECTION_STRING ,Label.CENTER) );
            Checkbox err_cb=new Checkbox(null,null,error_protection!=0);
            err_cb.addItemListener( new ItemListener() {
                  public void itemStateChanged(ItemEvent e) {
                       int new_error_protection=e.getStateChange();
                       if (!owner.isStarted) {
                           error_protection=(new_error_protection==e.SELECTED) ? 1 : 0 ;
                           owner.setErrorProtect(error_protection);
                       } else {
                           Checkbox in_cb=(Checkbox)e.getItemSelectable();
                           in_cb.setState(error_protection!=0);
                       }
                  }
	    } );
	    errPanel.add("East",err_cb );

            Panel orgPanel=new Panel();
            orgPanel.setLayout(new BorderLayout() );
            orgPanel.add("West",new Label(ORIGINAL_STRING,Label.CENTER) );
            Checkbox org_cb=new Checkbox(null,null,original!=0);
            org_cb.addItemListener( new ItemListener() {
                  public void itemStateChanged(ItemEvent e) {
                       int new_original=e.getStateChange();
                       if (!owner.isStarted) {
                           original=(new_original==e.SELECTED) ? 1 : 0 ;
                           owner.setOriginal(original);
                       } else {
                           Checkbox in_cb=(Checkbox)e.getItemSelectable();
                           in_cb.setState(original!=0);
                       }
                  }
	    } );
	    orgPanel.add("East",org_cb );

            Panel cpyPanel=new Panel();
            cpyPanel.setLayout(new BorderLayout() );
            cpyPanel.add("West",new Label(COPYRIGHT_STRING,Label.CENTER) );
            Checkbox cpy_cb=new Checkbox(null,null,copyright!=0);
            cpy_cb.addItemListener( new ItemListener() {
                  public void itemStateChanged(ItemEvent e) {
                       int new_copyright=e.getStateChange();
                       if (!owner.isStarted) {
                           copyright=(new_copyright==e.SELECTED) ? 1 : 0 ;
                           owner.setOriginal(copyright);
                       } else {
                           Checkbox in_cb=(Checkbox)e.getItemSelectable();
                           in_cb.setState(copyright!=0);
                       }
                  }
	    } );
	    cpyPanel.add("East",cpy_cb );

            Panel layerPanel=new Panel();
            layerPanel.setLayout(new BorderLayout() );
            layerPanel.add("West",new Label(LAYER_STRING,Label.CENTER) );
            Choice layer_choice=new Choice();
            layer_choice.add(LAYER_1_STRING);
            layer_choice.add(LAYER_2_STRING);
            layer_choice.select(LAYER_2_STRING);

            layer_choice.addItemListener( new ItemListener() {
                  public void itemStateChanged(ItemEvent e) {
                       String s=(String)e.getItem();
                       if (!owner.isStarted) {
                           layer=s.equals(LAYER_1_STRING) ? 1 : 2;
                           setAvailableRates();
                           owner.setLayer(layer);
                       } else {
                           Choice in_choice=(Choice)e.getItemSelectable();
                           in_choice.select( (layer==1) ? LAYER_1_STRING : LAYER_2_STRING);
                       }

                  }

	    } );
	    layerPanel.add("East",layer_choice );

            Panel cnlPanel=new Panel();
            cnlPanel.setLayout(new BorderLayout() );
            cnlPanel.add("West",new Label(MODE_STRING,Label.CENTER) );
            Choice mode_choice=new Choice();

            if (nChannels==1) {
                mode_choice.add(MONO_STRING);

            } else {
                mode_choice.add(STEREO_STRING);
                mode_choice.add(JOINT_STEREO_STRING);
                mode_choice.select( (joint_stereo !=0 ) ?
                                    JOINT_STEREO_STRING :
                                    STEREO_STRING );

                mode_choice.addItemListener( new ItemListener() {
                      public void itemStateChanged(ItemEvent e) {
                           String s=(String)e.getItem();
                           if (!owner.isStarted) {
                               joint_stereo= s.equals(STEREO_STRING) ? 0 : 1;

                               int mode=(joint_stereo==0) ?
                                    NativeEncoder.MPEG_STEREO :
                                    NativeEncoder.MPEG_JOINT_STEREO ;

                               owner.SetEncodingType(mode);
                           } else {
                               Choice in_choice=(Choice)e.getItemSelectable();
                               in_choice.select( (joint_stereo !=0 ) ?
                                                 JOINT_STEREO_STRING :
                                                 STEREO_STRING );
                           }

                      }
                    } );

            }
            cnlPanel.add("East",  mode_choice );


            Panel ratePanel=new Panel();
            ratePanel.setLayout(new BorderLayout() );
            ratePanel.add("West",new Label(BITRATE_STRING,Label.CENTER) );
            ratesComp = new Choice();
            setAvailableRates();
            ratesComp.addItemListener( new ItemListener() {
                  public void itemStateChanged(ItemEvent e) {
                       String s=(String)e.getItem();
                       if (!owner.isStarted) {
                           bitrate=Integer.parseInt(s.substring(0,3).trim());
                           owner.setBitrate(bitrate);
                       } else {
                           Choice in_choice=(Choice)e.getItemSelectable();
                           in_choice.select( bitrate+" Kbits/sec " );
                       }

                  }
	    } );
	    ratePanel.add("East",ratesComp );

            componentPanel.add(ratePanel);
            componentPanel.add(layerPanel);
//            componentPanel.add(freqPanel); //sbd:disabled
            componentPanel.add(cnlPanel);
            componentPanel.add(errPanel);
            componentPanel.add(orgPanel);
            componentPanel.add(cpyPanel);
            component=componentPanel;

	 }

         return (Component)component;
     }

     Choice ratesComp;
    void setAvailableRates() {
        if (layer==1) {
            if (nChannels==1) {
                availableRates=owner.layer1BitRate_mono;
            } else {
                availableRates=owner.layer1BitRate_stereo;
            }
        } else {
            if (nChannels==1) {
                availableRates=owner.layer2BitRate_mono;
            } else {
                availableRates=owner.layer2BitRate_stereo;
            }
        }

        if (ratesComp!=null) {
            bitrate=128*nChannels;
            owner.setBitrate(bitrate);

            ratesComp.removeAll();
            for (int i=0;i<availableRates.length;i++) {
                ratesComp.add(availableRates[i]+" Kbits/sec ");
            }
            ratesComp.select( bitrate+" Kbits/sec " );

        }

    }

    public MpegAudioControlAdapter(NativeEncoder new_Owner, int new_Layer, int new_samplingFrequency,
           int new_nChannels,int new_bitrate,int new_copyright,int new_original,int new_error_protect, int new_joint_stereo) {

           owner=new_Owner;
           layer=new_Layer;
           samplingFrequency=new_samplingFrequency;
           nChannels=new_nChannels;
           bitrate=new_bitrate;
           copyright=new_copyright;
           original=new_original;
           error_protection=new_error_protect;
           joint_stereo=new_joint_stereo;
    }

}


