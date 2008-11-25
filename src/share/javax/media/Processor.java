/*
 * @(#)Processor.java	1.25 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.control.TrackControl;

/**
 * The <code>Processor</code> interface defines a module for processing 
 * and controlling time-based media data.
 * <code>Processor</code> extends the <code>Player</code> interface.
 *
 * Unlike a <code>Player</code>, which processes data as a "black box" and
 * only renders data to preset destinations, a <code>Processor</code> 
 * supports a programmatic interface that enables control over the media data processing
 * and access to output data streams. 
 * <p>
 * The processing performed by a <code>Processor</code> is split into
 * three stages:
 * <ul>
 * <li> Demultiplexing -
 *    an interleaved media stream is first demultiplexed into separate tracks of
 *    data streams that can be processed individually.
 * <li> Data transcoding -
 *    each track of data can be transcoded from one format to another.
 * <li> Multiplexing -
 *    the separate tracks can be multiplexed to form
 *    an interleaved stream of a particular container content type. 
 * </ul>
 * <p>
 * Both the data transcoding and multiplexing processes are programmable. 
 *
 * <h2>How a <code>Processor</code> Differs from a <code>Controller</code></h2>
 *
 * <code>Processor</code> extends the state transition cycle of a
 * <code>Controller</code> by adding the <i>Configuring</i> and
 * <i>Configured</i> states.
 * The purpose of these additional states is to further refine
 * the realizing process.  The
 * realizing step is essentially split into two phases: 
 * <ul>
 * <li> Source information gathering -
 * 	the input <code>DataSource</code> is queried and the input media 
 *	stream is parsed to get the format information for the tracks in the stream.
 * <li> Construction -
 *	the internals of the <code>Processor</code> are constructed to handle
 * 	the input media stream.
 * </ul>
 * <p>
 * Between these two steps, you can 
 * program the <code>Processor</code> to perform specific 
 * processing on its media stream.
 * <p>
 * The states of a <code>Processor</code> are:
 * <i>Unrealized,</i> <i>Configuring,</i> <i>Configured,</i>
 * <i>Realizing,</i> <i>Realized,</i> <i>Prefetching,</i>
 * <i>Prefetched,</i> and <i>Started</i>.
 *
 * <h3>The <i>Configuring</i> and <i>Configured</i> States</h3>
 * <p>
 * While it's in the <code>Configuring</code> state, a <code>Processor</code>
 * gathers the source information necessary to prepare the <code>Processor</code> to be programmed.
 * This might involve parsing an input file to access the individual
 * media tracks within the file, or connecting to a capturing device
 * to determine its capabilities.  A <code>ConfigureCompleteEvent</code>
 * is posted when the <code>Processor</code> reaches <i>Configured</i> state.
 * <p>
 * Once a <code>Processor</code> is <i>Configured,</i> you can program
 * it to perform particular processing on each track or to output data in a particular
 * format.
 *
 * <h3>Realizing a <code>Processor</code></h3>
 * <p>
 * When you're done programming the <code>Processor</code>, you call the <code>realize</code>
 * method to complete its construction.
 * <p>
 * Once the <code>Processor</code> is in the <i>Realized</i> state, reprogramming 
 * the <code>Processor</code> by calling the <code>TrackControl</code> methods or 
 * the <code>setContentDescriptor</code> method is not guaranteed
 * to work. This is because reprogramming the <code>Processor</code> might require reconstruction 
 * of its internals.
 * <p>
 * It is legal to call <code>realize</code> on a <code>Processor</code> while it 
 * is in the <i>Unrealized</i> state.  
 * This causes the <code>Processor</code> to transition from the
 * <i>Unrealized</i> state to the <i>Realized</i> state. As it does this, it goes through each
 * intermediate state: <code>Configuring</code>, <code>Configured</code>, and
 * <code>Realizing</code>.  However, when you directly realize a <code>Processor</code>, you miss the
 * opportunity to program it while it's in the 
 * <i>Configured</i> state--the <code>Processor</code> performs whatever default processing its 
 * implementation specifies.  
 *
 * <h3>Deallocating a <code>Processor</code></h3>
 * <p>
 * Calling <code>deallocate</code> changes the state of a <code>Processor</code>
 * in the same way as a <code>Controller</code> <b>except</b> that
 * if <code>deallocate</code> is called while the <code>Processor</code> 
 * is in the <i>Configuring</i> or
 * <i>Configured</i> state, the
 * <code>Processor</code> is returned to the <i>Unrealized</i> state. 
 *
 * <h2>Programming a <code>Processor</code></h2>
 * <p>
 * You can control both the transcoding and multiplexing performed by a <code>Processor</code>.  
 * Data transcoding is controlled separately for each track.
 * The <code>getTrackControls</code> method 
 * returns a <code>TrackControl</code> for each track. You use these
 * <code>TrackControl</code> objects to specify what processing you want to perform.
 * The multiplexing performed by a <code>Processor</code> is controlled by specifying 
 * the format that you want it to output. This is done through 
 * the <code>setContentDescriptor</code> method.
 * <p>
 * A <code>Processor</code> can be programmed while it is in the
 * <i>Configured</i> state.  A <code>NotConfiguredError</code> is thrown
 * if you attempt to program the <code>Processor</code> before
 * is configured.
 * <p>
 * If you do not program a <code>Processor</code> through the
 * <code>TrackControl</code> methods or by calling <code>setContentDescriptor</code>, 
 * it performs whatever default processing is specified by its implementation.
 *
 * <h2>Getting the Output from a <code>Processor</code></h2>
 * <p>
 * The processed output data streams can be retrieved from a <code>Processor</code> 
 * through its output <code>DataSource</code>.  The output 
 * <code>DataSource</code> provides the gateway for the output data to 
 * be read.  A <code>DataSource</code> output from a <code>Processor</code> 
 * can be a <code>PushDataSource</code>, <code>PushBufferDataSource</code>, 
 * <code>PullDataSource</code>, or <code>PullBufferDataSource</code> depending 
 * on the implementation of the <code>Processor</code>.
 * <p>
 * A <code>NotRealizedError</code> is thrown if 
 * <code>getDataOutput</code> is called on a <code>Processor</code>
 * that has not yet been realized.
 *
 *
 * <h2>Using a <code>Processor</code> as a <code>Player</code></h2>
 *
 * Many <code>Processor</code> implementations can be used like a
 * <code>Player</code> to render media data instead of sending it to an
 * output <code>DataSource</code>.  In this case, the <code>TrackControl</code> objects
 * provide additional information and control over the individual 
 * tracks to be rendered.  When used as a <code>Player</code>, a <code>Processor</code> 
 * does not produce an output <code>DataSource</code>.
 * To use a <code>Processor</code> as a <code>Player</code>, you call
 * <code>setContentDescriptor(null)</code>. 
 *
 * @see Controller
 * @see Player
 * @see TrackControl
 * @since JMF 2.0
 */

public interface Processor extends Player {

    /**
     * Returned by <code>getState</code>.
     */
    public final static int Configuring = 140;

    /**
     * Returned by <code>getState</code>.
     */
    public final static int Configured = 180;


    /**
     * Prepares the <code>Processor</code> to be programmed. The <code>Processor</code> 
     * gathers information about the data it is going to process.
     * Calling <code>configure</code> puts the <code>Processor</code> 
     * into the <i>Configuring</i> state and returns immediately. 
     * When this process is complete and the 
     * <code>Processor</code> is in the <i>Configured</i> state, 
     * the <code>Processor</code> posts a <code>ConfigureCompleteEvent</code>. 
     */
    public void configure();
    
    /**
     * Gets a <code>TrackControl</code> for each track in the media stream. 
     * This method can only be called once 
     * the <code>Processor</code> has been configured. 
     *
     * @return  An array of <code>TrackControl</code> objects.  An empty array is returned
     *   if there is no <code>TrackControl</code> available for this 
     * <code>Processor</code>.
     * @exception NotConfiguredError  If the <code>Processor</code> is in the <i>Unrealized</i> or 
     * <i>Configuring</i> state. 
     */
    public TrackControl[] getTrackControls() throws NotConfiguredError;

    /** 
     * Gets all of the content types that this <code>Processor</code>  
     * can output. The <code>Processor</code> builds the 
     * <code>ContentDescriptor</code> array according to its input 
     * <code>DataSource</code> and
     * the available codecs and multiplexers.
     *
     * @return  An array of the content types supported by this 
     * <code>Processor</code>.
     * @exception NotConfiguredError If the <code>Processor</code> is in the <i>Unrealized</i> or 
     * <i>Configuring</i> state. 
     */
    public ContentDescriptor[] getSupportedContentDescriptors() throws NotConfiguredError;
    
    /**
     * Sets the output content-type for this <code>Processor</code>.
     * If <code>setContentDescriptor</code> is not called, the output 
     * <code>DataSource</code> is set to raw output by default:
     * <code>(new ContentDescriptor(ContentDescriptor.RAW))</code>.  The source streams
     * from the <code>DataSource</code> are the demultiplexed tracks
     * from the input source.
     *
     * @return The content descriptor that most closely matches the specified content 
     * descriptor or null if the specified content descriptor cannot be set.
     * @param outputContentDescriptor  The content type to be used for the <code>Processor</code> output.
     * @exception NotConfiguredError If the <code>Processor</code> is in the <i>Unrealized</i> or 
     * <i>Configuring</i> state. 
     */
    public ContentDescriptor setContentDescriptor(ContentDescriptor outputContentDescriptor) throws NotConfiguredError ;
    
    /**
     * Gets the output content-type currently set for this <code>Processor</code>.
     *
     * @return  The current output content-type.
     * @exception NotConfiguredError If the <code>Processor</code> is in the <i>Unrealized</i> or 
     * <i>Configuring</i> state. 
     */
    public ContentDescriptor getContentDescriptor() throws NotConfiguredError;
    
    /** 
     * Gets the output <code>DataSource</code> from the <code>Processor</code>. The 
     * output <code>DataSource</code> is the output connection through which the processed 
     * streams are supplied. The output <code>DataSource</code> returned by the <code>Processor</code> is  
     * in the <i>Connected</I> state.
     * @exception NotRealizedError If the <code>Processor</code> is has not yet been 
     * realized.
     */
    public DataSource getDataOutput() throws NotRealizedError;
    
}
