/*
 * @(#)DataSource.java  1.3 98/12/19 
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.media.protocol.rtsp;

import java.io.*;
import javax.media.protocol.*;
import com.sun.media.*;
import com.sun.media.protocol.BasicPushBufferDataSource;
import javax.media.*;


public class DataSource extends BasicPushBufferDataSource
{
    private PushBufferStream[] srcStreams = null;
    private boolean stopped = true;
    Player streamplayer = null;
    
    public DataSource()
    {
        srcStreams= new PushBufferStream[ 1];
    }
    
    /**
     * Obtain the collection of streams that this source
     * manages. The collection of streams is entirely
     * content dependent. The mime-type of this
     * DataSource provides the only indication of
     * what streams can be available on this connection.
     *
     * @return collection of streams for this source.
     */
    
    public PushBufferStream[] getStreams() 
    {
        if (!connected)
      	    return null;
	return srcStreams;
    }
    
    public void setPlayer(Player player)
    {
        streamplayer = player;
    }
    
    public Player getPlayer(){
        return streamplayer;
    }
    
    public void setSourceStream( PushBufferStream stream)
    {
        if (srcStreams != null)
      	    srcStreams[0] = stream;
    }
    
    public void setLocator( MediaLocator mrl)
    {
        super.setLocator(mrl);
    }
    
    /**
     * Initiates data-transfer. Start must be called before
     * data is available. Connect must be called before start.
     * @exception IOException thrown if the source has IO trouble
     * at startup time.
     */
    
    public void start() throws IOException 
    {
        super.start();
    }
    
    /**
     * Stops data-transfer.
     * If the source has not already been connected and started,
     * stop does nothing.
     */
    
    public void stop() throws IOException 
    {
        super.stop();
    }
    
    public String getContentType() 
    {
	return "rtsp";
    }
    
    public boolean isStarted()
    {
        return started;
    }
    
    /**
     * Opens a connection to the source described by
     * the URL.<p>
     * Connect initiates communmication with the source.
     *
     * @exception IOException thrown if the connect has IO trouble.
     */
    
    public void connect() throws IOException 
    {
        connected = true;
    }
    
    /**
     * Close the connection to the source described by the URL. <p>
     * Disconnect frees resources used to maintain a connection
     * to the source. If no resources are in use, disconnect
     * is ignored.
     * Implies a stop, if stop hasn't already been called.
     */
    
    public void disconnect() 
    {
        connected = false;
    }
    
    /**
     * Returns an zero length array because no controls
     * are supported.
     *
     * @return a zero length <code>Object</code> array.
     */
    
    public Object[] getControls() 
    {
        return null;        
    }
    
    public void setControl( Object control)
    {
    }   

    /**
     * Returns <code>null</code> because no controls are implemented.
     *
     * @return <code>null</code>.
     */
    
    public Object getControl( String controlName) 
    {
        return null;
    }
}


