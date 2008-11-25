/*
 * @(#)Many2one.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.codec.audio;

import javax.media.format.*;
import javax.media.format.*;

import com.sun.media.*;
import javax.media.*;
/**
 * SimpleCodec is a codec that accepts a chunk of data of any size and generate output data,
 * It buffers samples internally if necessary.
 **/
public class Many2one extends  BasicCodec  {

    public String getName() {
	return "many frames to one converter";
    }

    int counter=0;
    boolean flagEOM=false;
    Format   af=(new AudioFormat(AudioFormat.LINEAR,
				 8000,
				 16,
				 1,
				 AudioFormat.LITTLE_ENDIAN,
				 AudioFormat.SIGNED,
				 Format.NOT_SPECIFIED,
				 Format.NOT_SPECIFIED,
				 (Byte.class)));

    public Format [] getSupportedInputFormats() {
	Format fmt [] = new Format[1];
	fmt[0] = af;
	return fmt;
    }

    public Format [] getSupportedOutputFormats(Format in) {
	Format fmt [] = new Format[1];
	fmt[0] = af;
	return fmt;
    }

    /** process the media from the input buffer to the output buffer **/
    public int process(Buffer inputBuffer, Buffer outputBuffer){


	// System.out.println("in  " + inputBuffer.getLength() );
	// System.out.println("out " + outputBuffer.getLength() );

	if (flagEOM) {
	    outputBuffer.setLength(0);
	    outputBuffer.setEOM(true);

	    flagEOM=false;
	    return BUFFER_PROCESSED_OK;
	}

	if (outputBuffer.isDiscard()) {
	    outputBuffer.setLength(0);
	    outputBuffer.setOffset(0);
	}


	if (inputBuffer.isEOM()) {
	    // this is a little tricky since we have to output two frames now:
	    // one to close former session, another to signle EOM
	    if (outputBuffer.getLength() > 0 ) {
		flagEOM=true;
		return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED ;
	    } else {
		// in case we have nothing in the output, we are done
		outputBuffer.setLength(0);
		outputBuffer.setEOM(true);
		return BUFFER_PROCESSED_OK;
	    }
	}




	if (outputBuffer.getData()==null)
	    outputBuffer.setData(new byte[10000]);


	System.arraycopy(inputBuffer.getData(),inputBuffer.getOffset(),
			 outputBuffer.getData(),outputBuffer.getLength(),
			 inputBuffer.getLength() );

	outputBuffer.setLength(outputBuffer.getLength() + inputBuffer.getLength() );


	if ( (++counter) == 5 ) {
	    counter=0;
	    outputBuffer.setFormat(af );
	    return BUFFER_PROCESSED_OK;

	}


	return BUFFER_PROCESSED_OK | OUTPUT_BUFFER_NOT_FILLED ;
    }

}
