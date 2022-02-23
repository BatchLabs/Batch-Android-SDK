package com.batch.android.messaging.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.batch.android.BatchMessage;
import com.batch.android.core.Logger;
import com.batch.android.messaging.model.AlertMessage;
import com.batch.android.messaging.view.helper.ThemeHelper;

/**
 * Alert messaging template fragment class. Doesn't change much from the base DialogFragment, but adds hooks for lifecycle callbacks.
 *
 */
public class AlertTemplateFragment extends BaseDialogFragment<AlertMessage> {

    private static final String TAG = "AlertTemplateFragment";

    public static AlertTemplateFragment newInstance(BatchMessage payloadMessage, AlertMessage messageModel) {
        final AlertTemplateFragment f = new AlertTemplateFragment();
        f.setMessageArguments(payloadMessage, messageModel);
        return f;
    }

    public AlertTemplateFragment() {
        super();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertMessage message = getMessageModel();
        if (message == null) {
            Logger.error(
                TAG,
                "Unknown error while creating alert fragment. Please report this to Batch's support. (code -3)"
            );
            return super.onCreateDialog(savedInstanceState);
        }

        final Context context = getContext();
        if (context == null) {
            Logger.error(
                TAG,
                "Unknown error while creating alert fragment. Please report this to Batch's support. (code -5)"
            );
            return super.onCreateDialog(savedInstanceState);
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(
            new ContextThemeWrapper(context, ThemeHelper.getDefaultTheme(context))
        );
        builder.setCancelable(true);

        if (message.titleText != null) {
            builder.setTitle(message.titleText);
        }
        builder.setMessage(message.bodyText);
        // I don't think we need a cancel button listener, OnCancelListener should do it
        builder.setNegativeButton(message.cancelButtonText, (dialog, which) -> analyticsDelegate.onClosed());

        if (message.acceptCTA != null) {
            builder.setPositiveButton(
                message.acceptCTA.label,
                (dialog, which) -> {
                    analyticsDelegate.onCTAClicked(0, message.acceptCTA);
                    messagingModule.performAction(getContext(), getPayloadMessage(), message.acceptCTA);
                }
            );
        }

        builder.setOnDismissListener(this);
        builder.setOnCancelListener(this);

        return builder.create();
    }

    //region: Auto close handling

    @Override
    protected void onAutoCloseCountdownStarted() {
        // This fragment doesn't handle auto closure
    }

    @Override
    protected boolean canAutoClose() {
        // This fragment doesn't handle auto closure
        return false;
    }

    @Override
    protected int getAutoCloseDelayMillis() {
        // This fragment doesn't handle auto closure
        return 0;
    }

    @Override
    protected void performAutoClose() {
        // This fragment doesn't handle auto closure
    }
    //endregion
}
