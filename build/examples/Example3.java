import java.net.*;
import java.util.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.rtp.rtcp.*;

public abstract class Example3 implements ControllerListener, 
                                          ReceiveStreamListener
{
    DataSource         rtpsource;
    Player             newplayer;
    ControllerListener listener;

    // --- cut from here... --- //

    public void update(ReceiveStreamEvent event) {
        if (event instanceof RemotePayloadChangeEvent) {
            // payload has changed. we need to close the old player and
            // create a new player  
            
            if (newplayer != null) {
                // stop player and wait for stop event
                newplayer.stop();
               
                // block until StopEvent received...
       
                // remove controllerlistener
                newplayer.removeControllerListener(listener);
               
                // remove any visual and control components 
                // attached to this application
               
                // close the player and wait for close event    
                newplayer.close();
               
                // block until ControllerClosedEvent received...
       
                try {
                    // when the player was closed, its datasource was
                    // disconnected. Now we must reconnect the datasource before
                    // a player can be created for it.This is the same datasource
                    // received from NewReceiveStreamEvent and used to create the 
                    // initial rtp player
                    
                    rtpsource.connect();
                    newplayer = Manager.createPlayer(rtpsource);
               
                    if (newplayer == null) {
                        System.err.println("Could not create player for new payload");
                        return;
                    }
                    
                    newplayer.addControllerListener(listener);
                    newplayer.realize();
       
                    // when the new player is realized, retrieve its visual and 
                    // control components
                } catch (Exception e) {
                    System.err.println("could not create player for new payload");
                }
            }
        }
    }

    // --- ...until here --- //
}
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
