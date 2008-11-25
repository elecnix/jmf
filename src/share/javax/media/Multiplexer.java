/*
 * @(#)Multiplexer.java	1.24 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;
import javax.media.format.*;
import javax.media.protocol.*;

/**
 * A <code>Multiplexer</code> is a media processing unit takes input data from multiple tracks, 
 * combines the data into an interleaved container format, then outputs the
 * interleaved data through an output <code>DataSource</code>. 
 * It typically has multiple inputs and one output.
 * 
 * A <CODE>Multiplexer</CODE> can also have a single track as input. In this case, 
 * multiple tracks don't have to be interleaved, but the <CODE>Multiplexer</CODE> 
 * might format the output stream with additional data interleaved with the input track.
 * <p>
 * If a <code>Multiplexer</code> implements the <code>Clock</code> interface,
 * it can be used by a <code>Player</code> as
 * the master time base for synchronization.   In this case, the
 * <code>Multiplexer</code> should update the media time and time-base time as 
 * it processes the media.
 * @since JMF 2.0
 */
public interface Multiplexer extends javax.media.PlugIn {

    /**
     * Lists the output content-descriptors that this <CODE>Multiplexer</CODE> supports.
     * @ param inputs An array of the formats of the data to be input to the <CODE>Multiplexer</CODE>.
     * If <code>inputs</code> is non-null, then this method lists the 
     * output content descriptors supported when the input data is in the specified
     * formats. If <code>inputs</code> is null, this method lists
     * all of the output content descriptors that this <CODE>Multiplexer</CODE> supports.
     & @ return An array of the content descriptors supported by this <CODE>Multiplexer</CODE>.
     */
    public ContentDescriptor[] getSupportedOutputContentDescriptors(Format inputs[]);

    /**
     * Returns the input formats that this <CODE>Multiplexer</CODE> supports. The <CODE>Format</CODE> objects
     * that are returned might be loosely defined. In this case, <code>setInputFormat</code>
     * should be called with a more specific <CODE>Format</CODE> to make sure that the desired
     * <CODE>Format</CODE> is supported by this <CODE>Multiplexer</CODE>.
     * @return An array of <CODE>Format</CODE> objects. If no formats are supported, then
     * an array of size zero is returned.
     */
    public Format [] getSupportedInputFormats();

    /**
     * Sets the number of input tracks that the <CODE>Multiplexer</CODE> can expect
     * to find in the input stream.
     * @param numTracks The number of input tracks that the input stream contains.
     * @return The number of input tracks actually set. This
     * might be less than the number of tracks specified. If the number specified exceeds
     * the number of tracks that the <CODE>Multiplexer</CODE> can handle, this 
     * method returns the maximum number 
     * of tracks supported by the <CODE>Multiplexer</CODE>.
     * 
     */
    public int setNumTracks(int numTracks);

    /**
     * Sets the input <CODE>Format</CODE> for the specified track. 
     * @param format The input <CODE>Format</CODE> of the specified track.
     * @param trackID The index number of the track for which the <CODE>Format</CODE> is being set.
     * @return The <CODE>Format</CODE> preferred by the <CODE>Multiplexer</CODE>. 
     * This might the same as the specified <CODE>Format</CODE>, a more well-defined <CODE>Format</CODE>
     * than was specified, or null if the specified <CODE>Format</CODE> is not supported by the 
     * <CODE>Multiplexer</CODE> at all.
     */
    public Format setInputFormat(Format format, int trackID);

    /**
     * Processes the input <CODE>Buffer</CODE> and multiplexes it with data from other
     * tracks.  The multiplexed output is sent to an output 
     * <code>DataSource</code>. 
     * @param buffer The <CODE>Buffer</CODE> of data to process.
     * @param trackID The index number of the track to which the input <CODE>Buffer</CODE>
     *		belongs.
     * @return <CODE>BUFFER_PROCESSED_OK</CODE> If the processing is successful.  Other
     * possible return codes are defined in <CODE>PlugIn</CODE>. 
     * @see PlugIn
     */
    public int process(Buffer buffer, int trackID);

    /**
     * Gets the output <code>DataSource</code> from this <CODE>Multiplexer</CODE>.
     * The <CODE>DataSource</CODE> is returned in the connected state. 
     * The returned <CODE>DataSource</CODE>  can be a push or pull 
     * <CODE>DataSource</CODE>--<code>Push[Pull]DataSource</code> or 
     * <code>Push[Pull]BufferDataSource</code>.  
     * @return The output <code>DataSource</code> for this <CODE>Multiplexer</CODE>.
     */
    public DataSource getDataOutput();

    /**
     * Sets the output content-type for this <CODE>Multiplexer</CODE>.
     *
     * @param outputContentDescriptor  A <CODE>ContentDescriptor</CODE> that describes the 
     * content-type of the data to be output by the <CODE>Multiplexer</CODE>.
     * @return The content descriptor that most closely matches the specified content 
     * descriptor or null if the specified content descriptor cannot be set.
     */
    public ContentDescriptor setContentDescriptor(ContentDescriptor outputContentDescriptor);
    
}

