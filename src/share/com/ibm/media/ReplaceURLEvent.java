/*
 * @(#)ReplaceURLEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media;

import javax.media.*;
import java.net.*;

/**
 * This class is a JMF event that represent a hot link occurence in HotMedia
 * Player. KThe event is assoiciated with a URL which should replace the
 * current played URL.
 */
public class ReplaceURLEvent extends ControllerEvent {

    private URL u; // The URL assoiciated with the event
  
  /**
   * Constructor
   *
   * @param from  the Controller that generated the event
   * @param u  the URL assoiciated with the event
   */
  public ReplaceURLEvent(Controller from, URL u) {
      
    super(from);
    this.u = u;
  }
  
  /** 
   * Returns the URL associated with the event
   *
   * @return  the URL
   */  
  public URL getURL() {
    
    return u;
  }
  
}
