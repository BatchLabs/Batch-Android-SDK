package com.batch.android.messaging.parsing

import androidx.annotation.VisibleForTesting
import com.batch.android.core.Logger
import com.batch.android.core.Parameters
import com.batch.android.json.JSONArray
import com.batch.android.json.JSONException
import com.batch.android.json.JSONObject
import com.batch.android.messaging.model.Action
import com.batch.android.messaging.model.Message
import com.batch.android.messaging.model.cep.CEPMessage
import com.batch.android.messaging.model.cep.CloseOptions
import com.batch.android.messaging.model.cep.InAppComponent
import com.batch.android.messaging.model.cep.InAppProperty.*
import com.batch.android.messaging.model.cep.RootContainer
import com.batch.android.module.MessagingModule

object CEPPayloadParser {

    /** Default values for light and dark colors. */
    @VisibleForTesting const val DEFAULT_LIGHT_COLOR: String = "#FFFFFFFF"

    /** Default value for dark color. */
    @VisibleForTesting const val DEFAULT_DARK_COLOR: String = "#000000FF"

    /** Default values for margins */
    private const val DEFAULT_MARGIN: Int = 0

    /** Default values for radius */
    private const val DEFAULT_RADIUS: Int = 4

    /** Default values for border width */
    private const val DEFAULT_BORDER_WIDTH: Int = 0

    /** Default values for max lines */
    private const val DEFAULT_MAX_LINES: Int = 0

    /** Default values for max lines */
    private const val DEFAULT_FONT_SIZE: Int = 12

    /** Default values for thickness */
    private const val DEFAULT_THICKNESS: Int = 0

    /** Default values for timeout */
    const val DEFAULT_WEBVIEW_TIMEOUT: Int = 10

    /**
     * Parses a JSON payload representing an in-app message into a `Message` object.
     *
     * @param payload The JSON object containing the message payload.
     * @return A `Message` object representing the parsed in-app message.
     * @throws PayloadParsingException If the payload is malformed or missing required fields, or if
     *   any parsing errors occur. The exception message will contain details about the failure.
     */
    @JvmStatic
    @Throws(PayloadParsingException::class)
    fun parsePayload(payload: JSONObject): Message {
        try {

            val minMessagingApiLevel = payload.reallyOptInteger("minMLvl", null)

            // Ensure the the SDK is not too old to display the message
            if (
                minMessagingApiLevel != null &&
                    minMessagingApiLevel > Parameters.MESSAGING_API_LEVEL
            ) {
                Logger.error(
                    MessagingModule.TAG,
                    "This SDK is too old to display this message. Please update it.",
                )
                throw PayloadParsingException("SDK too old")
            }
            val format = Format.valueOf(payload.getString("format").uppercase())
            val rootContainer = parseRootContainer(payload.getJSONObject("root"), format)
            val position =
                VerticalAlignment.valueOf(payload.reallyOptString("position", "center").uppercase())
            val closeOptions = parseCloseOptions(payload.optJSONObject("closeOptions"))
            val texts = parseMap(payload.optJSONObject("texts"))
            val urls = parseMap(payload.optJSONObject("urls"))
            val actions = parseActions(payload.optJSONObject("actions"))
            val message =
                CEPMessage(format, rootContainer, position, closeOptions, texts, urls, actions)
                    .apply {
                        this.devTrackingIdentifier = payload.reallyOptString("trackingId", null)
                        this.eventData = payload.optJSONObject("eventData")
                    }
            return message
        } catch (e: Exception) {
            throw PayloadParsingException("Failed parsing mobile landing.", e)
        }
    }

    /**
     * Parses a JSON object representing the root container of a layout.
     *
     * @param payload The JSON object containing the root container data. It is expected to have
     *   keys such as "children", "backgroundColor", "margins", "radius", and "border".
     * @return A [RootContainer] object populated with the parsed data.
     * @throws PayloadParsingException if the payload is null or if any required fields are missing
     *   or malformed.
     * @throws JSONException if there are issues with parsing the json array or json object.
     */
    @VisibleForTesting
    fun parseRootContainer(payload: JSONObject?, format: Format): RootContainer {
        if (payload == null) throw PayloadParsingException("Root container cannot be null")
        return RootContainer(
            parseComponents(payload.getJSONArray("children"), format),
            parseColor(payload.optJSONArray("backgroundColor")),
            parseMargin(payload.optJSONArray("margin")),
            parseRadius(payload.optJSONArray("radius")),
            parseBorder(payload),
        )
    }

    /**
     * Parses a JSONArray representing a list of in-app component definitions into a list of
     * `InAppComponent` objects.
     *
     * @param payload The JSONArray containing the definitions of in-app components. Each element in
     *   the array is expected to be a JSONObject.
     * @return A list of `InAppComponent` objects parsed from the input JSONArray.
     * @throws PayloadParsingException If the provided `payload` is null or empty, indicating an
     *   invalid or missing component definition.
     * @throws JSONException if the provided payload is not a valid JSONArray or if elements within
     *   the array are not valid JSONObjects.
     * @throws Exception if parseComponent throws exception
     */
    @VisibleForTesting
    fun parseComponents(payload: JSONArray?, format: Format): List<InAppComponent> {
        if (payload == null || payload.length() == 0)
            throw PayloadParsingException("children cannot be null or empty")
        val result = mutableListOf<InAppComponent>()
        for (i in 0 until payload.length()) {
            val child = payload.getJSONObject(i)
            result.add(parseComponent(child, format))
        }
        return result.toList()
    }

    /**
     * Parses a JSONArray representing a list of column definitions into a list of
     * `InAppComponent.Column` objects.
     *
     * @param payload The JSONArray containing the columns of in-app components. Each element in the
     *   array is expected to be a JSONObject.
     * @return A list of `InAppComponent.Column` objects parsed from the input JSONArray.
     */
    @VisibleForTesting
    fun parseColumnsChildren(payload: JSONArray?, format: Format): List<InAppComponent.Column> {
        if (payload == null || payload.length() == 0)
            throw PayloadParsingException("children cannot be null or empty")
        val result = mutableListOf<InAppComponent.Column>()
        for (i in 0 until payload.length()) {
            if (payload.isNull(i)) {
                result.add(InAppComponent.EmptySpacer())
                continue
            }
            val child = payload.getJSONObject(i)
            if (child.getString("type") == "columns") {
                throw PayloadParsingException("Columns component cannot have columns children")
            }
            result.add(parseComponent(child, format) as InAppComponent.Column)
        }
        return result.toList()
    }

    /**
     * Parses a JSON payload representing a component and returns the corresponding InAppComponent
     * object.
     *
     * This function acts as a factory for creating different types of InAppComponent objects based
     * on the "type" field within the provided JSON payload. It supports parsing for "text",
     * "button", "image", "divider", and "columns" component types.
     *
     * @param payload The JSONObject representing the component. Must not be null and must contain a
     *   "type" field.
     * @return An instance of InAppComponent corresponding to the parsed component type.
     * @throws PayloadParsingException If the payload is null, if the "type" field is missing or if
     *   the component type is unknown.
     */
    @VisibleForTesting
    fun parseComponent(payload: JSONObject?, format: Format): InAppComponent {
        if (payload == null) throw PayloadParsingException("Component cannot be null")
        return when (payload.getString("type")) {
            "text" -> parseText(payload)
            "button" -> parseButton(payload)
            "image" -> parseImage(payload, format)
            "divider" -> parseDivider(payload)
            "columns" -> parseColumns(payload, format)
            "spacer" -> parseSpacer(payload, format)
            "webview" -> parseWebView(payload)
            else ->
                throw PayloadParsingException(
                    "Unknown component type: ${payload.getString("type")}"
                )
        }
    }

    /**
     * Parses a JSON payload to create an [InAppComponent.Text] object.
     *
     * @param payload The JSON object containing the text properties. Must not be null.
     * @return An [InAppComponent.Text] object initialized with the parsed properties.
     * @throws PayloadParsingException If the `payload` is null or if there is an error during
     *   parsing.
     * @throws IllegalArgumentException if "textAlignment" value is invalid.
     * @throws JSONException If there is any error while accessing the json object
     */
    @VisibleForTesting
    fun parseText(payload: JSONObject?): InAppComponent.Text {
        if (payload == null) throw PayloadParsingException("Text cannot be null")
        return InAppComponent.Text(
            payload.getString("id"),
            parseColor(payload.getJSONArray("color")),
            parseMargin(payload.optJSONArray("margin")),
            HorizontalAlignment.valueOf(payload.reallyOptString("textAlign", "center").uppercase()),
            parseFontDecoration(payload.optJSONArray("fontDecoration")),
            payload.reallyOptInteger("fontSize", DEFAULT_FONT_SIZE),
            payload.reallyOptInteger("maxLines", DEFAULT_MAX_LINES),
        )
    }

    /**
     * Parses a JSON payload representing a button and returns an [InAppComponent.Button] object.
     *
     * @param payload The JSON object containing the button's configuration.
     * @return An [InAppComponent.Button] object representing the parsed button.
     * @throws PayloadParsingException If the payload is null, or if mandatory fields in JSON Object
     *   are missing
     * @throws IllegalArgumentException If an invalid value is provided for "align" or
     *   "textAlignment"
     * @throws JSONException If there is any error while accessing the json object
     */
    @VisibleForTesting
    fun parseButton(payload: JSONObject?): InAppComponent.Button {
        if (payload == null) throw PayloadParsingException("Button cannot be null")
        return InAppComponent.Button(
            payload.getString("id"),
            parseColor(payload.optJSONArray("backgroundColor")),
            parseColor(payload.optJSONArray("textColor")),
            parseMargin(payload.optJSONArray("margin")),
            parseMargin(payload.optJSONArray("padding")),
            Size(payload.reallyOptString("width", "100%")),
            HorizontalAlignment.valueOf(payload.reallyOptString("align", "center").uppercase()),
            parseRadius(payload.optJSONArray("radius")),
            parseBorder(payload),
            HorizontalAlignment.valueOf(payload.reallyOptString("textAlign", "center").uppercase()),
            parseFontDecoration(payload.optJSONArray("fontDecoration")),
            payload.reallyOptInteger("fontSize", DEFAULT_FONT_SIZE),
            payload.reallyOptInteger("maxLines", DEFAULT_MAX_LINES),
        )
    }

    /**
     * Parses a JSON payload to create an InAppComponent.Image object.
     *
     * @param payload The JSON object containing the image's configuration.
     * @return An InAppComponent.Image object representing the parsed image.
     * @throws PayloadParsingException If the payload is null, or if mandatory fields in JSON Object
     *   are missing
     * @throws IllegalArgumentException If an invalid value is provided for "align"
     * @throws JSONException If there is any error while accessing the json object
     */
    @VisibleForTesting
    fun parseImage(payload: JSONObject?, format: Format): InAppComponent.Image {
        if (payload == null) throw PayloadParsingException("Image cannot be null")

        val size = Size(payload.getString("height"))
        if (format == Format.MODAL && size.isFill()) {
            throw PayloadParsingException("Image cannot have fill value in a Modal format")
        }

        return InAppComponent.Image(
            payload.getString("id"),
            size,
            parseMargin(payload.optJSONArray("margin")),
            InAppComponent.Image.AspectRatio.valueOf(
                payload
                    .reallyOptString("aspect", InAppComponent.Image.AspectRatio.FILL.toString())
                    .uppercase()
            ),
            parseRadius(payload.optJSONArray("radius")),
        )
    }

    /**
     * Parses a JSON payload to create an InAppComponent.Divider object.
     *
     * @param payload The JSON object containing the divider's configuration.
     * @return An InAppComponent.Divider object representing the parsed divider.
     * @throws PayloadParsingException If the payload is null, or if mandatory fields in JSON Object
     *   are missing
     * @throws IllegalArgumentException If an invalid value is provided for "align"
     */
    @VisibleForTesting
    fun parseDivider(payload: JSONObject?): InAppComponent.Divider {
        if (payload == null) throw PayloadParsingException("Divider cannot be null")
        return InAppComponent.Divider(
            payload.reallyOptInteger("thickness", DEFAULT_THICKNESS),
            parseColor(payload.optJSONArray("color")),
            Size(payload.reallyOptString("width", "100%")),
            HorizontalAlignment.valueOf(payload.reallyOptString("align", "center").uppercase()),
            parseMargin(payload.optJSONArray("margin")),
        )
    }

    /**
     * Parses a JSON payload to create an InAppComponent.Columns object.
     *
     * @param payload The JSON object containing the columns' configuration.
     * @return An InAppComponent.Columns object representing the parsed columns.
     * @throws PayloadParsingException If the payload is null, or if mandatory fields in JSON Object
     *   are missing
     * @throws IllegalArgumentException If an invalid value is provided for "align"
     */
    @VisibleForTesting
    fun parseColumns(payload: JSONObject?, format: Format): InAppComponent.Columns {
        if (payload == null) throw PayloadParsingException("Columns cannot be null")
        return InAppComponent.Columns(
            parseColumnRatio(payload.optJSONArray("ratios")),
            payload.optInt("spacing"),
            parseMargin(payload.optJSONArray("margin")),
            VerticalAlignment.valueOf(
                payload.reallyOptString("contentAlign", "center").uppercase()
            ),
            parseColumnsChildren(payload.optJSONArray("children"), format),
        )
    }

    /**
     * Parses a JSON payload to create an InAppComponent.Spacer object.
     *
     * @param payload The JSON object containing the spacer's configuration.
     * @return An InAppComponent.Spacer object representing the parsed spacer.
     * @throws PayloadParsingException If the payload is null, or if mandatory fields in JSON Object
     *   are missing
     */
    @VisibleForTesting
    fun parseSpacer(payload: JSONObject?, format: Format): InAppComponent.Spacer {
        if (payload == null) throw PayloadParsingException("Columns cannot be null")
        val size = Size(payload.getString("height"))
        if (format == Format.MODAL && size.isFill()) {
            throw PayloadParsingException("Spacer cannot have fill value in a Modal format")
        }
        return InAppComponent.Spacer(size)
    }

    /**
     * Parses a JSON payload to create an InAppComponent.WebView object.
     *
     * @param payload The JSON object containing the webview's configuration.
     * @return An InAppComponent.WebView object representing the parsed webview.
     * @throws PayloadParsingException If the payload is null, or if mandatory fields in JSON Object
     *   are missing
     */
    @VisibleForTesting
    fun parseWebView(payload: JSONObject?): InAppComponent.WebView {
        if (payload == null) throw PayloadParsingException("WebView cannot be null")
        return InAppComponent.WebView(
            payload.getString("id"),
            payload.reallyOptInteger("timeout", DEFAULT_WEBVIEW_TIMEOUT),
            payload.optBoolean("inAppDeeplinks", true),
            payload.optBoolean("devMode", false),
        )
    }

    /**
     * Parses a JSON payload representing the CloseOptions for an In-App message.
     *
     * @property payload Optional close options json value.
     * @return The CloseOptions object.
     */
    @VisibleForTesting
    fun parseCloseOptions(payload: JSONObject?): CloseOptions? {
        if (payload == null) return null

        val auto =
            payload.optJSONObject("auto").let {
                if (it == null) {
                    null
                } else {
                    CloseOptions.Auto(it.getInt("delay"), parseColor(it.getJSONArray("color")))
                }
            }

        val button =
            payload.optJSONObject("button").let {
                if (it == null) {
                    null
                } else {
                    CloseOptions.Button(
                        parseColor(it.optJSONArray("color")),
                        parseColor(
                            it.optJSONArray("backgroundColor") ?: JSONArray().put("#00000000")
                        ),
                    )
                }
            }
        return CloseOptions(auto, button)
    }

    /**
     * Parses a JSON payload representing the actions for an In-App message.
     *
     * @property payload Optional actions json value.
     * @return A Map of action names to Action objects.
     * @throws JSONException If there is any error while accessing the json object
     */
    @VisibleForTesting
    fun parseActions(payload: JSONObject?): Map<String, Action> {
        val result = mutableMapOf<String, Action>()

        if (payload == null) return result

        for (key in payload.keys()) {
            payload.getJSONObject(key).let {
                result[key] = Action(it.getString("action"), it.optJSONObject("params"))
            }
        }
        return result.toMap()
    }

    /**
     * Parses a JSON payload representing a Border object.
     *
     * @param payload The JSON object containing the border properties.
     * @return An [Border] object representing the parsed border.
     */
    @VisibleForTesting
    fun parseBorder(payload: JSONObject?): Border? {
        if (payload == null || (!payload.has("borderWidth") && !payload.has("borderColor"))) {
            return null
        }
        return Border(
            payload.reallyOptInteger("borderWidth", DEFAULT_BORDER_WIDTH),
            parseColor(payload.optJSONArray("borderColor")),
        )
    }

    /**
     * Parses a JSON payload representing a list of font decorations.
     *
     * @param payload The JSON array containing the font decorations.
     * @return A list of [FontDecoration] objects.
     */
    @VisibleForTesting
    fun parseFontDecoration(payload: JSONArray?): Set<FontDecoration> {
        val result = mutableSetOf<FontDecoration>()
        if (payload == null) return setOf()
        for (i in 0 until payload.length()) {
            result.add(FontDecoration.valueOf(payload.getString(i).uppercase()))
        }
        return result.toSet()
    }

    /**
     * Parses a JSON payload representing a LightDarkColorTuple.
     *
     * @param value The JSON array containing the color values.
     * @return A [ThemeColors] object representing the parsed color.
     */
    @VisibleForTesting
    fun parseColor(value: JSONArray?): ThemeColors {
        if (value == null) {
            return ThemeColors(DEFAULT_LIGHT_COLOR, DEFAULT_DARK_COLOR)
        }
        return when (value.length()) {
            0 -> ThemeColors(DEFAULT_LIGHT_COLOR, DEFAULT_DARK_COLOR)
            1 -> ThemeColors(value.getString(0), value.getString(0))
            else -> ThemeColors(value.getString(0), value.getString(1))
        }
    }

    /**
     * Parses a JSON payload representing a Margin array.
     *
     * @param value The JSON array containing the integer values.
     * @return Margin object.
     */
    @VisibleForTesting
    fun parseMargin(value: JSONArray?): Margin {
        return if (value == null) {
            Margin(DEFAULT_MARGIN)
        } else {
            when (value.length()) {
                0 -> Margin(DEFAULT_MARGIN)
                1 -> Margin(value.getInt(0))
                2 -> Margin(value.getInt(0), value.getInt(1))
                3 -> throw PayloadParsingException("Cannot parse margin array with 3 values")
                4 -> Margin(value.getInt(0), value.getInt(1), value.getInt(2), value.getInt(3))

                else ->
                    throw PayloadParsingException(
                        "Cannot parse margin array with more than 4 values"
                    )
            }
        }
    }

    /**
     * Parses a JSON payload representing a radius array.
     *
     * @param value The JSON array containing the integer values.
     * @return A CornerRadius object.
     */
    @VisibleForTesting
    fun parseRadius(value: JSONArray?): CornerRadius {
        return if (value == null) {
            CornerRadius(DEFAULT_RADIUS)
        } else {
            when (value.length()) {
                0 -> CornerRadius(DEFAULT_RADIUS)
                1 -> CornerRadius(value.getInt(0))
                2 -> CornerRadius(value.getInt(0), value.getInt(1))
                3 -> throw PayloadParsingException("Cannot parse radius array with 3 values")
                4 ->
                    CornerRadius(value.getInt(0), value.getInt(1), value.getInt(2), value.getInt(3))

                else ->
                    throw PayloadParsingException(
                        "Cannot parse radius array with more than 4 values"
                    )
            }
        }
    }

    /**
     * Parses a JSON payload representing a list of column ratios.
     *
     * @param value The JSON array containing the column ratios.
     * @return A float array representing the column ratios.
     * @throws PayloadParsingException If the value is null.
     */
    @VisibleForTesting
    fun parseColumnRatio(value: JSONArray?): FloatArray {
        if (value == null) throw PayloadParsingException("Columns ratio cannot be null")
        val result = FloatArray(value.length())
        for (i in 0 until value.length()) {
            result[i] = value.getDouble(i).toFloat()
        }
        // Ensure the sum of ratios is 100 or 1
        if (!listOf(1F, 100F).contains(result.sum())) {
            throw PayloadParsingException("Sum of columns ratio must be 1 or 100")
        }
        return result
    }

    /**
     * Parses a JSON payload representing a map of strings.
     *
     * @param payload The JSON object containing the map.
     * @return A map of strings representing the parsed key-value pairs.
     */
    @VisibleForTesting
    fun parseMap(payload: JSONObject?): Map<String, String> {
        val result = mutableMapOf<String, String>()

        if (payload == null) return mapOf()

        for (key in payload.keys()) {
            payload.getString(key).let { result[key] = it }
        }
        return result
    }
}
