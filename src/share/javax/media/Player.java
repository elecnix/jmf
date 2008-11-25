/*
 * @(#)Player.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import java.awt.Component;
import javax.media.protocol.DataSource;
import java.io.IOException;

/**
 * <code>Player</code> is a <code>MediaHandler</code> for rendering
 * and controlling time based media data.
 * <code>Player</code> extends the <code>Controller</code> interface.
 * <code>Player</code> provides methods for
 * obtaining AWT components, media processing controls, and a way to
 * manage other <code>Controllers</code>.  
 *
 * <h2>How a Player Differs from a Controller</h2>
 * 
 * <code>Player</code> relaxes some restrictions that a
 * <code>Controller</code> imposes 
 * on what methods can be called on a <i>Started</i> 
 * <code>Controller</code>.
 * It also provides a way to manage groups of <CODE>Controllers</CODE>.
 *
 * <h3>Methods Restricted to <i>Stopped</i>&nbsp;Players</h3>
 *
 * The following methods cannot be invoked on a <i>Started</i> 
 * <CODE>Player</CODE>. If they are, <code>ClockStartedError</code> 
 * is thrown.
 * <ul>
 * <li> <code>setTimeBase</code>
 * <li> <code>syncStart</code>
 * <li> <code>deallocate</code>
 * <li> <code>addController</code>
 * <li> <code>removeController</code>
 * </ul>
 * <p>
 * 
 * <h3>Methods Allowed on <i>Started</i>&nbsp;Players</h3>
 * Unlike a <code>Controller</code>, the following methods are <i>legal</i> on 
 * a <code>Player</code> in the <i>Started</i> state:
 * <ul>
 * <li> <code>setMediaTime</code>
 * <li> <code>setRate</code>
 * </ul>
 * Invoking these methods on a <i>Started</i>&nbsp;<code>Player</code> might
 * initiate significant and time-consuming processing, depending
 * on the location and type of media being processed.
 * 
 * These methods might also cause the state of the <code>Player</code> to
 * change.
 * If this happens, the appropriate <code>TransitionEvents</code> are posted 
 * by the <code>Player</code> when its state changes.
 * <p>
 * 
 * For example, a <code>Player</code> might have to enter
 * the <i>Prefetching</i> state to process a <code>setMediaTime</code>
 * invocation.
 * In this case, the <code>Player</code> posts a <code>RestartingEvent</code>,
 * a <code>PrefetchCompleteEvent</code>, and a <code>StartEvent</code> as 
 * it moves from the <i>Started</i> state to <i>Prefetching</i>, back to 
 * <i>Prefetched</i>, and finally back to the <i>Started</i> state.
 * 
 * <h3>Methods that are Illegal on <i>Unrealized</i> Players</h3>
 * 
 * As with <code>Controller</code>, it is illegal to call the following methods
 * on  an <i>Unrealized</i>&nbsp;<code>Player</code>:
 * <ul>
 * <li> <code>getTimeBase</code>
 * <li> <code>setTimeBase</code>
 * <li> <code>setMediaTime</code>
 * <li> <code>setRate</code>
 * <li> <code>setStopTime</code> 
 * <li> <code>getStartLatency</code>
 * </ul>
 * <p>
 * It is also illegal to call the following <code>Player</code> methods on an 
 * <i>Unrealized</i>&nbsp;<code>Player</code>:
 * <ul>
 * <li> <code>getVisualComponent</code>
 * <li> <code>getControlPanelComponent</code> 
 * <li> <code>getGainControl</code>
 * <li> <code>addController</code>
 * <li> <code>removeController</code>
 * </ul>
 * <p>
 * 
 * The <code>Player</code> throws a <code>NotRealizedError</code>
 * if any of these methods are called while the <code>Player</code>
 * is in the <i>Unrealized</i> state.
 *
 * <h3>Start Method </h3>
 *
 * As a convenience, <code>Player</code> provides a <code>start</code>
 * method that can be invoked before a <code>Player</code>
 * is <i>Prefetched</i>.
 * This method attempts to transition the <code>Player</code> to
 * the <i>Started</i> state from whatever state it's currently in.
 * For example, if the <code>Player</code> is <i>Unrealized</i>,
 * <code>start</code> implicitly calls <code>realize</code>,
 * <code>prefetch</code>, and <code>Clock.syncStart</code>.
 * The appropriate <code>TransitionEvents</code> are posted as
 * the <code>Player</code> moves through each state on its way
 * to <i>Started</i>.
 *
 * <h3>RestartingEvent</h3>
 *
 * If <code>setMediaTime</code> or <code>setRate</code> cause a perceptible
 * delay  in the presentation of the media, the <code>Player</code> posts a 
 * <code>RestartingEvent</code> and transitions to the <i>Prefetching</i>
 * state.
 * The previous state and target state of a <code>RestartingEvent</code>  
 * is always <i>Started</i>. <code>RestartingEvent</code> is a subclass
 * of <code>StopEvent</code>.
 *
 * <h3>DurationUpdateEvent</h3>
 * 
 * Because a <code>Player</code> cannot always know the duration of the media
 * it is playing, the <code>Duration</code> interface defines that
 * <code>getDuration</code> returns <code>Duration.DURATION_UNKNOWN</code>
 * until the duration can be determined.
 * A <code>DurationUpdateEvent</code> is generated when the <code>Player</code>
 * can determine its duration or the if its duration
 * changes, which can happen at any time. When the end of the media
 * is reached, the duration should be known.
 *
 * <h2>Managing other Controllers</h2>
 *
 * In some situations, an application might want to use a single
 * <code>Player</code> to
 * control other <code>Players</code> or <code>Controllers</code>.
 * A single controlling <code>Player</code> can be used to
 * invoke <code>start</code>, <code>stop</code>, <code>setMediaTime</code>,
 * and other methods on the entire group. The controlling
 * <code>Player</code> manages all of the state transitions and event posting.
 * <p> 
 * It is also possible to construct a simple <code>Controller</code>
 * to update animations, report on media time-line progress, or
 * provide other timing-related functions. Such <code>Controllers</code> can 
 * operate in sync with a controlling <code>Player</code>.
 *
 * <h3>Adding a Controller</h3>
 *
 * To have a <CODE>Player</CODE> assume control over a <code>Controller</code>,
 * use the <code>addController</code> method.
 * A <code>Controller</code> can not be added to a <i>Started</i>&nbsp;
 * <code>Player</code>. If <code>addController</code> is called on
 * a <i>Started</i>&nbsp;<code>Player</code>,
 * a  <code>ClockStartedError</code> is thrown.
 * An <i>Unrealized</i>&nbsp or <code>Started</code>;<code>Controller</code> 
 * cannot be added to a
 * <code>Player</code>; a <code>NotRealizedError</code> is thrown if the
 * <code>Controller</code> is <i>Unrealized</i>; 
 * a <code>ClockStartedError</code> is thrown if the <code>Controller</code>
 * is <i>Started</i>.
 * <p>
 *
 * Once a <code>Controller</code> has been added, the <code>Player</code>:
 * <ul>
 * <li>Invokes <code>setTimeBase</code> on the <code>Controller</code> with the
 * <code>Player's</code>&nbsp;<code>TimeBase</code>.
 *
 * If this fails, <code>addController</code> throws
 * an <code>IncompatibleTimeBaseException</code>.
 *
 * <li>Synchronizes the <code>Controller</code> with the <code>Player</code>
 * using <code>setMediaTime</code>, <code>setStopTime</code>,
 * and <code>setRate</code>.
 *
 * <li>Takes the added <code>Controller's</code> latency into account when
 * computing the <code>Player's</code> start latency.
 * When <code>getStartLatency</code> is called,
 * the <code>Player</code> returns the greater of:
 * its latency before the <code>Controller</code> was added and
 * the latency of the added <code>Controller</code>.
 *
 * <li>Takes the added <code>Controller's</code> duration into account when
 * computing the <code>Player's</code>
 * duration.  When <code>getDuration</code> is called,
 * the <code>Player</code> returns the greater of:
 * its duration before the <code>Controller</code> was added and
 * the duration of the added <code>Controller</code>.
 * If either of these values is DURATION_UNKNOWN,
 * <CODE>getDuration</CODE> returns DURATION_UNKNOWN.
 * If either of these values is DURATION_UNBOUNDED <CODE>getDuration</CODE>
 * returns DURATION_UNBOUNDED.
 * 
 * <li> Adds itself as a <code>ControllerListener</code> for the
 * added <code>Controller</code> so that it can
 * manage the events that the <code>Controller</code> generates.
 * (See the <a href="#events">Events</a> section below for more information.)
 *
 * <li>Invokes control methods on the added <code>Controller</code>
 * in response to methods invoked on the <code>Player</code>. The 
 * methods that affect
 * managed <code>Controllers</code> are discussed below.
 * 
 * </ul>
 *
 * Once a <code>Controller</code> has been added to a <code>Player</code>,
 * methods should only be called on the <code>Controller</code> through the
 * managing <code>Player</code>.
 * It is  not defined how the <code>Controller</code> or <code>Player</code>
 * will behave if methods are called directly on an added
 * <code>Controller</code>.
 * You cannot place a controlling <CODE>Player</CODE> under the control of a
 * <CODE>Player</CODE> that it is managing; the resulting behavior is
 * undefined.
 * <p>
 *
 * When a <code>Controller</code> is added to a
 * <code>Player</code>, the <code>Player</code>
 * does not transition the added <code>Controller</code> to
 * new state, nor does the <code>Player</code> transition itself
 * forward.
 * The <code>Player</code> either transitions back to the
 * <i>realized</i> state if the added <code>Controller</code>
 * is <i>realized</i> or <i>prefetching</i> or it stays
 * in the <i>prefetched</i> state if the both the <code>Player</code>
 * and the added <code>Controller</code> are in the <i>prefetched</i>
 * state. If the <code>Player</code> makes a state transition
 * as a result of adding a <code>Controller</code> the <code>Player</code>
 * posts a <code>TransitionEvent</code>.
 * 
 * <h3>Removing a Controller</h3>
 *
 * To stop a <code>Player</code> from managing another
 * <code>Controller</code>, call <code>removeController</code>.
 * The managing <code>Player</code> must be <i>Stopped</i> before
 * <code>removeController</code> can be called.
 * A <code>ClockStartedError</code> is thrown if <code>removeController</code>
 * is called on a <i>Started</i>&nbsp;<code>Player</code>.
 * <p>
 *
 * When a <code>Controller</code> is removed from a <code>Player's</code>
 * control, the <code>Player</code>:
 * <ul>
 * <li> Resets the <code>Controller's</code>&nbsp;<code>TimeBase</code>
 * to its default.
 * <li> Recalculates its duration and posts a
 * <code>DurationUpdateEvent</code> if the <code>Player's</code> duration
 * is different without the <CODE>Controller</CODE> added.
 * <li> Recalculates its start latency.
 * </ul>
 *
 * <h3>Setting the Media Time and Rate of a Managing Player</h3>
 *
 * When you call <code>setMediaTime</code> on a <code>Player</code> that's
 * managing other <code>Controllers</code>,
 * its actions differ depending on whether or not the <code>Player</code>
 * is <I>Started</I>.
 * If the <code>Player</code> is not <i>Started</i>, it simply
 * invokes <code>setMediaTime</code> on all of the
 * <code>Controllers</code> it's managing.
 * <p>
 *
 * If the <code>Player</code> is <i>Started</i>,
 * it posts a <code>RestartingEvent</code> and
 * performs the following tasks for each managed <code>Controller</code>:
 * 
 * <ul>
 * <li>Invokes <code>stop</code> on the <code>Controller</code>.
 * <li>Invokes <code>setMediaTime</code> on the <code>Controller</code>.
 * <li>Invokes <code>prefetch</code> on the <code>Controller</code>.
 * <li>Waits for a <code>PrefetchCompleteEvent</code> from
 * the <code>Controller</code>.
 * <li>Invokes <code>syncStart</code> on the <code>Controller</code>
 * </ul>
 *
 * <p>
 *
 * The same is true when <code>setRate</code> is called on a
 * managing <code>Player</code>.
 * The <code>Player</code> attempts to set the specified rate
 * on all managed <code>Controllers</code>, stopping and restarting
 * the <code>Controllers</code> if necessary.
 * If some of the <code>Controllers</code> do not support the requested rate,
 * the <code>Player</code> returns the rate that was actually set.
 * All <code>Controllers</code> are guaranteed to have been successfully
 * set to the rate returned. 
 * 
 * <h3>Starting a Managing Player</h3>
 * When you call <code>start</code> on a managing <code>Player</code>,
 * all of the  <code>Controllers</code> managed by
 * the <code>Player</code> are transitioned to
 * the <i>Prefetched</i> state. When the <code>Controllers</code>
 * are <i>Prefetched</i>,
 * the managing <code>Player</code> calls <code>syncStart</code>
 * with a time consistent with the latencies of each of the managed
 * <code>Controllers</code>.
 *  
 * 
 * <h3>Calling realize, prefetch, stop, or deallocate on a Managing Player</h3>
 * 
 * When you call <code>realize</code>, <code>prefetch</code>, 
 * <code>stop</code>,  or <code>deallocate</code> on a managing
 * <code>Player</code>, 
 * the <code>Player</code> calls that method on all of the 
 * <code>Controllers</code> that it is managing.
 * The <code>Player</code> moves from one state to the
 * next when all of its <code>Controllers</code> have reached that state. 
 * For example, a <code>Player</code> in the <i>Prefetching</i>
 * state does not transition into the <i>Prefetched</i>
 * state until all of its managed <code>Controllers</code>
 * are <i>Prefetched</i>.
 * The  <code>Player</code> posts <code>TransitionEvents</code> normally
 * as it changes state.
 * <p>
 *
 * <h3>Calling syncStart or setStopTime on a Managing Player</h3>
 * When you call <code>syncStart</code> or <code>setStopTime</code> on a
 * managing <code>Player</code>, the <code>Player</code>
 * calls that method  on all of the <code>Controllers</code> that it
 * is managing. (The <code>Player</code> 
 * must be in the correct state or an error is thrown.
 * For example, the <code>Player</code> must be <I>Prefetched</I>
 * before you can call <code>syncStart</code>.)
 * 
 * <h3>Setting the Time Base of a Managing Player</h3>
 * When <code>setTimeBase</code> is called on a managing <code>Player</code>,
 * the <code>Player</code> calls <code>setTimeBase</code> on all of
 * the <code>Controllers</code> it's managing. 
 * If <code>setTimeBase</code> fails on any of the <code>Controllers</code>,
 * an <code>IncompatibleTimeBaseException</code> is thrown
 * and the  <code>TimeBase</code> last used
 * is restored for all of the <code>Controllers</code>.
 * 
 * <h3>Getting the Duration of a Managing Player</h3>
 * Calling <code>getDuration</code> on a managing <code>Player</code>
 * returns the maximum duration of all of the added
 * <code>Controllers</code> and the managing <code>Player</code>.
 * If the <CODE>Player</CODE>  or any <CODE>Controller</CODE>
 * has not resolved its duration, <code>getDuration</code>
 * returns <code>Duration.DURATION_UNKNOWN</code>.
 *
 * <h3> Closing a Managing Player</h3>
 * When <code>close</code> is called on a managing <code>Player</code>
 * all managed <code>Controllers</code> are closed as well.
 * 
 * <a name="events"><h3>Events</h3></a>
 * Most events posted by a managed <code>Controller</code> are filtered
 * by the managing <code>Player</code>. Certain events are sent directly
 * from the <code>Controller</code> through the <code>Player</code> and to the
 * listeners registered with the <code>Player</code>.
 * <p>
 *
 * To handle the events that a managed <code>Controller</code> can generate,
 * the <code>Player</code> registers a listener with the
 * <code>Controller</code> when it is added.
 * Other listeners that are registered with the <code>Controller</code>
 * must be careful not to invoke methods on the <code>Controller</code>
 * while it is being managed by the <code>Player</code>.
 * Calling a control method on a managed <code>Controller</code> directly
 * will produce unpredictable results.
 * <p>
 *
 * When a <CODE>Controller</CODE> is removed from the <code>Player's</code>
 * list of managed <code>Controllers</code>,
 * the <code>Player</code> removes itself from the <code>Controller's</code>
 * listener list.
 * 
 * <h4>Transition Events</h4>
 * A managing <code>Player</code> posts <code>TransitionEvents</code> normally 
 * as it moves between states, but
 * the managed <code>Controllers</code> affect when the <code>Player</code>
 * changes state.
 * In general, 
 * a <code>Player</code> does not post a transition event until all of its
 * managed <code>Controllers</code> have posted the event.
 * 
 * <h4>Status Change Events</h4>
 * The managing <code>Player</code> collects the
 * <code>RateChangeEvents</code>, <code>StopTimeChangeEvents</code>,
 * and <code>MediaTimeSetEvents</code> posted by its
 * managed <code>Controllers</code> and posts a single event for the group.
 * 
 * <h4>DurationUpdateEvent</h4>
 * A <code>Player</code> posts a <code>DurationUpdateEvent</code> when
 * it determines its duration or its duration changes.
 * A managing <code>Player's</code> duration might change if a managed
 * <code>Controller</code> updates or discovers its duration.
 * In general, if a managed <code>Controller</code> 
 * posts a <code>DurationUpdateEvent</code> and the new duration
 * changes the managing <code>Player's</code> duration,
 * the <code>Player</code> posts a <code>DurationUpdateEvent</code>
 *
 * <h4>CachingControlEvent</h4>
 * A managing <CODE>Player</CODE> reposts <CODE>CachingControlEvents</CODE>
 * received from a <CODE>Players</CODE> that it manages, but otherwise
 * ignores the events.
 *
 * <h4>ControllerErrorEvents</h4>
 * A managing <CODE>Player</CODE> immediately reposts
 * any <CODE>ControllerErrorEvent</CODE> received from a
 * <CODE>Controller</CODE> that it is managing.
 * After a <CODE>ControllerErrorEvent</CODE> has been
 * received from a managed <CODE>Controller</CODE>, a
 * managing <CODE>Player</CODE> no longer invokes any methods
 * on the managed <CODE>Controller</CODE>; the
 * managed <CODE>Controller</CODE> is ignored from that point on.
 * 
 * @see Manager
 * @see GainControl
 * @see Clock 
 * @see TransitionEvent
 * @see RestartingEvent
 * @see DurationUpdateEvent
 * @see java.awt.Component
 *
 * @version 1.7, 02/08/21
 */

public interface Player extends MediaHandler, Controller {

    /**
     * Gets the display <code>Component</code> for this <code>Player</code>.
     * The display <code>Component</code> is where visual media
     * is rendered.
     * If this <code>Player</code> has no visual component,
     * <code>getVisualComponent</code> returns <CODE>null</CODE>.
     * For example, <code>getVisualComponent</code> might return
     * <CODE>null</CODE> if the <code>Player</code> only plays audio.
     *
     *
     * @return The media display <code>Component</code> for this
     * <code>Player</code>.
    */
    public Component getVisualComponent();

    /**
     * Gets the object for controlling this <code>Player's</code>
     * audio gain. 
     * If this player does not have a 
     * <code>GainControl</code>, <code>getGainControl</code> returns
     * <CODE>null</CODE>.  
     * For example, <code>getGainControl</code> might return
     * <CODE>null</CODE> if the <code>Player</code> does not play audio data.
     *
     * @return The <code>GainControl</code> object for this
     * <code>Player</code>.
    */
    public GainControl getGainControl();

    /**
     * Gets the <code>Component</code> that provides the default user
     * interface for controlling this <code>Player</code>.
     * If this <code>Player</code> has no default control panel,
     * <code>getControlPanelComponent</code> returns <CODE>null</CODE>.
     *
     * @return The default control panel GUI for this <code>Player</code>.
     */
    public Component getControlPanelComponent();
    
    /**
     * Starts the <code>Player</code> as soon as possible.
     * The <CODE>start</CODE> method attempts to transition the
     * <code>Player</code> to the <i>Started</i> state.
     * If the <CODE>Player</CODE> has not been <i>Realized</i> or
     * <i>Prefetched</i>, <code>start</code> automatically performs
     * those actions. The appropriate events
     * are posted as the <code>Player</code> moves through each state.
     */
    public void start();

    /**
     * Tells the <CODE>Player</CODE> to assume control of another <code>Controller</code>.
     *
     * @param newController The <code>Controller</code> to be managed.
     *
     * @exception IncompatibleTimeBaseException Thrown if the added
     * <code>Controller</code> cannot take this     
     * <code>Player's</code>&nbsp;<CODE>TimeBase</CODE>.
     */
    public void addController(Controller newController)
	throws IncompatibleTimeBaseException;

    /**
     * Tells the <CODE>Player</CODE> to stop controlling a <code>Controller</code>.
     *
     * @param oldController The <code>Controller</code> to stop managing.
     */
    public void removeController(Controller oldController);

}
