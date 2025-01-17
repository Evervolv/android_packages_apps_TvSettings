/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.tv.settings.accessibility;

import android.app.admin.DevicePolicyManager;
import android.app.tvsettings.TvSettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;

import java.util.List;
import java.util.Set;

/**
 * Fragment for controlling accessibility service
 */
public class AccessibilityServiceFragment extends SettingsPreferenceFragment implements
        AccessibilityServiceConfirmationFragment.OnAccessibilityServiceConfirmedListener {
    private static final String ARG_PACKAGE_NAME = "packageName";
    private static final String ARG_SERVICE_NAME = "serviceName";
    private static final String ARG_SETTINGS_ACTIVITY_NAME = "settingsActivityName";
    private static final String ARG_LABEL = "label";

    private RestrictedSwitchPreference mEnablePref;

    /**
     * Put args in bundle
     * @param args Bundle to prepare
     * @param packageName Package of accessibility service
     * @param serviceName Class of accessibility service
     * @param activityName Class of accessibility service settings activity
     * @param label Screen title
     */
    public static void prepareArgs(@NonNull Bundle args, String packageName, String serviceName,
            String activityName, String label) {
        args.putString(ARG_PACKAGE_NAME, packageName);
        args.putString(ARG_SERVICE_NAME, serviceName);
        args.putString(ARG_SETTINGS_ACTIVITY_NAME, activityName);
        args.putString(ARG_LABEL, label);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final Context themedContext = getPreferenceManager().getContext();
        final PreferenceScreen screen =
                getPreferenceManager().createPreferenceScreen(themedContext);
        screen.setTitle(getArguments().getString(ARG_LABEL));

        mEnablePref = new RestrictedSwitchPreference(themedContext);
        mEnablePref.setTitle(R.string.system_accessibility_status);
        mEnablePref.setFragment(AccessibilityServiceConfirmationFragment.class.getName());
        screen.addPreference(mEnablePref);

        final Preference settingsPref = new Preference(themedContext);
        settingsPref.setTitle(R.string.system_accessibility_config);
        final String activityName = getArguments().getString(ARG_SETTINGS_ACTIVITY_NAME);
        final String packageName = getArguments().getString(ARG_PACKAGE_NAME);
        if (!TextUtils.isEmpty(activityName) && !TextUtils.isEmpty(packageName)) {
            settingsPref.setIntent(new Intent(Intent.ACTION_MAIN)
                    .setComponent(new ComponentName(packageName, activityName)));
        } else {
            settingsPref.setEnabled(false);
        }
        screen.addPreference(settingsPref);

        final String serviceName = getArguments().getString(ARG_SERVICE_NAME);
        ComponentName serviceComponent=null;
        if(!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(serviceName)){
            serviceComponent = new ComponentName(packageName, serviceName);
        }
        if (serviceComponent != null
            && serviceComponent
            .flattenToString()
            .equals(
                getResources()
                    .getString(R.string.
                                   accessibility_screen_reader_flattened_component_name))) {
            final Preference screenReaderPref = new Preference(themedContext);
            screenReaderPref.setTitle(
                getResources().getString(
                    R.string.screen_reader_service_title));
            screenReaderPref.setSummary(R.string.screen_reader_summary);
            screenReaderPref.setEnabled(false);
            screen.addPreference(screenReaderPref);
        }

        setPreferenceScreen(screen);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mEnablePref && !mEnablePref.isDisabledByAdmin()) {
            // Prepare confirmation dialog and reverts switch until result comes back.
            updateEnablePref();
            // Pass to super to launch confirmation dialog.
            super.onPreferenceTreeClick(preference);
            return true;
        } else {
            return super.onPreferenceTreeClick(preference);
        }
    }

    private void updateEnablePref() {
        final String packageName = getArguments().getString(ARG_PACKAGE_NAME);
        final String serviceName = getArguments().getString(ARG_SERVICE_NAME);
        final ComponentName serviceComponent = new ComponentName(packageName, serviceName);
        final Set<ComponentName> enabledServices =
                AccessibilityUtils.getEnabledServicesFromSettings(getActivity());
        final boolean enabled = enabledServices.contains(serviceComponent);
        mEnablePref.setChecked(enabled);
        AccessibilityServiceConfirmationFragment.prepareArgs(mEnablePref.getExtras(),
                new ComponentName(packageName, serviceName),
                getArguments().getString(ARG_LABEL), !enabled);

        DevicePolicyManager dpm = getContext().getSystemService(DevicePolicyManager.class);
        final List<String> permittedServices = dpm.getPermittedAccessibilityServices(
                UserHandle.myUserId());
        final boolean serviceAllowed = permittedServices == null
                || permittedServices.contains(packageName);

        if (serviceAllowed || enabled) {
            mEnablePref.setEnabled(true);
        } else {
            // Disable accessibility service that are not permitted.
            final EnforcedAdmin admin =
                    RestrictedLockUtilsInternal.checkIfAccessibilityServiceDisallowed(
                            getContext(), packageName, UserHandle.myUserId());
            if (admin != null) {
                mEnablePref.setDisabledByAdmin(admin);
            } else {
                mEnablePref.setEnabled(false);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateEnablePref();
    }

    @Override
    public void onAccessibilityServiceConfirmed(ComponentName componentName, boolean enabling) {
        AccessibilityUtils.setAccessibilityServiceState(getActivity(),
                componentName, enabling);
        if (mEnablePref != null) {
            mEnablePref.setChecked(enabling);
        }
    }

    @Override
    protected int getPageId() {
        // Partial matching of Service's Android componentName for logging a11y services.
        String serviceName = getArguments().getString(ARG_SERVICE_NAME);
        if (TextUtils.isEmpty(serviceName)) {
            return TvSettingsEnums.PAGE_CLASSIC_DEFAULT;
        } else if (serviceName.contains("TalkBack")) {
            return TvSettingsEnums.SYSTEM_A11Y_TALKBACK;
        } else if (serviceName.contains("AccessibilityMenu")) {
            return TvSettingsEnums.SYSTEM_A11Y_A11Y_MENU;
        } else if (serviceName.contains("SelectToSpeak")) {
            return TvSettingsEnums.SYSTEM_A11Y_STS;
        } else if (serviceName.contains("SwitchAccess")) {
            return TvSettingsEnums.SYSTEM_A11Y_SWITCH_ACCESS;
        } else {
            return TvSettingsEnums.PAGE_CLASSIC_DEFAULT;
        }
    }
}
