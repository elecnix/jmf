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

public abstract class Example10_2
{
    Processor processor;

    void test()
    {
        // --- cut from here... --- //


        // First, we'll need a DataSource that captures live audio: 
       
        AudioFormat format = new AudioFormat(AudioFormat.ULAW, 
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
      
       
        // Create a processor for this capture device & exit if we 
        // cannot create it 
        try { 
            Processor p = Manager.createProcessor(di.getLocator()); 
        } catch (IOException e) { 
            System.exit(-1); 
        } catch (NoProcessorException e) { 
            System.exit(-1); 
        } 

        // at this point, we have succesfully created the processor. 
        // Realize it and block until it is configured. 
       
        processor.configure(); 
       
        // block until it has been configured 
       
        processor.setContentDescriptor( 
            new ContentDescriptor( ContentDescriptor.RAW));
       
        TrackControl track[] = processor.getTrackControls();
       
        boolean encodingOk = false;
       
        // Go through the tracks and try to program one of them to
        // output ULAW_RTP data.
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
                // we could not set this track to gsm, so disable it
                track[i].setEnabled(false); 
            } 
        } 
        
        // Realize it and block until it is realized.
        processor.realize();    
       
        // block until realized. 
       
        // get the output datasource of the processor  and exit 
        // if we fail 
       
        DataSource ds = null; 
       
        try { 
            ds = processor.getDataOutput(); 
        } catch (NotRealizedError e){ 
            System.exit(-1); 
        } 
        
        // Create a SessionManager and hand over the  
        // datasource for SendStream creation. 
       
        SessionManager rtpsm = new com.sun.media.rtp.RTPSessionMgr(); 
        
        // rtpsm.initSession(...); 
        // rtpsm.startSession(...); 

        try {
            rtpsm.createSendStream(ds, 0);
        } catch (IOException e)	{
            e.printStackTrace();
        } catch( UnsupportedFormatException e) {
            e.printStackTrace();
        }

        // --- ...until here --- //
    }
}
