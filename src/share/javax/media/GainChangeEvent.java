/*
 * @(#)GainChangeEvent.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * A <code>GainChangeEvent</code> is posted by a
 * <code>GainControl</code> when its state has been updated.
 *
 * <h2>Java Beans support </h2>
 * Any implementation of this object is required to be subclassed
 * from either java.util.EventObject or sunw.util.EventObject.
 *
 * @see GainControl
 * @see GainChangeListener
 *
 * @version 1.3, 02/08/21
 */
public class GainChangeEvent extends MediaEvent {

    GainControl eventSrc;
    boolean newMute;
    float newDB;
    float newLevel;

    public GainChangeEvent(GainControl from,
			  boolean mute, float dB, float level) {
	super(from);
	eventSrc = from;
	newMute = mute;
	newDB = dB;
	newLevel = level;
    }

    /**
     * Get the object that posted this event.
     *
     * @return The object that posted this event.
     */
     public Object getSource() {
	 return eventSrc;
     }

    /**
     * Get the <code>GainControl</code> that posted this event.  
     *
     * @return The <code>GainControl</code> that posted this event.
     */
    public GainControl getSourceGainControl() {
	return eventSrc;
    }

    /**
     * Get the <code>GainControl's</code> new gain value in dB.
     *
     * @return The <code>GainControl's</code> new gain value, in dB.
     */
    public float getDB() {
	return newDB;
    }

    /**
     * Get the <code>GainControl's</code> new gain value in the level scale.
     *
     * @return The <code>GainControl's</code> new gain, in the level scale.
    */
    public float getLevel() {
	return newLevel;
    }

    /**
     * Get the <code>GainControl's</code> new mute value.
     *
     * @return The <code>GainControl's</code> new mute value.
    */
    public boolean getMute() {
	return newMute;
    }

}
