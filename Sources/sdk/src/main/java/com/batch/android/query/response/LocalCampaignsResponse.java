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

    /**
     * Global in-app cappings
     */
    private GlobalCappings cappings;

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

    @Nullable
    public GlobalCappings getCappings() {
        return cappings;
    }

    public void setCappings(GlobalCappings cappings) {
        this.cappings = cappings;
    }

    public boolean hasCappings() {
        return cappings != null;
    }

    /**
     * Global In-App Cappings
     */
    public static class GlobalCappings {

        /**
         * Time-Based Capping
         * Eg: Display no more than 3 in-apps every 1 hours
         */
        public static class TimeBasedCapping {

            /**
             * Number of views allowed
             */
            private final Integer views;

            /**
             * Capping duration (in seconds)
             */
            private final Integer duration;

            public TimeBasedCapping(Integer views, Integer duration) {
                this.views = views;
                this.duration = duration;
            }

            @Nullable
            public Integer getViews() {
                return views;
            }

            @Nullable
            public Integer getDuration() {
                return duration;
            }
        }

        /**
         * Number of in-apps displayable during a user session
         */
        private final Integer session;

        /**
         * List of time-based cappings
         */
        private final List<TimeBasedCapping> timeBasedCappings;

        public GlobalCappings(Integer session, List<TimeBasedCapping> timeBasedCappings) {
            this.session = session;
            this.timeBasedCappings = timeBasedCappings;
        }

        @Nullable
        public Integer getSession() {
            return session;
        }

        @Nullable
        public List<TimeBasedCapping> getTimeBasedCappings() {
            return timeBasedCappings;
        }
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
