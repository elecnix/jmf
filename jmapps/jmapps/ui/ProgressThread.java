/*
 * @(#)ProgressThread.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.ui;

import javax.media.*;


/**
* This thread class is used by SaveAsDialog to monitor the progress of saving
* the file and updating the Progress dialog.
*/
public class ProgressThread extends Thread {

    private Processor       processor;
    private ProgressDialog  dlgProgress;
    private boolean         boolTerminate = false;
    private boolean         boolSuspended = false;


    /**
    * This constructor creates object ProgressThread.
    * @param    processor      processor, that does file save
    * @param    dlgProgress    Progress dialog
    */
    public ProgressThread ( Processor processor, ProgressDialog dlgProgress ) {
        this.processor = processor;
        this.dlgProgress = dlgProgress;
    }

    public synchronized void terminateNormaly () {
        boolTerminate = true;
	try {
	    this.interrupt();
	} catch (Exception ex) {}
    }
    
    public synchronized void pauseThread () {
        boolSuspended = true;
    }
    
    public synchronized void resumeThread () {
        boolSuspended = false;
        notify ();
    }
    
    
    /**
     *
     */
    public void run () {
        int    nPos;
	
        boolTerminate = false;
        while ( !boolTerminate && !this.isInterrupted() ) {
            try {
                sleep ( 200 );
                if ( boolSuspended == true ) {
                    synchronized ( this ) {
                        while ( boolSuspended )
                            wait ();
                    }
                }
		
		nPos = (int) processor.getMediaTime().getSeconds();
		dlgProgress.setCurPos ( nPos );
            }catch ( Exception exception ) {
		boolTerminate = true;
		break;
            }
        }
    }
}


