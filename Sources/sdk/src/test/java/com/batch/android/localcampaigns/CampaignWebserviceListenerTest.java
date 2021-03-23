package com.batch.android.localcampaigns;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.batch.android.JSONObjectMockitoMatcher;
import com.batch.android.di.DI;
import com.batch.android.di.DITestUtils;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.query.response.LocalCampaignsResponse;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CampaignWebserviceListenerTest
{
    @After
    public void tearDown()
    {
        DI.reset();
    }

    @Test
    public void testPersistence() throws JSONException
    {
        final Context context = ApplicationProvider.getApplicationContext();
        final CampaignManager campaignManager = DITestUtils.mockSingletonDependency(CampaignManager.class,
                null);

        new LocalCampaignsResponse(context, getErrorPayload(), true);

        new LocalCampaignsResponse(context, getInvalidCampaignTypePayload(), true);
        new LocalCampaignsResponse(context, getEmptyPayload(), true);
        new LocalCampaignsResponse(context, getEmptyWithUnknownKeyPayload(), true);

        new LocalCampaignsResponse(context, getSingleCampaignPayload(), true);
        new LocalCampaignsResponse(context,
                getSinglePersistableCampaignWithTransientPayload(),
                true);

        Mockito.verify(campaignManager, Mockito.times(1)).deleteSavedCampaignsAsync(context);
        Mockito.verify(campaignManager, Mockito.times(3)).saveCampaignsResponseAsync(Mockito.any(),
                JSONObjectMockitoMatcher.eq(new JSONObject("{\"campaigns\": []}")));

        JSONObject expectedPersistedPayload = getSingleCampaignPayload();
        expectedPersistedPayload.remove("id");
        Mockito.verify(campaignManager, Mockito.times(2)).saveCampaignsResponseAsync(Mockito.any(),
                JSONObjectMockitoMatcher.eq(expectedPersistedPayload));
    }

    private JSONObject getErrorPayload() throws JSONException
    {
        return new JSONObject(
                "{\"id\":\"ffbb\",\"error\": {\"code\": 2, \"reason\": \"internal error\"}}");
    }

    private JSONObject getInvalidCampaignTypePayload() throws JSONException
    {
        return new JSONObject("{\"id\":\"ffbb\",\"campaigns\": {}}");
    }

    private JSONObject getEmptyPayload() throws JSONException
    {
        return new JSONObject("{\"id\":\"ffbb\",\"campaigns\": []}");
    }

    private JSONObject getEmptyWithUnknownKeyPayload() throws JSONException
    {
        return new JSONObject("{\"id\":\"ffbb\",\"invalid\":1,\"campaigns\": []}");
    }

    private JSONObject getSingleCampaignPayload() throws JSONException
    {
        return new JSONObject("{\"id\":\"ffbb\",\"campaigns\": [" +
                "    {" +
                "       \"campaignId\": \"25876676\"," +
                "       \"campaignToken\": \"ffed98550583631424ab69225b4f74aa\"," +
                "       \"minimumApiLevel\": 1," +
                "       \"priority\": 1 ," +
                "       \"minDisplayInterval\": 0," +
                "       \"startDate\": { \"ts\": 1672603200000, \"userTZ\": false}," +
                "       \"triggers\": [{ \"type\": \"EVENT\", \"event\": \"_DUMMY_EVENT\" }]," +
                "       \"persist\": true," +
                "       \"eventData\": {}," +
                "       \"output\": {" +
                "           \"type\": \"LANDING\"," +
                "           \"payload\": { \"id\": \"25876676\", \"did\": null, \"ed\": {}, \"kind\": \"_dummy\" }" +
                "       }" +
                "    }" +
                "]}");
    }

    private JSONObject getSinglePersistableCampaignWithTransientPayload() throws JSONException
    {
        return new JSONObject("{\"id\":\"ffbb\",\"campaigns\": [" +
                "    {" +
                "       \"campaignId\": \"25876676\"," +
                "       \"campaignToken\": \"ffed98550583631424ab69225b4f74aa\"," +
                "       \"minimumApiLevel\": 1," +
                "       \"priority\": 1 ," +
                "       \"minDisplayInterval\": 0," +
                "       \"startDate\": { \"ts\": 1672603200000, \"userTZ\": false}," +
                "       \"triggers\": [{ \"type\": \"EVENT\", \"event\": \"_DUMMY_EVENT\" }]," +
                "       \"persist\": true," +
                "       \"eventData\": {}," +
                "       \"output\": {" +
                "           \"type\": \"LANDING\"," +
                "           \"payload\": { \"id\": \"25876676\", \"did\": null, \"ed\": {}, \"kind\": \"_dummy\" }" +
                "       }" +
                "    }," +
                "    {" +
                "       \"campaignId\": \"123\"," +
                "       \"campaignToken\": \"cdef\"," +
                "       \"minimumApiLevel\": 1," +
                "       \"priority\": 1 ," +
                "       \"minDisplayInterval\": 0," +
                "       \"startDate\": { \"ts\": 1672603200000, \"userTZ\": false}," +
                "       \"triggers\": [{ \"type\": \"EVENT\", \"event\": \"_DUMMY_EVENT\" }]," +
                "       \"persist\": false," +
                "       \"eventData\": {}," +
                "       \"output\": {" +
                "           \"type\": \"LANDING\"," +
                "           \"payload\": { \"id\": \"25876676\", \"did\": null, \"ed\": {}, \"kind\": \"_dummy\" }" +
                "       }" +
                "    }" +
                "]}");
    }
}
