package com.batch.android.core.domain

import android.annotation.SuppressLint
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.batch.android.di.DITest
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@SmallTest
class DomainManagerTests : DITest() {
    // region Parameters
    companion object {
        const val BATCH = "batch.com"
        const val WEBSERVICE_BASE: String = "https://ws.%s"
        const val METRIC_WEBSERVICE_BASE = "https://wsmetrics.%s/api-sdk"
    }

    private val mockStore: IDomainStore = Mockito.mock(IDomainStore::class.java)
    private var manager: DomainManager = MockDomainManager(mockStore, true)

    // endregion

    // region Setup
    @SuppressLint("CheckResult")
    @Before
    override fun setUp() {
        super.setUp()
        simulateBatchStart(ApplicationProvider.getApplicationContext())

        manager.resetDomainToOriginal()
        Mockito.`when`(mockStore.currentDomain).thenReturn(DomainManager.domains[0])
    }

    // endregion

    // region  NextDomain
    @Test
    fun testNextDomain() {
        /** Given original domain to [DomainStore.DNS_DOMAIN_ORIGINAL] */
        Mockito.`when`(mockStore.currentDomain).thenReturn(DomainStore.DNS_DOMAIN_ORIGINAL)
        /** When we get the next domain */
        val nextDomain = manager.nextDomain()

        /** Then the next domain should be the second element of [DomainManager.domains] */
        assertEquals(DomainManager.domains[1], nextDomain)
    }

    @Test
    fun testNextDomainOutOfRange() {
        /** Given a current domain */
        Mockito.`when`(mockStore.currentDomain).thenReturn(DomainManager.domains[1])

        /** When we get the next domain */
        val nextDomain = manager.nextDomain()

        /** Then next domain should be the current one */
        assertEquals(DomainManager.domains[1], nextDomain)
    }

    @Test
    fun testNextDomainWithUnknownCurrentDomain() {
        /** Given a current domain to `batchy.xyz` */
        Mockito.`when`(mockStore.currentDomain).thenReturn("batchy.xyz")

        /** When we get the next domain */
        val nextDomain = manager.nextDomain()

        /** Then the new domain should be [DomainStore.DNS_DOMAIN_ORIGINAL] */
        assertEquals(DomainManager.domains[0], nextDomain)
        assertEquals(DomainStore.DNS_DOMAIN_ORIGINAL, nextDomain)
    }

    // endregion

    // region UpdateDomainIfNeeded
    @SuppressLint("CheckResult")
    @Test
    fun testUpdateDomainIfNeededNoPreviousError() {
        /** Given first error When update domain if needed */
        manager.updateDomainIfNeeded()

        /** Then domain error should be incremented */
        verify(mockStore, times(1)).incrementErrorCount()
    }

    @SuppressLint("CheckResult")
    @Test
    fun testUpdateDomainIfNeededWithPreviousErrorLessThanThrottle() {
        /** Given previous error */
        Mockito.`when`(mockStore.lastErrorUpdateDate()).thenReturn(Date())

        /** When update domain if needed */
        manager.updateDomainIfNeeded()

        /** Throttle check should return before incrementing error count */
        verify(mockStore, never()).incrementErrorCount()
    }

    @SuppressLint("CheckResult")
    @Test
    fun testUpdateDomainIfNeededWithPreviousErrorGreaterThanThrottle() {
        /** Given previous error */
        val date = Date(Date().time - (DomainManager.DNS_DOMAIN_ERROR_MIN_DELAY_SECOND + 1) * 1000L)
        Mockito.`when`(mockStore.lastErrorUpdateDate()).thenReturn(date)

        /** When update domain if needed */
        manager.updateDomainIfNeeded()

        /** Then domain error should be incremented */
        verify(mockStore, times(1)).incrementErrorCount()
        verify(mockStore, never()).updateDomain(Mockito.anyString())
    }

    @SuppressLint("CheckResult")
    @Test
    fun testUpdateDomainIfNeededWithPreviousErrorsReachedLimit() {
        /** Given previous errors */
        val date = Date(Date().time - (DomainManager.DNS_DOMAIN_ERROR_MIN_DELAY_SECOND + 1) * 1000L)
        Mockito.`when`(mockStore.lastErrorUpdateDate()).thenReturn(date)
        Mockito.`when`(mockStore.incrementErrorCount()).thenReturn(3)

        /** When update domain if needed */
        manager.updateDomainIfNeeded()

        /** Then domain error should be incremented */
        verify(mockStore, times(1)).incrementErrorCount()
        verify(mockStore, times(1)).updateDomain(DomainManager.domains[1])
    }

    // endregion

    // region Url
    @Test
    fun testUrlWebService() {
        /** Given a current domain When retrieve url for webservice */
        val url = manager.url(DomainService.WEB, false)

        /** Then url should be `https://ws.batch.com` */
        assertEquals(url, String.format(WEBSERVICE_BASE, BATCH))
    }

    @Test
    fun testUrlMetrics() {
        /** Given a current domain When retrieve url for metrics */
        val url = manager.url(DomainService.METRIC, false)

        /** Then url should be `https://wsmetrics.batch.com/api-sdk` */
        assertEquals(url, String.format(METRIC_WEBSERVICE_BASE, BATCH))
    }

    @Test
    fun testUrlWebServiceWithNextDomain() {
        /** Given a next domain */
        Mockito.`when`(mockStore.currentDomain).thenReturn(DomainManager.domains[1])

        /** When retrieve url for webservice */
        val url = manager.url(DomainService.WEB, false)

        /** Then url should be `https://ws.\(nextDomain)` */
        assertEquals(url, String.format(WEBSERVICE_BASE, DomainManager.domains[1]))
    }

    @Test
    fun testUrlMetricsWithNextDomain() {
        /** Given a next domain */
        Mockito.`when`(mockStore.currentDomain).thenReturn(DomainManager.domains[1])

        /** When retrieve url for metrics */
        val url = manager.url(DomainService.METRIC, false)

        /** Then url should be `https://wsmetrics\(nextDomain)/api-sdk` */
        assertEquals(url, String.format(METRIC_WEBSERVICE_BASE, DomainManager.domains[1]))
    }

    @Test
    fun testUrlWebServiceWithNextDomainAndOverride() {
        /** Given a next domain */
        Mockito.`when`(mockStore.currentDomain).thenReturn(DomainManager.domains[1])

        /** When retrieve url for webservice */
        val url = manager.url(DomainService.WEB, true)

        /** Then url should be `https://ws.\(nextDomain)` */
        assertEquals(url, String.format(WEBSERVICE_BASE, BATCH))
    }

    @Test
    fun testUrlMetricsWithNextDomainAndOverride() {
        /** Given a next domain */
        Mockito.`when`(mockStore.currentDomain).thenReturn(DomainManager.domains[1])

        /** When retrieve url for metrics */
        val url = manager.url(DomainService.METRIC, true)

        /** Then url should be `https://wsmetrics\(nextDomain)/api-sdk` */
        assertEquals(url, String.format(METRIC_WEBSERVICE_BASE, BATCH))
    }

    // endregion

    // region Feature flags
    @Test
    fun testFeatureFlagNotActivated() {
        /** Given a next domain */
        val store: IDomainStore = DomainStore()
        store.updateDomain(DomainManager.domains[1])

        manager = MockDomainManager(store, false)

        /** When retrieve url for webservice */
        val url = manager.url(DomainService.WEB, false)

        /** Then url should be `https://ws.\(nextDomain)` */
        assertEquals(url, String.format(WEBSERVICE_BASE, BATCH))
    }
    // endregion
}
