package com.batch.android.query;

import android.content.Context;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.CampaignManager;
import com.batch.android.localcampaigns.ViewTracker;
import com.batch.android.localcampaigns.ViewTrackerUnavailableException;
import com.batch.android.localcampaigns.model.LocalCampaign;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Query for local local campaigns sync
 */

public class LocalCampaignsQuery extends Query {

    private static final String TAG = "LocalCampaignsQuery";

    /**
     * Campaign Ids associated to their capping
     */
    private Map<String, Integer> capping = new HashMap<>();

    public LocalCampaignsQuery(CampaignManager campaignManager, Context context) {
        super(context, QueryType.LOCAL_CAMPAIGNS);
        // Add capping of campaigns loaded
        List<LocalCampaign> localCampaigns = campaignManager.getCampaignList();
        ViewTracker viewTracker = campaignManager.getViewTracker();

        List<String> campaignsIds = new ArrayList<>(localCampaigns.size());
        for (LocalCampaign campaign : localCampaigns) {
            campaignsIds.add(campaign.id);
        }

        try {
            capping.putAll(viewTracker.getViewCounts(campaignsIds));
        } catch (ViewTrackerUnavailableException e) {
            Logger.internal(TAG, "View tracker unavailable: can't send view counts to the backend.");
        }
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        JSONObject viewsJson = new JSONObject();
        for (Map.Entry<String, Integer> entry : capping.entrySet()) {
            JSONObject countJson = new JSONObject();
            countJson.put("count", entry.getValue());
            viewsJson.put(entry.getKey(), countJson);
        }
        json.put("views", viewsJson);
        return json;
    }
}
