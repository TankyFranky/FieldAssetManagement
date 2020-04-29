package com.example.fieldassetmanagement;

import androidx.appcompat.app.AppCompatActivity;
import gr.net.maroulis.library.EasySplashScreen;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

public class SplashFieldAssetManagement extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EasySplashScreen FieldAssetSplash = new EasySplashScreen(SplashFieldAssetManagement.this)
                .withFullScreen()
                .withTargetActivity(MainActivity.class)
                .withSplashTimeOut(1000)
                .withBackgroundColor(Color.parseColor("#ebb702"))
                .withFooterText("By: Francesco Marrato")
                .withBeforeLogoText("Company Name Here")
                .withAfterLogoText("App Name Here")
                .withLogo(R.mipmap.ic_launcher_round);

        FieldAssetSplash.getFooterTextView().setTextColor(Color.WHITE);
        FieldAssetSplash.getBeforeLogoTextView().setTextColor(Color.WHITE);
        FieldAssetSplash.getAfterLogoTextView().setTextColor(Color.WHITE);

        View FieldAssetSplashScreen = FieldAssetSplash.create();
        setContentView(FieldAssetSplashScreen);
    }
}
