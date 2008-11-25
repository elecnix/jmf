/*
 * @(#)BufSlot.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.audio;


public class BufSlot {
  // public MixedDirectAudioRenderer mdar;
  public byte[] data;
  public int validLen;
  public boolean allocated;
  public boolean instop;
  public boolean consumed;

  public BufSlot() {
    // mdar = null;
    data = new byte[MixedDirectAudioRenderer.MIXLEN];
    validLen = 0;
    allocated = false;
    instop = true;
    consumed = true;
  }
}
