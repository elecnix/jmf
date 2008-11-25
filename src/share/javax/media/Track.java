/*
 * @(#)Track.java	1.14 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import javax.media.Format;

/**
 * A <CODE>Track</CODE> abstracts the information specific to an individual
 * track in a media stream. 
 * A media stream might contain multiple media tracks, such as separate tracks for
 * audio, video, and midi data. A <CODE>Track</CODE> is the output of a <code>Demultiplexer</code>.
 * @see Demultiplexer
 * @since JMF 2.0
 */ 
public interface Track extends Duration {

    /**
     * The <CODE>mapFrameToTime</CODE> method returns this value if the time mapping
     * cannot be established or the <CODE>Track</CODE> contains an audio stream.
     */
    public static Time TIME_UNKNOWN = Time.TIME_UNKNOWN;

    /**
     * The <CODE>mapTimeToFrame</CODE> method returns this value if the time mapping
     * cannot be established or the <CODE>Track</CODE> contains an audio stream.
     * 
     */
    public static int FRAME_UNKNOWN = Integer.MAX_VALUE;

    /**
     * Gets the <code>Format</code> of the data in this 
     * <code>Track</code>.
     * 
     * @return The <code>Format</code> associated with this 
     * <code>Track</code>.
     */
    public Format getFormat();


    /**
     * Enables or disables this <code>Track</code>.
     * A <CODE>Demultiplexer</CODE>  discards the data from
     * disabled tracks.
     * @param t A boolean value indicating whether or not to enable this <CODE>Track</CODE>. Set to 
     * <CODE>true</CODE> to enable this <CODE>Track</CODE>, <CODE>false</CODE> to disable this <CODE>Track</CODE>.
     */
    public void setEnabled(boolean t);

    /**
     * Checks whether or not this <code>Track</code> is enabled.
     * A <CODE>Demultiplexer</CODE>  discards the data from
     * disabled tracks.
     * @return <CODE>true</CODE> if the <code>Track</code> is enabled, <CODE>false</CODE> if it is not.
     */
    public boolean isEnabled();


    /**
     * Retrieves the start time of this <CODE>Track</CODE>.
     * @return A <CODE>Time</CODE> object that specifies the start time for this <CODE>Track</CODE>.
     */
    public Time getStartTime();


    /**
     * Reads the next frame for this <CODE>Track</CODE>.
     * <p>
     * This method might block if the  
     * data for a complete frame is not available. It might also block if the stream contains
     * intervening data for a different interleaved <CODE>Track</CODE>.
     * Once the other <CODE>Track</CODE> is read by a <CODE>readFrame</CODE> call from a
     * different thread, this method can read the frame. If the intervening
     * <CODE>Track</CODE> has been disabled, data for that <CODE>Track</CODE> is read and discarded.
     * <p>
     * Note: This scenario is necessary only if a <CODE>PullDataSource</CODE> <CODE>Demultiplexer</CODE>
     * implementation wants to avoid buffering data locally and
     * copying the data to the <CODE>Buffer</CODE> passed in as a parameter.
     * Implementations might decide to buffer data and not
     * block (if possible) and incur data copy overhead.
     * <p>
     * If this method is called on a <CODE>Track</CODE> that has been disabled,
     * it returns immediately with the <CODE>DISCARD</CODE> flag set on. 
     * <p>
     * Each track has a sequence number that is updated by the <CODE>Demultiplexer</CODE> 
     * for each frame.
     * If downstream nodes receive non-contiguous sequence numbers
     * for a particular <CODE>Track</CODE>, they know that a data overflow has
     * occurred for that <CODE>Track</CODE>.
     * @param buffer The <CODE>Buffer</CODE> into which the data is to be read.  If
     * <CODE>readFrame</CODE> is successful, <code>buffer.getLength</code> returns the length of the
     * data that was read.
     */
    public void readFrame(Buffer buffer);

    /**
     * Converts the given media time to the corresponding frame number.
     * <p>
     * The frame returned is the nearest frame that has a media time
     * less than or equal to the given media time.
     * <p>
     * @param mediaTime the input media time for the conversion.
     * @return the converted frame number the given media time.  If the
     *   conversion fails, FRAME_UNKNOWN is returned.
     */
    public int mapTimeToFrame(Time t);

    /**
     * Gets the <CODE>Time</CODE> that corresponds to the specified frame number.
     * @return A <CODE>Time</CODE> object that corresponds to the specified frame.
     * If the mapping cannot be established or the <CODE>Track</CODE> is an audio track, 
     * <CODE>TIME_UNKNOWN</CODE> is returned.
     */
    public Time mapFrameToTime(int frameNumber);


    /**
     * Adds an event listener for this <CODE>Track</CODE>. If the <CODE>readFrame</CODE> call in 
     * progress will block, the listener is notified. This enables the listener to
     * stop the clock and post a <CODE>RestartingEvent</CODE>.
     * @param listener The <CODE>TrackListener</CODE> to add.
     */
    public void setTrackListener(TrackListener listener);

}

