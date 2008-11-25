/*
 * @(#)LocalParticipant.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;

import java.util.Vector;
import javax.media.rtp.rtcp.*;

/**
 * Interface representing the local participant.  See Participant
 * for details. 
 *
 */
public interface LocalParticipant extends Participant
{
  /**
   * Changes the source description (SDES) information sent out in
   * RTCP packets  for the local participant; This method effectively
   * changes the SDES information provided  by the user in
   * RTPSM.initSession(); see that method for more information. 
   * <P> 
   *
   * @param sourceDesc The new source description data. <P>
   */
    public void 
    setSourceDescription( SourceDescription[] sourceDesc);
}
