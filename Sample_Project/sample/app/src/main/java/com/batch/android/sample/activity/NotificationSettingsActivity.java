/*
 * Copyright (c) 2015 Batch.com. All rights reserved.
 */

package com.batch.android.sample.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.LayoutRes;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatDelegate;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.batch.android.Batch;
import com.batch.android.sample.R;

/**
 * Simple Preference activity for managing Notification settings
 *
 * Nothing complicated here, simply implement Batch and AppCompat
 */
public class NotificationSettingsActivity extends PreferenceActivity
{

    private AppCompatDelegate delegate;

    private AppCompatDelegate getDelegate()
    {
        if (delegate == null)
        {
            delegate = AppCompatDelegate.create(this, null);
        }
        return delegate;
    }

    @Override
    @SuppressWarnings("deprecated")
    protected void onCreate(Bundle savedInstanceState)
    {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);

        // Use the deprecated preferences API for 2.3 compatibility
        addPreferencesFromResource(R.xml.settings_notification);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == android.R.id.home)
        {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    @Override
    public MenuInflater getMenuInflater()
    {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID)
    {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view)
    {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params)
    {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params)
    {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume()
    {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color)
    {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Batch.onStart(this);
    }

    @Override
    protected void onStop()
    {
        Batch.onStop(this);
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy()
    {
        Batch.onDestroy(this);
        super.onDestroy();
        getDelegate().onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        Batch.onNewIntent(this, intent);
        super.onNewIntent(intent);
    }

    public void invalidateOptionsMenu()
    {
        getDelegate().invalidateOptionsMenu();
    }

}
