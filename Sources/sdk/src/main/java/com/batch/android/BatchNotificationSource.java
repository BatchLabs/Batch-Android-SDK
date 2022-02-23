package com.batch.android;

import com.batch.android.annotation.PublicSDK;

/**
 * BatchNotificationSource represents how the push was sent from Batch: via the Transactional API, or using a Push Campaign
 * The value might be unknown for forward compatibility, or if the information was missing.
 */
@PublicSDK
public enum BatchNotificationSource {
    UNKNOWN,
    CAMPAIGN,
    TRANSACTIONAL,
    TRIGGER,
}
