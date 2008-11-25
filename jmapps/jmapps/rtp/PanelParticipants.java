/*
 * @(#)PanelParticipants.java	1.5 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package jmapps.rtp;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.rtp.*;
import javax.media.rtp.rtcp.*;
import javax.media.rtp.event.*;

import com.sun.media.util.JMFI18N;
import com.sun.media.rtp.util.Signed;

import jmapps.ui.*;


public class PanelParticipants extends JMPanel implements ComponentListener,
                                        ActionListener, SessionListener,
                                        ReceiveStreamListener, RemoteListener {

    private SessionManager  mngrSession;

    private TreeControl     controlTree = null;
    private JMPanel         panelView = null;
    private CardLayout      layoutView = null;
    private JMPanel         panelCurrentView = null;

    private static final int    MARGINH = 6;

    public static final String  PARTICIPANTS            = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.partcipants" );
    public static final String  REMOTE_RECEIVE_STREAM   = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.partcipants.remotereceivestream" );
    public static final String  UNKNOWN_STREAM          = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.partcipants.unknownstream" );
    public static final String  REPORT                  = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.partcipants.report" );
    public static final String  LATEST_SENDER_REPORT    = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.partcipants.latestsenderreport" );
    public static final String  REPORT_BLOCK            = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.partcipants.reportblock" );
    public static final String  REPORT_BLOCK_BY_ME      = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.partcipants.reportblockbyme" );
    public static final String  SENDER_REPORT_BLOCK     = JMFI18N.getResource ( "jmstudio.rtpsessionctrl.partcipants.senderreportblock" );


    public PanelParticipants ( SessionManager mngrSession ) {
	    super ();

        this.mngrSession = mngrSession;
        try {
            init();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init () throws Exception {
        TreeNode    nodeRoot;
        TreeNode    node;
        JMPanel     panel;

        this.addComponentListener ( this );
        this.setLayout ( null );

        controlTree = new TreeControl ();
        this.add ( controlTree );

        layoutView = new CardLayout ();
        panelView = new JMPanel ( layoutView );
        panelView.setEtchedBorder ();
        this.add ( panelView );

        nodeRoot = controlTree.createRootElement ( PARTICIPANTS );
        panel = new ViewParticipantList ( mngrSession );
        nodeRoot.setUserData ( panel );
        fillParticipantsNode ( nodeRoot );
        controlTree.setCurrentElement ( nodeRoot );
        nodeRoot.addActionListener ( this );
        this.setViewPanel ( nodeRoot );

        mngrSession.addSessionListener ( this );
        mngrSession.addReceiveStreamListener ( this );
        mngrSession.addRemoteListener ( this );
    }

    public Dimension getPreferredSize () {
        Dimension   dim;

        dim = new Dimension ( 480, 360 );
        return ( dim );
    }

    public void actionPerformed ( ActionEvent event ) {
        Object      objSource;
        String      strAction;

        objSource = event.getSource ();
        if ( objSource == null  ||  !(objSource instanceof TreeNode) )
            return;

        strAction = event.getActionCommand ();
        if ( strAction.equals(TreeNode.ACTION_NODE_ADDED) ) {
        }
        else if ( strAction.equals(TreeNode.ACTION_NODE_REMOVED) ) {
            removeViewPanel ( (TreeNode)objSource );
        }
        else if ( strAction.equals(TreeNode.ACTION_NODE_EXPANDED) ) {
        }
        else if ( strAction.equals(TreeNode.ACTION_NODE_COLLAPSED) ) {
        }
        else if ( strAction.equals(TreeNode.ACTION_NODE_SETCURRENT) ) {
            setViewPanel ( (TreeNode)objSource );
        }
        else if ( strAction.equals(TreeNode.ACTION_NODE_RESETCURRENT) ) {
        }
    }

    public void update ( SessionEvent event ) {
        Participant     participant;

        if ( event instanceof NewParticipantEvent ) {
            participant = ((NewParticipantEvent)event).getParticipant();
            addParticipant ( participant );
        }
    }

    public void update ( RemoteEvent event ) {
        Participant     participant;
        ReceiverReport  reportReceiver;
        SenderReport    reportSender;
        TreeNode        node;
        Object          objPanel;

        if ( event instanceof ReceiverReportEvent ){
            participant = ((ReceiverReportEvent)event).getReport().getParticipant();
            if ( participant != null  &&  participant.getStreams().size() == 0 ) {
                node = findParticipantListNode ();
                if ( node != null ) {
                    objPanel = node.getUserData ();
                    if ( objPanel != null  &&  objPanel instanceof ViewParticipantList )
                        ((ViewParticipantList)objPanel).updateFields ();

                    reportReceiver = ((ReceiverReportEvent)event).getReport();
                    addReport ( participant, reportReceiver );
                }
            }
        }
        if ( event instanceof SenderReportEvent ) {
            participant = ((SenderReportEvent)event).getReport().getParticipant();
            if ( participant != null  &&  participant.getStreams().size() > 0 ) {
                node = findParticipantListNode ();
                if ( node != null ) {
                    objPanel = node.getUserData ();
                    if ( objPanel != null  &&  objPanel instanceof ViewParticipantList )
                        ((ViewParticipantList)objPanel).updateFields ();

                    reportSender = ((SenderReportEvent)event).getReport();
                    addReport ( participant, reportSender );
                }
            }
        }
    }

    public void update ( ReceiveStreamEvent event){
        Participant     participant;
        ReceiveStream   stream;
        TreeNode        node;
        Object          objPanel;

        // if this is a timeOut or ByeEvent, we need to remove the
        // participants from the list of participants
        if ( event instanceof TimeoutEvent  ||  event instanceof ByeEvent ) {
            participant = null;
            stream = null;

            if ( event instanceof TimeoutEvent  &&  ((TimeoutEvent)event).participantLeaving() ) {
                participant = ((TimeoutEvent)event).getParticipant();
            }
            if ( event instanceof ByeEvent  &&  ((ByeEvent)event).participantLeaving() ) {
                participant = ((ByeEvent)event).getParticipant();
            }
            else if ( event instanceof TimeoutEvent ) {
                stream = ((TimeoutEvent)event).getReceiveStream();
            }
            else if ( event instanceof ByeEvent ) {
                stream = ((ByeEvent)event).getReceiveStream();
            }

            if ( participant != null ) {
                removeParticipant ( participant );
            }
            if ( stream != null ) {
                removeRtpStream ( stream );
            }
        }
        // if this is a NewReceiveStreamEvent, from an already existant
        // PASSVE paritcipant, we will need to remove the
        // Participant from the Passive list and add it to the
        // active list
        if ( event instanceof NewReceiveStreamEvent ) {
            stream = ((NewReceiveStreamEvent)event).getReceiveStream();
            participant = stream.getParticipant ();
            if ( participant == null )
                return;

            node = findParticipantNode ( participant );
            if ( node == null )
                addParticipant ( participant );
            addRtpStream ( participant, stream );

            node = this.findParticipantListNode ();
            objPanel = node.getUserData();
            if ( objPanel != null  &&  (objPanel instanceof ViewParticipantList) )
                ((ViewParticipantList)objPanel).updateFields ();

        }
        if ( event instanceof InactiveReceiveStreamEvent ) {
            // don't know what to do here
        }
        if ( event instanceof ActiveReceiveStreamEvent ) {
        }
    }

    public void componentResized ( ComponentEvent event ) {
        layoutComponents ();
    }

    public void componentMoved ( ComponentEvent event ) {
    }

    public void componentShown ( ComponentEvent event ) {
    }

    public void componentHidden ( ComponentEvent event ) {
    }


    private void fillParticipantsNode ( TreeNode nodeParent ) {
        int         i;
        int         nCount;
        Vector      vectorParticipants;
        Vector      vector;
        Object      objParticipant;
        String      strName;
        TreeNode    node;
        JMPanel     panel;

        // get all participants into one vector
        vectorParticipants = mngrSession.getRemoteParticipants ();
        objParticipant = mngrSession.getLocalParticipant ();
        if ( !vectorParticipants.contains(objParticipant) )
            vectorParticipants.addElement ( objParticipant );
        vector = mngrSession.getActiveParticipants ();
        nCount = vector.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            objParticipant = vector.elementAt ( i );
            if ( vectorParticipants.contains(objParticipant) )
                continue;
            vectorParticipants.addElement ( objParticipant );
        }
        vector = mngrSession.getPassiveParticipants ();
        nCount = vector.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            objParticipant = vector.elementAt ( i );
            if ( vectorParticipants.contains(objParticipant) )
                continue;
            vectorParticipants.addElement ( objParticipant );
        }

        // now fill the tree with participants from collected vector
        nCount = vectorParticipants.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            objParticipant = vectorParticipants.elementAt ( i );
            if ( objParticipant == null  ||  !(objParticipant instanceof Participant) )
                continue;

            strName = ((Participant)objParticipant).getCNAME ();
            node = controlTree.createSubElement ( nodeParent, strName );
            panel = new ViewParticipantInfo ( mngrSession, (Participant)objParticipant );
            node.setUserData ( panel );
            fillParticipantInfoNode ( node, (Participant)objParticipant );
            node.addActionListener ( this );
        }
    }

    private void fillParticipantInfoNode ( TreeNode nodeParent, Participant participant ) {
        int             i;
        int             nCount;
        TreeNode        node;
        Vector          vectorStreams;
        Object          objStream;
        Vector          vectorReports;
        Object          objReport;
        String          strName;
        long            lSSRC;
        JMPanel         panel;
        Object          objPanel;

        if ( participant == null )
            return;

        vectorStreams = participant.getStreams ();
        nCount = vectorStreams.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            objStream = vectorStreams.elementAt ( i );
            if ( objStream == null  ||  !(objStream instanceof RTPStream) )
                continue;
            addRtpStream ( nodeParent, (RTPStream)objStream );
        }

        vectorReports = participant.getReports ();
        nCount = vectorReports.size ();
        for ( i = 0;  i < nCount;  i++ ) {
            objReport = vectorReports.elementAt ( i );
            if ( objReport == null  ||  !(objReport instanceof Report) )
                continue;
            addReport ( nodeParent, (Report)objReport );
        }
    }

    private void fillStreamInfoNode ( TreeNode nodeParent, RTPStream streamRtp ) {
        SenderReport    reportSender;
        TreeNode        node;
        String          strName;
        JMPanel         panel;

        if ( streamRtp == null )
            return;

        reportSender = streamRtp.getSenderReport();
        if ( reportSender != null ) {
            strName = createStreamLatestReportNodeName ();
            node = controlTree.createSubElement ( nodeParent, strName );
            panel = new ViewSenderReport ( mngrSession, reportSender );
            node.setUserData ( panel );
            fillReportInfoNode ( node, reportSender );
            node.addActionListener ( this );
        }
    }

    private void fillReportInfoNode ( TreeNode nodeParent, Report objReport ) {
        updateReportBlocks ( nodeParent, objReport );
    }

    private void addParticipant ( Participant participant ) {
        TreeNode    nodeParent;
        TreeNode    node;
        String      strName;
        JMPanel     panel;
        Object      objPanel;

        if ( participant == null )
            return;

        nodeParent = findParticipantListNode ();
        node = findParticipantNode ( participant );
        if ( node != null )
            // is there already
            return;

        strName = participant.getCNAME ();
        node = controlTree.createSubElement ( nodeParent, strName );
        panel = new ViewParticipantInfo ( mngrSession, participant );
        node.setUserData ( panel );
        fillParticipantInfoNode ( node, participant );
        node.addActionListener ( this );

        objPanel = nodeParent.getUserData();
        if ( objPanel != null  &&  (objPanel instanceof ViewParticipantList) )
            ((ViewParticipantList)objPanel).updateFields ();
    }

    private void removeParticipant ( Participant participant ) {
        TreeNode    nodeParent;
        TreeNode    node;
        String      strName;
        JMPanel     panel;
        Object      objPanel;

        node = findParticipantNode ( participant );
        if ( node == null )
            return;

        nodeParent = findParticipantListNode ();
        controlTree.destroySubElement ( nodeParent, node );

        objPanel = nodeParent.getUserData();
        if ( objPanel != null  &&  (objPanel instanceof ViewParticipantList) )
            ((ViewParticipantList)objPanel).updateFields ();
    }

    private void addRtpStream ( Participant participant, RTPStream stream ) {
        TreeNode    nodeParticipant;
        TreeNode    nodeStream;

        nodeParticipant = findParticipantNode ( participant );
        addRtpStream ( nodeParticipant, stream );
    }

    private void addRtpStream ( TreeNode nodeParticipant, RTPStream stream ) {
        TreeNode    nodeStream;
        String      strName;
        JMPanel     panel;

        if ( nodeParticipant == null )
            return;
        nodeStream = findStreamNode ( nodeParticipant, stream );
        if ( nodeStream != null )
            return;

        strName = createStreamNodeName ( stream );
        nodeStream = controlTree.createSubElement ( nodeParticipant, strName );
        panel = new ViewRtpStreamInfo ( mngrSession, stream );
        nodeStream.setUserData ( panel );
        fillStreamInfoNode ( nodeStream, stream );
        nodeStream.addActionListener ( this );
    }

    private void removeRtpStream ( RTPStream stream ) {
        Participant participant;
        TreeNode    nodeParticipant;
        TreeNode    nodeStream;

        participant = stream.getParticipant();
        if ( participant == null )
            return;

        nodeParticipant = findParticipantNode ( participant );
        removeRtpStream ( nodeParticipant, stream );
    }

    private void removeRtpStream ( TreeNode nodeParticipant, RTPStream stream ) {
        TreeNode    nodeStream;

        if ( nodeParticipant == null )
            return;
        nodeStream = findStreamNode ( nodeParticipant, stream );
        if ( nodeStream == null )
            return;

        controlTree.destroySubElement ( nodeParticipant, nodeStream );
    }

    private void addReport ( Participant participant, Report report ) {
        TreeNode    nodeParticipant;

        if ( participant == null )
            return;
        nodeParticipant = findParticipantNode ( participant );
        addReport ( nodeParticipant, report );
    }

    private void addReport ( TreeNode nodeParticipant, Report report ) {
        String      strName;
        TreeNode    node;
        Object      objPanel;

        if ( nodeParticipant == null )
            return;
        strName = createReportNodeName ( report );
        node = nodeParticipant.getSubElement(strName);
        if ( node != null ) {
            // just update the existing report
            objPanel = node.getUserData();
            if ( objPanel != null  ) {
                if ( objPanel instanceof ViewReceiverReport  &&  report instanceof ReceiverReport )
                    ((ViewReceiverReport)objPanel).updateFields ( (ReceiverReport)report );
                else if ( objPanel instanceof ViewSenderReport  &&  report instanceof SenderReport )
                    ((ViewSenderReport)objPanel).updateFields ( (SenderReport)report );
            }
            updateReportBlocks ( node, report );
        }
        else {
            // create new node
            node = controlTree.createSubElement ( nodeParticipant, strName );
            if ( report instanceof SenderReport ) {
                objPanel = new ViewSenderReport ( mngrSession, (SenderReport)report );
            }
            else if ( report instanceof ReceiverReport ) {
                objPanel = new ViewReceiverReport ( (ReceiverReport)report );
            }
            else {
                objPanel = null;
            }
            node.setUserData ( objPanel );
            fillReportInfoNode ( node, report );
            node.addActionListener ( this );

            objPanel = nodeParticipant.getUserData();
            if ( objPanel != null  &&  (objPanel instanceof ViewParticipantInfo) )
                ((ViewParticipantInfo)objPanel).updateFields ( report );
        }

        if ( report instanceof SenderReport ) {
            node = findStreamNode ( nodeParticipant, ((SenderReport)report).getStream() );
            if ( node != null )
                node = findStreamLatestReportNode ( node );
            if ( node != null ) {
                objPanel = node.getUserData();
                if ( objPanel != null  &&  (objPanel instanceof ViewSenderReport) )
                    ((ViewSenderReport)objPanel).updateFields ( (SenderReport)report );
                updateReportBlocks ( node, report );
            }
        }
    }

    private void updateReportBlocks ( TreeNode nodeReport, Report report ) {
        int             i;
        int             nCount;
        SenderReport    reportSender;
        ReceiverReport  reportReceiver;
        Vector          vectorFeedbacks;
        Feedback        feedback;
        TreeNode        node;
        String          strName;
        JMPanel         panel;
        Object          objPanel;

        if ( report == null )
            return;

        if ( report instanceof SenderReport ) {
            reportSender = (SenderReport)report;

            feedback = reportSender.getSenderFeedback ();
            strName = REPORT_BLOCK_BY_ME;
            node = nodeReport.getSubElement ( strName );
            if ( node == null ) {
                node = controlTree.createSubElement ( nodeReport, strName );
                panel = new ViewReportBlock ( feedback );
                node.setUserData ( panel );
                node.addActionListener ( this );
            }
            else {
                objPanel = node.getUserData();
                if ( objPanel != null  &&  objPanel instanceof ViewReportBlock )
                    ((ViewReportBlock)objPanel).updateFields ( feedback );
            }


            vectorFeedbacks = reportSender.getFeedbackReports ();
            nCount = vectorFeedbacks.size ();
            for ( i = 0;  i < nCount;  i++ ) {
                feedback = (Feedback) vectorFeedbacks.elementAt ( i );
                strName = SENDER_REPORT_BLOCK + " " + i;
                node = nodeReport.getSubElement ( strName );
                if ( node == null ) {
                    node = controlTree.createSubElement ( nodeReport, strName );
                    panel = new ViewReportBlock ( feedback );
                    node.setUserData ( panel );
                    node.addActionListener ( this );
                }
                else {
                    objPanel = node.getUserData();
                    if ( objPanel != null  &&  objPanel instanceof ViewReportBlock )
                        ((ViewReportBlock)objPanel).updateFields ( feedback );
                }
            }
        }
        else if ( report instanceof ReceiverReport ) {
            reportReceiver = (ReceiverReport)report;
            vectorFeedbacks = reportReceiver.getFeedbackReports ();
            nCount = vectorFeedbacks.size ();
            for ( i = 0;  i < nCount;  i++ ) {
                feedback = (Feedback) vectorFeedbacks.elementAt ( i );
                strName = REPORT_BLOCK + " " + i;
                node = nodeReport.getSubElement ( strName );
                if ( node == null ) {
                    node = controlTree.createSubElement ( nodeReport, strName );
                    panel = new ViewReportBlock ( feedback );
                    node.setUserData ( panel );
                    node.addActionListener ( this );
                }
                else {
                    objPanel = node.getUserData();
                    if ( objPanel != null  &&  objPanel instanceof ViewReportBlock )
                        ((ViewReportBlock)objPanel).updateFields ( feedback );
                }
            }
        }
    }

    private TreeNode findParticipantListNode () {
        TreeNode    nodeRoot;

        nodeRoot = controlTree.getRootElement ();
        return ( nodeRoot );
    }

    private TreeNode findParticipantNode ( Participant participant ) {
        TreeNode    nodeRoot;
        TreeNode    node;

        nodeRoot = controlTree.getRootElement ();
        node = nodeRoot.getSubElement ( participant.getCNAME() );
        return ( node );
    }

    private TreeNode findStreamNode ( TreeNode nodeParticipant, RTPStream stream ) {
        TreeNode    node;
        String      strName;

        strName = createStreamNodeName ( stream );
        node = nodeParticipant.getSubElement ( strName );
        return ( node );
    }

    private TreeNode findStreamLatestReportNode ( TreeNode nodeStream ) {
        String      strName;
        TreeNode    node;

        strName = createStreamLatestReportNodeName ();
        node = nodeStream.getSubElement ( strName );
        return ( node );
    }

    private String createStreamNodeName ( RTPStream stream ) {
        String      strName;

        if ( stream == null )
            return ( null );

        if ( stream instanceof ReceiveStream )
            strName = REMOTE_RECEIVE_STREAM + ": ";
        else
            strName = UNKNOWN_STREAM + ": ";
        strName = strName + stream.getParticipant().getCNAME ();
        return ( strName );
    }

    private String createReportNodeName ( Report report ) {
        String      strName;
        long        lSSRC;

        lSSRC = report.getSSRC ();
        strName = REPORT + " " + Signed.UnsignedInt((int)lSSRC);
        return ( strName );
    }

    private String createStreamLatestReportNodeName () {
        String      strName;

        strName = LATEST_SENDER_REPORT;
        return ( strName );
    }

    private void setViewPanel ( TreeNode node ) {
        Object      objData;
        String      strName;

        if ( node == null )
            return;

        objData = node.getUserData ();
        strName = node.getFullPathName ();
        if ( objData != null  &&  objData instanceof JMPanel ) {
            panelCurrentView = (JMPanel) objData;
            if ( panelCurrentView.getParent() == null )
                panelView.add ( panelCurrentView, strName );
            layoutView.show ( panelView, strName );
        }
    }

    private void removeViewPanel ( TreeNode node ) {
        Object      objData;
        String      strName;

        objData = node.getUserData ();
        if ( objData != null  &&  objData instanceof JMPanel ) {
            if ( ((JMPanel)objData).getParent() == panelView )
                panelView.remove ( (JMPanel)objData );
        }
        setViewPanel ( controlTree.getCurrentElement() );
    }

    private void layoutComponents () {
        Dimension   dim;
        int         nWidthTree;

        dim = this.getSize ();
        nWidthTree = (dim.width - MARGINH) / 2;
        if ( controlTree != null )
            controlTree.setBounds ( 0, 0, nWidthTree, dim.height );
        if ( panelView != null ) {
            panelView.setBounds ( nWidthTree + MARGINH, 0, dim.width - (nWidthTree + MARGINH), dim.height );
            panelView.doLayout ();
        }
        repaint ();
    }

}


