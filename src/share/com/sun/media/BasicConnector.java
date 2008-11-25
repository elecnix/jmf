/*
 * @(#)BasicConnector.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import javax.media.*;
import javax.media.Format;
/**
 * Defines implementation of the Connector interface.
 *
 **/
public abstract class BasicConnector implements Connector{
     /** the module which registered this connector **/
     protected Module module=null;
     /** minimum number of data chunks this connector should allocate **/
     protected int minSize=1;
     /** the format chosen for this connector. null if none **/
     protected Format format=null;
     /** the circularBuffer which is the connector memory **/
     protected CircularBuffer circularBuffer=null;
     /** the name the module chose for this connector **/
     protected String name = null;
     /** the data transfer protocol chosen for this Connector.<br>
      *  either <i>ProtocolPush, ProtocolSafe</i>
      *  @see Connector#ProtocolPush
      *  @see Connector#ProtocolSafe
      **/
     protected int protocol = ProtocolPush;

    /** returns the circular buffer
     * @see #circularBuffer
     **/
    public Object getCircularBuffer() {
        return circularBuffer;
    }

    /**
     * sets the CircularBuffer of this Connector.
     * @see #circularBuffer
     **/
    public void setCircularBuffer(Object cicularBuffer){
       this.circularBuffer = (CircularBuffer)cicularBuffer;
    }

    /**
     * Selects a format for this Connector. Delegates this call to its owning Module
     * @see #format
     **/
    public void setFormat(Format format){
      module.setFormat((Connector)this,format);
      this.format=format;
    }

    /**
     * The selected format.
     * @see #format
     */
    public Format getFormat(){
      return format;
    }

    /**
     * Returns the Module which owns this Connector.
     * @see #module
     */
    public Module getModule() {
        return module;
    }
    /**
     * sets the Module which owns this Connector.
     * @see #module
     *
     */
    public void setModule(Module module) {
        this.module = module;
    }


    /**
     * sets the <b>minimum</b> number of buffer objects this Connector should create.
     * The default value should be one buffer object.
     * @see #minSize
     **/
    public void setSize(int numOfBufferObjects){
        minSize = numOfBufferObjects;
    }

    /**
     * gets the <b>minimum</b> number of buffer objects this Connector should create.
     * @see #minSize
     **/
    public int getSize() {
        return minSize;
    }


    /**
     * restores this Connector to its initial state
     **/
    public void reset() {
      circularBuffer.reset();
    }

    /** returns the name of this Connector
     *  @see #name
     **/
    public String getName(){
        return name;
    }

    /** sets the name of this Connector
     *  @see #name
     **/
    public void setName(String name){
        this.name=name;
    }

    /** sets the data transfer protocol used by this connector.
     *  @see #protocol
     **/
    public void setProtocol(int protocol){
        this.protocol=protocol;
    }

    /** returns the data transfer protocol used by this connector.
     *  @see #protocol
     **/
    public int getProtocol(){
        return protocol;
    }

    public void print() {
	circularBuffer.print();
    }


}
