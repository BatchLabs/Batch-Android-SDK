package com.batch.android.debug;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.batch.android.Batch;
import com.batch.android.R;
import com.batch.android.debug.fragment.IdentifierDebugFragment;
import com.batch.android.debug.fragment.LocalCampaignDebugFragment;
import com.batch.android.debug.fragment.LocalCampaignsDebugFragment;
import com.batch.android.debug.fragment.MainDebugFragment;
import com.batch.android.debug.fragment.UserDataDebugFragment;
import com.batch.android.di.providers.CampaignManagerProvider;

/**
 * Debug activity that display info from Batch SDK
 *
 */
public class BatchDebugActivity extends FragmentActivity implements OnMenuSelectedListener {

    public static final int MAIN_DEBUG_FRAGMENT = 0;
    public static final int IDENTIFIER_DEBUG_FRAGMENT = 1;
    public static final int USER_DATA_DEBUG_FRAGMENT = 2;
    public static final int LOCAL_CAMPAIGNS_DEBUG_FRAGMENT = 3;
    public static final int LOCAL_CAMPAIGN_DEBUG_FRAGMENT = 4;

    private Fragment[] fragments = new Fragment[5];

    private void switchFragment(int newIndex, boolean first, String campaignToken) {
        if (newIndex >= 0 && newIndex < fragments.length) {
            if (fragments[newIndex] == null) {
                switch (newIndex) {
                    case MAIN_DEBUG_FRAGMENT:
                        fragments[newIndex] = MainDebugFragment.newInstance();
                        break;
                    case IDENTIFIER_DEBUG_FRAGMENT:
                        fragments[newIndex] = IdentifierDebugFragment.newInstance();
                        break;
                    case USER_DATA_DEBUG_FRAGMENT:
                        fragments[newIndex] = UserDataDebugFragment.newInstance();
                        break;
                    case LOCAL_CAMPAIGNS_DEBUG_FRAGMENT:
                        fragments[newIndex] = LocalCampaignsDebugFragment.newInstance(CampaignManagerProvider.get());
                        break;
                    case LOCAL_CAMPAIGN_DEBUG_FRAGMENT:
                        fragments[newIndex] =
                            LocalCampaignDebugFragment.newInstance(campaignToken, CampaignManagerProvider.get());
                        break;
                }
            }

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if (first) {
                fragmentTransaction
                    .replace(R.id.com_batchsdk_debug_fragment_container, fragments[newIndex])
                    .commitNow();
            } else {
                fragmentTransaction
                    .replace(R.id.com_batchsdk_debug_fragment_container, fragments[newIndex])
                    .addToBackStack(null)
                    .commit();
            }
        }
    }

    private void switchFragment(int newIndex, boolean first) {
        switchFragment(newIndex, first, null);
    }

    @Override
    public void onMenuSelected(int menu) {
        switchFragment(menu, false);
    }

    @Override
    public void onCampaignMenuSelected(String campaignToken) {
        switchFragment(LOCAL_CAMPAIGN_DEBUG_FRAGMENT, false, campaignToken);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.com_batchsdk_debug_view);
        if (savedInstanceState == null) {
            switchFragment(MAIN_DEBUG_FRAGMENT, true);
        }

        getActionBar().setTitle(R.string.com_batchsdk_debug_view_title);
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
        super.onDestroy();
    }
}
