package com.visahunter.operator;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class OperatorDeviceAdminReceiver extends DeviceAdminReceiver {
    private static final String TAG = "OperatorAdminReceiver";

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), OperatorDeviceAdminReceiver.class);
    }

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        Log.i(TAG, "Work Profile Provisioning Complete!");
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = getComponentName(context);

        try {
            // Set friendly profile name
            dpm.setProfileName(admin, "VisaHunter Workspace");
            
            // Enable the profile
            dpm.setProfileEnabled(admin);
            
            // Automatically clone Almaviva into the work profile
            boolean cloned = dpm.installExistingPackage(admin, "it.almaviva.visti");
            Log.i(TAG, "Cloned it.almaviva.visti into Work Profile: " + cloned);

            Toast.makeText(context, "✅ تم تفعيل مساحة العمل المعزولة بنجاح واستنساخ المافيفا!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error finalizing work profile: " + e.getMessage(), e);
        }
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Log.i(TAG, "Device Admin enabled.");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Log.i(TAG, "Device Admin disabled.");
    }
}
