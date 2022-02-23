package com.batch.android;

import static com.batch.android.module.MessagingModule.ACTION_DISMISS_INTERSTITIAL;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.Logger;
import com.batch.android.messaging.fragment.DialogEventListener;
import com.batch.android.messaging.fragment.ListenableDialog;

/**
 * Activity that only lives to display a messaging fragment
 */
@PublicSDK
public class MessagingActivity extends FragmentActivity implements DialogEventListener {

    private static final String TAG = "MessagingActivity";
    private static final String ROTATED = "ROTATED";
    private static final String DIALOG_FRAGMENT_TAG = "batchMessage";

    private BroadcastReceiver dismissReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && ACTION_DISMISS_INTERSTITIAL.equalsIgnoreCase(intent.getAction())) {
                finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null || !savedInstanceState.getBoolean(ROTATED, false)) {
            boolean messageDisplayed = false;
            final Intent i = getIntent();
            if (i != null) {
                final Bundle b = i.getExtras();
                if (b != null) {
                    try {
                        messageDisplayed = showMessage(BatchMessage.getMessageForBundle(b));
                    } catch (BatchPushPayload.ParsingException e) {
                        Logger.internal(TAG, e);
                    }
                }
            }

            if (!messageDisplayed) {
                finish();
            }
        } else {
            Fragment f = getSupportFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG);
            if (f instanceof ListenableDialog) {
                ((ListenableDialog) f).setDialogEventListener(this);
            }
        }

        LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(dismissReceiver, new IntentFilter(ACTION_DISMISS_INTERSTITIAL));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(ROTATED, true);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Batch.onStart(this);
    }

    @Override
    protected void onStop() {
        Batch.onStop(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Batch.onDestroy(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dismissReceiver);
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        // As the dialog will handle the exit animation, we need a stub animation that does
        // nothing but wait
        overridePendingTransition(0, R.anim.com_batchsdk_window_stub);
    }

    private boolean showMessage(BatchMessage message) {
        if (message == null) {
            return false;
        }

        try {
            DialogFragment df = Batch.Messaging.loadFragment(this, message);

            if (df instanceof ListenableDialog) {
                ((ListenableDialog) df).setDialogEventListener(this);
                df.show(getSupportFragmentManager(), DIALOG_FRAGMENT_TAG);
                return true;
            } else {
                Logger.error(TAG, "Unknown error while showing Batch Message (code -1)");
                return false;
            }
        } catch (BatchMessagingException e) {
            Logger.error(TAG, "Unknown error while showing Batch Message (code -2)", e);
        }

        return false;
    }

    @Override
    public void onDialogDismiss(DialogFragment dialog) {
        if (!isChangingConfigurations()) {
            // The activity is fully transparent and does not respond to touch, but
            // we need to finish it so that we don't leak.
            // We should NOT finish on rotation, as the dialog will be dismissed
            // only to be recreated. Finishing the activity breaks rotation.
            finish();
        }
    }

    public static void startActivityForMessage(Context c, BatchMessage message) {
        if (message == null) {
            return;
        }

        final Intent i = new Intent(c, MessagingActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        message.writeToIntent(i);
        c.startActivity(i);

        LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent(ACTION_DISMISS_INTERSTITIAL));
    }
}
