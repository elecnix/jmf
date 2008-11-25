/**
 * @(#)BufferControl.java	1.5 02/08/21
 *
 * The BufferControl is used to exercise user level control over the
 * buffering done by a particular object. The object exporting this control
 * is responsible for buffering data, and users may change the length of
 * the buffer and the minimum threshold of the buffer based on their needs.
 * By default, the buffer length and minimum threshold will be set to certain
 * default values.  These default values can be retrieved and changed by the
 * user if desired. 
 * A buffer length is always set by this object. A minimum threshold for 
 * jitter buffering may or may not be set depending on the format. .e.g. 
 * in case of audio, a non-zero minimum threshold will be set (jitter 
 * buffering will be done), while in case of video, a zero minimum threshold
 * will be set (no jitter buffering will be done).
 * Each buffer control should have its own default and maximum buffer
 * lengths, which are set using MAX_VALUE and DEFAULT_VALUE. 
 */

package javax.media.control;

import javax.media.*;

public abstract interface BufferControl extends Control{
  
  static final long DEFAULT_VALUE = -1;
  static final long MAX_VALUE = -2;
  
  /**
   * Retrieves the buffer length set on the object exporting this control.
   * The buffer length is in millisecs. The actual size of the buffer
   * will be calculated depending on the format of the stream.
   * @return the buffer length in millisecs.
   */
  public long getBufferLength();

  /**
   * Sets the buffer length of the buffer maintained by this
   * object in milliseconds. Returns the actual buffer length set. A value of
   * MAX_VALUE indicates that the buffer length should be set to a large
   * enough value so as not to drop any packets. i.e. the limit is only
   * to prevent abnormal usage of memory, but would indicate buffering
   * of all data is to be done and no packet dropping is to be implemented.
   * DEFAULT_VALUE indicates that the buffer length should be restored to its
   * default value.
   * @return the actual buffer length set on this control
   */
  public long setBufferLength(long time);
  
  /**
   * Retrieve the minimum threshold value in milliseconds of the buffer 
   * maintained by this control. The minimum threshold is used to siginify
   * the minimum amount of data that is to be buffered by the control before pushing 
   * data out or allowing data to be pulled out (jitter buffer). Data will
   * only be available from this object when this minimum threshold has been
   * reached. In case the amount of data in the buffer reduces below this 
   * value, data will again be buffered until this minimum threshold
   * is reached.
   * @return The minimum threshold set on this control
   */
  public long getMinimumThreshold();

  /**
   * Sets the minimum threshold value in milliseconds for the buffer
   * maintained by this control.  Returns the actual minimum threshold 
   * value set.A value of MAX_VALUE indicates the minimum threshold
   * should be set to the maximum value as maintained by this cotrol. A
   * value of DEFAULT_VALUE indicates that the threshold should be
   * restored to its original length. 
   * @return the actual minimum threshold set on this control
   */ 
  public long setMinimumThreshold(long time);
  
  /**
   * If false, the set minimum threshold value will not be used by this control
   * in any of its buffer calculations. If true, data is not available
   * until the threshold condition is satisifed. 
   * The default is that the threshold is enabled.
   */
  public void setEnabledThreshold(boolean b);
  
  /**
   * Returns true if threshold calculations are enabled, false otherwise
   * @return true if threshold is enabled, false otherwise
   */
  public boolean getEnabledThreshold();
}



