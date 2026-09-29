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

package ch.protonmail.android.mailnps.domain.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailnps.domain.model.NpsScore
import me.proton.core.domain.entity.UserId

/**
 * NPS submission. Eligibility is owned by the backend and signalled through the `NPSFeedbackAndroidMail`
 * feature flag, so the client keeps no local "seen" state: recording a score or a dismissal is what marks
 * the prompt as seen server-side and turns the flag off, and the 180-day window is enforced by the backend.
 */
interface NpsRepository {

    /** Records the user's [score] and optional [comment] for [userId]. */
    suspend fun recordScore(
        userId: UserId,
        score: NpsScore,
        comment: String?
    ): Either<DataError, Unit>

    /** Records that the user dismissed the prompt for [userId]. */
    suspend fun dismiss(userId: UserId): Either<DataError, Unit>
}
