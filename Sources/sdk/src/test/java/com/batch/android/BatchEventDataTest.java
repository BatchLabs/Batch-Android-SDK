package com.batch.android;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BatchEventDataTest {

    @Test
    public void testValidData() throws JSONException, URISyntaxException {
        URI uri = new URI("batch://batch.com");

        BatchEventData data = new BatchEventData();
        data.addTag("FOO");
        data.addTag("bAr");
        data.addTag("baz");

        data.put("INT", 1);
        data.put("LONG", 1L);
        data.put("FLOAT", 1.0f);
        data.put("DOUBLE", 1.0d);
        data.put("BOOL", true);
        data.put("STRING", "foobar");
        data.put("123", " 456 ");
        data.put("now", new Date(1589466748930L));
        data.put("url", uri);

        JSONObject json = data.toInternalJSON();

        List<String> tags = jsonArrayToList(json.getJSONArray("tags"));
        Assert.assertTrue(tags.contains("foo"));
        Assert.assertTrue(tags.contains("bar"));
        Assert.assertTrue(tags.contains("baz"));

        JSONObject values = json.getJSONObject("attributes");

        Assert.assertEquals(1, values.get("int.i"));
        Assert.assertEquals(1L, values.get("long.i"));
        Assert.assertEquals(1.0f, values.get("float.f"));
        Assert.assertEquals(1.0d, values.get("double.f"));
        Assert.assertEquals(true, values.get("bool.b"));
        Assert.assertEquals("foobar", (String) values.get("string.s"));
        Assert.assertEquals(" 456 ", (String) values.get("123.s"));
        Assert.assertEquals(1589466748930L, values.get("now.t"));
        Assert.assertEquals(uri, values.get("url.u"));

        Assert.assertEquals(null, json.opt("converted"));
    }

    @Test
    public void testSizeLimits() throws JSONException {
        BatchEventData data = new BatchEventData();

        for (int i = 0; i < 20; i++) {
            data.addTag(Integer.toString(i));
            data.put(Integer.toString(i), i);
        }

        JSONObject json = data.toInternalJSON();
        JSONArray tags = json.getJSONArray("tags");
        JSONObject values = json.getJSONObject("attributes");
        Assert.assertEquals(10, tags.length());
        Assert.assertEquals(15, values.length());
    }

    @Test
    public void testUpdateWhenFull() {
        BatchEventData data = new BatchEventData();

        data.put("hip1", "hop");
        data.put("hip2", "hop");
        data.put("hip3", "hop");
        data.put("hip4", "hop");
        data.put("hip5", "hop");
        data.put("hip6", "hop");
        data.put("hip7", "hop");
        data.put("hip8", "hop");
        data.put("hip9", "hop");
        data.put("hip10", "hop");
        data.put("hip11", "hop");
        data.put("hip12", "hop");
        data.put("hip13", "hop");
        data.put("hip14", "hop");
        data.put("hip15", "hop");
        data.put("hip16", "hop");

        Map<String, BatchEventData.TypedAttribute> attr = data.getAttributes();
        Assert.assertEquals(attr.get("hip1").value, "hop");
        Assert.assertEquals(attr.get("hip2").value, "hop");
        Assert.assertEquals(attr.get("hip3").value, "hop");
        Assert.assertEquals(attr.get("hip4").value, "hop");
        Assert.assertEquals(attr.get("hip5").value, "hop");
        Assert.assertEquals(attr.get("hip6").value, "hop");
        Assert.assertEquals(attr.get("hip7").value, "hop");
        Assert.assertEquals(attr.get("hip8").value, "hop");
        Assert.assertEquals(attr.get("hip9").value, "hop");
        Assert.assertEquals(attr.get("hip10").value, "hop");
        Assert.assertEquals(attr.get("hip11").value, "hop");
        Assert.assertEquals(attr.get("hip12").value, "hop");
        Assert.assertEquals(attr.get("hip13").value, "hop");
        Assert.assertEquals(attr.get("hip14").value, "hop");
        Assert.assertEquals(attr.get("hip15").value, "hop");

        data.put("hip5", "test");
        attr = data.getAttributes();
        Assert.assertEquals(attr.get("hip5").value, "test");
    }

    @Test
    public void testInvalidData() throws JSONException {
        BatchEventData data = new BatchEventData();

        data.addTag(
            "A way too long string that goes for quiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiite too long" +
            "Lorem ipsum dolor and other various stuff."
        );
        data.addTag("");

        data.put(
            "string",
            "A way too long string that goes for quiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiite too long" +
            "Lorem ipsum dolor and other various stuff."
        );
        data.put("invalid_key%%%", "foobar");
        data.put("key_that_is_too_long_really_it_should_be_more_than_thirty_chars", "foobar");
        data.put("null_date", (Date) null);

        JSONObject json = data.toInternalJSON();
        JSONArray tags = json.getJSONArray("tags");
        JSONObject values = json.getJSONObject("attributes");
        Assert.assertEquals(0, tags.length());
        Assert.assertEquals(0, values.length());
    }

    @Test
    public void testLegacyDataConversion() throws JSONException, URISyntaxException {
        JSONObject legacyData = new JSONObject();
        URI uri = new URI("batch://batch.com");

        // Don't remove the casts or the test fails due to JSONObject weirdness
        legacyData.put("int", 1);
        legacyData.put("long", 1L);
        legacyData.put("float", (Float) 1.0f);
        legacyData.put("double", (Double) 1.0d);
        legacyData.put("bool", true);
        legacyData.put("string", "foobar");
        legacyData.put("url", uri);

        BatchEventData data = new BatchEventData(legacyData);
        JSONObject json = data.toInternalJSON();

        Assert.assertEquals(0, json.getJSONArray("tags").length());

        JSONObject values = json.getJSONObject("attributes");

        Assert.assertEquals(1, values.get("int.i"));
        Assert.assertEquals(1L, values.get("long.i"));
        Assert.assertEquals(1.0f, values.get("float.f"));
        Assert.assertEquals(1.0d, values.get("double.f"));
        Assert.assertEquals(true, values.get("bool.b"));
        Assert.assertEquals("foobar", (String) values.get("string.s"));
        Assert.assertEquals(uri, (URI) values.get("url.u"));

        Assert.assertTrue(json.getBoolean("converted"));
    }

    @Test
    public void testLegacyDataConversionOrdering() throws JSONException {
        // This test checks that the first 10 legacy array keys are picked in a predictable way
        // They should be ordered

        String value = "test";

        List<String> unorderedKeys = new ArrayList<>(20);
        unorderedKeys.add("drLAjNhvYs");
        unorderedKeys.add("wNMFqBvSHe");
        unorderedKeys.add("xZivnkZdZv");
        unorderedKeys.add("ZEZVbaXwDD");
        unorderedKeys.add("tvwZZnHsoJ");
        unorderedKeys.add("nCDiIffIqq");
        unorderedKeys.add("bXybuzBSvX");
        unorderedKeys.add("uImQWnrAyw");
        unorderedKeys.add("dIHDhyyDsk");
        unorderedKeys.add("AEBVYnPTuo");
        unorderedKeys.add("jfzUsSnTDf");
        unorderedKeys.add("vhochDgxOB");
        unorderedKeys.add("bJZgGgwKIM");
        unorderedKeys.add("GvdPlhWfyT");
        unorderedKeys.add("HQiXZQNHLs");
        unorderedKeys.add("wUGgNuvdTY");
        unorderedKeys.add("JHLZaOOoBQ");
        unorderedKeys.add("vemRXpXcUK");
        unorderedKeys.add("MEiAzZWjga");
        unorderedKeys.add("FViUCTCzfE");

        JSONObject legacyData = new JSONObject();
        for (String key : unorderedKeys) {
            legacyData.put(key, value);
        }

        BatchEventData data = new BatchEventData(legacyData);
        JSONObject json = data.toInternalJSON();

        JSONObject values = json.getJSONObject("attributes");
        Assert.assertEquals(15, values.length());

        // Dicts are not ordered, so we need to sort the keys beforehand, and check if they're all there

        List<String> expectedConvertedKeys = new ArrayList<>(unorderedKeys);
        expectedConvertedKeys.sort((o1, o2) -> o1.toLowerCase(Locale.US).compareTo(o2.toLowerCase(Locale.US)));
        expectedConvertedKeys = expectedConvertedKeys.subList(0, 14);

        for (String key : expectedConvertedKeys) {
            Assert.assertNotNull(values.opt(key.toLowerCase(Locale.US) + ".s"));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> jsonArrayToList(JSONArray json) throws JSONException {
        final List<T> array = new ArrayList<>();

        for (int i = 0; i < json.length(); i++) {
            array.add((T) json.get(i));
        }

        return array;
    }
}
