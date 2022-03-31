package com.batch.android.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * Test for ParametersPostDataProvider
 *
 */
public class ParametersPostDataProviderTest {

    /**
     * Test that extracted data can be read
     *
     * @throws Exception
     */
    @Test
    public void testReadData() throws Exception {
        String key = "key";
        String value = "value";

        Map<String, String> input = new HashMap<>();
        input.put(key, value);

        ParametersPostDataProvider provider = new ParametersPostDataProvider(input);
        assertEquals("Given input is not equals to raw data returned", input, provider.getRawData());

        byte[] data = provider.getData();
        assertEquals("decoded data is not equals to input", key + "=" + value, new String(data));
    }

    @Test
    public void testIsEmpty() {
        Map<String, String> input = new HashMap<>();
        ParametersPostDataProvider provider = new ParametersPostDataProvider(input);
        assertTrue(provider.isEmpty());

        input.put("key", "value");
        provider = new ParametersPostDataProvider(input);
        assertFalse(provider.isEmpty());
    }
}
