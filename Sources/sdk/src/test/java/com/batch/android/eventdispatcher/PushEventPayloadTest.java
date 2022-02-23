package com.batch.android.eventdispatcher;

import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.BatchPushPayload;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.Iterator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PushEventPayloadTest {

    private static Bundle jsonStringToBundle(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return jsonToBundle(jsonObject);
        } catch (JSONException ignored) {}

        return null;
    }

    private static Bundle jsonToBundle(JSONObject jsonObject) throws JSONException {
        Bundle bundle = new Bundle();
        Iterator iter = jsonObject.keys();

        while (iter.hasNext()) {
            String key = (String) iter.next();
            String value = jsonObject.getString(key);
            bundle.putString(key, value);
        }

        return bundle;
    }

    @Test
    public void testValidData() {
        String stringPayload =
            "{\"google.delivered_priority\":\"high\",\"google.sent_time\":1569598588132,\"google.ttl\":2419200,\"google.original_priority\":\"high\",\"msg\":\"test swag transac 4\",\"from\":\"519597932539\",\"title\":\"test Transac #4\",\"google.message_id\":\"0:1569598588136037%1e91dabb5c13d641\",\"com.batch\":\"{\\\"t\\\":\\\"t\\\",\\\"od\\\":{\\\"n\\\":\\\"925a4e70-e13c-11e9-bbd4-cf7d44429d5e\\\"},\\\"i\\\":\\\"Testtransac4-1569598588\\\",\\\"l\\\":\\\"https://batch.com/test?utm_campaign=test&utm_content=content-id-1\\\"}\",\"collapse_key\":\"default\",\"utm_source\":\"salkutsaut\"}";
        Bundle payload = jsonStringToBundle(stringPayload);

        Assert.assertNotNull(payload);

        BatchPushPayload pushPayload = null;
        try {
            pushPayload = BatchPushPayload.payloadFromReceiverExtras(payload);
        } catch (BatchPushPayload.ParsingException e) {
            Assert.fail(e.getMessage());
        }
        PushEventPayload eventPayload = new PushEventPayload(pushPayload);

        Assert.assertEquals(
            eventPayload.getDeeplink(),
            "https://batch.com/test?utm_campaign=test&utm_content=content-id-1"
        );
        Assert.assertNull(eventPayload.getTrackingId());
        Assert.assertEquals(eventPayload.getCustomValue("utm_source"), "salkutsaut");
        Assert.assertNull(eventPayload.getCustomValue("com.batch"));
        Assert.assertNotNull(eventPayload.getPushPayload());
        Assert.assertNull(eventPayload.getMessagingPayload());
        Assert.assertFalse(eventPayload.isPositiveAction());

        PushEventPayload eventPayload2 = new PushEventPayload(pushPayload, true);
        Assert.assertTrue(eventPayload2.isPositiveAction());
    }
}
