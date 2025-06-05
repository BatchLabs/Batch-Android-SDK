package com.batch.android.core.domain;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.metrics.MetricRegistry;
import java.util.Date;

public class DomainStore implements IDomainStore {

    //region Parameters
    private static final String TAG = "DomainStore";
    private static final Integer DNS_DOMAIN_ERROR_MIN_DELAY_SECOND = 5;
    protected static final Integer DNS_DOMAIN_LAST_CHECK_MIN_DELAY_SECOND = 172800; // 48 hours
    public static final String DNS_DOMAIN_ORIGINAL = "batch.com";

    //endregion

    //region Functions
    @NonNull
    @Override
    public String getCurrentDomain() {
        return ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .get(ParameterKeys.DNS_DOMAIN_KEY, DomainManager.domains.get(0));
    }

    @Override
    public void updateDomain(@Nullable String domain) {
        /// Reset domain error parameter values
        resetErrorCountIfNeeded();

        if (domain != null) {
            /// Save the new domain
            Logger.internal(
                TAG,
                String.format("Set BAParameter key: '%s' with '%s'.", ParameterKeys.DNS_DOMAIN_KEY, domain)
            );

            ParametersProvider
                .get(RuntimeManagerProvider.get().getContext())
                .set(ParameterKeys.DNS_DOMAIN_KEY, domain, true);
        } else {
            /// Reset the domain
            Logger.internal(TAG, String.format("Remove BAParameter key: '%s'.", ParameterKeys.DNS_DOMAIN_KEY));

            ParametersProvider.get(RuntimeManagerProvider.get().getContext()).remove(ParameterKeys.DNS_DOMAIN_KEY);
        }

        /// Update domain last update date
        ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .set(ParameterKeys.DNS_DOMAIN_LAST_UPDATE_DATE, String.valueOf(new Date().getTime()), true);

        Logger.internal(TAG, String.format("New current domain: '%s.", getCurrentDomain()));
    }

    @SuppressLint("DefaultLocale")
    @Override
    public int incrementErrorCount() {
        int errorCount = Integer.parseInt(
            ParametersProvider
                .get(RuntimeManagerProvider.get().getContext())
                .get(ParameterKeys.DNS_DOMAIN_ERROR_COUNT_KEY, "0")
        );

        /// Increment the counter
        int newValue = errorCount + 1;
        Date errorUpdateDate = new Date();

        Logger.internal(
            TAG,
            String.format(
                "Increment BAParameter key: '%s' by 1, (old value: %d, new value: %d, domain: %s). %s",
                ParameterKeys.DNS_DOMAIN_ERROR_COUNT_KEY,
                errorCount,
                newValue,
                getCurrentDomain(),
                errorUpdateDate
            )
        );

        /// Save new the domain error values
        ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .set(ParameterKeys.DNS_DOMAIN_ERROR_COUNT_KEY, String.valueOf(newValue), true);
        ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .set(ParameterKeys.DNS_DOMAIN_ERROR_UPDATE_DATE, String.valueOf(errorUpdateDate.getTime()), true);

        /// Add metric
        MetricRegistry.dnsErrorCount.labels("KO").inc();

        return newValue;
    }

    @Override
    public void updateLastCheckDomainDate() {
        ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .set(ParameterKeys.DNS_DOMAIN_LAST_CHECK_DATE, String.valueOf(new Date().getTime()), true);
    }

    @Override
    public void resetDomainToOriginal() {
        updateDomain(null);
    }

    @Override
    public void resetErrorCountIfNeeded() {
        /// No domain error count case
        if (
            Integer.parseInt(
                ParametersProvider
                    .get(RuntimeManagerProvider.get().getContext())
                    .get(ParameterKeys.DNS_DOMAIN_ERROR_COUNT_KEY, "0")
            ) ==
            0
        ) {
            return;
        }

        /// Reset domain error count
        ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .remove(ParameterKeys.DNS_DOMAIN_ERROR_COUNT_KEY);

        ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .remove(ParameterKeys.DNS_DOMAIN_ERROR_UPDATE_DATE);

        /// Add metric
        MetricRegistry.dnsErrorCount.labels("OK").reset();
    }

    @Override
    @Nullable
    public Date lastErrorUpdateDate() {
        String value = ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .get(ParameterKeys.DNS_DOMAIN_ERROR_UPDATE_DATE, null);

        if (value == null) {
            return null;
        }
        return new Date(Long.parseLong(value));
    }

    @Override
    public boolean canCheckOriginalDomainAvailability() {
        /// No domain changes
        String currentDomain = ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .get(ParameterKeys.DNS_DOMAIN_KEY, null);
        String lastDomainUpdateDateStringValue = ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .get(ParameterKeys.DNS_DOMAIN_LAST_UPDATE_DATE, null);

        if (currentDomain == null || lastDomainUpdateDateStringValue == null) {
            return false;
        }
        /// Current domain is the original one
        if (currentDomain.equals(DNS_DOMAIN_ORIGINAL)) {
            return false;
        }

        String lastDomainCheckDateStringValue = ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .get(ParameterKeys.DNS_DOMAIN_LAST_CHECK_DATE, null);
        /// Never check case
        if (lastDomainCheckDateStringValue == null) {
            return true;
        }

        long lastDomainCheckDate = Long.parseLong(lastDomainCheckDateStringValue);
        /// Throttler case or all good
        return (lastDomainCheckDate + (DNS_DOMAIN_ERROR_MIN_DELAY_SECOND * 1000L)) < new Date().getTime();
    }

    @Override
    public boolean isOriginalDomain(String domain) {
        /// Avoid subdomain in comparison
        return domain.contains(DNS_DOMAIN_ORIGINAL);
    }

    @Override
    public boolean isCurrentDomain(String domain) {
        return domain.contains(getCurrentDomain());
    }
    //endregion
}
