package com.batch.android.messaging.css.builtin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.messaging.css.ImportFileProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Provides some builtin SDK styles
 */
public class BuiltinStyleProvider implements ImportFileProvider {

    private static Map<String, String[]> metaStyles = generateMetaStyles();

    @Override
    @Nullable
    public String getContent(@NonNull String importName) {
        importName = importName.toLowerCase(Locale.US);
        final String[] metaImportNames = metaStyles.get(importName);
        if (metaImportNames != null) {
            final StringBuilder fullStyle = new StringBuilder();
            for (String metaImport : metaImportNames) {
                final String metaImportContent = getContent(metaImport);
                if (metaImportContent != null) {
                    fullStyle.append(metaImportContent);
                }
            }

            if (fullStyle.length() > 0) {
                return fullStyle.toString();
            } else {
                return null;
            }
        }

        switch (importName) {
            case "generic1_h-cta":
                return BuiltinStyles.GENERIC1_H_CTA;
            case "generic1_v-cta":
                return BuiltinStyles.GENERIC1_V_CTA;
            case "generic1_base":
                return BuiltinStyles.GENERIC1_BASE;
            case "banner1":
                return BuiltinStyles.BANNER1;
            case "modal1":
                return BuiltinStyles.MODAL1;
            case "banner-icon":
            case "modal-icon":
                return BuiltinStyles.BANNER_ICON_ADDON;
            case "image1_base":
                return BuiltinStyles.IMAGE1_BASE;
            case "image1_detached":
                return BuiltinStyles.IMAGE1_DETACHED;
            case "image1_fullscreen":
                return BuiltinStyles.IMAGE1_FULLSCREEN;
            case "webview1":
                return BuiltinStyles.WEBVIEW1;
            default:
                return null;
        }
    }

    private static Map<String, String[]> generateMetaStyles() {
        final Map<String, String[]> retVal = new HashMap<>();
        retVal.put("generic1-h-cta", new String[] { "generic1_h-cta", "generic1_base" });
        retVal.put("generic1-v-cta", new String[] { "generic1_v-cta", "generic1_base" });
        retVal.put("image1-fullscreen", new String[] { "image1_fullscreen", "image1_base" });
        retVal.put("image1-detached", new String[] { "image1_detached", "image1_base" });
        return retVal;
    }
}
