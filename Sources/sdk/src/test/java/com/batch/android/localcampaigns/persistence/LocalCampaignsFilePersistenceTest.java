package com.batch.android.localcampaigns.persistence;

import android.app.Activity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import com.batch.android.TestActivity;
import com.batch.android.json.JSONObject;
import java.util.UUID;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LocalCampaignsFilePersistenceTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<>(TestActivity.class, false, true);

    private LocalCampaignsFilePersistence persister;
    private Activity activity;

    @Before
    public void setUp() {
        persister = new LocalCampaignsFilePersistence();
        activity = mActivityRule.getActivity();
    }

    @After
    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        Mockito.validateMockitoUsage();
    }

    @Test
    public void testHasSavedData() throws PersistenceException {
        String randomFileName = UUID.randomUUID().toString() + ".toto";

        Assert.assertFalse("The file must not exist", persister.hasSavedData(activity, randomFileName));

        persister.persistData(activity, new JSONObject(), randomFileName);
        Assert.assertTrue("The file was supposed to be created.", persister.hasSavedData(activity, randomFileName));

        persister.deleteData(activity, randomFileName);
        Assert.assertFalse("The file was supposed to be deleted.", persister.hasSavedData(activity, randomFileName));
    }

    @Test
    public void testSaveAndLoadData() throws Exception {
        String randomFileName = UUID.randomUUID().toString() + ".toto";

        JSONObject savedData = new JSONObject();
        savedData.put("toto", 11);
        savedData.put("tata", 12);

        persister.persistData(activity, savedData, randomFileName);

        JSONObject expectedData = new JSONObject(savedData);
        expectedData.put(
            LocalCampaignsFilePersistence.PERSISTENCE_SAVE_VERSION_KEY,
            LocalCampaignsFilePersistence.PERSISTENCE_CURRENT_FILE_VERSION
        );

        JSONObject loadedData = persister.loadData(activity, randomFileName);
        Assert.assertNotNull(loadedData);

        Assert.assertEquals("Loaded data not equals to saved data", expectedData.toString(), loadedData.toString());

        savedData.put(LocalCampaignsFilePersistence.PERSISTENCE_SAVE_VERSION_KEY, -1);
        persister.persistData(activity, savedData, randomFileName);

        // Must throw an exception when loading a file with an invalid save version
        exception.expect(PersistenceException.class);
        persister.loadData(activity, randomFileName);
    }
}
