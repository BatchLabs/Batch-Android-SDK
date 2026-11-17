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

    @Test
    fun testPutInRemovedCancelsPendingAdd() {
        val partial = ProfilePartialUpdateAttribute(null)
        partial.putInAdded(listOf("a"))
        partial.putInRemoved(listOf("a"))
        Assert.assertNull(partial.added)
        Assert.assertEquals(listOf("a"), partial.removed)
        Assert.assertEquals("{\"\$remove\":[\"a\"]}", partial.serializedValue.toString())
    }

    @Test
    fun testPutInAddedCancelsPendingRemove() {
        val partial = ProfilePartialUpdateAttribute(null)
        partial.putInRemoved(listOf("a"))
        partial.putInAdded(listOf("a"))
        Assert.assertNull(partial.removed)
        Assert.assertEquals(listOf("a"), partial.added)
        Assert.assertEquals("{\"\$add\":[\"a\"]}", partial.serializedValue.toString())
    }

    @Test
    fun testCancellationLeavesOtherItemsUntouched() {
        val partial = ProfilePartialUpdateAttribute(listOf("a", "b"), listOf("c", "d"))
        partial.putInRemoved(listOf("a"))
        Assert.assertEquals(listOf("b"), partial.added)
        Assert.assertEquals(listOf("c", "d", "a"), partial.removed)

        partial.putInAdded(listOf("d"))
        Assert.assertEquals(listOf("b", "d"), partial.added)
        Assert.assertEquals(listOf("c", "a"), partial.removed)
    }

    @Test
    fun testCancellationDoesNotMutateSourceLists() {
        val added = listOf("a")
        val partial = ProfilePartialUpdateAttribute(added)
        partial.putInRemoved(listOf("a"))
        Assert.assertEquals(listOf("a"), added)
    }
}
