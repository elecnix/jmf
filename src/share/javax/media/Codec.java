/*
 * @(#)Codec.java	1.13 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import javax.media.*;
import javax.media.format.*;

/**
 * A <code>Codec</code> is a media processing unit that accepts a <code>Buffer</code> 
 * object as its input, performs some processing on the input data, and then puts the 
 * result in an output <code>Buffer</code> object. It has one input and one output. 
 * Typical examples of codecs include audio decoders, video encoders, and effects.
 * 
 * <p>
 * A  codec usually works in one of the following modes:
 * <ul>
 *
 * <li>Frame based mode. In this mode, the codec accepts one frame of data from its input
 * and converts it to one frame of output data.
 * The codec must consume its input <code>Buffer</code> and generate
 * an output <code>Buffer</code>.
 * <p>
 * This mode is useful when the codec can handle any size of input. A
 * simple gain codec fits this model: it multiplies each
 * sample with the gain factor and puts the product in an output <code>Buffer</code>.
 * Another scenario where this mode is useful is when the codec can only process
 * data that's in a fixed, pre-determined frame size
 * and the input <code>Buffer</code> is already packetized accordingly.
 * One example of such a codec is a GSM audio decoder, which accepts a compressed
 * GSM audio packet from an RTP depacketizer, decodes the packet, and then puts the result in
 * an output <code>Buffer</code>. <br>&nbsp;</li>
 * 
 * <li>Stream based mode. In this mode, the codec accepts chunks of data from its input and
 * might generate an output <code>Buffer</code>.
 * The codec might consume only part of the input <code>Buffer</code> each time its <code>process</code>
 * method is called and  might not generate an output <code>Buffer</code> during each round of processing.
 * This mode is useful in stream packetizers, which accept a stream of bytes and divide the stream
 * into packets (frames) that are used in the next processing phase.
 * Another scenario where this mode is useful is when two audio processing units that have
 * incompatible frame sizes need to be chained.
 * </li>
 * </ul>
 * <p>
 * Some restrictions apply to the processing a <code>Codec</code> can perform on its input
 * and output <code>Buffer</code> objects:
 * <ul>
 * <li> The <code>Codec</code> might receive an output <code>Buffer</code> that is not big enough to hold its output data.
 * In this case, the <code>Codec</code> should allocate a new <code>Buffer</code> for its output data. <br>&nbsp;</li>
 * 
 * <li> The <code>Codec</code> cannot cache references to <code>Buffer</code> object fields.
 * It must read all of the parameters from the input and output <code>Buffer</code> objects each time
 * its  <code>process</code> method is called. <br>&nbsp;</li>
 * 
 * <li> If the <code>Codec</code> needs to keep references to a <code>Buffer</code> object's data (for performance reasons),
 * the <code>Codec</code> must assign other data to the input <code>Buffer</code> object by calling  <code>setData</code>
 * before returning from the <code>process</code> method. 
 * The data assigned can be null, but it is better to assign 
 * some unneeded data to the <code>Buffer</code>, such as input data received earlier.
 * Such manipulations can be used for in-place processing (where the output of the processing
 * is put in the same location as input data in order to enhance memory utilization) or for codecs
 * that need access to more than one frame of data without copying the data (for example, temporal video effects).
 * </li>
 * </ul>
 * @since JMF 2.0
 **/
public interface Codec extends javax.media.PlugIn  {

    /**
     * Lists all of the input formats that this codec accepts.
     * @return An array that contains the supported input <code>Formats</code>.
     */
    public Format [] getSupportedInputFormats();

    /**
     * Lists the output formats that this codec can generate.
     * If <code>input</code> is non-null, this method lists the possible
     * output formats that can be generated from input data of the specified <code>Format</code>.
     * If <code>input</code> is null, this method lists
     * all of the output formats supported by this plug-in.
     * @param input The <code>Format</code> of the data to be used as input to the plug-in.
     * @return An array that contains the supported output <code>Formats</code>.
     */
    public Format [] getSupportedOutputFormats(Format input);

    /**
     * Sets the format of the data to be input to this codec.
     * @param format The <code>Format</code> to be set.
     * @return The <code>Format</code> that was set, which might be the
     * supported <code>Format</code> that most closely matches the one specified. 
     * Returns null if the specified <code>Format</code> is not supported and 
     * no reasonable match could be found. 
     */
    public Format setInputFormat(Format format);

    /**
     * Sets the format for the data this codec outputs.
     * @param format The <code>Format</code> to be set.
     * @return The <code>Format</code> that was set, which might be the
     * <code>Format</code> that most closely matched the one specified. 
     * Returns null if the specified <code>Format</code> is not supported and 
     * no reasonable match could be found. 
     */
    public Format setOutputFormat(Format format);

    /**
     * Performs the media processing defined by this codec.
     * @param input The <code>Buffer</code> that contains the media data to be processed.
     * @param output The <code>Buffer</code> in which to store the processed media data.
     * @return <CODE>BUFFER_PROCESSED_OK</CODE> if the processing is successful.  Other
     * possible return codes are defined in <CODE>PlugIn</CODE>.
     * @see PlugIn
     */
    public int process(Buffer input, Buffer output);

}

