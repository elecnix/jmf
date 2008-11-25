import java.awt.*;
import java.io.*;
import java.net.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;

public abstract class Example1 implements ControllerListener
{
    MediaLocator mrl = null;
    Player player = null;

    private boolean test()
    {
        // --- cut from here... --- //


        MediaLocator mrl= new MediaLocator("rtp://224.144.251.104:49150/audio/1");
        
        if (mrl == null) {
            System.err.println("Can't build MRL for RTP");
            return false;
        }
        
        // Create a player for this rtp session
        try {
            player = Manager.createPlayer(mrl);
        } catch (NoPlayerException e) {
            System.err.println("Error:" + e);
            return false;
        } catch (MalformedURLException e) {
            System.err.println("Error:" + e);
            return false;
        } catch (IOException e) {
            System.err.println("Error:" + e);
            return false;
        }
        
        if (player != null) {
            if (this.player == null) {
                this.player = player;
                player.addControllerListener(this);
                player.realize();
            }
        }

        // --- ...until here --- //

        return true;
    }

    Component     controlComp;
    Component     visualComp;
    MenuComponent zoomMenu;
    double        defaultScale;
    Panel         framePanel;

    // --- cut from here... --- //

    public synchronized void controllerUpdate(ControllerEvent ce) {
        if (ce instanceof FormatChangeEvent) {
            Dimension vSize = new Dimension(320,0);
            Component oldVisualComp = visualComp;
            
            if ((visualComp = player.getVisualComponent()) != null) {
                if (oldVisualComp != visualComp) {
                    if (oldVisualComp != null) {
                        oldVisualComp.remove(zoomMenu);
                    }
                    
                    framePanel.remove(oldVisualComp);
                    
                    vSize = visualComp.getPreferredSize();
                    vSize.width = (int) (vSize.width * defaultScale);
                    vSize.height = (int) (vSize.height * defaultScale);
                    
                    framePanel.add(visualComp);
                    
                    visualComp.setBounds(0, 0, vSize.width, vSize.height);
                    
                    addPopupMenu(visualComp);
                }
            }
            
            Component oldComp = controlComp;
            
            if ((controlComp = player.getControlPanelComponent()) != null) 
            {
                
                if (oldComp != controlComp)
                {
                    framePanel.remove(oldComp);
                    framePanel.add(controlComp);
                                         
                    if (controlComp != null) {
                        int prefHeight = controlComp.getPreferredSize().height;
                        
                        controlComp.setBounds(0, 
                                              vSize.height,
                                              vSize.width,
                                              prefHeight);
                    }
                }
            }
        }
    }
    
    // --- ...until here --- //

    private void addPopupMenu( Component comp)
    {
    }
}
