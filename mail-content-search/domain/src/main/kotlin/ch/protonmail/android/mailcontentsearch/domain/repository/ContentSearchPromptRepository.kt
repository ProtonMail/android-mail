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

package ch.protonmail.android.mailcontentsearch.domain.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

/**
 * Whether each account has answered each of the one-off content search messages.
 *
 * [ContentSearchPrompt.SetUpOffer] is offered by the feature spotlight and by the search screen's
 * banner. Both write here, so answering one stops the other from asking again.
 */
interface ContentSearchPromptRepository {

    fun observeDismissed(userId: UserId, prompt: ContentSearchPrompt): Flow<Either<PreferencesError, Boolean>>

    suspend fun dismiss(userId: UserId, prompt: ContentSearchPrompt): Either<PreferencesError, Unit>

    /** Forgets the account's answer, so the message is shown again. */
    suspend fun reset(userId: UserId, prompt: ContentSearchPrompt): Either<PreferencesError, Unit>
}
