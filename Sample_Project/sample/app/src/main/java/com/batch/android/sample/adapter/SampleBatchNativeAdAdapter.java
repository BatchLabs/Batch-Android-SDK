/*
 * Copyright (c) 2015 Batch.com. All rights reserved.
 */

package com.batch.android.sample.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.batch.android.Batch;
import com.batch.android.BatchNativeAd;
import com.batch.android.sample.R;

/**
 * Sample implementation of BaseBatchNativeAdAdapter
 */
public class SampleBatchNativeAdAdapter extends BaseBatchNativeAdAdapter
{
    public SampleBatchNativeAdAdapter(BaseAdapter adapter, int adPosition, int adRepeatInterval)
    {
        super(adapter, adPosition, adRepeatInterval);
    }

    @Override
    public View getAdView(final BatchNativeAd ad, View convertView, ViewGroup parent)
    {
        if (convertView == null)
        {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_ad, parent, false);
        }

        // Cover Image is optional, so don't display it if we haven't got one
        if (ad.getCoverImage() != null)
        {
            ImageView coverImage = (ImageView) convertView.findViewById(R.id.ad_cell_cover_image);
            coverImage.setVisibility(View.VISIBLE);
            coverImage.setImageBitmap(ad.getCoverImage());
        }
        else
        {
            convertView.findViewById(R.id.ad_cell_cover_image).setVisibility(View.GONE);
        }

        ((TextView) convertView.findViewById(R.id.ad_cell_title_text)).setText(ad.getTitle());
        ((TextView) convertView.findViewById(R.id.ad_cell_body_text)).setText(ad.getBody());
        ((ImageView) convertView.findViewById(R.id.ad_cell_icon_image)).setImageBitmap(ad.getIconImage());

        Button cta = (Button) convertView.findViewById(R.id.ad_cell_cta);
        cta.setText(ad.getCallToAction());
        cta.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ad.performClickAction();
            }
        });

        // Rating is optional too
        if (ad.hasStarRating())
        {
            convertView.findViewById(R.id.ad_cell_rating).setVisibility(View.VISIBLE);

            double rating = ad.getStarRating();
            for (double i = 1.0; i < 6; i += 1.0)
            {
                ImageView imageView = getImageViewForRating(convertView, i);
                if (rating >= i)
                {
                    imageView.setImageResource(R.drawable.ad_star_full);
                }
                else if (rating < i && rating > i - 1)
                {
                    imageView.setImageResource(R.drawable.ad_star_half);
                }
                else
                {
                    imageView.setImageResource(R.drawable.ad_star_empty);
                }
            }
        }
        else
        {
            convertView.findViewById(R.id.ad_cell_rating).setVisibility(View.GONE);
        }

        return convertView;
    }

    @Override
    public View createAdLoadingView(ViewGroup parent)
    {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_ad_loading, parent, false);
    }

    @Override
    public String getAdPlacement()
    {
        return Batch.DEFAULT_PLACEMENT;
    }

    /**
     * Helper method for the ad view that gives you the right image view for a star number
     *
     * @param parent     Parent view containing the star ImageViews
     * @param starNumber The star you want
     * @return ImageView for the star number, null if not found
     */
    protected ImageView getImageViewForRating(View parent, double starNumber)
    {
        switch ((int) starNumber)
        {
            case 1:
                return (ImageView) parent.findViewById(R.id.ad_cell_star1);
            case 2:
                return (ImageView) parent.findViewById(R.id.ad_cell_star2);
            case 3:
                return (ImageView) parent.findViewById(R.id.ad_cell_star3);
            case 4:
                return (ImageView) parent.findViewById(R.id.ad_cell_star4);
            case 5:
                return (ImageView) parent.findViewById(R.id.ad_cell_star5);
            default:
                return null;
        }
    }
}
