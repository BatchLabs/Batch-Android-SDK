package com.batch.android.messaging;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import com.batch.android.core.Logger;
import com.batch.android.core.Parameters;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.css.CSSParsingException;
import com.batch.android.messaging.css.Parser;
import com.batch.android.messaging.css.builtin.BuiltinStyleProvider;
import com.batch.android.messaging.model.Action;
import com.batch.android.messaging.model.AlertMessage;
import com.batch.android.messaging.model.BannerMessage;
import com.batch.android.messaging.model.BaseBannerMessage;
import com.batch.android.messaging.model.BaseBannerMessage.CTADirection;
import com.batch.android.messaging.model.CTA;
import com.batch.android.messaging.model.ImageMessage;
import com.batch.android.messaging.model.Message;
import com.batch.android.messaging.model.ModalMessage;
import com.batch.android.messaging.model.UniversalMessage;
import com.batch.android.messaging.model.WebViewMessage;
import com.batch.android.module.MessagingModule;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * Parses the messaging message contained into a payload, either from in-app or landings.
 *
 */
public class PayloadParser {

    private static final String TAG = "PayloadParser";

    public static Message parsePayload(JSONObject payload) throws PayloadParsingException {
        if (payload == null) {
            throw new PayloadParsingException("The payload cannot be null");
        }

        Message resultMessage = null;

        try {
            resultMessage = parseBasePayload(payload);
        } catch (JSONException e) {
            throw new PayloadParsingException("Error while decoding the JSON payload (code 2)", e);
        }

        if (resultMessage instanceof AlertMessage) {
            try {
                resultMessage = parseAlertPayload(payload, (AlertMessage) resultMessage);
            } catch (JSONException e) {
                throw new PayloadParsingException("Error while decoding the JSON payload (code 3)", e);
            }
        } else if (resultMessage instanceof UniversalMessage) {
            try {
                resultMessage = parseUniversalPayload(payload, (UniversalMessage) resultMessage);
            } catch (JSONException e) {
                throw new PayloadParsingException("Error while decoding the JSON payload (code 4)", e);
            }
        } else if (resultMessage instanceof BannerMessage) {
            try {
                resultMessage = parseBannerPayload(payload, (BannerMessage) resultMessage);
            } catch (JSONException e) {
                throw new PayloadParsingException("Error while decoding the JSON payload (code 6)", e);
            }
        } else if (resultMessage instanceof ModalMessage) {
            try {
                resultMessage = parseModalPayload(payload, (ModalMessage) resultMessage);
            } catch (JSONException e) {
                throw new PayloadParsingException("Error while decoding the JSON payload (code 7)", e);
            }
        } else if (resultMessage instanceof ImageMessage) {
            try {
                resultMessage = parseImagePayload(payload, (ImageMessage) resultMessage);
            } catch (JSONException e) {
                throw new PayloadParsingException("Error while decoding the JSON payload (code 8)", e);
            }
        } else if (resultMessage instanceof WebViewMessage) {
            try {
                resultMessage = parseWebViewPayload(payload, (WebViewMessage) resultMessage);
            } catch (JSONException e) {
                throw new PayloadParsingException("Error while decoding the JSON payload (code 9)", e);
            }
        }

        return resultMessage;
    }

    public static Message parseBasePayload(JSONObject payload) throws PayloadParsingException, JSONException {
        final String kind = payload.getString("kind");

        Message message;

        if ("alert".equalsIgnoreCase(kind)) {
            message = new AlertMessage();
        } else if ("universal".equalsIgnoreCase(kind)) {
            message = new UniversalMessage();
        } else if ("banner".equalsIgnoreCase(kind)) {
            message = new BannerMessage();
        } else if ("modal".equalsIgnoreCase(kind)) {
            message = new ModalMessage();
        } else if ("image".equalsIgnoreCase(kind)) {
            message = new ImageMessage();
        } else if ("webview".equalsIgnoreCase(kind)) {
            message = new WebViewMessage();
        } else {
            throw new PayloadParsingException("Unknown message kind");
        }

        Integer minimumMessagingAPIVersion = payload.reallyOptInteger("minapi", null);
        if (
            minimumMessagingAPIVersion != null &&
            minimumMessagingAPIVersion > 0 &&
            minimumMessagingAPIVersion > Parameters.MESSAGING_API_LEVEL
        ) {
            Logger.error(MessagingModule.TAG, "This SDK is too old to display this message. Please update it.");
            throw new PayloadParsingException("SDK too old");
        }

        Integer minimumAndroidAPIVersion = payload.reallyOptInteger("min_android_sdk", null);
        if (
            minimumAndroidAPIVersion != null &&
            minimumAndroidAPIVersion > 0 &&
            minimumAndroidAPIVersion > Build.VERSION.SDK_INT
        ) {
            Logger.error(MessagingModule.TAG, "This device is too old to display this message. Please update it.");
            throw new PayloadParsingException("Device too old");
        }

        message.messageIdentifier = payload.getString("id");
        message.devTrackingIdentifier = payload.reallyOptString("did", null);
        message.bodyText = payload.reallyOptString("body", null);
        message.bodyRawHtml = payload.reallyOptString("body_html", null);
        message.eventData = payload.optJSONObject("ed");

        return message;
    }

    private static AlertMessage parseAlertPayload(JSONObject payload, AlertMessage message)
        throws PayloadParsingException, JSONException {
        message.titleText = payload.reallyOptString("title", null);
        message.cancelButtonText = payload.getString("cancelLabel");

        if (TextUtils.isEmpty(message.titleText) && TextUtils.isEmpty(message.bodyText)) {
            throw new PayloadParsingException("Alert payload requires at least a title or a body");
        }

        JSONObject ctaPayload = payload.optJSONObject("cta");
        if (ctaPayload != null) {
            CTA acceptCTA = parseCTA(ctaPayload);
            //noinspection ConstantConditions
            if (acceptCTA != null) {
                message.acceptCTA = acceptCTA;
            }
        }

        return message;
    }

    private static UniversalMessage parseUniversalPayload(JSONObject payload, UniversalMessage message)
        throws PayloadParsingException, JSONException {
        message.headingText = payload.reallyOptString("h1", null);
        message.titleText = payload.reallyOptString("h2", null);
        message.subtitleText = payload.reallyOptString("h3", null);
        message.heroImageURL = payload.reallyOptString("hero", null);
        message.videoURL = payload.reallyOptString("video", null);
        message.heroDescription = payload.reallyOptString("hdesc", null);
        message.css = payload.getString("style");
        message.attachCTAsBottom = payload.reallyOptBoolean("attach_cta_bottom", null);
        message.stackCTAsHorizontally = payload.reallyOptBoolean("stack_cta_h", null);
        message.stretchCTAsHorizontally = payload.reallyOptBoolean("stretch_cta_h", null);
        message.flipHeroVertical = payload.reallyOptBoolean("flip_hero_v", null);
        message.flipHeroHorizontal = payload.reallyOptBoolean("flip_hero_h", null);
        message.showCloseButton = payload.reallyOptBoolean("close", null);
        message.heroSplitRatio = payload.reallyOptDouble("hero_split_ratio", null);
        message.autoCloseDelay = payload.reallyOptInteger("auto_close", 0);

        if (message.heroSplitRatio != null && (message.heroSplitRatio <= 0 || message.heroSplitRatio >= 1)) {
            Logger.internal(TAG, "Hero split ratio is <= 0 or >= 1. Ignoring.");
            message.heroSplitRatio = null;
        }

        final JSONArray jsonCTAs = payload.optJSONArray("cta");
        if (jsonCTAs != null) {
            for (int i = 0; i < jsonCTAs.length(); i++) {
                final JSONObject jsonCTA = jsonCTAs.optJSONObject(i);
                if (jsonCTA == null) {
                    continue;
                }
                message.ctas.add(parseCTA(jsonCTA));
            }
        }

        // Try to parse the css, to see if there's an error
        try {
            if (new Parser(new BuiltinStyleProvider(), message.css).parse() == null) {
                throw new PayloadParsingException("Style parsing exception (-23)");
            }
        } catch (CSSParsingException e) {
            Logger.internal(TAG, "Parsing exception", e);
            throw new PayloadParsingException("Style parsing exception (-24)");
        }

        return message;
    }

    private static BannerMessage parseBannerPayload(JSONObject payload, BannerMessage message)
        throws PayloadParsingException, JSONException {
        parseBaseBannerPayload(payload, message);
        return message;
    }

    private static ModalMessage parseModalPayload(JSONObject payload, ModalMessage message)
        throws PayloadParsingException, JSONException {
        parseBaseBannerPayload(payload, message);
        return message;
    }

    private static void parseBaseBannerPayload(JSONObject payload, BaseBannerMessage message)
        throws PayloadParsingException, JSONException {
        message.css = payload.getString("style");
        message.titleText = payload.reallyOptString("title", null);
        message.globalTapDelay = payload.reallyOptInteger("global_tap_delay", 0);
        message.allowSwipeToDismiss = payload.reallyOptBoolean("swipe_dismiss", true);
        message.imageURL = payload.reallyOptString("image", null);
        message.imageDescription = payload.reallyOptString("image_description", null);
        message.showCloseButton = payload.reallyOptBoolean("close", true);
        message.autoCloseDelay = payload.reallyOptInteger("auto_close", 0);

        final JSONArray jsonCTAs = payload.optJSONArray("cta");
        if (jsonCTAs != null) {
            for (int i = 0; i < jsonCTAs.length(); i++) {
                final JSONObject jsonCTA = jsonCTAs.optJSONObject(i);
                if (jsonCTA == null) {
                    continue;
                }
                message.ctas.add(parseCTA(jsonCTA));
            }
        }

        final JSONObject jsonGlobalTapAction = payload.optJSONObject("action");
        if (jsonGlobalTapAction != null) {
            message.globalTapAction = parseAction(jsonGlobalTapAction);
        }

        String ctaDirection = payload.reallyOptString("cta_direction", null);
        if (ctaDirection != null) {
            ctaDirection = ctaDirection.toLowerCase(Locale.US);
            switch (ctaDirection) {
                case "h":
                    message.ctaDirection = CTADirection.HORIZONTAL;
                    break;
                case "v":
                    message.ctaDirection = CTADirection.VERTICAL;
                    break;
                default:
                    Logger.internal(
                        TAG,
                        "Parsing error: base banner: \"cta_direction\" is neither 'h' or 'v': ignoring"
                    );
            }
        }

        // Try to parse the css, to see if there's an error
        try {
            if (new Parser(new BuiltinStyleProvider(), message.css).parse() == null) {
                throw new PayloadParsingException("Style parsing exception (-23)");
            }
        } catch (CSSParsingException e) {
            Logger.internal(TAG, "Parsing exception", e);
            throw new PayloadParsingException("Style parsing exception (-24)");
        }
    }

    private static ImageMessage parseImagePayload(JSONObject payload, ImageMessage message)
        throws PayloadParsingException, JSONException {
        message.isFullscreen = payload.optBoolean("fullscreen", true);
        message.allowSwipeToDismiss = payload.reallyOptBoolean("swipe_dismiss", true);
        message.imageURL = payload.getString("image");
        message.imageDescription = payload.optString("image_description", null);

        int width = payload.optInt("width", 0);
        int height = payload.optInt("height", 0);
        if (width < 0 || height < 0) {
            throw new PayloadParsingException("Image: invalid width or height");
        }

        // If we don't have a valid size, fallback on 3:2
        if (width == 0 || height == 0) {
            message.imageSize = new Size2D(2, 3);
        } else {
            message.imageSize = new Size2D(width, height);
        }

        message.css = payload.getString("style");
        // Try to parse the css, to see if there's an error
        try {
            if (new Parser(new BuiltinStyleProvider(), message.css).parse() == null) {
                throw new PayloadParsingException("Style parsing exception (-23)");
            }
        } catch (CSSParsingException e) {
            Logger.internal(TAG, "Parsing exception", e);
            throw new PayloadParsingException("Style parsing exception (-24)");
        }

        message.autoCloseDelay = payload.reallyOptInteger("auto_close", 0);
        message.globalTapDelay = payload.reallyOptInteger("global_tap_delay", 0);
        message.globalTapAction = parseAction(payload.getJSONObject("action"));

        return message;
    }

    private static WebViewMessage parseWebViewPayload(JSONObject payload, WebViewMessage message)
        throws PayloadParsingException, JSONException {
        message.url = payload.getString("url");
        try {
            URL url = new URL(message.url);
            String scheme = url.getProtocol().toLowerCase(Locale.US);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new PayloadParsingException("URL isn't 'http' or 'https' (-31)");
            }
        } catch (MalformedURLException e) {
            Logger.internal(TAG, "Parsing exception", e);
            throw new PayloadParsingException("Could not parse URL (-30)");
        }

        message.devMode = payload.reallyOptBoolean("dev", false);
        message.openDeeplinksInApp = payload.reallyOptBoolean("inAppDeeplinks", false);

        message.timeout = payload.reallyOptInteger("timeout", 0);
        message.css = payload.getString("style");
        // Try to parse the css, to see if there's an error
        try {
            if (new Parser(new BuiltinStyleProvider(), message.css).parse() == null) {
                throw new PayloadParsingException("Style parsing exception (-23)");
            }
        } catch (CSSParsingException e) {
            Logger.internal(TAG, "Parsing exception", e);
            throw new PayloadParsingException("Style parsing exception (-24)");
        }

        return message;
    }

    private static Action parseAction(JSONObject actionJSON) throws PayloadParsingException, JSONException {
        final String actionString = actionJSON.reallyOptString("a", null);
        JSONObject args = actionJSON.optJSONObject("args");

        if (args == null) {
            args = new JSONObject();
        }

        return new Action(actionString, args);
    }

    private static CTA parseCTA(JSONObject ctaJSON) throws PayloadParsingException, JSONException {
        final String label = ctaJSON.getString("l");
        final String actionString = ctaJSON.reallyOptString("a", null);
        JSONObject args = ctaJSON.optJSONObject("args");

        if (args == null) {
            args = new JSONObject();
        }

        return new CTA(label, actionString, args);
    }

    private static Spanned parseHtmlString(String rawHtml) {
        if (rawHtml == null) {
            return null;
        }

        Spanned output;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            output = Html.fromHtml(rawHtml, 0);
        } else {
            output = Html.fromHtml(rawHtml);
        }

        if (output == null || output.length() == 0) {
            return null;
        }

        return output;
    }
}
