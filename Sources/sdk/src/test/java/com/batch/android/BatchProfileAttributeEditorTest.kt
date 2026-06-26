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
import com.batch.android.profile.ProfileUpdateOperation
import com.batch.android.user.AttributeType
import com.batch.android.user.UserAttribute
import java.net.URI
import java.util.Date
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.powermock.reflect.Whitebox

@RunWith(AndroidJUnit4::class)
@SmallTest
class BatchProfileAttributeEditorTest : DITest() {

    private lateinit var trackerModule: TrackerModule

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
            setTopicPreferences(listOf("News", "Offers"))
            addToTopicPreferences(listOf("Beta"))
            removeFromTopicPreferences(listOf("Offers"))
            setAttribute("string_att", "hello")
            // this attribute should not be sent from the install data changed event since it more
            // than 64 chars
            setAttribute(
                "string_att_cep",
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque.",
            )
            setAttribute("int_att", 3)
            setAttribute("double_att", 3.6)
            setAttribute("date_att", Date(1596975143943L))
            setAttribute("url_att", URI("https://batch.com/pricing"))
            setAttribute("array_att", listOf("michelle", "bresil"))
            // this one should not be sent from the install data changed event since items have more
            // than 64 chars
            setAttribute(
                "array_att_cep",
                listOf(
                    "Pellentesque habitant morbi tristique senectus et netus et malesuada",
                    "Pellentesqua habitanti morbi tristique senectus et netus et malesuada",
                ),
            )
            addToArray(
                "array_partial",
                listOf(
                    "i",
                    "don't",
                    "Pellentesque habitant morbi tristique senectus et netus et malesuada",
                ),
            )
            removeFromArray("array_partial", "know")
            removeFromArray(
                "array_partial_2",
                listOf(
                    "i",
                    "don't",
                    "Pellentesque habitant morbi tristique senectus et netus et malesuada",
                ),
            )
            addToArray("array_partial_2", "know")
            save()
        }

        val expectedProfileDataChangedParams =
            JSONObject().apply {
                put("email", "test@batch.com")
                put("phone_number", "+33612345678")
                put("email_marketing", "subscribed")
                put("sms_marketing", "subscribed")
                put("language", "fr")
                put("region", "FR")
                put(
                    "topic_preferences",
                    JSONArray().apply {
                        put("news")
                        put("beta")
                    },
                )
                put(
                    "custom_attributes",
                    JSONObject().apply {
                        put("string_att.s", "hello")
                        put(
                            "string_att_cep.s",
                            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque.",
                        )
                        put("int_att.i", 3L)
                        put("double_att.f", 3.6)
                        put("date_att.t", 1596975143943L)
                        put("url_att.u", "https://batch.com/pricing")
                        put(
                            "array_att.a",
                            JSONArray().apply {
                                put("michelle")
                                put("bresil")
                            },
                        )
                        put(
                            "array_att_cep.a",
                            JSONArray().apply {
                                put(
                                    "Pellentesque habitant morbi tristique senectus et netus et malesuada"
                                )
                                put(
                                    "Pellentesqua habitanti morbi tristique senectus et netus et malesuada"
                                )
                            },
                        )
                        put(
                            "array_partial.a",
                            JSONObject().apply {
                                put(
                                    "\$add",
                                    JSONArray().apply {
                                        put("i")
                                        put("don't")
                                        put(
                                            "Pellentesque habitant morbi tristique senectus et netus et malesuada"
                                        )
                                    },
                                )
                                put("\$remove", JSONArray().apply { put("know") })
                            },
                        )
                        put(
                            "array_partial_2.a",
                            JSONObject().apply {
                                put(
                                    "\$remove",
                                    JSONArray().apply {
                                        put("i")
                                        put("don't")
                                        put(
                                            "Pellentesque habitant morbi tristique senectus et netus et malesuada"
                                        )
                                    },
                                )
                                put("\$add", JSONArray().apply { put("know") })
                            },
                        )
                    },
                )
            }

        val expectedInstallDataChangedParams =
            JSONObject().apply {
                put(
                    "added",
                    JSONObject().apply {
                        put("string_att.s", "hello")
                        put("int_att.i", 3L)
                        put("double_att.f", 3.6)
                        put("date_att.t", 1596975143943L)
                        put("url_att.u", URI("https://batch.com/pricing"))
                        put(
                            "t.array_att",
                            JSONArray().apply {
                                put("bresil")
                                put("michelle")
                            },
                        )
                        put(
                            "t.array_partial",
                            JSONArray().apply {
                                put("don't")
                                put("i")
                            },
                        )
                        put("t.array_partial_2", JSONArray().apply { put("know") })
                    },
                )
            }

        // Ensure profile data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED),
                JSONObjectPartialMatcher.eq(expectedProfileDataChangedParams),
            )

        // Ensure install data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.timeout(5000).times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.INSTALL_DATA_CHANGED),
                JSONObjectPartialMatcher.eq(expectedInstallDataChangedParams),
            )
    }

    /** Ensure null values are sent to remove */
    @Test
    fun testFullMethodsWithNull() {
        // To fill the initial db state
        BatchProfileAttributeEditor().apply {
            setLanguage("fr")
            setRegion("FR")
            setEmailAddress("test@batch.com")
            setPhoneNumber("+33612345678")
            setTopicPreferences(listOf("News", "Offers"))
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
            setTopicPreferences(null)
            removeAttribute("string_att")
            removeAttribute("int_att")
            removeAttribute("double_att")
            removeAttribute("url_att")
            removeAttribute("array_att")
            save()
        }

        // Expected profile data changed event parameter
        val expectedProfileDataChangedParams =
            JSONObject().apply {
                put("email", JSONObject.NULL)
                put("phone_number", JSONObject.NULL)
                put("language", JSONObject.NULL)
                put("region", JSONObject.NULL)
                put("topic_preferences", JSONObject.NULL)
                put(
                    "custom_attributes",
                    JSONObject().apply {
                        put("string_att", JSONObject.NULL)
                        put("int_att", JSONObject.NULL)
                        put("double_att", JSONObject.NULL)
                        put("url_att", JSONObject.NULL)
                        put("array_att", JSONObject.NULL)
                    },
                )
            }

        // Expected profile data changed event parameter
        val expectedInstallDataChangedParams =
            JSONObject().apply {
                put(
                    "removed",
                    JSONObject().apply {
                        put("string_att.s", "hello")
                        put("int_att.i", 3L)
                        put("double_att.f", 3.6)
                        put("url_att.u", URI("https://batch.com/pricing"))
                        put(
                            "t.array_att",
                            JSONArray().apply {
                                put("bresil")
                                put("michelle")
                            },
                        )
                    },
                )
            }

        // Ensure profile data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED),
                JSONObjectPartialMatcher.eq(expectedProfileDataChangedParams),
            )

        Mockito.verify(trackerModule, Mockito.timeout(1500).times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.INSTALL_DATA_CHANGED),
                JSONObjectPartialMatcher.eq(expectedInstallDataChangedParams),
            )
    }

    /** Ensure we do not send null value for email/region/language .. */
    @Test
    fun testOnlyOneAttribute() {

        BatchProfileAttributeEditor().apply {
            setAttribute("string_att", "hello")
            save()
        }

        val expectedParams =
            JSONObject().apply {
                put("custom_attributes", JSONObject().apply { put("string_att.s", "hello") })
            }

        val expectedInstallDataChangedParams =
            JSONObject().apply {
                put("added", JSONObject().apply { put("string_att.s", "hello") })
                put("removed", JSONObject())
            }

        // Ensure profile data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED),
                JSONObjectMockitoMatcher.eq(expectedParams),
            )

        // Ensure install data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.timeout(1500).times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.INSTALL_DATA_CHANGED),
                JSONObjectPartialMatcher.eq(expectedInstallDataChangedParams),
            )
    }

    @Test
    fun testTopicPreferences() {

        BatchProfileAttributeEditor().apply {
            setTopicPreferences(
                listOf(
                    "1",
                    "2",
                    "3",
                    "4",
                    "5",
                    "6",
                    "7",
                    "8",
                    "9",
                    "10",
                    "11",
                    "12",
                    "13",
                    "14",
                    "15",
                    "16",
                    "17",
                    "18",
                    "19",
                    "20",
                    "21",
                    "22",
                    "23",
                    "24",
                    "25",
                )
            )
            addToTopicPreferences(listOf("26"))
            save()
        }

        val expectedParams =
            JSONObject().apply {
                put(
                    "topic_preferences",
                    JSONArray(
                        listOf(
                            "1",
                            "2",
                            "3",
                            "4",
                            "5",
                            "6",
                            "7",
                            "8",
                            "9",
                            "10",
                            "11",
                            "12",
                            "13",
                            "14",
                            "15",
                            "16",
                            "17",
                            "18",
                            "19",
                            "20",
                            "21",
                            "22",
                            "23",
                            "24",
                            "25",
                        )
                    ),
                )
            }

        // Ensure profile data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED),
                JSONObjectMockitoMatcher.eq(expectedParams),
            )
    }

    @Test
    fun testAddToArrayAfterRemoveAttributeDeduplicates() {
        BatchProfileAttributeEditor().apply {
            removeAttribute("arr")
            addToArray("arr", listOf("a", "b", "a"))
            save()
        }

        val expectedParams =
            JSONObject().apply {
                put(
                    "custom_attributes",
                    JSONObject().apply {
                        put(
                            "arr.a",
                            JSONArray().apply {
                                put("b")
                                put("a")
                            },
                        )
                    },
                )
            }

        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED),
                JSONObjectPartialMatcher.eq(expectedParams),
            )
    }

    @Test
    fun testAddToArrayAfterRemoveAttributeWith26ItemsOneDuplicateIsAccepted() {
        // 26 inputs but the first is repeated at the end — dedup yields 25 unique items
        val items = (1..25).map { it.toString() } + listOf("1")
        BatchProfileAttributeEditor().apply {
            removeAttribute("arr")
            addToArray("arr", items)
            save()
        }

        val expectedArray =
            JSONArray().apply {
                (2..25).forEach { put(it.toString()) }
                put("1")
            }
        val expectedParams =
            JSONObject().apply {
                put("custom_attributes", JSONObject().apply { put("arr.a", expectedArray) })
            }

        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED),
                JSONObjectPartialMatcher.eq(expectedParams),
            )
    }

    @Test
    fun testSetAttributeWith26ItemsOneDuplicateIsAccepted() {
        // 26 inputs but the first is repeated at the end → dedup yields 25 unique items
        val items = (1..25).map { it.toString() } + listOf("1")
        BatchProfileAttributeEditor().apply {
            setAttribute("arr", items)
            save()
        }

        val expectedArray =
            JSONArray().apply {
                // "1" moved to last position (last-wins); items 2..25 keep their order
                (2..25).forEach { put(it.toString()) }
                put("1")
            }
        val expectedParams =
            JSONObject().apply {
                put("custom_attributes", JSONObject().apply { put("arr.a", expectedArray) })
            }

        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED),
                JSONObjectPartialMatcher.eq(expectedParams),
            )
    }

    @Test
    fun testArrayAttributeDeduplication() {
        BatchProfileAttributeEditor().apply {
            // Full set: spec example — last occurrence wins
            setAttribute("arr_set", listOf("d", "e", "d", "a", "f", "a"))
            // Partial add with duplicates
            addToArray("arr_add", listOf("a", "b", "a"))
            // Partial remove with duplicates
            removeFromArray("arr_remove", listOf("x", "y", "x"))
            save()
        }

        val expectedParams =
            JSONObject().apply {
                put(
                    "custom_attributes",
                    JSONObject().apply {
                        put(
                            "arr_set.a",
                            JSONArray().apply {
                                put("e")
                                put("d")
                                put("f")
                                put("a")
                            },
                        )
                        put(
                            "arr_add.a",
                            JSONObject().apply {
                                put(
                                    "\$add",
                                    JSONArray().apply {
                                        put("b")
                                        put("a")
                                    },
                                )
                            },
                        )
                        put(
                            "arr_remove.a",
                            JSONObject().apply {
                                put(
                                    "\$remove",
                                    JSONArray().apply {
                                        put("y")
                                        put("x")
                                    },
                                )
                            },
                        )
                    },
                )
            }

        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED),
                JSONObjectPartialMatcher.eq(expectedParams),
            )
    }

    @Test
    fun testTopicPreferencesDeduplication() {
        // Full set path: case-folding creates duplicates that are deduplicated
        BatchProfileAttributeEditor().apply {
            setTopicPreferences(listOf("Sport", "news", "sport"))
            save()
        }

        val expectedSetParams =
            JSONObject().apply {
                put(
                    "topic_preferences",
                    JSONArray().apply {
                        put("news")
                        put("sport")
                    },
                )
            }

        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED),
                JSONObjectMockitoMatcher.eq(expectedSetParams),
            )

        Mockito.clearInvocations(trackerModule)

        // Partial add path: duplicates within a single call and across two calls
        BatchProfileAttributeEditor().apply {
            addToTopicPreferences(listOf("sport", "news", "sport"))
            // Second call: "News" normalizes to "news" — already in $add, moves to end
            addToTopicPreferences(listOf("News"))
            save()
        }

        val expectedAddParams =
            JSONObject().apply {
                put(
                    "topic_preferences",
                    JSONObject().apply {
                        put(
                            "\$add",
                            JSONArray().apply {
                                put("sport")
                                put("news")
                            },
                        )
                    },
                )
            }

        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED),
                JSONObjectMockitoMatcher.eq(expectedAddParams),
            )
    }

    /**
     * Ensure that an oversized profile payload blocks the entire save: neither the CEP event nor
     * the legacy install event should be sent.
     *
     * CEP strings are capped at 300 chars by the editor's own validation, so we bypass it via
     * Whitebox to inject 1300-char values directly into profileUpdateOperation — the same technique
     * used in ProfileModuleTest. Language is set through the public API so that super.save() would
     * have had something to commit if not blocked, making this a clean atomicity proof.
     */
    @Test
    fun testSaveRejectedWhenPayloadTooLarge() {
        val editor = BatchProfileAttributeEditor()
        editor.setLanguage("fr")

        // Inject oversized attributes directly, bypassing editor validation.
        val profileUpdateOperation: ProfileUpdateOperation =
            Whitebox.getInternalState(editor, "profileUpdateOperation")
        repeat(20) { i ->
            profileUpdateOperation.addAttribute(
                "attr_$i",
                UserAttribute("a".repeat(1300), AttributeType.STRING),
            )
        }

        editor.save()

        Mockito.verify(trackerModule, Mockito.never())
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED),
                ArgumentMatchers.any(JSONObject::class.java),
            )
        Mockito.verify(trackerModule, Mockito.after(500).never())
            .track(
                ArgumentMatchers.eq(InternalEvents.INSTALL_DATA_CHANGED),
                ArgumentMatchers.any(JSONObject::class.java),
            )
    }

    /** Ensure we do not send empty event */
    @Test
    fun testEmptyAttributes() {

        BatchProfileAttributeEditor().apply { save() }

        // Ensure profile data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.never())
            .track(
                ArgumentMatchers.eq(InternalEvents.PROFILE_DATA_CHANGED),
                Mockito.any(JSONObject::class.java),
            )

        // Ensure install data changed event is sent with rights parameters
        Mockito.verify(trackerModule, Mockito.timeout(1500).times(0))
            .track(
                ArgumentMatchers.eq(InternalEvents.INSTALL_DATA_CHANGED),
                Mockito.any(JSONObject::class.java),
            )
    }
}
