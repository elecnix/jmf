/*
 * @(#)SessionDescription.java	1.8 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.sdp;

import java.io.*;
import java.util.*;

public class SessionDescription extends Parser {
    public Vector timeDescriptions;
    public Vector sessionAttributes;
    public boolean connectionIncluded;

    // Values:
    public String version;
    public String origin;
    public String sessionName;
    public String sessionInfo;
    public String uri;
    public String email;
    public String phone;
    public String connectionInfo;
    public String bandwidthInfo;
    public String timezoneAdjustment;
    public String encryptionKey;


    public SessionDescription(ByteArrayInputStream bin) {
        connectionIncluded = false;

        // Protocol Version:
        version = getLine(bin);

        // Origin:
        if (getToken(bin, "o=", true)) {
            origin = getLine(bin);
            // System.out.println( "origin: " + origin);
        }

        // Session Name:
        if (getToken(bin, "s=", true)) {
            sessionName = getLine(bin);
            // System.out.println( "session name: " + sessionName);
        }

        // Session and Media Information:
        if (getToken(bin, "i=", false)) {
            sessionInfo = getLine(bin);
            // System.out.println( "session info: " + sessionInfo);
        }

        // URI:
        if (getToken(bin, "u=", false)) {
            uri = getLine(bin);
            // System.out.println( "uri: " + uri);
        }

        // E-Mail:
        if (getToken(bin, "e=", false)) {
            email = getLine(bin);
            // System.out.println( "email: " + email);
        }

        // Try a second E-Mail (Bug in PRISS protocol):
        if (getToken(bin, "e=", false)) {
            email = getLine(bin);
            // System.out.println( "email: " + email);
        }

        // phone number:
        if (getToken(bin, "p=", false)) {
            phone = getLine(bin);
            // System.out.println( "phone: " + phone);
        }

        // connection information:
        if (getToken(bin, "c=", false)) {
            connectionIncluded = true;

            connectionInfo = getLine(bin);

            // System.out.println( "connection info: " + connectionInfo);
        }

        // bandwidth information:
        if (getToken(bin, "b=", false)) {
            bandwidthInfo = getLine(bin);

            System.out.println("bandwidth info: " + bandwidthInfo);
        }

        // time description:
        timeDescriptions = new Vector();

        boolean found = getToken(bin, "t=", true);

        while (found) {
            TimeDescription timeDescription = new TimeDescription(bin);

            timeDescriptions.addElement(timeDescription);

            found = getToken(bin, "t=", false);
        }

        // time zone adjustments:
        if (getToken(bin, "z=", false)) {
            timezoneAdjustment = getLine(bin);
            // System.out.println( "timezone adjustment: " + timezoneAdjustment);
        }

        // encryption key:
        if (getToken(bin, "k=", false)) {
            encryptionKey = getLine(bin);
            // System.out.println( "encryption key: " + encryptionKey);
        }

        // session attributes:
        sessionAttributes = new Vector();

        found = getToken(bin, "a=", false);

        while (found) {
            String sessionAttribute = getLine(bin);
            // System.out.println( "session attribute: " + sessionAttribute);

            int index = sessionAttribute.indexOf(':');

            if (index > 0) {
                String name = sessionAttribute.substring(0, index);
                String value = sessionAttribute.substring(index + 1);

                MediaAttribute attribute = new MediaAttribute(name, value);

                sessionAttributes.addElement(attribute);
            }

            found = getToken(bin, "a=", false);
        }
    }

    public MediaAttribute getSessionAttribute(String name) {
        MediaAttribute attribute = null;

        if (sessionAttributes != null) {
            for (int i = 0; i < sessionAttributes.size(); i++) {
                MediaAttribute entry =
                        (MediaAttribute) sessionAttributes.elementAt(i);

                if (entry.getName().equals(name)) {
                    attribute = entry;
                    break;
                }
            }
        }

        return attribute;
    }
}

