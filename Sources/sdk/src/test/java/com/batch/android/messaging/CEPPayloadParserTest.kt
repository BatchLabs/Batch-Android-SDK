package com.batch.android.messaging

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.batch.android.json.JSONArray
import com.batch.android.json.JSONObject
import com.batch.android.messaging.model.Action
import com.batch.android.messaging.model.cep.CEPMessage
import com.batch.android.messaging.model.cep.CloseOptions
import com.batch.android.messaging.model.cep.InAppComponent
import com.batch.android.messaging.model.cep.InAppProperty
import com.batch.android.messaging.model.cep.InAppProperty.Format
import com.batch.android.messaging.model.cep.RootContainer
import com.batch.android.messaging.parsing.CEPPayloadParser
import com.batch.android.messaging.parsing.PayloadParsingException
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@SmallTest
class CEPPayloadParserTest {

    private val jsonPayload =
        JSONObject(
            "{\"id\":\"05e5b035b98452bfed9913c165f7c9cb-push_action-u1738580470616\",\"format\":\"fullscreen\",\"position\":\"center\",\"minMLvl\":30,\"trackingId\":\"my-custom-tracking-identifer\",\"root\":{\"radius\":[10,15,20,25],\"borderWidth\":1,\"borderColor\":[\"#FFFFFF00\",\"#00000000\"],\"backgroundColor\":[\"#FFFFFF00\",\"#00000000\"],\"margin\":[4,8,4,8],\"children\":[{\"id\":\"IMG_1\",\"type\":\"image\",\"aspect\":\"fit\",\"margin\":[8,8,8,8],\"height\":\"300px\",\"radius\":[8,8,8,8],\"contentDescription\":\"A cat riding a bike\"},{\"id\":\"TXT_1\",\"type\":\"text\",\"fontSize\":12,\"margin\":[4,4,4,4],\"textAlign\":\"center\",\"fontDecoration\":[\"bold\",\"italic\"],\"padding\":4,\"color\":[\"#CCFF22\",\"#330000\"],\"maxLines\":null},{\"id\":\"TXT_2\",\"type\":\"text\",\"fontSize\":14,\"margin\":[4,4,4,4],\"textAlign\":\"center\",\"fontDecoration\":[\"italic\"],\"padding\":[4,4,4,4],\"color\":[\"#CCFF22\",\"#330000\"],\"maxLines\":null},{\"type\":\"spacer\",\"height\":\"fill\"},{\"type\":\"columns\",\"contentAlign\":\"center\",\"spacing\":8,\"margin\":[4,4,4,4],\"ratios\":[0.4,0.2,0.4],\"children\":[{\"id\":\"BTN_1\",\"type\":\"button\",\"radius\":[4,4,4,4],\"margin\":[4,4,4,4],\"padding\":[4,4,4,4],\"width\":\"80%\",\"align\":\"center\",\"textAlign\":\"center\",\"textColor\":[\"#CCFF22\",\"#330000\"],\"fontDecoration\":[\"bold\",\"italic\"],\"backgroundColor\":[\"#FFFFFFFF\",\"#00000000\"],\"fontSize\":11,\"maxLines\":1},null,{\"id\":\"BTN_2\",\"type\":\"button\",\"radius\":[4,4,4,4],\"fontDecoration\":[\"bold\"],\"margin\":[4,4,4,4],\"padding\":[4,4,4,4],\"width\":\"80%\",\"align\":\"center\",\"textAlign\":\"center\",\"textColor\":[\"#CCFF22\",\"#330000\"],\"backgroundColor\":[\"#FFFFFFFF\",\"#00000000\"],\"fontSize\":11,\"maxLines\":1}]}]},\"urls\":{\"IMG_1\":\"https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png\"},\"texts\":{\"IMG_1\":\"A cat riding a bike\",\"TXT_1\":\"<span data-color='#CC000000' data-color-dark='#CCFFFF00' data-b>This is a title</span>\",\"TXT_2\":\"<span data-color='#00000000' data-color-dark='#FFFFFF00' data-b>Lorem ipsum dolor ♥️ sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</span>\",\"BTN_1\":\"<span data-color='#CC000000' data-color-dark='#CCFFFF00' data-b>Nope</span>\",\"BTN_2\":\"<span data-color='#CC000000' data-color-dark='#CCFFFF00' data-b>Yes, I want to subscribe</span>\"},\"actions\":{\"IMG_1\":{\"action\":\"batch.deeplink\",\"params\":{\"u\":\"https://www.google.com\",\"i\":true}},\"BTN_1\":{\"action\":\"batch.dismiss\"},\"BTN_2\":{\"action\":\"batch.clipboard\",\"params\":{\"t\":\"PROMO_CODE\"}}},\"closeOptions\":{\"auto\":{\"delay\":5,\"color\":[\"#00000000\",\"#FFFFFF00\"]},\"button\":{\"backgroundColor\":[\"#00000000\",\"#FFFFFF00\"],\"color\":[\"#FFFFFF00\",\"#00000000\"]}},\"eventData\":{\"n\":\"2d3e2b40-e21e-11ef-84ff-01e8fce91ad8\",\"an\":\"push_action\",\"ct\":\"05e5b035b98452bfed9913c165f7c9cb\",\"i\":\"05e5b035b98452bfed9913c165f7c9cb-push_action\"}}"
        )

    private val webViewJsonPayload =
        JSONObject(
            "{\"urls\":{\"01E34289-51CF-45D4-8EF0-9F76F1AE55A8\":\"https://batch.com\"},\"closeOptions\":{\"button\":{\"color\":[\"#292945ff\",\"#7575ffff\"],\"backgroundColor\":[\"#ebebebff\",\"#4d4d4dff\"]}},\"minMLvl\":0,\"texts\":{},\"position\":\"top\",\"root\":{\"radius\":[0,0,0,0],\"backgroundColor\":[\"#FFFFFFFF\",\"000000FF\"],\"children\":[{\"devMode\":true,\"timeout\":10,\"inAppDeeplinks\":true,\"type\":\"webview\",\"id\":\"01E34289-51CF-45D4-8EF0-9F76F1AE55A8\"}],\"margin\":[0,0,0,0],\"borderWidth\":0,\"borderColor\":[\"#ff0000ff\",\"#ffbdbdff\"]},\"actions\":{},\"eventData\":{\"n\":\"push_06c2tbq1zbhv03h7318n0484t9rwe0q6\",\"pid\":\"push_06c2tbq1zbhv03h7318n0484t9rwe0q6\"},\"format\":\"webview\"}"
        )

    private val expectedMessage =
        CEPMessage(
            Format.FULLSCREEN,
            RootContainer(
                listOf(
                    InAppComponent.Image(
                        "IMG_1",
                        InAppProperty.Size("300px"),
                        InAppProperty.Margin(8, 8, 8, 8),
                        InAppComponent.Image.AspectRatio.FIT,
                        InAppProperty.CornerRadius(8, 8, 8, 8),
                    ),
                    InAppComponent.Text(
                        "TXT_1",
                        InAppProperty.ThemeColors("#CCFF22", "#330000"),
                        InAppProperty.Margin(4, 4, 4, 4),
                        InAppProperty.HorizontalAlignment.CENTER,
                        setOf(
                            InAppProperty.FontDecoration.BOLD,
                            InAppProperty.FontDecoration.ITALIC,
                        ),
                        12,
                        0,
                    ),
                    InAppComponent.Text(
                        "TXT_2",
                        InAppProperty.ThemeColors("#CCFF22", "#330000"),
                        InAppProperty.Margin(4, 4, 4, 4),
                        InAppProperty.HorizontalAlignment.CENTER,
                        setOf(InAppProperty.FontDecoration.ITALIC),
                        14,
                        0,
                    ),
                    InAppComponent.Spacer(InAppProperty.Size("fill")),
                    InAppComponent.Columns(
                        floatArrayOf(0.4F, 0.2F, 0.4F),
                        8,
                        InAppProperty.Margin(4),
                        InAppProperty.VerticalAlignment.CENTER,
                        listOf(
                            InAppComponent.Button(
                                "BTN_1",
                                InAppProperty.ThemeColors("#FFFFFFFF", "#00000000"),
                                InAppProperty.ThemeColors("#CCFF22", "#330000"),
                                InAppProperty.Margin(4),
                                InAppProperty.Margin(4),
                                InAppProperty.Size("80%"),
                                InAppProperty.HorizontalAlignment.CENTER,
                                InAppProperty.CornerRadius(4),
                                null,
                                InAppProperty.HorizontalAlignment.CENTER,
                                setOf(
                                    InAppProperty.FontDecoration.ITALIC,
                                    InAppProperty.FontDecoration.BOLD,
                                ),
                                11,
                                1,
                            ),
                            InAppComponent.EmptySpacer(),
                            InAppComponent.Button(
                                "BTN_2",
                                InAppProperty.ThemeColors("#FFFFFFFF", "#00000000"),
                                InAppProperty.ThemeColors("#CCFF22", "#330000"),
                                InAppProperty.Margin(4),
                                InAppProperty.Margin(4),
                                InAppProperty.Size("80%"),
                                InAppProperty.HorizontalAlignment.CENTER,
                                InAppProperty.CornerRadius(4),
                                null,
                                InAppProperty.HorizontalAlignment.CENTER,
                                setOf(InAppProperty.FontDecoration.BOLD),
                                11,
                                1,
                            ),
                        ),
                    ),
                ),
                InAppProperty.ThemeColors("#FFFFFF00", "#00000000"),
                InAppProperty.Margin(4, 8),
                InAppProperty.CornerRadius(10, 15, 20, 25),
                InAppProperty.Border(1, InAppProperty.ThemeColors("#FFFFFF00", "#00000000")),
            ),
            InAppProperty.VerticalAlignment.CENTER,
            CloseOptions(
                CloseOptions.Auto(5, InAppProperty.ThemeColors("#00000000", "#FFFFFF00")),
                CloseOptions.Button(
                    InAppProperty.ThemeColors("#FFFFFF00", "#00000000"),
                    InAppProperty.ThemeColors("#00000000", "#FFFFFF00"),
                ),
            ),
            mapOf(
                "IMG_1" to "A cat riding a bike",
                "TXT_1" to
                    "<span data-color='#CC000000' data-color-dark='#CCFFFF00' data-b>This is a title</span>",
                "TXT_2" to
                    "<span data-color='#00000000' data-color-dark='#FFFFFF00' data-b>Lorem ipsum dolor ♥️ sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</span>",
                "BTN_1" to
                    "<span data-color='#CC000000' data-color-dark='#CCFFFF00' data-b>Nope</span>",
                "BTN_2" to
                    "<span data-color='#CC000000' data-color-dark='#CCFFFF00' data-b>Yes, I want to subscribe</span>",
            ),
            mapOf(
                "IMG_1" to
                    "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"
            ),
            mapOf(
                "IMG_1" to
                    Action(
                        "batch.deeplink",
                        JSONObject().apply {
                            put("u", "https://www.google.com")
                            put("i", true)
                        },
                    ),
                "BTN_1" to Action("batch.dismiss", null),
                "BTN_2" to Action("batch.clipboard", JSONObject().apply { put("t", "PROMO_CODE") }),
            ),
        )

    @Test
    fun testParsePayload() {
        val current = CEPPayloadParser.parsePayload(jsonPayload)
        Assert.assertEquals(expectedMessage, current)
    }

    @Test
    fun testParseRootContainer() {
        val format = Format.valueOf(jsonPayload.getString("format").uppercase())
        val current = CEPPayloadParser.parseRootContainer(jsonPayload.getJSONObject("root"), format)
        Assert.assertEquals(expectedMessage.rootContainer, current)
    }

    @Test
    fun testParseComponents() {
        val format = Format.valueOf(jsonPayload.getString("format").uppercase())
        val current =
            CEPPayloadParser.parseComponents(
                jsonPayload.getJSONObject("root").getJSONArray("children"),
                format,
            )
        Assert.assertEquals(expectedMessage.rootContainer.children, current)
    }

    @Test
    fun testParseColumnsChildren() {
        val inputs = JSONArray().apply { put(JSONObject().put("type", "columns")) }
        assertFailsWith(
            exceptionClass = PayloadParsingException::class,
            message = "Columns component cannot have columns children",
            block = { CEPPayloadParser.parseColumnsChildren(inputs, Format.MODAL) },
        )
    }

    @Test
    fun testParseComponent() {
        val parser = spy(CEPPayloadParser)
        val components =
            listOf(
                JSONObject().put("type", "text"),
                JSONObject().put("type", "button"),
                JSONObject().put("type", "image"),
                JSONObject().put("type", "divider"),
                JSONObject().put("type", "columns"),
            )
        for (component in components) {
            try {
                parser.parseComponent(component, Format.FULLSCREEN)
            } catch (_: Exception) {}
        }
        verify(parser, times(1)).parseText(components[0])
        verify(parser, times(1)).parseButton(components[1])
        verify(parser, times(1)).parseImage(components[2], Format.FULLSCREEN)
        verify(parser, times(1)).parseDivider(components[3])
        verify(parser, times(1)).parseColumns(components[4], Format.FULLSCREEN)
    }

    @Test
    fun testParseText() {
        val expected = expectedMessage.rootContainer.children[1]
        val current =
            CEPPayloadParser.parseText(
                jsonPayload.getJSONObject("root").getJSONArray("children").getJSONObject(1)
            )
        Assert.assertEquals(expected, current)
        Assert.assertThrows(PayloadParsingException::class.java) {
            CEPPayloadParser.parseText(null)
        }
    }

    @Test
    fun testParseButton() {
        val expected =
            (expectedMessage.rootContainer.children[4] as InAppComponent.Columns).children[0]
        val current =
            CEPPayloadParser.parseButton(
                jsonPayload
                    .getJSONObject("root")
                    .getJSONArray("children")
                    .getJSONObject(4)
                    .getJSONArray("children")
                    .getJSONObject(0)
            )
        Assert.assertEquals(expected, current)
        Assert.assertThrows(PayloadParsingException::class.java) {
            CEPPayloadParser.parseButton(null)
        }
    }

    @Test
    fun testParseImage() {
        val expected = expectedMessage.rootContainer.children[0] as InAppComponent.Image
        val current =
            CEPPayloadParser.parseImage(
                jsonPayload.getJSONObject("root").getJSONArray("children").getJSONObject(0),
                Format.FULLSCREEN,
            )
        Assert.assertEquals(expected, current)
        Assert.assertThrows(PayloadParsingException::class.java) {
            CEPPayloadParser.parseImage(null, Format.FULLSCREEN)
        }
        Assert.assertThrows(PayloadParsingException::class.java) {
            CEPPayloadParser.parseImage(
                JSONObject(
                    "{\"id\":\"IMG_1\",\"type\":\"image\",\"aspect\":\"fit\",\"margin\":[8,8,8,8],\"height\":\"fill\",\"radius\":[8,8,8,8],\"contentDescription\":\"A cat riding a bike\"}"
                ),
                Format.MODAL,
            )
        }
    }

    @Test
    fun testParseDivider() {
        val expected =
            InAppComponent.Divider(
                2,
                InAppProperty.ThemeColors("#000", "FFF"),
                InAppProperty.Size("80%"),
                InAppProperty.HorizontalAlignment.CENTER,
                InAppProperty.Margin(8, 0),
            )
        val current =
            CEPPayloadParser.parseDivider(
                JSONObject().apply {
                    put("type", "divider")
                    put("thickness", 2)
                    put(
                        "color",
                        JSONArray().apply {
                            put("#000")
                            put("FFF")
                        },
                    )
                    put("width", "80%")
                    put("align", "center")
                    put(
                        "margin",
                        JSONArray().apply {
                            put(8)
                            put(0)
                            put(8)
                            put(0)
                        },
                    )
                }
            )
        Assert.assertEquals(expected, current)
        Assert.assertThrows(PayloadParsingException::class.java) {
            CEPPayloadParser.parseDivider(null)
        }
    }

    @Test
    fun testParseSpacer() {
        val expected = InAppComponent.Spacer(InAppProperty.Size("fill"))
        val expected2 = InAppComponent.Spacer(InAppProperty.Size("300px"))
        val current =
            CEPPayloadParser.parseSpacer(
                JSONObject().apply {
                    put("type", "spacer")
                    put("height", "fill")
                },
                Format.FULLSCREEN,
            )
        val current2 =
            CEPPayloadParser.parseSpacer(
                JSONObject().apply {
                    put("type", "spacer")
                    put("height", "300px")
                },
                Format.FULLSCREEN,
            )
        val current3 =
            CEPPayloadParser.parseSpacer(
                JSONObject().apply {
                    put("type", "spacer")
                    put("height", "300px")
                },
                Format.MODAL,
            )
        Assert.assertEquals(expected, current)
        Assert.assertEquals(expected2, current2)
        Assert.assertEquals(expected2, current3)
        Assert.assertThrows(PayloadParsingException::class.java) {
            CEPPayloadParser.parseDivider(null)
        }
        Assert.assertThrows(PayloadParsingException::class.java) {
            CEPPayloadParser.parseSpacer(
                JSONObject().apply {
                    put("type", "spacer")
                    put("height", "fill")
                },
                Format.MODAL,
            )
        }
    }

    @Test
    fun testParseColumns() {
        val expected = expectedMessage.rootContainer.children[4] as InAppComponent.Columns
        val current =
            CEPPayloadParser.parseColumns(
                jsonPayload.getJSONObject("root").getJSONArray("children").getJSONObject(4),
                Format.FULLSCREEN,
            )
        Assert.assertEquals(expected, current)
        Assert.assertThrows(PayloadParsingException::class.java) {
            CEPPayloadParser.parseColumns(null, Format.FULLSCREEN)
        }
    }

    @Test
    fun testParseWebView() {
        val format = Format.valueOf(webViewJsonPayload.getString("format").uppercase())
        Assert.assertEquals(Format.WEBVIEW, format)

        val current =
            CEPPayloadParser.parseWebView(
                webViewJsonPayload.getJSONObject("root").getJSONArray("children").getJSONObject(0)
            )
        Assert.assertEquals(
            InAppComponent.WebView("01E34289-51CF-45D4-8EF0-9F76F1AE55A8", 10, true, true),
            current,
        )
    }

    @Test
    fun testParseCloseOptions() {
        val expected = expectedMessage.closeOptions
        val current = CEPPayloadParser.parseCloseOptions(jsonPayload.getJSONObject("closeOptions"))
        Assert.assertEquals(expected, current)
    }

    @Test
    fun testParseActions() {
        val expected = expectedMessage.actions
        val current = CEPPayloadParser.parseActions(jsonPayload.getJSONObject("actions"))
        Assert.assertEquals(expected, current)
    }

    @Test
    fun testParseBorder() {
        val expected = InAppProperty.Border(1, InAppProperty.ThemeColors("#FFFFFF00", "#00000000"))
        val current = CEPPayloadParser.parseBorder(jsonPayload.getJSONObject("root"))
        Assert.assertEquals(expected, current)
    }

    @Test
    fun testParseFontDecoration() {
        val current =
            CEPPayloadParser.parseFontDecoration(
                jsonPayload
                    .getJSONObject("root")
                    .getJSONArray("children")
                    .getJSONObject(1)
                    .getJSONArray("fontDecoration")
            )
        val expected = setOf(InAppProperty.FontDecoration.BOLD, InAppProperty.FontDecoration.ITALIC)
        Assert.assertEquals(expected, current)
    }

    @Test
    fun testParseColor() {
        val current =
            CEPPayloadParser.parseColor(
                jsonPayload.getJSONObject("root").getJSONArray("backgroundColor")
            )
        Assert.assertEquals(InAppProperty.ThemeColors("#FFFFFF00", "#00000000"), current)
        Assert.assertEquals(
            InAppProperty.ThemeColors("#FFFFFF00", "#FFFFFF00"),
            CEPPayloadParser.parseColor(JSONArray().apply { put("#FFFFFF00") }),
        )
        Assert.assertEquals(
            InAppProperty.ThemeColors(
                CEPPayloadParser.DEFAULT_LIGHT_COLOR,
                CEPPayloadParser.DEFAULT_DARK_COLOR,
            ),
            CEPPayloadParser.parseColor(JSONArray()),
        )
        Assert.assertEquals(
            InAppProperty.ThemeColors(
                CEPPayloadParser.DEFAULT_LIGHT_COLOR,
                CEPPayloadParser.DEFAULT_DARK_COLOR,
            ),
            CEPPayloadParser.parseColor(null),
        )
    }

    @Test
    fun testParseMargin() {
        val expected = InAppProperty.Margin(4, 8, 4, 8)
        val current =
            CEPPayloadParser.parseMargin(jsonPayload.getJSONObject("root").getJSONArray("margin"))
        Assert.assertEquals(expected, current)

        val current2 =
            CEPPayloadParser.parseMargin(
                JSONArray().apply {
                    put(4)
                    put(8)
                }
            )
        Assert.assertEquals(4f, current2.top)
        Assert.assertEquals(8f, current2.right)
        Assert.assertEquals(4f, current2.bottom)
        Assert.assertEquals(8f, current2.left)

        Assert.assertEquals(InAppProperty.Margin(), CEPPayloadParser.parseMargin(null))
        Assert.assertThrows(PayloadParsingException::class.java) {
            CEPPayloadParser.parseMargin(
                JSONArray().apply {
                    put(1)
                    put(2)
                    put(3)
                }
            )
        }
        Assert.assertThrows(PayloadParsingException::class.java) {
            CEPPayloadParser.parseMargin(
                JSONArray().apply {
                    put(1)
                    put(2)
                    put(3)
                    put(4)
                    put(5)
                }
            )
        }
    }

    @Test
    fun testParseRadius() {
        val current =
            CEPPayloadParser.parseRadius(jsonPayload.getJSONObject("root").getJSONArray("radius"))
        val expected = InAppProperty.CornerRadius(10, 15, 20, 25)
        Assert.assertEquals(expected, current)

        val current2 =
            CEPPayloadParser.parseRadius(
                JSONArray().apply {
                    put(10)
                    put(15)
                }
            )
        Assert.assertEquals(10f, current2.topLeft)
        Assert.assertEquals(15f, current2.topRight)
        Assert.assertEquals(10f, current2.bottomRight)
        Assert.assertEquals(15f, current2.bottomLeft)

        Assert.assertEquals(InAppProperty.CornerRadius(), CEPPayloadParser.parseRadius(null))
        Assert.assertThrows(PayloadParsingException::class.java) {
            CEPPayloadParser.parseRadius(
                JSONArray().apply {
                    put(1)
                    put(2)
                    put(3)
                }
            )
        }
        Assert.assertThrows(PayloadParsingException::class.java) {
            CEPPayloadParser.parseRadius(
                JSONArray().apply {
                    put(1)
                    put(2)
                    put(3)
                    put(4)
                    put(5)
                }
            )
        }
    }

    @Test
    fun testParseColumnRatio() {
        val current =
            CEPPayloadParser.parseColumnRatio(
                jsonPayload
                    .getJSONObject("root")
                    .getJSONArray("children")
                    .getJSONObject(4)
                    .getJSONArray("ratios")
            )
        val expected = floatArrayOf(0.4F, 0.2F, 0.4F)
        assertContentEquals(expected, current)
        Assert.assertTrue(current.sum() == 1F)
    }

    @Test
    fun testParseMap() {
        val current = CEPPayloadParser.parseMap(jsonPayload.getJSONObject("texts"))
        val expected = expectedMessage.texts
        Assert.assertEquals(expected, current)
    }
}
