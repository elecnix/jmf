/*
 * @(#)AudioDeviceUnavailableEvent.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * An <code>AudioDeviceUnavailableEvent</code> indicates that the <code>Controller</code> could not
 * fetch the audio device.
 * @since JMF 2.0
 */
public class AudioDeviceUnavailableEvent extends ControllerEvent {

    /**
     * Constructs an event for the specified controller.
     * @param from The controller which generated this event.
     */
    public AudioDeviceUnavailableEvent(Controller from) {
	super(from);
    }

}
