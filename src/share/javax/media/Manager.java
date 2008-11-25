/*
 * @(#)Manager.java	1.76 03/04/30
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullDataSource;
import javax.media.protocol.PushDataSource;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.URLDataSource;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.SourceCloneable;
import javax.media.control.TrackControl;
import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.control.FormatControl;
import javax.media.protocol.CaptureDevice;
import javax.media.rtp.RTPManager;

// For mesages logging.
import com.sun.media.Log;

/**
 * <code>Manager</code> is the access point for obtaining
 * system dependent resources such as <code>Players</code>,
 * <code>DataSources</code>, <code>Processors</code>, <code>DataSinks</code>,
 * the system <code>TimeBase</code>, the cloneable and merging utility
 * <code>DataSources</code>.
 * <p>
 *
 * A <code>DataSource</code> is an object used to 
 * deliver time-based multimedia data that is specific
 * to a delivery protocol.
 * <p>
 * A <code>Player</code> is an object used to
 * control and render multimedia data that
 * is specific to the content type of the data.
 * <p>
 * A <code>Processor</code> is an object similar to a Player which is
 * used to process data and output the processed data.
 * <p>
 * A <code>DataSink</code> is an object that takes a <code>DataSource</code>
 * as input and renders the output to a specified destination.
 * <p>
 * A <code>DataSource</code> provides
 * a <code>Player</code>, <code>Processor</code> or <code>DataSink</code>
 * with media data;
 * a <CODE>Player</CODE>, <code>Processor</code> or <code>DataSink</code> 
 * must have a <CODE>DataSource</CODE>.
 * <code>Manager</code> provides access to a protocol and media independent
 * mechanism for constructing <code>DataSources</code>, <code>Players</code>, 
 * <code>Processors</code> and <code>DataSinks</code>. 
 *
 * <h2>Creating Players and DataSources</h2>
 *
 * <code>Manager</code> will create<code>Players</code> from a
 * <code>URL</code>, a <CODE>MediaLocator</CODE> or a <code>DataSource</code>.
 * Creating a <code>Player</code> requires the following:
 * <ul>
 * <li> Obtain the connected <code>DataSource</code> for the specified
 * protocol
 * <li> Obtain the <code>Player</code> for the content-type
 * specified by the <code>DataSource</code>
 * <li> Attach the <code>DataSource</code> to the <code>Player</code>
 * using the <code>setSource</code> method.
 * </ul>
 *
 * <h3>Finding DataSources by Protocol</h3>
 *
 * A <code>MediaLocator</code> defines a protocol for obtaining
 * content.
 * <code>DataSources</code> are identified by the protocol
 * that they support. <code>Manager</code> uses the protocol
 * name to find <code>DataSource</code> classes.
 * <p>
 * 
 * To find a <code>DataSource</code> using a <code>MediaLocator</code>,
 * <code>Manager</code> constructs a list of class names from the protocol
 * package-prefix list and the protocol name obtained
 * from the <code>MediaLocator</code>.
 * For each class name in the constructed list a new <code>DataSource</code>
 * is instantiated, the <code>MediaLocator</code> is attached,
 * and the <code>DataSource</code> is connected.
 * If no errors have occurred, the procces is considered finished and the
 * connected <code>DataSource</code> is used by
 * <code>Manager</code> in any following operations.
 * If there was an error then the next class name in the list
 * is tried.
 * The exact details of the search algorithm is described in
 * the method documentation below.
 *
 * <h3>Finding Players by Content Type</h3>
 *
 * A <code>Player</code> is a <code>MediaHandler</code>.
 * A <code>MediaHandler</code> is a an object that reads
 * data from a <code>DataSource</code>. There are three types
 * of supported <code>MediaHandler</code>: <code>MediaProxy</code>,
 * <code>Player</code> and <code>Processor</code>.
 * <p>
 *
 * <code>MediaHandlers</code> are identified by the content type that they
 * support. A <code>DataSource</code> identifies the content type
 * of the data it produces with the <code>getContentType</code> method.
 * <code>Manager</code> uses the content type name to
 * find instances of <code>MediaHandler</code>.
 * <p>
 *
 * To find a <code>MediaHandler</code> using a content type name,
 * <code>Manager</code> constructs a list of class names from
 * the content package-prefix list and the content type name.
 * For each class name in the constructed list a new <code>MediaHandler</code>
 * is instantiated, and the <code>DataSource</code> is attached to
 * the <code>MediaHandler</code> using <coded>MediaHandler.setSource</code>.
 * <p>
 *
 * If the <code>MediaHandler</code> is a <code>Player</code> and the
 * <code>setSource</code> was successful the process is finished
 * and the <code>Player</code> is returned.
 * If the <code>setSource</code> failed, another name in the
 * list is tried.
 * <p>
 *
 * If the <code>MediaHandler</code> is a <code>MediaProxy</code>
 * then a new <code>DataSource</code> is obtained from the
 * <code>MediaProxy</code>, a new list is created for the
 * content type the <code>DataSource</code> supports and the
 * whole thing is tried again.
 * <p>
 * 
 * If a valid <code>Player</code> is not found then the whole
 * procedure is repeated with "unknown" substituted
 * for the content-type name. The "unknown" content type is supported
 * by generic <code>Players</code> that are capable of handling
 * a large variety of media types, often in a platform dependent
 * way.
 *<p>
 *
 * The detailed creation algorithm is specified in the methods below.
 *
 * <h3>Creating a Realized Player</h3> 
 * Versions of <code>createRealizedPlayer</code> calls are provided as an 
 * acceleration to create a Player.  The returned player is in the
 * <I>Realized</I> state.  In addition to <code>NoPlayerException</code>
 * and <code>IOException</code>, <code>CannotRealizeException</code> can
 * be thrown if the <code>Manager</code> cannot realize the 
 * <code>Player</code>.
 * <p>
 *
 * <h2>Creating Processors</h2>
 * 
 * <code>Processors</code> are created in the same way as <code>Players</code>
 * as outlined above.  <code>Manager</code> also provides an additional way 
 * to create a Processor via the <code>createRealizedProcessor</code> call.  A 
 * <code>ProcessorModel</code> is used to fully identify the
 * input and output requirements of a <code>Processor</code>.  The
 * <code>createRealizedProcessor</code> call takes a 
 * <code>ProcessorModel</code> as input and create a <code>Processor</code> 
 * that adheres to the given
 * <code>ProcessorModel</code>.  The returned Processor is
 * in the <I>Realized</I> state.  The method is a blocking call.<p>
 * If the <code>Manager</code> fails to find a <code>Processor</code>
 * that fits the <code>ProcessorModel</code>, a 
 * <code>NoProcessorException</code> is thrown.  If there is a problem
 * creating and realizing a <code>Processor</code>, it will throw an 
 * <code>IOException</code> or <code>CannotRealizeException</code> depending 
 * on the circumstances.
 *
 * <h2>Creating DataSinks</h2>
 *
 * <code>DataSinks</code> are created from an input <code>DataSource</code>
 * <code>MediaLocator</code>.  The <code>MediaLocator</code> identifies the
 * protocol and content of the <code>DataSink</code> to be used.  The
 * search for the particular <code>DataSink</code> class to be created
 * is similar to the process of creating a <code>DataSource</code>. 
 * The detail search and creation algorithm is described in the method
 * documentation below.
 *
 * <h2>Player and Processor Threads</h2>
 *
 * <code>Players</code> and <code>Processors</code> process media data 
 * asynchronously from the main program flow.
 * This implies that a <code>Player</code> or <code>Processor</code> must 
 * often manage one or more threads.
 * The threads managed by the <code>Player</code> or <code>Processor</code>
 * are not in the thread group of the application that calls
 * <code>createPlayer</code> or <code>createProcessor</code>.
 *
 * <h2>System Time Base</h2>
 *
 * All <code>Players</code> need a <code>TimeBase</code>. Many  
 * use a system-wide <code>TimeBase</code>, often based on
 * a time-of-day clock.
 * <code>Manager</code> provides access to the system <code>TimeBase</code>
 * through <code>getSystemTimeBase</code>.
 *
 * <h2>Cloning and Merging DataSources</h2>
 *
 * <code>DataSources</code> can be cloned or merged.  If a
 * <code>DataSource</code> is cloned, more than one 
 * <code>MediaHandler</code> can use it as input.  Merging more than one
 * <code>DataSources</code> will generate one <code>DataSource</code> which
 * contains all the <code>SourceStreams</code> of the constituent
 * <code>DataSources</code><p> 
 * 
 * The <code>Manager</code> provides two methods:
 * <code>createCloneableDataSource</code> and
 * <code>createMergingDataSource</code> for such purpose.
 * <p>
 *
 * <h2>Manager Hints</h2>
 *
 * Using the <code>setHint</code> method, the preference for how
 * the <code>Manager</code> creates the objects can be specified.
 * However, a particular implementation of the <code>Manager</code>
 * can choose to ignore the requested hints.
 * @see #MAX_SECURITY
 * @see #CACHING
 * @see #LIGHTWEIGHT_RENDERER
 * @see #PLUGIN_PLAYER
 * <p>
 *
 * 
 * @since 1.0 , new methods added in 2.0
 * @see java.net.URL
 * @see MediaLocator
 * @see PackageManager
 * @see javax.media.protocol.DataSource
 * @see javax.media.protocol.URLDataSource
 * @see MediaHandler
 * @see Player
 * @see Processor
 * @see MediaProxy
 * @see TimeBase
 *
 * @version 2.0, 98/05/18.
 */

public final class Manager {

    private static String VERSION = "2.1.1e";

    /**
     * Boolean hint to turn on/off maximum security.
     */
    public static final int MAX_SECURITY = 1;

    /**
     * Boolean hint to turn on/off the use of playback caching.
     */
    public static final int  CACHING = 2;

    /**
     * Boolean hint to turn on/off the use of light weight rendering.
     * If on, the <code>Manager</code> will try to create
     * <code>Players</code> that use renderers which can interoperate with 
     * light weight GUI components.
     */
    public static final int  LIGHTWEIGHT_RENDERER = 3;

    /**
     * Boolean hint to request the <code>Manager</code> to create
     * <code>Players</code> that support
     * <code>PlugIns</code>.  Such <code>Players</code> use 
     * <code>PlugIns</code> to demultiplex, decode, render or multiplex 
     * the data.  It will also support <code>TrackControls</code> for
     * application level programming.
     */
    public static final int  PLUGIN_PLAYER = 4;


    private static int numberOfHints = 4; // Update this as you add more hints

    private static SystemTimeBase sysTimeBase = null;
    
    public final static String UNKNOWN_CONTENT_NAME = "unknown";

    private static boolean jdkInit = false;
    private static Method forName3ArgsM;
    private static Method getSystemClassLoaderM;
    private static ClassLoader systemClassLoader;
    private static Method getContextClassLoaderM;

    private static String fileSeparator = System.getProperty("file.separator");
    private static Hashtable hintTable = new Hashtable();
    static {
	/* Default values for the hints */
	hintTable.put(new Integer(MAX_SECURITY), new Boolean(false));
	hintTable.put(new Integer(CACHING), new Boolean(true));
	hintTable.put(new Integer(LIGHTWEIGHT_RENDERER), new Boolean(false));
	hintTable.put(new Integer(PLUGIN_PLAYER), new Boolean(false));
    }
    /**
     * This private constructor keeps anyone from actually
     * getting a <CODE>Manager</CODE>.
     */
    private Manager() {}

    /**
     * Returns the version string for this revision of JMF.
     */
    public static String getVersion() {
	return VERSION;
    }

    /**
     * Create a <CODE>Player</CODE> for the specified media.
     * This creates a MediaLocator from the URL and then
     * calls <CODE>createPlayer</CODE>.
     *
     * @param sourceURL The <CODE>URL</CODE> that describes the media data.
     * @return A new <CODE>Player</CODE>.
     * @exception NoPlayerException Thrown if no <CODE>Player</CODE>
     * can be found.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     */
    public static Player createPlayer(URL sourceURL)
	throws IOException, NoPlayerException {
	return createPlayer(new MediaLocator(sourceURL));
    }

    /**
     * Create a <code>Player</code> for the specified media.
     * <p>
     * The algorithm for creating a <CODE>Player</CODE> from
     * a <code>MediaLocator</code> is:
     * <ol>
     * <li>Get the protocol from the <code>MediaLocator</code>.
     * <li>Get a list of <code>DataSource</code> classes that
     * support the protocol, using the protocol package-prefix-list.
     * <li> For each source class in the list:
     * <ol>
     * <li>Instantiate a new <code>DataSource</code>,
     * <li>Call the <code>connect</code> method to connect the source.
     * <li>Get the media content-type-name (using <code>getContentType</code>)
     * from the source.
     * <li>Get a list of <code>MediaHandler</code> classes that support the
     * media-content-type-name, using the content package-prefix-list.
     * <li>For each <code>MediaHandler</code> class in the list:
     * <ol>
     * <li>Instantiate a new <code>MediaHandler</code>.
     * <li>Attach the source to the <code>MediaHandler</code> by calling
     * <code>MediaHandler.setSource</code>.
     * <li>If there are no failures, determine the type of
     * the <code>MediaHandler</code>; otherwise try the next
     * <coded>MediaHandler</code> in the list.
     * <li>If the <code>MediaHandler</code> is a <code>Player</code>,
     * return the new <code>Player</code>.
     * <li>If the <code>MediaHandler</code> is a <code>MediaProxy</code>,
     * obtain a new <code>DataSource</code> from the <code>MediaProxy</code>,
     * obtain the list of <code>MediaHandlers</code> that support the new
     * <code>DataSource</code>, and continue searching the new list.
     * </ol>
     * <li>If no <code>MediaHandler</code> is found for this source,
     * try the next source in the list.
     * </ol>
     * <li>If no <code>Player</code> is found after trying all of the sources,
     * reuse the source list.<br>
     * This time, for each source class in the list:
     * <ol>
     * <li>Instantiate the source.
     * <li>Call the <code>connect</code> method to connect to the source.
     * <li>Use the content package-prefix-list to create a list of 
     * <code>MediaHandler</code> classes that support the
     * "unknown" content-type-name.
     * <li>For each <code>MediaHandler</code> class in the list,
     * search for a <code>Player</code> as in the previous search.
     * <ol>
     * <li>If no <code>Player</code> is found after trying all of the sources,
     * a <CODE>NoPlayerException</CODE> is thrown.
     * </ol>
     * </ol>
     * @param sourceLocator A <CODE>MediaLocator</CODE> that describes
     * the media content.
     * @return A <CODE>Player</CODE> for the media described by the source.
     * @exception NoPlayerException Thrown if no <CODE>Player</CODE> can
     * be found.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     */
    public static Player createPlayer(MediaLocator sourceLocator)
	throws IOException, NoPlayerException {

        Player newPlayer = null;
	Hashtable sources = new Hashtable(10); // A repository of connected sources.
	    
	boolean needPluginPlayer = ((Boolean)Manager.getHint(PLUGIN_PLAYER)).booleanValue();

	// For RTP, the "non-plugin" player
	// (com.sun.media.content.rtp.Handler) actually supports plugin.
	String protocol = sourceLocator.getProtocol();
	if (protocol != null && 
	    (protocol.equalsIgnoreCase("rtp") || protocol.equalsIgnoreCase("rtsp")))
	    needPluginPlayer = false;

	try {
	    newPlayer = createPlayerForContent(sourceLocator, needPluginPlayer, sources);
	} catch (NoPlayerException e) {
	    // ... and if that doesn't work, try finding
	    // a player for the UNKNOWN_CONTENT_NAME.

	    if (needPluginPlayer)
		throw e;
	    newPlayer = createPlayerForContent(sourceLocator, true, sources);
	}

	// Disconnect the unsed sources
	if (sources.size() != 0) {
	    Enumeration enum = sources.elements();
	    while (enum.hasMoreElements()) {
		DataSource ds = (DataSource)enum.nextElement();
		ds.disconnect();
	    }
	}

	return newPlayer;
    }

    /**
     * Create a <code>Player</code> for the <code>DataSource</code>.
     * <p>
     * The algorithm for creating a <CODE>Player</CODE> from
     * a <code>DataSource</code> is:
     * <ol>
     * <li>Get the media content-type-name from the source by
     * calling <code>getContentType</code>.
     * <li>Use the content package-prefix-list to get a list of 
     * <code>Player</code> classes that support the media content-type name.
     * <li>For each <code>Player</code> class in the list:
     * <ol>
     * <li>Instantiate a new <code>Player</code>.
     * <li>Attach the source to the <code>Player</code> by calling
     * <code>setSource</code> on the <code>Player</code>.
     * <li>If there are no failures,  return the new <code>Player</code>;
     * otherwise,
     * try the next <code>Player</code> in the list.
      </ol>
     * <li>If no <code>Player</code> is found for this source:
     * <ol>
     * <li>Use the content package-prefix-list to create a list 
     * of <code>Player</code> classes that support the
     * "unknown" content-type-name.
     * <li>For each <code>Player</code> class in the list:
     * <ol>
     * <li>Instantiate a new <code>Player</code>.
     * <li>Attach the source to the <code>Player</code> by
     * calling <code>setSource</code> 
     * on the <code>Player</code>.
     * <li>If there are no failures, return the new <code>Player</code>;
     * otherwise, try the next <code>Player</code> in the list.
     * </ol>
     * </ol>
     * <li>If no <code>Player</code> can be created,
     * a <CODE>NoPlayerException</CODE> is thrown.
     * </ol>
     * @param DataSource The <CODE>DataSource</CODE> that describes
     * the media content.
     * @return A new <code>Player</code>.
     * @exception NoPlayerException Thrown if a <code>Player</code> can't
     * be created.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     */
    public static Player createPlayer(DataSource source)
	throws IOException, NoPlayerException {

	Player newPlayer;
	boolean needPluginPlayer = ((Boolean)Manager.getHint(PLUGIN_PLAYER)).booleanValue();
	String contentType = source.getContentType();

	// For RTP, the "non-plugin" player
	// (com.sun.media.content.rtp.Handler) actually supports plugin.
	if (contentType != null && 
	    (contentType.equalsIgnoreCase("rtp") || contentType.equalsIgnoreCase("rtsp")))
	    needPluginPlayer = false;

	try {
	    // First try and create one using the source
	    // as the content identifier ...
	    if (needPluginPlayer)
		contentType = UNKNOWN_CONTENT_NAME;
	    newPlayer = createPlayerForSource(source, contentType, null);
	} catch( NoPlayerException e) {
	    // ... if that doesn't work use the unknown-content type.
	    if (needPluginPlayer)
		throw e;
	    newPlayer = createPlayerForSource(source, UNKNOWN_CONTENT_NAME, null);
	}

	return newPlayer;
    }

    /**
     * Create a Realized <code>Player</code> for the specified media.
     * <p>
     * This is a blocking method that creates a <code>Player</code>, calls
     * realize on it and returns only after the <code>Player</code> has
     * been Realized. Realizing a <code>Player</code> could be a time consuming
     * operation and one should use caution when using this method as it
     * could block the thread for several seconds.
     * @param ml The <code>MediaLocator</code> that describes the source of
     * the media.
     * @return A new <code>Player</code> that is in the <code>Realized</code>
     * state.
     * @exception NoPlayerException Thrown if a <code>Player</code> can't
     * be created.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     * @exception CannotRealizeException Thrown if there was a problem 
     * realizing the Player.
     */
    public static Player createRealizedPlayer(URL sourceURL)
	throws IOException, NoPlayerException, CannotRealizeException {
	Player p = createPlayer(sourceURL);
	blockingCall(p, Controller.Realized);
	return p;
    }

    /**
     * Create a Realized <code>Player</code> for the specified media.
     * <p>
     * This is a blocking method that creates a <code>Player</code>, calls
     * realize on it and returns only after the <code>Player</code> has
     * been Realized. Realizing a <code>Player</code> could be a time consuming
     * operation and one should use caution when using this method as it
     * could block the thread for several seconds.
     * @param ml The <code>MediaLocator</code> that describes the source of
     * the media.
     * @return A new <code>Player</code> that is in the <code>Realized</code>
     * state.
     * @exception NoPlayerException Thrown if a <code>Player</code> can't
     * be created.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     * @exception CannotRealizeException Thrown if there was a problem 
     * realizing the Player.
     */
    public static Player createRealizedPlayer(MediaLocator ml)
	throws IOException, NoPlayerException, CannotRealizeException {
	Player p = createPlayer(ml);
	blockingCall(p, Controller.Realized);
	return p;
    }

    /**
     * Create a Realized <code>Player</code> for the specified source.
     * <p>
     * This is a blocking method that creates a <code>Player</code>, calls
     * realize on it and returns only after the <code>Player</code> has
     * been Realized. Realizing a <code>Player</code> could be a time consuming
     * operation and one should use caution when using this method as it
     * could block the thread for several seconds.
     * @param source The <code>DataSource</code> that describes the media
     * content.
     * @return A new <code>Player</code> that is in the <code>Realized</code>
     * state.
     * @exception NoPlayerException Thrown if a <code>Player</code> can't
     * be created.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     * @exception CannotRealizeException Thrown if there was a problem 
     * realizing the Player.
     */
    public static Player createRealizedPlayer(DataSource source)
	throws IOException, NoPlayerException, CannotRealizeException {
	Player p = createPlayer(source);
	blockingCall(p, Controller.Realized);
	return p;
    }

    /**
     * Create a <code>Processor</code> for the specified media.
     * <p>
     * The algorithm is similar to that for creating a <code>Player</code>
     * from a <code>URL</code>
     * @param sourceURL A <CODE>URL</CODE> that describes
     * the media content.
     * @return A <CODE>Processor</CODE> for the media described by the source.
     * @exception NoProcessorException Thrown if no <CODE>Processor</CODE> can
     * be found.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     */
    public static Processor createProcessor(URL sourceURL)
	throws IOException, NoProcessorException {
	return createProcessor(new MediaLocator(sourceURL));
    }

    /**
     * Create a <code>Processor</code> for the specified media.
     * <p>
     * The algorithm is similar to that for creating a <code>Player</code>
     * from a <code>MediaLocator</code>
     * @param sourceLocator A <CODE>MediaLocator</CODE> that describes
     * the media content.
     * @return A <CODE>Processor</CODE> for the media described by the source.
     * @exception NoProcessorException Thrown if no <CODE>Processor</CODE> can
     * be found.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     */
    public static Processor createProcessor(MediaLocator sourceLocator)
	throws IOException, NoProcessorException {

        Processor newProcessor = null;
	Hashtable sources = new Hashtable(10); // A repository of connected sources.
	    
	try {
	    newProcessor = createProcessorForContent(sourceLocator, 
					false, sources);
	} catch (NoProcessorException e) {
	    // ... and if that doesn't work, try finding
	    // a player for the UNKNOWN_CONTENT_NAME.
	    newProcessor = createProcessorForContent(sourceLocator, 
					true, sources);
	}

	// Disconnect the unsed sources
	if (sources.size() != 0) {
	    Enumeration enum = sources.elements();
	    while (enum.hasMoreElements()) {
		DataSource ds = (DataSource)enum.nextElement();
		ds.disconnect();
	    }
	}

	return newProcessor;
    }

    /**
     * Create a <code>Processor</code> for the <code>DataSource</code>.
     * <p>
     * The algorithm for creating a <CODE>Processor</CODE> is similar
     * to creating a <code>Player</code> from a <code>DataSource</code>.
     * @param DataSource The <CODE>DataSource</CODE> that describes
     * the media content.
     * @return A new <code>Processor</code>.
     * @exception NoProcessorException Thrown if a <code>Processor</code> can't
     * be created.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     */
    public static Processor createProcessor(DataSource source)
	throws IOException, NoProcessorException {

	Processor newProcessor;
	try {
	    // First try and create one using the source
	    // as the content identifier ...
	    newProcessor = createProcessorForSource(source, source.getContentType(), null);
	} catch( NoProcessorException e) {
	    // ... if that doesn't work use the unknown-content type.
	    newProcessor = createProcessorForSource(source, UNKNOWN_CONTENT_NAME, null);
	}

	return newProcessor;
    }

    /**
     * Create a Realized <code>Processor</code> for the specified
     * <code>ProcessorModel</code>.
     * <P>
     * This method accepts a <code>ProcessorModel</code> that describes
     * the required input and/or output format of the media data. It is a
     * blocking method and returns only after the <code>Processor</code>
     * reaches the <code>Realized</code> state.
     * @see ProcessorModel
     * @param model The <code>ProcessorModel</code> that describes the input and
     * output media
     * @return A new <code>Processor</code> that is in the <code>Realized</code>
     * state.
     * @exception NoProcessorException Thrown if a <code>Processor</code> can't
     * be created.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     * @exception CannotRealizeException Thrown if there was a problem 
     * realizing the Player.
     */
    public static Processor createRealizedProcessor(ProcessorModel model)
	throws IOException, NoProcessorException, CannotRealizeException {

	DataSource ds = null;
	MediaLocator ml = null;
	Processor processor = null;
	ContentDescriptor [] cds;
	ContentDescriptor rcd;
	boolean matched = false;
	Format [] reqFormats = null;
	Format    prefFormat;
	int    reqNumTracks = -1;
	int [] procToReqMap;
	TrackControl [] procTracks;
	int    i, j, k;
	int    nTracksEnabled = 0;
	boolean [] enabled;
	
	if (model == null)
	    throw new NoProcessorException("null ProcessorModel");

	// Figure out if we should use a datasource or a media locator as the
	// source. DataSource takes precedence over MediaLocator.
	ds = model.getInputDataSource();
	if (ds != null) {
	    processor = Manager.createProcessor(ds);
	} else if ((ml = model.getInputLocator()) != null) {
	    processor = Manager.createProcessor(ml);
	} else {
	    // Capture sources...
	    int nDevices = getNTypesOfCaptureDevices();
	    Vector dataSourceList = new Vector(1);
	    reqNumTracks = model.getTrackCount(nDevices);
	    reqFormats = new Format[reqNumTracks];
	    for (i = 0; i < reqNumTracks; i++) {
		reqFormats[i] = model.getOutputTrackFormat(i);
		// Look for a direct match
		Vector deviceList = CaptureDeviceManager.getDeviceList(reqFormats[i]);
		if (deviceList == null || deviceList.size() == 0) {
		    // No direct match. Find a similar device and hope to transcode
		    if (reqFormats[i] instanceof AudioFormat)
			deviceList = CaptureDeviceManager.getDeviceList(
							      new AudioFormat(null));
		    else if (reqFormats[i] instanceof VideoFormat)
			deviceList = CaptureDeviceManager.getDeviceList(
							      new VideoFormat(null));
		}

		if (deviceList.size() != 0) {
		    CaptureDeviceInfo cdi = (CaptureDeviceInfo) deviceList.elementAt(0);
		    if (cdi != null && cdi.getLocator() != null) {
			try {
			    DataSource crds = Manager.createDataSource(cdi.getLocator());
			    if (crds instanceof CaptureDevice) {
				// Set the format
				FormatControl [] fc = ((CaptureDevice)crds).getFormatControls();
				if (fc.length > 0) {
				    Format [] supported = fc[0].getSupportedFormats();
				    if (supported.length > 0) {
					for (int f = 0; f < supported.length; f++) {
					    if (supported[f].matches(reqFormats[i])) {
						Format intersect =
						    supported[f].intersects(reqFormats[i]);
						if (intersect != null) {
						    if (fc[0].setFormat(intersect) != null)
							break;
						}
					    }
					}
				    }
				}
			    }
			    // ds.connect(); ???
			    dataSourceList.addElement(crds);
			} catch (IOException ioe) {
			} catch (NoDataSourceException ndse) {
			}
		    }
		}
	    }
	    
	    if (dataSourceList.size() == 0) {
		throw new NoProcessorException("No suitable capture devices found!");
	    } else if (dataSourceList.size() > 1) {
		// Merge the datasources
		DataSource [] dataSourceArray = new DataSource[dataSourceList.size()];
		for (k = 0; k < dataSourceList.size(); k++)
		    dataSourceArray[k] = (DataSource) dataSourceList.elementAt(k);
		try {
		    ds = Manager.createMergingDataSource(dataSourceArray);
		} catch (IncompatibleSourceException ise) {
		    throw new NoProcessorException("Couldn't merge capture devices");
		}
	    } else
		ds = (DataSource) dataSourceList.elementAt(0);
	    processor = Manager.createProcessor(ds);
	}

	if (processor == null)
	    throw new NoProcessorException("Couldn't create Processor for source");

	// Configure the processor
	blockingCall(processor, Processor.Configured);

	// Content descriptor stuff
	rcd = model.getContentDescriptor();
	if (rcd == null) {
	    processor.setContentDescriptor(null);
	} else {
	    cds = processor.getSupportedContentDescriptors();
	    if (cds == null || cds.length == 0)
		throw new NoProcessorException("Processor doesn't support output");
	    
	    for (i = 0; i < cds.length; i++) {
		if (rcd.matches(cds[i])) {
		    if (processor.setContentDescriptor(cds[i]) != null) {
			matched = true;
			break;
		    } 
		}
	    }
	    if (!matched)
		throw new NoProcessorException("Processor doesn't support requested " +
					       "output ContentDescriptor");
	    
	}

	procTracks = processor.getTrackControls();
	if (procTracks != null && procTracks.length > 0) {
	    // Format stuff
	    int nValidTracks = 0;
	    for (i = 0; i < procTracks.length; i++) {
		if (procTracks[i].isEnabled())
		    nValidTracks++;
	    }
	    if (reqNumTracks == -1)
		reqNumTracks = model.getTrackCount(nValidTracks);
	    if (reqNumTracks > 0) {
		if (reqFormats == null) 
		    reqFormats = new Format[reqNumTracks];
		procToReqMap = new int[reqNumTracks];     // Whats the proc's trackNo for a
						      // requested track no.
		for (i = 0; i < reqNumTracks; i++) {
		    if (reqFormats[i] == null)
			reqFormats[i] = model.getOutputTrackFormat(i);
		    procToReqMap[i] = -1;
		}
		
		enabled = new boolean[procTracks.length];
		// First try the default track formats.
		for (i = 0; i < procTracks.length; i++) {
		    enabled[i] = false;
		    if (!procTracks[i].isEnabled())
			continue;
		    prefFormat = procTracks[i].getFormat();
		    for (j = 0; j < reqNumTracks; j++) {
			if ( procToReqMap[j] == -1 &&
			     ( reqFormats[j] == null ||
			       prefFormat.matches(reqFormats[j]) ) ) {
			    
			    if (model.isFormatAcceptable(j, prefFormat)) {
				procToReqMap[j] = i;
				enabled[i] = true;
				nTracksEnabled++;
				//procTracks[i].setFormat(prefFormat); // no need to do this
				break;
			    }
			}
		    }
		}
		
		for (i = 0; i < procTracks.length && nTracksEnabled < reqNumTracks; i++) {
		    boolean used = false;
		    Format [] suppFormats;
		    // If not enabled by processor, its not to be used
		    if (!procTracks[i].isEnabled())
			continue;
		    // Check if its already been matched with one of the requested
		    for (j = 0; j < reqNumTracks; j++)
			if (procToReqMap[j] == i)
			    used = true;
		    if (used)
			continue;
		    
		    // Get all the supported formats for this track (transcode)
		    suppFormats = procTracks[i].getSupportedFormats();
		    if (suppFormats == null || suppFormats.length == 0)
			continue;
		    
		    matched = false;
		    for (k = 0; k < suppFormats.length && !matched; k++) {
			prefFormat = suppFormats[k];
			for (j = 0; j < reqNumTracks && !matched; j++) {
			    //System.err.println("trying " + prefFormat + " && " + reqFormats[j]);   
			    if ( procToReqMap[j] == -1 &&
				 ( reqFormats[j] == null ||
				   prefFormat.matches(reqFormats[j]) ) ) {
				
				if (model.isFormatAcceptable(j, prefFormat)) {
				    if (procTracks[i].setFormat(prefFormat) != null) {
					procToReqMap[j] = i;
					enabled[i] = true;
					nTracksEnabled++;
					matched = true;
					break;
				    }
				}
			    }
			    
			}
		    }
		}
		
		if (nTracksEnabled < reqNumTracks) {
		    // What should we do if all requested tracks are not satisfied?
		    // Is failing the right thing to do
		    throw new CannotRealizeException("Unable to provide all " +
						     "requested tracks");
		}
	    }
	}

	blockingCall(processor, Controller.Realized);

	return processor;
    }

    /**
     * Create a <code>DataSource</code> for the specified media.
     *
     * @param sourceURL The <CODE>URL</CODE> that describes the media data.
     * @return A new <CODE>DataSource</CODE> for the media.
     * @exception NoDataSourceException Thrown if no <code>DataSource</code>
     * can be found.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     */
    public static DataSource createDataSource(URL sourceURL)
	throws IOException, NoDataSourceException  {
	return
	    createDataSource(new MediaLocator(sourceURL));
    }
    
    /**
     * Create a <code>DataSource</code> for the specified media.
     * <p>
     *
     * Returns a data source for the protocol specified by
     * the <CODE>MediaLocator</CODE>. The returned data source
     * is <i>connected</i>; <code>DataSource.connect</code> has been
     * invoked.
     * <p>
     *
     * The algorithm for creating a <code>DataSource</code> from
     * a <code>MediaLocator</code> is:
     * <ol>
     * <li>Get the protocol from the <code>MediaLocator</code>.
     * <li>Use the protocol package-prefix list to get a list of 
     * <code>DataSource</code> classes that
     * support the protocol.
     * <li> For each source class in the list:
     * <ol>
     * <li>Instantiate a new <code>DataSource</code>.
     * <li>Call <code>connect</code> to connect the source.
     * <li>If there are no errors, return the connected
     * source; otherwise, try the next source in the list.
     * </ol>
     * <li>If no source has been found, obtain a <code>URL</code> from the
     * <code>MediaLocator</code> and use it to create
     * a <code>URLDataSource</code>
     * <li>If no source can be found, a <CODE>NoDataSourceException</CODE>
     * is thrown.
     * </ol>
     * 
     *
     * @param sourceLocator The source protocol for the media data.
     * @return A connected <CODE>DataSource</CODE>.
     * @exception NoDataSourceException Thrown if no <CODE>DataSource</CODE>
     * can be found.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     */
    static public DataSource createDataSource(MediaLocator sourceLocator)
	throws IOException, NoDataSourceException {

	DataSource source = null;

	// For each DataSource that implements the protocol
        // that's specified in the source locator ....
	Enumeration protoList =
	    getDataSourceList(sourceLocator.getProtocol()).elements();
	while(protoList.hasMoreElements()) {
	    
	    String protoClassName = (String)protoList.nextElement();
	    try {

		// ... Try and instance a DataSource ....
		Class protoClass = getClassForName(protoClassName);
		source = (DataSource)protoClass.newInstance();

		// ... and get it connected ....
		source.setLocator(sourceLocator);
		source.connect();

		//  ... o.k. we've found one, so we're done.
		break;
		
	    } catch (ClassNotFoundException e) {
		// try another data source.
		source = null;
	    } catch (InstantiationException e) {
		// try another data source.
		source = null;
	    } catch (IllegalAccessException e) {
		// try another data source.
		source = null;
	    } catch (Exception e) {
		source = null;
		String err = "Error instantiating class: " + protoClassName + " : " + e;
		Log.error(e);
		throw new NoDataSourceException(err);
	    } catch (Error e) {
		source = null;
		String err = "Error instantiating class: " + protoClassName + " : " + e;
		Log.error(e);
		throw new NoDataSourceException(err);
	    }
	}
	
	// If we haven't found one installed,
	// we'll try and create one from a URL/URLDataSource.
// TEMPORARILY COMMENTED OUT URLDataSource: 1/15/99
// 	if( source == null) {
// 	    try {
// 		source = new URLDataSource(sourceLocator.getURL());
// 		source.connect();
// 	    } catch(MalformedURLException me) {
// 		// Can't get a URL so we're done.
// 		source = null;
// 	    }
// 	}
	
        // If we still haven't found one, we're done
	// and we don't have a source.
	if( source == null) {
	    throw new NoDataSourceException("Cannot find a DataSource for: " + sourceLocator);
	}

	Log.comment("DataSource created: " + source + "\n");

	return source;
    }
    
    /**
     * Creates a merged <code>DataSource</code> from an array of sources. 
     * All sources must be of the same type (i.e <code>PullDataSource</code>,
     * <code>PushDataSource</code>, etc.) otherwise an
     * IncompatibleSourceException is thrown.
     * The returned <code>DataSource</code> is of the same type of 
     * the given sources. Its content-type is RAW if all sources are of 
     * type RAW. Otherwise, its content-type is MIXED. 
     *
     * @param sources the <code>DataSources</code> to be merged
     * @return  a <code>DataSource</code> which contains all the streams 
     * of the original sources
     * @exception IncompatibleSourceException  if the sources are not 
     * of the same type
     */
    static public DataSource createMergingDataSource(DataSource[] sources) 
	throws IncompatibleSourceException {
	
	// check if the sources type matches
	if (sources.length == 0)
	    throw new IncompatibleSourceException("No sources");

	if (sources[0] instanceof PullDataSource) {
	    for (int i = 1; i < sources.length; i ++) {
		if (!(sources[i] instanceof PullDataSource))
		    throw new IncompatibleSourceException("One of the sources isn't matching the others"); 
	    }
	    PullDataSource pds[] = new PullDataSource[sources.length];
	    for (int i = 0; i < pds.length; i++)
		pds[i] = (PullDataSource)sources[i];
	    return reflectMDS("com.ibm.media.protocol.MergingPullDataSource", pds);
	}

	if (sources[0] instanceof PushDataSource) {
	    for (int i = 1; i < sources.length; i ++) {
		if (!(sources[i] instanceof PushDataSource))
		    throw new IncompatibleSourceException("One of the sources isn't matching the others"); 
	    }
	    PushDataSource pds[] = new PushDataSource[sources.length];
	    for (int i = 0; i < pds.length; i++)
		pds[i] = (PushDataSource)sources[i];
	    return reflectMDS("com.ibm.media.protocol.MergingPushDataSource",pds);
	}

	if (sources[0] instanceof PullBufferDataSource) {
	    for (int i = 1; i < sources.length; i ++) {
		if (!(sources[i] instanceof PullBufferDataSource))
		    throw new IncompatibleSourceException("One of the sources isn't matching the others"); 
	    }
	    PullBufferDataSource pds[] = new PullBufferDataSource[sources.length];
	    for (int i = 0; i < pds.length; i++)
		pds[i] = (PullBufferDataSource)sources[i];
	    return reflectMDS("com.ibm.media.protocol.MergingPullBufferDataSource",pds);
	}
	
	if (sources[0] instanceof PushBufferDataSource) {
	    boolean anyCapture = false;
	    for (int i = 1; i < sources.length; i ++) {
		if (!(sources[i] instanceof PushBufferDataSource))
		    throw new IncompatibleSourceException("One of the sources isn't matching the others");
		if (sources[i] instanceof CaptureDevice)
		    anyCapture = true;
	    }
	    PushBufferDataSource pds[] = new PushBufferDataSource[sources.length];
	    for (int i = 0; i < pds.length; i++)
		pds[i] = (PushBufferDataSource)sources[i];
	    if (anyCapture)
		return reflectMDS("com.ibm.media.protocol.MergingCDPushBDS",pds);
	    else
		return reflectMDS("com.ibm.media.protocol.MergingPushBufferDataSource",pds);
	}
	
	return null;
    }
    
    /**
     * Creates a cloneable <code>DataSource</code>. The returned 
     * <code>DataSource</code> implements the <code>SourceCloneable</code> 
     * interface and enables the creation of clones by the createClone method.
     * <br> 
     * If the input <code>DataSource</code> implements
     * <code>SourceCloneable</code>, it will be returned right away
     * as the result.  Otherwise, a "proxy" <code>DataSource</code> is
     * created.  It implements the <code>SourceCloneable</code> interface and
     * can be used to generate other clones.
     * <br>
     * When <code>createCloneableDataSource</code> is called on a
     * <code>DataSource</code>, the returned <code>DataSource</code> should
     * be used in place of the original <code>DataSource</code>.  Any
     * attempt to use the original <code>DataSource</code> may generate
     * unpredictable results.
     * <br>
     * The resulted cloneable <code>DataSource</code> can be used to
     * generate clones.  The clones generated may or may not has the
     * same properties of the original DataSource depending on the
     * implementation.  Therefore, they should be checked against the
     * properties required for the application.  If the original
     * <code>DataSource</code> is not SourceCloneable and a "proxy"
     * <code>DataSource</code> is resulted, the clones generated from
     * this "proxy" <code>DataSource</code> is of type
     * <code>PushDataSource or PushBufferDataSource</code> depending on 
     * the type of the original <code>DataSource</code>.  In this case,
     * each clone pushes data at the same rate that the original 
     * <code>DataSource</code> is pulled or pushed.
     *
     * @see SourceCloneable
     * @param source the DataSource to be cloned
     * @return a cloneable DataSource for the given source
     */

    static private DataSource reflectMDS(String cname, Object pds) {
	Class cls;
	Constructor cc;
	Class[] paramTypes = new Class[1];
	Object[] arg = new Object[1];

	try {
	    cls = Class.forName(cname);
	    paramTypes[0] = pds.getClass();
	    
	    cc = cls.getConstructor(paramTypes);

	    if ( cname.indexOf("PullDataSource") >= 0 ) {
		arg[0] = (PullDataSource[])pds;
	    } else if (cname.indexOf("PushDataSource") >= 0) {
		arg[0] = (PushDataSource[])pds;
	    } else if (cname.indexOf("PullBufferDataSource") >= 0) {
		arg[0] = (PullBufferDataSource[])pds;
	    } else if (cname.indexOf("PushBufferDataSource") >= 0) {
		arg[0] = (PushBufferDataSource[])pds;
	    } else if ( cname.indexOf("CDPushBDS") >= 0 ) {
		arg[0] = (PushBufferDataSource[])pds;
	    }

	    return (DataSource)(cc.newInstance(arg));
	} catch (Exception ex) {
	}

	return null;
    }
    
    static private DataSource reflectDS(String cname, DataSource source) {
	Class cls;
	Constructor cc;
	Class[] paramTypes = new Class[1];
	Object[] arg = new Object[1];

	try {
	    cls = Class.forName(cname);

	    if ( cname.indexOf("PullDataSource") >= 0) {
		paramTypes[0] = PullDataSource.class;
		arg[0] = (PullDataSource)source;
	    } else if ( cname.indexOf("PushDataSource") >= 0) {
		paramTypes[0] = PushDataSource.class;
		arg[0] = (PushDataSource)source;
	    } else if ( cname.indexOf("PullBufferDataSource") >= 0) {
		paramTypes[0] = PullBufferDataSource.class;
		arg[0] = (PullBufferDataSource)source;
	    } else if ( cname.indexOf("PushBufferDataSource") >= 0 ) {
		paramTypes[0] = PushBufferDataSource.class;
		arg[0] = (PushBufferDataSource)source;
	    }

	    cc = cls.getConstructor(paramTypes);
	    return (DataSource)(cc.newInstance(arg));
	} catch (Exception ex) {
	    // return null;
	}
	
	return null;
    }

    static public DataSource createCloneableDataSource(DataSource source) {

	if (source instanceof SourceCloneable)
	    return source;

	
	if (source instanceof CaptureDevice) {
	    // the created clone will support CaptureDevice interface
	    if (source instanceof javax.media.protocol.PullDataSource) 
		return reflectDS("com.ibm.media.protocol.CloneableCapturePullDataSource", source);
	    
	    if (source instanceof javax.media.protocol.PushDataSource) 
		return reflectDS("com.ibm.media.protocol.CloneableCapturePushDataSource", source);
	    
	    if (source instanceof javax.media.protocol.PullBufferDataSource)
		return reflectDS("com.ibm.media.protocol.CloneableCapturePullBufferDataSource", source);
	    
	    if (source instanceof javax.media.protocol.PushBufferDataSource)
		return reflectDS("com.ibm.media.protocol.CloneableCapturePushBufferDataSource", source);
	}
	
	// Otherwise create a regular non-capture DataSource
	if (source instanceof javax.media.protocol.PullDataSource) 
	    return reflectDS("com.ibm.media.protocol.CloneablePullDataSource", source);
	
	if (source instanceof javax.media.protocol.PushDataSource) 
	    return reflectDS("com.ibm.media.protocol.CloneablePushDataSource", source);

	if (source instanceof javax.media.protocol.PullBufferDataSource) 
	    return reflectDS("com.ibm.media.protocol.CloneablePullBufferDataSource", source);
	
	if (source instanceof javax.media.protocol.PushBufferDataSource)
	    return reflectDS("com.ibm.media.protocol.CloneablePushBufferDataSource", source);
	
	return null;
    }

    /**
     * Get the time-base object for the system.
     * @return The system time base.
     */
    public static TimeBase getSystemTimeBase() {
	if (sysTimeBase == null) {
	    sysTimeBase = new SystemTimeBase();
	}
	return sysTimeBase;
    }

    /**
     * Create a player for the <CODE>MediaLocator</CODE>.
     * <p>
     * If <CODE>useUnknownContent</CODE> is <CODE>true</CODE>,
     * a <CODE>Player</CODE> for
     * content-type  <CODE>UNKNOWN_CONTENT_NAME</CODE> is created; otherwise,
     * the <CODE>DataSource</CODE> determines the content-type with the
     * <code>getContentType</code> method.
     *
     * @param sourceLocator Used to determine the protocol that
     * the <CODE>DataSource</CODE> will use.
     * @param useKnownContent Used to determine the content type used
     * to find a <CODE>Player</CODE>.
     * @returns A new <CODE>Player</CODE>.
     * @exception NoPlayerException Thrown if no <CODE>Player</CODE> can
     * be found.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     */
    static Player createPlayerForContent(MediaLocator sourceLocator,
			       boolean useUnknownContent, Hashtable sources)
	throws IOException, NoPlayerException {

	Player newPlayer = null;
	boolean sourceUsed[] = new boolean[1];
	sourceUsed[0] = false;	// A pass-by-referenced boolean to indicate
				// if the data source has been used.
				// If so, it need to be disconnected.
	    
	// For each DataSource that implements the protocol
        // that's specified in the source ...

	Enumeration protoList =
	    getDataSourceList(sourceLocator.getProtocol()).elements();
	while(protoList.hasMoreElements()) {
	    
	    String protoClassName = (String)protoList.nextElement();

	    DataSource source = null; // 
	    try {

		// Look into the registry to see if that DataSource
		// has already been created.
		if ((source = (DataSource)sources.get(protoClassName)) == null) {

		    // ... Try an instance a DataSource ....
		    Class protoClass = getClassForName(protoClassName);
		    source = (DataSource)protoClass.newInstance();

		    // ... and get it connected ....
		    source.setLocator(sourceLocator);
		    source.connect();
		} else
		    sources.remove(protoClassName);

		// ... o.k. we've found one, so now try and get
		// a Player for it.
		try {
		    if( useUnknownContent) {
			// Either use the default content type ...
			newPlayer = createPlayerForSource(source, 
					UNKNOWN_CONTENT_NAME, sourceUsed);
		    } else {
			// ... or let the source specify the content type.
			newPlayer =
			    createPlayerForSource(source, 
					source.getContentType(), sourceUsed);
		    }

		    // If we got one we're done.
		    break;
		    
		} catch (NoPlayerException e) {
		    // Go try another one.
		    newPlayer = null;
		}
		
		// No luck so try another source.
		if (sourceUsed[0])
		    source.disconnect();
		else
		    sources.put(protoClassName, source);
		
	    } catch (ClassNotFoundException e) {
		// try another data source.
		source = null;
	    } catch (InstantiationException e) {
		// try another one.
		source = null;
	    } catch (IllegalAccessException e) {
		// try another one.
		source = null;
	    } catch (Exception e) {
		source = null;
		String err = "Error instantiating class: " + protoClassName + " : " + e;
		Log.error(e);
		throw new NoPlayerException(err);
	    } catch (Error e) {
		source = null;
		String err = "Error instantiating class: " + protoClassName + " : " + e;
		Log.error(e);
		throw new NoPlayerException(err);
	    }
	}

// TEMPORARILY COMMENTED OUT URLDataSource: 1/15/99
	// If we don't have a Player yet, then try and create a Player
	// from the URL data source.
// 	DataSource source = null;
// 	sourceUsed[0] = false;
// 	if( newPlayer == null) {
// 	    try {

// 		if ((source = (DataSource)sources.get("javax.media.URLDataSource")) == null) {
// 		    source = new URLDataSource(sourceLocator.getURL());
// 		    source.connect();
// 		} else
// 		    sources.remove("javax.media.URLDataSource");

// 		// Got the data source so attach it to
// 		// a player.
// 		if( useUnknownContent) {
// 		    // Either use the default content type ...
// 		    newPlayer = createPlayerForSource(source, 
// 					UNKNOWN_CONTENT_NAME, sourceUsed);
// 		} else {
// 		    // ... or let the source specify the content type.
// 		    newPlayer =
// 			createPlayerForSource(source, 
// 					source.getContentType(), sourceUsed);
// 		}

// 	    } catch(MalformedURLException me) {
// 		// Can't get a URL so we're done.
// 		source = null;
// 	    } finally {
// 		if (source != null) {
// 		    if (sourceUsed[0])
// 			source.disconnect();
// 		    else
// 	        	sources.put("javax.media.URLDataSource", source);
// 		}
// 	    }
// 	}

	if( newPlayer == null)
	    throw new NoPlayerException("Cannot find a Player for :" + sourceLocator);

	return newPlayer;
    }


    /**
     * Create a <CODE>Player</CODE> for a particular content type
     * using the source.
     *
     * @param source The source of media for the <CODE>Player</CODE>.
     * @param contentTypeName The type of content the <CODE>Player</CODE>
     * should handle.
     * @return A new <CODE>Player</CODE>.
     * @exception NoPlayerException Thrown if no <CODE>Player</CODE> can
     * be found for the source and content-type.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     */
    static Player createPlayerForSource(DataSource source, 
		String contentTypeName, boolean sourceUsed[])
		throws IOException, NoPlayerException {

	Player newPlayer = null;
	if (sourceUsed != null) sourceUsed[0] = true;

	// Try every handler we can find for this content type.
	Enumeration playerList =
	    getHandlerClassList(contentTypeName).elements();

	MediaHandler mHandler;
	DataSource newSource = null;
	while(playerList.hasMoreElements()) {
	    String handlerClassName = (String)playerList.nextElement();

	    try {
		
		// ... try and instance the handler ...
		Class handlerClass = getClassForName(handlerClassName);
		mHandler = (MediaHandler)handlerClass.newInstance();
		// ... set the DataSource on it ...
		mHandler.setSource(source);

		// if this is a Player then we're done.
		if( mHandler instanceof Player) {
		    newPlayer = (Player)mHandler;
		    break;
		}

		// Otherwise it must be a proxy.
		// Get a new data source, and content type  ...
		MediaProxy mProxy = (MediaProxy)mHandler;
		newSource = mProxy.getDataSource();
		String newContentType = newSource.getContentType();
		// .. recurse to try and create a Player with it.
		try{
		    newPlayer =
			createPlayerForSource(newSource,newContentType, null);
		}catch (NoPlayerException e){
		    newPlayer = createPlayerForSource(newSource,
						      UNKNOWN_CONTENT_NAME,
						      null);
		    if (newPlayer != null)
			break;
		}
	    
		
	    } catch (ClassNotFoundException e) {
		// Couldn't find the handler so try another.
		newPlayer = null;
		if (sourceUsed != null) sourceUsed[0] = false;
	    } catch (InstantiationException e) {
		// Can't instance the handler so try another.
		newPlayer = null;
		if (sourceUsed != null) sourceUsed[0] = false;
	    } catch (IllegalAccessException e) {
		// Can't get at the handler so try another.
		newPlayer = null;
		if (sourceUsed != null) sourceUsed[0] = false;
	    } catch (IncompatibleSourceException e) {
		// The handler didn't know what to
		// do with the DataSource so try another handler.
		newPlayer = null;
	    } catch (NoDataSourceException e) {
		// Proxy failed to produce a new data source
		// see if there are other proxies out there.
		newPlayer = null;
	    } catch (Exception e) {
		newPlayer = null;
		String err = "Error instantiating class: " + handlerClassName + " : " + e;
		throw new NoPlayerException(err);
	    } catch (Error e) {
		String err = "Error instantiating class: " + handlerClassName + " : " + e;
 		Log.error(e);
		throw new NoPlayerException(err);
	    }

	}

	if( newPlayer == null) {
	    throw new NoPlayerException("Cannot find a Player for: " + source);
	}

	Log.comment("Player created: " + newPlayer);
	Log.comment("  using DataSource: " + source + "\n");

	return newPlayer;
    }
    
    /**
     * Create a player for the <CODE>MediaLocator</CODE>.
     * <p>
     * If <CODE>useUnknownContent</CODE> is <CODE>true</CODE>,
     * a <CODE>Processor</CODE> for
     * content-type  <CODE>UNKNOWN_CONTENT_NAME</CODE> is created; otherwise,
     * the <CODE>DataSource</CODE> determines the content-type with the
     * <code>getContentType</code> method.
     *
     * @param sourceLocator Used to determine the protocol that
     * the <CODE>DataSource</CODE> will use.
     * @param useKnownContent Used to determine the content type used
     * to find a <CODE>Processor</CODE>.
     * @returns A new <CODE>Processor</CODE>.
     * @exception NoProcessorException Thrown if no <CODE>Processor</CODE> can
     * be found.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     */
    static Processor createProcessorForContent(MediaLocator sourceLocator,
			       boolean useUnknownContent, Hashtable sources)
	throws IOException, NoProcessorException {

	Processor newProcessor = null;
	boolean sourceUsed[] = new boolean[1];
	sourceUsed[0] = false;	// A pass-by-referenced boolean to indicate
				// if the data source has been used.
				// If so, it need to be disconnected.
	    
	// For each DataSource that implements the protocol
        // that's specified in the source ...
	Enumeration protoList =
	    getDataSourceList(sourceLocator.getProtocol()).elements();
	while(protoList.hasMoreElements()) {
	    
	    String protoClassName = (String)protoList.nextElement();
	    DataSource source = null;
	    try {

		// Look into the registry to see if that DataSource
		// has already been created.
		if ((source = (DataSource)sources.get(protoClassName)) == null) {

		    // ... Try an instance a DataSource ....
		    Class protoClass = getClassForName(protoClassName);
		    source = (DataSource)protoClass.newInstance();

		    // ... and get it connected ....
		    source.setLocator(sourceLocator);
		    source.connect();
		} else
		    sources.remove(protoClassName);

		// ... o.k. we've found one, so now try and get
		// a Processor for it.
		try {
		    if( useUnknownContent) {
			// Either use the default content type ...
			newProcessor = createProcessorForSource(source, 
					UNKNOWN_CONTENT_NAME, sourceUsed);
		    } else {
			// ... or let the source specify the content type.
			newProcessor =
			    createProcessorForSource(source, 
					source.getContentType(), sourceUsed);
		    }

		    // If we got one we're done.
		    break;
		    
		} catch (NoProcessorException e) {
		    // Go try another one.
		    newProcessor = null;
		}
		
		// No luck so try another source.
		if (sourceUsed[0])
		    source.disconnect();
		else
		    sources.put(protoClassName, source);
		
	    } catch (ClassNotFoundException e) {
		// try another data source.
		source = null;
	    } catch (InstantiationException e) {
		// try another one.
		source = null;
	    } catch (IllegalAccessException e) {
		// try another one.
		source = null;
	    } catch (Exception e) {
		String err = "Error instantiating class: " + protoClassName + " : " + e;
		Log.error(e);
		throw new NoProcessorException(err);
	    } catch (Error e) {
		String err = "Error instantiating class: " + protoClassName + " : " + e;
		Log.error(e);
		throw new NoProcessorException(err);
	    }
	}

	// TEMPORARILY COMMENTED OUT URLDataSource: 1/15/99
	// If we don't have a Processor yet, then try and create a Processor
	// from the URL data source.
// 	DataSource source = null;
// 	sourceUsed[0] = false;
// 	if( newProcessor == null) {
// 	    try {

// 		if ((source = (DataSource)sources.get("javax.media.URLDataSource")) == null) {
// 		    source = new URLDataSource(sourceLocator.getURL());
// 		    source.connect();
// 		} else
// 		    sources.remove("javax.media.URLDataSource");

// 		// Got the data source so attach it to
// 		// a player.
// 		if( useUnknownContent) {
// 		    // Either use the default content type ...
// 		    newProcessor = createProcessorForSource(source, 
// 					UNKNOWN_CONTENT_NAME, sourceUsed);
// 		} else {
// 		    // ... or let the source specify the content type.
// 		    newProcessor =
// 			createProcessorForSource(source, 
// 					source.getContentType(), sourceUsed);
// 		}

// 	    } catch(MalformedURLException me) {
// 		// Can't get a URL so we're done.
// 		source = null;
// 	    } finally {
// 		if (source != null) {
// 		    if (sourceUsed[0])
// 			source.disconnect();
// 		    else
// 	        	sources.put("javax.media.URLDataSource", source);
// 		}
// 	    }
// 	}

	if( newProcessor == null)
	    throw new NoProcessorException("Cannot find a Processor for: " + sourceLocator);

	return newProcessor;
    }


    /**
     * Create a <CODE>Processor</CODE> for a particular content type
     * using the source.
     *
     * @param source The source of media for the <CODE>Processor</CODE>.
     * @param contentTypeName The type of content the <CODE>Processor</CODE>
     * should handle.
     * @return A new <CODE>Processor</CODE>.
     * @exception NoProcessorException Thrown if no <CODE>Processor</CODE> can
     * be found for the source and content-type.
     * @exception IOException Thrown if there was a problem connecting
     * with the source.
     */
    static Processor createProcessorForSource(DataSource source, 
		String contentTypeName, boolean sourceUsed[])
		throws IOException, NoProcessorException {

	Processor newProcessor = null;
	if (sourceUsed != null) sourceUsed[0] = true;

	// Try every handler we can find for this content type.
	Enumeration playerList =
	    getProcessorClassList(contentTypeName).elements();

	MediaHandler mHandler;
	DataSource newSource = null;
	while(playerList.hasMoreElements()) {
	    String handlerClassName = (String)playerList.nextElement();

	    try {
		
		// ... try and instance the handler ...
		Class handlerClass = getClassForName(handlerClassName);
		mHandler = (MediaHandler)handlerClass.newInstance();
		
		// ... set the DataSource on it ...
		mHandler.setSource(source);

		// if this is a Processor then we're done.
		if( mHandler instanceof Processor) {
		    newProcessor = (Processor)mHandler;
		    break;
		}

		// Otherwise it must be a proxy.
		// Get a new data source, and content type  ...
		MediaProxy mProxy = (MediaProxy)mHandler;
		newSource = mProxy.getDataSource();
		String newContentType = newSource.getContentType();

		// .. recurse to try and create a Player with it.
		try{
		    newProcessor =
			createProcessorForSource(newSource,newContentType, null);
		}catch (NoProcessorException e){
		    newProcessor = createProcessorForSource(newSource,
							 UNKNOWN_CONTENT_NAME,
							 null);
		    if (newProcessor != null)
			break;
		}
		
	    } catch (ClassNotFoundException e) {
		// Couldn't find the handler so try another.
		newProcessor = null;
		if (sourceUsed != null) sourceUsed[0] = false;
	    } catch (InstantiationException e) {
		// Can't instance the handler so try another.
		newProcessor = null;
		if (sourceUsed != null) sourceUsed[0] = false;
	    } catch (IllegalAccessException e) {
		// Can't get at the handler so try another.
		newProcessor = null;
		if (sourceUsed != null) sourceUsed[0] = false;
	    } catch (IncompatibleSourceException e) {
		// The handler didn't know what to
		// do with the DataSource so try another handler.
		newProcessor = null;
	    } catch (NoDataSourceException e) {
		// Proxy failed to produce a new data source
		// see if there are other proxies out there.
		newProcessor = null;
	    } catch (Exception e) {
		newProcessor = null;
		String err = "Error instantiating class: " + handlerClassName + " : " + e;
		Log.error(e);
		throw new NoProcessorException(err);
	    } catch (Error e) {
		newProcessor = null;
		String err = "Error instantiating class: " + handlerClassName + " : " + e;
		Log.error(e);
		throw new NoProcessorException(err);
	    }

	}

	if( newProcessor == null) {
	    throw new NoProcessorException("Cannot find a Processor for: " + source);
	}

	Log.comment("Processor created: " + newProcessor);
	Log.comment("  using DataSource: " + source + "\n");

	return newProcessor;
    }


    /**
     * Create a <code>DataSink</code> for the specified input
     * Datasource and destination  Medialocator.
     * <p>
     * 
     * The algorithm for creating a <CODE>DataSink</CODE> from
     * a <code>MediaLocator and datasource </code> is:
     * <ol>
     * <li>Get the protocol from the <code>MediaLocator</code>.
     * <li>Get a list of <code>MediaHandler</code> classes  within the
     * datasink package that support the
     * protocol, using the content package-prefix-list.
     * i.e. search for content-prefix.media.datasink.protocol.Handler
     * <li>For each <code>MediaHandler</code> class in the list:
     * <ol>
     * <li>Instantiate a new <code>MediaHandler</code>.
     * <li>If the <code>MediaHandler</code> is a
     * <code>DataSink</code>,
     * Attach the source to the <code>MediaHandler</code> by calling
     * <code>MediaHandler.setSource</code>
     * <li>If there are no failures, return this
     * <code>DataSink</code>;otherwise try the next
     * <code>MediaHandler</code> in the list.
     * 
     * <li>If the <code>MediaHandler</code> is a <code>DataSinkProxy</code>,
     * obtain the content type of the proxy using the getContentType() method.
     * Now obtain a list of <code>MediaHandlers</code> that support
     * the protocol of the Medialocator and the content type returned by
     * the proxy <BR> 
     * i.e. look for content-prefix.media.datasink.protocol.content-type.Handler 
     * <li>If a <code>MediaHandler</code> is found and it is a
     * DataSink, attach the datasource  
     * to it by calling <code>MediaHandler.setSource</code>.<BR>
     * <li>Return the <code>DataSink</code> if there are no errors.
     *
     * <li>If no <code>MediaHandler</code> is found, or if there are
     * any errors,try the next  <code>MediaHandler</code in the list.
     * </ol>
     * <li>If no <code>DataSink</code> is found after trying all of the handlers,
     * a <CODE>NoDataSinkException</CODE> is thrown.
     * </ol>
     * </ol>
     * @param datasource The input <CODE>DataSource</CODE> to the DataSink
     * @param destLocator A <CODE>MediaLocator</CODE> that describes
     * the destination of the media to be handled by the datasink
     * 
     * @return A <CODE>DataSink</CODE> for the media described by the
     * destLocator and that supports the datasource .
     * @exception NoDataSinkException Thrown if no <CODE>DataSink</CODE> can
     * be found.
     * @see NoDataSinkException
     * 
     */
    static public DataSink createDataSink(DataSource datasource,
					  MediaLocator destLocator)
	throws NoDataSinkException {

	String handlerName = "media.datasink." + destLocator.getProtocol() +
	                     ".Handler";

	Vector classList = buildClassList(getContentPrefixList(), handlerName);

	Enumeration handlerList = classList.elements();

	DataSink dataSink = null;
	boolean done = false;
	while (!done && handlerList.hasMoreElements()) {
	    String handlerClassName = (String)handlerList.nextElement();
	    try {
		
		// ... try and instance the handler ...
		Class handlerClass = getClassForName(handlerClassName);
		Object object =  handlerClass.newInstance();
		// ... set the DataSource on it ...
		if (object instanceof DataSink) {
		    dataSink = (DataSink) object;
		    dataSink.setSource(datasource);
		    dataSink.setOutputLocator(destLocator);
		    done = true;
		    break; // we are done
		}


		// Otherwise it must be a DataSinkProxy
		// Get a new data source, and content type  ...
		DataSinkProxy dsProxy = (DataSinkProxy) object;
		String contentType = dsProxy.getContentType(destLocator);
		// .. recurse to try and create a Player with it.

		handlerName = "media.datasink." + destLocator.getProtocol() +
		    "." +
		    contentType +
		    ".Handler";

		Vector dataSinkList = buildClassList(getContentPrefixList(), handlerName);
		Enumeration elements = dataSinkList.elements();
		
		while (elements.hasMoreElements()) {
		    String dsClassName = (String) elements.nextElement();
		    try {
			dataSink = (DataSink) getClassForName(dsClassName).newInstance();
			// ... set the DataSource on it ...
			dataSink.setSource(datasource);
			dataSink.setOutputLocator(destLocator);
			done = true;
			break; // we are done
		    } catch (Exception e) {
			dataSink = null;
		    }
		}
		
	    } catch (Exception e) {
		dataSink = null;
	    } catch (Error e) {
		dataSink = null;
	    }
	}
	if ( dataSink == null )
	    throw new NoDataSinkException("Cannot find a DataSink for: " + datasource);

	Log.comment("DataSink created: " + dataSink);
	Log.comment("  using DataSource: " + datasource + "\n");

	return dataSink;
    }

    /**
     * Retrieve the directory that's used for playback caching.
     * @return the directory that's used for playback caching,
     * 		null if the directory is not specified.
     */
    public static String getCacheDirectory() {
	String cacheDir;
	Object cdir = com.sun.media.util.Registry.get("secure.cacheDir");

	if ( (cdir != null) && (cdir instanceof String) ) {
	    cacheDir = (String) cdir;;

	    if (cacheDir.indexOf(fileSeparator) == -1) {
		if (fileSeparator.equals("/")) {
		    cacheDir = "/tmp";
		} else if (fileSeparator.equals("\\")) {
		    cacheDir = "C:" + fileSeparator + "temp";
		} else {
		    cacheDir = null;
		}
	    }
	    return cacheDir;
	}

	if (fileSeparator.equals("/")) {
	    cacheDir = "/tmp";
	} else if (fileSeparator.equals("\\")) {
	    cacheDir = "C:" + fileSeparator + "temp";
	} else {
	    cacheDir = null;
	}
	return cacheDir;
    }

    /**
     * Specify a hint for the <code>Manager</code> to use.
     * @param hint The name of the hint to be set.
     * @param value The value the hint is to be set. 
     * @see #MAX_SECURITY
     * @see #CACHING
     * @see #LIGHTWEIGHT_RENDERER
     * @see #PLUGIN_PLAYER
     */
    public static void setHint(int hint, Object value) {
	if ( (value != null) &&
	     (hint >= 1) &&
	     (hint <= numberOfHints) ) { // if test not really necessary
	    hintTable.put(new Integer(hint), value);
	}
    }

    /**
     * Retrieve the value of a hint set.
     * @param hint The name of the hint.
     * @return The value of the hint.
     * @see #MAX_SECURITY
     * @see #CACHING
     * @see #LIGHTWEIGHT_RENDERER
     * @see #PLUGIN_PLAYER
     */
    public static Object getHint(int hint) {
	if ( (hint >= 1) && (hint <= numberOfHints) ) {
	    return hintTable.get(new Integer(hint));
	} else {
	    return null;
	}
    }

    static final int DONE = 0;
    static final int SUCCESS = 1;

    /**
     * Realize a player or processor.  It blocks until the player is
     * realized or if the realize fails.
     * Throws a CannotRealizeException if the realize fails.
     */
    static private void blockingCall(Player p, int state) throws CannotRealizeException {

	// Use this sort of as a pass-by-reference variable.
	boolean sync[] = new boolean[2];
	ControllerListener cl;

	sync[DONE] = false;
	sync[SUCCESS] = false;
	cl = new MCA(sync, state);

	p.addControllerListener(cl);
	if (state == Controller.Realized)
	    p.realize();
	else if (state == Processor.Configured)
	    ((Processor)p).configure();

	// Wait for notification from the controller.
	synchronized (sync) {
	    while (!sync[DONE]) {
		try {
		    sync.wait();
		} catch (InterruptedException e) {}
	    }
	}

	p.removeControllerListener(cl);
	if (!sync[SUCCESS])
	    throw new CannotRealizeException();
    }

    
    /**
     * Build a list of <CODE>DataSource</CODE> class names from the
     * protocol prefix-list and a protocol name.
     * <p>
     * The first name in the list will always be:
     * <blockquote><pre>
     * media.protocol.&lt;protocol&gt;.DataSource
     * </pre></blockquote>
     * <p>
     *
     * Each additional name looks like:
     * <blockquote><pre>
     * &lt;protocol-prefix&gt;.media.protocol.&lt;protocol&gt;.DataSource
     * </pre></blockquote>
     * for every <CODE>&lt;protocol-prefix&gt;</CODE> in the
     * protocol-prefix-list.
     *
     * @param protocol The name of the protocol the source must
     * support.
     * @return A vector of strings, where each string is
     * a <CODE>Player</CODE> class-name.
     */
    static public Vector getDataSourceList(String protocolName) {

	// The first element is the name of the protocol handler ...
	String sourceName =
	    "media.protocol." + protocolName + ".DataSource";

	return buildClassList(getProtocolPrefixList(), sourceName);
    }

    /**
     * Build a list of Player <CODE>Handler</CODE> classes from the
     * content-prefix-list and a content name.
     * <p>
     * The first name in the list will always be:
     * <blockquote><pre>
     * media.content.&lt;contentType&gt;.Handler
     * </pre></blockquote>
     * <p>
     *
     * Each additional name looks like:
     * <blockquote><pre>
     * &lt;content-prefix&gt;.media.content.&lt;contentName&gt;.Player
     * </pre></blockquote>
     * for every <CODE>&lt;content-prefix&gt;</CODE> in the
     * content-prefix-list.
     *
     * @param contentName The content type to use in the class name.
     * @return A vector of strings where each one is a <CODE>Player</CODE>
     * class-name.
     */
    static public Vector getHandlerClassList(String contentName) {

	// players are found by content type ....
	String handlerName = "media.content." + 
			ContentDescriptor.mimeTypeToPackageName(contentName) + 
			".Handler";

	// ... build a list of classes using the content-prefix-list.
	return buildClassList(getContentPrefixList(), handlerName);
    }

    /**
     * Build a list of Processor <CODE>Handler</CODE> classes from the
     * content-prefix-list and a content name.
     * <p>
     * The first name in the list will always be:
     * <blockquote><pre>
     * media.processor.&lt;contentType&gt;.Handler
     * </pre></blockquote>
     * <p>
     *
     * Each additional name looks like:
     * <blockquote><pre>
     * &lt;content-prefix&gt;.media.processor.&lt;contentName&gt;.Processor
     * </pre></blockquote>
     * for every <CODE>&lt;content-prefix&gt;</CODE> in the
     * content-prefix-list.
     *
     * @param contentName The content type to use in the class name.
     * @return A vector of strings where each one is a <CODE>Processor</CODE>
     * class-name.
     */
    static public Vector getProcessorClassList(String contentName) {

	// players are found by content type ....
	String handlerName = "media.processor." + 
			ContentDescriptor.mimeTypeToPackageName(contentName) + 
			".Handler";

	// ... build a list of classes using the content-prefix-list.
	return buildClassList(getContentPrefixList(), handlerName);
    }

    /**
     * Build a list of complete class names.
     *<p>
     * 
     * For each element of the prefix-list
     * the following element is added to the list:
     * <blockquote><pre>
     *    &lt;prefix&gt;.&lt;name&gt;
     * </pre></blockquote>
     * These are added to the list in the same order as the prefixes appear
     * in the prefix-list.
     * </ol>
     * 
     * @param prefixList The list of prefixes to prepend to the class name.
     * @param name The name of the class to build the list for.
     * @return A vector of class name strings.
     */
    static Vector buildClassList(Vector prefixList, String name) {
	
	// New list which has the name as the first element ...
	Vector classList = new Vector();

	// Try and instance one directly from the classpath
	// if it's there.
	// $jdr: This has been objected to as confusing,
	// the argument for it's inclusion is that it
	// gives the user (via the classpath) a way
	// of modifying the search list at run time
	// for all applications.
	classList.addElement(name);

	// ... for each prefix append the name and put it
	// in the class list ...
	Enumeration prefix = prefixList.elements();
	while( prefix.hasMoreElements()) {
	    String prefixName = (String)prefix.nextElement();
	    classList.addElement(prefixName + "." + name);
	}

	// ... done
	return classList;
    }
    

    static Vector getContentPrefixList() {
	return (Vector)PackageManager.getContentPrefixList().clone();
    }

    static Vector getProtocolPrefixList() {
	return (Vector) PackageManager.getProtocolPrefixList().clone();
    }

    private static int getNTypesOfCaptureDevices() {
	int nDevices = 0;
	Vector audioDevs = CaptureDeviceManager.getDeviceList(new AudioFormat(null));
	Vector videoDevs = CaptureDeviceManager.getDeviceList(new VideoFormat(null));
	if (audioDevs != null && audioDevs.size() > 0)
	    nDevices++;
	if (videoDevs != null && videoDevs.size() > 0)
	    nDevices++;
	return nDevices;
    }


    private static boolean checkIfJDK12() {
	if (jdkInit)
	    return (forName3ArgsM != null);
	jdkInit = true;
	try {
	    forName3ArgsM = Class.class.getMethod("forName",
						  new Class[] {
		String.class, boolean.class, ClassLoader.class
		    });
	    
	    getSystemClassLoaderM = ClassLoader.class.getMethod("getSystemClassLoader", null);

	    // TODO: may need to invoke RuntimePermission("getClassLoader") privilege
	    systemClassLoader = (ClassLoader) getSystemClassLoaderM.invoke(ClassLoader.class, null);

	    getContextClassLoaderM = Thread.class.getMethod("getContextClassLoader", null);

	    return true;
	} catch (Throwable t) {
	    forName3ArgsM = null;
	    return false;
	}
    }

    // This is a Package private class
    static Class getClassForName(String className) 
                throws ClassNotFoundException {
	/**
	 *  Note: if we don't want this functionality
	 *  just replace it with Class.forName(className)
	 */

	try {
	    return Class.forName(className);
	} catch (Exception e) {
	    if (!checkIfJDK12()) {
		throw new ClassNotFoundException(e.getMessage());
	    }
	} catch (Error e) {
	    if (!checkIfJDK12()) {
		throw e;
	    }
	}

	/**
	 *  In jdk1.2 application, when you have jmf.jar in the ext directory and
	 *  you want to access a class that is not in jmf.jar but is in the CLASSPATH,
	 *  you have to load it using the the system class loader.
	 */
	try {
	    return (Class) forName3ArgsM.invoke(Class.class, new Object[] {
		className, new Boolean(true), systemClassLoader});
	} catch (Throwable e) {
	}

	/**
	 *  In jdk1.2 applet, when you have jmf.jar in the ext directory and
	 *  you want to access a class that is not in jmf.jar but applet codebase,
	 *  you have to load it using the the context class loader.
	 */
	try {
	    // TODO: may need to invoke RuntimePermission("getClassLoader") privilege
	    ClassLoader contextClassLoader =
		(ClassLoader) getContextClassLoaderM.invoke(Thread.currentThread(), null);
	    return (Class) forName3ArgsM.invoke(Class.class, new Object[] {
		className, new Boolean(true), contextClassLoader});
	} catch (Exception e) {
	    throw new ClassNotFoundException(e.getMessage());
	} catch (Error e) {
	    throw e;
	}
    }

    
}


/**
 * A utility class as a ControllerLister to implement the blocking realize.
 */
class MCA extends ControllerAdapter {

    // Pass-by-reference for this class.
    boolean sync[];
    int     state;
    
    public MCA(boolean sync[], int state) {
	this.sync = sync;
	this.state = state;
    }

    private void succeed() {
	synchronized (sync) {
	    sync[Manager.DONE] = true;
	    sync[Manager.SUCCESS] = true;
	    sync.notify();
	}
    }

    private void fail() {
	synchronized (sync) {
	    sync[Manager.DONE] = true;
	    sync[Manager.SUCCESS] = false;
	    sync.notify();
	}
    }

    public void configureComplete(ConfigureCompleteEvent evt) {
	if (state == Processor.Configured)
	    succeed();
    }
    
    public void realizeComplete(RealizeCompleteEvent evt) {
	if (state == Controller.Realized)
	    succeed();
    }

    public void controllerError(ControllerErrorEvent evt) {
	fail();
    }

    public void deallocate(DeallocateEvent evt) {
	fail();
    }

    public void controllerClosed(ControllerClosedEvent evt) {
	fail();
    }
}

