/*
 * @(#)SourceCloneable.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media.protocol;

/**
 * An interface that a DataSource should implement if it needs to be
 * cloneable. To create
 * a cloneable DataSource, use <code>Manager.createCloneableDataSource</code>.
 * @see DataSource
 * @see PushDataSource
 * @see PullDataSource
 * @see PushBufferDataSource
 * @see PullBufferDataSource
 * @see javax.media.Manager#createCloneableDataSource
 * @since JMF 2.0
 */
public interface SourceCloneable {

    /** 
     * Create a clone of the original <code>DataSource</code> that 
     * provides a copy of the same data streams.  The clones generated 
     * may or may not have the same properties of the original DataSource 
     * depending on the implementation.  Therefore, they should be 
     * checked against the properties required for the application.  
     * <br>
     * For example, the original <code>DataSource</code> may be a "pull" 
     * <code>DataSource</code> (<code>PullDataSource</code> or 
     * <code>PullBufferDataSource</code>).  But the resulted clone may be the 
     * equivalent "push" <code>DataSource</code>.  In that case, the 
     * resulting "push" <code>DataSource</code> will push data at the 
     * same rate at which the original <code>DataSource</code> is being 
     * pulled.
     *
     * @return a clone of the DataSource, or null if a clone could not be
     * created.
     */
    public DataSource createClone();
}
