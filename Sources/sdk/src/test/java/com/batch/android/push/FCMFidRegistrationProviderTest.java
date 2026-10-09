package com.batch.android.push;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FCMFidRegistrationProviderTest {

    private static final String FID = "eg9b8t2eQlC_G9kRuVatSB";
    private static final String OTHER_FID = "fk2t3BUTSXeb9-eqCL091G";

    private ExecutorService executor;

    @Before
    public void setUp() {
        executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void testReturnsFidOnSuccessfulRegistration() throws Exception {
        FakeProvider provider = new FakeProvider("123456", Tasks.forResult(null), Tasks.forResult(FID));
        Assert.assertEquals(FID, getRegistrationOffMainThread(provider));
        Assert.assertTrue(provider.didFetchInstallationId);
    }

    @Test
    public void testReturnsNullWhenRegisterFails() throws Exception {
        FakeProvider provider = new FakeProvider(
            "123456",
            Tasks.forException(new Exception("register failed")),
            Tasks.forResult(FID)
        );
        Assert.assertNull(getRegistrationOffMainThread(provider));
        Assert.assertTrue(provider.didRegister);
    }

    @Test
    public void testReturnsNullWhenFidRetrievalFails() throws Exception {
        FakeProvider provider = new FakeProvider(
            "123456",
            Tasks.forResult(null),
            Tasks.forException(new Exception("getId failed"))
        );
        Assert.assertNull(getRegistrationOffMainThread(provider));
    }

    @Test
    public void testReturnsNullWithoutSenderID() throws Exception {
        FakeProvider provider = new FakeProvider(null, Tasks.forResult(null), Tasks.forResult(FID));
        Assert.assertNull(getRegistrationOffMainThread(provider));
        Assert.assertFalse(provider.didRegister);
    }

    @Test
    public void testRegistersWithFCMOnlyOnceAfterSuccess() throws Exception {
        FakeProvider provider = new FakeProvider("123456", Tasks.forResult(null), Tasks.forResult(FID));
        Assert.assertEquals(FID, getRegistrationOffMainThread(provider));
        Assert.assertEquals(FID, getRegistrationOffMainThread(provider));
        // A refresh must not call register() again: it would trigger onRegistered() and loop
        Assert.assertEquals(1, provider.registerCount);
        // Before and after the registration, then once for the refresh
        Assert.assertEquals(3, provider.fetchInstallationIdCount);
    }

    @Test
    public void testReturnsFidRegisteredByFCMWhenRegistrationReplacesIt() throws Exception {
        FakeProvider provider = new FakeProvider("123456", Tasks.forResult(null), Tasks.forResult(FID));
        // e.g. FID_ALREADY_USED: register() clears the FID and registers a new one
        provider.fidTaskAfterRegister = Tasks.forResult(OTHER_FID);
        Assert.assertEquals(OTHER_FID, getRegistrationOffMainThread(provider));

        // The refresh triggered by onRegistered() must not register again
        Assert.assertEquals(OTHER_FID, getRegistrationOffMainThread(provider));
        Assert.assertEquals(1, provider.registerCount);
    }

    @Test
    public void testReturnsNullWhenFidRetrievalFailsAfterRegistration() throws Exception {
        FakeProvider provider = new FakeProvider("123456", Tasks.forResult(null), Tasks.forResult(FID));
        provider.fidTaskAfterRegister = Tasks.forException(new Exception("getId failed"));
        Assert.assertNull(getRegistrationOffMainThread(provider));

        // Nothing was recorded as registered, so the next attempt registers again
        provider.fidTask = Tasks.forResult(FID);
        provider.fidTaskAfterRegister = null;
        Assert.assertEquals(FID, getRegistrationOffMainThread(provider));
        Assert.assertEquals(2, provider.registerCount);
    }

    @Test
    public void testRetriesRegistrationAfterFailure() throws Exception {
        FakeProvider provider = new FakeProvider(
            "123456",
            Tasks.forException(new Exception("register failed")),
            Tasks.forResult(FID)
        );
        Assert.assertNull(getRegistrationOffMainThread(provider));

        provider.registerTask = Tasks.forResult(null);
        Assert.assertEquals(FID, getRegistrationOffMainThread(provider));
        Assert.assertEquals(2, provider.registerCount);
    }

    @Test
    public void testRegistersAgainWhenFidChanges() throws Exception {
        FakeProvider provider = new FakeProvider("123456", Tasks.forResult(null), Tasks.forResult(FID));
        Assert.assertEquals(FID, getRegistrationOffMainThread(provider));

        // e.g. after FirebaseInstallations.delete(): Firebase does not register the new FID by itself
        provider.fidTask = Tasks.forResult(OTHER_FID);
        Assert.assertEquals(OTHER_FID, getRegistrationOffMainThread(provider));
        Assert.assertEquals(OTHER_FID, getRegistrationOffMainThread(provider));
        Assert.assertEquals(2, provider.registerCount);
    }

    @Test
    public void testDoesNotRegisterWhenFidRetrievalFails() throws Exception {
        FakeProvider provider = new FakeProvider(
            "123456",
            Tasks.forResult(null),
            Tasks.forException(new Exception("getId failed"))
        );
        Assert.assertNull(getRegistrationOffMainThread(provider));

        provider.fidTask = Tasks.forResult(FID);
        Assert.assertEquals(FID, getRegistrationOffMainThread(provider));
        Assert.assertEquals(1, provider.registerCount);
        Assert.assertEquals(3, provider.fetchInstallationIdCount);
    }

    @Test
    public void testShortname() {
        Assert.assertEquals("FCM-FID", new FakeProvider(null, null, null).getShortname());
    }

    /**
     * Tasks.await refuses to run on the main thread.
     */
    private String getRegistrationOffMainThread(FCMFidRegistrationProvider provider) throws Exception {
        return executor.submit(provider::getRegistration).get();
    }

    private static class FakeProvider extends FCMFidRegistrationProvider {

        Task<Void> registerTask;
        Task<String> fidTask;
        /**
         * When set, replaces fidTask once register() is called, like Firebase replacing the FID while registering.
         */
        Task<String> fidTaskAfterRegister;
        boolean didRegister = false;
        boolean didFetchInstallationId = false;
        int registerCount = 0;
        int fetchInstallationIdCount = 0;

        FakeProvider(String senderID, Task<Void> registerTask, Task<String> fidTask) {
            super();
            this.senderID = senderID;
            this.registerTask = registerTask;
            this.fidTask = fidTask;
        }

        @NonNull
        @Override
        Task<Void> registerWithFCM() {
            didRegister = true;
            registerCount++;
            if (fidTaskAfterRegister != null && registerTask.isSuccessful()) {
                fidTask = fidTaskAfterRegister;
            }
            return registerTask;
        }

        @NonNull
        @Override
        Task<String> fetchInstallationId() {
            didFetchInstallationId = true;
            fetchInstallationIdCount++;
            return fidTask;
        }
    }
}
