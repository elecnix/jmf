/*
 * @(#)StringControlAdapter.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import javax.media.*;
import com.sun.media.*;
import java.awt.*;

public class StringControlAdapter extends AtomicControlAdapter
implements StringControl {

    String value;
    String title;
    
    public StringControlAdapter() {
	super(null, true, null);
    }

    public StringControlAdapter(Component c, boolean def, Control parent) {
	super(c, def, parent);
    }

    public String setValue(String value) {
	this.value = value;
	informListeners();
	return value;
    }

    public String getValue() {
	return value;
    }

    public String getTitle() {
	return title;
    }

    public String setTitle(String title) {
	this.title = title;
	informListeners();
	return title;
    }
}
