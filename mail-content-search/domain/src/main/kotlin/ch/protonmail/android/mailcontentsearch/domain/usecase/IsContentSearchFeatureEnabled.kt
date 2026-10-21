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
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchSettingsRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Whether the content search feature is available to the given account.
 *
 * Rust owns the answer. It reads the launch flags and rules out 32-bit devices.
 * A failed read counts as unavailable.
 *
 * The 64-bit check repeats the one in Rust on purpose.
 * It keeps 32-bit devices out once the launch flags are removed.
 */
class IsContentSearchFeatureEnabled @Inject constructor(
    private val contentSearchSettingsRepository: ContentSearchSettingsRepository,
    private val deviceArchitectureProvider: DeviceArchitectureProvider
) {

    suspend operator fun invoke(userId: UserId): Boolean = deviceArchitectureProvider.is64Bit() &&
        contentSearchSettingsRepository.isFeatureEnabled(userId).getOrNull() == true
}
