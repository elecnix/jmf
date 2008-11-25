/*
 * @(#)MSHeavyComponent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.video;

import java.awt.*;

public class MSHeavyComponent
    extends com.sun.media.renderer.video.HeavyComponent
    implements com.ms.awt.HeavyComponent {
    
    public boolean needsHeavyPeer() {
	return true;
    }
}
