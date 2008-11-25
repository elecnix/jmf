/*
 * @(#)RateConfigureable.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

/**
 * <code>DataSources</code> support the
 * <CODE>RateConfigureable</CODE> interface if they use
 * different rate-configurations to support multiple
 * media display speeds.
 *
 * @see DataSource
 * @see RateConfiguration
 * @see RateRange
 * @version 1.2, 02/08/21.
 */

public interface RateConfigureable {

    /**
     * Get the rate configurations that this object supports.
     * There must always be one and only one for
     * a <CODE>RateConfiguration</CODE> that covers a rate of 1.0.
     *
     * @return The collection of <CODE>RateConfigurations</CODE> that this
     * source supports.
     */
    public RateConfiguration[] getRateConfigurations();

    /**
     * Set a new <CODE>RateConfiguration</CODE>.
     * The new configuration should have been obtained by calling
     * <CODE>getRateConfigurations</CODE>.
     * Returns the actual <CODE>RateConfiguration</CODE> used.
     *
     * @param config The <CODE>RateConfiguration</CODE> to use.
     * @return The actual <CODE>RateConfiguration</CODE> used by the source.
     */
    public RateConfiguration setRateConfiguration(RateConfiguration config);

}
