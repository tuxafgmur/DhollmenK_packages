/*
 * Copyright (C) 2013-2014 Tuxafgmur (Dhollmen)
 *
 * Insprired by The CyanogenMod Project module
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.SystemProperties;

import android.preference.PreferenceManager;

import com.android.settings.R;
import com.android.settings.Utils;

import java.util.Arrays;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver_Settings";

	public static final String GOV_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
	public static final String IOSCHED_FILE = "/sys/block/mmcblk0/queue/scheduler";
	public static final String FREQ_MIN_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";
	public static final String FREQ_MAX_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
	public static final String GPU_OC_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/gpu_oc";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            applyPERFORMANCE(context);
        }
    }

    private void applyPERFORMANCE(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean("pref_performance_seton_boot", false) == false) {
            return;
        }

        String mPrefGovernor = prefs.getString("pref_cpu_gov", null);
        String mPrefIOSched = prefs.getString("pref_io_sched", null);
        String mPrefMinFreq = prefs.getString("pref_cpu_freq_min", null);
        String mPrefMaxFreq = prefs.getString("pref_cpu_freq_max", null);
        String mPrefGPUVal = prefs.getString("pref_gpu_freq", null);

		if (mPrefGovernor != null) {
			Utils.fileWriteOneLine(GOV_FILE, mPrefGovernor);
		}

		if (mPrefIOSched != null) {
			Utils.fileWriteOneLine(IOSCHED_FILE, mPrefIOSched);
		}

		if (mPrefMinFreq != null) {
			Utils.fileWriteOneLine(FREQ_MIN_FILE, mPrefMinFreq);
		}

		if (mPrefMaxFreq != null) {
			Utils.fileWriteOneLine(FREQ_MAX_FILE, mPrefMaxFreq);
		}

		if (mPrefGPUVal != null) {
			Utils.fileWriteOneLine(GPU_OC_FILE, mPrefGPUVal);
		}
    }

}
