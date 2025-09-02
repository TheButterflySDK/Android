package com.example.butterflyhost;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import com.butterfly.sdk.ButterflySdk;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "your-api-key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Optional: control icon colors
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        ImageButton button = findViewById(R.id.generate_button);

        final Activity activity = this;

        button.setOnClickListener(v ->
                ButterflySdk.open(activity, API_KEY)
        );

        boolean useDarkIcons = true; // dark icons = light status bar background
        controller.setAppearanceLightStatusBars(useDarkIcons);     // requires API 23+
        controller.setAppearanceLightNavigationBars(useDarkIcons); // requires API 26+
        
        // Handle deep link if app was launched from a URL (cold start)
        ButterflySdk.handleIncomingIntent(this, getIntent(), API_KEY);
        // OR:
        ButterflySdk.handleDeepLink(this, "https:www.your-website.com/path?param=value", API_KEY);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle deep link if app was already running (warm start)
        ButterflySdk.handleIncomingIntent(this, intent, API_KEY);
    }
}
