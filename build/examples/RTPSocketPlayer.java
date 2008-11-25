// --- cut from here... --- //

import java.io.*;
import java.net.*;
import java.util.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.rtp.rtcp.*;

public class RTPSocketPlayer implements ControllerListener {
    // ENTER THE FOLLOWING SESSION PARAMETERS FOR YOUR RTP SESSION 
    
    // RTP Session address, multicast, unicast or broadcast address
    String address = "224.144.251.245";
    
    // RTP Session port
    int port = 49150;
    
    // Media Type i.e. one of audio or video
    String media = "audio";
    
    // DO NOT MODIFY ANYTHING BELOW THIS LINE  
         
    // The main rtpsocket abstraction which we will create and send
    // to the Manager for appropriate handler creation
    RTPSocket rtpsocket = null;
       
    // The control RTPPushDataSource of the above RTPSocket 
    RTPPushDataSource rtcpsource = null;
    
    // The GUI to handle the player
    // PlayerWindow playerWindow;
    
    // The handler created for the RTP session, 
    // as returned by the Manager
    Player player;

    // maximum size of buffer for UDP receive from the sockets
    private  int maxsize = 2000;


    UDPHandler rtp = null;
    UDPHandler rtcp = null;
      
    public RTPSocketPlayer() {
        // create the RTPSocket
        rtpsocket = new RTPSocket();
       
        // set its content type : 
        // rtpraw/video for a video session 
        // rtpraw/audio for an audio session
        String content = "rtpraw/" + media;
        rtpsocket.setContentType(content);
        
        // set the RTP Session address and port of the RTP data
        rtp = new UDPHandler(address, port);
        
        // set the above UDP Handler to be the 
        // sourcestream of the rtpsocket
        rtpsocket.setOutputStream(rtp);
        
        // set the RTP Session address and port of the RTCP data
        rtcp = new UDPHandler(address, port +1);
        
        // get a handle over the RTCP Datasource so that we can 
        // set the sourcestream and deststream of this source 
        // to the rtcp udp handler we created above.
        rtcpsource = rtpsocket.getControlChannel();
        
        // Since we intend to send RTCP packets from the         
        // network to the session manager and vice-versa, we need
        // to set the RTCP UDP handler as both the input and output 
        // stream of the rtcpsource.
        rtcpsource.setOutputStream(rtcp);
        rtcpsource.setInputStream(rtcp);
        
        // connect the RTP socket data source before 
        // creating the player
        try {
            rtpsocket.connect();
            player = Manager.createPlayer(rtpsocket);
            // rtpsocket.start();
        } catch (NoPlayerException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }

        if (player != null) {
            player.addControllerListener(this);
            // send this player to out playerwindow
            // playerWindow = new PlayerWindow(player);
        }
    }

    public synchronized void controllerUpdate(ControllerEvent ce) {
        if ((ce instanceof DeallocateEvent) ||
            (ce instanceof ControllerErrorEvent)) {
        
            // stop udp handlers
            if (rtp != null) rtp.close();
            
            if (rtcp != null) rtcp.close();
        }
    }
    
    // method used by inner class UDPHandler to open a datagram or
    // multicast socket as the case maybe
    
    private DatagramSocket InitSocket(String sockaddress, int sockport) {
        InetAddress addr = null;
        DatagramSocket sock = null;

        try {
            addr = InetAddress.getByName(sockaddress);
            
            if (addr.isMulticastAddress()) {
                MulticastSocket msock = new MulticastSocket(sockport);
                msock.joinGroup(addr);
                sock = (DatagramSocket)msock;           
            } 
            else {              
                sock = new DatagramSocket(sockport,addr);
            }
            
            return sock;
        }
        catch (SocketException e) {
            e.printStackTrace();
            return null;
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    

    // INNER CLASS UDP Handler which will receive UDP RTP Packets and
    // stream them to the handler of the sources stream. IN case of
    // RTCP, it will also accept RTCP packets and send them on the
    // underlying network.

    public class UDPHandler extends Thread implements PushSourceStream, 
                                                      OutputDataStream
    {
        DatagramSocket        mysock;
        DatagramPacket        dp;
        SourceTransferHandler outputHandler;
        String                myAddress;
        int                   myport;
        boolean               closed = false;


        // in the constructor we open the socket and create the main
        // UDPHandler thread.
        
        public UDPHandler(String haddress, int hport) {
            myAddress = haddress;
            myport = hport;
            mysock = InitSocket(myAddress,myport);                  
            setDaemon(true);
            start();
        }

        // the main thread receives RTP data packets from the
        // network and transfer's this data to the output handler of
        // this stream.
        
        public void run() {
            int len;

            while(true) {
                if (closed) {
                    cleanup();
                    return;
                }
                try {
                    do {
                        dp = new DatagramPacket( new byte[maxsize],
                                                 maxsize);
                        
                        mysock.receive(dp);

                        if (closed){
                            cleanup();
                            return;
                        }
                        
                        len = dp.getLength();

                        if (len > (maxsize >> 1)) maxsize = len << 1;
                    }
                    while (len >= dp.getData().length);
                }catch (Exception e){
                    cleanup();
                    return;
                }
                
                if (outputHandler != null) {
                    outputHandler.transferData(this);
                }
            }
        }

        public void close() {
            closed = true;
        }

        private void cleanup() {
            mysock.close();
            stop();
        }
        
        // methods of PushSourceStream
        public Object[] getControls() {
            return new Object[0];
        }
        
        public Object getControl(String controlName) {
            return null;
        }

        public ContentDescriptor getContentDescriptor() {
            return null;
        }

        public long getContentLength() {
            return SourceStream.LENGTH_UNKNOWN;
        }

        public boolean endOfStream() {
            return false;
        }

        // method by which data is transferred from the underlying
        // network to the session manager.
        
        public int read(byte buffer[],
                        int offset,
                        int length) 
        {
            System.arraycopy(dp.getData(),
                             0,
                             buffer,
                             offset,
                             dp.getLength());
            
            return dp.getData().length;
        }                
        
        public int getMinimumTransferSize(){
            return dp.getLength();
        }
        
        public void setTransferHandler(SourceTransferHandler
                                       transferHandler)
        {
            this.outputHandler = transferHandler;
        }
        
        // methods of OutputDataStream used by the session manager to 
        // transfer data to the underlying network.
        
        public int write(byte[] buffer,
                         int offset,
                         int length)
        {
            InetAddress addr = null;
        
            try {
                addr = InetAddress.getByName(myAddress);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            DatagramPacket dp = new DatagramPacket( buffer, 
                                                    length,
                                                    addr,
                                                    myport);
            try {
                mysock.send(dp);
            } catch (IOException e){
                e.printStackTrace();
            }
            
            return dp.getLength();
        }
    }

    public static void main(String[] args) {
        new RTPSocketPlayer();
    }
}

// --- ...until here --- //

