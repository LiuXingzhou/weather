package com.islxz.weather.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.islxz.weather.R;
import com.islxz.weather.fragment.WeatherFragment;

public class MainActivity extends AppCompatActivity {

    private WeatherFragment mWeatherFragment;
    private FragmentManager mFragmentManager;
    private FragmentTransaction mFragmentTransaction;

    private FrameLayout mFrameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Window window = getWindow();

        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams
                .FLAG_TRANSLUCENT_STATUS);
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager
                .LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        mWeatherFragment = new WeatherFragment();
        mFragmentManager = getSupportFragmentManager();
        mFragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentTransaction.replace(R.id.main_fl, mWeatherFragment);
        mFragmentTransaction.commit();
    }
}
