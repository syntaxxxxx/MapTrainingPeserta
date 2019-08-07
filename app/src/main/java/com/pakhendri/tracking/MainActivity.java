package com.pakhendri.tracking;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onMap(View view) {
        startActivity(new Intent(this,MapsActivity.class));
        finish();
    }

    public void onPlace(View view) {
        startActivity(new Intent(this,PlacePickerActivity.class));
        finish();

    }
}
