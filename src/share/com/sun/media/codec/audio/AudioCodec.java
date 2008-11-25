/*
 * @(#)AudioCodec.java	1.4 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.codec.audio;

import javax.media.format.*;
import javax.media.format.*;
import javax.media.*;
import com.sun.media.*;

public abstract class AudioCodec extends BasicCodec {

    public Format setInputFormat(Format format) {
      if (matches(format, inputFormats) == null)
          return null;
      inputFormat = format;
      return format;
    }


    public Format setOutputFormat(Format format) {
      if (matches(format, getSupportedOutputFormats(inputFormat)) == null)
          return null;
      if (!(format instanceof AudioFormat))
          return null;
      outputFormat = (AudioFormat)format;
      return format;
    }


    /**
     * Checks the header of the compressed audio packet and detects any format
     * changes. Does not modify the buffer in any way.
     * TBD: how to select spesific output format
     */
    public boolean checkFormat(Format format) {
       if (inputFormat == null  ||
           outputFormat == null ||
           format != inputFormat ||
           !format.equals(inputFormat) ) {
              inputFormat = format;
              Format fs[] = getSupportedOutputFormats(format);
              outputFormat = (AudioFormat)fs[0];
       }
       return outputFormat != null;
    }


}
