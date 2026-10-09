package com.batch.android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.batch.android.core.InternalPushData;
import com.batch.android.core.KVUserPreferencesStorage;
import com.batch.android.di.DITest;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowNotificationManager;

/** End-to-end coverage of the path every push landing goes through, from the notification builder to
 * {@link IntentParser#isPayloadAuthentic}. */
@RunWith(AndroidJUnit4.class)
public class PushLandingAuthenticityEndToEndTest extends DITest {

    private static final String BATCH_DATA =
        "{\"i\":\"pid\",\"ld\":{\"kind\":\"alert\",\"id\":\"a\",\"did\":\"a\",\"title\":\"t\",\"body\":\"b\",\"cta\":{\"l\":\"OK\",\"a\":null}}}";

    private Bundle newReceiverExtras() {
        Bundle b = new Bundle();
        b.putString(InternalPushData.BATCH_BUNDLE_KEY, BATCH_DATA);
        // Without an alert the presenter displays nothing.
        b.putString(Batch.Push.ALERT_KEY, "Notification body");
        b.putString(Batch.Push.TITLE_KEY, "Notification title");
        return b;
    }

    private IntentParser parserForLaunchIntentOfDisplayedNotification(Context context) throws Exception {
        // Without an icon the presenter aborts before building any intent.
        Batch.Push.setSmallIconResourceId(android.R.drawable.ic_dialog_info);
        BatchPushNotificationPresenter.displayForPush(context, newReceiverExtras());

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        ShadowNotificationManager shadow = Shadows.shadowOf(manager);
        List<Notification> notifications = shadow.getAllNotifications();
        assertFalse("The push should have been displayed", notifications.isEmpty());

        Notification notification = notifications.get(0);
        assertNotNull("The notification must carry a content intent", notification.contentIntent);
        Intent launchIntent = Shadows.shadowOf(notification.contentIntent).getSavedIntent();
        return new IntentParser(launchIntent);
    }

    /** Nominal path: a regression here silently breaks every mobile landing. */
    @Test
    public void testLandingFromDisplayedNotificationIsAuthentic() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        IntentParser parser = parserForLaunchIntentOfDisplayedNotification(context);

        assertTrue("The launch intent should carry a landing", parser.hasLanding());
        assertTrue(
            "A landing coming from a real Batch notification must be accepted",
            parser.isPayloadAuthentic(new KVUserPreferencesStorage(context))
        );
    }

    /** A third-party app starting the exported launcher activity with a payload it crafted itself. */
    @Test
    public void testForgedLandingIntentIsRejected() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        // A secret must exist, otherwise this would pass for the wrong reason.
        parserForLaunchIntentOfDisplayedNotification(context);

        Intent forged = new Intent();
        forged.putExtra(Batch.Push.PAYLOAD_KEY, newReceiverExtras());

        IntentParser parser = new IntentParser(forged);
        assertTrue("The forged intent does carry a landing", parser.hasLanding());
        assertFalse(
            "A landing with no signature must be refused",
            parser.isPayloadAuthentic(new KVUserPreferencesStorage(context))
        );
    }

    /** Swapping the landing under a genuine signature must not work either. */
    @Test
    public void testTamperedLandingIntentIsRejected() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        IntentParser genuine = parserForLaunchIntentOfDisplayedNotification(context);
        assertTrue(genuine.isPayloadAuthentic(new KVUserPreferencesStorage(context)));

        Bundle tampered = genuine.getPushBundle();
        tampered.putString(InternalPushData.BATCH_BUNDLE_KEY, BATCH_DATA.replace("\"title\":\"t\"", "\"title\":\"x\""));
        Intent intent = new Intent();
        intent.putExtra(Batch.Push.PAYLOAD_KEY, tampered);

        assertFalse(
            "Swapping the payload under a genuine signature must be refused",
            new IntentParser(intent).isPayloadAuthentic(new KVUserPreferencesStorage(context))
        );
    }
}
