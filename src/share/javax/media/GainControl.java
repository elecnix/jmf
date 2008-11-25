/*
 * @(#)GainControl.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * <code>GainControl</code> is an interface for manipulating audio signal
 * gain.
 *
 * <h3>Gain and Gain Measures</h3>
 * Gain is a multiplicative value applied to
 * an audio signal that modifies the amplitude
 * of the signal. 
 * This interface allows the gain to be specified in either decibels
 * or using a floating point value that varies between 0.0 and 
 * 1.0.
 *
 * <h4>Specifying Gain in Decibels</h4>
 * The decibel scale is valid over all <code>float</code>
 * values. A gain of 0.0 dB implies that the audio signal
 * is neither amplified nor attenuated. Positive values
 * amplify the audio signal, negative values attenuate
 * the audio signal. The relationship between a linear
 * gain multiplier and the gain specified in decibels is:
 * <p>
 *
 *<CENTER><CODE> value = pow(10.0, gainDB/20.0) </CODE></CENTER>
 *
 *<h4>Specifying Gain in the Level Scale</h4>
 * The level scale ranges from 0.0 to 1.0, where 0.0 represents
 * a gain that is virtually indistinguishable from silence and
 * 1.0 represents the value that is, in some sense,
 * the maximum gain. In other words, 1.0 represents the highest gain value that 
 * produces "useful" results. The mapping for
 * producing a linear multiplicative value is
 * implementation dependent.
 *
 * <h4>Decibel and Level Interactions</h4>
 * The dB and level scales are representations
 * of the same gain value. Calling <code>setLevel</code> 
 * will affect subsequent <code>getDB</code> invocations.
 * Level and dB are interrelated in the following ways:
 *
 * <ul>
 * <li>Level Silence Threshold. After <code>setLevel(0.0)</code>, <code>getDB</code> 
 * returns the value for which smaller values are not usefully 
 * distinguishable from silence.
 * Calling <code>setDB</code> with values equal to or less than this silence
 * threshold causes <code>getLevel</code> to return a value of 0.0.
 *
 * <li>Level Maximum Threshold. After <code>setLevel(1.0)</code>, <code>getDB</code>
 * returns the value for which  larger values are not useful. 
 * Calling <code>setDB</code> with values equal to or greater than this
 * threshold causes <code>getLevel</code> to return a value of 1.0.
 *
 * <li>The decibel interface is not limited to the thresholds
 * described by the level interface.
 * For example, if you call <code>setDB</code> with a value that is greater than the
 * maximum level threshold and then immediately call 
 * <code>getDB</code>,  <code>getDB</code>  returns the gain 
 * that was returned by the <code>setDB</code>,   <i>not</i> the value that
 * would be returned if you called <code>setLevel(1.0)</code> and then
 * called <code>getDB</code>.
 * 
 * <li>Both measures increase gain monotonically with increasing
 *  measure values.
 *  </ul>
 *
 * <h4>Defaults</h4>
 * Gain defaults to a value of 0.0 dB. The corresponding level
 * is implementation dependent. Note that for some implementations,
 * the default level might change on a per-instance basis.
 *
 * <h3>Mute</h3>
 * Muting is independent of the gain. If mute is <CODE>true</CODE>,
 * no audio signal is produced by this object; if mute is <CODE>false</CODE>
 * an audio signal is produced and the gain is applied to the
 * signal.
 *
 * <h3>Gain Change Events</h3>
 * When the state of the <code>GainControl</code> changes, a
 * <code>GainChangeEvent</code> is posted.
 * This event is delivered through an object
 * that implements <code>GainChangeListener</code> and has been registered as a listener
 * with the <code>GainControl</code> using <code>addGainChangeListener</code>.

 * @see GainChangeEvent
 * @see GainChangeListener
 * @see Control
 * @version 1.2, 02/08/21
*/

public interface GainControl extends Control {
    
    /**
     * Mute or unmute the signal associated with this <code>GainControl</code>.
     *
     * Calling <code>setMute(true)</code> on
     * an object that is already muted is ignored, as is calling
     * <code>setMute(false)</code> on an object that is not currently muted.
     * Going from a muted to an unmuted state doesn't effect the
     * gain.
     *
     * @param mute Specify <CODE>true</CODE> to mute the signal, <CODE>false</CODE> to unmute the signal.
    */
    public void setMute(boolean mute);

    /**
     * Get the mute state of the signal associated with this 
     * <code>GainControl</code>.
     *
     * @return The mute state.

    */
     public boolean getMute();

    
    /**
     * Set the gain in decibels.
     * Setting the gain to 0.0 (the default) implies that the audio
     * signal is neither amplified nor attenuated.
     * Positive values amplify the audio signal and negative values attenuate
     * the signal.
     *
     * @param gain The new gain in dB.
     * @return The gain that was actually set.
     */
    public float setDB(float gain);
    
    /**
     * Get the current gain set for this object in dB.
     * @return The gain in dB.
    */
    public float getDB();

    /**
     * Set the gain using a floating point scale
     * with values between 0.0 and 1.0.
     * 0.0 is silence; 1.0 is the loudest
     * useful level that this <code>GainControl</code> supports.
     *
     * @param level The new gain value specified in the level scale.
     * @return The level that was actually set.
     */
    public float setLevel(float level);

    /**
     * Get the current gain set for this
     * object as a value between 0.0 and 1.0 
     *
     * @return The gain in the level scale (0.0-1.0).
    */
    public float getLevel();


    /**
     * Register for gain change update events.
     * A <code>GainChangeEvent</code> is posted when the state
     * of the <code>GainControl</code> changes.
     *
     * @param listener The object to deliver events to.
    */
    public void addGainChangeListener(GainChangeListener listener);

    /**
     * Remove interest in gain change update events.
     *
     * @param listener The object that has been receiving events.
    */
    public void removeGainChangeListener(GainChangeListener listener);
    
}
