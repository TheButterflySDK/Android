# The Butterfly SDK for Android
[![License: MIT](https://img.shields.io/badge/License-Apache-yellow.svg)](https://github.com/TheButterflySDK/Android/blob/main/LICENSE)
[![](https://jitpack.io/v/TheButterflySDK/Android.svg)](https://jitpack.io/#TheButterflySDK/Android)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://github.com/TheButterflySDK/Android)

[The Butterfly SDK](https://github.com/TheButterflySDK/About/blob/main/README.md) helps your app to take an active part in the fight against domestic violent.

## Installation
### 🔌 & ▶️

### Install via gradle

#### Using "jitpack.io"

In your root-level "build.gradle" file, put:
```
    allprojects {
        repositories {
            ...
            maven { url "https://jitpack.io" }
        }
   }
```

In your app-level "build.gradle" file, put:
```
   dependencies {
      implementation 'com.github.TheButterflySDK:Android:0.9.6'
   }
```

## Usage

To recognize your app in TheButterflySDK servers you'll need an application key. You can set it via code, as demonstrated here.
In order to present our reporter, we'll need the current Activity.

#### Example in Java (pretty much like Kotlin 🤓)

```Java
// Whenever you wish to open our screen, simply call:
ButterflySdk.openReporter(activity, "YOUR_API_KEY");
```
