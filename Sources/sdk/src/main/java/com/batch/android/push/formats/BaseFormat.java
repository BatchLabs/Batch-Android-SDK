package com.batch.android.push.formats;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class BaseFormat {

    protected String title;

    protected String body;

    /**
     * Large icon
     */
    protected Bitmap icon;

    /**
     * Large image. Called picture to make it more understandable
     */
    protected Bitmap picture;

    public BaseFormat(@NonNull String title, @NonNull String body, @Nullable Bitmap icon, @Nullable Bitmap picture) {
        this.title = title;
        this.body = body;
        this.icon = icon;
        this.picture = picture;
    }
}
