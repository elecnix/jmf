import javax.media.*;
import javax.media.protocol.*;
import javax.media.format.*;
import java.io.*;

public class Example5_6 {
    
    public static void main(String [] args) {

	// ---------------- CUT HERE START ----------------- //
	
	Format formats[] = new Format[2];
	formats[0] = new AudioFormat(AudioFormat.IMA4);
	formats[1] = new VideoFormat(VideoFormat.CINEPAK);
	FileTypeDescriptor outputType =
	    new FileTypeDescriptor(FileTypeDescriptor.QUICKTIME);
	Processor p = null;
	
	try {
	    p = Manager.createRealizedProcessor(new ProcessorModel(formats,
								   outputType));
	} catch (IOException e) {
	    System.exit(-1);
	} catch (NoProcessorException e) {
	    System.exit(-1);
	} catch (CannotRealizeException e) {
	    System.exit(-1);
	}
	// get the output of the processor
	DataSource source = p.getDataOutput();
	// create a File protocol MediaLocator with the location of the file to
	// which bits are to be written
	MediaLocator dest = new MediaLocator("file://foo.mov");
	// create a datasink to do the file writing & open the sink to make sure
	// we can write to it.
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
	// now start the filewriter and processor
	try {
	    filewriter.start();
	} catch (IOException e) {
	    System.exit(-1);
	}
	p.start();
	// stop and close the processor when done capturing...
	// close the datasink when EndOfStream event is received...

	// ----------------- CUT HERE END ---------------- //
	try {
	    Thread.currentThread().sleep(4000);
	} catch (InterruptedException ie) {
	}
	p.stop();
	p.close();
	try {
	    Thread.currentThread().sleep(1000);
	} catch (InterruptedException ie) {
	}
	filewriter.close();
	try {
	    Thread.currentThread().sleep(4000);
	} catch (InterruptedException ie) {
	}

	System.exit(0);
    }
}
