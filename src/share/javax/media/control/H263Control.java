/*
 * @(#)H263Control.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.control;

/**
 * This interface is a Control for specifying the parameters for H263
 * video codec.
 * @since JMF 2.0
 */
public interface H263Control extends javax.media.Control {

    /**
     * Returns if unrestricted vector extension is supported
     * @return if unrestricted vector extension is supported
     */
    public boolean isUnrestrictedVectorSupported();

    /**
     * Sets the unrestricted vector mode
     * @param newUnrestrictedVectorMode the requested unrestricted vector
     * mode
     * @return the actual unrestricted vector mode that was set
     */
    public boolean setUnrestrictedVector(boolean newUnrestrictedVectorMode);

    /**
     * Returns if unrestricted vector was enabled.
     * Decoders return the support for this mode.
     * @return if unrestricted vector was enabled
     */
    public boolean getUnrestrictedVector();


    /**
     * Returns if arithmeticc coding extension is supported
     * @return if arithmeticc coding extension is supported
     */
    public boolean isArithmeticCodingSupported();

    /**
     * Sets the arithmeticc coding mode
     * @param newArithmeticCodingMode the requested arithmeticc coding
     * mode
     * @return the actual arithmeticc coding mode that was set
     */
    public boolean setArithmeticCoding(boolean newArithmeticCodingMode);

    /**
     * Returns if arithmeticc coding was enabled.
     * Decoders return the support for this mode.
     * @return if arithmeticc coding was enabled
     */
    public boolean getArithmeticCoding();


    /**
     * Returns if advanced prediction extension is supported
     * @return if advanced prediction extension is supported
     */
    public boolean isAdvancedPredictionSupported();

    /**
     * Sets the advanced prediction mode
     * @param newAdvancedPredictionMode the requested advanced prediction
     * mode
     * @return the actual advanced prediction mode that was set
     */
    public boolean setAdvancedPrediction(boolean newAdvancedPredictionMode);

    /**
     * Returns if advanced prediction was enabled.
     * Decoders return the support for this mode.
     * @return if advanced prediction was enabled
     */
    public boolean getAdvancedPrediction();


    /**
     * Returns if PB Frames extension is supported
     * @return if PB Frames extension is supported
     */
    public boolean isPBFramesSupported();

    /**
     * Sets the PB Frames mode
     * @param newPBFramesMode the requested PB Frames
     * mode
     * @return the actual PB Frames mode that was set
     */
    public boolean setPBFrames(boolean newPBFramesMode);

    /**
     * Returns if PB Frames was enabled.
     * Decoders return the support for this mode.
     * @return if PB Frames was enabled
     */
    public boolean getPBFrames();


    /**
     * Returns if error compensation extension is supported
     * @return if error compensation extension is supported
     */
    public boolean isErrorCompensationSupported();

    /**
     * Sets the error compensation mode
     * @param newtErrorCompensationMode the requested error compensation
     * mode
     * @return the actual error compensation mode that was set
     */
    public boolean setErrorCompensation(boolean newtErrorCompensationMode);

    /**
     * Returns if error compensation was enabled.
     * Decoders return the support for this mode.
     * @return if error compensation was enabled
     */
    public boolean getErrorCompensation();

    /**
     *  Returns the refernce decoder parameter HRD_B
     *  @return the refernce decoder parameter HRD_B
     **/
    public int getHRD_B();

    /**
     *  Returns the refernce decoder parameter BppMaxKb
     *  @return the refernce decoder parameter BppMaxKb
     **/
    public int getBppMaxKb();


}
