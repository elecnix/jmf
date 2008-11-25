/*
 * @(#)MediaPlayerVolumePropertyEditor.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

//  Last Updated: 07/28/98
//  Notes: N/A  



package javax.media.bean.playerbean;

/**
  *
  *   Special case property editor for the MediaPlayer bean Volume property.
  *
  *   Invoked  by Java Beans<sup><font size=-2>TM</sup></font> application builders.
  *
  * @version 1.0
  *
  */

public class MediaPlayerVolumePropertyEditor
		extends java.beans.PropertyEditorSupport 
{

	/**
	  *
	  *   Generates a code fragment that can be used
	  *   to initialize a variable with the current Volume property value.
      *
      * @return  The initialization <CODE>String</CODE>.
	  *
	  */
	  
  public String getJavaInitializationString() 
  {
    return "new java.lang.String(\""+getAsText()+"\")";
	}

	/**
	  *
	  * Gets an array of tags for the Volume property.
      *
	  * @return A <CODE>String</CODE> array that contains the tags for the Volume property.
	  *
	  */
  public String[] getTags() 
  {
		String result[] = {
		  MediaPlayerResource.getString("ZERO"),
		  MediaPlayerResource.getString("ONE"),
		  MediaPlayerResource.getString("TWO"),
		  MediaPlayerResource.getString("THREE"),
		  MediaPlayerResource.getString("FOUR"),
		  MediaPlayerResource.getString("FIVE"),
		};
		return result;
  }
}
