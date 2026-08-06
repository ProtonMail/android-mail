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
import ch.protonmail.android.mailcontentsearch.domain.ContentIndexingScheduler
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Puts [userId] at the front of the orchestrator's queue, and a worker behind it.
 *
 * Used when the user turns content search on for an account: without it the account would only be
 * picked up whenever the orchestrator next looks for a candidate.
 *
 * The worker matters as much as the start does. Turning the toggle on is very often followed by
 * leaving the app, and without a foreground service behind it the process is free to die on the way
 * out - leaving the account indexed no further than the seconds the settings screen was open, until
 * some later foreground transition happens to enqueue one.
 */
class StartContentIndexingForUser @Inject constructor(
    private val repository: ContentSearchRepository,
    private val scheduler: ContentIndexingScheduler
) {

    suspend operator fun invoke(userId: UserId): Either<DataError, Unit> =
        repository.startIndexingForUser(userId).onRight { scheduler.ensureWorkerRunning() }
}
