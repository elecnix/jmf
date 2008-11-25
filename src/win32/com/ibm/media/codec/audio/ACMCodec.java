/*
 * @(#)ACMCodec.java	1.18 99/07/16
 */

package com.ibm.media.codec.audio;

import java.util.*;
import java.io.*;
import javax.media.*;
import javax.media.format.*;
import javax.media.format.*;

import com.sun.media.format.*;
import com.sun.media.util.*;

public class ACMCodec implements Codec, DynamicPlugIn {

  /**
   * Holds the supported input formats
   */
  private Vector supportedInputFormats = null; 

  /** 
   * Holds the supported output formats
   */
  private Vector supportedOutputFormats = null;

  /**
   * The actual input formats array to be returned 
   */
  private AudioFormat[] inputFormats = null;

  /**
   * The actual output formats array to be returned
   */
  private AudioFormat[] outputFormats = null;

  /**
   * The input format for the codec
   */
  private AudioFormat inputFormat = null;

  /**
   * The output format for the codec
   */
  private AudioFormat outputFormat = null;

  /**
   * This hold the native handle for this instance's ACM codec
   */
  private long nativeHandle;

  static {
    com.sun.media.JMFSecurityManager.loadLibrary("jmacm");
  }

  /**
   * Obtain the collection of objects that
   * control the object that implements this interface.
   * <p>
   *
   * If no controls are supported, a zero length
   * array is returned.
   *
   * @return the collection of object controls
   */
  public Object[] getControls() {
    // no controls implemented
    return new Control[0];
  }
  
  /**
   * Obtain the object that implements the specified
   * <code>Class</code> or <code>Interface</code>
   * The full class or interface name must be used.
   * <p>
   * 
   * If the control is not supported then <code>null</code>
   * is returned.
   *
   * @return the object that implements the control,
   * or <code>null</code>.
   */
  public Object getControl(String controlType) {
    // no controls implemented
    return null;
  }
  
  /**
   * Returns a descriptive name for the plug-in.
   * This is a user readable string.
   */
  public String getName() {
    return "ACM Wrapper Codec";
  }
  
  /**
   * Opens the plug-in software or hardware component and acquires
   * necessary resources. If all the needed resources could not be
   * acquired, it throws a ResourceUnavailableException. Buffer should not
   * be passed into the plug-in without first calling this method.
   */
  public void open() throws ResourceUnavailableException {

    nativeHandle = openACMStream(inputFormat, outputFormat);
    if (nativeHandle == 0)
      throw new ResourceUnavailableException("ACM stream coun't been opened");
  }

  /**
   * Opens an ACM stream to be used for transcoding
   *
   * @param inputFormat  the input format for this codec
   * @param outputFormat  the ourpur format for this codec
   * @return the native handle of the ACM codec or 0 if codec couldn't be open
   */
  private native long openACMStream(AudioFormat inputFormat, 
				    AudioFormat outputFormat);
  
  /**
   * Closes the plug-in component and releases resources. No more data
   * will be accepted by the plug-in after a call to this method. The
   * plug-in can be reinstated after being closed by calling
   * <code>open</code>.
   */
  public void close() {

    closeACMStream(nativeHandle);
  }
  
  /**
   * Closes the ACM stream used for transcoding.
   *
   * @param nativeHandle  the native handle of the ACM codec
   */
  private native void closeACMStream(long nativeHandle);

  /**
   * Resets the state of the plug-in. Typically at end of media or when media
   * is repositioned.
   */
  public void reset() {
    resetACMStream(nativeHandle);
  }
  
  /**
   * Resets the ACM stream.
   *
   * @param nativeHandle  the native handle of the ACM codec
   */
  private native void resetACMStream(long nativeHandle);

  /**
   * Lists the possible input formats supported by this plug-in.
   */
  public Format[] getSupportedInputFormats() {
    if (inputFormats != null) 
      return inputFormats;
    else {
      supportedInputFormats = new Vector();
      fillSupportedInputFormats();
      int size = supportedInputFormats.size();
      inputFormats = new AudioFormat[size];
      for (int index = 0; index < size; index++) {
	inputFormats[index] = (AudioFormat)supportedInputFormats.elementAt(index);
      }
      return inputFormats;
    }
  }
  
  /**
   * Fills the supportedInputFormat Vector with the ACM supported input 
   * formats.
   */
  private native void fillSupportedInputFormats();
  
  /**
   * Lists the possible output formats of the processed data.
   * If <code>input</code> is non-null, then it lists the possible
   * output formats given that the input buffer is of the format specified
   * by <code>input</code>. If <code>input</code> is null, then it lists
   * all possible output formats that this plug-in advertises.
   */
  public Format[] getSupportedOutputFormats(Format input) {

    if (input == null) {
      outputFormats= new AudioFormat[1];
      outputFormats[0] = new AudioFormat(null);
      return outputFormats;
    }
    if (!(input instanceof AudioFormat)) {
      outputFormats = new AudioFormat[0];
      return outputFormats;
    }
    else {
      /* fillSuppotedFormats(input) is called since each call to 
	 getSupportedOutputFoprmats(input) can have a different 
	 parameter and we wouldn't like to hold all supported formats
	 for all input possibilites ahead (will be a huge overhead */
      
      supportedOutputFormats = new Vector(); 
      fillSupportedOutputFormats((AudioFormat)input);
      int size = supportedOutputFormats.size();
      outputFormats = new AudioFormat[size];

      for (int index = 0; index < size; index++) 
	outputFormats[index] = (AudioFormat)supportedOutputFormats.elementAt(index);

      return outputFormats;
    }
  }
  
  /**
   * Fills the supportedOutputFormat Vector with the ACM supported output 
   * formats.
   */
  private native void fillSupportedOutputFormats(AudioFormat input);
  
  /**
   * Set the buffer input format.
   *
   * @return false if the format is not supported.
   */
  public Format setInputFormat(Format format) {

      if (!(format instanceof AudioFormat)) {
	  return null;
      }
      inputFormat = (AudioFormat)format;

      return format;
  }

  /**
   * Set the buffer output format.
   *
   * @return false if the format is not supported.
   */
  public Format setOutputFormat(Format format) {

    if (!(format instanceof AudioFormat)) 
      return null;
    outputFormat = (AudioFormat)format;
    
    return format;
  }
  
  /** 
   * Process the media 
   *
   * @return BUFFER_PROCESSED_OK if the processing is successful.  Other
   * possible return codes are defined in PlugIn. 
   * @see PlugIn
   */
  public int process(Buffer input, Buffer output) {

    // shouldn't it be done in the module ?
    if (input.isEOM()) {
      output.setLength(0) ;
      output.setEOM(true);
      return BUFFER_PROCESSED_OK;
    }

    int oldInputOffset = input.getOffset();
    // preper destination data
    int destLength = getDestinationBufferSize(nativeHandle, input.getLength());
    if (output.getData() == null || 
	destLength > ((byte[])output.getData()).length) {
      byte[] destination = new byte[destLength];
      output.setData(destination);
    }
    output.setLength(destLength);
    output.setOffset(0);
    output.setFormat(outputFormat);

    if (!ACMProcess(nativeHandle, (byte[])input.getData(), input.getOffset(), 
		    input.getLength(), input, (byte[])output.getData(), 
		    output.getLength(), output)) 
      return BUFFER_PROCESSED_FAILED;

    if (oldInputOffset != input.getOffset()) 
      return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
    else 
      return BUFFER_PROCESSED_OK;
  }
  
  /**
   * Gets the estimated size of the destination data.
   *
   * @param nativeHandle  the native handle of the ACM codec
   * @param inputSize  the size of the input data
   *
   * @return the size of the destination data
   */
  private native int getDestinationBufferSize(long nativeHandle, int input);

  /**
   * Does the ACM codec processing 
   *
   * @param nativeHandle  the native handle of the ACM codec
   * @param input  the input data
   * @param inputLength  the length of the input data
   * @param inputBuffer  the input Buffer object, passed for offset update
   * @param output  the output data
   * @param outputLength  the length of the output data
   * @param outputBuffer  the output Buffer object, passed for length update
   *
   * @return false if an error has occur
   */
  private native boolean ACMProcess(long nativeHandle, byte[] input, 
				    int inputOffset, int inputLength, 
				    Buffer inputBuffer, byte[] output, 
				    int outputLength, Buffer outputBuffer);

  /**
   * An array of format objects that cover the generic input formats
   * that this plugin supports. For example, a VideoRenderer may not
   * know the exact RGBFormat it supports, so it returns a dummy RGBFormat
   * with mostly unspecified values.
   */
  public Format[] getBaseInputFormats() {
    
    Format[] formats = new Format[1];
    formats[0] = new AudioFormat(null);

    return formats;
  }
  
  /**
   * An array of format objects that cover the generic output formats
   * that this plugin supports. For example, a Codec may not
   * know the exact output RGBFormat it supports, so it returns a dummy
   * RGBFormat with mostly unspecified values.
   */
  public Format[] getBaseOutputFormats() {
    
    return getBaseInputFormats();
  }
}








