package com.batch.android.localcampaigns.impl

import android.annotation.SuppressLint
import com.batch.android.FailReason
import com.batch.android.di.DI
import com.batch.android.di.DITestUtils
import com.batch.android.localcampaigns.CampaignManager
import com.batch.android.module.LocalCampaignsModule
import com.batch.android.webservice.listener.impl.LocalCampaignsWebserviceListenerImpl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.times

class LocalCampaignsWebserviceListenerImplTest {
    @Mock private lateinit var localCampaignsModule: LocalCampaignsModule

    @Mock private lateinit var campaignManager: CampaignManager

    private lateinit var listener: LocalCampaignsWebserviceListenerImpl

    @Before
    fun setUp() {
        localCampaignsModule =
            DITestUtils.mockSingletonDependency(LocalCampaignsModule::class.java, null)
        campaignManager = DITestUtils.mockSingletonDependency(CampaignManager::class.java, null)

        listener = LocalCampaignsWebserviceListenerImpl.provide()
    }

    @After
    fun cleanUp() {
        DI.reset()
    }

    @SuppressLint("CheckResult")
    @Test
    fun `onCampaignsFetched success`() {
        listener.onSuccess(arrayListOf())

        Mockito.verify(campaignManager, times(1)).setNextAvailableJITTimestampWithDefaultDelay()
    }

    @SuppressLint("CheckResult")
    @Test
    fun `onCampaignsFetched failure`() {
        listener.onError(FailReason.UNEXPECTED_ERROR)

        Mockito.verify(campaignManager, never())
            .setNextAvailableJITTimestampWithCustomDelay(Mockito.anyInt())
        Mockito.verify(campaignManager, never()).setNextAvailableJITTimestampWithDefaultDelay()
    }
}
