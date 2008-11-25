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

public abstract class Example10_3
{
    Processor processor;

    void test()
    {
        // --- cut from here... --- //

        // First find a capture device that will capture linear audio
        // data at 8bit 8Khz 
       
        AudioFormat format= new AudioFormat(AudioFormat.LINEAR, 
                                            8000, 
                                            8, 
                                            1); 

        Vector devices= CaptureDeviceManager.getDeviceList( format);

        CaptureDeviceInfo di= null;

        if (devices.size() > 0) {
             di = (CaptureDeviceInfo) devices.elementAt( 0);
        }
        else {
            // exit if we could not find the relevant capturedevice. 
            System.exit(-1); 
        }
        
        // Now create a processor for this capturedevice & exit if we 
        // cannot create it 
        try { 
            Processor p = Manager.createProcessor(di.getLocator()); 
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
       
            // get the output datasource of the processor  and exit 
            // if we fail 
       
            DataSource origDataSource = null; 
       
            try { 
                origDataSource = processor.getDataOutput(); 
            } catch (NotRealizedError e) { 
                System.exit(-1); 
            } 
       
            // We want to send the stream of this datasource over two 
            // RTP sessions. 
       
            // So we need to clone the output datasource of the  
            // processor and hand the clone over to the second 
            // SessionManager 
       
            DataSource cloneableDataSource = null; 
            DataSource clonedDataSource = null; 
       
            cloneableDataSource 
              = Manager.createCloneableDataSource(origDataSource); 

            clonedDataSource 
              = ((SourceCloneable)cloneableDataSource).createClone(); 
       
            // Now create the first SessionManager and hand over the
            // first datasource for SendStream creation. 
       
            SessionManager rtpsm1 
              = new com.sun.media.rtp.RTPSessionMgr(); 

            // rtpsm1.initSession(...); 
            // rtpsm1.startSession(...); 

            try {
                rtpsm1.createSendStream(cloneableDataSource, // 1st datasource 
                                        0);      
                                                 
            } catch (IOException e) {
                e.printStackTrace();
            } catch( UnsupportedFormatException e) {
                e.printStackTrace();
            }

            try {
                cloneableDataSource.connect();
                cloneableDataSource.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
       
            // create the second RTPSessionMgr and hand over the 
            // cloned datasource 
            if (clonedDataSource != null) { 
                SessionManager rtpsm2 
                  = new com.sun.media.rtp.RTPSessionMgr(); 

                // rtpsm2.initSession(...); 
                // rtpsm2.startSession(...); 

                try {
                    rtpsm2.createSendStream(clonedDataSource,0); 
                } catch (IOException e) {
                    e.printStackTrace();
                } catch( UnsupportedFormatException e) {
                    e.printStackTrace();
                }
            } 
        } 
        else { 
            // we failed to set the encoding to gsm. So deallocate 
            // and close the processor before we leave. 
       
            processor.deallocate(); 
            processor.close(); 
        }

        // --- ...until here --- // 
    }    
}
