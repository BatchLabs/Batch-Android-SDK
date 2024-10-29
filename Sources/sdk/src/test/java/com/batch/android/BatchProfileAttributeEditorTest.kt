package com.batch.android

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.batch.android.di.DITest
import com.batch.android.di.DITestUtils
import com.batch.android.event.InternalEvents
import com.batch.android.json.JSONArray
import com.batch.android.json.JSONObject
import com.batch.android.module.TrackerModule

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

import java.net.URI
import java.util.Date

@RunWith(AndroidJUnit4::class)
@SmallTest
class BatchProfileAttributeEditorTest : DITest() {

    private lateinit var trackerModule : TrackerModule

    override fun setUp() {
        super.setUp()
        simulateBatchStart(ApplicationProvider.getApplicationContext())
        trackerModule = DITestUtils.mockSingletonDependency(TrackerModule::class.java, null)
    }

    @Test
    fun testFullMethods() {
        Batch.Profile.identify("arnaudr")
        BatchProfileAttributeEditor().apply {
            setLanguage("fr")
            setRegion("FR")
            setEmailAddress("test@batch.com")
            setPhoneNumber("+33612345678")
            setEmailMarketingSubscription(BatchEmailSubscriptionState.SUBSCRIBED)
            setSMSMarketingSubscription(BatchSMSSubscriptionState.SUBSCRIBED)
            setAttribute("string_att", "hello")
            setAttribute("int_att", 3)
            setAttribute("double_att", 3.6)
            setAttribute("date_att", Date(1596975143943L))
            setAttribute("url_att", URI("https://batch.com/pricing"))
            setAttribute("array_att", listOf("michelle", "bresil"))
            addToArray("array_partial", listOf("i", "don't"))
            removeFromArray("array_partial", "know")
            removeFromArray("array_partial_2", listOf("i", "don't"))
            addToArray("array_partial_2", "know")
            save()
        }

        val expectedProfileDataChangedParams = JSONObject().apply {
            put("email", "test@batch.com")
            put("phone_number", "+33612345678")
            put("email_marketing", "subscribed")
            put("sms_marketing", "subscribed")
            put("language", "fr")
            put("region", "FR")
            put("custom_attributes", JSONObject().apply {
                put("string_att.s", "hello")
                put("int_att.i", 3L)
                put("double_att.f", 3.6)
                put("date_att.t", 1596975143943L)
                put("url_att.u", "https://batch.com/pricing")
                put("array_att.a", JSONArray().apply {
                    put("michelle")
                    put("bresil")
                })
                put("array_partial.a", JSONObject().apply {
                    put("\$add", JSONArray().apply { put("i"); put("don't") } )
                    put("\$remove", JSONArray().apply { put("know") } )
                })
                put("array_partial_2.a", JSONObject().apply {
                    put("\$remove", JSONArray().apply { put("i"); put("don't") } )
                    put("\$add", JSONArray().apply { put("know") } )
                })
            })
        }

        val expectedInstallDataChangedParams = JSONObject().apply {
            put("added", JSONObject().apply {
                put("string_att.s", "hello")
                put("int_att.i", 3L)
                put("double_att.f", 3.6)
                put("date_att.t", 1596975143943L)
                put("url_att.u", URI("https://batch.com/pricing"))
                put("t.array_att", JSONArray().apply {
                    put("bresil")
                    put("michelle")
                })
                put("t.array_partial", JSONArray().apply {
                    put("don't")
                    put("i")
                })
                put("t.array_partial_2", JSONArray().apply {
                    put("know")
                })
            })
        }

        // Ensure profile data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.times(1))
                .track(ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED), JSONObjectPartialMatcher.eq(expectedProfileDataChangedParams))

        // Ensure install data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.timeout(2000).times(1))
                .track(ArgumentMatchers.eq(InternalEvents.INSTALL_DATA_CHANGED), JSONObjectPartialMatcher.eq(expectedInstallDataChangedParams))
    }

    /**
     * Ensure null values are sent to remove
     */
    @Test
    fun testFullMethodsWithNull() {
        // To fill the initial db state
        BatchProfileAttributeEditor().apply {
            setLanguage("fr")
            setRegion("FR")
            setEmailAddress("test@batch.com")
            setPhoneNumber("+33612345678")
            setAttribute("string_att", "hello")
            setAttribute("int_att", 3)
            setAttribute("double_att", 3.6)
            setAttribute("url_att", URI("https://batch.com/pricing"))
            setAttribute("array_att", listOf("michelle", "bresil"))
            save()
        }
        // Waiting for debounce
        Thread.sleep(1500)

        // Remove some attributes
        Batch.Profile.identify("arnaudr")
        BatchProfileAttributeEditor().apply {
            setLanguage(null)
            setRegion(null)
            setEmailAddress(null)
            setPhoneNumber(null)
            removeAttribute("string_att")
            removeAttribute("int_att")
            removeAttribute("double_att")
            removeAttribute("url_att")
            removeAttribute("array_att")
            save()
        }

        // Expected profile data changed event parameter
        val expectedProfileDataChangedParams = JSONObject().apply {
            put("email", JSONObject.NULL)
            put("phone_number", JSONObject.NULL)
            put("language", JSONObject.NULL)
            put("region", JSONObject.NULL)
            put("custom_attributes", JSONObject().apply {
                put("string_att", JSONObject.NULL)
                put("int_att", JSONObject.NULL)
                put("double_att", JSONObject.NULL)
                put("url_att", JSONObject.NULL)
                put("array_att", JSONObject.NULL)
            })
        }

        // Expected profile data changed event parameter
        val expectedInstallDataChangedParams = JSONObject().apply {
            put("removed", JSONObject().apply {
                put("string_att.s", "hello")
                put("int_att.i", 3L)
                put("double_att.f", 3.6)
                put("url_att.u", URI("https://batch.com/pricing"))
                put("t.array_att", JSONArray().apply {
                    put("bresil")
                    put("michelle")
                })
            })
        }

        // Ensure profile data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.times(1))
                .track(ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED), JSONObjectPartialMatcher.eq(expectedProfileDataChangedParams))

        Mockito.verify(trackerModule, Mockito.timeout(1500).times(1))
                .track(ArgumentMatchers.eq(InternalEvents.INSTALL_DATA_CHANGED), JSONObjectPartialMatcher.eq(expectedInstallDataChangedParams))
    }

    /**
     * Ensure we do not send null value for email/region/language ..
     */
    @Test
    fun testOnlyOneAttribute() {

        BatchProfileAttributeEditor().apply {
            setAttribute("string_att", "hello")
            save()
        }

        val expectedParams = JSONObject().apply {
            put("custom_attributes", JSONObject().apply {
                put("string_att.s", "hello")
            })
        }

        val expectedInstallDataChangedParams = JSONObject().apply {
            put("added", JSONObject().apply {
                put("string_att.s", "hello")
            })
            put("removed", JSONObject())
        }

        // Ensure profile data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.times(1))
                .track(ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED), JSONObjectMockitoMatcher.eq(expectedParams))

        // Ensure install data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.timeout(1500).times(1))
                .track(ArgumentMatchers.eq(InternalEvents.INSTALL_DATA_CHANGED), JSONObjectPartialMatcher.eq(expectedInstallDataChangedParams))
    }

    /**
     * Ensure we do not send empty event
     */
    @Test
    fun testEmptyAttributes() {

        BatchProfileAttributeEditor().apply {
            save()
        }

        // Ensure profile data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.never())
                .track(ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED), Mockito.any(JSONObject::class.java))


        // Ensure install data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.timeout(1500).times(0))
                .track(ArgumentMatchers.eq(InternalEvents.INSTALL_DATA_CHANGED), Mockito.any(JSONObject::class.java))
    }
}