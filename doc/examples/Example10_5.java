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

public abstract class Example10_5
{
    Processor processor;

    void test()
    {
        // --- cut from here... --- //

        // Find a capture device that will capture linear audio 
        // data at 8bit 8Khz 
        
        AudioFormat format = new AudioFormat(AudioFormat.LINEAR, 
                                             8000, 
                                             8, 
                                             1); 
 
        Vector devices= CaptureDeviceManager.getDeviceList( format);

        CaptureDeviceInfo di= null;
        if (devices.size() > 0) {
             di = (CaptureDeviceInfo) devices.elementAt( 0);
        }
        else {
            // exit if we could not find the relevant capture device.
            System.exit(-1); 
        }

        // Create a processor for this capturedevice & exit if we 
        // cannot create it 
        
        try { 
            processor = Manager.createProcessor(di.getLocator()); 
        } catch (IOException e) { 
            System.exit(-1); 
        } catch (NoProcessorException e) { 
            System.exit(-1); 
        } 
        
        // configure the processor  
        processor.configure(); 
       
        // block until it has been configured 
       
        processor.setContentDescriptor( 
            new ContentDescriptor( ContentDescriptor.RAW));
       
        TrackControl track[] = processor.getTrackControls(); 
        boolean encodingOk = false;
       
        // Go through the tracks and try to program one of them to
        // output gsm data. 
        for (int i = 0; i < track.length; i++) { 
            if (!encodingOk && track[i] instanceof FormatControl) { 

                if (((FormatControl)track[i]).
                    setFormat( new AudioFormat(AudioFormat.GSM_RTP, 
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
                // we could not set this track to gsm, so disable it 
                track[i].setEnabled(false); 
            } 
        } 
        
        // At this point, we have determined where we can send out 
        // gsm data or not. 
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
            // datasink
            // our RTP datasimnk will multicast the audio 
            
            try {
                MediaLocator m = new MediaLocator("rtpraw://");
                // here, manager will look for a datasink in 
                // <protocol.prefix>.media.protocol.rtpraw.DataSink 
                // the datasink will create an RTPSocket at
                // <protocol.prefix>.media.protocol.rtpraw.DataSource 
                // and sink all RTP data to this socket.
               
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
