/*
 * @(#)PacketSizeAdapter.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import javax.media.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Implementation for PacketSizeControl
 */
public class PacketSizeAdapter implements javax.media.control.PacketSizeControl {
    protected Codec owner=null;
    protected boolean isSetable;
    protected int packetSize;
    Component component=null;
    String CONTROL_STRING="Packet Size";


    public PacketSizeAdapter(Codec newOwner, int newPacketSize, boolean newIsSetable) {
        packetSize= newPacketSize;
        owner = newOwner;
        isSetable = newIsSetable;
    }

    /**
     * Sets the desired maximum data size on the data that is output by this
     * encoder. This parameter is to be used as a means to convey the
     * preferred size of individual data units (packets) that are output
     * by this encoder. Returns the actual packet size that was set.
     * @param packetsize The number of bytes the maximum packet size
     * is set to
     * @return the actual packet size set by the encoder
     */
    public int setPacketSize(int numBytes) {
        return packetSize;
    }

    /**
     * Retrieve the maximum packet size used by this encoder.
     * @return Maximum packet size used by this encoder.
     */
    public int getPacketSize() {
        return packetSize;
    }

    public Component getControlComponent() {
        if (component ==null ) {
            Panel componentPanel=new Panel();
            componentPanel.setLayout(new BorderLayout() );
            componentPanel.add("Center",new Label(CONTROL_STRING,Label.CENTER) );
            TextField tf=new TextField(packetSize+"",5);
            tf.setEditable(isSetable );

            tf.addActionListener( (ActionListener)new PacketSizeListner(tf)  );

            componentPanel.add("East",tf );
            componentPanel.invalidate();
            component=componentPanel;

        }
        return (Component)component;
    }

    class PacketSizeListner implements java.awt.event.ActionListener  {
	TextField tf;
	public PacketSizeListner(TextField source) {
	    tf=source;
	}
	
	public void actionPerformed (ActionEvent e) {
	    try {
		int newPacketSize= Integer.parseInt(tf.getText() );
		System.out.println("newPacketSize "+newPacketSize);
		
		setPacketSize(newPacketSize);
	    } catch (Exception exception) {
	    }
	    
	    tf.setText(packetSize+"");
	    
	}
	
    }   
}

