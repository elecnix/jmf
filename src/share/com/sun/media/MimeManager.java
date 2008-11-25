/*
 * @(#)MimeManager.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Enumeration;
import com.sun.media.util.Registry;

public final class MimeManager {
    private static Hashtable additionalMimeTable = null;
    protected static final Hashtable defaultHashTable;
    private static MimeTable mimeTable;
    private static MimeTable defaultMimeTable;
    protected static final Hashtable extTable;
    
    private MimeManager() {
    }

    static {
	defaultHashTable = new Hashtable();
	mimeTable = new MimeTable();
	extTable = new Hashtable();

	// NOTE: The order of doPut and put is important
	// When adding to defaultHashTable, follow this
	// procedure.

	mimeTable.doPut("mov", "video/quicktime");
	defaultHashTable.put("mov", "video/quicktime");

	mimeTable.doPut("avi", "video/x_msvideo");
	defaultHashTable.put("avi", "video/x_msvideo");

	mimeTable.doPut("mpg", "video/mpeg");
	defaultHashTable.put("mpg", "video/mpeg");

	mimeTable.doPut("mpv", "video/mpeg");
	defaultHashTable.put("mpv", "video/mpeg");

	mimeTable.doPut("viv", "video/vivo");
	defaultHashTable.put("viv", "video/vivo");

	mimeTable.doPut("au", "audio/basic");
	defaultHashTable.put("au", "audio/basic");

	mimeTable.doPut("wav", "audio/x_wav");
	defaultHashTable.put("wav", "audio/x_wav");

	mimeTable.doPut("aiff", "audio/x_aiff");
	defaultHashTable.put("aiff", "audio/x_aiff"); // Default extension is aiff

	mimeTable.doPut("aif", "audio/x_aiff");
	defaultHashTable.put("aif", "audio/x_aiff");

	mimeTable.doPut("mid", "audio/midi");
	defaultHashTable.put("mid", "audio/midi");

	mimeTable.doPut("midi", "audio/midi");
	defaultHashTable.put("midi", "audio/midi");

	mimeTable.doPut("rmf", "audio/rmf");
	defaultHashTable.put("rmf", "audio/rmf");

	mimeTable.doPut("gsm", "audio/x_gsm");
	defaultHashTable.put("gsm", "audio/x_gsm");

	mimeTable.doPut("mp2", "audio/mpeg");
	defaultHashTable.put("mp2", "audio/mpeg");

	mimeTable.doPut("mp3", "audio/mpeg");
	defaultHashTable.put("mp3", "audio/mpeg");

	mimeTable.doPut("mpa", "audio/mpeg");
	defaultHashTable.put("mpa", "audio/mpeg");

	mimeTable.doPut("g728", "audio/g728");
	defaultHashTable.put("g728", "audio/g728");

	mimeTable.doPut("g729", "audio/g729");
	defaultHashTable.put("g729", "audio/g729");

	mimeTable.doPut("g729a", "audio/g729a");
	defaultHashTable.put("g729a", "audio/g729a");

	mimeTable.doPut("cda", "audio/cdaudio");
	defaultHashTable.put("cda", "audio/cdaudio");

	mimeTable.doPut("mvr", "application/mvr");
	defaultHashTable.put("mvr", "application/mvr");

	mimeTable.doPut("swf", "application/x-shockwave-flash");
	defaultHashTable.put("swf", "application/x-shockwave-flash");

	mimeTable.doPut("spl", "application/futuresplash");
	defaultHashTable.put("spl", "application/futuresplash");

	mimeTable.doPut("jmx", "application/x_jmx");
	defaultHashTable.put("jmx", "application/x_jmx");


	Object t = Registry.get("additionalMimeTable");
	if ( (t != null) && (t instanceof Hashtable) ) {
	    additionalMimeTable = (Hashtable) t;
	}

	// Remove illegal entries if any in the additionalMimeTable
	// These entries are those that remap default entries.
	if ((additionalMimeTable != null) && (!additionalMimeTable.isEmpty())) {
	    Enumeration e = additionalMimeTable.keys();
	    while (e.hasMoreElements()) {
		String ext = (String) e.nextElement();
		if (defaultHashTable.containsKey(ext)) {
		    additionalMimeTable.remove(ext);
		} else {
		    mimeTable.doPut(ext, (String) additionalMimeTable.get(ext));
		}
	    }
	}

// 	{ // TEST BLOCK
// 	    System.out.println("MimeTable: extTable is " + extTable);
// 	    System.out.println("mimeTable is " + getMimeTable());
// 	    Enumeration el = getMimeTable().elements();
// 	    while (el.hasMoreElements()) {
// 		String type = (String) el.nextElement();
// 		System.out.println("Def. extension of mimetype " + type +
// 				   " is " + getDefaultExtension(type));
// 	    }
// 	}

	defaultMimeTable =  (MimeTable) mimeTable.clone();
    }

    /**
     * Specify a mapping from a file extension to a mime type
     * example addMimeType("urv", "video/unreal")
     * You cannot override built-in mappings; for example
     * you cannot change the mapping of "mov" file extension
     * Returns true if the mapping was successfully made
     */
    public static final boolean addMimeType(String fileExtension,
					    String mimeType) {

 	if (additionalMimeTable == null)
 	    additionalMimeTable = new Hashtable();
	
	if (mimeTable.doPut(fileExtension, mimeType)) {
	    additionalMimeTable.put(fileExtension, mimeType);
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * Only the mime types that were added using addMimeType can be
     * removed with this method.
     * Returns true if the mapping was successfully removed
     */
    public static final boolean removeMimeType(String fileExtension) {
	//	return mimeTable.doRemove(fileExtension);
	if ( mimeTable.doRemove(fileExtension) ) {
	    if (additionalMimeTable != null)
		additionalMimeTable.remove(fileExtension);
	    return true;
	} else {
	    return false;
	}
    }

    public static final String getMimeType(String fileExtension) {
	// System.out.println("MimeManager: getMimeType " + fileExtension);
	return (String) mimeTable.get(fileExtension);
    }


    /**
     * Returns the mime table. This includes any mime entries that
     * were added using addMimeType
     */
    public static final Hashtable getMimeTable() {
	    return mimeTable;
    }


    /**
     * Returns the mime table which doesn't include any entries that
     * were added using addMimeType.
     */
    public static final Hashtable getDefaultMimeTable() {
	    return defaultMimeTable;
    }

    // Note: this extTable could have been built using lazy evaluation
    // That is, build it on first invocation of this method.
    // But if we build this table from the mimeTable, then
    // we will lose the order in which the entries were put
    // into the mimeTable and so getDefaultExtension may
    // return the wrong value. For example, for audio/x_aiff
    // we may not get "aiff" corresponding to the first entry
    // but instead get "aif", corresponding to the second entry.
    public static final String getDefaultExtension(String mimeType) {
	return (String) extTable.get(mimeType);
    }

    /**
     * Commit the changes
     */
    public static void commit() {
	// We should call set this even if additionalMimeTable is null
	// or empty.
	Registry.set("additionalMimeTable", additionalMimeTable);
	try {
	    Registry.commit();
	} catch (IOException e) {
	    System.err.println("IOException on commit " + e.getMessage());
	}
    }

}


final class MimeTable extends Hashtable {

    public final synchronized void clear() {
	// NOP
    }
    
    public final synchronized Object put(Object key, Object value) {
	// NOP
	return null;
    }
    
    public final synchronized Object remove(Object key) {
	// NOP
	return null;
    }

    protected final synchronized boolean doPut(String key, String value) {
	if (!MimeManager.defaultHashTable.containsKey(key)) {
	    super.put(key, value);
	    if ( MimeManager.extTable.get(value) == null )
		MimeManager.extTable.put(value, key);
	    return true;
	} else {
	    System.err.println("Cannot override default mime-table entries");
	    return false; // Cannot override default mime-table entries.
	}
    }
    
    protected final synchronized boolean doRemove(String key) {
	if (!MimeManager.defaultHashTable.containsKey(key)) {
	    if (get(key) != null)
		MimeManager.extTable.remove(get(key));
	    return (super.remove(key) != null);
	} else {
	    return false; // Cannot remove default mime-table entries.
	}
    }
}
