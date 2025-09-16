package com.batch.android.core.domain;

import static com.batch.android.core.Parameters.SDK_VERSION;

import com.batch.android.di.providers.DomainManagerProvider;
import com.batch.android.processor.Singleton;

public final class DomainURLBuilder {

    /**
     * Retrieves the domain URL for the specified service.
     *
     * @param service The domain service to retrieve the URL for.
     * @return The domain URL for the specified service.
     */
    private static String getDomainURL(DomainService service, boolean shouldCheckOriginalDomainAvailability) {
        IDomainManager manager = DomainManagerProvider.get(new DomainStore());
        boolean canCheckOriginalDomainAvailability =
            shouldCheckOriginalDomainAvailability && manager.canCheckOriginalDomainAvailability();

        return manager.url(service, canCheckOriginalDomainAvailability);
    }

    /**
     * Base URL of the WS
     * To change this, simply build another type of the sdk (see build.gradle)
     */
    private static String buildBaseWebServiceURL(boolean shouldCheckOriginalDomainAvailability) {
        return getDomainURL(DomainService.WEB, shouldCheckOriginalDomainAvailability) + "/a/" + SDK_VERSION;
    }

    /**
     * URL of the start WS
     */
    public static final String START_WS_URL = buildBaseWebServiceURL(true) + "/st/%s";
    /**
     * URL of the restore WS
     */
    public static final String TRACKER_WS_URL = buildBaseWebServiceURL(false) + "/tr/%s";
    /**
     * URL of the push WS
     */
    public static final String PUSH_WS_URL = buildBaseWebServiceURL(false) + "/t/%s";
    /**
     * URL of the attributes send WS
     */
    public static final String ATTR_SEND_WS_URL = buildBaseWebServiceURL(false) + "/ats/%s";
    /**
     * URL of the attributes check WS
     */
    public static final String ATTR_CHECK_WS_URL = buildBaseWebServiceURL(false) + "/atc/%s";
    /**
     * URL of the local campaigns WS
     */
    public static final String LOCAL_CAMPAIGNS_WS_URL = buildBaseWebServiceURL(false) + "/inapp/%s";
    /**
     * URL of the inbox fetch WS
     */
    public static final String INBOX_FETCH_WS_URL = buildBaseWebServiceURL(false) + "/inbox/%s/%s/%s";
    /**
     * URL of the inbox sync WS
     */
    public static final String INBOX_SYNC_WS_URL = buildBaseWebServiceURL(false) + "/inbox/%s/sync/%s/%s";
    /**
     * URL of the metrics WS
     */
    public static final String METRIC_WS_URL = getDomainURL(DomainService.METRIC, false);

    /**
     * URL of the local campaigns JIT for MEP campaigns (check just in time) WS
     */
    public static final String LOCAL_CAMPAIGNS_JIT_MEP_WS_URL = buildBaseWebServiceURL(false) + "/inapp_jit_mep/%s";

    /**
     * URL of the local campaigns JIT for CEP campaigns (check just in time) WS
     */
    public static final String LOCAL_CAMPAIGNS_JIT_CEP_WS_URL = buildBaseWebServiceURL(false) + "/inapp_jit_cep/%s";
}
