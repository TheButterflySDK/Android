package com.example.butterflyhost;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.butterfly.sdk.ButterflySdk;
import com.butterfly.thebutterflyhost_android.R;

public class MainActivity extends AppCompatActivity {

    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.generate_button);
        button.setText("🦋");

        final Activity activity = this;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ButterflySdk.openReporter(activity,"your-api-key");
            }
        });

    }
}
