/*
 * @(#)DBItem.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.customizer;

/**
 *  This class represents an entry value in the hashtable defiend in 
 *  CustomDB.java
 * 
 *  @version 2.0
 */

public class DBItem {

    int[] tags;
    boolean marked = false;

    public DBItem(int[] tagarray) {
	tags = tagarray;
	marked = false;
    }

    public boolean isMarked() {
	return marked;
    }

    public void setMark(boolean m) {
	marked = m;
    }

    public boolean contains(int tag) {
	for ( int i = 0; i < tags.length; i++)
	    if (tags[i] == tag)
		return true;
	return false;
    }

}
	
  
