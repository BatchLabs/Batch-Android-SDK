package com.batch.android.post;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import org.junit.Test;

/**
 * Test for JSONPostDataProvider
 *
 */
public class JSONPostDataProviderTest {

    /**
     * Test that extracted data can be read
     *
     * @throws Exception
     */
    @Test
    public void testReadData() throws Exception {
        String key = "key";
        String value = "value";

        JSONObject input = new JSONObject();
        input.put(key, value);

        JSONPostDataProvider provider = new JSONPostDataProvider(input);
        assertTrue("Given input is not equals to raw data returned", areEquals(input, provider.getRawData()));

        byte[] data = provider.getData();
        assertNotNull("data returned is null", data);

        assertTrue("decoded data is not equals to input", areEquals(input, new JSONObject(new String(data))));
    }

    @Test
    public void testIsEmpty() throws JSONException {
        JSONObject input = new JSONObject();
        JSONPostDataProvider provider = new JSONPostDataProvider(input);
        assertTrue(provider.isEmpty());

        input.put("key", "value");
        provider = new JSONPostDataProvider(input);
        assertFalse(provider.isEmpty());
    }

    /**
     * Are those 2 json object equals
     *
     * @param obj1
     * @param obj2
     * @return
     */
    private boolean areEquals(JSONObject obj1, JSONObject obj2) {
        if (obj1 == obj2) {
            return true;
        }

        return obj1.toString().equals(obj2.toString());
    }
}
