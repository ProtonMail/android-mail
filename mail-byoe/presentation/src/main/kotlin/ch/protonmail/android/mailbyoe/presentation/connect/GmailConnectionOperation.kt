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

package ch.protonmail.android.mailbyoe.presentation.connect

import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionResult

sealed interface GmailConnectionOperation {

    sealed interface GmailConnectionAction : GmailConnectionOperation {
        data object RetryClicked : GmailConnectionAction
        data class PageClosed(val result: GmailConnectionResult) : GmailConnectionAction
        data object OpenInboxClicked : GmailConnectionAction
    }

    sealed interface GmailConnectionEvent : GmailConnectionOperation {
        data class PageReady(val url: String) : GmailConnectionEvent
        data object PreparationFailed : GmailConnectionEvent
        data object ConnectionFailed : GmailConnectionEvent
        data object ConnectionFinished : GmailConnectionEvent
        data class ConnectionSucceeded(val email: String?, val importEmails: Boolean) : GmailConnectionEvent
        data object InboxSelected : GmailConnectionEvent
    }
}
