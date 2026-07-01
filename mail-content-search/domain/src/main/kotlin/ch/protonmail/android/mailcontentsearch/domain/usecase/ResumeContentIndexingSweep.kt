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

import ch.protonmail.android.mailcontentsearch.domain.model.EnqueueIndexingResult
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentIndexingScheduler
import javax.inject.Inject

/**
 * Ensures a sweep is running without disturbing one already in progress (KEEP policy). Used when the
 * app returns to the foreground so a sweep that was cancelled (e.g. via the notification's Cancel
 * action) resumes, while an in-progress sweep is left to continue untouched.
 */
class ResumeContentIndexingSweep @Inject constructor(
    private val scheduler: ContentIndexingScheduler,
    private val isContentSearchAllowedOnMobileData: IsContentSearchAllowedOnMobileData
) {

    suspend operator fun invoke(): EnqueueIndexingResult =
        scheduler.enqueueSweep(allowMobileData = isContentSearchAllowedOnMobileData(), replaceExisting = false)
}
