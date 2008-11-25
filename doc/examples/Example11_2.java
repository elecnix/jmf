import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.rtp.rtcp.*;

public abstract class Example11_2 implements ReceiveStreamListener
{
    // --- cut from here... --- //

    // method to be implemented by ReceiveStreamListener

    public void update(ReceiveStreamEvent event) {
        // find the source session manager for this event
        SessionManager source = (SessionManager)event.getSource();
       
        // create a filewriter datasink if a new ReceiveStream 
        // is detected
        if (event instanceof NewReceiveStreamEvent) {
            String cname = "Java Media Player";
            ReceiveStream stream = null;
            
            try {
                // get the ReceiveStream
                stream =((NewReceiveStreamEvent)event)
                        .getReceiveStream();

                Participant part = stream.getParticipant();

                // get the ReceiveStream datasource
                DataSource dsource = stream.getDataSource();

                // hand this datasource over to a file datasink
                MediaLocator f = new MediaLocator("file://foo.au");

                Manager.createDataSink(dsource, f);    
            } catch (Exception e) {
                System.err.println("newReceiveStreamEvent exception " 
                                   + e.getMessage());
                return;
            }
        }
    }

    // --- ...until here --- // 
}
