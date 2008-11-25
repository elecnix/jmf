/*
 * @(#)MediaHandler.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import javax.media.protocol.DataSource;
import java.io.IOException;

/**
 * <code>MediaHandler</code> is the base interface for objects
 * that read and manage media content delivered from a
 * <code>DataSource</code>.
 * <p>
 * 
 * There are currently three supported types of <code>MediaHandler/code>:
 * <code>Player</code>, <code>MediaProxy</code> and <code>DataSink</code>.
 **/
 
public interface MediaHandler {

    /**
     * Sets the media source this <code>MediaHandler</code>
     * should use to obtain content.
     *
     * @param source The <code>DataSource</code> used
     * by this <code>MediaHandler</code>.
     *
     * @exception IOException Thrown if there is an error
     * using the <code>DataSource</code>
     *
     * @exception IncompatibleSourceException Thrown if
     * this <code>MediaHandler</code> cannot make use
     * of the <code>DataSource</code>.
     */
    public void setSource(DataSource source)
	throws IOException, IncompatibleSourceException;

}
