package com.batch.android.post;

import android.content.Context;
import androidx.annotation.NonNull;
import com.batch.android.WebserviceParameterUtils;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.CampaignManagerProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.SQLUserDatasourceProvider;
import com.batch.android.localcampaigns.ViewTracker;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.msgpack.MessagePackHelper;
import com.batch.android.msgpack.core.MessageBufferPacker;
import com.batch.android.msgpack.core.MessagePack;
import com.batch.android.msgpack.core.MessageUnpacker;
import com.batch.android.user.SQLUserDatasource;
import com.batch.android.user.UserAttribute;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalCampaignsJITPostDataProvider extends MessagePackPostDataProvider<Collection<LocalCampaign>> {

    private static final String TAG = "LocalCampaignsJITPostDataProvider";

    private static final String IDS_KEY = "ids";
    private static final String CAMPAIGNS_KEY = "campaigns";
    private static final String ATTRIBUTES_KEY = "attributes";
    private static final String VIEWS_KEY = "views";
    private static final String COUNT_KEY = "count";
    private static final String ELIGIBLE_CAMPAIGNS_KEY = "eligibleCampaigns";

    private final Collection<LocalCampaign> campaigns;

    public LocalCampaignsJITPostDataProvider(Collection<LocalCampaign> campaigns) {
        this.campaigns = campaigns;
    }

    @Override
    public Collection<LocalCampaign> getRawData() {
        return campaigns;
    }

    @Override
    byte[] pack() throws Exception {
        ViewTracker viewTracker = CampaignManagerProvider.get().getViewTracker();
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

        Map<String, Object> postData = new HashMap<>();

        // Adding system ids
        Map<String, Object> ids = new HashMap<>();
        Context context = RuntimeManagerProvider.get().getContext();
        if (context != null) {
            ids = WebserviceParameterUtils.getWebserviceIdsAsMap(context);
        }

        // Adding campaigns ids to check
        List<String> campaignIds = new ArrayList<>();
        for (LocalCampaign campaign : campaigns) {
            campaignIds.add(campaign.id);
        }

        // Adding views count for each campaign
        Map<String, Integer> counts = viewTracker.getViewCounts(campaignIds);
        Map<String, Object> views = new HashMap<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            Map<String, Object> countMap = new HashMap<>();
            countMap.put(COUNT_KEY, entry.getValue());
            views.put(entry.getKey(), countMap);
        }

        // Adding attributes
        final SQLUserDatasource datasource = SQLUserDatasourceProvider.get(context);
        Map<String, Object> attributes = UserAttribute.getServerMapRepresentation(datasource.getAttributes());

        postData.put(IDS_KEY, ids);
        postData.put(CAMPAIGNS_KEY, campaignIds);
        postData.put(ATTRIBUTES_KEY, attributes);
        postData.put(VIEWS_KEY, views);

        MessagePackHelper.packObject(packer, postData);
        packer.close();
        return packer.toByteArray();
    }

    @NonNull
    public List<String> unpack(byte[] data) {
        List<String> eligibleCampaigns = new ArrayList<>();
        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
        try {
            // Unpack root map header
            unpacker.unpackMapHeader();

            // Unpack "eligibleCampaigns" key
            String key = unpacker.unpackString();

            if (ELIGIBLE_CAMPAIGNS_KEY.equals(key)) {
                // Unpack list of campaign id
                int eligibleCampaignsSize = unpacker.unpackArrayHeader();
                for (int i = 0; i < eligibleCampaignsSize; i++) {
                    String campaignId = unpacker.unpackString();
                    eligibleCampaigns.add(campaignId);
                }
            }
        } catch (IOException e) {
            Logger.internal(TAG, "Could not unpack campaign jit response.");
        }
        return eligibleCampaigns;
    }

    @Override
    public boolean isEmpty() {
        return campaigns.isEmpty();
    }
}
