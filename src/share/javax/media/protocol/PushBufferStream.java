/*
 * @(#)PushBufferStream.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.protocol;

import java.io.IOException;
import javax.media.Buffer;

/**
 * Abstracts a read interface that pushes data in the form of Buffer objects
 * This interface allows a source stream to transfer data in the form
 * of an entire media chunk to the user of this source stream. The
 * media object or chunk transferred is the Buffer object as defined in
 * javax.media.Buffer. The user of the stream will allocate an empty Buffer
 * object and pass this over to the source stream in the read() method.
 * The source stream will allocate the Buffer object's data and
 * header, set them on the Buffer and send them over to the user.<BR><BR>
 * 
 * The structure of the data and header of the Buffer object is
 * determined by the format attribute of the Buffer object or the content type of
 * the source stream<BR><BR>
 *
 * It is possible for the user of the stream to indicate to the
 * source stream to NOT allocate the data object, but to
 * instead use the data passed in the read() method. In this case,
 * the user must pass non null data object to the stream  in the Buffer
 * object. The type of data can be determined by the getDataType()
 * method on the format object returned by calling getFormat() on this
 * bufferstream. The <code>getDataType</code> method will return an object
 * describing the Buffer's data and can be used by the stream user to allocate its
 * own data and hand it over to the read method.  <BR><BR>
 * 
 * @see javax.media.Format 
 * @see PushBufferDataSource
 * @since JMF 2.0
 */
public interface PushBufferStream extends SourceStream {

    /**
     * Get the format tupe of the data that this source stream provides.
     * @return A <CODE>Format</CODE> object that describes the data in this stream.
     */
    public javax.media.Format getFormat();

    /**
     * Read from the stream without blocking.
     * @throws IOException Thrown if an error occurs while reading
     *
     */
    public void read(Buffer buffer) throws IOException;

    /**
     * Register an object to service data transfers to this stream.
     * <p>
     * If a handler is already registered when
     * <CODE>setTransferHandler</CODE> is called,
     * the handler is replaced;
     * there can only be one handler at a time. This method should be
     * invoked with a parameter value of <code>null</code> if the currently
     * registered handler doesn't wish to be notified of available data anymore.
     * 
     * @param transferHandler The handler to transfer data to.
     */
    public void setTransferHandler(BufferTransferHandler transferHandler);
}
