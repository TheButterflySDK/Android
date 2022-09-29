package com.butterfly.sdk.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.BATTERY_SERVICE;

import com.butterfly.sdk.BFContextProvider;

public class DeviceInfoFetcher {

  private static final int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
  private static final int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

  /** Substitute for missing values. */
  private static final String[] EMPTY_STRING_LIST = new String[] {};
  private static final String TAG = DeviceInfoFetcher.class.getSimpleName();

  /** Constructs DeviceInfo. The {@code contentResolver} must not be null. */

  static public Map<String, Object> getDeviceInfo() {
    Map<String, Object> info = new HashMap<>();
    info.put("board", Build.BOARD);
    info.put("bootloader", Build.BOOTLOADER);
    info.put("brand", Build.BRAND);
    info.put("device", Build.DEVICE);
    info.put("display", Build.DISPLAY);
    info.put("fingerprint", Build.FINGERPRINT);
    info.put("hardware", Build.HARDWARE);
    info.put("host", Build.HOST);
    info.put("id", Build.ID);
    info.put("manufacturer", Build.MANUFACTURER);
    info.put("model", Build.MODEL);
    info.put("product", Build.PRODUCT);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      info.put("supported32BitAbis", Arrays.asList(Build.SUPPORTED_32_BIT_ABIS));
      info.put("supported64BitAbis", Arrays.asList(Build.SUPPORTED_64_BIT_ABIS));
      info.put("supportedAbis", Arrays.asList(Build.SUPPORTED_ABIS));
    } else {
      // Unavailable in this OS version
      info.put("supported32BitAbis", Arrays.asList(EMPTY_STRING_LIST));
      info.put("supported64BitAbis", Arrays.asList(EMPTY_STRING_LIST));
      info.put("supportedAbis", Arrays.asList(EMPTY_STRING_LIST));
    }
    info.put("tags", Build.TAGS);
    info.put("type", Build.TYPE);
    info.put("isPhysicalDevice", !isEmulator());
    //info.put("deviceId", getAndroidId());

    info.put("screenSize", screenWidth + "x" + screenHeight);
    info.put("carrier", getCarrierName());

    int batteryLevel = getDeviceBatteryPercentageLevel();
    if (batteryLevel > -1) {
      info.put("batteryLevel", batteryLevel);
    } else {
      // Unavailable in this OS version
      info.put("batteryLevel", "unknown");
    }

    Map<String, Object> version = new HashMap<>();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      version.put("baseOS", Build.VERSION.BASE_OS);
      version.put("previewSdkInt", Build.VERSION.PREVIEW_SDK_INT);
      version.put("securityPatch", Build.VERSION.SECURITY_PATCH);
    }
    version.put("codename", Build.VERSION.CODENAME);
    version.put("incremental", Build.VERSION.INCREMENTAL);
    version.put("release", Build.VERSION.RELEASE);
    version.put("sdkInt", Build.VERSION.SDK_INT);
    info.put("version", version);

    SdkLogger.Companion.log(TAG, info);

    return info;
  }

  private static String getCarrierName() {
    Context applicationContext = BFContextProvider.getApplicationContext();
    if (applicationContext == null) return "<unknown>";

    TelephonyManager manager = (TelephonyManager) applicationContext.getSystemService(Context.TELEPHONY_SERVICE);
    String carrierName;

    if (manager != null) {
      carrierName = manager.getNetworkOperatorName();
    } else {
      carrierName = "unavailable";
    }

    return carrierName;
  }

  public static int getDeviceBatteryPercentageLevel() {
    int batteryLevel = -1;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      Context applicationContext = BFContextProvider.getApplicationContext();
      if (applicationContext != null) {
        BatteryManager batteryManager = (BatteryManager) applicationContext.getSystemService(BATTERY_SERVICE);
        if (batteryManager != null) {
          batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
      }
    } else {
      // Hmmmm.... What should we do in case we're running on lower than Lollipop?
      // There's an option to observe the battery status but I'd like to find a way only to query the battery level.
    }

    return batteryLevel;
  }

  /**
   * Returns the Android hardware device ID that is unique between the device + user and app
   * signing. This key will change if the app is uninstalled or its data is cleared. Device factory
   * reset will also result in a value change.
   *
   * On Android 8.0 (API level 26) and higher versions of the platform,
   * a 64-bit number (expressed as a hexadecimal string),
   * unique to each combination of: app-signing key, user, and device.
   * (From: https://developer.android.com/reference/android/provider/Settings.Secure#ANDROID_ID)
   *
   * @return The android ID
   */
  @SuppressLint("HardwareIds")
  static public String getAndroidId() {
    Context applicationContext = BFContextProvider.getApplicationContext();
    if (applicationContext != null) {
      ContentResolver contentResolver = applicationContext.getContentResolver();
      return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);
    } else {
      SdkLogger.Companion.error(TAG, "This should never happen...");
      return "";
    }
  }

  /**
   * A simple emulator-detection based on the flutter tools detection logic and a couple of legacy
   * detection systems
   */
  static private boolean isEmulator() {
    return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
        || Build.FINGERPRINT.startsWith("generic")
        || Build.FINGERPRINT.startsWith("unknown")
        || Build.HARDWARE.contains("goldfish")
        || Build.HARDWARE.contains("ranchu")
        || Build.MODEL.contains("google_sdk")
        || Build.MODEL.contains("Emulator")
        || Build.MODEL.contains("Android SDK built for x86")
        || Build.MANUFACTURER.contains("Genymotion")
        || Build.PRODUCT.contains("sdk_google")
        || Build.PRODUCT.contains("google_sdk")
        || Build.PRODUCT.contains("sdk")
        || Build.PRODUCT.contains("sdk_x86")
        || Build.PRODUCT.contains("vbox86p")
        || Build.PRODUCT.contains("emulator")
        || Build.PRODUCT.contains("simulator");
  }
}
