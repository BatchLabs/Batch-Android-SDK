package com.batch.android;

import android.app.Activity;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import com.batch.android.json.JSONObject;
import java.net.MalformedURLException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test opt-out of sensible parameters
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParamsOptoutTest {

    private static final String apiKey = "apiKey";

    @Rule
    public ActivityTestRule<TestActivity> activityRule = new ActivityTestRule<>(TestActivity.class, false, true);

    /**
     * Reference of the activity context
     */
    private Activity activity;

    // ----------------------------------------->

    @Before
    public void setUp() throws Exception {
        /*
        activity = activityRule.getActivity();

        Batch.setConfig(new Config(apiKey).setCanUseAdvertisingID(false).setCanUseAndroidID(false));
        Batch.onStart(activity); */// FIXME
    }

    // ------------------------------------------>

    /**
     * Test opt-out of androidid
     *
     * @throws Exception
     */
    @Test
    public void testAndroidIDOptout() throws Exception {
        /*JSONObject params = new TestWebservice(activity).getPostData().getJSONObject("ids");

		assertFalse(params.has(SystemParameterShortName.ANDROID_ID.shortName));
		assertTrue(params.isNull(SystemParameterShortName.ANDROID_ID.shortName));

		params = new TestWebservice(activity).getPostData();
		
		assertFalse(params.has(SystemParameterShortName.ANDROID_ID.shortName));	*///FIXME repair that test
    }

    /**
     * Test opt-out of advertising id
     *
     * @throws Exception
     */
    @Test
    public void testAdvertisingIDOptout() throws Exception {
        /*Thread.sleep(2000); // Wait for Advertising ID to popup
		
		JSONObject params = new TestWebservice(activity).getPostData().getJSONObject("ids");
		
		assertFalse(params.has(SystemParameterShortName.ADVERTISING_ID.shortName));
		assertTrue(params.isNull(SystemParameterShortName.ADVERTISING_ID.shortName));

		params = new TestWebservice(activity).getPostData();
		
		assertFalse(params.has(SystemParameterShortName.ADVERTISING_ID.shortName));*///FIXME repair that test
    }

    // --------------------------------------------->

    public static class TestWebservice extends BatchWebservice {

        public TestWebservice(Context context) throws MalformedURLException {
            super(context, RequestType.POST, "http://test.com/");
        }

        public JSONObject getPostData() {
            return super.getPostDataProvider().getRawData();
        }

        // ----------------------------------------------->

        @Override
        protected String getPropertyParameterKey() {
            return null;
        }

        @Override
        protected String getURLSorterPatternParameterKey() {
            return null;
        }

        @Override
        protected String getCryptorTypeParameterKey() {
            return null;
        }

        @Override
        protected String getCryptorModeParameterKey() {
            return null;
        }

        @Override
        protected String getPostCryptorTypeParameterKey() {
            return null;
        }

        @Override
        protected String getReadCryptorTypeParameterKey() {
            return null;
        }

        @Override
        protected String getSpecificConnectTimeoutKey() {
            return null;
        }

        @Override
        protected String getSpecificReadTimeoutKey() {
            return null;
        }

        @Override
        protected String getSpecificRetryCountKey() {
            return null;
        }
    }
}
