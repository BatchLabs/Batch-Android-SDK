package com.batch.android.profile

import androidx.test.filters.SmallTest
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

    @Test
    fun testPutInAddedDeduplicatesKeepingLastOccurrence() {
        val partial = ProfilePartialUpdateAttribute(null)
        partial.putInAdded(listOf("a", "b", "a", "c"))
        Assert.assertEquals(listOf("b", "a", "c"), partial.added)
    }

    @Test
    fun testPutInAddedDeduplicatesAcrossMultipleCalls() {
        val partial = ProfilePartialUpdateAttribute(null)
        partial.putInAdded(listOf("a", "b"))
        // "a" reappears — last occurrence wins, moves to the end
        partial.putInAdded(listOf("c", "a"))
        Assert.assertEquals(listOf("b", "c", "a"), partial.added)
    }

    @Test
    fun testPutInRemovedDeduplicatesKeepingLastOccurrence() {
        val partial = ProfilePartialUpdateAttribute(null)
        partial.putInRemoved(listOf("d", "e", "d", "a", "f", "a"))
        Assert.assertEquals(listOf("e", "d", "f", "a"), partial.removed)
    }
}
