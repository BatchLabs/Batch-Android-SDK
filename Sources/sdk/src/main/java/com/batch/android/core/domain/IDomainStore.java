package com.batch.android.core.domain;

import androidx.annotation.Nullable;
import java.util.Date;

public interface IDomainStore {
    /**
     * Determines the current domain.
     * <p>
     * This domain is typically the one being used in the app for performing operations.
     *
     * @return The current domain.
     */
    String getCurrentDomain();

    /**
     * Updates the current domain with a new provided domain.
     * <p>
     * If {@code domain} is null, it resets the domain to {@code DomainStore.DNS_DOMAIN_ORIGINAL}.
     *
     * @param domain The new domain to set, or null to reset to the original domain.
     */
    void updateDomain(@Nullable String domain);

    /**
     * Increments the error count and returns the new error count value.
     * <p>
     * Used to track the number of errors related to the domain.
     *
     * @return The new error count value.
     */
    int incrementErrorCount();

    /**
     * Updates the date of the last check performed on the domain.
     */
    void updateLastCheckDomainDate();

    /**
     * Resets the domain to its original value {@code DomainStore.DNS_DOMAIN_ORIGINAL}.
     */
    void resetDomainToOriginal();

    /**
     * Resets the error count if needed, for instance if a successful API call was done on the current domain.
     */
    void resetErrorCountIfNeeded();

    /**
     * Returns the date of the last error update, or null if no error has been recorded.
     *
     * @return The date of the last error update, or null.
     */
    @Nullable
    Date lastErrorUpdateDate();

    /**
     * Determines if the original domain can be checked for availability.
     *
     * @return True if the original domain can be checked, false otherwise.
     */
    boolean canCheckOriginalDomainAvailability();

    /**
     * Determine if the given domain is the original one
     */
    boolean isOriginalDomain(String domain);

    /**
     * Determine if the given domain is the current one
     */
    boolean isCurrentDomain(String domain);
}
