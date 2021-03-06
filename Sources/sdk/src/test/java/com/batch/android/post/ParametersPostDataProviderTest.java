package com.batch.android.post;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Test for ParametersPostDataProvider
 *
 */
public class ParametersPostDataProviderTest
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

        Map<String, String> input = new HashMap<>();
        input.put(key, value);

        ParametersPostDataProvider provider = new ParametersPostDataProvider(input);
        assertEquals("Given input is not equals to raw data returned",
                input,
                provider.getRawData());

        byte[] data = provider.getData();
        assertEquals("decoded data is not equals to input", key + "=" + value, new String(data));
    }
}
