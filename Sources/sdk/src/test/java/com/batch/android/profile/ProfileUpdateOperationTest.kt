package com.batch.android.profile

import androidx.test.filters.SmallTest
import com.batch.android.profile.ProfileDataHelper.AttributeValidationException
import com.batch.android.user.AttributeType
import com.batch.android.user.UserAttribute
import org.junit.Assert
import org.junit.Test

@SmallTest
class ProfileUpdateOperationTest {

    @Test
    fun testAddToListAfterSetAttribute() {

        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.addAttribute(
            "test",
            UserAttribute(ProfileStringArrayAttribute(listOf("a")), AttributeType.STRING_ARRAY),
        )
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))
        Assert.assertEquals(
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .attribute
                .value
                ?.get(0),
            "a",
        )

        profileUpdateOperation.addToCustomArrayAttribute("test", listOf("b"))
        Assert.assertEquals(
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .attribute
                .value
                ?.get(1),
            "b",
        )
    }

    @Test
    fun testAddToListFirst() {
        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.addToCustomArrayAttribute("test", listOf("a"))
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))

        val value =
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .partialUpdates
        Assert.assertEquals(value?.added?.get(0), "a")
    }

    @Test
    fun testAddToListAfterRemoveAttribute() {

        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.removeAttribute("test")
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))

        Assert.assertNull(profileUpdateOperation.customAttributes["test"]?.value)
        profileUpdateOperation.addToCustomArrayAttribute("test", ArrayList(listOf("a")))

        val value =
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .attribute
                .value
        Assert.assertEquals("a", value?.get(0))
    }

    @Test
    fun testAddToListTwoTimes() {
        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.addToCustomArrayAttribute("test", ArrayList(listOf("a")))
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))
        Assert.assertEquals(
            1,
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .partialUpdates
                ?.added
                ?.size,
        )
        Assert.assertEquals(
            "a",
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .partialUpdates
                ?.added
                ?.get(0),
        )

        profileUpdateOperation.addToCustomArrayAttribute("test", ArrayList(listOf("b")))
        Assert.assertEquals(
            2,
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .partialUpdates
                ?.added
                ?.size,
        )
        Assert.assertEquals(
            "b",
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .partialUpdates
                ?.added
                ?.get(1),
        )

        Assert.assertThrows(AttributeValidationException::class.java) {
            profileUpdateOperation.addToCustomArrayAttribute(
                "test",
                ArrayList(
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
                    )
                ),
            )
        }
        Assert.assertEquals(
            2,
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .partialUpdates
                ?.added
                ?.size,
        )
    }

    @Test
    fun testRemoveFromListAfterSetAttribute() {

        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.addAttribute(
            "test",
            UserAttribute(ProfileStringArrayAttribute(listOf("a", "b")), AttributeType.STRING_ARRAY),
        )
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))
        Assert.assertEquals(
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .attribute
                .value
                ?.size,
            2,
        )

        profileUpdateOperation.removeFromCustomArrayAttribute("test", ArrayList(listOf("b")))
        Assert.assertEquals(
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .attribute
                .value
                ?.size,
            1,
        )
        Assert.assertEquals(
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .attribute
                .value
                ?.get(0),
            "a",
        )

        profileUpdateOperation.removeFromCustomArrayAttribute("test", ArrayList(listOf("a")))
        Assert.assertFalse(profileUpdateOperation.customAttributes.containsKey("test"))
    }

    @Test
    fun testRemoveFromListFirst() {
        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.removeFromCustomArrayAttribute("test", listOf("a"))
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))

        val value =
            profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute
        Assert.assertEquals(value.partialUpdates?.removed?.get(0), "a")
    }

    @Test
    fun testRemoveFromListAfterRemoveAttribute() {

        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.removeAttribute("test")
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))

        Assert.assertNull(profileUpdateOperation.customAttributes["test"]?.value)
        profileUpdateOperation.removeFromCustomArrayAttribute("test", ArrayList(listOf("a")))

        Assert.assertNull(profileUpdateOperation.customAttributes["test"]?.value)
    }

    @Test
    fun testRemoveFromListTwoTimes() {
        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.removeFromCustomArrayAttribute("test", ArrayList(listOf("a")))
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))

        Assert.assertEquals(
            "a",
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .partialUpdates
                ?.removed
                ?.get(0),
        )

        profileUpdateOperation.removeFromCustomArrayAttribute("test", ArrayList(listOf("b")))
        Assert.assertEquals(
            2,
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .partialUpdates
                ?.removed
                ?.size,
        )
        Assert.assertEquals(
            "b",
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .partialUpdates
                ?.removed
                ?.get(1),
        )

        Assert.assertThrows(AttributeValidationException::class.java) {
            profileUpdateOperation.removeFromCustomArrayAttribute(
                "test",
                ArrayList(
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
                    )
                ),
            )
        }
        Assert.assertEquals(
            2,
            (profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute)
                .partialUpdates
                ?.removed
                ?.size,
        )
    }

    @Test
    fun testAddToAndRemoveFromList() {
        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.addToCustomArrayAttribute("test", ArrayList(listOf("a")))
        profileUpdateOperation.removeFromCustomArrayAttribute("test", ArrayList(listOf("b")))
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))

        val value =
            profileUpdateOperation.customAttributes["test"]?.value as ProfileStringArrayAttribute
        Assert.assertEquals(value.partialUpdates?.added?.size, 1)
        Assert.assertEquals(value.partialUpdates?.added?.get(0), "a")
        Assert.assertEquals(value.partialUpdates?.removed?.size, 1)
        Assert.assertEquals(value.partialUpdates?.removed?.get(0), "b")
    }
}
