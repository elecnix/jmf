
import javax.media.*;
import javax.media.protocol.*;
import javax.media.control.*;
import javax.media.format.*;
import java.io.*;
import java.util.Vector;

public class Example5_4 {

    public void doCaptureAndSave() {

	
	// ---------------- CUT HERE ------------------ //

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
	// Configure the processor
	if (!sh.configure(10000))
	    System.exit(-1);
	// Set the output content type and realize the processor
	p.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.WAVE));
	if (!sh.realize(10000))
	    System.exit(-1);
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
	
	// ------------- CUT HERE -------------- //
    }

    public static void main(String [] args) {
	(new Example5_4()).doCaptureAndSave();
	System.exit(0);
    }
}
