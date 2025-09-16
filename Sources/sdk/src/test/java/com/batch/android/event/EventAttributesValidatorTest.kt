package com.batch.android.event

import androidx.test.filters.SmallTest
import com.batch.android.BatchEventAttributes
import java.net.URI
import java.util.Date
import org.junit.Assert
import org.junit.Test

@SmallTest
class EventAttributesValidatorTest {

    @Test
    fun testIsEventNameValid() {
        Assert.assertFalse(EventAttributesValidator.isEventNameValid("invalid event name"))
        Assert.assertFalse(EventAttributesValidator.isEventNameValid("invalid-event-name"))
        Assert.assertFalse(EventAttributesValidator.isEventNameValid("invalid_event_name@"))
        Assert.assertFalse(EventAttributesValidator.isEventNameValid("invalid_event_name\n"))
        Assert.assertTrue(EventAttributesValidator.isEventNameValid("valid_event_name"))
    }

    @Test
    fun testLabelAttribute() {
        var errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply {
                    put(
                        "\$label",
                        "a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_",
                    )
                }
            )
        Assert.assertTrue(errors[0].equals("\$label: cannot be longer than 200 characters"))

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply { put("\$label", "") }
            )
        Assert.assertTrue(errors[0].equals("\$label: cannot be empty or only made of whitespace"))

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply { put("\$label", "with_multi_\n_line") }
            )
        Assert.assertTrue(errors[0].equals("\$label: cannot be multiline"))

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply { put("\$label", "a_valid_label") }
            )
        Assert.assertTrue(errors.isEmpty())
    }

    @Test
    fun testTagsAttribute() {
        var errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply {
                    putStringList(
                        "\$tags",
                        listOf(
                            "tag_0",
                            "tag_1",
                            "tag_2",
                            "tag_3",
                            "tag_4",
                            "tag_5",
                            "tag_6",
                            "tag_7",
                            "tag_8",
                            "tag_9",
                            "tag_10",
                        ),
                    )
                }
            )
        Assert.assertTrue(errors[0].equals("\$tags: must not contain more than 10 values"))

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply {
                    putStringList("\$tags", listOf("tag_0", "tag_1", ""))
                }
            )
        Assert.assertTrue(errors[0].equals("\$tags[2]: tag cannot be empty or made of whitespace"))

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply {
                    putStringList(
                        "\$tags",
                        listOf(
                            "tag_0",
                            "tag_too_long_tag_too_long_tag_too_long_tag_too_long_tag_too_long_",
                        ),
                    )
                }
            )
        Assert.assertTrue(errors[0].equals("\$tags[1]: tag cannot be longer than 64"))

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply {
                    putStringList("\$tags", listOf("tag_0", "tag_1", "tag_\n_2"))
                }
            )
        Assert.assertTrue(errors[0].equals("\$tags[2]: tag cannot be multiline"))

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply {
                    putStringList("\$tags", listOf("tag_0", "tag_1", "tag_2"))
                }
            )
        Assert.assertTrue(errors.isEmpty())
    }

    @Test
    fun testStringAttribute() {
        var errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply {
                    put(
                        "string_attr",
                        "a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_",
                    )
                }
            )
        Assert.assertTrue(
            errors[0].equals("string_attr: string attribute cannot be longer than 200 characters")
        )

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply { put("string_attr", "") }
            )
        Assert.assertTrue(
            errors[0].equals("string_attr: string attribute cannot be empty or made of whitespace")
        )

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply { put("string_attr", "with_multi_\n_line") }
            )
        Assert.assertTrue(errors[0].equals("string_attr: string attribute cannot be multiline"))

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply { put("string_attr", "a_valid_string") }
            )
        Assert.assertTrue(errors.isEmpty())

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply { put("String_Attr", "a_valid_string") }
            )
        Assert.assertTrue(errors.isEmpty())
    }

    @Test
    fun testURLAttribute() {
        var errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply {
                    put(
                        "url_attr",
                        URI(
                            "https://batch.com/home?id=a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_to_long"
                        ),
                    )
                }
            )
        Assert.assertTrue(
            errors[0].equals("url_attr: URL attributes cannot be longer than 2048 characters")
        )

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply { put("url_attr", URI("batch.com")) }
            )
        Assert.assertTrue(
            errors[0].equals(
                "url_attr: URL attributes must follow the format 'scheme://[authority][path][?query][#fragment]'"
            )
        )

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply {
                    put("url_attr", URI("https://batch.com/home?id=123"))
                }
            )
        Assert.assertTrue(errors.isEmpty())
    }

    @Test
    fun testObjectAttribute() {
        var errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply {
                    put(
                        "obj_attr",
                        BatchEventAttributes().apply {
                            put("\$label", "a_valid_label")
                            putStringList("\$tags", listOf("tag_0", "tag_1", "tag_2"))
                            put(
                                "sub_obj_1",
                                BatchEventAttributes().apply {
                                    put(
                                        "sub_obj_2",
                                        BatchEventAttributes().apply {
                                            put("sub_obj_3", BatchEventAttributes().apply {})
                                        },
                                    )
                                },
                            )
                        },
                    )
                }
            )
        Assert.assertTrue(
            errors[0].equals("obj_attr.\$label: Labels are not allowed in sub-objects")
        )
        Assert.assertTrue(errors[1].equals("obj_attr.\$tags: Tags are not allowed in sub-objects"))
        Assert.assertTrue(
            errors[2].equals(
                "obj_attr.sub_obj_1.sub_obj_2.sub_obj_3: Object attributes cannot be nested in more than three levels"
            )
        )

        var eventData = BatchEventAttributes()
        for (i in 0..20) {
            eventData.put("attr_$i", "val")
        }
        errors = EventAttributesValidator.computeValidationErrors(eventData)
        Assert.assertTrue(
            errors[0].equals("<attributes root>: objects cannot hold more than 20 attributes")
        )

        eventData =
            BatchEventAttributes().apply {
                put(
                    "my_car",
                    BatchEventAttributes().apply {
                        put("brand", "car_brand")
                        put("year", 2024)
                        put("4x4", false)
                        put("model_url", URI("https://batch.com/"))
                        put(
                            "engine",
                            BatchEventAttributes().apply {
                                put("manufacturer", "manu")
                                put("cylinders", 6)
                                put("cylinder_capacity", 3.5)
                                put("manufacturing_date", Date(1596975143943L))
                            },
                        )
                    },
                )
                put("string_attr", "a_test_string")
                put("int_attr", 13)
                put("double_attr", 13.4567)
                put("date_attr", Date(1596975143943L)) // "2020-08-09T12:12:23.943Z"
                put("url_attr", URI("https://batch.com/"))
                putStringList("string_list", listOf("A", "B", "C"))
                putObjectList(
                    "list_items",
                    listOf(
                        BatchEventAttributes().apply {
                            put("brand", "car_brand")
                            put("year", 2024)
                            put("4x4", false)
                            put("model_url", URI("https://batch.com/"))
                            put(
                                "engine",
                                BatchEventAttributes().apply {
                                    put("manufacturer", "manu")
                                    put("cylinders", 6)
                                    put("cylinder_capacity", 3.5)
                                    put("manufacturing_date", Date(1596975143943L))
                                },
                            )
                        },
                        BatchEventAttributes().apply {
                            put("brand", "car_brand")
                            put("year", 2024)
                            put("4x4", false)
                            put("model_url", URI("https://batch.com/"))
                            put(
                                "engine",
                                BatchEventAttributes().apply {
                                    put("manufacturer", "manu")
                                    put("cylinders", 6)
                                    put("cylinder_capacity", 3.5)
                                    put("manufacturing_date", Date(1596975143943L))
                                },
                            )
                        },
                    ),
                )
                put("\$label", "test_label")
                putStringList("\$tags", listOf("tagA", "tagB", "tagC", "tagC"))
            }
        errors = EventAttributesValidator.computeValidationErrors(eventData)
        Assert.assertTrue(errors.isEmpty())
    }

    @Test
    fun testStringArrayAttribute() {
        var errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply {
                    putStringList(
                        "string_array_attr",
                        listOf(
                            "a",
                            "b",
                            "a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_a_way_too_long_label_",
                        ),
                    )
                }
            )
        Assert.assertTrue(
            errors[0].equals(
                "string_array_attr[2]: string attribute cannot be longer than 200 characters"
            )
        )

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply {
                    putStringList("string_array_attr", listOf("a", "", "c"))
                }
            )
        Assert.assertTrue(
            errors[0].equals(
                "string_array_attr[1]: string attribute cannot be empty or made of whitespace"
            )
        )

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply {
                    putStringList("string_array_attr", listOf("a", "with\nlinebreak", "c"))
                }
            )
        Assert.assertTrue(
            errors[0].equals("string_array_attr[1]: string attribute cannot be multiline")
        )

        val list = ArrayList<String>()
        for (i in 0..25) {
            list.add("val_$i")
        }
        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply { putStringList("list_attr", list) }
            )
        Assert.assertTrue(
            errors[0].equals("list_attr: array attributes cannot have more than 25 elements")
        )

        errors =
            EventAttributesValidator.computeValidationErrors(
                BatchEventAttributes().apply {
                    putStringList("string_array_attr", listOf("a", "b", "c"))
                }
            )
        Assert.assertTrue(errors.isEmpty())
    }
}
