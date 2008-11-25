/*
 * @(#)NativeEncoder_ms.java	1.8 03/04/24
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio.gsm;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;

/**
 * MS GSM encoder plugin wrapper, which uses native methods to do the encoding.
 * @author Shay Ben-David bendavid@haifa.vnet.ibm.com
 */
public final class NativeEncoder_ms extends JavaEncoder_ms {

     ////////////////////////////////////////////////////////////////////////////
    // Variables

   /** <FONT COLOR="#FF0000">
    *  Licensed Materials - Property of IBM                         <br><br>
    *  "Restricted Materials of IBM"                                <br><br>
    *  5648-B81                                                     <br><br>
    *  (c) Copyright IBM Corporation 1997,1999 All Rights Reserved  <br><br>
    *  US Government Users Restricted Rights - Use, duplication or
    *  disclosure restricted by GSA ADP Schedule Contract with
    *  IBM Corporation.</FONT>
    *
    **/
    public static final String a_copyright_notice="(c) Copyright IBM Corporation 1997,1999.";

    int nativeData;
     ////////////////////////////////////////////////////////////////////////////
    // Methods
    protected native void  initNative();
    protected native void  resetNative();
    protected native void  freeNative();
    protected native boolean  codecProcess(byte[] inpData,int readPtr,
                                    byte[] outData,int writePtr,
  			            int inpLength,
				    int[]  readBytes,int[] writeBytes,
                                    int[]  frameNumber,
				    int[] regions,int[] regionsTypes) ;

    public NativeEncoder_ms() {
    }

    public void open() throws ResourceUnavailableException{
        try {
	    JMFSecurityManager.loadLibrary("jmutil");
	    JMFSecurityManager.loadLibrary("jmgsm");
            initNative();
            return;

	} catch (Throwable t) {
        System.out.println(t);
	}


        throw new ResourceUnavailableException("Unable to load "+PLUGIN_NAME);
    }

    public void codecReset() {
        resetNative();
    }

    public void close() {
        freeNative();
    }



}



