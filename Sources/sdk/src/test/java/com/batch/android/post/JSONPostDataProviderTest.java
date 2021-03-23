package com.batch.android.post;

import com.batch.android.json.JSONObject;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for JSONPostDataProvider
 *
 */
public class JSONPostDataProviderTest
{
    /**
     * Test that extracted data can be read
     *
     * @throws Exception
     */
    @Test
    public void testReadData() throws Exception
    {
        String key = "key";
        String value = "value";

        JSONObject input = new JSONObject();
        input.put(key, value);

        JSONPostDataProvider provider = new JSONPostDataProvider(input);
        assertTrue("Given input is not equals to raw data returned",
                areEquals(input, provider.getRawData()));

        byte[] data = provider.getData();
        assertNotNull("data returned is null", data);

        assertTrue("decoded data is not equals to input",
                areEquals(input, new JSONObject(new String(data))));
    }

    /**
     * Are those 2 json object equals
     *
     * @param obj1
     * @param obj2
     * @return
     */
    private boolean areEquals(JSONObject obj1, JSONObject obj2)
    {
        if (obj1 == obj2) {
            return true;
        }

        return obj1.toString().equals(obj2.toString());
    }
}
