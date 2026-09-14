/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.domain.usecase

import ch.protonmail.android.mailcommon.domain.system.DeviceArchitectureProvider
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import javax.inject.Inject
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsContentSearchFeatureEnabled as IsContentSearchFeatureEnabledFlag

/**
 * Whether the content search feature is available on this build.
 *
 * Other than checking the FF, we check the device arch (as 32-bit is unsupported).
 */
class IsContentSearchFeatureEnabled @Inject constructor(
    @IsContentSearchFeatureEnabledFlag private val contentSearchFlag: FeatureFlag<Boolean>,
    private val deviceArchitectureProvider: DeviceArchitectureProvider
) {

    suspend operator fun invoke(): Boolean = deviceArchitectureProvider.is64Bit() && contentSearchFlag.get()
}
