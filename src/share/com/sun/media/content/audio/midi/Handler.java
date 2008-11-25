/*
 * @(#)Handler.java	1.16 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.content.audio.midi;

import java.io.*;
import javax.media.*;
import javax.media.protocol. *;
import javax.sound.midi.*;
import com.sun.media.*;
import com.sun.media.protocol.*;
import com.sun.media.parser.BasicPullParser;
import com.sun.media.parser.BasicTrack;
import com.sun.media.controls.GainControlAdapter;


public class Handler extends BasicPlayer {

    private MidiController controller;
    protected javax.media.protocol.DataSource datasource = null;
    private boolean closed = false;
    private int META_EVENT_END_OF_MEDIA = 47;
    private Control [] controls = null;

    public Handler() {
	controller = new MidiController();
	manageController(controller);
    }


    public void setSource(javax.media.protocol.DataSource source)
	throws IOException, IncompatibleSourceException {
	super.setSource(source);
	controller.setSource(source);
	// datasource = source;
    }

    // TODO: check with ivan
    protected boolean audioEnabled() {
	return true;
    }

    protected boolean videoEnabled() {
	return false;
    }

    // TODO: check with ivan. Use AudioTimeBase
    protected TimeBase getMasterTimeBase() {
	return controller.getMasterTimeBase();
    }

    // TODO: check with ivan
    public void updateStats() {
    }

    //    class MidiController extends BasicController implements MidiDevice.Listener {
    class MidiController extends BasicController implements
                           MetaEventListener {
	//	    ControllerEventListener {
	private MidiParser midiParser;
	private javax.media.Track track = null;
	private Buffer buffer = new Buffer();
	private PullSourceStream stream;
	private Sequencer sequencer = null;
	private Synthesizer synthesizer = null;
	protected MidiChannel channels[]; 
	private Sequence sequence = null;
	private byte[] mididata = null;
	private MidiFileInputStream is = null;
	private Time duration = Duration.DURATION_UNKNOWN;
	private GCA gc;

	protected boolean isConfigurable() {
	    return false;
	}


	public void setSource(javax.media.protocol.DataSource source)
	    throws IOException, IncompatibleSourceException {
	    // super.setSource(source);
	    midiParser = new MidiParser();
	    midiParser.setSource(source);
	    datasource = source;
	}

	// TODO: check with ivan
	protected TimeBase getMasterTimeBase() {
	    return new SystemTimeBase();
	}


	protected boolean doRealize() {
	    if (datasource == null)
		return false;
	    
	    try {
		datasource.start();
	    } catch (IOException e) {
		return false;
	    }
	    stream = midiParser.getStream();

	    long contentLength = stream.getContentLength();
	    long minLocation = 0;
	    long maxLocation;
	    int bufferSize;

	    minLocation = 0;
	    if ( contentLength != SourceStream.LENGTH_UNKNOWN ) {
		// maxLocation = contentLength - 1;
		maxLocation = contentLength;
		bufferSize = (int) contentLength;
	    } else {
		maxLocation = Long.MAX_VALUE;
		bufferSize = (int) maxLocation;
	    }

	    int numBuffers = 1;
	    track = new BasicTrack(midiParser, // parser
				   null, // format
				/*enabled=*/ true,
				   Duration.DURATION_UNKNOWN, // duration
				   new Time(0), // start time
				   numBuffers,
				   bufferSize,
				   stream,
				   minLocation,
				   maxLocation
				   );
	    
	    return true;
	}


	protected boolean doPrefetch() {
	    if (track == null)
		return false;

	    if (sequencer == null) {
		try {
		    sequencer = MidiSystem.getSequencer();
		    // System.out.println("sequencer is " + sequencer);

		    if (sequencer instanceof Synthesizer) {
			synthesizer = (Synthesizer)sequencer;
			channels = synthesizer.getChannels();
		    } 

		} catch (MidiUnavailableException e) {
		    return false;
		}
		sequencer.addMetaEventListener( (MetaEventListener) this );
	    }

	    if (buffer.getLength() == 0) {
		// Read is done only once.
		track.readFrame(buffer);

		if (buffer.isDiscard() || buffer.isEOM()) {
		    buffer.setLength(0);
		    return false;
		}
		mididata = (byte[]) buffer.getData();
		// System.out.println("buffer length is " + buffer.getLength());
		is = new MidiFileInputStream(mididata,
							 buffer.getLength());
	    }

	    synchronized(this) {
		if (is != null) {
		    try {
			is.rewind(); // Exception will never be thrown
		    } catch (Exception e) {
		    }
// 		    fs = MidiSystem.getFileStream(is);
// 		    if (fs == null) {
// 			return false;
// 		    }


		    // TODO: Bug in JavaSound. getDuration from Sequence returns 0
		    // When it works, you have to set fs to null
		    // call is.rewind and create fs again $$$$$
// 		    if (sequence == null) {
// 			sequence = MidiSystem.getSequence(fs);
// 			System.out.println("sequence is " + sequence);
// 			if (sequence != null) {
// 			    long durationNano = sequence.getDuration();
// 			    System.out.println("durationNano is " + durationNano);
// 			    duration = new Time(durationNano);
// 			}
// 		    }
		} else {
		    return false;
		}
	    }

	    try {
		sequencer.open();
	    } catch (MidiUnavailableException e) {
		// Typically if audio device cannot be grabbed
		// or IOException if not data in the stream
		Log.error("Cannot open sequencer " + e + "\n");
		return false;
	    } catch (Exception e) {
		Log.error("Cannot open sequencer " + e + "\n");
		return false;
	    }

	    try {
		sequencer.setSequence(new BufferedInputStream(is)); // TODO: avoid using BufferedInputStream
		long durationNano = sequencer.getMicrosecondLength() * 1000;
		// System.out.println("durationNano is " + durationNano);
		duration = new Time(durationNano);
		// sequencer.setSequence(is);
	    } catch (InvalidMidiDataException e) {
		Log.error("Invalid Midi Data " + e + "\n");
		sequencer.close();
		return false;
	    } catch (Exception e) {
		Log.error("Error setting sequence " + e + "\n");
		sequencer.close();
		return false;
	    }

	    return true;
	}

	protected void abortRealize() {
	}

	protected void abortPrefetch() {
	    if ( (sequencer != null) && sequencer.isOpen() ) {
		sequencer.close();
	    }
	}

	protected void doStart() {
	    if (sequencer == null)
		return;

	    // sequencer should have been successfully in prefetch
	    // for the control to come here.
	    if (!sequencer.isOpen())
		return;
	    sequencer.start();
	}

	protected void doStop() {
	    if (sequencer == null)
		return;

	    sequencer.stop();

	    sendEvent( new StopByRequestEvent(this, Started,
					      Prefetched,
					      getTargetState(),
					      getMediaTime()));

	}


	protected void doDeallocate() {
	    if (sequencer == null)
		return;
	    // Do I need to check sequencer.isOpen() before calling close
	    synchronized(this) {
		try {
		    sequencer.close();
// 		    if (fs != null) {
// 			fs.close();
// 			fs = null;
// 		    }
		} catch (Exception e) {
		    Log.error("Exception when deallocating: " + e + "\n");
		}
	    }
	} 

	protected void doClose() {
	    if (closed)
		return;

	    doDeallocate();
	    // Disconnect the data source
	    if (datasource != null) {
		datasource.disconnect();
	    }
	    datasource = null;
	    sequencer.removeMetaEventListener(this);
	    closed = true;
	    super.doClose();
	}


	protected float doSetRate(float factor) {
	    if (sequencer != null) {
		sequencer.setTempoFactor(factor);
		return sequencer.getTempoFactor();
	    } else {
		return 1.F;
	    }
	}

	protected void doSetMediaTime(Time when) {
	    // System.out.println("doSetMediaTime: " + when.getNanoseconds());
	    if ( (when != null) && (sequencer != null) ) {
		sequencer.setMicrosecondPosition(when.getNanoseconds()/1000);
	    }
	}

	//	public void update( MidiDevice.Event event ) {
	public void meta(MetaMessage me) {
 	    // System.out.println("meta: " + me + " : " +
 	    //		       me.getType());
	    if (me.getType() != META_EVENT_END_OF_MEDIA)
		return;
	    
	    if ( (sequencer != null) &&
		 (sequencer.isOpen()) ) {
		stopControllerOnly();
		sequencer.stop(); // Is this necessary
		if (duration == Duration.DURATION_UNKNOWN) {
		    duration = getMediaTime();
		    sendEvent(new DurationUpdateEvent(this, duration));
		}
 		// System.out.println("Got Sequencer.EOM");
		
		sendEvent(new EndOfMediaEvent(this, Started, Prefetched,
					      getTargetState(), getMediaTime()));

 	    }

	}

// 	public void controlChange( ShortEvent event ) {
// 	    System.out.println(" ShortEvent received: " + event);
// 	}

	public Time getDuration() {
	    return duration;
	}

	public Control [] getControls() {
	    if (controls == null) {
		controls = new Control[1];
		gc = new GCA();
		controls[0] = gc;
	    }
	    return controls;
	}

	public void gainChange(float g) {
	    if ( (channels == null) || (gc == null) )
		return;
	    // No need to check if mute is on or off
	    float level = gc.getLevel();

	    for (int i = 0; i < channels.length; i++) {                
 		channels[i].controlChange(7, (int)(level * 127.0));
	    }
	}

	public void muteChange(boolean muted) {
	    if (channels == null)
		return;

	    for (int i = 0; i < channels.length; i++) {
		channels[i].setMute(muted);
	    }
	}

	class GCA extends GainControlAdapter {
	    
	    GCA() {
		super(1.0f);
	    }
	    
	    public void setMute(boolean mute) {
		super.setMute(mute);
		muteChange(mute);
	    }
	    
	    public float setLevel(float g) {
		float level = super.setLevel(g);
		gainChange(g);
		return level;
	    }
	}

    } // class MidiController

    class MidiParser extends BasicPullParser {
	
	public ContentDescriptor [] getSupportedInputContentDescriptors() {
	    return null; // method not used
	}

// 	public void setSource(DataSource source)
// 	    throws IOException, IncompatibleSourceException {
// 	    super.setSource(source);
// 	}

	public PullSourceStream getStream() {
	    PullSourceStream stream = (PullSourceStream) streams[0];
	    return stream;
	}


	public javax.media.Track[] getTracks() throws IOException, BadHeaderException {
	    //	    return new javax.media.Track[0];
	    return null;
	}

	public Time setPosition(Time where, int rounding) {
	    return null;
	}

	public Time getMediaTime() {
	    return null;
	}

	public Time getDuration() {
	    return null;
	}

	public String getName() {
	    return "Parser for MIDI file format";
	}


    } // class MidiParser


    class MidiFileInputStream extends InputStream {
	private int length;
	private int index = 0;
	private byte[] data;
	private int markpos = 0;

	MidiFileInputStream(byte[] data, int length) {
	    this.data = data;
	    this.length = length;
	}

	public void rewind() {
	    index = 0;
	    markpos = 0;
	}

// 	public boolean markSupported() {
// 	    System.out.println("markSupported: returns true");
// 	    return true;
// 	}

// 	public synchronized void mark(int readlimit) {
// 	    System.out.println("mark: readlimit, index " + readlimit +
// 			       " : " + index);
// 	    markpos = index;
// 	}

// 	public synchronized void reset() throws IOException {
// 	    System.out.println("reset: " + markpos);
// 	    index = markpos;
// 	}


	public int read() throws IOException {
	    if (index >= length)
		return -1;
	    else
		return (int) data[index++];
	}

	// TODO: synchronize
	public int available() throws IOException {
	    return (length - index);
	}
	
	public int read(byte b[]) throws IOException {
	    // TODO: check bounds
	    return read(b, 0, b.length);
	}

	public int read(byte b[], int off, int len) throws IOException {
	    // TODO: check bounds
	    if (len > available())
		len = available();
	    if (len == 0)
		return -1;
	    System.arraycopy(data, index, b, off, len);
	    index += len;
	    return len;
	}

    } // class MidiFileInputStream



}
