/*
 * @(#)EncryptionInfo.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;

/**
 * Class to encapsulate the encryption parameters of an RTP Session.
 */
public class EncryptionInfo implements java.io.Serializable {
    private byte[] key;
    private int type;
    
    public final static int NO_ENCRYPTION = 0;
    // this order has to match with order of strings in
    // encryptorTypes[] of DefaultEncryptorFactory.java  
    public final static int XOR = 1;
    public final static int MD5 = 2;
    public final static int DES =3 ;
    public final static int TRIPLE_DES = 4;
     
    public EncryptionInfo(int type,
			  byte[] key){
	this.type = type;
	this.key = key;
    }
    /**
     * Accessor method to get the type of encryption this object specifies. <P>
     *
     * @return The type of encryption this object specifies. <P>
     */
    public int getType(){
	return type;
    }
    /**
     * Accessor method to get the encryption key this object encapsulates. <P>
     *
     * @return The encryption key. <P>
     */
    public byte[] getKey(){
	return key;
    }
    
}









