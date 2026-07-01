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

package ch.protonmail.android.mailcontentsearch.domain.usecase

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchSettingsRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class EnableContentSearch @Inject constructor(
    private val settingsRepository: ContentSearchSettingsRepository,
    private val preferencesRepository: ContentSearchPreferencesRepository
) {

    // Enabling turns the flag on and clears any explicit opt-out, so an account the user previously
    // disabled is treated as opted-in again. Symmetric to [DisableContentSearch], which records the opt-out.
    suspend operator fun invoke(userId: UserId): Either<DataError, Unit> =
        settingsRepository.setEnabled(userId, true).onRight { preferencesRepository.clearUserOptedOut(userId) }
}
