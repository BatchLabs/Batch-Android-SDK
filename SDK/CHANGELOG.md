CHANGELOG
=========

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
