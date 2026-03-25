/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailspotlight.domain.usecase

import ch.protonmail.android.mailevents.domain.repository.AppInstallRepository
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class IsRecentAppInstall @Inject constructor(
    private val appInstallRepository: AppInstallRepository,
    private val clock: Clock
) {

    operator fun invoke(threshold: Duration = 1.days): Boolean {
        val installTime = appInstallRepository.getFirstInstallTime()
        val now = clock.now().toEpochMilliseconds()
        return now - installTime < threshold.inWholeMilliseconds
    }
}
