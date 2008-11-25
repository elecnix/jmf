/*
 * @(#)Clock.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 *
 * The <code>Clock</code> interface is implemented by objects that support
 * the Java Media time model. 
 * For example, this interface might be implemented by an object that 
 * decodes and renders MPEG movies.
 *
 * <p>
 *
 * <h2>Clock and TimeBase</h2>
 *
 * <p>
 * A <code>Clock</code> contains a <code>TimeBase</code> that provides a source of time, 
 * much like a crystal oscillator. The only information that a <code>TimeBase</code> provides is 
 * its current time; it does not provide any methods for influencing how time is kept.  
 * 
 * A <code>Clock</code> defines a transformation on the time that its <code>TimeBase</code> keeps, typically marking
 * time for a particular media stream. The time that a <code>Clock</code> keeps is referred to as the <i>media time</i>.
 *
 * <p>
 * <h3>Clock Transform</h3>
 * 
 * The transformation that a <code>Clock</code> defines on a <code>TimeBase</code>
 * is defined by three parameters: rate, <i>media start-time</i> (mst), and 
 * <i>time-base start-time</i> (tbst). 
 * Given a <i>time-base time</i> (tbt),
 * the <i>media time</i> (mt) can be calculated
 * using the following transformation:
 * <p>
 * 
 * <CENTER><CODE>mt =  mst + (tbt - tbst)*rate </CODE></CENTER>
 * <p>
 * 
 * The rate is simply a scale factor that is applied 
 * to the <code>TimeBase</code>.
 * For example, a rate of 2.0 indicates that the <code>Clock</code>
 * will run at twice the rate
 * of its <code>TimeBase</code>.  Similarly, a negative rate indicates that
 * the <code>Clock</code> runs in the opposite direction of its <code>TimeBase</code>.
 * <p>
 * 
 * The <i>time-base start-time</i> and the
 * <i>media  start-time</i> define a common point in time
 * at which the <code>Clock</code> and the <code>TimeBase</code> are synchronized.
 * <p>
 * 
 * <h3>Default Time Base</h3>
 * 
 * A <code>Clock</code> has a default <code>TimeBase</code>.
 * For many objects that support the <code>Clock</code> interface, the default
 * <code>TimeBase</code> is the system <code>TimeBase</code>. 
 * The system <code>TimeBase</code> can be obtained from <code>Manager</code> through
 * the  <code>getSystemTimeBase</code> method. 
 * <p>
 * 
 * Some <code>Clocks</code> have a <code>TimeBase</code> other than 
 * the system <code>TimeBase</code>. For example, an audio renderer that implements the <code>Clock</code>
 * interface might have a <code>TimeBase</code> that represents a
 * hardware clock.
 * <p>
 * 
 * 
 * <h2>Using a Clock</h2>
 * 
 * You can get the <code>TimeBase</code> associated with a <code>Clock</code> by calling the <code>getTimeBase</code> method. To change the 
 * <CODE>TimeBase</CODE> that a <CODE>Clock</CODE> uses, you call the <code>setTimeBase</code> method.
 * These get and set methods can be used together to synchronize different <code>Clocks</code> to the
 * same <code>TimeBase</code>.
 * <p>
 * For example, an application might want to force a video renderer to sync to the <code>TimeBase</code> of an audio renderer. To do this,
 * the application would call <code>getTimeBase</code> on the audio renderer and then use the value returned to call <code>setTimeBase</code> on the video renderer.
 * This would ensure that the two rendering objects use the same source of time. 
 *
 * You can reset a <CODE>Clock</CODE> to use its default <CODE>TimeBase</CODE> by
 * calling <CODE>setTimeBase(null)</CODE>.
 * <p>
 *
 * Some <code>Clocks</code> are incapable of using another <code>TimeBase</code>.
 * If this is the case, an <code>IncompatibleTimeBaseException</code> is thrown when <code>setTimeBase</code> 
 * is called.
 * <p>
 * 
 * <code>Clock</code> also provides methods for getting and setting a <code>Clock's</code>&nbsp;<i>media time</i> and rate: 
 * <ul>
 * <li><code>getMediaTime</code> and <code>setMediaTime</code>
 * <li><code>getRate</code> and <code>setRate</code>
 * </ul>
 * <a name="start">
 * <h3>Starting a Clock</h3></a>
 *
 * Until a <code>Clock's</code>&nbsp;<code>TimeBase</code> transformation takes effect, the <code>Clock</code> is 
 * in the <i>Stopped</i> state. Once all three transformation parameters (<i>media start-time</i>, 
 * <i>time-base start-time</i>, and rate)  have been provided 
 * to the <code>Clock</code>, it enters the <i>Started</i> state.
 * <p>
 *
 * To start a <code>Clock</code>, <code>syncStart</code> is called with 
 * the <i>time-base start-time</i> as an argument.
 * The new <i>media start-time</i> is taken as the current
 * <i>media time</i>, and the current rate defines the <code>Clock's</code> rate parameter.
 * When <code>syncStart</code> is called, the <code>Clock</code> and its <code>TimeBase</code> are
 * locked in sync and the <code>Clock</code> is considered to be in the <i>Started</i> state.
 * <p>
 *
 * When a <code>Clock</code> is stopped and then restarted (using <code>syncStart</code>), 
 * the <i>media start-time</i>
 * for the restarted <code>Clock</code> is the current <i>media time</i>.
 *
 * The <code>syncStart</code> method is often used to synchronize
 * two <code>Clocks</code> that share the same <code>TimeBase</code>. 
 * When the <i>time-base start-time</i> and rate of each clock are set to the same values and 
 * each <code>Clock</code> is set with the appropriate <i>media start-time</i>,
 * the two <code>Clocks</code> will run in sync.
 * <p>
 *
 * When <code>syncStart</code> is called with a
 * new <i>time-base start-time</i>,
 * the synchronization with the <i>media time</i> doesn't occur
 * until the <code>TimeBase</code>
 * reaches the <i>time-base start-time</i>.
 * The <code>getMediaTime</code> method returns
 * the untransformed <i>media time</i> until the <code>TimeBase</code>
 * reaches the <i>time-base start-time</i>. 
 * <p>
 * The <CODE>getSyncTime</CODE> method behaves slightly differently.
 * Once <CODE>syncStart</CODE> is invoked, <CODE>getSyncTime</CODE>
 * always reports the transformed <i>time-base time</i>, 
 * whether or not the <i>time-base start-time</i> has been reached.
 * You can use <code>getSyncTime</code> to determine how much time remains
 * before the <i>time-base start-time</i> is reached. 
 * When the <i>time-base start-time</i> is reached,
 * both
 * <CODE>getMediaTime</CODE> and <CODE>getSyncTime</CODE>
 * return the same value.
 * <p>
 *
 * Objects that implement
 * the <code>Clock</code> interface can provide more convenient start
 * methods than <CODE>syncStart</CODE>.
 * For example, <code>Player</code> defines <code>start</code>, 
 * which should be used instead of <code>syncStart</code> 
 * to start a <code>Player</code>.
 * 
 * <h3>Stopping a Clock</h3>
 * 
 * A <i>Stopped</i>&nbsp;<code>Clock</code> is no longer synchronized to
 * its <code>TimeBase</code>. When a <code>Clock</code> is <i>Stopped</i>,
 * its <i>media time</i>  no longer moves in rate-adjusted synchronization with
 * the <i>time-base time</i> provided by its <code>TimeBase</code>.
 * <p>
 * 
 * There are two ways to explicitly stop a <code>Clock</code>: you can invoke
 * <code>stop</code> or set a <i>media stop-time</i>.
 * When <code>stop</code> is invoked, synchronization with the
 * <code>TimeBase</code> immediately stops. 
 * When a <i>media stop-time</i> is set,
 * synchronization stops when the <i>media stop-time</i> passes.
 * <p>
 *
 * A <CODE>Clock's</CODE> rate affects how its <i>media stop-time</i>
 * is interpreted.
 * If its rate is positive, the <code>Clock</code>
 * stops when the <i>media time</i> becomes
 * greater than or equal to the stop time. 
 * If its rate is negative, the <code>Clock</code> stops
 * when the <i>media time</i> becomes
 * less than or equal to the stop time.
 * <p>
 *
 * If the stop-time is set to a value that the <code>Clock</code>
 * has already passed, the <code>Clock</code> immediately stops.
 * <p>
 * 
 * Once a stop-time is set, it remains in effect until it is changed
 * or cleared.
 * To clear a stop-time, call <code>setStopTime</code>
 * with <code>Clock.RESET</code>.
 * A <CODE>Clock's</CODE> stop-time is cleared automatically when it stops.
 * <p>
 *
 * If no stop-time is ever set or if the stop-time is cleared,
 * the only way to stop the <code>Clock</code> is
 * to call the <code>stop</code> method.
 * <p>
 *
 * <h2>Clock State</h2>
 * 
 * Conceptually, a <code>Clock</code> is
 * always in one of two states: <i>Started</i> or <i>Stopped</i>.
 * A <code>Clock</code> enters the <i>Started</i> state after 
 * <code>syncStart</code> has been called and the <CODE>Clock</CODE>
 * is mapped to its <code>TimeBase</code>.
 * A <code>Clock</code> returns to the <i>Stopped</i> state immediately
 * when the <code>stop</code> method is called or the
 * <i>media time</i> passes the stop time.
 * <p>
 *
 * Certain methods can only be invoked when the <code>Clock</code> is in a
 * particular state.
 * If the <code>Clock</code> is in the wrong state when one of these methods
 * is called, an error or exception is thrown.
 * 
 * <h3>Methods Restricted to <i>Started</i> Clocks</h3>
 *
 * The <code>mapToTimeBase</code> method can only be called on a 
 * <code>Clock</code> in the <i>Started</i> state. 
 * If it is invoked on a <i>Stopped</i>&nbsp;<code>Clock</code>,
 * a <code>ClockStoppedException</code> is thrown.
 * This is because the <code>Clock</code> is not synchronized to
 * a <code>TimeBase</code> when it is <i>Stopped</i>.
 *
 * <h3>Methods Restricted to <i>Stopped</i> Clocks</h3>
 *
 * The following methods can only be called on a  
 * <code>Clock</code> in the <i>Stopped</i> state.
 * If invoked on a <i>Started</i>&nbsp;
 * <code>Clock</code>, these methods throw a <code>ClockStartedError</code>.
 * <ul>
 * <li> <code>syncStart</code>
 * <li> <code>setTimeBase</code>
 * <li> <code>setMediaTime</code>
 * <li> <code>setRate</code>
 * </ul>
 * 
 * Resetting the rate, the <i>media time</i>, the time base, or the 
 * <i>time-base start-time</i> implies a complete remapping 
 * between the <code>Clock</code> and its
 * <code>TimeBase</code> and is not allowed on
 * a <i>Started</i>&nbsp;<code>Clock</code>. 
 *
 * <h3>Methods with Additional Restrictions</h3>
 * 
 * A race condition occurs if a new <i>media stop-time</i> is set
 * when a <code>Clock</code> is already approaching a previously
 * set <i>media stop-time</i>.
 * In this situation, it impossible to guarantee when the <code>Clock</code>
 * will stop. To prevent this race condition, <code>setStopTime</code> can
 * only be set once on  a <i>Started</i>&nbsp;<code>Clock</code>.
 * A <code>StopTimeSetError</code> is thrown if <code>setStopTime</code>
 * is called and the <i>media stop-time</i> has already been set.
 * <p>
 *
 * There are no restrictions on calling <code>setStopTime</code> on a 
 * <i>Stopped</i>&nbsp;<code>Clock</code>; the stop time can always be
 * reset if  the <code>Clock</code> is <i>Stopped</i>.
 * 
 * @see TimeBase
 * @see Player
 * @version 1.3, 02/08/21
*/
public interface Clock {

    /**
     * Returned by <CODE>getStopTime</CODE> if the stop-time is unset.
     */
    static public final Time RESET = new Time(Long.MAX_VALUE);
    
    /**
     * Sets the <code>TimeBase</code> for this <code>Clock</code>.
     * This method can only be called on a 
     * <i>Stopped</i>&nbsp;<code>Clock</code>.
     * A <code>ClockStartedError</code> is thrown if
     * <code>setTimeBase</code> is called on a <i>Started</i>&nbsp;
     * <code>Clock</code>.    
     * 
     * <p>
     * A <code>Clock</code> has a default <code>TimeBase</code> that
     * is determined by the implementation. 
     * To reset a <code>Clock</code> to its default 
     * <code>TimeBase</code>, call <code>setTimeBase(null)</code>.
     * @param master The new <CODE>TimeBase</CODE> or <CODE>null</CODE> to reset the <code>Clock</code>
     * to its default <code>TimeBase</code>.
     * @exception IncompatibleTimeBaseException Thrown if
     * the <code>Clock</code> can't use the specified <code>TimeBase</code>.
     */
public void setTimeBase(TimeBase master) throws IncompatibleTimeBaseException;
    
    /**
     * Synchronizes the current <i>media time</i> to the specified 
     * <I>time-base time</I> and start the <code>Clock</code>. 
     * The <code>syncStart</code> method sets the <i>time-base start-time</i>,
     * and puts the <code>Clock</code> in the <i>Started</i> state.
     * This method can only be called on a
     * <i>Stopped</i>&nbsp;<code>Clock</code>. 
     * A <code>ClockStartedError</code> is thrown if
     * <code>setTimeBase</code> is called on a <i>Started</i>&nbsp;
     * <code>Clock</code>.  
     * 
     * @param at The <i>time-base time</i> to equate with the
     * current <i>media time</i>.
     */
    public void syncStart(Time at);
    
    /**
     * Stops the <code>Clock</code>. 
     * Calling <code>stop</code> releases the <code>Clock</code> from
     * synchronization with the <code>TimeBase</code>.
     * After this request is issued, the <code>Clock</code> is in the 
     * <i>Stopped</i> state.
     * If <code>stop</code> is called on
     * a <i>Stopped</i>&nbsp;<code>Clock</code>, the request is ignored.
     *
     */
    public void stop();

    /**
     *
     * Sets the <i>media time</i> at which you want the <code>Clock</code>
     * to stop.
     * The <code>Clock</code> will stop when its <i>media time</i>
     * passes the stop-time.
     * To clear the stop time, set it to: <code>Clock.RESET</code>.
     * <p>
     *
     * You can always call <code>setStopTime</code> on a <i>Stopped</i>&nbsp;
     * <code>Clock</code>.
     * <p>
     *
     * On a <i>Started</i>&nbsp;<code>Clock</code>, the stop-time can only
     * be set <I>once</I>.
     * 
     * A <code>StopTimeSetError</code> is thrown if <code>setStopTime</code>
     * is called and the <i>media stop-time</i> has already been set.
     *
     * @param stopTime The time at which you want the
     * <code>Clock</code> to stop, in <i>media time</i>.
    */
    public void setStopTime(Time stopTime);

    /**
     * Gets the last value successfully set by <CODE>setStopTime</CODE>.
     * 
     * Returns the constant <CODE>Clock.RESET</CODE> if no stop time is set.
     * (<CODE>Clock.RESET</CODE> is the default stop time.)
     *
     * @return The current stop time. 
     */
    public Time getStopTime();
    
    /**
     * Sets the <code>Clock's</code>&nbsp;<i>media time</i>.
     * This method can only be called on
     * a <i>Stopped</i>&nbsp;<code>Clock</code>.  
     * A <code>ClockStartedError</code> is thrown if
     * <code>setMediaTime</code> is called on a <i>Started</i>&nbsp;
     * <code>Clock</code>.
     * 
     * @param now The new media time.
     */
    public void setMediaTime(Time now);

    /**
     * Gets this <code>Clock's</code> current <i>media time</i>.
     * A <i>Started</i>&nbsp;<code>Clock's</code>&nbsp;<i>media time</i>
     * is based on  its <code>TimeBase</code>
     * and rate, as described in <a href="#start"><I>Starting a Clock</I></a>.
     *
     * @return The current <i>media time</i>.
     */
    public Time getMediaTime();

    /**
     * Gets this <code>Clock's</code> current <i>media time</i>
     * in nanoseconds.
     * 
     * @return The current <i>media time</i> in nanoseconds.
     */
    public long getMediaNanoseconds();
    
    /**
     * Gets the current <i>media time</i> or the time until this
     * <code>Clock</code> will synchronize to its <code>TimeBase</code>.
     * The <code>getSyncTime</code> method is used by <code>Players</code> and
     * advanced applet writers to  synchronize <code>Clocks</code>.
     * <p>
     *
     * Like <code>getMediaTime</code>, this method returns 
     * the <code>Clock's</code> current <i>media time</i>,
     * which is based on its <code>TimeBase</code> and rate.
     *
     * However, when <code>syncStart</code> is used to start
     * the <code>Clock</code>, <code>getSyncTime</code> performs a countdown
     * to the time-base start-time, returning the time remaining until
     * the <i>time-base start-time</i>.
     * Once the <code>TimeBase</code> reaches the
     * <i>time-base start-time</i>,&nbsp;<code>getSyncTime</code>
     * and <code>getMediaTime</code> will return the same value.<p>
     *
     */
    public Time getSyncTime();

    /**
     * Gets the <code>TimeBase</code> that this <code>Clock</code> is using.
     */
    public TimeBase getTimeBase();

    /**
     * Gets the <code>TimeBase</code> time corresponding to the specified <i>media time</i>.
     *
     * @exception ClockStoppedException Thrown if <CODE>mapToTimeBase</CODE> is called on a <i>Stopped</i>&nbsp;
     * <code>Clock</code>.
     * 
     * @param t The <i>media time</i> to map from.
     *
     * @return The <I>time-base time</I> in <I>media-time</I> coordinates.
     */
    public Time mapToTimeBase(Time t) throws ClockStoppedException;

    /**
     * Gets the current temporal scale factor.
     * The scale factor defines the relationship
     * between the <code>Clock's</code>&nbsp;<i>media time</i> 
     * and its <code>TimeBase</code>.<p>
     *
     * For example, a rate of 2.0 indicates that <i>media time</i>
     * will pass twice as fast as the <code>TimeBase</code> time once
     * the <code>Clock</code>
     * starts.  Similarly, a negative rate indicates that
     * the <code>Clock</code> runs in the opposite direction of its <code>TimeBase</code>. 
     * All <code>Clocks</code> are
     * guaranteed to support a rate of 1.0, the default rate. <code>Clocks</code>  are not required 
     * to support any other rate.<p>
    */
    public float getRate();
 
    /**
     * Sets the temporal scale factor.
     * The argument <i>suggests</i> the scale factor to use.<p>
     * 
     * The <code>setRate</code> method returns the actual rate set by the
     * <code>Clock</code>.  <code>Clocks</code> should set their rate as close to 
     * the requested
     * value as possible, but are not required to set the rate to the exact
     * value of any argument other than 1.0. A <code>Clock</code> is only guaranteed to set
     * its rate exactly to 1.0.
     * <p>
     * 
     * You can only call this method on a <i>Stopped</i>&nbsp;<code>Clock</code>. A
     * <code>ClockStartedError</code> is thrown if <code>setRate</code> is called on a <i>Started</i>&nbsp;<code>Clock</code>.<p>
     * 
     * @param factor The temporal scale factor (rate) to set.
     * @return The actual rate set.
     */
    public float setRate(float factor);

}
