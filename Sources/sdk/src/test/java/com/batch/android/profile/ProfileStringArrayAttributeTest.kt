package com.batch.android.profile

import androidx.test.filters.SmallTest
import com.batch.android.json.JSONArray
import com.batch.android.json.JSONObject
import com.batch.android.profile.ProfileDataHelper.AttributeValidationException
import org.junit.Assert
import org.junit.Test

@SmallTest
class ProfileStringArrayAttributeTest {

    @Test
    fun testSetAttributeAndToJson() {
        val attribute = ProfileStringArrayAttribute()
        attribute.setAttribute(listOf("a", "b"))

        Assert.assertEquals(listOf("a", "b"), attribute.attribute.value)

        val json = attribute.toJSON() as JSONArray
        Assert.assertEquals(2, json.length())
        Assert.assertEquals("a", json.getString(0))
        Assert.assertEquals("b", json.getString(1))
    }

    @Test
    fun testAddToArrayCreatesPartialUpdatesWhenUnset() {
        val attribute = ProfileStringArrayAttribute()
        attribute.addToArray(listOf("a", "b"))

        Assert.assertNull(attribute.attribute.value)
        Assert.assertEquals(listOf("a", "b"), attribute.partialUpdates?.added)

        val json = attribute.toJSON() as JSONObject
        val added = json.getJSONArray("\$add")
        Assert.assertEquals(2, added.length())
        Assert.assertEquals("a", added.getString(0))
        Assert.assertEquals("b", added.getString(1))
        Assert.assertFalse(json.has("\$remove"))
    }

    @Test
    fun testRemoveFromArrayCreatesPartialUpdatesWhenUnset() {
        val attribute = ProfileStringArrayAttribute()
        attribute.removeFromArray(listOf("a", "b"))

        Assert.assertNull(attribute.attribute.value)
        Assert.assertEquals(listOf("a", "b"), attribute.partialUpdates?.removed)

        val json = attribute.toJSON() as JSONObject
        val removed = json.getJSONArray("\$remove")
        Assert.assertEquals(2, removed.length())
        Assert.assertEquals("a", removed.getString(0))
        Assert.assertEquals("b", removed.getString(1))
        Assert.assertFalse(json.has("\$add"))
    }

    @Test
    fun testAddAndRemoveWhenAttributeSet() {
        val attribute = ProfileStringArrayAttribute()
        attribute.setAttribute(listOf("a", "b"))
        attribute.addToArray(listOf("c"))
        attribute.removeFromArray(listOf("b"))

        Assert.assertEquals(listOf("a", "c"), attribute.attribute.value)
        Assert.assertNull(attribute.partialUpdates)
    }

    @Test
    fun testValidateFailsOnEmptyArray() {
        val attribute = ProfileStringArrayAttribute()
        attribute.setAttribute(emptyList())

        Assert.assertThrows(AttributeValidationException::class.java) { attribute.validate() }
    }

    @Test
    fun testSetAttributeDeduplicatesKeepingLastOccurrence() {
        val attribute = ProfileStringArrayAttribute()
        attribute.setAttribute(listOf("d", "e", "d", "a", "f", "a"))
        Assert.assertEquals(listOf("e", "d", "f", "a"), attribute.attribute.value)
    }

    @Test
    fun testConstructorDeduplicates() {
        val attribute = ProfileStringArrayAttribute(listOf("a", "b", "a", "c"))
        Assert.assertEquals(listOf("b", "a", "c"), attribute.attribute.value)
    }

    @Test
    fun testAddToArrayOnExistingAttributeDeduplicates() {
        val attribute = ProfileStringArrayAttribute()
        attribute.setAttribute(listOf("a", "b"))
        // "a" already present — it moves to the end (last occurrence wins)
        attribute.addToArray(listOf("a", "c"))
        Assert.assertEquals(listOf("b", "a", "c"), attribute.attribute.value)
    }

    @Test
    fun testAddToArrayNewAttributeDeduplicates() {
        val attribute = ProfileStringArrayAttribute()
        attribute.addToArray(listOf("a", "b", "a"))
        Assert.assertEquals(listOf("b", "a"), attribute.partialUpdates?.added)
    }

    @Test
    fun testRemoveFromArrayNewAttributeDeduplicates() {
        val attribute = ProfileStringArrayAttribute()
        attribute.removeFromArray(listOf("a", "b", "a"))
        Assert.assertEquals(listOf("b", "a"), attribute.partialUpdates?.removed)
    }

    @Test
    fun testAddToArrayAfterShouldDeleteDeduplicates() {
        // Simulate removeAttribute then addToArray: attribute starts in shouldDelete state
        val attribute = ProfileStringArrayAttribute(null as List<String>?)
        Assert.assertTrue(attribute.attribute.shouldDelete())
        attribute.addToArray(listOf("a", "b", "a"))
        Assert.assertEquals(listOf("b", "a"), attribute.attribute.value)
        Assert.assertFalse(attribute.attribute.shouldDelete())
    }

    @Test
    fun testIsEmpty() {
        val attribute = ProfileStringArrayAttribute()
        Assert.assertTrue(attribute.isEmpty)

        attribute.setAttribute(listOf("a"))
        Assert.assertFalse(attribute.isEmpty)

        attribute.setAttribute(emptyList())
        Assert.assertTrue(attribute.isEmpty)
    }
}
