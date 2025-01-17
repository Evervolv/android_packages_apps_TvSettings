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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.android.tv.settings.connectivity.util.State;
import com.android.tv.settings.connectivity.util.StateMachine;

/**
 * State responsible for determining whether to enter the advanced flow.
 */
public class OptionsOrConnectState implements State {
    private final FragmentActivity mActivity;
    private Fragment mFragment;

    public OptionsOrConnectState(FragmentActivity activity) {
        mActivity = activity;
    }

    @Override
    public void processForward() {
        mFragment = null;
        StateMachine stateMachine = ViewModelProviders.of(mActivity).get(StateMachine.class);
        UserChoiceInfo userChoiceInfo = ViewModelProviders.of(mActivity).get(UserChoiceInfo.class);
        UserChoiceInfo.ConnectionFailedStatus status = userChoiceInfo.getConnectionFailedStatus();
        if (status == UserChoiceInfo.ConnectionFailedStatus.AUTHENTICATION) {
            userChoiceInfo.setConnectionFailedStatus(null);
            stateMachine.getListener().onComplete(this, StateMachine.RESTART);
        } else if (status != null) {
            userChoiceInfo.setConnectionFailedStatus(null);
            stateMachine.getListener().onComplete(this, StateMachine.ENTER_ADVANCED_FLOW);
        } else {
            userChoiceInfo.setConnectionFailedStatus(null);
            stateMachine.getListener().onComplete(this, StateMachine.CONNECT);
        }
    }

    @Override
    public void processBackward() {
        mFragment = null;
        StateMachine stateMachine = ViewModelProviders.of(mActivity).get(StateMachine.class);
        UserChoiceInfo userChoiceInfo = ViewModelProviders.of(mActivity).get(UserChoiceInfo.class);
        userChoiceInfo.setConnectionFailedStatus(null);
        stateMachine.back();
    }

    @Override
    public Fragment getFragment() {
        return mFragment;
    }
}
