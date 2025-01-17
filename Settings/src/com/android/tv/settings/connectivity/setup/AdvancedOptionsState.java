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
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import androidx.lifecycle.ViewModelProviders;

import com.android.tv.settings.R;
import com.android.tv.settings.connectivity.WifiConfigHelper;
import com.android.tv.settings.connectivity.util.State;
import com.android.tv.settings.connectivity.util.StateMachine;

import java.util.List;

/**
 * State responsible for showing first page of advanced flow.
 */
public class AdvancedOptionsState implements State {
    private final FragmentActivity mActivity;
    private Fragment mFragment;

    public AdvancedOptionsState(FragmentActivity activity) {
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

        mFragment = new AdvancedOptionsFragment();
        FragmentChangeListener listener = (FragmentChangeListener) mActivity;
        if (listener != null) {
            listener.onFragmentChange(mFragment, true);
        }
    }

    @Override
    public void processBackward() {
        mFragment = new AdvancedOptionsFragment();
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
     * Fragment that makes user to decide whether to use advanced flow to set up network.
     */
    public static class AdvancedOptionsFragment extends WifiConnectivityGuidedStepFragment {
        private StateMachine mStateMachine;
        private AdvancedOptionsFlowInfo mAdvancedOptionsFlowInfo;
        private UserChoiceInfo mUserChoiceInfo;

        private String getWifiSsid() {
            WifiConfiguration wifiConfiguration = mUserChoiceInfo.getWifiConfiguration();
            if (wifiConfiguration != null) {
                String ssid = WifiInfo.sanitizeSsid(wifiConfiguration.SSID);
                if (!TextUtils.isEmpty(ssid)) {
                    return ssid;
                }
            }
            return mAdvancedOptionsFlowInfo.getPrintableSsid();
        }

        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(
                    R.string.title_wifi_advanced_options,
                    getWifiSsid()
            );
            return new GuidanceStylist.Guidance(title, null, null, null);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            mAdvancedOptionsFlowInfo = ViewModelProviders
                    .of(getActivity())
                    .get(AdvancedOptionsFlowInfo.class);
            mUserChoiceInfo = ViewModelProviders
                    .of(getActivity())
                    .get(UserChoiceInfo.class);
            mStateMachine = ViewModelProviders
                    .of(getActivity())
                    .get(StateMachine.class);
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            Context context = getActivity();
            actions.add(new GuidedAction.Builder(context)
                    .title(R.string.wifi_action_advanced_no)
                    .id(GuidedAction.ACTION_ID_NO)
                    .build());
            actions.add(new GuidedAction.Builder(context)
                    .title(R.string.wifi_action_advanced_yes)
                    .id(GuidedAction.ACTION_ID_YES)
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            mAdvancedOptionsFlowInfo.put(AdvancedOptionsFlowInfo.ADVANCED_OPTIONS,
                    action.getTitle());
            if (action.getId() == GuidedAction.ACTION_ID_NO) {
                mStateMachine.getListener().onComplete(this, StateMachine.ADVANCED_FLOW_COMPLETE);
            } else if (action.getId() == GuidedAction.ACTION_ID_YES) {
                mStateMachine.getListener().onComplete(this, StateMachine.CONTINUE);
            }
        }
    }
}
