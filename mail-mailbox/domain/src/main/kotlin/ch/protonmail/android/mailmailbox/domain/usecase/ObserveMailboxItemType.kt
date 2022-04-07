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
import ch.protonmail.android.mailsettings.domain.ObserveMailSettings
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.mailsettings.domain.entity.ViewMode
import javax.inject.Inject

class ObserveMailboxItemType @Inject constructor(
    private val observeMailSettings: ObserveMailSettings,
) {
    operator fun invoke() = observeMailSettings().mapLatest {
        when (it?.viewMode?.enum) {
            ViewMode.ConversationGrouping -> MailboxItemType.Conversation
            else -> MailboxItemType.Message
        }
    }
}
