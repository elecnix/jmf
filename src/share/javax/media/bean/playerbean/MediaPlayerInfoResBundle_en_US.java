/*
 * @(#)MediaPlayerInfoResBundle_en_US.java	1.8 02/08/21
 *  Licensed Materials - Property of IBM
 *  "Restricted Materials of IBM"
 *  5648-B81
 *  (c) Copyright IBM Corporation 1997,1999 All Rights Reserved
 *  US Government Users Restricted Rights - Use, duplication or
 *  disclosure restricted by GSA ADP Schedule Contract with
 *  IBM Corporation.
 *
 */
/**
  *
  * Class name: MediaPlayerInfoResBundle_en_US.java
  * Description:
  *   This is a bean info property resource bundle file for
  *   en_US locale specific.
  *
  * Invocation:
  *   Invoked: by the classLoader.
  *
  * @see MedaiPlayerResource
  *
  * @version 1.0
  * Last Updated: 02/20/99
  * Notes: N/A  *
  *
  */

package javax.media.bean.playerbean;
import  java.util.ListResourceBundle;
import  java.text.*;

/**
  * Description:
  *   This is a bean info property resource bundle file for
  *   en_US locale specific.
  */
  
public class MediaPlayerInfoResBundle_en_US extends ListResourceBundle
{
  protected Object[][] getContents()
  {
    return contents;
  }

  private Object[][] contents =
  {
    // The following strings are used in bean properties
    // user interface.
    {"MEDIA_LOCATION",          "media location"},
    {"CONTROL_PANEL_VISIBLE",   "show control panel"},
    {"CACHING_CONTROL_VISIBLE", "show caching control "},
    {"FIXED_ASPECT_RATIO",      "fixed aspect ratio"},
    {"LOOP",                    "loop"},

    {"ZOOM",                    "zoom"},
    {"SCALE_HALF",              "Scale 1:2"},
    {"SCALE_NORMAL",            "Scale 1:1"},
    {"SCALE_DOUBLE",            "Scale 2:1"},
    {"SCALE_QUADRUPLE",         "Scale 4:1"},
    
    {"1:2",                     "Scale 1:2"},
    {"1:1",                     "Scale 1:1"},
    {"2:1",                     "Scale 2:1"},
    {"4:1",                     "Scale 4:1"},
    
    // constant string values for the Volume property
    {"VOLUME",                  "volume"},
    
    {"ZERO",                    "0"},
  	{"ONE",                     "1"},
	  {"TWO",                     "2"},
	  {"THREE",                   "3"},
	  {"FOUR",                    "4"},
	  {"FIVE",                    "5"},

    {"NAME",                    "name"},

    {"MEDIA_PLAYER",            "MediaPlayer"},
    {"SET",                     "Set"},
    {"YES",                     "Yes"},
    {"NO",                      "No"},
    {"BEAN",                    "Bean"},
    {"SET_MEDIA_LOCATION",      "Set Media Location"},
    {"READY",                   "Ready"},
    
    // protocol
    {"HTTP",                    "http://"},
    {"HTTPS",                    "https://"},
    {"RTP",                     "rtp://"},
    {"FTP",                     "ftp://"},
    {"FILE",                    "file:///"},
    {"CODEBASE",                "<codebase>/"},
    {"CHOOSE_ONE",              "choose one"},
    
    //error case
    {"NO IMAGE:BAD_URL",        "Couldn't create image: badly specified URL"},
    {"UNABLE_CREATE_PLAYER",    "Unable to create a player"},
    {"NO_URL",                  "no URL specified"},
    {"UNABLE_CREATE_PLAYER_FOR ",    "Unable to create a player for"},
    {"COULD_NOT_START_PLAYER",  "Could not start the player"},
    {"PLAYER_NO_COMPATIBLE_TIME_BASE", "Players have no compatible time base."},
    {"IO_EXCEPTION",            "IO Exception: "},
    {"INCOMPATIBLE_SOURCE_EXCEPTION", "Incompatible Source exception: "},
    {"INCOMPATIBLE_TIME_BASE",  "Incompatible Time base: "},
    {"UNABLE_TO_ZOOM",          "Unable to zoom"},
    {"UNABLE_TO_RESIZE",        "unable to resize"},
    {"CANNOT_FIND_STRING",      "Can't find the string "},
    {"IN_RESOURCE_FILE",        "in resource file"},
    {"CLASS_NOT_FOUND",         "class not found"},
    {"CALL_A_STOPPED_CLOCK",    "call a stopped clock"}, 
    {"SHOULD NOT OCCUR",        "should not occur situation"},
  };
}
