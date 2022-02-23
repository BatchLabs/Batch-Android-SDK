package com.batch.android;

import com.batch.android.messaging.model.BannerMessage;

/**
 * Helper to access package private methods of {@link BatchBannerView}
 *
 * @hide
 */
public class BatchBannerViewPrivateHelper {

    public static BatchBannerView newInstance(
        BatchMessage rawMsg,
        BannerMessage msg,
        MessagingAnalyticsDelegate analyticsDelegate
    ) {
        return new BatchBannerView(rawMsg, msg, analyticsDelegate);
    }
}
