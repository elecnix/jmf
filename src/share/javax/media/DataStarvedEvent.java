/*
 * @(#)DataStarvedEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * <CODE>DataStarvedEvent</CODE> indicates that a <code>Controller</code> has
 * lost data or has stopped receiving data altogether. This
 * transitions the <CODE>Controller</CODE> into a <i>Stopped</i> state.
 *
 * @see Controller
 * @see ControllerListener
 *
 * @version 1.2, 02/08/21
 *
 */
public class DataStarvedEvent extends StopEvent {

    public DataStarvedEvent(Controller from,
			      int previous, int current, int target,
			      Time mediaTime) {
        super(from, previous, current, target, mediaTime);
    }

}
