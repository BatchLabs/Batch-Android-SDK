package com.batch.android.messaging.model.mep;

import com.batch.android.messaging.Size2D;
import com.batch.android.messaging.model.Action;
import java.io.Serializable;

public class ImageMessage extends MEPMessage implements Serializable {

    public String css;
    public Action globalTapAction;
    public long globalTapDelay; // ms
    public boolean allowSwipeToDismiss;
    public String imageURL;
    public String imageDescription;
    public Size2D imageSize;
    public int autoCloseDelay; // ms
    public boolean isFullscreen;

    public ImageMessage(String messageIdentifier) {
        super(messageIdentifier);
    }
}
