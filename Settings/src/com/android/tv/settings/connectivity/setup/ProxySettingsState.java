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

package com.android.tv.settings.connectivity.setup;

import android.content.Context;
import android.net.IpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import androidx.lifecycle.ViewModelProviders;

import com.android.tv.settings.R;
import com.android.tv.settings.connectivity.WifiConfigHelper;
import com.android.tv.settings.connectivity.util.AdvancedOptionsFlowUtil;
import com.android.tv.settings.connectivity.util.State;
import com.android.tv.settings.connectivity.util.StateMachine;

import java.util.List;

/**
 * State responsible for choosing proxy setting.
 */
public class ProxySettingsState implements State {
    private final FragmentActivity mActivity;
    private Fragment mFragment;

    public ProxySettingsState(FragmentActivity activity) {
        mActivity = activity;
    }

    @Override
    public void processForward() {
        if (isNetworkLockedDown()) {
            StateMachine stateMachine = ViewModelProviders
                    .of(mActivity)
                    .get(StateMachine.class);
            stateMachine.getListener().onComplete(this, StateMachine.ADVANCED_FLOW_COMPLETE);
            return;
        }

        mFragment = new ProxySettingsFragment();
        FragmentChangeListener listener = (FragmentChangeListener) mActivity;
        if (listener != null) {
            listener.onFragmentChange(mFragment, true);
        }
    }

    @Override
    public void processBackward() {
        mFragment = new ProxySettingsFragment();
        FragmentChangeListener listener = (FragmentChangeListener) mActivity;
        if (listener != null) {
            listener.onFragmentChange(mFragment, false);
        }
    }

    private boolean isNetworkLockedDown() {
        UserChoiceInfo userChoiceInfo = ViewModelProviders
                .of(mActivity)
                .get(UserChoiceInfo.class);
        WifiConfiguration wifiConfiguration = userChoiceInfo.getWifiConfiguration();
        return WifiConfigHelper.isNetworkLockedDown(mActivity, wifiConfiguration);
    }

    @Override
    public Fragment getFragment() {
        return mFragment;
    }

    /**
     * Fragment that makes user choose proxy settings.
     */
    public static class ProxySettingsFragment extends WifiConnectivityGuidedStepFragment {
        private static final long WIFI_ACTION_PROXY_NONE = 100001;
        private static final long WIFI_ACTION_PROXY_MANUAL = 100002;
        private StateMachine mStateMachine;
        private AdvancedOptionsFlowInfo mAdvancedOptionsFlowInfo;

        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.title_wifi_proxy_settings);
            return new GuidanceStylist.Guidance(
                    title,
                    getString(R.string.proxy_warning_limited_support),
                    null,
                    null);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            mAdvancedOptionsFlowInfo = ViewModelProviders
                    .of(getActivity())
                    .get(AdvancedOptionsFlowInfo.class);
            mStateMachine = ViewModelProviders
                    .of(getActivity())
                    .get(StateMachine.class);
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            Context context = getActivity();
            actions.add(new GuidedAction.Builder(context)
                    .title(R.string.wifi_action_proxy_none)
                    .id(WIFI_ACTION_PROXY_NONE)
                    .build());
            actions.add(new GuidedAction.Builder(context)
                    .title(R.string.wifi_action_proxy_manual)
                    .id(WIFI_ACTION_PROXY_MANUAL)
                    .build());
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            CharSequence title = null;
            if (mAdvancedOptionsFlowInfo.containsPage(AdvancedOptionsFlowInfo.PROXY_SETTINGS)) {
                title = mAdvancedOptionsFlowInfo.get(AdvancedOptionsFlowInfo.PROXY_SETTINGS);
            } else if (mAdvancedOptionsFlowInfo.getIpConfiguration().getProxySettings()
                    == IpConfiguration.ProxySettings.NONE) {
                title = getString(R.string.wifi_action_proxy_none);
            } else if (mAdvancedOptionsFlowInfo.getInitialProxyInfo() != null) {
                title = getString(R.string.wifi_action_proxy_manual);
            }
            moveToPosition(title);
        }

        private void moveToPosition(CharSequence title) {
            if (title == null) return;
            for (int i = 0; i < getActions().size(); i++) {
                if (TextUtils.equals(getActions().get(i).getTitle(), title)) {
                    setSelectedActionPosition(i);
                    break;
                }
            }
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            mAdvancedOptionsFlowInfo.put(AdvancedOptionsFlowInfo.PROXY_SETTINGS,
                    action.getTitle());
            if (action.getId() == WIFI_ACTION_PROXY_NONE) {
                AdvancedOptionsFlowUtil.processProxySettings(getActivity());
                if (mAdvancedOptionsFlowInfo.isSettingsFlow()) {
                    mStateMachine.getListener().onComplete(this,
                            StateMachine.ADVANCED_FLOW_COMPLETE);
                } else {
                    mStateMachine.getListener().onComplete(this, StateMachine.IP_SETTINGS);
                }
            } else if (action.getId() == WIFI_ACTION_PROXY_MANUAL) {
                mStateMachine.getListener().onComplete(this, StateMachine.PROXY_HOSTNAME);
            }
        }
    }
}
