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

package com.android.tv.settings.connectivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.EthernetManager;
import android.net.EthernetManager.InterfaceStateListener;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.android.settingslib.utils.ThreadUtils;
import com.android.tv.settings.library.network.AccessPoint;
import com.android.tv.settings.library.network.WifiTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Listens for changes to the current connectivity status.
 */
public class ConnectivityListener implements WifiTracker.WifiListener {
    private static final String TAG = "ConnectivityListener";

    private final Context mContext;
    private Listener mListener;

    private WifiTracker mWifiTracker;

    private final ConnectivityManager mConnectivityManager;
    private final WifiManager mWifiManager;
    private final EthernetManager mEthernetManager;
    private WifiNetworkListener mWifiListener;
    private final BroadcastReceiver mNetworkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateConnectivityStatus();
            if (mListener != null) {
                mListener.onConnectivityChange();
            }
        }
    };
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mCellSignalStrength = signalStrength;
            mListener.onConnectivityChange();
        }
    };

    private SignalStrength mCellSignalStrength;
    private int mNetworkType;
    private String mWifiSsid;
    private int mWifiSignalStrength;
    private LinkProperties mLateLp = null;
    private final InterfaceStateListener mEthernetListener;
    private final ArrayMap<String, IpConfiguration> mAvailableInterfaces = new ArrayMap<>();
    private final Handler mUiHandler = ThreadUtils.getUiThreadHandler();

    public ConnectivityListener(Context context, Listener listener, Lifecycle lifecycle) {
        mContext = context;
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        mWifiManager = mContext.getSystemService(WifiManager.class);
        mEthernetManager = mContext.getSystemService(EthernetManager.class);
        mListener = listener;
        if (mWifiManager != null) {
            lifecycle.addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onStart(LifecycleOwner owner) {
                    start();
                }

                @Override
                public void onStop(@NonNull LifecycleOwner owner) {
                    stop();
                }
            });

            mWifiTracker = new WifiTracker(context, this, lifecycle, mWifiManager,
                    mConnectivityManager);
        }
        mEthernetListener = (iface, state, role, configuration) -> {
            if (state == EthernetManager.STATE_LINK_UP) {
                mAvailableInterfaces.put(iface, configuration);
            } else {
                mAvailableInterfaces.remove(iface);
            }
            updateConnectivityStatus();
            if (mListener != null) {
                mListener.onConnectivityChange();
            }
        };
        updateConnectivityStatus();
    }

    private void start() {
        updateConnectivityStatus();
        IntentFilter networkIntentFilter = new IntentFilter();
        networkIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        networkIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        networkIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        networkIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        mContext.registerReceiver(mNetworkReceiver, networkIntentFilter);
        final TelephonyManager telephonyManager = mContext
                .getSystemService(TelephonyManager.class);
        if (telephonyManager != null) {
            telephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }
        mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback, mUiHandler);
        if (mEthernetManager != null) {
            mEthernetManager.addInterfaceStateListener(mContext.getMainExecutor(),
                    mEthernetListener);
        }
    }

    private void stop() {
        mContext.unregisterReceiver(mNetworkReceiver);
        mWifiListener = null;
        final TelephonyManager telephonyManager = mContext
                .getSystemService(TelephonyManager.class);
        if (telephonyManager != null) {
            telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        if (mEthernetManager != null) {
            mEthernetManager.removeInterfaceStateListener(mEthernetListener);
        }
    }

    public void setWifiListener(WifiNetworkListener wifiListener) {
        mWifiListener = wifiListener;
    }

    public String getWifiIpAddress() {
        if (isWifiConnected()) {
            Network network = mWifiManager.getCurrentNetwork();
            return formatIpAddresses(network);
        } else {
            return "";
        }
    }

    /**
     * Return the MAC address of the currently connected Wifi AP.
     */
    @SuppressLint("HardwareIds")
    public String getWifiMacAddress(AccessPoint ap) {
        if (isWifiConnected() && mWifiManager.getConnectionInfo() != null) {
            return mWifiManager.getConnectionInfo().getMacAddress();
        }
        if (ap != null) {
            WifiConfiguration wifiConfig = ap.getConfig();
            if (wifiConfig != null
                    && isWifiMacAddressRandomized(ap)) {
                return wifiConfig.getRandomizedMacAddress().toString();
            }
        }

        // return device MAC address
        final String[] macAddresses = mWifiManager.getFactoryMacAddresses();
        if (macAddresses != null && macAddresses.length > 0) {
            return macAddresses[0];
        }

        Log.e(TAG, "Unable to get MAC address");
        return "";
    }

    /**
     * Return whether the connected Wifi supports MAC address randomization.
     */
    public boolean isMacAddressRandomizationSupported() {
        return mWifiManager.isConnectedMacRandomizationSupported();
    }

    /**
     * Return whether the MAC address of the currently connected Wifi AP is randomized.
     */
    public int getWifiMacRandomizationSetting(AccessPoint ap) {
        if (ap == null || ap.getConfig() == null) {
            return WifiConfiguration.RANDOMIZATION_NONE;
        }
        return ap.getConfig().macRandomizationSetting;
    }

    /**
     * Return whether the randomized MAC address is used.
     */
    public boolean isWifiMacAddressRandomized(AccessPoint ap) {
        return getWifiMacRandomizationSetting(ap) != WifiConfiguration.RANDOMIZATION_NONE;
    }

    /**
     * Apply the setting of whether to use MAC address randimization.
     */
    public void applyMacRandomizationSetting(AccessPoint ap, boolean enable) {
        if (ap != null && ap.getConfig() != null) {
            ap.getConfig().macRandomizationSetting = enable
                    ? WifiConfiguration.RANDOMIZATION_PERSISTENT
                    : WifiConfiguration.RANDOMIZATION_NONE;
            mWifiManager.updateNetwork(ap.getConfig());
            // To activate changing, we need to reconnect network. WiFi will auto connect to
            // current network after disconnect(). Only needed when this is connected network.
            final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getNetworkId() == ap.getConfig().networkId) {
                mWifiManager.disconnect();
            }
        }
    }

    public boolean isEthernetConnected() {
        return mNetworkType == ConnectivityManager.TYPE_ETHERNET;
    }

    public boolean isWifiConnected() {
        if (mNetworkType == ConnectivityManager.TYPE_WIFI) {
            return true;
        } else {
            if (mWifiManager != null) {
                WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
                return connectionInfo.getNetworkId() != -1;
            }
        }
        return false;
    }

    public boolean isCellConnected() {
        return mNetworkType == ConnectivityManager.TYPE_MOBILE;
    }

    private void ensureRunningOnUiThread() {
        if (mUiHandler.getLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Not running on the UI thread: "
                    + Thread.currentThread().getName());
        }
    }

    private boolean isEthernetEnabled() {
        return mEthernetManager != null;
    }

    /**
     * Return whether Ethernet port is available.
     */
    public boolean isEthernetAvailable() {
        ensureRunningOnUiThread();
        return isEthernetEnabled() && (mAvailableInterfaces.size() > 0);
    }

    private Network getFirstEthernet() {
        final Network[] networks = mConnectivityManager.getAllNetworks();
        for (final Network network : networks) {
            NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                return network;
            }
        }
        return null;
    }

    private String formatIpAddresses(Network network) {
        final LinkProperties linkProperties = mConnectivityManager.getLinkProperties(network);
        if (linkProperties == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        boolean gotAddress = false;
        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
            if (gotAddress) {
                sb.append("\n");
            }
            sb.append(linkAddress.getAddress().getHostAddress());
            gotAddress = true;
        }
        if (gotAddress) {
            return sb.toString();
        } else {
            return null;
        }
    }

    /**
     * Returns the formatted IP addresses of the Ethernet connection or null
     * if none available.
     */
    public String getEthernetIpAddress() {
        final Network network = getFirstEthernet();
        if (network == null) {
            return null;
        }
        return formatIpAddresses(network);
    }


    /**
     * Get the current Ethernet interface name.
     */
    public String getEthernetInterfaceName() {
        ensureRunningOnUiThread();
        if (mAvailableInterfaces.size() == 0) return null;
        return mAvailableInterfaces.keyAt(0);
    }

    /**
     * Get the current IP configuration of Ethernet interface.
     */
    public IpConfiguration getEthernetIpConfiguration() {
        ensureRunningOnUiThread();
        if (mAvailableInterfaces.size() == 0) return null;
        return mAvailableInterfaces.valueAt(0);
    }

    public int getWifiSignalStrength(int maxLevel) {
        if (mWifiManager != null) {
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            return WifiManager.calculateSignalLevel(wifiInfo.getRssi(), maxLevel);
        }
        return 0;
    }

    public int getCellSignalStrength() {
        if (isCellConnected() && mCellSignalStrength != null) {
            return mCellSignalStrength.getLevel();
        } else {
            return 0;
        }
    }

    /**
     * Return a list of wifi networks. Ensure that if a wifi network is connected that it appears
     * as the first item on the list.
     */
    public List<AccessPoint> getAvailableNetworks() {
        return mWifiTracker == null ? new ArrayList<>() : mWifiTracker.getAccessPoints();
    }

    public boolean isWifiEnabledOrEnabling() {
        return mWifiManager != null
                && (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED
                || mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING);
    }

    public void setWifiEnabled(boolean enable) {
        if (mWifiManager != null) {
            mWifiManager.setWifiEnabled(enable);
        }
    }

    private void updateConnectivityStatus() {
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            mNetworkType = ConnectivityManager.TYPE_NONE;
        } else {
            switch (networkInfo.getType()) {
                case ConnectivityManager.TYPE_WIFI: {

                    if (mWifiManager == null) {
                        break;
                    }
                    // Determine if this is
                    // an open or secure wifi connection.
                    mNetworkType = ConnectivityManager.TYPE_WIFI;

                    String ssid = getSsid();
                    if (!TextUtils.equals(mWifiSsid, ssid)) {
                        mWifiSsid = ssid;
                    }

                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                    // Calculate the signal strength.
                    int signalStrength;
                    if (wifiInfo != null) {
                        // Calculate the signal strength between 0 and 3.
                        signalStrength = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 4);
                    } else {
                        signalStrength = 0;
                    }
                    if (mWifiSignalStrength != signalStrength) {
                        mWifiSignalStrength = signalStrength;
                    }
                    break;
                }

                case ConnectivityManager.TYPE_ETHERNET:
                    mNetworkType = ConnectivityManager.TYPE_ETHERNET;
                    break;

                case ConnectivityManager.TYPE_MOBILE:
                    mNetworkType = ConnectivityManager.TYPE_MOBILE;
                    break;

                default:
                    mNetworkType = ConnectivityManager.TYPE_NONE;
                    break;
            }
        }
    }

    @Override
    public void onWifiStateChanged(int state) {
        updateConnectivityStatus();
        if (mListener != null) {
            mListener.onConnectivityChange();
        }
    }

    @Override
    public void onConnectedChanged() {
        updateConnectivityStatus();
        if (mListener != null) {
            mListener.onConnectivityChange();
        }
    }

    @Override
    public void onAccessPointsChanged() {
        if (mWifiListener != null) {
            mWifiListener.onWifiListChanged();
        }
    }

    public void onIpAddrChanged() {
        if (mListener != null) {
            mListener.onConnectivityChange();
        }
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public interface Listener {
        void onConnectivityChange();
    }

    private final NetworkCallback mNetworkCallback = new NetworkCallback() {
        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties lp) {

            if (mLateLp != null) {
                if (!Objects.equals(mLateLp, lp)) {
                    if ((lp.hasIpv4Address() && !mLateLp.hasIpv4Address())
                            || (lp.hasGlobalIpv6Address() && !mLateLp.hasGlobalIpv6Address())) {
                        onIpAddrChanged();
                    }
                }
            }
            mLateLp = lp;
        }
    };

    public interface WifiNetworkListener {
        void onWifiListChanged();
    }

    /**
     * Get the SSID of current connected network.
     *
     * @return SSID
     */
    public String getSsid() {
        if (mWifiManager == null) {
            return null;
        }
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        // Find the SSID of network.
        String ssid = null;
        if (wifiInfo != null) {
            ssid = wifiInfo.getSSID();
            if (ssid != null) {
                ssid = sanitizeSsid(ssid);
            }
        }
        return ssid;
    }

    public static String sanitizeSsid(@Nullable String string) {
        return removeDoubleQuotes(string);
    }

    public static String removeDoubleQuotes(@Nullable String string) {
        if (string == null) return null;
        final int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }
}
