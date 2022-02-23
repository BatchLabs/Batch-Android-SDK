package com.batch.android.actions;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.P;
import static org.mockito.ArgumentMatchers.eq;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import com.batch.android.Batch;
import com.batch.android.di.DITestUtils;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.module.ActionModule;
import com.batch.android.module.UserModule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.annotation.Config;
import org.robolectric.res.android.Asset;
import org.robolectric.shadows.ShadowLog;

@RunWith(AndroidJUnit4.class)
@MediumTest
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*" })
@PrepareForTest({ UserModule.class })
public class UserEventActionTest {

    private Context context;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;

        context = ApplicationProvider.getApplicationContext();

        RuntimeManagerProvider.get().setContext(context);
    }

    @Test
    public void testTrackEventAction() throws JSONException {
        ActionModule actionModule = new ActionModule();
        UserModule userModule = DITestUtils.mockSingletonDependency(UserModule.class, null);

        PowerMockito
            .doNothing()
            .when(userModule)
            .trackPublicEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(JSONObject.class));

        ArgumentCaptor<JSONObject> eventDataCaptor = ArgumentCaptor.forClass(JSONObject.class);

        String eventJSON = "{'e':'event_test', 'l':'label_test'}";
        actionModule.performAction(context, "batch.user.event", new JSONObject(eventJSON), null);
        Mockito.verify(userModule).trackPublicEvent(eq("event_test"), eq("label_test"), eventDataCaptor.capture());

        JSONObject eventData = eventDataCaptor.getValue();
        Assert.assertNotNull(eventData);
        Assert.assertNotNull(eventData.getJSONObject("attributes"));
        Assert.assertTrue(eventData.getJSONObject("attributes").keySet().isEmpty());
        Assert.assertNotNull(eventData.getJSONArray("tags"));
        Assert.assertEquals(0, eventData.getJSONArray("tags").length());
    }

    @Test
    public void testTrackEventWithoutLabelAction() throws JSONException {
        ActionModule actionModule = new ActionModule();
        UserModule userModule = DITestUtils.mockSingletonDependency(UserModule.class, null);

        PowerMockito
            .doNothing()
            .when(userModule)
            .trackPublicEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(JSONObject.class));

        ArgumentCaptor<JSONObject> eventDataCaptor = ArgumentCaptor.forClass(JSONObject.class);

        String eventJSON = "{'e':'event_test'}";
        actionModule.performAction(context, "batch.user.event", new JSONObject(eventJSON), null);
        Mockito.verify(userModule).trackPublicEvent(eq("event_test"), eq(null), eventDataCaptor.capture());

        JSONObject eventData = eventDataCaptor.getValue();
        Assert.assertNotNull(eventData);
        Assert.assertNotNull(eventData.getJSONObject("attributes"));
        Assert.assertTrue(eventData.getJSONObject("attributes").keySet().isEmpty());
        Assert.assertNotNull(eventData.getJSONArray("tags"));
        Assert.assertEquals(0, eventData.getJSONArray("tags").length());
    }

    @Test
    public void testTrackEventWithTagsAction() throws JSONException {
        ActionModule actionModule = new ActionModule();
        UserModule userModule = DITestUtils.mockSingletonDependency(UserModule.class, null);

        PowerMockito
            .doNothing()
            .when(userModule)
            .trackPublicEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(JSONObject.class));

        ArgumentCaptor<JSONObject> eventDataCaptor = ArgumentCaptor.forClass(JSONObject.class);

        String eventJSON = "{'e':'event_test', 'l':'label_test', 't':['tag1', 'tag2', 'tag3']}";
        actionModule.performAction(context, "batch.user.event", new JSONObject(eventJSON), null);
        Mockito.verify(userModule).trackPublicEvent(eq("event_test"), eq("label_test"), eventDataCaptor.capture());

        JSONObject eventData = eventDataCaptor.getValue();
        Assert.assertNotNull(eventData);
        Assert.assertNotNull(eventData.getJSONObject("attributes"));
        Assert.assertTrue(eventData.getJSONObject("attributes").keySet().isEmpty());
        Assert.assertNotNull(eventData.getJSONArray("tags"));
        Assert.assertEquals(3, eventData.getJSONArray("tags").length());
        Assert.assertEquals("tag1", eventData.getJSONArray("tags").optString(0));
        Assert.assertEquals("tag2", eventData.getJSONArray("tags").optString(1));
        Assert.assertEquals("tag3", eventData.getJSONArray("tags").optString(2));
    }

    @Test
    @Config(sdk = { JELLY_BEAN_MR2, P })
    public void testTrackEventWithAttrAction() throws JSONException {
        ActionModule actionModule = new ActionModule();
        UserModule userModule = DITestUtils.mockSingletonDependency(UserModule.class, null);

        PowerMockito
            .doNothing()
            .when(userModule)
            .trackPublicEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(JSONObject.class));

        ArgumentCaptor<JSONObject> eventDataCaptor = ArgumentCaptor.forClass(JSONObject.class);

        String eventJSON =
            "{'e':'event_test', 'l':'label_test', 'a':{'bool':true, 'int':64, 'double': 68987.256, 'string':'tototo', 'date': '2020-08-09T12:12:23.943Z'}}}";
        actionModule.performAction(context, "batch.user.event", new JSONObject(eventJSON), null);
        Mockito.verify(userModule).trackPublicEvent(eq("event_test"), eq("label_test"), eventDataCaptor.capture());

        JSONObject eventData = eventDataCaptor.getValue();
        Assert.assertNotNull(eventData);

        Assert.assertNotNull(eventData.getJSONObject("attributes"));
        Assert.assertTrue(eventData.getJSONObject("attributes").optBoolean("bool.b"));
        Assert.assertEquals(64, eventData.getJSONObject("attributes").optInt("int.i"));
        Assert.assertEquals(68987.256, eventData.getJSONObject("attributes").optDouble("double.f"), 0);
        Assert.assertEquals("tototo", eventData.getJSONObject("attributes").optString("string.s"));
        Assert.assertEquals(1596975143943L, eventData.getJSONObject("attributes").optLong("date.t"));

        Assert.assertNotNull(eventData.getJSONArray("tags"));
        Assert.assertEquals(0, eventData.getJSONArray("tags").length());
    }
}
