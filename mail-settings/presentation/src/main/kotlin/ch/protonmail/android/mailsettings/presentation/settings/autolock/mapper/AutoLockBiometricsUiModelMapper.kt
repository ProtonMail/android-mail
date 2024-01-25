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

import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsState
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockBiometricsUiModel
import javax.inject.Inject

class AutoLockBiometricsUiModelMapper @Inject constructor() {

    fun toUiModel(state: AutoLockBiometricsState): AutoLockBiometricsUiModel {
        return when (state) {
            is AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled ->
                AutoLockBiometricsUiModel(
                    enabled = state.enabled,
                    biometricsEnrolled = true,
                    biometricsHwAvailable = true
                )

            is AutoLockBiometricsState.BiometricsAvailable.BiometricsNotEnrolled ->
                AutoLockBiometricsUiModel(
                    enabled = false,
                    biometricsEnrolled = false,
                    biometricsHwAvailable = true
                )

            is AutoLockBiometricsState.BiometricsNotAvailable ->
                AutoLockBiometricsUiModel(
                    enabled = false,
                    biometricsEnrolled = false,
                    biometricsHwAvailable = false
                )
        }
    }
}
