package com.batch.android.core.domain;

interface IDomainManager {
    boolean isFeatureActivated();
    /**
     * Update current domain if needed
     */
    void updateDomainIfNeeded();

    /**
     * Updates the date of the last check performed on the domain.
     */
    void updateLastCheckDomainDate();

    /**
     * Reset current domain to the original one
     */
    void resetDomainToOriginal();

    /**
     * Resets the error count if needed, for instance if a success api call was done on the current domain.
     */
    void resetErrorCountIfNeeded();

    /**
     * Determines if the original domain can be checked for availability.
     */
    boolean canCheckOriginalDomainAvailability();

    /**
     * Returns the URL associated with a specific service.
     * The `overrideWithOriginal` parameter determines whether the URL should be based on the original domain or the current domain.
     */
    String url(DomainService service, boolean overrideWithOriginal);

    /**
     * Determine if the given domain is the original one
     */
    boolean isOriginalDomain(String domain);

    /**
     * Determine if the given domain is the current one
     */
    boolean isCurrentDomain(String domain);
}
