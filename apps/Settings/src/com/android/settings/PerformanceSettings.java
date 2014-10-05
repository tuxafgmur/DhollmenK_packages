/*
 * Copyright (C) 2013 Tuxafgmur (Dhollmen)
 *
 * Based on work from The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

/*
 * Displays preferences for I/O, CPU and GPU performance related Settings
 */
public class PerformanceSettings extends SettingsPreferenceFragment implements
										Preference.OnPreferenceChangeListener {

    public static final String GOV_PREF = "pref_cpu_gov";
    public static final String GOV_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
    public static final String GOV_LIST_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors";

    public static final String IOSCHED_PREF = "pref_io_sched";
    public static final String IOSCHED_LIST_FILE = "/sys/block/mmcblk0/queue/scheduler";
    
	public static final String FREQ_MIN_PREF = "pref_cpu_freq_min";
    public static final String FREQ_MAX_PREF = "pref_cpu_freq_max";
	public static final String SCALE_CUR_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
    public static final String FREQ_MIN_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";
    public static final String FREQ_MAX_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
    public static final String FREQ_LIST_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies";
    
    public static final String GPU_PREF = "pref_gpu_freq";
    public static final String GPU_FREQ_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/gpu_frequency";
    public static final String GPU_OC_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/gpu_oc";
    public static final String GPU_MHZ = "307000 384000 512000";
    public static final String GPU_MHZ_VAL = "0 1 2";
    
    public static final String SOB_PREF = "pref_performance_seton_boot";
    
    private String mGovernorFormat;
    private String mIOSchedulerFormat;
    private String mMinFrequencyFormat;
    private String mMaxFrequencyFormat;
	private String mGpuFormat;
    
    private ListPreference mGovernorPref;
    private ListPreference mIOSchedulerPref;
    private ListPreference mMinFrequencyPref;
    private ListPreference mMaxFrequencyPref;
    private ListPreference mGpuPref;

    private Switch mEnabledSwitch;
    
    private boolean mUnavailable;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (android.os.Process.myUserHandle().getIdentifier() != UserHandle.USER_OWNER) {
            mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getActivity(), null));
            return;
        }
        
        mGovernorFormat = getString(R.string.cpu_governors_summary);
        mIOSchedulerFormat = getString(R.string.io_sched_summary);
        mMinFrequencyFormat = getString(R.string.cpu_min_freq_summary);
        mMaxFrequencyFormat = getString(R.string.cpu_max_freq_summary);
		mGpuFormat = getString(R.string.gpu_freq_summary);
        
        String[] availableGovernors = new String[0];
		String[] availableIOSchedulers = new String[0];        
        String[] availableFrequencies = new String[0];
        String[] availableGpuFreqs = new String[0];
        String[] availableGpuVals = new String[0];
        String[] frequencies;
        String availableGovernorsLine;
        String availableIOSchedulersLine;
        String availableFrequenciesLine;
        String availableGpuFreqLine;
        String availableGpuValLine;
        String currentIOScheduler = null;
        String temp;
		String val;
        int bropen, brclose;
        
        addPreferencesFromResource(R.xml.performance_prefs);

        PreferenceScreen prefScreen = getPreferenceScreen();

        mGovernorPref = (ListPreference) prefScreen.findPreference(GOV_PREF);
        mIOSchedulerPref = (ListPreference) prefScreen.findPreference(IOSCHED_PREF);
        mMinFrequencyPref = (ListPreference) prefScreen.findPreference(FREQ_MIN_PREF);
        mMaxFrequencyPref = (ListPreference) prefScreen.findPreference(FREQ_MAX_PREF);
		mGpuPref = (ListPreference) prefScreen.findPreference(GPU_PREF);
        
        // Governor
        if (!Utils.fileExists(GOV_LIST_FILE) ||
				!Utils.fileExists(GOV_FILE) || 
				(temp = Utils.fileReadOneLine(GOV_FILE)) == null ||
				(availableGovernorsLine = Utils.fileReadOneLine(GOV_LIST_FILE)) == null) {
			prefScreen.removePreference(mGovernorPref);
        } else {
            availableGovernors = availableGovernorsLine.split(" ");
            mGovernorPref.setEntryValues(availableGovernors);
            mGovernorPref.setEntries(availableGovernors);
            mGovernorPref.setValue(temp);
            mGovernorPref.setSummary(String.format(mGovernorFormat, temp));
            mGovernorPref.setOnPreferenceChangeListener(this);
        }

        // Scheduler
        if (!Utils.fileExists(IOSCHED_LIST_FILE) ||
            (availableIOSchedulersLine = Utils.fileReadOneLine(IOSCHED_LIST_FILE)) == null) {
            prefScreen.removePreference(mIOSchedulerPref);
        } else {
            availableIOSchedulers = availableIOSchedulersLine.replace("[", "").replace("]", "").split(" ");
            bropen = availableIOSchedulersLine.indexOf("[");
            brclose = availableIOSchedulersLine.lastIndexOf("]");
            if (bropen >= 0 && brclose >= 0)
                currentIOScheduler = availableIOSchedulersLine.substring(bropen + 1, brclose);
            mIOSchedulerPref.setEntryValues(availableIOSchedulers);
            mIOSchedulerPref.setEntries(availableIOSchedulers);
            if (currentIOScheduler != null)
                mIOSchedulerPref.setValue(currentIOScheduler);
            mIOSchedulerPref.setSummary(String.format(mIOSchedulerFormat, currentIOScheduler));
            mIOSchedulerPref.setOnPreferenceChangeListener(this);
        }
        
        // Cpu
        if (!Utils.fileExists(FREQ_LIST_FILE) || (availableFrequenciesLine = Utils.fileReadOneLine(FREQ_LIST_FILE)) == null) {
            mMinFrequencyPref.setEnabled(false);
            mMaxFrequencyPref.setEnabled(false);
        } else {
            availableFrequencies = availableFrequenciesLine.split(" ");
            frequencies = new String[availableFrequencies.length];
            for (int i = 0; i < frequencies.length; i++) {
                frequencies[i] = toMHz(availableFrequencies[i]);
            }
            // Min frequency
            if (!Utils.fileExists(FREQ_MIN_FILE) || (temp = Utils.fileReadOneLine(FREQ_MIN_FILE)) == null) {
                mMinFrequencyPref.setEnabled(false);
            } else {
                mMinFrequencyPref.setEntryValues(availableFrequencies);
                mMinFrequencyPref.setEntries(frequencies);
                mMinFrequencyPref.setValue(temp);
                mMinFrequencyPref.setSummary(String.format(mMinFrequencyFormat, toMHz(temp)));
                mMinFrequencyPref.setOnPreferenceChangeListener(this);
            }
            // Max frequency
            if (!Utils.fileExists(FREQ_MAX_FILE) || (temp = Utils.fileReadOneLine(FREQ_MAX_FILE)) == null) {
                mMaxFrequencyPref.setEnabled(false);
            } else {
                mMaxFrequencyPref.setEntryValues(availableFrequencies);
                mMaxFrequencyPref.setEntries(frequencies);
                mMaxFrequencyPref.setValue(temp);
                mMaxFrequencyPref.setSummary(String.format(mMaxFrequencyFormat, toMHz(temp)));
                mMaxFrequencyPref.setOnPreferenceChangeListener(this);
            }
        }
        
        // Gpu
        if (!Utils.fileExists(GPU_OC_FILE) || (temp = Utils.fileReadOneLine(GPU_FREQ_FILE)) == null || (val = Utils.fileReadOneLine(GPU_OC_FILE)) == null) {
			prefScreen.removePreference(mGpuPref);
        } else {
			availableGpuValLine = GPU_MHZ_VAL;
            availableGpuVals = availableGpuValLine.split(" ");
            availableGpuFreqLine = GPU_MHZ;
            availableGpuFreqs = availableGpuFreqLine.split(" ");
            frequencies = new String[availableGpuFreqs.length];
            for (int i = 0; i < frequencies.length; i++) {
                frequencies[i] = toMHz(availableGpuFreqs[i]);
            }
            mGpuPref.setEntryValues(availableGpuVals);
            mGpuPref.setEntries(frequencies);
            mGpuPref.setValue(val);
            mGpuPref.setSummary(temp);
            mGpuPref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void onResume() {
        String availableIOSchedulersLine;
        String currentIOScheduler;
        String temp;
        int bropen, brclose;
        
        super.onResume();

        if (Utils.fileExists(GOV_FILE) && (temp = Utils.fileReadOneLine(GOV_FILE)) != null) {
            mGovernorPref.setSummary(String.format(mGovernorFormat, temp));
        }
        
        if (Utils.fileExists(IOSCHED_LIST_FILE) && (availableIOSchedulersLine = Utils.fileReadOneLine(IOSCHED_LIST_FILE)) != null) {
            bropen = availableIOSchedulersLine.indexOf("[");
            brclose = availableIOSchedulersLine.lastIndexOf("]");
            if (bropen >= 0 && brclose >= 0) {
                currentIOScheduler = availableIOSchedulersLine.substring(bropen + 1, brclose);
                mIOSchedulerPref.setSummary(String.format(mIOSchedulerFormat, currentIOScheduler));
            }
        }
        
        if (Utils.fileExists(FREQ_MIN_FILE) && (temp = Utils.fileReadOneLine(FREQ_MIN_FILE)) != null) {
            mMinFrequencyPref.setValue(temp);
            mMinFrequencyPref.setSummary(String.format(mMinFrequencyFormat, toMHz(temp)));
        }

        if (Utils.fileExists(FREQ_MAX_FILE) && (temp = Utils.fileReadOneLine(FREQ_MAX_FILE)) != null) {
            mMaxFrequencyPref.setValue(temp);
            mMaxFrequencyPref.setSummary(String.format(mMaxFrequencyFormat, toMHz(temp)));
        }

        if (Utils.fileExists(GPU_OC_FILE) && (temp = Utils.fileReadOneLine(GPU_FREQ_FILE)) != null) {
            mGpuPref.setSummary(temp);
        }
    }

   @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();
        mEnabledSwitch = new Switch(activity);
        final int padding = activity.getResources().getDimensionPixelSize(R.dimen.action_bar_switch_padding);
        mEnabledSwitch.setPaddingRelative(0, 0, padding, 0);
    }    
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String fname = "";
        String tmp;
        if (newValue != null) {
            if (preference == mGovernorPref) {
                fname = GOV_FILE;
            } else if (preference == mIOSchedulerPref) {
                fname = IOSCHED_LIST_FILE;
            } else if (preference == mMinFrequencyPref) {
                fname = FREQ_MIN_FILE;
            } else if (preference == mMaxFrequencyPref) {
                fname = FREQ_MAX_FILE;
            } else if (preference == mGpuPref) {
				fname = GPU_OC_FILE;
            }

            if (Utils.fileWriteOneLine(fname, (String) newValue)) {
                if (preference == mGovernorPref) {
                    mGovernorPref.setSummary(String.format(mGovernorFormat, (String) newValue));
                } else if (preference == mIOSchedulerPref) {
                    mIOSchedulerPref.setSummary(String.format(mIOSchedulerFormat, (String) newValue));
                } else if (preference == mMinFrequencyPref) {
                    mMinFrequencyPref.setSummary(String.format(mMinFrequencyFormat, toMHz((String) newValue)));
                } else if (preference == mMaxFrequencyPref) {
                    mMaxFrequencyPref.setSummary(String.format(mMaxFrequencyFormat, toMHz((String) newValue)));
                } else if (preference == mGpuPref) {
					tmp = Utils.fileReadOneLine(GPU_FREQ_FILE);
					mGpuPref.setSummary(tmp);
				}
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private String toMHz(String mhzString) {
        return new StringBuilder().append(Integer.valueOf(mhzString) / 1000).append(" MHz") .toString();
    }
}