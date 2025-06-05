package com.batch.android.profile

import androidx.test.filters.SmallTest
import com.batch.android.BatchEmailSubscriptionState
import com.batch.android.BatchSMSSubscriptionState
import com.batch.android.json.JSONArray
import com.batch.android.json.JSONObject
import com.batch.android.user.AttributeType
import com.batch.android.user.UserAttribute
import java.net.URI
import java.util.Date
import org.junit.Assert
import org.junit.Test

@SmallTest
class ProfileDataSerializerTest {

    @Test
    fun testSerialize() {

        val dataModel =
            ProfileUpdateOperation().apply {
                setEmail("test@batch.com")
                setLanguage("fr")
                setRegion("FR")
                setEmailMarketing(BatchEmailSubscriptionState.SUBSCRIBED)
                setPhoneNumber("+33612345678")
                setSMSMarketing(BatchSMSSubscriptionState.SUBSCRIBED)
                addAttribute("string_att", UserAttribute("hello", AttributeType.STRING))
                addAttribute("int_att", UserAttribute(3, AttributeType.LONG))
                addAttribute("double_att", UserAttribute(3.6, AttributeType.DOUBLE))
                addAttribute(
                    "url_att",
                    UserAttribute(URI("https://batch.com/pricing"), AttributeType.URL),
                )
                addAttribute("date_att", UserAttribute(Date(1596975143943L), AttributeType.DATE))
                addAttribute(
                    "array_att",
                    UserAttribute(listOf("michel", "bresil"), AttributeType.STRING_ARRAY),
                )
                addToList("array_partial", listOf("i", "don't"))
                removeFromList("array_partial", listOf("know"))
            }

        val actual = ProfileDataSerializer.serialize(dataModel)

        Assert.assertEquals("test@batch.com", actual.getString("email"))
        Assert.assertEquals("fr", actual.getString("language"))
        Assert.assertEquals("FR", actual.getString("region"))
        Assert.assertEquals("subscribed", actual.getString("email_marketing"))
        Assert.assertEquals("subscribed", actual.getString("sms_marketing"))
        Assert.assertEquals("+33612345678", actual.getString("phone_number"))

        val actualCustomAttributes = actual.getJSONObject("custom_attributes")
        Assert.assertEquals("hello", actualCustomAttributes.getString("string_att.s"))
        Assert.assertEquals(3, actualCustomAttributes.getInt("int_att.i"))
        Assert.assertEquals(3.60, actualCustomAttributes.getDouble("double_att.f"), 0.00)
        Assert.assertEquals(
            "https://batch.com/pricing",
            actualCustomAttributes.getString("url_att.u"),
        )
        Assert.assertEquals(1596975143943L, actualCustomAttributes.getLong("date_att.t"))

        val actualArrayAttribute = actualCustomAttributes.getJSONArray("array_att.a")
        Assert.assertEquals("michel", actualArrayAttribute.getString(0))
        Assert.assertEquals("bresil", actualArrayAttribute.getString(1))

        val actualPartialAttribute = actualCustomAttributes.getJSONObject("array_partial.a")
        val actualAddPartialAttribute = actualPartialAttribute.getJSONArray("\$add")
        val actualRemovePartialAttribute = actualPartialAttribute.getJSONArray("\$remove")
        Assert.assertEquals("i", actualAddPartialAttribute.getString(0))
        Assert.assertEquals("don't", actualAddPartialAttribute.getString(1))
        Assert.assertEquals("know", actualRemovePartialAttribute.getString(0))
    }

    @Test
    fun testSerializeWithNullValues() {

        val dataModel =
            ProfileUpdateOperation().apply {
                setEmail(null)
                setLanguage(null)
                setRegion(null)
                setPhoneNumber(null)
            }

        val actual = ProfileDataSerializer.serialize(dataModel)
        Assert.assertEquals(JSONObject.NULL, actual.get("email"))
        Assert.assertEquals(JSONObject.NULL, actual.get("language"))
        Assert.assertEquals(JSONObject.NULL, actual.get("region"))
        Assert.assertEquals(JSONObject.NULL, actual.get("phone_number"))
    }

    @Test
    fun testSerializePartialUpdateAttribute() {

        // Case: Add only
        val attributeAddOnly = ProfilePartialUpdateAttribute(listOf("a", "b"))
        val expectedAddOnly = JSONObject().apply { put("\$add", JSONArray(listOf("a", "b"))) }
        Assert.assertNull(
            ProfileDataSerializer.serializePartialUpdateAttribute(attributeAddOnly)
                .optJSONArray("\$remove")
        )
        Assert.assertEquals(
            expectedAddOnly.toString(),
            ProfileDataSerializer.serializePartialUpdateAttribute(attributeAddOnly).toString(),
        )

        // Case: Remove only
        val attributeRemoveOnly = ProfilePartialUpdateAttribute(null, listOf("a", "b"))
        val expectedRemoveOnly = JSONObject().apply { put("\$remove", JSONArray(listOf("a", "b"))) }
        Assert.assertNull(
            ProfileDataSerializer.serializePartialUpdateAttribute(attributeRemoveOnly)
                .optJSONArray("\$add")
        )
        Assert.assertEquals(
            expectedRemoveOnly.toString(),
            ProfileDataSerializer.serializePartialUpdateAttribute(attributeRemoveOnly).toString(),
        )

        // Case: Both
        val attributeBoth = ProfilePartialUpdateAttribute(listOf("a", "b"), listOf("c", "d"))
        val expectedBoth =
            JSONObject().apply {
                put("\$add", JSONArray(listOf("a", "b")))
                put("\$remove", JSONArray(listOf("c", "d")))
            }
        Assert.assertNotNull(
            ProfileDataSerializer.serializePartialUpdateAttribute(attributeBoth)
                .optJSONArray("\$add")
        )
        Assert.assertNotNull(
            ProfileDataSerializer.serializePartialUpdateAttribute(attributeBoth)
                .optJSONArray("\$remove")
        )
        Assert.assertEquals(
            expectedBoth.toString(),
            ProfileDataSerializer.serializePartialUpdateAttribute(attributeBoth).toString(),
        )
    }
}
