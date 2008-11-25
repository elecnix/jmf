/*
 * @(#)Demultiplexer.java	1.12 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import java.io.IOException;
import javax.media.format.*;
import javax.media.protocol.ContentDescriptor;

/**
 * A <code>Demultiplexer</code> is a media processing unit that 
 * takes an interleaved media stream as input, extracts the individual tracks from the stream,
 * and outputs the resulting tracks. It has one input and multiple outputs.
 * <p>
 * A <code>Demultiplexer</code> is a <CODE>MediaHandler</CODE> and needs to
 * implement the <code>setSource</code> method. This method
 * should throw an <CODE>IncompatibleSourceException</CODE> if the
 * <code>Demultiplexer</code> cannot use the specified <code>DataSource</code>.
 * This typically happens if:
 * <ul>
 * <li>The <code>Demultiplexer</code> doesn't support the type of <CODE>DataSource</CODE> specified (push or
 * pull). <br>&nbsp;</li>
 * <li>The <code>Demultiplexer</code> requires a positionable <CODE>DataSource</CODE>.<br>&nbsp;</li>
 * <li>The <code>Demultiplexer</code>  requires a seekable,  random-access stream.
 * (For example, the QuickTime <CODE>Demultiplexer</CODE> has this requirement.)</li>
 * </ul>
 * The <code>setSource</code> method should throw an <CODE>IOException</CODE> if the specified
 * <code>DataSource</code> contains a null stream array.
 * <p>
 * Note: No data is read by the <code>setSource</code> method.
 * When trying to select a <CODE>Demultiplexer</CODE> for a particular content-type from a
 * list of <CODE>Demultiplexer</CODE> implementations obtained through the <CODE>PlugInManager</CODE>,
 * the <CODE>Processor</CODE> picks the first
 * <CODE>Demultiplexer</CODE> that doesn't for throw an
 * <CODE>IncompatibleSourceException</CODE> or an
 * <CODE>IOException</CODE> when <CODE>setSource</CODE> is called. 
 * If a particular <code>Demultiplexer</code> reads some
 * data from the stream and then throws an <CODE>IncompatibleSourceException</CODE>,
 * the next <CODE>Demultiplexer</CODE> in the list gets the stream in a different state.
 * @since JMF 2.0
 */
public interface Demultiplexer extends PlugIn, MediaHandler, Duration {

    /**
     * Lists the all of the input content descriptors that this <CODE>Demultiplexer</CODE> supports.
     */
    public ContentDescriptor [] getSupportedInputContentDescriptors();

    /**
     * Signals that data is going to start being read from the <CODE>Demultiplexer</CODE>. 
     * The <CODE>start</CODE> method is called before any calls are made to <code>readFrame</code>.
     *
     * @exception IOException If there is an error when trying to read from the <code>DataSource</code>.
     */
    public void start() throws IOException;

    /**
     * Signals that data is going to stop being read from the <CODE>Demultiplexer</CODE>.
     * After the <CODE>stop</CODE> method is called, <code>readFrame</code> will not be called again unless  
     * <code>start</code> is called first.
     */
    public void stop();

    /**
     *<p>
     * Retrieves the individual tracks that the media stream contains.
     * A stream can contain multiple media tracks, such as separate tracks for
     * audio, video, and midi data. The information specific to a track
     * is abstracted by an instance of a class that implements the <code>Track</code> interface.
     * The <code>Track</code> interface also provides methods for enabling or disabling
     * a track.
     * <p>
     * When <CODE>getTracks</CODE> is called, the stream header is read and
     * parsed (if there is one), the track information is retrieved,
     * the maximum frame size for each track is computed, and 
     * the play list is built (if applicable).
     * @return 
     * An array of <code>Track</code> objects. The length of the array
     * is equal to the number of tracks in the stream.
     * @exception BadHeaderException If the header information
     * is incomplete or inconsistent.
     * @exception IOException If there is an error when trying to read from the <code>DataSource</code>.
     */
    public Track[] getTracks() throws IOException, BadHeaderException;


    /**
     * Checks whether or not the stream can be repositioned
     * to the beginning.
     * @return <CODE>true</CODE> if the stream can be repositioned, <CODE>false</CODE> if it cannot.
     */
    public boolean isPositionable();
    
    /**
     * Checks whether or not the stream can be positioned at any <CODE>Time</CODE>.
     * If <CODE>isRandomAccess</CODE> returns <CODE>true</CODE>, then the stream is also positionable (<CODE>isPositionable</CODE> returns <CODE>true</CODE>).
     * However, a stream can be positionable but not random access--the <CODE>isPositionable</CODE> method might return <CODE>true</CODE> even
     * if <CODE>isRandomAccess</CODE> returns <CODE>false</CODE>.
     * @return <CODE>true</CODE> if the stream is a random access stream, <CODE>false</CODE> if it is not.
     */
    public boolean isRandomAccess();

    /**
     * Sets the stream position (media time) to the specified <CODE>Time</CODE>.
     * Returns the rounded position that was actually set.
     * Implementations should set the position to a key frame, if possible.
     * @param time The new stream position, specified as a <CODE>Time</CODE>.
     * @param round The rounding technique to be used: <CODE>RoundUp</CODE>, <CODE>RoundDown</CODE>, or <CODE>RoundNearest</CODE>.
     * @return The actual stream position that was set as a <CODE>Time</CODE> object.
     */
    Time setPosition(Time where, int rounding);

    /**
     * Gets the current media time. This is the stream position that the next <CODE>readFrame</CODE> will read.
     * @return The current position in the media stream as a <CODE>Time</CODE> object.
     */
    public Time getMediaTime();

    /**
     * Gets the duration of this media stream
     * when played at the default rate.
     * <p>
     * Note that each track can have a different duration and a
     * different start time. This method returns
     * the total duration from when the first track starts and the last track ends.
     * @return A <CODE>Time</CODE> object that represents the duration
     * or <CODE>DURATION_UNKNOWN</CODE> if the duration cannot be determined.
     *
     */
    public Time getDuration();

}
