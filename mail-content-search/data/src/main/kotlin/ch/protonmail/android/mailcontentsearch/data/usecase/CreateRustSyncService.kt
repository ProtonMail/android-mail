/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.data.usecase

import ch.protonmail.android.mailcontentsearch.data.wrapper.SyncServiceWrapper
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import uniffi.mail_uniffi.SyncService
import javax.inject.Inject

class CreateRustSyncService @Inject constructor() {

    /**
     * Safe to call once per operation instead of caching an instance: as of the current SDK,
     * `SyncService(userSession)` only wraps a weak pointer and delegates every call to the one
     * shared sync actor already registered for the user session - it does not spawn a new
     * worker or hold independent state per construction.
     */
    operator fun invoke(userSession: MailUserSessionWrapper) =
        SyncServiceWrapper(SyncService(userSession.getRustUserSession()))
}
