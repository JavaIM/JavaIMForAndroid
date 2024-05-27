package org.yuezhikong.JavaIMAndroid;

import android.os.Bundle;
import android.view.Menu;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.yuezhikong.JavaIMAndroid.pages.HomeFragment;
import org.yuezhikong.JavaIMAndroid.pages.JavaIMPage;
import org.yuezhikong.JavaIMAndroid.pages.SettingFragment;

public class MainActivity extends AppCompatActivity {
    private HomeFragment homeFragment;
    private SettingFragment settingFragment;

    private JavaIMPage currentPage;

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
            if (!currentPage.isStarted())
                return false;
            if (menuItem.getItemId() == R.id.navigation_home_page)
                currentPage = homeFragment;
            else if (menuItem.getItemId() == R.id.navigation_settings_page)
                currentPage = settingFragment;
            else
                return false;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.mainContainer, currentPage);
            transaction.commit();
            return true;
        });

        currentPage = homeFragment;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.mainContainer, currentPage);
        transaction.commit();
    }
}
