package com.example.butterflyhost;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

import com.butterfly.sdk.ButterflySdk;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "your-api-key";
    private static final Uri MOCKED_DEEPLINK_URI = Uri.parse("your-deeplink-url");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageButton button = findViewById(R.id.generate_button);

        final Activity activity = this;

        button.setOnClickListener(v ->
                ButterflySdk.openReporter(activity, API_KEY)
//                ButterflySdk.handleIncomingIntent(activity, MOCKED_DEEPLINK_URI, API_KEY)
        );

        // Handle deep link if app was launched from a URL (cold start)
        ButterflySdk.handleIncomingIntent(this, getIntent(), API_KEY);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle deep link if app was already running (warm start)
        ButterflySdk.handleIncomingIntent(this, intent, API_KEY);
    }
}
