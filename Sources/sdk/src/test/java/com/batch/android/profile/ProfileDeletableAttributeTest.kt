package com.batch.android.profile

import androidx.test.filters.SmallTest
import com.batch.android.json.JSONObject
import org.junit.Assert
import org.junit.Test

@SmallTest
class ProfileDeletableAttributeTest {

    @Test
    fun testGetValue() {
        Assert.assertEquals("value", ProfileDeletableAttribute("value").value)
        Assert.assertNull("value", ProfileDeletableAttribute(null).value)
    }

    @Test
    fun testGetSerializedValue() {
        Assert.assertEquals("value", ProfileDeletableAttribute("value").serializedValue)
        Assert.assertEquals(JSONObject.NULL, ProfileDeletableAttribute(null).serializedValue)
    }
}