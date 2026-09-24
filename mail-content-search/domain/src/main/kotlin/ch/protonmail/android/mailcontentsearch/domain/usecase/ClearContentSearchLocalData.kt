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
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPromptRepository
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * Wipes the account's index and forgets that it was told indexing is done.
 *
 * The next indexing run then ends with the "indexing complete" note again.
 * Failing to forget the note is only logged, as the index is already gone by then.
 */
class ClearContentSearchLocalData @Inject constructor(
    private val repository: ContentSearchRepository,
    private val promptRepository: ContentSearchPromptRepository
) {

    suspend operator fun invoke(userId: UserId): Either<DataError, Unit> = repository.clearLocalData(userId)
        .onRight {
            promptRepository.reset(userId, ContentSearchPrompt.IndexingComplete)
                .onLeft { Timber.w("content-search: could not reset the indexing complete note for $userId") }
        }
}
