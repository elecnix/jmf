/*
 * "@(#)PlayerWindow.java	1.19 02/08/21
 *
 * Copyright (c) 1999 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */


package com.sun.media.ui;

import javax.media.*;
import javax.media.control.MonitorControl;
import java.awt.*;
import java.awt.event.*;
import com.sun.media.*;
import com.sun.media.controls.*;
import javax.media.format.*;

import com.sun.media.util.JMFI18N;


public class PlayerWindow extends Frame implements ControllerListener {

    private static final String    MENU_ZOOM_1_2 = JMFI18N.getResource("mediaplayer.zoom.1:2");
    private static final String    MENU_ZOOM_1_1 = JMFI18N.getResource("mediaplayer.zoom.1:1");
    private static final String    MENU_ZOOM_2_1 = JMFI18N.getResource("mediaplayer.zoom.2:1");
    private static final String    MENU_ZOOM_4_1 = JMFI18N.getResource("mediaplayer.zoom.4:1");
    private static final String    MENU_ZOOM = JMFI18N.getResource("mediaplayer.menu.zoom");

    Player player;
    Panel framePanel;
    ComponentListener cl;
    ComponentListener fcl;
    WindowListener wl;
    MouseListener ml;
    
    Component controlComp = null;
    Component visualComp = null;
    Insets insets;
    PopupMenu zoomMenu = null;
    boolean windowCreated = false;
    boolean newVideo = true;
    boolean panelResized = false;
    boolean autoStart = true;
    boolean autoLoop = true;
    Component progressBar = null;
    private Integer playerLock = new Integer(1);
    
    public PlayerWindow(Player player) {
	this(player, JMFI18N.getResource("mediaplayer.windowtitle"), true, true);
    }

    public PlayerWindow(Player player, String title) {
	this(player, title, true, true);
    }

    public PlayerWindow(Player player, String title, boolean autoStart) {
	this(player, title, autoStart, true);
    }

    public PlayerWindow(Player player, String title, boolean autoStart,
			boolean autoLoop) {
	super(title);
	this.autoStart = autoStart;
	this.autoLoop = autoLoop;
	this.player = player;
	setLayout( new BorderLayout() );

	framePanel = new Panel();
	framePanel.setLayout( null );
	add(framePanel, "Center");
	
	insets = getInsets();
	setSize(insets.left + insets.right + 320,
		insets.top + insets.bottom + 30);

	setVisible(true);
	
	// Anonymous class
	addWindowListener( wl = new WindowAdapter() {
	    public void windowClosing(WindowEvent we) {
		killThePlayer();
	    }
	});

	framePanel.addComponentListener( fcl = new ComponentAdapter() {
	    public void componentResized(ComponentEvent ce) {
		panelResized = true;
		doResize();
	    }
	});

	addComponentListener( fcl = new ComponentAdapter() {
	    public void componentResized(ComponentEvent ce) {
		insets = getInsets();
		Dimension dim = getSize();
		framePanel.setSize(dim.width - insets.left - insets.right,
				   dim.height - insets.top - insets.bottom);
	    }
	});

	/*
	addComponentListener( cl = new ComponentAdapter() {
	    public void componentResized(ComponentEvent ce) {
		framePanel.invalidate();
		doResize();
	    }
	});
	*/

	player.addControllerListener( this );
	player.realize();
	//player.start();
    }

    void sleep(long time) {
    	try {
            Thread.currentThread().sleep(time);
        } catch (Exception e) {
        }
    }

    public void addNotify() {
	super.addNotify();
	windowCreated = true;
	invalidate();
    }

    public void doResize() {
	Dimension d = framePanel.getSize();
	int videoHeight = d.height;
	if (controlComp != null) {
	    videoHeight -= controlComp.getPreferredSize().height;
	    if (videoHeight < 2)
		videoHeight = 2;
	    if (d.width < 80)
		d.width = 80;
	    controlComp.setBounds(0, videoHeight,
				  d.width, controlComp.getPreferredSize().height);
	    controlComp.invalidate();
	}

	if (visualComp != null) {
	    visualComp.setBounds(0, 0, d.width, videoHeight);
	}
	framePanel.validate();
    }

    public void killThePlayer() {
	synchronized (playerLock) {
	    /*
	      player.stop();
	      player.deallocate();
	    */
	    // in order to avoid deadlock problems, remove visual and
	    // control component if they are present, before closing the
	    // player
	    if (visualComp != null){
		framePanel.remove(visualComp);
		visualComp = null;
	    }
	    if (controlComp != null){
		framePanel.remove(controlComp);
		controlComp = null;
	    }
	    if (player != null)
		player.close();
	}
    }

    public void controllerUpdate(ControllerEvent ce) {
	synchronized (playerLock) {
	    if (ce instanceof RealizeCompleteEvent) {
		int width = 320;
		int height = 0;
		insets = getInsets();
		
		if (progressBar != null)
		    framePanel.remove(progressBar);
		
		if (( visualComp = player.getVisualComponent()) != null) {
		    width = visualComp.getPreferredSize().width;
		    height = visualComp.getPreferredSize().height;
		    framePanel.add(visualComp);
		    visualComp.setBounds(0, 0, width, height);
		    addPopupMenu(visualComp);
		} else {
		    MonitorControl mc = (MonitorControl)
			player.getControl("javax.media.control.MonitorControl");
		    if (mc != null) {
			Control [] controls = player.getControls();
			Panel mainPanel = new Panel( new BorderLayout() );
			Panel currentPanel = mainPanel;
			for (int i = 0; i < controls.length; i++) {
			    if (controls[i] instanceof MonitorControl) {
				mc = (MonitorControl) controls[i];
				mc.setEnabled(true);
				if (mc.getControlComponent() != null) {
				    currentPanel.add("North", mc.getControlComponent());
				    Panel newPanel = new Panel( new BorderLayout() );
				    currentPanel.add("South", newPanel);
				    currentPanel = newPanel;
				}
			    }
			}
			visualComp = mainPanel;
			width = visualComp.getPreferredSize().width;
			height = visualComp.getPreferredSize().height;
			framePanel.add(visualComp);
			visualComp.setBounds(0, 0, width, height);
		    }
		}
		
		if ((controlComp = player.getControlPanelComponent()) != null) {
		    int prefHeight = controlComp.getPreferredSize().height;
		    framePanel.add(controlComp);
		    controlComp.setBounds(0, height, width, prefHeight);
		    height += prefHeight;
		}
		
		setSize(width + insets.left + insets.right,
			height + insets.top + insets.bottom);
		if (autoStart)
		    player.prefetch();
		
	    } else if (ce instanceof PrefetchCompleteEvent) {
		if (visualComp != null) {
		    Dimension vSize = visualComp.getPreferredSize();
		    if (controlComp != null)
			vSize.height +=
			    controlComp.getPreferredSize().height;
		    panelResized = false;
		    setSize(vSize.width + insets.left + insets.right,
			    vSize.height + insets.top + insets.bottom);
		    int waited = 0;
		    while (panelResized == false && waited < 2000) {
			try {
			    waited += 50;
			    Thread.currentThread().sleep(50);
			    Thread.currentThread().yield();
			} catch (Exception e) {}
		    }
		} else {
		    int height = 1;
		    if (controlComp != null)
			height = controlComp.getPreferredSize().height;
		    setSize(320+insets.left + insets.right,
			    height + insets.top + insets.bottom);
		}
		
		if (autoStart) {
		    if (player != null &&
			player.getTargetState() != Controller.Started) {
			player.start();
		    }
		}
		
	    } else if (ce instanceof EndOfMediaEvent) {
		if (autoLoop) {
		    player.setMediaTime(new Time(0));
		    player.start();
		}
		/*
		  if (player.getMediaNanoseconds() == 0)
		  player.start();
		  else
		  System.err.println("Failed to loop back: the player is not seekable.");
		*/
	    } else if (ce instanceof ControllerErrorEvent) {
		System.err.println("Received controller error");
		killThePlayer();
		dispose();
	    } else if (ce instanceof SizeChangeEvent) {
		// The video size has changed, resize the panel
		if (framePanel != null) {
		    SizeChangeEvent sce = (SizeChangeEvent) ce;
		    int nooWidth = sce.getWidth();
		    int nooHeight = sce.getHeight();
		    // Add the height of the default control component
		    if (controlComp != null)
			nooHeight += controlComp.getPreferredSize().height;
		    if (  framePanel.getSize().width != nooWidth ||
			  framePanel.getSize().height != nooHeight) {
			setSize(nooWidth + insets.left + insets.right,
				nooHeight + insets.top + insets.bottom);
			//validate();
		    } else
			doResize();
		    
		    if (controlComp != null)
			controlComp.invalidate();
		}
	    } else if (ce instanceof FormatChangeEvent) {
		Dimension vSize = new Dimension(320,0);
		Component oldVisualComp = visualComp;
		if ((visualComp = player.getVisualComponent()) != null) {
		    if (oldVisualComp != visualComp){
			if (oldVisualComp != null && zoomMenu != null)
			    oldVisualComp.remove(zoomMenu);
			framePanel.remove(oldVisualComp);
			vSize = visualComp.getPreferredSize();
			framePanel.add(visualComp);
			visualComp.setBounds(0, 0, vSize.width, vSize.height);
			addPopupMenu(visualComp);
		    }
		}
		Component oldComp = controlComp;
		if ((controlComp = player.getControlPanelComponent()) != null) {
		    if (oldComp != controlComp){
			framePanel.remove(oldComp);
			framePanel.add(controlComp);
			if (controlComp != null){
			    int prefHeight = controlComp.getPreferredSize().height;
			    controlComp.setBounds(0, vSize.height,vSize.width, prefHeight);
			}
		    }
		}
	    } else if (ce instanceof ControllerClosedEvent) {
		if (visualComp != null) {
		    if (zoomMenu != null)
			visualComp.remove(zoomMenu);
		    visualComp.removeMouseListener(ml);
		}
		removeWindowListener(wl);
		removeComponentListener(cl);
		/*
		  player.removeControllerListener(this);
		  player.close();
		*/
		if (framePanel != null)
		    framePanel.removeAll();
		player = null;
		visualComp = null;
		controlComp = null;
		sleep(200);
		dispose();
	    } else if (ce instanceof CachingControlEvent) {
		CachingControl cc = ((CachingControlEvent)ce).getCachingControl();
		if (cc != null && progressBar == null) {
		    progressBar = cc.getControlComponent();
		    if (progressBar == null)
			progressBar = cc.getProgressBarComponent();
		    if (progressBar != null) {
			framePanel.add(progressBar);
			Dimension prefSize = progressBar.getPreferredSize();
			progressBar.setBounds(0, 0, prefSize.width, prefSize.height);
			insets = getInsets();
			framePanel.setSize(prefSize.width, prefSize.height);
			setSize(insets.left + insets.right + prefSize.width,
				insets.top + insets.bottom + prefSize.height);
		    }
		}
	    }
	}
    }

    public void zoomTo(float z) {
	if (visualComp != null) {
	    insets = getInsets();
	    Dimension d = visualComp.getPreferredSize();
	    d.width = (int) (d.width * z);
	    d.height = (int) (d.height * z);
	    if (controlComp != null)
		d.height += controlComp.getPreferredSize().height;
	    
	    setSize(d.width + insets.left + insets.right,
		    d.height + insets.top + insets.bottom);
	}
    }

    private void addPopupMenu(Component visual) {
	
	MenuItem mi;
	ActionListener zoomSelect;
	
	zoomMenu = new PopupMenu(MENU_ZOOM);

	zoomSelect = new ActionListener() {
	    public void actionPerformed(ActionEvent ae) {
		String action = ae.getActionCommand();
		if ( action.equals(MENU_ZOOM_1_2) )
		    zoomTo(0.5f);
		else if ( action.equals(MENU_ZOOM_1_1) )
		    zoomTo(1.0f);
		else if ( action.equals(MENU_ZOOM_2_1) )
		    zoomTo(2.0f);
		else if ( action.equals(MENU_ZOOM_4_1) )
		    zoomTo(4.0f);
	    }
	};
	visual.add(zoomMenu);
	mi = new MenuItem(MENU_ZOOM_1_2);
	zoomMenu.add(mi);
	mi.addActionListener(zoomSelect);
	mi = new MenuItem(MENU_ZOOM_1_1);
	zoomMenu.add(mi);
	mi.addActionListener(zoomSelect);
	mi = new MenuItem(MENU_ZOOM_2_1);
	zoomMenu.add(mi);
	mi.addActionListener(zoomSelect);
	mi = new MenuItem(MENU_ZOOM_4_1);
	zoomMenu.add(mi);
	mi.addActionListener(zoomSelect);

	visual.addMouseListener( ml = new MouseAdapter() {
	    public void mousePressed(MouseEvent me) {
		if (me.isPopupTrigger())
		    zoomMenu.show(visualComp, me.getX(), me.getY());
	    }

	    public void mouseReleased(MouseEvent me) {
		if (me.isPopupTrigger())
		    zoomMenu.show(visualComp, me.getX(), me.getY());
	    }

	    public void mouseClicked(MouseEvent me) {
		if (me.isPopupTrigger())
		    zoomMenu.show(visualComp, me.getX(), me.getY());
	    }
	} );
    }
}
    
    
