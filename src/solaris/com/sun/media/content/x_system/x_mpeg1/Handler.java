/*
 * @(#)Handler.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.content.x_system.x_mpeg1 ;


import com.sun.media.codec.video.mpx2.Mpx2CommandInterface ;
import com.sun.media.codec.video.mpx2.Mpx2Player ;


/**
 * A MPEG1 systems stream player implementation using MPX2
 */
public class Handler extends Mpx2Player implements Mpx2CommandInterface {
	public Handler() {
		super(MPX2_MPEG1_SYSTEM_STREAM) ;
	}
}

