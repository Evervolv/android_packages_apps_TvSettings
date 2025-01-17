/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tv.twopanelsettings.slices.compat.widget;

import java.util.List;

/**
 * The slice items we can render on the available space.
 *
 */
// @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
// @Deprecated // Supported for TV
class DisplayedListItems {
    private final List<SliceContent> mDisplayedItems;
    private final int mHiddenItemCount;

    DisplayedListItems(List<SliceContent> displayedItems, int hiddenItemCount) {
        mDisplayedItems = displayedItems;
        mHiddenItemCount = hiddenItemCount;
    }

    List<SliceContent> getDisplayedItems() {
        return mDisplayedItems;
    }

    int getHiddenItemCount() {
        return mHiddenItemCount;
    }
}
