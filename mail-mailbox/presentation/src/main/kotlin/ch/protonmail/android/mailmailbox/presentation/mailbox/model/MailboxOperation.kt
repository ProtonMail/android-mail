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

package ch.protonmail.android.mailmailbox.presentation.mailbox.model

import ch.protonmail.android.maillabel.domain.model.MailLabel
import me.proton.core.mailsettings.domain.entity.ViewMode

internal sealed interface MailboxOperation {
    sealed interface AffectingTopAppBar
    sealed interface AffectingUnreadFilter
    sealed interface AffectingMailboxList
}

internal sealed interface MailboxViewAction : MailboxOperation {
    object EnterSelectionMode : MailboxViewAction, MailboxOperation.AffectingTopAppBar
    object ExitSelectionMode : MailboxViewAction, MailboxOperation.AffectingTopAppBar
    data class OpenItemDetails(val item: MailboxItemUiModel) : MailboxViewAction
    object Refresh : MailboxViewAction
    object EnableUnreadFilter : MailboxViewAction, MailboxOperation.AffectingUnreadFilter
    object DisableUnreadFilter : MailboxViewAction, MailboxOperation.AffectingUnreadFilter
}

internal sealed interface MailboxEvent : MailboxOperation {
    data class ItemDetailsOpenedInViewMode(
        val item: MailboxItemUiModel,
        val preferredViewMode: ViewMode
    ) : MailboxEvent

    data class NewLabelSelected(
        val selectedLabel: MailLabel,
        val selectedLabelCount: Int?
    ) : MailboxEvent, MailboxOperation.AffectingTopAppBar, MailboxOperation.AffectingUnreadFilter

    data class SelectedLabelChanged(
        val selectedLabel: MailLabel
    ) : MailboxEvent, MailboxOperation.AffectingTopAppBar

    data class SelectedLabelCountChanged(
        val selectedLabelCount: Int
    ) : MailboxEvent, MailboxOperation.AffectingUnreadFilter
}


