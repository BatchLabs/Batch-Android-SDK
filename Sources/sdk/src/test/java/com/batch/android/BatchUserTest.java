package com.batch.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.core.Promise;
import com.batch.android.di.DITest;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.json.JSONObject;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BatchUserTest extends DITest {

    private BatchUserDataEditor editor;
    private Context context;

    @Before
    public void setUp() {
        super.setUp();
        context = ApplicationProvider.getApplicationContext();
        simulateBatchStart(context);
        editor = Batch.User.editor();
    }

    @After
    public void tearDown() {
        super.tearDown();
        //Batch.getUserProfile().setLanguage(null).setRegion(null).setCustomID(null);
    }

    @Test
    public void testSetLanguage() throws Exception {
        BatchUserProfile user = Batch.getUserProfile();
        assertNotNull(user);

        String originalLanguage = user.getLanguage();

        user.setLanguage("tr");

        assertEquals("tr", user.getLanguage());

        user.setLanguage(null);

        assertEquals(originalLanguage, user.getLanguage());
    }

    @Test
    public void testSetRegion() throws Exception {
        BatchUserProfile user = Batch.getUserProfile();
        assertNotNull(user);

        String originalRegion = user.getRegion();

        user.setRegion("BR");

        assertEquals("BR", user.getRegion());

        user.setRegion(null);

        assertEquals(originalRegion, user.getRegion());
    }

    @Test
    public void testCustomID() throws Exception {
        BatchUserProfile user = Batch.getUserProfile();
        assertNotNull(user);

        assertNull(user.getCustomID());

        user.setCustomID("abcd");

        assertEquals("abcd", user.getCustomID());

        user.setCustomID(null);

        assertNull(user.getCustomID());
    }

    @Test
    public void testProfileVersionCustomID() throws Exception {
        BatchUserProfile user = Batch.getUserProfile();
        assertNotNull(user);

        user.setCustomID(null);

        long version = user.getVersion();
        assertTrue(version >= 1);

        user.setCustomID(null);
        assertEquals(version, user.getVersion());

        user.setCustomID("en");
        assertTrue(version != user.getVersion());

        version = user.getVersion();
        user.setCustomID("en");
        assertEquals(version, user.getVersion());

        user.setCustomID("fr");
        assertTrue(version != user.getVersion());

        version = user.getVersion();
        user.setCustomID(null);
        assertTrue(version != user.getVersion());
    }

    @Test
    public void testProfileVersionRegion() throws Exception {
        BatchUserProfile user = Batch.getUserProfile();
        assertNotNull(user);

        user.setRegion(null);

        long version = user.getVersion();
        assertTrue(version >= 1);

        user.setRegion(null);
        assertEquals(version, user.getVersion());

        user.setRegion("en");
        assertTrue(version != user.getVersion());

        version = user.getVersion();
        user.setRegion("en");
        assertEquals(version, user.getVersion());

        user.setRegion("fr");
        assertTrue(version != user.getVersion());

        version = user.getVersion();
        user.setRegion(null);
        assertTrue(version != user.getVersion());
    }

    @Test
    public void testProfileVersionLanguage() throws Exception {
        BatchUserProfile user = Batch.getUserProfile();
        assertNotNull(user);

        user.setLanguage(null);

        long version = user.getVersion();
        assertTrue(version >= 1);

        user.setLanguage(null);
        assertEquals(version, user.getVersion());

        user.setLanguage("en");
        assertTrue(version != user.getVersion());

        version = user.getVersion();
        user.setLanguage("en");
        assertEquals(version, user.getVersion());

        user.setLanguage("fr");
        assertTrue(version != user.getVersion());

        version = user.getVersion();
        user.setLanguage(null);
        assertTrue(version != user.getVersion());
    }

    @Test
    public void testSerialization() throws Exception {
        JSONObject json = new WebserviceImpl(context).getJSON();

        assertFalse(json.has("upr"));

        Batch.getUserProfile().setLanguage("es").setRegion("MX");
        json = new WebserviceImpl(context).getJSON();

        assertTrue(json.has("upr"));

        JSONObject upr = json.getJSONObject("upr");

        assertEquals("es", upr.getString("ula"));
        assertEquals("MX", upr.getString("ure"));
        assertNotNull(upr.getInt("upv"));
    }

    @Test
    public void testAttributesRead() throws Exception {
        editor.setAttribute("today", new Date());
        editor.setAttribute("float_value", 3.2);
        editor.setAttribute("int_value", 4);
        editor.setAttribute("url_value", new URI("batch://batch.com"));
        editor.setAttribute("wrong_url_value", new URI("batch.com"));
        editor.save(false);

        MockBatchAttributesFetchListener listener = new MockBatchAttributesFetchListener();

        final Context ctx = RuntimeManagerProvider.get().getContext();
        UserDataAccessor.fetchAttributes(ctx, listener, false);

        Map<String, BatchUserAttribute> result = listener.getAttributes();

        assertTrue(listener.didFinish());
        assertFalse(listener.didFail());
        assertNotNull(result);
        assertEquals(4, listener.getAttributes().size()); // 3 attributes were set

        BatchUserAttribute dateValue = result.get("today");
        BatchUserAttribute urlValue = result.get("url_value");
        BatchUserAttribute wrongUrlValue = result.get("wrong_url_value");
        assertNotNull(dateValue);
        assertNull(dateValue.getStringValue());
        assertNull(dateValue.getNumberValue());
        assertNull(dateValue.getBooleanValue());
        assertNull(dateValue.getUriValue());
        assertNotNull(dateValue.getDateValue());

        assertNull(wrongUrlValue);
        assertNotNull(urlValue);
        assertNotNull(urlValue.getUriValue());

        // remove changes from test
        editor.clearAttributes();
        editor.save(false);
    }

    @Test
    public void testTagCollectionsRead() throws Exception {
        editor.clearTags();
        editor.save(false);
        editor.addTag("collection_1", "tag_1");
        editor.addTag("collection_1", "tag_2");
        editor.addTag("collection_2", "tag_3");
        editor.addTag("collection_3", "TAG_4");
        editor.save(false);

        MockBatchTagCollectionsFetchListener listener = new MockBatchTagCollectionsFetchListener();

        Context ctx = RuntimeManagerProvider.get().getContext();

        UserDataAccessor.fetchTagCollections(ctx, listener, false);

        Map<String, Set<String>> result = listener.getTagCollections();

        assertTrue(listener.didFinish());
        assertFalse(listener.didFail());
        assertNotNull(result);
        assertEquals(result.size(), 3); // 3 collections were set
        Set<String> collection1 = result.get("collection_1");
        assertTrue(collection1.contains("tag_2"));
        assertFalse(collection1.contains("tag_3"));
        Set<String> collection3 = result.get("collection_3");
        assertTrue(collection3.contains("tag_4")); // tags are set to lowercase when saved

        // remove changes from test
        editor.clearTags();
        editor.save(false);
    }

    @Test
    public void testCustomDataRead() throws Exception {
        // Inital test
        String initialRegion = Batch.User.getRegion(context);
        String initialLanguage = Batch.User.getLanguage(context);
        String initialIdentifier = Batch.User.getIdentifier(context);

        assertNull(initialRegion);
        assertNull(initialLanguage);
        assertNull(initialIdentifier);

        editor.setRegion("az");
        editor.setLanguage("ba");
        editor.setIdentifier("pp");

        Promise<Void> savePromise = editor.save(false);

        // No error
        assertEquals(Promise.Status.RESOLVED, savePromise.getStatus());
        // Test reading
        assertEquals("az", Batch.User.getRegion(context));
        assertEquals("ba", Batch.User.getLanguage(context));
        assertEquals("pp", Batch.User.getIdentifier(context));

        // Clear custom data
        editor.setRegion(null);
        editor.setLanguage(null);
        editor.setIdentifier(null);

        savePromise = editor.save(false);

        // No error
        assertEquals(Promise.Status.RESOLVED, savePromise.getStatus());

        // Test if clearing succeeded and that we're back to initial state.
        assertNull(Batch.User.getRegion(context));
        assertNull(Batch.User.getLanguage(context));
        assertEquals(Batch.User.getRegion(context), initialRegion);
        assertEquals(Batch.User.getLanguage(context), initialLanguage);
        assertNull(Batch.User.getIdentifier(context));
    }

    public static class WebserviceImpl extends BatchWebservice {

        protected WebserviceImpl(Context context) throws MalformedURLException {
            super(context, RequestType.POST, "http://test.com/%s");
        }

        public JSONObject getJSON() {
            return super.getPostDataProvider().getRawData();
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
