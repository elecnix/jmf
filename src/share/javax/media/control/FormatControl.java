/*
 * @(#)FormatControl.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

import javax.media.Format;


/**
 * The <code>FormatControl</code> interfaces is implemented by objects which
 * supports format setting.
 * @since JMF 2.0 
 */

public interface FormatControl extends javax.media.Control {

    /**
     * Obtain the format that this object is set to.
     * @return the current format.
     */
    public Format getFormat();

    /**
     * Sets the data format.
     * The method returns null if the format is not supported.
     * Otherwise, it returns the format that's actually set.
     * <p>
     * However in some situations, returning a non-null
     * format does not necessarily mean that the format is supported
     * since determining the supported formats may be state dependent
     * or simply too costly.  In such cases, the setFormat call will
     * succeed but the object may fail in another state-transition
     * operation such as when the object is being initialized.
     * <p>
     * Alternatively, the getSupportedFormats method can be used to 
     * query for the list of supported formats.  The resulting list 
     * can be used to screen for the valid formats before setting 
     * that on the object.
     *
     * @return null if the format is not supported; otherwise return
     * the format that's actually set.
     */
    public Format setFormat(Format format);

    /**
     * Lists the possible input formats supported by this plug-in.
     * @return an array of the supported formats
     */
    public Format [] getSupportedFormats();

    /**
     * Return the state of the track.  
     * @return A boolean telling whether or not the track is enabled.
     */
    public boolean isEnabled();

    /**
     * Enable or disable the track.
     * @param enabled true if the track is to be enabled.
     */
    public void setEnabled(boolean enabled);
}

