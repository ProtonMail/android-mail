
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

package ch.protonmail.android.mailsettings.data.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.domain.model.AlternativeRoutingPreference
import ch.protonmail.android.mailsettings.data.repository.local.AlternativeRoutingLocalDataSource
import ch.protonmail.android.mailsettings.domain.repository.AlternativeRoutingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AlternativeRoutingRepositoryImpl @Inject constructor(
    private val alternativeRoutingLocalDataSource: AlternativeRoutingLocalDataSource
) : AlternativeRoutingRepository {

    override fun observe(): Flow<Either<PreferencesError, AlternativeRoutingPreference>> =
        alternativeRoutingLocalDataSource.observe()

    override suspend fun save(
        alternativeRoutingPreference: AlternativeRoutingPreference
    ): Either<PreferencesError, Unit> = alternativeRoutingLocalDataSource.save(alternativeRoutingPreference)
}
