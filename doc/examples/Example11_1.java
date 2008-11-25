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

public abstract class Example11_1
{
    Processor processor;

    void test()
    {
        // --- cut from here... --- //

        // Create a Processor for the selected file. Exit if the 
        // Processor cannot be created.
        try { 
            String url= "file:/home/foo/foo.au";

            processor 
              = Manager.createProcessor( new MediaLocator(url)); 
        } catch (IOException e) { 
            System.exit(-1); 
        } catch (NoProcessorException e) { 
            System.exit(-1); 
        } 
        
        // configure the processor
        processor.configure(); 

        // Block until the Processor has been configured 

        TrackControl track[] = processor.getTrackControls(); 
        
        boolean encodingOk = false;
        
        // Go through the tracks and try to program one of them to
        // output ulaw data. 
        for (int i = 0; i < track.length; i++) { 
            if (!encodingOk && track[i] instanceof FormatControl) { 
                       
                if (((FormatControl)track[i]).
                    setFormat( new AudioFormat(AudioFormat.ULAW_RTP, 
                                               8000, 
                       		               8, 
                           		       1)) == null) {

                    track[i].setEnabled(false); 
                }
                else {
                    encodingOk = true; 
                }
            } 
            else { 
                // we could not set this track to ulaw, so disable it 
                track[i].setEnabled(false); 
            } 
        }
        
        // At this point, we have determined where we can send out 
        // ulaw data or not. 
        // realize the processor 
        
        if (encodingOk) { 
            processor.realize(); 
            
            // block until realized. 
            // get the output datasource of the processor and exit 
            // if we fail 
            DataSource ds = null;
            
            try { 
                ds = processor.getDataOutput(); 
            } catch (NotRealizedError e) { 
                System.exit(-1);
            }
            
            // hand this datasource to manager for creating an RTP 
            // datasink.
            // our RTP datasink will multicast the audio 
            
            try {
                String url= "rtp://224.144.251.104:49150/audio/1";

                MediaLocator m = new MediaLocator(url);

                DataSink d = Manager.createDataSink(ds, m);

                d.open();
                d.start(); 
            } catch (Exception e) {
                System.exit(-1);
            }
        }     
          
        // --- ...until here --- // 
    }    
}

