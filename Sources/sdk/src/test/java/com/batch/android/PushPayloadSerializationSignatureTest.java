package com.batch.android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.batch.android.core.InternalPushData;
import com.batch.android.core.KVUserPreferencesStorage;
import com.batch.android.core.PushPayloadSigner;
import com.batch.android.di.DITest;
import com.batch.android.di.providers.PushModuleProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;

/** Signature of the payload serialization paths an integration can reach on its own. */
@RunWith(AndroidJUnit4.class)
public class PushPayloadSerializationSignatureTest extends DITest {

    private static final String BATCH_DATA =
        "{\"i\":\"pid\",\"ld\":{\"kind\":\"alert\",\"id\":\"a\",\"did\":\"a\",\"title\":\"t\",\"body\":\"b\",\"cta\":{\"l\":\"OK\",\"a\":null}}}";

    private Bundle newExtras() {
        Bundle b = new Bundle();
        b.putString(InternalPushData.BATCH_BUNDLE_KEY, BATCH_DATA);
        return b;
    }

    private boolean isAuthentic(Context context, Bundle payload) {
        return PushPayloadSigner.isPushExtrasAuthentic(new KVUserPreferencesStorage(context), payload);
    }

    @Test
    public void testWriteToIntentExtrasSignsPayload() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        simulateBatchStart(context);

        BatchPushPayload payload = BatchPushPayload.payloadFromReceiverExtras(newExtras());
        Intent intent = new Intent();
        payload.writeToIntentExtras(intent);

        assertTrue(isAuthentic(context, intent.getBundleExtra(Batch.Push.PAYLOAD_KEY)));
    }

    @Test
    public void testWriteToBundleSignsPayload() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        simulateBatchStart(context);

        BatchPushPayload payload = BatchPushPayload.payloadFromReceiverExtras(newExtras());
        Bundle bundle = new Bundle();
        payload.writeToBundle(bundle);

        assertTrue(isAuthentic(context, bundle.getBundle(Batch.Push.PAYLOAD_KEY)));
    }

    @Test
    public void testSerializationRoundTripStaysAuthentic() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        simulateBatchStart(context);

        Bundle bundle = new Bundle();
        BatchPushPayload.payloadFromReceiverExtras(newExtras()).writeToBundle(bundle);

        BatchPushPayload readBack = BatchPushPayload.payloadFromBundle(bundle);
        assertTrue(isAuthentic(context, readBack.getPushBundle()));
    }

    /** Batch is deliberately not started: makePendingIntent must sign with the context it was given. */
    @Test
    public void testMakePendingIntentSignsWhileBatchIsStopped() {
        Context context = ApplicationProvider.getApplicationContext();

        PendingIntent pendingIntent = PushModuleProvider
            .get()
            .makePendingIntent(context, new Intent(context, BatchActionActivity.class), newExtras());

        Intent launchIntent = Shadows.shadowOf(pendingIntent).getSavedIntent();
        assertTrue(isAuthentic(context, launchIntent.getBundleExtra(Batch.Push.PAYLOAD_KEY)));
    }

    /** Batch is deliberately left stopped: onServiceCreate is the documented way to enable signing. */
    @Test
    public void testOnServiceCreateEnablesSigningFromBackgroundService() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Batch.start("FAKE_API_KEY");

        Intent unsignedIntent = new Intent();
        BatchPushPayload.payloadFromReceiverExtras(newExtras()).writeToIntentExtras(unsignedIntent);
        assertFalse(
            "Without a context the payload cannot be signed",
            isAuthentic(context, unsignedIntent.getBundleExtra(Batch.Push.PAYLOAD_KEY))
        );

        Batch.onServiceCreate(context, false);

        Intent signedIntent = new Intent();
        BatchPushPayload.payloadFromReceiverExtras(newExtras()).writeToIntentExtras(signedIntent);
        assertTrue(
            "onServiceCreate must synchronously give the SDK a context to sign with",
            isAuthentic(context, signedIntent.getBundleExtra(Batch.Push.PAYLOAD_KEY))
        );
    }

    /** Guards against the check becoming vacuous. */
    @Test
    public void testUnsignedPayloadIsRejected() {
        Context context = ApplicationProvider.getApplicationContext();
        simulateBatchStart(context);
        // A secret must exist, so that rejection comes from the missing signature.
        PushPayloadSigner.signPushExtras(new KVUserPreferencesStorage(context), newExtras());

        assertFalse(isAuthentic(context, newExtras()));
    }
}
