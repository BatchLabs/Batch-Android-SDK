package com.batch.android.event

import androidx.test.filters.SmallTest
import com.batch.android.BatchEventAttributes
import com.batch.android.json.JSONArray
import org.junit.Assert

import org.junit.Test
import java.net.URI
import java.util.Date

@SmallTest
class EventAttributesSerializerTest {

    @Test
    fun testSerialize() {
        val eventData = BatchEventAttributes().apply {
            put("my_car", BatchEventAttributes().apply {
                put("brand", "car_brand")
                put("year", 2024)
                put("4x4", false)
                put("model_url", URI("https://batch.com/"))
                put("engine", BatchEventAttributes().apply {
                    put("manufacturer", "manu")
                    put("cylinders", 6)
                    put("cylinder_capacity", 3.5)
                    put("manufacturing_date", Date(1596975143943L))
                })
            })
            put("string_attr", "a_test_string")
            put("int_attr", 13)
            put("double_attr", 13.4567)
            put("date_attr", Date(1596975143943L)) //"2020-08-09T12:12:23.943Z"
            put("url_attr",  URI("https://batch.com/"))
            putStringList("string_list", listOf("A", "B", "C"))
            putObjectList("list_items", listOf(
                    BatchEventAttributes().apply {
                        put("brand", "car_brand")
                        put("year", 2024)
                        put("4x4", false)
                        put("model_url", URI("https://batch.com/"))
                        put("engine", BatchEventAttributes().apply {
                            put("manufacturer", "manu")
                            put("cylinders", 6)
                            put("cylinder_capacity", 3.5)
                            put("manufacturing_date", Date(1596975143943L))
                        })
                    },
                    BatchEventAttributes().apply {
                        put("brand", "car_brand")
                        put("year", 2024)
                        put("4x4", false)
                        put("model_url", URI("https://batch.com/"))
                        put("engine", BatchEventAttributes().apply {
                            put("manufacturer", "manu")
                            put("cylinders", 6)
                            put("cylinder_capacity", 3.5)
                            put("manufacturing_date", Date(1596975143943L))
                        })
                    }
            ))
            put("\$label", "test_label")
            putStringList("\$tags", listOf("tagA", "tagB", "tagC", "tagC"))
        }

        val json = EventAttributesSerializer.serialize(eventData)
        Assert.assertEquals("test_label", json["label"])
        Assert.assertEquals(3, json.getJSONArray("tags").length())
        Assert.assertEquals(JSONArray(listOf("tagA", "tagB", "tagC")), json.getJSONArray("tags"))

        val attributes = json.getJSONObject("attributes")
        Assert.assertEquals("a_test_string", attributes["string_attr.s"])
        Assert.assertEquals(13, attributes["int_attr.i"])
        Assert.assertEquals(13.4567, attributes["double_attr.f"])
        Assert.assertEquals(1596975143943L, attributes["date_attr.t"])
        Assert.assertEquals("https://batch.com/", attributes["url_attr.u"])
        Assert.assertEquals(JSONArray(listOf("A", "B", "C")), attributes.getJSONArray("string_list.a"))

        val car = attributes.getJSONObject("my_car.o")
        Assert.assertEquals("car_brand", car["brand.s"])
        Assert.assertEquals(2024, car["year.i"])
        Assert.assertEquals("https://batch.com/", car["model_url.u"])
        Assert.assertEquals(false, car["4x4.b"])

        val engine =  car.getJSONObject("engine.o")
        Assert.assertEquals("manu", engine["manufacturer.s"])
        Assert.assertEquals(6, engine["cylinders.i"])
        Assert.assertEquals(3.5, engine["cylinder_capacity.f"])
        Assert.assertEquals(1596975143943L, engine["manufacturing_date.t"])

        val items = attributes.getJSONArray("list_items.a")
        Assert.assertEquals(2, items.length())

        val item = items.getJSONObject(0)
        Assert.assertEquals("car_brand", item["brand.s"])
        Assert.assertEquals(2024, item["year.i"])
        Assert.assertEquals("https://batch.com/", item["model_url.u"])
        Assert.assertEquals(false, item["4x4.b"])
        Assert.assertEquals("manu", item.getJSONObject("engine.o")["manufacturer.s"])
        Assert.assertEquals(6, item.getJSONObject("engine.o")["cylinders.i"])
        Assert.assertEquals(3.5, item.getJSONObject("engine.o")["cylinder_capacity.f"])
        Assert.assertEquals(1596975143943L, item.getJSONObject("engine.o")["manufacturing_date.t"])
    }
}