/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;
import android.text.TextUtils;

import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

import java.util.function.Function;

/** Controls the update for battery tips card */
public class BatteryTipsController extends BasePreferenceController {

    private static final String TAG = "BatteryTipsController";
    private static final String ROOT_PREFERENCE_KEY = "battery_tips_category";
    private static final String CARD_PREFERENCE_KEY = "battery_tips_card";

    private final String[] mPowerAnomalyKeys;

    @VisibleForTesting
    BatteryTipsCardPreference mCardPreference;
    @VisibleForTesting
    PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    public BatteryTipsController(Context context) {
        super(context, ROOT_PREFERENCE_KEY);
        mPowerUsageFeatureProvider = FeatureFactory.getFeatureFactory()
            .getPowerUsageFeatureProvider();
        mPowerAnomalyKeys = context.getResources().getStringArray(R.array.power_anomaly_keys);
    }

    private boolean isTipsCardVisible() {
        // TODO: compared with the timestamp of last user dismiss action in sharedPreference.
        return mPowerUsageFeatureProvider.isBatteryTipsEnabled();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCardPreference = screen.findPreference(CARD_PREFERENCE_KEY);
    }

    @VisibleForTesting
    int getPowerAnomalyEventIndex(String powerAnomalyKey) {
        for (int index = 0; index < mPowerAnomalyKeys.length; index++) {
            if (mPowerAnomalyKeys[index].equals(powerAnomalyKey)) {
                return index;
            }
        }
        return -1;
    }

    private <T> T getInfo(PowerAnomalyEvent powerAnomalyEvent,
                          Function<WarningBannerInfo, T> warningBannerInfoSupplier,
                          Function<WarningItemInfo, T> warningItemInfoSupplier) {
        if (powerAnomalyEvent.hasWarningBannerInfo() && warningBannerInfoSupplier != null) {
            return warningBannerInfoSupplier.apply(powerAnomalyEvent.getWarningBannerInfo());
        } else if (powerAnomalyEvent.hasWarningItemInfo() && warningItemInfoSupplier != null) {
            return warningItemInfoSupplier.apply(powerAnomalyEvent.getWarningItemInfo());
        }
        return null;
    }

    private String getString(PowerAnomalyEvent powerAnomalyEvent,
                             Function<WarningBannerInfo, String> warningBannerInfoSupplier,
                             Function<WarningItemInfo, String> warningItemInfoSupplier,
                             int resourceId, int resourceIndex) {
        String string =
                getInfo(powerAnomalyEvent, warningBannerInfoSupplier, warningItemInfoSupplier);

        if (!TextUtils.isEmpty(string) || resourceId < 0) {
            return string;
        }

        if (resourceIndex >= 0) {
            string = mContext.getResources().getStringArray(resourceId)[resourceIndex];
        }

        return string;
    }

    @VisibleForTesting
    void handleBatteryTipsCardUpdated(PowerAnomalyEvent powerAnomalyEvent) {
        if (!isTipsCardVisible()) {
            mCardPreference.setVisible(false);
            return;
        }
        if (powerAnomalyEvent == null) {
            mCardPreference.setVisible(false);
            return;
        }

        // Get card preference strings and navigate fragment info
        final int index = getPowerAnomalyEventIndex(powerAnomalyEvent.getKey());

        String titleString = getString(powerAnomalyEvent, WarningBannerInfo::getTitleString,
                WarningItemInfo::getTitleString, R.array.power_anomaly_titles, index);
        if (titleString.isEmpty()) {
            mCardPreference.setVisible(false);
            return;
        }

        String mainBtnString = getString(powerAnomalyEvent,
                WarningBannerInfo::getMainButtonString, WarningItemInfo::getMainButtonString,
                R.array.power_anomaly_main_btn_strings, index);
        String dismissBtnString = getString(powerAnomalyEvent,
                WarningBannerInfo::getCancelButtonString, WarningItemInfo::getCancelButtonString,
                R.array.power_anomaly_dismiss_btn_strings, index);

        String destinationClassName = getString(powerAnomalyEvent,
                WarningBannerInfo::getMainButtonDestination,
                WarningItemInfo::getMainButtonDestination,
                -1, -1);
        Integer sourceMetricsCategory = getInfo(powerAnomalyEvent,
                WarningBannerInfo::getMainButtonSourceMetricsCategory,
                WarningItemInfo::getMainButtonSourceMetricsCategory);

        // Updated card preference and main button fragment launcher
        mCardPreference.setTitle(titleString);
        mCardPreference.setMainButtonLabel(mainBtnString);
        mCardPreference.setDismissButtonLabel(dismissBtnString);
        mCardPreference.setMainButtonLauncherInfo(destinationClassName, sourceMetricsCategory);
        mCardPreference.setVisible(true);
    }
}