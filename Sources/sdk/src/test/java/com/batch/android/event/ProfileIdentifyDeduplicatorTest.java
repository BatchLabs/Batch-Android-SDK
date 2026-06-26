package com.batch.android.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.junit.Test;

public class ProfileIdentifyDeduplicatorTest {

    // --------------- helpers ---------------

    private static Event identifyEvent(String customId) {
        String params = customId != null
            ? "{\"identifiers\":{\"custom_id\":\"" + customId + "\"}}"
            : "{\"identifiers\":{}}";
        return new Event(
            "id-" + customId,
            InternalEvents.PROFILE_IDENTIFY,
            new Date(),
            TimeZone.getDefault(),
            params,
            Event.State.NEW,
            0L,
            null,
            null
        );
    }

    private static Event identifyEventNullId() {
        return new Event(
            "id-null",
            InternalEvents.PROFILE_IDENTIFY,
            new Date(),
            TimeZone.getDefault(),
            "{\"identifiers\":{\"custom_id\":null}}",
            Event.State.NEW,
            0L,
            null,
            null
        );
    }

    private static Event otherEvent(String name) {
        return new Event("id-" + name, name, new Date(), TimeZone.getDefault(), null, Event.State.NEW, 0L, null, null);
    }

    // --------------- extractCustomId ---------------

    @Test
    public void extractCustomId_nullParameters_returnsNull() {
        assertNull(ProfileIdentifyDeduplicator.extractCustomId(null));
    }

    @Test
    public void extractCustomId_invalidJson_returnsNull() {
        assertNull(ProfileIdentifyDeduplicator.extractCustomId("not-json"));
    }

    @Test
    public void extractCustomId_noIdentifiersObject_returnsNull() {
        assertNull(ProfileIdentifyDeduplicator.extractCustomId("{\"foo\":\"bar\"}"));
    }

    @Test
    public void extractCustomId_identifiersWithoutCustomId_returnsNull() {
        assertNull(ProfileIdentifyDeduplicator.extractCustomId("{\"identifiers\":{\"other\":\"value\"}}"));
    }

    @Test
    public void extractCustomId_identifiersWithNullCustomId_returnsNull() {
        assertNull(ProfileIdentifyDeduplicator.extractCustomId("{\"identifiers\":{\"custom_id\":null}}"));
    }

    @Test
    public void extractCustomId_validCustomId_returnsValue() {
        assertEquals(
            "user123",
            ProfileIdentifyDeduplicator.extractCustomId("{\"identifiers\":{\"custom_id\":\"user123\"}}")
        );
    }

    @Test
    public void extractCustomId_emptyCustomId_returnsEmptyString() {
        assertEquals("", ProfileIdentifyDeduplicator.extractCustomId("{\"identifiers\":{\"custom_id\":\"\"}}"));
    }

    // --------------- deduplicate ---------------

    @Test
    public void deduplicate_emptyList_returnsEmpty() {
        List<Event> result = ProfileIdentifyDeduplicator.deduplicate(new ArrayList<>());
        assertEquals(0, result.size());
    }

    @Test
    public void deduplicate_noIdentifyEvents_returnsAllUnchanged() {
        List<Event> events = Arrays.asList(otherEvent("_START"), otherEvent("_STOP"));
        List<Event> result = ProfileIdentifyDeduplicator.deduplicate(events);
        assertEquals(2, result.size());
        assertEquals("_START", result.get(0).getName());
        assertEquals("_STOP", result.get(1).getName());
    }

    @Test
    public void deduplicate_singleIdentifyEvent_returnsIt() {
        List<Event> events = Collections.singletonList(identifyEvent("alice"));
        List<Event> result = ProfileIdentifyDeduplicator.deduplicate(events);
        assertEquals(1, result.size());
    }

    @Test
    public void deduplicate_twoIdentifyWithSameCustomId_keepsOnlyFirst() {
        Event first = identifyEvent("alice");
        Event second = identifyEvent("alice");
        List<Event> result = ProfileIdentifyDeduplicator.deduplicate(Arrays.asList(first, second));
        assertEquals(1, result.size());
        assertEquals(first.getId(), result.get(0).getId());
    }

    @Test
    public void deduplicate_twoIdentifyWithDifferentCustomId_keepsBoth() {
        Event first = identifyEvent("alice");
        Event second = identifyEvent("bob");
        List<Event> result = ProfileIdentifyDeduplicator.deduplicate(Arrays.asList(first, second));
        assertEquals(2, result.size());
    }

    @Test
    public void deduplicate_threeIdentifyAllSameCustomId_keepsOnlyFirst() {
        List<Event> events = Arrays.asList(identifyEvent("alice"), identifyEvent("alice"), identifyEvent("alice"));
        List<Event> result = ProfileIdentifyDeduplicator.deduplicate(events);
        assertEquals(1, result.size());
    }

    @Test
    public void deduplicate_twoNullCustomIds_keepsOnlyFirst() {
        Event first = identifyEventNullId();
        Event second = identifyEventNullId();
        List<Event> result = ProfileIdentifyDeduplicator.deduplicate(Arrays.asList(first, second));
        assertEquals(1, result.size());
        assertEquals(first.getId(), result.get(0).getId());
    }

    @Test
    public void deduplicate_nullThenNonNullCustomId_keepsBoth() {
        List<Event> result = ProfileIdentifyDeduplicator.deduplicate(
            Arrays.asList(identifyEventNullId(), identifyEvent("alice"))
        );
        assertEquals(2, result.size());
    }

    @Test
    public void deduplicate_nonIdentifyBetweenSameCustomIds_secondIdentifyRemoved() {
        Event first = identifyEvent("alice");
        Event middle = otherEvent("_START");
        Event second = identifyEvent("alice");
        List<Event> result = ProfileIdentifyDeduplicator.deduplicate(Arrays.asList(first, middle, second));
        assertEquals(2, result.size());
        assertEquals(first.getId(), result.get(0).getId());
        assertEquals(middle.getId(), result.get(1).getId());
    }

    @Test
    public void deduplicate_nonIdentifyBetweenDifferentCustomIds_allKept() {
        Event first = identifyEvent("alice");
        Event middle = otherEvent("_START");
        Event second = identifyEvent("bob");
        List<Event> result = ProfileIdentifyDeduplicator.deduplicate(Arrays.asList(first, middle, second));
        assertEquals(3, result.size());
    }

    @Test
    public void deduplicate_identifyCustomIdChangesAndThenRepeats_deduplicatesSecondRepeat() {
        // alice → bob → alice: all three are distinct transitions, all kept
        List<Event> events = Arrays.asList(identifyEvent("alice"), identifyEvent("bob"), identifyEvent("alice"));
        List<Event> result = ProfileIdentifyDeduplicator.deduplicate(events);
        assertEquals(3, result.size());
    }

    @Test
    public void deduplicate_originalListUnmodified() {
        List<Event> original = new ArrayList<>(Arrays.asList(identifyEvent("alice"), identifyEvent("alice")));
        ProfileIdentifyDeduplicator.deduplicate(original);
        assertEquals(2, original.size());
    }
}
