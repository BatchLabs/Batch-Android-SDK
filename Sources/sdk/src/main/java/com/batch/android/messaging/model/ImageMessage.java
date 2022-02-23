package com.batch.android.messaging.model;

import com.batch.android.messaging.Size2D;
import java.io.Serializable;

public class ImageMessage extends Message implements Serializable {

    public String css;
    public Action globalTapAction;
    public long globalTapDelay; // ms
    public boolean allowSwipeToDismiss;
    public String imageURL;
    public String imageDescription;
    public Size2D imageSize;
    public int autoCloseDelay; // ms
    public boolean isFullscreen;
}
