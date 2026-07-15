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

package ch.protonmail.android.mailsidebar.presentation.usecase

import ch.protonmail.android.maillabel.domain.model.CategorySystemLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelIdWithCategory
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ResolveLocalCategoryLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCategoryAwareUnreadCount
import ch.protonmail.android.mailsettings.domain.usecase.ObserveUserPreferredViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Observes the unread count of the Primary category within the Inbox.
 *
 * The Primary category id is resolved directly from Rust via [ResolveLocalCategoryLabelId]
 *
 * Emits `null` when the ids can't be resolved, letting the caller fall back to the label's
 * unfiltered unread count.
 */
class ObservePrimaryCategoryUnreadCount @Inject constructor(
    private val observeUserPreferredViewMode: ObserveUserPreferredViewMode,
    private val observeMailLabels: ObserveMailLabels,
    private val resolveLocalCategoryLabelId: ResolveLocalCategoryLabelId,
    private val observeCategoryAwareUnreadCount: ObserveCategoryAwareUnreadCount
) {

    operator fun invoke(userId: UserId): Flow<Int?> = observeUserPreferredViewMode(userId)
        .flatMapLatest {
            observeInboxPrimaryLabelWithCategory(userId).flatMapLatest { labelWithCategory ->
                if (labelWithCategory == null) flowOf(null)
                else observeCategoryAwareUnreadCount(userId, labelWithCategory)
            }
        }

    private fun observeInboxPrimaryLabelWithCategory(userId: UserId): Flow<MailLabelIdWithCategory?> =
        observeMailLabels(userId).map { mailLabels ->
            val inboxLabelId = mailLabels.inboxLabelId() ?: return@map null
            val primaryCategoryId = resolveLocalCategoryLabelId(userId, CategorySystemLabelId.Primary)
                ?: return@map null
            MailLabelIdWithCategory(
                mailLabelId = MailLabelId.System(inboxLabelId),
                categoryLabelId = primaryCategoryId
            )
        }

    private fun MailLabels.inboxLabelId() = system
        .firstOrNull { it.systemLabelId == SystemLabelId.Inbox }
        ?.id
        ?.labelId
}
