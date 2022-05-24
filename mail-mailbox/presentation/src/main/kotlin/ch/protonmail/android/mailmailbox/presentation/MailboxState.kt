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

package ch.protonmail.android.mailmailbox.presentation

import androidx.compose.runtime.Stable
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation
import ch.protonmail.android.mailmailbox.presentation.model.MailboxTopAppBarState

@Stable
data class MailboxState(
    val topAppBar: MailboxTopAppBarState,
    val selectedLocation: SidebarLocation?,
    val unread: Int,
    val openItemEffect: Effect<OpenMailboxItemRequest>
) {

    companion object {

        val Loading = MailboxState(
            topAppBar = MailboxTopAppBarState.Loading,
            selectedLocation = null,
            unread = 0,
            openItemEffect = Effect.empty()
        )
    }
}
