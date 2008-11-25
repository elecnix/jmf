/*
 * @(#)RTPHeader.java	1.11 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;

/**
 * This class describes the RTP header of the RTP packet coming in
 * from the network. This class defines fields as defined in the RTP
 * Header of the RTP Internet draft
 */
public class RTPHeader implements java.io.Serializable {

    public  static final int VALUE_NOT_SET = -1;
    /**
     * true if there is an extension header present after the main 12
     * byte RTP header. 
     */
    private boolean extensionPresent;
  /**
     * If the extensionPresent bit was set, this is reserved for the
     * type of extension present or a identification number for the
     * extension. In the RTP extension header, this is a 2 byte id to be
     * defined by the extension profile.
     */
    private int extensionType;
    /*
     * A byte array actually describing the extension. The length of
     * this array will give the length of the extenstion data present.
     */
    private byte extension[];

    public RTPHeader(){
	extensionPresent = false;
	extensionType = VALUE_NOT_SET;
	extension = null;
    }
    public RTPHeader(int marker){
	extensionPresent = false;
	extensionType = VALUE_NOT_SET;
	extension = null;
    }
    /**
     * Constructor for RTPHeader
     */
    public RTPHeader(boolean extensionPresent,
		     int extensionType,
		     byte[] extension){

	this.extensionPresent = extensionPresent;
	this.extensionType = extensionType;
	this.extension = extension;
    }
    /**
     * Returns true if there is an extension header present after the main 12
     * byte RTP header. 
     */
    public boolean isExtensionPresent(){
	return extensionPresent;
    }
    /**
     * If the extensionPresent bit was set, this is reserved for the
     * type of extension present or a identification number for the
     * extension. In the RTP extension header, this is a 2 byte id to be
     * defined by the extension profile.This method returns this extension type
     */
    public int getExtensionType(){
	return extensionType;
    }
    /*
     * Returns a byte array actually describing the extension. The length of
     * this array will give the length of the extenstion data present.
     */
    public byte[] getExtension(){
	return extension;
    }
    public void setExtensionPresent(boolean p){
	extensionPresent = p;
    }
    /**
     * If the extensionPresent bit was set, this is reserved for the
     * type of extension present or a identification number for the
     * extension. In the RTP extension header, this is a 2 byte id to be
     * defined by the extension profile.This method returns this extension type
     */
    public void setExtensionType(int t){
	extensionType = t;
    }
    /*
     * Returns a byte array actually describing the extension. The length of
     * this array will give the length of the extenstion data present.
     */
    public void setExtension(byte[] e){
	extension = e;
    }
}
