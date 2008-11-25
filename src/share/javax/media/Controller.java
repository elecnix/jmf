/*
 * @(#)Controller.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 *
 * The <code>Controller</code> interface, which extends <code>Clock</code>,
 * provides resource-allocation
 * state information, event generation, and a mechanism for obtaining objects
 * that provide additional control over a <code>Controller</code>.
 *
 * <h2>Controller Life Cycle</h2>
 *
 *
 * As a <code>Clock</code>, a <code>Controller</code> is always either
 * <I>Started</I> or <I>Stopped</I>. 
 * However, <code>Controller</code> subdivides
 * <code>Clock's</code>&nbsp;<I>Stopped</I> state into five
 * resource-allocation phases:
 * <i>Unrealized</i>, <i>Realizing</i>, <i>Realized</i>,
 * <i>Prefetching</i>, and <i>Prefetched</i>.
 * <p>
 *
 * The purpose of these life-cycle states is to provide
 * programmatic control over potentially time-consuming operations.
 * For example, when a <code>Controller</code> is first constructed, it's in 
 * the <i>Unrealized</i> state.
 * While <I>Realizing</I>, the <code>Controller</code> 
 * performs the communication necessary to locate all of the resources
 * it needs to  function (such as communicating with a server,
 * other controllers, or a file system).
 * The <code>realize</code> method allows an application to initiate this
 * potentially time-consuming process (<i>Realizing</i>) at an
 * appropriate time.
 * When a <code>Controller</code> is <i>Realizing</i> or <i>Prefetching</i>,
 * it will eventually transition to another state, such as <i>Realized</i>,
 * <i>Prefetched</i>, or even <i>Unrealized</i>.
 * <p>
 * Because a <code>Controller</code> is often in one state and on its way to
 * another, its destination or <i>target</i> state is an integral part
 * of the <code>Controller</code> life cycle.
 * You can query a <code>Controller</code> to determine both its
 * current state and its target state.
 * <p>
 *
 * A <code>Controller</code> typically moves from the <i>Unrealized</i> state
 * through <i>Realizing</i> to the <i>Realized</i> state,
 * then through <i>Prefetching</i>
 * to the <i>Prefetched</i> state, and finally on to the <i>Started</i> state.
 * When a <code>Controller</code> finishes because
 * the end of the  media stream is reached,
 * its stop time is reached, 
 * or the <code>stop</code> method is invoked,
 * the <code>Controller</code> moves from the <i>Started</i> state
 * back to <i>Prefetched</i>
 * or possibly back to <i>Realized</i>, and is ready to 
 * repeat the cycle.
 * <p>
 *
 * To use a <code>Controller</code>, you must set up parameters to
 * manage its movement through these life-cycle states and then
 * move it through the states using the <code>Controller</code>
 * state transition methods. 
 * To keep track of the <code>Controller's</code> current state,
 * you monitor the state transition events that it posts when changing states.
 *
 * <h3>State Transition Methods</h3>
 *
 * A <code>Controller</code> has five methods that are used to induce
 * life cycle state changes:
 * <code>realize</code>, <code>prefetch</code>, <code>deallocate</code>,
 * <code>syncStart</code>, and <code>stop</code>.
 *
 * To transition a <code>Controller</code> to the <i>Realized</i>,
 * <i>Prefetched</i>, or <i>Started</i> state,
 * you use the corresponding method: <code>realize</code>, 
 * <code>prefetch</code>, or <code>syncStart</code>. The
 * <code>deallocate</code> and <code>stop</code> methods can change
 * a requested state transition or trigger a state change.
 * <p>
 *
 * The forward transition methods (<code>realize</code>,
 * <code>prefetch</code>, and <code>syncStart</code>) are executed
 * asynchronously and return immediately.
 * When the requested operation is complete, the <code>Controller</code>
 * posts a <code>ControllerEvent</code>
 * indicating that the target state has been reached,
 * <code>stop</code> or <code>deallocate</code> has been invoked, or
 * that an error has occurred.
 * <p>
 *
 * The <code>deallocate</code> and <code>stop</code> methods can
 * change the target state and induce a transition back to
 * a previous state. For example, calling <code>deallocate</code> on
 * a <code>Controller</code> in the 
 * <i>Prefetching</i> state will move it back to
 * the <i>Realized</i> state.  These methods are synchronous. 
 *
 * 
 * <h3>State Transition Events</h3>
 *
 * A <code>Controller</code> often moves between states
 * in an asynchronous manner.
 * To facilitate the tracking of a <code>Controller's</code> state,
 * every time its state or target state changes,
 * the <code>Controller</code> is required to post a <code>TransitionEvent</code> that describes
 * its previous state, current state, and new target state.
 * By monitoring the <code>Controller</code> event stream, you can determine
 * exactly what a <code>Controller</code> is doing at any point in time.
 * <p>
 *
 * When one of the asynchronous forward state transition methods 
 * completes, the <code>Controller</code> posts the appropriate <code>TransitionEvent</code> 
 * or a <code>ControllerErrorEvent</code> indicating that
 * the <code>Controller</code> is no longer usable.
 * For more information about <code>ControllerEvents</code>, see the <a href="#CE"><I>Controller Events section</I></a>. 
 * <p>
 *
 * To facilitate simple asynchronous method protocols,
 * a <code>Controller</code> always posts a method completion event
 * when one of the asynchronous forward state transition methods is invoked, even 
 * if no state or target state change occurs.
 * For example, if <code>realize</code> is called on a
 * <i>Prefetching</i>&nbsp;<code>Controller</code>,
 * a <code>RealizeCompleteEvent</code>
 * is immediately posted, even though the <code>Controller</code> remains
 * in the <i>Prefetching</i> state and the target state is still
 * <i>Prefetched</i>.
 * The method completion events always report the <code>Controller's</code> previous,
 * current, and target state at the time the event was posted.
 * <p>
 *
 *
 * <h2>Controller States</h2>
 *
 * This section describes the semantics of each of the <code>Controller</code>
 * states.
 * 
 *
 * <h3>Unrealized State</h3>
 * A newly instanced <code>Controller</code> starts in
 * the <i>Unrealized</i> state.
 * An <i>Unrealized</i>&nbsp;<code>Controller</code> knows very little about its
 * internals and does not have enough information to acquire all of the resources
 * it needs to function. 
 * In particular, an <i>Unrealized</i>&nbsp;<code>Controller</code> does
 * not know enough to properly construct
 * a <code>Clock</code>. 
 * Therefore, it is illegal to call the following methods
 * on an <i>Unrealized</i>&nbsp;<code>Controller</code>:
 * <ul>
 * <li> <CODE>getTimeBase</CODE>
 * <li> <CODE>setTimeBase</CODE>
 * <li> <CODE>setMediaTime</CODE>
 * <li> <CODE>setRate</CODE>
 * <li> <CODE>setStopTime</CODE>
 * <li> <CODE>getStartLatency</CODE>
 * </ul>
 *
 * A <code>NotRealizedError</code> is thrown if any of these methods are called
 * on an <I>Unrealized</I>&nbsp;<CODE>Controller</CODE>.
 *
 * <h3>Realizing and Realized States</h3>
 *
 * A <code>Controller</code> is <i>Realized</i> when it has obtained 
 * all of the information necessary for it to acquire the resources it needs 
 * to function.
 * A <i>Realizing</i>&nbsp;<code>Controller</code> is in the process of
 * identifying the resources that it needs to acquire.
 * <i>Realizing</i> can be a resource-consuming and time-consuming process.
 * A <i>Realizing</i>&nbsp;<code>Controller</code> might have to communicate with
 * a server, read a file, or interact with a set of objects.
 * <p>
 *
 * Although a <i>Realized</i>&nbsp;<code>Controller</code> does not have to acquire
 * any resources, a <i>Realized</i>&nbsp;<code>Controller</code> is likely to have
 * acquired all of the resources it needs except those that imply exclusive use of a 
 * scarce system resource, such as an audio device or MPEG decoding hardware.
 * <p>
 *
 * Normally, a <code>Controller</code> moves from the <i>Unrealized</i> state
 * through <i>Realizing</i> and on to the <i>Realized</i> state.
 * After <code>realize</code> has been invoked on a <code>Controller</code>, the only way it can return 
 * to the <i>Unrealized</i> state is if <code>deallocate</code> is
 * invoked before <i>Realizing</i> is completed.
 * Once a <code>Controller</code> reaches the <i>Realized</i> state, it
 * never returns to the <i>Unrealized</i> state.  It remains in one of four
 * states: <i>Realized</i>, <i>Prefetching</i>, <i>Prefetched</i>, or
 * <i>Started</i>.
 * <p>
 *
 * <h4>Realize Method</h4>
 * The <code>realize</code> method executes asynchronously and completion
 * is signaled by a <code>RealizeCompleteEvent</code> or a
 * <code>ControllerErrorEvent</code>.
 * <p>
 *
 * <h3>Prefetching and Prefetched States</h3>
 *
 * Once <i>Realized</i>, a <code>Controller</code> might still need to 
 * perform a number of time-consuming tasks before it is ready to be started. 
 * For example, it might need to acquire scarce hardware resources, 
 * fill buffers with media data, or perform other start-up processing. 
 * While performing these tasks, the <code>Controller</code>
 * is in the <i>Prefetching</i> state. 
 * When finished, it moves into the <i>Prefetched</i> state.
 * Over a <code>Controller's</code> lifetime, <i>Prefetching</i> might have to recur when certain methods are invoked.
 * For example, calling <code>setMediaTime</code> might cause a <code>Player</code> to be
 * <i>Prefetched</i> again before it is <i>Started</i>.
 * <p>
 *
 * Once a <code>Controller</code> is <i>Prefetched</i>, it is capable of starting as quickly 
 * as is possible for that <code>Controller</code>. 
 * <i>Prefetching</i> reduces the startup latency of a <code>Controller</code> to the minimum
 * possible value. (The startup latency is the value returned by <code>getStartLatency</code>.)
 * <p>
 *
 * Typically, a <code>Controller</code> moves from the <i>Realized</i> state through
 * <i>Prefetching</i> and on to the <i>Prefetched</i> state.
 * Once <i>Prefetched</i>, a <code>Controller</code> 
 * remains <i>Prefetched</i> unless <code>deallocate</code>, <code>syncStart</code> 
 * or a method such as <code>setMediaTime</code>, which changes its state and 
 * increases its startup latency, is invoked.
 * 
 * <p>
 * 
 * When a <i>Started</i>&nbsp;<code>Controller</code> stops, it returns to the <i>Prefetched</i>
 * or <i>Realized</i> state.
 * <p> 
 *
 * <h4>Prefetch Method</h4>
 *
 * The <code>prefetch</code> method is asynchronous and its completion is signaled
 * by a <code>PrefetchCompleteEvent</code> or a <code>ControllerErrorEvent</code>.
 * As a convenience, if <code>prefetch</code> is invoked before a
 * <code>Controller</code> has reached the <i>Realized</i> state, 
 * an implicit <code>realize</code> is invoked by changing the target state
 * to <i>Prefetched</i>.
 * Both a <code>RealizeCompleteEvent</code> and a <code>PrefetchCompleteEvent</code> are
 * posted by the <code>Controller</code> as it
 * transitions to the <I>Prefetched</I> state.
 * <p>
 *
 * If a <code>Controller</code> is <i>Prefetching</i> and cannot obtain all of the
 * resources it needs to start, it posts a <code>ResourceUnavailableEvent</code>
 * instead of a <code>PrefetchCompleteEvent</code>. 
 * This is a catastrophic error condition
 * from which the <code>Controller</code> cannot recover.
 *
 * <h3>Started State</h3>
 *
 * Once <i>Prefetched</i>, a <code>Controller</code> can enter the
 * <i>Started</i> state. A <I>Started</I>&nbsp;<CODE>Controller's</CODE>&nbsp;<CODE>Clock</CODE> is running and is processing data.
 * A <code>Controller</code> returns to the <i>Prefetched</i> or <i>Realized</i>
 * state when it stops because it has either reached its stop time, reached the end of the media,
 * or because the <code>stop</code> method was invoked.
 * <p>
 *
 * When the <code>Controller</code> moves from the <i>Prefetched</i>
 * to the <i>Started</i> state, it posts a <code>StartEvent</code>. 
 * When it moves from the <i>Started</i> state to a stopped state, it posts a <code>StopEvent</code>.
 * <p>
 * 
 * A <code>Controller</code> is a <code>Clock</code>; therefore, <code>syncStart</code>,
 * <code>setTimeBase</code>, <code>setMediaTime</code>, and <code>setRate</code>
 * are illegal when the <code>Controller</code> is in the <i>Started</i> state.
 * <p>
 *
 * <h4>syncStart</h4>
 * The only way to start a <code>Controller</code> is to call
 * <code>syncStart</code>.
 * <p>
 *
 * It is illegal to call <code>syncStart</code> unless the <code>Controller</code> 
 * is in the <i>Prefetched</i> state. If <code>syncStart</code> is called before the <code>Controller</code> is <I>Prefetched</I>,
 *  a <code>NotPrefetchedError</code> is thrown.
 * <code>Player</code> defines a <code>start</code> method that relaxes this
 * requirement. 
 * <p>
 * 
 * 
 * <h3>Freeing the Resources Used by a Controller</h3>
 *
 * <code>Deallocate</code> is used to stop a  <code>Controller's</code> resource consumption. For example, 
 * when <code>Applet.stop</code> is called, <code>deallocate</code> should be called to free the resources that were used by the <code>Controller</code>.
 * <code>Deallocate</code> stops any resource-consuming activity
 * and releases any exclusive-use resources that the <code>Controller</code>
 * has acquired.
 * <code>Deallocate</code> executes synchronously;
 * when <code>deallocate</code> returns, the resources have been released.
 * <p>
 *
 * If the <code>Controller</code> is <i>Unrealized</i> or <i>Realizing</i>, calling <code>deallocate</code> returns it to the
 * <i>Unrealized</i> state. 
 * Otherwise, calling <code>deallocate</code> returns a <code>Controller</code> to  the <i>Realized</i> state.
 * Regardless of the state that a <code>Controller</code> is in,
 * <code>deallocate</code> must relinquish any exclusive-use
 * system resources that it holds;
 * the only way to guarantee that a <code>Controller</code> is not holding
 * resources is to call the <code>deallocate</code> method.
 * <p>
 * 
 * It is illegal to call <code>deallocate</code> on a  <i>Started</i>&nbsp;<code>Controller</code>. 
 * You must stop the <code>Controller</code> before
 * it can relinquish its resources.
 * <p>
 *
 * When <code>deallocate</code> is called,  a <code>Controller</code> posts a special <code>StopEvent</code>,
 * <code>DeallocateEvent</code>.
 * 
 * <a name="CE"><h2>Controller Events</h2></a>
 *
 * <code>Controller</code> events asynchronously deliver
 * information about <code>Controller</code> state changes.
 * There are four kinds of notifications:
 * life cycle transition, method acknowledgement, 
 * state notification, and error notification.
 * <p>
 *
 * To receive events, an object must implement the <code>ControllerListener</code>
 * interface and use the <code>addControllerListener</code> method to register its interest in a <code>Controller's</code> events. 
 * All <code>Controller</code> events are posted to each registered listener.
 * <p>
 *
 * The <code>Controller</code> event mechanism is extensible and 
 * some <code>Controllers</code> define events other than 
 * the ones described here.
 * For example, the <code>DurationUpdateEvents</code> posted by a <code>Player</code> 
 * are <code>ControllerEvents</code>.
 *
 * <dl>
 * <dt> <code>TransitionEvent</code>
 * <dd>
 * <code>TransitionEvents</code> are posted when a <code>Controller's</code>
 * current or target state changes. <code>TransitionEvent</code>
 * is subclassed to provide a small set of events
 * that are posted for particular kinds of transitions that
 * merit special interest. The class name of the event indicates
 * either the reason that the event was posted
 * (such as <code>EndOfMediaEvent</code>),
 * or the particular transition representedby the event (such as
 * <code>PrefetchCompleteEvent</code>).
 * <p>
 *
 * In addition to being posted for state transitions,
 * the method acknowledgement events <code>RealizeCompleteEvent</code>,
 * <code>PrefetchCompleteEvent</code>, <code>StartEvent</code>,
 * <code>DeallocateEvent</code>,
 * and <code>StopByRequestEvent</code> are always posted to signify method
 * completion even if no transition has taken place.
 * <p>
 * 
 * </dd>
 *
 * <dl>
 * <dt> <code>RealizeCompleteEvent</code>
 * <dd>
 * Posted when a <code>Controller</code> moves from the
 * <i>Realizing</i> to the <i>Realized</i> state,
 * or when the <code>realize</code> method is invoked and the
 * <code>Controller</code> is already <i>Realized</i>.
 * </dd>
 *
 * <dt> <code>PrefetchCompleteEvent</code>
 * <dd>
 * Posted when a <code>Controller</code> moves from the <i>Prefetching</i> to
 * the <i>Prefetched</i> state, or when the <code>prefetch</code> method is
 * invoked and the <code>Controller</code> is already <i>Prefetched</i>.
 * </dd>
 *
 * <dt> <code>StartEvent</code>
 * <dd>
 * Posted when a <code>Controller</code> moves from the <i>Prefetched</i> to
 * the <i>Started</i> state.
 * <p>
 * </dd>
 *
 * <dt> <code>StopEvent</code>
 * <dd>
 * Posted when a <code>Controller</code> moves backwards, for example, when moving from  <i>Prefetched</i>
 * to <i>Realized</i> or from <i>Started</i> to <i>Prefetched</i>.
 * The <i>reason</i> that a stop event occurs is often important; this information is provided through several subclasses
 * of <code>StopEvent</code>.
 * </dd>
 *
 * <dl>
 * <dt> <code>StopAtTimeEvent</code>
 * <dd>
 * Posted when a <code>Controller</code> changes state because it has reached
 * its stop time.
 * </dd>
 *
 * <dt> <code>StopByRequestEvent</code>
 * <dd>
 * Posted when a <code>Controller</code> changes state because <code>stop</code> is invoked.
 * This event is also posted as an acknowledgement to <code>stop</code> requests.
 * </dd>
 *
 * <dt> <code>DeallocateEvent</code>
 * <dd>
 * Posted when the <code>deallocate</code> method is invoked, indicating
 * a possible state change and the loss of exclusive-use resources.
 * The current state is either <i>Unrealized</i> or <i>Realized</i>.
 * This event doesn't always indicate a state change. For example, it is posted
 * even if <code>deallocate</code> is called on a <i>Realized</i>&nbsp;
 * <code>Controller</code>.
 * </dd>
 *
 * <dt> <code>EndOfMediaEvent</code>
 * <dd>
 * Posted when a <code>Controller</code> has reached the end of the media.
 * </dd>
 *
 * <dt> <CODE>ControllerClosedEvent</CODE>
 * <dd> When a <CODE>Controller</CODE> closes it is no longer usable,
 * and it will post a <CODE>ControllerClosedEvent</CODE>.
 * Once this has happened, method calls on the <CODE>Controller</CODE>
 * have undefined behavior.
 * A <CODE>Controller</CODE> will close for one of two reasons; either
 * the <CODE>close</CODE> method was invoked on the <CODE>Controller</CODE>, or
 * an error has occurred. If a <CODE>Controller</CODE> is closed because the
 * <CODE>close</CODE> method was invoked, it posts a <CODE>ControllerClosedEvent</CODE>.
 * If an error occurs it posts one of the <CODE>ControllerErrorEvents</CODE>.
 * </dd>
 * </dl>
 * </dl>
 *
 * <dt> <code>ControllerErrorEvent</code>
 * <dd>
 * This is the super class of all of the error events that can be posted
 * by a <code>Controller</code>. While this event is rarely posted, you should watch
 * for it when processing
 * other error events--this is how you can detect implementation-specific
 * error events.
 * <p>
 *
 * When a <code>ControllerErrorEvent</code> is posted, it indicates a catastrophic
 * error from which the <code>Controller</code> cannot recover.  There is no
 * recovery mechanism for a <code>Controller</code> once one of these events has
 * been posted.
 * <p>
 *
 * <dl>
 * <dt> <code>ResourceUnavailableEvent</code>
 * <dd>
 * This error event is posted during <i>Prefetching</i> or <i>Realizing</i>
 * to indicate that the operation failed because a required resource was
 * unavailable.
 * </dd>
 * 
 * <dt> <code>DataLostErrorEvent</code>
 * <dd>
 * This error event is posted when a <code>Controller</code> has lost data.
 * </dd>
 * <dt> <code>InternalErrorEvent</code>
 * <dd>
 * This error event is posted when something goes
 * wrong with the <code>Controller</code> for an implementation-specific reason. 
 * This usually indicates that there is a problem with the implementation.
 * </dd>
 * </dl>
 *
 * <dt> Status Change Events
 * <dd>
 * A small number of status changes occur
 * in a <code>Controller</code> where notification of the change is useful,
 * particularly for updating user interface components.
 * Notification of these changes is provided through three
 * <code>ControllerEvents</code>:
 * </dd>
 *
 * <dl>
 * <dt> <code>RateChangeEvent</code>
 * <dd>
 * Posted when the rate of a <code>Controller</code> changes.
 * 
 * <dt> <code>StopTimeChangeEvent</code>
 * <dd>
 * Posted when the stop time of a <code>Controller</code> changes.
 * 
 * <dt> <code>MediaTimeSetEvent</code>
 * <dd>
 * Posted when the media time has been set using
 * the <code>setMediaTime</code> method. 
 *  This event is <i>not</i> periodically posted when media time changes 
 * due to normal <code>Controller</code> processing and <code>Clock</code> operation.
 * </dl>
 *
 * </dl>
 *
 * <h2>Controls</h2>
 *
 * A <code>Control</code> is an object that provides a way to
 * affect some aspect of a Controller's operation
 * in a specific way. 
 * The <code>Control</code> interface provides access to a
 * GUI <code>Component</code> that is
 * specific to the particular <code>Control</code>.
 * For example, the <code>GainControl</code> interface provides a way
 * to display a GUI control that allows the user to change the volume.
 * <p>
 *
 * A <code>Controller</code> makes available a collection of <code>Controls</code>
 * that influence the <code>Controller's</code> behavior.
 * To access these <code>Controls</code>, use the <code>getControls</code>
 * method, which returns an array of supported <code>Controls</code>.
 * If you know the full class or interface name of the desired <code>Control</code>,
 * you can use <code>getControl</code>.
 * <p>
 *
 * Since an application using a <code>Controller</code> might not know how
 * to use all of the <code>Controls</code> supported by a <code>Controller</code>,
 * it can make the functionality available to a user by
 * providing access to the <code>Component</code> for
 * the <code>Control</code>.
 * <p>
 *
 * @see Player
 * @see Control
 * @see ControllerListener
 * @see ControllerEvent
 * @see TransitionEvent
 * @see RealizeCompleteEvent
 * @see PrefetchCompleteEvent
 * @see StartEvent
 * @see StopEvent
 * @see EndOfMediaEvent
 * @see ControllerErrorEvent
 * @see DataLostErrorEvent
 * @see ResourceUnavailableEvent
 * @see InternalErrorEvent
 * @see RateChangeEvent
 * @see MediaTimeSetEvent
 * @see ClockStartedError
 * @see NotRealizedError
 * 
 * @version 1.1, 98/05/13
 */

public interface Controller extends Clock, Duration {

    /**
     * Returned by <CODE>getStartLatency</CODE>.
     */
    public final static Time LATENCY_UNKNOWN = new Time(Long.MAX_VALUE);

    /**
     * Returned by <CODE>U</CODE>.
     */
    public final static int Unrealized = 100;
    /**
     * Returned by <CODE>getState</CODE>.
     */
    public final static int Realizing = 200;
    /**
     * Returned by <CODE>getState</CODE>.
     */
    public final static int Realized = 300;
    /**
     * Returned by <CODE>getState</CODE>.
     */
    public final static int Prefetching = 400;
    /**
     * Returned by <CODE>getState</CODE>.
     */
    public final static int Prefetched = 500;
    /**
     * Returned by <CODE>getState</CODE>.
     */
    public final static int Started = 600;
  
    /**
     * Gets the current state of this <code>Controller</code>.
     * The state is an integer constant as defined above.
     * <p>
     * <B>Note:</B>
     * A race condition can occur between
     * the return of this method and the execution of
     * a state changing method.
     * 
     * @return The <code>Controller's</code> current state.
     */
    public int getState();


    /**
     * Gets the current target state of this <code>Controller</code>.
     * The state is an integer constant as defined above.
     *<p>
     * <B>Note:</B>
     * A race condition can occur between
     * the return of this method and the execution of
     * a state changing method.
     *
     * @return The <code>Controller's</code> current target state.
    */
    public int getTargetState();
    
    /**
     * Constructs the media dependent portions of the <code>Controller</code>.
     * This may include examining media data and might
     * take some time to complete.
     * <p>
     * The <code>realize</code> method puts the <code>Controller</code> into the <i>Realizing</i> state and returns immediately.
     * When <code>realize</code> is complete and the <code>Controller</code> is in the
     * <i>Realized</i> state, the <code>Controller</code> posts a
     * <code>RealizeCompleteEvent</code>.
     *
     */
    public void realize();

    /**
     * Processes as much data as necessary
     * to reduce the <CODE>Controller's</CODE> start latency to the shortest possible time.
     * This typically involves examining media data and takes some
     * time to complete.
     * <p>
     * The <code>prefetch</code> method puts the <code>Controller</code> into the <i>Prefetching</i> state and returns immediately.
     * When <i>Prefetching</i> is complete and the <code>Controller</code> is in
     * the <i>Prefetched</i> state, the <code>Controller</code> posts
     * a <code>PrefetchCompleteEvent</code>.
     *
     */
    public void prefetch();

    /**
     * Aborts the current operation and cease any activity that
     * consumes system resources. If a <code>Controller</code> is not yet 
     * <i>Realized</i>,
     * it returns to the <i>Unrealized</i> state. Otherwise, the <code>Controller</code>
     * returns to the <i>Realized</i> state.
     * <p>
     * It is illegal to call <code>deallocate</code> on a <i>Started</i>&nbsp;<code>Controller</code>. 
     * A <CODE>ClockStartedError</CODE> is thrown if <CODE>deallocate</CODE>
     * is called and the <CODE>Controller</CODE> is in the <i>Started</i> state.
     */
    public void deallocate();

    /**
     *
     * Releases all resources and cease all activity.
     * The <CODE>close</CODE> method indicates that the <code>Controller</code> will
     * no longer be used and can
     * shut itself down.
     * A <code>ControllerClosedEvent</code> is posted. 
     * Methods invoked on a closed <code>Controller</code>
     * might throw errors.
     */
    public void close();
    
    /**
     * Gets the <code>Controller's</code> start latency in nanoseconds. The start latency represents a 
     * worst-case estimate of the  amount of time it will take
     * to present the first frame of data.
     * <p>
     *
     * This method is useful for determining how far in advance the
     * <code>syncStart</code> method must be invoked to ensure 
     * that media will be
     * rendered at the specified start time.
     * <p>
     * For a <code>Controller</code> that has a variable start latency,
     * the value returned represents the maximum possible
     * start latency.  If you call <code>getStartLatency</code> on a <CODE>Controller</CODE> that isn't <I>Prefetched</I> 
     * and <code>getStartLatency</code> returns <code>LATENCY_UNKNOWN</code>, calling
     * <code>prefetch</code> and then calling <code>getStartLatency</code> again after the <code>Controller</code> posts 
     * a <code>PrefetchCompleteEvent</code> might
     * return a more accurate estimate. 
     * If <code>getStartLatency</code> still returns <code>LATENCY_UNKNOWN</code>, 
     * the start latency is indeterminate and you might not be able to use
     * <code>syncStart</code> to 
     * synchronize the <code>Controller</code> with other <code>Controllers</code>.
     * <p>
     * <b>Note</b>: In most cases, the value returned by
     * <code>getStartLatency</code> will change once the
     * <code>Controller</code> is <i>Prefetched</i>.
     *
     * @return The time it will take before the first frame of media
     * can be presented.
     */
    public Time getStartLatency();

    /**
     * Gets a list of the <code>Control</code> objects supported by 
     * this <code>Controller</code>.
     * If there are no controls, an array of length zero
     * is returned.
     *
     * @return A list of <code>Controller</code>&nbsp;<code>Controls</code>.
     */
    public Control[] getControls();

    /**
     * Gets the <code>Control</code> that supports the specified 
     * class or interface. The full class
     * or interface name should be specified.
     * <code>Null</code> is returned if the <code>Control</code>
     * is not supported.
     *
     * @return <code>Control</code> for the class or interface
     * name.
     */
    public Control getControl(String forName);
 
    /**
     * Specifies a <code>ControllerListener</code> to which
     * this <code>Controller</code>  will send events.
     * A <code>Controller</code> can have multiple
     * <code>ControllerListeners</code>.
     *
     * @param listener The listener to which the <CODE>Controller</CODE>
     * will post events.
     */
    public void addControllerListener(ControllerListener listener);

    /**
     * Removes the specified listener from this <code>Controller's</code>
     * listener list.
     *
     * @param listener The listener that has been receiving events from this
     * <code>Controller</code>.
     */
    public void removeControllerListener(ControllerListener listener);
    

}
