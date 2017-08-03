![Dashboard Items](https://raw.github.com/BatchLabs/android-sdk/master/readme_logo.png)

# Batch Sample Apps
The samples are minimal examples demonstrating proper integrations of the Batch SDK, including implementation of Batch Push functionality.

You will need an up-to-date SDK and Android Studio installation in order to properly use this sample.

## App

1. Clone or download the repository.

2. Open the sample project in Android Studio.

## Dashboard

### 1. Login to your [Batch.com](https://batch.com/) account or create a new one.

### 2. Add your new sample app
Add a new app on the [dashboard](https://dashboard.batch.com/app/new) using the manual option, as your sample app doesn't have an App Store or Play Store URL to autopopulate.

### 3. Retrieve the dev API key
Within your newly-created app, find the dev API key either here on step 1 of the wizard screen under `API KEY`, or in the settings menu under *API Keys*. 

Place the dev API key in your sample app's `BatchSampleApplication.java`, in the startWithAPIKey method call.

At this point, feel free to launch your app. 

### 4. Testing Push

To test the push functionality, add your GCM Sender ID in `BatchSampleApplication.java`. Your token will be logged in your device's logcat, which you can use in the dashboard's test push function. This can be found on the Message screen of the push campaign creation wizard.
You will also be able to change the notification settings from the main menu.

## Resources
* [Full Batch documentation](https://dashboard.batch.com/doc)
* [support@batch.com](support@batch.com)