package com.batch.android.core.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import java.util.Date;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DomainStoreTest {

    //region ParametersProvider
    @Nullable
    String getDomain() {
        return ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .get(ParameterKeys.DNS_DOMAIN_KEY, null);
    }

    @Nullable
    Integer getDomainErrorCount() {
        String value = ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .get(ParameterKeys.DNS_DOMAIN_ERROR_COUNT_KEY, null);

        if (value == null) {
            return null;
        }
        return value.transform(Integer::parseInt);
    }

    @Nullable
    Long getDomainErrorUpdateDate() {
        String value = ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .get(ParameterKeys.DNS_DOMAIN_ERROR_UPDATE_DATE, null);

        if (value == null) {
            return null;
        }
        return value.transform(Long::parseLong);
    }

    @Nullable
    Long getDomainLastUpdateDate() {
        String value = ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .get(ParameterKeys.DNS_DOMAIN_LAST_UPDATE_DATE, null);

        if (value == null) {
            return null;
        }
        return value.transform(Long::parseLong);
    }

    Long getDomainLastCheckDate() {
        String value = ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .get(ParameterKeys.DNS_DOMAIN_LAST_CHECK_DATE, null);

        if (value == null) {
            return null;
        }
        return value.transform(Long::parseLong);
    }

    //endregion

    private DomainStore store;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        RuntimeManagerProvider.get().setContext(context);

        store = new DomainStore();

        /// Remove local config
        Parameters parameters = ParametersProvider.get(RuntimeManagerProvider.get().getContext());

        parameters.remove(DomainStore.DNS_DOMAIN_ORIGINAL);
        parameters.remove(ParameterKeys.DNS_DOMAIN_ERROR_COUNT_KEY);
        parameters.remove(ParameterKeys.DNS_DOMAIN_ERROR_UPDATE_DATE);
        parameters.remove(ParameterKeys.DNS_DOMAIN_LAST_UPDATE_DATE);
        parameters.remove(ParameterKeys.DNS_DOMAIN_LAST_CHECK_DATE);
    }

    //region CurrentDomain
    @Test
    public void testCurrentDomain() {
        /// Given a domain
        String domain = DomainManager.domains.get(1);
        ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .set(ParameterKeys.DNS_DOMAIN_KEY, domain, true);

        /// When get current domain value
        /// Then current domain should be `DomainManager.domains[1]`
        assertEquals(domain, store.getCurrentDomain());
    }

    @Test
    public void testCurrentDomainWithNil() {
        /// No given domain
        ParametersProvider.get(RuntimeManagerProvider.get().getContext()).remove(ParameterKeys.DNS_DOMAIN_KEY);

        /// When get current domain value
        /// Then current domain should be `DNS_DOMAIN_ORIGINAL`
        Assert.assertEquals(DomainStore.DNS_DOMAIN_ORIGINAL, store.getCurrentDomain());
    }

    //endregion

    //region UpdateDomain
    @Test
    public void testUpdateDomain() {
        /// Given the new domain
        String newDomain = DomainManager.domains.get(1);

        /// When we update the domain
        store.updateDomain(newDomain);
        long updateDate = new Date().getTime();

        /// Then the new domain should be the second element of `DomainManager.domains`
        String savedDomain = getDomain();

        assertEquals(newDomain, savedDomain);

        /// Then sdk saved value should be reset
        Integer domainErrorCount = getDomainErrorCount();
        Long domainErrorUpdateDate = getDomainErrorUpdateDate();

        assertNull(domainErrorCount);
        assertNull(domainErrorUpdateDate);

        /// Then domain last update date should be updated
        Long savedUpdateDate = getDomainLastUpdateDate();
        assertNotNull(savedUpdateDate);
        assertEquals(updateDate, savedUpdateDate, 100);
    }

    @Test
    public void testUpdateDomainWithNil() {
        /// Not given domain
        /// When we update the current domain
        store.updateDomain(null);
        long updateDate = new Date().getTime();

        /// Then the new domain should be nil
        String savedDomain = getDomain();
        assertNull(savedDomain);

        /// Then sdk saved value should be reset
        assertNull(getDomain());
        assertNull(getDomainErrorCount());
        assertNull(getDomainErrorUpdateDate());
        assertNull(getDomainLastCheckDate());

        /// Then domain last update date should be updated
        Long savedUpdateDate = getDomainLastUpdateDate();
        assertNotNull(savedUpdateDate);
        assertEquals(updateDate, savedUpdateDate, 100);
    }

    //endregion

    //region IncrementErrorCount
    @Test
    public void testIncrementErrorCountInitialState() {
        /// Given initial state of domain error count
        /// When the error count is incremented
        int errorCount = store.incrementErrorCount();
        long errorDate = new Date().getTime();

        /// Then domain error count should be 1
        assertEquals(1, errorCount);
        Long errorUpdateDate = getDomainErrorUpdateDate();
        assertNotNull(errorUpdateDate);
        assertEquals(errorDate, errorUpdateDate, 100);
    }

    @Test
    public void testIncrementErrorCountAdvancedState() {
        /// Given advanced state of domain error count
        int initialCount = 2;
        ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .set(ParameterKeys.DNS_DOMAIN_ERROR_COUNT_KEY, String.valueOf(initialCount), true);

        /// When the error count is incremented
        int errorCount = store.incrementErrorCount();
        long errorDate = new Date().getTime();

        /// Then domain error count should be 3
        assertEquals(initialCount + 1, errorCount);
        Long errorUpdateDate = getDomainErrorUpdateDate();
        assertNotNull(errorUpdateDate);
        assertEquals(errorDate, errorUpdateDate, 100);
    }

    //endregion

    //region resetErrorCountIfNeeded
    @Test
    public void testResetErrorCountIfNeeded() {
        /// Given domain error count set
        ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .set(ParameterKeys.DNS_DOMAIN_ERROR_COUNT_KEY, String.valueOf(2), true);

        /// When the domain error count is reset
        store.resetErrorCountIfNeeded();

        /// Then domain error count should be nil
        Integer errorCount = getDomainErrorCount();
        Long errorUpdateDate = getDomainErrorUpdateDate();
        assertNull(errorCount);
        assertNull(errorUpdateDate);
    }

    //region UpdateLastCheckDomainDate
    @Test
    public void testUpdateLastCheckDomainDate() {
        /// Given un sdk start
        /// When the last domain check date is updated
        store.updateLastCheckDomainDate();
        long savedDate = new Date().getTime();

        /// Then last domain check date should be set
        Long lastCheckDate = getDomainLastCheckDate();
        assertEquals(savedDate, lastCheckDate, 100);
    }

    //endregion

    //region CanCheckOriginalDomainAvailability
    @Test
    public void testCanCheckOriginalDomainAvailabilityNoDomainChange() {
        /// Given no domain change
        ParametersProvider.get(RuntimeManagerProvider.get().getContext()).remove(ParameterKeys.DNS_DOMAIN_KEY);
        ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .remove(ParameterKeys.DNS_DOMAIN_LAST_UPDATE_DATE);

        /// When check availability
        boolean canCheck = store.canCheckOriginalDomainAvailability();

        /// Then should be false because no domain change
        assertFalse(canCheck);
    }

    @Test
    public void testCanCheckOriginalDomainAvailabilityWithOriginalDomain() {
        /// Given current domain is the original one
        store.updateDomain(DomainStore.DNS_DOMAIN_ORIGINAL);

        /// When check availability
        boolean canCheck = store.canCheckOriginalDomainAvailability();

        /// Then should be false because it's the original one
        assertFalse(canCheck);
    }

    @Test
    public void testCanCheckOriginalDomainAvailabilityFirstCheck() {
        /// Given a first check
        store.updateDomain(DomainManager.domains.get(1));

        /// When check availability
        boolean canCheck = store.canCheckOriginalDomainAvailability();

        /// Then should be true because it's the first check
        assertTrue(canCheck);
    }

    @Test
    public void testCanCheckOriginalDomainAvailabilityLessThanThrottle() {
        /// Given multiple check
        store.updateDomain(DomainManager.domains.get(1));
        store.updateLastCheckDomainDate();

        /// When check availability
        boolean canCheck = store.canCheckOriginalDomainAvailability();

        /// Then should be false because it's less than the throttle condition
        assertFalse(canCheck);
    }

    @Test
    public void testCanCheckOriginalDomainAvailabilityMoreThanThrottle() {
        /// Given a check date greater than the throttler condition
        store.updateDomain(DomainManager.domains.get(1));

        Long lastCheckDate = new Date().getTime() -
        ((DomainStore.DNS_DOMAIN_LAST_CHECK_MIN_DELAY_SECOND + 3600) * 1000L);
        ParametersProvider
            .get(RuntimeManagerProvider.get().getContext())
            .set(ParameterKeys.DNS_DOMAIN_LAST_CHECK_DATE, String.valueOf(lastCheckDate), true);

        /// When check availability
        boolean canCheck = store.canCheckOriginalDomainAvailability();

        /// Then should be true because it's greater than the throttle condition
        assertTrue(canCheck);
    }

    //endregion

    //region CanCheckOriginalDomainAvailability
    @Test
    public void testIsOriginalDomainTrue() {
        // Given the original domain
        String domain = DomainStore.DNS_DOMAIN_ORIGINAL;

        // Whe check the domain
        boolean isOriginalDomain = store.isOriginalDomain(domain);

        // Then it should be true
        assertTrue(isOriginalDomain);
    }

    @Test
    public void testIsOriginalDomainFalse() {
        // Given the a domain
        String domain = DomainManager.domains.get(1);

        // Whe check the domain
        boolean isOriginalDomain = store.isOriginalDomain(domain);

        // Then it should be false
        assertFalse(isOriginalDomain);
    } //endregion
}
