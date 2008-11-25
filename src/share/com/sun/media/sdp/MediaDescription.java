/*
 * @(#)MediaDescription.java	1.9 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.sdp;

import java.io.*;
import java.util.*;

public class MediaDescription extends Parser {
    // Values:
    public String name;
    public String port;
    public String protocol;
    public int payload_type;
    public String payload;
    public String mediaTitle;
    public String connectionInfo;
    public String bandwidthInfo;
    public String encryptionKey;
    public Vector mediaAttributes;


    public MediaDescription(ByteArrayInputStream bin, boolean connectionIncluded) {
        // Media Name and Transport Address:
        String line = getLine(bin);

        int end = line.indexOf(' ');

        name = line.substring(0, end);


        int start = end + 1;

        end = line.indexOf(' ', start);

        port = line.substring(start, end);


        start = end + 1;

        end = line.indexOf(' ', start);

        protocol = line.substring(start, end);

        start = end + 1;

        payload = line.substring(start);

        try {
            payload_type = new Integer(payload).intValue();
        } catch (Exception e) {
            payload_type = -1;
        }


        // Session and Media Information:
        if (getToken(bin, "i=", false)) {
            mediaTitle = getLine(bin);

            System.out.println("media title: " + mediaTitle);
        }

        // Connection Information:
        boolean mandatory = true;

        if (connectionIncluded) {
            mandatory = false;
        }

        if (getToken(bin, "c=", mandatory)) {
            connectionInfo = getLine(bin);

            System.out.println("connection info: " + connectionInfo);
        }

        // Bandwidth Information:
        if (getToken(bin, "b=", false)) {
            bandwidthInfo = getLine(bin);

            System.out.println("bandwidth info: " + bandwidthInfo);
        }

        // Encryption Key:
        if (getToken(bin, "k=", false)) {
            encryptionKey = getLine(bin);

            System.out.println("encryption key: " + encryptionKey);
        }

        // Media Attributes:
        mediaAttributes = new Vector();

        boolean found = getToken(bin, "a=", false);

        while (found) {
            String mediaAttribute = getLine(bin);

            int index = mediaAttribute.indexOf(':');

            if (index > 0) {
                String name = mediaAttribute.substring(0, index);
                String value = mediaAttribute.substring(index + 1);

                MediaAttribute attribute = new MediaAttribute(name, value);

                mediaAttributes.addElement(attribute);
            }

            found = getToken(bin, "a=", false);
        }
    }

    public MediaAttribute getMediaAttribute(String name) {
        MediaAttribute attribute = null;

        if (mediaAttributes != null) {
            for (int i = 0; i < mediaAttributes.size(); i++) {
                MediaAttribute entry = (MediaAttribute) mediaAttributes.elementAt(i);

                if (entry.getName().equals(name)) {
                    attribute = entry;
                    break;
                }
            }
        }

        return attribute;
    }
}


