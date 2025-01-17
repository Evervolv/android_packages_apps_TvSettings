/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.tv.settings.accessories;

import static android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES;
import static android.content.Intent.FLAG_RECEIVER_FOREGROUND;
import static android.content.Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND;

import static com.android.tv.settings.accessories.ConnectedDevicesSliceProvider.KEY_EXTRAS_DEVICE;
import static com.android.tv.settings.accessories.ConnectedDevicesSliceUtils.DIRECTION_BACK;
import static com.android.tv.settings.accessories.ConnectedDevicesSliceUtils.EXTRAS_DIRECTION;
import static com.android.tv.settings.accessories.ConnectedDevicesSliceUtils.EXTRAS_SLICE_URI;
import static com.android.tv.settings.accessories.ConnectedDevicesSliceUtils.FIND_MY_REMOTE_PHYSICAL_BUTTON_ENABLED_SETTING;
import static com.android.tv.settings.accessories.ConnectedDevicesSliceUtils.notifyDeviceChanged;
import static com.android.tv.settings.accessories.ConnectedDevicesSliceUtils.notifyToGoBack;
import static com.android.tv.settings.accessories.ConnectedDevicesSliceUtils.setFindMyRemoteButtonEnabled;
import static com.android.tv.settings.accessories.ConnectedDevicesSliceUtils.setBacklightMode;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.android.tv.settings.R;
/**
 * This broadcast receiver handles these cases:
 * (a) Bluetooth toggle on.
 * (b) The followup pending intent for "rename"/"forget" preference to notify TvSettings UI flow to
 * go back.
 */
public class ConnectedDevicesSliceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "ConnectedSliceReceiver";

    static final String ACTION_FIND_MY_REMOTE = "com.google.android.tv.FIND_MY_REMOTE";
    static final String ACTION_TOGGLE_CHANGED =
            "com.android.tv.settings.accessories.TOGGLE_CHANGED";
    // The extra to specify toggle type.
    static final String EXTRA_TOGGLE_TYPE = "TOGGLE_TYPE";
    static final String EXTRA_TOGGLE_STATE = "TOGGLE_STATE";
    // Bluetooth off is handled differently by ResponseActivity with confirmation dialog.
    static final String BLUETOOTH_ON = "BLUETOOTH_ON";
    static final String ACTIVE_AUDIO_OUTPUT = "ACTIVE_AUDIO_OUTPUT";

    static final String BACKLIGHT_MODE = "BACKLIGHT_MODE";
    static final String ACTION_BACKLIGHT = "com.google.android.tv.BACKLIGHT";
    static final String KEY_BACKLIGHT_MODE = "key_backlight_mode";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Handle CEC control toggle.
        final String action = intent.getAction();
        if (ACTION_TOGGLE_CHANGED.equals(action)) {
            final boolean isChecked = intent.getBooleanExtra(EXTRA_TOGGLE_STATE, false);
            final String toggleType = intent.getStringExtra(EXTRA_TOGGLE_TYPE);
            if (toggleType != null) {
                switch (toggleType) {
                    case BLUETOOTH_ON -> {
                        if (AccessoryUtils.getDefaultBluetoothAdapter() != null) {
                            AccessoryUtils.getDefaultBluetoothAdapter().enable();
                        }
                        return;
                    }
                    case ACTIVE_AUDIO_OUTPUT -> {
                        boolean enable = intent.getBooleanExtra(EXTRA_TOGGLE_STATE, false);
                        BluetoothDevice device = intent.getParcelableExtra(KEY_EXTRAS_DEVICE,
                                BluetoothDevice.class);
                        AccessoryUtils.setActiveAudioOutput(enable ? device : null);
                        // refresh device
                        notifyDeviceChanged(context, device);
                    }
                    case FIND_MY_REMOTE_PHYSICAL_BUTTON_ENABLED_SETTING -> {
                        setFindMyRemoteButtonEnabled(context, isChecked);
                        context.getContentResolver().notifyChange(
                                ConnectedDevicesSliceUtils.FIND_MY_REMOTE_SLICE_URI, null);
                    }
                }
            }
        } else if (ACTION_FIND_MY_REMOTE.equals(action)) {
            context.sendBroadcast(
                    new Intent(ACTION_FIND_MY_REMOTE)
                            .putExtra("reason", "SETTINGS")
                            .setFlags(FLAG_INCLUDE_STOPPED_PACKAGES | FLAG_RECEIVER_FOREGROUND
                                    | FLAG_RECEIVER_INCLUDE_BACKGROUND),
                    "com.google.android.tv.permission.FIND_MY_REMOTE");
        } else if (ACTION_BACKLIGHT.equals(action)) {
            final int modes = intent.getIntExtra(BACKLIGHT_MODE, 1);
            // save user selection
            setBacklightMode(context, modes);

            context.sendBroadcast(
                    new Intent(ACTION_BACKLIGHT)
                            .putExtra(BACKLIGHT_MODE, modes)
                            .setFlags(FLAG_INCLUDE_STOPPED_PACKAGES | FLAG_RECEIVER_FOREGROUND
                                    | FLAG_RECEIVER_INCLUDE_BACKGROUND),
                    "com.google.android.tv.permission.BACKLIGHT");
        }

        // Notify TvSettings to go back to the previous level.
        String direction = intent.getStringExtra(EXTRAS_DIRECTION);
        if (DIRECTION_BACK.equals(direction)) {
            notifyToGoBack(context, Uri.parse(intent.getStringExtra(EXTRAS_SLICE_URI)));
        }
    }

    public static PendingIntent getBacklightModeIntent(
            Context context, Uri sliceUri, String backlightMode) {
        Intent intent = new Intent(context, ConnectedDevicesSliceBroadcastReceiver.class);

        // Intents are considered the same if only extras different. Thus putting backlightMode in
        // Uri query to create different PendingIntents for different backlightMode changing
        // requests.
        final Uri sliceUriWithQuery =
                sliceUri.buildUpon()
                        .appendQueryParameter(
                                KEY_BACKLIGHT_MODE, backlightMode)
                        .build();

        intent.setAction(ACTION_BACKLIGHT);

        final String[] backlightModes =
                context.getResources().getStringArray(R.array.backlight_modes);
        int modes = -1;
        for (int i = 0; i < backlightModes.length; i ++){
            if (backlightModes[i].equals(backlightMode)) {
                modes = i;
                break;
            }
        }
        intent.putExtra(BACKLIGHT_MODE, modes);
        intent.setData(sliceUriWithQuery);

        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
