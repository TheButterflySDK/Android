package com.butterfly.sdk;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;

public class ButterflySdk {

    public static void openReporter(Activity activity, String key) {
        openDialog(activity, key);
    }

    private static void openDialog(Activity activity, String apiKey) {
        String languageCode = activity.getResources().getString(R.string.language_code);
        String urlString = "https://butterfly-host.web.app/reporter?language=" + languageCode + "&api_key=" + apiKey +"&is-embedded-via-mobile-sdk=1";

        activity.startActivity(new Intent(activity,WebViewerActivity.class).putExtra("url", urlString));
    }
}
