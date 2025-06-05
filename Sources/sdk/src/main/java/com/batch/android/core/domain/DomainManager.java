package com.batch.android.core.domain;

import android.annotation.SuppressLint;
import com.batch.android.core.Logger;
import com.batch.android.processor.Module;
import com.batch.android.processor.Singleton;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Module
@Singleton
public class DomainManager implements IDomainManager {

    //region Parameters
    public static final int DNS_DOMAIN_ERROR_LIMIT_COUNT = 3;
    public static final int DNS_DOMAIN_ERROR_MIN_DELAY_SECOND = 5;
    private static final boolean DNS_FALLBACK_FEATURE_FLAG = false;
    private static final String TAG = "DomainManager";

    private final IDomainStore store;

    public static final List<String> domains = Arrays.asList(DomainStore.DNS_DOMAIN_ORIGINAL, "batch.io");

    //endregion

    //region Initializer
    public DomainManager(IDomainStore store) {
        this.store = store;
    }

    //endregion

    //region Functions
    String nextDomain() {
        /// Feature flag check
        if (!isFeatureActivated()) {
            return DomainStore.DNS_DOMAIN_ORIGINAL;
        }

        /// Get the current domain index
        int currentDomainIndex = DomainManager.domains.indexOf(store.getCurrentDomain());
        if (currentDomainIndex == -1) {
            return DomainStore.DNS_DOMAIN_ORIGINAL;
        }

        /// Determine the next domain index
        int index = currentDomainIndex + 1;

        /// Check the range of the next domain index
        if (index < domains.size()) {
            // Return the new domain
            return domains.get(index);
        } else {
            return store.getCurrentDomain();
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void updateDomainIfNeeded() {
        /// Feature flag check
        if (!isFeatureActivated()) {
            return;
        }

        /// No previous error case
        Date lastErrorUpdateDate = store.lastErrorUpdateDate();
        if (lastErrorUpdateDate == null) {
            store.incrementErrorCount();
            return;
        }

        /// Throttler guard case
        long delay = new Date().getTime() - lastErrorUpdateDate.getTime();
        if (delay < DNS_DOMAIN_ERROR_MIN_DELAY_SECOND * 1000L) {
            Logger.internal(
                TAG,
                String.format(
                    "Skip incrementation due to delay case: '%d' ms, should be less than '%d' ms",
                    delay,
                    DNS_DOMAIN_ERROR_MIN_DELAY_SECOND * 1000L
                )
            );
            return;
        } else {
            Logger.internal(
                TAG,
                String.format(
                    "Incrementation due to delay: '%d' ms,  is greater than '%d' ms",
                    delay,
                    DNS_DOMAIN_ERROR_MIN_DELAY_SECOND * 1000L
                )
            );
        }

        int errorCount = store.incrementErrorCount();

        /// Domain limit error reached case
        if (errorCount < DNS_DOMAIN_ERROR_LIMIT_COUNT) {
            return;
        }

        /// Determine the next domain to use
        String newValue = nextDomain();

        if (newValue.equals(store.getCurrentDomain())) {
            return;
        }

        /// Update the current domain with the next domain
        store.updateDomain(newValue);
    }

    @Override
    public void updateLastCheckDomainDate() {
        /// Feature flag check
        if (!isFeatureActivated()) {
            return;
        }

        store.updateLastCheckDomainDate();
    }

    @Override
    public void resetDomainToOriginal() {
        /// Feature flag check
        if (!isFeatureActivated()) {
            return;
        }

        store.resetDomainToOriginal();
    }

    @Override
    public void resetErrorCountIfNeeded() {
        /// Feature flag check
        if (!isFeatureActivated()) {
            return;
        }

        store.resetErrorCountIfNeeded();
    }

    @Override
    public boolean canCheckOriginalDomainAvailability() {
        /// Feature flag check
        if (!isFeatureActivated()) {
            return false;
        }

        return store.canCheckOriginalDomainAvailability();
    }

    @Override
    public String url(DomainService service, boolean overrideWithOriginal) {
        return service.url(
            (overrideWithOriginal || !isFeatureActivated()) ? DomainStore.DNS_DOMAIN_ORIGINAL : store.getCurrentDomain()
        );
    }

    @Override
    public boolean isOriginalDomain(String domain) {
        return store.isOriginalDomain(domain);
    }

    @Override
    public boolean isCurrentDomain(String domain) {
        return store.isCurrentDomain(domain);
    }

    @Override
    public boolean isFeatureActivated() {
        return DNS_FALLBACK_FEATURE_FLAG;
    }
    //endregion
}
