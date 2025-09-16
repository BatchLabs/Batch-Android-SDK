package com.batch.android.post;

import android.content.Context;
import androidx.annotation.NonNull;
import com.batch.android.WebserviceParameterUtils;
import com.batch.android.core.ByteArrayHelper;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.CampaignManagerProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.SQLUserDatasourceProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.ViewTracker;
import com.batch.android.localcampaigns.ViewTrackerUnavailableException;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.query.response.LocalCampaignsResponse;
import com.batch.android.user.SQLUserDatasource;
import com.batch.android.user.UserAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class LocalCampaignsJITPostDataProvider extends JSONPostDataProvider {

    private static final String TAG = "LocalCampaignsJITPostDataProvider";

    private static final String IDS_KEY = "ids";
    private static final String CAMPAIGNS_KEY = "campaigns";
    private static final String ATTRIBUTES_KEY = "attributes";
    private static final String VIEWS_KEY = "views";
    private static final String COUNT_KEY = "count";
    private static final String ELIGIBLE_CAMPAIGNS_KEY = "eligibleCampaigns";

    @NonNull
    private final Collection<LocalCampaign> campaigns;

    @NonNull
    private final LocalCampaignsResponse.Version version;

    public LocalCampaignsJITPostDataProvider(
        @NonNull Collection<LocalCampaign> campaigns,
        @NonNull LocalCampaignsResponse.Version campaignsVersion
    ) {
        this.campaigns = campaigns;
        this.version = campaignsVersion;
    }

    @Override
    public JSONObject getRawData() {
        ViewTracker viewTracker = CampaignManagerProvider.get().getViewTracker();

        JSONObject postData = new JSONObject();

        // Adding system ids
        JSONObject ids = new JSONObject();
        String customUserId = null;
        Context context = RuntimeManagerProvider.get().getContext();
        if (context != null) {
            ids = WebserviceParameterUtils.getWebserviceIdsAsJson(context);
            customUserId = UserModuleProvider.get().getCustomID(context);
        }

        // Adding campaigns ids to check
        List<String> campaignIdsList = new ArrayList<>();
        for (LocalCampaign campaign : campaigns) {
            campaignIdsList.add(campaign.id);
        }
        JSONArray campaignIds = new JSONArray(campaignIdsList);

        // Adding views count for each campaign
        JSONObject views = new JSONObject();
        Map<String, Integer> counts;
        try {
            if (version == LocalCampaignsResponse.Version.CEP) {
                counts = viewTracker.getViewCountsByCampaignIdsAndCustomUserId(campaignIdsList, customUserId);
            } else {
                counts = viewTracker.getViewCountsByCampaignIds(campaignIdsList);
            }
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                JSONObject countMap = new JSONObject();
                countMap.put(COUNT_KEY, entry.getValue());
                views.put(entry.getKey(), countMap);
            }
        } catch (ViewTrackerUnavailableException e) {
            Logger.internal(TAG, "Could not get view tracker count", e);
        } catch (JSONException e) {
            Logger.internal(TAG, "Error while serializing view counts", e);
        }

        // Adding attributes
        if (version == LocalCampaignsResponse.Version.MEP) {
            final SQLUserDatasource datasource = SQLUserDatasourceProvider.get(context);
            Map<String, Object> attributes = UserAttribute.getServerMapRepresentation(datasource.getAttributes(), true);
            try {
                postData.put(ATTRIBUTES_KEY, new JSONObject(attributes));
            } catch (JSONException e) {
                Logger.internal(TAG, "Error while serializing attributes", e);
            }
        }

        try {
            postData.put(IDS_KEY, ids);
            postData.put(CAMPAIGNS_KEY, campaignIds);
            postData.put(VIEWS_KEY, views);
        } catch (JSONException e) {
            Logger.internal(TAG, "Error while serializing JIT request", e);
        }
        return postData;
    }

    @Override
    public byte[] getData() {
        return ByteArrayHelper.getUTF8Bytes(getRawData().toString());
    }

    public List<String> deserializeResponse(JSONObject response) throws JSONException {
        JSONArray campaigns = response.getJSONArray(ELIGIBLE_CAMPAIGNS_KEY);
        List<String> eligibleCampaigns = new ArrayList<>();
        for (int i = 0; i < campaigns.length(); i++) {
            eligibleCampaigns.add(campaigns.getString(i));
        }
        return eligibleCampaigns;
    }
}
