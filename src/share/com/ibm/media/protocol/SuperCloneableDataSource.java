/*
 * @(#)SuperCloneableDataSource.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.ibm.media.protocol;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.protocol.*;

/**
 * This is a utility class that creates clones of a DataSource.
 * The CloneableDataSource is itself a DataSource that
 * reflects the functionality and type of the input DataSource.  The input
 * should be of one of the following types: PullDataSource, 
 * PullBufferDataSource, PushDataSource or PushBufferDataSource. 
 * The resulting CloneableDataSource will be of the same type.
 * To create a clone of this data source, call the <code>getClone()</code>
 * method on this object. Any clone created from this DataSource will by Default
 * be a PushDataSource or PushBufferDataSource.
 * <p>
 * The cloned DataSource shares the properties (duration, 
 * content type, etc.) of the original DataSource.
 * <p>
 * Calling <code>connect</code>, <code>disconnect</code>,
 * <code>start</code>, <code>stop</code> on the CloneableDataSource (master)
 * will propagate the same calls to the cloned (slave) DataSources.
 *
 * This is a class used by the CloneablePullDataSource, 
 * CloneablePushDataSource, CloneablePullBufferDataSource, 
 * CloneablePushDataSource and shouldn't be used explicitly by developers.
 *
 * @see javax.media.protocol.DataSource
 * @since JMF 2.0
 */

class SuperCloneableDataSource extends DataSource {

    /**
     * The DataSource to be cloned.
     */
    protected DataSource input;

    /**
     * An array of adapters where each adapter correspond to a stream in the 
     * DataSource we are cloning.
     */
    public CloneableSourceStreamAdapter[] streamsAdapters;
  
    /* The streams that will be returned by this DataSource and will be used 
     * by the connected Handler.
     */
    public SourceStream[] streams = null;

    /**
     * The cloned DataSources.
     */
    private Vector clones = new Vector();

    /**
     * Constructor that takes a DataSource object for cloning.
     * @param input the DataSource for cloning.
     */
    SuperCloneableDataSource(DataSource input) {    
	this.input = input;
	SourceStream[] originalStreams = null;

	if (input instanceof PullDataSource)
	    originalStreams = ((PullDataSource)input).getStreams();
	if (input instanceof PushDataSource)
	    originalStreams = ((PushDataSource)input).getStreams();
	if (input instanceof PullBufferDataSource)
	    originalStreams = ((PullBufferDataSource)input).getStreams();
	if (input instanceof PushBufferDataSource)
	    originalStreams = ((PushBufferDataSource)input).getStreams();
	streamsAdapters = new CloneableSourceStreamAdapter[originalStreams.length];
	// create a cloneable adapter for each stream
	for (int i = 0; i < originalStreams.length; i++) 
	    streamsAdapters[i] = new CloneableSourceStreamAdapter(originalStreams[i]);
    }
  
    /**
     * Clone the original datasource, returning an object of the type
     * <code>PushDataSource</code> or <code>PushBufferDataSource</code>. 
     * If the original data source was a
     * PullDataSource, then this will be a PushDataSource which pushes at
     * the same rate at which the CloneableDataSource is being pulled.
     * @return a slave DataSource for this DataSource.
     */
    javax.media.protocol.DataSource createClone() {
    
	DataSource newSlave;

	if ((input instanceof PullDataSource) || 
	    (input instanceof PushDataSource))
	    newSlave = new PushDataSourceSlave();
	else // input is a Buffer type DataSource
	    newSlave = new PushBufferDataSourceSlave();

	clones.addElement(newSlave);

	try {
	    newSlave.connect();
	} catch (IOException e) {
	    return null;
	}

	return newSlave;
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
    public String getContentType() {

	return input.getContentType();
    }
  
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
    public void connect() throws IOException {
    
	input.connect();
    }
  
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
    public void disconnect() {
    
	input.disconnect();
    }
  
    /**
     * Initiate data-transfer. The <CODE>start</CODE> method must be
     * called before data is available.
     *(You must call <CODE>connect</CODE> before calling <CODE>start</CODE>.)
     *
     * @exception IOException Thrown if there are IO problems with the source
     * when <CODE>start</CODE> is called.
     */
    public void start() throws IOException {

	input.start(); 
    }
  
    /**
     * Stop the data-transfer.
     * If the source has not been connected and started,
     * <CODE>stop</CODE> does nothing.
     */
    public void stop() throws IOException {
    
	input.stop();
    }
  
    /**
     * Obtain the collection of objects that
     * control the object that implements this interface.
     * <p>
     *
     * If no controls are supported, a zero length
     * array is returned.
     *
     * @return the collection of object controls
     */
    public Object[] getControls() {
    
	return input.getControls();
    }
  
    /**
     * Obtain the object that implements the specified
     * <code>Class</code> or <code>Interface</code>
     * The full class or interface name must be used.
     * <p>
     * 
     * If the control is not supported then <code>null</code>
     * is returned.
     *
     * @return the object that implements the control,
     * or <code>null</code>.
     */
    public Object getControl(String controlType) {
    
	return input.getControl(controlType);
    }
  
    /**
     * Get the duration of the media represented
     * by this object.
     * The value returned is the media's duration
     * when played at the default rate.
     * If the duration can't be determined  (for example, the media object is presenting live
     * video)  <CODE>getDuration</CODE> returns <CODE>DURATION_UNKNOWN</CODE>.
     *
     * @return A <CODE>Time</CODE> object representing the duration or DURATION_UNKNOWN.
     */
    public Time getDuration() {
    
	return input.getDuration();
    }
  
    class PushDataSourceSlave extends PushDataSource {
    
	PushSourceStream[] streams = null;
    
	public PushDataSourceSlave() {
	    streams = new PushSourceStream[streamsAdapters.length];
	    for (int i = 0; i < streams.length; i++)
		streams[i] = (PushSourceStream)streamsAdapters[i].createSlave();
	}

	public String getContentType() {
      
	    return input.getContentType();
	}
    
	public void connect() throws IOException {
	    for (int i = 0; i < streams.length; i++) {
		((SourceStreamSlave)streams[i]).connect();
	    }
	}
    
	public void disconnect() {
	    for (int i = 0; i < streams.length; i++) {
		((SourceStreamSlave)streams[i]).disconnect();
	    }
	}
    
	public void start() throws IOException {
      
	    // DO NOTHING SINCE THIS IS A CLONE
	}
    
	public void stop() throws IOException {
    
	    // DO NOTHING SINCE THIS IS A CLONE
	}
    
	public PushSourceStream[] getStreams() {
	    return streams;
	}

	public Object[] getControls() {
      
	    // should we duplicate the controls?
	    return input.getControls();
	}
    
	public Object getControl(String controlType) {
      
	    // should we duplicate the control?
	    return input.getControl(controlType);
	}
    
	public Time getDuration() {
      
	    return input.getDuration();
	}
    }

    class PushBufferDataSourceSlave extends PushBufferDataSource {

	PushBufferStream[] streams = null;

	public PushBufferDataSourceSlave() {
	    streams = new PushBufferStream[streamsAdapters.length];
	    for (int i = 0; i < streams.length; i++)
		streams[i] = (PushBufferStream)streamsAdapters[i].createSlave();
	}

	public String getContentType() {
      
	    return input.getContentType();
	}
    
	public void connect() throws IOException {
	    for (int i = 0; i < streams.length; i++) {
		((SourceStreamSlave)streams[i]).connect();
	    }
	}
    
	public void disconnect() {
	    for (int i = 0; i < streams.length; i++) {
		((SourceStreamSlave)streams[i]).disconnect();
	    }
	}
    
	public void start() throws IOException {
      
	    // DO NOTHING SINCE THIS IS A CLONE
	}
    
	public void stop() throws IOException {
    
	    // DO NOTHING SINCE THIS IS A CLONE
	}
    
	public PushBufferStream[] getStreams() {
	    return streams;
	}

	public Object[] getControls() {
      
	    // should we duplicate the controls?
	    return input.getControls();
	}
    
	public Object getControl(String controlType) {
      
	    // should we duplicate the control?
	    return input.getControl(controlType);
	}
    
	public Time getDuration() {
      
	    return input.getDuration();
	}
    } 
}
