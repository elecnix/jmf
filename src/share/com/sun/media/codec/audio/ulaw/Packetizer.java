/*
 * @(#)Packetizer.java	1.10 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.audio.ulaw;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;
import com.sun.media.*;
import com.sun.media.controls.*;

/**
 * Implements an MuLaw Packetizer.
 */
public class Packetizer extends com.ibm.media.codec.audio.AudioPacketizer {


     ////////////////////////////////////////////////////////////////////////////
    // Methods
    public Packetizer() {
        packetSize=480;
       	supportedInputFormats = new AudioFormat[] {
	        new AudioFormat(
                    AudioFormat.ULAW,
                    Format.NOT_SPECIFIED,
                    8,
		    1,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    8,
                    Format.NOT_SPECIFIED,
                    Format.byteArray
                )
        } ;
        defaultOutputFormats  = new AudioFormat[] {
	        new AudioFormat(
                    AudioFormat.ULAW_RTP,
                    Format.NOT_SPECIFIED,
                    8,
		    1,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    8,
                    Format.NOT_SPECIFIED,
                    Format.byteArray
                )
        } ;

        PLUGIN_NAME="Mu-Law Packetizer";

    }

    protected  Format[] getMatchingOutputFormats(Format in) {

        AudioFormat af =(AudioFormat) in;

        supportedOutputFormats = new AudioFormat[] {
                new AudioFormat(
                    AudioFormat.ULAW_RTP,
                    af.getSampleRate(),
                    8,
		    1,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    8,
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

        sample_count = packetSize;

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

        int numOfPackets=numBytes;

        if (numOfPackets < 10) {
            numOfPackets=10;
        }

        if (numOfPackets > 8000) {
            numOfPackets=8000;
        }
        packetSize= numOfPackets;


        ((Packetizer)owner).setPacketSize(packetSize);

        return packetSize;
    }

}
