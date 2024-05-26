package org.yuezhikong.JavaIMAndroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private HomeFragment homeFragment;
    private SettingFragment settingFragment;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        homeFragment = new HomeFragment();
        settingFragment = new SettingFragment();

        BottomNavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setOnItemSelectedListener(menuItem -> {
            Fragment fragment;
            if (menuItem.getItemId() == R.id.navigation_home_page)
                fragment = homeFragment;
            else if (menuItem.getItemId() == R.id.navigation_settings_page)
                fragment = settingFragment;
            else
                return false;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.mainContainer, fragment);
            transaction.commit();
            return true;
        });

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.mainContainer, homeFragment);
        transaction.commit();
    }
}
