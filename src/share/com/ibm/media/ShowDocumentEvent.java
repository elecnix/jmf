/*
 * @(#)ShowDocumentEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media;

import javax.media.*;
import java.net.*;

/**
 * This class is a JMF event that represent a hot link occurence in HotMedia
 * Player. The event is assoiciated with a URL (HTML page) which should be
 * showed.
 */
public class ShowDocumentEvent extends ControllerEvent {

  private URL u; // The URL assoiciated with the event
  private String s; // The String associated with the event
  
  /**
   * Constructor
   *
   * @param from  the Controller that generated the event
   * @param u  the URL assoiciated with the event
   * @param s  the String assoiciated with the event
   */
  public ShowDocumentEvent(Controller from, URL u, String s) {
      
    super(from);
    this.u = u;
    this.s = s;
  }
  
  /** 
   * Returns the URL associated with the event
   *
   * @return  the URL
   */  
  public URL getURL() {
    
    return u;
  }
   
  /** 
   * Returns the Sting associated with the event
   *
   * @return  the URL
   */  
  public String getString() {
    
    return s;
  }
  
}
