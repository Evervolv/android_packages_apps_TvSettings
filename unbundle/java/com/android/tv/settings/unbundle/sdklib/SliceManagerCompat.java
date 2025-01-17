/*
 * Copyright (C) 2021 The Android Open Source Project
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

import com.android.tv.twopanelsettings.slices.base.SliceManager;
import android.net.Uri;

public class SliceManagerCompat {
    private final SliceManager mManager;

    public SliceManagerCompat(SliceManager sliceManager) {
        this.mManager = sliceManager;
    }

    public void grantPermissionFromUser(Uri uri, String pkg, boolean allSlices) {
        mManager.grantPermissionFromUser(uri, pkg, allSlices);
    }
}
