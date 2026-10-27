package com.visahunter.operator;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "OperatorMainActivity";
    private static final int REQUEST_PROVISION_PROFILE = 1001;

    private TextView tvProfileStatus;
    private TextView tvA11yStatus;
    private Button btnSetupWorkProfile;
    private Button btnCloneAlmaviva;
    private Button btnLaunchPersonalAlmaviva;
    private Button btnLaunchWorkAlmaviva;
    private Button btnOpenUserSettings;
    private Button btnEnableAccessibility;

    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private boolean isWorkProfile = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = OperatorDeviceAdminReceiver.getComponentName(this);

        initViews();
        checkEnvironmentStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkEnvironmentStatus();
    }

    private void initViews() {
        tvProfileStatus = findViewById(R.id.tvProfileStatus);
        tvA11yStatus = findViewById(R.id.tvA11yStatus);
        btnSetupWorkProfile = findViewById(R.id.btnSetupWorkProfile);
        btnCloneAlmaviva = findViewById(R.id.btnCloneAlmaviva);
        btnLaunchPersonalAlmaviva = findViewById(R.id.btnLaunchPersonalAlmaviva);
        btnLaunchWorkAlmaviva = findViewById(R.id.btnLaunchWorkAlmaviva);
        btnOpenUserSettings = findViewById(R.id.btnOpenUserSettings);
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility);

        btnSetupWorkProfile.setOnClickListener(v -> setupWorkProfile());
        btnCloneAlmaviva.setOnClickListener(v -> cloneAlmavivaPackage());
        btnLaunchPersonalAlmaviva.setOnClickListener(v -> launchPersonalAlmaviva());
        btnLaunchWorkAlmaviva.setOnClickListener(v -> launchWorkAlmaviva());
        btnOpenUserSettings.setOnClickListener(v -> openNativeUserSettings());
        btnEnableAccessibility.setOnClickListener(v -> handleAccessibilityClick());
    }

    private void checkEnvironmentStatus() {
        // 1. Check if running inside Work Profile
        isWorkProfile = dpm.isProfileOwnerApp(getPackageName());

        if (isWorkProfile) {
            tvProfileStatus.setText("💼 مساحة العمل المعزولة (Work Profile) - نشطة");
            tvProfileStatus.setTextColor(ContextCompat.getColor(this, R.color.work_profile_badge));
            btnSetupWorkProfile.setVisibility(View.GONE);
            btnCloneAlmaviva.setVisibility(View.VISIBLE);
        } else {
            // Check if device already has a work profile configured
            UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
            List<UserHandle> userProfiles = userManager.getUserProfiles();
            boolean hasManagedProfile = false;
            for (UserHandle handle : userProfiles) {
                if (!handle.equals(Process.myUserHandle())) {
                    hasManagedProfile = true;
                    break;
                }
            }

            if (hasManagedProfile) {
                tvProfileStatus.setText("📱 الملف الأساسي (توجد مساحة عمل معزولة 💼)");
                tvProfileStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_gold));
                btnSetupWorkProfile.setText("🔄 إعادة ضبط / فحص مساحة العمل");
            } else {
                tvProfileStatus.setText("📱 الملف الشخصي الأساسي (User 0)");
                tvProfileStatus.setTextColor(ContextCompat.getColor(this, R.color.text_main));
                btnSetupWorkProfile.setText("💼 تفعيل مساحة العمل المعزولة (Work Profile)");
            }
            btnCloneAlmaviva.setVisibility(View.GONE);
        }

        // 2. Check Accessibility Service Status
        boolean a11yActive = AlmavivaOperatorAccessibilityService.isRunning() || isAccessibilityServiceEnabled();
        if (a11yActive) {
            tvA11yStatus.setText("🟢 خدمة النقر التلقائي للـ QR: متصلة وجاهزة");
            tvA11yStatus.setTextColor(ContextCompat.getColor(this, R.color.success));
            btnEnableAccessibility.setText("✅ خدمة النقر مفعلة");
        } else {
            tvA11yStatus.setText("🔴 خدمة النقر التلقائي للـ QR: معطلة");
            tvA11yStatus.setTextColor(ContextCompat.getColor(this, R.color.danger));
            btnEnableAccessibility.setText("⚡ تفعيل خدمة النقر التلقائي");
        }
    }

    private void setupWorkProfile() {
        try {
            Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MAIN_COLOR, 0xFF0D47A1);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, true);
            }

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, REQUEST_PROVISION_PROFILE);
                Toast.makeText(this, "جاري إطلاق معالج مساحة العمل المعزولة من أندرويد...", Toast.LENGTH_SHORT).show();
            } else {
                showMultiUserFallbackDialog();
            }
        } catch (Exception e) {
            Log.e(TAG, "Provisioning failed: " + e.getMessage(), e);
            showMultiUserFallbackDialog();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PROVISION_PROFILE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "🎉 تم إنشاء مساحة العمل المعزولة بنجاح!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "تم إلغاء معالج مساحة العمل أو تم إنشاؤها مسبقاً.", Toast.LENGTH_SHORT).show();
            }
            checkEnvironmentStatus();
        }
    }

    private void cloneAlmavivaPackage() {
        if (!isWorkProfile) {
            Toast.makeText(this, "يجب فتح هذه الخاصية من داخل مساحة العمل", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            boolean success = dpm.installExistingPackage(adminComponent, "it.almaviva.visti");
            if (success) {
                Toast.makeText(this, "✅ تم استنساخ تطبيق المافيفا داخل مساحة العمل بنجاح!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "⚠️ تأكد من تثبيت تطبيق المافيفا الأصلي على هاتفك أولاً", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "خطأ أثناء الاستنساخ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchPersonalAlmaviva() {
        try {
            PackageManager pm = getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage("it.almaviva.visti");
            if (intent != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "تطبيق المافيفا غير مثبت في الملف الأساسي", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "تعذر تشغيل التطبيق: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchWorkAlmaviva() {
        try {
            LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
            UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
            List<UserHandle> profiles = userManager.getUserProfiles();

            UserHandle workProfile = null;
            for (UserHandle handle : profiles) {
                if (!handle.equals(Process.myUserHandle())) {
                    workProfile = handle;
                    break;
                }
            }

            if (workProfile != null && launcherApps != null) {
                launcherApps.startMainActivity(
                        new ComponentName("it.almaviva.visti", "it.almaviva.visti.MainActivity"),
                        workProfile,
                        null,
                        null
                );
                Toast.makeText(this, "🚀 تم فتح تطبيق المافيفا المعزول (الحساب الثاني)!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "لم يتم العثور على مساحة عمل مفعلة، قم بتفعيلها أولاً", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            // Fallback: direct standard launch
            launchPersonalAlmaviva();
        }
    }

    private void openNativeUserSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_USER_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Settings.ACTION_SYNC_SETTINGS);
                startActivity(fallback);
            } catch (Exception ex) {
                Toast.makeText(this, "افتح إعدادات الهاتف -> النظام -> المستخدمون المتعددون", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handleAccessibilityClick() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "ابحث عن 'مساعد تشغيل المافيفا المعزول' واضغط تفعيل", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح الإعدادات: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        ComponentName expectedComponentName = new ComponentName(this, AlmavivaOperatorAccessibilityService.class);
        String enabledServicesSetting = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null) return false;

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);
        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
            if (enabledService != null && enabledService.equals(expectedComponentName)) {
                return true;
            }
        }
        return false;
    }

    private void showMultiUserFallbackDialog() {
        new AlertDialog.Builder(this)
                .setTitle("👥 خيار المستخدمين المتعددين (Native Multi-User)")
                .setMessage("هاتفك يدعم إنشاء مستخدمين معزولين تماماً (Add User) من إعدادات النظام مباشرة كما نجح معك سابقاً.\n\n" +
                        "هل تريد فتح شاشة إعدادات المستخدمين في هاتفك لإنشاء مستخدم معزول جديد؟")
                .setPositiveButton("فتح إعدادات المستخدمين", (d, w) -> openNativeUserSettings())
                .setNegativeButton("إلغاء", null)
                .show();
    }
}
