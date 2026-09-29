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

package ch.protonmail.android.mailnps.domain.usecase

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailnps.domain.repository.NpsRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Records an explicit dismissal (swiping the sheet away or tapping outside it) for [userId], so the backend
 * counts the non-responder and marks the prompt seen. Only an explicit dismissal records: if the
 * app is closed while the sheet is up without the user acting, nothing is recorded and the prompt returns.
 */
class DismissNps @Inject constructor(
    private val npsRepository: NpsRepository
) {

    suspend operator fun invoke(userId: UserId): Either<DataError, Unit> = npsRepository.dismiss(userId)
}
