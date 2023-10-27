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

import static com.android.tv.settings.accessories.ConnectedDevicesSliceProvider.KEY_EXTRAS_DEVICE;
import static com.android.tv.settings.accessories.ConnectedDevicesSliceUtils.DIRECTION_BACK;
import static com.android.tv.settings.accessories.ConnectedDevicesSliceUtils.EXTRAS_DIRECTION;
import static com.android.tv.settings.accessories.ConnectedDevicesSliceUtils.EXTRAS_SLICE_URI;
import static com.android.tv.settings.accessories.ConnectedDevicesSliceUtils.notifyDeviceChanged;
import static com.android.tv.settings.accessories.ConnectedDevicesSliceUtils.notifyToGoBack;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings.Global;

/**
 * This broadcast receiver handles these cases:
 * (a) Bluetooth toggle on.
 * (b) The followup pending intent for "rename"/"forget" preference to notify TvSettings UI flow to
 * go back.
 */
public class ConnectedDevicesSliceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "ConnectedSliceReceiver";

    static final String ACTION_TOGGLE_CHANGED =
            "com.android.tv.settings.accessories.TOGGLE_CHANGED";
    // The extra to specify toggle type.
    static final String EXTRA_TOGGLE_TYPE = "TOGGLE_TYPE";
    static final String EXTRA_TOGGLE_STATE = "TOGGLE_STATE";
    // Bluetooth off is handled differently by ResponseActivity with confirmation dialog.
    static final String BLUETOOTH_ON = "BLUETOOTH_ON";
    /**
     * The {@link Global} integer setting name.
     *
     * <p>The settings tells whether the physical button integration for FMR feature is enabled.
     * Default value: 1. */
    static final String FMR_ON_PHYSICAL_BUTTON_ENABLED = "fmr_on_physical_button_enabled";
    static final String ACTIVE_AUDIO_OUTPUT = "ACTIVE_AUDIO_OUTPUT";

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
                  case FMR_ON_PHYSICAL_BUTTON_ENABLED -> {
                        Global.putInt(context.getContentResolver(),
                                FMR_ON_PHYSICAL_BUTTON_ENABLED, isChecked ? 1 : 0);
                        context.getContentResolver().notifyChange(
                                ConnectedDevicesSliceUtils.FMR_SLICE_URI, null);
                    }
                }
            }
        }

        // Notify TvSettings to go back to the previous level.
        String direction = intent.getStringExtra(EXTRAS_DIRECTION);
        if (DIRECTION_BACK.equals(direction)) {
            notifyToGoBack(context, Uri.parse(intent.getStringExtra(EXTRAS_SLICE_URI)));
        }
    }
}
