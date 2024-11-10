package com.posed.xpalter;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etModel, etBrand, etManufacturer, etDevice, etSerial, etIMEI;
    private EditText etTargetAppPackage;
    private Switch swHideUsbDebugging, swHideAccessibility, swHideApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.xp_layout);

        // 设备信息伪造相关输入框
        etModel = findViewById(R.id.etModel);
        etBrand = findViewById(R.id.etBrand);
        etManufacturer = findViewById(R.id.etManufacturer);
        etDevice = findViewById(R.id.etDevice);
        etSerial = findViewById(R.id.etSerial);
        etIMEI = findViewById(R.id.etIMEI);

        // 权限隐藏相关输入框和开关
        etTargetAppPackage = findViewById(R.id.etTargetAppPackage);
        swHideUsbDebugging = findViewById(R.id.swHideUsbDebugging);
        swHideAccessibility = findViewById(R.id.swHideAccessibility);
        swHideApp = findViewById(R.id.swHideApp);

        // 应用按钮点击事件
        findViewById(R.id.btnApply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applySettings();
            }
        });
    }

    private void applySettings() {
        // 伪造设备信息
        String model = etModel.getText().toString();
        String brand = etBrand.getText().toString();
        String manufacturer = etManufacturer.getText().toString();
        String device = etDevice.getText().toString();
        String serial = etSerial.getText().toString();
        String imei = etIMEI.getText().toString();

        if (!TextUtils.isEmpty(model) && !TextUtils.isEmpty(brand)) {
            DeviceSpoofTool.setFakeDeviceInfo(model, brand, manufacturer, device, serial);
            DeviceSpoofTool.setFakeTelephonyInfo(imei, "89014103211118510720");  // 示例 SIM 序列号
        } else {
            Toast.makeText(this, "请输入伪造的设备信息", Toast.LENGTH_SHORT).show();
            return;
        }

        // 处理权限隐藏开关
        String targetPackage = etTargetAppPackage.getText().toString();
        if (!TextUtils.isEmpty(targetPackage)) {
            AppPermissionTool.setHideUsbDebugging(swHideUsbDebugging.isChecked());
            AppPermissionTool.setHideAccessibilityPermission(targetPackage, swHideAccessibility.isChecked());
            AppPermissionTool.setHideAppFromList(targetPackage, swHideApp.isChecked());
        } else {
            Toast.makeText(this, "请输入目标应用包名", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "设置已应用", Toast.LENGTH_SHORT).show();
    }
}
