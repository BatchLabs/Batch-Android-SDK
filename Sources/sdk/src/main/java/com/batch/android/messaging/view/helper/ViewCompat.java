/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.batch.android.messaging.view.helper;

import static android.content.Context.ACCESSIBILITY_SERVICE;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Compat view methods
 */
public class ViewCompat {

    private static AtomicInteger sNextGeneratedId = null;

    static {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            sNextGeneratedId = new AtomicInteger(1);
        }
    }

    /**
     * Generate a value suitable for use in {@link View#setId(int)}.
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    public static int generateViewId() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return View.generateViewId();
        } else {
            for (;;) {
                final int result = sNextGeneratedId.get();
                // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
                int newValue = result + 1;
                if (newValue > 0x00FFFFFF) {
                    newValue = 1; // Roll over to 1, not 0.
                }
                if (sNextGeneratedId.compareAndSet(result, newValue)) {
                    return result;
                }
            }
        }
    }

    public static Point getScreenSize(Context context) {
        final Resources resources = context.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();

        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;

        // Flip the size if it's landscape
        if (widthPixels > heightPixels) {
            //noinspection SuspiciousNameCombination
            widthPixels = metrics.heightPixels;
            //noinspection SuspiciousNameCombination
            heightPixels = metrics.widthPixels;
        }

        return new Point(
            (int) StyleHelper.pixelsToDp(resources, (float) widthPixels),
            (int) StyleHelper.pixelsToDp(resources, (float) heightPixels)
        );
    }

    /**
     * Returns whether touch exploration is enbaled
     * <p>
     * That often means that Talkback is enabled, but more importantly it means that
     * the user is using some kind of touch assist. Meaning that they'll be slower to tap things.
     * <p>
     * Usually you'll want to use this method to disable things based on timers, like an
     * auto dismiss.
     */
    public static boolean isTouchExplorationEnabled(Context context) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
        if (am == null) {
            return false;
        }
        return am.isEnabled() && am.isTouchExplorationEnabled();
    }
}
