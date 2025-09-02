package com.butterfly.sdk;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import com.butterfly.sdk.logic.WebViewerActivity;
import com.butterfly.sdk.utils.SdkLogger;

public class ButterflySdk {
    private static final String TAG = "ButterflySdk";

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

    /// Opens the Butterfly screen.
    public static void open(Activity activity, String apiKey) {
        openButterflyScreen(activity, apiKey);
    }

    @Deprecated()
    /// Deprecated: Use `open(Activity activity, String apiKey)` instead.
    public static void openReporter(Activity activity, String key) {
        openButterflyScreen(activity, key);
    }

    public static void handleIncomingIntent(Activity activity, Intent intent, String apiKey) {
        if (apiKey == null || intent == null || intent.getData() == null || apiKey.isEmpty()) return;

        handleDeepLink(activity, intent.getData(), apiKey);
    }

    public static void handleDeepLink(Activity activity, String urlString, String apiKey) {
        Uri uri = null;
        try {
            uri = Uri.parse(urlString);
        } catch (Exception e) {
            SdkLogger.Companion.error(TAG, e);
        }

        handleDeepLink(activity, uri, apiKey);
    }

    public static void handleDeepLink(Activity activity, Uri uri, String apiKey) {
        if (apiKey == null || uri == null || activity == null || uri.toString().isEmpty() || apiKey.isEmpty()) return;

        WebViewerActivity.Companion.handleIncomingURI(activity, uri, apiKey);
    }

    private static void openButterflyScreen(Activity activity, String apiKey) {
        if (apiKey == null || activity == null || apiKey.isEmpty()) return;

        WebViewerActivity.Companion.open(activity, apiKey);
    }
}
