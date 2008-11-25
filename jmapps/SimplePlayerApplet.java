/*
 * @(#)SimplePlayerApplet.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.lang.String;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import java.util.Properties;
import javax.media.*;

/**
  * This is a Java Applet that demonstrates how to create a simple
  * media player with a media event listener. It will play the
  * media clip right away and continuously loop.
  * 
  * <!-- Sample HTML
  * <applet code=SimplePlayerApplet width=320 height=300>
  * <param name=file value="sun.avi">
  * </applet>
  * -->
  */
public class SimplePlayerApplet extends Applet implements ControllerListener {
    
    // media Player
    Player player = null;
    // component in which video is playing
    Component visualComponent = null;
    // controls gain, position, start, stop
    Component controlComponent = null;
    // displays progress during download
    Component progressBar = null;
    boolean firstTime = true;
    long CachingSize = 0L;    
    Panel panel = null;
    int controlPanelHeight = 0;
    int videoWidth = 0;
    int videoHeight = 0;

    /**
     * Read the applet file parameter and create the media 
     * player.
     */
    public void init() {
	setLayout(null);
	panel = new Panel();
	panel.setLayout( null );
	add(panel);
	panel.setBounds(0, 0, 320, 240);

	// input file name from html param
	String mediaFile = null;
	// URL for our media file
	MediaLocator mrl = null;
	URL url = null;

	// Get the media filename info.
	// The applet tag should contain the path to the
 	// source media file, relative to the html page.
	
	if ((mediaFile = getParameter("FILE")) == null)
	    Fatal("Invalid media file parameter");

	try {
	    url = new URL(getDocumentBase(), mediaFile);
	    mediaFile = url.toExternalForm();
	} catch (MalformedURLException mue) {
	}
	
	try {
	    // Create a media locator from the file name
	    if ((mrl = new MediaLocator(mediaFile)) == null)
		Fatal("Can't build URL for " + mediaFile);

 	    // Create an instance of a player for this media
	    try {
		player = Manager.createPlayer(mrl);
	    } catch (NoPlayerException e) {
		System.out.println(e);
		Fatal("Could not create player for " + mrl);
	    }

	    // Add ourselves as a listener for a player's events
	    player.addControllerListener(this);

	} catch (MalformedURLException e) {
	    Fatal("Invalid media file URL!");
	} catch (IOException e) {
	    Fatal("IO exception creating player for " + mrl);
	}

	// This applet assumes that its start() calls 
	// player.start(). This causes the player to become
	// realized. Once realized, the applet will get
	// the visual and control panel components and add
	// them to the Applet. These components are not added
	// during init() because they are long operations that
	// would make us appear unresposive to the user.
    }

    /**
     * Start media file playback. This function is called the
     * first time that the Applet runs and every
     * time the user re-enters the page.
     */

    public void start() {
	//$ System.out.println("Applet.start() is called");
        // Call start() to prefetch and start the player.
        if (player != null)
	    player.start();
    }

    /**
     * Stop media file playback and release resource before
     * leaving the page.
     */
    public void stop() {
	//$ System.out.println("Applet.stop() is called");
        if (player != null) {
            player.stop();
            player.deallocate();
        }
    }

    public void destroy() {
	//$ System.out.println("Applet.destroy() is called");
	player.close();
    }


    /**
     * This controllerUpdate function must be defined in order to
     * implement a ControllerListener interface. This 
     * function will be called whenever there is a media event
     */
    public synchronized void controllerUpdate(ControllerEvent event) {
	// If we're getting messages from a dead player, 
	// just leave
	if (player == null)
	    return;
	
	// When the player is Realized, get the visual 
	// and control components and add them to the Applet
	if (event instanceof RealizeCompleteEvent) {
	    if (progressBar != null) {
		panel.remove(progressBar);
		progressBar = null;
	    }

	    int width = 320;
	    int height = 0;
	    if (controlComponent == null)
		if (( controlComponent = 
		      player.getControlPanelComponent()) != null) {
		    
		    controlPanelHeight = controlComponent.getPreferredSize().height;
		    panel.add(controlComponent);
		    height += controlPanelHeight;
		}
	    if (visualComponent == null)
		if (( visualComponent =
		      player.getVisualComponent())!= null) {
		    panel.add(visualComponent);
		    Dimension videoSize = visualComponent.getPreferredSize();
		    videoWidth = videoSize.width;
		    videoHeight = videoSize.height;
		    width = videoWidth;
		    height += videoHeight;
		    visualComponent.setBounds(0, 0, videoWidth, videoHeight);
		}

	    panel.setBounds(0, 0, width, height);
	    if (controlComponent != null) {
		controlComponent.setBounds(0, videoHeight,
					   width, controlPanelHeight);
		controlComponent.invalidate();
	    }
	    
 	} else if (event instanceof CachingControlEvent) {
	    if (player.getState() > Controller.Realizing)
		return;
	    // Put a progress bar up when downloading starts, 
	    // take it down when downloading ends.
	    CachingControlEvent e = (CachingControlEvent) event;
	    CachingControl cc = e.getCachingControl();

	    // Add the bar if not already there ...
	    if (progressBar == null) {
	        if ((progressBar = cc.getControlComponent()) != null) {
		    panel.add(progressBar);
		    panel.setSize(progressBar.getPreferredSize());
		    validate();
		}
	    }
	} else if (event instanceof EndOfMediaEvent) {
	    // We've reached the end of the media; rewind and
	    // start over
	    player.setMediaTime(new Time(0));
	    player.start();
	} else if (event instanceof ControllerErrorEvent) {
	    // Tell TypicalPlayerApplet.start() to call it a day
	    player = null;
	    Fatal(((ControllerErrorEvent)event).getMessage());
        } else if (event instanceof ControllerClosedEvent) {
	    panel.removeAll();
	}
    }

    void Fatal (String s) {
	// Applications will make various choices about what
	// to do here. We print a message
	System.err.println("FATAL ERROR: " + s);
	throw new Error(s); // Invoke the uncaught exception
			    // handler System.exit() is another
			    // choice.
	
    }
}
