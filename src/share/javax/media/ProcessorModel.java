/*
 * @(#)ProcessorModel.java	1.13 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import javax.media.format.*;
import javax.media.protocol.*;

/**
 * Encapsulates the basic information required to create a
 * <code>Processor</code>. A <code>ProcessorModel</code> can be passed to
 * <code>Manager.createRealizedProcessor</code> to construct a <code>Processor</code>.
 *<P>
 * Extend this class to provide more control over what the
 * output format of each track should be and let the <code>
 * Manager.createRealizedProcessor</code> method do the rest of
 * the work of creating and realizing a suitable <code>Processor</code>.
 * @since JMF 2.0
 */
public class ProcessorModel {

    private Format [] formats = null;
    private MediaLocator inputLocator = null;
    private DataSource inputDataSource = null;
    private ContentDescriptor outputContentDescriptor = null;

    /**
     * Creates a <code>ProcessorModel</code> with null properties.
     */
    public ProcessorModel() {
	// Nothing to do
    }

    /**
     * Creates a <code>ProcessorModel</code> for the specified track formats and
     * output content-type. This constructor creates a <code>ProcessorModel</code>
     * that can be used to construct a <code>Processor</code> for capturing media data.
     * @param formats An array of <code>Format</code> objects that contains the desired track formats.
     * @param outputContentDescriptor A <code>ContentDescriptor</code> that describes the desired output content-type.
     * @return A <code>ProcessorModel</code> that encapsulates the specified attributes.
     */
    public ProcessorModel(Format [] formats,
			  ContentDescriptor outputContentDescriptor) {
	this.outputContentDescriptor = outputContentDescriptor;
	this.formats = formats;
    }

    /**
     * Creates a <code>ProcessorModel</code> for the specified input <code>DataSource</code>, track formats, and
     * output type.
     * @param inputDataSource The <code>DataSource</code> that identifies the media source for the <code>ProcessorModel</code>. 
     * @param formats An array of <code>Format</code> objects that contains the desired track formats.
     * @param outputContentDescriptor A <code>ContentDescriptor</code> that describes the desired output content-type.
     * @return A <code>ProcessorModel</code> that encapsulates the specified attributes.
     */
    public ProcessorModel(DataSource inputDataSource,
			  Format [] formats,
			  ContentDescriptor outputContentDescriptor) {
	this.inputDataSource = inputDataSource;
	this.formats = formats;
	this.outputContentDescriptor = outputContentDescriptor;
	
    }

    /**
     * Creates a <code>ProcessorModel</code> for the specified input 
     * <code>MediaLocator</code>, track formats, and output type.
     * @param inputLocator The <code>MediaLocator</code> that identifies the media source for this <code>ProcessorModel</code>. 
     * @param formats An array of <code>Format</code> objects that contains the desired track formats.
     * @param outputContentDescriptor A <code>ContentDescriptor</code> that describes the desired output content-type.
     * @return A <code>ProcessorModel</code> that encapsulates the specified attributes.
     */
    public ProcessorModel(MediaLocator inputLocator,
			  Format [] formats,
			  ContentDescriptor outputContentDescriptor) {
	this.inputLocator = inputLocator;
	this.formats = formats;
	this.outputContentDescriptor = outputContentDescriptor;
	
    }

    /**
     * Gets the number of tracks required as the output of the
     * <code>Processor</code> constructed with this <CODE>ProcessorModel</CODE>.
     * @param availableTrackCount The number of tracks available, as an integer.
     * @return The number of tracks required, as an integer.
     */
    public int getTrackCount(int availableTrackCount) {
	if (formats != null)
	    return formats.length;
	return -1;
    }

    /**
     * Gets the preferred <code>Format</code> of the specified track.
     * @param tIndex The index of the track for which you want to get the preferred <code>Format</code>.
     * @return The preferred <code>Format</code> for the track.
     */
    public Format getOutputTrackFormat(int tIndex) {
	if (formats != null && formats.length > tIndex)
	    return formats[tIndex];
	return null;
    }

    /**
     * Checks whether or not <code>tFormat</code> is an acceptable format for the track
     * <code>tIndex</code>. 
     * @param tIndex The index of the track you want to check.
     * @param tFormat The <code>Format</code> to check.     
     * @return <CODE>true</CODE> if the <CODE>Format</CODE> is acceptable, otherwise returns <CODE>false</CODE>.
     */
    public boolean isFormatAcceptable(int tIndex, Format tFormat) {
	if (formats != null && formats.length > tIndex) {
	    if (tFormat.matches(formats[tIndex]))
		return true;
	    else
		return false;
	}
	return true;
    }

    /**
     * Gets the output content-type specified by this <code>ProcessorModel</code>. 
     * @return A <code>ContentDescriptor</code> that defines the output content-type. Returns null if the 
     * output type is not applicable or the output streams are to contain raw data that doesn't pertain to a specific content-type.
     */
    public ContentDescriptor getContentDescriptor() {
	return outputContentDescriptor;
    }

    /**
     * <I>Tentative</I>.Returns the name of the file to write the output to.
     * If this is not null,
     * and the output data source has a single stream, then the output is written
     * to this file.
     *
     */
    /**
    public String getOutputFileName() {
	// TODO: return the output file name
	return null;
    }
    */
    
    /**
     * Gets the input <code>DataSource</code> that specifies the media source for this <code>ProcessorModel</code>. 
     * The <code>inputLocator</code> is ignored
     * if this value is non-null.
     * @return A <code>DataSource</code> that specifies the media source. 
     */
    public javax.media.protocol.DataSource getInputDataSource() {
	return inputDataSource;
    }

    /**
     * Gets the input <code>MediaLocator</code> that specifies the media source for this <code>ProcessorModel</code>.
     * This value is ignored if an input <code>DataSource</code> is specified.
     * @return A <code>MediaLocator</code> that specifies the media source. 
     */
    public MediaLocator getInputLocator() {
	return inputLocator;
    }
}
