/*
 * Copyright (c) 2015 Batch.com. All rights reserved.
 */

package com.batch.android.sample.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.batch.android.Batch;
import com.batch.android.BatchURLListener;
import com.batch.android.BatchUnlockListener;
import com.batch.android.CodeErrorInfo;
import com.batch.android.FailReason;
import com.batch.android.Offer;
import com.batch.android.sample.UnlockManager;

/**
 * Base Activity for all of the sample's activities
 */
public class BatchActivity extends AppCompatActivity implements BatchUnlockListener, BatchURLListener
{
    private static final String TAG = "BatchActivity";

    @Override
    protected void onStart()
    {
        super.onStart();
        Batch.Unlock.setUnlockListener(this);
        Batch.Unlock.setURLListener(this);
        Batch.onStart(this);
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        Batch.onNewIntent(this, intent);
    }

    @Override
    protected void onStop()
    {
        Batch.onStop(this);
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        Batch.onDestroy(this);
        super.onDestroy();
    }

    @Override
    public void onRedeemAutomaticOffer(Offer offer)
    {
        Log.i(TAG, "Redeeming automatic offer");
        UnlockManager.getInstance(this).unlockItems(offer);
        UnlockManager.getInstance(this).showRedeemAlert(this, offer);
    }

    @Override
    public void onURLWithCodeFound(String s)
    {
        Log.i(TAG, "onURLWithCodeFound " + s);
    }

    @Override
    public void onURLCodeSuccess(String s, Offer offer)
    {
        Log.i(TAG, "onURLCodeSuccess " + s);
        UnlockManager.getInstance(this).unlockItems(offer);
        UnlockManager.getInstance(this).showRedeemAlert(this, offer);
    }

    @Override
    public void onURLCodeFailed(String s, FailReason failReason, CodeErrorInfo codeErrorInfo)
    {
        Log.i(TAG, "onURLCodeFailed " + s + " " + failReason.toString() + " " + codeErrorInfo.toString());
    }
}
