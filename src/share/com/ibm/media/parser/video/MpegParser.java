/*
 * @(#)MpegParser.java	1.50 01/02/27
 *
 *  Licensed Materials - Property of IBM
 *  "Restricted Materials of IBM"
 *  5648-B81
 *  (c) Copyright IBM Corporation 1998,1999 All Rights Reserved
 *  US Government Users Restricted Rights - Use, duplication or
 *  disclosure restricted by GSA ADP Schedule Contract with
 *  IBM Corporation.
 *
 */

package com.ibm.media.parser.video;

import java.security.*;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.io.OutputStream;       //EE temporary!
import java.io.FileOutputStream;   //EE temporary!

import java.io.IOException;
import java.awt.Dimension;
import javax.media.*;
import javax.media.protocol.DataSource;
import javax.media.protocol.SourceStream;
import javax.media.protocol.PullSourceStream;
import javax.media.protocol.Seekable;
import javax.media.protocol.Positionable;
import javax.media.Format;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import com.sun.media.format.WavAudioFormat;
import com.sun.media.parser.BasicPullParser;
import com.sun.media.parser.BasicTrack;
import com.sun.media.util.jdk12;
import com.sun.media.util.LoopThread;
import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;
import com.sun.media.CircularBuffer;
import com.sun.media.Log;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;


public class MpegParser extends BasicPullParser {

   /*
    * Temporary fields
    */
    /* temporary for saving in output files */
    boolean saveOutputFlag = false;  //true;
    String  AoutName = "Audio.mpg";
    String  VoutName = "Video.mpg";
    FileOutputStream aout;
    FileOutputStream vout;
    /* temporary for throwing all data away */
    boolean throwOutputFlag = false;  /// true;

   /*  As long as MPEG audio decoder doesn't exist, the audio track should be
    *  hidden inside the parser. That's mean that only video tracks are output
    *  of getTracks, and once in a while the audio buffer should be just flushed.
    *  There is a similar option to ignore the video data.
    */
    boolean hideAudioTracks = false;  //true;
    boolean hideVideoTracks = false;

   /*
    * Constants definitions for the MPEG-1 system layer splitter
    */
    static final long  NO_PTS_VAL                   = -3333333;   /* an arbitray value */
    private static final float EPSILON_PTS                  = 45000;      /* which is 0.5 sec in PTS units */
    private static final float EPSILON_NS                   = 500000000;  /* which is 0.5 sec in nanoseconds */

   /* The nature of MPEG video stream is that I frames usually apears every
    * 15 frames. In setPosition, we want to have an I frame before the time
    * being setted. This is why we will try to look for time stamp of 0.5 seconds
    * earlier (in common frame rates of 24-30 frames per second, 0.5 second will
    * probably include an I frame).
    */
    private static final long  PRE_ROLLING_DELTA_NS         = 500000000; /* which is 0.5 sec in nano-seconds */

    /* packet/track/stream type */
    private static final byte  UNKNOWN_TYPE                 = 0;
    private static final byte  AUDIO_TYPE                   = 1;
    private static final byte  VIDEO_TYPE                   = 2;
    private static final byte  SYS11172_TYPE                = 3;

    /* streams buffer size */
    private static final int   AUDIO_TRACK_BUF_SIZE        = 100000;
    private static final int   VIDEO_TRACK_BUF_SIZE        = 200000;

    /* codes definition */
    private static final int   PACK_START_CODE              = 0x000001BA;
    private static final int   SYSTEM_HEADER_START_CODE     = 0x000001BB;
    private static final int   PACKET_START_CODE_24         = 0x000001;    /* 24 bits of 0x000001 */
    private static final int   END_CODE                     = 0x000001B9;
    private static final int   MIN_STREAM_CODE              = 0x00BC;
    private static final int   MAX_STREAM_CODE              = 0x00FF;
    private static final int   PRIVATE_STREAM2_CODE         = 0x00BF;
    private static final int   VIDEO_PICTURE_START_CODE     = 0x00000100;
    private static final int   VIDEO_SEQUENCE_HEADER_CODE   = 0x000001B3;
    private static final int   VIDEO_GROUP_START_CODE       = 0x000001B8;

    /* streams IDs */
    private static final int   MAX_AUDIO_STREAMS            = 32;
    private static final int   MAX_VIDEO_STREAMS            = 16;
    private static final int   MAX_NUM_STREAMS              = 48;
    private static final int   MIN_AUDIO_ID                 =  0;
    private static final int   MAX_AUDIO_ID                 = 31;
    private static final int   MIN_VIDEO_ID                 = 32;
    private static final int   MAX_VIDEO_ID                 = 47;

    private static int MAX_TRACKS_SUPPORTED = MAX_NUM_STREAMS;


   /*
    * Fields
    */
    private static ContentDescriptor[] supportedFormat =
	new ContentDescriptor[] {    new ContentDescriptor("audio.mpeg"),
		     new ContentDescriptor(FileTypeDescriptor.MPEG),
		     new ContentDescriptor(FileTypeDescriptor.MPEG_AUDIO)};
    private PullSourceStream stream = null;
    private TrackList[] trackList = new TrackList[MAX_TRACKS_SUPPORTED];
    private Track[] tracks  = null;
    private Track[] videoTracks = null;      /// temporary, for hiding audio tracks
    private Track[] audioTracks = null;      /// temporary, for hiding video tracks
    private int videoCount = 0;              /// temporary, for hiding audio tracks
    private int audioCount = 0;              /// temporary, for hiding video tracks
    private int numSupportedTracks = 0;
    private int numTracks = 0;
    private int numPackets = 0;
    private int initTmpBufLen;               // a buffer used in the initation phase
    private byte[] initTmpStreamBuf;
    private byte streamType = UNKNOWN_TYPE;  // stream can be of: Unknown/Audio/Video/System11172 type
    private long streamContentLength = 0L;
    private SystemHeader sysHeader = new SystemHeader();


    private boolean sysHeaderSeen = false;
    boolean EOMflag = false;
    boolean parserErrorFlag = false;
    private boolean durationInitialized = false;
    private boolean sysPausedFlag = false;
    private boolean seekableStreamFlag = false;     // can be seeked at least to the beginning
    private boolean randomAccessStreamFlag = true;  // can be seeked to any location


    /* some JMFSecurity stuff */
    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege=false;
    private Method mSecurity[] = new Method[1];
    private Class clSecurity[] = new Class[1];
    private Object argsSecurity[][] = new Object[1][0];

    private long startLocation = 0;

    static {
	try {
	    jmfSecurity = JMFSecurityManager.getJMFSecurity();
	    securityPrivelege = true;
	} catch (SecurityException e) {
	}
    }


    /* Time is managed in few sets of variables (for different types of media),
     * each has it's own units:
     *        durationNs - in nanoseconds
     *        PTS        - is 33 bits of the presentation time stamp value.
     *                     Upon the MPEG standard, it probably should be unsigned,
     *                     but since there are movies with negetive values, we will
     *                     treat the time stamp as SIGNED !!
     *        AV...Ns    - time for Audio/Video only streams, in nanoseconds
     * Important: every public method who return time, calculate the time
     *            relative to the startPTS !
     */

    /** content duration in NS **/
    private Time durationNs = Duration.DURATION_UNKNOWN;
    /** last seek time **/
    private Time lastSetPositionTime = new Time(0L);
    /** first content PTS **/
    private long startPTS   = NO_PTS_VAL;
    /** last encountered  content PTS **/
    long currentPTS = NO_PTS_VAL;
    /** end of content PTS **/
    long endPTS     = NO_PTS_VAL;

    private long AVstartTimeNs = 0L;
    private long AVcurrentTimeNs = 0L;
    private long AVlastTimeNs = 0L;
    private long lastAudioNs = 0L;
    /// private long AVtotalBytesRead = 0L;  /* counter, in byte units */

    /* Sub-thread for filling the tracks inner buffers with data
     *  (only for system-type bitstream)
     */
    private MpegBufferThread mpThread = null;

    static int[][][] bitrates = {
	{       // MPEG-2.5
	    { -1 },                                                                       // reserved
	    { 0,  8, 16, 24, 32, 40, 48,  56,  64,  80,  96, 112, 128, 144, 160, -1 },   
            // Layer III
	    { 0,  8, 16, 24, 32, 40, 48,  56,  64,  80,  96, 112, 128, 144, 160, -1 },   
            // Layer II
	    { 0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, -1 }     
	    // Layer I
	},
	{       // reserved
	    { -1 }
	},
	{       // MPEG-2
	    { -1 },                                                                       // reserved
	    { 0,  8, 16, 24, 32, 40, 48,  56,  64,  80,  96, 112, 128, 144, 160, -1 },   
            // Layer III
	    { 0,  8, 16, 24, 32, 40, 48,  56,  64,  80,  96, 112, 128, 144, 160, -1 },   
            // Layer II
	    { 0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, -1 }     
	    // Layer I
	},
	{       // MPEG-1
	    { -1 },                                                                       // reserved
	    { 0, 32, 40, 48,  56,  64,  80,  96, 112, 128, 160, 192, 224, 256, 320, -1 
	    }, // Layer III
	    { 0, 32, 48, 56,  64,  80,  96, 112, 128, 160, 192, 224, 256, 320, 384, -1 
	    }, // Layer II
	    { 0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, -1 
	    }  // Layer I
	}
    };

    static int[][] samplerates = {
	{ 11025, 12000,  8000, -1 },    // MPEG-2.5
	{ -1 },                         // reserved
	{ 22050, 24000, 16000, -1 },    // MPEG-2
	{ 44100, 48000, 32000, -1 }     // MPEG-1
    };


    public void setSource(DataSource source)
        throws IOException, IncompatibleSourceException {

        super.setSource(source);
        stream = (PullSourceStream) streams[0];
        streamContentLength = stream.getContentLength();   // can be LENGTH_UNKNOWN
        seekableStreamFlag = (streams[0] instanceof Seekable);
	if (!seekableStreamFlag)
	    throw new IncompatibleSourceException("Mpeg Stream is not Seekable");
	randomAccessStreamFlag = seekableStreamFlag && ((Seekable) streams[0]).isRandomAccess();
    }


    public ContentDescriptor [] getSupportedInputContentDescriptors() {
        return supportedFormat;
    }


    public void start() throws IOException {
        super.start();
        sysPausedFlag = false;
	if (mpThread != null)
	    mpThread.start();
    }

    public void stop() {
        super.stop();
        sysPausedFlag = true;
	if (mpThread != null)
	    mpThread.pause();

	// Release any blocking readFrame.
	TrackList info;
        for (int i = 0; i < numTracks ; i++) {
            if (tracks[i] != null && tracks[i].isEnabled()) {
                info = ((MediaTrack)tracks[i]).getTrackInfo();
                info.releaseReadFrame();
            }
        }
    }


    public void close() {
	stop();
        flushInnerBuffers();
	super.close();
	if (mpThread != null)
	    mpThread.kill();
    }


   /*
    *  getTracks - read the MPEG system header, fill the inner buffers,
    *              configure the track layout, activate the inner thread
    */
    public Track[] getTracks()
        throws IOException, BadHeaderException {

        /* check if the tracks are already initialized */
        if (streamType == SYS11172_TYPE) {
            if ((hideAudioTracks) && (videoTracks != null)) {
                return videoTracks;
            }
            if ((hideVideoTracks) && (audioTracks != null)) {
                return audioTracks;
            }
        }
        if (tracks != null) {
            return tracks;
        }

        try {
            initTmpBufLen = (AUDIO_TRACK_BUF_SIZE < VIDEO_TRACK_BUF_SIZE) ?
                                   AUDIO_TRACK_BUF_SIZE : VIDEO_TRACK_BUF_SIZE;
            initTmpStreamBuf = new byte[initTmpBufLen];

            /* detect stream type: Audio only / Video only / interleaved (system) */
            initTmpBufLen = detectStreamType(initTmpStreamBuf);

            /* extract the tracks information */
            switch (streamType) {
                case AUDIO_TYPE :
                case VIDEO_TYPE :
                       initTrackAudioVideoOnly();
                       break;
                case SYS11172_TYPE :
                       initTrackSystemStream();
                       break;
                case UNKNOWN_TYPE :
                default :
                       throw new BadHeaderException("Couldn't detect stream type");
            }
            // System.out.println("Number of tracks: " + numTracks);

            initDuration();

            if (saveOutputFlag) {
                aout = new FileOutputStream(AoutName);
                vout = new FileOutputStream(VoutName);
            }


            /* activate the inner thread for filling the inner buffers */
            if (streamType == SYS11172_TYPE) {
		if ( /*securityPrivelege  && */ (jmfSecurity != null) ) {
		    String permission = null;
		    try {
			if (jmfSecurity.getName().startsWith("jmf-security")) {
			    permission = "thread";
			    jmfSecurity.requestPermission(mSecurity, clSecurity, argsSecurity,
							  JMFSecurity.THREAD);
			    mSecurity[0].invoke(clSecurity[0], argsSecurity[0]);
			    
			    permission = "thread group";
			    jmfSecurity.requestPermission(mSecurity, clSecurity, argsSecurity,
							  JMFSecurity.THREAD_GROUP);
			    mSecurity[0].invoke(clSecurity[0], argsSecurity[0]);
			} else if (jmfSecurity.getName().startsWith("internet")) {
			    PolicyEngine.checkPermission(PermissionID.THREAD);
			    PolicyEngine.assertPermission(PermissionID.THREAD);
			}
		    } catch (Throwable e) {
       			if (JMFSecurityManager.DEBUG) {
        	            System.err.println( "Unable to get " + permission +
	        		     	        " privilege  " + e);
                        }
			securityPrivelege = false;
			// TODO: Do the right thing if permissions cannot be obtained.
			// User should be notified via an event
		    }
		}

		if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
		    try {
			Constructor cons = jdk12CreateThreadAction.cons;
			mpThread = (MpegBufferThread) jdk12.doPrivM.invoke(
                                           jdk12.ac,
					   new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               MpegBufferThread.class,
                                           })});
		    } catch (Exception e) {
			System.err.println("MpegParser: Caught Exception " + e);
		    }
		} else {
		    mpThread = new MpegBufferThread();
		}
		if (mpThread != null) {
		    mpThread.setParser(this);
		    mpThread.start(); // I don't think you need permission for start
		}
                if (saveOutputFlag || throwOutputFlag) {
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {}
                }
            }

            /* return the resulting tracks */
            if (streamType == SYS11172_TYPE) {
                if (hideAudioTracks) {
                    return videoTracks;
                }
                if (hideVideoTracks) {
                    return audioTracks;
                }
            }
            return tracks;

        } catch (BadDataException e) {
            parserErrorFlag = true;
            throw new BadHeaderException("Bad data");
        } catch (BadHeaderException e) {
            parserErrorFlag = true;
            throw e;
        } catch (IOException e) {
            updateEOMState();
            EOMflag = true;
            throw e;
        }

    }


    private boolean isValidMp3Header(int code) {
	return 
	    (((code >>> 21) & 0x7ff) == 0x7ff && // sync
	    ((code >>> 19) & 0x3) != 1 && // version
	    ((code >>> 17) & 0x3) != 0 && // layer
	    ((code >>> 12) & 0xf) != 0 && // bit rate
	    ((code >>> 12) & 0xf) != 0xf && // bit rate
	    ((code >>> 10) & 0x3) != 0x3 && // sample rate
	    (code & 0x3) != 0x2); // emphasis
    }

   /*
    *  Try to detect whether the stream is MPEG 11172-1 system bitstream,
    *  or MPEG video only stream, or MPEg audio only stream
    */
    private int detectStreamType(byte[] streamBuf) throws IOException {

        int i=0, code, videoCount=0, audioCount=0;
        boolean found=false;

        /* Copy each byte from the bitsream into a temporary buffer.
         * If the stream is system, continue from where we got to in the
         * stream and don't use the bytes in the temporary buffer.
         * If the stream is raw MPEG audio/video - just copy the temporary
         * buffer into the single track buffer.
         */
        if (streamType != UNKNOWN_TYPE) {
            return 0;
        }

        try {
            /* try to look for generic codes */
            readBytes(stream, streamBuf, 4);
            while ((!found) && (i < streamBuf.length-5)) {
                code = ((streamBuf[i] & 0xFF) << 24)
                     | ((streamBuf[i+1] & 0xFF) << 16)
                     | ((streamBuf[i+2] & 0xFF) << 8)
                     | (streamBuf[i+3] & 0xFF);

                switch (code) {
                    case PACK_START_CODE :
                            /* check what happen right after the pack header end */
                            /* byte 0010XXX1 */
                            i++;
                            readBytes(stream, streamBuf, i+3, 1);   /* read the next byte */
                            if ((streamBuf[i+3] & (byte)0xF1) == (byte)0x21) {
                                streamType = SYS11172_TYPE;
                                found = true;
                            }


                            continue;   /* not a "real" pack code - skip on reading next byte */

                     case VIDEO_SEQUENCE_HEADER_CODE :
                            if (i == 0) {      /* first code on the bitstream */
                                streamType = VIDEO_TYPE;
                                found = true;
                            }
                    case VIDEO_PICTURE_START_CODE :
                    case VIDEO_GROUP_START_CODE :
                            videoCount++;
                            break;

                    default :
                            /* check if audio frame sync word and legal layer code */
                            if ( ((code & 0xFFF00000) == 0xFFF00000) &&
                                 ((code & 0x00060000) != 0x00000000) &&
				 isValidMp3Header(code) ) {
                                audioCount++;
//                                 if (i == 0) {  /* first code on the bitstream */
                                   streamType = AUDIO_TYPE;
                                   found = true;
//                                 }
				   startLocation = i;
                            }
                            /* otherwise, do nothing */
                            break;
                }

                i++;
                readBytes(stream, streamBuf, i+3, 1);   /* read the next byte */
            }
        } catch (IOException e) {
            /* apply some very very simple logic */
            if (streamType == UNKNOWN_TYPE) {
                if (videoCount > 0) {
                    streamType = VIDEO_TYPE;
                }
                else if (audioCount > 0) {
                    streamType = AUDIO_TYPE;
                }
            }
            updateEOMState();
            EOMflag = true;
            throw e;
        }

        /* apply some simple logic */
        if (streamType == UNKNOWN_TYPE) {
            if (videoCount > 4) {
                streamType = VIDEO_TYPE;
            }
            else if (audioCount > 20) {
                streamType = AUDIO_TYPE;
            }
        }
	
	// I think this applies only to Audio
	if (seekableStreamFlag && (streamType == AUDIO_TYPE)) {
	    int duration = -1;
	    
	    Seekable s = (Seekable) stream;
	    long currentPos = s.tell();
	    
	    // s.seek(0);
	    s.seek(startLocation);

	    int frameHeader = readInt(stream);
	    
	    int h_id         = (frameHeader>>>19) & 0x03; // MPEG version
	    
	    int h_layer      = (frameHeader>>>17) & 0x03; // Audio Layer
	    
	    int h_bitrate    = (frameHeader>>>12) & 0x0f;
	    
	    int h_samplerate = (frameHeader>>>10) & 0x03;
	    
	    int h_padding    = (frameHeader>>> 9) & 0x01;
	    
	    int h_mode       = (frameHeader>>> 6) & 0x03; // Channel mode
	    
	    int bitrate = bitrates[h_id][h_layer][h_bitrate];
	    // TODO: check if streamContentLength is not unknown/unbounded
	    // 		duration = (int)(streamContentLength/(bitrate * 125));
	    
	    // Look for Xing VBR header
	    int offset = (((h_id & 1) == 1) ? 
                                  ((h_mode != 3) ? 
                                      (32+4) : 
                                      (17+4))
				  : ((h_mode != 3) ? 
                                        (17+4) :
                                        ( 9+4)));
	    s.seek(offset);
	    String hdr = readString(stream);
	    if (hdr.equals("Xing")) {
		int flags = readInt(stream);
		int frames = readInt(stream);
		int bytes = readInt(stream);
		int samplerate = samplerates[h_id][h_samplerate];
		int frameSize = 144000 * bitrate / samplerate + h_padding;
		duration = (frameSize * frames) / (bitrate * 125); // Fixed time per frame
		if (duration > 0) {
		    durationInitialized = true;
		    durationNs = new Time((double) duration);
		}
	    }
	    s.seek(currentPos);
	}
	return (i+4);
    }


    private void initTrackAudioVideoOnly()
                   throws IOException, BadHeaderException, BadDataException {
        TrackList trackInfo;
        int possibleLen, itmp=0;

        numTracks = 1;
        tracks = new Track[1];
        trackList[0] = new TrackList();

        /* fill the whole buffer with data */
        possibleLen = (streamType == AUDIO_TYPE) ? AUDIO_TRACK_BUF_SIZE :
                                                        VIDEO_TRACK_BUF_SIZE;
        if (initTmpBufLen < possibleLen) {
            if (possibleLen > initTmpStreamBuf.length) {  /* enlarge buffer if needed */
                byte[] tmpBuf2 = new byte[possibleLen];
                System.arraycopy (initTmpStreamBuf, 0, tmpBuf2, 0, initTmpBufLen);
                initTmpStreamBuf = tmpBuf2;
            }
            try {
                itmp = readBytes(stream, initTmpStreamBuf, initTmpBufLen, (possibleLen - initTmpBufLen));
            } catch (IOException e) {
                updateEOMState();
                EOMflag = true;
            }
            initTmpBufLen += itmp;
        }
        trackInfo = trackList[0];

        do {                          /* look for track's embeded info */
            extractStreamInfo(initTmpStreamBuf, 0, initTmpBufLen, true);
            if (trackInfo.infoFlag) {
                break;    /* the info was found */
            }
            /* else - read more data, throw the existing data */
            try {
                itmp = readBytes(stream, initTmpStreamBuf, possibleLen);
            } catch (IOException e) {
                updateEOMState();
                EOMflag = true;
                break;
            }
            initTmpBufLen = itmp;
        } while (trackInfo.infoFlag == false) ;

        /* it's a real problem if we didn't detect any valid info till now */
        if (trackInfo.infoFlag == false) {   /* not a legal stream */
            numTracks = 0;
            tracks = null;
            throw new BadHeaderException("Sorry, No tracks found");
        }

        /* now, if seekable, move to the beginning of the file */
        /// if (seekable....) {
             ((Seekable)stream).seek(0L);
             initTmpBufLen = 0;
             EOMflag = false;
        /// } else {
        /* cannot jump to file beginning, just 'remember' the data exist in the buffer
         * (maybe we lose some, if we had more than one loop)
         */
        ///     ............
        /// }
    }


    private void initTrackSystemStream()
        throws IOException, BadHeaderException, BadDataException {

        int i;
        tracks  = new Track[MAX_TRACKS_SUPPORTED];   /* temporary allocation */
        for (i = 0 ; i < tracks.length ; i++) {
            tracks[i] = null;
        }
        for (i = 0 ; i < trackList.length ; i++) {
            trackList[i] = null;
        }

        /* read first chunks of data */
        mpegSystemParseBitstream(false, 0L, true, NO_PTS_VAL);

        /* it's a real problem if we didn't detect any existing track till now */
        if (numTracks == 0) {
            throw new BadHeaderException("Sorry, No tracks found");
        }

        /* now create a correct length array of tracks */
        {
            Track[] tmpTracks = new Track[numTracks];
            for (i = 0 ; i < numTracks ; i++) {
                tmpTracks[i] = tracks[i];   /* copy pointer */
            }
            tracks = tmpTracks;
        }

        /* reorgenize the order of the tracks in the tracks array */
        if (hideAudioTracks) {
            TrackList trackInfo;
            int v;
            /* count video tracks first */
            for (i = 0 ; i < numTracks ; i++) {
                if (tracks[i] != null) {
                    trackInfo = ((MediaTrack)tracks[i]).getTrackInfo();
                    if (trackInfo.trackType == VIDEO_TYPE) {
                        videoCount++;
                    }
                }
            }
            if (videoCount == 0) {   /* no video tracks */
                throw new BadHeaderException("Sorry, No video tracks found");
            }
            videoTracks = new Track[videoCount];
            /* copy pointers to video tracks only */
            for (i=0, v=0 ; i < numTracks ; i++) {
                if (tracks[i] != null) {
                    trackInfo = ((MediaTrack)tracks[i]).getTrackInfo();
                    if (trackInfo.trackType == VIDEO_TYPE) {
                        videoTracks[v] = tracks[i];
                    }
                }
            }
        }
        if (hideVideoTracks) {
            TrackList trackInfo;
            int v;
            /* count video tracks first */
            for (i = 0 ; i < numTracks ; i++) {
                if (tracks[i] != null) {
                    trackInfo = ((MediaTrack)tracks[i]).getTrackInfo();
                    if (trackInfo.trackType == AUDIO_TYPE) {
                        audioCount++;
                    }
                }
            }
            if (audioCount == 0) {   /* no audio tracks */
                throw new BadHeaderException("Sorry, No video tracks found");
            }
            audioTracks = new Track[audioCount];
            /* copy pointers to audio tracks only */
            for (i=0, v=0 ; i < numTracks ; i++) {
                if (tracks[i] != null) {
                    trackInfo = ((MediaTrack)tracks[i]).getTrackInfo();
                    if (trackInfo.trackType == AUDIO_TYPE) {
                        audioTracks[v] = tracks[i];
                    }
                }
            }
        }
    }


    /**
     * Returns a descriptive name for the plug-in.
     * This is a user readable string.
     */
    public String getName() {
        return "Parser for MPEG-1 file format";
    }


    /*
     * some time 'macros'
     */
    private long convPTStoNanoseconds (long val) {
        return (val * 100000 / 9L);
    }


    private long convNanosecondsToPTS (long val) {
        return (val * 9 / 100000L);
    }


    private long convBytesToTimeAV(long bytes) {
        long time;

	if (trackList[0] == null)
	    return 0;

        if (streamType == AUDIO_TYPE) {
            if (((Audio)(trackList[0].media)).bitRate == 0) {
                time = 0L;
            } else {
                time = (bytes << 3) / ((Audio)(trackList[0].media)).bitRate;
                time *= 1000000L;              // for nanoseconds
            }
        } else {       /* VIDEO_TYPE */
            if (((Video)(trackList[0].media)).bitRate == 0) {
                time = 0L;
            } else {
                time = (bytes << 3) / ((Video)(trackList[0].media)).bitRate;
                time *= 1000000000L;           // for nanoseconds
            }
        }
        return time;
    }


    private long convTimeToBytesAV(long time) {
        long bytes;

        if (streamType == AUDIO_TYPE) {
            bytes = (time >> 3) * ((Audio)(trackList[0].media)).bitRate;
            bytes /= 1000000L;              // because of nanoseconds
        } else {       /* VIDEO_TYPE */
            bytes = (time >> 3) * ((Video)(trackList[0].media)).bitRate;
            bytes /= 1000000000L;           // because of nanoseconds
        }
        return bytes;
    }


    /*
     * Get the duration of the stream.
     * If the stream length is unknown - need to update this "on the fly"
     *    (during the first run only)
     */
    public Time getDuration() {

        if (durationInitialized) {
            return durationNs;
        }
        else {    // try to update the duration
            if (EOMflag) {
                // updateEOMState() is already called when EOM was detected

                durationInitialized = true ;
            }
            return durationNs;
       }
    }


    private void initDuration() {
        if (streamContentLength != SourceStream.LENGTH_UNKNOWN) {
            if (streamType == SYS11172_TYPE) {
                if (randomAccessStreamFlag) {
                    initDurationSystemSeekableRA();
                }
            } else {
                updateDurationAudioVideoOnly();
            }
        }
    }


    /* track information was extracted for sure before calling to this method */
    private void updateDurationAudioVideoOnly() {
	if (durationInitialized) // NEW
	    return;
        AVstartTimeNs = 0L;
        AVcurrentTimeNs = 0L;
        AVlastTimeNs = convBytesToTimeAV(streamContentLength);
        durationNs = new Time(AVlastTimeNs - AVstartTimeNs);
        durationInitialized = true ;
    }


    private void initDurationSystemSeekableRA() {
        long baseLocation=0L, ltmp;
        int saveNumPackets = numPackets;
        boolean saveEOMflag = EOMflag;

        baseLocation = ((Seekable)stream).tell();
        /* look for the base time */
        if (startPTS == NO_PTS_VAL) {
            EOMflag = false;
            ((Seekable)stream).seek(0L);
            try {
                mpegSystemParseBitstream(true, 64*1024L, false, NO_PTS_VAL);
            } catch (Exception e) {
            }
        }
        if (startPTS == NO_PTS_VAL) {
            startPTS = 0L;
        }

        /* look for the EOM time */
        if (endPTS == NO_PTS_VAL) {
            EOMflag = false;
            currentPTS = NO_PTS_VAL;
            ltmp = streamContentLength - 128*1024;
            if (ltmp < 0) {
                ltmp = 0;
            }
            ((Seekable)stream).seek(ltmp);
            try {
                mpegSystemParseBitstream(true, 128*1024L, false, NO_PTS_VAL);
            } catch (Exception e) {
            }
            endPTS = currentPTS;
        }
        if (endPTS == NO_PTS_VAL) {
            endPTS = startPTS;
        }

        /* calc the duration */
        ltmp = endPTS - startPTS;
        if (ltmp < 0) {   /* wrong values */
            ltmp = 0;
            parserErrorFlag = true;
        }
        durationNs = new Time(convPTStoNanoseconds(ltmp));
        lastSetPositionTime = new Time(convPTStoNanoseconds(startPTS));
        ((Seekable)stream).seek(baseLocation);
        EOMflag = saveEOMflag;
        numPackets = saveNumPackets;
        durationInitialized = true ;
    }


    /**
     * Generate the EOM buffer and add to the buffer Q. 
     */
    void updateTrackEOM () {
        for (int i = 0 ; i < trackList.length ; i++) {
	    if (trackList[i] != null)
		trackList[i].generateEOM();
        }
    }


   /*
    *  updateEOMState - update some veriables and flags in case of EOM detected
    */
    void updateEOMState () {
       /* there is a problem with using the getLocation(stream) method:
        * in case of non-Seekable stream, the BasicPullParser just
        * count the number of bytes, without considering skipping
        * (treatment there is mistaken!!), no reseting at EOM, etc...
        * That's why the following calculation will work properly for
        * non-Seekable streams IF AND ONLY IF there was no scroll-movement
        * on the first pass till the first EOM!!
        * For Seekable streams there is no problem.
        */

        if (! durationInitialized) {
            if (streamContentLength == SourceStream.LENGTH_UNKNOWN) {
                streamContentLength = getLocation(stream);
            }

            /* for System - both cases of not random-accessible or stream length unknown */
            if (streamType == SYS11172_TYPE) {
                if (startPTS == NO_PTS_VAL) {
                    startPTS = 0L;
                }
                if (endPTS == NO_PTS_VAL) {
                    endPTS = currentPTS;
                }
                if (endPTS == NO_PTS_VAL) {
                    endPTS = startPTS;
                }
                long ltmp = endPTS - startPTS;
                if (ltmp < 0) {   /* wrong values */
                    ltmp = 0;
                    parserErrorFlag = true;
                }
                durationNs = new Time(convPTStoNanoseconds(ltmp));
                durationInitialized = true ;
            } else {    /* for Audio/Video only */
                updateDurationAudioVideoOnly();
            }

            /* update the "global" duration */
            ////???? sendEvent (new DurationUpdateEvent(this, durationNs));
        }
    }


    /*
     * Get the current processed media time
     *
     * For System stream, it is NOT so simple:  this is the time of the data
     *     processed by the inner thread, but probably NOT the time of the data
     *     inputed to the decoders by readFrame()  !!!
     *     maybe we will try to calculate what's the situation in the inner buffers...
     * For Audio/Video only, it is the time of the last data that was read
     *     in readFrame()
     * For both, it is not known what is the situation in the codec's-plugin buffers...
     */
    public Time getMediaTime() {
        Time mtime;

        if (streamType == SYS11172_TYPE) {
            if (currentPTS == NO_PTS_VAL) {
                mtime = new Time(0L);
            } else {
                mtime = new Time(convPTStoNanoseconds(currentPTS - startPTS));
            }
        } else {   /* Audio / Viseo only */
            ///  AVcurrentTimeNs = convBytesToTimeAV(AVtotalBytesRead);
            AVcurrentTimeNs = convBytesToTimeAV(getLocation(stream));
            mtime = new Time(AVcurrentTimeNs);
        }
        return mtime;
    }


    /*
     * Seek for a specific time (or nearest) in the stream, and reset
     * the parser's buffers
     */
    public Time setPosition(Time where, int rounding) {
        Time newTime = null, preWhere;

        if ((! durationInitialized) || (durationNs == Duration.DURATION_UNKNOWN)) {
            return new Time(0L);
        }

        preWhere = new Time(where.getNanoseconds() - PRE_ROLLING_DELTA_NS);

	long newTimeNs;

        if (streamType == SYS11172_TYPE) {

            flushInnerBuffers();

            long preWherePTS, wherePTS, newPTS;
            preWherePTS = convNanosecondsToPTS(preWhere.getNanoseconds());
            preWherePTS += startPTS;   /* 'convert' to our PTS values */
            wherePTS    = convNanosecondsToPTS(where.getNanoseconds());
            wherePTS    += startPTS;   /* 'convert' to our PTS values */
            newPTS = setPositionSystemSeekableRA(preWherePTS, wherePTS);
                     /* newPTS is already in the 'outside world' values */
            newTimeNs = convPTStoNanoseconds(newPTS);
	    lastAudioNs = newTimeNs;
        } else {     /* Audio/Video only */
            newTimeNs = setPositionAudioVideoOnly(preWhere.getNanoseconds(),
                                                   where.getNanoseconds());
	    lastAudioNs = newTimeNs;
        }

        newTime = new Time(newTimeNs);

	// To guarantee that the position time is never the same.
	if (lastSetPositionTime.getNanoseconds() == newTimeNs)
	    newTimeNs++;
	lastSetPositionTime = new Time(newTimeNs);

        EOMflag = false;
        parserErrorFlag = false;

// System.out.println("Set position to: "+(float)where.getSeconds()+"  -->  "+(float)preWhere.getSeconds()+"  -->  "+(float)newTime.getSeconds());
// System.out.flush();

        return newTime;
    }


    private long setPositionAudioVideoOnly(long where, long origWhere) {
        long newTime, pos;

        if (origWhere <= AVstartTimeNs + EPSILON_NS) {
            newTime = AVstartTimeNs;
            ((Seekable)stream).seek(0L);
            /// AVtotalBytesRead = 0L;
        } else if (origWhere >= AVlastTimeNs - EPSILON_NS) {
            newTime = AVlastTimeNs - AVstartTimeNs;
            ((Seekable)stream).seek(streamContentLength);
            /// AVtotalBytesRead = streamContentLength;
        } else {
            newTime = where;
            pos = convTimeToBytesAV(where);
            ((Seekable)stream).seek(pos);
            /// AVtotalBytesRead = pos;
        }
        return newTime;
    }


    private long setPositionSystemSeekableRA(long wherePTS, long origWherePTS) {
        long newTime = NO_PTS_VAL;
        long lres = -1;
        long range, step, pos;
        long saveStartPTS = startPTS;
        boolean saveEOMflag = EOMflag;
        boolean zeroPosFlag = false;

        if ((endPTS == NO_PTS_VAL) || (startPTS == NO_PTS_VAL)) {
            newTime = 0L;
            ((Seekable)stream).seek(0L);
        } else if (origWherePTS <= startPTS + EPSILON_PTS) {
            newTime = 0L;
            ((Seekable)stream).seek(0L);
        } else if (origWherePTS >= endPTS - EPSILON_PTS) {
            newTime = endPTS - startPTS;
            ((Seekable)stream).seek(streamContentLength);
        } else if (endPTS - startPTS < EPSILON_PTS) {
            newTime = 0L;
            ((Seekable)stream).seek(0L);
        } else {
            /* try to guess the location */
            pos = (long)(streamContentLength *
                  ((wherePTS - startPTS) / ((float)(endPTS - startPTS))));
            step = 20 * 1024L;   /* arbitrary */
            pos -= step;
            if (pos < 0) {
                pos = 0;
            }
            range = streamContentLength - pos;   // first range is till the end of media
            while (true) {
                ((Seekable)stream).seek(pos);
                currentPTS = NO_PTS_VAL;
                startPTS = NO_PTS_VAL;
                EOMflag = false;
                try {
                    lres = mpegSystemParseBitstream(true, range, false, wherePTS);
                } catch (IOException e) {
                    lres = -2;
                    saveEOMflag = true;
                } catch (Exception e) {
                    lres = -1;
                }
                if (lres >= 0) {   /* PTS found */
                    newTime = currentPTS - saveStartPTS;
                    ((Seekable)stream).seek(lres);
                    break;
                } else if (lres == -2) {
                    newTime = endPTS - saveStartPTS;
                    ((Seekable)stream).seek(streamContentLength);
                    break;
                } else {    /* lres == -1 */
                    pos -= step;
                    if (pos <= 0) {
                        if (zeroPosFlag) {  /* couldn't find any. decide on 0L */
                           newTime = 0L;
                           ((Seekable)stream).seek(0L);
                           break;
                        }
                        pos = 0;
                        zeroPosFlag = true;   /* a flag to prevent loop forever */
                    }
                    range = 3 * step;
                }
            }  /* end of while() */
            startPTS = saveStartPTS;
            EOMflag = saveEOMflag;     // redandant, actually
        }

        return newTime;
    }


   /*
    *  parse the bitstream into the tracks inner buffers
    *  justLooking == false  --> regular parsing into the tracks buffers
    *  justLooking == true   --> only parse and update PTS params
    *                            (don't save in the inner buffers!)
    *  newPTS == NO_PTS_VAL  --> do not look for specific time stamp
    *  newPTS == xxx         --> look for the time stamp which is close to xxx
    *
    *  return value: meaningful only if (newPTS != NO_PTS_VAL).
    *                return the start location of the current pack in the stream
    *                or -1 if should look before. (-2: problem or eom)
    *       new fix: return the PTS prior to the newPTS, and not the first one after it.
    */
    long mpegSystemParseBitstream (boolean justLooking, long range, 
		boolean justEnough, long newPTS)
                throws IOException, BadHeaderException, BadDataException {
        byte bval;
        byte[] buf1 = new byte[1];
        int code = 0;
        boolean read4 = true, packFound = false;
        long baseLocation = getLocation(stream);
        long lastPacketLocation = baseLocation;
        long lastLastPacketLocation = baseLocation;
        long loc = baseLocation+4;
        long lastCurrentPTS = NO_PTS_VAL;
        long savePTS = NO_PTS_VAL;

        while ((!sysPausedFlag && !EOMflag) || justLooking || justEnough) {
            if (justEnough && !needingMore()) {
                break;    /* stop if we've gotten enough data */
            }
            if (justLooking) {
                if (getLocation(stream) - baseLocation > range) {
                    break;    /* stop if parsed more than range bytes */
                }
                if (newPTS != NO_PTS_VAL) {  /* check if PTS was found */
                    if (newPTS < startPTS) {
                        return (-1L);   /* should seek before this point */
                    }
                    if (newPTS <= currentPTS) {
                        if (newPTS == currentPTS) {
                            return lastPacketLocation;
                        } else {   /* < */
                            currentPTS = lastCurrentPTS;
                            return lastLastPacketLocation;
                        }
                    }
                }
            }

            if (read4) {    /* read 4 bytes of code */
                code = readInt(stream, true);
            } else {        /* read only the next byte */
                readBytes(stream, buf1, 1);
                code = ((code << 8) & 0xFFFFFF00) | (buf1[0] & 0x00FF);
            }

            switch (code) {
                case PACK_START_CODE :
                        parsePackHeader();
                        read4 = true;
                        packFound = true;
                        break;

                case SYSTEM_HEADER_START_CODE :
                        parseSystemHeader();
                        read4 = true;
                        break;

                case END_CODE :
                        EOMflag = true;
                        /// ???? if ((lastPTS == NO_PTS_VAL) && (newPTS == NO_PTS_VAL)) {
                        if (endPTS == NO_PTS_VAL) {  /// maybe update always if lastPTS wasn't accurate enough....
                            endPTS = currentPTS;
                        }
                        if ((!justLooking) || (newPTS != NO_PTS_VAL)) {
                            updateEOMState();
                        }
                        break;

                default :
                        /* packet start code (it's only 24 bits) or default (error) */
                        if ( ((code >> 8) == PACKET_START_CODE_24) &&
                             ((!justLooking) || (packFound & justLooking)) ) {
                            if (justLooking && (newPTS != NO_PTS_VAL)) {
                                loc = getLocation(stream);
                                savePTS = currentPTS;
                            }
                            bval = (byte)(code & 0x000000FF);
                            parsePacket(bval, justLooking);
                            read4 = true;

                            /* update for setPosition call */
                            if (justLooking && (newPTS != NO_PTS_VAL)) {
                                /* it seems there is no special need to initialize
                                 * the 'lastCurrentPTS' and the 'lastLastPacketLocation'
                                 */
                                if (savePTS != currentPTS) {  /* new PTS here */
                                    lastCurrentPTS = savePTS;
                                    lastLastPacketLocation = lastPacketLocation;
                                    lastPacketLocation = loc - 4;
                                }
                            }
                            break;
                        } else {       /* another code - shouldn't be */
                            read4 = false;
                            break;
                        }
            }
        }

        /* in general, can catch here BadDataException & BadHeaderException,
         * that may thrown because wrong start code (if read4 was false....)
         */

        return ((EOMflag) ? (-2L) : (-1L));
    }


   /*
    *  parse the pack header
    */
    private void parsePackHeader () throws IOException, BadDataException {
        byte[] buf1 = new byte[1];

        readBytes(stream, buf1, 1);
        if ((buf1[0] & (byte)0xF0) != (byte)0x20) {    /* check 0010xxxx */
            throw new BadDataException("invalid pack header");
        }

        if ((buf1[0] & (byte)0x01) != (byte)0x01) {    /* check marker bit #0 */
            throw new BadDataException("illegal marker bit");
        }

        /* skip mux_rate */
        skip(stream, 7);

        /* we decide that there is no point to extract the value of the SCR
         * (Systen Clock Reference) here, because there are movies in which
         * there isn't any match between the SCR time and the PTS time.
         * If we would extract it here, the code would be:
         *    long scr = ((long)(buf1[0] & 0x000E)) << 29;
         *    scr = ((scr << 31) >> 31);                 << make it signed num !? >>
         *    int itmp = readInt(stream, true);
         *    if ((itmp & 0x00010001) != 0x00010001) {   << check 2 marker bits on #16 and #0 >>
         *        throw new BadDataException("illegal marker bit");
         *    }
         *    int itmp2 = (itmp & 0xFFFE0000) >> 2;      << bits 29..15 >>
         *    scr |= ((long) (itmp2 & 0x3fffffff));
         *    scr |= (long)((itmp & 0x0000FFFE) >> 1);   << bits 14..0 >>
         *    if (startSCR == NO_PTS_VAL) {
         *        startSCR = scr;
         *    }
         *    currentSCR = scr;
         *    skip(stream, 3);
         */
    }


   /*
    *  parse the MPEG system header
    *
    *  According to the 11172-1 standard, all system headers in a specific
    *  bitstream, should be identical. The reality is different. That's why
    *  there is no point of really parsing the next system headers and comparing
    *  it (by equal()) to the first one...
    */
    private void parseSystemHeader () throws IOException, BadHeaderException {

        byte bval;
        byte[] buf1 = new byte[1];
        int  itmp, size, scale, streamID, i, len;
        short stmp;

        /* read header length */
        len = readShort(stream, true);

        if (sysHeaderSeen) {   /* not the first system header */
            skip(stream, len);
        } else {               /* first one - parse it */
            sysHeader.resetSystemHeader();
            sysHeader.headerLen = len;
            /* ...could check if there are enough bytes in the input... */
            itmp = readInt(stream, true);
            len -= 4;
            if ((itmp & 0x80000100) != 0x80000100) { // marker bits on #31 and #8
                throw new BadHeaderException("illegal marker bits in system header");
            }
            sysHeader.rateBound = (itmp & 0x7FFFFE00) >> 9;
            sysHeader.audioBound = (itmp & 0x000000FC) >> 2;
            sysHeader.fixedFlag = (itmp & 0x00000002) >> 1;
            sysHeader.CSPSFlag = itmp & 0x00000001;
            readBytes(stream, buf1, 1);
            bval = buf1[0];
            len -= 1;
            if ((bval & (byte)0x20) != (byte)0x20) { // check marker bits #5
                throw new BadHeaderException("illegal marker bits in system header");
            }
            sysHeader.audioLockFlag = (bval & 0x0080) >> 7;
            sysHeader.videoLockFlag = (bval & 0x0040) >> 6;
            sysHeader.videoBound = bval & 0x001F;
            readBytes(stream, buf1, 1);
            len -= 1;
            sysHeader.reserved = buf1[0];

            /* read streams STD info */
            while (len > 1) {
                readBytes(stream, buf1, 1);
                bval = buf1[0];
                len -= 1;
                if ((bval & (byte)0x80) != (byte)0x80)  // end of STD info
                    break;

                /* check if STD refers to all audio streams */
                if (bval == (byte)0xb8) {
                    stmp = readShort(stream, true);
                    len -= 2;
                    if ((stmp & 0x0000C000) != 0x0000C000) {
                        throw new BadHeaderException("illegal marker bits in system header");
                    }
                    size = stmp & 0x00001FFF;      /* in 128 byte units */
                    sysHeader.allAudioSTDFlag = true;
                    for (i = MIN_AUDIO_ID ; i <= MAX_AUDIO_ID ; i++) {
                       /* do not set the stream_flags[i] field, because
                        * info isn't track specific
                        */
                        sysHeader.STDBufBoundScale[i] = 0;
                        sysHeader.STDBufSizeBound[i] = size;
                    }
                }
                    /* check if STD refers to all video streams */
                else if (bval == (byte)0xb9) {
                    stmp = readShort(stream, true);
                    len -= 2;
                    if ((stmp & 0x0000C000) != 0x0000C000) {
                        throw new BadHeaderException("illegal marker bits in system header");
                    }
                    size = stmp & 0x00001FFF;   /* in 1024 byte units */
                    sysHeader.allVideoSTDFlag = true;
                    for (i = MIN_VIDEO_ID ; i <= MAX_VIDEO_ID ; i++) {
                       /* do not set the stream_flags[i] field, because
                        * info isn't track specific
                        */
                        sysHeader.STDBufBoundScale[i] = 1;
                        sysHeader.STDBufSizeBound[i] = size;
                    }
                }
                    /* STD information of specific stream/track */
                else {
                    if (((bval & 0x00FF) < MIN_STREAM_CODE)
                            || ((bval & 0x00FF) > MAX_STREAM_CODE)) {
                        throw new BadHeaderException("illegal track number in system header");
                    }
                    streamID = getStreamID(bval);
                    if ((streamID >= 0) && (streamID < MAX_NUM_STREAMS)) {
                        stmp = readShort(stream, true);
                        len -= 2;
                        if ((stmp & 0x0000C000) != 0x0000C000) {
                            throw new BadHeaderException("illegal marker bits in system header");
                        }
                        scale = (stmp & 0x00002000) >> 13;
                        size = stmp & 0x00001FFF;   /* in 1024 byte units */
                        sysHeader.streamFlags[streamID] = true;
                        sysHeader.STDBufBoundScale[streamID] = scale;
                        sysHeader.STDBufSizeBound[streamID] = size;
                    }
               }
            }

            if (len < 0) {
                throw new BadHeaderException("illegal system header");
            }
            if (len > 0) {
                skip(stream, len);
            }

            sysHeaderSeen = true;  /////

        }
    }


   /*
    *  parse a packet
    */
    private void parsePacket (byte bval, boolean justLooking)
                         throws IOException, BadDataException {
        int streamID, itmp, itmp2;
        int packetLen, count=0, dataSize;
        int STDBufSize=0;
        int STDBufScale=0;
        int numWrittenToTmpBuf = 0;
        byte[] tmpBuf = null;
        byte[] buf1 = new byte[1];
        long pts;
        TrackList trackInfo;

        /* identify the stream ID */
        if (((bval & 0x00FF) < MIN_STREAM_CODE) || ((bval & 0x00FF) > MAX_STREAM_CODE)) {
            throw new BadDataException("invalid stream(track) number");
        }
        streamID = getStreamID(bval);

        /* read packet length */
        packetLen = readShort(stream, true);
        buf1[0] = bval;
        /* could check here if there are are enough bytes in the input */

        if ((buf1[0] & 0x00FF) != PRIVATE_STREAM2_CODE) {
            /* skip stuffing bytes */
            do {
                readBytes(stream, buf1, 1);
                count++;
            } while (buf1[0] == (byte)0xFF);

            /* STD buf details (meanwhile do nothing with this info) */
            if ((buf1[0] & (byte)0xC0) == (byte)0x40) {
                STDBufScale = ((buf1[0] & 0x0020) >> 5);
                STDBufSize = (((int)buf1[0]) & 0x001F) << 8;
                readBytes(stream, buf1, 1);
                STDBufSize |= (int)buf1[0];
                readBytes(stream, buf1, 1);
                count += 2;
            }

            /* PTS - presentation time stamp (for now, do not try to attach to a spesific frame header) */
            if ((buf1[0] & (byte)0xE0) == (byte)0x20) {
                pts = ((long)(buf1[0] & 0x000E)) << 29;
                pts = ((pts << 31) >> 31);      /* make it signed num! (and lose the 33 bit) */
                if ((buf1[0] & (byte)0x01) != (byte)0x01) {    /* check marker bit #0 */
                    throw new BadDataException("illegal marker bit");
                }
                itmp = readInt(stream, true);
                count += 4;
                if ((itmp & 0x00010001) != 0x00010001) {      /* check 2 marker bits on #16 and #0 */
                    throw new BadDataException("illegal marker bit");
                }
                // for bits 29..15 - problem if msb==1:   pts |= (long)((itmp & 0xFFFE0000) >> 2);
                itmp2 = (itmp & 0xFFFE0000) >> 2;  /* bits 29..15 */
                pts |= ((long) (itmp2 & 0x3fffffff));  /* bits 29..15 */
                pts |= (long)((itmp & 0x0000FFFE) >> 1);  /* bits 14..0  */
                currentPTS = pts;
                if (startPTS == NO_PTS_VAL) {
                    startPTS = currentPTS;
                    if ((startPTS > 0) && (startPTS <= EPSILON_PTS)) {   /* actually zero time */
                        startPTS = 0L;
                    }
                }
                /* if there is a DTS - decoding time stamp - skip it */
                if ((buf1[0] & (byte)0xF0) == (byte)0x30) {
                    skip(stream, 5);
                    count += 5;
                }
            } else if (buf1[0] != (byte)0x0F) {   /* else - just validate the 8 bit code */
                throw new BadDataException("invalid packet");
            }
        } /* end of not PRIVATE_STREAM2_CODE */

        /* handle the packet data */
        dataSize = packetLen - count;
        if (justLooking) {
            skip(stream, dataSize);
            return;
        }

        /* check if it is reserved or private stream */
        if ((streamID < 0) || (streamID >= MAX_NUM_STREAMS)) {
            skip(stream, dataSize);
        } else {      /* (regular) audio/video stream */
            if (trackList[streamID] == null) {    /* in the first packet of the track */
                trackList[streamID] = new TrackList();
            }
            trackInfo = trackList[streamID];

            /* if stream not initialized yet, and/or couldn't extract stream info yet */
            if (trackInfo.infoFlag == false) {
                tmpBuf = new byte[dataSize];
                numWrittenToTmpBuf = extractStreamInfo(tmpBuf, streamID, dataSize, false);
             }

            if (trackInfo.infoFlag == false) {  /* no header found in this packet */
                trackList[streamID] = null;
                if (numWrittenToTmpBuf < dataSize) {
                    skip(stream, (dataSize-numWrittenToTmpBuf));  /* skip the rest of the packet */
                }
            } else {
                /* update PTS if needed */
                if (startPTS == NO_PTS_VAL) {
                    trackInfo.startPTS = currentPTS;
                }

                /* now copy data */
                trackInfo.copyStreamDataToInnerBuffer(tmpBuf, 
						numWrittenToTmpBuf,
						dataSize - numWrittenToTmpBuf,
						currentPTS);

                trackInfo.numPackets++;
                if (dataSize > trackInfo.maxPacketSize) {
                    trackInfo.maxPacketSize = dataSize;
                }
            }
        }

        numPackets++;
    }


   /*
    *  parse the packet and extract specific information
    */
    private int extractStreamInfo (byte[] tmpBuf, int streamID,
                                   int dataLen, boolean AVOnlyState)
                                   throws IOException, BadDataException {
        byte stype = UNKNOWN_TYPE;
        TrackList trackInfo = trackList[streamID];
        int numBytes;

        /* check if need to initialize the buffer and the stream structure */
        if (trackInfo.trackType == UNKNOWN_TYPE) {
            /* update fields */
            stype = (AVOnlyState) ? streamType :
                    ((streamID < MIN_VIDEO_ID) ? AUDIO_TYPE : VIDEO_TYPE);
            trackInfo.init(stype);
            sysHeader.streamFlags[streamID] = true;
            trackInfo.startPTS = currentPTS;
        }

        /* extract specific audio / video info */
        if (stype == AUDIO_TYPE) {
            numBytes = extractAudioInfo(tmpBuf, trackInfo, dataLen, AVOnlyState);
        } else {    /* VIDEO_TYPE  */
           numBytes = extractVideoInfo(tmpBuf, trackInfo, dataLen, AVOnlyState);
        }

        if (trackInfo.infoFlag == true) {
            if (AVOnlyState) {
                tracks[0] = new MediaTrack(trackInfo);
            } else {
                tracks[numTracks] = new MediaTrack(trackInfo);
                numTracks++;
            }
        }

        return numBytes;
    }


   /*
    *  parse the audio packet and extract specific audio information
    *
    *  Remark: audio.ID bit is set to '1' for standard MPEG-1 audio
    *          (ISO/IEC 11172-3). If this bit is set to '0', MPEG-2
    *           backwards compatible audio (ISO/IEC 13818-3) extension for
    *           lower sampling frequencies is used.
    */
    private int extractAudioInfo (byte[] tmpBuf, TrackList trackInfo,
                                  int dataLen, boolean AVOnlyState)
                                                         throws IOException, BadDataException {

        Audio audio = new Audio();
        int br, sr, numBytes;

        /* tables for standard MPEG-1 audio */
        int samplingFrequencyTable[] = {44100, 48000, 32000};
        final short bitrateIndexTableL2[] =
              {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384};
        /* bitrate table for layer 3:
         *    {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320};
         */

        /* tables for MPEG-2 audio extension */
        /* int samplingFrequencyTable[] = {22050, 24000, 16000}; */
        final short bitrateIndexTableL23Ext[] =
              {0, 8, 16, 24, 32, 40, 48,  56, 64, 80, 96, 112, 128, 144, 160};
        /* bitrate table for layer 1 extension:
              {0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256};
         */

        numBytes = (AVOnlyState) ? dataLen : (readBytes(stream, tmpBuf, dataLen));
        for (int i = (int) startLocation ; i < numBytes-3 ; i++) {
            // doesn't handle  header start code which is splitted on end-of-packet
            if ( (tmpBuf[i] == (byte)0xFF) && ( (tmpBuf[i+1] &(byte)0xF0) == (byte)0xF0) ) {
                audio.ID = (tmpBuf[i+1] & 0x0008) >> 3;
                audio.layer = 4 - ((tmpBuf[i+1] & 0x0006) >> 1);
                audio.protection = (tmpBuf[i+1] & 0x0001);
                br = (tmpBuf[i+2] & 0x00F0) >> 4;
                sr = (tmpBuf[i+2] & 0x000C) >> 2;
		{
		    if ( (sr < 0) || (sr >= samplingFrequencyTable.length) ) {
			throw new BadDataException("Non Standard sample rates not supported");
		    }
		}
                audio.mode = (tmpBuf[i+3] & 0x00C0) >> 6;
                audio.modeExt = (tmpBuf[i+3] & 0x0030) >> 4;
                audio.channels = (audio.mode == 3) ? 1 : 2;
                audio.copyright = (tmpBuf[i+3] & 0x0008) >> 3;
                audio.original = (tmpBuf[i+3] & 0x0004)>> 2;
                audio.emphasis = (tmpBuf[i+3] & 0x0003);
                audio.valid = (br != 0x000F);
                /* calculate sampling frequency and bitrate values */
                if (audio.ID == 1) {    /* standard MPEG-1 */
                    audio.sampleRate = samplingFrequencyTable[sr];
                    if (audio.layer == 3) {
                        if (br < 2) {
                            audio.bitRate = bitrateIndexTableL2[br];
                        } else if (br == 2) {
                            audio.bitRate = 40;
                        } else {
                            audio.bitRate = bitrateIndexTableL2[br-1];
                        }
                    } else if (audio.layer == 2) {
                        audio.bitRate = bitrateIndexTableL2[br];
                    } else {      /* layer 1 */
                        audio.bitRate = br << 5;
                    }
                } else {                /* extension MPEG-2 */
                    audio.sampleRate = samplingFrequencyTable[sr]>>1;
                    if ((audio.layer == 3) || (audio.layer == 2)) {
                        audio.bitRate = bitrateIndexTableL23Ext[br];
                    } else {      /* layer 1 */
                        if (br < 9) {
                            audio.bitRate = bitrateIndexTableL2[br];
                        } else if (br == 9) {
                            audio.bitRate = 144;
                        } else if (br == 10) {
                            audio.bitRate = bitrateIndexTableL2[br-1];
                        } else if (br == 11) {
                            audio.bitRate = 176;
                        } else {
                            audio.bitRate = bitrateIndexTableL2[br-2];
                        }
                    }
                }

                /* this is a calculation for one decoded frame length, in bytes */
                // if (audio.bitRate < 30) {
                //     decodeFrameLen = 10 * 1024;
                // } else {
                //     br = (audio.layer == 1) ? 48 : 144;
                //     decodeFrameLen = (int)(8 * (float)(audio.bitRate * 1000 * br) / audio.sampleRate);
                // }

                /* num of bytes for a duration of 1 second */
                trackInfo.readFrameSize = (audio.bitRate * 1000) >> 3;

                trackInfo.infoFlag = true;
                trackInfo.media = audio;
                break;
            }
        }

//trackInfo.media.toString();
        return numBytes;
    }


   /*
    *  parse the video packet and extract specific video information
    */
    private int extractVideoInfo (byte[] tmpBuf, TrackList trackInfo,
                                  int dataLen, boolean AVOnlyState)
                                          throws IOException, BadDataException {
        Video video = new Video();
        int i, code, numBytes, pr;
        float aspectRatioTable[] =
                    {0.0f, 1.0f, 0.6735f, 0.7031f, 0.7615f, 0.8055f, 0.8437f,
                     0.8935f, 0.9375f, 0.9815f, 1.0255f, 1.0695f, 1.1250f,
                     1.1575f, 1.2015f, 1.0f};
         float pictureRateTable[] =
                    {0.0f, 23.976f, 24.0f, 25.0f, 29.97f, 30.0f, 50.0f, 59.94f,
                     60.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f};

        numBytes = (AVOnlyState) ? dataLen : (readBytes(stream, tmpBuf, dataLen));
        for (i = 0 ; i < numBytes-10 ; i++) {
            // doesn't handle  header start code which is splitted on end-of-packet
            // not working in Win, because of padding with the sign bit:
            // code = data[i] << 24 | data[i+1] << 16 | data[i+2] << 8 | data [i+3];
            code = ((tmpBuf[i] << 24) & 0xFF000000) | ((tmpBuf[i+1] << 16) & 0x00FF0000) |
                   ((tmpBuf[i+2] << 8) & 0x0000FF00) | (tmpBuf[i+3] & 0x000000FF);
            if (code == VIDEO_SEQUENCE_HEADER_CODE) {
                video.width = (tmpBuf[i+4+0] & 0x00FF) << 4;
                video.width |= (((int)tmpBuf[i+4+1]) >> 4) & 0x000F;
                video.height = (tmpBuf[i+4+1] & 0x000F) << 8;
                video.height |= (tmpBuf[i+4+2] & 0x00FF);
                pr = (tmpBuf[i+4+3] & 0x00F0) >> 4;
                video.pelAspectRatio = aspectRatioTable[pr];
                pr = tmpBuf[i+4+3] & 0x000F;
                video.pictureRate = pictureRateTable[pr];
                pr = ( (tmpBuf[i+4+4] & 0x00FF) << 10 ) |
                     ( (tmpBuf[i+4+5] & 0x00FF) << 2 ) |
                     ( (tmpBuf[i+4+6] & 0x00C0) >> 6);
                video.bitRate = pr * 400;    // bitrate in units of 400 bps
                if ( (video.pelAspectRatio == 0.0) || (video.pictureRate == 0.0) ) {
                    throw new BadDataException("video header corrupted");
                }
                if (video.pictureRate < 23.0) {
                    trackInfo.readFrameSize = 64 * 1024;
                } else {
                    /* readFrameSize should be 1 second, but limited to not more
                       than half the video track buf size (arbitrary) */
                    trackInfo.readFrameSize = (int)(video.bitRate >> 3);
                    if (trackInfo.readFrameSize > (VIDEO_TRACK_BUF_SIZE>>1)) {
                        trackInfo.readFrameSize = VIDEO_TRACK_BUF_SIZE>>1 ;
                    }
                }
                trackInfo.infoFlag = true;
                trackInfo.media = video;
//trackInfo.media.toString();
                break;
            }
        }

        return numBytes;
    }


   /*
    *  getStreamID - match value into an index
    */
    private int getStreamID (byte bval) {
        return ((bval & 0xFF) - 0xC0);
    }

    
    /* 
     * The inner class can use this method as they cannot use the 
     * getLocation(stream) method.
     */
    private long getLocation() {
        return getLocation(stream);
    }


   /*
    *  check if all the inner buffers have 'enough' space for another packet
    *  (in the future, may check it only for the next relevant track....)
    */
    boolean needingMore() {
        TrackList trackInfo;

        for (int i = 0; i < numTracks ; i++) {
            if (tracks[i] != null) {
                trackInfo = ((MediaTrack)tracks[i]).getTrackInfo();
		if (trackInfo.bufQ.canRead()) {
                    return false;
                }
            }
        }
        return true;
    }


    void flushInnerBuffers() {

        TrackList trackInfo;

        for (int i = 0; i < numTracks ; i++) {
            if (tracks[i] != null) {

                trackInfo = ((MediaTrack)tracks[i]).getTrackInfo();

		// Release the wait in copyStreamDataToInnerBuffer.
		synchronized (trackInfo.bufQ) {
		    trackInfo.flushFlag = true;
		    trackInfo.bufQ.notifyAll();
		}

                trackInfo.flushBuffer();
            }
        }
    }


    /// a temporary utility for debugging !
    void saveInnerBuffersToFiles() {
        TrackList trackInfo;

        for (int i = 0; i < numTracks ; i++) {
            if (tracks[i] != null) {
                trackInfo = ((MediaTrack)tracks[i]).getTrackInfo();
                trackInfo.saveBufToFile();
            }
        }
    }


    void throwInnerBuffersContents() {
        TrackList trackInfo;
        for (int i = 0; i < numTracks ; i++) {
            if (tracks[i] != null) {
                trackInfo = ((MediaTrack)tracks[i]).getTrackInfo();
                trackInfo.flushBuffer();
            }
        }
    }
    /// temporary !!
    /* ===================================================================== */


   /*
    *  Media class
    */
    private abstract class Media {
        abstract Format createFormat();
    }


    /* ===================================================================== */

   /*
    *  Audio specific information class
    */
    private class Audio extends Media {
        boolean valid = false;
        int ID = 0;
        int layer = 0;
        int protection = 0;
        int bitRate = 0;     /* in Kbits/sec == bits/msec */
        int sampleRate = 0;
        int mode = 0;
        int modeExt = 0;
        int copyright= 0;
        int original = 0;
        int emphasis= 0;
        int channels=0;
        AudioFormat format = null;


    Format createFormat() {
        if (format != null)
            return format;

	String encodingString;
	if (layer == 3)
	    encodingString = AudioFormat.MPEGLAYER3;
	else
	    encodingString = AudioFormat.MPEG;

//System.out.println("Audio layer="+layer+"   encodingString="+encodingString);

        int bitsPerSample = 16;
        int frameSizeInBits = ((layer == 1) ? 352 : 1024)
                              * channels * bitsPerSample;

//         format = new AudioFormat(encodingString,
//                      (double) sampleRate,
//                      bitsPerSample,
//                      channels,
//                      AudioFormat.LITTLE_ENDIAN,
//                      AudioFormat.SIGNED,
//                      frameSizeInBits,
//                      Format.NOT_SPECIFIED, // No FRAME_RATE specified
//                      byte[].class);

	int bytesPerSecond = (bitRate * 1000) >> 3;
        format = new WavAudioFormat(encodingString,
				    (double) sampleRate,
				    bitsPerSample,
				    channels,
				    frameSizeInBits,
				    bytesPerSecond,
				    AudioFormat.LITTLE_ENDIAN,
				    AudioFormat.SIGNED,
				    Format.NOT_SPECIFIED, // No FRAME_RATE specified
				    byte[].class,
				    null  // Codec Specific Header
				    );


        return format;
    }


    public String toString() {
        System.out.println("Audio Media: " + format);
        System.out.println("Number of channels " + channels);
        System.out.println("valid " + valid);
        System.out.println("ID " + ID);
        System.out.println("layer " + layer);
        System.out.println("protection " + protection);
        System.out.println("bitrate " + bitRate);
        System.out.println("sample rate " + sampleRate);
        System.out.println("Mode " + mode + " ext "+ modeExt);
        System.out.println("copyright " + copyright);
        System.out.println("original " + original);
        System.out.println("emphasis " + emphasis);
        System.out.println("channels " + channels);
        return super.toString();
    }
    }

    /* ===================================================================== */

   /*
    *  Video specific information class
    */
    private class Video extends Media {
        int width=0;
        int height=0;
        float pelAspectRatio = 0;
        float pictureRate = 0;
        int bitRate = 0;
        VideoFormat format = null;


    Format createFormat() {
        int size = (int)(width * height * 1.5);       ////  *3 ??  *4 ??
        if (format != null)
            return format;

        format = new VideoFormat(VideoFormat.MPEG,
                                 new java.awt.Dimension(width, height),
                                 size,
                                 byte[].class,
                                 (float)pictureRate);

        return format;
    }


    public String toString() {
        System.out.println("Video Media: " + format);
        System.out.println("width " + width);
        System.out.println("height " + height);
        System.out.println("pixel aspect ratio " + pelAspectRatio);
        System.out.println("picture rate " + pictureRate);
        System.out.println("bitrate " + bitRate);
        return super.toString();
    }
    }

    /* ===================================================================== */

   /*
    *  Track information class
    */
    private class TrackList {
    byte trackType = UNKNOWN_TYPE;   /* Chunk identifier: Unknown/Audio/Video */
    Time duration = Duration.DURATION_UNKNOWN;   // TODO: NEED THIS?
    long startPTS = NO_PTS_VAL;    // the stream reset time ("zero")
    boolean infoFlag = false;    // indicates wether audio/video info was extracted
    int  numPackets = 0;
    int  maxPacketSize = 0;
    int  readFrameSize = 0;      // size of 'chunk' in readFrame (~1 frame for video, ~8 frames for audio)
    Media media;                 // Info specific to each track type
    boolean supported = false;   // Is this track type supported
    boolean flushFlag = false;

    CircularBuffer bufQ = null;
    Buffer current = null;

    MpegParser parser = MpegParser.this;


    void init (byte stype) {
        supported = true;
        trackType = stype;

	// We buffer up to 15 frames of compressed MPEG video and
	// 5 secs worth of compressed audio.
	if (trackType == VIDEO_TYPE)
	    bufQ = new CircularBuffer(15);
	else
	    bufQ = new CircularBuffer(10);
    }


   /*
    * return the number of data bytes in the inner buffer
    */
    int readyDataBytes () {
	return 1;
    }


   /*
    * fill the inner buffer from the input stream
    */
    void copyStreamDataToInnerBuffer (byte in[], int inSize, int size, long pts)
	throws IOException {

	int total = size;
	int len = 0;
	if (inSize > 0)
	    total += inSize;
	else
	    inSize = 0;

	synchronized (bufQ) {
	    if (current != null) {
		len = current.getLength();
		if (len != 0 && len + total >= readFrameSize) {
		   // We are done reading one frame.
		    bufQ.writeReport();
		    bufQ.notify();
		    current = null;
		}
	    }

	    byte data[];

	    flushFlag = false;

	    if (current == null) {
		while (!bufQ.canWrite() && !flushFlag) {
		    try {
			bufQ.wait();
		    } catch (InterruptedException e) {}
		}

		if (flushFlag)
		    return;

		current = bufQ.getEmptyBuffer();

		current.setFlags(0);
		current.setOffset(0);
		current.setLength(0);
		current.setTimeStamp(convPTStoNanoseconds(pts));

		// Check the buffer for the enough space.
		int bsize = (total > readFrameSize ? total : readFrameSize);
		data = (byte[])current.getData();

		if (data == null || data.length < bsize) {
		    data = new byte[bsize];
		    current.setData(data);
		}
	    } else
		data = (byte[])current.getData();

	    len = current.getLength();

	    // First copy the spilled over temp buffer.
	    if (inSize > 0)
		System.arraycopy(in, 0, data, len, inSize);

	    // Read data from the stream into the buffer.
	    parser.readBytes(stream, data, len + inSize, size);

	    current.setLength(len + total);
	}  // synchronized bufQ
    }


   /*
    * fill the output buffer from the inner buffer
    */
    void copyFromInnerBuffer (Buffer out) {

	Buffer buf; 

	synchronized (bufQ) {
	    while (!bufQ.canRead() && !sysPausedFlag && !parserErrorFlag) {
		try {
		    bufQ.wait();
		} catch (InterruptedException e) {}
	    }

            if (sysPausedFlag || parserErrorFlag) {
        	out.setLength(0);
        	out.setDiscard(true);
		return;
            }

	    buf = bufQ.read();
	    byte saved[] = (byte[])out.getData();

	    out.copy(buf);
	    buf.setData(saved);

	    bufQ.readReport();
	    bufQ.notify();
	}
    }


    /**
     * Release any blocking readFrame.
     */
    void releaseReadFrame() {
	synchronized (bufQ) {
	    bufQ.notifyAll();
	}
    }


    /**
     * Generate the EOM buffer and add to the buffer Q. 
     */
    void generateEOM () {

	Buffer buf;

	synchronized (bufQ) {
	    // We'll need to push the last bit of data left in the buffer Q.
	    if (current != null) {
		bufQ.writeReport();
		bufQ.notify();
		current = null;
	    }

	    // Now grab an empty buffer and fill that with the EOM flag.
	    while (!bufQ.canWrite()) {
		try {
		    bufQ.wait();
		} catch (InterruptedException e) {}
	    }
	    buf = bufQ.getEmptyBuffer();

	    buf.setFlags(buf.FLAG_EOM);
	    buf.setLength(0);

	    bufQ.writeReport();
	    bufQ.notify();
	}
    }


    /*
     * flush the inner buffer
     */
    void flushBuffer() {
	synchronized (bufQ) {
	    if (current != null) {
		current.setDiscard(true);
		bufQ.writeReport();
		current = null;
	    }
	    while (bufQ.canRead()) {
		bufQ.read();
		bufQ.readReport();
	    }
	    bufQ.notifyAll();
	}
    }


    public String toString() {
        System.out.println("track type " + trackType + "(0 ?, 1 audio, 2 video)");
        System.out.println("start PTS " + startPTS);
        System.out.println("info flag " + infoFlag);
        System.out.println("number of packets " + numPackets);
        System.out.println("maximum packet size " + maxPacketSize);
        System.out.println("supported " + supported);
        System.out.println("duration (?) " + duration);
        return media.toString();
    }


    /// a temporary utility for debugging !
    void saveBufToFile(){
/*
        FileOutputStream fout;
        int size = readyDataBytes();
        int itmp = bufLen - readPtr;
        fout = (trackType == AUDIO_TYPE) ? aout : vout;
        if (size == 0)
            return;
        try {
            if (size > itmp) {
                fout.write (buf, readPtr, itmp);
                fout.write (buf, 0, (size-itmp));
            } else {
                fout.write (buf, readPtr, size);
            }
            fout.flush();
            readPtr = 0;
            writePtr = 0;
        } catch (java.io.IOException e) {
            System.out.println ("   EE>> problem in writing to files");
        }
*/
    }
    }

    /* ===================================================================== */

   /*
    *  Implemantation of the Track interface
    */
    private class MediaTrack implements Track {
        private TrackList trackInfo;
        private boolean enabled;
        private long sequenceNumber = 0;
        private Format format;
        private TrackListener listener;
        MpegParser parser = MpegParser.this;


    MediaTrack (TrackList trackInfo) {
        this.trackInfo = trackInfo;
        enabled = true;               // EE??
        format = trackInfo.media.createFormat();
    }


    public void setTrackListener(TrackListener l) {
        listener = l;
    }

    public Format getFormat () {
        return format;
    }


    public void setEnabled (boolean t) {
        enabled = t;
    }

    public boolean isEnabled () {
        return enabled;
    }

    public Time getDuration () {
        return trackInfo.duration;
    }


    public Time getStartTime () {
        if (streamType == SYS11172_TYPE) {
            return new Time(((double)startPTS) / 90000.0);
        } else {
            return new Time(AVstartTimeNs);
        }
    }

    public void readFrame (Buffer buffer) {

        /* check first */
        if (buffer == null) {
            return;
        }
        if (!enabled) {
            buffer.setDiscard(true);
            return;
        }

        /* now read frame into the buffer */
        if (streamType == SYS11172_TYPE) {
            systemStreamReadFrame(buffer);
        } else {
            AudioVideoOnlyReadFrame(buffer);
        }

        buffer.setFormat(format);
        buffer.setSequenceNumber(++sequenceNumber);

	if (format instanceof AudioFormat) {
	    long tmp = buffer.getTimeStamp();
	    buffer.setTimeStamp(lastAudioNs);
	    lastAudioNs = tmp;
	}
    }


    private void AudioVideoOnlyReadFrame (Buffer buffer) {

        if (sysPausedFlag || parserErrorFlag) {
            buffer.setLength(0);
            buffer.setDiscard(true);
        }

        int size = trackInfo.readFrameSize;
        Object obj = buffer.getData();
        byte[] data;

        if ( (obj == null) ||
             (! (obj instanceof byte[]) ) ||
             ( ((byte[])obj).length < size) ) {
            data = new byte[size];
            buffer.setData(data);
        } else {
            data = (byte[]) obj;
        }

        int read1=0, read2=size;
        int actualBytesRead=0, counter=0;

        /* first, need to check if there is some data left in the initiation buffer */
        if (initTmpBufLen > 0) {
            read1 = (initTmpBufLen > size) ? size : initTmpBufLen;
            System.arraycopy (initTmpStreamBuf, 0, data, 0, read1);
            initTmpBufLen -= read1;
            read2 -= read1;
            counter = read1;
            /// AVtotalBytesRead += read1;
        }

        if (trackInfo.trackType == AUDIO_TYPE) {
	    buffer.setTimeStamp(convBytesToTimeAV(getLocation() - read1));
	}

        /* now, read from the input stream */
        if ((read2 > 0) && !EOMflag) {
            try {
                actualBytesRead = parser.readBytes (stream, data, read1, read2);
                if (actualBytesRead == com.sun.media.protocol.BasicSourceStream.LENGTH_DISCARD) {
                    if (read1 == 0) {
                        buffer.setDiscard(true);
			return;
		    }
                } else {
                    counter += actualBytesRead;
                    /// AVtotalBytesRead += actualBytesRead;
                }
            } catch (IOException e) {
                updateEOMState();
                EOMflag = true;
                if (AVlastTimeNs == 0) {
                    /// AVcurrentTimeNs = convBytesToTimeAV(AVtotalBytesRead);
                    AVcurrentTimeNs = convBytesToTimeAV(getLocation());
                    AVlastTimeNs = AVcurrentTimeNs;
                }
            }
        }

        if (EOMflag) {
            if (read1 > 0) {
		buffer.setLength(read1);
		buffer.setOffset(0);
            } else {
                buffer.setLength(0);
                buffer.setEOM(true);
            }
        }

	buffer.setOffset(0);
	buffer.setLength(counter);
	//buffer.setTimeStamp(lastSetPositionTimeNs);
    }


    private void systemStreamReadFrame(Buffer buffer) {

        /* copy from inner buffer to the output */
        trackInfo.copyFromInnerBuffer(buffer);

        if (sysPausedFlag || parserErrorFlag)
	    return;

        /* check for disabled tracks */
        for (int i = 0; i < numTracks ; i++) {
            if (tracks[i] != null) {
                if (!tracks[i].isEnabled() ) {
                    TrackList AtrackInfo = ((MediaTrack)tracks[i]).getTrackInfo();
                    AtrackInfo.flushBuffer();
                }
            }
        }

        /* debugging: it's an opportunity to 'flush' the audio inner buffers */
        if (hideAudioTracks) {
            TrackList AtrackInfo;
            for (int i = 0; i < numTracks ; i++) {
                if (tracks[i] != null) {
                    AtrackInfo = ((MediaTrack)tracks[i]).getTrackInfo();
                    if (AtrackInfo.trackType == AUDIO_TYPE) {
                        AtrackInfo.flushBuffer();
                    }
                }
            }
        }

        if (hideVideoTracks) {
            TrackList AtrackInfo;
            for (int i = 0; i < numTracks ; i++) {
                if (tracks[i] != null) {
                    AtrackInfo = ((MediaTrack)tracks[i]).getTrackInfo();
                    if (AtrackInfo.trackType == VIDEO_TYPE) {
                        AtrackInfo.flushBuffer();
                    }
                }
            }
        }
    }


    public int mapTimeToFrame (Time t) {
        return 0;       // TODO
    }


    public Time mapFrameToTime (int frameNumber) {
        return null;    // TODO
    }

    private TrackList getTrackInfo () {
        return trackInfo;
    }

    }


    /* ===================================================================== */

   /*
    *  A class for holding and manipulating the system data
    *  of the MPEg bitstream
    */
    private class SystemHeader {

        int headerLen = 0;           /* 16 */
        int rateBound = 0;           /* (1+)22(+1) */
        int audioBound = 0;          /* 6 */
        int fixedFlag = 0;           /* 1 */
        int CSPSFlag = 0;            /* 1 */
        int audioLockFlag = 0;       /* 1 */
        int videoLockFlag = 0;       /* 1 */
        int videoBound = 0;          /* (1+)5 */
        int reserved = 0;            /* 8 */
        boolean allAudioSTDFlag = false;    /* STD is for all audio streams */
        boolean allVideoSTDFlag = false;    /* STD is for all video streams */
        boolean streamFlags[] = new boolean [MAX_NUM_STREAMS];   /* 8xN */
        int STDBufBoundScale[] = new int [MAX_NUM_STREAMS];      /* (2+)1xN */
        int STDBufSizeBound[] = new int [MAX_NUM_STREAMS];       /* 13xN */


    SystemHeader () {
        for (int i = 0; i < MAX_NUM_STREAMS; i++) {
            streamFlags[i] = false;
            STDBufBoundScale[i] = 0;
            STDBufSizeBound[i] = 0;
        }
    }


    /* reset system header fields */
    void resetSystemHeader () {
        headerLen = 0;
        rateBound = 0;
        audioBound = 0;
        fixedFlag = 0;
        CSPSFlag = 0;
        audioLockFlag = 0;
        videoLockFlag = 0;
        videoBound = 0;
        reserved = 0;
        allAudioSTDFlag = false;
        allVideoSTDFlag = false;
        for (int i = 0; i < MAX_NUM_STREAMS; i++) {
            streamFlags[i] = false;
            STDBufBoundScale[i] = 0;
            STDBufSizeBound[i] = 0;
        }
    }

    void printFields () {
        System.out.println("headerLen "+headerLen);
        System.out.println("rateBound "+rateBound);
        System.out.println("audioBound "+audioBound);
        System.out.println("fixedFlag "+fixedFlag);
        System.out.println("CSPSFlag "+CSPSFlag);
        System.out.println("audioLockFlag "+audioLockFlag);
        System.out.println("videoLockFlag "+videoLockFlag);
        System.out.println("videoBound "+videoBound);
        System.out.println("reserved "+reserved);
        System.out.println("allAudioSTDFlag "+allAudioSTDFlag);
        System.out.println("allVideoSTDFlag "+allVideoSTDFlag);
        for (int i = 0; i < MAX_NUM_STREAMS; i++) {
            if (streamFlags[i])
                System.out.println("["+i+"]  STDBufBoundScale "+
		                             STDBufBoundScale[i]+
		                        "     STDBufSizeBound "+
					STDBufSizeBound[i]);
        }
    }
    }
}

/** this exception is thrown when mpeg data is not valid **/
class BadDataException extends MediaException {
    
    /* Creates a simple exception object. */
    BadDataException() {
	super();
    }
    
    /* Creates an exception object with a specific reason. */
    BadDataException(String reason) {
	super(reason);
    }
}

/**
 * This class used to be an inner class, which is the correct thing to do.
 * Changed it to a package private class because of jdk1.2 security.
 * For jdk1.2 and above applets, MpegBufferThread is created in a
 * privileged block using jdk12CreateThreadAction. jdk12CreateThreadAction
 * class is unable to create and instantiate an inner class 
 * in MpegParser class
 */

/**
 *  A class for managing the thread which fill the inner buffers of
 *  the tracks.
 */
class MpegBufferThread extends LoopThread {

    private MpegParser parser;

    MpegBufferThread() {
	this.setName(this.getName() + " (MpegBufferThread)");
	useVideoPriority();
    }

    void setParser(MpegParser p) {
	parser = p;
    }

    public boolean process() {

	// If EOM is set, then we'll generate the EOM marker.
	if (parser.EOMflag) {
	    parser.updateTrackEOM();
	    pause();
	    return true;
	}

	try {
	    /* fill buffers */
	    parser.mpegSystemParseBitstream(false, 0L, false, MpegParser.NO_PTS_VAL);

	} catch (BadDataException e) {
	    parser.parserErrorFlag = true;
	} catch (BadHeaderException e) {
	    parser.parserErrorFlag = true;
	} catch (IOException e) {
	    parser.updateEOMState();
	    parser.EOMflag = true;
	    if (parser.endPTS == MpegParser.NO_PTS_VAL)
		parser.endPTS = parser.currentPTS;
	}

	if (parser.parserErrorFlag) {
	    Log.error("MPEG parser error: possibly with a corrupted bitstream.");
	    pause();
	}

	return true;     /* for loop thread to continue */
    }
}
