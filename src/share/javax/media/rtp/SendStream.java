/*
 * @(#)SendStream.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;

import java.io.IOException;
import javax.media.rtp.rtcp.*;

/**
 * Interface SendStream -- interface representing a sending stream
 * within an  RTP session. <P>
 */
public interface SendStream 
extends RTPStream 
{
    /**
     * Changes the source description (SDES) reports being sent in RTCP
     * packets for  this sending stream. </P>
     *
     * @param sourceDesc The new source description data. <P>
     */
    public void 
    setSourceDescription( SourceDescription[] sourceDesc);
    /**
     * Removes the stream from the session.  When this method is called
     * the RTPSM  deallocates all resources associated with this stream
     * and releases references to this object  as well as the Player
     * which had been providing the send stream.   <P>
     */
    public void
    close();
    /**
     * Will temporarily stop the RTPSendStream i.e. the local
     * participant will stop sending out data on the network at this time.
     * @exception IOException Thrown if there was any IO problems when
     * stopping the RTPSendStream. A stop to the sendstream will also
     * cause a stop() to be called on the stream's datasource. This
     * could also throw an IOException, consistent with datasources in
     * JMF.  
     */
    public void
    stop() throws IOException;
    /**
     * Will resume data transmission over the network on this
     RTPSendStream.
     * @exception IOException Thrown if there was any IO problems when
     * starting the RTPSendStream. A start to the sendstream will also
     * cause a start() to be called on the stream's datasource. This
     * could also throw an IOException, consistent with datasources in
     * JMF.  
     */
    public void
    start() throws IOException;
    
    /*
     * Set the bit rate of the outgoing network data for this
     * sendstream. The send stream will send data out at the rate it is
     * received from the datasource of this stream as a default
     * value. This rate can be changed by setting the bit rate on the
     * sendstream in which case the SessionManager will perform some
     * amount of buffering to maintain the desired outgoing bitrate
     * @param the bit rate at which data should be sent over the
     network
     * @returns the actual bit rate set.
     */
    public int
    setBitRate(int bitRate);

    /**
     * Retrieve statistics on data and control packets transmitted on
     * this RTPSendStream. All the transmission statistics pertain to
     * this particular RTPSendStream only. Global statistics on all data
     * sent out from this participant (host) can be retrieved using
     * getGlobalTransmission() method from the SessionManager.
     */
    public TransmissionStats
    getSourceTransmissionStats();
}



