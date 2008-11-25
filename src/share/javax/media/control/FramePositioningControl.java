/*
 * @(#)FramePositioningControl.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

import javax.media.Time;
import javax.media.Track;

/**
 * The <code>FramePositioningControl</code> is the interface to control 
 * precise positioning to a video frame for <code>Players</code> 
 * and <code>Processors</code>.
 * <p>
 * Frame numbers for a bounded movie should generally begin with 0,
 * corresponding to media time 0.  Each video frame of a movie
 * should have a unique frame number that is one bigger than the
 * previous frame. 
 * <p>
 * There is a direct mapping between the frame number and the media
 * time of a video frame; although not all <code>Players</code> can
 * compute that relationship.  For <code>Players</code> that can
 * compute that relationship, the <code>mapFrameToTime</code> and
 * <code>mapTimeToFrame</code> methods can be used.
 * <p>
 * When a <code>Player</code> is seeked or skipped to a new video frame, 
 * the media time of the <code>Player</code> will be changed to the 
 * media time of the corresponding video frame.  A 
 * <code>MediaTimeSetEvent</code> will be sent.
 * @since JMF 2.0
 */
public interface FramePositioningControl extends javax.media.Control {

    public static Time TIME_UNKNOWN = Track.TIME_UNKNOWN;

    public static int FRAME_UNKNOWN = Track.FRAME_UNKNOWN;

    /**
     * Seek to a given video frame.
     * <p>
     * The <code>Player</code> (or <code>Processor</code>) will 
     * attempt to jump to the given frame.  
     * The frame that the <code>Player</code> actually jumped to will 
     * be returned.
     * <p>
     * The media time of the <code>Player</code> will be updated 
     * to reflect the new
     * position set.  A <code>MediaTimeSetEvent</code> will be sent. 
     * <p>
     * This method can be called on a stopped or started <code>Player</code>.
     * Like <code>setMediaTime</code>, if the <code>Player</code> is
     * in the <i>Started</i> state, this method may cause the
     * <code>Player</code> to change states.  If that happens, the
     * appropriate <code>TransitionEvents</code> will be posted by
     * the <code>Player</code> when its state changes.
     * <p>
     * @param frameNumber the frame to seek to.
     * @return the actual frame that the Player has seeked to. 
     */
    public int seek(int frameNumber);

    /**
     * Skip a given number of frames from the current position.
     * <p>
     * The <code>Player</code> (or <code>Processor</code>) will 
     * attempt to skip the given number 
     * of frames relative to the current position.
     * The actual number of frames skipped will be returned.
     * <p>
     * The media time of the <code>Player</code> will be updated to 
     * reflect the new
     * position set.  A <code>MediaTimeSetEvent</code> will be sent. 
     * <p>
     * This method can be called on a stopped or started <code>Player</code>.
     * If the <code>Player</code> is in the <i>Started</i> state,
     * the current position is not exact/well-defined.  Hence, the frame
     * actually skipped to will not be exact.
     * <p>
     * Like <code>setMediaTime</code>, if the <code>Player</code> is
     * in the <i>Started</i> state, this method may cause the
     * <code>Player</code> to change states.  If that happens, the
     * appropriate <code>TransitionEvents</code> will be posted by
     * the <code>Player</code> when its state changes.
     * <p>
     * @param framesToSkip the number of frames to skip from the current
     *   position.  If framesToSkip is positive, it will seek forward
     *   by framesToSkip number of frames.  If framesToSkip is negative, 
     *   it will seek backward by framesToSkip number of frames.
     *   e.g. skip(-1) will step backward one frame.
     * @return the actual number of frames skipped.  
     */
    public int skip(int framesToSkip);

    /**
     * Converts the given frame number to the corresponding media time.
     * <p>
     * @param frameNumber the input frame number for the conversion.
     * @return the converted media time for the given frame.  If the
     *   conversion fails, TIME_UNKNOWN is returned.
     */
    public Time mapFrameToTime(int frameNumber);

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
    public int mapTimeToFrame(Time mediaTime);
}


