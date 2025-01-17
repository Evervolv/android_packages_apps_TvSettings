/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tv.twopanelsettings.slices;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.tv.twopanelsettings.R;
import com.android.tv.twopanelsettings.slices.compat.core.SliceActionImpl;

/**
 * A preference that represents a slice provided by another app.
 */
public class SlicePreference extends Preference implements HasSliceAction, HasSliceUri,
        HasCustomContentDescription {

    private static final String TAG = "SlicePreference";
    private static final String SEPARATOR = ",";

    private String mUri;
    private int mActionId;
    private SliceActionImpl mAction;
    private SliceActionImpl mFollowUpAction;
    private String mContentDescription;

    public SlicePreference(Context context) {
        super(context);
        init(null);
    }

    public SlicePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (!TextUtils.isEmpty(mContentDescription)) {
            holder.itemView.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            holder.itemView.setContentDescription(mContentDescription);
        }
    }

    protected void init(@Nullable AttributeSet attrs) {
        if (attrs != null) {
            initStyleAttributes(attrs);
        }
    }

    private void initStyleAttributes(AttributeSet attrs) {
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.SlicePreference);
        for (int i = a.getIndexCount() - 1; i >= 0; i--) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.SlicePreference_uri) {
                mUri = a.getString(attr);
                break;
            }
        }
    }

    public void setUri(String uri) {
        mUri = uri;
    }

    public String getUri() {
        return mUri;
    }

    @Override
    public int getActionId() {
        return mActionId;
    }

    @Override
    public void setActionId(int actionId) {
        mActionId = actionId;
    }

    public void setSliceAction(SliceActionImpl action) {
        mAction = action;
    }

    @Override
    public SliceActionImpl getFollowupSliceAction() {
        return mFollowUpAction;
    }

    @Override
    public void setFollowupSliceAction(SliceActionImpl sliceAction) {
        mFollowUpAction = sliceAction;
    }

    @Override
    public SliceActionImpl getSliceAction() {
        return mAction;
    }

    /**
     * Sets the accessibility content description that will be read to the TalkBack users when they
     * select this preference.
     */
    public void setContentDescription(String contentDescription) {
        this.mContentDescription = contentDescription;
    }

    public String getContentDescription() {
        return mContentDescription;
    }
}
