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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import arrow.core.Either
import ch.protonmail.android.mailcategory.domain.model.CategoryLabelId
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import javax.inject.Inject

class SetActiveCategoryLabel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) {

    operator fun invoke(categoryLabelId: CategoryLabelId, viewMode: ViewMode): Either<PaginationError, Unit> =
        when (viewMode) {
            ViewMode.ConversationGrouping ->
                conversationRepository.setActiveCategoryLabel(categoryLabelId)

            ViewMode.NoConversationGrouping ->
                messageRepository.setActiveCategoryLabel(categoryLabelId)
        }
}
