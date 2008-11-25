/*
 * @(#)MpegAudioControl.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

/**
 * This interface is a Control for specifying the parameters for MPEG audio.
 * @since JMF 2.0
 */
public interface MpegAudioControl extends javax.media.Control {

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
    public int getSupportedAudioLayers();

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
    public int getSupportedSamplingRates();

    /**
     * Indicates support for single channel layout
     */
    public final static int SINGLE_CHANNEL  	        = 1<<0;

    /**
     * Indicates support for two channels stereo layout
     */
    public final static int TWO_CHANNELS_STEREO         = 1<<1;

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
    public int getSupportedChannelLayouts();

    /**
     * Returns the low freuqency channel support capability.<br>
     */
    public boolean isLowFrequencyChannelSupported();

    /**
     * Returns the multilingual mode support capability.<br>
     */
    public boolean isMultilingualModeSupported();

    /**
     * Controls the MPEG Audio Layer.<br>
     * @return the layer which was actually set.
     */
    public int setAudioLayer(int audioLayer);

    /**
     * Returns the current MPEG Audio Layer.<br>
     */
    public int getAudioLayer();

    /**
     * Controls the MPEG Audio channel layout.<br>
     * @return the channel layout which was actually set.
     */
    public int setChannelLayout(int channelLayout);

    /**
     * Returns the current MPEG Audio channel layout.<br>
     */
    public int getChannelLayout();

    /**
     * Controls the Low Frequency Channel mode.
     * "true" is on; "false" is off. <br>
     * @return true if Low Frequency Channel mode is actually turned on.
     */
    public boolean setLowFrequencyChannel(boolean on);

    /**
     * Returns true if Low Frequency Channel mode is turned on.
     */
    public boolean getLowFrequencyChannel();

    /**
     * Controls the Multilingual mode.
     * "true" is on; "false" is off. <br>
     * @return true if Multilingual mode is actually turned on.
     */
    public boolean setMultilingualMode(boolean on);

    /**
     * Returns true if Multilingual mode is turned on.
     */
    public boolean getMultilingualMode();

}
