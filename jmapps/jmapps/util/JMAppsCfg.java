/*
 * @(#)JMAppsCfg.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.util;

import java.util.*;
import java.io.*;
import java.awt.*;

import javax.media.*;

import com.sun.media.util.JMFI18N;

import jmapps.ui.*;


public class JMAppsCfg {

    public static final String  KEY_OPEN_FILE = "Last Open File";
    public static final String  KEY_OPEN_RTP = "Last Open RTP";
    public static final String  KEY_OPEN_URL = "Last Open URL";
    public static final String  KEY_CAPTURE_AUDIO = "Last Capture Audio Device";
    public static final String  KEY_CAPTURE_VIDEO = "Last Capture Video Device";
    public static final String  KEY_TRANSMIT_RTP = "Last Transmit RTP";
    public static final String  KEY_TRANSMIT_SOURCE = "Last Transmit Source";
    public static final String  KEY_SAVE_FILE_CONTENT = "Last Save File Content Type";
    public static final String  KEY_SAVE_FILE_TRACKS = "Last Save File Tracks";
    public static final String  KEY_SAVE_FILE_DIR = "Last Save File Directory";
    public static final String  KEY_RECENT_URL = "Recent URL";
    public static final String  KEY_JMSTUDIO_FRAME_POS = "Location of JMStudio Frame";
    public static final String  KEY_AUTO_PLAY = "Auto Play";
    public static final String  KEY_AUTO_LOOP = "Auto Loop";
    public static final String  KEY_KEEP_ASPECT = "Keep Aspect Ratio";


    private static String               nameFileCfg = ".JMAppsCfg";
    private static final String         signatureFileCfg = "JMStudio configuration data file.";
    private static final String         signatureHashtable = "Embedded Hashtable";

    private Hashtable   hashProperties = new Hashtable();


    public JMAppsCfg () {
        try {
            init();
        }
        catch ( Exception exception ) {
            exception.printStackTrace ();
        }
    }

    public void save () {
        saveFile ();
    }

    public String getLastOpenFile () {
        Object  objValue;
        String  strFile = null;

        objValue = hashProperties.get ( KEY_OPEN_FILE );
        if ( objValue != null )
            strFile = objValue.toString ();
        return ( strFile );
    }

    public void setLastOpenFile ( String strFile ) {
        hashProperties.put ( KEY_OPEN_FILE, strFile );
    }

    public RtpData getLastOpenRtpData () {
        Object          objValue;
        RtpData         dataRtp = null;

        objValue = hashProperties.get ( KEY_OPEN_RTP );
        if ( objValue != null  &&  objValue instanceof RtpData )
            dataRtp =(RtpData)objValue;
        return ( dataRtp );
    }

    public void setLastOpenRtpData ( RtpData dataRtp ) {
        hashProperties.put ( KEY_OPEN_RTP, dataRtp );
    }

    public String getLastTransmitRtpSource () {
        Object  objValue;
        String  strValue = null;

        objValue = hashProperties.get ( KEY_TRANSMIT_SOURCE );
        if ( objValue != null )
            strValue = objValue.toString ();
        return ( strValue );
    }

    public void setLastTransmitRtpSource ( String strValue ) {
        hashProperties.put ( KEY_TRANSMIT_SOURCE, strValue );
    }

    public RtpData getLastTransmitRtpData ( String strTrack ) {
        Object          objValue;
        RtpData         dataRtp = null;
        Hashtable       hashTransmitRtp;

        objValue = hashProperties.get ( KEY_TRANSMIT_RTP );
        if ( objValue != null  &&  objValue instanceof Hashtable ) {
            hashTransmitRtp = (Hashtable) objValue;
        }
        else {
            hashTransmitRtp = new Hashtable ();
            hashProperties.put ( KEY_TRANSMIT_RTP, hashTransmitRtp );
        }

        objValue = hashTransmitRtp.get ( strTrack );
        if ( objValue != null  &&  objValue instanceof RtpData )
            dataRtp =(RtpData)objValue;
        return ( dataRtp );
    }

    public void setLastTransmitRtpData ( RtpData dataRtp, String strTrack ) {
        Object          objValue;
        Hashtable       hashTransmitRtp;

        objValue = hashProperties.get ( KEY_TRANSMIT_RTP );
        if ( objValue != null  &&  objValue instanceof Hashtable ) {
            hashTransmitRtp = (Hashtable) objValue;
        }
        else {
            hashTransmitRtp = new Hashtable ();
            hashProperties.put ( KEY_TRANSMIT_RTP, hashTransmitRtp );
        }
        hashTransmitRtp.put ( strTrack, dataRtp );
    }

    public String getLastOpenUrl () {
        Object  objValue;
        String  strValue = null;

        objValue = hashProperties.get ( KEY_OPEN_URL );
        if ( objValue != null )
            strValue = objValue.toString ();
        return ( strValue );
    }

    public void setLastOpenUrl ( String strValue ) {
        hashProperties.put ( KEY_OPEN_URL, strValue );
    }

    public CaptureDeviceData getLastCaptureAudioData () {
        Object                  objValue;
        CaptureDeviceData       dataCapture = null;

        objValue = hashProperties.get ( KEY_CAPTURE_AUDIO );
        if ( objValue != null  &&  objValue instanceof CaptureDeviceData )
            dataCapture =(CaptureDeviceData)objValue;
        return ( dataCapture );
    }

    public void setLastCaptureAudioData ( CaptureDeviceData dataCapture ) {
        hashProperties.put ( KEY_CAPTURE_AUDIO, dataCapture );
    }

    public CaptureDeviceData getLastCaptureVideoData () {
        Object                  objValue;
        CaptureDeviceData       dataCapture = null;

        objValue = hashProperties.get ( KEY_CAPTURE_VIDEO );
        if ( objValue != null  &&  objValue instanceof CaptureDeviceData )
            dataCapture =(CaptureDeviceData)objValue;
        return ( dataCapture );
    }

    public void setLastCaptureVideoData ( CaptureDeviceData dataCapture ) {
        hashProperties.put ( KEY_CAPTURE_VIDEO, dataCapture );
    }

    public String getLastSaveFileContentType () {
        Object  objValue;
        String  strValue = null;

        objValue = hashProperties.get ( KEY_SAVE_FILE_CONTENT );
        if ( objValue != null )
            strValue = objValue.toString ();
        return ( strValue );
    }

    public void setLastSaveFileContentType ( String strValue ) {
        hashProperties.put ( KEY_SAVE_FILE_CONTENT, strValue );
    }

    public TrackData getLastSaveFileTrackData ( String strTrack ) {
        Object          objValue;
        TrackData       dataTrack = null;
        Hashtable       hashSaveFile;

        objValue = hashProperties.get ( KEY_SAVE_FILE_TRACKS );
        if ( objValue != null  &&  objValue instanceof Hashtable ) {
            hashSaveFile = (Hashtable) objValue;
        }
        else {
            hashSaveFile = new Hashtable ();
            hashProperties.put ( KEY_SAVE_FILE_TRACKS, hashSaveFile );
        }

        objValue = hashSaveFile.get ( strTrack );
        if ( objValue != null  &&  objValue instanceof TrackData )
            dataTrack =(TrackData)objValue;
        return ( dataTrack );
    }

    public void setLastSaveFileTrackData ( TrackData dataTrack, String strTrack ) {
        Object          objValue;
        Hashtable       hashSaveFile;

        objValue = hashProperties.get ( KEY_SAVE_FILE_TRACKS );
        if ( objValue != null  &&  objValue instanceof Hashtable ) {
            hashSaveFile = (Hashtable) objValue;
        }
        else {
            hashSaveFile = new Hashtable ();
            hashProperties.put ( KEY_SAVE_FILE_TRACKS, hashSaveFile );
        }
        hashSaveFile.put ( strTrack, dataTrack );
    }

    public String getLastSaveFileDir () {
        Object  objValue;
        String  strValue = null;

        objValue = hashProperties.get ( KEY_SAVE_FILE_DIR );
        if ( objValue != null )
            strValue = objValue.toString ();
        return ( strValue );
    }

    public void setLastSaveFileDir ( String strValue ) {
        hashProperties.put ( KEY_SAVE_FILE_DIR, strValue );
    }

    public Enumeration getRecentUrlTypes () {
        Enumeration     enumTypes = null;
        Object          objValue;
        Hashtable       hashRecentUrls;

        objValue = hashProperties.get ( KEY_RECENT_URL );
        if ( objValue != null  &&  objValue instanceof Hashtable ) {
            hashRecentUrls = (Hashtable) objValue;
            enumTypes = hashRecentUrls.keys ();
        }
        return ( enumTypes );
    }

    public Vector getRecentUrls ( String strUrlType ) {
        Object          objValue;
        Vector          vectorUrls = null;
        Hashtable       hashRecentUrls;

        objValue = hashProperties.get ( KEY_RECENT_URL );
        if ( objValue != null  &&  objValue instanceof Hashtable ) {
            hashRecentUrls = (Hashtable) objValue;
        }
        else {
            hashRecentUrls = new Hashtable ();
            hashProperties.put ( KEY_RECENT_URL, hashRecentUrls );
        }

        objValue = hashRecentUrls.get ( strUrlType );
        if ( objValue != null  &&  objValue instanceof Vector )
            vectorUrls =(Vector)objValue;
        return ( vectorUrls );
    }

    public void addRecentUrls ( String strUrlType, String strUrl ) {
        Object          objValue;
        Vector          vectorUrls = null;
        Hashtable       hashRecentUrls;

        objValue = hashProperties.get ( KEY_RECENT_URL );
        if ( objValue != null  &&  objValue instanceof Hashtable ) {
            hashRecentUrls = (Hashtable) objValue;
        }
        else {
            hashRecentUrls = new Hashtable ();
            hashProperties.put ( KEY_RECENT_URL, hashRecentUrls );
        }

        objValue = hashRecentUrls.get ( strUrlType );
        if ( objValue != null  &&  objValue instanceof Vector ) {
            vectorUrls =(Vector)objValue;
        }
        else {
            vectorUrls = new Vector ();
            hashRecentUrls.put ( strUrlType, vectorUrls );
        }

        if ( vectorUrls.contains(strUrl) )
            vectorUrls.removeElement ( strUrl );
        else if ( vectorUrls.size() >= 16 )
            vectorUrls.removeElementAt ( 15 );
        vectorUrls.insertElementAt ( strUrl, 0 );
    }

    public Point getJMStudioFrameLocation ( int nFrameIndex ) {
        int     nCount;
        int     nIndex;
        Object  objValue;
        Point   pointValue = null;
        Vector  vectorValues = null;

        objValue = hashProperties.get ( KEY_JMSTUDIO_FRAME_POS );
        if ( objValue != null  &&  objValue instanceof Vector ) {
            vectorValues = (Vector) objValue;
            nCount = vectorValues.size ();
            if ( nFrameIndex < nCount )
                nIndex = nFrameIndex;
            else
                nIndex = nCount - 1;
            objValue = vectorValues.elementAt ( nIndex );
            if ( objValue != null  &&  objValue instanceof Point ) {
                pointValue = new Point ( (Point)objValue );
                pointValue.x += 20 * (nFrameIndex - nIndex);
                pointValue.y += 20 * (nFrameIndex - nIndex);
            }

        }

        if ( pointValue == null )
            pointValue = new Point ( 20 * nFrameIndex, 20 * nFrameIndex );
        return ( pointValue );
    }

    public void setJMStudioFrameLocation ( Point pointValue, int nFrameIndex ) {
        int     nCount;
        Object  objValue;
        Vector  vectorValues = null;

        objValue = hashProperties.get ( KEY_JMSTUDIO_FRAME_POS );
        if ( objValue != null  &&  objValue instanceof Vector ) {
            vectorValues = (Vector) objValue;
        }
        else {
            vectorValues = new Vector ();
            hashProperties.put ( KEY_JMSTUDIO_FRAME_POS, vectorValues );
        }

        nCount = vectorValues.size ();
        if ( nFrameIndex < nCount )
            vectorValues.setElementAt ( pointValue, nFrameIndex );
        else
            vectorValues.addElement ( pointValue );
    }

    public boolean getAutoPlay () {
        Object      objValue;
        boolean     boolValue = true;

        objValue = hashProperties.get ( KEY_AUTO_PLAY );
        if ( objValue != null  &&  objValue instanceof Boolean )
            boolValue = ((Boolean)objValue).booleanValue ();
        return ( boolValue );
    }

    public void setAutoPlay ( boolean boolValue ) {
        Boolean     objValue;

        objValue = new Boolean ( boolValue );
        hashProperties.put ( KEY_AUTO_PLAY, objValue );
    }

    public boolean getAutoLoop () {
        Object      objValue;
        boolean     boolValue = true;

        objValue = hashProperties.get ( KEY_AUTO_LOOP );
        if ( objValue != null  &&  objValue instanceof Boolean )
            boolValue = ((Boolean)objValue).booleanValue ();
        return ( boolValue );
    }

    public void setAutoLoop ( boolean boolValue ) {
        Boolean     objValue;

        objValue = new Boolean ( boolValue );
        hashProperties.put ( KEY_AUTO_LOOP, objValue );
    }

    public boolean getKeepAspectRatio () {
        Object      objValue;
        boolean     boolValue = false;

        objValue = hashProperties.get ( KEY_KEEP_ASPECT );
        if ( objValue != null  &&  objValue instanceof Boolean )
            boolValue = ((Boolean)objValue).booleanValue ();
        return ( boolValue );
    }

    public void setKeepAspectRatio ( boolean boolValue ) {
        Boolean     objValue;

        objValue = new Boolean ( boolValue );
        hashProperties.put ( KEY_KEEP_ASPECT, objValue );
    }



    protected void init () throws Exception {
        readFile ();
    }


    private void readFile () {
        FileInputStream         streamFile = null;
        ObjectInputStream       streamObject = null;
        String                  strSignatue;
        String                  strVersion;
        Object                  objValue;
        String                  strPath;

        try {
            strPath = System.getProperty ( "user.home" );
            if ( strPath != null )
                nameFileCfg = strPath + File.separator + nameFileCfg;
            streamFile = new FileInputStream ( nameFileCfg );
        }
        catch ( Exception exception ) {
            // so we will start from scratch
            return;
        }

        try {
            streamObject = new ObjectInputStream ( streamFile );

            strSignatue = streamObject.readUTF ();
            strVersion = streamObject.readUTF ();

            objValue = streamObject.readObject ();
            if ( objValue.toString().equals(signatureHashtable) )
                hashProperties = readHashtable ( streamObject );
        }
        catch ( Exception exception ) {
            MessageDialog.createErrorDialog ( null,
                                JMFI18N.getResource("jmstudio.error.cfgfile.read"),
                                exception );
        }

        try {
            if ( streamObject != null )
                streamObject.close ();
            if ( streamFile != null )
                streamFile.close ();
        }
        catch ( Exception exception ) {
            MessageDialog.createErrorDialog ( null,
                                JMFI18N.getResource("jmstudio.error.cfgfile.close"),
                                exception );
        }
    }

    private Hashtable readHashtable ( ObjectInputStream streamObject ) throws Exception {
        int             i;
        int             nSize;
        String          strKey;
        Object          objValue;
        Hashtable       hashRead;

        hashRead = new Hashtable ();
        nSize = streamObject.readInt ();
        for ( i = 0;  i < nSize;  i++ ) {
            strKey = streamObject.readUTF ();
            objValue = streamObject.readObject ();
            if ( objValue != null  &&  objValue.toString().equals(signatureHashtable) ) {
                objValue = readHashtable ( streamObject );
            }
            hashRead.put ( strKey, objValue );
        }
        return ( hashRead );
    }

    private void saveFile () {
        FileOutputStream        streamFile = null;
        ObjectOutputStream      streamObject = null;
        String                  strVersion;
        int                     nSize;
        Enumeration             enumKeys;
        String                  strKey;
        Object                  objValue;


        try {
            streamFile = new FileOutputStream ( nameFileCfg );
            streamObject = new ObjectOutputStream ( streamFile );
        }
        catch ( Exception exception ) {
//            MessageDialog.createErrorDialog ( null,
//                                JMFI18N.getResource("jmstudio.error.cfgfile.create"),
//                                exception );
            System.out.println ( JMFI18N.getResource("jmstudio.error.cfgfile.create")
                                                + " " + exception.getMessage() );
       	    return; 
	}

        try {
            streamObject.writeUTF ( signatureFileCfg );
            strVersion = Manager.getVersion ();
            streamObject.writeUTF ( strVersion );

            writeHashtable ( streamObject, hashProperties );
        }
        catch ( Exception exception ) {
            MessageDialog.createErrorDialog ( null,
                                JMFI18N.getResource("jmstudio.error.cfgfile.write"),
                                exception );
        }

        try {
            if ( streamObject != null )
                streamObject.close ();
            if ( streamFile != null )
                streamFile.close ();
        }
        catch ( Exception exception ) {
            MessageDialog.createErrorDialog ( null,
                                JMFI18N.getResource("jmstudio.error.cfgfile.close"),
                                exception );
        }
    }

    private void writeHashtable ( ObjectOutputStream streamObject, Hashtable hashWrite ) throws Exception {
        int             nSize;
        Enumeration     enumKeys;
        String          strKey;
        Object          objValue;


        streamObject.writeObject ( signatureHashtable );
        nSize = hashWrite.size ();
        streamObject.writeInt ( nSize );
        enumKeys = hashWrite.keys ();
        while ( enumKeys.hasMoreElements() ) {
            strKey = (String) enumKeys.nextElement();

            objValue = hashWrite.get ( strKey );
//System.out.println ( "AAAAA Writing object: " + objValue.getClass().getName() );
            if ( objValue instanceof Hashtable ) {
                streamObject.writeUTF ( strKey );
                writeHashtable ( streamObject, (Hashtable)objValue );
            }
            else if ( !(objValue instanceof Serializable) ) {
/*CHANGE*/
                System.out.println ( "Error. Not Serializable object" );
            }
            else {
                streamObject.writeUTF ( strKey );
                streamObject.writeObject ( objValue );
            }
            streamObject.flush ();
        }
    }


    public RtpData createRtpDataObject () {
        RtpData         dataRtp;

        dataRtp = new RtpData ();
        return ( dataRtp );
    }

    public class RtpData implements Serializable {
        public String   strAddress0 = "0";
        public String   strAddress1 = "0";
        public String   strAddress2 = "0";
        public String   strAddress3 = "0";
        public String   strPort = "0";
        public String   strTtl = "1";

        private void writeObject ( ObjectOutputStream streamOut )
                        throws IOException {
            streamOut.writeUTF ( strAddress0 );
            streamOut.writeUTF ( strAddress1 );
            streamOut.writeUTF ( strAddress2 );
            streamOut.writeUTF ( strAddress3 );
            streamOut.writeUTF ( strPort );
            streamOut.writeUTF ( strTtl );
        }

        private void readObject ( ObjectInputStream streamIn )
                        throws IOException, ClassNotFoundException {
            strAddress0 = streamIn.readUTF ();
            strAddress1 = streamIn.readUTF ();
            strAddress2 = streamIn.readUTF ();
            strAddress3 = streamIn.readUTF ();
            strPort = streamIn.readUTF ();
            strTtl = streamIn.readUTF ();
        }

        public String toString () {
            return ( "RtpData address " + strAddress0 + "." + strAddress1 + "."
                        + strAddress2 + "." + strAddress3 + "; port " + strPort
                        + "; TTL " + strTtl );
        }
    }

    public CaptureDeviceData createCaptureDeviceDataObject () {
        CaptureDeviceData       dataCaptureDevice;

        dataCaptureDevice = new CaptureDeviceData ();
        return ( dataCaptureDevice );
    }

    public class CaptureDeviceData implements Serializable {
        public boolean  boolUse = false;
        public String   strDeviceName = null;
        public Format   format = null;

        private void writeObject ( ObjectOutputStream streamOut )
                        throws IOException {
            streamOut.writeBoolean ( boolUse );
            if ( strDeviceName == null ) {
                streamOut.writeBoolean ( false );
            }
            else {
                streamOut.writeBoolean ( true );
                streamOut.writeUTF ( strDeviceName );
            }
            if ( format == null ) {
                streamOut.writeBoolean ( false );
            }
            else {
                streamOut.writeBoolean ( true );
                streamOut.writeObject ( format );
            }
        }

        private void readObject ( ObjectInputStream streamIn )
                        throws IOException, ClassNotFoundException {
            Object      objValue;
            boolean     boolRead;

            boolUse = streamIn.readBoolean ();

            boolRead = streamIn.readBoolean ();
            if ( boolRead == true )
                strDeviceName = streamIn.readUTF ();
            else
                strDeviceName = null;

            boolRead = streamIn.readBoolean ();
            if ( boolRead == true )
                objValue = streamIn.readObject ();
            else
                objValue = null;
            if ( objValue != null  &&  objValue instanceof Format )
                format = (Format) objValue;
            else
                format = null;
        }
    }

    public TrackData createTrackDataObject () {
        TrackData       dataTrack;

        dataTrack = new TrackData ();
        return ( dataTrack );
    }

    public class TrackData implements Serializable {
        public boolean  boolEnable = false;
        public Format   format = null;

        private void writeObject ( ObjectOutputStream streamOut )
                        throws IOException {
            streamOut.writeBoolean ( boolEnable );
            if ( format == null ) {
                streamOut.writeBoolean ( false );
            }
            else {
                streamOut.writeBoolean ( true );
                streamOut.writeObject ( format );
            }
        }

        private void readObject ( ObjectInputStream streamIn )
                        throws IOException, ClassNotFoundException {
            Object      objValue;
            boolean     boolRead;

            boolEnable = streamIn.readBoolean ();
            boolRead = streamIn.readBoolean ();
            if ( boolRead == true )
                objValue = streamIn.readObject ();
            else
                objValue = null;
            if ( objValue != null  &&  objValue instanceof Format )
                format = (Format) objValue;
            else
                format = null;
        }
    }

}


