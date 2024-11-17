package com.posed.xpalter;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;

import com.posed.xpalter.hider.ui.HiderRootActivity;

public class MainActivity extends AppCompatActivity {

    private Switch swHideUsbDebugging, swHideAccessibility, swHideApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.xp_layout);
        // 权限隐藏相关输入框和开关
        swHideUsbDebugging = findViewById(R.id.swHideUsbDebugging);
        swHideAccessibility = findViewById(R.id.swHideAccessibility);
        swHideApp = findViewById(R.id.swHideApp);

        swHideUsbDebugging.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            }
        });
        swHideAccessibility.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            }
        });
        swHideApp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            }
        });
        findViewById(R.id.hider_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.startActivity(new Intent(MainActivity.this, HiderRootActivity.class));
            }
        });
    }

    private void applySettings() {

    }


}
