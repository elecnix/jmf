/*
 * @(#)MediaPlayerResource.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

// @version 1.0
// Last Updated: 07/28/98
// Notes: N/A  

package javax.media.bean.playerbean;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
 
/** 
  *
  *  Defines the resource boundle for a <CODE>MediaPlayer</CODE>, mainly for
  *   error messages. Invoked  by the class loader.
  *
  * @see MediaPlayerBeanInfo
  * @see MediaPlayer
  * @see MediaPlayerVolumePropertyEditor
  * @see MediaPlayerMediaLocationEditor  
  */
  
public class MediaPlayerResource
{

/** 
  * Defines the <CODE>ResourceBundle</CODE>.
  */ 
  
  public static ResourceBundle resourceBundle;

  // Static initalizer for the resource bundle
  static
  {
    try
    {
      resourceBundle = ResourceBundle.getBundle(
            "javax.media.bean.playerbean.MediaPlayerInfoResBundle", Locale.getDefault());
    }
    catch (MissingResourceException e)
    {
	//System.err.println("javax.media.bean.playerbean.MediaPlayerInfoResBundle.class not found");
    }
  }

  /**
    *
    *   Gets a <CODE>String</CODE> that  corresponds to the
    *   specified input string from a resource boundle file.
    *
    * @param inputString The <CODE>String</CODE> from a resource boundle file.
    *
    */
  public static String getString(String inputString)
  {
    String s = null;
    try
    {
      s = resourceBundle.getString(inputString);
    }
    catch(MissingResourceException e)
    {
        //System.err.println(resourceBundle.getString("CANNOT_FIND_STRING")+inputString+
        //                resourceBundle.getString("IN_RESOURCE_FILE"));
        s = '*'+inputString+'*';
    }
    catch(Throwable e)
    {
        //System.err.println("No resource file");
        s = '*'+inputString+'*';
    }
    return s;
  }


}
