/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.dashclock.configuration;

import android.app.Activity;
import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.apps.dashclock.DashClockService;
import com.google.android.apps.dashclock.HelpUtils;
import com.google.android.apps.dashclock.LogUtils;
import com.google.android.apps.dashclock.Utils;
import com.google.android.apps.dashclock.api.DashClockExtension;

import net.nurik.roman.dashclock.R;

import static com.google.android.apps.dashclock.LogUtils.FORCE_DEBUG;
/**
 * The primary widget configuration activity. Serves as an interstitial when adding the widget, and
 * shows when pressing the settings button in the widget.
 */
public class ConfigurationActivity extends Activity {
    private static final String TAG = LogUtils.makeLogTag(ConfigurationActivity.class);

    public static final String LAUNCHER_ACTIVITY_NAME =
            "com.google.android.apps.dashclock.configuration.ConfigurationLauncherActivity";

    public static final String EXTRA_START_SECTION =
            "com.google.android.apps.dashclock.configuration.extra.START_SECTION";

    public static final int START_SECTION_EXTENSIONS = 0;
    public static final int START_SECTION_APPEARANCE = 1;
    public static final int START_SECTION_DAYDREAM = 2;
    public static final int START_SECTION_ADVANCED = 3;

    private static final int[] SECTION_LABELS = new int[]{
            R.string.section_extensions,
            R.string.section_appearance,
            R.string.section_daydream,
            R.string.section_advanced,
    };

    @SuppressWarnings("unchecked")
    private static final Class<? extends Fragment>[] SECTION_FRAGMENTS = new Class[]{
            ConfigureExtensionsFragment.class,
            ConfigureAppearanceFragment.class,
            ConfigureDaydreamFragment.class,
            ConfigureAdvancedFragment.class,
    };

    // only used when adding a new widget
    private int mNewWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private int mStartSection = START_SECTION_EXTENSIONS;

    private boolean mBackgroundCleared = false;

    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setupFauxDialog();
        super.onCreate(savedInstanceState);

        Utils.enableDisablePhoneOnlyExtensions(this);

        Intent intent = getIntent();
        if (intent != null) {
            if (Intent.ACTION_CREATE_SHORTCUT.equals(intent.getAction())) {
                Intent.ShortcutIconResource icon = new Intent.ShortcutIconResource();
                icon.packageName = getPackageName();
                icon.resourceName = getResources().getResourceName(R.drawable.ic_launcher);
                setResult(RESULT_OK, new Intent()
                        .putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.title_configure))
                        .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon)
                        .putExtra(Intent.EXTRA_SHORTCUT_INTENT,
                                Intent.makeMainActivity(
                                        new ComponentName(this, ConfigurationActivity.class))));
                finish();
            }

            mStartSection = intent.getIntExtra(EXTRA_START_SECTION, 0);
        }

        setContentView(R.layout.activity_configure);

        if (intent != null
                && AppWidgetManager.ACTION_APPWIDGET_CONFIGURE.equals(intent.getAction())) {
            mNewWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mNewWidgetId);
            // See http://code.google.com/p/android/issues/detail?id=2539
            setResult(RESULT_CANCELED, new Intent()
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mNewWidgetId));
        }

        // Set up UI widgets
        setupActionBar();
    }

    private void setupFauxDialog() {
        // Check if this should be a dialog
        TypedValue tv = new TypedValue();
        if (!getTheme().resolveAttribute(R.attr.isDialog, tv, true) || tv.data == 0) {
            return;
        }

        // Should be a dialog; set up the window parameters.
        DisplayMetrics dm = getResources().getDisplayMetrics();

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = getResources().getDimensionPixelSize(R.dimen.configure_dialog_width);
        params.height = Math.min(
                getResources().getDimensionPixelSize(R.dimen.configure_dialog_max_height),
                dm.heightPixels * 3 / 4);
        params.alpha = 1.0f;
        params.dimAmount = 0.5f;
        getWindow().setAttributes(params);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        showWallpaper();
    }

    public void setTranslucentActionBar(boolean translucentActionBar) {
        Drawable backgroundDrawable = getResources().getDrawable(translucentActionBar
                        ? R.drawable.ab_background_translucent : R.drawable.ab_background);
        getActionBar().setBackgroundDrawable(backgroundDrawable);
        showWallpaper();
    }

    private void showWallpaper() {
        if (!mBackgroundCleared) {
            // We initially show a background so that the activity transition (zoom-up animation
            // in Android 4.1) doesn't show the system wallpaper (which is needed in the
            // appearance configuration fragment). Upon user interaction (i.e. once we know the
            // activity transition has finished), clear the background so that the system wallpaper
            // can be seen when the appearance configuration fragment is shown.
            findViewById(R.id.content_container).setBackground(null);
            mBackgroundCleared = true;
        }
    }

    private void setupActionBar() {
        final LayoutInflater inflater = getLayoutInflater();
        View navContainerView = inflater.inflate(R.layout.include_configure_actionbar_nav, null);
        navContainerView.findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // "Done"
                        if (mNewWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            setResult(RESULT_OK, new Intent()
                                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mNewWidgetId));
                        }

                        finish();
                    }
                });

        Spinner sectionSpinner = (Spinner) navContainerView.findViewById(R.id.section_spinner);
        sectionSpinner.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return SECTION_LABELS.length;
            }

            @Override
            public Object getItem(int position) {
                return SECTION_LABELS[position];
            }

            @Override
            public long getItemId(int position) {
                return position + 1;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.list_item_configure_ab_spinner,
                            parent, false);
                }
                ((TextView) convertView.findViewById(android.R.id.text1)).setText(
                        getString(SECTION_LABELS[position]));
                return convertView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.list_item_configure_ab_spinner_dropdown,
                            parent, false);
                }
                ((TextView) convertView.findViewById(android.R.id.text1)).setText(
                        getString(SECTION_LABELS[position]));
                return convertView;
            }
        });

        sectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> spinner, View view, int position, long id) {
                Class<? extends Fragment> fragmentClass = SECTION_FRAGMENTS[position];
                Fragment currentFragment = getFragmentManager().findFragmentById(
                        R.id.content_container);
                if (currentFragment != null && fragmentClass.equals(currentFragment.getClass())) {
                    return;
                }

                try {
                    Fragment newFragment = fragmentClass.newInstance();
                    getFragmentManager().beginTransaction()
                            .replace(R.id.content_container, newFragment)
                            .commitAllowingStateLoss();
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> spinner) {
            }
        });

        sectionSpinner.setSelection(mStartSection);

        getActionBar().setCustomView(navContainerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.configure_overflow, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (FORCE_DEBUG) {
            MenuItem sendLogsItem = menu.findItem(R.id.action_send_logs);
            if (sendLogsItem != null) {
                sendLogsItem.setVisible(false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_get_more_extensions:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/search?q=DashClock+Extension"
                                + "&c=apps"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;

            case R.id.action_send_logs:
                LogUtils.sendDebugLog(ConfigurationActivity.this);
                return true;

            case R.id.action_about:
                HelpUtils.showAboutDialog(ConfigurationActivity.this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Update extensions (settings may have changed)
        // TODO: update only those extensions whose settings have changed
        Intent updateExtensionsIntent = new Intent(this, DashClockService.class);
        updateExtensionsIntent.setAction(DashClockService.ACTION_UPDATE_EXTENSIONS);
        updateExtensionsIntent.putExtra(DashClockService.EXTRA_UPDATE_REASON,
                DashClockExtension.UPDATE_REASON_SETTINGS_CHANGED);
        startService(updateExtensionsIntent);

        // Update all widgets, including a new one if it was just added
        // We can't only update the new one because settings affecting all widgets may have
        // been changed.

        Intent widgetUpdateIntent = new Intent(this, DashClockService.class);
        widgetUpdateIntent.setAction(DashClockService.ACTION_UPDATE_WIDGETS);
        startService(widgetUpdateIntent);
    }
}
