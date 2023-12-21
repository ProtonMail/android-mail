/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper

import androidx.annotation.StringRes
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInterval
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockIntervalUiModel
import javax.inject.Inject

class AutoLockIntervalsUiModelMapper @Inject constructor() {

    fun toIntervalsListUiModel(): List<AutoLockIntervalUiModel> =
        AutoLockInterval.values().map { AutoLockIntervalUiModel(it, it.getDescriptionStringRes()) }

    fun toSelectedIntervalUiModel(selectedInterval: AutoLockInterval) =
        AutoLockIntervalUiModel(selectedInterval, selectedInterval.getDescriptionStringRes())

    @StringRes
    private fun AutoLockInterval.getDescriptionStringRes(): Int = when (this) {
        AutoLockInterval.Immediately -> R.string.mail_settings_auto_lock_immediately
        AutoLockInterval.FiveMinutes -> R.string.mail_settings_auto_lock_item_timer_description_five_minutes
        AutoLockInterval.FifteenMinutes -> R.string.mail_settings_auto_lock_item_timer_description_fifteen_minutes
        AutoLockInterval.OneHour -> R.string.mail_settings_auto_lock_item_timer_description_one_hour
        AutoLockInterval.OneDay -> R.string.mail_settings_auto_lock_item_timer_description_one_day
    }
}
