/*
 * Copyright (c) 2015 Batch.com. All rights reserved.
 */

package com.batch.android.sample.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.batch.android.AdDisplayListener;
import com.batch.android.Batch;
import com.batch.android.BatchAdsError;
import com.batch.android.BatchInterstitialListener;
import com.batch.android.sample.R;


/**
 * Batch Ads activity
 */
public class AdsActivity extends BatchActivity implements AdDisplayListener
{
    private static final String TAG = "AdsActivity";

    private TextView statusTextView;
    private Button displayButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ads);

        statusTextView = (TextView) findViewById(R.id.ads_status_text);
        displayButton = (Button) findViewById(R.id.ads_display_button);

        displayButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Batch.Ads.displayInterstitial(AdsActivity.this, Batch.DEFAULT_PLACEMENT, AdsActivity.this);
            }

        });

        findViewById(R.id.ads_load_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Batch.Ads.loadInterstitial(Batch.DEFAULT_PLACEMENT, new BatchInterstitialListener()
                {
                    @Override
                    public void onInterstitialReady(String s)
                    {
                        // Enable the "display" button
                        refreshUI();
                    }

                    @Override
                    public void onFailedToLoadInterstitial(String s, BatchAdsError batchAdsError)
                    {
                        Toast.makeText(AdsActivity.this, "Failed to load interstitial for placement "
                                + s + ". Error: " + batchAdsError.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        findViewById(R.id.ads_native_ads_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(AdsActivity.this, NativeAdsActivity.class));
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        refreshUI();
    }

    private void refreshUI()
    {
        if (Batch.Ads.hasInterstitialReady(Batch.DEFAULT_PLACEMENT))
        {
            statusTextView.setText(R.string.ads_status_loaded);
            displayButton.setEnabled(true);
        }
        else
        {
            statusTextView.setText(R.string.ads_status_not_loaded);
            displayButton.setEnabled(false);
        }
    }

    @Override
    public void onNoAdDisplayed()
    {
        Log.d(TAG, "onNoAdDisplayed()");
        refreshUI();
    }

    @Override
    public void onAdDisplayed()
    {
        Log.d(TAG, "onAdDisplayed()");
    }

    @Override
    public void onAdClosed()
    {
        Log.d(TAG, "onAdClosed()");
        refreshUI();
    }

    @Override
    public void onAdCancelled()
    {
        Log.d(TAG, "onAdCancelled()");
    }

    @Override
    public void onAdClicked()
    {
        Log.d(TAG, "onAdClicked()");
    }
}
