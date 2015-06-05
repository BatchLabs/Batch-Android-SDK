/*
 * Copyright (c) 2015 Batch.com. All rights reserved.
 */

package com.batch.android.sample.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.batch.android.Batch;
import com.batch.android.BatchCodeListener;
import com.batch.android.BatchRestoreListener;
import com.batch.android.CodeErrorInfo;
import com.batch.android.FailReason;
import com.batch.android.Feature;
import com.batch.android.Offer;
import com.batch.android.sample.R;
import com.batch.android.sample.UnlockManager;

import java.util.List;

/**
 * Batch Unlock activity
 */
public class UnlockActivity extends BatchActivity implements BatchCodeListener
{

    /**
     * Currently displayed wait dialog, so that we can dismiss it
     */
    private Dialog waitDialog;

    /**
     * Receiver for UnlockManager's update broadcasts
     */
    private BroadcastReceiver unlockBroadcastReceiver;

    /**
     * Switch view representing the "No Ads" feature state
     */
    private SwitchCompat noAdsSwitch;

    /**
     * Switch view representing the "Pro Trial" feature state
     */
    private SwitchCompat proTrialSwitch;

    /**
     * Text view for showing how many days are left for the pro trial (if applicable)
     */
    private TextView proTrialText;

    /**
     * Text view for showing the remaining lives count
     */
    private TextView livesText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);

        unlockBroadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                // Refresh the UI when UnlockManager notifies us of changes
                refreshUI();
            }
        };

        noAdsSwitch = (SwitchCompat) findViewById(R.id.unlock_no_ads_switch);
        proTrialSwitch = (SwitchCompat) findViewById(R.id.unlock_pro_trial_switch);
        proTrialText = (TextView) findViewById(R.id.unlock_pro_trial_text);
        livesText = (TextView) findViewById(R.id.unlock_lives_text);

        findViewById(R.id.unlock_restore_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                restore();
            }
        });

        findViewById(R.id.unlock_redeem_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showRedeemDialog();
            }
        });

        refreshUI();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // Register to UnlockManager's broadcasts, to keep the UI fresh
        final IntentFilter unlockFilter = new IntentFilter(UnlockManager.ACTION_UNLOCKS_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(unlockBroadcastReceiver, unlockFilter);
    }

    @Override
    protected void onPause()
    {
        // Unregister the broadcast receiver to avoid leaks
        LocalBroadcastManager.getInstance(this).unregisterReceiver(unlockBroadcastReceiver);
        super.onPause();
    }

    /**
     * Try to restore the user's features
     */
    private void restore()
    {
        showWaitDialog();

        Batch.Unlock.restore(new BatchRestoreListener()
        {
            @Override
            public void onRestoreSucceed(List<Feature> list)
            {
                hideWaitDialog();

                AlertDialog.Builder builder = new AlertDialog.Builder(UnlockActivity.this);
                builder.setMessage(R.string.unlock_restore_success)
                        .setPositiveButton(R.string.ok, null);

                AlertDialog dialog = builder.create();
                dialog.show();
            }

            @Override
            public void onRestoreFailed(FailReason failReason)
            {
                showErrorDialog(failReason, R.string.unlock_restore_error);
            }
        });
    }

    /**
     * Shows an dialog with a code input, and try to redeem it.
     */
    private void showRedeemDialog() {
        final EditText codeEditText = new AppCompatEditText(UnlockActivity.this);
        codeEditText.setHint(R.string.unlock_code);
        codeEditText.setSingleLine();

        AlertDialog.Builder builder = new AlertDialog.Builder(UnlockActivity.this);
        builder.setMessage(R.string.unlock_button_redeem);
        builder.setPositiveButton(R.string.unlock_redeem, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                showWaitDialog();
                Batch.Unlock.redeemCode(codeEditText.getText().toString(), UnlockActivity.this);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.setView(codeEditText);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Show a loading dialog
     */
    private void showWaitDialog()
    {
        hideWaitDialog();

        waitDialog = ProgressDialog.show(this, null, getString(R.string.unlock_loading));
    }

    /**
     * Hide a previously shown loading dialog
     */
    private void hideWaitDialog() {
        if (waitDialog != null) {
            waitDialog.dismiss();
            waitDialog = null;
        }
    }

    /**
     * Method that shows a Batch FailReason into a user-friendly AlertDialog
     * @param failReason Batch's FailReason
     * @param titleRes String resource ID for the Alert's title
     */
    private void showErrorDialog(FailReason failReason, int titleRes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(UnlockActivity.this);
        builder.setTitle(getString(titleRes));

        String reasonStr;
        switch( failReason )
        {
            case INVALID_CODE:
                reasonStr = "The code is invalid";
                break;
            case NETWORK_ERROR:
                reasonStr = "Network is unavailable";
                break;
            case MISMATCH_CONDITIONS:
                reasonStr = "Conditions are not matching";
                break;
            default:
                reasonStr = "Internal error, please try again later";
        }

        builder.setMessage(reasonStr);
        builder.setCancelable(true);
        builder.create().show();
    }

    /**
     * Refresh the UI with the state of the unlocks
     */
    private void refreshUI()
    {
        final UnlockManager unlockManager = UnlockManager.getInstance(this);
        noAdsSwitch.setChecked(unlockManager.hadNoAds());
        proTrialSwitch.setChecked(unlockManager.hasProTrial());

        long proTrialTimeLeft = unlockManager.timeLeftForProTrial();
        String timeLeftText;

        if (proTrialTimeLeft == UnlockManager.PRO_TRIAL_UNLIMITED)
        {
            timeLeftText = "Unlimited";
        }
        else if (proTrialTimeLeft == 0)
        {
            timeLeftText = "";
        }
        else if (proTrialTimeLeft < 86400)
        {
            timeLeftText = "Less than a day left";
        }
        else
        {
            timeLeftText = (proTrialTimeLeft/86400) + "+ days left";
        }
        proTrialText.setText(timeLeftText);

        livesText.setText(Long.toString(unlockManager.getLivesCount()));
    }

    // Implementation of BatchCodeListener

    @Override
    public void onRedeemCodeSuccess(String s, Offer offer)
    {
        hideWaitDialog();

        AlertDialog.Builder builder = new AlertDialog.Builder(UnlockActivity.this);
        builder.setMessage(R.string.unlock_code_success)
                .setPositiveButton(R.string.ok, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onRedeemCodeFailed(String s, FailReason failReason, CodeErrorInfo codeErrorInfo)
    {
        hideWaitDialog();
        showErrorDialog(failReason, R.string.unlock_code_error);
    }
}
