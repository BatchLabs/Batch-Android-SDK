package com.batch.android.debug.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.batch.android.R;
import com.batch.android.date.BatchDate;
import com.batch.android.localcampaigns.CampaignManager;
import com.batch.android.localcampaigns.model.LocalCampaign;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocalCampaignDebugFragment extends Fragment implements View.OnClickListener {

    private static final String CAMPAIGN_TOKEN_KEY = "campaign_token";

    private TextView token;
    private TextView startDate;
    private TextView endDate;
    private TextView capping;
    private TextView gracePeriod;
    private TextView trigger;

    private CampaignManager campaignManager;

    public static LocalCampaignDebugFragment newInstance(String campaignToken, CampaignManager campaignManager) {
        Bundle args = new Bundle();
        args.putString(CAMPAIGN_TOKEN_KEY, campaignToken);

        LocalCampaignDebugFragment fragment = new LocalCampaignDebugFragment();
        fragment.setArguments(args);
        fragment.setCampaignManager(campaignManager);
        return fragment;
    }

    private void setCampaignManager(CampaignManager campaignManager) {
        this.campaignManager = campaignManager;
    }

    private @Nullable LocalCampaign getCurrentCampaign() {
        String campaignToken = getArguments().getString(CAMPAIGN_TOKEN_KEY);
        if (campaignToken != null) {
            for (LocalCampaign campaign : campaignManager.getCampaignList()) {
                if (campaign.publicToken != null && campaign.publicToken.equals(campaignToken)) {
                    return campaign;
                }
            }
        }
        return null;
    }

    private String formatDate(BatchDate date) {
        DateFormat df = new SimpleDateFormat(getString(R.string.com_batchsdk_debug_date_format), Locale.US);
        return df.format(new Date(date.getTime()));
    }

    private String getShareString(LocalCampaign campaign) {
        String shareContent = "Batch SDK - In-App Campaign:\n";

        if (campaign.publicToken != null) {
            shareContent =
                shareContent.concat(
                    String.format(
                        "%s: %s\n",
                        getString(R.string.com_batchsdk_local_campaign_debug_fragment_token),
                        campaign.publicToken
                    )
                );
        } else {
            shareContent =
                shareContent.concat(
                    String.format(
                        "%s: %s\n",
                        getString(R.string.com_batchsdk_local_campaign_debug_fragment_token),
                        getString(R.string.com_batchsdk_debug_view_empty)
                    )
                );
        }

        if (campaign.startDate != null) {
            shareContent =
                shareContent.concat(
                    String.format(
                        "%s: %s\n",
                        getString(R.string.com_batchsdk_local_campaign_debug_fragment_start_date),
                        formatDate(campaign.startDate)
                    )
                );
        } else {
            shareContent =
                shareContent.concat(
                    String.format(
                        "%s: %s\n",
                        getString(R.string.com_batchsdk_local_campaign_debug_fragment_start_date),
                        getString(R.string.com_batchsdk_debug_view_empty)
                    )
                );
        }

        if (campaign.endDate != null) {
            shareContent =
                shareContent.concat(
                    String.format(
                        "%s: %s\n",
                        getString(R.string.com_batchsdk_local_campaign_debug_fragment_end_date),
                        formatDate(campaign.endDate)
                    )
                );
        } else {
            shareContent =
                shareContent.concat(
                    String.format(
                        "%s: %s\n",
                        getString(R.string.com_batchsdk_local_campaign_debug_fragment_end_date),
                        getString(R.string.com_batchsdk_debug_view_empty)
                    )
                );
        }

        if (campaign.capping != null) {
            shareContent =
                shareContent.concat(
                    String.format(
                        Locale.US,
                        "%s: %d\n",
                        getString(R.string.com_batchsdk_local_campaign_debug_fragment_capping),
                        campaign.capping
                    )
                );
        } else {
            shareContent =
                shareContent.concat(
                    String.format(
                        "%s: %s\n",
                        getString(R.string.com_batchsdk_local_campaign_debug_fragment_capping),
                        getString(R.string.com_batchsdk_debug_view_empty)
                    )
                );
        }

        shareContent =
            shareContent.concat(
                String.format(
                    Locale.US,
                    "%s: %d\n",
                    getString(R.string.com_batchsdk_local_campaign_debug_fragment_period),
                    campaign.minimumDisplayInterval
                )
            );

        if (!campaign.triggers.isEmpty()) {
            StringBuilder triggers = new StringBuilder();
            for (LocalCampaign.Trigger trigger : campaign.triggers) {
                triggers.append(trigger.getClass().getSimpleName());
            }

            shareContent =
                shareContent.concat(
                    String.format(
                        "%s: %s\n",
                        getString(R.string.com_batchsdk_local_campaign_debug_fragment_trigger),
                        triggers.toString()
                    )
                );
        } else {
            shareContent =
                shareContent.concat(
                    String.format(
                        "%s: %s\n",
                        getString(R.string.com_batchsdk_local_campaign_debug_fragment_trigger),
                        getString(R.string.com_batchsdk_debug_view_empty)
                    )
                );
        }
        return shareContent;
    }

    private void displayCampaign(LocalCampaign campaign) {
        if (campaign.publicToken != null) {
            token.setText(campaign.publicToken);
        } else {
            token.setText(R.string.com_batchsdk_debug_view_empty);
        }

        if (campaign.startDate != null) {
            startDate.setText(formatDate(campaign.startDate));
        } else {
            startDate.setText(R.string.com_batchsdk_debug_view_empty);
        }

        if (campaign.endDate != null) {
            endDate.setText(formatDate(campaign.endDate));
        } else {
            endDate.setText(R.string.com_batchsdk_debug_view_empty);
        }

        if (campaign.capping != null) {
            capping.setText(String.format(Locale.US, "%d", campaign.capping));
        } else {
            capping.setText(R.string.com_batchsdk_debug_view_empty);
        }

        gracePeriod.setText(String.format(Locale.US, "%d", campaign.minimumDisplayInterval));
        if (!campaign.triggers.isEmpty()) {
            StringBuilder triggers = new StringBuilder();
            for (LocalCampaign.Trigger trigger : campaign.triggers) {
                triggers.append(trigger.getClass().getSimpleName());
            }
            trigger.setText(triggers.toString());
        } else {
            trigger.setText(R.string.com_batchsdk_debug_view_empty);
        }
    }

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.com_batchsdk_local_campaign_debug_fragment, container, false);
        token = view.findViewById(R.id.com_batchsdk_local_campaign_debug_fragment_token);
        startDate = view.findViewById(R.id.com_batchsdk_local_campaign_debug_fragment_start_date);
        endDate = view.findViewById(R.id.com_batchsdk_local_campaign_debug_fragment_end_date);
        capping = view.findViewById(R.id.com_batchsdk_local_campaign_debug_fragment_capping);
        gracePeriod = view.findViewById(R.id.com_batchsdk_local_campaign_debug_fragment_period);
        trigger = view.findViewById(R.id.com_batchsdk_local_campaign_debug_fragment_trigger);
        View shareButton = view.findViewById(R.id.com_batchsdk_identifier_debug_fragment_share_button);
        shareButton.setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LocalCampaign campaign = getCurrentCampaign();
        if (campaign != null) {
            displayCampaign(campaign);
        }
    }

    @Override
    public void onClick(View v) {
        LocalCampaign campaign = getCurrentCampaign();
        if (campaign != null) {
            String shareContent = getShareString(campaign);
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.com_batchsdk_debug_view_title));
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.com_batchsdk_debug_view_share)));
        }
    }
}
