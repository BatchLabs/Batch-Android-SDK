package com.batch.android;

import com.batch.android.annotation.PublicSDK;
import java.util.EnumSet;

/**
 * Enum that define how notifications should be display
 *
 */
@PublicSDK
public enum PushNotificationType {
    /**
     * Display no notification at all
     */
    NONE(0),

    /**
     * Add sound to the notification
     */
    SOUND(1 << 0),

    /**
     * Add vibration to the notification
     */
    VIBRATE(1 << 1),

    /**
     * Add lights to the notification (if available on the device)
     */
    LIGHTS(1 << 2),

    /**
     * Display a notification
     */
    ALERT(1 << 3);

    // ------------------------------------->

    /**
     * Integer value
     */
    private int value;

    /**
     * @param value
     */
    PushNotificationType(int value) {
        this.value = value;
    }

    // ------------------------------------->

    public static EnumSet<PushNotificationType> fromValue(int value) {
        EnumSet<PushNotificationType> types = EnumSet.noneOf(PushNotificationType.class);

        for (PushNotificationType type : values()) {
            if (type != NONE && (value & type.value) == type.value) {
                types.add(type);
            }
        }

        if (types.isEmpty()) {
            types.add(PushNotificationType.NONE);
        }

        return types;
    }

    public static int toValue(EnumSet<PushNotificationType> types) {
        int val = 0;

        for (PushNotificationType type : types) {
            val |= type.value;
        }

        return val;
    }
}
