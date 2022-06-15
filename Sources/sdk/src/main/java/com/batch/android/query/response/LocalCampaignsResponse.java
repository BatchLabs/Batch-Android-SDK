package com.batch.android.query.response;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.query.LocalCampaignsQuery;
import com.batch.android.query.QueryType;
import java.util.ArrayList;
import java.util.List;

/**
 * Response for {@link LocalCampaignsQuery}
 */

public class LocalCampaignsResponse extends Response {

    private static final String TAG = "LocalCampaignsResponse";

    /**
     * Local campaign response error
     */
    private Error error;

    /**
     * List of local campaigns
     */
    private List<LocalCampaign> campaigns;

    /**
     * Optional, time between all campaigns in seconds
     */
    private Long minDisplayInterval;

    public LocalCampaignsResponse(String queryID) {
        super(QueryType.LOCAL_CAMPAIGNS, queryID);
    }

    public boolean hasCampaigns() {
        return campaigns != null && campaigns.size() > 0;
    }

    @NonNull
    public List<LocalCampaign> getCampaigns() {
        return campaigns != null ? campaigns : new ArrayList<>();
    }

    @NonNull
    public List<LocalCampaign> getCampaignsToSave() {
        List<LocalCampaign> campaignsToSave = new ArrayList<>();
        if (campaigns == null || campaigns.isEmpty()) {
            return campaignsToSave;
        }
        for (LocalCampaign campaign : campaigns) {
            if (campaign.persist) {
                campaignsToSave.add(campaign);
            }
        }
        return campaignsToSave;
    }

    @Nullable
    public Long getMinDisplayInterval() {
        return minDisplayInterval;
    }

    public void setCampaigns(List<LocalCampaign> campaigns) {
        this.campaigns = campaigns;
    }

    public void setMinDisplayInterval(Long minDisplayInterval) {
        this.minDisplayInterval = minDisplayInterval;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public boolean hasError() {
        return error != null;
    }

    public static class Error {

        /**
         * Error code
         */
        private int code;

        /**
         * Error message
         */
        private String message;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @NonNull
        @Override
        public String toString() {
            return "Error{" + "code=" + code + ", message='" + message + '\'' + '}';
        }
    }
}
