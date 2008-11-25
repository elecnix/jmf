/*
 * @(#)GraphNode.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media;

import javax.media.*;


/**
 * Used as a node for the node graph.
 * A node contains a pointer to the plugin, the input and output formats,
 * a pointer pointing to the previous node and a level marker.
 * With the use of the "prev" node pointer, a full "track" of plugins
 * can be represented.
 * It also serves as a cache to store the supported input and output
 * formats.  That way, the plugin doesn't have to be queried every
 * time for the supported formats.
 */
class GraphNode {
	Class clz;
	String cname;
	PlugIn plugin;
	int type = -1;
	Format input, output = null;
	Format supportedIns[], supportedOuts[];
	GraphNode prev;
	int level;
	boolean failed = false;
	boolean custom = false;

	static int ARRAY_INC = 30;

	GraphNode(PlugIn plugin, Format input, GraphNode prev, int level) {
	    this((String)(plugin == null ? null : plugin.getClass().getName()),
			plugin, input, prev, level);
	}

	GraphNode(String cname, PlugIn plugin, Format input, GraphNode prev, int level) {
	    this.cname = cname;
	    this.plugin = plugin;
	    this.input = input;
	    this.prev = prev;
	    this.level = level;
	}

	GraphNode(GraphNode gn, Format input, GraphNode prev, int level) {
	    this.cname = gn.cname;
	    this.plugin = gn.plugin;
	    this.type = gn.type;
	    this.custom = gn.custom;
	    this.input = input;
	    this.prev = prev;
	    this.level = level;
	    this.supportedIns = gn.supportedIns;
	    if (gn.input == input)
		supportedOuts = gn.supportedOuts;
	}

	Format [] getSupportedInputs() {
	    if (supportedIns != null)
		return supportedIns;
	    else if (plugin == null)
		return null;
	    else if ((type == -1 || type == PlugInManager.CODEC) &&
		     plugin instanceof Codec)
		supportedIns = ((Codec)plugin).getSupportedInputFormats(); 
	    else if ((type == -1 || type == PlugInManager.RENDERER) &&
		     plugin instanceof Renderer)
		supportedIns = ((Renderer)plugin).getSupportedInputFormats(); 
	    else if (plugin instanceof Multiplexer)
		supportedIns = ((Multiplexer)plugin).getSupportedInputFormats(); 
	    return supportedIns;
	}

	Format [] getSupportedOutputs(Format in) {
	    if (in == input && supportedOuts != null)
		return supportedOuts;
	    else if (plugin == null)
		return null;
	    else if ((type == -1 || type == PlugInManager.RENDERER) &&
		     plugin instanceof Renderer)
		return  null;
	    else if ((type == -1 || type == PlugInManager.CODEC) &&
		     plugin instanceof Codec) {
		Format outs[];
		outs = ((Codec)plugin).getSupportedOutputFormats(in); 
		if (input == in /* && supportedOuts == null */)
		    supportedOuts = outs;
		return outs;
	    }
	    return null;
	}

	public void resetAttempted() {
	    attemptedIdx = 0;
	    attempted = null;
	}

	int attemptedIdx = 0;
	Format attempted[] = null; // An array of input formats attempted for
				// this plugin.  This is for pruning the
				// the visited paths.
	boolean checkAttempted(Format input) {
	    if (attempted == null) {
		attempted = new Format[ARRAY_INC];
		attempted[attemptedIdx++] = input;
		return false;
	    }
	    int j;
	    for (j = 0; j < attemptedIdx; j++) {
		if (input.equals(attempted[j]))
		    return true;
	    }
	    // The given input format has not been attempted for 
	    // this plugin, we'll add that in.
	    if (attemptedIdx >= attempted.length) {
		// Expand the array.
		Format newarray[] = new Format[attempted.length+ARRAY_INC];
		System.arraycopy(attempted, 0, newarray, 0, attempted.length);
		attempted = newarray;
	    }
	    attempted[attemptedIdx++] = input;
	    return false;
	}

}


