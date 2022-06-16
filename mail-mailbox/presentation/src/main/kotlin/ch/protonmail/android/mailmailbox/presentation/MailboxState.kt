/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.mailmailbox.presentation

import androidx.compose.runtime.Stable
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.model.UnreadFilterState

@Stable
sealed interface MailboxState {

    val currentMailLabel: MailLabel?
        get() = null

    val openItemEffect: Effect<OpenMailboxItemRequest>
        get() = Effect.empty()

    val topAppBar: MailboxTopAppBarState
        get() = MailboxTopAppBarState.Loading

    val unreadFilterState: UnreadFilterState
        get() = UnreadFilterState.Loading

    @Stable
    data class Data(
        override val currentMailLabel: MailLabel?,
        override val openItemEffect: Effect<OpenMailboxItemRequest>,
        override val topAppBar: MailboxTopAppBarState,
        override val unreadFilterState: UnreadFilterState
    ) : MailboxState

    object Loading : MailboxState

    object NotLoggedIn : MailboxState
}
