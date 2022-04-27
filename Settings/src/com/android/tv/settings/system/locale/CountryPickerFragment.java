/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.tv.settings.system.locale;


import static com.android.tv.settings.system.locale.LanguagePickerFragment.KEY_LOCALE_INFO;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocaleStore;
import com.android.tv.settings.RadioPreference;
import com.android.tv.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Locale;

/** Country picker settings screen for locale selection. */
@Keep
public class CountryPickerFragment extends SettingsPreferenceFragment {
    private static final String EXTRA_PARENT_LOCALE = "PARENT_LOCALE";
    private static final String TAG = "CountryPickerFragment";
    private static final String COUNTRY_PICKER_RADIO_GROUP = "country_picker_group";
    private static final boolean DEBUG = Build.isDebuggable();
    private LocaleDataViewModel mLocaleDataViewModel;


    public static Bundle prepareArgs(LocaleStore.LocaleInfo localeInfo) {
        Bundle b = new Bundle();
        b.putSerializable(EXTRA_PARENT_LOCALE, localeInfo);
        return b;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
        mLocaleDataViewModel = new ViewModelProvider(getActivity()).get(LocaleDataViewModel.class);
        final Context themedContext = getPreferenceManager().getContext();

        mLocaleDataViewModel.getCurrentLocale().observe(this, this::updateUI);

        LocaleStore.LocaleInfo parentLocale = (LocaleStore.LocaleInfo) getArguments()
                .getSerializable(EXTRA_PARENT_LOCALE);
        final PreferenceScreen screen =
                getPreferenceManager().createPreferenceScreen(themedContext);
        if (parentLocale != null) {
            screen.setTitle(parentLocale.getFullNameNative());
        }
        Locale currentLocale = mLocaleDataViewModel.getCurrentLocale().getValue();
        ArrayList<LocaleStore.LocaleInfo> localeInfoCountryList = mLocaleDataViewModel
                .getLocaleInfoList(parentLocale);
        Preference activePref = null;
        if (localeInfoCountryList != null) {
            for (LocaleStore.LocaleInfo localeInfo : localeInfoCountryList) {
                RadioPreference preference = new RadioPreference(getContext());
                preference.setTitle(LocaleHelper.getDisplayCountry(
                        localeInfo.getLocale(), localeInfo.getLocale()));
                if (localeInfo.getLocale().equals(currentLocale)) {
                    activePref = preference;
                    preference.setChecked(true);
                } else {
                    preference.setChecked(false);
                }
                preference.setRadioGroup(COUNTRY_PICKER_RADIO_GROUP);
                preference.getExtras().putSerializable(KEY_LOCALE_INFO, localeInfo);
                screen.addPreference(preference);
            }
        }
        if (activePref != null && savedInstanceState == null) {
            scrollToPreference(activePref);
        }

        setPreferenceScreen(screen);
    }

    private void updateUI(Locale locale) {
        if (DEBUG) {
            Log.d(TAG, "Updating UI, Locale changed to: " + locale);
        }
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (pref instanceof RadioPreference) {
                LocaleStore.LocaleInfo localeInfo = (LocaleStore.LocaleInfo)
                        pref.getExtras().getSerializable(KEY_LOCALE_INFO);
                if (localeInfo.getLocale() != null && localeInfo.getLocale().equals(locale)) {
                    ((RadioPreference) pref).clearOtherRadioPreferences(getPreferenceScreen());
                }
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof RadioPreference) {
            RadioPreference localePref = (RadioPreference) preference;
            LocaleStore.LocaleInfo localeInfo = localePref.getExtras().getSerializable(
                    KEY_LOCALE_INFO, LocaleStore.LocaleInfo.class);
            if (localeInfo != null) {
                getContext().getSystemService(ActivityManager.class).setDeviceLocales(
                        new LocaleList(localeInfo.getLocale()));
                mLocaleDataViewModel.getCurrentLocale().setValue(localeInfo.getLocale());
            }
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }
}
