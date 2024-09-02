package com.example.butterflyhost;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.butterfly.sdk.ButterflySdk;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageButton button = findViewById(R.id.generate_button);
        final Activity activity = this;

        button.setOnClickListener(v ->
                ButterflySdk
                        .openReporter(activity,"your-api-key")
        );
    }
}
