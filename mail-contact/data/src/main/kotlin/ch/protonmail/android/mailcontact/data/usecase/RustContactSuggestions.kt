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

package ch.protonmail.android.mailcontact.data.usecase

import ch.protonmail.android.mailcommon.data.mapper.LocalDeviceContact
import uniffi.mail_uniffi.ContactSuggestionsResult
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.contactSuggestions
import javax.inject.Inject

/**
 * Wrapper around the Rust FFI call for contact suggestions.
 */
class RustContactSuggestions @Inject constructor() {

    suspend operator fun invoke(
        deviceContacts: List<LocalDeviceContact>,
        session: MailUserSession
    ): ContactSuggestionsResult = contactSuggestions(deviceContacts, session)
}

