
package com.vipercn.viper4android_v2.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.vipercn.viper4android_v2.activity.V4AJniInterface;
import com.vipercn.viper4android_v2.activity.ViPER4Android;
import com.vipercn.viper4android_v2.service.ViPER4AndroidService;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        boolean bJniLoaded = V4AJniInterface.CheckLibrary();

        SharedPreferences prefSettings = context.getSharedPreferences(
                ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings", 0);
        boolean bDriverConfigured = prefSettings.getBoolean(
                "viper4android.settings.driverconfigured", false);
        if (bDriverConfigured) {
            context.startService(new Intent(context, ViPER4AndroidService.class));
        }
    }
}
