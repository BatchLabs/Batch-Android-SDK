/*
 * Copyright (c) 2015 Batch.com. All rights reserved.
 */

package com.batch.android.sample.adapter;

import android.database.DataSetObserver;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.batch.android.Batch;
import com.batch.android.BatchAdsError;
import com.batch.android.BatchNativeAd;
import com.batch.android.BatchNativeAd.State;
import com.batch.android.BatchNativeAdListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Base adapter for showing Native Ads in a list (or anything that takes an Adapter).
 * It extends another adapter by composition, and adds Batch Native Ads in it.
 * <p/>
 * To use it, extend this class and implement the required methods.
 * Then, instantiate it with your standard adapter, and set it at the list's adapter.
 * Don't forget to call onStart on it!
 */
public abstract class BaseBatchNativeAdAdapter extends BaseAdapter implements BatchNativeAdListener
{
    /**
     * Base adapter of the developer, for composition
     */
    private BaseAdapter adapter;
    /**
     * Ads per position
     */
    private SparseArray<BatchNativeAd> ads = new SparseArray<>();
    /**
     * Locked state of ads per position
     */
    private SparseArray<State> states = new SparseArray<>();

    /**
     * Stored ads to load at next start
     */
    private List<BatchNativeAd> adsToLoad = new ArrayList<>();

    /**
     * Position of the first ad
     */
    private int adPosition;

    /**
     * Repeat interval of ads if multiple ads wanted (null if no repeat)
     */
    private Integer adRepeatInterval;

    /**
     * Boolean to track whether the activity has been started or not
     */
    private boolean activityStarted = false;

    /**
     * Create a BaseAdapter with a native ad the given position
     *
     * @param adapter    Your standard list adapter
     * @param adPosition Position of the Ad
     */
    public BaseBatchNativeAdAdapter(BaseAdapter adapter, int adPosition)
    {
        this(adapter, adPosition, -1);
    }

    /**
     * Create a BaseAdapter with a native ad the given position and other ads coming after with the given interval
     *
     * @param adapter    Your standard list adapter
     * @param adPosition Position of the Ad
     * @parap adRepeat Ad Repeat interval
     */
    public BaseBatchNativeAdAdapter(BaseAdapter adapter, int adPosition, int adRepeatInterval)
    {
        if (adapter == null)
        {
            throw new NullPointerException("Adapter == null");
        }

        if (adPosition < 0)
        {
            throw new IllegalArgumentException("adPosition must be >= 0");
        }

        this.adapter = adapter;
        this.adPosition = adPosition;
        this.adRepeatInterval = adRepeatInterval > 0 ? adRepeatInterval : null;

        createMissingAds();
    }

    /**
     * Call this method after calling {@link Batch#onStart(android.app.Activity)} to tell the adapter that Ads can now be loaded
     */
    public void onStart()
    {
        activityStarted = true;
        synchronized (adsToLoad)
        {
            for (BatchNativeAd ad : adsToLoad)
            {
                Batch.Ads.loadNativeAd(ad, this);
            }

            adsToLoad.clear();
        }
    }

// ------------------------------------------>

    /**
     * Create or re-use a view for the given ad
     * Works similarly to BaseAdapter's getView
     *
     * @param ad          The Batch Native Ad to display
     * @param convertView View to reuse if not null. Otherwise, inflate a new one.
     * @param parent      AdView's parent for inflation.
     * @return The ad view. Can't be null.
     */
    public abstract View getAdView(BatchNativeAd ad, View convertView, ViewGroup parent);

    /**
     * Create a view to indicate the user the ad is loading
     *
     * @param parent Loading view's parent for inflation.
     * @return The loading view. Can't be null.
     */
    public abstract View createAdLoadingView(ViewGroup parent);

    /**
     * Get the placement of the ad (use {@link Batch#DEFAULT_PLACEMENT} for default)
     *
     * @return
     */
    public abstract String getAdPlacement();

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        int count = getNumberOfAdsBefore(position);
        State state = states.get(position);

        if (!isAdDisplayed(state))
        {
            return adapter.getView(position - count, convertView, parent);
        }

        if (state == State.READY)
        {
            BatchNativeAd ad = ads.get(position);

            View adView = getAdView(ad, convertView, parent);
            ad.registerView(adView);

            return adView;
        }
        else
        {
            return convertView == null ? createAdLoadingView(parent) : convertView;
        }
    }

    @Override
    public long getItemId(int position)
    {
        int count = getNumberOfAdsBefore(position);
        State state = states.get(position);

        if (isAdDisplayed(state))
        {
            return ads.get(position)._getId().getMostSignificantBits();
        }

        return adapter.getItemId(position - count);
    }

    @Override
    public Object getItem(int position)
    {
        int count = getNumberOfAdsBefore(position);
        State state = states.get(position);

        if (!isAdDisplayed(state))
        {
            return adapter.getItem(position - count);
        }

        return ads.get(position);
    }

    @Override
    public int getCount()
    {
        int adsCount = 0;
        for (int i = 0; i < states.size(); i++)
        {
            State state = states.get(states.keyAt(i));

            if (isAdDisplayed(state))
            {
                adsCount++;
            }
        }

        return adapter.getCount() + adsCount;
    }

    @Override
    public void notifyDataSetChanged()
    {
        // Update states of ads and create new ones if expired
        for (int i = 0; i < ads.size(); i++)
        {
            int position = ads.keyAt(i);
            BatchNativeAd ad = ads.get(position);

            if (ad.getState() == State.EXPIRED)
            {
                ad.unregisterView();
                createAdForPosition(position);
            }
            else
            {
                states.put(position, ad.getState());
            }
        }

        // Create new ads if count of object is higher than previously
        createMissingAds();

        adapter.notifyDataSetChanged();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated()
    {
        states.clear();

        for (int i = 0; i < ads.size(); i++)
        {
            BatchNativeAd ad = ads.get(ads.keyAt(i));
            ad.unregisterView();
        }

        ads.clear();

        adapter.notifyDataSetInvalidated();
        super.notifyDataSetInvalidated();
    }

    @Override
    public int getItemViewType(int position)
    {
        int count = getNumberOfAdsBefore(position);
        State state = states.get(position);

        if (!isAdDisplayed(state))
        {
            return adapter.getItemViewType(position - count);
        }

        return state == State.READY ? adapter.getViewTypeCount() : adapter.getViewTypeCount() + 1;
    }

    @Override
    public int getViewTypeCount()
    {
        return adapter.getViewTypeCount() + 2;
    }

    @Override
    public boolean hasStableIds()
    {
        return adapter.hasStableIds();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer)
    {
        adapter.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer)
    {
        adapter.unregisterDataSetObserver(observer);
    }

    @Override
    public boolean areAllItemsEnabled()
    {
        return adapter.areAllItemsEnabled();
    }

    @Override
    public boolean isEnabled(int position)
    {
        int count = getNumberOfAdsBefore(position);
        State state = states.get(position);

        if (!isAdDisplayed(state))
        {
            return adapter.isEnabled(position - count);
        }

        return true;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent)
    {
        int count = getNumberOfAdsBefore(position);
        State state = states.get(position);

        if (!isAdDisplayed(state))
        {
            return adapter.getDropDownView(position - count, convertView, parent);
        }

        return null;
    }

    @Override
    public boolean isEmpty()
    {
        return adapter.isEmpty();
    }

// ------------------------------------------->

    /**
     * Get the number of native ads shown before this position
     *
     * @param position
     * @return
     */
    private int getNumberOfAdsBefore(int position)
    {
        int count = 0;

        for (int i = 0; i < position; i++)
        {
            State state = states.get(i);
            if (state != null && (state == State.READY || state == State.NOT_LOADED))
            {
                count++;
            }
        }

        return count;
    }

    /**
     * Is an ad displayed in the list for this state
     *
     * @param state Batch Nativate Ad state
     * @return Whether it is displayed or not
     */
    private boolean isAdDisplayed(State state)
    {
        return state != null && (state == State.READY || state == State.NOT_LOADED);
    }

    /**
     * Create and load an ad for the given position
     *
     * @param adPosition
     */
    private void createAdForPosition(final int adPosition)
    {
        final BatchNativeAd ad = new BatchNativeAd(getAdPlacement());

        states.put(adPosition, ad.getState());
        ads.put(adPosition, ad);

        synchronized (adsToLoad)
        {
            if (activityStarted)
            {
                Batch.Ads.loadNativeAd(ad, BaseBatchNativeAdAdapter.this);
            }
            else
            {
                adsToLoad.add(ad);
            }
        }
    }

    /**
     * Create all missing ads
     */
    private void createMissingAds()
    {
        if (adapter.getCount() >= adPosition && states.get(adPosition) == null)
        {
            createAdForPosition(adPosition);
        }

        if (adRepeatInterval != null)
        {
            int adRepeatIndex = 1;
            int nextAdPosition = adPosition + (adRepeatIndex * adRepeatInterval);

            while (adapter.getCount() >= nextAdPosition)
            {
                if (states.get(nextAdPosition) == null)
                {
                    createAdForPosition(nextAdPosition);
                }

                adRepeatIndex++;
                nextAdPosition = adPosition + (adRepeatIndex * adRepeatInterval);
            }
        }

    }

    @Override
    public void onFailToLoadNativeAd(BatchNativeAd ad, BatchAdsError error)
    {
        BaseBatchNativeAdAdapter.this.notifyDataSetChanged();
    }

    @Override
    public void onAdReady(BatchNativeAd ad)
    {
        BaseBatchNativeAdAdapter.this.notifyDataSetChanged();
    }
}
