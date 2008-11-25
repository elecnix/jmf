/*
 * @(#)PullBufferStream.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;
import java.io.*;
import javax.media.Buffer;


/**
 * Abstracts a read interface that data is pulled from in the form of Buffer objects
 * This interface allows a sourcestream to transfer data in the form
 * of an entire media chunk to the user of this sourcestream. The
 * media object or chunk transferred is the Buffer object as defined in
 * javax.media.Buffer. The user of the stream will allocate an empty Buffer
 * object and pass this over to the sourcestream in the read() method.
 * The source of the stream will allocate the Buffer object's data and
 * header, set them on the Buffer and send them over to the user.
 * 
 * The structure of the data and header of the Buffer object is
 * determined by the format attribute of the Buffer object or the content
 * type of the sourcestream<BR><BR>
 *
 * It is possible for the user of the stream to indicate to the
 * sourcestream to NOT allocate the data object, but to
 * instead use the data passed in the read() method. In this case,
 * the user must pass non null data object to the stream  in the Buffer
 * object. The type of data can be determined by the getDataType()
 * method on the format object returned by calling <code>getFormat</code> on
 * this bufferstream. The <code>getDataType</code> method will return a Class
 * describing the Buffer's data and can be used by the stream user to allocate its
 * own data and hand it over to the read method. 
 *
 * @see javax.media.Format
 * @see javax.media.Buffer
 * @see PullBufferDataSource
 * @since JMF 2.0 
 */
public interface PullBufferStream extends SourceStream {

    /**
     * Find out if data is available now.
     * Returns <CODE>true</CODE> if a call to <CODE>read</CODE> would block
     * for data.
     *
     * @return <CODE>true</CODE> if read would block; otherwise
     * returns <CODE>false</CODE>.
     */
    public boolean willReadBlock();

    /**
     * Block and read a buffer from the stream. <code>buffer</code> should
     * be non-null.
     * <p>
     * @throws IOException Thrown if an error occurs while reading. 
     */
    public void read(Buffer buffer)throws IOException;

    /**
     * Get the format type of the data that this source stream provides.
     * @return A <CODE>Format</CODE> object that describes the data in this stream.
     */
    public javax.media.Format getFormat();
}
