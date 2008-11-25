/*
 * @(#)DataSource.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

import javax.media.*;
import javax.media.Duration;
import java.io.IOException;
import java.net.*;

/**
 * A <CODE>DataSource</CODE> is an abstraction for media protocol-handlers.
 * <CODE>DataSource</CODE> manages the life-cycle of the media source
 * by providing a simple connection protocol.
 *
 * <h2>Source Controls</h2>
 * 
 * A <code>DataSource</code> might support an operation
 * that is not part of the <code>DataSource</code>
 * class definition. For example a source could support
 * positioning its media to a particular time.
 * Some operations are dependent on the data stream that the
 * source is managing, and support cannot be determined
 * until after the source has been connected.
 * <p>
 *
 * To obtain all of the objects that provide control
 * over a <code>DataSource</code>, use <code>getControls</code>
 * which returns an array of <code>Object</code>
 * To determine if a particular kind of control
 * is available and obtain the object that implements
 * it, use <code>getControl</code> which takes
 * the name of the Class or Interface that of the
 * desired control.
 *
 * <h2>DataSource Properties</h2>
 * 
 * A <code>DataSource</code> may implement the following few 
 * well-defined interfaces:
 * <br> 
 * <code>Positionable</code>: if the <code>DataSource</code> can be positioned.
 * <br>
 * <code>RateConfigureable</code>: if the <code>DataSource</code> supports
 * different playback rates.
 * <br>
 * <code>SourceCloneable</code>: if the <code>DataSource</code> can be
 * cloned.
 * <br>
 * <code>CaptureDevice</code>: if the <code>DataSource</code> is a 
 * capture device.
 *
 *
 * @see Manager
 * @see Positionable
 * @see RateConfigureable
 * @see CaptureDevice
 * @see SourceCloneable
 *
 * @version 1.4, 02/08/21
 */
abstract public class DataSource implements Controls, Duration {
    
    MediaLocator sourceLocator;

    /**
     * A no-argument constructor required by pre 1.1 implementations
     * so that this class can be instantiated by
     * calling <CODE>Class.newInstance</CODE>.
     *
     */
    public DataSource() {
	sourceLocator = null;
    }

    /**
     * Construct a <CODE>DataSource</CODE> from a <CODE>MediaLocator</CODE>.
     * This method should be overloaded by subclasses;
     * the default implementation just keeps track of
     * the <CODE>MediaLocator</CODE>.
     *
     *
     * @param source The <CODE>MediaLocator</CODE> that describes
     * the <CODE>DataSource</CODE>.
     */
    public DataSource(MediaLocator source) {
	sourceLocator = null;
	setLocator(source);
    }


    /**
     * Set the connection <CODE>source</CODE> for this <CODE>DataSource</CODE>.
     * This method should only be called once; an error is thrown if 
     * the locator has already been set.
     *
     * @param source The <CODE>MediaLocator</CODE> that describes the
     * media source.
     */
    public void setLocator(MediaLocator source) {
	if( sourceLocator == null) {
	    sourceLocator = source;
	} else {
	    // $jdr: Should we name the error here?
	    throw new java.lang.Error("Locator already set on DataSource.");
	}
    }

    /**
     * Get the <CODE>MediaLocator</CODE> that describes this source.
     * Returns <CODE>null</CODE> if the locator hasn't been set. 
     * (Very unlikely.)
     * @return The <CODE>MediaLocator</CODE> for this source.
     */
    public MediaLocator getLocator() {
	return sourceLocator;
    }
    
    /**
     * Check to see if this connection has been
     * initialized with a <CODE>MediaLocator</CODE>.
     * If the connection hasn't been initialized,
     * <CODE>initCheck</CODE> throws an <CODE>UninitializedError</CODE>.
     * Most methods should call <CODE>initCheck</CODE> on entry.
     *
     */
    protected void initCheck() {
	if(sourceLocator ==  null) {
	    // $jdr: This should a real media error.
	    throw new java.lang.Error("Uninitialized DataSource error.");
	}
    }
    
    /**
     * Get a string that describes the content-type of the media
     * that the source is providing.
     * <p>
     * It is an error to call <CODE>getContentType</CODE> if the source is
     * not connected.
     *
     * @return The name that describes the media content.
     */
    public abstract String getContentType();
    
    /**
     * Open a connection to the source described by
     * the <CODE>MediaLocator</CODE>.
     * <p>
     *
     * The <CODE>connect</CODE> method initiates communication with the source.
     *
     * @exception IOException Thrown if there are IO problems
     * when <CODE>connect</CODE> is called.
     */
    public abstract void connect() throws IOException; 

    /**
     * Close the connection to the source described by the locator.
     * <p>
     * The <CODE>disconnect</CODE> method frees resources used to maintain a
     * connection to the source.
     * If no resources are in use, <CODE>disconnect</CODE> is ignored.
     * If <CODE>stop</CODE> hasn't already been called,
     * calling <CODE>disconnect</CODE> implies a stop.
     *
     */
    public abstract void disconnect();

    /**
     * Initiate data-transfer. The <CODE>start</CODE> method must be
     * called before data is available.
     *(You must call <CODE>connect</CODE> before calling <CODE>start</CODE>.)
     *
     * @exception IOException Thrown if there are IO problems with the source
     * when <CODE>start</CODE> is called.
     */
    public abstract void start() throws IOException;

    /**
     * Stop the data-transfer.
     * If the source has not been connected and started,
     * <CODE>stop</CODE> does nothing.
     */
    public abstract void stop() throws IOException;

}
