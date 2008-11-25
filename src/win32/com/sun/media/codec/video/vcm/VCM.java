/*
 * @(#)VCM.java	1.10 03/04/24
 *
 * Copyright (c) 1996-2003 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.video.vcm;

import com.sun.media.vfw.BitMapInfo;

/**
 * VCM wrapper for windows.
 */
class VCM {

    static final int ICMODE_COMPRESS = 1;
    static final int ICMODE_DECOMPRESS = 2;
    static final int ICDECOMPRESS_HURRYUP = 4;
    static final int ICDECOMPRESS_NOTKEYFRAME = 8;
    static final int ICDECOMPRESS_PREROLL = 16;
    static final int ICCOMPRESS_KEYFRAME = 1;

    static final int VIDCF_QUALITY  = 1;
    static final int VIDCF_CRUNCH   = 2;
    static final int VIDCF_TEMPORAL = 4;
    static final int VIDCF_FASTC    = 32;

    static final int AVIIF_KEYFRAME = 16;
    
    
    /**
     * Returns a Handle to a compressor/decompressor that
     * accepts the specified input format and if non-null, the specified
     * output format.
     */
    native static int icLocate(String fccType, String fccHandler,
			       BitMapInfo biIn, BitMapInfo biOut,
			       int icMode);

    /**
     * Returns a handle to the specified compressor/decompressor.
     */
    native static int icOpen(String fccType, String fccHandler,
			     int icMode);

    /**
     * Closes the driver with the specified handle.
     */
    native static boolean icClose(int handle);

    /**
     * Get Info about a driver specified by handle.
     * Returns true if succesful.
     */
    native static boolean icGetInfo(int handle, ICInfo info);

    /**
     * Get Info about a handler with specified fourcc.
     */
    native static boolean icInfo(String fccType, String fccHandler, ICInfo info);

    /**
     * Get info about the handler with an index of enum.
     */
    native static boolean icInfoEnum(String fccType, int enum, ICInfo info);

    /**
     * Prepare for decompression.
     */
    native static boolean icDecompressBegin(int handle,
					    BitMapInfo biIn, BitMapInfo biOut);

    /**
     * Decompress the data.
     */
    native static int icDecompress(int handle, long flags,
				   BitMapInfo biIn,
				   Object dataIn, long inBytes,
				   BitMapInfo biOut,
				   Object dataOut, long outBytes,
				   int outType);

    /**
     * End the decompression sequence. Should be done when resetting or
     * repositioning.
     */
    native static boolean icDecompressEnd(int handle);

    native static boolean icDecompressQuery(int handle, BitMapInfo biIn,
					    BitMapInfo biOut);

    /**
     * Returns the output format in biOut. Input format needs to be specified
     * in biIn.
     */
    native static boolean icDecompressGetFormat(int handle, BitMapInfo biIn,
						BitMapInfo biOut);


    native static boolean icCompressBegin(int handle, BitMapInfo biIn,
					  BitMapInfo biOut);

    native static int     icCompress(int handle,
				     int inFlags,
				     BitMapInfo biOut,
				     Object dataOut,
				     BitMapInfo biIn,
				     Object dataIn,
				     int [] ckid,
				     int [] outFlags,
				     int frameNo,
				     int reqFrameSize,
				     int quality,
				     BitMapInfo biPrev,
				     Object dataPrev);

    native static boolean icCompressEnd(int handle);

    native static int     icCompressGetFormat(int handle,
					      BitMapInfo biIn,
					      BitMapInfo biOut);
    
    native static int     icCompressGetFormatSize(int handle,
						  BitMapInfo biIn);

}
