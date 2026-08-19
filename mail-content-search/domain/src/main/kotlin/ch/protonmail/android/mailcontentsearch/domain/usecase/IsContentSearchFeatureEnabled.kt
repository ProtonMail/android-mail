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
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Whether the content search feature is available on this build for [userId].
 *
 * The SDK answers this from the user session, so an account with no session yet - signed out, or
 * still unlocking - reads as unavailable rather than as an app-wide "off".
 *
 * Not to be confused with [IsContentSearchEnabled], which reports whether the *user* has switched
 * content search on in settings.
 */
class IsContentSearchFeatureEnabled @Inject constructor(
    private val contentSearchRepository: ContentSearchRepository,
    private val deviceArchitectureProvider: DeviceArchitectureProvider
) {

    suspend operator fun invoke(userId: UserId): Boolean =
        deviceArchitectureProvider.is64Bit() && contentSearchRepository.isFeatureEnabled(userId)
}
