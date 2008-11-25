/*
 * @(#)MediaProxy.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import javax.media.protocol.DataSource;
import java.io.IOException;

/*
 * Typically, a <code>MediaProxy</code> reads a text configuration file
 * that contains all of the information needed to 
 * make a connection to a server and obtain media data.
 * To produce a <code>Player</code> from a <code>MediaLocator</code>
 * referencing the configuration file,
 * <code>Manger</code>:
 * <ul>
 * <li>constructs a <code>DataSource</code>
 * for the protocol described by the <code>MediaLocator</code>
 * <li>constructs a <code>MediaProxy</code> to read
 * the configuration file using the content-type of the
 * <code>DataSource</code>
 * <li> obtains a new <code>DataSource</code>
 * from the <code>MediaProxy</code>
 * <li>constructs the <code>Player</code> using the content-type of the new
 * <code>DataSource</code>
 * </ul>
 */
public interface MediaProxy extends MediaHandler {

    /**
     * Obtain the new <code>DataSource</code>.
     * The <code>DataSource</code> is already connected.
     *
     * @exception IOException Thrown when if there are IO
     * problems in reading the the original or new
     * <code>DataSource</code>.
     *
     * @exception NoDataSourceException Thrown if this proxy
     * can't produce a <code>DataSource</code>.
     * 
     * @return the new <code>DataSource</code> for this content.
     */
    public DataSource getDataSource()
	throws IOException, NoDataSourceException;

}
