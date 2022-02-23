package com.batch.android.tracker;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.event.CollapsibleEvent;
import com.batch.android.event.Event;
import com.batch.android.event.Event.State;
import com.batch.android.json.JSONObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the SQLite DAO
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class DatasourceTest {

    private TrackerDatasource datasource;

    private Context appContext;

    @Before
    public void setUp() throws Exception {
        appContext = ApplicationProvider.getApplicationContext();
        datasource = new TrackerDatasource(appContext);
        datasource.clearDB();
    }

    @After
    public void tearDown() throws Exception {
        datasource.close();
    }

    // -------------------------------------------------->

    /**
     * Test a simple insert
     *
     * @throws Exception
     */
    @Test
    public void testSimpleInsert() throws Exception {
        final String eventName = "TEST_EVENT";

        datasource.addEvent(new Event(appContext, new Date().getTime(), eventName, null));

        List<Event> events = datasource.getAllEvents();
        assertEquals(1, events.size());
        assertEquals(eventName, events.get(0).getName());
    }

    /**
     * Test an insert with parameters
     *
     * @throws Exception
     */
    @Test
    public void testParamsInsert() throws Exception {
        final String eventName = "TEST_EVENT";
        JSONObject parameters = new JSONObject();
        parameters.put("param1", "value1");
        parameters.put("param2", "value2");

        datasource.addEvent(new Event(appContext, new Date().getTime(), eventName, parameters));

        List<Event> events = datasource.getAllEvents();
        assertEquals(1, events.size());
        assertEquals(eventName, events.get(0).getName());

        JSONObject params = new JSONObject(events.get(0).getParameters());
        assertEquals("value1", params.getString("param1"));
        assertEquals("value2", params.getString("param2"));
    }

    /**
     * Test that collapsible events only end up once in the datasource, but regular events don't
     *
     * @throws Exception
     */
    @Test
    public void testMultipleInserts() throws Exception {
        final String normalEventName = "TEST_EVENT";
        final String collapsibleEventName = "TEST_COLLAPSIBLE_EVENT";

        datasource.addEvent(new CollapsibleEvent(appContext, new Date().getTime(), collapsibleEventName, null));
        datasource.addEvent(new CollapsibleEvent(appContext, new Date().getTime(), collapsibleEventName, null));
        datasource.addEvent(new Event(appContext, new Date().getTime(), normalEventName, null));
        datasource.addEvent(new Event(appContext, new Date().getTime(), normalEventName, null));
        datasource.addEvent(new CollapsibleEvent(appContext, new Date().getTime(), collapsibleEventName, null));

        List<Event> events = datasource.getAllEvents();
        assertEquals(3, events.size());
        assertEquals(normalEventName, events.get(0).getName());
        assertEquals(normalEventName, events.get(1).getName());
        assertEquals(collapsibleEventName, events.get(2).getName());
    }

    /**
     * Test extract events
     *
     * @throws Exception
     */
    @Test
    public void testExtractEvents() throws Exception {
        insertBatchEvents(8);

        List<Event> events = datasource.extractEventsToSend(6);
        assertEquals(6, events.size());

        List<Event> dbEvents = datasource.getAllEvents();
        assertEquals(8, dbEvents.size());

        int newEvents = 0;
        int sendingEvents = 0;
        for (Event event : dbEvents) {
            if (event.getState() == State.NEW) {
                newEvents++;
            } else if (event.getState() == State.SENDING) {
                sendingEvents++;
            }
        }

        assertEquals(2, newEvents);
        assertEquals(6, sendingEvents);
    }

    /**
     * Test delete events
     */
    @Test
    public void testDeleteEvents() {
        insertBatchEvents(10);

        List<Event> dbEvents = datasource.getAllEvents();
        assertEquals(10, dbEvents.size());

        List<String> eventsIDs = new ArrayList<>();
        for (Event event : dbEvents) {
            eventsIDs.add(event.getId());
        }

        datasource.deleteEvents(eventsIDs.toArray(new String[eventsIDs.size()]));

        dbEvents = datasource.getAllEvents();
        assertEquals(0, dbEvents.size());
    }

    /**
     * Test update of events state
     */
    @Test
    public void testUpdateEvents() {
        insertBatchEvents(10);

        List<Event> dbEvents = datasource.getAllEvents();
        assertEquals(10, dbEvents.size());

        List<String> eventsIDs = new ArrayList<>();
        for (Event event : dbEvents) {
            assertEquals(Event.State.NEW, event.getState());
            eventsIDs.add(event.getId());
        }

        datasource.updateEventsToOld(eventsIDs.toArray(new String[eventsIDs.size()]));

        dbEvents = datasource.getAllEvents();
        for (Event event : dbEvents) {
            assertEquals(Event.State.OLD, event.getState());
        }

        datasource.updateEventsToNew(eventsIDs.toArray(new String[eventsIDs.size()]));

        dbEvents = datasource.getAllEvents();
        for (Event event : dbEvents) {
            assertEquals(Event.State.NEW, event.getState());
        }
    }

    // -------------------------------------->

    /**
     * Insert multiple events into DB
     *
     * @param number
     */
    private void insertBatchEvents(int number) {
        for (int i = 0; i < number; i++) {
            if (!datasource.addEvent(new Event(appContext, new Date().getTime(), "event" + i, null))) {
                throw new RuntimeException("Insertion failure");
            }
        }
    }
}
