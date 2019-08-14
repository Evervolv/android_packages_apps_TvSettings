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

package com.android.tv.twopanelsettings.slices.builders;

import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Pair;
import androidx.slice.Slice;
import androidx.slice.SliceSpecs;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.core.SliceHints;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

// TODO: Remove unused code and add test.
/**
 * Builder for constructing slices composed of rows of TvSettings style preferences.
 */
public class PreferenceSliceBuilder extends TemplateSliceBuilder {

    private PreferenceSliceBuilderImpl mImpl;

    /**
     * Constant representing infinity.
     */
    public static final long INFINITY = SliceHints.INFINITY;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        View.LAYOUT_DIRECTION_RTL, View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_INHERIT,
        View.LAYOUT_DIRECTION_LOCALE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LayoutDirection {

    }

    /**
     * Create a builder which will construct a slice made up of rows of content.
     *
     * @param uri Uri to tag for this slice.
     * @hide
     */
    public PreferenceSliceBuilder(@NonNull Context context, @NonNull Uri uri) {
        super(context, uri);
    }

    /**
     * Create a ListBuilder for constructing slice content.
     * <p>
     * A slice requires an associated time-to-live, i.e. how long the data contained in the slice
     * can remain fresh. If your slice has content that is not time sensitive, set a TTL of {@link
     * #INFINITY}.
     *
     * @param uri Uri to tag for this slice.
     * @param ttl the length in milliseconds that the content in this slice can live for.
     */
    public PreferenceSliceBuilder(@NonNull Context context, @NonNull Uri uri, long ttl) {
        super(context, uri);
        mImpl.setTtl(ttl);
    }

    /**
     * Create a ListBuilder for constructing slice content.
     * <p>
     * A slice requires an associated time-to-live, i.e. how long the data contained in the slice
     * can remain fresh. If your slice has content that is not time sensitive, set {@link Duration}
     * to null and the TTL will be {@link #INFINITY}.
     *
     * @param uri Uri to tag for this slice.
     * @param ttl the {@link Duration} that the content in this slice can live for.
     */
    public PreferenceSliceBuilder(@NonNull Context context, @NonNull Uri uri,
            @Nullable Duration ttl) {
        super(context, uri);
        mImpl.setTtl(ttl);
    }

    @Override
    protected TemplateBuilderImpl selectImpl(Uri uri) {
        return new PreferenceSliceBuilderImpl(getBuilder(), SliceSpecs.LIST, getClock());
    }

    /**
     * Construct the slice defined by this PreferenceSliceBuilder
     */
    @NonNull
    @Override
    public Slice build() {
        return mImpl.build();
    }

    @Override
    void setImpl(TemplateBuilderImpl impl) {
        mImpl = (PreferenceSliceBuilderImpl) impl;
    }

    /**
     * Add a preference builder.
     */
    public PreferenceSliceBuilder addPreference(RowBuilder builder) {
        mImpl.addPreference(builder);
        return this;
    }

    /**
     * Add a preferenceCategory builder.
     */
    public PreferenceSliceBuilder addPreferenceCategory(RowBuilder builder) {
        mImpl.addPreferenceCategory(builder);
        return this;
    }

    /** Set a custom screen title for slice. */
    public PreferenceSliceBuilder addScreenTitle(RowBuilder builder) {
        mImpl.addScreenTitle(builder);
        return this;
    }

    /** Indicates that the slice is not ready yet **/
    public PreferenceSliceBuilder setNotReady() {
        mImpl.setNotReady();
        return this;
    }

    public static class RowBuilder {

        private final Uri mUri;
        private boolean mHasEndActionOrToggle;
        private boolean mHasEndImage;
        private boolean mHasDefaultToggle;
        private boolean mTitleItemLoading;
        private IconCompat mTitleIcon;
        private SliceAction mTitleAction;
        private SliceAction mPrimaryAction;
        private SliceAction mFollowupAction;
        private CharSequence mTitle;
        private boolean mTitleLoading;
        private CharSequence mSubtitle;
        private boolean mSubtitleLoading;
        private CharSequence mContentDescription;
        private int mLayoutDirection = -1;
        private List<Object> mEndItems = new ArrayList<>();
        private List<Integer> mEndTypes = new ArrayList<>();
        private List<Boolean> mEndLoads = new ArrayList<>();
        private boolean mTitleActionLoading;
        private CharSequence mTargetSliceUri;
        private CharSequence mKey;
        private boolean mIconNeedsToBeProcessed;
        private boolean mIsCheckMark;

        public static final int TYPE_ICON = 1;
        public static final int TYPE_ACTION = 2;

        /**
         * Builder to construct a row.
         */
        public RowBuilder() {
            mUri = null;
        }

        /**
         * Builder to construct a normal row.
         *
         * @param uri Uri to tag for this slice.
         */
        public RowBuilder(Uri uri) {
            mUri = uri;
        }

        /**
         * Builder to construct a row.
         *
         * @param parent The builder constructing the parent slice.
         */
        public RowBuilder(@NonNull ListBuilder parent) {
            this();
        }

        /**
         * Builder to construct a row.
         *
         * @param uri Uri to tag for this slice.
         */
        public RowBuilder(@NonNull ListBuilder parent, @NonNull Uri uri) {
            this(uri);
        }

        /**
         * Builder to construct a normal row.
         *
         * @param uri Uri to tag for this slice.
         */
        public RowBuilder(@NonNull Context context, @NonNull Uri uri) {
            this(uri);
        }

        /**
         * Sets the title item to be the provided icon. There can only be one title item, this will
         * replace any other title items that may have been set.
         *
         * @param icon the image to display.
         */
        private RowBuilder setTitleItem(@NonNull IconCompat icon) {
            return setTitleItem(icon, false /* isLoading */);
        }

        /**
         * Sets the title item to be the provided icon. There can only be one title item, this will
         * replace any other title items that may have been set.
         * <p>
         * When set to true, the parameter {@code isLoading} indicates that the app is doing work to
         * load this content in the background, in this case the template displays a placeholder
         * until updated.
         *
         * @param icon the image to display.
         * @param isLoading whether this content is being loaded in the background.
         */
        @NonNull
        private RowBuilder setTitleItem(@Nullable IconCompat icon, boolean isLoading) {
            mTitleAction = null;
            mTitleIcon = icon;
            mTitleItemLoading = isLoading;
            return this;
        }

        /**
         * Sets the title item to be a tappable icon. There can only be one title item, this will
         * replace any other title items that may have been set.
         */
        @NonNull
        private RowBuilder setTitleItem(@NonNull SliceAction action) {
            return setTitleItem(action, false /* isLoading */);
        }

        /**
         * Sets the title item to be a tappable icon. There can only be one title item, this will
         * replace any other title items that may have been set.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         *
         * @param isLoading indicates whether the app is doing work to load the added content in the
         * background or not.
         */
        @NonNull
        private RowBuilder setTitleItem(@NonNull SliceAction action, boolean isLoading) {
            mTitleAction = action;
            mTitleIcon = null;
            mTitleActionLoading = isLoading;
            return this;
        }

        /**
         * Sets the icon for the preference builder.
         */
        @NonNull
        public RowBuilder setIcon(@NonNull IconCompat icon) {
            return setTitleItem(icon);
        }

        /**
         * The action specified here will be sent when the whole row is clicked.
         * <p>
         * If this is the first row in a {@link ListBuilder} this action will also be used to define
         * the {@link androidx.slice.widget.SliceView#MODE_SHORTCUT} representation of the slice.
         */
        @NonNull
        private RowBuilder setPrimaryAction(@NonNull SliceAction action) {
            mPrimaryAction = action;
            return this;
        }

        /**
         * Set a pendingIntent for the preference builder.
         * @param pendingIntent pendingIntent
         * @return builder
         */
        @NonNull
        public RowBuilder setPendingIntent(@NonNull PendingIntent pendingIntent) {
            return setPrimaryAction(new SliceAction(pendingIntent, "", false));
        }

        /**
         * Set a followup pendingIntent for the preference builder. After the initial pendingIntent
         * is launched and result is retrieved by TvSettings, TvSettings will pack the result into
         * the followup PendingIntent and launch it.
         * @param pendingIntent followup pendingIntent
         * @return builder
         */
        @NonNull
        public RowBuilder setFollowupPendingIntent(@NonNull PendingIntent pendingIntent) {
            mFollowupAction = new SliceAction(pendingIntent, "", false);
            return this;
        }

      /**
         * Sets the title for the row builder. A title should fit on a single line and is ellipsized
         * if too long.
         */
        @NonNull
        public RowBuilder setTitle(@NonNull CharSequence title) {
            return setTitle(title, false);
        }

        /**
         * Sets the title for the row builder. A title should fit on a single line and is ellipsized
         * if too long.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         *
         * @param isLoading indicates whether the app is doing work to load the added content in the
         * background or not.
         */
        @NonNull
        public RowBuilder setTitle(@Nullable CharSequence title, boolean isLoading) {
            mTitle = title;
            mTitleLoading = isLoading;
            return this;
        }

        /**
         * Sets the subtitle for the row builder. A subtitle should fit on a single line and is
         * ellipsized if too long.
         */
        @NonNull
        public RowBuilder setSubtitle(@NonNull CharSequence subtitle) {
            return setSubtitle(subtitle, false /* isLoading */);
        }

        /**
         * Sets the subtitle for the row builder. A subtitle should fit on a single line and is
         * ellipsized if too long.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         *
         * @param isLoading indicates whether the app is doing work to load the added content in the
         * background or not.
         */
        @NonNull
        public RowBuilder setSubtitle(@Nullable CharSequence subtitle, boolean isLoading) {
            mSubtitle = subtitle;
            mSubtitleLoading = isLoading;
            return this;
        }

        /**
         * Adds an icon to the end items of the row builder.
         *
         * @param icon the image to display.
         */
        @NonNull
        private RowBuilder addEndItem(@NonNull IconCompat icon) {
            return addEndItem(icon, false /* isLoading */);
        }

        /**
         * Adds an icon to the end items of the row builder.
         * <p>
         * When set to true, the parameter {@code isLoading} indicates that the app is doing work to
         * load this content in the background, in this case the template displays a placeholder
         * until updated.
         *
         * @param icon the image to display.
         * @param isLoading whether this content is being loaded in the background.
         */
        @NonNull
        private RowBuilder addEndItem(@Nullable IconCompat icon, boolean isLoading) {
            if (mHasEndActionOrToggle) {
                throw new IllegalArgumentException("Trying to add an icon to end items when an"
                    + "action has already been added. End items cannot have a mixture of "
                    + "actions and icons.");
            }
            mEndItems.add(new Pair<>(icon, 0));
            mEndTypes.add(TYPE_ICON);
            mEndLoads.add(isLoading);
            mHasEndImage = true;
            return this;
        }

        /**
         * Adds an action to the end items of the row builder. A mixture of icons and actions is not
         * permitted. If an icon has already been added, this will throw {@link
         * IllegalArgumentException}.
         */
        @NonNull
        private RowBuilder addEndItem(@NonNull SliceAction action) {
            return addEndItem(action, false /* isLoading */);
        }


        /**
         * Add a switch for the preference.
         * @param pendingIntent pendingIntent
         * @param actionTitle title for the switch, also used for contentDescription.
         * @param isChecked the state of the switch
         * @return
         */
        @NonNull
        public PreferenceSliceBuilder.RowBuilder addSwitch(
                PendingIntent pendingIntent, @NonNull CharSequence actionTitle, boolean isChecked) {
            SliceAction switchAction = new SliceAction(pendingIntent, actionTitle, isChecked);
            return addEndItem(switchAction);
        }

        /**
         * Adds an action to the end items of the row builder. A mixture of icons and actions is not
         * permitted. If an icon has already been added, this will throw {@link
         * IllegalArgumentException}.
         * <p>
         * Use this method to specify content that will appear in the template once it's been
         * loaded.
         * </p>
         *
         * @param isLoading indicates whether the app is doing work to load the added content in the
         * background or not.
         */
        @NonNull
        private RowBuilder addEndItem(@NonNull SliceAction action, boolean isLoading) {
            if (mHasEndImage) {
                throw new IllegalArgumentException("Trying to add an action to end items when an"
                    + "icon has already been added. End items cannot have a mixture of "
                    + "actions and icons.");
            }
            if (mHasDefaultToggle) {
                throw new IllegalStateException("Only one non-custom toggle can be added "
                    + "in a single row. If you would like to include multiple toggles "
                    + "in a row, set a custom icon for each toggle.");
            }
            mEndItems.add(action);
            mEndTypes.add(TYPE_ACTION);
            mEndLoads.add(isLoading);
            mHasDefaultToggle = action.getImpl().isDefaultToggle();
            mHasEndActionOrToggle = true;
            return this;
        }

        /**
         * Sets the content description for the row.
         */
        @NonNull
        public RowBuilder setContentDescription(@NonNull CharSequence description) {
            mContentDescription = description;
            return this;
        }

        /**
         * Set the target slice uri for the builder.
         * @param targetSliceUri indicates the target slice uri when the preference is focused.
         * @return builder
         */
        public RowBuilder setTargetSliceUri(@NonNull CharSequence targetSliceUri) {
            mTargetSliceUri = targetSliceUri;
            return this;
        }

        /**
         * Set the key for the builder.
         * @param key indicates the key for the preference.
         * @return builder
         */
        public RowBuilder setKey(@NonNull CharSequence key) {
            mKey = key;
            return this;
        }

        /**
         * Sets the desired layout direction for the content in this row.
         *
         * @param layoutDirection the layout direction to set.
         */
        @NonNull
        public RowBuilder setLayoutDirection(@LayoutDirection int layoutDirection) {
            mLayoutDirection = layoutDirection;
            return this;
        }

        /**
         * Set whether the toggle use a checkmark style. Otherwise, a switch style is used.
         * @param isCheckMark use checkmark
         */
        @NonNull
        public RowBuilder setCheckmark(boolean isCheckMark) {
            mIsCheckMark = isCheckMark;
            return this;
        }

        /**
         * Set whether the icon needs to be processed by TvSettings.
         * @param needed if true, TvSettings will add a round border around the given icon
         */
        @NonNull
        public RowBuilder setIconNeedsToBeProcessed(boolean needed) {
            mIconNeedsToBeProcessed = needed;
            return this;
        }

        /**
         *
         */
        public boolean iconNeedsToBeProcessed() {
            return mIconNeedsToBeProcessed;
        }

        /**
         *
         */
        public boolean isCheckMark() {
            return mIsCheckMark;
        }

        /**
         * Get the target slice uri.
         */
        public CharSequence getTargetSliceUri() {
            return mTargetSliceUri;
        }

        /** Get the key for the builder */
        public CharSequence getKey() {
            return mKey;
        }

        public Uri getUri() {
            return mUri;
        }

        public boolean hasEndActionOrToggle() {
            return mHasEndActionOrToggle;
        }

        public boolean hasEndImage() {
            return mHasEndImage;
        }

        public boolean hasDefaultToggle() {
            return mHasDefaultToggle;
        }

        public boolean isTitleItemLoading() {
            return mTitleItemLoading;
        }

        public IconCompat getTitleIcon() {
            return mTitleIcon;
        }

        public SliceAction getTitleAction() {
            return mTitleAction;
        }

        public SliceAction getPrimaryAction() {
            return mPrimaryAction;
        }

        public SliceAction getFollowupAction() {
            return mFollowupAction;
        }

        public CharSequence getTitle() {
            return mTitle;
        }

        public boolean isTitleLoading() {
            return mTitleLoading;
        }

        public CharSequence getSubtitle() {
            return mSubtitle;
        }

        public boolean isSubtitleLoading() {
            return mSubtitleLoading;
        }

        public CharSequence getContentDescription() {
            return mContentDescription;
        }

        public int getLayoutDirection() {
            return mLayoutDirection;
        }

        public List<Object> getEndItems() {
            return mEndItems;
        }

        public List<Integer> getEndTypes() {
            return mEndTypes;
        }

        public List<Boolean> getEndLoads() {
            return mEndLoads;
        }

        public boolean isTitleActionLoading() {
            return mTitleActionLoading;
        }
    }
}
