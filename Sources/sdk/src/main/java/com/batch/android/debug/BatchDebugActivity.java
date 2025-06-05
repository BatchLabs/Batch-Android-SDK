package com.batch.android.debug;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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

    private final Fragment[] fragments = new Fragment[5];

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
                fragmentTransaction.replace(R.id.com_batchsdk_debug_fragment_content, fragments[newIndex]).commitNow();
            } else {
                fragmentTransaction
                    .replace(R.id.com_batchsdk_debug_fragment_content, fragments[newIndex])
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

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.com_batchsdk_debug_view_title);
        }
        setupWindowInsets();
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

    private void setupWindowInsets() {
        Window window = getWindow();
        if (window == null) {
            return;
        }
        // Set the window to not fit system windows
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // Set the status bar and navigation bar to be light
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(
            window,
            window.getDecorView()
        );
        windowInsetsController.setAppearanceLightStatusBars(true);
        windowInsetsController.setAppearanceLightNavigationBars(true);

        // Apply insets to the container view
        LinearLayout containerView = findViewById(R.id.com_batchsdk_debug_fragment_container);
        ViewCompat.setOnApplyWindowInsetsListener(
            containerView,
            (view, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                if (params != null) {
                    params.leftMargin = insets.left;
                    params.bottomMargin = insets.bottom;
                    params.rightMargin = insets.right;
                    params.topMargin = insets.top;
                    view.setLayoutParams(params);
                }
                return WindowInsetsCompat.CONSUMED;
            }
        );
        ViewCompat.requestApplyInsets(containerView);
    }
}
