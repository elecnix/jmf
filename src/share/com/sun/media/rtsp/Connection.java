/*
 * @(#)Connection.java	1.9 99/06/21
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.media.rtsp;

import java.io.*;
import java.net.*;

public class Connection extends Thread implements Runnable {
    public int connectionId;
    private Socket socket;
    private RtspManager rtspManager;
    private MessageProcessor mp;
    private boolean connectionIsAlive;

    public Connection(RtspManager rtspManager, int connectionId,
            byte dstAddress[], int port) throws UnknownHostException,
    ConnectException {
        this.rtspManager = rtspManager;
        this.connectionId = connectionId;

        String domain = new String(dstAddress);

        InetAddress dst = InetAddress.getByName(domain);

        try {
            // System.err.println( "dst: " + dst);

            socket = new Socket(dst, port);

            // System.err.println( "TI Socket: " + socket.toString());

            start();
        } catch (IOException e) {
            throw new ConnectException();
        }
    }

    public Connection(RtspManager rtspManager, int connectionId, Socket socket) {
        this.rtspManager = rtspManager;
        this.connectionId = connectionId;
        this.socket = socket;

        start();
    }

    public boolean sendData(byte message[]) {
        boolean success = false;

        try {
            OutputStream out = socket.getOutputStream();

            out.write(message);

            out.flush();

            success = true;
        } catch (IOException e) {
            // e.printStackTrace();
        }

        return success;
    }

    public void run() {
        // System.out.println( "Connection-" + connectionId + " running...");

        connectionIsAlive = true;

        while (connectionIsAlive) {
            try {
                InputStream in = socket.getInputStream();

                DataInputStream din = new DataInputStream(in);

                byte ch = din.readByte();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                // read message header:

                baos.write(ch);

                while (!eomReached(baos.toByteArray())) {
                    baos.write(din.readByte());
                }

                // read message body:

                int length = getContentLength(new String(baos.toByteArray()));

                for (int i = 0; i < length; i++) {
                    baos.write(din.readByte());
                }

                if (mp == null) {
                    mp = new MessageProcessor(connectionId, rtspManager);
                }

                mp.processMessage(baos.toByteArray());
            }
            catch (Exception e) {
                // System.out.println( "RTSP Connection terminated");
		
		connectionIsAlive = false;
            }
        }
    }

    private boolean eomReached(byte buffer[]) {
        boolean endReached = false;

        int size = buffer.length;

        if (size >= 4) {
            if (buffer[size - 4] == '\r' && buffer[size - 3] == '\n' &&
                    buffer[size - 2] == '\r' && buffer[size - 1] == '\n') {
                endReached = true;
            }
        }

        return endReached;
    }

    private int getContentLength(String msg_header) {
        int length;

        int start = msg_header.indexOf("Content-length");

        if (start == -1) {
            // fix for QTSS:
            start = msg_header.indexOf("Content-Length");
        }

        if (start == -1) {
            length = 0;
        } else {
            start = msg_header.indexOf(':', start) + 2;

            int end = msg_header.indexOf('\r', start);

            String length_str = msg_header.substring(start, end);

            length = new Integer(length_str).intValue();
        }

        return length;
    }

    public void cleanup() {
        Debug.println("RTSP::Connection:cleanup, id=" + connectionId);

        close();

        rtspManager.removeConnection(connectionId);
    }

    public void close() {
        connectionIsAlive = false;

        try {
            if (socket != null) {
                // System.out.println( "close socket");

                socket.close();

                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getIpAddress() {
        return socket.getInetAddress().getHostAddress();
    }
}



