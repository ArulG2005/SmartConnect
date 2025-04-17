package com.logicfirst.smartconnect;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    FrameLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        container = findViewById(R.id.container);

        // Default Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new Home_Screen()).commit();

        // Navigation Item Select Listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selected = null;

            if (item.getItemId() == R.id.nav_home) {
                selected = new Home_Screen();
            } else if (item.getItemId() == R.id.nav_history) {
                selected = new History();
            } else if (item.getItemId() == R.id.nav_settings) {
                selected = new Settings();
            }


            if (selected != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, selected).commit();
            }
            return true;
        });
    }
}
