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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;

import com.android.tv.twopanelsettings.R;
import com.android.tv.twopanelsettings.slices.InfoFragment;

@Keep
public class AccessibilityTimeoutInfoFragment extends InfoFragment {
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        ImageView imageView;
        imageView = (ImageView) view.findViewById(R.id.info_title_icon);
        imageView.setImageResource(R.drawable.ic_info_outline_base);
        imageView.setVisibility(View.VISIBLE);

        TextView textView;
        textView = (TextView) view.findViewById(R.id.info_title);
        textView.setText(R.string.accessibility_timeout_info_title);
        textView.setVisibility(View.VISIBLE);

        textView = (TextView) view.findViewById(R.id.info_summary);
        textView.setText(R.string.accessibility_timeout_info_description);
        textView.setVisibility(View.VISIBLE);

        return view;
    }
}
