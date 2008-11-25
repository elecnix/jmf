/*
 * @(#)MediaPlayerBeanInfo.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
 
// @version 1.0
// Last Updated: 01/28/99
// Notes: N/A  


package javax.media.bean.playerbean;

import java.beans.*;

/**
  *   Provides explicit bean information for the 
  *   <CODE>MediaPlayer</CODE>.  It provides descriptions of each property and  
  *   indicates which <CODE>MediaPlayer</CODE> properties are bound.
  *  Invoked  by JavaBeans<sup><font size=-2>TM</sup></font> application builders.
  */
  
public class MediaPlayerBeanInfo extends SimpleBeanInfo 
{
  
  private final static Class beanClass = MediaPlayer.class;

  /**
    *  Gets a list of bean <CODE>PropertyDescriptor</CODE> objects, one for 
    *   each public property in the <CODE>MediaPlayer</CODE>. 
    *
    * @return  A <CODE>PropertyDescriptor</CODE> array that contains all of the bean property 
    * descriptors for this <CODE>MediaPlayer</CODE>.
    */
  public PropertyDescriptor [] getPropertyDescriptors() 
  {
	  try 
	  {
	    /* general UI properties */
	    PropertyDescriptor background = new
      	PropertyDescriptor("background", beanClass);
	    PropertyDescriptor foreground = new
		    PropertyDescriptor("foreground", beanClass);
	    PropertyDescriptor font = new
		    PropertyDescriptor("font", beanClass);
		
	    /* Behavior & appearance properties for the bean  */
	    
	    PropertyDescriptor Url = 
	              new PropertyDescriptor("mediaLocation", beanClass);
	    Url.setDisplayName(MediaPlayerResource.getString("MEDIA_LOCATION"));
	    Url.setBound(true);
		  Url.setPropertyEditorClass(MediaPlayerMediaLocationEditor.class);
  
	    PropertyDescriptor PanelVisible =
	              new	PropertyDescriptor("controlPanelVisible", beanClass);
	    PanelVisible.setDisplayName(
	              MediaPlayerResource.getString("CONTROL_PANEL_VISIBLE"));
	    PanelVisible.setBound(true);

      PropertyDescriptor CacheVisible =
                new	PropertyDescriptor("cachingControlVisible", beanClass);
	    CacheVisible.setDisplayName(
	              MediaPlayerResource.getString("CACHING_CONTROL_VISIBLE"));
	    CacheVisible.setBound(true);

      PropertyDescriptor FixedAspectRatio =
                new PropertyDescriptor("fixedAspectRatio", beanClass);
	    FixedAspectRatio.setDisplayName(
	              MediaPlayerResource.getString("FIXED_ASPECT_RATIO"));
	    FixedAspectRatio.setBound(true);
      
	    PropertyDescriptor loop = 
	              new PropertyDescriptor("playbackLoop", beanClass);
	    loop.setBound(true);
    	loop.setDisplayName(MediaPlayerResource.getString("LOOP"));
      
      PropertyDescriptor volume =
                new PropertyDescriptor("volumeLevel", beanClass);
	    volume.setBound(true);
		  volume.setDisplayName(MediaPlayerResource.getString("VOLUME"));
		  volume.setPropertyEditorClass(MediaPlayerVolumePropertyEditor.class);
    
	    /* build an array of property descriptors */
	    PropertyDescriptor rv[] = { Url, PanelVisible, CacheVisible, 
			          FixedAspectRatio,loop, volume,
			          background, foreground, font};
	    return rv;
	  } 
	  catch (IntrospectionException e) 
	  {
	    throw new Error(e.toString());
	  }
	}

  /**
    *  Gets the default property index for this bean.
    *  The "mediaLocation" property is the default property 
    *   for the <CODE>MediaPlayer</CODE>.
    *
    * @return  The default property index for the bean.
    */
  public int getDefaultPropertyIndex() 
  {
	  return 1;
  }
  
  /**
    * Gets the bean descriptor for this bean, which defines information 
    * about <CODE>MediaPlayer</CODE> such as its <CODE>displayName</CODE>.
    *
    * @return  The <CODE>BeanDescriptor</CODE> for <CODE>MediaPlayer</CODE>.
    *
    */
  public BeanDescriptor getBeanDescriptor() 
  {
		BeanDescriptor bd = new BeanDescriptor(MediaPlayer.class);
		bd.setDisplayName(MediaPlayerResource.getString("MEDIA_PLAYER")+
		                  " " + MediaPlayerResource.getString("BEAN"));
		//"MediaPlayer Bean");
		return bd;
  }

  /**
    * Gets a list of the events fired by this bean.
    *
    * @return  An array of <CODE>EventSetDescriptor</CODE> objects that describes the kinds 
    *     of events fired by this bean. 
    */
  public EventSetDescriptor[] getEventSetDescriptors() 
  {
	  try 
	  {
	    
	    EventSetDescriptor cl = new EventSetDescriptor(beanClass,
							   "controllerUpdate",
							   javax.media.ControllerListener.class,
							   "controllerUpdate");
							   
	    EventSetDescriptor pc = new EventSetDescriptor(beanClass,
							   "propertyChange",
							   java.beans.PropertyChangeListener.class,
							   "propertyChange");
	    cl.setDisplayName("Controller Events");
	    EventSetDescriptor[] rv = {cl, pc};
	    return rv;
	  } 
	  catch (IntrospectionException e) 
	  {
	    throw new Error(e.toString());
	  }
  }

  /**
    *
    *  Gets the icon to be used in the bean tool for this bean.
    *
    * @param ic Indicates the format for the icon image. The valid format constants are defined
    * in <CODE>SimpleBeanInfo</CODE>: <CODE>ICON_COLOR_16x16</CODE>, <CODE>ICON_COLOR_32x32</CODE>,
    * <CODE>ICON_MONO_16x16</CODE>, and <CODE>ICON_MONO_16x16</CODE>.
    * 
    * @return  The icon to be used as an AWT <CODE>Image</CODE>. 
    */
  public java.awt.Image getIcon(int ic) {
	  switch (ic) {
	    case BeanInfo.ICON_COLOR_16x16:
	      return loadImage("MediaPlayerColor16.gif");
	    case BeanInfo.ICON_COLOR_32x32:
	      return loadImage("MediaPlayerColor32.gif");
	    case BeanInfo.ICON_MONO_16x16:
	      return loadImage("MediaPlayerMono16.gif");
	    case BeanInfo.ICON_MONO_32x32:
	      return loadImage("MediaPlayerMono32.gif");
	    default:
	      return null;
	  }
  }

}
