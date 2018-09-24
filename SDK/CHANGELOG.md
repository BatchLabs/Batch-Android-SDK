CHANGELOG
=========

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
