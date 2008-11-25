/*
 * @(#)Server.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.rtsp;


import java.net.*;
import java.io.*;
import java.util.*;


public class Server extends Thread {
    private RtspManager rtspManager;
    private ServerSocket serverSocket;

    public Server(RtspManager rtspManager) {
        this.rtspManager = rtspManager;

        try {
            serverSocket = new ServerSocket(RtspPort.getPort());

            System.err.println("Server Socket: " + serverSocket.toString());
            System.err.println("Socket is connected to: " +
                    serverSocket.getInetAddress().getLocalHost());
            System.err.println("Local port: " + serverSocket.getLocalPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        Debug.println("Server running...");

        if (serverSocket == null) {
            return;
        }

        // TransportRx transportRx;

        while (true) {
            try {
                Debug.println("accepting...");

                Socket socket = serverSocket.accept();

                rtspManager.addConnection(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        try {
            Debug.println("...closing server socket");

            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


