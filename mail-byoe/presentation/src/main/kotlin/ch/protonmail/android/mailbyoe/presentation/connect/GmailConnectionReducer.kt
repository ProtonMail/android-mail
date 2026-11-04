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

import ch.protonmail.android.mailbyoe.presentation.connect.GmailConnectionOperation.GmailConnectionAction
import ch.protonmail.android.mailbyoe.presentation.connect.GmailConnectionOperation.GmailConnectionEvent
import ch.protonmail.android.mailcommon.presentation.Effect
import javax.inject.Inject

class GmailConnectionReducer @Inject constructor() {

    fun newStateFrom(current: GmailConnectionState, operation: GmailConnectionOperation): GmailConnectionState =
        when (operation) {
            is GmailConnectionAction.RetryClicked -> GmailConnectionState.Loading
            is GmailConnectionAction.PageClosed,
            is GmailConnectionAction.OpenInboxClicked -> current
            is GmailConnectionEvent.PageReady -> GmailConnectionState.OpeningPage(Effect.of(operation.url))
            is GmailConnectionEvent.PreparationFailed,
            is GmailConnectionEvent.ConnectionFailed -> GmailConnectionState.Failed
            is GmailConnectionEvent.ConnectionFinished -> GmailConnectionState.Finished(Effect.of(Unit))
            is GmailConnectionEvent.UpsellReady -> GmailConnectionState.UpsellRequested(Effect.of(operation.visibility))
            is GmailConnectionEvent.ConnectionSucceeded ->
                GmailConnectionState.Connected(
                    email = operation.email,
                    importEmails = operation.importEmails,
                    openInbox = Effect.empty()
                )
            is GmailConnectionEvent.InboxSelected ->
                if (current is GmailConnectionState.Connected) current.copy(openInbox = Effect.of(Unit)) else current
        }
}
