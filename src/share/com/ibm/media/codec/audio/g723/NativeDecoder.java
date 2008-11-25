/*
 * @(#)NativeDecoder.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio.g723;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;


public class NativeDecoder extends JavaDecoder {

    ////////////////////////////////////////////////////////////////////////////
    // Variables
    ////////////////////////////////////////////////////////////////////////////

    // variable used by native code to store a pointer to the C++ class
    int nativeData;



    ////////////////////////////////////////////////////////////////////////////
    // Native methods

    // initialize the native codec
    private native void initNative();

    // free any buffers allocated by the native codec
    private native void freeNative();

    // free any buffers allocated by the native codec
    private native void resetNative();

    private native boolean decodeNative(byte[] inpBuffer,int readPtr,
                                        byte[] outBuffer,int writePtr,
					int inpLength);




    ////////////////////////////////////////////////////////////////////////////
    // Methods


    public NativeDecoder() {
    }

    /** Initializes the codec.  **/
    public void open() throws ResourceUnavailableException {
    	try {
            com.sun.media.JMFSecurityManager.loadLibrary("jmutil");
            com.sun.media.JMFSecurityManager.loadLibrary("jmg723");
            initNative();
            return;

        } catch (Throwable t) {
            System.err.println("can not load "+PLUGIN_NAME);
            System.err.println("reason : "+t);
            throw new ResourceUnavailableException("can not load "+PLUGIN_NAME);

	}

    }

    /** Clean up **/
    public void close() {
        freeNative();
    }

    public void reset() {
        resetNative();
    }

    protected void decode( byte[] inpData,int readPtr,
                           byte[] outData,int writePtr,
			   int inpLength) {

        decodeNative(inpData,readPtr,outData,writePtr,inpLength);
    }





}

