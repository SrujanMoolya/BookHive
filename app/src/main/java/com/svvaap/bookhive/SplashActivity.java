package com.svvaap.bookhive;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check if user is logged in
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                // User is logged in, go to MainActivity
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
            } else {
                // User is not logged in, go to LoginFragment via MainActivity
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                intent.putExtra("showLogin", true);
                startActivity(intent);
            }
            finish();
        }, SPLASH_DURATION);
    }
}
