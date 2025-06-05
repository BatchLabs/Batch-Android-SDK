package com.batch.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.batch.android.di.DITest
import com.batch.android.di.DITestUtils
import com.batch.android.di.providers.ProfileModuleProvider
import com.batch.android.di.providers.SQLUserDatasourceProvider
import com.batch.android.di.providers.UserModuleProvider
import com.batch.android.event.EventAttributesSerializer
import com.batch.android.event.InternalEvents
import com.batch.android.json.JSONArray
import com.batch.android.json.JSONObject
import com.batch.android.module.ProfileModule
import com.batch.android.module.TrackerModule
import com.batch.android.query.response.AttributesCheckResponse
import com.batch.android.query.response.AttributesSendResponse
import com.batch.android.webservice.listener.impl.AttributesCheckWebserviceListenerImpl
import com.batch.android.webservice.listener.impl.AttributesSendWebserviceListenerImpl
import java.net.URI
import java.util.Date
import java.util.EnumSet
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.powermock.reflect.Whitebox

@RunWith(AndroidJUnit4::class)
@SmallTest
class ProfileModuleTest : DITest() {

    private lateinit var context: Context
    private lateinit var trackerModule: TrackerModule

    override fun setUp() {
        super.setUp()
        context = ApplicationProvider.getApplicationContext()
        Whitebox.setInternalState(Batch::class.java, "install", Install(context))
        trackerModule = DITestUtils.mockSingletonDependency(TrackerModule::class.java, null)
    }

    @Test
    fun testIdentifyWithNullID() {
        simulateBatchStart(context)

        // Default custom id
        UserModuleProvider.get().setCustomID(context, "default_cus_test")

        // Given custom id
        val customID = null

        // Expected profile identify payload
        val expectedParams = JSONObject()
        val identifiers = JSONObject()
        identifiers.put("custom_id", JSONObject.NULL)
        identifiers.put("install_id", Batch.User.getInstallationID())
        expectedParams.put("identifiers", identifiers)

        // Expected profile changed payload (no cus key)
        val expectedProfileChangedParams = JSONObject()
        expectedProfileChangedParams.put("upv", UserModuleProvider.get().getVersion(context) + 1)

        ProfileModuleProvider.get().identify(customID)

        // Verify events are triggered
        Mockito.verify(trackerModule, Mockito.timeout(1500).times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_IDENTIFY),
                JSONObjectMockitoMatcher.eq(expectedParams),
            )

        // Ensure custom identifier has been locally saved
        Assert.assertNull(UserModuleProvider.get().getCustomID(context))
    }

    @Test
    fun testIdentifyWithValidID() {
        simulateBatchStart(context)

        // Given custom id
        val customID = "my_test_id"

        // Expected profile identify payload
        val expectedParams = JSONObject()
        val identifiers = JSONObject()
        identifiers.put("custom_id", customID)
        identifiers.put("install_id", Batch.User.getInstallationID())
        expectedParams.put("identifiers", identifiers)

        // Expected profile changed payload
        val expectedProfileChangedParams = JSONObject()
        expectedProfileChangedParams.put("cus", customID)
        expectedProfileChangedParams.put("upv", UserModuleProvider.get().getVersion(context) + 1)

        ProfileModuleProvider.get().identify(customID)

        // Verify events are triggered
        Mockito.verify(trackerModule, Mockito.timeout(1500).times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_IDENTIFY),
                JSONObjectMockitoMatcher.eq(expectedParams),
            )

        // Ensure custom identifier has been locally saved
        Assert.assertEquals(customID, UserModuleProvider.get().getCustomID(context))
    }

    @Test
    fun testIdentifyWithInvalidID() {
        simulateBatchStart(context)

        // Given custom id (1025 char)
        val customID =
            "my_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_id_1111"

        // Expected profile identify payload
        val expectedParams = JSONObject()
        val identifiers = JSONObject()
        identifiers.put("custom_id", customID)
        identifiers.put("install_id", Batch.User.getInstallationID())
        expectedParams.put("identifiers", identifiers)

        // Expected profile changed payload
        val expectedProfileChangedParams = JSONObject()
        expectedProfileChangedParams.put("cus", customID)
        expectedProfileChangedParams.put("upv", UserModuleProvider.get().getVersion(context) + 1)

        ProfileModuleProvider.get().identify(customID)

        // Verify events are not triggered
        Mockito.verify(trackerModule, Mockito.never())
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_IDENTIFY),
                JSONObjectMockitoMatcher.eq(expectedParams),
            )

        // Ensure custom identifier is still null
        Assert.assertNull(UserModuleProvider.get().getCustomID(context))
    }

    @Test
    fun testTrackPublicEvent() {
        simulateBatchStart(context)
        val eventData =
            BatchEventAttributes().apply {
                put(
                    "my_car",
                    BatchEventAttributes().apply {
                        put("brand", "car_brand")
                        put("year", 2024)
                        put("4x4", false)
                        put("model_url", URI("https://batch.com/"))
                        put(
                            "engine",
                            BatchEventAttributes().apply {
                                put("manufacturer", "manu")
                                put("cylinders", 6)
                                put("cylinder_capacity", 3.5)
                                put("manufacturing_date", Date(1596975143943L))
                            },
                        )
                    },
                )
                put("string_attr", "a_test_string")
                put("int_attr", 13)
                put("double_attr", 13.4567)
                put("date_attr", Date(1596975143943L)) // "2020-08-09T12:12:23.943Z"
                put("url_attr", URI("https://batch.com/"))
                putStringList("string_list", listOf("A", "B", "C"))
                putObjectList(
                    "list_items",
                    listOf(
                        BatchEventAttributes().apply {
                            put("brand", "brand-1")
                            put("year", 1998)
                        },
                        BatchEventAttributes().apply {
                            put("brand", "brand-2")
                            put("year", 2000)
                        },
                    ),
                )
                put("\$label", "test_label")
                putStringList("\$tags", listOf("tagA", "tagB", "tagC"))
            }
        ProfileModuleProvider.get().trackPublicEvent("test_event", eventData)
        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq("E.TEST_EVENT"),
                JSONObjectPartialMatcher.eq(EventAttributesSerializer.serialize(eventData)),
            )
    }

    @Test
    fun testOnProjectChanged() {
        simulateBatchStart(context)
        val fakeProjectKey = "project_1234567890"
        val profileModule = DITestUtils.mockSingletonDependency(ProfileModule::class.java, null)
        val atcResponse =
            AttributesCheckResponse("test_query_id").apply {
                setActionString("OK")
                projectKey = fakeProjectKey
            }
        AttributesCheckWebserviceListenerImpl().onSuccess(atcResponse)
        Mockito.verify(profileModule, Mockito.times(1)).onProjectChanged(null, fakeProjectKey)
    }

    @Test
    fun testOnProjectDidNotChanged() {
        simulateBatchStart(context)
        val fakeProjectKey = "project_1234567890"
        val profileModule = DITestUtils.mockSingletonDependency(ProfileModule::class.java, null)

        // We are simulating a fresh install on sdk V2 where use has wrote data to the profile
        val atsResponse =
            AttributesSendResponse("test_query_id").apply {
                transactionID = "fake_transaction_id"
                version = 1
                projectKey = fakeProjectKey
            }
        AttributesSendWebserviceListenerImpl().onSuccess(atsResponse)
        val atcResponse =
            AttributesCheckResponse("test_query_id").apply {
                setActionString("OK")
                projectKey = fakeProjectKey
            }
        AttributesCheckWebserviceListenerImpl().onSuccess(atcResponse)
        // Ensuring onProjectChanged is not triggered
        Mockito.verify(profileModule, Mockito.never()).onProjectChanged(null, fakeProjectKey)
    }

    @Test
    fun testCustomUserIdMigration() {
        // Start Batch
        simulateBatchStart(context)

        // Set default custom user id
        val customUserId = "fake-custom-user-id"
        UserModuleProvider.get().setCustomID(context, customUserId)

        // Trigger on project changed
        ProfileModuleProvider.get().onProjectChanged(null, "project_1234567890")

        // Ensure identify event is triggered
        val expectedParams = JSONObject()
        val identifiers = JSONObject()
        identifiers.put("custom_id", customUserId)
        identifiers.put("install_id", Batch.User.getInstallationID())
        expectedParams.put("identifiers", identifiers)

        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_IDENTIFY),
                JSONObjectMockitoMatcher.eq(expectedParams),
            )
    }

    @Test
    fun testCustomUserIdMigrationDisabled() {
        // Disable migration
        Batch.disableMigration(EnumSet.of(BatchMigration.CUSTOM_ID))

        // Start Batch
        simulateBatchStart(context)

        // Set default custom user id
        val customUserId = "fake-custom-user-id"
        UserModuleProvider.get().setCustomID(context, customUserId)

        // Trigger on project changed
        ProfileModuleProvider.get().onProjectChanged(null, "project_1234567890")

        // Ensure identify event is triggered
        val expectedParams = JSONObject()
        val identifiers = JSONObject()
        identifiers.put("custom_id", customUserId)
        identifiers.put("install_id", Batch.User.getInstallationID())
        expectedParams.put("identifiers", identifiers)

        Mockito.verify(trackerModule, Mockito.never())
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_IDENTIFY),
                JSONObjectMockitoMatcher.eq(expectedParams),
            )
    }

    @Test
    fun testCustomDataMigration() {
        // Start Batch
        simulateBatchStart(context)

        // Set default custom user language and region
        UserModuleProvider.get().setLanguage(context, "fr")
        UserModuleProvider.get().setRegion(context, "FR")

        // Set legacy custom attributes
        SQLUserDatasourceProvider.get(context).apply {
            acquireTransactionLock(1)
            setAttribute("string_att", "hello")
            setAttribute("int_att", 3)
            setAttribute("double_att", 3.6)
            setAttribute("url_att", URI("https://batch.com/pricing"))
            addTag("array_att", "attr1")
            addTag("array_att", "attr2")
            commitTransaction()
        }

        // Trigger on project changed
        ProfileModuleProvider.get().onProjectChanged(null, "project_1234567890")

        // Expected Params
        val expectedParams =
            JSONObject().apply {
                put("language", "fr")
                put("region", "FR")
                put(
                    "custom_attributes",
                    JSONObject().apply {
                        put("string_att.s", "hello")
                        put("int_att.i", 3L)
                        put("double_att.f", 3.6)
                        put("url_att.u", URI("https://batch.com/pricing"))
                        put(
                            "array_att.a",
                            JSONArray().apply {
                                put("attr2")
                                put("attr1")
                            },
                        )
                    },
                )
            }

        // Ensure profile data changed trigger with expected parameters
        Mockito.verify(trackerModule, Mockito.timeout(100).times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED),
                JSONObjectPartialMatcher.eq(expectedParams),
            )
    }

    @Test
    fun testCustomDataMigrationDisabled() {
        // Disable migration
        Batch.disableMigration(EnumSet.of(BatchMigration.CUSTOM_DATA))

        // Start Batch
        simulateBatchStart(context)

        // Set default custom user language and region
        UserModuleProvider.get().setLanguage(context, "fr")
        UserModuleProvider.get().setRegion(context, "FR")

        // Set legacy custom attributes
        SQLUserDatasourceProvider.get(context).apply {
            acquireTransactionLock(1)
            setAttribute("string_att", "hello")
            setAttribute("int_att", 3)
            setAttribute("double_att", 3.6)
            setAttribute("url_att", URI("https://batch.com/pricing"))
            addTag("array_att", "attr1")
            addTag("array_att", "attr2")
            commitTransaction()
        }

        // Trigger on project changed
        ProfileModuleProvider.get().onProjectChanged(null, "project_1234567890")

        // Expected Params
        val expectedParams =
            JSONObject().apply {
                put("language", "fr")
                put("region", "FR")
                put(
                    "custom_attributes",
                    JSONObject().apply {
                        put("string_att.s", "hello")
                        put("int_att.i", 3L)
                        put("double_att.f", 3.6)
                        put("url_att.u", URI("https://batch.com/pricing"))
                        put(
                            "array_att.a",
                            JSONArray().apply {
                                put("attr2")
                                put("attr1")
                            },
                        )
                    },
                )
            }

        // Ensure profile data changed trigger with expected parameters
        Mockito.verify(trackerModule, Mockito.timeout(100).times(0))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED),
                JSONObjectPartialMatcher.eq(expectedParams),
            )
    }
}
