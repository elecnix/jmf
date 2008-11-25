/*
 * @(#)Packetizer.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio.g723;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.sun.media.controls.*;

/**
 * Implements an G723 Packetizer.
 */
public class Packetizer extends com.ibm.media.codec.audio.AudioPacketizer {


     ////////////////////////////////////////////////////////////////////////////
    // Methods
    public Packetizer() {
        packetSize=48;
       	supportedInputFormats = new AudioFormat[] {
	        new AudioFormat(
                    AudioFormat.G723,
                    8000,
                    Format.NOT_SPECIFIED,
		    1,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    24*8,
                    Format.NOT_SPECIFIED,
                    Format.byteArray
                )
        } ;
        defaultOutputFormats  = new AudioFormat[] {
	        new AudioFormat(
                    AudioFormat.G723_RTP,
                    8000,
                    Format.NOT_SPECIFIED,
		    1,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    24*8,
                    Format.NOT_SPECIFIED,
                    Format.byteArray
                )
        } ;

        PLUGIN_NAME="G723 Packetizer";

    }

    protected  Format[] getMatchingOutputFormats(Format in) {

        AudioFormat af =(AudioFormat) in;

        supportedOutputFormats = new AudioFormat[] {
                new AudioFormat(
                    AudioFormat.G723_RTP,
                    8000,
                    Format.NOT_SPECIFIED,
		    1,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    24*8,
                    Format.NOT_SPECIFIED,
                    Format.byteArray
                )
        };
        return  supportedOutputFormats;
    }

    public void open() throws ResourceUnavailableException{
        setPacketSize(packetSize);
        reset();
    }


    public  java.lang.Object[] getControls() {
        if (controls==null) {
             controls=new Control[1];
             controls[0]=new PacketSizeAdapter(this,packetSize,true);
	}
        return (Object[])controls;
    }

    public synchronized void setPacketSize(int newPacketSize) {
        packetSize=newPacketSize;

        sample_count = (packetSize / 24 )*240;

        if (history==null) {
            history=new byte[packetSize];
            return;
        }

        if (packetSize > history.length ) {
            byte[] newHistory=new byte[packetSize];
            System.arraycopy(history,0,newHistory,0,historyLength);
            history=newHistory;
        }
    }



}


class PacketSizeAdapter extends com.sun.media.controls.PacketSizeAdapter {
    public PacketSizeAdapter(Codec newOwner, int newPacketSize, boolean newIsSetable) {
        super(newOwner,newPacketSize,newIsSetable);
    }

    public int setPacketSize(int numBytes) {

        int numOfPackets=numBytes/24;

        if (numOfPackets < 1) {
            numOfPackets=1;
        }

        if (numOfPackets > 100) {
            numOfPackets=100;
        }
        packetSize= numOfPackets*24;


        ((Packetizer)owner).setPacketSize(packetSize);

        return packetSize;
    }

}
