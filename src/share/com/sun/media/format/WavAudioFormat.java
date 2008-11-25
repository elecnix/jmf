/*
 * @(#)WavAudioFormat.java	1.16 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.format;

import javax.media.Format;
import java.util.Hashtable;

public class WavAudioFormat extends javax.media.format.AudioFormat {

    public final static int WAVE_FORMAT_PCM = 0x0001;
    public final static int WAVE_FORMAT_ADPCM = 0x0002;
    public final static int WAVE_FORMAT_ALAW = 0x0006;
    public final static int WAVE_FORMAT_MULAW = 0x0007;
    public final static int WAVE_FORMAT_OKI_ADPCM = 0x0010;
    public final static int WAVE_FORMAT_DIGISTD = 0x0015;
    public final static int WAVE_FORMAT_DIGIFIX = 0x0016;
    public final static int WAVE_FORMAT_GSM610 = 0x0031;
    public final static int WAVE_IBM_FORMAT_MULAW = 0x0101;
    public final static int WAVE_IBM_FORMAT_ALAW = 0x0102;
    public final static int WAVE_IBM_FORMAT_ADPCM = 0x0103;
    public final static int WAVE_FORMAT_DVI_ADPCM = 0x0011;
    public final static int WAVE_FORMAT_SX7383 = 0x1C07;
    public final static int WAVE_FORMAT_DSPGROUP_TRUESPEECH = 0x0022;

    public final static int WAVE_FORMAT_MSNAUDIO = 0x32;
    public final static int WAVE_FORMAT_MSG723 = 0x42;
    public final static int WAVE_FORMAT_MPEG_LAYER3 = 0x55;
    public final static int WAVE_FORMAT_VOXWARE_AC8 = 0x70;
    public final static int WAVE_FORMAT_VOXWARE_AC10 = 0x71;
    public final static int WAVE_FORMAT_VOXWARE_AC16 = 0x72;
    public final static int WAVE_FORMAT_VOXWARE_AC20 = 0x73;
    public final static int WAVE_FORMAT_VOXWARE_METAVOICE = 0x74;
    public final static int WAVE_FORMAT_VOXWARE_METASOUND = 0x75;
    public final static int WAVE_FORMAT_VOXWARE_RT29H = 0x76;
    public final static int WAVE_FORMAT_VOXWARE_VR12 = 0x77;
    public final static int WAVE_FORMAT_VOXWARE_VR18 = 0x78;
    public final static int WAVE_FORMAT_VOXWARE_TQ40 = 0x79;
    public final static int WAVE_FORMAT_VOXWARE_TQ60 = 0x81;
    public final static int WAVE_FORMAT_MSRT24 = 0x82;

    protected byte [] codecSpecificHeader = null;
    private int averageBytesPerSecond = NOT_SPECIFIED;


    public final static Hashtable formatMapper = new Hashtable();
    public final static Hashtable reverseFormatMapper = new Hashtable();
    static {
	formatMapper.put(new Integer(WAVE_FORMAT_PCM), LINEAR);
	formatMapper.put(new Integer(WAVE_FORMAT_ADPCM), MSADPCM);
	//	formatMapper.put(new Integer(WAVE_FORMAT_DVI_ADPCM), DVI);
	formatMapper.put(new Integer(WAVE_FORMAT_DVI_ADPCM), IMA4_MS);
	formatMapper.put(new Integer(WAVE_FORMAT_ALAW), ALAW);
	formatMapper.put(new Integer(WAVE_FORMAT_MULAW), ULAW);
	formatMapper.put(new Integer(WAVE_FORMAT_GSM610), GSM_MS);

	formatMapper.put(new Integer(WAVE_FORMAT_DSPGROUP_TRUESPEECH),
		       TRUESPEECH);

	formatMapper.put(new Integer(WAVE_FORMAT_MSNAUDIO),
		       MSNAUDIO);

	formatMapper.put(new Integer(WAVE_FORMAT_VOXWARE_AC8),
		       VOXWAREAC8);

	formatMapper.put(new Integer(WAVE_FORMAT_VOXWARE_AC10),
		       VOXWAREAC10);

	formatMapper.put(new Integer(WAVE_FORMAT_VOXWARE_AC16),
		       VOXWAREAC16);

	formatMapper.put(new Integer(WAVE_FORMAT_VOXWARE_AC20),
		       VOXWAREAC20);

	formatMapper.put(new Integer(WAVE_FORMAT_VOXWARE_METAVOICE),
		       VOXWAREMETAVOICE);

	formatMapper.put(new Integer(WAVE_FORMAT_VOXWARE_METASOUND),
		       VOXWAREMETASOUND);


	formatMapper.put(new Integer(WAVE_FORMAT_VOXWARE_RT29H),
		       VOXWARERT29H);

	formatMapper.put(new Integer(WAVE_FORMAT_VOXWARE_VR12),
		       VOXWAREVR12);

	formatMapper.put(new Integer(WAVE_FORMAT_VOXWARE_VR18),
		       VOXWAREVR18);

	formatMapper.put(new Integer(WAVE_FORMAT_VOXWARE_TQ40),
		       VOXWARETQ40);

	formatMapper.put(new Integer(WAVE_FORMAT_VOXWARE_TQ60),
		       VOXWARETQ60);


	formatMapper.put(new Integer(WAVE_FORMAT_MPEG_LAYER3),
		       MPEGLAYER3);

	formatMapper.put(new Integer(WAVE_FORMAT_MSRT24),
		       MSRT24);

	// Reverse mappings: These will be used by FileWriters
	reverseFormatMapper.put(LINEAR.toLowerCase(), new Integer(WAVE_FORMAT_PCM));
	reverseFormatMapper.put(MSADPCM.toLowerCase(), new Integer(WAVE_FORMAT_ADPCM));
	reverseFormatMapper.put(IMA4_MS.toLowerCase(), new Integer(WAVE_FORMAT_DVI_ADPCM));
	reverseFormatMapper.put(ALAW.toLowerCase(), new Integer(WAVE_FORMAT_ALAW));
	reverseFormatMapper.put(ULAW.toLowerCase(), new Integer(WAVE_FORMAT_MULAW));
	reverseFormatMapper.put(GSM_MS.toLowerCase(), new Integer(WAVE_FORMAT_GSM610));
	reverseFormatMapper.put(TRUESPEECH.toLowerCase(), new Integer(WAVE_FORMAT_DSPGROUP_TRUESPEECH));
	reverseFormatMapper.put(MSNAUDIO.toLowerCase(), new Integer(WAVE_FORMAT_MSNAUDIO));
	reverseFormatMapper.put(VOXWAREAC8.toLowerCase(), new Integer(WAVE_FORMAT_VOXWARE_AC8));
	reverseFormatMapper.put(VOXWAREAC10.toLowerCase(), new Integer(WAVE_FORMAT_VOXWARE_AC10));
	reverseFormatMapper.put(VOXWAREAC16.toLowerCase(), new Integer(WAVE_FORMAT_VOXWARE_AC16));
	reverseFormatMapper.put(VOXWAREAC20.toLowerCase(), new Integer(WAVE_FORMAT_VOXWARE_AC20));
	reverseFormatMapper.put(VOXWAREMETAVOICE.toLowerCase(), new Integer(WAVE_FORMAT_VOXWARE_METAVOICE));
	reverseFormatMapper.put(VOXWAREMETASOUND.toLowerCase(), new Integer(WAVE_FORMAT_VOXWARE_METASOUND));
	reverseFormatMapper.put(VOXWARERT29H.toLowerCase(), new Integer(WAVE_FORMAT_VOXWARE_RT29H));
	reverseFormatMapper.put(VOXWAREVR12.toLowerCase(), new Integer(WAVE_FORMAT_VOXWARE_VR12));
	reverseFormatMapper.put(VOXWAREVR18.toLowerCase(), new Integer(WAVE_FORMAT_VOXWARE_VR18));
	reverseFormatMapper.put(VOXWARETQ40.toLowerCase(), new Integer(WAVE_FORMAT_VOXWARE_TQ40));
	reverseFormatMapper.put(VOXWARETQ60.toLowerCase(), new Integer(WAVE_FORMAT_VOXWARE_TQ60));
	reverseFormatMapper.put(MPEGLAYER3.toLowerCase(), new Integer(WAVE_FORMAT_MPEG_LAYER3));
	reverseFormatMapper.put(MSRT24.toLowerCase(), new Integer(WAVE_FORMAT_MSRT24));


    }

    public WavAudioFormat(String encoding) {
	super(encoding);
    }

    public WavAudioFormat(String encoding, double sampleRate,
			  int sampleSizeInBits, int channels,
			  int frameSizeInBits, int averageBytesPerSecond,
			  byte [] codecSpecificHeader) {
	super(encoding, sampleRate, sampleSizeInBits, channels);
	this.codecSpecificHeader = codecSpecificHeader;
	this.averageBytesPerSecond = averageBytesPerSecond;
	this.frameRate = averageBytesPerSecond;
	this.frameSizeInBits = frameSizeInBits;
    }

    /**
     * Construct a WavAudioFormat with all the given parameters.
     * The bit mask identifies which parameters are actually valid.
     */
    public WavAudioFormat(String encoding, double sampleRate,
		       int sampleSizeInBits, int channels,
		       int frameSizeInBits, int averageBytesPerSecond,
		       int endian, int signed, 
		       float frameRate, Class dataType,
		       byte [] codecSpecificHeader) {
	super(encoding, sampleRate, sampleSizeInBits, channels, endian, signed,
	      frameSizeInBits, frameRate, dataType);
	this.codecSpecificHeader = codecSpecificHeader;
	this.averageBytesPerSecond = averageBytesPerSecond;
	this.frameRate = averageBytesPerSecond;
    }


    /**
     * Returns the average bytes per second.
     */
    public int getAverageBytesPerSecond() {
	return averageBytesPerSecond;
    }

    /**
     * Returns the codec specific header bytes available in the wav header.
     */
    public byte [] getCodecSpecificHeader() {
	return codecSpecificHeader;
    }

    /**
     * @return True if the given format is the same as this one.
     */
    public boolean equals(Object format) {
	if (format instanceof WavAudioFormat) {
	    WavAudioFormat other = (WavAudioFormat) format;
	    
	    if (!super.equals(format))
		return false;

	    if (codecSpecificHeader == null && other.codecSpecificHeader == null)
		return true;

	    if (codecSpecificHeader == null || other.codecSpecificHeader == null)
		return false;

	    if (codecSpecificHeader.length != other.codecSpecificHeader.length)
		return false;

	    for (int i = 0; i < codecSpecificHeader.length; i++) {
		if ( codecSpecificHeader[i] != other.codecSpecificHeader[i] )
		    return false;
	    }
	    return true;
	}
	return false;
    }

    /**
     * Test to see if the given format matches this format.
     * Matches compares attributes that are defined and ignore attributes
     * that are unspecified.
     * Two formats do not have to be of the same class to be considered
     * a match.  Say "A" are "B" are the two classes.  If "A" derives "B"
     * or "B" derives "A", then a match is possible (after comparing
     * individual attributes.  Otherwise, matches fails.  This is to
     * prevent matching VideoFormat and AudioFormat, for example.
     * @return true if the given format matches this one.
     */
    public boolean matches(Format format) {
	if (!super.matches(format))
	    return false;
	if (!(format instanceof WavAudioFormat))
	    return true;

	WavAudioFormat other = (WavAudioFormat) format;
	
	if ( (codecSpecificHeader == null) ||
	     other.codecSpecificHeader == null )
	    return true;
	
	if (codecSpecificHeader.length != other.codecSpecificHeader.length)
	    return false;

	for (int i = 0; i < codecSpecificHeader.length; i++) {
	    if ( codecSpecificHeader[i] != other.codecSpecificHeader[i] )
		return false;
	}
	return true;
    }

    /**
     * Find the common attributes of two matching formats.
     * If the given format does not match this one, the result is
     * undefined.  Otherwise, it returns a format object 
     * with its attributes set to the common attributes of the two. 
     * @return a format object with its attributes set to the common
     * attributes of the two.
     * @see matches
     */
    public Format intersects(Format format) {
	Format fmt;
	if ((fmt = super.intersects(format)) == null)
	    return null;
	if (!(format instanceof WavAudioFormat))
	    return fmt;
	WavAudioFormat other = (WavAudioFormat)format;
	WavAudioFormat res = (WavAudioFormat)fmt;
	res.codecSpecificHeader = (codecSpecificHeader != null ?
				   codecSpecificHeader : other.codecSpecificHeader);
	return res;
    }

    /**
     * Return a clone of this format.
     */
    public Object clone() {
	WavAudioFormat f = new WavAudioFormat(encoding);
	f.copy(this);
	return f;
    }

    /**
     * Copy the attributes from the given object.
     */
    protected void copy(Format f) {
	super.copy(f);
	WavAudioFormat other = (WavAudioFormat) f;

	averageBytesPerSecond = other.averageBytesPerSecond;
	codecSpecificHeader = other.codecSpecificHeader;
    }
}
