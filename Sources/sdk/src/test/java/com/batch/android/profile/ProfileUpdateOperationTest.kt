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
            UserAttribute(ArrayList(listOf("a")), AttributeType.STRING_ARRAY),
        )
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))
        Assert.assertEquals(
            (profileUpdateOperation.customAttributes["test"]?.value as List<*>)[0],
            "a",
        )

        profileUpdateOperation.addToList("test", listOf("b"))
        Assert.assertEquals(
            (profileUpdateOperation.customAttributes["test"]?.value as List<*>)[1],
            "b",
        )
    }

    @Test
    fun testAddToListFirst() {
        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.addToList("test", listOf("a"))
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))

        val value =
            profileUpdateOperation.customAttributes["test"]?.value as ProfilePartialUpdateAttribute
        Assert.assertEquals(value.added?.get(0), "a")
    }

    @Test
    fun testAddToListAfterRemoveAttribute() {

        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.removeAttribute("test")
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))

        Assert.assertNull(profileUpdateOperation.customAttributes["test"]?.value)
        profileUpdateOperation.addToList("test", ArrayList(listOf("a")))

        val value = profileUpdateOperation.customAttributes["test"]?.value as List<*>
        Assert.assertEquals(value[0], "a")
    }

    @Test
    fun testAddToListTwoTimes() {
        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.addToList("test", ArrayList(listOf("a")))
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))
        Assert.assertEquals(
            1,
            (profileUpdateOperation.customAttributes["test"]?.value
                    as ProfilePartialUpdateAttribute)
                .added
                ?.size,
        )
        Assert.assertEquals(
            "a",
            (profileUpdateOperation.customAttributes["test"]?.value
                    as ProfilePartialUpdateAttribute)
                .added
                ?.get(0),
        )

        profileUpdateOperation.addToList("test", ArrayList(listOf("b")))
        Assert.assertEquals(
            2,
            (profileUpdateOperation.customAttributes["test"]?.value
                    as ProfilePartialUpdateAttribute)
                .added
                ?.size,
        )
        Assert.assertEquals(
            "b",
            (profileUpdateOperation.customAttributes["test"]?.value
                    as ProfilePartialUpdateAttribute)
                .added
                ?.get(1),
        )

        Assert.assertThrows(AttributeValidationException::class.java) {
            profileUpdateOperation.addToList(
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
            (profileUpdateOperation.customAttributes["test"]?.value
                    as ProfilePartialUpdateAttribute)
                .added
                ?.size,
        )
    }

    @Test
    fun testRemoveFromListAfterSetAttribute() {

        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.addAttribute(
            "test",
            UserAttribute(ArrayList(listOf("a", "b")), AttributeType.STRING_ARRAY),
        )
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))
        Assert.assertEquals(
            (profileUpdateOperation.customAttributes["test"]?.value as List<*>).size,
            2,
        )

        profileUpdateOperation.removeFromList("test", ArrayList(listOf("b")))
        Assert.assertEquals(
            (profileUpdateOperation.customAttributes["test"]?.value as List<*>).size,
            1,
        )
        Assert.assertEquals(
            (profileUpdateOperation.customAttributes["test"]?.value as List<*>)[0],
            "a",
        )

        profileUpdateOperation.removeFromList("test", ArrayList(listOf("a")))
        Assert.assertFalse(profileUpdateOperation.customAttributes.containsKey("test"))
    }

    @Test
    fun testRemoveFromListFirst() {
        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.removeFromList("test", listOf("a"))
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))

        val value =
            profileUpdateOperation.customAttributes["test"]?.value as ProfilePartialUpdateAttribute
        Assert.assertEquals(value.removed?.get(0), "a")
    }

    @Test
    fun testRemoveFromListAfterRemoveAttribute() {

        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.removeAttribute("test")
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))

        Assert.assertNull(profileUpdateOperation.customAttributes["test"]?.value)
        profileUpdateOperation.removeFromList("test", ArrayList(listOf("a")))

        Assert.assertNull(profileUpdateOperation.customAttributes["test"]?.value)
    }

    @Test
    fun testRemoveFromListTwoTimes() {
        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.removeFromList("test", ArrayList(listOf("a")))
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))

        Assert.assertEquals(
            "a",
            (profileUpdateOperation.customAttributes["test"]?.value
                    as ProfilePartialUpdateAttribute)
                .removed
                ?.get(0),
        )

        profileUpdateOperation.removeFromList("test", ArrayList(listOf("b")))
        Assert.assertEquals(
            2,
            (profileUpdateOperation.customAttributes["test"]?.value
                    as ProfilePartialUpdateAttribute)
                .removed
                ?.size,
        )
        Assert.assertEquals(
            "b",
            (profileUpdateOperation.customAttributes["test"]?.value
                    as ProfilePartialUpdateAttribute)
                .removed
                ?.get(1),
        )

        Assert.assertThrows(AttributeValidationException::class.java) {
            profileUpdateOperation.removeFromList(
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
            (profileUpdateOperation.customAttributes["test"]?.value
                    as ProfilePartialUpdateAttribute)
                .removed
                ?.size,
        )
    }

    @Test
    fun testAddToAndRemoveFromList() {
        val profileUpdateOperation = ProfileUpdateOperation()
        profileUpdateOperation.addToList("test", ArrayList(listOf("a")))
        profileUpdateOperation.removeFromList("test", ArrayList(listOf("b")))
        Assert.assertTrue(profileUpdateOperation.customAttributes.containsKey("test"))

        val value =
            profileUpdateOperation.customAttributes["test"]?.value as ProfilePartialUpdateAttribute
        Assert.assertEquals(value.added?.size, 1)
        Assert.assertEquals(value.added?.get(0), "a")
        Assert.assertEquals(value.removed?.size, 1)
        Assert.assertEquals(value.removed?.get(0), "b")
    }
}
