/*
 * @(#)SdpParser.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.sdp;

import java.io.*;
import java.util.*;

public class SdpParser extends Parser {
    public SessionDescription sessionDescription;
    public Vector mediaDescriptions;

    public SdpParser(byte data[]) {
        init();

        ByteArrayInputStream bin = new ByteArrayInputStream(data);

        parseData(bin);
    }

    public void parseData(ByteArrayInputStream bin) {
        if (getToken(bin, "v=", true)) {
            sessionDescription = new SessionDescription(bin);

            mediaDescriptions = new Vector();

            boolean found = getToken(bin, "m=", false);

            while (found) {
                MediaDescription mediaDescription = new MediaDescription(bin,
                        sessionDescription .connectionIncluded);

                mediaDescriptions.addElement(mediaDescription);

                found = getToken(bin, "m=", false);
            }
        }
    }

    public MediaAttribute getSessionAttribute(String name) {
        MediaAttribute attribute = null;

        if (sessionDescription != null) {
            attribute = sessionDescription.getSessionAttribute(name);
        }

        return attribute;
    }

    public MediaDescription getMediaDescription(String name) {
        MediaDescription description = null;

        if (mediaDescriptions != null) {
            for (int i = 0; i < mediaDescriptions.size(); i++) {
                MediaDescription entry =
                        (MediaDescription) mediaDescriptions.elementAt(i);

                if (entry.name.equals(name)) {
                    description = entry;
                    break;
                }
            }
        }

        return description;
    }

    public Vector getMediaDescriptions() {
	return mediaDescriptions;
    }

    static String input =
            "v=0\r\n" + "o=mhandley 2890844526 2890842807 IN IP4 126.16.64.4\r\n" +
            "s=SDP Seminar\r\n" +
            "i=A Seminar on the session description protocol\r\n" +
            "u=http://www.cs.ucl.ac.uk/staff/M.Handley/sdp.03.ps\r\n" +
            "e=mjb@isi.edu (Mark Handley)\r\n" +
            "c=IN IP4 224.2.17.12/127\r\n" + "t=2873397496 2873404696\r\n" +
            "a=recvonly\r\n" + "m=audio 49170 RTP/AVP 0\r\n" +
            "m=video 51372 RTP/AVP 31\r\n" + "m=application 32416 udp wbr\n" + "a=orient:portrait\r\n";

    public static void main(String[] args) {
        new SdpParser(input.getBytes());
    }
}
