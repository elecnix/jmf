/*
 * @(#)ExtendedCachingControl.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * @since JMF 2.0
 */
public interface ExtendedCachingControl extends CachingControl {
    /**
     *  Block until you have buffered up t.getSeconds() seconds
     * of data. Can be tricky to implement for non-interleaved
     * quicktime or avi files with all the audio in the beginning and all the
     * video at the end
     */
    void setBufferSize(Time t);

    Time getBufferSize();

    /**
     * If downloading is in progress, it will be paused
     */
    void pauseDownload();

    /**
     * If downloading was paused, it will be resumed
     */
    public void resumeDownload();

    public long getStartOffset();
 
    public long getEndOffset();

    void addDownloadProgressListener(DownloadProgressListener l, int numKiloBytes);

    void removeDownloadProgressListener(DownloadProgressListener l);

}

