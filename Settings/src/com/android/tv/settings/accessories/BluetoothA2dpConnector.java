/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.List;

public class BluetoothA2dpConnector implements BluetoothDevicePairer.BluetoothConnector {

    public static final String TAG = "BluetoothA2dpConnector";

    private static final boolean DEBUG = false;

    private static final int MSG_CONNECT_TIMEOUT = 1;
    private static final int MSG_CONNECT = 2;

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int CONNECT_DELAY = 1000;

    private Context mContext;
    private BluetoothDevice mTarget;
    private BluetoothDevicePairer.OpenConnectionCallback mOpenConnectionCallback;
    private BluetoothA2dp mA2dpProfile;
    private boolean mConnectionStateReceiverRegistered = false;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_CONNECT_TIMEOUT:
                    if (isTargetConnected()) { // False timeout
                        mA2dpProfile.setConnectionPolicy(mTarget,
                                BluetoothProfile.CONNECTION_POLICY_ALLOWED);
                        succeeded();
                    } else {
                        Log.w(TAG, "handleMessage(MSG_CONNECT_TIMEOUT)");
                        failed();
                    }
                    break;
                case MSG_CONNECT:
                    if (mA2dpProfile == null) {
                        break;
                    }
                    // must set CONNECTION_POLICY_ALLOWED or auto-connection will not
                    // occur, however this setting does not appear to be sticky
                    // across a reboot
                    Log.i(TAG, "handleMessage(MSG_CONNECT)");
                    mA2dpProfile.setConnectionPolicy(mTarget, BluetoothProfile.CONNECTION_POLICY_ALLOWED);
                    break;
                default:
                    Log.d(TAG, "handleMessage(" + m.what + "): unhandled");
                    break;
            }
        }
    };

    private BroadcastReceiver mConnectionStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device =
                    (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (DEBUG) {
                Log.d(TAG, "There was a connection status change for: " + device.getAddress());
            }

            if (!device.equals(mTarget)) {
                return;
            }

            if (BluetoothDevice.ACTION_UUID.equals(intent.getAction())) {
                // regardless of the UUID content, at this point, we're sure we can initiate a
                // profile connection.
                mHandler.sendEmptyMessageDelayed(MSG_CONNECT_TIMEOUT, CONNECT_TIMEOUT_MS);
                if (!mHandler.hasMessages(MSG_CONNECT)) {
                    mHandler.sendEmptyMessageDelayed(MSG_CONNECT, CONNECT_DELAY);
                }
            } else { // BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED

                int previousState = intent.getIntExtra(
                        BluetoothA2dp.EXTRA_PREVIOUS_STATE, BluetoothA2dp.STATE_CONNECTING);
                int state = intent.getIntExtra(
                        BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_CONNECTING);

                if (DEBUG) {
                    Log.d(TAG, "Connection states: old = " + previousState + ", new = " + state);
                }

                if (previousState == BluetoothA2dp.STATE_CONNECTING) {
                    if (state == BluetoothA2dp.STATE_CONNECTED) {
                        Log.i(TAG, "onReceive(): connected");
                        succeeded();
                    } else if (state == BluetoothA2dp.STATE_DISCONNECTED) {
                        Log.e(TAG, "onReceive(): Failed to connect");
                        failed();
                    }

                    unregisterConnectionStateReceiver();
                    closeA2dpProfileProxy();
                }
            }
        }
    };

    private void succeeded() {
        Log.d(TAG, "succeeded()");
        mHandler.removeCallbacksAndMessages(null);
        mOpenConnectionCallback.succeeded();
    }

    private void failed() {
        Log.e(TAG, "failed()");
        mHandler.removeCallbacksAndMessages(null);
        mOpenConnectionCallback.failed();
    }

    private BluetoothProfile.ServiceListener mServiceConnection =
            new BluetoothProfile.ServiceListener() {

        @Override
        public void onServiceDisconnected(int profile) {
            Log.d(TAG, "onServiceDisconnected(" + profile + ")");
            unregisterConnectionStateReceiver();
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (DEBUG) {
                Log.d(TAG, "Connection made to bluetooth proxy." );
            }
            mA2dpProfile = (BluetoothA2dp) proxy;
            Log.d(TAG, "onServiceConnected(" + profile
                    +
                    ", ...): Connecting to target: " + mTarget.getAddress());

            if (isTargetConnected()) { // Already connected, no need to wait.
                mA2dpProfile.setConnectionPolicy(mTarget,
                        BluetoothProfile.CONNECTION_POLICY_ALLOWED);
                succeeded();
                return;
            }

            registerConnectionStateReceiver();
            // We initiate SDP because connecting to A2DP before services are discovered leads to
            // error.
            mTarget.fetchUuidsWithSdp();
        }
    };

    private BluetoothA2dpConnector() {
    }

    public BluetoothA2dpConnector(Context context, BluetoothDevice target,
                                  BluetoothDevicePairer.OpenConnectionCallback callback) {
        mContext = context;
        mTarget = target;
        mOpenConnectionCallback = callback;
    }

    @Override
    public void openConnection(BluetoothAdapter adapter) {
        if (DEBUG) {
            Log.d(TAG, "opening connection");
        }
        if (!adapter.getProfileProxy(mContext, mServiceConnection, BluetoothProfile.A2DP)) {
            failed();
        }
    }

    @Override
    public void dispose() {
        unregisterConnectionStateReceiver();
        closeA2dpProfileProxy();
    }

    private void closeA2dpProfileProxy() {
        mHandler.removeCallbacksAndMessages(null);
        if (mA2dpProfile != null) {
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                adapter.closeProfileProxy(BluetoothProfile.A2DP, mA2dpProfile);
                mA2dpProfile = null;
            } catch (Throwable t) {
                Log.w(TAG, "Error cleaning up A2DP proxy", t);
            }
        }
    }

    private void registerConnectionStateReceiver() {
        Log.d(TAG, "registerConnectionStateReceiver()");
        IntentFilter filter = new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        mContext.registerReceiver(mConnectionStateReceiver, filter);
        mConnectionStateReceiverRegistered = true;
    }

    private void unregisterConnectionStateReceiver() {
        if (mConnectionStateReceiverRegistered) {
            Log.d(TAG, "unregisterConnectionStateReceiver()");
            mContext.unregisterReceiver(mConnectionStateReceiver);
            mConnectionStateReceiverRegistered = false;
        }
    }

    private boolean isTargetConnected() {
        if (mA2dpProfile == null) {
            return false;
        }

        List<BluetoothDevice> connectedDevices = mA2dpProfile.getConnectedDevices();
        return connectedDevices.contains(mTarget);
    }
}
