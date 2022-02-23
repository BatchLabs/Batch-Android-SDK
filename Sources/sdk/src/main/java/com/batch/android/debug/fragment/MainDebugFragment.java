package com.batch.android.debug.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.batch.android.R;
import com.batch.android.debug.BatchDebugActivity;
import com.batch.android.debug.OnMenuSelectedListener;

public class MainDebugFragment extends Fragment {

    private OnMenuSelectedListener listener;

    public static MainDebugFragment newInstance() {
        return new MainDebugFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (OnMenuSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnMenuSelectedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.com_batchsdk_main_debug_fragment, container, false);
        View identifier = view.findViewById(R.id.com_batchsdk_main_debug_fragment_identifier);
        identifier.setOnClickListener((View v) -> {
            listener.onMenuSelected(BatchDebugActivity.IDENTIFIER_DEBUG_FRAGMENT);
        });

        View userData = view.findViewById(R.id.com_batchsdk_main_debug_fragment_user_data);
        userData.setOnClickListener((View v) -> {
            listener.onMenuSelected(BatchDebugActivity.USER_DATA_DEBUG_FRAGMENT);
        });

        View localCampaign = view.findViewById(R.id.com_batchsdk_main_debug_fragment_local_campaign);
        localCampaign.setOnClickListener((View v) -> {
            listener.onMenuSelected(BatchDebugActivity.LOCAL_CAMPAIGNS_DEBUG_FRAGMENT);
        });
        return view;
    }
}
