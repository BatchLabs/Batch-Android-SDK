package com.batch.android.messaging.view;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Saved state for views that implement a countdown animation
 */
public class AnimatedCountdownSavedState extends View.BaseSavedState {

    public boolean animating = false;
    public long animationEndDate = 0L;
    public long duration = 0;

    public AnimatedCountdownSavedState(Parcel source) {
        super(source);
        readParcel(source, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressWarnings("unused")
    public AnimatedCountdownSavedState(Parcel source, ClassLoader loader) {
        super(source, loader);
        readParcel(source, loader);
    }

    @SuppressWarnings("unused")
    public AnimatedCountdownSavedState(Parcelable superState) {
        super(superState);
    }

    private void readParcel(Parcel in, ClassLoader loader) {
        animating = in.readByte() != 0;
        animationEndDate = in.readLong();
        duration = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeByte((byte) (animating ? 1 : 0));
        out.writeLong(animationEndDate);
        out.writeLong(duration);
    }

    @NonNull
    @Override
    public String toString() {
        return (
            "AnimatedCountdownSavedState { animating: " +
            animating +
            ", animationEndDate: " +
            animationEndDate +
            ", duration: " +
            duration +
            "}"
        );
    }

    public static final Creator<AnimatedCountdownSavedState> CREATOR = new Creator<AnimatedCountdownSavedState>() {
        @Override
        public AnimatedCountdownSavedState createFromParcel(Parcel source) {
            return new AnimatedCountdownSavedState(source);
        }

        public AnimatedCountdownSavedState[] newArray(int size) {
            return new AnimatedCountdownSavedState[size];
        }
    };
}
