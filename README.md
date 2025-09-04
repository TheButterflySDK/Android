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
      implementation 'com.github.TheButterflySDK:Android:2.2.2'
   }
```

## Usage (APIs)

To recognize your app in TheButterflySDK servers you'll need an application key. You can set it via code, as demonstrated here.
In order to present our reporter, we'll need the current Activity.

### Open The Butterfly
Whenever you wish to open our screen, simply call:

#### Java
```java
ButterflySdk.open(activity, "YOUR_API_KEY");
```

#### Kotlin 🤓
```kotlin
ButterflySdk.open(activity, "YOUR_API_KEY")
```

### Forward the deep link to the SDK for handling

#### Kotlin 🤓
```kotlin
@Override
protected void onCreate(Bundle savedInstanceState) {
    // ...
    ButterflySdk.handleDeepLink(activity, /* linkUriRepresentation | linkStringRepresentation */, "YOUR_API_KEY");
    // OR:
    ButterflySdk.handleIncomingIntent(activity, intent, "YOUR_API_KEY");
    // ...
}

@Override
protected void onNewIntent(Intent intent) {
    // ...
    ButterflySdk.handleDeepLink(activity, /* linkUriRepresentation | linkStringRepresentation */, "YOUR_API_KEY");
    // OR:
    ButterflySdk.handleIncomingIntent(activity, intent, "YOUR_API_KEY");
    // ...
}
```

## Integration tests
### How?
You can easily verify your API key 🔑 by simply running the SDK in **DEBUG mode** and start a chat with Betty 💬.

### Enjoy and good luck ❤️
