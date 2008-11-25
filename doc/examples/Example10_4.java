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

public abstract class Example10_4
{
    Processor  processor;

    void test()
    {
        // --- cut from here... --- //

        // Find a capture device that will capture linear 8bit 8Khz 
        // audio 

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
 
        // Since we have located a capturedevice, create a data 
        // source for it. 

        DataSource origDataSource= null;

        try { 
            origDataSource = Manager.createDataSource(di.getLocator()); 
        } catch (IOException e) { 
            System.exit(-1); 
        } catch (NoDataSourceException e) { 
           System.exit(-1); 
        }
         
        SourceStream streams[] = ((PushDataSource)origDataSource)
                                   .getStreams(); 
       
        DataSource cloneableDataSource = null;
        DataSource clonedDataSource = null;

        if (streams.length == 1) { 
            cloneableDataSource 
              = Manager.createCloneableDataSource(origDataSource); 
            
            clonedDataSource 
              = ((SourceCloneable)cloneableDataSource).createClone(); 
        } 
        else { 
            // DataSource has more than 1 stream and we should try to 
            // set the encodings of these streams to dvi and gsm 
        } 
       
        // at this point, we have a cloneable data source and its clone,
        // Create one processor from each of these datasources. 
       
       Processor p1 = null;

        try { 
            p1 = Manager.createProcessor(cloneableDataSource); 
        } catch (IOException e) { 
            System.exit(-1); 
        } catch (NoProcessorException e) { 
            System.exit(-1); 
        } 
       
        p1.configure(); 
       
        // block until configured. 

        TrackControl track[] = p1.getTrackControls(); 
        boolean encodingOk = false; 
       
        // Go through the tracks and try to program one of them 
        // to output gsm data 
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
                track[i].setEnabled(false); 
            } 
        }
       
        if (encodingOk) { 
            processor.realize(); 
            // block until realized. 
            // ...
            // get the output datasource of the processor 
            DataSource ds = null; 
            
            try { 
                ds = processor.getDataOutput(); 
            } catch (NotRealizedError e) { 
                System.exit(-1); 
            } 
           
            // Now create the first SessionManager and hand over the 
            // first datasource for SendStream creation . 
       
            SessionManager rtpsm1 
              = new com.sun.media.rtp.RTPSessionMgr(); 

            // rtpsm1.initSession(...); 
            // rtpsm1.startSession(...); 

            try {
                rtpsm1.createSendStream(ds, // first datasource 
                                        0); // first sourcestream of 
                                            // first datasource 
            } catch (IOException e) {
                e.printStackTrace();
            } catch( UnsupportedFormatException e) {
                e.printStackTrace();
            }
        }
       
        // Now repeat the above with the cloned data source and 
        // set the encoding to dvi. i.e create a processor with 
        // inputdatasource clonedDataSource
        // and set encoding of one of its tracks to dvi. 
        // create SessionManager giving it the output datasource of 
        // this processor. 

        // --- ...until here --- //
    }
}
