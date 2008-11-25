/*
 * @(#)NativeDecoder_ms.java	1.1 98/11/24 
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.ibm.media.codec.audio.gsm;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.sun.media.util.*;

/**
 * MS GSM decoder plugin wrapper, which uses native methods to do the decoding.
 * @author Shay Ben-David bendavid@haifa.vnet.ibm.com
 */
public final class NativeDecoder_ms extends JavaDecoder_ms {

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
     protected native void  freeNative();
     protected native void  resetNative();
     protected native void  decodeNative(byte[] inpData,int readPtr,
                                byte[] outData,int writePtr,int inpLength) ;


    public NativeDecoder_ms() {
    }

    public void open() throws ResourceUnavailableException{
        try {
	    JMFSecurityManager.loadLibrary("jmutil");
	    JMFSecurityManager.loadLibrary("jmgsm");
            initNative();
            return;

	} catch (Throwable t) {
	}

        throw new ResourceUnavailableException("Unable to load "+PLUGIN_NAME);
    }

    public void reset() {
        resetNative();
    }

    public void close() {
        freeNative();
    }

    protected void decode(byte[] inpData,int readPtr,byte[] outData,int writePtr,int inpLength) {
        decodeNative(inpData,readPtr,outData,writePtr,inpLength);
    }


}



