package com.batch.android;

import com.batch.android.messaging.model.Message;

/**
 * Helper to access package private methods of {@link BatchBannerView}
 *
 * @hide
 */
public class BatchBannerViewPrivateHelper {

    public static BatchBannerView newInstance(
        BatchMessage rawMsg,
        Message msg,
        MessagingAnalyticsDelegate analyticsDelegate
    ) {
        return new BatchBannerView(rawMsg, msg, analyticsDelegate);
    }
}
