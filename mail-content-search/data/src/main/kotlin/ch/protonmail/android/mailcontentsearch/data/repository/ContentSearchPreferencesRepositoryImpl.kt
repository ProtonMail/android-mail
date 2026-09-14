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

package ch.protonmail.android.mailcontentsearch.data.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import ch.protonmail.android.mailsettings.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ContentSearchPreferencesRepositoryImpl @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository
) : ContentSearchPreferencesRepository {

    override suspend fun getAllowMobileData(): Either<PreferencesError, Boolean> =
        appSettingsRepository.observeAppSettings().first().useMobileDataForContentSearchIndexing.enabled.right()

    override suspend fun setAllowMobileData(value: Boolean): Either<PreferencesError, Unit> =
        appSettingsRepository.updateUseMobileDataForContentSearch(value).fold(
            ifLeft = { PreferencesError.left() },
            ifRight = { Unit.right() }
        )
}
