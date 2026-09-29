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
import ch.protonmail.android.mailnps.domain.model.NpsScore
import ch.protonmail.android.mailnps.domain.repository.NpsRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Records the user's NPS [score] (and optional [comment]) for [userId]. Recording is what marks the prompt
 * as seen for the backend's 180-day window, so no separate "mark seen" call is needed. A blank comment is
 * normalised to `null` so telemetry never carries whitespace-only text.
 */
class SubmitNpsFeedback @Inject constructor(
    private val npsRepository: NpsRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        score: NpsScore,
        comment: String?
    ): Either<DataError, Unit> = npsRepository.recordScore(userId, score, comment?.trim()?.ifBlank { null })
}
