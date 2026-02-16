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

package ch.protonmail.android.mailevents.domain.usecase

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailevents.domain.repository.EventsRepository
import javax.inject.Inject

class TrackSignupEvent @Inject constructor(
    private val eventsRepository: EventsRepository
) {

    suspend operator fun invoke(): Either<DataError, Unit> {
        if (eventsRepository.hasSignupEventBeenSent()) {
            return Either.Right(Unit)
        }

        return eventsRepository.sendEvent(
            AppEvent.AccountCreated(
                registrationMethod = null,
                referralCode = null
            )
        ).also {
            if (it.isRight()) {
                eventsRepository.markSignupEventSent()
            }
        }
    }
}
