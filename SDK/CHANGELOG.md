CHANGELOG
=========

1.15.1
----

**Core**
* Tweaked how the log levels work to be more like how logcat works. The default logger level is now VERBOSE and INTERNAL implies all other log levels.
  Breaking: LoggerLevel.isGreaterOrEqual has been renamed to LoggerLevel.canLog
**Push**
* The "Registration ID/Push Token" log has been accidentally set as internal in 1.15.0. It is now public again.

1.15.0
----  

**BREAKING**
* Migrated to AndroidX. Batch now **explicitely** depends on androidx core. The SDK now explicitly depends on `androidx.core:core:1.0.0`. Please upgrade your version of the support library accordingly.
* BatchNotificationAction.getSupportActions no longer throws exception com.batch.android.MissingDependencyException, as androidx.core is now an explicit dependency.

**Core**

* Fix an exception that occurred when the `<application>` entry in the manifest had no `<meta-data>` children.
* Improved In-App view tracker error handling.
* Added support for external analytics using `EventDispatcher`. See the documentation for more details.
* Added a new feature for developers: a debug view is now available using `Batch.Debug.startDebugActivity`.
* Remove obfuscation on `BatchOptOutResultListener.ErrorPolicy` enum.
* Added the ability to change the logger level using `Config`.

**Actions**

* Added an interface `BatchDeeplinkInterceptor` to override some aspects of a deeplink that Batch wants to open.

**Messaging**

* Improved accessibility of all message formats
* Fixed an issue where an Image/Modal format set to auto close would crash if the user moved to another app before the countdown was elapsed.

**Push**

* System notification authorization is now reported to Batch's servers.  
  This includes channels support: the default channel override is used to measure the status if set, otherwise Batch's default channel is used.
* `Batch.Push.setNotificationsColor()` now explicitly takes a ColorInt
* `Batch.Push.setSmallIconResourceId()` now explicitly takes a DrawableRes
* The notification default color will now be the value of your theme's `colorPrimary` attribute rather than black.
* If no small icon has been set when configuring Batch, the SDK will now fallback on Firebase's `com.google.firebase.messaging.default_notification_icon`.  

**Inbox**

* Added the `markAsDeleted` method on `BatchInboxFetcher`, allowing the deletion of notifications

**User**

* Saving a UserDataEditor before Batch is started now enqueues the changes instead of failing. If Batch is not started before the process dies, changes will be lost. This change does not affect BatchUserProfile, which has been deprecated for a while.


1.14.4
----

**Messaging**

* Fix a crash that could occur on resume if the application process was killed after backgrounding the app on a messaging activity.

1.14.3
----

**Messaging**

* Fix a crash that could occur when the activity backing the "Image" format is stopped.

1.14.2
----

**In-App**

* Fix grace periods greater than 119 hours not working

**User**

* Fixed wrong visiblity and obfuscation of methods in `BatchUserAttribute`
* Added @NonNull annotations to `BatchAttributesFetchListener` and `BatchTagCollectionsFetchListener`

1.14.1
----

- Switched back to Proguard from R8. This should fix an issue with Firebase Performance.
- Notifications using BigPictureStyle will now show the big picture as the large icon when collapsed, if no other large icon was set.
  Note: this feature requires androidx.core
- All SDK XML resources have been marked as private and will not show up in code completion anymore.

1.14.0
----

**BREAKING: Batch now requires Java 1.8 source support.**  
You may need to enable desugaring to make Batch work with your project by adding the following in your `build.gradle`:
```
android {
  //...
  compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
  }
}
```

**NOTE:** This release is one of the last supporting the legacy Android support libraries. Please start migrating to AndroidX to ensure a smooth upgrade experience in the future.

**Core**

* Requires Android Studio 3.0 or higher with d8 enabled.
* Fix some potential bugs with BatchActions in some locales.
* Batch will not catch RuntimeExceptions occurring in implementations of BatchNotificationInterceptor: they will now crash your app as they should have.
* Batch.push.setGCMSenderId() has been deprecated: Please migrate to FCM as soon as possible. GCM and GCM-legacy support will be removed from the SDK in a later release.
* Synchronization of user attributes and tags is now more optimized, resulting in less network requests.
* Other various bugfixes.
* Included new license information in the release zips and the artifact metadata. If you integrate using maven central, licenses will be picked up by `com.google.android.gms.oss-licenses-plugin`.

**User**

* High level data (language/region/custom user id) can now be read back.
* User data (attributes and tags) can now be read back. [Documentation](https://batch.com/doc/android/custom-data/custom-attributes.html#_reading-attributes-and-tag-collections)


**Push**
* Notification content is now visible on the lockscreen by default, even when locked. Use a notification interceptor if you want to override the visibility when showing sensitive content.

**Messaging**
* Added support for two new UI formats: Modal, and Image. See the documentation for more information.
* Added support for GIFs in Mobile Landings and In-App messages.
* Added support for rich text.
* Added support for text scrolling in all formats. Banners will now have a maximum body height of ~160pt, and their text will scroll.

* Support for opening deeplinks from In-App Messaging and Mobile Landings into a Custom Tab
* Added new methods on Batch.Messaging.LifecycleListener allowing you to track more information such as close/autoclose and button presses. More info in the Mobile Landings documentation.
* Batch.Messaging.LifecycleListener methods are no longer called on each rotation  

1.13.0
----

**Core**

* SDK is now built with the Pie SDK, and targets API Level 28.

* Opting-out from the SDK now sends an event notifying the server of this. If a data wipe has been asked, the request will also be forwarded to the server.
  New methods have been introduced to be informed of the status of the request to update your UI accordingly, and possibly revert the opt-out if the network is unreachable.

**Push**

* Exposed the tag Batch uses to display notifications in the constant `Batch.NOTIFICATION_TAG`. You can use this to cancel notifications, or use other APIs that require finding notifications by ID and tag.

* Added `Batch.Messaging.setShowForegroundLandings()`. It allows you to disable Batch's default behaviour to forward push messages containing landings to the app if it is in the foreground. Call this method with `false` will result in notifications always being posted no matter what the application state is.

**Events**

Event data support has been overhauled. As a result:  

* Introduced `BatchEventData`. Use this class to attach attributes and tags to an event. See this class' documentation for more information about limits.
* `Batch.User.trackEvent(String name, Stringlabel, JSONObject data)` has been deprecated
 - Calls to this method will log deprecation warnings in the console
 - Legacy data (JSONObject) will be converted to `BatchEventData`. Same data format restrictions apply: Any key/value entry that can't be converted will be ignored, and logged. Tags are not supported
* Introduced `Batch.User.trackEvent(String name, Stringlabel, BatchEventData data)`, replacing the deprecated method.

More info in the [event documentation](/doc/android/custom-data/custom-events.html#_event-data).

1.12.1
----
* FCM registration errors are now more explicitly logged
* Fix an error when a "null" registration ID could be logged
* Add support for server-side Sender ID detection. The Sender ID has also been included in the registration broadcast introduced in 1.12.0

1.12.0
----
* Batch now fully supports Firebase Cloud Messaging. Note that existing Batch integrations will NOT automatically be migrated to FCM. You should follow the [migration steps](https://batch.com/doc/android/advanced/fcm-migration.html) before April 2019.

* Added methods to handle opting-out from Batch, and wiping user data.
  Please see `Batch.optOut()`, `Batch.optOutAndWipeData()` and `Batch.optIn()`.
  You can control whether Batch is opted out from by default using a manifest meta-data entry:  
  `<meta-data android:name="batch_opted_out_by_default" android:value="false" />`
  For compatibility reasons, Batch will be enabled by default.
  More info in our documentation.

* When a push registration identifier has successfully been fetched, Batch will now broadcast it as the action definied by `Batch.ACTION_REGISTRATION_IDENTIFIER_OBTAINED`. More info in the documentation.
* Banners now correctly track their display, and trigger a `onBatchMessageShown()` call on your `LifecycleListener` if one is set.
* Displaying a banner will dismiss any currently displayed banner
* Deprecated Config.setCanUseAndroidID: Android id is never collected anymore. 
* Deprecated Config.setCanUseInstanceID: users should migrate to FCM.
* Added `android.permission.INTERNET`, `android.permission.WAKE_LOCK` and `android.permission.VIBRATE` in Batch's manifest, meaning that your app will automatically require them with no additional manifest changes. This should not impact you as they're required by many libraries, including Google's, and have always been required for Batch to work. If you didn't have them already, this might trigger a permission request on pre Marshmallow devices: please look at the manifest merger output to see if this impacts you.
* Fixed some rare crashes that could happen on some Android emulators that broadcast malformed Google Play Services intents.
* Various javadoc fixes
    
Note: This version is the **last one** supporting Android Studio 2.x. Batch 1.13 will ONLY support Android Studio 3.x and later.

1.11.0
----
* Added support for Banners in In-App Messaging

  If your app uses the Mobile Landing/In-App Messaging manual mode, you need to update your implementation to support banners. Please check the [updated manual mode documentation](https://batch.com/doc/android/mobile-landings.html) for more information.

* Fix an issue where an exception could be thrown if an invalid deeplink was supplied when setting up an In-App Message's buttons.
* The SDK will now log the current Installation ID on start

1.10.2
----
* Unobfuscated InAppMessageUserActionSource, which is implemented by BatchInAppMessage
* Fixed an exception that could be thrown when using the Inbox APIs in the background. 
  While it will not throw an exception anymore, the fetcher will still not work in these conditions if used in 'user identifier' mode: In order to fix this, please use Batch.Inbox.getFetcher(Context, String, String) rather than Batch.Inbox.getFetcher(String, String).
* Fix a bug where the image of an In-App message could be distorted after a double rotation.
* Added a method `getNotificationIdentifier()` on BatchInboxNotificationContent, which allows you to retrieve a unique identifier for this specific notification.
* Added a variant of `Batch.Push.appendBatchData()` compatible with `BatchNotificationInterceptor` implementations
* Added `Batch.Push.makePendingIntent(Intent)`, to generate a valid PendingIntent from an Intent, suitable for a Notification's builder. Please note that it will override the intent's action. If you rely on a custom action, you will have to make your own PendingIntent.
* Added `Batch.Push.makePendingIntentForDeeplink(String)`, to generate a valid PendingIntent for a deeplink string, suitable for a Notification's builder, using Batch's action activity.
* Improve builtin proguard rules. The number of library classes kept because of Batch is now much lower.
* Added detection for invalid sender IDs, and overall better error reporting when FCM/GCM registration fails.

1.10.1
----
* Improved handling of new Android O backgrounding limitations
* Fixed Do Not Disturb mode not working properly
* Fixed a bug where activities opened by a push using a deeplink did not contain the Batch.Push.PAYLOAD_KEY extra
* Fixed an issue with Android Oreo channels
* Added APIs to copy Batch's data from an intent to another. See Batch.copyBatchExtras
* Added a method on BatchInAppMessage, allowing you to fetch the visual content of the message. See BatchInAppMessage.getContent for more info
* Added proguard rules to exclude support-v4 NotificationCompat related classes 

1.10.0
----
* Introduced In-App Campaigns
* Added a Do Not Disturb mode on Batch.Messaging, allowing easier control of when landings will be shown

1.9.2
----
* Batch is now available on Maven Central!
* Unofbuscated classes that have been wrongly obfuscated before. BatchNotificationAction is now available.
* Introducing BatchNotificationInterceptor, which allows you to change the generated notificationId or change the NotificationCompat.Builder instance that Batch uses
  to display notifications. This allows you to add features such as a Wear extender, etc.

1.9.1
----
* Batch SDK is now built with and targets API 26
* Added support for Android 8.0 Notification Channels
  See Batch.Push.getChannelsManager() and our channels documentation (coming soon) to tweak how Batch handles them.
* Batch requires support-v4 26.0.0, for notifications to work with Android 8.0. (As of writing, only 26.0.0-beta2 is available)
* Eclipse support had been dropped

1.9.0
----
* Added the Inbox module, allowing you to fetch previously received notifications from your code. More info: https://batch.com/doc/android/inbox.html 

1.8.0
----
* BREAKING CHANGE: Removed Batch.Ads and Batch.Unlock methods and related classes.
* Added Batch.User.trackLocation, allowing you to natively track user position updates
* Added global notification sound override using Batch.Push.setSound(Uri)
* Un-obfuscated the exception returned by some of BatchPushPayload's methods
* Deprecated Batch.isRunningInDevMode. It is useless, as DEV API Keys always start with "DEV"

1.7.4
----
* Fix Batch.Messaging.LifecycleListener being wrongly obfuscated by Proguard, making it harder to use than intended

1.7.3
----
* Fixed a bug that could cause notifications to open the wrong deeplink if multiple notifications were present in the notification shade

1.7.2
----
* Fixed a bug where events would stop working after rotation in some cases
* Fixed a bug where Batch wouldn't work properly with translucent/floating activities

1.7.1
-----
* Fixed a bug where mobile landings buttons could be misplaced

1.7.0
-----
* Introduced [Mobile Landings](https://batch.com/doc/android/mobile-landings.html)

1.6.0
-----
* Batch now requires Android 4.0.3 (API Level 15)
* Batch now targets Android 7 (API Level 24), and should only be used in apps that compile with that SDK version. You should also use a v24 support-v4 library with it.
* Updated notification behaviour to be more adapted to changes introduced in Nougat
* `setCanUseAdvancedDeviceInformation()` has been introduced on the `Config` object to reduce the quantity of device information Batch will use. Note that disabling this will limit several dashboard features.
* `BatchPushData` has been deprecated in favour of `BatchPushPayload`, which is easier to use and will allow you to read anything used by Batch's standard push receiver. It's also easier to instanciate from an intent/bundle, and easier to serialize.
* InstanceID support has been merged into the main SDK. If migrating from an older SDK, you'll need to add a new service in your manifest, as described in the [push setup](/doc/android/sdk-integration/push-setup.html).
* The overall method count has been reduced.

1.5.4
-----
* Fix a rare memory leak for the last activity
* Internal bugfixes

1.5.3
-----
* Added Batch.Push.getNotificationsType()
* Threads used by Batch are now named
* Batch.User.getEditor() has been renamed: Please use Batch.User.editor(). The old method will still work, but has been deprecated
* Additional intent flags can now be set for the activity started when opening a Push using Batch.Push.setAdditionalIntentFlags(int)
* Added "BatchActivityLifecycleHelper", an implementation of the "Application.ActivityLifecycleCallbacks" interface to use with "registerActivityLifecycleCallbacks()"  

1.5.2
-----
* Fix JSON issues on Android 4.3 and below

1.5.1
-----
* Fix a bug where clearTagCollection incorrectly cleared all attributes

1.5
----
* Custom user data (attributes, tags and events)
* Added an API to retrieve Batch's unique installation identifier
* Deprecated BatchUserProfile
* Added ability to start Batch in a service

1.4
-----
* Batch Ads has been discontinued
* Added a method to get the last known push token
* Minor push bugfixes

1.3.2
-----
* New manual push helper


1.3.1
-----
* Push enhancements
* Minor bug fixes


1.3
-----
* Native Ads


1.2.5
-----
* Bug fix


1.2.4
-----
* Optimisations


1.2.3
-----
* Bug fix


1.2
-----
* Add Ads


1.1
-----
* Add Push notifications support


1.0
-----

 * Batch release.
