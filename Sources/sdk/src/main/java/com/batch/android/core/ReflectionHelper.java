package com.batch.android.core;

import android.view.View;
import androidx.core.app.NotificationCompat;
import java.lang.reflect.Method;

/**
 * Class grouping all of the reflection based checks that we are doing.
 * <p>
 * Using generic helpers is prohibited, as proguard does not detect the use, and does not rewrite the obfuscated names correctly.
 * <p>
 * That means that this class WILL have a lot of the same code by design.
 * Please do not refactor this out :)
 */
public class ReflectionHelper {

  //region AndroidX Library

  public static boolean isAndroidXFragmentPresent() {
    try {
      Class.forName("androidx.fragment.app.Fragment");
      return true;
    } catch (Throwable ex) {
      return false;
    }
  }

  public static boolean isAndroidXAppCompatActivityPresent() {
    try {
      Class.forName("androidx.appcompat.app.AppCompatActivity");
      return true;
    } catch (Throwable ex) {
      return false;
    }
  }

  public static boolean isInstanceOfCoordinatorLayout(Object o) {
    if (o == null) {
      return false;
    }
    try {
      Class coordinatorClass = Class.forName(
        "androidx.coordinatorlayout.widget.CoordinatorLayout"
      );
      return o.getClass().isAssignableFrom(coordinatorClass);
    } catch (Throwable ex) {
      return false;
    }
  }

  public static boolean optOutOfSmartReply(NotificationCompat.Builder builder) {
    try {
      Method method = builder
        .getClass()
        .getMethod("setAllowSystemGeneratedContextualActions", boolean.class);
      method.invoke(builder, false);
    } catch (Throwable ignored) {
      return false;
    }
    return true;
  }

  public static boolean optOutOfDarkMode(View view) {
    try {
      Method method = view
        .getClass()
        .getMethod("setForceDarkAllowed", boolean.class);
      method.invoke(view, false);
    } catch (Throwable ignored) {
      return false;
    }
    return true;
  }

  //endregion

  //region Google Play Services

  public static boolean isGMSGoogleCloudMessagingPresent() {
    try {
      Class.forName("com.google.android.gms.gcm.GoogleCloudMessaging");
      return true;
    } catch (Throwable ex) {
      return false;
    }
  }

  public static boolean isGMSInstanceIDPresent() {
    try {
      Class.forName("com.google.android.gms.iid.InstanceID");
      return true;
    } catch (Throwable ex) {
      return false;
    }
  }
  //endregion
}
