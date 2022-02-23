package com.batch.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.core.WebserviceErrorCause;
import com.batch.android.json.JSONObject;
import java.net.MalformedURLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test {@link BatchWebservice#onRetry(WebserviceErrorCause)}
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BatchWebserviceRetryTest {

    private Context appContext;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
    }

    /**
     * Test that retry is null on init
     *
     * @throws Exception
     */
    @Test
    public void testNoRetry() throws Exception {
        BatchWebserviceTest ws = new BatchWebserviceTest(appContext);

        JSONObject json = ws.getPostDataProvider().getRawData();
        assertEquals(0, json.getInt("rc"));
        assertFalse(json.has("lastFail"));
    }

    /**
     * Test counter and causes of retry
     *
     * @throws Exception
     */
    @Test
    public void testRetry() throws Exception {
        BatchWebserviceTest ws = new BatchWebserviceTest(appContext);

        // First retry
        ws.onRetry(WebserviceErrorCause.NETWORK_TIMEOUT);
        JSONObject json = ws.getPostDataProvider().getRawData();

        assertEquals(1, json.getInt("rc"));
        assertEquals(
            WebserviceErrorCause.NETWORK_TIMEOUT.toString(),
            json.getJSONObject("lastFail").getString("cause")
        );

        // Second retry
        ws.onRetry(WebserviceErrorCause.NETWORK_TIMEOUT);
        json = ws.getPostDataProvider().getRawData();

        assertEquals(2, json.getInt("rc"));
        assertEquals(
            WebserviceErrorCause.NETWORK_TIMEOUT.toString(),
            json.getJSONObject("lastFail").getString("cause")
        );

        // Third retry
        ws.onRetry(WebserviceErrorCause.PARSING_ERROR);
        json = ws.getPostDataProvider().getRawData();

        assertEquals(3, json.getInt("rc"));
        assertEquals(WebserviceErrorCause.PARSING_ERROR.toString(), json.getJSONObject("lastFail").getString("cause"));
    }

    // ------------------------------------------>

    /**
     * Stub Batch webservice
     *
     */
    public static class BatchWebserviceTest extends BatchWebservice {

        protected BatchWebserviceTest(Context context) throws MalformedURLException {
            super(context, RequestType.POST, "http://www.test.com/");
        }

        @Override
        protected String getPropertyParameterKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getURLSorterPatternParameterKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getCryptorTypeParameterKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getCryptorModeParameterKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getPostCryptorTypeParameterKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getReadCryptorTypeParameterKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getSpecificConnectTimeoutKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getSpecificReadTimeoutKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getSpecificRetryCountKey() {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
