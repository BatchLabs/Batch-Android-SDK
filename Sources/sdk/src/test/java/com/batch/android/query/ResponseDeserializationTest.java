package com.batch.android.query;

import com.batch.android.json.JSONException;
import com.batch.android.query.response.AttributesCheckResponse;
import com.batch.android.query.response.AttributesSendResponse;
import com.batch.android.query.response.PushResponse;
import com.batch.android.query.response.StartResponse;
import com.batch.android.query.response.TrackingResponse;
import com.batch.android.query.serialization.deserializers.AttributesCheckResponseDeserializer;
import com.batch.android.query.serialization.deserializers.AttributesSendResponseDeserializer;
import com.batch.android.query.serialization.deserializers.PushResponseDeserializer;
import com.batch.android.query.serialization.deserializers.StartResponseDeserializer;
import com.batch.android.query.serialization.deserializers.TrackingResponseDeserializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ResponseDeserializationTest {

    private ResponseFactory factory;

    @Before
    public void setUp() {
        factory = new ResponseFactory();
    }

    @Test
    public void testAttributesCheckResponseDeserializer() throws JSONException {
        AttributesCheckResponseDeserializer deserializer = new AttributesCheckResponseDeserializer(
            factory.createJsonAttributesCheckResponse()
        );
        AttributesCheckResponse response = deserializer.deserialize();
        Assert.assertEquals("dummy_id", response.getQueryID());
        Assert.assertEquals(AttributesCheckResponse.Action.RECHECK, response.getAction());
        Assert.assertEquals(Long.valueOf(1499960145L), response.getTime());
        Assert.assertEquals(1L, response.getVersion());
    }

    @Test
    public void testAttributesSendResponseDeserializer() throws JSONException {
        AttributesSendResponseDeserializer deserializer = new AttributesSendResponseDeserializer(
            factory.createJsonAttributesSendResponse()
        );
        AttributesSendResponse response = deserializer.deserialize();
        Assert.assertEquals("dummy_id", response.getQueryID());
        Assert.assertEquals("1234-1234-1234", response.transactionID);
        Assert.assertEquals(1L, response.version);
    }

    @Test
    public void testPushResponseDeserializer() throws JSONException {
        PushResponseDeserializer deserializer = new PushResponseDeserializer(factory.createJsonPushResponse());
        PushResponse response = deserializer.deserialize();
        Assert.assertEquals("dummy_id", response.getQueryID());
    }

    @Test
    public void testStartResponseDeserializer() throws JSONException {
        StartResponseDeserializer deserializer = new StartResponseDeserializer(factory.createJsonStartResponse());
        StartResponse response = deserializer.deserialize();
        Assert.assertEquals("dummy_id", response.getQueryID());
    }

    @Test
    public void testTrackingResponseDeserializer() throws JSONException {
        TrackingResponseDeserializer deserializer = new TrackingResponseDeserializer(factory.createJsonTrackResponse());
        TrackingResponse response = deserializer.deserialize();
        Assert.assertEquals("dummy_id", response.getQueryID());
    }
}
