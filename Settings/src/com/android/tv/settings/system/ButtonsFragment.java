/*
 * Copyright (C) 2021 The LineageOS Project
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
 * limitations under the License
 */

package com.android.tv.settings.system;

import android.app.tvsettings.TvSettingsEnums;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;

import evervolv.provider.EVSettings;

/**
 * The button settings screen in TV settings.
 */
@Keep
public class ButtonsFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String KEY_ADVANCED_REBOOT = "advanced_reboot";

    public static ButtonsFragment newInstance() {
        return new ButtonsFragment();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.buttons, null);

        TwoStatePreference advancedReboot = findPreference(KEY_ADVANCED_REBOOT);
        advancedReboot.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (KEY_ADVANCED_REBOOT.equals(preference.getKey())) {
            EVSettings.Secure.putInt(getContext().getContentResolver(),
                    EVSettings.Secure.ADVANCED_REBOOT, (Boolean) newValue ? 1 : 0);
        }
        return true;
    }
}
