/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmailbox.domain.usecase

import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation
import ch.protonmail.android.mailmailbox.domain.model.SystemLabelId
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ViewMode
import javax.inject.Inject

class ObserveMailboxItemType @Inject constructor(
    private val observeMailSettings: ObserveMailSettings,
) {

    private val messagesOnlyLabelsIds = listOf(
        SystemLabelId.DRAFT,
        SystemLabelId.ALL_DRAFT,
        SystemLabelId.SENT,
        SystemLabelId.ALL_SENT
    ).map { it.asLabelId() }

    operator fun invoke(location: SidebarLocation): Flow<MailboxItemType> =
        if (location.labelId in messagesOnlyLabelsIds) flowOf(MailboxItemType.Message)
        else invoke()

    operator fun invoke(): Flow<MailboxItemType> = observeMailSettings().mapLatest {
        when (it?.viewMode?.enum) {
            ViewMode.ConversationGrouping -> MailboxItemType.Conversation
            else -> MailboxItemType.Message
        }
    }
}
