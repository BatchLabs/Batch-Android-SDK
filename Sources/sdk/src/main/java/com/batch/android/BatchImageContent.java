package com.batch.android;

import android.graphics.Point;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.Size2D;
import com.batch.android.messaging.model.ImageMessage;

/**
 * Model for the content of an image-only message
 */
@PublicSDK
public class BatchImageContent implements BatchInAppMessage.Content {

    private Action globalTapAction;
    private long globalTapDelay; // ms
    private boolean allowSwipeToDismiss;
    private String imageURL;
    private String imageDescription;
    private Size2D imageSize;
    private int autoCloseDelay; // ms
    private boolean isFullscreen;

    BatchImageContent(@NonNull ImageMessage from) {
        globalTapDelay = from.globalTapDelay;
        allowSwipeToDismiss = from.allowSwipeToDismiss;
        imageURL = from.imageURL;
        imageDescription = from.imageDescription;
        imageSize = from.imageSize;
        autoCloseDelay = from.autoCloseDelay;
        isFullscreen = from.isFullscreen;

        if (from.globalTapAction != null) {
            globalTapAction = new BatchImageContent.Action(from.globalTapAction);
        }
    }

    @PublicSDK
    public static class Action {

        private String action;

        private JSONObject args;

        Action(@NonNull com.batch.android.messaging.model.Action from) {
            action = from.action;
            if (from.args != null) {
                try {
                    args = new JSONObject(from.args);
                } catch (JSONException e) {
                    args = new JSONObject();
                }
            }
        }

        @Nullable
        public String getAction() {
            return action;
        }

        @Nullable
        public JSONObject getArgs() {
            return args;
        }
    }

    public boolean isFullscreen() {
        return isFullscreen;
    }

    public int getAutoCloseDelay() {
        return autoCloseDelay;
    }

    public Point getImageSize() {
        if (imageSize == null) {
            return null;
        }
        return new Point(imageSize.width, imageSize.height);
    }

    public String getImageDescription() {
        return imageDescription;
    }

    public String getImageURL() {
        return imageURL;
    }

    public boolean isAllowSwipeToDismiss() {
        return allowSwipeToDismiss;
    }

    public long getGlobalTapDelay() {
        return globalTapDelay;
    }

    public Action getGlobalTapAction() {
        return globalTapAction;
    }
}
