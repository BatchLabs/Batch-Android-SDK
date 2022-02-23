package com.batch.android.messaging;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.Serializable;

/**
 * Size2D class similar to {@link android.graphics.Point} but implements serializable
 */
public class Size2D implements Parcelable, Serializable {

    public int width;
    public int height;

    public Size2D(int width, int height) {
        this.width = width;
        this.height = height;
    }

    protected Size2D(Parcel in) {
        width = in.readInt();
        height = in.readInt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Size2D point = (Size2D) o;
        return width == point.width && height == point.height;
    }

    @Override
    public int hashCode() {
        return 31 * width + height;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(width);
        dest.writeInt(height);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Size2D> CREATOR = new Creator<Size2D>() {
        @Override
        public Size2D createFromParcel(Parcel in) {
            return new Size2D(in);
        }

        @Override
        public Size2D[] newArray(int size) {
            return new Size2D[size];
        }
    };
}
