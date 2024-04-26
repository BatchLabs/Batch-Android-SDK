package com.batch.android.profile

import androidx.test.filters.SmallTest
import com.batch.android.json.JSONObject
import org.junit.Assert
import org.junit.Test

@SmallTest
class ProfilePartialUpdateAttributeTest {

    @Test
    fun testPutInAdded() {
       val partial = ProfilePartialUpdateAttribute(null)
        Assert.assertNull(partial.added)
        Assert.assertNull(partial.removed)
        partial.putInAdded(listOf("a", "b"))
        Assert.assertNotNull(partial.added)
        Assert.assertNull(partial.removed)
        Assert.assertEquals(listOf("a", "b"), partial.added)
    }

    @Test
    fun testPutInRemoved() {
        val partial = ProfilePartialUpdateAttribute(null)
        Assert.assertNull(partial.added)
        Assert.assertNull(partial.removed)
        partial.putInRemoved(listOf("a", "b"))
        Assert.assertNotNull(partial.removed)
        Assert.assertNull(partial.added)
        Assert.assertEquals(listOf("a", "b"), partial.removed)
    }
}