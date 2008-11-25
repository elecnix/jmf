/*
 * @(#)ProgressSlider.java	1.37 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.ui;

import java.security.*;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.awt.*;
import java.awt.event.*;
import javax.media.*;
import com.sun.media.controls.*;
import com.sun.media.util.*;
import com.sun.media.JMFSecurity;
import com.sun.media.JMFSecurityManager;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;


public class ProgressSlider extends BasicComp
       implements MouseListener, MouseMotionListener, Runnable, ComponentListener, ControllerListener
{
    Image imageGrabber;
    Image imageGrabberDown;

    int grabberWidth;
    int grabberHeight;
    boolean grabbed;
    boolean entered;
    int grabberPosition;
    int leftBorder = 0;
    int rightBorder = 0;
    int sliderWidth;
    MediaThread timer = null;
    protected boolean justSeeked = false;
    protected boolean stopTimer = false;
    
    private static JMFSecurity jmfSecurity = null;
    private static boolean securityPrivelege=false;
    private Method m[] = new Method[1];
    private Class cl[] = new Class[1];
    private Object args[][] = new Object[1][0];
    private Player player;
    private DefaultControlPanel controlPanel;

    private ToolTip    toolTip = null;
    private double     progressCaching = 1.0; // 0.0 - 1.0
  
    private boolean resetMediaTime = false;
    
    static {
	try {
	    jmfSecurity = JMFSecurityManager.getJMFSecurity();
	    securityPrivelege = true;
	} catch (SecurityException e) {
	}
    }

    public ProgressSlider(String label, DefaultControlPanel cp, Player p) {
	super(label);
	player = p;
	controlPanel = cp;
	// Load the images
	imageGrabber = fetchImage("grabber.gif");
	imageGrabberDown = fetchImage("grabber-pressed.gif");

	// Get the sizes of the images
	grabberWidth = imageGrabber.getWidth(this);
	grabberHeight = imageGrabber.getHeight(this);
	leftBorder = grabberWidth / 2;
	rightBorder = leftBorder;

	addMouseListener( this );
	addMouseMotionListener( this );
	grabberPosition = 0;
	grabbed = false;
	entered = false;
	this.height = 18;
	this.width = 20;
	sliderWidth = this.width - leftBorder - rightBorder;

        this.addComponentListener ( this );
        player.addControllerListener ( this );
    }

    public void addNotify() {
	super.addNotify();
	    
	if ( /*securityPrivelege && */ (jmfSecurity != null) ) {
	    String permission = null;
	    try {
		if (jmfSecurity.getName().startsWith("jmf-security")) {
		    permission = "thread";
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.THREAD);
		    m[0].invoke(cl[0], args[0]);
		    
		    permission = "thread group";
		    jmfSecurity.requestPermission(m, cl, args, JMFSecurity.THREAD_GROUP);
		    m[0].invoke(cl[0], args[0]);
		} else if (jmfSecurity.getName().startsWith("internet")) {
		    PolicyEngine.checkPermission(PermissionID.THREAD);
		    PolicyEngine.assertPermission(PermissionID.THREAD);
		}
	    } catch (Throwable e) {
		if (JMFSecurityManager.DEBUG) {
		    System.err.println("Unable to get " + permission +
				       " privilege  " + e);
		}
		securityPrivelege = false;
		// TODO: Do the right thing if permissions cannot be obtained.
		// User should be notified via an event
	    }
	}

 	if ( (jmfSecurity != null) && (jmfSecurity.getName().startsWith("jdk12"))) {
	    try {
		final ProgressSlider slider = this;
		Constructor cons = jdk12CreateThreadRunnableAction.cons;
		
		timer = (MediaThread) jdk12.doPrivM.invoke(
                                           jdk12.ac,
					   new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               MediaThread.class,
                                               this
                                           })});

		timer.setName("Progress Slider thread");
		// timer.useControlPriority();

		
		cons = jdk12PriorityAction.cons;
		jdk12.doPrivM.invoke(
				     jdk12.ac,
				     new Object[] {
 					  cons.newInstance(
 					   new Object[] {
                                               timer,
                                               new Integer(timer.getControlPriority())
                                           })});
		stopTimer = false;
		timer.start();
		
	    } catch (Exception e) {
	    }

 	} else {
	    timer = new MediaThread(this);
	    timer.setName("Progress Slider thread");
	    timer.useControlPriority();
	    stopTimer = false;
	    timer.start();
 	}

    }


    // Cannot make removeNotify synchronized.  It will deadlock
    // with other mouse event listeners.  So we'll have to create
    // another lock to synchronize removeNotify and dispose.
    Object disposeLock = new Object();
    Object syncStop = new Object();
    
    public void removeNotify() {
	if (timer != null) {
	    synchronized (syncStop) {
		stopTimer = true;
		timer = null;
	    }
	}
	synchronized (disposeLock) {
            if ( toolTip != null ) {
        	toolTip.setVisible ( false );
        	//toolTip = null;
            }
	}
	super.removeNotify();
    }

    public synchronized void dispose() {
	synchronized (syncStop) {
	    if (timer != null) {
		stopTimer = true;
	    }
	}
	removeMouseListener( this );
	removeMouseMotionListener( this );
        removeComponentListener ( this );
	synchronized (disposeLock) {
	    if (toolTip != null) {
		toolTip.dispose();
		toolTip = null;
	    }
	}
	timer = null;
	player = null;
    }

    public void run() {
	int counter = 0;
	int pausecnt = -1;
	int sleepTime;
	boolean doUpdate = true;

	while (!stopTimer) {
	  try {
	    if ( player != null && player.getState() == Controller.Started) {
		doUpdate = true;
		pausecnt = -1;
	    } else if (player != null && pausecnt < 5) {
		pausecnt ++;
		doUpdate = true;
	    } else if ( resetMediaTime ) {
		doUpdate = true;
		resetMediaTime = false;
	    } else {
		doUpdate = false;
	    }
	  
	    try {
		if (doUpdate) {
		    long nanoDuration = player.getDuration().getNanoseconds();
	      
		    if (nanoDuration > 0) {
			long nanoTime = player.getMediaNanoseconds();
			// Update the time less often if the slider is not
			// present.
			seek((float)nanoTime / nanoDuration);
			if ( grabbed == false )
			    updateToolTip ( nanoTime );
		    }
		}
	    } catch (Exception e) { }

	    sleepTime = (isEnabled() ? 200 : 1000);

	    try { Thread.sleep(sleepTime); } catch (Exception e) {}
	  
	    counter++;
	    if (counter == 1000/sleepTime) {
		counter = 0;
		controlPanel.update();
	    }
	  
	    if (justSeeked) {
		justSeeked = false;
		try { Thread.sleep(1000); } catch (Exception e) {}
	    }
	  } catch (Exception e) {
	  }
	}
    }

    public void paint(Graphics g) {

	// Draw the rod
	int            x;
	int            y;
	int            grabberY;
        int            downloadCredit;
        String         strTime;
        Font           font;
        FontMetrics    fontMetrics;

	if (isEnabled()) {
            y = (this.height / 2) - 2;
            grabberY = (this.height / 2) - (grabberHeight / 2);
            g.setColor( getBackground().darker() );
	    g.drawRect (leftBorder, y, sliderWidth, 3);
            g.setColor(getBackground());
            downloadCredit = grabberWidth;
	    g.draw3DRect(leftBorder, y, (int)((sliderWidth - downloadCredit) * progressCaching + downloadCredit), 3, false);

	    // Paint the download display
	    /*
	    if ((displayControl != null) && displayControl.isEnable()) {
		int percent = (int) displayControl.getMaxValue();
		if (percent < 100) {
		    g.setColor(Color.yellow);  // Loading zone
		    x = sliderWidth * percent / 100 + 3;
		    y += 2;
		    g.drawLine(x, y, this.width - 4, y);

		    g.setColor(Color.green);
		    g.drawLine(0, y, x, y);
		} else {
		    g.setColor(Color.green);
		    y += 2;
		    g.drawLine(0, y, this.width - 4, y);
		}
	    }
	    */

	    // Paint the grabber
	    if (grabbed || entered)
		g.drawImage(imageGrabberDown,
			    grabberPosition + leftBorder - grabberWidth / 2, grabberY,
			    this);
	    else
		g.drawImage(imageGrabber,
			    grabberPosition + leftBorder - grabberWidth / 2, grabberY,
			    this);
	}
        else if (player != null) {
            strTime = formatTime ( player.getMediaNanoseconds() );
            font = this. getFont ();
            g.setFont ( font );
            fontMetrics = this.getFontMetrics ( font );
            g.drawString ( strTime, 2, 2 + fontMetrics.getAscent() );
        }
    }

    private void sliderSeek(float fraction) {
	if (player == null)
	    return;
	long value = (long) (fraction * player.getDuration().getNanoseconds());
	justSeeked = true;
	if (value >= 0) {
	    player.setMediaTime(new Time(value));
	    controlPanel.resetPauseCount();
	    controlPanel.update();
	}
    }

    public void seek(float fraction) {
	if (justSeeked)
	    return;
	if (!grabbed) {
	    int newPosition = (int) (fraction * sliderWidth);
	    if (newPosition > sliderWidth)
		newPosition = sliderWidth;
	    if (newPosition < 0)
		newPosition = 0;
	    if (grabberPosition != newPosition || !isEnabled()) {
		grabberPosition = newPosition;
		repaint();
	    }
	}
    }

    public Dimension getPreferredSize() {
        return new Dimension(20, this.height);
    }

    public float sliderToSeek(int x) {
	float s = (float)(x) / (float)(sliderWidth);
	return s;
    }

    public int mouseToSlider(int x) {
	if (x < leftBorder)
	    x = leftBorder;
	if (x > this.width - rightBorder)
	    x = this.width - rightBorder;

	x -= leftBorder;
	return x;
    }

    public void mousePressed(MouseEvent me) {
	if (!isEnabled())
	    return;
	grabbed = true;
	grabberPosition = mouseToSlider(me.getX());
	repaint();
    }

    public synchronized void mouseReleased(MouseEvent me) {
	if (!isEnabled())
	    return;
	grabbed = false;
	grabberPosition = mouseToSlider(me.getX());
	float seek = sliderToSeek(grabberPosition);
	sliderSeek(seek);
        if ( toolTip != null  &&  entered == false ) {
            toolTip.dispose ();
            toolTip = null;
        }
	repaint();
    }

    public void mouseClicked(MouseEvent me) {
    }

    public synchronized void mouseEntered(MouseEvent me) {
//        Dimension    dim;
        Point        pointScreen;

	entered = true;
        if ( toolTip == null  &&  isEnabled() && player != null) {
            toolTip = new ToolTip ( "time/duration" );
            updateToolTip ( player.getMediaNanoseconds() );
            if ( this.isShowing() ) {
                pointScreen = this.getLocationOnScreen ();
//                dim = toolTip.getSize ();
//                pointScreen.x += me.getX() - dim.width - 2;
//                pointScreen.y += me.getY();
//                dim = this.getSize ();
                pointScreen.y += this.height + 4;
                toolTip.setLocation ( pointScreen );
                toolTip.show ();
            }
        }
	repaint();
    }

    public synchronized void mouseExited(MouseEvent me) {
        if ( toolTip != null  &&  grabbed == false ) {
            toolTip.dispose ();
            toolTip = null;
        }
	if (!isEnabled())
	    return;
	entered = false;
	repaint();
    }

    public synchronized void mouseMoved(MouseEvent me) {
        Dimension    dim;
        Point        pointScreen;

        if ( toolTip != null  &&  this.isShowing() ) {
            dim = toolTip.getSize ();
            pointScreen = this.getLocationOnScreen ();
            pointScreen.x += me.getX() - dim.width - 2;
            pointScreen.y += me.getY();
//            toolTip.setLocation ( pointScreen );
        }
    }

    public synchronized void mouseDragged(MouseEvent me) {
        float    seek;
        long     value;

	if (!isEnabled() || player == null)
	    return;
	int newPosition = mouseToSlider(me.getX());
	if (newPosition != grabberPosition) {
	    grabberPosition = newPosition;
            seek = sliderToSeek(grabberPosition);
	    if (player.getState() != Controller.Started)
		sliderSeek(seek);
            value = (long) (seek * player.getDuration().getNanoseconds());
            updateToolTip ( value );
	    repaint();
	}
    }

    public void componentResized ( ComponentEvent event ) {
        Dimension    dim;

        dim = this.getSize ();
        if ( dim.width - leftBorder - rightBorder < 1 )
            return;

	grabberPosition = (int) (grabberPosition * ((float)(dim.width-leftBorder-rightBorder) / (this.width - leftBorder - rightBorder)));
	this.width = dim.width;
	sliderWidth = this.width - leftBorder - rightBorder;
   }

    public void componentMoved ( ComponentEvent event ) {
    }

    public void componentShown ( ComponentEvent event ) {
    }

    public void componentHidden ( ComponentEvent event ) {
    }

    public synchronized void controllerUpdate ( ControllerEvent event ) {
        CachingControl    cachingControl;
        long              length;
        long              progress;

	if ( event instanceof CachingControlEvent ) {
	    cachingControl = ((CachingControlEvent)event).getCachingControl();
            length = cachingControl.getContentLength ();
            progress = cachingControl.getContentProgress ();
            progressCaching = (double)progress / length;
            repaint ();
	} else if ( event instanceof MediaTimeSetEvent) {
	  resetMediaTime = true;
	}
    }

    private String formatTime ( Time time ) {
	long    nano;
        String  strTime = new String ( "<unknown>" );

	if ( time == null  ||  time == Time.TIME_UNKNOWN  ||  time == javax.media.Duration.DURATION_UNKNOWN )
	    return ( strTime );

	nano = time.getNanoseconds();
        strTime = formatTime ( nano );
	return ( strTime );
    }

    private String formatTime ( long nanoSeconds ) {
	int     hours;
	int     minutes;
	int     seconds;
	int     hours10;
	int     minutes10;
	int     seconds10;
	long    nano10;
        String  strTime;

	seconds = (int) (nanoSeconds / Time.ONE_SECOND);
	hours = seconds / 3600;
	minutes = ( seconds - hours * 3600 ) / 60;
	seconds = seconds - hours * 3600 - minutes * 60;
	nanoSeconds = (long) ((nanoSeconds % Time.ONE_SECOND) / (Time.ONE_SECOND/100));

        hours10 = hours / 10;
        hours = hours % 10;
        minutes10 = minutes / 10;
        minutes = minutes % 10;
        seconds10 = seconds / 10;
        seconds = seconds % 10;
        nano10 = nanoSeconds / 10;
        nanoSeconds = nanoSeconds % 10;

        strTime = new String ( "" + hours10 + hours + ":" + minutes10 + minutes + ":" + seconds10 + seconds + "." + nano10 + nanoSeconds );
	return ( strTime );
    }

    public void updateToolTip ( long nanoMedia ) {
        String       strTool;
        Time         timeDuration;

        if ( toolTip == null || player == null )
            return;

        timeDuration = player.getDuration ();
        strTool = new String ( formatTime(nanoMedia) + " / " + formatTime(timeDuration) );
        toolTip.setText ( strTool );
    }
}


