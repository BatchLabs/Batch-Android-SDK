/*
 * Copyright (c) 2015 Batch.com. All rights reserved.
 */

package com.batch.android.sample.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.batch.android.sample.R;
import com.batch.android.sample.adapter.SampleBatchNativeAdAdapter;

/**
 * Activity that implements a ListView with Batch Native Ads
 */
public class NativeAdsActivity extends BatchActivity
{

    /**
     * Position of the first native ad
     */
    private static final int NATIVE_AD_POSITION = 2;

    /**
     * Interval at which native ads will be shown in the list
     */
    private static final int NATIVE_AD_REPEAT_INTERVAL = 5;

    /**
     * Our list
     */
    private ListView listView;

    /**
     * Adapter for the standard list items
     */
    private ArrayAdapter itemsAdapter;

    /**
     * Native Ad adapter
     */
    private SampleBatchNativeAdAdapter nativeAdAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_ads);

        listView = (ListView) findViewById(android.R.id.list);

        // Basic adapter for our fake list items
        itemsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                new String[] {"One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight"});

        // Wrap it in our BaseBatchNativeAdAdapter implementation
        nativeAdAdapter = new SampleBatchNativeAdAdapter(itemsAdapter, NATIVE_AD_POSITION,
                NATIVE_AD_REPEAT_INTERVAL);

        listView.setAdapter(nativeAdAdapter);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // Tell the BaseBatchNativeAdAdapter that Batch started
        // It is REQUIRED, otherwise no Ad will be shown
        nativeAdAdapter.onStart();
    }
}
