package com.batch.android;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.batch.android.core.InternalPushData;
import com.batch.android.core.KVUserPreferencesStorage;
import com.batch.android.core.PushPayloadSigner;
import com.batch.android.di.DITest;
import com.batch.android.di.providers.PushModuleProvider;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;

/**
 * Non-regression tests for the payload propagation paths that carry a landing but do NOT go through the
 * automatic notification builder's signing step: notification action intents and the manual-integration
 * helpers ({@code Batch.Push.appendBatchData} / {@code makePendingIntent}). Both must sign the payload so
 * that {@code Batch.onStart} accepts the legitimate landing they carry.
 */
@RunWith(AndroidJUnit4.class)
public class PushPayloadSignerPropagationTest extends DITest {

    private static final String BATCH_DATA =
        "{\"i\":\"pid\",\"ld\":{\"kind\":\"alert\",\"id\":\"a\",\"did\":\"a\",\"title\":\"t\",\"body\":\"b\",\"cta\":{\"l\":\"OK\",\"a\":null}}}";

    private Bundle newExtras() {
        Bundle b = new Bundle();
        b.putString(InternalPushData.BATCH_BUNDLE_KEY, BATCH_DATA);
        return b;
    }

    /** Regression: the payload snapshot serialized into notification action intents must be signed. */
    @Test
    public void testNotificationActionPayloadIsSigned() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        simulateBatchStart(context);
        BatchPushPayload payload = BatchPushPayload.payloadFromReceiverExtras(newExtras());

        BatchNotificationAction action = new BatchNotificationAction();
        action.label = "Label";
        action.actionIdentifier = "id";
        action.drawableName = "ic_test";
        action.actionArguments = new com.batch.android.json.JSONObject();
        action.hasUserInterface = true;

        List<NotificationCompat.Action> actions = BatchNotificationAction.getSupportActions(
            context,
            Collections.singletonList(action),
            payload,
            1
        );

        Intent actionIntent = Shadows.shadowOf(actions.get(0).actionIntent).getSavedIntent();
        Bundle actionPayload = actionIntent.getBundleExtra(Batch.Push.PAYLOAD_KEY);

        KVUserPreferencesStorage storage = new KVUserPreferencesStorage(context);
        assertTrue(PushPayloadSigner.isPushExtrasAuthentic(storage, actionPayload));
    }

    /** Regression: the manual custom-receiver path must sign the payload it appends to the open intent. */
    @Test
    public void testAppendBatchDataPayloadIsSigned() {
        Context context = ApplicationProvider.getApplicationContext();
        simulateBatchStart(context);

        Intent openIntent = new Intent();
        PushModuleProvider.get().appendBatchData(newExtras(), openIntent);

        Bundle openPayload = openIntent.getBundleExtra(Batch.Push.PAYLOAD_KEY);
        KVUserPreferencesStorage storage = new KVUserPreferencesStorage(context);
        assertTrue(PushPayloadSigner.isPushExtrasAuthentic(storage, openPayload));
    }
}
