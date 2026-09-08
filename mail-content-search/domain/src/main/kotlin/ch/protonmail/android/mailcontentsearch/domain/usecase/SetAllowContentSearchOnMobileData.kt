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

import ch.protonmail.android.mailcontentsearch.domain.ContentIndexingScheduler
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import javax.inject.Inject

/**
 * Records the app-wide toggle. It lands in the app settings Rust itself reads, so there is nothing to
 * fan out to the individual accounts: the orchestrator picks the new value up for all of them.
 *
 * The worker is a separate matter. Its network constraint is fixed at enqueue time, so it has to be
 * replaced for the new answer to reach it - see [ContentIndexingScheduler.rescheduleWorker].
 */
class SetAllowContentSearchOnMobileData @Inject constructor(
    private val repository: ContentSearchPreferencesRepository,
    private val scheduler: ContentIndexingScheduler
) {

    suspend operator fun invoke(value: Boolean) {
        repository.setAllowMobileData(value)
        scheduler.rescheduleWorker()
    }
}
