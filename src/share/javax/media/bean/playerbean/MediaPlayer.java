/*
 * @(#)MediaPlayer.java	1.22 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

//  @version 1.0
//  Last Updated: 02/19/99
//  Notes: N/A


package javax.media.bean.playerbean;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.lang.String;
import java.lang.reflect.*;
import java.net.*;
import java.io.*;
import java.util.Properties;
import java.util.Vector;
import javax.media.*;
import javax.media.protocol.*;
import java.beans.*;
import javax.media.rtp.*;
import javax.media.format.*;
import javax.media.rtp.event.*;
import java.applet.AppletContext;


/**
  *   <CODE>MediaPlayer</CODE> encapsulates a player in a 
  *   JavaBeans<sup><font size="-2">TM</sup></font> component. 
  *   <CODE>MediaPlayer</CODE> implements the <CODE>Player</CODE> and <CODE>Externalizable</CODE> 
  *   interfaces. 
  *  <p>
  *   A <CODE>MediaPlayer</CODE> can be used as a 
  *   full-featured <CODE>Player</CODE> with either the default <CODE>Player</CODE> controls or
  *   custom controls. 
  *
  *  <h3><CODE>MediaPlayer</CODE> Properties</h3>
  *   <p>
  *   The following bean properties are supported by <CODE>MediaPlayer</CODE>:
  *   <ul>
  *   <li>loop: Indicates whether or not the media will be played repeatedly. 
  *   
  *   <li>media location: The location of the media to be played. The location can be either an
  *       absolute address such as <CODE>http://xena/demo/media/Sample1.mov</CODE>
  *       or an address relative to the codebase address such as <CODE>media/Sample1.mov</CODE>
  *   <li>name: The name of this <CODE>MediaPlayer</CODE>.
  *   <li>size and position: The size and position of this 
  *       <CODE>MediaPlayer</CODE>.
  *   <li>volume: The audio volume as a number from 0 to 5, where  0 is muted 
  *   and 5 is the loudest possible volume. The default value is 3.
  *       
  *   <li>show control panel: Indicates whether or not the video control panel is 
  *       displayed.
  *   <li>show caching control: Indicates whether or not the caching control is displayed.
  *       The caching control shows the download progress. 
  *         
  *   <li>fixed aspect ratio: Indicates whether or not the aspect ratio of the video  
  *       is maintained when the visual component is resized.
  *   </ul>
  * <p>
  * Two additional properties can be set. These are not bean properties, but provide
  * additional control over the presentation: 
  * <ul>
  * <li>ZoomLevel: Can be set through the popup menu and via set and get methods.  
  *   (Click the right mouse button over the media to bring up the popup menu.)
  *   
  * <li>MediaLocationVisible: Can be set through get and set methods. The media location 
  * is not editable during runtime.
  * 
  * <h3>Invoking <CODE>MediaPlayer</CODE></h3>
  * <p>  
  * You can instantiate MediaPlayer directly or using a bean-enabled IDE tool such as NetObjects
  * BeanBuilder. To instantiate <CODE>MediaPlayer</CODE> directly:
  * 
  * <pre>
  *	      ClassLoader cl = this.getClass().getClassLoader();
  *	      MediaPlayer pb = (MediaPlayer)Beans.instantiate(cl,
  *		   			     "javax.media.bean.playerbean.MediaPlayer");
  * </pre>   
  *  
  */

public class MediaPlayer extends Container
implements Player,Externalizable {

    private PropertyChangeSupport changes = new PropertyChangeSupport(this);

    // the media location the user specified.  It can be absolute or relative
    // address.
    private String urlString = "";
    private MediaLocator mrl;
    private URL  url; 
    private AppletContext mpAppletContext = null;  //applet context of container
  
    //Should the Player's control panel be visible?
    private boolean panelVisible = true;

    //Should the Player's caching control panel be visible?
    private boolean cachingVisible = false;
  
    //Should maintain aspect ratio?
    private boolean fixedAspectRatio = true;
  
    //Should the user be able to scale the video?
    private boolean fitVideo = true;
  
    //Should the video loop after it has reached the end?
    private boolean looping = true;

    //Private variables:

    //The actual JMF Player
    transient Player player = null;

    //The panel which contains the player components: [visual component,
    // control panel]
    transient Panel panel;

    // the panel which contains the visual component of a player.
    transient Panel vPanel;
  
    //the panel for control component and url
    transient Panel newPanel;

    //And the visual component itself
    transient Component visualComponent;

    //and the controlComponent
    transient Component controlComponent;
  
    //and the cachingControl progress bar component
    transient Component cachingComponent;

    //How high is the controlPanel
    private transient int controlPanelHeight = 0;
  
    //How high is the urlField
    private transient int urlFieldHeight = 0;
  
    //The preferredHeight
    private int preferredHeight;

    //And PreferredWidth
    private int preferredWidth;

    //And, of course, the current state of the player
    private int state;

    // a set of MediaPlayers, which are placed under this bean's control
    private Vector controlListeners = new Vector();

    //popup menu for controlling the size
    PopupMenu zoomMenu = null;
  
    //code base of Applet
    private URL mpCodeBase = null;

    protected transient GainControl gainControl;

    // external  for the current volume level
    protected transient String curVolumeLevel = 
    MediaPlayerResource.getString("THREE");
    // internal presentation for the current volume level
    protected transient float curVolumeValue = 0.6f;

    // external presentation for the current zoom level
    protected transient String curZoomLevel = 
    MediaPlayerResource.getString("SCALE_NORMAL");
    // internal presentation for the current zoom level
    protected transient float curZoomValue = 1.0f;

    transient protected Time  mediaTime;

    private InternalControllerAdapter selfListener;
  
    //media length for caching control  
    private long contentLength=0;
  
    private boolean fixtedFirstTime=true;
  
    private boolean displayURL = false;
  
    //if popup menu is active 
    private boolean isPopupActive = true;
  
    //The widget displaying the media location for the player
    private transient TextField urlName;
  
    private transient visualMouseAdapter mouseListen;
  
    /**
     *
     * InternalControllerAdapter:
     *   Inner class to catch player event
     *
     */
    class InternalControllerAdapter  extends ControllerAdapter {
	private MediaPlayer thisBean;
	/**
	 *
	 * Constructs an <CODE>InternalControllerAdapter</CODE>.
	 * @param b  Instance of <CODE>MediaPlayer</CODE> bean  
	 *
	 */
	public  InternalControllerAdapter(MediaPlayer b) {
	    super();
	    thisBean=b;
	}
    
	/**
	 *
	 * Replaces the URL that is invoked when a hot 
	 *                 link to a new URL is triggered.
	 *
	 * @param ReplaceURLEvent   A <CODE>ControllerEvent</CODE> that indicates that the  
	 *                          mediaLocation has been changed. 
	 *
	 */
    
	public void replaceURL(Object e) { 
	    debug("ReplaceURLEvent ");
	    URL  retobj=null;
	    try {
		Class cls = Class.forName("com.ibm.media.ReplaceURLEvent");  
		Method meth = cls.getMethod("getURL",null);
		retobj = (URL)meth.invoke(e, null);
	    } catch (Throwable t) {
		System.err.println(t);
	    }
	    setMediaLocation(retobj.toExternalForm());
	    thisBean.start(); 
	}
	 
	/**
	 *
	 * Displays the html document which is invoked when a 
	 *                   hotlink to a html is triggered in a mvr file
	 *
	 * @param   ShowDocumentEvent a <CODE>ControllerEvent</CODE> that indicates that   
	 *           URL to show in a web page.               
	 *
	 */
    
	public void showDocument(Object e) { 
	    debug("ShowDocumentEvent ");
	    initSetCodeBase();
	    URL  retURL=null;
	    String retContext=null;
	    try {
		Class cls = Class.forName("com.ibm.media.ShowDocumentEvent");  
		Method meth = cls.getMethod("getURL",null);
		retURL = (URL)meth.invoke(e, null);
		meth = cls.getMethod("getString",null);
		retContext = (String)meth.invoke(e, null);
	    } catch (Throwable t) {
		System.err.println(t);
	    }
	    if (mpAppletContext !=null) {
		mpAppletContext.showDocument(retURL,retContext);
	    }
	}
    
	/**
	 *
	 * Gets the current state.
	 *
	 * @param TransitionEvent   A <CODE>ControllerEvent</CODE> that indicates that a 
	 *                          <CODE>Controller</CODE> has changed state. 
	 *
	 */
	public void transition(TransitionEvent e) { 
	    debug("TransitionEvent "+e);
	    state = e.getCurrentState();
	}
	  
	/**
	 *
	 * When the player is Realized, it calls doRealize
	 *    internal function to get the visual, gain, control components and
	 *    add them to the panel of bean. If fixedAspectRatio is set, it will 
	 *    check if Bean has more space to expand the visual component and calculate
	 *    the  
	 *
	 * @param RealizeCompleteEvent   A <CODE>ControllerEvent</CODE> that indicates a 
	 *                               <CODE>Controller</CODE> has finished realizing. 
	 */
	   
	public void realizeComplete(RealizeCompleteEvent e) {
	    debug("  Event:  RealizeComplete");
	    doRealize();	  
	}
	   
	/**
	 *
	 * When the player is Prefetched, removes the progress bar,
	 *          sets the size, and sets the visual component visible
	 *   
	 * @param prefetchCompleteEvent  A <CODE>ControllerEvent</CODE> that indicates a
	 *                               <CODE>Controller</CODE> has finished Prefetching. 
	 */ 
	public void prefetchComplete(PrefetchCompleteEvent e) {
	    debug("  Event:prefetchComplete");
	    //doPrefetch();
	   
	}
	/**
	 *
	 * When player format changes, remove the old visual component  
	 *          then create the new one.
	 *   
	 * @param FormatChangeEvent  A <CODE>ControllerEvent</CODE> that indicates a
	 *                               <CODE>Controller</CODE> changes format. 
	 */  
	public void formatChange(FormatChangeEvent e) {
	    debug(" formatChangeevent");
	    Component oldVisualComp = visualComponent;
	    if (player==null) {
		Fatal(MediaPlayerResource.getString("SHOULD NOT OCCUR"));
		System.exit(-1);
	    }
	    if (( visualComponent = player.getVisualComponent())!= null) {      
		if (oldVisualComp !=visualComponent) {
		    if (oldVisualComp !=null) {
			oldVisualComp.remove(zoomMenu);
			vPanel.remove(oldVisualComp);
			panel.remove(vPanel);
			Component oldComp = controlComponent;
			if ((oldComp != null) && (newPanel != null)) {
			    newPanel.remove(oldComp);
			    if (displayURL == true)
				newPanel.remove(urlName);
			    panel.remove(newPanel);
			}
		    }
		    doRealize();
		} else if ((getMediaLocation().endsWith("mvr"))||
			   (getMediaLocation().endsWith("MVR"))) {
		    //     calculateSize();
		    zoomTo((float)1.0);          //mvr doesn't zoom so not calculation needed
		    //     zoomTo(curZoomValue);
		}
	    } else {
		Component oldComp = controlComponent;
		if ((( controlComponent = player.getControlPanelComponent()) != null) && 
		    (isControlPanelVisible())) {
		    if (  oldComp != controlComponent) {
			if (oldComp!=null) {
			    newPanel.remove(oldComp);
			    if (displayURL==true)
				newPanel.remove(urlName);
			    panel.remove(newPanel);
			}
			doRealize();
		    }
		} else {    //new control comp is null   
		    if ((oldVisualComp !=null)&&(vPanel!=null)) {
			oldVisualComp.remove(zoomMenu);
			vPanel.remove(oldVisualComp);
			panel.remove(vPanel);
		    }
		}
	    }
      
	}
  	   
	/**
	 *
	 * When the player is started.
	 *
	 * @param    StartEvent   A <CODE>ControllerEvent</CODE> that indicates a <CODE>Controller</CODE>
	 *                               has entered into the <EM>Started</EM> state
	 */ 
	public void start(StartEvent e) {
	    debug("  Event:StartEvent ");   
	}
      
	/**
	 *
	 * When the player reach the end of media, if loop is set, 
	 *    restart it.
	 * @param     EndOfMediaEvent  A <CODE>ControllerEvent</CODE> that indicates a <CODE>Controller</CODE>
	 * has reached the end of its media and is stopping. 
	 *
	 */ 
	public void endOfMedia(EndOfMediaEvent e) {
	    // We've reached the end of the media; rewind and
	    // start over
	    debug("  Event:EndofMediaEvent");
	    if (isPlayBackLoop()) {
		synchronized (this) {
		    if (player==null) {
			Fatal(MediaPlayerResource.getString("SHOULD NOT OCCUR"));
			System.exit(-1);
		    }    
		    if ( player != null )
			player.setMediaTime(new Time(0));
		    if ( player != null )
			player.start();
		}
	    }
	}
      
	/**
	 *
	 * When the player has an error condition occur, log the 
	 *    error message.
	 * @param     controllerErrorEvent  A <CODE>ControllerEvent</CODE> that indicates a 
	 * <CODE>Controller</CODE> error condition occurs that will cause 
	 * a <CODE>Controller</CODE> to cease functioning.
	 *
	 */
	public void controllerError(ControllerErrorEvent e) {
	    // Tell TypicalPlayerApplet.start() to call it a day
	    debug("  Event:ControllerErrorEvent");
	    player = null;
	    Fatal(e.getMessage());
	}
      
	/**
	 *
	 * When the input video has changed in size, recalculate, 
	 *               and redraw.
	 * @param     SizeChangeEvent  A <CODE>ControllerEvent</CODE> that indicates the
	 *            input video has changed in size and the video renderer needs
	 *            to be resized to specified size
	 *
	 */  
	public void sizeChange(SizeChangeEvent e) {
	    debug("  Event:SizeChangeEvent");
	    int newPanelHeight =0;
	    if ((controlComponent != null) && isControlPanelVisible())
		newPanelHeight= controlPanelHeight;
	
	    if ((urlName!=null) &&(isMediaLocationVisible()))
		newPanelHeight += urlFieldHeight;
	    if (visualComponent != null){
		visualComponent.setSize(e.getWidth(), e.getHeight());
		preferredHeight = visualComponent.getPreferredSize().height; 
		preferredWidth = visualComponent.getPreferredSize().width; 
	    }
	    if (panel != null) {
		if (vPanel != null) {
		    vPanel.setBounds(0, 0, e.getWidth(),
				     e.getHeight()-newPanelHeight );
		    vPanel.validate();
		}  
		panel.setBounds(0, 0, e.getWidth(), e.getHeight());
		panel.validate();
	    }
	    invalidate();
	    validate();
	}
    
	/**
	 *
	 * When the caching event generated, if no progress
	 *               bar, then create it, if download completed, remove 
	 *               the progress bar.
	 * @param     cachingControlEvent  A <CODE>ControllerEvent</CODE> that indicates the
	 *            caching state change.
	 *
	 */    
	public void cachingControl(CachingControlEvent e) {
	    debug("  Event:  CachingControl");
	    if (isCachingControlVisible() == true) {
		CachingControl cc = e.getCachingControl();
		long contentLength=cc.getContentLength();
		if ((cc != null && cachingComponent == null)&& 
		    (contentLength!=CachingControl.LENGTH_UNKNOWN))  {
		    cachingComponent = cc.getControlComponent();
		    if (cachingComponent == null)
			cachingComponent = cc.getProgressBarComponent();
		
		    if (cachingComponent != null) {
			panel.add("South",cachingComponent);
			Dimension prefSize = cachingComponent.getPreferredSize();
			cachingComponent.setBounds(0, 0, prefSize.width, prefSize.height);
			panel.setSize(prefSize.width, prefSize.height);
			panel.validate();
		    }
		} else {
		    if (cc.getContentProgress()==contentLength){
			if (cachingComponent!=null) {
			    panel.remove(cachingComponent);
			    validate();
			}
		    }
		}
	    }
	}
      
    }
 
    /**
     *   Constructs a MediaPlayer with its main panel preparing for 
     *   caching control events and the following visual display.
     *
     */
    public MediaPlayer() {
      
	super();
	setLayout(null);
	if (panel == null) {
	    panel = new Panel();
	    panel.setLayout(new BorderLayout());
	    panel.setVisible(false);
	    add("Center",panel);
	}
	   
    }

    /**
     *
     * doRealize(): internal function to get the visual, gain, control components and
     *    add them to the panel of bean. If fixedAspectRatio is set, it will 
     *    check if Bean has more space to expand the visual component and calculate
     *    the zoom factor.  
     *
     */
    private synchronized void doRealize() {
	debug("in doRealize");
	if (player==null) {
	    Fatal(MediaPlayerResource.getString("SHOULD NOT OCCUR"));
	    System.exit(-1);
	}
	gainControl = player.getGainControl();
	    
	if (gainControl !=null) {
	    float re=gainControl.setLevel(curVolumeValue);  
	}
	      
	if (( visualComponent = player.getVisualComponent())!= null) {
	    vPanel = new Panel();
	    vPanel.setLayout( new BorderLayout() );
	    panel.setVisible(false);
	    visualComponent.setVisible(false);
	    vPanel.add("Center", visualComponent);
	    panel.add("Center", vPanel);
	  
	    addComponentListener(new visualComponentAdapter(this));
	    addPopupMenu(visualComponent);
	    setPopupActive();
	}
      
	newPanel=new Panel();
	newPanel.setLayout(new BorderLayout());
	panel.add("South",newPanel);
	urlName = new TextField(10);
	urlName.setEditable(false);
      
	if (displayURL==true) {
	    urlName.setText(urlString);
	    newPanel.add("South",urlName);
	}
	        
	if ((( controlComponent = player.getControlPanelComponent()) != null)&& 
	    (isControlPanelVisible())) {

	    controlComponent.setVisible(false);
	    newPanel.add("North",controlComponent);
	}
		    
	calculateSize();
      
	if (visualComponent == null) {
	    if (controlComponent !=null)
		panel.setSize(controlComponent.getPreferredSize().width,
			      controlPanelHeight+urlFieldHeight );     
	    else
		panel.setSize(100,urlFieldHeight);
	  
	}	
	showVisual();
    }
    
    private  void calculateSize() {
        if ( player == null )
            return;
	debug("claculateSize");
	if (( visualComponent = player.getVisualComponent())!= null) {
	    //   visualComponent.setSize(visualComponent.getPreferredSize());    
	    preferredHeight =visualComponent.getPreferredSize().height; 
	    preferredWidth =visualComponent.getPreferredSize().width; 
	    
	}
	
	if (((( controlComponent = player.getControlPanelComponent()) != null)&& 
	     (isControlPanelVisible()))||(displayURL==true)) {
	    if ((controlComponent!=null)&& (isControlPanelVisible())) {
		controlPanelHeight = controlComponent.getPreferredSize().height;
		preferredHeight +=controlPanelHeight;
		if (preferredWidth ==0)
		    preferredWidth = 320;
		
	    }
	    if (displayURL==true) {
		urlFieldHeight = urlName.getPreferredSize().height;
		preferredHeight +=urlFieldHeight;
	    }
	    
	}
	
	if (visualComponent != null) {
	    Dimension vSize = visualComponent.getPreferredSize();
	    int totalHeight=0;
	    if (controlComponent != null) {
		totalHeight+=controlComponent.getPreferredSize().height;
	    }
	    if (displayURL==true) {
		totalHeight +=urlName.getPreferredSize().height;
	    }
	    
	    if ((  fixedAspectRatio==true) && (curZoomValue == 1.0)) {
		
		//compare the ratio of parent and panel, and get the smaller number
		
		if ((vSize.width!=0) &&(vSize.height!=0)) {
		    if (((float)getSize().width/(float)vSize.width) >=
			((float)(getSize().height-totalHeight)/
			 (float)(vSize.height))) {
			
			curZoomValue =  
			    ((float)(getSize().height-totalHeight)/
			     (float)(vSize.height));
		    } else {    
			curZoomValue = 
			    ((float)getSize().width/vSize.width);
		    }
		}
		if (curZoomValue <0.5)
		    curZoomValue=1;
		
	    }	
	    if (newPanel !=null)
                newPanel.setSize(visualComponent.getPreferredSize().width,
				 controlPanelHeight+urlFieldHeight);
	    
	} 	 
	
    }

    
    private synchronized void showVisual() {
	//debug(" doPrefetch");
	if (cachingComponent!=null) {
	    panel.remove(cachingComponent);
	    validate();
	}
	
	if ((visualComponent!=null) && 
	    ((fixedAspectRatio==true) || (fixtedFirstTime==false))) {
	    zoomTo(curZoomValue);
	} else {
	    panel.setSize(getSize());
	}
	panel.setVisible(true);
	if (visualComponent != null) {
	    visualComponent.setVisible(true);  
	}
	if ((controlComponent != null)&& (isControlPanelVisible())) {
	    controlComponent.setVisible(true); 
	}
	
	panel.validate();
	validate(); 
    }
    
    /**
     * initSetCodeBase():
     *   set codebase for relative media location.
     *
     */
    private void initSetCodeBase() {
	if (mpCodeBase!=null)
	    return;
	
	Container p = getParent();
	while (p!=null) {
	    if (p instanceof Applet) 
		break;
	    p = p.getParent();
	}
	if (p!=null) {
	    setCodeBase(((Applet)p).getCodeBase());
	    mpAppletContext = ((java.applet.Applet) p).getAppletContext();
	}
	
    }
    
    
    // MediaPlayer property methods:
    
    /**
     * Gets the value of the media location property for this <CODE>MediaPlayer</CODE>.
     *
     * @return  A <CODE>String</CODE> that contains the media location.
     *
     */
    public String getMediaLocation() {
	if (mrl !=null) {
	    return mrl.toString();
	} else {
	    return urlString;
	}
    }

    /**
     *
     * Gets a <CODE>MediaLocator</CODE> for this <CODE>MediaPlayer</CODE>
     * that corresponds to the specified <CODE>String</CODE>.
     *
     * @param filename A <CODE>String</CODE> that contains the media name 
     * from which to create the <CODE>MediaLocator</CODE>.
     * @return A <CODE>MediaLocator</CODE> that corresponds to the specified <CODE>String</CODE>.
     *
     */
    protected MediaLocator getMediaLocator(String filename) {
	MediaLocator localml = null;
	
	if (filename.regionMatches(true, 0, "<codebase>", 0, 10)) {
	    try {
		if (mpCodeBase == null)
		    initSetCodeBase();
		localml = new MediaLocator( new URL(mpCodeBase, filename.substring(11)));
	    } catch (java.net.MalformedURLException e) {
		if (mpCodeBase != null ) {
		    log(MediaPlayerResource.getString("NO IMAGE:BAD_URL")+
			filename+" " + mpCodeBase);
		    urlString = " ";
		}
		return null;
	    }  
	} else {
	    localml = new MediaLocator(filename);
	}
   
	return localml;
    }
 
    /**
     *   Sets the media location property for this <CODE>MediaPlayer</CODE>.
     *   This property specifies the location of the media file to be 
     *   presented by the <CODE>MediaPlayer</CODE>.
     *   If a <CODE>Player</CODE> already exists for this <CODE>MediaPlayer</CODE>, 
     *   this method stops that <CODE>Player</CODE> and releases all
     *   of the resources it is using.  Then a new <CODE>Player</CODE> is created to
     *   present the specified media file.
     *   All controller listeners  registered  for the old
     *   <CODE>Player</CODE> are added to the listener list of the new <CODE>Player</CODE>.
     *
     * @param location A <CODE>String</CODE> that contains the location of the media file.
     *
     */
    public void setMediaLocation(String location) {
   
	try {
	    String old = "";
	    if (mrl !=null) {
		old = mrl.toExternalForm();
		if (panel != null)
		    panel.removeAll();
		if (player != null) {
		    player.stop();
		    player.close();
		    panel.validate();
		    if (controlListeners.contains(selfListener))
		        controlListeners.removeElement(selfListener);
		}
	    }
	    urlString = location;
	    if ((mrl = getMediaLocator(location))==null) {
		//urlString = "";
		return;
	    }
	    try {
		player = javax.media.Manager.createPlayer(mrl);
	    } catch (Exception e) {
		player = null;
		urlString = " ";
		changes.firePropertyChange("mediaLocation",  location, old);
		Fatal(MediaPlayerResource.getString("UNABLE_CREATE_PLAYER")+e);
		return;
	    }
	   	    
	    if (player==null) {
		return;
	    }
      
	    if (urlName !=null) {
		urlName.setText(urlString);
		urlName.setFont(getFont());
	    }
	    player.addControllerListener(selfListener=new InternalControllerAdapter(this));
      
	    if (!controlListeners.isEmpty()) {   
		for (int i=0;i<controlListeners.size();i++)
		    player.addControllerListener(
						 (ControllerListener)controlListeners.elementAt(i));
	    }
	    changes.firePropertyChange("mediaLocation", old, location);
      
	} catch (Exception e) {
	    mrl = null;
	    e.printStackTrace();
	    Fatal(MediaPlayerResource.getString("UNABLE_CREATE_PLAYER")+e);
	    return;
	}
    }

    /**
     *   Sets the <CODE>MediaLocator</CODE> for this <CODE>MediaPlayer</CODE>. This  
     *   method creates a <CODE>Player</CODE> for the specified <CODE>MediaLocator</CODE>. 
     *   If a <CODE>Player</CODE> already exists for this <CODE>MediaPlayer</CODE>, 
     *   this method stops that <CODE>Player</CODE> and releases all
     *   of the resources it is using.  Then a new <CODE>Player</CODE> is created with
     *   the specified <CODE>MediaLocator</CODE>.
     *   All controller listeners  registered  for the old
     *   <CODE>Player</CODE> are added to the listener list of the new <CODE>Player</CODE>.
     *
     * @param locator A <CODE>MediaLocator</CODE> that identifies the media file to be 
     * presented by the <CODE>MediaPlayer</CODE>.
     *
     */
    public void setMediaLocator(MediaLocator locator) {
	try {
	    debug("setMediaLocator");
	    if (locator != null) {
		if (mrl !=null) {
		    if (panel != null)
		        panel.removeAll();
		    if (player != null) {
		        player.stop();
		        player.close();
		        if (controlListeners.contains(selfListener))
			    controlListeners.removeElement(selfListener);
		    }
		}
	    } else
		return;
	      
	    try {
		player = javax.media.Manager.createPlayer(locator);
	    } catch (NoPlayerException e) {
		player = null;
		urlString = " ";
		Fatal(MediaPlayerResource.getString("UNABLE_CREATE_PLAYER")+e);
		return;
	    } catch (java.io.IOException e) {
		player = null;
		urlString = " ";
		Fatal(MediaPlayerResource.getString("IO_EXCEPTION")+e);
		return;
	    }
	    
	    if (player==null) {
	        return;
	    }
	    urlString = locator.toExternalForm();
	    mrl = locator;
	    if (urlName !=null) {
		urlName.setText(urlString);
		urlName.setFont(getFont());
	    }
	    player.addControllerListener(selfListener=new InternalControllerAdapter(this));
	    
	    if (!controlListeners.isEmpty()) {
		for (int i=0;i<controlListeners.size();i++)
		    player.addControllerListener(
						 (ControllerListener)controlListeners.elementAt(i));
	    }
	   
	} catch (Exception e) {
	    url = null;
	    e.printStackTrace();
	    Fatal(MediaPlayerResource.getString("UNABLE_CREATE_PLAYER")+e);
	    return;
	}
    }

    /**
     *   Sets the <CODE>DataSource</CODE> for this <CODE>MediaPlayer</CODE>. This  
     *   method creates a <CODE>Player</CODE> for the specified <CODE>DataSource</CODE>. 
     *   If a <CODE>Player</CODE> already exists for this <CODE>MediaPlayer</CODE>, 
     *   this method stops that <CODE>Player</CODE> and releases all
     *   of the resources it is using.  Then a new <CODE>Player</CODE> is created with
     *   the specified <CODE>DataSource</CODE>.
     *   All controller listeners  registered  for the old
     *   <CODE>Player</CODE> are added to the listener list of the new <CODE>Player</CODE>. 
     *
     * @param ds A <CODE>DataSource</CODE> that identifies the media file to be 
     * presented by the <CODE>MediaPlayer</CODE>.
     *
     */
    public void setDataSource(DataSource ds) {
	try {
	    debug("setDataSource");
	    if (ds != null) {
		if (panel != null)
		    panel.removeAll();
		if (player != null) {
		    player.stop();
		    if (controlListeners.contains(selfListener))
		        controlListeners.removeElement(selfListener);
		}
	    }
	    if (urlName !=null) {
		if (ds.getLocator()!=null)
		    urlName.setText(ds.getLocator().toExternalForm());
		else
		    urlName.setText(""); 
		urlName.setFont(getFont());
	    }
	    try {
		player = javax.media.Manager.createPlayer(ds);
	    } catch (Exception e) {
		player = null;
		urlString = " ";
		Fatal(MediaPlayerResource.getString("UNABLE_CREATE_PLAYER")+e);
		return;
	    }
	    if (player==null) {
	        return;
	    }
	   
	    if (ds.getLocator()!=null) 
	        urlString = ds.getLocator().toExternalForm();
	    else
	        urlString = ""; 
	    player.addControllerListener(selfListener=new InternalControllerAdapter(this));
	    
	    if (!controlListeners.isEmpty()) {   
		for (int i=0;i<controlListeners.size();i++)
		    player.addControllerListener(
						 (ControllerListener)controlListeners.elementAt(i));
	    }
	   
	} catch (Exception e) {
	    mrl = null;
	    e.printStackTrace();
	    Fatal(MediaPlayerResource.getString("UNABLE_CREATE_PLAYER")+e);
	    return;
	}
    }

    /**
     *   Specifies an existing <CODE>Player</CODE> to use to present media 
     *   for this <CODE>MediaPlayer</CODE>.
     *   If a <CODE>Player</CODE> already exists for this <CODE>MediaPlayer</CODE>, 
     *   this method stops that <CODE>Player</CODE> and releases all
     *   of the resources it is using.  
     *   All controller listeners  registered  for the old
     *   <CODE>Player</CODE> are added to the listener list of the new <CODE>Player</CODE>. 
     *
     * @param newPlayer The <CODE>Player</CODE> to use to present media data for 
     * this <CODE>MediaPlayer</CODE>.
     *
     */
    public void setPlayer(Player newPlayer) {
	debug("setPlayer");
	    
	if (newPlayer == null)
	    return;
   
	if (newPlayer != null) {
	    if (panel != null)
		panel.removeAll();
	    if (player != null) {
		player.stop();
		if (controlListeners.contains(selfListener))
		    controlListeners.removeElement(selfListener);
	    }
	}
	player=newPlayer;
	urlString = "";
	player.addControllerListener(selfListener=new InternalControllerAdapter(this));
	switch(player.getState()) {
	case (Prefetched):
	    debug("player state prefetched ");  
	    break;
	case (Prefetching):
	    debug("player state prefetching ");  
	    break;
	case (Realized):
	    debug("player state Realized ");  
	    break;
	case (Realizing):
	    debug("player state  Realizing");  
	    break;
	case (Started):
	    debug("player state started ");  
	    break;
	case (Unrealized):
	    debug("player state Unrealized ");      
	    break;
	}
	if ((player.getState()==Realized) ||
	    (player.getState()==Prefetching)) 
	    doRealize();
	 
	if  ((player.getState()==Prefetched)  ||
	     (player.getState()==Started)) {
	    doRealize();
	    //doPrefetch();   
	} 	    
	if (!controlListeners.isEmpty()) {
	    for (int i=0;i<controlListeners.size();i++)
		player.addControllerListener(
					     (ControllerListener)controlListeners.elementAt(i));
	}
	   
    }

    /**
     *
     * Gets the loop property for this <CODE>MediaPlayer</CODE>. This property indicates
     * whether the <CODE>MediaPlayer</CODE> should play the media stream 
     * repeatedly or stop when the end of media is reached.
     *
     * @return <CODE>true</CODE> if loop is enabled, <CODE>false</CODE> if the media stream 
     * is to be played only once.
     *
     */
	  
    public boolean getPlaybackLoop() {
	return looping;
	    
    }

    /**
     *
     * Sets the loop property for this <CODE>MediaPlayer</CODE>.  This property indicates
     * whether the <CODE>MediaPlayer</CODE> should play the media stream 
     * repeatedly or stop when the end of media is reached.
     *
     * @param val  The boolean value for the loop property. Specify <CODE>true</CODE> 
     * to enable looping, <CODE>false</CODE> if the media stream 
     * is to be played only once.
     *
     */
    public void setPlaybackLoop(boolean val) {
	boolean old = looping;
	looping = val;
	changes.firePropertyChange("playbackLoop", new Boolean(old),
				   new Boolean(looping));
    }

    /**
     *
     * Gets the loop property for this <CODE>MediaPlayer</CODE>. This property indicates
     * whether the <CODE>MediaPlayer</CODE> should play the media stream 
     * repeatedly or stop when the end of media is reached.
     *
     * @return <CODE>true</CODE> if loop is enabled, <CODE>false</CODE> if the media stream 
     * is to be played only once.
     *
     */
    public boolean isPlayBackLoop() {
	return looping;
    }

    /**
     *
     * Sets the zoom property for this <CODE>MediaPlayer</CODE>. This property specifies
     * a scale factor for the visual components. When <CODE>zoom</CODE> is called, the 
     * visual components are resized accordingly. 
     *
     * @param scale A <CODE>String</CODE> that specifies the zoom factor. Possible values are
     *     <CODE>Scale 1:2</CODE>, <CODE>Scale 1:1</CODE>, <CODE>Scale 2:1</CODE>, 
     *  and <CODE>Scale 4:1</CODE>.
     *
     */
    public void setZoomTo(String scale) {
	debug("setZoomTo");
	curZoomLevel = scale;
	if (scale.trim().equals(MediaPlayerResource.getString("1:2"))) {
	    curZoomValue = 0.5f;
	} else if (scale.trim().equals(MediaPlayerResource.getString("1:1"))) {
	    curZoomValue = 1.0f;
	} else if (scale.trim().equals(MediaPlayerResource.getString("2:1"))) {
	    curZoomValue = 2.0f;
	} else if (scale.trim().equals(MediaPlayerResource.getString("4:1"))) {
	    curZoomValue = 4.0f;
	}

	fixtedFirstTime=false;
	zoomTo(curZoomValue);
    }  
   
    /**
     *
     * Gets the zoom property for this <CODE>MediaPlayer</CODE>.  This property specifies
     * a scale factor for the visual components.
     *
     * @return A <CODE>String</CODE> that specifies the zoom factor. Possible values are
     *  <CODE>Scale 1:2</CODE>, <CODE>Scale 1:1</CODE>, <CODE>Scale 2:1</CODE>, 
     *  and <CODE>Scale 4:1</CODE>.
     *
     */
    public String getZoomTo() {
	return this.curZoomLevel;
    }
    
    /**
     *
     * Gets the height of the control <CODE>Component</CODE> for this <CODE>MediaPlayer</CODE>.
     *
     * @return The control <CODE>Component</CODE> height as an integer.
     *
     */
    public int getControlPanelHeight() {
	  
	if (isControlPanelVisible())
	    return controlPanelHeight;
	else
	    return 0;
    }
	
    /**
     *
     * Gets the height of the media-location text field for this <CODE>MediaPlayer</CODE>.
     *
     * @return The height of the media location text field as an integer.
     *
     */
    public int getMediaLocationHeight() {
	  
	if (isMediaLocationVisible())
	    return urlFieldHeight;
	else
	    return 0;
    }
	
    /**
     *
     *  Sets the audio volume property for this <CODE>MediaPlayer</CODE>. 
     *  The volume can range from zero to five, where zero is silence
     *  and five is the maximum volume.
     *
     * @param volumeString A <CODE>String</CODE> that represents the audio volume. 
     * Possible values are "0", "1", "2", "3", "4", or "5".
     *
     */
    public void setVolumeLevel(String volumeString) {
	debug("in setVolumeLevel");
	if (volumeString == null) {
	    return;
	}
	String old = curVolumeLevel;
	int level = Integer.parseInt(volumeString);
	curVolumeLevel = volumeString;
	curVolumeValue = level * 0.2f;

	if (gainControl != null)
	    gainControl.setLevel(curVolumeValue);
		
	changes.firePropertyChange("volumeLevel", old, curVolumeLevel);
	
    }
	
    /**
     *
     * Gets the audio volume property for this <CODE>MediaPlayer</CODE>.
     * The volume can range from zero to five, where zero is silence
     * and five is the maximum volume.
     * @return A <CODE>String</CODE> that represents the audio volume. 
     * Possible values are "0", "1", "2", "3", "4", or "5".
     *
     */
    public String getVolumeLevel() {
	return this.curVolumeLevel;
    }

    /**
     *
     * Gets the MediaLocationVisible property for this <CODE>MediaPlayer</CODE>.
     *
     * @return <CODE>true</CODE> if the media location is visible at runtime, 
     * <CODE>false</CODE> if it is not.
     *
     */
    public boolean isMediaLocationVisible() {
	return displayURL;
    }

    /**
     *
     * Sets the MediaLocationVisible property for this <CODE>MediaPlayer</CODE>.
     *
     * @param val A boolean that indicates whether or not the media location is visible.
     * Set to <CODE>true</CODE> if the media location is visible at runtime,
     * <CODE>false</CODE> if it is not.
     *
     */
    public void setMediaLocationVisible(boolean val) {
	if (displayURL != val) {
	    if (urlName !=null) {
		if (val) { 
		    urlName.setText(urlString);
		    newPanel.add("South",urlName);
		} else {
		    newPanel.remove(urlName);
		}
	    }
	 	  
	    panel.validate();
	    validate();
	    displayURL = val;
	}
    }
    /**
     *
     * Gets the control panel visibility property for this <CODE>MediaPlayer</CODE>.
     * This property indicates whether or not the control panel is visible at runtime.
     *
     * @return <CODE>true</CODE> if the control panel is visible at runtime,
     * <CODE>false</CODE> if it is not.
     *
     */
    public boolean isControlPanelVisible() {
	return panelVisible;
    }

    /**
     *
     * Sets the control panel visibility property for this <CODE>MediaPlayer</CODE>.
     * This property indicates whether or not the control panel is displayed at runtime.
     *
     * @param isVisible A boolean value that indicates whether or not the control panel 
     * should be displayed. Set to <CODE>true</CODE> to display the control panel.
     *
     */
    public void setControlPanelVisible(boolean isVisible) {
	if (panelVisible != isVisible) {
	    boolean old=panelVisible;
	    if (controlComponent != null) {
		if (isVisible) {
		    panel.add("South", controlComponent);
		} else {
		    panel.remove(controlComponent);
		}
	    }
	    invalidate();
	    validate();
	    panelVisible = isVisible;
	    changes.firePropertyChange("controlPanelVisible", new Boolean(old),
				       new Boolean(panelVisible));
	}
	  
    }

    /**
     *
     * Gets the caching control visibility property for this <CODE>MediaPlayer</CODE>.
     * This property indicates whether or not the caching control is displayed at runtime.
     * The caching control displays the download progress for media accessed over the network.
     *
     * @return <CODE>true</CODE> if the control panel is displayed at runtime,
     * <CODE>false</CODE> if it is not.
     *
     */
    public boolean isCachingControlVisible() {
	return cachingVisible;
    }

    /**
     *
     * Sets the caching control visibility property for this <CODE>MediaPlayer</CODE>.
     * This property indicates whether or not the caching control is displayed at runtime.
     * The caching control displays the download progress for media accessed over the network.
     *
     * @param isVisible A boolean value that indicates whether or not the caching control
     *  should be  displayed at
     *   runtime. Set to <CODE>true</CODE> to display the control.
     *
     */
    public void setCachingControlVisible(boolean isVisible) {
	if (cachingVisible != isVisible) {
	    boolean old = cachingVisible;
	    if (cachingComponent != null) {
		if (isVisible) {
		    panel.add("South", cachingComponent);
		} else {
		    panel.remove(cachingComponent);
		}
	    }
	    invalidate();
	    validate();
	    changes.firePropertyChange("cachingControlVisible", new Boolean(old),
				       new Boolean(cachingVisible));  
	    
	}
	cachingVisible = isVisible;
    }

    /**
     *
     * Gets the fixed aspect ratio property for this <CODE>MediaPlayer</CODE>. This 
     * property indicates whether or not the aspect ratio of the video  
     * is maintained when the visual component is resized.
     *
     * @return A boolean value that indicates whether or not the aspect ratio is maintained.
     * Returns <CODE>true</CODE> if it is, <CODE>false</CODE> if it is not.
     *
     */
    public boolean isFixedAspectRatio() {
	return fixedAspectRatio;
    }

    /**
     *
     * Sets the fixed aspect ratio property for this <CODE>MediaPlayer</CODE>. This 
     * property indicates whether or not the aspect ratio of the video  
     * is maintained when the visual component is resized.
     *
     * @param isFixed  A boolean value that indicates whether or not the aspect 
     * ratio should be maintained.
     * Set to <CODE>true</CODE> to maintain the aspect ratio.
     *
     */
    public void setFixedAspectRatio(boolean isFixed) {
	boolean old = fixedAspectRatio;
	fixedAspectRatio = isFixed;
	changes.firePropertyChange("fixedAspectRatio", new Boolean(old),
				   new Boolean(fixedAspectRatio));
    }

    /**
     *
     * Specifies whether or not the Zoom Popup Menu is active.
     * If <CODE>setPopupActive</CODE> is set to false, the popup menu won't be created.
     *
     * @param isActive A boolean value that indicates whether or not the popup menu
     * should be created.
     * Set to <CODE>true</CODE> to activate the popup menu.
     *
     */
    public void setPopupActive(boolean isActive) {
	if (isActive !=isPopupActive) {
	    isPopupActive = isActive; 
	    setPopupActive();
	}
    }
  
    /**
     *
     *   Private class activate popup according to private variable isPopupActive.
     *
     */
    private void setPopupActive() {
	if (isPopupActive ==true)
	    visualComponent.addMouseListener( mouseListen= new visualMouseAdapter());
	else if(mouseListen!=null)
	    visualComponent.removeMouseListener(  mouseListen);
    }
   
    // Player methods

    // method to get the Player's visual component.
 
    public Component getVisualComponent() {
	if (player != null) {
	    return player.getVisualComponent();
	}
	return null;
    }

    // method to get the player's gain control, if there is one.

    public GainControl getGainControl() {
	if (player != null) {
	    return player.getGainControl();
	}
	return null;
    }

    // method to get the player's default control Component.

    public Component getControlPanelComponent() {
	if (player != null) {
	    return player.getControlPanelComponent();
	}
	return null;
    }
  
 
    /**
     *
     * Starts the <CODE>MediaPlayer</CODE> as soon as possible. If the <CODE>MediaPlayer</CODE> does 
     * not already have a <CODE>Player</CODE>, 
     * it creates one and then
     * starts it.
     *
     */
    public void start() {
	if (player != null) {
	    player.start();
	} else {
	    if (mrl ==null) {
		initSetCodeBase();
		mrl=getMediaLocator(urlString);
	    }
	    
	    if (mrl !=null) {
		try {
		    player = javax.media.Manager.createPlayer(mrl);
		    player.addControllerListener(selfListener =
						 new InternalControllerAdapter(this));
		    start();
		    //add original list to new player
		    if (controlListeners.size()>0) {   
			for (int i=0;i<controlListeners.size();i++)
			    player.addControllerListener(
							 (ControllerListener)controlListeners.elementAt(i));
		    }
		} catch (Exception e) {
		    player= null;
		    urlString = " ";
		    Fatal(MediaPlayerResource.getString("UNABLE_CREATE_PLAYER")+e);
		    return;
		}
	    } else 
		Fatal(MediaPlayerResource.getString("COULD_NOT_START_PLAYER"));
	    
	     
	}
    }


    public void addController(Controller n) {
	try {
	    if (player != null) {
		player.addController(n);
	    }
	} catch (IncompatibleTimeBaseException e) {
	    Fatal(MediaPlayerResource.getString("PLAYER_NO_COMPATIBLE_TIME_BASE"));
	    return;
	}
    }
 
    /**
     *
     * Gets the current <CODE>Player</CODE> for this <CODE>MediaPlayer</CODE>. 
     *
     * @return The <CODE>Player</CODE> that this <CODE>MediaPlayer</CODE> is currently using.
     *
     */
    
    public Player getPlayer() {
	return player;
    }
  

    public void removeController(Controller old) {
	if (player != null) {
	    player.removeController(old);
	}
    }

    // MediaHandler method

    // method to set the source used to obtain content.

    public void setSource(DataSource data) {
	try {
	    if (player != null) {
		player.setSource(data);
	    }
	} catch (IOException e) {
	    Fatal(MediaPlayerResource.getString("IO_EXCEPTION")+e);
	    return;
	} catch (IncompatibleSourceException e) {
	    Fatal(MediaPlayerResource.getString("INCOMPATIBLE_SOURCE_EXCEPTION") + e);
	    return;
	}
    }
  
    // Controller methods

    // method to get the current state of the player

    public int getState() {
	if (player != null) {
	    return player.getState();
	} else {
	    return Controller.Unrealized;
	}
    }

    // method to get the target state of the player

    public int getTargetState() {
	if (player != null) {
	    return player.getTargetState();
	} else {
	    return Controller.Unrealized;
	}
    }

    // method to realize the player

    public void realize() {
	if (player != null) {
	    player.realize();
	} else {
	    return;
	}
    }

    //  method to prefetch the player
   
    public void prefetch() {
	if (player != null) {
	    player.prefetch();
	} else {
	    return;
	}
    }

    // method to Abort the current operation and cease any activity
    // that consumes system resources.
  
    public void deallocate() {
	if (player != null) {
	    debug("in deallocate");
	    player.deallocate();
	} else {
	    return;
	   
	}
    }

    // method to release all resources and cease all activity.
 
    public void close() {
	if (player != null) {
	    panel.removeAll();
	    player.close();
	    player = null;
	} else {
	    return;
	}
    }

    // method to get the Controller's start latency in nanoseconds.
  
    public Time getStartLatency() {
	if (player != null) {
	    return player.getStartLatency();
	} else {
	    return LATENCY_UNKNOWN;
	}
    }

    // method to get a list of the Control objects that this Controller supports.
  
    public Control[] getControls() {
	if (player != null) {
	    return player.getControls();
	} else {
	    return new Control[0];
	}
    }

    // method to get the Control that supports the specified class or interface.
 
    public Control getControl(String forName) {
	if (player != null) {
	    return player.getControl(forName);
	} else {
	    return null;
	}
    }

    // method to specify a ControllerListener to which this Controller will send
    // events. A Controller can have multiple ControllerListeners.

    public void addControllerListener(ControllerListener listener) {
	if (player != null) {
	    player.addControllerListener(listener);
	}
	if (!controlListeners.contains(listener))
	    controlListeners.addElement(listener);
    }


    // method to remove the specified listener from this Controller's listener list.
 
    public void removeControllerListener(ControllerListener listener) {
	if (player != null) {
	    player.removeControllerListener(listener);
	}
	if (controlListeners.contains(listener)) {
	    controlListeners.removeElement(listener);
	}
    }

    // Clock methods

    // method to set the TimeBase for this Clock. 
  
    public void setTimeBase(TimeBase master) {
	try {
	    if (player != null) {
		player.setTimeBase(master);
	    }
	} catch (IncompatibleTimeBaseException e) {
	    Fatal(MediaPlayerResource.getString("INCOMPATIBLE_TIME_BASE")+e);
	    return;
	}
    }

    // method to synchronize the current media time to the specified time-base time
    // and start the Clock. 
  
    public void syncStart(Time at) {
	debug("syncStart ");
	if (player != null) {
	    player.syncStart(at);
	}
    }

    // method to stop the Clock. 
  
    public void stop() {
	if (player != null) {
	    player.stop();
	}
    }

    /**
     * Stops the clock and deallocates the system resources used by this
     * player.
     *
     */
    public void stopAndDeallocate() {
	if (player != null) {
	    player.stop();
	}
    }

    // method to set the media time at which you want the Clock to stop.
 
    public void setStopTime(Time stopTime) {
	if (player != null) {
	    player.setStopTime(stopTime);
	}
    }

    // method to get the last value successfully set by setStopTime.
 
    public Time getStopTime() {
	if (player != null) {
	    return player.getStopTime();
	} else {
	    return null;
	}
    }

    // method to set the Clock's media time.

    public void setMediaTime(Time now) {
	if (player != null) {
	    player.setMediaTime(now);
	}
    }

    // method to get this Clock's current media time. 
  
    public Time getMediaTime() {
	if (player != null) {
	    return player.getMediaTime();
	} else {
	    return LATENCY_UNKNOWN;
	}
    }


    /**
     * Gets the current media time in nanoseconds.
     *
     * @return The current media time in nanoseconds.
     *
     */
    public long getMediaNanoseconds() {
	if (player != null) {
	    return player.getMediaNanoseconds();
	} else {
	    return Long.MAX_VALUE;
	}
    }

    // method to get the current media time or the time until this Clock
    // will synchronize to its TimeBase.
  
    public Time getSyncTime() {
	if (player != null) {
	    return player.getSyncTime();
	} else {
	    return LATENCY_UNKNOWN;
	}
    }

    // method to get the TimeBase that this Clock is using.
  
    public TimeBase getTimeBase() {
	if (player != null) {
	    return player.getTimeBase();
	} else {
	    return null;
	}
    }

    // method to get the TimeBase time that corresponds to the specified media time.
 
    public Time mapToTimeBase(Time t) {
	try {
	    if (player != null) {
		return player.mapToTimeBase(t);
	    }
	} catch (ClockStoppedException e) {
	    log(MediaPlayerResource.getString("CALL_A_STOPPED_CLOCK"));
	}

	return LATENCY_UNKNOWN;
    }

    // method to get the current temporal scale factor. 
  
    public float getRate() {
	if (player != null) {
	    return player.getRate();
	} else {
	    return 0.0f;
	}
    }

    // method to set the temporal scale factor. 
  
    public float setRate(float factor) {
	if (player != null) {
	    return player.setRate(factor);
	} else {
	    return 0.0f;
	}
    }

    // Duration method
    // method to get the duration of the media represented by this object.
 
    public Time getDuration() {
	if (player != null) {
	    return player.getDuration();
	} else {
	    return DURATION_UNKNOWN;
	}
    }

  
    /**
     *  Ensures the ability to synchronously
     *  change state. Supports serialization.
     * @param s The state to wait for. The state constants are defined in <CODE>Controller</CODE>.
     * @see Controller#Unrealized
     * @see Controller#Realizing
     * @see Controller#Unrealized
     * @see Controller#Realized
     * @see Controller#Prefetching
     * @see Controller#Prefetched
     * @see Controller#Started
     */
    public synchronized void waitForState(int s) {
	while (state != s) {
	    try {
		wait(1000);
	    } catch (Exception e) {}
	}
    }

    // really simplistic error handling
    Method errMeth = null;

    private void Fatal(String name) {
	// Use reflection to get at the Log.
	try {
	    if (errMeth == null) {
		Class cls = Class.forName("com.sun.media.Log");  
		Class params[] = new Class [] 
		{ Class.forName("java.lang.Object") };
		errMeth = cls.getMethod("error", params);
	    }
	    Object args[] = new Object [] { name };
	    errMeth.invoke(null, args);
	} catch (Throwable t) {
	    System.err.println(name);
	}
    }

    private void log(String name) {
	// Use reflection to get at the Log.
	try {
	    if (errMeth == null) {
		Class cls = Class.forName("com.sun.media.Log");  
		Class params[] = new Class [] 
		{ Class.forName("java.lang.Object") };
		errMeth = cls.getMethod("comment", params);
	    }
	    Object args[] = new Object [] { name };
	    errMeth.invoke(null, args);
	} catch (Throwable t) {
	    System.err.println(name);
	}
    }


    // Externalizable methods
    /**
     * Restores the contents of this object
     *
     * @param in The stream that can read primitive and object
     *   data types.
     *
     * @exception IOException If an IO error occurs while try to read the object.
     * @exception java.lang.ClassNotFoundException If no definition for the class can be found.
     */
    public void readExternal(ObjectInput in) throws IOException, 
	ClassNotFoundException {
	//save the component state
	setBounds((Rectangle)in.readObject());
	setBackground((Color) in.readObject());
	setForeground((Color) in.readObject());
	setFont((Font) in.readObject());
	setVisible(in.readBoolean());
	setEnabled(in.readBoolean());
	//restore MediaPlayer state
	String n = (String)(in.readObject());
	if (n != null) {
	    mrl = new MediaLocator(n);
	}
	if (mrl !=null) {
	    try {
		setMediaLocator(mrl);
	    } catch (Exception e) { }
	}
	setMediaLocationVisible(in.readBoolean());
	panelVisible = in.readBoolean();
	cachingVisible = in.readBoolean();
	fixedAspectRatio = in.readBoolean();
	preferredHeight = in.readInt();
	preferredWidth = in.readInt();
	  
	//restore Player state if appropriate
	if (in.readBoolean()) {
	    int s = in.readInt();
	    int ts = in.readInt();
	    state = Unrealized;
	    if (s >= Realized) {
		long mt = in.readLong();
		long st = in.readLong();
		float r = in.readFloat();
		if (ts >= Prefetched) {
		    player.prefetch();
		    waitForState(Prefetched);
		} else if (ts >= Realized) {
		    player.realize();
		    waitForState(Realized);
		}
		player.setMediaTime(new Time(mt));
		player.setStopTime(new Time(st));
		player.setRate(r);
		float l = in.readFloat();
		if (l != -1.0F) {
		    GainControl g = player.getGainControl();
		    if (g != null) {
			boolean mute = in.readBoolean();
			g.setLevel(l);
			g.setMute(mute);
		    } else {
			in.readBoolean();
		    }
		}
		    
		if (ts >= Started) {
		    player.start();
		}
	    }
	}
	invalidate();
	validate();
	  	  
    }

    /**
     *
     * Saves the contents of this object by calling the <CODE>ObjectOutput</CODE>
     * <CODE>writeObject</CODE> method.
     *
     * @param out The stream that can read primitive and object
     *   data types.
     * @exception IOException If an IO error occurs while attempting to save the object.
     */
    public void writeExternal(ObjectOutput out) throws IOException {
	//save the component state
	out.writeObject(getBounds());
	out.writeObject(getBackground());
	out.writeObject(getForeground());
	out.writeObject(getFont());
	out.writeBoolean(isVisible());
	out.writeBoolean(isEnabled());
	//save the MediaPlayer state
	
	if (mrl !=null) {
	    out.writeObject(mrl.toExternalForm());
	} else {
	    out.writeObject(null);
	}
	out.writeBoolean(displayURL);
	out.writeBoolean(panelVisible);  
	out.writeBoolean(cachingVisible); 
	out.writeBoolean(fixedAspectRatio);
	out.writeInt(preferredHeight);
	out.writeInt(preferredWidth);
	//save the Player state if appropriate
	if (player == null) {
	    out.writeBoolean(false);
	} else {
	    out.writeBoolean(true);
	    out.writeInt(player.getState());
	    out.writeInt(player.getTargetState());
	    if (player.getState() >= Realized) {
		out.writeLong(player.getMediaNanoseconds());
		out.writeLong(player.getStopTime().getNanoseconds());
		out.writeFloat(player.getRate());
		//we need to save Gain information:
		GainControl g;
		if ((g = player.getGainControl()) != null) {
		    out.writeFloat(g.getLevel());
		    out.writeBoolean(g.getMute());
		} else {
		    out.writeFloat(-1.0F);
		}
	    }
	}
    }
  
    /**
     *
     * zoomTo():
     *   method to handle the resize of the visual component of a player
     *   according to its input scale.
     *
     * @param float z: the scale for resizing.
     *
     */
    private void zoomTo(float z) {
	debug("zoomTo "+z);
	int ddwidth=0;
	int ddheight=0;
	if ((visualComponent != null) && fitVideo) {
	    try {
		Dimension d=visualComponent.getPreferredSize();
		ddwidth = (int)(d.width * z);
		ddheight = (int)(d.height * z);
      
		int dheight=0;
		if ((controlComponent != null) && isControlPanelVisible())
		    dheight = controlComponent.getPreferredSize().height;
		if (displayURL ==true)
		    dheight += urlName.getPreferredSize().height;
		    
		if (newPanel!=null)
		    newPanel.setSize(ddwidth,dheight);
		panel.setSize(ddwidth , ddheight+dheight );	
      			    		      
		if ((fixedAspectRatio ==true) || 
		    ((fixedAspectRatio==false)&&(fixtedFirstTime==false)))  
		    center(this,panel,true,dheight);
		else
		    center(this,panel,false,dheight);
		  
		panel.validate();
  	
	    } catch (Exception e) {
		log(MediaPlayerResource.getString("UNABLE_TO_ZOOM")+e);
	    }
		   
	}
    }
  
    /**
     *
     * aspectRatio():
     *   calculate the aspect Ratio according to the width, height and control 
     *   component, MediaLocation text height if they exist
     *
     * @param float width:  width of panel.
     *        float height:  height of panel.
     *        int   controllerHeight:  including height of control component
     *                                 and mediaLocation height
     *
     */
    private float aspectRatio(float width, float height,
			      int controllerHeight) {
	return (width/((float) (height-controllerHeight)));
    }

    /**
     *
     * center():
     *   This method moves the component to the center of parent 
     *   container. If fixAspectRatio is specified, then the width
     *   and height will be adjusted accordingly.
     *
     * @param parent  parent container
     *        comp    component to be added to parent
     *        fit     if maintain Aspect Ratio
     *        dheight height of controlComponent 
     */
    
    private void center(Container parent, Panel comp, boolean fit, int dheight) {
	int pwidth = parent.getSize().width;
	int pheight = parent.getSize().height;
  
	int width = comp.getSize().width;
	int height = comp.getSize().height;
	if (fit) {
	    float aspect = aspectRatio((float) width,(float) height, dheight);
	    if (width > pwidth) {
		height = (int) ((float)pwidth/aspect) + dheight;
		if (height > pheight) {
		    width = (int) (aspect*(pheight-dheight));
		    height = pheight;
		} else
		    width = pwidth;
		comp.setBounds(parent.getBounds().x,parent.getBounds().y,width,height);
	    } else if (height > pheight) {
		width = (int) (aspect*(pheight-dheight));
		height = pheight;
           	comp.setBounds(parent.getBounds().x,parent.getBounds().y,width,height);
	    }
	}
	comp.setLocation((pwidth/2)-(width/2), (pheight/2)-(height/2));
	comp.setSize( width,height);
    }

  
    /**
     *
     * addPopupMenu():
     *   popup menu for some preset scales of the visualComponent
     *
     * @param Component visual: the visual component to add the popup menu
     *   to.
     *
     */
    
    private void addPopupMenu(Component visual) {
	MenuItem mi;
	ActionListener zoomSelect;

	zoomMenu = new PopupMenu("Zoom");
	zoomSelect = new popupActionListener();
	visual.add(zoomMenu);
	mi = new MenuItem("Scale 1:2");
	zoomMenu.add(mi);
	mi.addActionListener(zoomSelect);
	mi = new MenuItem("Scale 1:1");
	zoomMenu.add(mi);
	mi.addActionListener(zoomSelect);
	mi = new MenuItem("Scale 2:1");
	zoomMenu.add(mi);
	mi.addActionListener(zoomSelect);
	mi = new MenuItem("Scale 4:1");
	zoomMenu.add(mi);
	mi.addActionListener(zoomSelect);
	   
    }
  
   
    /**
     * Resizes the visual <CODE>Component</CODE>, control <CODE>Component</CODE>,
     * and <CODE>urlName</CODE> according to  the specified dimensions.
     *   
     *
     * @param x The x coordinate of the rectangle.
     * @param y The y coordinate of the rectangle.
     * @param w The width of the rectangle.
     * @param h The height of the rectangle.
     *
     */
    public void setBounds(int x, int y, int w, int h) {
	debug("setBounds "+x +" "+y+" "+w+" "+h+" ");
	super.setBounds(x,y,w,h);
	Dimension d = getSize();
	int pheight = d.height;
	int pwidth = d.width;
	int p = 0;
	int totalHeight = 0;
	  
	if ((urlName!=null) &&(isMediaLocationVisible())) {
	    totalHeight = urlFieldHeight=urlName.getPreferredSize().height;
	    if ((pheight < 5) && (displayURL==true)) {
		pheight = 5;
	    }
	}    
	if ((controlComponent != null) && isControlPanelVisible()) {
	    controlPanelHeight=controlComponent.getPreferredSize().height;
	    totalHeight +=controlPanelHeight;
	   
	    if (d.width < 160)
		d.width = 160;
	    if ((pheight < 2) && (visualComponent != null))
		{
		    pheight += 2;
		}
	  
	}
	 
	if (visualComponent !=null) {
	    Dimension vSize = visualComponent.getPreferredSize();
	    if (fixedAspectRatio == true) {
		//compare the ratio of parent and panel, and get the smaller number
		if (((float)pwidth/(float)vSize.width) >=
		    ((float)(pheight-totalHeight)/
		     (float)(vSize.height-totalHeight)))
		    curZoomValue =  
			((float)(pheight-totalHeight)/
			 (float)(vSize.height-totalHeight));
		else    
		    curZoomValue = (float)pwidth/(float)vSize.width;
		
		if (curZoomValue <1.0)
		    curZoomValue=1;
		zoomTo(curZoomValue);
	    } else
		panel.setBounds(getBounds());
	   
	} else {
	    panel.setSize(getSize());
	    validate();
	}
	
    }

    /**
     *
     * Gets the dimensions of the preferred size.
     *
     * @return A <CODE>Dimension</CODE> that represents the preferred size.
     *
     */
    public Dimension getPreferredSize() {
	return new Dimension(preferredWidth, preferredHeight);
    }

    /**
     *
     * Adds a <CODE>PropertyChangeListener</CODE> to the listener list.
     *
     * @param c The <CODE>PropertyChangeListener</CODE> to be
     * added.
     *
     */
    public void addPropertyChangeListener(PropertyChangeListener c) {
	changes.addPropertyChangeListener(c);
    }

    /**
     *
     * Removes a <CODE>PropertyChangeListener</CODE> from the listener list.
     *
     * @param c The <CODE>PropertyChangeListener</CODE> to be
     *   removed.
     *
     */
    public void removePropertyChangeListener(PropertyChangeListener c) {
	changes.removePropertyChangeListener(c);
    }

    /**
     * Sets the codebase of the running applet.
     *
     * @param cb The <CODE>URL</CODE> for the codebase of the currently running applet.
     *
     */
    public void setCodeBase(URL cb) {
	mpCodeBase = cb;
    }

    /**
     * An inner class that's a  component listener for receiving component events.
     */
    
    private class visualComponentAdapter extends ComponentAdapter {
	private MediaPlayer thisBean;
    
	/**
	 * Constructor.
	 *
	 * @param b The instance of a <CODE>MediaPlayer</CODE> bean.
	 *
	 */
	public visualComponentAdapter(MediaPlayer b) {
	    super();
	    thisBean = b;
	}
    
	/**
	 *  Gets the notification that the bean has been resized and centers the panel inside the bean.
	 *
	 * @param ce The <CODE>Component</CODE> event.
	 *
	 */
	public void componentResized(ComponentEvent ce) {
     
	    if (ce.getSource() == thisBean) {
		debug("componentResized");
		int dheight=0;
		if ((controlComponent != null) && isControlPanelVisible())
		    dheight =controlComponent.getPreferredSize().height;
		center(thisBean,panel,isFixedAspectRatio(),dheight);
	    }
      
	}
   
    }
   
    /**
     *   An internal class for receiving mouse events.  Only need to implement
     *   the  methods we are interested in.
     *
     */
    private class visualMouseAdapter extends MouseAdapter {
 
	/**
	 *
	 *   Displays the zoomMenu according to the x, y values from the input <CODE>MouseEvent</CODE>.
	 *
	 * @param me: The <CODE>MouseEvent</CODE> that contains the mouse action information.
	 *
	 */
	public void mousePressed(MouseEvent me) {
	    if (me.isPopupTrigger()) {
		zoomMenu.show(visualComponent, me.getX(), me.getY());
	    }
	}

	/**
	 *
	 *   Displays the zoomMenu according the to x, y values from the input <CODE>MouseEvent</CODE>.
	 *
	 * @param me The <CODE>MouseEvent</CODE> that contains the mouse action information.
	 *
	 */
	public void mouseReleased(MouseEvent me) {
	    if (me.isPopupTrigger()) {
		zoomMenu.show(visualComponent, me.getX(), me.getY());
	    }
	}

	/**
	 *
	 *   Displays the zoomMenu according to the  x, y values from the input <CODE>MouseEvent</CODE>.
	 *
	 * @param me The <CODE>MouseEvent</CODE>  that contains the mouse action information.
	 *
	 */
	public void mouseClicked(MouseEvent me) {
	    if (me.isPopupTrigger()) {
		zoomMenu.show(visualComponent, me.getX(), me.getY());
	    }
	}
	  
    }

    /**
     *
     * An <CODE>ActionListener</CODE> for zoom panel selection.
     *
     */
    
    private class popupActionListener implements ActionListener {
	/**
	 *   Calls setZoomTo to resize the visual
	 *   component according to the option the user was chosen.
	 *
	 * @param ae  Contains the information about
	 *   the selection of the zoomMenu.
	 *
	 */
	public void actionPerformed(ActionEvent ae) {
	    String action = ae.getActionCommand();
	    setZoomTo(action);
	}
    }

 
    /**
     *   Saves the media stop time to a variable.
     *
     */
    public void saveMediaTime() {
	mediaTime = getMediaTime();
    }

    /**
     *   Restores the media time saved by <CODE>saveMediaTime</CODE>
     *   so that the video resumes from the time it was stoped.
     *
     */
    public void restoreMediaTime() {
	setMediaTime(mediaTime);
    }

  
    //{{DECLARE_CONTROLS
    //}}
    private void debug(String s) {
	//	  System.out.println(s);
    }
}

