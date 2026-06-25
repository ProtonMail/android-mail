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

import arrow.core.Either
import arrow.core.left
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class UpdateCategoryView @Inject constructor(
    private val userSessionRepository: UserSessionRepository
) {

    suspend operator fun invoke(enabled: Boolean): Either<DataError, Unit> {
        val userId = userSessionRepository.observePrimaryUserId().firstOrNull()
            ?: return DataError.Local.NoUserSession.left()

        return userSessionRepository.updateCategoryView(userId, enabled)
    }
}
