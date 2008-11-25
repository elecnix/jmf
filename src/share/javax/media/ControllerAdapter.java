/*
 * @(#)ControllerAdapter.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;
import javax.media.format.FormatChangeEvent;

/**
 * The event adapter which recieves <code>ControllerEvents</code> and
 * dispatches them to an appropriate stub-method.  Classes that extend
 * this adapter can easily replace only the message handlers they are
 * interested in.
 * 
 * For example, the following code extends a ControllerAdapter with a JDK
 * 1.1 anonymous inner-class to make a self-contained player that
 * resets itself back to the beginning and deallocates itself when the
 * player reaches the end of the media:
 * 
 * <code>
 * player.addControllerListener(new ControllerAdapter() {
 *     public void endOfMedia(EndOfMediaEvent e) {
 *         Controller controller = e.getSource();
 *         controller.stop();
 *         controller.setMediaTime(0);
 *         controller.deallocate();
 *     }
 * });
 * </code>
 * 
 * @see ControllerListener
 * @see Controller#addControllerListener(ControllerListener)
 *
 * @version 1.3, 98/11/06
 *
 */

public class ControllerAdapter
    implements ControllerListener, java.util.EventListener {

    public void cachingControl(CachingControlEvent e) {}
	
    public void controllerError(ControllerErrorEvent e) {}

    public void dataLostError(DataLostErrorEvent e) {}

    public void dataStarved(DataStarvedEvent e) {}

    public void internalError(InternalErrorEvent e) {}

    public void resourceUnavailable(ResourceUnavailableEvent e) {}

    public void durationUpdate(DurationUpdateEvent e) {}

    public void mediaTimeSet(MediaTimeSetEvent e) {}

    public void rateChange(RateChangeEvent e) {}

    public void stopTimeChange(StopTimeChangeEvent e) {}

    public void transition(TransitionEvent e) {}

    public void prefetchComplete(PrefetchCompleteEvent e) {}

    public void realizeComplete(RealizeCompleteEvent e) {}

    public void start(StartEvent e) {}

    public void stop(StopEvent e) {}

    public void deallocate(DeallocateEvent e) {}

    public void endOfMedia(EndOfMediaEvent e) {}

    public void restarting(RestartingEvent e) {}

    public void stopAtTime(StopAtTimeEvent e) {}

    public void stopByRequest(StopByRequestEvent e) {}

    public void audioDeviceUnavailable(AudioDeviceUnavailableEvent e) {}

    public void configureComplete(ConfigureCompleteEvent e) {}

    public void controllerClosed(ControllerClosedEvent e) {}

    public void sizeChange(SizeChangeEvent e) {}

    public void connectionError(ConnectionErrorEvent e) {}

    public void formatChange(FormatChangeEvent e){}
    
    public void replaceURL(Object e) {}
    
    public void showDocument(Object e) {} 
    
    /**  
     * Main dispatching function.  Subclasses should not need to
     * override this method, but instead subclass only the individual
     * event methods listed above that they need
     */
    public void controllerUpdate(ControllerEvent e) {
	if (e instanceof FormatChangeEvent){
	    formatChange((FormatChangeEvent)e);
	    
	} else if (e instanceof CachingControlEvent) {
	    cachingControl((CachingControlEvent)e);
	    
	} else if (e instanceof ControllerErrorEvent) {
	    controllerError((ControllerErrorEvent)e);
	    
	    if (e instanceof DataLostErrorEvent) {
		dataLostError((DataLostErrorEvent)e);
		
	    } else if (e instanceof InternalErrorEvent) {
		internalError((InternalErrorEvent)e);

	    } else if (e instanceof ResourceUnavailableEvent) {
		resourceUnavailable((ResourceUnavailableEvent)e);

	    } else if (e instanceof ConnectionErrorEvent) {
		connectionError((ConnectionErrorEvent)e);
	    }

	} else if (e instanceof DurationUpdateEvent) {
	    durationUpdate((DurationUpdateEvent)e);

	} else if (e instanceof MediaTimeSetEvent) {
	    mediaTimeSet((MediaTimeSetEvent)e);

	} else if (e instanceof RateChangeEvent) {
	    rateChange((RateChangeEvent)e);

	} else if (e instanceof StopTimeChangeEvent) {
	    stopTimeChange((StopTimeChangeEvent)e);

	} else if (e instanceof AudioDeviceUnavailableEvent) {
	    audioDeviceUnavailable((AudioDeviceUnavailableEvent)e);

	} else if (e instanceof ControllerClosedEvent) {
	    controllerClosed((ControllerClosedEvent)e);

	} else if (e instanceof SizeChangeEvent) {
	    sizeChange((SizeChangeEvent)e);

	} else if (e instanceof TransitionEvent) {
	    transition((TransitionEvent)e);

	    if (e instanceof ConfigureCompleteEvent) {
		configureComplete((ConfigureCompleteEvent)e);

	    } else if (e instanceof PrefetchCompleteEvent) {
		prefetchComplete((PrefetchCompleteEvent)e);

	    } else if (e instanceof RealizeCompleteEvent) {
		realizeComplete((RealizeCompleteEvent)e);
		
	    } else if (e instanceof StartEvent) {
		start((StartEvent)e);

	    } else if (e instanceof StopEvent) {
		stop((StopEvent)e);
		
		if (e instanceof DeallocateEvent) {
		    deallocate((DeallocateEvent)e);
		
		} else if (e instanceof EndOfMediaEvent) {
		    endOfMedia((EndOfMediaEvent)e);
		    
		} else if (e instanceof RestartingEvent) {
		    restarting((RestartingEvent)e);
		    
		} else if (e instanceof StopAtTimeEvent) {
		    stopAtTime((StopAtTimeEvent)e);
		    
		} else if (e instanceof StopByRequestEvent) {
		    stopByRequest((StopByRequestEvent)e);

		} else if (e instanceof DataStarvedEvent) {
		    dataStarved((DataStarvedEvent)e);
		}
	    }
	} else if (e.getClass().getName().equals("com.ibm.media.ReplaceURLEvent")) {
	    // Specialized event for HotMedia playback.
	    replaceURL(e);
	} else if (e.getClass().getName().equals("com.ibm.media.ShowDocumentEvent"))	{
	    // Specialized event for HotMedia playback.
	    showDocument(e);
   }
    }
}
