package com.batch.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.core.Parameters;
import com.batch.android.core.Webservice;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test Batch webservice global response reader
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BatchWebserviceTest {

    private Context appContext;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
    }

    /**
     * Test failure when header is missing
     *
     * @throws Exception
     */
    @Test
    public void testMissingHeader() throws Exception {
        MockBatchWebservice ws = new MockBatchWebservice(appContext, MockBatchWebservice.State.MISSING_HEADER);

        try {
            ws.getBodyIsValid();
            fail("The webservice shouldn't suceeed");
        } catch (JSONException | Webservice.WebserviceError e) {
            assertNotNull(e);
        }
    }

    /**
     * Test failure when header status is invalid
     *
     * @throws Exception
     */
    @Test
    public void testInvalidHeader() throws Exception {
        MockBatchWebservice ws = new MockBatchWebservice(appContext, MockBatchWebservice.State.INVALID_HEADER);

        try {
            ws.getBodyIsValid();
            fail("The webservice shouldn't suceeed");
        } catch (JSONException | Webservice.WebserviceError e) {
            assertNotNull(e);
        }
    }

    /**
     * Test failure on missing body
     *
     * @throws Exception
     */
    @Test
    public void testMissingBody() throws Exception {
        MockBatchWebservice ws = new MockBatchWebservice(appContext, MockBatchWebservice.State.MISSING_BODY);

        try {
            ws.getBodyIsValid();
            fail("The webservice shouldn't suceeed");
        } catch (JSONException | Webservice.WebserviceError e) {
            assertNotNull(e);
        }
    }

    /**
     * Test success when response is OK
     *
     * @throws Exception
     */
    @Test
    public void testComplete() throws Exception, Webservice.WebserviceError {
        MockBatchWebservice ws = new MockBatchWebservice(appContext, MockBatchWebservice.State.COMPLETE);
        assertNotNull(ws.getBodyIsValid());
    }

    /**
     * Test default post parameters
     *
     * @throws Exception
     */
    @Test
    public void testDefaultPostParameters() throws Exception {
        MockBatchWebservice ws = new MockBatchWebservice(appContext, MockBatchWebservice.State.COMPLETE);

        JSONObject postParams = ws.getDefaultPostParameters();
        assertNotNull(postParams);

        /*
         * Get ids
         */
        JSONObject postParamsIds = postParams.getJSONObject("ids");
        assertNotNull(postParamsIds);
    }

    /**
     * Test default headers
     *
     * @throws Exception
     */
    @Test
    public void testDefaultHeaders() throws Exception {
        MockBatchWebservice ws = new MockBatchWebservice(appContext, MockBatchWebservice.State.COMPLETE);

        Map<String, String> headers = ws.getDefaultHeaders();
        assertNotNull(headers);

        /*
         * Gzip
         */
        assertNotNull(headers.get("Accept-Encoding"));
        assertEquals("gzip", headers.get("Accept-Encoding"));

        /*
         * UA
         */
        assertNotNull(headers.get("UserAgent"));
        assertNotNull(headers.get("x-UserAgent"));

        /*
         * AL
         */
        assertNotNull(headers.get("Accept-Language"));
    }

    /**
     * Test plugin and wrapper user agent
     *
     * @throws Exception
     */
    @Test
    public void testPluginUserAgent() throws Exception {
        final String pluginVersion = "Plugin/0.1";
        final String bridgeVersion = "Bridge/0.1";
        final String expectedUserAgentPrefix = String.format("%s %s ", pluginVersion, bridgeVersion);

        System.setProperty(Parameters.PLUGIN_VERSION_ENVIRONEMENT_VAR, pluginVersion);
        System.setProperty(Parameters.BRIDGE_VERSION_ENVIRONEMENT_VAR, bridgeVersion);

        MockBatchWebservice ws = new MockBatchWebservice(appContext, MockBatchWebservice.State.COMPLETE);
        Map<String, String> headers;
        String userAgent;

        // Check that the User agent starts with "PluginVersion BridgeVersion "

        headers = ws.getDefaultHeaders();
        assertNotNull(headers);

        userAgent = headers.get("UserAgent");
        assertNotNull(userAgent);
        assertEquals(userAgent, headers.get("x-UserAgent"));

        assertTrue(userAgent.startsWith(expectedUserAgentPrefix));
        assertFalse(userAgent.equals(expectedUserAgentPrefix));

        // Check that the User agent isn't prefixed anymore if we remove the env variables

        System.clearProperty(Parameters.PLUGIN_VERSION_ENVIRONEMENT_VAR);
        System.clearProperty(Parameters.BRIDGE_VERSION_ENVIRONEMENT_VAR);

        ws = new MockBatchWebservice(appContext, MockBatchWebservice.State.COMPLETE);
        headers = ws.getDefaultHeaders();
        assertNotNull(headers);

        userAgent = headers.get("UserAgent");
        assertNotNull(userAgent);

        assertFalse(userAgent.startsWith(expectedUserAgentPrefix));
    }

    @Test
    public void testStatus() throws Exception {
        assertEquals(MockBatchWebservice.get500ErrorReason(), MockBatchWebservice.getResponseErrorCause(500));
        assertEquals(MockBatchWebservice.get404ErrorReason(), MockBatchWebservice.getResponseErrorCause(404));
    }
}
