package com.toffeegryphon.solartime;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.INTERNET}, 0);
        }

        Typeface typeface = Typeface.createFromAsset(getAssets(), "dense.otf");
        typeface = Typeface.create(typeface, Typeface.BOLD);

        TextView title = findViewById(R.id.header);
        title.setTypeface(typeface);

        TextView setWallpaperView = findViewById(R.id.setWallpaperView);

        setWallpaperView.setTypeface(typeface);
        setWallpaperView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(getApplicationContext(), SubsolarWallpaperService.class));
                startActivity(intent);
            }
        });
    }
}
