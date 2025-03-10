# The Butterfly SDK for Android
[![License: MIT](https://img.shields.io/badge/License-Apache-yellow.svg)](https://github.com/TheButterflySDK/Android/blob/main/LICENSE)
[![](https://jitpack.io/v/TheButterflySDK/Android.svg)](https://jitpack.io/#TheButterflySDK/Android)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://github.com/TheButterflySDK/Android)

[The Butterfly SDK](https://github.com/TheButterflyButton/About/blob/main/README.md) helps your app to take an active part in the fight against domestic violence.

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
      implementation 'com.github.TheButterflySDK:Android:2.1.1'
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

## Integration tests
#### How?
You can easily verify your application key 🔑 by simply running the SDK in **DEBUG mode**.

This will cause our servers to skip the part of sending reports to real live support centers, they will only verify the API key. Eventually you'll get success / failure result.


### Enjoy and good luck ❤️
