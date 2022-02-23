package com.batch.android.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class UserDataDiffTest {

    @Test
    public void testAttributesDiff() {
        long ts = 123456000;

        Map<String, UserAttribute> newAttributes = new HashMap<>();
        newAttributes.put("c.integer", new UserAttribute(2, AttributeType.LONG));
        newAttributes.put("c.string", new UserAttribute("foo", AttributeType.STRING));
        newAttributes.put("c.date", new UserAttribute(new Date(ts), AttributeType.DATE));

        Map<String, UserAttribute> oldAttributes = new HashMap<>();
        oldAttributes.put("c.string", new UserAttribute("foobar", AttributeType.STRING));
        oldAttributes.put("c.removed", new UserAttribute("removed", AttributeType.STRING));
        oldAttributes.put("c.date", new UserAttribute(new Date(ts), AttributeType.DATE));

        UserDataDiff.Result diff = new UserDataDiff(
            newAttributes,
            oldAttributes,
            Collections.<String, Set<String>>emptyMap(),
            Collections.<String, Set<String>>emptyMap()
        )
            .result;

        Map<String, UserAttribute> expectedAddedAttributes = new HashMap<>();
        expectedAddedAttributes.put("c.integer", new UserAttribute(2, AttributeType.LONG));
        expectedAddedAttributes.put("c.string", new UserAttribute("foo", AttributeType.STRING));

        Map<String, UserAttribute> expectedRemovedAttributes = new HashMap<>();
        expectedRemovedAttributes.put("c.removed", new UserAttribute("removed", AttributeType.STRING));
        expectedRemovedAttributes.put("c.string", new UserAttribute("foobar", AttributeType.STRING));

        assertEquals(expectedAddedAttributes, diff.addedAttributes);
        assertEquals(expectedRemovedAttributes, diff.removedAttributes);
        assertEquals(new HashMap<String, Set<String>>(), diff.addedTags);
        assertEquals(new HashMap<String, Set<String>>(), diff.removedTags);
        assertTrue(diff.hasChanges());

        UserDataDiff.Result noChangeDiff = new UserDataDiff(
            newAttributes,
            newAttributes,
            Collections.<String, Set<String>>emptyMap(),
            Collections.<String, Set<String>>emptyMap()
        )
            .result;

        assertEquals(new HashMap<String, UserAttribute>(), noChangeDiff.addedAttributes);
        assertEquals(new HashMap<String, UserAttribute>(), noChangeDiff.removedAttributes);
        assertEquals(new HashMap<String, Set<String>>(), noChangeDiff.addedTags);
        assertEquals(new HashMap<String, Set<String>>(), noChangeDiff.removedTags);
        assertFalse(noChangeDiff.hasChanges());
    }

    @Test
    public void testTagsDiff() {
        Map<String, Set<String>> newCollections = new HashMap<>();
        newCollections.put("newtags", set("added", "collection"));
        newCollections.put("added_one", set("foo", "bar"));
        newCollections.put("removed_one", set("bar"));
        newCollections.put("updated_one", set("foo", "baz"));
        newCollections.put("unchanged", set("foo"));

        Map<String, Set<String>> oldCollections = new HashMap<>();
        oldCollections.put("removed", set("remo", "ved"));
        oldCollections.put("added_one", set("foo"));
        oldCollections.put("removed_one", set("foo", "bar"));
        oldCollections.put("updated_one", set("foo", "bar"));
        oldCollections.put("unchanged", set("foo"));

        UserDataDiff.Result diff = new UserDataDiff(
            Collections.<String, UserAttribute>emptyMap(),
            Collections.<String, UserAttribute>emptyMap(),
            newCollections,
            oldCollections
        )
            .result;

        Map<String, Set<String>> expectedAddedTags = new HashMap<>();
        expectedAddedTags.put("updated_one", set("baz"));
        expectedAddedTags.put("added_one", set("bar"));
        expectedAddedTags.put("newtags", set("added", "collection"));

        Map<String, Set<String>> expectedRemovedTags = new HashMap<>();
        expectedRemovedTags.put("updated_one", set("bar"));
        expectedRemovedTags.put("removed", set("ved", "remo"));
        expectedRemovedTags.put("removed_one", set("foo"));

        assertEquals(expectedAddedTags, diff.addedTags);
        assertEquals(expectedRemovedTags, diff.removedTags);
        assertEquals(new HashMap<String, UserAttribute>(), diff.addedAttributes);
        assertEquals(new HashMap<String, UserAttribute>(), diff.removedAttributes);
        assertTrue(diff.hasChanges());

        UserDataDiff.Result noChangesDiff = new UserDataDiff(
            Collections.<String, UserAttribute>emptyMap(),
            Collections.<String, UserAttribute>emptyMap(),
            newCollections,
            newCollections
        )
            .result;
        assertEquals(new HashMap<String, UserAttribute>(), noChangesDiff.addedAttributes);
        assertEquals(new HashMap<String, UserAttribute>(), noChangesDiff.removedAttributes);
        assertEquals(new HashMap<String, Set<String>>(), noChangesDiff.addedTags);
        assertEquals(new HashMap<String, Set<String>>(), noChangesDiff.removedTags);
        assertFalse(noChangesDiff.hasChanges());
    }

    @Test
    public void testEventSerialization() throws JSONException {
        Map<String, Set<String>> newCollections = new HashMap<>();
        newCollections.put("newtags", set("new"));

        Map<String, Set<String>> oldCollections = new HashMap<>();
        oldCollections.put("oldtags", set("old"));

        Map<String, UserAttribute> newAttributes = new HashMap<>();
        newAttributes.put("c.new", new UserAttribute("newvalue", AttributeType.STRING));

        long attributeTimestamp = 123456000;

        Map<String, UserAttribute> oldAttributes = new HashMap<>();
        oldAttributes.put("c.old", new UserAttribute(new Date(attributeTimestamp), AttributeType.DATE));

        UserDataDiff.Result diff = new UserDataDiff(newAttributes, oldAttributes, newCollections, oldCollections)
            .result;

        JSONObject serialized = diff.toEventParameters(2);

        JSONObject expectedAdded = new JSONObject();
        expectedAdded.put("new.s", "newvalue");
        expectedAdded.put("t.newtags", new JSONArray(set("new")));

        JSONObject expectedRemoved = new JSONObject();
        expectedRemoved.put("old.t", attributeTimestamp);
        expectedRemoved.put("t.oldtags", new JSONArray(set("old")));

        assertEquals(2, serialized.getLong("version"));
        assertEquals(expectedAdded.toString(), serialized.getJSONObject("added").toString());
        assertEquals(expectedRemoved.toString(), serialized.getJSONObject("removed").toString());
    }

    private Set<String> set(String... strings) {
        if (strings == null) {
            return Collections.emptySet();
        }

        return new HashSet<>(Arrays.asList(strings));
    }
}
