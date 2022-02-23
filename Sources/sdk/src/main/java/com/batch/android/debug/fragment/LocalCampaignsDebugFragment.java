package com.batch.android.debug.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.batch.android.FailReason;
import com.batch.android.LocalCampaignsWebservice;
import com.batch.android.R;
import com.batch.android.core.Logger;
import com.batch.android.debug.OnMenuSelectedListener;
import com.batch.android.di.providers.LocalCampaignsWebserviceListenerImplProvider;
import com.batch.android.di.providers.TaskExecutorProvider;
import com.batch.android.localcampaigns.CampaignManager;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.query.response.LocalCampaignsResponse;
import com.batch.android.webservice.listener.LocalCampaignsWebserviceListener;
import java.util.ArrayList;
import java.util.List;

public class LocalCampaignsDebugFragment extends Fragment {

    private static final String TAG = "LocalCampaignsDebugFragment";

    private TextView title;
    private ListView campaignList;

    private ArrayAdapter<String> campaignAdapter;
    private OnMenuSelectedListener listener;

    private CampaignManager campaignManager;

    private LocalCampaignsWebserviceListener webserviceListener = new LocalCampaignsWebserviceListener() {
        private LocalCampaignsWebserviceListener sdkImpl = LocalCampaignsWebserviceListenerImplProvider.get();

        @Override
        public void onSuccess(List<LocalCampaignsResponse> response) {
            sdkImpl.onSuccess(response);
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> loadLocalCampaigns());
            }
        }

        @Override
        public void onError(FailReason reason) {
            sdkImpl.onError(reason);
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> loadLocalCampaigns());
            }
        }
    };

    public static LocalCampaignsDebugFragment newInstance(CampaignManager campaignManager) {
        LocalCampaignsDebugFragment frag = new LocalCampaignsDebugFragment();
        frag.setCampaignManager(campaignManager);
        return frag;
    }

    private void setCampaignManager(CampaignManager campaignManager) {
        this.campaignManager = campaignManager;
    }

    private void refreshLocalCampaigns() {
        Toast
            .makeText(getContext(), R.string.com_batchsdk_local_campaign_debug_fragment_refreshing, Toast.LENGTH_SHORT)
            .show();

        try {
            TaskExecutorProvider
                .get(getActivity().getApplicationContext())
                .submit(new LocalCampaignsWebservice(getActivity().getApplicationContext(), webserviceListener));
        } catch (Exception e) {
            Logger.internal(TAG, "Error while refreshing in-app campaigns", e);
        }
    }

    private void loadLocalCampaigns() {
        List<String> campaignsToken = new ArrayList<>();
        for (LocalCampaign campaign : campaignManager.getCampaignList()) {
            if (campaign.publicToken != null) {
                campaignsToken.add(campaign.publicToken);
            }
        }

        if (campaignAdapter == null) {
            campaignAdapter =
                new ArrayAdapter<>(
                    getContext(),
                    android.R.layout.simple_list_item_1,
                    android.R.id.text1,
                    campaignsToken
                );
        } else {
            campaignAdapter.clear();
            campaignAdapter.addAll(campaignsToken);
        }
        campaignList.setAdapter(campaignAdapter);
        title.setText(getString(R.string.com_batchsdk_local_campaign_debug_fragment_title, campaignsToken.size()));
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
        View view = inflater.inflate(R.layout.com_batchsdk_local_campaigns_debug_fragment, container, false);
        title = view.findViewById(R.id.com_batchsdk_local_campaign_debug_fragment_title);
        campaignList = view.findViewById(R.id.com_batchsdk_local_campaign_debug_fragment_list);
        campaignList.setOnItemClickListener((AdapterView<?> parent, View itemView, int position, long id) -> {
            listener.onCampaignMenuSelected(campaignAdapter.getItem(position));
        });

        Button refreshButton = view.findViewById(R.id.com_batchsdk_local_campaign_debug_fragment_refresh_button);
        refreshButton.setOnClickListener((View v) -> {
            refreshLocalCampaigns();
        });
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadLocalCampaigns();
    }
}
