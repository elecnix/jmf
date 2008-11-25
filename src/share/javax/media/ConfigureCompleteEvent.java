/*
 * @(#)ConfigureCompleteEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>ConfigureCompleteEvent</code> is posted when a <code>Processor</code> finishes
 * <I>Configuring</I>. This occurs when a <code>Processor</code> 
 * moves from the <i>Configuring</i> state to the <i>Configured</i>
 * state, or as an acknowledgement that the <code>configure</code>
 * method was called and the <code>Processor</code> is already <i>Configured</i>.
 *
 * @see Processor
 * @see ControllerListener
 * @see TransitionEvent
 * @version 1.6, 98/08/11.
 * @since JMF 2.0
 */
public class ConfigureCompleteEvent extends TransitionEvent {

    /**
     * Construct a new <CODE>ConfigureCompleteEvent</CODE>.
     *
     * @param processor The <code>Processor</code> that is generating this event.
     * @param previous The state that the <code>Processor</code> was in before this event.
     * @param current The state that the <code>Processor</code> is in as a result of this event.
     * @param target The state that the <code>Processor</code> is heading to.
    */
    public ConfigureCompleteEvent(Controller processor,
				 int previous, int current, int target) {
	super(processor, previous, current, target);
    }
}


