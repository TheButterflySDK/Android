package com.butterfly.sdk;

import android.app.Activity;
import android.content.Intent;

import com.butterfly.sdk.logic.WebViewerActivity;

public class ButterflySdk {
    /**
     * Sets the main user interface's language, no matter what's the language of the user's device.
     * @param interfaceLanguage An enum represents the language, currently accepting only Hebrew or English.
     */
    public static void overrideLanguage(String interfaceLanguage) {
        if(interfaceLanguage == null || interfaceLanguage.length() != 2) return;

        WebViewerActivity.Companion.setLanguageCodeToOverride(interfaceLanguage);
    }

    /**
     * Sets the country of the reporter that will be used in the Butterfly servers, no matter where it was really sent from.
     * @param countryCode A two letters country code.
     */
    public static void overrideCountry(String countryCode) {
        WebViewerActivity.Companion.setCountryCodeToOverride(countryCode);
    }

    /**
     * Sets a new color theme of the Butterfly's screens
     * @param customColorHexa A string represents the hexadecimal value of the color. Examples of possible formats: "0xFF91BA48", "FF91BA48", "91BA48"
     */
    public static void useCustomColor(String customColorHexa) {
        WebViewerActivity.Companion.setCustomColorHexaString(customColorHexa);
    }

    public static void openReporter(Activity activity, String key) {
        openDialog(activity, key);
    }

    public static void handleIncomingIntent(Activity activity, Intent intent, String key) {
        openURL(activity, intent, key);
    }

    private static void openDialog(Activity activity, String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) return;

        WebViewerActivity.Companion.open(activity, apiKey);
    }

    private static void openURL(Activity activity, Intent intent, String apiKey) {
        if (apiKey == null || intent.getData() == null || apiKey.isEmpty()) return;

        WebViewerActivity.Companion.handleIncomingURL(activity, intent.getData(), apiKey);
    }
}
