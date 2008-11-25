
import javax.media.*;
import javax.media.protocol.*;
import javax.media.control.*;
import javax.media.format.*;
import java.io.*;
import java.util.Vector;

public class Example5_5 {

    public void doCaptureAndSave() {

	

	CaptureDeviceInfo di = null;
	Processor p = null;
	StateHelper sh = null;
	Vector deviceList = CaptureDeviceManager.getDeviceList(new
				AudioFormat(AudioFormat.LINEAR, 44100, 16, 2));
	if (deviceList.size() > 0)
	    di = (CaptureDeviceInfo)deviceList.firstElement();
	else
	    // Exit if we can't find a device that does linear, 44100Hz, 16 bit,
	    // stereo audio.
	    System.exit(-1);
	try {
	    p = Manager.createProcessor(di.getLocator());
	    sh = new StateHelper(p);
	} catch (IOException e) {
	    System.exit(-1);
	} catch (NoProcessorException e) {
	    System.exit(-1);
	}

	// ---------------- CUT HERE ------------------ //

	// Configure the processor
	if (!sh.configure(10000))
	    System.exit(-1);
	// Set the output content type
	p.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.WAVE));

	// Get the track control objects
	TrackControl track[] = p.getTrackControls();
	boolean encodingPossible = false;
	// Go through the tracks and try to program one of them
	// to output ima4 data.
	for (int i = 0; i < track.length; i++) {
	    try {
		track[i].setFormat(new AudioFormat(AudioFormat.IMA4_MS));
		encodingPossible = true;
	    } catch (Exception e) {
		// cannot convert to ima4
		track[i].setEnabled(false);
	    }
	}

	if (!encodingPossible) {
	    sh.close();
	    System.exit(-1);
	}
	// Realize the processor
	if (!sh.realize(10000))
	    System.exit(-1);

	// ------------- CUT HERE -------------- //
	
	// get the output of the processor
	DataSource source = p.getDataOutput();
	// create a File protocol MediaLocator with the location of the
	// file to which the data is to be written
	MediaLocator dest = new MediaLocator("file://foo.wav");
	// create a datasink to do the file writing & open the sink to
	// make sure we can write to it.
	DataSink filewriter = null;
	try {
	    filewriter = Manager.createDataSink(source, dest);
	    filewriter.open();
	} catch (NoDataSinkException e) {
	    System.exit(-1);
	} catch (IOException e) {
	    System.exit(-1);
	} catch (SecurityException e) {
	    System.exit(-1);
	}
	// if the Processor implements StreamWriterControl, we can
	// call setStreamSizeLimit
	// to set a limit on the size of the file that is written.
	StreamWriterControl swc = (StreamWriterControl)
	    p.getControl("javax.media.control.StreamWriterControl");
	//set limit to 5MB
	if (swc != null)
	    swc.setStreamSizeLimit(5000000);

	// now start the filewriter and processor
	try {
	    filewriter.start();
	} catch (IOException e) {
	    System.exit(-1);
	}
	// Capture for 5 seconds
	sh.playToEndOfMedia(5000);
	sh.close();
	// Wait for an EndOfStream from the DataSink and close it...
	filewriter.close();
	
    }

    public static void main(String [] args) {
	(new Example5_5()).doCaptureAndSave();
	System.exit(0);
    }
}
