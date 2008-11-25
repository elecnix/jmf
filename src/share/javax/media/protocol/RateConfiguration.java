/*
 * @(#)RateConfiguration.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;


/**
 * A configuration of streams for a particular rate.
 *
 * @see DataSource
 * @see RateConfigureable
 * @version 1.2, 02/08/21.
 */

public interface RateConfiguration {

    /**
     * Get the <CODE>RateRange</CODE> for this configuration.
     *
     * @return The rate supported by this configuration.
     */
    public RateRange getRate();
    
    /**
     * Get the streams that will have content at this rate.
     *
     * @return The streams supported at this rate.
     */
    public SourceStream[] getStreams();
}
    

    
