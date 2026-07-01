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
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class DisableContentSearch @Inject constructor(
    private val setContentSearchEnabled: SetContentSearchEnabled
) {

    // Disabling only clears the flag: the running sweep observes the change, drops this account
    // from its eligible set and pauses it. There is no per-account worker to cancel.
    suspend operator fun invoke(userId: UserId): Either<DataError, Unit> = setContentSearchEnabled(userId, false)
}
