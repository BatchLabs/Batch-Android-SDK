package com.batch.android.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test {@link PushPayloadSigner}: a landing carried by an intent must only be trusted when it was
 * signed by this SDK installation, so that a third-party app cannot forge one.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PushPayloadSignerTest {

    private static final String BATCH_DATA =
        "{\"i\":\"push-id\",\"ld\":{\"kind\":\"webview\",\"url\":\"https://batch.com\"}}";

    private Context appContext;
    private KVUserPreferencesStorage storage;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
        storage = new KVUserPreferencesStorage(appContext);
        storage.clear();
    }

    @After
    public void tearDown() {
        storage.clear();
    }

    private Bundle newBatchExtras() {
        Bundle extras = new Bundle();
        extras.putString(InternalPushData.BATCH_BUNDLE_KEY, BATCH_DATA);
        return extras;
    }

    @Test
    public void testSignedPayloadIsAuthentic() {
        Bundle extras = newBatchExtras();
        PushPayloadSigner.signPushExtras(storage, extras);

        assertNotNull(extras.getString(InternalPushData.SIGNATURE_BUNDLE_KEY));
        assertTrue(PushPayloadSigner.isPushExtrasAuthentic(storage, extras));
    }

    @Test
    public void testUnsignedPayloadIsRejected() {
        // A third-party app crafting a payload without a signature must be rejected, even though the
        // installation already has a secret (i.e. it has built notifications before).
        PushPayloadSigner.signPushExtras(storage, newBatchExtras());

        Bundle forged = newBatchExtras();
        assertFalse(PushPayloadSigner.isPushExtrasAuthentic(storage, forged));
    }

    @Test
    public void testTamperedPayloadIsRejected() {
        Bundle extras = newBatchExtras();
        PushPayloadSigner.signPushExtras(storage, extras);

        extras.putString(
            InternalPushData.BATCH_BUNDLE_KEY,
            "{\"i\":\"push-id\",\"ld\":{\"kind\":\"webview\",\"url\":\"https://example.com\"}}"
        );
        assertFalse(PushPayloadSigner.isPushExtrasAuthentic(storage, extras));
    }

    @Test
    public void testSignatureFromAnotherInstallationIsRejected() {
        // Sign with the current secret, then simulate a different installation by wiping the secret.
        Bundle extras = newBatchExtras();
        PushPayloadSigner.signPushExtras(storage, extras);
        assertTrue(PushPayloadSigner.isPushExtrasAuthentic(storage, extras));

        storage.clear();
        assertFalse(PushPayloadSigner.isPushExtrasAuthentic(storage, extras));
    }

    @Test
    public void testPayloadWithoutBatchDataIsRejected() {
        assertFalse(PushPayloadSigner.isPushExtrasAuthentic(storage, new Bundle()));
        assertFalse(PushPayloadSigner.isPushExtrasAuthentic(storage, null));
    }
}
