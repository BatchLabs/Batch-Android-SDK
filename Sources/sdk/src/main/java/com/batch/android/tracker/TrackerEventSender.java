package com.batch.android.tracker;

import com.batch.android.WebserviceLauncher;
import com.batch.android.core.TaskRunnable;
import com.batch.android.event.Event;
import com.batch.android.event.EventSender;
import com.batch.android.runtime.RuntimeManager;
import com.batch.android.webservice.listener.TrackerWebserviceListener;

import java.util.List;

public class TrackerEventSender extends EventSender
{
    /**
     * Event broadcasted when the event sending WS finish
     */
    private final static String WEBSERVICE_HAS_FINISHED_EVENT = "ba_event_ws_finished";

// --------------------------------------------->

    public TrackerEventSender(RuntimeManager runtimeManager, EventSenderListener listener)
    {
        super(runtimeManager, listener);
    }

// --------------------------------------------->

    @Override
    protected String getWebserviceFinishedEvent()
    {
        return WEBSERVICE_HAS_FINISHED_EVENT;
    }

    @Override
    protected TaskRunnable getWebserviceTask(List<Event> events, TrackerWebserviceListener listener)
    {
        return WebserviceLauncher.initTrackerWebservice(runtimeManager, events, listener);
    }

}
